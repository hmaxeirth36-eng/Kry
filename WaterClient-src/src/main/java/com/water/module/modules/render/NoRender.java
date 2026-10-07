package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.biome.Biome.Precipitation;

public final class NoRender extends Module {
   public static NoRender instance;
   public final Setting<Boolean> rain = new Setting<>("Rain", true);
   public final Setting<Boolean> snow = new Setting<>("Snow", true);
   public final Setting<Boolean> thunder = new Setting<>("Thunder", true);

   public NoRender() {
      super("NoRender", Category.RENDER);
      instance = this;
      this.addSetting(this.rain);
      this.addSetting(this.snow);
      this.addSetting(this.thunder);
   }

   public static boolean isActive() {
      return instance != null && instance.isEnabled() && mc != null && mc.world != null;
   }

   public static boolean isRainHidden() {
      return isActive() && instance.rain.getValue();
   }

   public static boolean isSnowHidden() {
      return isActive() && instance.snow.getValue();
   }

   public static boolean isThunderHidden() {
      return isActive() && instance.thunder.getValue();
   }

   public static boolean areAllPrecipitationsHidden() {
      return isRainHidden() && isSnowHidden();
   }

   public static boolean shouldSkipWeatherRendering() {
      return areAllPrecipitationsHidden();
   }

   public static Precipitation filterPrecipitation(Precipitation precipitation) {
      if (!isActive() || precipitation == null) {
         return precipitation;
      } else if (precipitation == Precipitation.RAIN && isRainHidden()) {
         return Precipitation.NONE;
      } else {
         return precipitation == Precipitation.SNOW && isSnowHidden() ? Precipitation.NONE : precipitation;
      }
   }

   public static boolean shouldMuteSound(SoundEvent sound) {
      if (isActive() && sound != null) {
         return !isRainHidden() || sound != SoundEvents.WEATHER_RAIN && sound != SoundEvents.WEATHER_RAIN_ABOVE
            ? isThunderHidden() && (sound == SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER || sound == SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT)
            : true;
      } else {
         return false;
      }
   }
}
