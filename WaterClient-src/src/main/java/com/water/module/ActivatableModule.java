package com.water.module;

public abstract class ActivatableModule extends Module {
   public int activationKey = 0;
   public boolean wasActivationKeyPressed = false;

   public ActivatableModule(String text, Category category) {
      super(text, category);
   }

   public void onActivationKeyPressed() {
      this.toggle();
   }

   public int getActivationKey() {
      return this.activationKey;
   }

   public void setActivationKey(int keyCode) {
      this.activationKey = keyCode;
      ModuleManager.INSTANCE.saveConfig();
   }

   public void applyActivationKey(int keyCode) {
      this.activationKey = keyCode;
   }

   public static String watermarkFragment() {
      return "C";
   }
}
