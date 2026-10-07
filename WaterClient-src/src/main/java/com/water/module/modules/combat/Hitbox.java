package com.water.module.modules.combat;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;

public final class Hitbox extends Module {
   public static Hitbox INSTANCE;
   public final Setting<Float> size = new Setting<>("Expand", 1.0F, 0.5F, 2.0F);

   public Hitbox() {
      super("Hitbox", Category.COMBAT);
      this.addSetting(this.size);
      INSTANCE = this;
   }
}
