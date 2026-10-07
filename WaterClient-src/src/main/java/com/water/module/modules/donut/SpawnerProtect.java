package com.water.module.modules.donut;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.water.WaterClient;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Friends;
import com.water.module.setting.Setting;
import com.water.util.StaffAlertWebhook;
import com.water.util.ThreatInfo;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class SpawnerProtect extends Module {
   public static final String SILK_TOUCH_REQUIRED = "Need a Silk Touch pickaxe in hotbar";
   public static final int SCAN_RADIUS = 32;
   public static final double MAX_BREAK_REACH = 5.0;
   public static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8L);
   public static final int SUCCESS_COLOR = 5624994;
   public static final int ERROR_COLOR = 14838378;
   public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
   public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).followRedirects(Redirect.NORMAL).build();
   public final Setting<Integer> criticalDistance = new Setting<>("Critical Distance", 5, 1, 20);
   public final Setting<String> webhookUrl = new Setting<String>("Webhook", "") {
      @Override
      public boolean matchesName(String text) {
         return super.matchesName(text) || "Webhook URL".equalsIgnoreCase(text);
      }
   };
   public BlockPos currentTarget;
   public boolean disconnectScheduled;
   public int minedSpawnerCount;

   public SpawnerProtect() {
      super("SpawnerProtect", Category.DONUT);
      this.addSetting(this.criticalDistance);
      this.addSetting(this.webhookUrl);
   }

   @Override
   public void onEnable() {
      this.resetState();
      if (!this.hasSilkTouchPickaxe()) {
         this.disconnectWithReason("Need a Silk Touch pickaxe in hotbar");
      }
   }

   @Override
   public void onDisable() {
      this.stopMining();
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (!this.disconnectScheduled) {
            if (!this.hasSilkTouchPickaxe()) {
               this.disconnectWithReason("Need a Silk Touch pickaxe in hotbar");
            } else {
               ThreatInfo threatInfo = this.scanForThreats();
               if (threatInfo.hasCriticalThreat()) {
                  this.sendAlertAndDisconnect(this.buildThreatAlert(threatInfo.threat(), threatInfo.distance()));
               } else if (!threatInfo.hasAnyEnemy()) {
                  this.stopMining();
               } else {
                  int i = this.findSilkTouchSlot();
                  if (i == -1) {
                     this.disconnectWithReason("Need a Silk Touch pickaxe in hotbar");
                  } else {
                     this.selectSlot(i);
                     this.startSneaking();
                     if (this.currentTarget == null || !this.isSpawner(this.currentTarget) || !mc.player.canInteractWithBlockAt(this.currentTarget, 5.0)) {
                        this.currentTarget = this.findNearestSpawner();
                        if (this.currentTarget == null) {
                           this.sendAlertAndDisconnect(this.buildAllMinedAlert());
                           return;
                        }
                     }

                     Direction direction = this.getFacingFor(this.currentTarget);
                     this.lookAt(this.currentTarget, direction);
                     mc.interactionManager.updateBlockBreakingProgress(this.currentTarget, direction);
                     mc.world.spawnBlockBreakingParticle(this.currentTarget, direction);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     if (!this.isSpawner(this.currentTarget)) {
                        this.minedSpawnerCount++;
                        this.currentTarget = null;
                     }
                  }
               }
            }
         }
      }
   }

   public ThreatInfo scanForThreats() {
      double d0 = squared(this.criticalDistance.getValue());
      boolean flag = false;
      boolean flag1 = false;
      PlayerEntity playerEntity = null;
      double d1 = -1.0;

      for (PlayerEntity playerEntity1 : mc.world.getPlayers()) {
         if (playerEntity1 != mc.player
            && !playerEntity1.isSpectator()
            && !playerEntity1.isTeammate(mc.player)
            && (!Friends.isSpawnerProtectEnabled() || !Friends.isFriend(playerEntity1.getName().getString()))) {
            flag = true;
            double d2 = mc.player.squaredDistanceTo(playerEntity1);
            if (d2 <= d0) {
               flag1 = true;
               double d3 = Math.sqrt(d2);
               if (playerEntity == null || d3 < d1) {
                  playerEntity = playerEntity1;
                  d1 = d3;
               }
               break;
            }
         }
      }

      return new ThreatInfo(flag, flag1, playerEntity, d1);
   }

   public BlockPos findNearestSpawner() {
      ArrayList<BlockPos> arrayList = new ArrayList<>();
      BlockPos blockPos = mc.player.getBlockPos();
      short short1 = 1024;

      for (int i = -32; i <= 32; i++) {
         for (int j = -32; j <= 32; j++) {
            for (int k = -32; k <= 32; k++) {
               int l = i * i + j * j + k * k;
               if (l <= short1) {
                  BlockPos blockPos1 = blockPos.add(i, j, k);
                  if (this.isSpawner(blockPos1) && mc.player.canInteractWithBlockAt(blockPos1, 5.0)) {
                     arrayList.add(blockPos1.toImmutable());
                  }
               }
            }
         }
      }

      return arrayList.stream().min(Comparator.comparingDouble(this::squaredDistanceTo)).orElse(null);
   }

   public boolean hasSilkTouchPickaxe() {
      return this.findSilkTouchSlot() != -1;
   }

   public int findSilkTouchSlot() {
      for (int i = 0; i < 9; i++) {
         if (this.isSilkTouchPickaxe(mc.player.getInventory().getStack(i))) {
            return i;
         }
      }

      return -1;
   }

   public boolean isSilkTouchPickaxe(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         String s = Registries.ITEM.getId(stack.getItem()).getPath();
         if (!s.endsWith("_pickaxe")) {
            return false;
         } else {
            RegistryEntry registryEntry = mc.world
               .getRegistryManager()
               .getOrThrow(RegistryKeys.ENCHANTMENT)
               .getEntry(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).get(Enchantments.SILK_TOUCH));
            return registryEntry != null && EnchantmentHelper.getLevel(registryEntry, stack) > 0;
         }
      } else {
         return false;
      }
   }

   public boolean isSpawner(BlockPos pos) {
      return mc.world != null && mc.world.getBlockState(pos).isOf(Blocks.SPAWNER);
   }

   public Direction getFacingFor(BlockPos pos) {
      Vec3d vec3d = mc.player.getEyePos().subtract(Vec3d.ofCenter(pos));
      return Direction.getFacing(vec3d.x, vec3d.y, vec3d.z);
   }

   public void lookAt(BlockPos pos, Direction facing) {
      Vec3d vec3d = mc.player.getEyePos();
      Vec3d vec3d1 = Vec3d.ofCenter(pos);
      double d0 = vec3d1.x - vec3d.x;
      double d1 = vec3d1.y - vec3d.y;
      double d2 = vec3d1.z - vec3d.z;
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      float f = (float)(Math.toDegrees(Math.atan2(d2, d0)) - 90.0);
      float f1 = (float)(-Math.toDegrees(Math.atan2(d1, d3)));
      mc.player.setYaw(f);
      mc.player.setPitch(f1);
      mc.crosshairTarget = new BlockHitResult(vec3d1, facing, pos, false);
   }

   public void selectSlot(int slot) {
      if (slot >= 0 && slot < 9 && mc.player.getInventory().getSelectedSlot() != slot) {
         mc.player.getInventory().setSelectedSlot(slot);
      }
   }

   public void startSneaking() {
      mc.options.sneakKey.setPressed(true);
      mc.player.setSneaking(true);
   }

   public void stopMining() {
      this.currentTarget = null;
      if (mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
         mc.interactionManager.cancelBlockBreaking();
      }

      if (mc.player != null) {
         mc.options.sneakKey.setPressed(false);
         mc.player.setSneaking(false);
      }
   }

   public void sendAlertAndDisconnect(StaffAlertWebhook payload) {
      this.stopMining();
      String s = this.trimWebhookUrl(this.webhookUrl.getValue());
      if (!this.isValidWebhookUrl(s)) {
         this.disconnectScheduled = false;
         this.disconnectWithReason(payload.disconnectReason());
      } else {
         this.disconnectScheduled = true;
         CompletableFuture.runAsync(() -> this.postWebhook(payload), Util.getIoWorkerExecutor().named("coordsnapper-send"))
            .whenComplete((var2x, var3) -> mc.execute(() -> {
               if (var3 != null) {
                  WaterClient.LOGGER.error("SpawnerProtect webhook failed", var3);
               }

               this.disconnectScheduled = false;
               this.disconnectWithReason(payload.disconnectReason());
            }));
      }
   }

   public StaffAlertWebhook buildAllMinedAlert() {
      String s = this.trimWebhookUrl(this.webhookUrl.getValue());
      return new StaffAlertWebhook(
         s,
         "[SpawnerProtect]",
         "All your spawners have been collected.",
         5624994,
         mc.player.getName().getString(),
         "",
         "",
         this.countSpawnersInInventory(),
         true,
         this.getServerAddress(),
         TIME_FORMATTER.format(LocalTime.now()),
         "https://mc-heads.net/body/" + this.orDefaultPlayerName(mc.player.getName().getString()),
         "SpawnerProtect finished"
      );
   }

   public StaffAlertWebhook buildThreatAlert(PlayerEntity player, double var2) {
      String s = this.trimWebhookUrl(this.webhookUrl.getValue());
      String s1 = player != null ? player.getName().getString() : "Unknown";
      return new StaffAlertWebhook(
         s,
         "[SpawnerProtect]",
         s1 + " came too close.",
         14838378,
         mc.player.getName().getString(),
         s1,
         String.format(Locale.ROOT, "%.1f", var2),
         this.countSpawnersInInventory(),
         false,
         this.getServerAddress(),
         TIME_FORMATTER.format(LocalTime.now()),
         "https://mc-heads.net/body/" + this.orDefaultPlayerName(s1),
         "Enemy within critical distance"
      );
   }

   public void postWebhook(StaffAlertWebhook payload) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("username", "SpawnerProtect");
      JsonObject jsonObject1 = new JsonObject();
      jsonObject1.addProperty("title", payload.title());
      jsonObject1.addProperty("description", payload.description());
      jsonObject1.addProperty("color", payload.color());
      JsonArray jsonArray = new JsonArray();
      jsonArray.add(this.embedField("Player", payload.playerName(), false));
      jsonArray.add(this.embedField("Time", payload.time(), true));
      jsonArray.add(this.embedField("Server", payload.serverIp(), true));
      jsonArray.add(this.embedField("All spawners mined", payload.allMined() ? "\u2705 Yes" : "\u274c No", false));
      jsonArray.add(this.embedField("Spawners in bag", payload.spawnersInInventory() + " spawners", false));
      if (!payload.threatName().isBlank()) {
         jsonArray.add(this.embedField("Threat", payload.threatName() + " (" + payload.distance() + " blocks)", false));
      }

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
         WaterClient.LOGGER.error("SpawnerProtect webhook request failed", exception);
         throw new IllegalStateException("Webhook request failed", exception);
      }

      int i = httpResponse.statusCode();
      if (i >= 200 && i < 300) {
         WaterClient.LOGGER.info("SpawnerProtect webhook sent");
      } else {
         String s = (String)httpResponse.body();
         if (s != null && !s.isBlank()) {
            WaterClient.LOGGER.warn("SpawnerProtect webhook rejected with status {} and body {}", i, this.truncate(s, 240));
            throw new IllegalStateException("HTTP " + i + ": " + this.truncate(s, 120));
         } else {
            WaterClient.LOGGER.warn("SpawnerProtect webhook rejected with status {}", i);
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

   public String truncate(String text, int maxLength) {
      if (text == null) {
         return "";
      } else {
         String s = text.replace('\n', ' ').replace('\r', ' ').trim();
         return s.length() <= maxLength ? s : s.substring(0, Math.max(0, maxLength - 3)) + "...";
      }
   }

   public int countSpawnersInInventory() {
      int i = 0;

      for (int j = 0; j < mc.player.getInventory().size(); j++) {
         ItemStack itemStack = mc.player.getInventory().getStack(j);
         if (!itemStack.isEmpty() && itemStack.isOf(Blocks.SPAWNER.asItem())) {
            i += itemStack.getCount();
         }
      }

      return i;
   }

   public void disconnectWithReason(String text) {
      if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
         mc.getNetworkHandler().getConnection().disconnect(Text.literal(text));
      }

      this.setEnabled(false);
   }

   public double squaredDistanceTo(BlockPos pos) {
      return mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
   }

   public static double squared(int value) {
      return (double)value * value;
   }

   public void resetState() {
      this.currentTarget = null;
      this.disconnectScheduled = false;
      this.minedSpawnerCount = 0;
   }
}
