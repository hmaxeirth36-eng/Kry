package com.water.util;

import java.util.List;
import net.minecraft.util.math.BlockPos;

public class BlockCluster {
   public final int length;
   public final List<BlockPos> positions;

   public BlockCluster(List<BlockPos> list) {
      this.positions = list;
      this.length = list.size();
   }

   public BlockPos centroid() {
      long i = 0L;
      long j = 0L;
      long k = 0L;

      for (BlockPos blockPos : this.positions) {
         i += blockPos.getX();
         j += blockPos.getY();
         k += blockPos.getZ();
      }

      return new BlockPos((int)(i / this.positions.size()), (int)(j / this.positions.size()), (int)(k / this.positions.size()));
   }
}
