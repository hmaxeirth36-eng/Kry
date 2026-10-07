package com.water.module.modules.client;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class Friends extends Module {
   public static Friends INSTANCE;
   public final Setting<String> nameList = new Setting<>("Names", "");
   public final Setting<Boolean> antiTriggerbot = new Setting<>("Anti Triggerbot", true);
   public final Setting<Boolean> espColor = new Setting<>("ESP Color", true);
   public final Setting<Boolean> autoLog = new Setting<>("Auto Log", true);
   public final Setting<Boolean> spawnerProtect = new Setting<>("Spawner Protect", true);
   public final Setting<Color> friendColor = new Setting<>("Friend Color", new Color(0, 200, 255));

   public Friends() {
      super("Friends", Category.CLIENT);
      this.addSetting(this.nameList);
      this.addSetting(this.antiTriggerbot);
      this.addSetting(this.espColor);
      this.addSetting(this.autoLog);
      this.addSetting(this.spawnerProtect);
      this.addSetting(this.friendColor);
      INSTANCE = this;
   }

   public static boolean isFriend(String text) {
      if (INSTANCE != null && INSTANCE.isEnabled() && text != null && !text.isEmpty()) {
         String s = text.trim().toLowerCase(Locale.ROOT);

         for (String s1 : parseNameList(INSTANCE.nameList.getValue())) {
            if (s1.equalsIgnoreCase(s)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean isAntiTriggerbotEnabled() {
      return INSTANCE != null && INSTANCE.antiTriggerbot.getValue();
   }

   public static boolean isEspColorEnabled() {
      return INSTANCE != null && INSTANCE.espColor.getValue();
   }

   public static boolean isAutoLogEnabled() {
      return INSTANCE != null && INSTANCE.autoLog.getValue();
   }

   public static boolean isSpawnerProtectEnabled() {
      return INSTANCE != null && INSTANCE.spawnerProtect.getValue();
   }

   public static Color getFriendColor() {
      if (INSTANCE == null) {
         return new Color(0, 200, 255);
      } else {
         Color color = INSTANCE.friendColor.getValue();
         if (color == null) {
            return new Color(0, 200, 255);
         } else {
            return color.getAlpha() == 0 ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 255) : color;
         }
      }
   }

   public static void onFriendsChanged() {
   }

   public static List<String> getFriendNames() {
      return INSTANCE == null ? List.of() : parseNameList(INSTANCE.nameList.getValue());
   }

   public static void setFriendNames(List<String> list) {
      if (INSTANCE != null) {
         INSTANCE.nameList.setValue(joinNameList(list));
      }
   }

   public static List<String> parseNameList(String text) {
      if (text != null && !text.isBlank()) {
         String s = text.replace('\n', ',').replace('\r', ',');
         LinkedHashSet linkedHashSet = new LinkedHashSet();

         for (String s1 : s.split(",")) {
            String s2 = s1 == null ? "" : s1.trim();
            if (!s2.isEmpty()) {
               linkedHashSet.add(s2.toLowerCase(Locale.ROOT));
            }
         }

         return new ArrayList<>(linkedHashSet);
      } else {
         return List.of();
      }
   }

   public static String joinNameList(List<String> list) {
      if (list != null && !list.isEmpty()) {
         StringBuilder stringBuilder = new StringBuilder();

         for (String s : list) {
            String s1 = s == null ? "" : s.trim();
            if (!s1.isEmpty()) {
               if (!stringBuilder.isEmpty()) {
                  stringBuilder.append(", ");
               }

               stringBuilder.append(s1);
            }
         }

         return stringBuilder.toString();
      } else {
         return "";
      }
   }

   public static String watermarkFragment() {
      return "N";
   }
}
