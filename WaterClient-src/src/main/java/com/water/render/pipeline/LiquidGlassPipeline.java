package com.water.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.water.render.Render2D;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class LiquidGlassPipeline {
   public static RenderPipeline pipeline;
   public static GpuBuffer uniformBuffer;
   public static final int UNIFORM_SIZE = 176;

   public LiquidGlassPipeline() {
   }

   public static void init() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("water", "liquidglass"))
               .withVertexShader(Identifier.of("water", "liquidglass_vertex"))
               .withFragmentShader(Identifier.of("water", "liquidglass_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withSampler("Sampler0")
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "LiquidGlass Uniforms", 136, 176L);
         } catch (Exception exception) {
            System.err.println("[LiquidGlass] Failed to init: " + exception.getMessage());
            exception.printStackTrace();
         }
      }
   }

   public static void wt(
      Matrix4f matrix,
      float var1,
      float var2,
      float var3,
      float var4,
      float[] var5,
      int var6,
      float var7,
      float var8,
      int var9,
      float var10,
      boolean var11,
      float var12,
      float var13,
      float var14,
      float var15
   ) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         Framebuffer framebuffer = minecraftClient.getFramebuffer();
         if (framebuffer != null && framebuffer.getColorAttachmentView() != null) {
            float f = (var9 >> 16 & 0xFF) / 255.0F;
            float f1 = (var9 >> 8 & 0xFF) / 255.0F;
            float f2 = (var9 & 0xFF) / 255.0F;
            float f3 = (var9 >> 24 & 0xFF) / 255.0F;
            float f4 = var5.length > 0 ? var5[0] : 0.0F;
            float f5 = var5.length > 1 ? var5[1] : f4;
            float f6 = var5.length > 2 ? var5[2] : f4;
            float f7 = var5.length > 3 ? var5[3] : f4;
            int i = framebuffer.textureWidth;
            int j = framebuffer.textureHeight;
            ByteBuffer byteBuffer = MemoryUtil.memAlloc(176);
            byteBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
            byteBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
            byteBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
            byteBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
            byteBuffer.position(64);
            byteBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
            byteBuffer.putFloat(i).putFloat(j);
            byteBuffer.position(96);
            byteBuffer.putFloat(f4).putFloat(f5).putFloat(f6).putFloat(f7);
            byteBuffer.position(112);
            byteBuffer.putFloat(var14);
            byteBuffer.putFloat(2.0F);
            byteBuffer.putFloat(var7);
            byteBuffer.putFloat(var8);
            byteBuffer.position(128);
            byteBuffer.putFloat(f).putFloat(f1).putFloat(f2).putFloat(f3);
            byteBuffer.position(144);
            byteBuffer.putFloat(var10);
            byteBuffer.putInt(var11 ? 1 : 0);
            byteBuffer.putFloat(var12);
            byteBuffer.putFloat(var13);
            byteBuffer.putFloat(var15);
            byteBuffer.putFloat(0.0F);
            byteBuffer.flip();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.writeToBuffer(uniformBuffer.slice(), byteBuffer);
            MemoryUtil.memFree(byteBuffer);
            GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

            try (RenderPass renderPass = commandEncoder.createRenderPass(
                  () -> "LiquidGlass", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.of(1.0)
               )) {
               Render2D.applyScissorToPass(renderPass);
               renderPass.setPipeline(pipeline);
               renderPass.setUniform("Uniforms", uniformBuffer);
               renderPass.bindTexture("Sampler0", framebuffer.getColorAttachmentView(), gpuSampler);
               renderPass.draw(0, 6);
            }
         }
      }
   }

   public static void xf() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      pipeline = null;
   }
}
