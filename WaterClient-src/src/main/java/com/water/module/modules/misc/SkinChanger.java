package com.water.module.modules.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.util.SkinData;
import com.water.util.SkinLookup;
import com.water.util.SkinTextureEntry;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.AssetInfo.TextureAsset;

public final class SkinChanger extends Module {
   public static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10L);
   public static final long INPUT_DEBOUNCE_MS = 600L;
   public static final String PRIMARY_LOOKUP_URL = "https://api.mojang.com/users/profiles/minecraft/";
   public static final String FALLBACK_LOOKUP_URL = "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
   public static final String PROFILE_LOOKUP_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
   public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).followRedirects(Redirect.NORMAL).build();
   public static volatile SkinTextures overrideSkin;
   public static volatile TextureAsset overrideTextureAsset;
   public final Setting<String> playerName = new Setting<>("Player Name", "");
   public final AtomicInteger requestGeneration = new AtomicInteger();
   public PlayerSkinTextureDownloader skinDownloader;
   public String lastObservedName = "";
   public String lastRequestedName = "";
   public long lastNameEditAt;

   public SkinChanger() {
      super("SkinChanger", Category.MISC);
      this.addSetting(this.playerName);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      this.lastObservedName = trimOrEmpty(this.playerName.getValue());
      this.lastRequestedName = "";
      this.lastNameEditAt = System.currentTimeMillis();
      if (this.lastObservedName.isEmpty()) {
         clearOverrideSkin();
      } else {
         this.requestSkin(this.lastObservedName);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.requestGeneration.incrementAndGet();
      this.lastRequestedName = "";
      clearOverrideSkin();
   }

   @Override
   public void onTick() {
      String s = trimOrEmpty(this.playerName.getValue());
      if (!Objects.equals(s, this.lastObservedName)) {
         this.lastObservedName = s;
         this.lastNameEditAt = System.currentTimeMillis();
      } else if (!s.isEmpty()) {
         if (!Objects.equals(s, this.lastRequestedName) && System.currentTimeMillis() - this.lastNameEditAt >= 600L) {
            this.requestSkin(s);
         }
      } else {
         if (overrideSkin != null || overrideTextureAsset != null) {
            this.lastRequestedName = "";
            clearOverrideSkin();
         }
      }
   }

   public static SkinTextures getOverrideSkinFor(UUID uUID) {
      if (overrideSkin != null && uUID != null) {
         UUID uuid = getSelfUuid();
         return uuid != null && uuid.equals(uUID) ? overrideSkin : null;
      } else {
         return null;
      }
   }

   public void requestSkin(String text) {
      this.lastRequestedName = text;
      int i = this.requestGeneration.incrementAndGet();
      CompletableFuture.<SkinLookup>supplyAsync(() -> this.lookupSkin(text), Util.getIoWorkerExecutor().named("skinchanger-lookup"))
         .thenCompose(
            var1x -> this.getSkinDownloader()
               .downloadAndRegisterTexture(this.buildTextureId(var1x), this.getCachePath(var1x.uuid()), var1x.textureUrl(), true)
               .thenApply(var1xx -> new SkinTextureEntry(var1x, var1xx))
         )
         .whenComplete((var3, var4) -> mc.execute(() -> {
            if (i == this.requestGeneration.get() && this.isEnabled()) {
               if (var4 != null) {
                  this.sendChatMessage("Failed to apply skin for " + text + ": " + getRootMessage(var4));
               } else {
                  applyOverrideSkin(var3.textureAsset(), var3.lookup().skinType());
                  this.sendChatMessage("Applied skin from " + var3.lookup().playerName() + ".");
               }
            } else {
               if (var3 != null) {
                  destroyTexture(var3.textureAsset());
               }
            }
         }));
   }

   public PlayerSkinTextureDownloader getSkinDownloader() {
      if (this.skinDownloader == null) {
         this.skinDownloader = new PlayerSkinTextureDownloader(mc.getNetworkProxy(), mc.getTextureManager(), mc::execute);
      }

      return this.skinDownloader;
   }

   public SkinLookup lookupSkin(String text) {
      try {
         UUID uuid = this.lookupUuid(text);
         SkinData skinData = this.fetchSkinData(uuid);
         return new SkinLookup(text, uuid, skinData.textureUrl(), skinData.skinType());
      } catch (InterruptedException interruptedException) {
         Thread.currentThread().interrupt();
         throw new IllegalStateException("Request interrupted", interruptedException);
      } catch (IOException ioexception) {
         throw new IllegalStateException(ioexception.getMessage(), ioexception);
      }
   }

   public UUID lookupUuid(String text) throws IOException, InterruptedException {
      JsonObject jsonObject = this.getJson("https://api.mojang.com/users/profiles/minecraft/" + this.urlEncode(text));
      if (jsonObject == null) {
         jsonObject = this.getJson("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + this.urlEncode(text));
      }

      if (jsonObject != null && jsonObject.has("id")) {
         return parseUuid(jsonObject.get("id").getAsString());
      } else {
         throw new IOException("Player not found");
      }
   }

   public SkinData fetchSkinData(UUID uUID) throws IOException, InterruptedException {
      JsonObject jsonObject = this.getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + uUID.toString().replace("-", ""));
      if (jsonObject != null && jsonObject.has("properties")) {
         for (JsonElement jsonElement : jsonObject.getAsJsonArray("properties")) {
            if (jsonElement.isJsonObject()) {
               JsonObject jsonObject1 = jsonElement.getAsJsonObject();
               if ("textures".equalsIgnoreCase(optString(jsonObject1, "name")) && jsonObject1.has("value")) {
                  String s = new String(Base64.getDecoder().decode(jsonObject1.get("value").getAsString()), StandardCharsets.UTF_8);
                  JsonObject jsonObject2 = JsonParser.parseString(s).getAsJsonObject();
                  JsonObject jsonObject3 = jsonObject2.getAsJsonObject("textures");
                  JsonObject jsonObject4 = jsonObject3 != null ? jsonObject3.getAsJsonObject("SKIN") : null;
                  if (jsonObject4 != null && jsonObject4.has("url")) {
                     String s1 = null;
                     JsonObject jsonObject5 = jsonObject4.getAsJsonObject("metadata");
                     if (jsonObject5 != null && jsonObject5.has("model")) {
                        s1 = jsonObject5.get("model").getAsString();
                     }

                     PlayerSkinType playerSkinType = "slim".equalsIgnoreCase(s1) ? PlayerSkinType.SLIM : PlayerSkinType.WIDE;
                     return new SkinData(jsonObject4.get("url").getAsString(), playerSkinType);
                  }
                  break;
               }
            }
         }

         throw new IOException("No usable skin texture found");
      } else {
         throw new IOException("Skin profile not found");
      }
   }

   public JsonObject getJson(String text) throws IOException, InterruptedException {
      HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(text))
         .timeout(HTTP_TIMEOUT)
         .header("Accept", "application/json")
         .header("User-Agent", "Water-SkinChanger")
         .GET()
         .build();
      HttpResponse httpResponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString());
      int i = httpResponse.statusCode();
      if (i == 404 || i == 204) {
         return null;
      } else if (i >= 200 && i < 300) {
         String s = (String)httpResponse.body();
         return s != null && !s.isBlank() ? JsonParser.parseString(s).getAsJsonObject() : null;
      } else {
         throw new IOException("HTTP " + i);
      }
   }

   public static synchronized void applyOverrideSkin(TextureAsset asset, PlayerSkinType skinType) {
      destroyTexture(overrideTextureAsset);
      overrideTextureAsset = asset;
      overrideSkin = SkinTextures.create(asset, null, null, skinType);
   }

   public static synchronized void clearOverrideSkin() {
      destroyTexture(overrideTextureAsset);
      overrideTextureAsset = null;
      overrideSkin = null;
   }

   public static void destroyTexture(TextureAsset asset) {
      if (asset != null && mc != null) {
         try {
            mc.getTextureManager().destroyTexture(asset.texturePath());
         } catch (Throwable throwable1) {
         }

         try {
            if (!asset.id().equals(asset.texturePath())) {
               mc.getTextureManager().destroyTexture(asset.id());
            }
         } catch (Throwable throwable) {
         }
      }
   }

   public void sendChatMessage(String text) {
      if (mc != null && mc.inGameHud != null) {
         try {
            mc.inGameHud.getChatHud().addMessage(Text.literal("[SkinChanger] " + text));
         } catch (Throwable throwable) {
         }
      }
   }

   public Identifier buildTextureId(SkinLookup lookup) {
      String s = lookup.playerName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
      if (s.isEmpty()) {
         s = "player";
      }

      return Identifier.of("water", "skins/" + s + "_" + lookup.uuid().toString().replace("-", ""));
   }

   public Path getCachePath(UUID uUID) {
      return mc.runDirectory.toPath().resolve("water-cache").resolve("skins").resolve(uUID.toString().replace("-", "") + ".png");
   }

   public String urlEncode(String text) {
      return URLEncoder.encode(text, StandardCharsets.UTF_8);
   }

   public static UUID parseUuid(String text) {
      String s = text.replace("-", "");
      return UUID.fromString(s.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
   }

   public static String trimOrEmpty(String text) {
      return text == null ? "" : text.trim();
   }

   public static String optString(JsonObject json, String text) {
      return json.has(text) ? json.get(text).getAsString() : "";
   }

   public static String getRootMessage(Throwable error) {
      Throwable throwable = error;

      while (throwable.getCause() != null) {
         throwable = throwable.getCause();
      }

      String s = throwable.getMessage();
      return s != null && !s.isBlank() ? s : throwable.getClass().getSimpleName();
   }

   public static UUID getSelfUuid() {
      if (mc == null) {
         return null;
      } else if (mc.player != null) {
         return mc.player.getUuid();
      } else {
         return mc.getSession() != null ? mc.getSession().getUuidOrNull() : null;
      }
   }
}
