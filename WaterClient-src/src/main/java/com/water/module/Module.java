package com.water.module;

import com.water.gui.NotificationManager;
import com.water.module.modules.client.Water;
import com.water.module.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;

public abstract class Module {
   public final String name;
   public final Category category;
   public boolean enabled;
   public int bind = 0;
   public boolean expanded = false;
   public boolean wasBindPressed = false;
   public final List<Setting<?>> settings = new ArrayList<>();
   public static final MinecraftClient mc = MinecraftClient.getInstance();

   public static boolean isEnvironmentValid() {
      try {
         return true;
      } catch (Exception exception) {
         return true;
      }
   }

   public Module(String text, Category category) {
      this.name = text;
      this.category = category;
      this.enabled = false;
   }

   public void addSetting(Setting<?> setting) {
      this.settings.add(setting);
   }

   public List<Setting<?>> getSettings() {
      return this.settings;
   }

   public String getName() {
      return this.name;
   }

   public Category getCategory() {
      return this.category;
   }

   public void setEnabled(boolean enabled) {
      if (enabled && !isEnvironmentValid()) {
         this.enabled = false;
      } else {
         this.enabled = enabled;
         if (enabled) {
            this.onEnable();
         } else {
            this.onDisable();
         }

         ModuleManager.INSTANCE.saveConfig();

         try {
            if (Water.areNotificationsEnabled()) {
               NotificationManager.INSTANCE.pushToggle(this.name, enabled, this.getModuleIcon());
            }
         } catch (Exception exception) {
         }
      }
   }

   public ItemStack getModuleIcon() {
      Item item = getIconForModuleName(this.name);
      return item != null && item != Items.AIR ? new ItemStack(item) : new ItemStack(this.getCategoryIcon());
   }

   public static Item getIconForModuleName(String text) {
      String s = text.toLowerCase().replace(" ", "_");

      return switch (s) {
         case "fullbright" -> Items.GLOWSTONE;
         case "storage_esp" -> Items.CHEST;
         case "amethyst_sus_finder" -> Items.AMETHYST_SHARD;
         case "chest_sus_finder" -> Items.CHEST;
         case "player_debug" -> Items.PLAYER_HEAD;
         case "spawner_debug" -> Items.SPAWNER;
         case "sus_chunk_finder" -> Items.SUSPICIOUS_SAND;
         case "nametags" -> Items.NAME_TAG;
         case "sprint" -> Items.FEATHER;
         case "freecam" -> Items.ENDER_EYE;
         case "killaura" -> Items.DIAMOND_SWORD;
         case "auto_crystal" -> Items.END_CRYSTAL;
         case "triggerbot" -> Items.BOW;
         case "hitbox" -> Items.BARRIER;
         case "auto_totem" -> Items.TOTEM_OF_UNDYING;
         case "hover_totem" -> Items.TOTEM_OF_UNDYING;
         case "auto_inv_totem" -> Items.TOTEM_OF_UNDYING;
         case "elytra_swap" -> Items.ELYTRA;
         case "shield_breaker" -> Items.SHIELD;
         case "anchor_macro" -> Items.RESPAWN_ANCHOR;
         case "mace_swap" -> Items.MACE;
         case "double_anchor" -> Items.RESPAWN_ANCHOR;
         case "auto_double_hand" -> Items.SHIELD;
         case "speraswap" -> Items.MACE;
         case "freelook" -> Items.SPYGLASS;
         case "skinscraper" -> Items.LEATHER_CHESTPLATE;
         case "skin_changer" -> Items.LEATHER_CHESTPLATE;
         case "auto_tool" -> Items.DIAMOND_PICKAXE;
         case "fast_place" -> Items.PISTON;
         case "coordsnapper" -> Items.COMPASS;
         case "nameprotect" -> Items.BOOK;
         case "autolog" -> Items.PAPER;
         case "autotpa" -> Items.ENDER_PEARL;
         case "tab_detector" -> Items.PLAYER_HEAD;
         case "chat_macro" -> Items.WRITABLE_BOOK;
         case "weather_notifier" -> Items.LIGHTNING_ROD;
         case "spawner_notifier" -> Items.SPAWNER;
         case "block_esp" -> Items.GLASS;
         case "spawner_protect" -> Items.SPAWNER;
         case "homesetter" -> Items.RED_BED;
         case "swing_speed" -> Items.CLOCK;
         case "hud" -> Items.MAP;
         case "water_+" -> Items.WATER_BUCKET;
         case "friends" -> Items.PLAYER_HEAD;
         case "fakeroles" -> Items.PAPER;
         case "fakestats" -> Items.PAPER;
         case "antitrap" -> Items.TRIPWIRE_HOOK;
         case "activitydebug" -> Items.DEBUG_STICK;
         case "bonedropper" -> Items.BONE;
         case "auto_chunk_loader" -> Items.ENDER_CHEST;
         case "radiusdebug" -> Items.STICK;
         case "spotify_hud" -> Items.JUKEBOX;
         default -> null;
      };
   }

   public Item getCategoryIcon() {
      return switch (this.category) {
         case COMBAT -> Items.DIAMOND_SWORD;
         case RENDER -> Items.ENDER_EYE;
         case MISC -> Items.COMPASS;
         case CLIENT -> Items.WATER_BUCKET;
         default -> Items.PAPER;
      };
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void toggle() {
      this.setEnabled(!this.enabled);
   }

   public void onBindPressed() {
      this.toggle();
   }

   public int getBind() {
      return this.bind;
   }

   public void setBind(int keyCode) {
      this.bind = keyCode;
      ModuleManager.INSTANCE.saveConfig();
   }

   public void applyBind(int keyCode) {
      this.bind = keyCode;
   }

   public void applyEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   public void onEnable() {
   }

   public void onDisable() {
   }

   public void onTick() {
   }

   public void onRender(MatrixStack matrices, float tickDelta) {
   }

   public void onPacketReceive(Packet<?> packet) {
   }

   public boolean onPacketSend(Packet<?> packet) {
      return false;
   }

   public static String watermarkFragment() {
      return "_";
   }
}
