package com.water.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.water.render.Render2D;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class EspGlowPipeline {
   public static RenderPipeline pipeline;
   public static GpuBuffer uniformBuffer;
   public static final int UNIFORM_SIZE = 128;
   public static float defaultGlowSize = 12.0F;

   public EspGlowPipeline() {
   }

   public static void init() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("water", "esp_glow_box"))
               .withVertexShader(Identifier.of("water", "esp_glow_box_vertex"))
               .withFragmentShader(Identifier.of("water", "esp_glow_box_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ESPGlow Uniforms", 136, 128L);
         } catch (Exception exception) {
            System.err.println("[ESPGlowPipeline] Init failed: " + exception.getMessage());
         }
      }
   }

   public static void drawGlowBox(Matrix4f matrix, float var1, float var2, float var3, float var4, Color color, float var6, float var7) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null) {
         ByteBuffer byteBuffer = MemoryUtil.memAlloc(128);

         try {
            byteBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
            byteBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
            byteBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
            byteBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
            byteBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
            byteBuffer.putFloat(color.getRed() / 255.0F);
            byteBuffer.putFloat(color.getGreen() / 255.0F);
            byteBuffer.putFloat(color.getBlue() / 255.0F);
            byteBuffer.putFloat(color.getAlpha() / 255.0F);
            byteBuffer.putFloat(var6);
            byteBuffer.putFloat(var7);
            byteBuffer.putFloat(0.0F).putFloat(0.0F);
            byteBuffer.flip();
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.writeToBuffer(uniformBuffer.slice(), byteBuffer);
            Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

            try (RenderPass renderPass = commandEncoder.createRenderPass(
                  () -> "ESPGlow", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.empty()
               )) {
               Render2D.applyScissorToPass(renderPass);
               renderPass.setPipeline(pipeline);
               renderPass.setUniform("Uniforms", uniformBuffer);
               renderPass.draw(0, 6);
            }
         } finally {
            MemoryUtil.memFree(byteBuffer);
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
