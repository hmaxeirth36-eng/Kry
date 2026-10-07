package com.water.gui;

import net.minecraft.item.ItemStack;

public final class Notification {
   public final String message;
   public final String details;
   public final ItemStack stack;
   public final int accentColor;
   public final long createdAt;

   public Notification(String text, String text2, ItemStack stack, int accentColor, long createdAt) {
      this.message = text;
      this.details = text2;
      this.stack = stack;
      this.accentColor = accentColor | 0xFF000000;
      this.createdAt = createdAt;
   }

   public boolean isExpired(long now) {
      return now - this.createdAt >= 3500L;
   }

   public float getAlpha(long now) {
      long i = now - this.createdAt;
      if (i <= 0L) {
         return 0.0F;
      } else if (i < 220L) {
         return this.easeOutCubic((float)i / 220.0F);
      } else {
         long j = 3280L;
         return i > j ? this.easeOutCubic(Math.max(0.0F, 1.0F - (float)(i - j) / 220.0F)) : 1.0F;
      }
   }

   public float easeOutCubic(float t) {
      float f = 1.0F - Math.max(0.0F, Math.min(1.0F, t));
      return 1.0F - f * f * f;
   }
}
