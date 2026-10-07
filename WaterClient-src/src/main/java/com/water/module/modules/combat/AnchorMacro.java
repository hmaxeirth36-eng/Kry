package com.water.module.modules.combat;

import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.setting.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class AnchorMacro extends ActivatableModule {
   public final Setting<Float> switchDelay = new Setting<>("Switch Delay", 0.0F, 0.0F, 20.0F);
   public final Setting<Float> glowstoneDelay = new Setting<>("Glowstone Delay", 0.0F, 0.0F, 20.0F);
   public final Setting<Float> explodeDelay = new Setting<>("Explode Delay", 0.0F, 0.0F, 20.0F);
   public final Setting<Float> totemSlot = new Setting<>("Totem Slot", 1.0F, 1.0F, 9.0F);
   public final Setting<Boolean> switchBack = new Setting<>("Switch Back", false);
   public int switchCounter;
   public int glowstoneDelayCounter;
   public int explodeDelayCounter;
   public boolean waitingForPop = false;

   public AnchorMacro() {
      super("Anchor Macro", Category.COMBAT);
      this.addSetting(this.switchDelay);
      this.addSetting(this.glowstoneDelay);
      this.addSetting(this.explodeDelay);
      this.addSetting(this.totemSlot);
      this.addSetting(this.switchBack);
   }

   @Override
   public void onEnable() {
      this.resetCounters();
      this.waitingForPop = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.resetCounters();
      this.waitingForPop = false;
      super.onDisable();
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
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.currentScreen == null) {
            if (!this.hasFoodInHands()) {
               if (!this.isRightMouseDown()) {
                  this.resetCounters();
               } else {
                  this.tickMacro();
               }
            }
         }
      }
   }

   public boolean hasFoodInHands() {
      boolean flag = mc.player.getMainHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD)
         || mc.player.getOffHandStack().getItem().getComponents().contains(DataComponentTypes.FOOD);
      boolean flag1 = mc.player.getMainHandStack().getItem() instanceof ShieldItem || mc.player.getOffHandStack().getItem() instanceof ShieldItem;
      boolean flag2 = this.isRightMouseDown();
      return (flag || flag1) && flag2;
   }

   public boolean isRightMouseDown() {
      return mc.getWindow() != null && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 1) == 1;
   }

   public void tickMacro() {
      if (mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
         if (blockHitResult.getType() == Type.BLOCK) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (blockState.isOf(Blocks.RESPAWN_ANCHOR)) {
               mc.options.useKey.setPressed(false);
               int i = blockState.get(RespawnAnchorBlock.CHARGES);
               if (i == 0) {
                  this.placeGlowstone(blockHitResult);
               } else {
                  this.swapToTotemSlot(blockHitResult);
               }
            }
         }
      }
   }

   public void placeGlowstone(BlockHitResult hit) {
      if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
         if (this.switchCounter < this.switchDelay.getValue().intValue()) {
            this.switchCounter++;
            return;
         }

         this.switchCounter = 0;
         if (!this.selectHotbarSlot(Items.GLOWSTONE)) {
            return;
         }
      }

      if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
         if (this.glowstoneDelayCounter < this.glowstoneDelay.getValue().intValue()) {
            this.glowstoneDelayCounter++;
            return;
         }

         this.glowstoneDelayCounter = 0;
         this.interactBlock(hit);
      }
   }

   public void swapToTotemSlot(BlockHitResult hit) {
      int i = Math.max(0, Math.min(8, this.totemSlot.getValue().intValue() - 1));
      if (mc.player.getInventory().getSelectedSlot() != i) {
         if (this.switchCounter < this.switchDelay.getValue().intValue()) {
            this.switchCounter++;
            return;
         }

         this.switchCounter = 0;
         mc.player.getInventory().setSelectedSlot(i);
      }

      if (mc.player.getInventory().getSelectedSlot() == i) {
         if (this.explodeDelayCounter < this.explodeDelay.getValue().intValue()) {
            this.explodeDelayCounter++;
            return;
         }

         this.explodeDelayCounter = 0;
         this.interactBlock(hit);
         if (this.switchBack.getValue()) {
            this.waitingForPop = true;
         }
      }
   }

   public int findHotbarSlot(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }

   public boolean selectHotbarSlot(Item item) {
      int i = this.findHotbarSlot(item);
      if (i != -1) {
         mc.player.getInventory().setSelectedSlot(i);
         return true;
      } else {
         return false;
      }
   }

   public void interactBlock(BlockHitResult hit) {
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   public void resetCounters() {
      this.switchCounter = 0;
      this.glowstoneDelayCounter = 0;
      this.explodeDelayCounter = 0;
   }
}
