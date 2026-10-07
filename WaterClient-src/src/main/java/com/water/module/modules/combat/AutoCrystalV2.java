package com.water.module.modules.combat;

import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.setting.Setting;
import java.util.Comparator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AutoCrystalV2 extends ActivatableModule {
   public final Setting<Double> breakRange = new Setting<>("Break Range", 5.0, 1.0, 10.0);
   public final Setting<Double> placeRange = new Setting<>("Place Range", 5.0, 1.0, 10.0);
   public final Setting<Integer> breakDelay = new Setting<>("Break Delay", 0, 0, 20);
   public final Setting<Integer> placeDelay = new Setting<>("Place Delay", 2, 0, 20);
   public final Setting<Boolean> autoObsidian = new Setting<>("Auto Obsidian", false);
   public final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   public final Setting<Boolean> multiTarget = new Setting<>("Multi Target", false);
   public int breakCooldown = 0;
   public int placeCooldown = 0;
   public int savedSlot = -1;
   public boolean wasActive = false;
   public int lastBrokenId = -1;

   public AutoCrystalV2() {
      super("Auto Crystal v2", Category.COMBAT);
      this.addSetting(this.breakRange);
      this.addSetting(this.placeRange);
      this.addSetting(this.breakDelay);
      this.addSetting(this.placeDelay);
      this.addSetting(this.autoObsidian);
      this.addSetting(this.switchBack);
      this.addSetting(this.multiTarget);
   }

   @Override
   public void onEnable() {
      this.resetCooldowns();
      this.wasActive = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.restoreSlot();
      this.resetCooldowns();
      super.onDisable();
   }

   @Override
   public void onActivationKeyPressed() {
      if (this.isEnabled()) {
         this.wasActive = !this.wasActive;
         if (!this.wasActive) {
            this.restoreSlot();
         }
      }
   }

   @Override
   public void onTick() {
      if (this.wasActive) {
         if (mc.player != null && mc.world != null) {
            if (mc.currentScreen == null) {
               this.tickCooldowns();
               PlayerEntity playerEntity = this.findTarget();
               if (playerEntity != null) {
                  if (this.breakCooldown <= 0) {
                     for (EndCrystalEntity endCrystalEntity : mc.world
                        .getEntitiesByClass(
                           EndCrystalEntity.class,
                           new Box(
                                 new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()),
                                 new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())
                              )
                              .expand(this.breakRange.getValue()),
                           var0 -> true
                        )) {
                        if (endCrystalEntity.getId() != this.lastBrokenId) {
                           double d0 = Math.sqrt(
                              Math.pow(mc.player.getX() - endCrystalEntity.getX(), 2.0)
                                 + Math.pow(mc.player.getY() - endCrystalEntity.getY(), 2.0)
                                 + Math.pow(mc.player.getZ() - endCrystalEntity.getZ(), 2.0)
                           );
                           if (!(d0 > this.breakRange.getValue())) {
                              this.attackCrystal(endCrystalEntity);
                              this.lastBrokenId = endCrystalEntity.getId();
                              this.breakCooldown = this.breakDelay.getValue();
                              break;
                           }
                        }
                     }
                  }

                  if (this.placeCooldown <= 0) {
                     BlockPos blockPos = this.findPlacePos(playerEntity);
                     if (blockPos != null) {
                        this.selectCrystalSlot();
                        this.placeCrystal(blockPos);
                        this.placeCooldown = this.placeDelay.getValue();
                     }
                  }
               }
            }
         }
      }
   }

   public void tickCooldowns() {
      if (this.breakCooldown > 0) {
         this.breakCooldown--;
      }

      if (this.placeCooldown > 0) {
         this.placeCooldown--;
      }
   }

   public void resetCooldowns() {
      this.breakCooldown = 0;
      this.placeCooldown = 0;
      this.lastBrokenId = -1;
   }

   public PlayerEntity findTarget() {
      double d0 = Math.max(this.breakRange.getValue(), this.placeRange.getValue());
      return mc.world
         .getPlayers()
         .stream()
         .filter(var0 -> var0 != mc.player && var0.isAlive() && !var0.isInvisibleTo(mc.player))
         .filter(
            var2 -> Math.sqrt(
                  Math.pow(mc.player.getX() - var2.getX(), 2.0) + Math.pow(mc.player.getY() - var2.getY(), 2.0) + Math.pow(mc.player.getZ() - var2.getZ(), 2.0)
               )
               <= d0
         )
         .min(
            Comparator.comparingDouble(
               var0 -> Math.sqrt(
                  Math.pow(mc.player.getX() - var0.getX(), 2.0) + Math.pow(mc.player.getY() - var0.getY(), 2.0) + Math.pow(mc.player.getZ() - var0.getZ(), 2.0)
               )
            )
         )
         .orElse(null);
   }

   public BlockPos findPlacePos(PlayerEntity player) {
      Vec3d vec3d = new Vec3d(player.getX(), player.getY(), player.getZ());
      double d0 = this.placeRange.getValue();
      BlockPos blockPos = null;
      double d1 = Double.MAX_VALUE;

      for (int i = (int)(mc.player.getX() - d0); i <= mc.player.getX() + d0; i++) {
         for (int j = (int)(mc.player.getY() - 2.0); j <= mc.player.getY() + 2.0; j++) {
            for (int k = (int)(mc.player.getZ() - d0); k <= mc.player.getZ() + d0; k++) {
               BlockPos blockPos1 = new BlockPos(i, j, k);
               if (this.isValidBase(blockPos1)) {
                  BlockPos blockPos2 = blockPos1.up();
                  if (mc.world.getBlockState(blockPos2).isAir() && mc.world.getOtherEntities(null, new Box(blockPos2)).isEmpty()) {
                     double d2 = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).distanceTo(Vec3d.ofCenter(blockPos2));
                     if (!(d2 > d0)) {
                        double d3 = vec3d.distanceTo(Vec3d.ofCenter(blockPos2));
                        double d4 = d3 - d2 * 0.3;
                        if (d4 < d1) {
                           d1 = d4;
                           blockPos = blockPos1;
                        }
                     }
                  }
               }
            }
         }
      }

      return blockPos;
   }

   public boolean isValidBase(BlockPos pos) {
      BlockState blockState = mc.world.getBlockState(pos);
      return blockState.isOf(Blocks.OBSIDIAN) || blockState.isOf(Blocks.BEDROCK);
   }

   public void attackCrystal(EndCrystalEntity crystal) {
      mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   public void placeCrystal(BlockPos pos) {
      BlockPos blockPos = pos.up();
      Vec3d vec3d = Vec3d.ofCenter(blockPos);
      BlockHitResult blockHitResult = new BlockHitResult(vec3d, Direction.UP, blockPos, false);
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   public void selectCrystalSlot() {
      if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) {
         int i = -1;

         for (int j = 0; j < 9; j++) {
            if (mc.player.getInventory().getStack(j).isOf(Items.END_CRYSTAL)) {
               i = j;
               break;
            }
         }

         if (i != -1) {
            if (this.switchBack.getValue() && this.savedSlot == -1) {
               this.savedSlot = mc.player.getInventory().getSelectedSlot();
            }

            mc.player.getInventory().setSelectedSlot(i);
         }
      }
   }

   public void restoreSlot() {
      if (this.switchBack.getValue() && this.savedSlot != -1 && mc.player != null) {
         mc.player.getInventory().setSelectedSlot(this.savedSlot);
      }

      this.savedSlot = -1;
   }

   public static String watermarkFragment() {
      return ";";
   }
}
