package com.water.module;

import net.minecraft.item.ItemStack;

public enum Category {
   COMBAT("COMBAT"),
   RENDER("RENDER"),
   MISC("MISC"),
   DONUT("DONUT"),
   CLIENT("CLIENT");

   public final String name;

    Category(String text) {
      this.name = text;
   }

   public String getName() {
      return this.name;
   }

   public ItemStack getIcon() {
      return ItemStack.EMPTY;
   }
}
