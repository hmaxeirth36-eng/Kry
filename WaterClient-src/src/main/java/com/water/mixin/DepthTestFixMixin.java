package com.water.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class DepthTestFixMixin {
   public DepthTestFixMixin() {
   }

   @Inject(
      method = {"renderHand"},
      at = {@At("HEAD")}
   )
   private void onBeforeRenderHand(float var1, boolean var2, Matrix4f matrix, CallbackInfo ci) {
      GL11.glDepthMask(true);
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void onRenderReturn(RenderTickCounter renderTickCounter, boolean var2, CallbackInfo ci) {
      GL11.glDepthMask(true);
   }
}
