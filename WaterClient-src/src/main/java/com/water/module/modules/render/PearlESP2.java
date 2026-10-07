package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.ProjectedPoint;
import com.water.render.Render3D;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.lwjgl.opengl.GL11;

public final class PearlESP2 extends Module {
   public static PearlESP2 instance;
   public final Setting<Double> alpha = new Setting<>("Alpha", 180.0, 0.0, 255.0);
   public final Setting<Boolean> tracers = new Setting<>("Tracers", true);
   public final Setting<Double> tracerW = new Setting<>("Tracer Width", 1.5, 0.5, 5.0);
   public final Setting<Boolean> showOwner = new Setting<>("Player Name", true);
   public final Setting<Color> color = new Setting<>("Color", new Color(148, 0, 211));
   public final List<double[]> hudPositions = new CopyOnWriteArrayList<>();

   public PearlESP2() {
      super("Pearl ESP", Category.RENDER);
      instance = this;
      this.addSetting(this.alpha);
      this.addSetting(this.tracers);
      this.addSetting(this.tracerW);
      this.addSetting(this.showOwner);
      this.addSetting(this.color);
   }

   public static void renderHud(DrawContext context, float tickDelta) {
      PearlESP2 pearlesp2 = instance;
      if (pearlesp2 != null && pearlesp2.isEnabled() && pearlesp2.showOwner.getValue()) {
         if (mc.world != null && mc.player != null && !mc.options.hudHidden) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);

               for (Entity entity : mc.world.getEntities()) {
                  if (entity instanceof EnderPearlEntity enderPearlEntity && enderPearlEntity.getOwner() != null) {
                     String s = enderPearlEntity.getOwner().getName().getString();
                     if (!s.isEmpty()) {
                        double d0 = MathHelper.lerp((double)tickDelta, enderPearlEntity.lastRenderX, enderPearlEntity.getX());
                        double d1 = MathHelper.lerp((double)tickDelta, enderPearlEntity.lastRenderY, enderPearlEntity.getY()) + 0.5;
                        double d2 = MathHelper.lerp((double)tickDelta, enderPearlEntity.lastRenderZ, enderPearlEntity.getZ());
                        ProjectedPoint projectedPoint = new ProjectedPoint();
                        if (Render3D.projectToPoint(Render3D.modelViewMatrix, Render3D.projectionMatrix, d0, d1, d2, projectedPoint)
                           && projectedPoint.visible
                           && !(projectedPoint.z < 0.0)
                           && !(projectedPoint.z > 1.0)
                           && !(projectedPoint.w <= 0.0)) {
                           float f = (float)(0.5 * mc.getWindow().getScaledWidth() * 0.025 / projectedPoint.w);
                           if (Float.isFinite(f) && !(f <= 0.0F)) {
                              Matrix3x2fStack matrix3x2fStack = context.getMatrices();
                              matrix3x2fStack.pushMatrix();
                              matrix3x2fStack.translate((float)projectedPoint.x, (float)projectedPoint.y);
                              matrix3x2fStack.scale(f, f);
                              Color colorx = pearlesp2.color.getValue();
                              int i = 0xFF000000 | colorx.getRed() << 16 | colorx.getGreen() << 8 | colorx.getBlue();
                              int j = mc.textRenderer.getWidth(s);
                              context.drawText(mc.textRenderer, s, -(j / 2), -4, i, true);
                              matrix3x2fStack.popMatrix();
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            Vec3d vec3d1 = RenderUtils.getLookVector(camera);
            Vec3d vec3d2 = RenderUtils.getRightVector(camera);
            Vec3d vec3d3 = RenderUtils.crossNormalized(vec3d1, vec3d2);
            Vec3d vec3d4 = vec3d1.multiply(150.0);
            int i = Math.max(0, Math.min(255, (int)Math.round(this.alpha.getValue())));
            Color colorx = this.color.getValue();
            Color color1 = new Color(colorx.getRed(), colorx.getGreen(), colorx.getBlue(), i);
            Color color2 = new Color(colorx.getRed(), colorx.getGreen(), colorx.getBlue(), 255);
            boolean flag = false;

            for (Entity entity : mc.world.getEntities()) {
               if (entity instanceof EnderPearlEntity) {
                  flag = true;
                  break;
               }
            }

            if (flag) {
               matrices.push();
               GL11.glDisable(2929);

               try {
                  ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

                  for (Entity entity1 : mc.world.getEntities()) {
                     if (entity1 instanceof EnderPearlEntity enderPearlEntity) {
                        double d0 = MathHelper.lerp((double)tickDelta, enderPearlEntity.lastRenderX, enderPearlEntity.getX()) - vec3d.x;
                        double d1 = MathHelper.lerp((double)tickDelta, enderPearlEntity.lastRenderY, enderPearlEntity.getY()) - vec3d.y;
                        double d2 = MathHelper.lerp((double)tickDelta, enderPearlEntity.lastRenderZ, enderPearlEntity.getZ()) - vec3d.z;
                        double d3 = 0.25;
                        shapeBatch.renderFilledBox(d0 - d3, d1 - d3, d2 - d3, d0 + d3, d1 + d3, d2 + d3, color1);
                        if (this.tracers.getValue()) {
                           Vec3d vec3d5 = RenderUtils.projectToScreenPlane(d0, d1 + d3, d2, vec3d1, vec3d2, vec3d3, 24.0, 2.75);
                           shapeBatch.renderLine(color2, vec3d4, vec3d5, this.tracerW.getValue().floatValue());
                        }
                     }
                  }

                  shapeBatch.flush();
               } finally {
                  GL11.glEnable(2929);
               }

               matrices.pop();
            }
         }
      }
   }

   public static String watermarkFragment() {
      return "3";
   }
}
