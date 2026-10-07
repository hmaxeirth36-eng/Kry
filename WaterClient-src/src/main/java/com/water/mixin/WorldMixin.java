package com.water.mixin;

import com.water.module.modules.render.NoRender;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({World.class})
public class WorldMixin {
   public WorldMixin() {
   }

   @Inject(
      method = {"getRainGradient"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$hideRainGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
      if (NoRender.shouldSkipWeatherRendering()) {
         cir.setReturnValue(0.0F);
      }
   }

   @Inject(
      method = {"getThunderGradient"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$hideThunderGradient(float tickDelta, CallbackInfoReturnable<Float> cir) {
      if (NoRender.isThunderHidden()) {
         cir.setReturnValue(0.0F);
      }
   }

   @Inject(
      method = {"playSoundClient(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$cancelWeatherPointSound(
      double x, double y, double z, SoundEvent sound, SoundCategory soundCategory, float volume, float pitch, boolean useDistance, CallbackInfo ci
   ) {
      if (NoRender.shouldMuteSound(sound)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"playSoundAtBlockCenterClient(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$cancelWeatherBlockSound(BlockPos pos, SoundEvent sound, SoundCategory soundCategory, float volume, float pitch, boolean useDistance, CallbackInfo ci) {
      if (NoRender.shouldMuteSound(sound)) {
         ci.cancel();
      }
   }
}
