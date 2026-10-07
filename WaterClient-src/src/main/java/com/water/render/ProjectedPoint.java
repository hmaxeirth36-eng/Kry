package com.water.render;

public final class ProjectedPoint {
   public double x;
   public double y;
   public double z;
   public double w;
   public boolean visible;

   public ProjectedPoint() {
   }

   public void set(double var1, double var3, double var5, double var7, boolean var9) {
      this.x = var1;
      this.y = var3;
      this.z = var5;
      this.w = var7;
      this.visible = var9;
   }
}
