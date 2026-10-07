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

public class ArcOutlinePipeline {
   public static RenderPipeline pipeline;
   public static GpuBuffer uniformBuffer;
   public static final int UNIFORM_SIZE = 160;

   public ArcOutlinePipeline() {
   }

   public static void init() {
      if (pipeline == null) {
         try {
            pipeline = RenderPipeline.builder()
               .withLocation(Identifier.of("water", "arc_outline"))
               .withVertexShader(Identifier.of("water", "arc_outline_vertex"))
               .withFragmentShader(Identifier.of("water", "arc_outline_fragment"))
               .withVertexFormat(VertexFormat.builder().build(), DrawMode.TRIANGLES)
               .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withCull(false)
               .build();
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "ArcOutline2D Uniforms", 136, 160L);
         } catch (Exception exception) {
            System.err.println("[ArcOutline2D] Failed to init: " + exception.getMessage());
         }
      }
   }

   public static void drawArcOutline(Matrix4f matrix, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int var8, int var9, float var10) {
      if (pipeline == null) {
         init();
      }

      if (pipeline != null && uniformBuffer != null) {
         float f = (var8 >> 16 & 0xFF) / 255.0F;
         float f1 = (var8 >> 8 & 0xFF) / 255.0F;
         float f2 = (var8 & 0xFF) / 255.0F;
         float f3 = (var8 >> 24 & 0xFF) / 255.0F;
         float f4 = (var9 >> 16 & 0xFF) / 255.0F;
         float f5 = (var9 >> 8 & 0xFF) / 255.0F;
         float f6 = (var9 & 0xFF) / 255.0F;
         float f7 = (var9 >> 24 & 0xFF) / 255.0F;
         ByteBuffer byteBuffer = MemoryUtil.memAlloc(160);
         byteBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
         byteBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
         byteBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
         byteBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
         byteBuffer.position(64);
         byteBuffer.putFloat(var1).putFloat(var2).putFloat(var3).putFloat(var3);
         byteBuffer.putFloat(var3).putFloat(var4).putFloat(var5).putFloat(var6);
         byteBuffer.putFloat(var10).putFloat(var7).putFloat(0.0F).putFloat(0.0F);
         byteBuffer.putFloat(f).putFloat(f1).putFloat(f2).putFloat(f3);
         byteBuffer.putFloat(f4).putFloat(f5).putFloat(f6).putFloat(f7);
         byteBuffer.flip();
         CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
         commandEncoder.writeToBuffer(uniformBuffer.slice(), byteBuffer);
         MemoryUtil.memFree(byteBuffer);
         Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();

         try (RenderPass renderPass = commandEncoder.createRenderPass(
               () -> "ArcOutline2D", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.of(1.0)
            )) {
            Render2D.applyScissorToPass(renderPass);
            renderPass.setPipeline(pipeline);
            renderPass.setUniform("Uniforms", uniformBuffer);
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
