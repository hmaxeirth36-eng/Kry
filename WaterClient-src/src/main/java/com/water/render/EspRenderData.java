package com.water.render;

import java.awt.Color;

public final class EspRenderData {
   public final double dx;
   public final double dy;
   public final double dz;
   public final double tracerTargetY;
   public final double halfWidth;
   public final double height;
   public final Color fill;
   public final Color tracer;
   public final boolean boxVisible;

   public EspRenderData(double var1, double var3, double var5, double var7, double var9, double var11, Color color, Color color2, boolean var15) {
      this.dx = var1;
      this.dy = var3;
      this.dz = var5;
      this.tracerTargetY = var7;
      this.halfWidth = var9;
      this.height = var11;
      this.fill = color;
      this.tracer = color2;
      this.boxVisible = var15;
   }
}
