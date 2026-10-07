package com.water.mixin;

import com.water.module.modules.misc.SwingSpeed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class})
public class LivingEntityMixin {
   public LivingEntityMixin() {
   }

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
      SwingSpeed swingSpeed = SwingSpeed.instance;
      if (swingSpeed != null && swingSpeed.isEnabled()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient.player != null && (Object)this == minecraftClient.player) {
            LivingEntity livingEntity = (LivingEntity)(Object)this;
            ItemStack itemStack = livingEntity.getStackInHand(Hand.MAIN_HAND);
            int i = itemStack.getSwingAnimation().duration();
            if (StatusEffectUtil.hasHaste(livingEntity)) {
               i -= 1 + StatusEffectUtil.getHasteAmplifier(livingEntity);
            } else if (livingEntity.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
               i += (1 + livingEntity.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2;
            }

            float f = swingSpeed.getSwingSpeed();
            int j = Math.max(1, Math.round(i / f));
            cir.setReturnValue(j);
         }
      }
   }
}
