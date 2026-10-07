package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.client.option.SimpleOption;

public final class AutoRender extends Module {
   public final Setting<Float> lowChunks = new Setting<>("Low Chunks", 2.0F, 2.0F, 32.0F);
   public final Setting<Float> highChunks = new Setting<>("High Chunks", 8.0F, 2.0F, 32.0F);
   public final Setting<Float> lowTicks = new Setting<>("Low Ticks", 8.0F, 1.0F, 40.0F);
   public final Setting<Float> resetUpY = new Setting<>("Reset Up Y", 4.0F, 1.0F, 32.0F);
   public final Setting<Float> triggerDownY = new Setting<>("Trigger Down Y", 3.0F, 1.0F, 32.0F);
   public int savedDistance = -1;
   public int lowTimer = 0;
   public int lastApplied = -1;
   public double lastY = 0.0;
   public double highestYAfterPulse = 0.0;
   public boolean pulsing = false;
   public boolean waitingForUp = false;
   public boolean armedAfterUp = false;
   public static Field valueField;

   public AutoRender() {
      super("AUTO RENDER", Category.MISC);
      this.addSetting(this.lowChunks);
      this.addSetting(this.highChunks);
      this.addSetting(this.lowTicks);
      this.addSetting(this.resetUpY);
      this.addSetting(this.triggerDownY);
   }

   @Override
   public void onEnable() {
      if (mc.options != null && mc.player != null) {
         this.savedDistance = mc.options.getClampedViewDistance();
         this.lastApplied = -1;
         this.lastY = mc.player.getY();
         this.highestYAfterPulse = this.lastY;
         this.startPulse();
      }
   }

   @Override
   public void onDisable() {
      if (mc.options != null) {
         if (this.savedDistance >= 2) {
            this.writeRenderDistanceOption(this.savedDistance);
         }

         this.savedDistance = -1;
         this.lowTimer = 0;
         this.lastApplied = -1;
         this.pulsing = false;
         this.waitingForUp = false;
         this.armedAfterUp = false;
      }
   }

   @Override
   public void onTick() {
      if (mc.options != null && mc.world != null && mc.player != null) {
         double d0 = mc.player.getY();
         if (this.pulsing) {
            this.lowTimer--;
            if (this.lowTimer <= 0) {
               this.applyHighRenderDistance();
               this.pulsing = false;
               this.waitingForUp = true;
               this.armedAfterUp = false;
               this.highestYAfterPulse = d0;
            } else {
               this.applyLowRenderDistance();
            }

            this.lastY = d0;
         } else {
            if (this.waitingForUp) {
               if (d0 > this.highestYAfterPulse) {
                  this.highestYAfterPulse = d0;
               }

               if (d0 >= this.lastY + this.resetUpY.getValue().floatValue()) {
                  this.armedAfterUp = true;
               }

               if (this.armedAfterUp && this.highestYAfterPulse - d0 >= this.triggerDownY.getValue().floatValue()) {
                  this.startPulse();
               }
            }

            this.lastY = d0;
         }
      }
   }

   public void startPulse() {
      this.pulsing = true;
      this.waitingForUp = false;
      this.armedAfterUp = false;
      this.lowTimer = Math.max(1, this.lowTicks.getValue().intValue());
      this.applyLowRenderDistance();
   }

   public void applyLowRenderDistance() {
      int i = this.clamp(this.lowChunks.getValue().intValue(), 2, 32);
      this.setRenderDistance(i);
   }

   public void applyHighRenderDistance() {
      int i = this.clamp(this.lowChunks.getValue().intValue(), 2, 32);
      int j = this.clamp(this.highChunks.getValue().intValue(), 2, 32);
      if (j < i) {
         j = i;
      }

      this.setRenderDistance(j);
   }

   public void setRenderDistance(int chunks) {
      chunks = this.clamp(chunks, 2, 32);
      if (this.lastApplied != chunks || mc.options.getClampedViewDistance() != chunks) {
         this.lastApplied = chunks;
         this.writeRenderDistanceOption(chunks);
      }
   }

   public void writeRenderDistanceOption(int chunks) {
      chunks = this.clamp(chunks, 2, 32);
      SimpleOption simpleOption = mc.options.getViewDistance();
      Field field = valueField;
      if (field == null) {
         field = this.findValueField(simpleOption);
         valueField = field;
      }

      if (field != null) {
         try {
            field.set(simpleOption, chunks);
            if (Integer.valueOf(chunks).equals(simpleOption.getValue())) {
               this.scheduleTerrainUpdate();
               return;
            }

            valueField = null;
         } catch (Exception exception1) {
         }
      }

      try {
         simpleOption.setValue(chunks);
      } catch (Exception exception) {
      }

      this.scheduleTerrainUpdate();
   }

   public void scheduleTerrainUpdate() {
      if (mc.worldRenderer != null) {
         mc.worldRenderer.scheduleTerrainUpdate();
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

   public int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
