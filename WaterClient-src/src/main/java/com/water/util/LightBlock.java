package com.water.util;

import net.minecraft.util.math.BlockPos;

public final class LightBlock {
   public final BlockPos pos;
   public final int level;

   public LightBlock(BlockPos pos, int var2) {
      this.pos = pos;
      this.level = var2;
   }
}
