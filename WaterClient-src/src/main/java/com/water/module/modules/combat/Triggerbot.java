package com.water.module.modules.combat;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Friends;
import com.water.module.setting.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

public final class Triggerbot extends Module {
   public static final int FIXED_DELAY_TICKS = 9;
   public final Setting<Boolean> onlyCrit = new Setting<>("Only Crit", false);
   public final Setting<Boolean> checkShield = new Setting<>("Check Shield", false);
   public int delayCounter = 0;

   public Triggerbot() {
      super("Triggerbot", Category.COMBAT);
      this.addSetting(this.onlyCrit);
      this.addSetting(this.checkShield);
   }

   @Override
   public void onEnable() {
      this.delayCounter = 0;
   }

   @Override
   public void onDisable() {
      this.delayCounter = 0;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         if (mc.currentScreen == null) {
            if (this.delayCounter > 0) {
               this.delayCounter--;
            } else if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.ENTITY) {
               if (mc.crosshairTarget instanceof EntityHitResult entityHitResult) {
                  Entity entity = entityHitResult.getEntity();
                  if (entity instanceof LivingEntity) {
                     if (entity != mc.player) {
                        if (!(entity instanceof PlayerEntity playerEntity && Friends.isAntiTriggerbotEnabled() && Friends.isFriend(playerEntity.getName().getString()))) {
                           if (!mc.options.attackKey.isPressed()) {
                              if (this.shouldAttack((LivingEntity)entity)) {
                                 this.attack(entity);
                                 this.delayCounter = 9;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean shouldAttack(LivingEntity entity) {
      return this.onlyCrit.getValue() && !this.isCritReady(mc.player) ? false : !this.checkShield.getValue() || !this.isHoldingShield(entity);
   }

   public boolean isHoldingShield(LivingEntity entity) {
      ItemStack itemStack = entity.getMainHandStack();
      ItemStack itemStack1 = entity.getOffHandStack();
      return itemStack.getItem() == Items.SHIELD || itemStack1.getItem() == Items.SHIELD;
   }

   public boolean isCritReady(PlayerEntity player) {
      if (player.fallDistance <= 0.05F) {
         return false;
      } else if (player.isOnGround()) {
         return false;
      } else {
         return !player.isTouchingWater() && !player.isInLava() && !player.isClimbing() && !player.hasVehicle() ? !player.isSprinting() : false;
      }
   }

   public void attack(Entity entity) {
      mc.interactionManager.attackEntity(mc.player, entity);
      mc.player.swingHand(Hand.MAIN_HAND);
   }
}
