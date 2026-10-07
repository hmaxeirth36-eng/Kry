package com.water.render;

import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class GlyphTexture {
   public NativeImageBackedTexture tex;
   public Identifier id;
   public int displayW;
   public int displayH;

   public GlyphTexture(NativeImageBackedTexture nativeImageBackedTexture, Identifier id, int var3, int var4) {
      this.tex = nativeImageBackedTexture;
      this.id = id;
      this.displayW = var3;
      this.displayH = var4;
   }
}
