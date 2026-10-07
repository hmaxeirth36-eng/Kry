package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;

public final class SwingSpeed extends Module {
   public static SwingSpeed instance;
   public final Setting<Float> swingSpeed = new Setting<>("Swing Speed", 1.0F, 0.1F, 2.0F);

   public SwingSpeed() {
      super("SwingSpeed", Category.MISC);
      instance = this;
      this.addSetting(this.swingSpeed);
   }

   public float getSwingSpeed() {
      float f = this.swingSpeed.getValue() == null ? 1.0F : this.swingSpeed.getValue();
      return f < 0.1F ? 0.1F : Math.min(f, 2.0F);
   }
}
