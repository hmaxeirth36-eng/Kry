package com.water.module.setting;

import com.water.module.ModuleManager;

public final class BindSetting extends Setting<Integer> {
   public boolean enabled;

   public BindSetting(String text, boolean var2, int var3, int var4, int var5) {
      super(text, var3, var4, var5);
      this.enabled = var2;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         this.enabled = enabled;
         ModuleManager.INSTANCE.onSettingChanged();
      }
   }

   public String serialize() {
      return this.enabled + "|" + this.getValue();
   }

   public void deserialize(String text) {
      if (text != null && !text.isBlank()) {
         String[] astring = text.split("\\|", 2);

         try {
            if (astring.length == 2) {
               this.enabled = Boolean.parseBoolean(astring[0]);
               this.setValue(Integer.parseInt(astring[1]));
               return;
            }

            this.setValue(Integer.parseInt(text));
         } catch (NumberFormatException numberFormatException) {
         }
      }
   }
}
