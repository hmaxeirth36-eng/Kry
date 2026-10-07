package com.water.module.setting;

import net.minecraft.item.ItemStack;

public record DropdownOption(String value, String label, ItemStack previewStack) {

   public ItemStack getPreviewStack() {
      return this.previewStack == null ? ItemStack.EMPTY : this.previewStack.copy();
   }
}
