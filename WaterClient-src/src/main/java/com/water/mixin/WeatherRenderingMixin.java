package com.water.mixin;

import com.water.module.modules.render.NoRender;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome.Precipitation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({WeatherRendering.class})
public abstract class WeatherRenderingMixin {
   public WeatherRenderingMixin() {
   }

   @Invoker("getPrecipitationAt")
   protected abstract Precipitation water$getPrecipitationAt(World var1, BlockPos var2);

   @Redirect(
      method = {"buildPrecipitationPieces"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WeatherRendering;getPrecipitationAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"
      )
   )
   private Precipitation water$filterRenderedPrecipitation(WeatherRendering weatherRendering, World world, BlockPos pos) {
      return NoRender.filterPrecipitation(this.water$getPrecipitationAt(world, pos));
   }

   @Redirect(
      method = {"addParticlesAndSound"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WeatherRendering;getPrecipitationAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome$Precipitation;"
      )
   )
   private Precipitation water$filterWeatherParticlesAndSounds(WeatherRendering weatherRendering, World world, BlockPos pos) {
      return NoRender.filterPrecipitation(this.water$getPrecipitationAt(world, pos));
   }
}
