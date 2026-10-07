package com.water.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.water.render.Render2D;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public class BlurPipeline {
   public static final int BLUR_ITERATIONS = 5;
   public static final int DOWNSAMPLE_SCALE = 2;
   public static final int BUFFER_SIZE = 256;
   public static final RenderPipeline PIPELINE_BLUR = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("water", "pipeline/blur_pass"))
         .withVertexShader(Identifier.of("water", "blur_pass_vertex"))
         .withFragmentShader(Identifier.of("water", "blur_pass_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   public static final RenderPipeline PIPELINE_FINAL = RenderPipelines.register(
      RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
         .withLocation(Identifier.of("water", "pipeline/blur_final"))
         .withVertexShader(Identifier.of("water", "blur_final_vertex"))
         .withFragmentShader(Identifier.of("water", "blur_final_fragment"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   public static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   public static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
   public static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   public static GpuBuffer uniformBuffer;
   public static GpuBuffer dummyVertexBuffer;
   public static ByteBuffer dataBuffer;
   public static GpuTexture copyTexture;
   public static GpuTextureView copyTextureView;
   public static GpuTexture[] pingPongTextures = new GpuTexture[2];
   public static GpuTextureView[] pingPongViews = new GpuTextureView[2];
   public static int lastWidth = 0;
   public static int lastHeight = 0;
   public static boolean initialized = false;
   public static long lastFrameTime = -1L;
   public static int cachedBlurSrc = 0;
   public static float cachedStrength = 0.0F;

   public BlurPipeline() {
   }

   public static void init() {
      if (!initialized) {
         dataBuffer = MemoryUtil.memAlloc(256);
         ByteBuffer byteBuffer = MemoryUtil.memAlloc(4);
         byteBuffer.putInt(0);
         byteBuffer.flip();
         dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "water:blur_dummy_vertex", 32, byteBuffer);
         MemoryUtil.memFree(byteBuffer);
         initialized = true;
      }
   }

   public static void captureAndBlur() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      int i = minecraftClient.getFramebuffer().textureWidth;
      int j = minecraftClient.getFramebuffer().textureHeight;
      ensureTextures(i, j);
      CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
      commandEncoder.copyTextureToTexture(minecraftClient.getFramebuffer().getColorAttachment(), copyTexture, 0, 0, 0, 0, 0, i, j);
      lastFrameTime = -1L;
   }

   public static void ensureTextures(int var0, int var1) {
      int i = var0 / 2;
      int j = var1 / 2;
      if (copyTexture == null || var0 != lastWidth || var1 != lastHeight) {
         if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
         }

         if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
         }

         copyTexture = RenderSystem.getDevice().createTexture(() -> "water:blur_copy", 5, TextureFormat.RGBA8, var0, var1, 1, 1);
         copyTextureView = RenderSystem.getDevice().createTextureView(copyTexture);

         for (int k = 0; k < 2; k++) {
            if (pingPongViews[k] != null) {
               pingPongViews[k].close();
               pingPongViews[k] = null;
            }

            if (pingPongTextures[k] != null) {
               pingPongTextures[k].close();
               pingPongTextures[k] = null;
            }

            int l = k;
            pingPongTextures[k] = RenderSystem.getDevice().createTexture(() -> "water:blur_pp_" + l, 13, TextureFormat.RGBA8, i, j, 1, 1);
            pingPongViews[k] = RenderSystem.getDevice().createTextureView(pingPongTextures[k]);
         }

         lastWidth = var0;
         lastHeight = var1;
         lastFrameTime = -1L;
      }
   }

   public static void drawBlurredRect(Matrix4f matrix, float var1, float var2, float var3, float var4, float var5, float var6, float var7) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.getFramebuffer() != null) {
         if (minecraftClient.getFramebuffer().getColorAttachment() != null) {
            init();
            int i = minecraftClient.getFramebuffer().textureWidth;
            int j = minecraftClient.getFramebuffer().textureHeight;
            int k = i / 2;
            int l = j / 2;
            ensureTextures(i, j);
            long i1 = System.nanoTime() / 16666666L;
            boolean flag = i1 != lastFrameTime || Math.abs(var6 - cachedStrength) > 0.01F;
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
               .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (flag) {
               commandEncoder.copyTextureToTexture(minecraftClient.getFramebuffer().getColorAttachment(), copyTexture, 0, 0, 0, 0, 0, i, j);
               writeBlurUniforms(i, j, k, l, 1.0F, var6);
               commandEncoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

               try (RenderPass renderPass = commandEncoder.createRenderPass(
                     () -> "water:blur_downsample", pingPongViews[0], OptionalInt.empty(), null, OptionalDouble.empty()
                  )) {
                  renderPass.setPipeline(PIPELINE_BLUR);
                  renderPass.setVertexBuffer(0, dummyVertexBuffer);
                  renderPass.bindTexture("Sampler0", copyTextureView, gpuSampler);
                  RenderSystem.bindDefaultUniforms(renderPass);
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("BlurData", uniformBuffer);
                  renderPass.draw(0, 6);
               }

               int j2 = Math.max(2, (int)(5.0F * var6));
               float[] afloat = new float[]{1.0F, 2.0F, 2.0F, 3.0F};

               for (int j1 = 0; j1 < j2; j1++) {
                  int k1 = j1 % 2;
                  int l1 = (j1 + 1) % 2;
                  float f = j1 < afloat.length ? afloat[j1] : 3.0F;
                  int i2 = j1;
                  writeBlurUniforms(k, l, k, l, f, 1.0F);
                  commandEncoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

                  try (RenderPass renderPass1 = commandEncoder.createRenderPass(
                        () -> "water:blur_" + i2, pingPongViews[l1], OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                     renderPass1.setPipeline(PIPELINE_BLUR);
                     renderPass1.setVertexBuffer(0, dummyVertexBuffer);
                     renderPass1.bindTexture("Sampler0", pingPongViews[k1], gpuSampler);
                     RenderSystem.bindDefaultUniforms(renderPass1);
                     renderPass1.setUniform("DynamicTransforms", gpuBufferSlice);
                     renderPass1.setUniform("BlurData", uniformBuffer);
                     renderPass1.draw(0, 6);
                  }
               }

               cachedBlurSrc = j2 % 2;
               lastFrameTime = i1;
               cachedStrength = var6;
            }

            float[] afloat1 = new float[]{var5, var5, var5, var5};
            int k2 = Render2D.getWindowWidth();
            int l2 = Render2D.getWindowHeight();
            writeFinalUniforms(matrix, var1, var2, var3, var4, k2, l2, afloat1, var7);
            commandEncoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

            try (RenderPass renderPass2 = commandEncoder.createRenderPass(
                  () -> "water:blur_final",
                  minecraftClient.getFramebuffer().getColorAttachmentView(),
                  OptionalInt.empty(),
                  minecraftClient.getFramebuffer().getDepthAttachmentView(),
                  OptionalDouble.of(1.0)
               )) {
               Render2D.applyScissorToPass(renderPass2);
               renderPass2.setPipeline(PIPELINE_FINAL);
               renderPass2.setVertexBuffer(0, dummyVertexBuffer);
               renderPass2.bindTexture("Sampler0", pingPongViews[cachedBlurSrc], gpuSampler);
               RenderSystem.bindDefaultUniforms(renderPass2);
               renderPass2.setUniform("DynamicTransforms", gpuBufferSlice);
               renderPass2.setUniform("BlurData", uniformBuffer);
               renderPass2.draw(0, 6);
            }
         }
      }
   }

   public static void writeBlurUniforms(int var0, int var1, int var2, int var3, float var4, float var5) {
      dataBuffer.clear();

      for (int i = 0; i < 16; i++) {
         dataBuffer.putFloat(0.0F);
      }

      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(var0).putFloat(var1);
      dataBuffer.putFloat(var0).putFloat(var1).putFloat(1.0F).putFloat(var4);
      dataBuffer.putFloat(var2).putFloat(var3).putFloat(1.0F).putFloat(var5);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.flip();
      uploadUniforms();
   }

   public static void writeFinalUniforms(Matrix4f matrix, float var1, float var2, float var3, float var4, int var5, int var6, float[] var7, float var8) {
      dataBuffer.clear();
      dataBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
      dataBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
      dataBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
      dataBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
      dataBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
      dataBuffer.putFloat(var5).putFloat(var6).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.putFloat(var5).putFloat(var6).putFloat(1.0F).putFloat(0.0F);
      dataBuffer.putFloat(var7[0]).putFloat(var7[1]).putFloat(var7[2]).putFloat(var7[3]);
      dataBuffer.putFloat(var8).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
      dataBuffer.flip();
      uploadUniforms();
   }

   public static void uploadUniforms() {
      int i = dataBuffer.remaining();
      if (uniformBuffer == null || uniformBuffer.size() < i) {
         if (uniformBuffer != null) {
            uniformBuffer.close();
         }

         uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "water:blur_uniform", 136, i);
      }
   }

   public static GpuTextureView getBlurTextureView() {
      return initialized && pingPongViews[cachedBlurSrc] != null ? pingPongViews[cachedBlurSrc] : null;
   }

   public static void close() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      if (dummyVertexBuffer != null) {
         dummyVertexBuffer.close();
         dummyVertexBuffer = null;
      }

      if (dataBuffer != null) {
         MemoryUtil.memFree(dataBuffer);
         dataBuffer = null;
      }

      if (copyTextureView != null) {
         copyTextureView.close();
         copyTextureView = null;
      }

      if (copyTexture != null) {
         copyTexture.close();
         copyTexture = null;
      }

      for (int i = 0; i < 2; i++) {
         if (pingPongViews[i] != null) {
            pingPongViews[i].close();
            pingPongViews[i] = null;
         }

         if (pingPongTextures[i] != null) {
            pingPongTextures[i].close();
            pingPongTextures[i] = null;
         }
      }

      lastWidth = 0;
      lastHeight = 0;
      initialized = false;
      lastFrameTime = -1L;
   }
}
