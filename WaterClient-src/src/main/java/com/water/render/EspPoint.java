package com.water.render;

import java.awt.Color;

public final class EspPoint {
   public final double x;
   public final double y;
   public final double z;
   public final Color color;
   public final double distSq;

   public EspPoint(double var1, double var3, double var5, Color color, double var8) {
      this.x = var1;
      this.y = var3;
      this.z = var5;
      this.color = color;
      this.distSq = var8;
   }
}
