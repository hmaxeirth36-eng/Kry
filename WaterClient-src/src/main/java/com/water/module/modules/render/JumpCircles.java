package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import com.water.util.TimedPos;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

public final class JumpCircles extends Module {
   public static final int SEGMENTS = 128;
   public final Setting<Float> lifetime = new Setting<>("Lifetime (s)", 1.5F, 0.1F, 5.0F);
   public final Setting<Float> startRadius = new Setting<>("Start Radius", 0.55F, 0.1F, 3.0F);
   public final Setting<Float> endRadius = new Setting<>("End Radius", 1.8F, 0.2F, 6.0F);
   public final Setting<Float> lineWidth = new Setting<>("Line Width", 2.0F, 0.5F, 6.0F);
   public final Setting<Color> color = new Setting<>("Color", new Color(120, 220, 255, 255));
   public final Setting<Boolean> glowMode = new Setting<>("Glow Mode", false);
   public final Setting<Boolean> glowFilled = new Setting<>("Glow Filled", false);
   public final List<TimedPos> circles = new ArrayList<>();
   public boolean wasOnGround = true;
   public double lastGroundX = 0.0;
   public double lastGroundY = 0.0;
   public double lastGroundZ = 0.0;

   public JumpCircles() {
      super("JumpCircles", Category.RENDER);
      this.addSetting(this.lifetime);
      this.addSetting(this.startRadius);
      this.addSetting(this.endRadius);
      this.addSetting(this.lineWidth);
      this.addSetting(this.color);
      this.addSetting(this.glowMode);
      this.addSetting(this.glowFilled);
   }

   @Override
   public void onEnable() {
      this.circles.clear();
      this.wasOnGround = true;
   }

   @Override
   public void onDisable() {
      this.circles.clear();
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         boolean flag = mc.player.isOnGround();
         if (flag) {
            this.lastGroundX = mc.player.getX();
            this.lastGroundY = mc.player.getY();
            this.lastGroundZ = mc.player.getZ();
         }

         if (this.wasOnGround && !flag && mc.player.getVelocity().y > 0.0) {
            this.circles.add(new TimedPos(this.lastGroundX, this.lastGroundY + 0.02, this.lastGroundZ, System.currentTimeMillis()));
         }

         this.wasOnGround = flag;
         long i = System.currentTimeMillis();
         long j = (long)(this.lifetime.getValue() * 1000.0F);
         Iterator iterator = this.circles.iterator();

         while (iterator.hasNext()) {
            if (i - ((TimedPos)iterator.next()).bornMs >= j) {
               iterator.remove();
            }
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.circles.isEmpty()) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            long i = System.currentTimeMillis();
            long j = (long)(this.lifetime.getValue() * 1000.0F);
            double d0 = this.startRadius.getValue().floatValue();
            double d1 = this.endRadius.getValue().floatValue();
            float f = this.lineWidth.getValue();
            Color colorx = this.color.getValue();
            int k = colorx.getAlpha();
            matrices.push();
            ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

            for (TimedPos timedPos : this.circles) {
               float f1 = (float)(i - timedPos.bornMs) / (float)j;
               if (f1 < 0.0F) {
                  f1 = 0.0F;
               }

               if (f1 > 1.0F) {
                  f1 = 1.0F;
               }

               float f2 = 1.0F - (1.0F - f1) * (1.0F - f1) * (1.0F - f1);
               double d2 = d0 + (d1 - d0) * f2;
               int l = (int)(k * (1.0F - f2));
               if (l > 0) {
                  Color color1 = new Color(colorx.getRed(), colorx.getGreen(), colorx.getBlue(), l);
                  double d3 = timedPos.x - vec3d.x;
                  double d4 = timedPos.y - vec3d.y;
                  double d5 = timedPos.z - vec3d.z;
                  Vec3d[] avec3d = new Vec3d[129];

                  for (int i1 = 0; i1 <= 128; i1++) {
                     double d6 = (Math.PI * 2) * (i1 / 128.0);
                     avec3d[i1] = new Vec3d(d3 + Math.cos(d6) * d2, d4, d5 + Math.sin(d6) * d2);
                  }

                  if (this.glowFilled.getValue()) {
                     int j2 = colorx.getRed();
                     int l2 = colorx.getGreen();
                     int j1 = colorx.getBlue();
                     byte b0 = 32;
                     float f3 = f * 8.0F;

                     for (int k1 = 1; k1 <= b0; k1++) {
                        double d7 = (double)k1 / b0;
                        double d8 = d2 * (1.0 - d7);
                        if (!(d8 < 0.05)) {
                           int l1 = Math.max(1, (int)(l * (1.0 - d7 * 0.6) * 0.55));
                           Color color4 = new Color(j2, l2, j1, l1);
                           Vec3d[] avec3d1 = new Vec3d[129];

                           for (int i2 = 0; i2 <= 128; i2++) {
                              double d9 = (Math.PI * 2) * (i2 / 128.0);
                              avec3d1[i2] = new Vec3d(d3 + Math.cos(d9) * d8, d4, d5 + Math.sin(d9) * d8);
                           }

                           drawCirclePath(shapeBatch, avec3d1, color4, f3);
                        }
                     }
                  }

                  if (this.glowMode.getValue()) {
                     int k2 = colorx.getRed();
                     int i3 = colorx.getGreen();
                     int j3 = colorx.getBlue();
                     Color color5 = new Color(k2, i3, j3, Math.max(1, l / 16));
                     Color color6 = new Color(k2, i3, j3, Math.max(1, l / 12));
                     Color color7 = new Color(k2, i3, j3, Math.max(1, l / 9));
                     Color color8 = new Color(k2, i3, j3, Math.max(1, l / 6));
                     Color color2 = new Color(k2, i3, j3, Math.max(1, l / 4));
                     Color color9 = new Color(k2, i3, j3, Math.max(1, l / 2));
                     Color color3 = new Color(k2, i3, j3, Math.min(255, (int)(l * 1.0F)));
                     Color color10 = new Color(255, 255, 255, Math.min(255, (int)(l * 1.4F)));
                     drawCirclePath(shapeBatch, avec3d, color5, f * 18.0F);
                     drawCirclePath(shapeBatch, avec3d, color6, f * 14.0F);
                     drawCirclePath(shapeBatch, avec3d, color7, f * 11.0F);
                     drawCirclePath(shapeBatch, avec3d, color8, f * 8.5F);
                     drawCirclePath(shapeBatch, avec3d, color2, f * 6.0F);
                     drawCirclePath(shapeBatch, avec3d, color9, f * 4.0F);
                     drawCirclePath(shapeBatch, avec3d, color3, f * 2.8F);
                     drawCirclePath(shapeBatch, avec3d, color10, f * 1.6F);
                  } else {
                     drawCirclePath(shapeBatch, avec3d, color1, f);
                  }
               }
            }

            shapeBatch.flush();
            matrices.pop();
         }
      }
   }

   public static void drawCirclePath(ShapeBatch batch, Vec3d[] vec, Color color, float var3) {
      for (int i = 1; i < vec.length; i++) {
         batch.renderLine(color, vec[i - 1], vec[i], var3);
      }
   }
}
