package com.water.render;

import com.water.gui.FontType;
import com.water.render.pipeline.TexturePipeline;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public class FontRenderer {
   public static final FontRenderer INSTANCE = new FontRenderer();
   public static volatile FontType currentFont = FontType.INTER;
   public Font awtFont;
   public FontType loadedFont = null;
   public static volatile int clipBottom = -1;
   public final AtomicInteger idCounter = new AtomicInteger(0);
   public final LinkedHashMap<String, GlyphTexture> cache = new LinkedHashMap<String, GlyphTexture>() {
      @Override
      public boolean removeEldestEntry(Entry<String, GlyphTexture> entry) {
         if (this.size() > 300) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null) {
               try {
                  minecraftClient.getTextureManager().destroyTexture(((GlyphTexture)entry.getValue()).id);
               } catch (Exception exception) {
               }
            } else {
               ((GlyphTexture)entry.getValue()).tex.close();
            }

            return true;
         } else {
            return false;
         }
      }
   };

   public FontRenderer() {
   }

   public static void setClipBottom(int var0) {
      clipBottom = var0;
   }

   public static void clearClip() {
      clipBottom = -1;
   }

   public static void setFont(FontType font) {
      if (font != currentFont) {
         currentFont = font;
         INSTANCE.awtFont = null;
         INSTANCE.loadedFont = null;
         INSTANCE.cache.clear();
      }
   }

   public static FontType getFont() {
      return currentFont;
   }

   public static String[] getFontNames() {
      FontType[] afonttype = FontType.values();
      String[] astring = new String[afonttype.length];

      for (int i = 0; i < afonttype.length; i++) {
         astring[i] = afonttype[i].displayName;
      }

      return astring;
   }

   public void ensureFontLoaded() {
      FontType fonttype = currentFont;
      if (this.loadedFont != fonttype || this.awtFont == null) {
         this.loadedFont = fonttype;
         if (fonttype == FontType.VANILLA) {
            this.awtFont = null;
         } else {
            try (InputStream inputStream = this.getClass().getResourceAsStream(fonttype.resourcePath)) {
               if (inputStream != null) {
                  this.awtFont = Font.createFont(fonttype.fontType, inputStream).deriveFont(0, fonttype.size);
                  return;
               }
            } catch (Exception exception) {
            }

            this.awtFont = new Font("SansSerif", 0, 16);
         }
      }
   }

   public GlyphTexture getGlyphTexture(String text) {
      String s = currentFont.name() + ":" + text;
      GlyphTexture glyphTexture = this.cache.get(s);
      if (glyphTexture != null) {
         return glyphTexture;
      } else {
         glyphTexture = this.createGlyphTexture(text);
         if (glyphTexture != null) {
            this.cache.put(s, glyphTexture);
         }

         return glyphTexture;
      }
   }

   public GlyphTexture createGlyphTexture(String text) {
      this.ensureFontLoaded();
      if (this.awtFont == null) {
         return null;
      } else {
         BufferedImage bufferedImage = new BufferedImage(1, 1, 2);
         Graphics2D graphics2D = bufferedImage.createGraphics();
         graphics2D.setFont(this.awtFont);
         FontRenderContext fontRenderContext = graphics2D.getFontRenderContext();
         Rectangle2D rectangle2D = this.awtFont.getStringBounds(text, fontRenderContext);
         graphics2D.dispose();
         int i = Math.max(1, (int)Math.ceil(rectangle2D.getWidth()) + 6);
         int j = Math.max(1, (int)Math.ceil(this.awtFont.getSize() * 1.3F));
         BufferedImage bufferedImage1 = new BufferedImage(i, j, 2);
         Graphics2D graphics2D1 = bufferedImage1.createGraphics();
         graphics2D1.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         graphics2D1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         graphics2D1.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         graphics2D1.setFont(this.awtFont);
         graphics2D1.setColor(Color.WHITE);
         graphics2D1.drawString(text, 1, this.awtFont.getSize() - 1);
         graphics2D1.dispose();

         try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage1, "png", byteArrayOutputStream);
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(() -> "water_font_cache", nativeImage);
            Identifier identifier = Identifier.of("water", "font_cache_" + this.idCounter.getAndIncrement());
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null) {
               minecraftClient.getTextureManager().registerTexture(identifier, nativeImageBackedTexture);
            }

            return new GlyphTexture(nativeImageBackedTexture, identifier, i / 2, j / 2);
         } catch (Exception exception) {
            return null;
         }
      }
   }

   public void drawString(DrawContext context, String text, float x, float y, int color) {
      if (text != null && !text.isEmpty()) {
         if (clipBottom < 0 || !(y >= clipBottom)) {
            if (currentFont == FontType.VANILLA) {
               MinecraftClient minecraftClient = MinecraftClient.getInstance();
               if (minecraftClient != null) {
                  context.drawText(minecraftClient.textRenderer, text, (int)x, (int)y, color, false);
               }
            } else {
               GlyphTexture glyphTexture = this.getGlyphTexture(text);
               if (glyphTexture != null && glyphTexture.tex.getGlTextureView() != null) {
                  Matrix4f matrix4f = Render2D.getProjectionMatrix(context);
                  TexturePipeline.drawTextureRect(matrix4f, x, y, glyphTexture.displayW, glyphTexture.displayH, glyphTexture.tex.getGlTextureView(), color, 0.0F);
               }
            }
         }
      }
   }

   public int getWidth(String text) {
      if (text == null || text.isEmpty()) {
         return 0;
      } else if (currentFont == FontType.VANILLA) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         return minecraftClient != null ? minecraftClient.textRenderer.getWidth(text) : text.length() * 6;
      } else {
         this.ensureFontLoaded();
         if (this.awtFont == null) {
            return text.length() * 6;
         } else {
            BufferedImage bufferedImage = new BufferedImage(1, 1, 2);
            Graphics2D graphics2D = bufferedImage.createGraphics();
            graphics2D.setFont(this.awtFont);
            int i = graphics2D.getFontMetrics().stringWidth(text) / 2;
            graphics2D.dispose();
            return i;
         }
      }
   }

   public int getWidth(CharSequence charSequence) {
      return this.getWidth(charSequence != null ? charSequence.toString() : "");
   }
}
