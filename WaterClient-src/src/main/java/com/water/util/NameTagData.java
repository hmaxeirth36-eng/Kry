package com.water.util;

import java.util.List;
import net.minecraft.text.Text;

public record NameTagData(long expiresAtMs, Text nameLabel, int nameWidth, HealthData healthData, List<ItemEntry> items, int itemRowWidth) {

   public boolean pu() {
      return this.nameLabel == null && this.healthData == null && this.items.isEmpty();
   }
}
