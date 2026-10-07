package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Water;
import com.water.module.setting.EntityListSetting;
import com.water.module.setting.Setting;
import com.water.render.EspBoxData;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Set;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MobESP extends Module {
   public static final double TRACER_START_DISTANCE = 150.0;
   public static final double TRACER_END_DISTANCE = 24.0;
   public static final double TRACER_BEHIND_MIN_SPREAD = 2.75;
   public final EntityListSetting mobs = new EntityListSetting("Mobs");
   public final Setting<Double> alpha = new Setting<>("Alpha", 100.0, 0.0, 255.0);
   public final Setting<Double> range = new Setting<>("Range", 128.0, 16.0, 512.0);
   public final Setting<Boolean> tracers = new Setting<>("Tracers", false);
   public final Setting<Color> outlineColor = new Setting<>("Outline color", new Color(255, 80, 80));
   public final Setting<Color> fillColor = new Setting<>("Fill color", new Color(255, 80, 80));
   public final Setting<Color> tracerColor = new Setting<>("Tracer color", new Color(255, 80, 80));

   public MobESP() {
      super("Mob ESP", Category.RENDER);
      this.addSetting(this.mobs);
      this.addSetting(this.alpha);
      this.addSetting(this.range);
      this.addSetting(this.tracers);
      this.addSetting(this.outlineColor);
      this.addSetting(this.fillColor);
      this.addSetting(this.tracerColor);
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         Set set = this.mobs.getSelectedMobs();
         if (!set.isEmpty()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               double d0 = vec3d.x;
               double d1 = vec3d.y;
               double d2 = vec3d.z;
               double d3 = this.range.getValue() * this.range.getValue();
               int i = this.clampAlpha(this.alpha.getValue());
               boolean flag = this.tracers.getValue();
               Color color = this.scaleAlpha(this.outlineColor.getValue(), i);
               Color color1 = this.scaleAlpha(this.fillColor.getValue(), Math.max(0, i / 3));
               Color color2 = flag ? this.scaleAlpha(this.tracerColor.getValue(), 255) : null;
               Vec3d vec3d1 = flag ? RenderUtils.getLookVector(camera) : null;
               Vec3d vec3d2 = flag ? RenderUtils.getRightVector(camera) : null;
               Vec3d vec3d3 = flag ? RenderUtils.crossNormalized(vec3d1, vec3d2) : null;
               Vec3d vec3d4 = flag ? vec3d1.multiply(150.0) : null;
               ArrayList arrayList = new ArrayList();

               for (Entity entity : mc.world.getEntities()) {
                  if (entity != mc.player
                     && !(entity instanceof PlayerEntity)
                     && entity instanceof LivingEntity
                     && entity.isAlive()
                     && set.contains(entity.getType())) {
                     Vec3d vec3d5 = this.getLerpedPos(entity, tickDelta);
                     double d4 = vec3d5.x - d0;
                     double d5 = vec3d5.y - d1;
                     double d6 = vec3d5.z - d2;
                     double d7 = d4 * d4 + d5 * d5 + d6 * d6;
                     if (!(d7 > d3)) {
                        double d8 = entity.getWidth() / 2.0;
                        double d9 = entity.getHeight();
                        double d10 = d5 + d9 * 0.5;
                        arrayList.add(new EspBoxData(d4, d5, d6, d10, d8, d9));
                     }
                  }
               }

               if (!arrayList.isEmpty()) {
                  matrices.push();
                  ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

                  for (EspBoxData espBoxData : (Iterable<EspBoxData>)arrayList) {
                     shapeBatch.renderOutlineBox(
                        espBoxData.dx - espBoxData.halfWidth,
                        espBoxData.dy,
                        espBoxData.dz - espBoxData.halfWidth,
                        espBoxData.dx + espBoxData.halfWidth,
                        espBoxData.dy + espBoxData.height,
                        espBoxData.dz + espBoxData.halfWidth,
                        color
                     );
                     shapeBatch.renderFilledBox(
                        espBoxData.dx - espBoxData.halfWidth,
                        espBoxData.dy,
                        espBoxData.dz - espBoxData.halfWidth,
                        espBoxData.dx + espBoxData.halfWidth,
                        espBoxData.dy + espBoxData.height,
                        espBoxData.dz + espBoxData.halfWidth,
                        color1
                     );
                  }

                  shapeBatch.flush();
                  if (flag) {
                     ShapeBatch shapeBatch1 = RenderUtils.beginShapeBatch(matrices);

                     for (EspBoxData espBoxData1 : (Iterable<EspBoxData>)arrayList) {
                        Vec3d vec3d6 = RenderUtils.projectToScreenPlane(espBoxData1.dx, espBoxData1.tracerTargetY, espBoxData1.dz, vec3d1, vec3d2, vec3d3, 24.0, 2.75);
                        shapeBatch1.renderLine(color2, vec3d4, vec3d6, Water.getTracerWidth());
                     }

                     shapeBatch1.flush();
                  }

                  matrices.pop();
               }
            }
         }
      }
   }

   public int clampAlpha(double alpha) {
      int i = (int)Math.round(alpha);
      return Math.max(0, Math.min(255, i));
   }

   public Color scaleAlpha(Color color, int factor) {
      int i = Math.max(0, Math.min(255, Math.round(color.getAlpha() / 255.0F * factor)));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), i);
   }

   public Vec3d getLerpedPos(Entity entity, float var2) {
      try {
         return entity.getLerpedPos(var2);
      } catch (Throwable throwable) {
         double d0 = MathHelper.lerp((double)var2, entity.lastRenderX, entity.getX());
         double d1 = MathHelper.lerp((double)var2, entity.lastRenderY, entity.getY());
         double d2 = MathHelper.lerp((double)var2, entity.lastRenderZ, entity.getZ());
         return new Vec3d(d0, d1, d2);
      }
   }
}
