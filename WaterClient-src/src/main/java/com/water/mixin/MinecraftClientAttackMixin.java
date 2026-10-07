package com.water.mixin;

import com.water.module.modules.combat.SpearSwap;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftClient.class})
public class MinecraftClientAttackMixin {
   public MinecraftClientAttackMixin() {
   }

   @Inject(
      method = {"handleInputEvents"},
      at = {@At("HEAD")},
      require = 0
   )
   private void water$preInput(CallbackInfo ci) {
      SpearSwap spearSwap = SpearSwap.INSTANCE;
      if (spearSwap != null && spearSwap.isEnabled()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null && minecraftClient.player != null && minecraftClient.options != null) {
            if (minecraftClient.currentScreen == null) {
               if (minecraftClient.options.attackKey.isPressed()) {
                  spearSwap.preAttack();
               } else {
                  spearSwap.noAttack();
               }
            }
         }
      }
   }
}
