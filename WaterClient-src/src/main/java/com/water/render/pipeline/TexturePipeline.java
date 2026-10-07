package com.water.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
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

public class TexturePipeline {
   public static RenderPipeline pipeline;
   public static GpuBuffer uniformBuffer;
   public static final int UNIFORM_SIZE = 128;

   public TexturePipeline() {
   }

   public static void init() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("water", "texture"))
               .withVertexShader(Identifier.of("water", "texture_vertex"))
               .withFragmentShader(Identifier.of("water", "texture_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withSampler("Sampler0")
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Texture2D Uniforms", 136, 128L);
         } catch (Exception exception) {
            System.err.println("[Texture2D] Failed to init: " + exception.getMessage());
            exception.printStackTrace();
         }
      }
   }

   public static void drawTexture(Matrix4f matrix, float var1, float var2, float var3, GpuTextureView texture, int var5, float var6, float var7) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null && texture != null) {
         float f = (var5 >> 16 & 0xFF) / 255.0F;
         float f1 = (var5 >> 8 & 0xFF) / 255.0F;
         float f2 = (var5 & 0xFF) / 255.0F;
         float f3 = (var5 >> 24 & 0xFF) / 255.0F;
         ByteBuffer byteBuffer = MemoryUtil.memAlloc(128);
         byteBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         byteBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         byteBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         byteBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         byteBuffer.position(64);
         byteBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var3);
         byteBuffer.putFloat(f).putFloat(f1).putFloat(f2).putFloat(f3);
         byteBuffer.putFloat(var6).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         byteBuffer.putFloat(var7).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         byteBuffer.flip();
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         commandEncoder.writeToBuffer(uniformBuffer.slice(), byteBuffer);
         MemoryUtil.memFree(byteBuffer);
         GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

         try (RenderPass renderPass = commandEncoder.createRenderPass(
               () -> "Texture2D", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.of(1.0)
            )) {
            Render2D.applyScissorToPass(renderPass);
            renderPass.setPipeline(pipeline);
            renderPass.setUniform("Uniforms", uniformBuffer);
            renderPass.bindTexture("Sampler0", texture, gpuSampler);
            renderPass.draw(0, 6);
         }
      }
   }

   public static void drawTextureRect(Matrix4f matrix, float var1, float var2, float var3, float var4, GpuTextureView texture, int var6, float var7) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null && texture != null) {
         float f = (var6 >> 16 & 0xFF) / 255.0F;
         float f1 = (var6 >> 8 & 0xFF) / 255.0F;
         float f2 = (var6 & 0xFF) / 255.0F;
         float f3 = (var6 >> 24 & 0xFF) / 255.0F;
         ByteBuffer byteBuffer = MemoryUtil.memAlloc(128);
         byteBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         byteBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         byteBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         byteBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         byteBuffer.position(64);
         byteBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
         byteBuffer.putFloat(f).putFloat(f1).putFloat(f2).putFloat(f3);
         byteBuffer.putFloat(0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         byteBuffer.putFloat(var7).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         byteBuffer.flip();
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         commandEncoder.writeToBuffer(uniformBuffer.slice(), byteBuffer);
         MemoryUtil.memFree(byteBuffer);
         GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

         try (RenderPass renderPass = commandEncoder.createRenderPass(
               () -> "AwtFont", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.of(1.0)
            )) {
            Render2D.applyScissorToPass(renderPass);
            renderPass.setPipeline(pipeline);
            renderPass.setUniform("Uniforms", uniformBuffer);
            renderPass.bindTexture("Sampler0", texture, gpuSampler);
            renderPass.draw(0, 6);
         }
      }
   }

   public static void close() {
      if (uniformBuffer != null) {
         uniformBuffer.close();
         uniformBuffer = null;
      }

      pipeline = null;
   }
}
