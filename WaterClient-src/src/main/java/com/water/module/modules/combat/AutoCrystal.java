package com.water.module.modules.combat;

import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.setting.Setting;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

public final class AutoCrystal extends ActivatableModule {
   public final Setting<Float> placeDelay = new Setting<>("Place Delay", 2.0F, 0.0F, 20.0F);
   public final Setting<Float> breakDelay = new Setting<>("Break Delay", 2.0F, 0.0F, 20.0F);
   public final Setting<Float> placeChance = new Setting<>("Place Chance", 100.0F, 0.0F, 100.0F);
   public final Setting<Float> breakChance = new Setting<>("Break Chance", 100.0F, 0.0F, 100.0F);
   public final Setting<Boolean> fakePunch = new Setting<>("Fake Punch", false);
   public final Setting<Boolean> antiWeakness = new Setting<>("Anti-Weakness", false);
   public final Random random = new Random();
   public boolean crystalling = false;
   public int placeClock = 0;
   public int breakClock = 0;
   public int _tick = 0;

   public AutoCrystal() {
      super("Auto Crystal", Category.COMBAT);
      this.addSetting(this.placeDelay);
      this.addSetting(this.breakDelay);
      this.addSetting(this.placeChance);
      this.addSetting(this.breakChance);
      this.addSetting(this.fakePunch);
      this.addSetting(this.antiWeakness);
   }

   @Override
   public void onEnable() {
      this.placeClock = 0;
      this.breakClock = 0;
      this.crystalling = false;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.placeClock = 0;
      this.breakClock = 0;
      this.crystalling = false;
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
      if (++this._tick % 2 == 0) {
         if (mc.player != null && mc.world != null && mc.currentScreen == null) {
            boolean flag = this.placeClock != 0;
            boolean flag1 = this.breakClock != 0;
            if (flag) {
               this.placeClock--;
            }

            if (flag1) {
               this.breakClock--;
            }

            if (!mc.player.isDead()) {
               if (!this.isActivationKeyDown()) {
                  this.placeClock = 0;
                  this.breakClock = 0;
                  this.crystalling = false;
               } else {
                  this.crystalling = true;
                  if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                     HitResult hitResult = mc.crosshairTarget;
                     int i = this.random.nextInt(100) + 1;
                     if (hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() == Type.BLOCK) {
                        BlockPos blockPos = blockHitResult.getBlockPos();
                        boolean flag2 = mc.world.getBlockState(blockPos).isOf(Blocks.OBSIDIAN);
                        boolean flag3 = mc.world.getBlockState(blockPos).isOf(Blocks.BEDROCK);
                        if (flag2 || flag3) {
                           boolean flag4 = this.canPlaceCrystalAt(blockPos);
                           if (!flag && i <= this.placeChance.getValue()) {
                              if (flag4) {
                                 mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
                                 mc.player.swingHand(Hand.MAIN_HAND);
                                 this.placeClock = this.placeDelay.getValue().intValue();
                              }

                              if (this.fakePunch.getValue() && !flag1 && i <= this.breakChance.getValue()) {
                                 mc.interactionManager.attackBlock(blockPos, blockHitResult.getSide());
                                 mc.player.swingHand(Hand.MAIN_HAND);
                                 this.breakClock = this.breakDelay.getValue().intValue();
                              }
                           }
                        }
                     }

                     i = this.random.nextInt(100) + 1;
                     if (hitResult instanceof EntityHitResult entityHitResult) {
                        Entity entity = entityHitResult.getEntity();
                        if (entity instanceof EndCrystalEntity && !flag1 && i <= this.breakChance.getValue()) {
                           int j = mc.player.getInventory().getSelectedSlot();
                           if (this.antiWeakness.getValue() && this.hasWeakness()) {
                              for (int k = 0; k < 9; k++) {
                                 ItemStack itemStack = mc.player.getInventory().getStack(k);
                                 String s = Registries.ITEM.getId(itemStack.getItem()).getPath();
                                 if (s.endsWith("_sword")) {
                                    mc.player.getInventory().setSelectedSlot(k);
                                    break;
                                 }
                              }
                           }

                           mc.interactionManager.attackEntity(mc.player, entity);
                           mc.player.swingHand(Hand.MAIN_HAND);
                           this.breakClock = this.breakDelay.getValue().intValue();
                           if (this.antiWeakness.getValue()) {
                              mc.player.getInventory().setSelectedSlot(j);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public boolean canPlaceCrystalAt(BlockPos pos) {
      BlockPos blockPos = pos.up();
      if (!mc.world.isAir(blockPos)) {
         return false;
      } else {
         Box box = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockPos.getX() + 1.0, blockPos.getY() + 2.0, blockPos.getZ() + 1.0);
         return mc.world.getOtherEntities(null, box).isEmpty();
      }
   }

   public boolean hasWeakness() {
      if (mc.player == null) {
         return false;
      } else {
         boolean flag = mc.player.hasStatusEffect(StatusEffects.WEAKNESS);
         if (!flag) {
            return false;
         } else {
            boolean flag1 = mc.player.hasStatusEffect(StatusEffects.STRENGTH);
            int i = mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier();
            int j = flag1 ? mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() : -1;
            return j <= i;
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

   public static String watermarkFragment() {
      return "_";
   }
}
