package com.water.module.setting;

public final class ButtonSetting extends Setting<String> {
   public final Runnable action;

   public ButtonSetting(String text, String text2, Runnable task) {
      super(text, text2);
      this.action = task;
   }

   public void trigger() {
      if (this.action != null) {
         this.action.run();
      }
   }
}
