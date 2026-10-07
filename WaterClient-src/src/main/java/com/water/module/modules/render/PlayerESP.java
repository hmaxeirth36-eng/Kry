package com.water.module.modules.render;

import com.mojang.authlib.GameProfile;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Friends;
import com.water.module.modules.client.Water;
import com.water.module.setting.Setting;
import com.water.render.EspRenderData;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayList;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class PlayerESP extends Module {
   public static final double TRACER_START_DISTANCE = 150.0;
   public static final double TRACER_END_DISTANCE = 24.0;
   public static final double TRACER_BEHIND_MIN_SPREAD = 2.75;
   public final Setting<Double> fillAlpha = new Setting<>("Fill Alpha", 180.0, 0.0, 255.0);
   public final Setting<Double> range = new Setting<>("Range", 256.0, 16.0, 512.0);
   public final Setting<Boolean> tracers = new Setting<>("Tracers", false);
   public final Setting<Color> fillColor = new Setting<>("Fill color", new Color(255, 0, 0));
   public final Setting<Color> tracerColor = new Setting<>("Tracer color", new Color(255, 0, 0));

   public PlayerESP() {
      super("Player ESP", Category.RENDER);
      this.addSetting(this.fillAlpha);
      this.addSetting(this.range);
      this.addSetting(this.tracers);
      this.addSetting(this.fillColor);
      this.addSetting(this.tracerColor);
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            double d0 = vec3d.x;
            double d1 = vec3d.y;
            double d2 = vec3d.z;
            double d3 = this.range.getValue() * this.range.getValue();
            int i = this.clampAlpha(this.fillAlpha.getValue());
            boolean flag = this.tracers.getValue();
            Color color = this.scaleAlpha(this.fillColor.getValue(), i);
            Color color1 = flag ? this.scaleAlpha(this.tracerColor.getValue(), 255) : null;
            Vec3d vec3d1 = flag ? RenderUtils.getLookVector(camera) : null;
            Vec3d vec3d2 = flag ? Freecam.getTracerOrigin(vec3d, tickDelta) : null;
            Vec3d vec3d3 = flag ? (vec3d2.equals(vec3d) ? vec3d1.multiply(150.0) : vec3d2.subtract(vec3d)) : null;
            ArrayList arrayList = new ArrayList();

            for (PlayerEntity playerEntity : mc.world.getPlayers()) {
               if (playerEntity != mc.player && playerEntity.isAlive() && !playerEntity.isSpectator()) {
                  boolean flag1 = Friends.isEspColorEnabled() && Friends.isFriend(this.getProfileName(playerEntity));
                  Vec3d vec3d4 = this.getLerpedPos(playerEntity, tickDelta);
                  double d4 = vec3d4.x;
                  double d5 = vec3d4.y;
                  double d6 = vec3d4.z;
                  double d7 = d4 - d0;
                  double d8 = d5 - d1;
                  double d9 = d6 - d2;
                  double d10 = d7 * d7 + d8 * d8 + d9 * d9;
                  if (!(d10 > d3)) {
                     Color color2;
                     Color color3;
                     if (flag1) {
                        Color color4 = Friends.getFriendColor();
                        color2 = this.scaleAlpha(color4, i);
                        color3 = flag ? this.scaleAlpha(color4, 255) : null;
                     } else {
                        color2 = color;
                        color3 = color1;
                     }

                     double d13 = playerEntity.getWidth() / 2.0;
                     double d11 = playerEntity.getHeight();
                     boolean flag2 = true;
                     double d12 = d8 + playerEntity.getHeight() * 0.5;
                     arrayList.add(new EspRenderData(d7, d8, d9, d12, d13, d11, color2, color3, flag2));
                  }
               }
            }

            if (!arrayList.isEmpty()) {
               matrices.push();
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

               for (EspRenderData espRenderData : (Iterable<EspRenderData>)arrayList) {
                  if (espRenderData.boxVisible) {
                     shapeBatch.renderFilledBox(
                        espRenderData.dx - espRenderData.halfWidth,
                        espRenderData.dy,
                        espRenderData.dz - espRenderData.halfWidth,
                        espRenderData.dx + espRenderData.halfWidth,
                        espRenderData.dy + espRenderData.height,
                        espRenderData.dz + espRenderData.halfWidth,
                        espRenderData.fill
                     );
                  }
               }

               shapeBatch.flush();
               if (flag) {
                  ShapeBatch shapeBatch1 = RenderUtils.beginShapeBatch(matrices);

                  for (EspRenderData espRenderData1 : (Iterable<EspRenderData>)arrayList) {
                     if (espRenderData1.tracer != null) {
                        Vec3d vec3d5 = new Vec3d(espRenderData1.dx, espRenderData1.tracerTargetY, espRenderData1.dz);
                        shapeBatch1.renderLine(espRenderData1.tracer, vec3d3, vec3d5, Water.getTracerWidth());
                     }
                  }

                  shapeBatch1.flush();
               }

               matrices.pop();
            }
         }
      }
   }

   public int clampAlpha(double alpha) {
      int i = (int)Math.round(alpha);
      if (i < 0) {
         return 0;
      } else {
         return i > 255 ? 255 : i;
      }
   }

   public Color scaleAlpha(Color color, int factor) {
      int i = Math.max(0, Math.min(255, Math.round(color.getAlpha() / 255.0F * factor)));
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), i);
   }

   public Vec3d getLerpedPos(PlayerEntity player, float var2) {
      try {
         return player.getLerpedPos(var2);
      } catch (Throwable throwable) {
         double d0 = MathHelper.lerp((double)var2, player.lastRenderX, player.getX());
         double d1 = MathHelper.lerp((double)var2, player.lastRenderY, player.getY());
         double d2 = MathHelper.lerp((double)var2, player.lastRenderZ, player.getZ());
         return new Vec3d(d0, d1, d2);
      }
   }

   public String getProfileName(PlayerEntity player) {
      if (player == null) {
         return "";
      } else {
         try {
            GameProfile gameProfile = player.getGameProfile();
            if (gameProfile != null) {
               try {
                  if (gameProfile.getClass().getMethod("getName").invoke(gameProfile) instanceof String s && !s.isBlank()) {
                     return s;
                  }
               } catch (Throwable throwable1) {
               }

               try {
                  if (gameProfile.getClass().getMethod("name").invoke(gameProfile) instanceof String s1 && !s1.isBlank()) {
                     return s1;
                  }
               } catch (Throwable throwable) {
               }
            }
         } catch (Throwable throwable2) {
         }

         return player.getName().getString();
      }
   }
}
