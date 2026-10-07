package com.water.mixin;

import com.water.module.modules.misc.FreeLook;
import com.water.module.modules.render.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class CameraMixin {
   public CameraMixin() {
   }

   @Shadow
   protected abstract void setPos(double var1, double var3, double var5);

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract float clipToSpace(float var1);

   @Shadow
   protected abstract void moveBy(float var1, float var2, float var3);

   @Inject(
      method = {"update"},
      at = {@At("TAIL")}
   )
   private void onUpdate(World world, Entity entity, boolean var3, boolean var4, float var5, CallbackInfo ci) {
      if (Freecam.instance != null && Freecam.instance.isEnabled()) {
         Freecam.instance.updateCameraMovement();
         double d1 = Freecam.instance.getInterpolatedX(var5);
         double d2 = Freecam.instance.getInterpolatedY(var5);
         double d0 = Freecam.instance.getInterpolatedZ(var5);
         float f1 = Freecam.instance.getInterpolatedYaw(var5);
         float f2 = Freecam.instance.getInterpolatedPitch(var5);
         this.setPos(d1, d2, d0);
         this.setRotation(f1, f2);
      } else {
         FreeLook freeLook = FreeLook.instance;
         if (freeLook != null && freeLook.isCameraActive() && entity != null) {
            Vec3d vec3d = entity.getCameraPosVec(var5);
            this.setRotation(freeLook.getCameraYaw(), freeLook.getCameraPitch());
            this.setPos(vec3d.x, vec3d.y, vec3d.z);
            float f = freeLook.getDistance();
            if (!freeLook.shouldWallClip()) {
               f = this.clipToSpace(f);
            }

            this.moveBy(-f, 0.0F, 0.0F);
         }
      }
   }

   @Inject(
      method = {"isThirdPerson"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onIsThirdPerson(CallbackInfoReturnable<Boolean> cir) {
      if (Freecam.instance != null && Freecam.instance.isEnabled()) {
         cir.setReturnValue(true);
      } else {
         if (FreeLook.instance != null && FreeLook.instance.isCameraActive()) {
            cir.setReturnValue(true);
         }
      }
   }
}
