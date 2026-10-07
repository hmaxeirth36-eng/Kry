package com.water.render;

import java.awt.Color;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class RenderBatch {
   public final BufferAllocator fillAllocator = new BufferAllocator(524288);
   public final Immediate fillImmediate = VertexConsumerProvider.immediate(this.fillAllocator);
   public final BufferAllocator lineAllocator = new BufferAllocator(524288);
   public final Immediate lineImmediate = VertexConsumerProvider.immediate(this.lineAllocator);
   public MatrixStack matrices;

   public RenderBatch() {
   }

   public void begin(MatrixStack matrices) {
      this.matrices = matrices;
   }

   public void renderFilledBox(double var1, double var3, double var5, double var7, double var9, double var11, Color color) {
      int i = toArgb(color);
      Entry entry = this.matrices.peek();
      float f = (float)var1;
      float f1 = (float)var3;
      float f2 = (float)var5;
      float f3 = (float)var7;
      float f4 = (float)var9;
      float f5 = (float)var11;
      VertexConsumer vertexConsumer = this.fillImmediate.getBuffer(RenderLayers.debugFilledBox());
      vertexConsumer.vertex(entry, f, f1, f2).color(i);
      vertexConsumer.vertex(entry, f3, f1, f2).color(i);
      vertexConsumer.vertex(entry, f3, f1, f5).color(i);
      vertexConsumer.vertex(entry, f, f1, f5).color(i);
      vertexConsumer.vertex(entry, f, f4, f2).color(i);
      vertexConsumer.vertex(entry, f, f4, f5).color(i);
      vertexConsumer.vertex(entry, f3, f4, f5).color(i);
      vertexConsumer.vertex(entry, f3, f4, f2).color(i);
      vertexConsumer.vertex(entry, f, f1, f2).color(i);
      vertexConsumer.vertex(entry, f, f4, f2).color(i);
      vertexConsumer.vertex(entry, f3, f4, f2).color(i);
      vertexConsumer.vertex(entry, f3, f1, f2).color(i);
      vertexConsumer.vertex(entry, f3, f1, f5).color(i);
      vertexConsumer.vertex(entry, f3, f4, f5).color(i);
      vertexConsumer.vertex(entry, f, f4, f5).color(i);
      vertexConsumer.vertex(entry, f, f1, f5).color(i);
      vertexConsumer.vertex(entry, f, f1, f5).color(i);
      vertexConsumer.vertex(entry, f, f4, f5).color(i);
      vertexConsumer.vertex(entry, f, f4, f2).color(i);
      vertexConsumer.vertex(entry, f, f1, f2).color(i);
      vertexConsumer.vertex(entry, f3, f1, f2).color(i);
      vertexConsumer.vertex(entry, f3, f4, f2).color(i);
      vertexConsumer.vertex(entry, f3, f4, f5).color(i);
      vertexConsumer.vertex(entry, f3, f1, f5).color(i);
      boolean flag = GL11.glIsEnabled(2929);
      boolean flag1 = GL11.glGetBoolean(2930);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      this.fillImmediate.draw();
      GL11.glDepthMask(flag1);
      if (flag) {
         GL11.glEnable(2929);
      } else {
         GL11.glDisable(2929);
      }
   }

   public void renderOutlineBox(double var1, double var3, double var5, double var7, double var9, double var11, Color color) {
      VertexConsumer vertexConsumer = this.lineImmediate.getBuffer(RenderLayers.lines());
      VertexRendering.drawOutline(this.matrices, vertexConsumer, VoxelShapes.cuboid(var1, var3, var5, var7, var9, var11), 0.0, 0.0, 0.0, toArgb(color), 2.0F);
   }

   public void renderLine(Color color, Vec3d vec, Vec3d vec2, float var4) {
      VertexConsumer vertexConsumer = this.lineImmediate.getBuffer(RenderLayers.lines());
      int i = toArgb(color);
      Entry entry = this.matrices.peek();
      Vector3f vector3f = new Vector3f((float)(vec2.x - vec.x), (float)(vec2.y - vec.y), (float)(vec2.z - vec.z)).normalize();
      float f = Math.max(1.0F, var4);
      vertexConsumer.vertex(entry, (float)vec.x, (float)vec.y, (float)vec.z).color(i).normal(entry, vector3f).lineWidth(f);
      vertexConsumer.vertex(entry, (float)vec2.x, (float)vec2.y, (float)vec2.z).color(i).normal(entry, vector3f).lineWidth(f);
   }

   public void flush() {
      boolean flag = GL11.glIsEnabled(2929);
      boolean flag1 = GL11.glGetBoolean(2930);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      this.lineImmediate.draw();
      GL11.glDepthMask(flag1);
      if (flag) {
         GL11.glEnable(2929);
      } else {
         GL11.glDisable(2929);
      }
   }

   public static int toArgb(Color color) {
      return color.getAlpha() << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }
}
