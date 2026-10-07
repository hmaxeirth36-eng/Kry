package com.water.module.modules.misc;

import com.water.gui.NotificationManager;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Water;
import com.water.module.setting.ModeSetting;
import com.water.module.setting.Setting;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public final class FreeLook extends Module {
   public static final float MIN_DISTANCE = 1.0F;
   public static final float MAX_DISTANCE = 15.0F;
   public static final float MIN_SENSITIVITY = 0.1F;
   public static final float MAX_SENSITIVITY = 3.0F;
   public static FreeLook instance;
   public final ModeSetting activationMode = new ModeSetting("Mode", "Hold", new String[]{"Activation Mode"}, "Hold", "Toggle");
   public final Setting<Float> distance = new Setting<>("Distance", 4.0F, 1.0F, 15.0F);
   public final Setting<Boolean> wallClip = new Setting<>("Wall Clip", true);
   public final Setting<Float> sensitivity = new Setting<>("Sensitivity", 1.0F, 0.1F, 3.0F);
   public final Setting<Boolean> invertY = new Setting<>("Invert Y", false);
   public boolean active;
   public Perspective savedPerspective;
   public boolean savedChunkCullingEnabled = true;
   public float cameraYaw;
   public float cameraPitch;

   public FreeLook() {
      super("FreeLook", Category.MISC);
      instance = this;
      this.addSetting(this.activationMode);
      this.addSetting(this.distance);
      this.addSetting(this.wallClip);
      this.addSetting(this.sensitivity);
      this.addSetting(this.invertY);
   }

   @Override
   public void onEnable() {
      this.active = false;
      this.savedPerspective = null;
      this.savedChunkCullingEnabled = true;
   }

   @Override
   public void onDisable() {
      this.exitFreeLook();
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.options != null && mc.getWindow() != null) {
         if (this.isHoldMode()) {
            int i = this.getBind();
            boolean flag = i != 0 && GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
            if (flag) {
               this.enterFreeLook();
            } else {
               this.exitFreeLook();
            }
         } else if (this.active) {
            this.maintainPerspective();
         }
      } else {
         this.exitFreeLook();
      }
   }

   @Override
   public void onBindPressed() {
      if (!this.isHoldMode()) {
         if (!this.isEnabled()) {
            this.toggle();
         } else {
            if (this.active) {
               this.exitFreeLook();
            } else {
               this.enterFreeLook();
            }
         }
      }
   }

   public boolean isCameraActive() {
      return this.isEnabled() && this.active && mc.player != null;
   }

   public void consumeMouseDelta(double var1, double var3) {
      if (this.isCameraActive()) {
         double d0 = this.invertY.getValue() ? -1.0 : 1.0;
         double d1 = 0.15 * this.getSensitivity();
         this.cameraYaw = MathHelper.wrapDegrees(this.cameraYaw + (float)(var1 * d1));
         this.cameraPitch = MathHelper.clamp(this.cameraPitch + (float)(var3 * d1 * d0), -90.0F, 90.0F);
      }
   }

   public float getCameraYaw() {
      return this.cameraYaw;
   }

   public float getCameraPitch() {
      return this.cameraPitch;
   }

   public float getDistance() {
      return MathHelper.clamp(this.distance.getValue(), 1.0F, 15.0F);
   }

   public boolean shouldWallClip() {
      return this.wallClip.getValue();
   }

   public float getSensitivity() {
      return MathHelper.clamp(this.sensitivity.getValue(), 0.1F, 3.0F);
   }

   public boolean isHoldMode() {
      return this.activationMode.is("Hold");
   }

   public void enterFreeLook() {
      if (!this.active && mc.player != null && mc.options != null) {
         this.savedPerspective = mc.options.getPerspective();
         this.savedChunkCullingEnabled = mc.chunkCullingEnabled;
         mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         mc.chunkCullingEnabled = false;
         this.cameraYaw = mc.player.getYaw();
         this.cameraPitch = mc.player.getPitch();
         this.active = true;
         this.notifyToggle(true);
      } else {
         this.maintainPerspective();
      }
   }

   public void maintainPerspective() {
      if (this.active && mc.options != null) {
         if (mc.options.getPerspective().isFirstPerson() || mc.options.getPerspective().isFrontView()) {
            mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            mc.chunkCullingEnabled = false;
         }
      }
   }

   public void exitFreeLook() {
      if (this.active) {
         this.active = false;
         if (mc.options != null) {
            mc.options.setPerspective(this.savedPerspective == null ? Perspective.FIRST_PERSON : this.savedPerspective);
         }

         mc.chunkCullingEnabled = this.savedChunkCullingEnabled;
         this.notifyToggle(false);
      }
   }

   public void notifyToggle(boolean var1) {
      try {
         if (Water.areNotificationsEnabled()) {
            NotificationManager.INSTANCE.pushToggle(this.getName(), var1, this.getModuleIcon());
         }
      } catch (Exception exception) {
      }
   }

   public static String watermarkFragment() {
      return "2";
   }
}
