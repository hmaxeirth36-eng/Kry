package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

public class Freecam extends Module {
   public static final float MIN_SCROLL_SPEED = 0.1F;
   public static final float MAX_SCROLL_SPEED = 10.0F;
   public static final float SCROLL_SPEED_STEP = 0.2F;
   public final Vector3d currentPosition = new Vector3d();
   public final Vector3d previousPosition = new Vector3d();
   public final Vector3d velocity = new Vector3d();
   public float yaw;
   public float pitch;
   public float previousYaw;
   public float previousPitch;
   public final Setting<Float> speed = new Setting<>("Speed", 1.0F, 0.1F, 10.0F);
   public final Setting<Boolean> smoothing = new Setting<>("Smooth", Boolean.TRUE);
   public final Setting<Boolean> keepSneak = new Setting<>("Keep Sneak", Boolean.FALSE);
   public final Setting<Boolean> tracerStickToEye = new Setting<>("Stick to Eye", Boolean.FALSE);
   public float lookSensitivity = 0.5F;
   public float currentSpeed;
   public Perspective savedPerspective;
   public boolean savedChunkCullingEnabled;
   public long lastFrameTime;
   public float savedPlayerYaw;
   public float savedPlayerPitch;
   public boolean wasSneaking;
   public static Freecam instance;

   public Freecam() {
      super("Freecam", Category.RENDER);
      instance = this;
      this.addSetting(this.speed);
      this.addSetting(this.smoothing);
      this.addSetting(this.keepSneak);
      this.addSetting(this.tracerStickToEye);
   }

   @Override
   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         this.savedPerspective = mc.options.getPerspective();
         this.savedChunkCullingEnabled = mc.chunkCullingEnabled;
         mc.chunkCullingEnabled = false;
         this.savedPlayerYaw = mc.player.getYaw();
         this.savedPlayerPitch = mc.player.getPitch();
         this.wasSneaking = mc.player.isSneaking();
         this.yaw = mc.player.getYaw();
         this.pitch = mc.player.getPitch();
         Vec3d vec3d = mc.player.getCameraPosVec(1.0F);
         this.currentPosition.set(vec3d.x, vec3d.y, vec3d.z);
         this.previousPosition.set(vec3d.x, vec3d.y, vec3d.z);
         this.previousYaw = this.yaw;
         this.previousPitch = this.pitch;
         this.lastFrameTime = System.currentTimeMillis();
         this.velocity.set(0.0, 0.0, 0.0);
         this.currentSpeed = this.getSpeed();
         mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
      } else {
         this.toggle();
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setYaw(this.savedPlayerYaw);
         mc.player.setPitch(this.savedPlayerPitch);
         mc.player.setHeadYaw(this.savedPlayerYaw);
         mc.player.setBodyYaw(this.savedPlayerYaw);
         mc.player.setSneaking(this.wasSneaking);
      }

      if (this.savedPerspective != null) {
         mc.options.setPerspective(this.savedPerspective);
      } else {
         mc.options.setPerspective(Perspective.FIRST_PERSON);
      }

      mc.chunkCullingEnabled = this.savedChunkCullingEnabled;
      this.velocity.set(0.0, 0.0, 0.0);
      this.currentSpeed = this.getSpeed();
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         mc.player.setYaw(this.savedPlayerYaw);
         mc.player.setPitch(this.savedPlayerPitch);
         mc.player.setHeadYaw(this.savedPlayerYaw);
         mc.player.setBodyYaw(this.savedPlayerYaw);
         if (this.keepSneak.getValue() && this.wasSneaking) {
            mc.player.setSneaking(true);
         }
      }
   }

   public void updateCameraMovement() {
      if (mc.player != null) {
         this.previousPosition.set(this.currentPosition);
         this.previousYaw = this.yaw;
         this.previousPitch = this.pitch;
         long i = System.currentTimeMillis();
         float f = (float)(i - this.lastFrameTime) / 1000.0F;
         this.lastFrameTime = i;
         f = Math.min(f, 0.1F);
         if (f < 0.001F) {
            f = 0.016F;
         }

         float f1 = (float)Math.toRadians(this.yaw);
         double d0 = -Math.sin(f1);
         double d1 = Math.cos(f1);
         double d2 = -Math.cos(f1);
         double d3 = -Math.sin(f1);
         double d4 = 0.0;
         double d5 = 0.0;
         double d6 = 0.0;
         double d7 = this.currentSpeed * 2.0;
         if (mc.options != null && mc.options.sprintKey.isPressed()) {
            d7 *= 2.0;
         }

         if (mc.options.forwardKey.isPressed()) {
            d4 += d0 * d7;
            d6 += d1 * d7;
         }

         if (mc.options.backKey.isPressed()) {
            d4 -= d0 * d7;
            d6 -= d1 * d7;
         }

         if (mc.options.rightKey.isPressed()) {
            d4 += d2 * d7;
            d6 += d3 * d7;
         }

         if (mc.options.leftKey.isPressed()) {
            d4 -= d2 * d7;
            d6 -= d3 * d7;
         }

         if (mc.options.jumpKey.isPressed()) {
            d5 += d7;
         }

         if (mc.options.sneakKey.isPressed()) {
            d5 -= d7;
         }

         double d8 = 5.0;
         if (this.smoothing.getValue()) {
            double d9 = 1.0 - Math.pow(0.001, f);
            this.velocity.x = MathHelper.lerp(d9, this.velocity.x, d4 * d8);
            this.velocity.y = MathHelper.lerp(d9, this.velocity.y, d5 * d8);
            this.velocity.z = MathHelper.lerp(d9, this.velocity.z, d6 * d8);
         } else {
            this.velocity.set(d4 * d8, d5 * d8, d6 * d8);
         }

         this.currentPosition.x = this.currentPosition.x + this.velocity.x * f;
         this.currentPosition.y = this.currentPosition.y + this.velocity.y * f;
         this.currentPosition.z = this.currentPosition.z + this.velocity.z * f;
      }
   }

   public void onScrollWheel(double amount) {
      float f = this.currentSpeed;
      float f1 = 0.5F;
      float f2 = f + (float)amount * f1;
      this.currentSpeed = MathHelper.clamp(f2, 0.1F, 10.0F);
   }

   public void updateRotation(double deltaX, double deltaY) {
      this.yaw += (float)deltaX;
      this.pitch += (float)deltaY;
      this.yaw = MathHelper.wrapDegrees(this.yaw);
      this.pitch = MathHelper.clamp(this.pitch, -90.0F, 90.0F);
   }

   public double getInterpolatedX(float tickDelta) {
      return MathHelper.lerp((double)tickDelta, this.previousPosition.x, this.currentPosition.x);
   }

   public double getInterpolatedY(float tickDelta) {
      return MathHelper.lerp((double)tickDelta, this.previousPosition.y, this.currentPosition.y);
   }

   public double getInterpolatedZ(float tickDelta) {
      return MathHelper.lerp((double)tickDelta, this.previousPosition.z, this.currentPosition.z);
   }

   public float getInterpolatedYaw(float tickDelta) {
      return MathHelper.lerp(tickDelta, this.previousYaw, this.yaw);
   }

   public float getInterpolatedPitch(float tickDelta) {
      return MathHelper.lerp(tickDelta, this.previousPitch, this.pitch);
   }

   public float getLookSensitivity() {
      return this.lookSensitivity;
   }

   public void adjustSpeed(double amount) {
      if (amount != 0.0) {
         float f = this.currentSpeed + (float)Math.signum(amount) * 0.2F;
         this.currentSpeed = MathHelper.clamp(f, 0.1F, 10.0F);
      }
   }

   public float getSpeed() {
      return MathHelper.clamp(this.speed.getValue(), 0.1F, 10.0F);
   }

   public boolean shouldTracersStickToEye() {
      return this.tracerStickToEye.getValue();
   }

   public static Vec3d getTracerOrigin(Vec3d vec, float tickDelta) {
      return instance != null && instance.isEnabled() && instance.shouldTracersStickToEye() && mc.player != null ? mc.player.getCameraPosVec(tickDelta) : vec;
   }
}
