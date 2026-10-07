package com.water.module.modules.combat;

import com.water.mixin.HandledScreenAccessor;
import com.water.module.Category;
import com.water.module.Module;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public final class HoverTotem extends Module {
   public int lastSwapAttemptHandlerSlotId = -1;

   public HoverTotem() {
      super("Hover Totem", Category.COMBAT);
   }

   @Override
   public void onDisable() {
      this.lastSwapAttemptHandlerSlotId = -1;
      super.onDisable();
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         if (mc.currentScreen instanceof HandledScreen handledScreen) {
            Slot slot = ((HandledScreenAccessor)handledScreen).water$getFocusedSlot();
            if (slot != null && !slot.getStack().isEmpty()) {
               if (!slot.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                  this.lastSwapAttemptHandlerSlotId = -1;
               } else if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                  if (slot.id != this.lastSwapAttemptHandlerSlotId) {
                     int i = mc.player.currentScreenHandler.syncId;
                     mc.interactionManager.clickSlot(i, slot.id, 40, SlotActionType.SWAP, mc.player);
                     this.lastSwapAttemptHandlerSlotId = slot.id;
                  }
               }
            } else {
               this.lastSwapAttemptHandlerSlotId = -1;
            }
         } else {
            this.lastSwapAttemptHandlerSlotId = -1;
         }
      }
   }
}
