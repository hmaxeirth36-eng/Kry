package com.water.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class Render3D {
   public static final Matrix4f projectionMatrix = new Matrix4f();
   public static final Matrix4f modelViewMatrix = new Matrix4f();
   public static final Matrix4f positionMatrix = new Matrix4f();
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public Render3D() {
   }

   public static Vec3d projectWithGl(Vec3d vec) {
      Camera camera = mc.getEntityRenderDispatcher().camera;
      int i = mc.getWindow().getHeight();
      int[] aint = new int[4];
      GL11.glGetIntegerv(2978, aint);
      Vector3f vector3f = new Vector3f();
      double d0 = vec.x - camera.getCameraPos().x;
      double d1 = vec.y - camera.getCameraPos().y;
      double d2 = vec.z - camera.getCameraPos().z;
      Vector4f vector4f = new Vector4f((float)d0, (float)d1, (float)d2, 1.0F).mul(positionMatrix);
      Matrix4f matrix4f = new Matrix4f(projectionMatrix);
      Matrix4f matrix4f1 = new Matrix4f(modelViewMatrix);
      matrix4f.mul(matrix4f1).project(vector4f.x(), vector4f.y(), vector4f.z(), aint, vector3f);
      return new Vec3d(vector3f.x / mc.getWindow().getScaleFactor(), (i - vector3f.y) / mc.getWindow().getScaleFactor(), vector3f.z);
   }

   public static Pair<Vec3d, Boolean> project(Matrix4f matrix, Matrix4f matrix2, Vec3d vec) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         ProjectedPoint projectedPoint = new ProjectedPoint();
         return !projectToPoint(matrix, matrix2, vec.x, vec.y, vec.z, projectedPoint)
            ? null
            : new Pair<>(new Vec3d(projectedPoint.x, projectedPoint.y, projectedPoint.z), projectedPoint.visible);
      } else {
         return null;
      }
   }

   public static boolean projectToPoint(Matrix4f matrix, Matrix4f matrix2, double var2, double var4, double var6, ProjectedPoint point) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null && point != null) {
         Vec3d vec3d = mc.gameRenderer.getCamera().getCameraPos();
         double d0 = var2 - vec3d.x;
         double d1 = var4 - vec3d.y;
         double d2 = var6 - vec3d.z;
         double d3 = matrix.m00() * d0 + matrix.m10() * d1 + matrix.m20() * d2 + matrix.m30();
         double d4 = matrix.m01() * d0 + matrix.m11() * d1 + matrix.m21() * d2 + matrix.m31();
         double d5 = matrix.m02() * d0 + matrix.m12() * d1 + matrix.m22() * d2 + matrix.m32();
         double d6 = matrix.m03() * d0 + matrix.m13() * d1 + matrix.m23() * d2 + matrix.m33();
         double d7 = matrix2.m00() * d3 + matrix2.m10() * d4 + matrix2.m20() * d5 + matrix2.m30() * d6;
         double d8 = matrix2.m01() * d3 + matrix2.m11() * d4 + matrix2.m21() * d5 + matrix2.m31() * d6;
         double d9 = matrix2.m02() * d3 + matrix2.m12() * d4 + matrix2.m22() * d5 + matrix2.m32() * d6;
         double d10 = matrix2.m03() * d3 + matrix2.m13() * d4 + matrix2.m23() * d5 + matrix2.m33() * d6;
         boolean flag = d10 > 0.0;
         double d11 = d10 != 0.0 ? 1.0 / d10 : 0.0;
         double d12 = d7 * d11;
         double d13 = d8 * d11;
         double d14 = d9 * d11;
         double d15 = (d12 * 0.5 + 0.5) * mc.getWindow().getScaledWidth();
         double d16 = (0.5 - d13 * 0.5) * mc.getWindow().getScaledHeight();
         point.set(d15, d16, d14, d10, flag);
         return true;
      } else {
         return false;
      }
   }

   public static Vector3f projectToVector(Matrix4f matrix, Matrix4f matrix2, Vec3d vec) {
      Pair pair = project(matrix, matrix2, vec);
      if (pair == null) {
         return null;
      } else {
         Vec3d vec3d = (Vec3d)pair.getLeft();
         return new Vector3f((float)vec3d.x, (float)vec3d.y, (float)vec3d.z);
      }
   }

   public static Vector3f worldToScreen(double var0, double var2, double var4) {
      return mc.gameRenderer != null && mc.getCameraEntity() != null ? worldToScreen(new Vec3d(var0, var2, var4)) : null;
   }

   public static Vector3f worldToScreen(Vec3d vec) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         Vec3d vec3d = projectWithGl(vec);
         return !(vec3d.z < 0.0) && !(vec3d.z > 1.0) ? new Vector3f((float)vec3d.x, (float)vec3d.y, (float)vec3d.z) : null;
      } else {
         return null;
      }
   }

   public static Vector3f projectClamped(Matrix4f matrix, Matrix4f matrix2, Vec3d vec) {
      if (mc.gameRenderer != null && mc.getCameraEntity() != null) {
         Vec3d vec3d = vec.subtract(mc.gameRenderer.getCamera().getCameraPos());
         if (vec3d.lengthSquared() < 1.0E-4) {
            return new Vector3f(mc.getWindow().getScaledWidth() / 2.0F, mc.getWindow().getScaledHeight() / 2.0F, 0.0F);
         } else {
            Vector4f vector4f = new Vector4f((float)vec3d.x, (float)vec3d.y, (float)vec3d.z, 1.0F);
            vector4f.mul(matrix);
            vector4f.mul(matrix2);
            boolean flag = vector4f.w() <= 0.0F;
            float f = Math.abs(vector4f.w());
            if (f < 0.001F) {
               f = 0.001F;
            }

            float f1 = vector4f.x() / f;
            float f2 = vector4f.y() / f;
            float f3 = mc.getWindow().getScaledWidth();
            float f4 = mc.getWindow().getScaledHeight();
            float f5 = f3 / 2.0F;
            float f6 = f4 / 2.0F;
            float f7 = (f1 * 0.5F + 0.5F) * f3;
            float f8 = (0.5F - f2 * 0.5F) * f4;
            if (!flag && f7 >= 0.0F && f7 <= f3 && f8 >= 0.0F && f8 <= f4) {
               return new Vector3f(f7, f8, 0.0F);
            } else {
               float f9 = f7 - f5;
               float f10 = f8 - f6;
               if (f9 == 0.0F && f10 == 0.0F) {
                  f10 = 1.0F;
               }

               float f11 = 10.0F;
               float f12 = f3 / 2.0F - f11;
               float f13 = f4 / 2.0F - f11;
               float f14 = Float.MAX_VALUE;
               float f15 = Float.MAX_VALUE;
               if (f9 != 0.0F) {
                  f14 = Math.abs(f12 / f9);
               }

               if (f10 != 0.0F) {
                  f15 = Math.abs(f13 / f10);
               }

               float f16 = Math.min(f14, f15);
               return new Vector3f(f5 + f9 * f16, f6 + f10 * f16, 0.0F);
            }
         }
      } else {
         return null;
      }
   }
}
