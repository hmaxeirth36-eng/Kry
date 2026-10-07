package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Water;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import com.water.util.RegionInfo;
import com.water.util.RegionMapData;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class RegionMap extends Module {
   public final Setting<Double> cellSize = new Setting<>("Cell Size", 18.0, 8.0, 40.0);
   public final Setting<Double> posX = new Setting<>("Position X", 10.0, 0.0, 10000.0);
   public final Setting<Double> posY = new Setting<>("Position Y", 10.0, 0.0, 10000.0);
   public final Setting<Boolean> showGrid = new Setting<>("Show Grid", true);
   public final Setting<Boolean> showLabels = new Setting<>("Show Labels", true);
   public final Setting<Boolean> showCoords = new Setting<>("Show Coordinates", true);
   public final Setting<Boolean> showPlayer = new Setting<>("Show Player", true);
   public final Setting<Boolean> showLegend = new Setting<>("Show Legend", true);
   public final RegionMapData mapData = new RegionMapData();
   public static final float PANEL_RADIUS = 8.0F;
   public static final float ROW_RADIUS = 5.0F;
   public static final int PANEL_PAD = 8;
   public static final int HEADER_H = 22;
   public static final int ROW_H = 17;
   public static final int ROW_STEP = 19;

   public RegionMap() {
      super("Region Map", Category.RENDER);
      this.addSetting(this.cellSize);
      this.addSetting(this.posX);
      this.addSetting(this.posY);
      this.addSetting(this.showGrid);
      this.addSetting(this.showLabels);
      this.addSetting(this.showCoords);
      this.addSetting(this.showPlayer);
      this.addSetting(this.showLegend);
   }

   public void render(DrawContext context, MinecraftClient client) {
      if (this.isEnabled()) {
         if (client.player != null && client.world != null) {
            int i = this.cellSize.getValue().intValue();
            int j = this.posX.getValue().intValue();
            int k = this.posY.getValue().intValue();
            int l = this.mapData.getMapSize();
            int i1 = l * i;
            int j1 = l * i;
            int k1 = Water.getAccentArgb();
            int l1 = Water.getBackgroundArgb();
            int i2 = -1511950;
            int j2 = -6642510;
            int k2 = i1 + 16;
            int l2 = 22 + j1 + 8;
            Render2D.drawRoundedRect(context, j, k, k2, l2, 8.0F, l1, false);
            Render2D.drawRoundedOutline(context, j, k, k2, l2, 8.0F, 1.0F, withAlphaFraction(k1, 0.45F), false);
            Render2D.G(context, j, k, k2, 22.0F, 8.0F, 8.0F, 0.0F, 0.0F, false, lighten(l1));
            FontRenderer.INSTANCE.drawString(context, "Region Map", j + 8, k + 6, i2);
            context.fill(j + 8, k + 22 - 1, j + k2 - 8, k + 22, withAlphaFraction(k1, 0.25F));
            int i3 = j + 8;
            int j3 = k + 22;

            for (int k3 = 0; k3 < l * l; k3++) {
               RegionInfo regionInfo = this.mapData.getRegionInfo(k3);
               if (regionInfo != null) {
                  int l3 = k3 % l;
                  int i4 = k3 / l;
                  int j4 = i3 + l3 * i;
                  int k4 = j3 + i4 * i;
                  Color color = this.mapData.getRegionColor(regionInfo.regionType());
                  context.fill(j4 + 1, k4 + 1, j4 + i - 1, k4 + i - 1, -872415232 | color.getRGB() & 16777215);
               }
            }

            if (this.showGrid.getValue()) {
               int k5 = withAlphaFraction(-1, 0.12F);

               for (int j6 = 0; j6 <= l; j6++) {
                  context.fill(i3 + j6 * i, j3, i3 + j6 * i + 1, j3 + j1, k5);
                  context.fill(i3, j3 + j6 * i, i3 + i1, j3 + j6 * i + 1, k5);
               }
            }

            if (this.showLabels.getValue() && i >= 14) {
               context.getMatrices().pushMatrix();
               context.getMatrices().scale(0.5F, 0.5F);

               for (int l5 = 0; l5 < l * l; l5++) {
                  RegionInfo regionInfo1 = this.mapData.getRegionInfo(l5);
                  if (regionInfo1 != null) {
                     int k6 = l5 % l;
                     int l6 = l5 / l;
                     int i7 = i3 + k6 * i;
                     int j7 = j3 + l6 * i;
                     String s = String.valueOf(regionInfo1.regionId());
                     int l4 = client.textRenderer.getWidth(s);
                     byte b0 = 9;
                     int i5 = (int)((i7 + (i - l4 * 0.5F) / 2.0F) * 2.0F);
                     int j5 = (int)((j7 + (i - b0 * 0.5F) / 2.0F) * 2.0F);
                     context.drawText(client.textRenderer, s, i5, j5, -1, false);
                  }
               }

               context.getMatrices().popMatrix();
            }

            if (this.showPlayer.getValue()) {
               this.drawPlayerMarker(context, client, i3, j3, i, k1);
            }

            int i6 = k + l2 + 4;
            if (this.showCoords.getValue()) {
               i6 = this.drawCoordsPanel(context, client, j, i6, l1, k1, i2, j2);
            }

            if (this.showLegend.getValue()) {
               this.drawLegend(context, client, j, i6, l1, k1, i2);
            }
         }
      }
   }

   public void drawPlayerMarker(DrawContext context, MinecraftClient client, int var3, int var4, int var5, int var6) {
      double d0 = client.player.getX();
      double d1 = client.player.getZ();
      int[] aint = this.mapData.worldToGrid(d0, d1);
      int i = this.mapData.getMapSize();
      if (aint[0] >= 0 && aint[0] < i && aint[1] >= 0 && aint[1] < i) {
         double[] adouble = this.mapData.worldToCellPosition(d0, d1);
         int j = var3 + aint[0] * var5 + 1;
         int k = var4 + aint[1] * var5 + 1;
         int l = j + var5 - 2;
         int i1 = k + var5 - 2;
         int j1 = Math.max(j + 3, Math.min(l - 3, (int)(var3 + aint[0] * var5 + adouble[0] * var5)));
         int k1 = Math.max(k + 3, Math.min(i1 - 3, (int)(var4 + aint[1] * var5 + adouble[1] * var5)));
         context.fill(j1 - 3, k1 - 3, j1 + 3, k1 + 3, -855638017);
         context.fill(j1 - 2, k1 - 2, j1 + 2, k1 + 2, var6 | 0xFF000000);
         double d2 = Math.toRadians(-client.player.getYaw() + 90.0F);
         int l1 = Math.max(j, Math.min(l, j1 + (int)(Math.cos(d2) * 5.0)));
         int i2 = Math.max(k, Math.min(i1, k1 + (int)(Math.sin(d2) * 5.0)));
         if (l1 != j1 || i2 != k1) {
            context.fill(l1 - 1, i2 - 1, l1 + 1, i2 + 1, -1);
         }
      }
   }

   public int drawCoordsPanel(DrawContext context, MinecraftClient client, int var3, int var4, int var5, int var6, int var7, int var8) {
      double d0 = client.player.getX();
      double d1 = client.player.getZ();
      int i = this.mapData.getRegionAt(d0, d1);
      String s = String.format("X: %d  Z: %d", (int)d0, (int)d1);
      String s1 = i != -1 ? String.format("Region %d  \u2022  %s", i, this.mapData.getRegionTypeName(d0, d1)) : null;
      byte b0 = 9;
      int j = s1 != null ? 2 : 1;
      int k = Math.max(FontRenderer.INSTANCE.getWidth(s), s1 != null ? FontRenderer.INSTANCE.getWidth(s1) : 0) + 16;
      int l = 22 + j * 17 + 8;
      Render2D.drawRoundedRect(context, var3, var4, k, l, 8.0F, var5, false);
      Render2D.drawRoundedOutline(context, var3, var4, k, l, 8.0F, 1.0F, withAlphaFraction(var6, 0.45F), false);
      Render2D.G(context, var3, var4, k, 22.0F, 8.0F, 8.0F, 0.0F, 0.0F, false, lighten(var5));
      FontRenderer.INSTANCE.drawString(context, "Coordinates", var3 + 8, var4 + 6, var7);
      context.fill(var3 + 8, var4 + 22 - 1, var3 + k - 8, var4 + 22, withAlphaFraction(var6, 0.25F));
      FontRenderer.INSTANCE.drawString(context, s, var3 + 8, var4 + 22 + 4, var7);
      if (s1 != null) {
         FontRenderer.INSTANCE.drawString(context, s1, var3 + 8, var4 + 22 + 4 + 19, var6);
      }

      return var4 + l + 4;
   }

   public void drawLegend(DrawContext context, MinecraftClient client, int var3, int var4, int var5, int var6, int var7) {
      String[] astring = this.mapData.getRegionTypeNames();
      Color[] acolor = this.mapData.getRegionTypeColors();
      int i = 0;

      for (String s : astring) {
         i = Math.max(i, FontRenderer.INSTANCE.getWidth(s));
      }

      byte b0 = 8;
      byte b1 = 5;
      int i1 = b0 + b1 + i + 16;
      int j1 = 22 + astring.length * 19 + 8;
      Render2D.drawRoundedRect(context, var3, var4, i1, j1, 8.0F, var5, false);
      Render2D.drawRoundedOutline(context, var3, var4, i1, j1, 8.0F, 1.0F, withAlphaFraction(var6, 0.45F), false);
      Render2D.G(context, var3, var4, i1, 22.0F, 8.0F, 8.0F, 0.0F, 0.0F, false, lighten(var5));
      FontRenderer.INSTANCE.drawString(context, "Legend", var3 + 8, var4 + 6, var7);
      context.fill(var3 + 8, var4 + 22 - 1, var3 + i1 - 8, var4 + 22, withAlphaFraction(var6, 0.25F));

      for (int j = 0; j < astring.length; j++) {
         int k = var4 + 22 + j * 19 + 3;
         int l = var3 + 8;
         Render2D.drawRoundedRect(context, l, k, b0, b0, 3.0F, 0xFF000000 | acolor[j].getRGB() & 16777215, false);
         FontRenderer.INSTANCE.drawString(context, astring[j], l + b0 + b1, k, var7);
      }
   }

   public static int withAlphaFraction(int argb, float alpha) {
      int i = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      return argb & 16777215 | i << 24;
   }

   public static int lighten(int argb) {
      int i = argb >> 24 & 0xFF;
      int j = Math.min(255, (argb >> 16 & 0xFF) + 10);
      int k = Math.min(255, (argb >> 8 & 0xFF) + 10);
      int l = Math.min(255, (argb & 0xFF) + 10);
      return i << 24 | j << 16 | k << 8 | l;
   }
}
