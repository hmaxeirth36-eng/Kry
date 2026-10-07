package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.ModeSetting;
import com.water.module.setting.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;

public final class BoneDropper extends Module {
   public static final int MIN_DELAY_MS = 100;
   public static final int MAX_DELAY_MS = 2000;
   public static final int DEFAULT_DELAY_MS = 300;
   public static final int PLAYER_INVENTORY_SLOT_COUNT = 36;
   public static final int PRIMARY_CLICK_BUTTON = 0;
   public static final long SPAWNER_DROP_TIMEOUT_MS = 4000L;
   public final ModeSetting mode = new ModeSetting("Mode", "Spawner", "Spawner", "Orders");
   public final Setting<Integer> delayMs = new Setting<>("Delay", 300, 100, 2000);
   public BoneDropperState state = BoneDropperState.SPAWNER_OPEN_MENU;
   public String lastMode = this.mode.getValue();
   public long nextActionAtMs;
   public int spawnerBoneCountBeforeDrop;
   public long spawnerDropRequestedAtMs;
   public boolean spawnerGridWasFullBeforeDrop;

   public BoneDropper() {
      super("BoneDropper", Category.DONUT);
      this.addSetting(this.mode);
      this.addSetting(this.delayMs);
   }

   @Override
   public void onEnable() {
      this.resetFlow();
   }

   @Override
   public void onDisable() {
      this.state = BoneDropperState.SPAWNER_OPEN_MENU;
      this.nextActionAtMs = 0L;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.currentScreen == null || mc.currentScreen instanceof HandledScreen) {
            if (!this.mode.getValue().equalsIgnoreCase(this.lastMode)) {
               this.resetFlow();
            }

            long i = System.currentTimeMillis();
            if (i >= this.nextActionAtMs) {
               if (this.mode.is("Spawner")) {
                  this.tickSpawnerFlow();
               } else {
                  this.tickOrderFlow();
               }
            }
         }
      }
   }

   public void tickSpawnerFlow() {
      switch (this.state) {
         case SPAWNER_OPEN_MENU:
            if (this.isScreenOpen()) {
               this.state = BoneDropperState.SPAWNER_WAIT_MENU;
               this.scheduleNextAction();
               return;
            }

            this.interactWithTargetBlock();
            this.state = BoneDropperState.SPAWNER_WAIT_MENU;
            this.scheduleNextAction();
            break;
         case SPAWNER_WAIT_MENU:
            if (!this.isScreenOpen()) {
               this.state = BoneDropperState.SPAWNER_OPEN_MENU;
               this.scheduleNextAction();
               return;
            }

            this.state = BoneDropperState.SPAWNER_SCAN_GRID;
            this.scheduleNextAction();
            break;
         case SPAWNER_SCAN_GRID:
            ScreenHandler screenHandler2 = this.getOpenHandler();
            if (screenHandler2 == null) {
               this.state = BoneDropperState.SPAWNER_OPEN_MENU;
               this.scheduleNextAction();
               return;
            }

            if (!this.hasClickableBottomSlot(screenHandler2)) {
               this.scheduleNextAction();
               return;
            }

            this.state = BoneDropperState.SPAWNER_CLICK_DROPPER;
            this.scheduleNextAction();
            break;
         case SPAWNER_CLICK_DROPPER:
            ScreenHandler screenHandler1 = this.getOpenHandler();
            if (screenHandler1 == null) {
               this.state = BoneDropperState.SPAWNER_OPEN_MENU;
               this.scheduleNextAction();
               return;
            }

            Slot slot = this.findBottomDropperSlot(screenHandler1);
            if (slot == null) {
               this.scheduleNextAction();
               return;
            }

            this.spawnerGridWasFullBeforeDrop = this.hasClickableBottomSlot(screenHandler1);
            if (!this.spawnerGridWasFullBeforeDrop) {
               this.state = BoneDropperState.SPAWNER_SCAN_GRID;
               this.scheduleNextAction();
               return;
            }

            this.spawnerBoneCountBeforeDrop = this.countItem(Items.BONE);
            this.spawnerDropRequestedAtMs = System.currentTimeMillis();
            this.clickSlot(slot);
            this.state = BoneDropperState.SPAWNER_WAIT_DROP_CONFIRM;
            this.scheduleNextAction();
            break;
         case SPAWNER_WAIT_DROP_CONFIRM:
            long i = System.currentTimeMillis();
            ScreenHandler screenHandler = this.getOpenHandler();
            boolean flag = this.spawnerGridWasFullBeforeDrop && screenHandler != null && !this.hasClickableBottomSlot(screenHandler);
            boolean flag1 = this.countItem(Items.BONE) > this.spawnerBoneCountBeforeDrop;
            if (flag || flag1) {
               if (screenHandler != null) {
                  this.closeScreen();
               }

               this.state = BoneDropperState.SPAWNER_DONE;
               this.nextActionAtMs = Long.MAX_VALUE;
               return;
            }

            if (this.spawnerDropRequestedAtMs > 0L && i - this.spawnerDropRequestedAtMs > 4000L) {
               this.state = screenHandler == null ? BoneDropperState.SPAWNER_OPEN_MENU : BoneDropperState.SPAWNER_SCAN_GRID;
               this.scheduleNextAction();
               return;
            }

            this.scheduleNextAction();
         case SPAWNER_DONE:
            break;
         default:
            this.state = BoneDropperState.SPAWNER_OPEN_MENU;
            this.scheduleNextAction();
      }
   }

   public void tickOrderFlow() {
      switch (this.state) {
         case ORDERS_SEND_COMMAND:
            if (!this.isScreenOpen()) {
               this.sendCommand("/order");
               this.state = BoneDropperState.ORDERS_WAIT_MENU;
               this.scheduleNextAction();
               return;
            }

            this.state = BoneDropperState.ORDERS_CLICK_CHEST_ONE;
            this.scheduleNextAction();
            break;
         case ORDERS_WAIT_MENU:
            if (!this.isScreenOpen()) {
               this.sendCommand("/order");
               this.scheduleNextAction();
               return;
            }

            this.state = BoneDropperState.ORDERS_CLICK_CHEST_ONE;
            this.scheduleNextAction();
            break;
         case ORDERS_CLICK_CHEST_ONE:
            this.clickItemSlot(BoneDropperItem.CHEST, BoneDropperState.ORDERS_CLICK_BONE);
            break;
         case ORDERS_CLICK_BONE:
            this.clickItemSlot(BoneDropperItem.BONE, BoneDropperState.ORDERS_CLICK_CHEST_TWO);
            break;
         case ORDERS_CLICK_CHEST_TWO:
            this.clickItemSlot(BoneDropperItem.CHEST, BoneDropperState.ORDERS_CLICK_DROPPER_ONE);
            break;
         case ORDERS_CLICK_DROPPER_ONE:
            this.clickItemSlot(BoneDropperItem.DROPPER, BoneDropperState.ORDERS_CLICK_ARROW);
            break;
         case ORDERS_CLICK_ARROW:
            this.clickItemSlot(BoneDropperItem.ARROW, BoneDropperState.ORDERS_CLICK_DROPPER_TWO);
            break;
         case ORDERS_CLICK_DROPPER_TWO:
            this.clickItemSlot(BoneDropperItem.DROPPER, BoneDropperState.ORDERS_CLICK_CHEST_ONE);
            break;
         default:
            this.state = BoneDropperState.ORDERS_SEND_COMMAND;
            this.scheduleNextAction();
      }
   }

   public void clickItemSlot(BoneDropperItem boneDropperItem, BoneDropperState boneDropperState) {
      ScreenHandler screenHandler = this.getOpenHandler();
      if (screenHandler == null) {
         this.sendCommand("/order");
         this.scheduleNextAction();
      } else {
         Slot slot = this.findItemSlot(screenHandler, boneDropperItem, boneDropperItem == BoneDropperItem.DROPPER || boneDropperItem == BoneDropperItem.ARROW);
         if (slot == null) {
            this.scheduleNextAction();
         } else {
            this.clickSlot(slot);
            this.state = boneDropperState;
            this.scheduleNextAction();
         }
      }
   }

   public boolean hasClickableBottomSlot(ScreenHandler handler) {
      List list = this.getBottomRowSlots(handler);
      if (list.isEmpty()) {
         return false;
      } else {
         for (Slot slot : (Iterable<Slot>)list) {
            if (slot.isEnabled()) {
               ItemStack itemStack = slot.getStack();
               if (itemStack.isEmpty() || !this.matchesItem(itemStack, BoneDropperItem.BONE)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   public List<Slot> getBottomRowSlots(ScreenHandler handler) {
      List<Slot> list = this.getContainerSlots(handler);
      if (list.isEmpty()) {
         return List.of();
      } else {
         int i = list.stream().mapToInt(var0 -> var0.y).max().orElse(Integer.MIN_VALUE);
         ArrayList arrayList = new ArrayList();

         for (Slot slot : (Iterable<Slot>)list) {
            if (slot.y < i) {
               arrayList.add(slot);
            }
         }

         return (List<Slot>)(arrayList.isEmpty() ? list : arrayList);
      }
   }

   public Slot findBottomDropperSlot(ScreenHandler handler) {
      List<Slot> list = this.getContainerSlots(handler);
      if (list.isEmpty()) {
         return null;
      } else {
         int i = list.stream().mapToInt(var0 -> var0.y).max().orElse(Integer.MIN_VALUE);
         Slot slot = this.findSlotForItem(list, BoneDropperItem.DROPPER, true, i);
         if (slot != null) {
            return slot;
         } else {
            Slot slot1 = this.findSlotForItem(list, BoneDropperItem.DROPPER, true, Integer.MIN_VALUE);
            if (slot1 != null) {
               return slot1;
            } else {
               for (int j = list.size() - 1; j >= 0; j--) {
                  Slot slot2 = (Slot)list.get(j);
                  if (slot2.isEnabled()) {
                     return slot2;
                  }
               }

               return null;
            }
         }
      }
   }

   public Slot findItemSlot(ScreenHandler handler, BoneDropperItem boneDropperItem, boolean var3) {
      return this.findSlotForItem(this.getContainerSlots(handler), boneDropperItem, var3, Integer.MIN_VALUE);
   }

   public Slot findSlotForItem(List<Slot> list, BoneDropperItem boneDropperItem, boolean var3, int var4) {
      Slot slot = null;

      for (Slot slot1 : list) {
         if (slot1.isEnabled() && (var4 == Integer.MIN_VALUE || slot1.y == var4) && this.matchesItem(slot1.getStack(), boneDropperItem)) {
            if (slot == null) {
               slot = slot1;
            } else if (var3) {
               if (slot1.y > slot.y || slot1.y == slot.y && slot1.x >= slot.x) {
                  slot = slot1;
               }
            } else if (slot1.y < slot.y || slot1.y == slot.y && slot1.x < slot.x) {
               slot = slot1;
            }
         }
      }

      return slot;
   }

   public boolean matchesItem(ItemStack stack, BoneDropperItem boneDropperItem) {
      if (stack != null && !stack.isEmpty()) {
         Item item = stack.getItem();
         String s = stack.getName().getString().toLowerCase(Locale.ROOT);

         return switch (boneDropperItem) {
            case BONE -> item == Items.BONE || s.contains("bone");
            case CHEST -> item == Items.CHEST || item == Items.TRAPPED_CHEST || item == Items.ENDER_CHEST || s.contains("chest") || s.contains("truhe");
            case DROPPER -> item == Items.DROPPER || s.contains("dropper");
            case ARROW -> item == Items.ARROW || s.contains("arrow") || s.contains("pfeil");
         };
      } else {
         return false;
      }
   }

   public List<Slot> getContainerSlots(ScreenHandler handler) {
      if (handler != null && handler.slots != null && !handler.slots.isEmpty()) {
         int i = Math.max(0, handler.slots.size() - 36);
         if (i == 0) {
            i = handler.slots.size();
         }

         ArrayList arrayList = new ArrayList(i);

         for (int j = 0; j < i; j++) {
            arrayList.add(handler.slots.get(j));
         }

         return arrayList;
      } else {
         return List.of();
      }
   }

   public int countItem(Item item) {
      if (mc.player == null) {
         return 0;
      } else {
         int i = 0;

         for (int j = 0; j < mc.player.getInventory().size(); j++) {
            ItemStack itemStack = mc.player.getInventory().getStack(j);
            if (!itemStack.isEmpty() && itemStack.isOf(item)) {
               i += itemStack.getCount();
            }
         }

         return i;
      }
   }

   public void clickSlot(Slot slot) {
      if (slot != null && mc.player != null && mc.interactionManager != null) {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot.id, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   public void interactWithTargetBlock() {
      if (mc.crosshairTarget instanceof BlockHitResult blockHitResult && mc.crosshairTarget.getType() == Type.BLOCK) {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
         mc.player.swingHand(Hand.MAIN_HAND);
      }
   }

   public void closeScreen() {
      if (mc.player != null) {
         if (mc.currentScreen instanceof HandledScreen) {
            mc.player.closeHandledScreen();
            mc.setScreen(null);
         }
      }
   }

   public ScreenHandler getOpenHandler() {
      return mc.currentScreen instanceof HandledScreen && mc.player != null ? mc.player.currentScreenHandler : null;
   }

   public boolean isScreenOpen() {
      return this.getOpenHandler() != null;
   }

   public void sendCommand(String text) {
      ClientPlayNetworkHandler clientPlayNetworkHandler = mc.player != null ? mc.player.networkHandler : mc.getNetworkHandler();
      if (clientPlayNetworkHandler != null) {
         String s = text.startsWith("/") ? text.substring(1) : text;

         try {
            clientPlayNetworkHandler.sendChatCommand(s);
         } catch (Throwable throwable) {
            clientPlayNetworkHandler.sendChatMessage(text);
         }
      }
   }

   public void resetFlow() {
      this.lastMode = this.mode.getValue();
      this.state = this.mode.is("Spawner") ? BoneDropperState.SPAWNER_OPEN_MENU : BoneDropperState.ORDERS_SEND_COMMAND;
      this.nextActionAtMs = 0L;
      this.spawnerBoneCountBeforeDrop = 0;
      this.spawnerDropRequestedAtMs = 0L;
      this.spawnerGridWasFullBeforeDrop = false;
   }

   public void scheduleNextAction() {
      this.nextActionAtMs = System.currentTimeMillis() + this.delayMs.getValue().intValue();
   }
}
