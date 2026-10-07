package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.client.option.SimpleOption;

public final class FullBright extends Module {
   public static final double FULL_BRIGHT_GAMMA = 10.0;
   public double previousGamma = 1.0;
   public boolean gammaApplied = false;
   public static Field valueField;

   public FullBright() {
      super("FullBright", Category.RENDER);
   }

   @Override
   public void onEnable() {
      if (mc.options != null) {
         this.previousGamma = mc.options.getGamma().getValue();
         this.setGamma(10.0);
         this.gammaApplied = true;
      }
   }

   @Override
   public void onDisable() {
      if (mc.options != null) {
         this.setGamma(this.gammaApplied ? this.previousGamma : 1.0);
         this.gammaApplied = false;
      }
   }

   @Override
   public void onTick() {
      if (mc.options != null) {
         if (!this.gammaApplied) {
            this.previousGamma = mc.options.getGamma().getValue();
            if (Math.abs(this.previousGamma - 10.0) < 1.0E-4) {
               this.previousGamma = 1.0;
            }

            this.setGamma(10.0);
            this.gammaApplied = true;
         } else {
            try {
               double d0 = mc.options.getGamma().getValue();
               if (Math.abs(d0 - 10.0) > 1.0E-4) {
                  this.setGamma(10.0);
               }
            } catch (Exception exception) {
            }
         }
      }
   }

   public void setGamma(double gamma) {
      SimpleOption simpleOption = mc.options.getGamma();
      Field field = valueField;
      if (field == null) {
         field = this.findValueField(simpleOption);
         valueField = field;
      }

      if (field != null) {
         try {
            field.set(simpleOption, gamma);
            Double d0 = (Double)simpleOption.getValue();
            if (d0 != null && Math.abs(d0 - gamma) <= 1.0E-4) {
               return;
            }

            valueField = null;
         } catch (Exception exception1) {
         }
      }

      try {
         simpleOption.setValue(gamma);
      } catch (Exception exception) {
      }
   }

   public Field findValueField(SimpleOption<?> option) {
      Object object;
      try {
         object = option.getValue();
      } catch (Exception exception) {
         object = null;
      }

      Field field = null;

      for (Field field1 : SimpleOption.class.getDeclaredFields()) {
         if (!Modifier.isStatic(field1.getModifiers())) {
            if ("value".equals(field1.getName())) {
               field = field1;
            }

            field1.setAccessible(true);

            try {
               Object object1 = field1.get(option);
               if (object == null ? object1 == null : object.equals(object1)) {
                  return field1;
               }
            } catch (Exception exception1) {
            }
         }
      }

      if (field != null) {
         field.setAccessible(true);
         return field;
      } else {
         return null;
      }
   }
}
