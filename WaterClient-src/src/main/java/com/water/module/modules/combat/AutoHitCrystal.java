package com.water.module.modules.combat;

import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.setting.Setting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class AutoHitCrystal extends ActivatableModule {
   public final Setting<Integer> delay = new Setting<>("Delay (ticks)", 1, 0, 10);
   public int cooldown = 0;

   public AutoHitCrystal() {
      super("AutoHitCrystal", Category.COMBAT);
      this.addSetting(this.delay);
   }

   @Override
   public void onEnable() {
      this.cooldown = 0;
   }

   @Override
   public void onActivationKeyPressed() {
   }

   public boolean isActivationKeyDown() {
      int i = this.getActivationKey();
      if (i == 0) {
         return true;
      } else if (mc.getWindow() == null) {
         return false;
      } else {
         try {
            return GLFW.glfwGetKey(mc.getWindow().getHandle(), i) == 1;
         } catch (Exception exception) {
            return false;
         }
      }
   }

   @Override
   public void onTick() {
      if (this.cooldown > 0) {
         this.cooldown--;
      } else if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.currentScreen == null) {
            if (this.isActivationKeyDown()) {
               if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == Type.BLOCK) {
                  BlockHitResult blockHitResult = (BlockHitResult)mc.crosshairTarget;
                  BlockPos blockPos = blockHitResult.getBlockPos();
                  BlockPos blockPos1 = blockPos.up();
                  BlockPos blockPos2 = blockPos1.up();
                  EndCrystalEntity endCrystalEntity = this.findCrystalAt(blockPos2);
                  if (endCrystalEntity != null) {
                     mc.interactionManager.attackEntity(mc.player, endCrystalEntity);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.cooldown = this.delay.getValue();
                  } else if (!this.isValidBase(blockPos1)) {
                     int j = this.findHotbarSlot(Items.OBSIDIAN);
                     if (j >= 0) {
                        if (mc.player.getInventory().getSelectedSlot() != j) {
                           mc.player.getInventory().setSelectedSlot(j);
                        }

                        BlockHitResult blockHitResult2 = new BlockHitResult(Vec3d.ofCenter(blockPos).add(0.0, 0.5, 0.0), Direction.UP, blockPos, false);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult2);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.cooldown = this.delay.getValue();
                     }
                  } else if (this.isSpaceFree(blockPos2)) {
                     int i = this.findHotbarSlot(Items.END_CRYSTAL);
                     if (i >= 0) {
                        if (mc.player.getInventory().getSelectedSlot() != i) {
                           mc.player.getInventory().setSelectedSlot(i);
                        }

                        BlockHitResult blockHitResult1 = new BlockHitResult(Vec3d.ofCenter(blockPos1).add(0.0, 0.5, 0.0), Direction.UP, blockPos1, false);
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult1);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.cooldown = this.delay.getValue();
                     }
                  }
               }
            }
         }
      }
   }

   public boolean isValidBase(BlockPos pos) {
      BlockState blockState = mc.world.getBlockState(pos);
      return blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.BEDROCK);
   }

   public boolean isSpaceFree(BlockPos pos) {
      if (!mc.world.isAir(pos)) {
         return false;
      } else {
         Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
         return mc.world.getOtherEntities(null, box).isEmpty();
      }
   }

   public EndCrystalEntity findCrystalAt(BlockPos pos) {
      Box box = new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);

      for (Entity entity : mc.world.getOtherEntities(null, box)) {
         if (entity instanceof EndCrystalEntity endCrystalEntity && endCrystalEntity.isAlive()) {
            return endCrystalEntity;
         }
      }

      return null;
   }

   public int findHotbarSlot(Item item) {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            return i;
         }
      }

      return -1;
   }
}
