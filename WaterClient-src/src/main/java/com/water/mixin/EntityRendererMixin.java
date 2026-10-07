package com.water.mixin;

import com.water.module.modules.combat.Hitbox;
import com.water.module.modules.misc.NameTags;
import com.water.util.LabelRenderOverride;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public class EntityRendererMixin {
   public EntityRendererMixin() {
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")}
   )
   private void water$updateNametagState(Entity entity, EntityRenderState entityRenderState, float var3, CallbackInfo ci) {
      if (entity instanceof LivingEntity livingEntity && NameTags.isActive()) {
         NameTags nameTags = NameTags.instance;
         if (nameTags != null && nameTags.shouldRenderForState(livingEntity, entityRenderState.squaredDistanceToCamera) && !entityRenderState.invisible) {
            LabelRenderOverride.markOverridden(entityRenderState);
         } else {
            LabelRenderOverride.clearOverride(entityRenderState);
         }
      } else {
         LabelRenderOverride.clearOverride(entityRenderState);
      }
   }

   @Inject(
      method = {"renderLabelIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$renderCustomNametag(EntityRenderState entityRenderState, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState, CallbackInfo ci) {
      if (LabelRenderOverride.isOverridden(entityRenderState)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"getShadowRadius"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void water$hitbox(EntityRenderState entityRenderState, CallbackInfoReturnable<Float> cir) {
      Hitbox hitbox = Hitbox.INSTANCE;
      if (hitbox != null && hitbox.isEnabled()) {
         cir.setReturnValue((Float)cir.getReturnValue() + hitbox.size.getValue());
      }
   }
}
