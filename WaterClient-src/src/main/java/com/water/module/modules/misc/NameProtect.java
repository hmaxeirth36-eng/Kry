package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;

public class NameProtect extends Module {
   public final Setting<String> fakeName = new Setting<>("FakeName", "Player");
   public static NameProtect instance;

   public NameProtect() {
      super("NameProtect", Category.MISC);
      instance = this;
      this.addSetting(this.fakeName);
   }

   public String getFakeName() {
      return this.fakeName.getValue();
   }
}
