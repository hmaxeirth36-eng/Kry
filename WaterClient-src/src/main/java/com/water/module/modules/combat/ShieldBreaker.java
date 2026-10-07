package com.water.module.modules.combat;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

public final class ShieldBreaker extends Module {
   public final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   public final Setting<Float> switchDelayMs = new Setting<>("Switch Delay", 0.0F, 0.0F, 500.0F);
   public boolean isBlockingState = false;
   public long firstDetectedTime = -1L;
   public boolean hasAttacked = false;
   public boolean needsSwitchBack = false;
   public int previousSlot = -1;

   public ShieldBreaker() {
      super("Shield Breaker", Category.COMBAT);
      this.addSetting(this.switchBack);
      this.addSetting(this.switchDelayMs);
   }

   @Override
   public void onEnable() {
      this.resetState();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      if (this.hasAttacked && this.switchBack.getValue() && this.needsSwitchBack && mc.player != null) {
         this.selectSlot(this.previousSlot);
      }

      this.resetState();
      super.onDisable();
   }

   public void resetState() {
      this.isBlockingState = false;
      this.firstDetectedTime = -1L;
      this.hasAttacked = false;
      this.needsSwitchBack = false;
      this.previousSlot = -1;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         PlayerEntity playerEntity = this.getTargetPlayer();
         if (playerEntity != null && playerEntity.isBlocking()) {
            if (!this.isBlockingState) {
               this.isBlockingState = true;
               this.firstDetectedTime = System.currentTimeMillis();
            }

            long i = this.switchDelayMs.getValue().longValue();
            if (!this.hasAttacked && this.firstDetectedTime >= 0L && System.currentTimeMillis() - this.firstDetectedTime >= i) {
               int j = this.findBestAxeSlot();
               if (j != -1) {
                  this.previousSlot = mc.player.getInventory().getSelectedSlot();
                  if (this.previousSlot != j) {
                     this.selectSlot(j);
                     this.needsSwitchBack = true;
                  }

                  mc.interactionManager.attackEntity(mc.player, playerEntity);
                  mc.player.swingHand(Hand.MAIN_HAND);
                  this.hasAttacked = true;
               }
            }
         } else {
            if (this.isBlockingState) {
               this.isBlockingState = false;
               this.firstDetectedTime = -1L;
            }

            if (this.hasAttacked) {
               if (this.switchBack.getValue() && this.needsSwitchBack && this.previousSlot != -1) {
                  this.selectSlot(this.previousSlot);
               }

               this.hasAttacked = false;
               this.needsSwitchBack = false;
               this.previousSlot = -1;
            }
         }
      }
   }

   public PlayerEntity getTargetPlayer() {
      return mc.crosshairTarget != null
            && mc.crosshairTarget.getType() == Type.ENTITY
            && ((EntityHitResult)mc.crosshairTarget).getEntity() instanceof PlayerEntity playerEntity
            && playerEntity != mc.player
         ? playerEntity
         : null;
   }

   public int findBestAxeSlot() {
      int i = -1;
      int j = -1;

      for (int k = 0; k < 9; k++) {
         ItemStack itemStack = mc.player.getInventory().getStack(k);
         if (itemStack.getItem() instanceof AxeItem) {
            int l = this.getAxeTier(itemStack);
            if (l > i) {
               i = l;
               j = k;
            }
         }
      }

      return j;
   }

   public int getAxeTier(ItemStack stack) {
      if (stack.isOf(Items.NETHERITE_AXE)) {
         return 6;
      } else if (stack.isOf(Items.DIAMOND_AXE)) {
         return 5;
      } else if (stack.isOf(Items.IRON_AXE)) {
         return 4;
      } else if (stack.isOf(Items.GOLDEN_AXE)) {
         return 3;
      } else if (stack.isOf(Items.STONE_AXE)) {
         return 2;
      } else {
         return stack.isOf(Items.WOODEN_AXE) ? 1 : 0;
      }
   }

   public void selectSlot(int slot) {
      if (mc.player != null) {
         if (slot >= 0 && slot <= 8) {
            mc.player.getInventory().setSelectedSlot(slot);
         }
      }
   }
}
