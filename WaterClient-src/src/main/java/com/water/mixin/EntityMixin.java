package com.water.mixin;

import com.water.module.modules.combat.Hitbox;
import com.water.module.modules.donut.FakeRoles;
import com.water.module.modules.misc.FreeLook;
import com.water.module.modules.misc.NameProtect;
import com.water.module.modules.render.Freecam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public class EntityMixin {
   public EntityMixin() {
   }

   // RECOVERY NOTE: the Hitbox module was an empty stub in the original jar — it registered a
   // "Expand" setting but had no implementation and nothing ever read it, so toggling it did
   // nothing. This injection is NEW code, not recovered, implementing the obvious intent:
   // expand the targeting margin of other players by (Expand - 1.0). At the default 1.0 it is
   // a no-op, so behaviour is unchanged unless the user raises the setting.
   @Inject(method = {"getTargetingMargin"}, at = {@At("RETURN")}, cancellable = true)
   private void waterExpandHitbox(CallbackInfoReturnable<Float> cir) {
      Hitbox hitbox = Hitbox.INSTANCE;
      if (hitbox != null && hitbox.isEnabled()) {
         Object self = this;
         if (self instanceof PlayerEntity && self != MinecraftClient.getInstance().player) {
            float expand = hitbox.size.getValue() - 1.0F;
            if (expand > 0.0F) {
               cir.setReturnValue(cir.getReturnValue() + expand);
            }
         }
      }
   }

   @Inject(
      method = {"changeLookDirection"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onChangeLookDirection(double var1, double var3, CallbackInfo ci) {
      Entity entity = (Entity)(Object)this;
      if (entity == MinecraftClient.getInstance().player) {
         if (Freecam.instance != null && Freecam.instance.isEnabled()) {
            Freecam.instance.updateRotation(var1 * 0.15 * Freecam.instance.getLookSensitivity(), var3 * 0.15 * Freecam.instance.getLookSensitivity());
            ci.cancel();
         } else {
            if (FreeLook.instance != null && FreeLook.instance.isCameraActive()) {
               FreeLook.instance.consumeMouseDelta(var1, var3);
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"isSneaking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onIsSneaking(CallbackInfoReturnable<Boolean> cir) {
      if (Freecam.instance != null && Freecam.instance.isEnabled() && (Object)this == MinecraftClient.getInstance().player) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"shouldRender(D)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onShouldRender(double var1, CallbackInfoReturnable<Boolean> cir) {
      if (Freecam.instance != null && Freecam.instance.isEnabled() && var1 < 25600.0) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"getDisplayName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onGetDisplayName(CallbackInfoReturnable<Text> cir) {
      if (FakeRoles.isActive()) {
         Text text = FakeRoles.applyToText((Text)cir.getReturnValue());
         if (text != cir.getReturnValue()) {
            cir.setReturnValue(text);
            return;
         }
      }

      if (NameProtect.instance != null && NameProtect.instance.isEnabled() && MinecraftClient.getInstance().getSession() != null) {
         String s1 = MinecraftClient.getInstance().getSession().getUsername();
         if (s1 != null) {
            Text text1 = (Text)cir.getReturnValue();
            if (text1 != null) {
               String s = text1.getString();
               if (s.contains(s1)) {
                  cir.setReturnValue(Text.literal(s.replace(s1, NameProtect.instance.getFakeName())));
               }
            }
         }
      }
   }

   @Inject(
      method = {"getName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void onGetName(CallbackInfoReturnable<Text> cir) {
      if (FakeRoles.isActive()) {
         Text text = FakeRoles.applyToText((Text)cir.getReturnValue());
         if (text != cir.getReturnValue()) {
            cir.setReturnValue(text);
            return;
         }
      }

      if (NameProtect.instance != null && NameProtect.instance.isEnabled() && MinecraftClient.getInstance().getSession() != null) {
         String s1 = MinecraftClient.getInstance().getSession().getUsername();
         if (s1 != null) {
            Text text1 = (Text)cir.getReturnValue();
            if (text1 != null) {
               String s = text1.getString();
               if (s.contains(s1)) {
                  cir.setReturnValue(Text.literal(s.replace(s1, NameProtect.instance.getFakeName())));
               }
            }
         }
      }
   }
}
