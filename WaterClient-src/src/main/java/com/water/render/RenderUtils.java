package com.water.render;

import java.awt.Color;
import java.lang.reflect.Method;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class RenderUtils {
   public static final Matrix4f POSITION_PROJECTION_MATRIX = new Matrix4f();
   public static final FrustumIntersection FRUSTUM = new FrustumIntersection();
   public static boolean frustumReady;
   public static double frustumX;
   public static double frustumY;
   public static double frustumZ;
   public static final ThreadLocal<RenderBatch> REUSABLE_BATCH = ThreadLocal.withInitial(RenderBatch::new);
   public static Method cameraPosMethod;

   public RenderUtils() {
   }

   public static ShapeBatch beginShapeBatch(MatrixStack matrices) {
      return new ShapeBatch(matrices);
   }

   public static void updateFrustum(Matrix4f matrix, Matrix4f matrix2, Vec3d vec) {
      if (matrix != null && matrix2 != null && vec != null) {
         matrix2.mul(matrix, POSITION_PROJECTION_MATRIX);
         FRUSTUM.set(POSITION_PROJECTION_MATRIX);
         frustumX = vec.x;
         frustumY = vec.y;
         frustumZ = vec.z;
         frustumReady = true;
      } else {
         frustumReady = false;
      }
   }

   public static boolean isBoxVisible(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      if (!frustumReady) {
         return true;
      } else {
         int i = FRUSTUM.intersectAab(
            (float)(minX - frustumX),
            (float)(minY - frustumY),
            (float)(minZ - frustumZ),
            (float)(maxX - frustumX),
            (float)(maxY - frustumY),
            (float)(maxZ - frustumZ)
         );
         return i == -1 || i == -2;
      }
   }

   public static Camera getCamera() {
      return MinecraftClient.getInstance().gameRenderer.getCamera();
   }

   public static Vec3d getCameraPos(Camera camera) {
      if (cameraPosMethod == null) {
         for (Method method : Camera.class.getMethods()) {
            if (method.getReturnType() == Vec3d.class && method.getParameterCount() == 0) {
               cameraPosMethod = method;
               break;
            }
         }
      }

      try {
         return (Vec3d)cameraPosMethod.invoke(camera);
      } catch (Exception exception) {
         return MinecraftClient.getInstance().player.getCameraPosVec(1.0F);
      }
   }

   public static void drawFilledBox(MatrixStack matrices, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color color) {
      RenderBatch renderBatch = REUSABLE_BATCH.get();
      renderBatch.begin(matrices);
      renderBatch.renderFilledBox(minX, minY, minZ, maxX, maxY, maxZ, color);
   }

   public static void drawOutlineBox(MatrixStack matrices, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Color color) {
      RenderBatch renderBatch = REUSABLE_BATCH.get();
      renderBatch.begin(matrices);
      renderBatch.renderOutlineBox(minX, minY, minZ, maxX, maxY, maxZ, color);
      renderBatch.flush();
   }

   public static void drawLine(MatrixStack matrices, Color color, Vec3d vec, Vec3d vec2) {
      drawLine(matrices, color, vec, vec2, 2.0F);
   }

   public static void drawLine(MatrixStack matrices, Color color, Vec3d vec, Vec3d vec2, float width) {
      RenderBatch renderBatch = REUSABLE_BATCH.get();
      renderBatch.begin(matrices);
      renderBatch.renderLine(color, vec, vec2, width);
      renderBatch.flush();
   }

   public static Vec3d getLookVector(Camera camera) {
      return new Vec3d(0.0, 0.0, 1.0).rotateX(-((float)Math.toRadians(camera.getPitch()))).rotateY(-((float)Math.toRadians(camera.getYaw()))).normalize();
   }

   public static Vec3d getRightVector(Camera camera) {
      return new Vec3d(1.0, 0.0, 0.0).rotateY(-((float)Math.toRadians(camera.getYaw()))).normalize();
   }

   public static Vec3d crossNormalized(Vec3d vec, Vec3d vec2) {
      return vec.crossProduct(vec2).normalize();
   }

   public static Vec3d projectToScreenPlane(double x, double y, double z, Vec3d vec, Vec3d vec2, Vec3d vec3, double var9, double var11) {
      double d0 = x * vec2.x + y * vec2.y + z * vec2.z;
      double d1 = x * vec3.x + y * vec3.y + z * vec3.z;
      double d2 = x * vec.x + y * vec.y + z * vec.z;
      double d3 = Math.max(Math.abs(d2), 0.25);
      double d4 = d0 / d3;
      double d5 = d1 / d3;
      if (d2 <= 0.0) {
         double d6 = Math.hypot(d4, d5);
         if (d6 < var11) {
            if (d6 < 1.0E-4) {
               d4 = var11;
               d5 = 0.0;
            } else {
               double d7 = var11 / d6;
               d4 *= d7;
               d5 *= d7;
            }
         }
      }

      return vec.add(vec2.multiply(d4)).add(vec3.multiply(d5)).normalize().multiply(var9);
   }

   public static Vec3d projectToScreenPlane(Vec3d vec, Vec3d vec2, Vec3d vec3, Vec3d vec4, double z, double var6) {
      return projectToScreenPlane(vec.x, vec.y, vec.z, vec2, vec3, vec4, z, var6);
   }

   public static Vec3d projectToScreen(Vec3d vec, Vec3d vec2, Vec3d vec3, Vec3d vec4, Vec3d vec5, double var5) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      int i = minecraftClient.getWindow().getScaledWidth();
      int j = minecraftClient.getWindow().getScaledHeight();
      Pair pair = Render3D.project(Render3D.modelViewMatrix, Render3D.projectionMatrix, vec2);
      if (pair != null && (Boolean)pair.getRight()) {
         Vec3d vec3d = (Vec3d)pair.getLeft();
         if (vec3d.x >= 0.0 && vec3d.x <= i && vec3d.y >= 0.0 && vec3d.y <= j) {
            return vec;
         }
      }

      Vector3f vector3f = Render3D.projectClamped(Render3D.modelViewMatrix, Render3D.projectionMatrix, vec2);
      return vector3f == null ? vec : unproject(vector3f.x, vector3f.y, i, j, vec3, vec4, vec5).multiply(var5);
   }

   public static Vec3d unproject(float screenX, float screenY, int screenWidth, int screenHeight, Vec3d vec, Vec3d vec2, Vec3d vec3) {
      double d0 = screenX / screenWidth * 2.0F - 1.0F;
      double d1 = 1.0F - screenY / screenHeight * 2.0F;
      double d2 = Render3D.projectionMatrix.m11() / Render3D.projectionMatrix.m00();
      double d3 = 1.0 / Render3D.projectionMatrix.m11();
      return vec.add(vec2.multiply(d0 * d2 * d3)).add(vec3.multiply(d1 * d3)).normalize();
   }

   public static String watermarkFragment() {
      return "D";
   }
}
