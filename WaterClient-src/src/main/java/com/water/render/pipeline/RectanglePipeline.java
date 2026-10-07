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
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public class RectanglePipeline {
   public static RenderPipeline pipeline;
   public static GpuBuffer uniformBuffer;
   public static final int UNIFORM_SIZE = 256;

   public RectanglePipeline() {
   }

   public static void init() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("water", "rectangle"))
               .withVertexShader(Identifier.of("water", "rectangle_vertex"))
               .withFragmentShader(Identifier.of("water", "rectangle_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "Rect2D Uniforms", 136, 256L);
         } catch (Exception exception) {
            System.err.println("[Rect2D] Failed to init: " + exception.getMessage());
            exception.printStackTrace();
         }
      }
   }

   public static void vP(
      Matrix4f matrix, float var1, float var2, float var3, float var4, float var5, float var6, float var7, float var8, float var9, int... var10
   ) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null) {
         int[] aint = expandColors(var10);
         ByteBuffer byteBuffer = MemoryUtil.memAlloc(256);
         byteBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         byteBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         byteBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         byteBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         byteBuffer.position(64);
         byteBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var4);
         byteBuffer.putFloat(var6).putFloat(var7).putFloat(var5).putFloat(var8);
         byteBuffer.position(96);
         byteBuffer.putFloat(var9).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
         byteBuffer.position(112);

         for (int i = 0; i < 9; i++) {
            int j = aint[i];
            byteBuffer.putFloat((j >> 16 & 0xFF) / 255.0F);
            byteBuffer.putFloat((j >> 8 & 0xFF) / 255.0F);
            byteBuffer.putFloat((j & 0xFF) / 255.0F);
            byteBuffer.putFloat((j >> 24 & 0xFF) / 255.0F);
         }

         byteBuffer.flip();
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         commandEncoder.writeToBuffer(uniformBuffer.slice(), byteBuffer);
         MemoryUtil.memFree(byteBuffer);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

         try (RenderPass renderPass = commandEncoder.createRenderPass(
               () -> "Rect2D", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.of(1.0)
            )) {
            Render2D.applyScissorToPass(renderPass);
            renderPass.setPipeline(pipeline);
            renderPass.setUniform("Uniforms", uniformBuffer);
            renderPass.draw(0, 6);
         }
      }
   }

   public static int[] expandColors(int[] colors) {
      if (colors.length == 1) {
         int j = colors[0];
         return new int[]{j, j, j, j, j, j, j, j, j};
      } else if (colors.length >= 9) {
         return colors;
      } else {
         int[] aint = new int[9];

         for (int i = 0; i < 9; i++) {
            aint[i] = i < colors.length ? colors[i] : colors[colors.length - 1];
         }

         return aint;
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
