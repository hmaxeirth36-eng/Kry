package com.water.module.modules.misc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.water.WaterClient;
import com.water.gui.NotificationManager;
import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.setting.Setting;
import com.water.util.AutoLogWebhook;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Util;

public final class CoordSnapper extends ActivatableModule {
   public static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8L);
   public static final int SUCCESS_COLOR = -11152222;
   public static final int ERROR_COLOR = -1938838;
   public static final int WARNING_COLOR = -1002662;
   public static final ItemStack NOTIFICATION_STACK = new ItemStack(Items.RECOVERY_COMPASS);
   public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ROOT);
   public static final long TRIGGER_COOLDOWN_MS = 250L;
   public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).followRedirects(Redirect.NORMAL).build();
   public final Setting<String> webhookUrl = new Setting<String>("Webhook", "") {
      @Override
      public boolean matchesName(String text) {
         return super.matchesName(text) || "Webhook URL".equalsIgnoreCase(text);
      }
   };
   public final Setting<Boolean> notification = new Setting<>("Notification", true);
   public volatile long lastTriggerAt;

   public CoordSnapper() {
      super("CoordSnapper", Category.MISC);
      this.addSetting(this.webhookUrl);
      this.addSetting(this.notification);
   }

   @Override
   public void onActivationKeyPressed() {
      if (this.isEnabled() && mc.player != null) {
         long i = System.currentTimeMillis();
         if (i - this.lastTriggerAt >= 250L) {
            this.lastTriggerAt = i;
            String s = this.trimWebhookUrl(this.webhookUrl.getValue());
            if (!this.isValidWebhookUrl(s)) {
               this.pushNotification("Webhook invalid", "Set a Discord webhook URL.", -1002662);
            } else {
               AutoLogWebhook autoLogWebhook = this.buildPayload(s);
               CompletableFuture.runAsync(() -> this.postWebhook(autoLogWebhook), Util.getIoWorkerExecutor().named("coordsnapper-send"))
                  .whenComplete((var2, var3x) -> mc.execute(() -> {
                     if (var3x != null) {
                        this.pushNotification("Send failed", this.getRootMessage(var3x), -1938838);
                     } else {
                        this.pushNotification("Coords sent", autoLogWebhook.formatCoords(), -11152222);
                     }
                  }));
            }
         }
      }
   }

   public AutoLogWebhook buildPayload(String text) {
      int i = mc.player.getBlockX();
      int j = mc.player.getBlockY();
      int k = mc.player.getBlockZ();
      String s = mc.player.getName().getString();
      String s1 = this.getServerAddress();
      String s2 = TIME_FORMATTER.format(ZonedDateTime.now());
      String s3 = this.orDefaultPlayerName(s);
      String s4 = "https://mc-heads.net/body/" + s3;
      return new AutoLogWebhook(text, s, i, j, k, s1, s2, s4);
   }

   public void postWebhook(AutoLogWebhook payload) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("username", "CoordSnapper");
      JsonObject jsonObject1 = new JsonObject();
      jsonObject1.addProperty("title", "CoordSnapper");
      jsonObject1.addProperty("color", 5624994);
      JsonArray jsonArray = new JsonArray();
      jsonArray.add(this.embedField("Name", payload.playerName(), false));
      jsonArray.add(this.embedField("Coords", payload.formatCoordsLabeled(), false));
      jsonArray.add(this.embedField("IP", payload.serverIp(), true));
      jsonArray.add(this.embedField("Time", payload.time(), true));
      jsonObject1.add("fields", jsonArray);
      JsonObject jsonObject2 = new JsonObject();
      jsonObject2.addProperty("url", payload.skinRenderUrl());
      jsonObject1.add("thumbnail", jsonObject2);
      JsonArray jsonArray1 = new JsonArray();
      jsonArray1.add(jsonObject1);
      jsonObject.add("embeds", jsonArray1);
      HttpRequest httpRequest = HttpRequest.newBuilder(this.withWaitParam(payload.webhook()))
         .timeout(HTTP_TIMEOUT)
         .header("Content-Type", "application/json")
         .header("Accept", "application/json")
         .header("User-Agent", "Water-CoordSnapper")
         .POST(BodyPublishers.ofString(jsonObject.toString()))
         .build();

      HttpResponse httpResponse;
      try {
         httpResponse = HTTP_CLIENT.send(httpRequest, BodyHandlers.ofString());
      } catch (Exception exception) {
         WaterClient.LOGGER.error("CoordSnapper webhook request failed", exception);
         throw new IllegalStateException("Webhook request failed", exception);
      }

      int i = httpResponse.statusCode();
      if (i >= 200 && i < 300) {
         WaterClient.LOGGER.info("CoordSnapper sent coords for {}", payload.playerName());
      } else {
         String s = (String)httpResponse.body();
         if (s != null && !s.isBlank()) {
            WaterClient.LOGGER.warn("CoordSnapper webhook rejected with status {} and body {}", i, this.truncate(s, 240));
            throw new IllegalStateException("HTTP " + i + ": " + this.truncate(s, 120));
         } else {
            WaterClient.LOGGER.warn("CoordSnapper webhook rejected with status {}", i);
            throw new IllegalStateException("HTTP " + i);
         }
      }
   }

   public JsonObject embedField(String text, String text2, boolean var3) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("name", text);
      jsonObject.addProperty("value", text2 != null && !text2.isBlank() ? text2 : "-");
      jsonObject.addProperty("inline", var3);
      return jsonObject;
   }

   public String getServerAddress() {
      ServerInfo serverInfo = mc.getCurrentServerEntry();
      if (serverInfo != null && serverInfo.address != null && !serverInfo.address.isBlank()) {
         String s = this.normalizeAddress(serverInfo.address);
         return s.isEmpty() ? "Singleplayer" : s;
      } else {
         return "Singleplayer";
      }
   }

   public String normalizeAddress(String text) {
      String s = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
      int i = s.indexOf(47);
      if (i >= 0) {
         s = s.substring(0, i);
      }

      int j = s.indexOf(58);
      if (j >= 0) {
         s = s.substring(0, j);
      }

      return s;
   }

   public String trimWebhookUrl(String text) {
      return text == null ? "" : text.trim();
   }

   public String orDefaultPlayerName(String text) {
      return text != null && !text.isBlank() ? text.trim() : "Steve";
   }

   public URI withWaitParam(String text) {
      URI uri = URI.create(text);
      String s = uri.getQuery();
      if (s == null || s.isBlank()) {
         return URI.create(text + "?wait=true");
      } else {
         return s.contains("wait=") ? uri : URI.create(text + "&wait=true");
      }
   }

   public boolean isValidWebhookUrl(String text) {
      if (text.isEmpty()) {
         return false;
      } else {
         try {
            URI uri = URI.create(text);
            String s = uri.getScheme();
            String s1 = uri.getHost();
            String s2 = uri.getPath();
            return ("https".equalsIgnoreCase(s) || "http".equalsIgnoreCase(s)) && s1 != null && !s1.isBlank() && s2 != null && s2.contains("/api/webhooks/");
         } catch (Exception exception) {
            return false;
         }
      }
   }

   public void pushNotification(String text, String text2, int var3) {
      if (mc != null && this.notification.getValue()) {
         mc.execute(() -> NotificationManager.INSTANCE.push(text, text2, NOTIFICATION_STACK, var3));
      }
   }

   public String getRootMessage(Throwable error) {
      Throwable throwable = error;

      while (throwable.getCause() != null) {
         throwable = throwable.getCause();
      }

      String s = throwable.getMessage();
      return s != null && !s.isBlank() ? this.truncate(s, 120) : throwable.getClass().getSimpleName();
   }

   public String truncate(String text, int maxLength) {
      if (text == null) {
         return "";
      } else {
         String s = text.replace('\n', ' ').replace('\r', ' ').trim();
         return s.length() <= maxLength ? s : s.substring(0, Math.max(0, maxLength - 3)) + "...";
      }
   }
}
