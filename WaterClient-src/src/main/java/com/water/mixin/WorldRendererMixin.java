package com.water.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.water.module.ModuleManager;
import com.water.module.modules.render.NoRender;
import com.water.render.Render3D;
import com.water.render.RenderUtils;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.memory.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class WorldRendererMixin {
   private static final Matrix4f capturedMatrix = new Matrix4f();
   private static boolean hasCapturedMatrix;
   private static float capturedTickDelta = 1.0F;

   public WorldRendererMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void captureRenderState(
      ObjectAllocator objectAllocator,
      RenderTickCounter renderTickCounter,
      boolean var3,
      Camera camera,
      Matrix4f matrix,
      Matrix4f matrix2,
      Matrix4f matrix3,
      GpuBufferSlice gpuBufferSlice,
      Vector4f vector4f,
      boolean var10,
      CallbackInfo ci
   ) {
      capturedMatrix.set(matrix);
      Render3D.modelViewMatrix.set(matrix);
      Render3D.positionMatrix.set(matrix);
      Render3D.projectionMatrix.set(matrix2);
      RenderUtils.updateFrustum(matrix, matrix2, camera.getCameraPos());
      hasCapturedMatrix = true;
      capturedTickDelta = renderTickCounter.getTickProgress(false);
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void onRender(CallbackInfo ci) {
      if (hasCapturedMatrix) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.multiplyPositionMatrix(capturedMatrix);
         ModuleManager.INSTANCE.onRender(matrixStack, capturedTickDelta);
      }
   }

   @Inject(
      method = {"renderWeather"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$skipWeatherPass(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice gpuBufferSlice, CallbackInfo ci) {
      if (NoRender.areAllPrecipitationsHidden()) {
         ci.cancel();
      }
   }
}
