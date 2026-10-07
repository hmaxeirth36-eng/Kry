package com.water.module.modules.combat;

import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.setting.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public final class DoubleAnchor extends ActivatableModule {
   public final Setting<Float> switchDelay = new Setting<>("Delay", 0.0F, 0.0F, 20.0F);
   public final Setting<Float> totemSlot = new Setting<>("Totem Slot", 1.0F, 1.0F, 9.0F);
   public final Setting<Boolean> switchBack = new Setting<>("Switch Back", false);
   public int delayCounter = 0;
   public int step = 0;
   public boolean isAnchoring = false;
   public BlockPos lastAnchorPos = null;
   public boolean waitingForPop = false;

   public DoubleAnchor() {
      super("Double Anchor", Category.COMBAT);
      this.addSetting(this.switchDelay);
      this.addSetting(this.totemSlot);
      this.addSetting(this.switchBack);
   }

   @Override
   public void onEnable() {
      this.resetCounters();
      this.isAnchoring = false;
      this.waitingForPop = false;
      this.lastAnchorPos = null;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.resetCounters();
      this.isAnchoring = false;
      this.waitingForPop = false;
      this.lastAnchorPos = null;
      super.onDisable();
   }

   @Override
   public void onBindPressed() {
      super.onBindPressed();
   }

   @Override
   public void onActivationKeyPressed() {
      if (this.isEnabled()) {
         this.resetCounters();
         this.isAnchoring = true;
         this.waitingForPop = false;
         this.lastAnchorPos = null;
      }
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (this.switchBack.getValue() && this.waitingForPop && packet instanceof HealthUpdateS2CPacket) {
         this.waitingForPop = false;
         if (mc.player != null) {
            int i = this.findHotbarSlot(Items.RESPAWN_ANCHOR);
            if (i != -1) {
               mc.player.getInventory().setSelectedSlot(i);
            }
         }
      }
   }

   @Override
   public void onTick() {
      if (this.isAnchoring) {
         if (mc.currentScreen == null) {
            if (mc.player != null && mc.world != null) {
               if (!this.hasAnchorAndGlowstone()) {
                  this.isAnchoring = false;
                  this.resetCounters();
               } else if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
                  if (mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.AIR)) {
                     this.isAnchoring = false;
                     this.resetCounters();
                  } else {
                     int j = Math.max(0, this.switchDelay.getValue().intValue());
                     if (this.delayCounter < j) {
                        this.delayCounter++;
                     } else {
                        if (this.step == 0) {
                           this.selectHotbarSlot(Items.RESPAWN_ANCHOR);
                        } else if (this.step == 1) {
                           this.interactBlock(blockHitResult);
                        } else if (this.step == 2) {
                           this.selectHotbarSlot(Items.GLOWSTONE);
                        } else if (this.step == 3) {
                           this.interactBlock(blockHitResult);
                        } else if (this.step == 4) {
                           this.selectHotbarSlot(Items.RESPAWN_ANCHOR);
                        } else if (this.step == 5) {
                           this.interactBlock(blockHitResult);
                           this.interactBlock(blockHitResult);
                        } else if (this.step == 6) {
                           this.selectHotbarSlot(Items.GLOWSTONE);
                        } else if (this.step == 7) {
                           this.interactBlock(blockHitResult);
                        } else if (this.step == 8) {
                           int i = this.totemSlot.getValue().intValue() - 1;
                           this.selectSlot(i);
                           this.lastAnchorPos = blockHitResult.getBlockPos();
                        } else if (this.step == 9) {
                           this.interactBlock(blockHitResult);
                           if (this.switchBack.getValue()) {
                              this.waitingForPop = true;
                           }
                        } else if (this.step == 10) {
                           this.isAnchoring = false;
                           this.resetCounters();
                           return;
                        }

                        this.step++;
                     }
                  }
               } else {
                  this.isAnchoring = false;
                  this.resetCounters();
               }
            }
         }
      }
   }

   public void resetCounters() {
      this.delayCounter = 0;
      this.step = 0;
   }

   public boolean hasAnchorAndGlowstone() {
      boolean flag = false;
      boolean flag1 = false;

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.player.getInventory().getStack(i);
         if (itemStack.isOf(Items.RESPAWN_ANCHOR)) {
            flag = true;
         }

         if (itemStack.isOf(Items.GLOWSTONE)) {
            flag1 = true;
         }
      }

      return flag && flag1;
   }

   public int findHotbarSlot(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }

   public void selectHotbarSlot(Item item) {
      int i = this.findHotbarSlot(item);
      if (i != -1) {
         mc.player.getInventory().setSelectedSlot(i);
      }
   }

   public void selectSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         mc.player.getInventory().setSelectedSlot(slot);
      }
   }

   public void interactBlock(BlockHitResult hit) {
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      mc.player.swingHand(Hand.MAIN_HAND);
   }
}
