package com.water.module.setting;

public final class ConfirmSetting extends Setting<Boolean> {
   public final String confirmTitle;
   public final String confirmMessage;

   public ConfirmSetting(String text, boolean var2, String text2, String text3) {
      super(text, var2);
      this.confirmTitle = text2;
      this.confirmMessage = text3;
   }

   public String getConfirmTitle() {
      return this.confirmTitle;
   }

   public String getConfirmMessage() {
      return this.confirmMessage;
   }
}
