package com.water.module.modules.misc;

import com.water.gui.NotificationManager;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.ModeSetting;
import com.water.module.setting.Setting;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class WeatherNotifier extends Module {
   public final ModeSetting notificationMode = new ModeSetting("Notification Mode", "Both", "Chat", "Toast", "Both");
   public final Setting<Boolean> notifyThunder = new Setting<>("Notify Thunder", true);
   public Boolean wasRaining = null;
   public Boolean wasThundering = null;

   public WeatherNotifier() {
      super("WeatherNotifier", Category.MISC);
      this.addSetting(this.notificationMode);
      this.addSetting(this.notifyThunder);
   }

   @Override
   public void onEnable() {
      this.wasRaining = null;
      this.wasThundering = null;
   }

   @Override
   public void onDisable() {
      this.wasRaining = null;
      this.wasThundering = null;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         boolean flag = mc.world.isRaining();
         boolean flag1 = mc.world.isThundering();
         if (this.wasRaining == null) {
            this.wasRaining = flag;
            this.wasThundering = flag1;
         } else {
            if (flag && !this.wasRaining) {
               this.notify("The rain started.", "Rain Started", -10835482);
            } else if (!flag && this.wasRaining) {
               this.notify("The rain stopped.", "Rain Stopped", -340971);
            }

            if (this.notifyThunder.getValue()) {
               if (flag1 && !this.wasThundering) {
                  this.notify("A thunderstorm started.", "Thunder Started", -4879105);
               } else if (!flag1 && this.wasThundering) {
                  this.notify("The thunderstorm ended.", "Thunder Ended", -340971);
               }
            }

            this.wasRaining = flag;
            this.wasThundering = flag1;
         }
      }
   }

   public void notify(String text, String text2, int var3) {
      String s = this.notificationMode.getValue();
      boolean flag = "Chat".equalsIgnoreCase(s) || "Both".equalsIgnoreCase(s);
      boolean flag1 = "Toast".equalsIgnoreCase(s) || "Both".equalsIgnoreCase(s);
      if (flag) {
         try {
            mc.inGameHud.getChatHud().addMessage(Text.literal("[WeatherNotifier] " + text));
         } catch (Throwable throwable) {
         }
      }

      if (flag1) {
         NotificationManager.INSTANCE.push("WeatherNotifier", text2, ItemStack.EMPTY, var3);
      }
   }
}
