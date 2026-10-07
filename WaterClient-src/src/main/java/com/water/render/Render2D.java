package com.water.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderPass;
import com.water.render.pipeline.ArcOutlinePipeline;
import com.water.render.pipeline.ArcPipeline;
import com.water.render.pipeline.BlurPipeline;
import com.water.render.pipeline.LiquidGlassPipeline;
import com.water.render.pipeline.OutlinePipeline;
import com.water.render.pipeline.RectanglePipeline;
import com.water.render.pipeline.TexturePipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public class Render2D {
   public static final List<Runnable> OVERRIDE_TASKS = new ArrayList<>();
   public static final float Z_OVERRIDE = 0.0F;
   public static final int FIXED_GUI_SCALE = 1;
   public static boolean scissorActive = false;
   public static int scissorX;
   public static int scissorY;
   public static int scissorWidth;
   public static int scissorHeight;

   public Render2D() {
   }

   public static int getWindowWidth() {
      Window window = MinecraftClient.getInstance().getWindow();
      return (int)Math.ceil(window.getWidth() / 1.0);
   }

   public static int getWindowHeight() {
      Window window = MinecraftClient.getInstance().getWindow();
      return (int)Math.ceil(window.getHeight() / 1.0);
   }

   public static float getScaleFactor() {
      Window window = MinecraftClient.getInstance().getWindow();
      int i = window.getScaleFactor();
      return i / 1.0F;
   }

   public static float scaleX(float value) {
      return value * getScaleFactor();
   }

   public static float scaleY(float value) {
      return value * getScaleFactor();
   }

   public static float scaleSize(float value) {
      return value * getScaleFactor();
   }

   public static Matrix4f createOrthoMatrix() {
      return new Matrix4f().ortho(0.0F, getWindowWidth(), getWindowHeight(), 0.0F, -1000.0F, 1000.0F);
   }

   public static Matrix4f getProjectionMatrix(DrawContext context) {
      Window window = MinecraftClient.getInstance().getWindow();
      Matrix3x2fStack matrix3x2fStack = context.getMatrices();
      return new Matrix4f()
         .ortho(0.0F, window.getScaledWidth(), window.getScaledHeight(), 0.0F, -1000.0F, 1000.0F)
         .mul(
            new Matrix4f(
               matrix3x2fStack.m00,
               matrix3x2fStack.m01,
               0.0F,
               0.0F,
               matrix3x2fStack.m10,
               matrix3x2fStack.m11,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               1.0F,
               0.0F,
               matrix3x2fStack.m20,
               matrix3x2fStack.m21,
               0.0F,
               1.0F
            )
         );
   }

   public static boolean isNonChatScreenOpen() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient.currentScreen != null && !(minecraftClient.currentScreen instanceof ChatScreen);
   }

   public static void drawRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color, boolean overlay) {
      drawRoundedRect(getProjectionMatrix(context), x, y, width, height, radius, color, overlay);
   }

   public static void drawRoundedRectGradient(DrawContext context, float x, float y, float width, float height, float radius, boolean overlay, int... colors) {
      vN(getProjectionMatrix(context), x, y, width, height, radius, radius, radius, radius, overlay, colors);
   }

   public static void G(
      DrawContext context, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBL, float radiusBR, boolean overlay, int... colors
   ) {
      vN(getProjectionMatrix(context), x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, overlay, colors);
   }

   public static void drawRoundedRect(Matrix4f matrix, float x, float y, float width, float height, float radius, int color, boolean overlay) {
      vN(matrix, x, y, width, height, radius, radius, radius, radius, overlay, color);
   }

   public static void vN(
      Matrix4f matrix, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBL, float radiusBR, boolean overlay, int... colors
   ) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> RectanglePipeline.vP(matrix, x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, 0.0F, colors));
      } else {
         RectanglePipeline.vP(matrix, x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, 0.0F, colors);
      }
   }

   public static void drawRoundedRectGradient(Matrix4f matrix, float x, float y, float width, float height, float radius, boolean overlay, int... colors) {
      vN(matrix, x, y, width, height, radius, radius, radius, radius, overlay, colors);
   }

   public static void drawRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color, boolean overlay) {
      vR(getProjectionMatrix(context), x, y, width, height, radius, radius, radius, radius, thickness, color, overlay);
   }

   public static void vS(
      DrawContext context, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBL, float radiusBR, float thickness, int color, boolean overlay
   ) {
      vR(getProjectionMatrix(context), x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, thickness, color, overlay);
   }

   public static void drawRoundedOutlineGradient(DrawContext context, float x, float y, float width, float height, float radius, float thickness, boolean overlay, int... colors) {
      vU(getProjectionMatrix(context), x, y, width, height, radius, radius, radius, radius, thickness, overlay, colors);
   }

   public static void vV(
      DrawContext context, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBL, float radiusBR, float thickness, boolean overlay, int... colors
   ) {
      vU(getProjectionMatrix(context), x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, thickness, overlay, colors);
   }

   public static void drawRoundedOutline(Matrix4f matrix, float x, float y, float width, float height, float radius, float thickness, int color, boolean overlay) {
      vR(matrix, x, y, width, height, radius, radius, radius, radius, thickness, color, overlay);
   }

   public static void vR(
      Matrix4f matrix, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBL, float radiusBR, float thickness, int color, boolean overlay
   ) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> OutlinePipeline.vY(matrix, x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, thickness, 0.0F, color));
      } else {
         OutlinePipeline.vY(matrix, x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, thickness, 0.0F, color);
      }
   }

   public static void drawRoundedOutlineGradient(Matrix4f matrix, float x, float y, float width, float height, float radius, float thickness, boolean overlay, int... colors) {
      vU(matrix, x, y, width, height, radius, radius, radius, radius, thickness, overlay, colors);
   }

   public static void vU(
      Matrix4f matrix, float x, float y, float width, float height, float radiusTL, float radiusTR, float radiusBL, float radiusBR, float thickness, boolean overlay, int... colors
   ) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> OutlinePipeline.vY(matrix, x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, thickness, 0.0F, colors));
      } else {
         OutlinePipeline.vY(matrix, x, y, width, height, radiusTL, radiusTR, radiusBL, radiusBR, thickness, 0.0F, colors);
      }
   }

   public static void drawBlur(DrawContext context, float x, float y, float width, float height, float radius, float alpha, boolean overlay) {
      Matrix4f matrix4f = getProjectionMatrix(context);
      if (overlay) {
         OVERRIDE_TASKS.add(() -> BlurPipeline.drawBlurredRect(matrix4f, x, y, width, height, radius, alpha, 0.0F));
      } else {
         BlurPipeline.drawBlurredRect(matrix4f, x, y, width, height, radius, alpha, 0.0F);
      }
   }

   public static void drawArc(DrawContext context, float var1, float var2, float var3, float var4, float var5, float var6, int color, boolean overlay) {
      drawArc(getProjectionMatrix(context), var1, var2, var3, var4, var5, var6, color, overlay);
   }

   public static void drawArcGradient(DrawContext context, float var1, float var2, float var3, float var4, float var5, float var6, boolean overlay, int... colors) {
      drawArcGradient(getProjectionMatrix(context), var1, var2, var3, var4, var5, var6, overlay, colors);
   }

   public static void drawArc(Matrix4f matrix, float var1, float var2, float var3, float var4, float var5, float var6, int color, boolean overlay) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> ArcPipeline.drawArc(matrix, var1, var2, var3, var4, var5, var6, 0.0F, color));
      } else {
         ArcPipeline.drawArc(matrix, var1, var2, var3, var4, var5, var6, 0.0F, color);
      }
   }

   public static void drawArcGradient(Matrix4f matrix, float var1, float var2, float var3, float var4, float var5, float var6, boolean overlay, int... colors) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> ArcPipeline.drawArc(matrix, var1, var2, var3, var4, var5, var6, 0.0F, colors));
      } else {
         ArcPipeline.drawArc(matrix, var1, var2, var3, var4, var5, var6, 0.0F, colors);
      }
   }

   public static void wk(
      DrawContext context, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int color, int outlineColor, boolean overlay
   ) {
      drawArcOutline(getProjectionMatrix(context), var1, var2, var3, var4, var5, var6, var7, color, outlineColor, overlay);
   }

   public static void drawArcOutline(Matrix4f matrix, float var1, float var2, float var3, float var4, float var5, float var6, float var7, int color, int outlineColor, boolean overlay) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> ArcOutlinePipeline.drawArcOutline(matrix, var1, var2, var3, var4, var5, var6, var7, color, outlineColor, 0.0F));
      } else {
         ArcOutlinePipeline.drawArcOutline(matrix, var1, var2, var3, var4, var5, var6, var7, color, outlineColor, 0.0F);
      }
   }

   public static void drawTexture(DrawContext context, float x, float y, float size, Identifier id, int color, float alpha, boolean overlay) {
      drawTexture(getProjectionMatrix(context), x, y, size, id, color, alpha, overlay);
   }

   public static void drawTexture(Matrix4f matrix, float x, float y, float size, Identifier id, int color, float alpha, boolean overlay) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      AbstractTexture abstractTexture = minecraftClient.getTextureManager().getTexture(id);
      if (abstractTexture != null) {
         if (overlay) {
            OVERRIDE_TASKS.add(() -> TexturePipeline.drawTexture(matrix, x, y, size, abstractTexture.getGlTextureView(), color, alpha, 0.0F));
            return;
         }

         TexturePipeline.drawTexture(matrix, x, y, size, abstractTexture.getGlTextureView(), color, alpha, 0.0F);
      }
   }

   public static void drawGlow(DrawContext context, float x, float y, float width, float height, float radius, float var6, float var7, int color, boolean overlay) {
      Matrix4f matrix4f = getProjectionMatrix(context);
      float[] afloat = new float[]{var7 * radius / 2.0F, var7 * radius / 2.0F, var7 * radius / 2.0F, var7 * radius / 2.0F};
      float f = (color >> 24 & 0xFF) / 255.0F;
      float f1 = height == 240.0F ? 100.0F : 50.0F;
      int i = color | 0xFF000000;
      float f2 = 1.0F;
      boolean flag = true;
      float f3 = 0.0F;
      if (overlay) {
         OVERRIDE_TASKS.add(() -> LiquidGlassPipeline.wt(matrix4f, x, y, width, height, afloat, color, f, f1, i, f2, flag, f3, var6, radius, 0.0F));
      } else {
         LiquidGlassPipeline.wt(matrix4f, x, y, width, height, afloat, color, f, f1, i, f2, flag, f3, var6, radius, 0.0F);
      }
   }

   public static void queueOverlayTask(Runnable task) {
      OVERRIDE_TASKS.add(task);
   }

   public static void flushOverlayTasks(DrawContext context) {
      if (!OVERRIDE_TASKS.isEmpty()) {
         GL11.glDisable(2929);
         OVERRIDE_TASKS.forEach(Runnable::run);
         OVERRIDE_TASKS.clear();
         GL11.glEnable(2929);
         scissorActive = false;
         GlStateManager._disableScissorTest();
      }
   }

   public static void clearOverlayTasks() {
      OVERRIDE_TASKS.clear();
   }

   public static boolean hasOverlayTasks() {
      return !OVERRIDE_TASKS.isEmpty();
   }

   public static void enableScissor(float x, float y, float width, float height, boolean overlay) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> applyScissor(x, y, width, height));
      } else {
         applyScissor(x, y, width, height);
      }
   }

   public static void applyScissor(float x, float y, float width, float height) {
      Window window = MinecraftClient.getInstance().getWindow();
      double d0 = window.getScaleFactor();
      int i = window.getFramebufferWidth();
      int j = window.getFramebufferHeight();
      scissorActive = true;
      scissorX = (int)x;
      scissorY = (int)y;
      scissorWidth = (int)width;
      scissorHeight = (int)height;
      int k = (int)Math.round(scissorX * d0);
      int l = (int)Math.round(scissorY * d0);
      int i1 = (int)Math.round(scissorWidth * d0);
      int j1 = (int)Math.round(scissorHeight * d0);
      int k1 = j - (l + j1);
      int l1 = Math.max(0, k);
      int i2 = Math.max(0, k1);
      int j2 = Math.min(i, k + i1);
      int k2 = Math.min(j, k1 + j1);
      int l2 = Math.max(0, j2 - l1);
      int i3 = Math.max(0, k2 - i2);
      if (l2 != 0 && i3 != 0) {
         GlStateManager._enableScissorTest();
         GlStateManager._scissorBox(l1, i2, l2, i3);
      } else {
         scissorActive = false;
         GlStateManager._disableScissorTest();
      }
   }

   public static void applyScissorToPass(RenderPass pass) {
      if (scissorActive) {
         pass.enableScissor(scissorX, scissorY, scissorX + scissorWidth, scissorY + scissorHeight);
      }
   }

   public static void disableScissor(boolean overlay) {
      if (overlay) {
         OVERRIDE_TASKS.add(() -> {
            scissorActive = false;
            GlStateManager._disableScissorTest();
         });
      } else {
         scissorActive = false;
         GlStateManager._disableScissorTest();
      }
   }

   public static boolean isInGameOrChat() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient.currentScreen == null || minecraftClient.currentScreen instanceof ChatScreen;
   }

   public static void pushUnscaled(DrawContext context) {
      Window window = MinecraftClient.getInstance().getWindow();
      double d0 = window.getScaleFactor();
      context.getMatrices().scale((float)(1.0 / d0), (float)(1.0 / d0));
   }

   public static void popUnscaled(DrawContext context) {
      Window window = MinecraftClient.getInstance().getWindow();
      double d0 = window.getScaleFactor();
      context.getMatrices().scale((float)d0, (float)d0);
   }

   public static int multiplyBrightness(int argb, float factor) {
      int i = argb & 0xFF000000;
      float f = (argb >> 16 & 0xFF) / 255.0F;
      float f1 = (argb >> 8 & 0xFF) / 255.0F;
      float f2 = (argb & 0xFF) / 255.0F;
      f = Math.min(f * factor, 1.0F);
      f1 = Math.min(f1 * factor, 1.0F);
      f2 = Math.min(f2 * factor, 1.0F);
      int j = (int)(f * 255.0F);
      int k = (int)(f1 * 255.0F);
      int l = (int)(f2 * 255.0F);
      return i | j << 16 | k << 8 | l;
   }

   public static int lerpArgb(int from, int to, float t) {
      int i = from >> 24 & 0xFF;
      int j = from >> 16 & 0xFF;
      int k = from >> 8 & 0xFF;
      int l = from & 0xFF;
      int i1 = to >> 24 & 0xFF;
      int j1 = to >> 16 & 0xFF;
      int k1 = to >> 8 & 0xFF;
      int l1 = to & 0xFF;
      int i2 = (int)(i + (i1 - i) * t);
      int j2 = (int)(j + (j1 - j) * t);
      int k2 = (int)(k + (k1 - k) * t);
      int l2 = (int)(l + (l1 - l) * t);
      return i2 << 24 | j2 << 16 | k2 << 8 | l2;
   }
}
