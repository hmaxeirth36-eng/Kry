package com.water.gui;

import com.water.module.ModuleManager;
import com.water.module.modules.render.BlockESP;
import com.water.render.Render2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class ModuleEditorScreen extends Screen {
   public static final int W = 480;
   public static final int H = 360;
   public static final int SIDEBAR_W = 150;
   public static final int PAD = 10;
   public static final int HEADER_H = 36;
   public static final int FOOTER_H = 40;
   public static final int CELL = 24;
   public static final int CELL_GAP = 3;
   public static final int COLS = 10;
   public static final int SEARCH_H = 22;
   public static final int TAB_H = 24;
   public static final int CP_SV = 100;
   public static final int CP_HUE_W = 14;
   public static final int CP_GAP = 6;
   public static final int CP_W = 128;
   public static final int CP_H = 136;
   public static final int C_BG = -267908078;
   public static final int C_SIDEBAR = -871558889;
   public static final int C_GRID_BG = -872019441;
   public static final int C_HEADER = -301330922;
   public static final int C_CELL_BG = -15657440;
   public static final int C_CELL_HOV = -15065040;
   public static final int C_BORDER = 872415231;
   public static final int C_TEXT = -2235153;
   public static final int C_MUTED = -11905688;
   public static final int C_ACCENT = -10754352;
   public static final int C_ACCENT2 = -12860240;
   public static final int C_RED = -2076576;
   public static final int C_SEARCH_BG = -15920096;
   public final BlockESP module;
   public final Screen parent;
   public final Set<Block> selectedBlocks = new LinkedHashSet<>();
   public final Map<Block, Color> blockColors = new LinkedHashMap<>();
   public final List<Block> allBlocks = new ArrayList<>();
   public List<Block> filteredBlocks = new ArrayList<>();
   public String searchQuery = "";
   public boolean showSelected = false;
   public int scrollOffset = 0;
   public Block cpBlock = null;
   public int cpX;
   public int cpY;
   public boolean cpDragSV = false;
   public boolean cpDragHue = false;
   public boolean cpDragAlpha = false;
   public float fadeIn = 0.0F;
   public long openNanos = 0L;
   public int hoverIdx = -1;

   public ModuleEditorScreen(BlockESP blockESP, Screen parent) {
      super(Text.literal(""));
      this.module = blockESP;
      this.parent = parent;
      this.selectedBlocks.addAll(blockESP.getSelectedBlocks());
      Registries.BLOCK.forEach(var1x -> {
         if (var1x != Blocks.AIR && !new ItemStack(var1x).isEmpty()) {
            this.allBlocks.add(var1x);
         }
      });
      this.allBlocks.sort(Comparator.comparing(var0 -> {
         try {
            return var0.getName().getString();
         } catch (Exception exception) {
            return "";
         }
      }));
      Map<Block, Color> map = blockESP.getBlockColors();

      for (Block block : this.selectedBlocks) {
         this.blockColors.put(block, map.getOrDefault(block, this.randomColor()));
      }

      this.refreshFilter();
   }

   public void refreshFilter() {
      this.applyFilter(false);
   }

   public void applyFilter(boolean selectedOnly) {
      Object object = this.showSelected ? new ArrayList<>(this.selectedBlocks) : this.allBlocks;
      if (this.searchQuery.isBlank()) {
         this.filteredBlocks = new ArrayList<>((Collection<? extends Block>)object);
      } else {
         String s = this.searchQuery.trim().toLowerCase();
         this.filteredBlocks = new ArrayList<>();

         for (Block block : (Iterable<Block>)object) {
            try {
               if (block.getName().getString().toLowerCase().contains(s)) {
                  this.filteredBlocks.add(block);
               }
            } catch (Exception exception) {
            }
         }
      }

      if (!selectedOnly) {
         this.scrollOffset = 0;
      } else {
         this.scrollOffset = Math.max(0, Math.min(this.getMaxScroll(), this.scrollOffset));
      }
   }

   public int getLeft() {
      return (this.width - 480) / 2;
   }

   public int getTop() {
      return (this.height - 360) / 2;
   }

   public int getPadding() {
      return 10;
   }

   public int getGridX() {
      return this.getLeft() + 150 + 10;
   }

   public int getGridY() {
      return this.getTop() + 36 + 24 + 22 + 8;
   }

   public int getGridWidth() {
      return 310;
   }

   public int getGridHeight() {
      return 222;
   }

   public int getVisibleRows() {
      return (int)Math.floor(this.getGridHeight() / 27.0);
   }

   public int getTotalRows() {
      return (int)Math.ceil(this.filteredBlocks.size() / 10.0);
   }

   public int getMaxScroll() {
      return Math.max(0, this.getTotalRows() - this.getVisibleRows());
   }

   public Color getBlockColor(Block block) {
      return this.blockColors.computeIfAbsent(block, var1x -> {
         Map<Block, Color> map = this.module.getBlockColors();
         return map.getOrDefault(var1x, this.randomColor());
      });
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.openNanos == 0L) {
         this.openNanos = System.nanoTime();
      }

      float f = (float)(System.nanoTime() - this.openNanos) / 1.0E9F;
      this.fadeIn = Math.min(1.0F, f / 0.18F);
      float f1 = this.easeOutCubic(this.fadeIn);
      int i = this.getLeft();
      int j = this.getTop();
      Render2D.drawRoundedRect(context, i, j, 480.0F, 360.0F, 12.0F, this.scaleAlpha(-267908078, f1), false);
      Render2D.drawRoundedOutline(context, i, j, 480.0F, 360.0F, 12.0F, 1.0F, this.scaleAlpha(872415231, f1), false);
      Render2D.drawRoundedRect(context, i, j, 150.0F, 360.0F, 12.0F, this.scaleAlpha(-871558889, f1), false);
      Render2D.drawRoundedRect(context, i + 150 - 1, j, 1.0F, 360.0F, 0.0F, this.scaleAlpha(587202559, f1), false);
      Render2D.G(context, i, j, 480.0F, 36.0F, 12.0F, 12.0F, 0.0F, 0.0F, false, this.scaleAlpha(-301330922, f1));
      Render2D.drawRoundedRect(context, i, j, 3.0F, 36.0F, 1.5F, this.scaleAlpha(-10754352, f1), false);
      context.drawText(this.textRenderer, Text.literal("BLOCK ESP"), i + 10 + 8, j + 10, this.scaleAlpha(-2235153, f1), false);
      String s = this.selectedBlocks.size() + " selected";
      context.drawText(this.textRenderer, s, i + 10 + 8, j + 22, this.scaleAlpha(-11905688, f1), false);
      this.drawSettingsPanel(context, i, j, mouseX, mouseY, f1);
      int k = i + 150;
      short short1 = 330;
      int l = j + 36;
      int i1 = (short1 - 30) / 2;
      int j1 = k + 10;
      int k1 = j1 + i1 + 10;
      this.drawButton(context, j1, l + 4, i1, 16, "ALL BLOCKS", !this.showSelected, f1);
      this.drawButton(context, k1, l + 4, i1, 16, "SELECTED (" + this.selectedBlocks.size() + ")", this.showSelected, f1);
      int l1 = j + 36 + 24;
      int i2 = k + 10;
      int j2 = short1 - 20;
      Render2D.drawRoundedRect(context, i2, l1 + 2, j2, 18.0F, 6.0F, this.scaleAlpha(-15920096, f1), false);
      Render2D.drawRoundedOutline(context, i2, l1 + 2, j2, 18.0F, 6.0F, 1.0F, this.scaleAlpha(872415231, f1), false);
      String s1 = this.searchQuery.isEmpty() ? "\u2315 Search blocks..." : "\u2315 " + this.searchQuery + "_";
      int k2 = this.searchQuery.isEmpty() ? this.scaleAlpha(-11905688, f1) : this.scaleAlpha(-2235153, f1);
      context.drawText(this.textRenderer, s1, i2 + 8, l1 + 9 - 3, k2, false);
      Render2D.drawRoundedRect(context, this.getGridX() - 4, this.getGridY() - 4, this.getGridWidth() + 8, this.getGridHeight() + 8, 8.0F, this.scaleAlpha(-872019441, f1), false);
      this.drawBlockGrid(context, mouseX, mouseY, f1);
      this.drawScrollbar(context, f1);
      this.drawFooter(context, i, j, mouseX, mouseY, f1);
      if (this.cpBlock != null) {
         this.drawColorPicker(context, mouseX, mouseY, f1);
      }
   }

   public void drawSettingsPanel(DrawContext context, int x, int y, int width, int height, float alpha) {
      int i = x + 10;
      int j = y + 36 + 12;
      context.drawText(this.textRenderer, Text.literal("SETTINGS"), i, j, this.scaleAlpha(-10754352, alpha), false);
      j += 18;
      context.fill(i, j, x + 150 - 10, j + 1, this.scaleAlpha(587202559, alpha));
      j += 8;
      this.drawToggle(context, i, j, 130, "Tracers", this.module.isTracersEnabled(), alpha);
      j += 26;
      this.drawToggle(context, i, j, 130, "Notify", this.module.isNotifyEnabled(), alpha);
   }

   public void drawToggle(DrawContext context, int x, int y, int width, String text, boolean enabled, float alpha) {
      int i = x + width - 22;
      int j = enabled ? this.scaleAlpha(-10754352, alpha * 0.8F) : this.scaleAlpha(-14011323, alpha);
      int k = enabled ? i + 12 : i + 2;
      Render2D.drawRoundedRect(context, i, y + 2, 20.0F, 8.0F, 4.0F, j, false);
      Render2D.drawRoundedRect(context, k, y + 3, 6.0F, 6.0F, 3.0F, enabled ? this.scaleAlpha(-2235153, alpha) : this.scaleAlpha(-11905688, alpha), false);
      context.drawText(this.textRenderer, text, x, y + 2, this.scaleAlpha(enabled ? -2235153 : -11905688, alpha), false);
   }

   public void drawButton(DrawContext context, int x, int y, int width, int height, String text, boolean hovered, float alpha) {
      int i = hovered ? this.scaleAlpha(-10754352, alpha * 0.18F) : this.scaleAlpha(301989887, alpha);
      int j = hovered ? this.scaleAlpha(-10754352, alpha * 0.7F) : this.scaleAlpha(872415231, alpha);
      int k = hovered ? this.scaleAlpha(-10754352, alpha) : this.scaleAlpha(-11905688, alpha);
      Render2D.drawRoundedRect(context, x, y, width, height, 5.0F, i, false);
      Render2D.drawRoundedOutline(context, x, y, width, height, 5.0F, 1.0F, j, false);
      context.drawText(this.textRenderer, text, x + (width - this.textRenderer.getWidth(text)) / 2, y + height / 2 - 4, k, false);
   }

   public void drawBlockGrid(DrawContext context, int mouseX, int mouseY, float alpha) {
      int i = this.getGridX();
      int j = this.getGridY();
      int k = this.scrollOffset;
      int l = Math.min(k + this.getVisibleRows() + 1, this.getTotalRows());
      this.hoverIdx = -1;

      for (int i1 = k; i1 < l; i1++) {
         for (int j1 = 0; j1 < 10; j1++) {
            int k1 = i1 * 10 + j1;
            if (k1 >= this.filteredBlocks.size()) {
               break;
            }

            Block block = this.filteredBlocks.get(k1);
            int l1 = i + j1 * 27;
            int i2 = j + (i1 - k) * 27;
            if (i2 + 24 >= j && i2 <= j + this.getGridHeight()) {
               boolean flag = this.selectedBlocks.contains(block);
               boolean flag1 = mouseX >= l1 && mouseX < l1 + 24 && mouseY >= i2 && mouseY < i2 + 24;
               if (flag1) {
                  this.hoverIdx = k1;
               }

               int j2 = flag ? this.scaleAlpha(-15065040, alpha) : (flag1 ? this.scaleAlpha(-15065040, alpha) : this.scaleAlpha(-15657440, alpha));
               Render2D.drawRoundedRect(context, l1, i2, 24.0F, 24.0F, 5.0F, j2, false);
               if (flag) {
                  Color color = this.getBlockColor(block);
                  int k2 = 838860800 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
                  Render2D.drawRoundedRect(context, l1, i2, 24.0F, 24.0F, 5.0F, k2, false);
                  int l2 = 0xFF000000 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
                  Render2D.drawRoundedOutline(context, l1, i2, 24.0F, 24.0F, 5.0F, block == this.cpBlock ? 2.5F : 1.5F, l2, false);
               } else if (flag1) {
                  Render2D.drawRoundedOutline(context, l1, i2, 24.0F, 24.0F, 5.0F, 1.0F, this.scaleAlpha(1728053247, alpha), false);
               }

               ItemStack itemStack = new ItemStack(block);
               if (!itemStack.isEmpty()) {
                  float f = 1.0F;
                  int i3 = (24 - (int)(16.0F * f)) / 2;
                  context.getMatrices().pushMatrix();
                  context.getMatrices().translate(l1 + i3, i2 + i3);
                  context.getMatrices().scale(f, f);
                  context.drawItem(itemStack, 0, 0);
                  context.getMatrices().popMatrix();
               }

               if (flag) {
                  Color color1 = this.getBlockColor(block);
                  int j3 = 0xFF000000 | color1.getRed() << 16 | color1.getGreen() << 8 | color1.getBlue();
                  Render2D.drawRoundedRect(context, l1 + 24 - 8, i2 + 24 - 8, 6.0F, 6.0F, 3.0F, -16777216, false);
                  Render2D.drawRoundedRect(context, l1 + 24 - 7, i2 + 24 - 7, 4.0F, 4.0F, 2.0F, j3, false);
               }
            }
         }
      }
   }

   public void drawScrollbar(DrawContext context, float alpha) {
      if (this.getTotalRows() > this.getVisibleRows()) {
         int i = this.getGridX() + this.getGridWidth() + 6;
         int j = this.getGridY();
         int k = this.getGridHeight();
         float f = Math.max(20.0F, k * ((float)this.getVisibleRows() / this.getTotalRows()));
         float f1 = j + (k - f) * ((float)this.scrollOffset / Math.max(1, this.getMaxScroll()));
         Render2D.drawRoundedRect(context, i, j, 3.0F, k, 1.5F, this.scaleAlpha(587202559, alpha), false);
         Render2D.drawRoundedRect(context, i, (int)f1, 3.0F, (int)f, 1.5F, this.scaleAlpha(-10754352, alpha), false);
      }
   }

   public void drawFooter(DrawContext context, int x, int y, int width, int height, float alpha) {
      int i = y + 360 - 40;
      Render2D.G(context, x, i, 480.0F, 40.0F, 0.0F, 0.0F, 12.0F, 12.0F, false, this.scaleAlpha(-301330922, alpha));
      context.fill(x, i, x + 480, i + 1, this.scaleAlpha(587202559, alpha));
      int j = i + 7;
      int k = x + 10;
      boolean flag = this.isInside(width, height, k, j, 90, 26);
      Render2D.drawRoundedRect(context, k, j, 90.0F, 26.0F, 5.0F, flag ? this.scaleAlpha(-2076576, alpha) : this.scaleAlpha(870338656, alpha), false);
      Render2D.drawRoundedOutline(context, k, j, 90.0F, 26.0F, 5.0F, 1.0F, this.scaleAlpha(-2076576, alpha * 0.6F), false);
      this.drawCenteredString(context, "CLEAR ALL", k, j, 90, 26, this.scaleAlpha(-2235153, alpha));
      int l = x + 480 - 10 - 80 - 10 - 80;
      boolean flag1 = this.isInside(width, height, l, j, 80, 26);
      Render2D.drawRoundedRect(context, l, j, 80.0F, 26.0F, 5.0F, flag1 ? this.scaleAlpha(872415231, alpha) : this.scaleAlpha(301989887, alpha), false);
      Render2D.drawRoundedOutline(context, l, j, 80.0F, 26.0F, 5.0F, 1.0F, this.scaleAlpha(872415231, alpha), false);
      this.drawCenteredString(context, "CANCEL", l, j, 80, 26, this.scaleAlpha(-2235153, alpha));
      int i1 = x + 480 - 10 - 80;
      boolean flag2 = this.isInside(width, height, i1, j, 80, 26);
      Render2D.drawRoundedRect(context, i1, j, 80.0F, 26.0F, 5.0F, flag2 ? this.scaleAlpha(-10754352, alpha) : this.scaleAlpha(872415231, alpha * 0.8F), false);
      Render2D.drawRoundedOutline(context, i1, j, 80.0F, 26.0F, 5.0F, 1.0F, this.scaleAlpha(-10754352, alpha * 0.8F), false);
      this.drawCenteredString(context, "SAVE", i1, j, 80, 26, this.scaleAlpha(flag2 ? -16777216 : -10754352, alpha));
   }

   public void drawColorPicker(DrawContext context, int mouseX, int mouseY, float alpha) {
      int i = this.cpX;
      int j = this.cpY;
      if (i + 128 + 8 > this.getLeft() + 480) {
         i = this.getLeft() + 480 - 128 - 8;
      }

      if (j + 136 + 8 > this.getTop() + 360) {
         j = this.getTop() + 360 - 136 - 8;
      }

      Color color = this.getBlockColor(this.cpBlock);
      float f = this.toHsb(color)[0];
      float f1 = this.toHsb(color)[1];
      float f2 = this.toHsb(color)[2];
      Render2D.drawRoundedRect(context, i - 4, j - 4, 144.0F, 152.0F, 8.0F, this.scaleAlpha(-16381424, alpha), false);
      Render2D.drawRoundedRect(context, i, j, 136.0F, 144.0F, 6.0F, this.scaleAlpha(-15919840, alpha), false);
      Render2D.drawRoundedOutline(context, i, j, 136.0F, 144.0F, 6.0F, 1.0F, this.scaleAlpha(-10754352, alpha * 0.5F), false);
      int k = i + 4;
      int l = j + 4;
      int i1 = k + 100 + 6;
      byte b0 = 14;
      float f3 = 100.0F / b0;
      float f4 = 100.0F / b0;

      for (int j1 = 0; j1 < b0; j1++) {
         float f5 = 1.0F - (float)j1 / b0;

         for (int k1 = 0; k1 < b0; k1++) {
            float f6 = (float)k1 / b0;
            context.fill(
               (int)(k + k1 * f3), (int)(l + j1 * f4), (int)(k + (k1 + 1) * f3), (int)(l + (j1 + 1) * f4), 0xFF000000 | Color.HSBtoRGB(f, f6, f5) & 16777215
            );
         }
      }

      Render2D.drawRoundedOutline(context, k, l, 100.0F, 100.0F, 2.0F, 1.0F, 1157627903, false);
      int k2 = k + (int)(f1 * 100.0F);
      int l2 = l + (int)((1.0F - f2) * 100.0F);
      Render2D.drawRoundedRect(context, k2 - 4, l2 - 4, 8.0F, 8.0F, 4.0F, -2013265920, false);
      Render2D.drawRoundedOutline(context, k2 - 4, l2 - 4, 8.0F, 8.0F, 4.0F, 2.0F, -1, false);

      for (int i3 = 0; i3 < 100; i3++) {
         float f8 = i3 / 100.0F;
         context.fill(i1, l + i3, i1 + 14, l + i3 + 1, 0xFF000000 | Color.HSBtoRGB(f8, 1.0F, 1.0F) & 16777215);
      }

      Render2D.drawRoundedOutline(context, i1, l, 14.0F, 100.0F, 2.0F, 1.0F, 1157627903, false);
      int j3 = l + (int)(f * 100.0F);
      Render2D.drawRoundedRect(context, i1 - 2, j3 - 1, 18.0F, 3.0F, 1.5F, -1, false);
      int k3 = l + 100 + 6;
      byte b1 = 120;

      for (int l1 = 0; l1 < b1; l1++) {
         float f7 = (float)l1 / b1;
         int i2 = Color.HSBtoRGB(f, f1, f2) & 16777215;
         context.fill(k + l1, k3, k + l1 + 1, k3 + 8, (int)(f7 * 255.0F) << 24 | i2);
      }

      Render2D.drawRoundedOutline(context, k, k3, b1, 8.0F, 2.0F, 1.0F, 1157627903, false);
      int l3 = k + (int)(color.getAlpha() / 255.0F * b1);
      Render2D.drawRoundedRect(context, l3 - 2, k3 - 2, 4.0F, 12.0F, 2.0F, -1, false);
      int i4 = k3 + 12;
      int j2 = 0xFF000000 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
      Render2D.drawRoundedRect(context, k, i4, b1, 6.0F, 2.0F, j2, false);
      Render2D.drawRoundedOutline(context, k, i4, b1, 6.0F, 2.0F, 1.0F, 872415231, false);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      int i = (int)click.x();
      int j = (int)click.y();
      int k = click.button();
      int l = this.getLeft();
      int i1 = this.getTop();
      if (this.cpBlock != null) {
         int j5 = this.cpX;
         int k5 = this.cpY;
         if (j5 + 128 + 8 > l + 480) {
            j5 = l + 480 - 128 - 8;
         }

         if (k5 + 136 + 8 > i1 + 360) {
            k5 = i1 + 360 - 136 - 8;
         }

         int l5 = j5 + 4;
         int i6 = k5 + 4;
         int j6 = l5 + 100 + 6;
         int k6 = i6 + 100 + 6;
         byte b0 = 120;
         if (i >= l5 && i < l5 + 100 && j >= i6 && j < i6 + 100) {
            this.pickColorFromField(i, j, l5, i6);
            this.cpDragSV = true;
            return true;
         } else if (i >= j6 && i < j6 + 14 && j >= i6 && j < i6 + 100) {
            this.pickHueFromSlider(j, i6);
            this.cpDragHue = true;
            return true;
         } else if (i >= l5 && i < l5 + b0 && j >= k6 && j < k6 + 8) {
            this.pickAlphaFromSlider(i, l5, b0);
            this.cpDragAlpha = true;
            return true;
         } else {
            if (i < j5 - 4 || i > j5 + 128 + 12 || j < k5 - 4 || j > k5 + 136 + 12) {
               this.cpBlock = null;
            }

            return true;
         }
      } else {
         int j1 = l + 10;
         short short1 = 130;
         int k1 = i1 + 36 + 38;
         int l1 = i1 + 36 + 64;
         if (j >= k1 && j <= k1 + 14 && i >= j1 && i <= j1 + short1) {
            this.module.setTracersEnabled(!this.module.isTracersEnabled());
            return true;
         } else if (j >= l1 && j <= l1 + 14 && i >= j1 && i <= j1 + short1) {
            this.module.setNotifyEnabled(!this.module.isNotifyEnabled());
            return true;
         } else {
            int i2 = l + 150;
            short short2 = 330;
            int j2 = i1 + 36;
            int k2 = (short2 - 30) / 2;
            int l2 = i2 + 10;
            int i3 = l2 + k2 + 10;
            if (j >= j2 + 4 && j <= j2 + 24 - 4) {
               if (i >= l2 && i <= l2 + k2) {
                  this.showSelected = false;
                  this.refreshFilter();
                  return true;
               }

               if (i >= i3 && i <= i3 + k2) {
                  this.showSelected = true;
                  this.refreshFilter();
                  return true;
               }
            }

            int j3 = this.getGridX();
            int k3 = this.getGridY();
            if (i >= j3 && i < j3 + this.getGridWidth() && j >= k3 && j < k3 + this.getGridHeight()) {
               int l3 = (i - j3) / 27;
               int i4 = (j - k3) / 27 + this.scrollOffset;
               int j4 = i4 * 10 + l3;
               if (l3 < 10 && j4 >= 0 && j4 < this.filteredBlocks.size()) {
                  Block block = this.filteredBlocks.get(j4);
                  if (k == 0) {
                     if (this.selectedBlocks.contains(block)) {
                        this.selectedBlocks.remove(block);
                     } else {
                        this.selectedBlocks.add(block);
                        this.blockColors.putIfAbsent(block, this.randomColor());
                     }

                     this.applyFilter(true);
                  } else if (k == 1) {
                     if (!this.selectedBlocks.contains(block)) {
                        this.selectedBlocks.add(block);
                        this.blockColors.putIfAbsent(block, this.randomColor());
                        this.applyFilter(true);
                     }

                     int k7 = j3 + l3 * 27;
                     int i5 = k3 + (i4 - this.scrollOffset) * 27;
                     this.cpX = k7 + 24 + 6;
                     this.cpY = i5 - 4;
                     this.cpBlock = block;
                  }

                  return true;
               }
            }

            int l6 = i1 + 360 - 40;
            int i7 = l6 + 7;
            if (j >= i7 && j <= i7 + 26) {
               int j7 = l + 10;
               int k4 = l + 480 - 10 - 80 - 10 - 80;
               int l4 = l + 480 - 10 - 80;
               if (this.isInside(i, j, j7, i7, 90, 26)) {
                  this.selectedBlocks.clear();
                  this.blockColors.clear();
                  this.refreshFilter();
                  return true;
               }

               if (this.isInside(i, j, k4, i7, 80, 26)) {
                  this.client.setScreen(this.parent);
                  return true;
               }

               if (this.isInside(i, j, l4, i7, 80, 26)) {
                  this.saveAndClose();
                  return true;
               }
            }

            return super.mouseClicked(click, doubled);
         }
      }
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      int i = (int)click.x();
      int j = (int)click.y();
      if (this.cpBlock != null) {
         int k = this.cpX;
         int l = this.cpY;
         if (k + 128 + 8 > this.getLeft() + 480) {
            k = this.getLeft() + 480 - 128 - 8;
         }

         if (l + 136 + 8 > this.getTop() + 360) {
            l = this.getTop() + 360 - 136 - 8;
         }

         int i1 = k + 4;
         int j1 = l + 4;
         int k1 = i1 + 100 + 6;
         int l1 = j1 + 100 + 6;
         byte b0 = 120;
         if (this.cpDragSV) {
            this.pickColorFromField(i, j, i1, j1);
            return true;
         }

         if (this.cpDragHue) {
            this.pickHueFromSlider(j, j1);
            return true;
         }

         if (this.cpDragAlpha) {
            this.pickAlphaFromSlider(i, i1, b0);
            return true;
         }
      }

      return super.mouseDragged(click, offsetX, offsetY);
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.cpDragSV = false;
      this.cpDragHue = false;
      this.cpDragAlpha = false;
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int i = this.getGridX();
      int j = this.getGridY();
      if (mouseX >= i && mouseX < i + this.getGridWidth() && mouseY >= j && mouseY < j + this.getGridHeight()) {
         this.scrollOffset = Math.max(0, Math.min(this.getMaxScroll(), this.scrollOffset + (verticalAmount > 0.0 ? -1 : 1)));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      String s = input.asString();
      if (s != null && !s.isEmpty()) {
         this.searchQuery = this.searchQuery + s;
         this.refreshFilter();
         return true;
      } else {
         return super.charTyped(input);
      }
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (input.getKeycode() == 259 && !this.searchQuery.isEmpty()) {
         this.searchQuery = this.searchQuery.substring(0, this.searchQuery.length() - 1);
         this.refreshFilter();
         return true;
      } else if (input.isEscape()) {
         if (this.cpBlock != null) {
            this.cpBlock = null;
            return true;
         } else {
            this.saveAndClose();
            return true;
         }
      } else {
         return super.keyPressed(input);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   public void pickColorFromField(int mouseX, int mouseY, int fieldX, int fieldY) {
      if (this.cpBlock != null) {
         Color color = this.getBlockColor(this.cpBlock);
         float f = Math.max(0.0F, Math.min(1.0F, (mouseX - fieldX) / 100.0F));
         float f1 = 1.0F - Math.max(0.0F, Math.min(1.0F, (mouseY - fieldY) / 100.0F));
         int i = Color.HSBtoRGB(this.toHsb(color)[0], f, f1);
         this.blockColors.put(this.cpBlock, new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, color.getAlpha()));
      }
   }

   public void pickHueFromSlider(int mouseY, int sliderY) {
      if (this.cpBlock != null) {
         Color color = this.getBlockColor(this.cpBlock);
         float f = Math.max(0.0F, Math.min(1.0F, (mouseY - sliderY) / 100.0F));
         int i = Color.HSBtoRGB(f, this.toHsb(color)[1], this.toHsb(color)[2]);
         this.blockColors.put(this.cpBlock, new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, color.getAlpha()));
      }
   }

   public void pickAlphaFromSlider(int mouseX, int sliderX, int sliderWidth) {
      if (this.cpBlock != null) {
         Color color = this.getBlockColor(this.cpBlock);
         int i = (int)(Math.max(0.0F, Math.min(1.0F, (float)(mouseX - sliderX) / sliderWidth)) * 255.0F);
         this.blockColors.put(this.cpBlock, new Color(color.getRed(), color.getGreen(), color.getBlue(), i));
      }
   }

   public float[] toHsb(Color color) {
      return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
   }

   public Color randomColor() {
      int i = Color.HSBtoRGB((float)Math.random(), 0.8F, 1.0F);
      return new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, 180);
   }

   public int scaleAlpha(int argb, float factor) {
      int i = Math.max(0, Math.min(255, (int)((argb >> 24 & 0xFF) * factor)));
      return i << 24 | argb & 16777215;
   }

   public float easeOutCubic(float t) {
      return 1.0F - (float)Math.pow(1.0F - Math.min(1.0F, t), 3.0);
   }

   public boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public void drawCenteredString(DrawContext context, String text, int x, int y, int width, int height, int color) {
      context.drawText(this.textRenderer, text, x + (width - this.textRenderer.getWidth(text)) / 2, y + (height - 8) / 2, color, false);
   }

   public void saveAndClose() {
      this.module.setSelectedBlocks(this.selectedBlocks);
      this.module.setBlockColors(this.blockColors);
      ModuleManager.INSTANCE.saveConfig();
      this.client.setScreen(this.parent);
   }
}
