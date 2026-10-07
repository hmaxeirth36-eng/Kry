package com.water.gui;

public enum FontType {
   INTER("Inter", "/assets/water/font/inter.ttf", 16.0F, 0),
   DEKATRON("Dekatron", "/assets/water/font/dekatron.otf", 16.0F, 0),
   MINECRAFT_TEN("Minecraft Ten", "/assets/water/font/minecraft_ten.ttf", 16.0F, 0),
   BASKETBALL("Basketball", "/assets/water/font/basketball.otf", 16.0F, 0),
   VANILLA("Vanilla", null, 16.0F, -1);

   public final String displayName;
   public final String resourcePath;
   public final float size;
   public final int fontType;

    FontType(String text, String text2, float var5, int var6) {
      this.displayName = text;
      this.resourcePath = text2;
      this.size = var5;
      this.fontType = var6;
   }
}
