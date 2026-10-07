package com.water.module.modules.combat;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoTotem extends Module {
   public final Setting<Float> delay = new Setting<>("Delay", 1.0F, 0.0F, 5.0F);
   public int delayCounter;

   public AutoTotem() {
      super("Auto Totem", Category.COMBAT);
      this.addSetting(this.delay);
   }

   @Override
   public void onEnable() {
      super.onEnable();
   }

   @Override
   public void onDisable() {
      super.onDisable();
   }

   @Override
   public void onTick() {
      if (mc.player != null) {
         int i = this.getDelayTicks();
         if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
            this.delayCounter = i;
         } else if (this.delayCounter > 0) {
            this.delayCounter--;
         } else {
            int j = this.findItemSlot(Items.TOTEM_OF_UNDYING);
            if (j != -1) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, toContainerSlot(j), 40, SlotActionType.SWAP, mc.player);
               this.delayCounter = i;
            }
         }
      }
   }

   public int getDelayTicks() {
      double d0 = this.delay.getValue().floatValue();
      return (int)Math.round(d0 * 20.0);
   }

   public int findItemSlot(Item item) {
      if (mc.player == null) {
         return -1;
      } else {
         for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(item)) {
               return i;
            }
         }

         return -1;
      }
   }

   public static int toContainerSlot(int slotIndex) {
      return slotIndex < 9 ? 36 + slotIndex : slotIndex;
   }

   public static String watermarkFragment() {
      return "2";
   }
}
