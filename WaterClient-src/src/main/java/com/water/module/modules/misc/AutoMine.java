package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;

public final class AutoMine extends Module {
   public AutoMine() {
      super("AutoMine", Category.MISC);
   }

   @Override
   public void onDisable() {
      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }
   }

   public static String watermarkFragment() {
      return "8";
   }
}
