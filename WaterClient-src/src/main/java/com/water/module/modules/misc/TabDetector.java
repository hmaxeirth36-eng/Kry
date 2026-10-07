package com.water.module.modules.misc;

import com.water.gui.NotificationManager;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.ModeSetting;
import com.water.module.setting.Setting;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class TabDetector extends Module {
   public final ModeSetting detectMode = new ModeSetting("Detect", "List", "Any", "List");
   public final Setting<String> targetPlayers = new Setting<>("Target Players", "");
   public final ModeSetting notificationMode = new ModeSetting("Notification Mode", "Both", "Chat", "Toast", "Both");
   public final Setting<Boolean> logOffline = new Setting<>("Log Offline", true);
   public final Set<String> currentTargetsOnline = new HashSet<>();
   public final Set<String> previousTargetsOnline = new HashSet<>();

   public TabDetector() {
      super("TabDetector", Category.MISC);
      this.targetPlayers.visibleWhen(() -> this.detectMode.is("List"));
      this.addSetting(this.detectMode);
      this.addSetting(this.targetPlayers);
      this.addSetting(this.notificationMode);
      this.addSetting(this.logOffline);
   }

   @Override
   public void onEnable() {
      this.currentTargetsOnline.clear();
      this.previousTargetsOnline.clear();
      this.collectMatchingPlayers(this.previousTargetsOnline);
   }

   @Override
   public void onDisable() {
      this.currentTargetsOnline.clear();
      this.previousTargetsOnline.clear();
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         boolean flag = this.detectMode.is("Any");
         Set set = flag ? Set.of() : this.parseNameList(this.targetPlayers.getValue());
         if (!flag && set.isEmpty()) {
            this.currentTargetsOnline.clear();
            this.previousTargetsOnline.clear();
         } else {
            this.currentTargetsOnline.clear();

            for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
               String s = this.getProfileName(playerListEntry);
               if (!s.isEmpty() && (mc.player == null || !s.equalsIgnoreCase(mc.player.getName().getString())) && (flag || this.matchesTarget(set, s))) {
                  this.currentTargetsOnline.add(s);
               }
            }

            HashSet hashSet = new HashSet<>(this.currentTargetsOnline);
            hashSet.removeAll(this.previousTargetsOnline);
            if (!hashSet.isEmpty()) {
               this.notifyJoined(hashSet);
            }

            if (this.logOffline.getValue()) {
               HashSet hashSet1 = new HashSet<>(this.previousTargetsOnline);
               hashSet1.removeAll(this.currentTargetsOnline);
               if (!hashSet1.isEmpty()) {
                  this.notifyLeft(hashSet1);
               }
            }

            this.previousTargetsOnline.clear();
            this.previousTargetsOnline.addAll(this.currentTargetsOnline);
         }
      }
   }

   public void notifyJoined(Set<String> set) {
      String s = String.join(", ", set);
      String s1 = set.size() == 1 ? "Target player joined: " + s : "Target players joined: " + s;
      this.notify(s1, set.size() == 1 ? "Target Player Joined!" : "Target Players Joined!", -1938838);
   }

   public void notifyLeft(Set<String> set) {
      String s = String.join(", ", set);
      String s1 = set.size() == 1 ? "Target player left: " + s : "Target players left: " + s;
      this.notify(s1, set.size() == 1 ? "Target Player Left!" : "Target Players Left!", -11152222);
   }

   public void notify(String text, String text2, int var3) {
      String s = this.notificationMode.getValue();
      boolean flag = "Chat".equalsIgnoreCase(s) || "Both".equalsIgnoreCase(s);
      boolean flag1 = "Toast".equalsIgnoreCase(s) || "Both".equalsIgnoreCase(s);
      if (flag) {
         try {
            mc.inGameHud.getChatHud().addMessage(Text.literal("[TabDetector] " + text));
         } catch (Throwable throwable) {
         }
      }

      if (flag1) {
         NotificationManager.INSTANCE.push("TabDetector", text2, ItemStack.EMPTY, var3);
      }
   }

   public void collectMatchingPlayers(Set<String> out) {
      out.clear();
      if (mc.getNetworkHandler() != null) {
         boolean flag = this.detectMode.is("Any");
         Set set = flag ? Set.of() : this.parseNameList(this.targetPlayers.getValue());
         if (flag || !set.isEmpty()) {
            for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
               String s = this.getProfileName(playerListEntry);
               if (!s.isEmpty() && (mc.player == null || !s.equalsIgnoreCase(mc.player.getName().getString())) && (flag || this.matchesTarget(set, s))) {
                  out.add(s);
               }
            }
         }
      }
   }

   public String getProfileName(PlayerListEntry listEntry) {
      try {
         if (listEntry != null && listEntry.getProfile() != null) {
            try {
               String s = listEntry.getProfile().name();
               return s == null ? "" : s;
            } catch (Throwable throwable) {
               return "";
            }
         } else {
            return "";
         }
      } catch (Throwable throwable1) {
         return "";
      }
   }

   public boolean matchesTarget(Set<String> set, String text) {
      return set.contains(text.toLowerCase(Locale.ROOT));
   }

   public Set<String> parseNameList(String text) {
      if (text != null && !text.isBlank()) {
         String s = text.replace('\n', ',').replace('\r', ',');
         LinkedHashSet linkedHashSet = new LinkedHashSet();

         for (String s1 : s.split(",")) {
            String s2 = s1 == null ? "" : s1.trim();
            if (!s2.isEmpty()) {
               linkedHashSet.add(s2.toLowerCase(Locale.ROOT));
            }
         }

         return linkedHashSet;
      } else {
         return Set.of();
      }
   }
}
