package com.water.module.modules.combat;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

public final class SpearSwap extends Module {
   public static SpearSwap INSTANCE;
   public final Setting<Boolean> lunge = new Setting<>("Lunge", true);
   public final Setting<Boolean> sharpness = new Setting<>("Sharpness", false);
   public final Setting<Boolean> onlySword = new Setting<>("Only Sword", false);
   public final Setting<Boolean> onlyAxe = new Setting<>("Only Axe", false);
   public final Setting<Boolean> switchBack = new Setting<>("Switch Back", true);
   public final Setting<Float> switchDelay = new Setting<>("Switch Delay", 1.0F, 1.0F, 20.0F);
   public int previousSlot = -1;
   public int countdown = 0;
   public boolean attackHeldLastCheck = false;

   public SpearSwap() {
      super("SpearSwap", Category.COMBAT);
      this.addSetting(this.lunge);
      this.addSetting(this.sharpness);
      this.addSetting(this.onlySword);
      this.addSetting(this.onlyAxe);
      this.addSetting(this.switchBack);
      this.addSetting(this.switchDelay);
      INSTANCE = this;
   }

   public void preAttack() {
      if (mc.player != null) {
         boolean flag = this.attackHeldLastCheck;
         this.attackHeldLastCheck = true;
         if (!flag) {
            if (this.countdown <= 0) {
               PlayerInventory playerInventory = mc.player.getInventory();
               int i = this.findBestWeaponSlot();
               if (i >= 0) {
                  if (playerInventory.getSelectedSlot() != i) {
                     this.previousSlot = playerInventory.getSelectedSlot();
                     playerInventory.setSelectedSlot(i);
                     this.countdown = Math.max(1, this.switchDelay.getValue().intValue());
                  }
               }
            }
         }
      }
   }

   public void noAttack() {
      this.attackHeldLastCheck = false;
   }

   @Override
   public void onEnable() {
      this.previousSlot = -1;
      this.countdown = 0;
   }

   @Override
   public void onDisable() {
      this.previousSlot = -1;
      this.countdown = 0;
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.options != null) {
         PlayerInventory playerInventory = mc.player.getInventory();
         if (this.countdown > 0) {
            this.countdown--;
            if (this.countdown == 0) {
               if (this.switchBack.getValue() && this.previousSlot >= 0 && this.previousSlot < 9 && playerInventory.getSelectedSlot() != this.previousSlot) {
                  playerInventory.setSelectedSlot(this.previousSlot);
               }

               this.previousSlot = -1;
            }
         }

         if (mc.options.attackKey.isPressed()) {
            this.preAttack();
         } else {
            this.noAttack();
         }
      }
   }

   public int findBestWeaponSlot() {
      PlayerInventory playerInventory = mc.player.getInventory();
      boolean flag = this.onlySword.getValue();
      boolean flag1 = this.onlyAxe.getValue();
      boolean flag2 = this.lunge.getValue();
      boolean flag3 = this.sharpness.getValue();
      int i = -1;
      int j = Integer.MIN_VALUE;

      for (int k = 0; k < 9; k++) {
         ItemStack itemStack = playerInventory.getStack(k);
         if (!itemStack.isEmpty()) {
            String s = Registries.ITEM.getId(itemStack.getItem()).getPath();
            String s1 = "";

            try {
               s1 = itemStack.getName().getString().toLowerCase();
            } catch (Throwable throwable) {
            }

            boolean flag4 = s.endsWith("_sword");
            boolean flag5 = itemStack.getItem() instanceof AxeItem;
            boolean flag6 = itemStack.getItem() == Items.TRIDENT || itemStack.getItem() == Items.MACE || s.contains("spear") || s1.contains("spear");
            boolean flag7 = this.hasLunge(itemStack);
            boolean flag8 = flag2 && flag7;
            if ((!flag || flag4) && (!flag1 || flag5) && (flag || flag1 || flag4 || flag5 || flag6 || flag8)) {
               int l = 0;
               if (flag8) {
                  l += 500;
               }

               if (flag6) {
                  l += 300;
               } else if (flag4) {
                  l += 200;
               } else if (flag5) {
                  l += 100;
               }

               if (flag3) {
                  l += this.getSharpnessLevel(itemStack) * 60;
               }

               if (l > j) {
                  j = l;
                  i = k;
               }
            }
         }
      }

      return i;
   }

   public boolean hasLunge(ItemStack stack) {
      try {
         ItemEnchantmentsComponent itemEnchantmentsComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
         if (itemEnchantmentsComponent == null) {
            return false;
         }

         for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            RegistryKey registryKey = (RegistryKey)registryEntry.getKey().orElse(null);
            if (registryKey != null) {
               if (registryKey.equals(Enchantments.WIND_BURST) || registryKey.equals(Enchantments.DENSITY) || registryKey.equals(Enchantments.RIPTIDE)) {
                  return true;
               }

               String s = registryKey.getValue().toString().toLowerCase();
               if (s.contains("lunge")) {
                  return true;
               }
            }
         }
      } catch (Throwable throwable) {
      }

      return false;
   }

   public int getSharpnessLevel(ItemStack stack) {
      try {
         ItemEnchantmentsComponent itemEnchantmentsComponent = stack.get(DataComponentTypes.ENCHANTMENTS);
         if (itemEnchantmentsComponent == null) {
            return 0;
         }

         for (RegistryEntry registryEntry : itemEnchantmentsComponent.getEnchantments()) {
            RegistryKey registryKey = (RegistryKey)registryEntry.getKey().orElse(null);
            if (registryKey != null && registryKey.equals(Enchantments.SHARPNESS)) {
               return itemEnchantmentsComponent.getLevel(registryEntry);
            }
         }
      } catch (Throwable throwable) {
      }

      return 0;
   }
}
