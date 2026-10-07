package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;

public final class Sprint extends Module {
   public boolean hadSprintToggled = false;

   public Sprint() {
      super("Sprint", Category.MISC);
   }

   @Override
   public void onEnable() {
      if (mc != null && mc.options != null) {
         this.hadSprintToggled = this.isSprintToggled();
         this.setSprintToggled(false);
      }
   }

   @Override
   public void onDisable() {
      if (mc != null && mc.options != null) {
         this.setSprintToggled(this.hadSprintToggled);

         try {
            mc.options.sprintKey.setPressed(false);
         } catch (Throwable throwable) {
         }
      }
   }

   @Override
   public void onTick() {
      if (mc != null && mc.player != null && mc.options != null) {
         this.setSprintToggled(false);

         try {
            mc.options.sprintKey.setPressed(true);
         } catch (Throwable throwable) {
         }
      }
   }

   public boolean isSprintToggled() {
      try {
         Object object = mc.options.getClass().getMethod("getSprintToggled").invoke(mc.options);
         return object == null ? false : object.getClass().getMethod("getValue").invoke(object) instanceof Boolean obool && obool;
      } catch (Throwable throwable) {
         return false;
      }
   }

   public void setSprintToggled(boolean var1) {
      try {
         Object object = mc.options.getClass().getMethod("getSprintToggled").invoke(mc.options);
         if (object == null) {
            return;
         }

         object.getClass().getMethod("setValue", Object.class).invoke(object, var1);
      } catch (Throwable throwable) {
      }
   }
}
