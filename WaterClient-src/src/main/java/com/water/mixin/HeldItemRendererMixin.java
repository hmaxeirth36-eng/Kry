package com.water.mixin;

import com.water.module.modules.misc.FreeLook;
import net.minecraft.client.render.item.HeldItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({HeldItemRenderer.class})
public class HeldItemRendererMixin {
   public HeldItemRendererMixin() {
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderFirstPersonItem(CallbackInfo ci) {
      if (FreeLook.instance != null && FreeLook.instance.isCameraActive()) {
         ci.cancel();
      }
   }
}
