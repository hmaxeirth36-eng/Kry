package com.water.module.modules.combat;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;

public final class AutoDoubleHand extends Module {
   public final Setting<Boolean> onTotemPop = new Setting<>("On Totem Pop", true);
   public final Setting<Boolean> onHealth = new Setting<>("On Health", true);
   public final Setting<Float> healthThreshold = new Setting<>("Health Threshold", 6.0F, 1.0F, 20.0F);
   public final Setting<Float> cooldown = new Setting<>("Cooldown", 5.0F, 0.0F, 40.0F);
   public boolean hadTotemHeldLastTick = false;
   public int cooldownTicks = 0;
   public int previousSlot = -1;

   public AutoDoubleHand() {
      super("AutoDoubleHand", Category.COMBAT);
      this.addSetting(this.onTotemPop);
      this.addSetting(this.onHealth);
      this.addSetting(this.healthThreshold);
      this.addSetting(this.cooldown);
   }

   @Override
   public void onEnable() {
      this.hadTotemHeldLastTick = false;
      this.cooldownTicks = 0;
      this.previousSlot = -1;
   }

   @Override
   public void onDisable() {
      this.hadTotemHeldLastTick = false;
      this.cooldownTicks = 0;
      this.previousSlot = -1;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.interactionManager != null) {
         if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
         }

         PlayerInventory playerInventory = mc.player.getInventory();
         boolean flag = mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING;
         boolean flag1 = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
         boolean flag2 = flag || flag1;
         boolean flag3 = this.hadTotemHeldLastTick && !flag2;
         this.hadTotemHeldLastTick = flag2;
         if (this.cooldownTicks <= 0) {
            boolean flag4 = false;
            if (this.onTotemPop.getValue() && flag3) {
               flag4 = true;
            }

            if (this.onHealth.getValue() && mc.player.getHealth() <= this.healthThreshold.getValue()) {
               flag4 = true;
            }

            if (flag4) {
               if (!flag) {
                  int i = this.findTotemSlot();
                  if (i >= 0) {
                     if (playerInventory.getSelectedSlot() != i) {
                        this.previousSlot = playerInventory.getSelectedSlot();
                        playerInventory.setSelectedSlot(i);
                        this.cooldownTicks = this.cooldown.getValue().intValue();
                     }
                  }
               }
            }
         }
      }
   }

   public int findTotemSlot() {
      PlayerInventory playerInventory = mc.player.getInventory();

      for (int i = 0; i < 9; i++) {
         if (playerInventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
            return i;
         }
      }

      return -1;
   }
}
