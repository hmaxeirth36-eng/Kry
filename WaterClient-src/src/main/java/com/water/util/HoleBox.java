package com.water.util;

import java.util.Objects;
import net.minecraft.util.math.Box;

public final class HoleBox {
   public final Box box;
   public final int depth;
   public final boolean is1x1;
   public final long createdAt;

   public HoleBox(Box box, int var2, boolean var3) {
      this.box = box;
      this.depth = var2;
      this.is1x1 = var3;
      this.createdAt = System.currentTimeMillis();
   }

   public boolean rS() {
      return true;
   }

   @Override
   public boolean equals(Object value) {
      if (this == value) {
         return true;
      } else {
         return value instanceof HoleBox holeBox ? Objects.equals(this.box, holeBox.box) : false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.box);
   }
}
