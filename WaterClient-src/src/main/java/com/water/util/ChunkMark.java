package com.water.util;

public final class ChunkMark {
   public final int x;
   public final int z;
   public boolean marked;

   public ChunkMark(int var1, int var2) {
      this.x = var1;
      this.z = var2;
      this.marked = true;
   }
}
