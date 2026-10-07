package com.water.mixin;

import com.water.module.modules.misc.SkinChanger;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayerEntity.class})
public abstract class AbstractClientPlayerEntityMixin {
   public AbstractClientPlayerEntityMixin() {
   }

   @Inject(
      method = {"getSkin"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$overrideOwnSkin(CallbackInfoReturnable<SkinTextures> cir) {
      AbstractClientPlayerEntity abstractClientPlayerEntity = (AbstractClientPlayerEntity)(Object)this;
      SkinTextures skinTextures = SkinChanger.getOverrideSkinFor(abstractClientPlayerEntity.getUuid());
      if (skinTextures != null) {
         cir.setReturnValue(skinTextures);
      }
   }
}
