package com.water.module.modules.misc;

import com.water.mixin.MinecraftClientAccessor;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;

public final class FastPlace extends Module {
   public final Setting<Boolean> onlyXP = new Setting<>("Only XP", false);
   public final Setting<Boolean> allowBlocks = new Setting<>("Blocks", true);
   public final Setting<Boolean> allowItems = new Setting<>("Items", true);
   public final Setting<Float> useDelay = new Setting<>("Delay", 0.0F, 0.0F, 10.0F);

   public FastPlace() {
      super("Fast Place", Category.MISC);
      this.addSetting(this.onlyXP);
      this.addSetting(this.allowBlocks);
      this.addSetting(this.allowItems);
      this.addSetting(this.useDelay);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.currentScreen == null) {
         if (mc.options.useKey.isPressed()) {
            ItemStack itemStack = mc.player.getMainHandStack();
            ItemStack itemStack1 = mc.player.getOffHandStack();
            if (this.shouldFastPlace(itemStack, itemStack1)) {
               MinecraftClientAccessor minecraftClientAccessor = (MinecraftClientAccessor)mc;
               int i = Math.max(0, this.useDelay.getValue().intValue());
               if (minecraftClientAccessor.water$getItemUseCooldown() != i) {
                  minecraftClientAccessor.water$setItemUseCooldown(i);
               }
            }
         }
      }
   }

   public boolean shouldFastPlace(ItemStack stack, ItemStack stack2) {
      boolean flag = stack.isOf(Items.EXPERIENCE_BOTTLE);
      boolean flag1 = stack2.isOf(Items.EXPERIENCE_BOTTLE);
      if (this.onlyXP.getValue()) {
         return flag || flag1;
      } else {
         Item item = stack.getItem();
         Item item1 = stack2.getItem();
         if (this.isFood(stack) || this.isFood(stack2)) {
            return false;
         } else if (stack.isOf(Items.RESPAWN_ANCHOR) || stack.isOf(Items.GLOWSTONE) || stack2.isOf(Items.RESPAWN_ANCHOR) || stack2.isOf(Items.GLOWSTONE)) {
            return false;
         } else if (!(item instanceof RangedWeaponItem) && !(item1 instanceof RangedWeaponItem)) {
            boolean flag2 = item instanceof BlockItem || item1 instanceof BlockItem;
            return flag2 ? this.allowBlocks.getValue() : this.allowItems.getValue();
         } else {
            return false;
         }
      }
   }

   public boolean isFood(ItemStack stack) {
      return stack.getComponents().contains(DataComponentTypes.FOOD);
   }
}
