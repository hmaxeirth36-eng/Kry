package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;
import java.util.function.Predicate;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooShootBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

public final class AutoTool extends Module {
   public AutoTool() {
      super("Auto Tool", Category.MISC);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.options.attackKey.isPressed() && mc.crosshairTarget != null) {
            HitResult hitResult = mc.crosshairTarget;
            if (hitResult.getType() == Type.ENTITY && hitResult instanceof EntityHitResult) {
               this.selectBestWeapon();
            } else {
               if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
                  this.selectBestToolFor(blockHitResult.getBlockPos());
               }
            }
         }
      }
   }

   public void selectBestToolFor(BlockPos pos) {
      BlockState blockState = mc.world.getBlockState(pos);
      ItemStack itemStack = mc.player.getMainHandStack();
      int i = -1;
      double d0 = -1.0;

      for (int j = 0; j < 9; j++) {
         ItemStack itemStack1 = mc.player.getInventory().getStack(j);
         double d1 = scoreTool(itemStack1, blockState, var0 -> true);
         if (d1 > d0) {
            d0 = d1;
            i = j;
         }
      }

      if (i != -1) {
         double d2 = scoreTool(itemStack, blockState, var0 -> true);
         if (d0 > d2 || !isTool(itemStack)) {
            this.selectSlot(i);
         }
      }
   }

   public void selectBestWeapon() {
      int i = -1;
      double d0 = Double.NEGATIVE_INFINITY;

      for (int j = 0; j < 9; j++) {
         ItemStack itemStack = mc.player.getInventory().getStack(j);
         if (!itemStack.isEmpty()) {
            double d1 = this.getAttackDamage(itemStack);
            if (d1 > d0) {
               d0 = d1;
               i = j;
            }
         }
      }

      if (i != -1) {
         this.selectSlot(i);
      }
   }

   public double getAttackDamage(ItemStack stack) {
      AttributeModifiersComponent attributeModifiersComponent = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      double d0 = 0.0;
      if (attributeModifiersComponent != null) {
         for (Entry entry : attributeModifiersComponent.modifiers()) {
            if (entry.attribute().toString().contains("attack_damage")) {
               d0 += entry.modifier().value();
            }
         }
      }

      if (d0 > 0.0) {
         return d0;
      } else {
         String s = Registries.ITEM.getId(stack.getItem()).getPath();
         if (s.endsWith("_sword")) {
            return 10.0 + this.getMaterialTier(s);
         } else {
            return s.endsWith("_axe") ? 5.0 + this.getMaterialTier(s) : 0.0;
         }
      }
   }

   public void selectSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         if (mc.player.getInventory().getSelectedSlot() != slot) {
            mc.player.getInventory().setSelectedSlot(slot);
         }
      }
   }

   public static double scoreTool(ItemStack stack, BlockState state, Predicate<ItemStack> predicate) {
      if (predicate.test(stack) && isTool(stack)) {
         String s = Registries.ITEM.getId(stack.getItem()).getPath();
         boolean flag = s.endsWith("_sword");
         return !stack.isSuitableFor(state)
               && (!flag || !(state.getBlock() instanceof BambooBlock) && !(state.getBlock() instanceof BambooShootBlock))
               && (!(stack.getItem() instanceof ShearsItem) || !(state.getBlock() instanceof LeavesBlock))
               && !state.isIn(BlockTags.WOOL)
            ? -1.0
            : stack.getMiningSpeedMultiplier(state) * 1000.0F;
      } else {
         return -1.0;
      }
   }

   public static boolean isTool(ItemStack stack) {
      return isToolItem(stack.getItem());
   }

   public static boolean isToolItem(Item item) {
      if (item instanceof ShearsItem) {
         return true;
      } else {
         String s = Registries.ITEM.getId(item).getPath();
         return s.endsWith("_pickaxe") || s.endsWith("_axe") || s.endsWith("_shovel") || s.endsWith("_hoe") || s.endsWith("_sword");
      }
   }

   public double getMaterialTier(String text) {
      if (text.startsWith("netherite_")) {
         return 6.0;
      } else if (text.startsWith("diamond_")) {
         return 5.0;
      } else if (text.startsWith("iron_")) {
         return 4.0;
      } else if (text.startsWith("golden_")) {
         return 3.0;
      } else if (text.startsWith("stone_")) {
         return 2.0;
      } else {
         return text.startsWith("wooden_") ? 1.0 : 0.0;
      }
   }
}
