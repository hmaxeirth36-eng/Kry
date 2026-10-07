package com.water.module.modules.combat;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import java.util.Random;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoInvTotem extends Module {
   public final Setting<Float> delay = new Setting<>("Delay", 2.0F, 0.0F, 20.0F);
   public final Setting<Boolean> hotbar = new Setting<>("Hotbar", false);
   public final Setting<Float> totemSlot = new Setting<>("Totem Slot", 1.0F, 1.0F, 9.0F);
   public final Setting<Boolean> forceTotem = new Setting<>("Force Totem", false);
   public final Setting<Boolean> autoOpen = new Setting<>("Auto Open", false);
   public final Setting<Float> closeDelay = new Setting<>("Close Delay", 3.0F, 0.0F, 20.0F);
   public static final int STATE_IDLE = 0;
   public static final int STATE_WAIT_OPEN = 1;
   public static final int STATE_INV_OPEN = 2;
   public static final int STATE_SWAPPED = 3;
   public int state = 0;
   public int tickCounter = 0;
   public boolean wasTotemInOffhand = true;
   public final Random random = new Random();

   public AutoInvTotem() {
      super("Auto Inv Totem", Category.COMBAT);
      this.addSetting(this.delay);
      this.addSetting(this.hotbar);
      this.addSetting(this.totemSlot);
      this.addSetting(this.forceTotem);
      this.addSetting(this.autoOpen);
      this.addSetting(this.closeDelay);
   }

   @Override
   public void onEnable() {
      this.state = 0;
      this.tickCounter = 0;
      this.wasTotemInOffhand = true;
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.state = 0;
      this.tickCounter = 0;
      super.onDisable();
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         PlayerInventory playerInventory = mc.player.getInventory();
         boolean flag = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
         if (this.autoOpen.getValue()) {
            this.tickStateMachine(playerInventory, flag);
            this.wasTotemInOffhand = flag;
         } else {
            this.wasTotemInOffhand = flag;
            if (!(mc.currentScreen instanceof InventoryScreen)) {
               this.tickCounter = 0;
            } else if (this.tickCounter < this.delay.getValue().intValue() + this.getRandomDelay()) {
               this.tickCounter++;
            } else if (!flag && this.swapTotemToOffhand(playerInventory)) {
               this.tickCounter = 0;
            } else {
               if (this.hotbar.getValue()) {
                  this.refillTotemSlot(playerInventory);
               }

               this.tickCounter = 0;
            }
         }
      }
   }

   public void tickStateMachine(PlayerInventory inventory, boolean var2) {
      switch (this.state) {
         case 0:
            if (this.wasTotemInOffhand && !var2 && this.findTotemSlot(inventory) != -1) {
               this.state = 1;
               this.tickCounter = this.delay.getValue().intValue() <= 0 ? 1 + this.random.nextInt(2) : 1 + this.random.nextInt(3);
            }

            if (!var2 && this.state == 0 && !(mc.currentScreen instanceof InventoryScreen) && this.findTotemSlot(inventory) != -1) {
               this.state = 1;
               this.tickCounter = this.delay.getValue().intValue() <= 0 ? 1 + this.random.nextInt(2) : 1 + this.random.nextInt(3);
            }
            break;
         case 1:
            if (this.tickCounter > 0) {
               this.tickCounter--;
               return;
            }

            if (!(mc.currentScreen instanceof InventoryScreen)) {
               mc.setScreen(new InventoryScreen(mc.player));
            }

            this.state = 2;
            this.tickCounter = this.delay.getValue().intValue() + this.getRandomDelay();
            break;
         case 2:
            if (!(mc.currentScreen instanceof InventoryScreen)) {
               this.state = 0;
               return;
            }

            if (this.tickCounter > 0) {
               this.tickCounter--;
               return;
            }

            boolean flag = false;
            if (!var2) {
               flag = this.swapTotemToOffhand(inventory);
            }

            boolean flag1 = false;
            if (this.hotbar.getValue()) {
               flag1 = this.refillTotemSlot(inventory);
            }

            if (!flag && !flag1 && !var2) {
               this.state = 3;
               this.tickCounter = 1;
            } else {
               this.state = 3;
               this.tickCounter = this.closeDelay.getValue().intValue() + this.getRandomDelay();
            }
            break;
         case 3:
            if (this.tickCounter > 0) {
               this.tickCounter--;
               return;
            }

            if (mc.currentScreen instanceof InventoryScreen) {
               mc.player.closeHandledScreen();
               mc.setScreen(null);
            }

            this.state = 0;
      }
   }

   public boolean swapTotemToOffhand(PlayerInventory inventory) {
      int i = this.findTotemSlot(inventory);
      if (i == -1) {
         return false;
      } else {
         int j = toContainerSlot(i);
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, j, 40, SlotActionType.SWAP, mc.player);
         return true;
      }
   }

   public boolean refillTotemSlot(PlayerInventory inventory) {
      int i = this.totemSlot.getValue().intValue() - 1;
      if (inventory.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
         return false;
      } else if (!inventory.getStack(i).isEmpty() && !this.forceTotem.getValue()) {
         return false;
      } else {
         int j = this.findTotemInMainInventory(inventory);
         if (j == -1) {
            return false;
         } else {
            int k = toContainerSlot(j);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, k, i, SlotActionType.SWAP, mc.player);
            return true;
         }
      }
   }

   public int findTotemSlot(PlayerInventory inventory) {
      for (int i = 9; i < 36; i++) {
         if (inventory.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }

      for (int j = 0; j < 9; j++) {
         if (inventory.getStack(j).getItem() == Items.TOTEM_OF_UNDYING) {
            return j;
         }
      }

      return -1;
   }

   public int findTotemInMainInventory(PlayerInventory inventory) {
      for (int i = 9; i < 36; i++) {
         if (inventory.getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }

      return -1;
   }

   public static int toContainerSlot(int slotIndex) {
      return slotIndex < 9 ? 36 + slotIndex : slotIndex;
   }

   public int getRandomDelay() {
      return this.delay.getValue().intValue() <= 0 ? 0 : this.random.nextInt(2);
   }

   public static String watermarkFragment() {
      return "0";
   }
}
