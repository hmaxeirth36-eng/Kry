package com.water.gui;

import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.client.Water;
import com.water.module.modules.render.StorageESP;
import com.water.module.setting.BlockListSetting;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class BlockSelectScreen extends Screen {
   public static final int W = 320;
   public static final int H = 300;
   public static final int PAD = 10;
   public static final int HEAD_H = 34;
   public static final int FOOT_H = 36;
   public static final int TAB_H = 24;
   public static final int SRCH_H = 22;
   public static final int CELL = 22;
   public static final int GAP = 3;
   public static final int COLS = 9;
   public static final int CP_W = 140;
   public static final int CP_H = 130;
   public static final int CP_SV = 90;
   public static final int CP_HUE = 12;
   public static final int CP_GAP = 6;
   public static final int C_BD_IN = -15327184;
   public static final int C_TEXT = -2234128;
   public static final int C_TEXT_DIM = -7824982;
   public static final int C_MUTED = -12298906;
   public static final int C_RED = -2539435;
   public static final int C_GRID_BG = -16183270;
   public static final int C_CELL_BG = -15656928;
   public static final int C_CELL_HOV = -15063498;
   public static final int C_CELL_SEL = -15785936;
   public static final int C_WHITE_10 = 285212671;
   public final Screen parent;
   public final Module module;
   public final BlockListSetting setting;
   public final Map<Block, Color> colorMap;
   public final Consumer<Map<Block, Color>> colorSaveCallback;
   public final Map<Block, Color> builtinColors = new LinkedHashMap<>();
   public final Set<Block> builtinBlocks = new LinkedHashSet<>();
   public final Set<Block> sel = new LinkedHashSet<>();
   public final List<Block> all = new ArrayList<>();
   public List<Block> filtered = new ArrayList<>();
   public String search = "";
   public boolean showSel = false;
   public int scroll = 0;
   public long openNs = 0L;
   public Block cpBlock = null;
   public boolean cpBuiltin = false;
   public int cpX;
   public int cpY;
   public boolean cpDragSV = false;
   public boolean cpDragHue = false;

   public BlockSelectScreen(Screen parent, Module module, BlockListSetting setting) {
      this(parent, module, setting, new LinkedHashMap<>(), null);
   }

   public BlockSelectScreen(Screen parent, Module module, BlockListSetting setting, Map<Block, Color> map, Consumer<Map<Block, Color>> var5) {
      super(Text.literal(""));
      this.parent = parent;
      this.module = module;
      this.setting = setting;
      this.colorMap = new LinkedHashMap<>(map);
      this.colorSaveCallback = var5;
      if (module instanceof StorageESP storageESP) {
         this.builtinColors.putAll(storageESP.getBuiltinBlockColors());
         this.builtinBlocks.addAll(this.builtinColors.keySet());
      }

      this.sel.addAll(setting.getSelectedBlocks());
      this.all.addAll(setting.getAvailableBlocks());
      this.all.removeIf(var0 -> var0 == null || var0 == Blocks.AIR || new ItemStack(var0).isEmpty());
      this.all.sort(Comparator.comparing(setting::getDisplayName, String.CASE_INSENSITIVE_ORDER));
      this.refreshFilter();
   }

   public int getLeft() {
      return (this.width - 320) / 2;
   }

   public int getTop() {
      return (this.height - 300) / 2;
   }

   public int getContentX() {
      return this.getLeft() + 10;
   }

   public int getGridY() {
      return this.getTop() + 34 + 24 + 22 + 6;
   }

   public int getPanelHeight() {
      return 300;
   }

   public int getGridHeight() {
      return 172;
   }

   public int getVisibleRows() {
      return Math.max(1, this.getGridHeight() / 25);
   }

   public int getTotalRows() {
      return (int)Math.ceil(this.getDisplayedBlocks().size() / 9.0);
   }

   public int getMaxScroll() {
      return Math.max(0, this.getTotalRows() - this.getVisibleRows());
   }

   public List<Block> getDisplayedBlocks() {
      return this.showSel ? this.getSelectedAndBuiltinBlocks() : this.filtered;
   }

   public List<Block> getSelectedAndBuiltinBlocks() {
      ArrayList arrayList = new ArrayList<>(this.builtinBlocks);

      for (Block block : this.sel) {
         if (!this.builtinBlocks.contains(block)) {
            arrayList.add(block);
         }
      }

      return arrayList;
   }

   public void refreshFilter() {
      this.applyFilter(false);
   }

   public void applyFilter(boolean selectedOnly) {
      if (this.search.isBlank()) {
         this.filtered = new ArrayList<>(this.all);
      } else {
         String s = this.search.trim().toLowerCase();
         this.filtered = new ArrayList<>();

         for (Block block : this.all) {
            if (this.setting.getDisplayName(block).toLowerCase().contains(s)) {
               this.filtered.add(block);
            }
         }
      }

      if (!selectedOnly) {
         this.scroll = 0;
      } else {
         this.scroll = Math.max(0, Math.min(this.getMaxScroll(), this.scroll));
      }
   }

   public Color getBlockColor(Block block) {
      return this.builtinColors.containsKey(block) ? this.builtinColors.get(block) : this.colorMap.computeIfAbsent(block, var1x -> this.randomColor());
   }

   public void setBlockColor(Block block, Color color) {
      if (this.builtinColors.containsKey(block)) {
         this.builtinColors.put(block, color);
      } else {
         this.colorMap.put(block, color);
      }
   }

   public Color randomColor() {
      int i = Color.HSBtoRGB((float)Math.random(), 0.75F, 1.0F);
      return new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, 200);
   }

   public boolean isBlockSelected(Block block) {
      return this.builtinBlocks.contains(block) || this.sel.contains(block);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.openNs == 0L) {
         this.openNs = System.nanoTime();
      }

      float f = this.easeOutCubic(Math.min(1.0F, (float)(System.nanoTime() - this.openNs) / 1.6E8F));
      int i = this.getLeft();
      int j = this.getTop();
      int k = Water.getAccentArgb();
      int l = Water.getBackgroundArgb();
      float f1 = Water.getGuiRoundness();
      float f2 = Math.max(5.0F, f1 * 0.6F);
      float f3 = Math.max(4.0F, f1 * 0.4F);
      float f4 = Water.getGlassIntensity();
      context.fill(0, 0, this.width, this.height, this.scaleAlpha(-2013265920, f));
      Render2D.drawRoundedRect(context, i - 4, j - 4, 328.0F, 308.0F, f1 + 3.0F, this.scaleAlpha(withAlpha(k & 16777215, (int)(28.0F * f)), 1.0F), false);
      Render2D.drawRoundedRect(context, i, j, 320.0F, 300.0F, f1, this.scaleAlpha(l, f), false);
      if (f4 > 0.01F) {
         Render2D.drawRoundedRect(context, i + 1, j + 1, 318.0F, 36.0F, f1, this.scaleAlpha(withAlpha(16777215, (int)(16.0F * f4)), f), false);
         Render2D.drawRoundedOutline(context, i, j, 320.0F, 300.0F, f1, 1.0F, this.scaleAlpha(withAlpha(16777215, (int)(55.0F * f4)), f), false);
      } else {
         Render2D.drawRoundedOutline(context, i, j, 320.0F, 300.0F, f1, 1.0F, this.scaleAlpha(k & 16777215 | 1426063360, f), false);
      }

      Render2D.G(context, i, j, 320.0F, 34.0F, f1, f1, 0.0F, 0.0F, false, this.scaleAlpha(withAlpha(0, 55), f));
      context.fill(i, j + 34, i + 320, j + 34 + 1, this.scaleAlpha(-15327184, f));
      Render2D.drawRoundedRect(context, i, j + 8, 3.0F, 18.0F, 1.5F, this.scaleAlpha(k, f), false);
      String s = (this.setting.getName() + " \u2014 " + (this.module == null ? "" : this.module.getName())).toUpperCase();
      this.drawString(context, s, i + 10 + 8, j + 10, this.scaleAlpha(-2234128, f));
      int i1 = this.builtinBlocks.size() + this.sel.size();
      this.drawString(context, i1 + " selected", i + 10 + 8, j + 22, this.scaleAlpha(-7824982, f));
      int j1 = j + 34 + 4;
      short short1 = 145;
      int k1 = i + 10;
      int l1 = k1 + short1 + 10;
      this.drawBlockCell(context, k1, j1, short1, 18, "ALL", !this.showSel, this.isInside(mouseX, mouseY, k1, j1, short1, 18), f, k);
      this.drawBlockCell(
         context,
         l1,
         j1,
         short1,
         18,
         "SELECTED (" + (this.builtinBlocks.size() + this.sel.size()) + ")",
         this.showSel,
         this.isInside(mouseX, mouseY, l1, j1, short1, 18),
         f,
         k
      );
      if (!this.showSel) {
         int i2 = j + 34 + 24 + 2;
         boolean flag = !this.search.isEmpty();
         Render2D.drawRoundedRect(context, i + 10, i2, 300.0F, 20.0F, f2, this.scaleAlpha(-16183270, f), false);
         Render2D.drawRoundedOutline(context, i + 10, i2, 300.0F, 20.0F, f2, 1.0F, this.scaleAlpha(flag ? k & 16777215 | 1711276032 : -15327184, f), false);
         this.drawString(context, flag ? "\u2315  " + this.search + "_" : "\u2315  Search blocks...", i + 10 + 8, i2 + 11 - 5, this.scaleAlpha(flag ? -2234128 : -12298906, f));
      } else {
         int k5 = j + 34 + 24 + 2;
         this.drawString(context, "Right-click a block to change its color", i + 10 + 4, k5 + 11 - 5, this.scaleAlpha(-12298906, f));
      }

      List list = this.getDisplayedBlocks();
      int l5 = this.getContentX();
      int j2 = this.getGridY();
      int k2 = this.getPanelHeight();
      int l2 = this.getGridHeight();
      Render2D.drawRoundedRect(context, l5 - 3, j2 - 3, k2 + 6, l2 + 6, f2, this.scaleAlpha(-16183270, f), false);
      String s1 = null;
      int i3 = 0;
      int j3 = 0;
      int k3 = this.getVisibleRows();

      for (int l3 = 0; l3 < k3; l3++) {
         for (int i4 = 0; i4 < 9; i4++) {
            int j4 = (l3 + this.scroll) * 9 + i4;
            if (j4 >= list.size()) {
               break;
            }

            Block block = (Block)list.get(j4);
            int k4 = l5 + i4 * 25;
            int l4 = j2 + l3 * 25;
            boolean flag1 = this.isBlockSelected(block);
            boolean flag2 = this.builtinBlocks.contains(block);
            boolean flag3 = this.isInside(mouseX, mouseY, k4, l4, 22, 22);
            Render2D.drawRoundedRect(context, k4, l4, 22.0F, 22.0F, f3, this.scaleAlpha(flag1 ? -15785936 : (flag3 ? -15063498 : -15656928), f), false);
            if (flag1) {
               Color color = this.getBlockColor(block);
               int i5 = (int)(50.0F * f) << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
               Render2D.drawRoundedRect(context, k4, l4, 22.0F, 22.0F, f3, i5, false);
               int j5 = (int)(255.0F * f) << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
               Render2D.drawRoundedOutline(context, k4, l4, 22.0F, 22.0F, f3, block == this.cpBlock ? 2.0F : 1.5F, j5, false);
               Render2D.drawRoundedRect(context, k4 + 22 - 7, l4 + 22 - 7, 6.0F, 6.0F, 3.0F, this.scaleAlpha(-16777216, f), false);
               Render2D.drawRoundedRect(
                  context,
                  k4 + 22 - 6,
                  l4 + 22 - 6,
                  4.0F,
                  4.0F,
                  2.0F,
                  this.scaleAlpha(0xFF000000 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue(), f),
                  false
               );
               if (flag2) {
                  Render2D.drawRoundedRect(context, k4 + 1, l4 + 1, 5.0F, 5.0F, 2.0F, this.scaleAlpha(k & 16777215 | -1442840576, f), false);
               }
            } else if (flag3) {
               Render2D.drawRoundedOutline(context, k4, l4, 22.0F, 22.0F, f3, 1.0F, this.scaleAlpha(k & 16777215 | 1140850688, f), false);
            }

            ItemStack itemStack = new ItemStack(block);
            if (!itemStack.isEmpty()) {
               context.drawItem(itemStack, k4 + 3, l4 + 3);
            }

            if (flag3) {
               s1 = this.setting.getDisplayName(block) + (flag2 ? " [built-in]" : "");
               i3 = k4;
               j3 = l4;
            }
         }
      }

      if (s1 != null) {
         String s2 = s1 + "  [RMB: color]";
         int k6 = this.getTextWidth(s2) + 10;
         int i7 = Math.min(i3, l5 + k2 - k6);
         int k7 = j3 - 15;
         if (k7 < j2) {
            k7 = j3 + 22 + 2;
         }

         Render2D.drawRoundedRect(context, i7, k7, k6, 13.0F, f3, this.scaleAlpha(-16117736, f), false);
         Render2D.drawRoundedOutline(context, i7, k7, k6, 13.0F, f3, 1.0F, this.scaleAlpha(-15327184, f), false);
         this.drawString(context, s2, i7 + 5, k7 + 2, this.scaleAlpha(-2234128, f));
      }

      if (list.isEmpty()) {
         this.drawString(context, "No blocks", l5 + k2 / 2 - this.getTextWidth("No blocks") / 2, j2 + l2 / 2 - 5, this.scaleAlpha(-12298906, f));
      }

      if (this.getTotalRows() > this.getVisibleRows()) {
         int i6 = l5 + k2 + 2;
         float f5 = Math.max(16.0F, (float)(l2 * this.getVisibleRows()) / this.getTotalRows());
         float f6 = j2 + (l2 - f5) * ((float)this.scroll / Math.max(1, this.getMaxScroll()));
         Render2D.drawRoundedRect(context, i6, j2, 3.0F, l2, 1.5F, this.scaleAlpha(-15327184, f), false);
         Render2D.drawRoundedRect(context, i6, (int)f6, 3.0F, (int)f5, 1.5F, this.scaleAlpha(k & 16777215 | -1442840576, f), false);
      }

      int j6 = j + 300 - 36;
      context.fill(i, j6, i + 320, j6 + 1, this.scaleAlpha(-15327184, f));
      Render2D.G(context, i, j6, 320.0F, 36.0F, 0.0F, 0.0F, f1, f1, false, this.scaleAlpha(withAlpha(0, 50), f));
      int l6 = j6 + 7;
      int j7 = i + 10;
      boolean flag4 = this.isInside(mouseX, mouseY, j7, l6, 76, 22);
      Render2D.drawRoundedRect(context, j7, l6, 76.0F, 22.0F, 11.0F, this.scaleAlpha(flag4 ? 869875797 : 349782101, f), false);
      Render2D.drawRoundedOutline(context, j7, l6, 76.0F, 22.0F, 11.0F, 1.0F, this.scaleAlpha(-2539435, f * (flag4 ? 0.9F : 0.4F)), false);
      this.drawString(context, "CLEAR ALL", j7 + (76 - this.getTextWidth("CLEAR ALL")) / 2, l6 + 7, this.scaleAlpha(-2539435, f));
      int l7 = i + 320 - 10 - 66 - 10 - 66;
      boolean flag5 = this.isInside(mouseX, mouseY, l7, l6, 66, 22);
      Render2D.drawRoundedRect(context, l7, l6, 66.0F, 22.0F, 11.0F, this.scaleAlpha(flag5 ? 419430399 : 150994943, f), false);
      Render2D.drawRoundedOutline(context, l7, l6, 66.0F, 22.0F, 11.0F, 1.0F, this.scaleAlpha(-15327184, f), false);
      this.drawString(context, "CANCEL", l7 + (66 - this.getTextWidth("CANCEL")) / 2, l6 + 7, this.scaleAlpha(-7824982, f));
      int i8 = i + 320 - 10 - 66;
      boolean flag6 = this.isInside(mouseX, mouseY, i8, l6, 66, 22);
      Render2D.drawRoundedRect(context, i8, l6, 66.0F, 22.0F, 11.0F, this.scaleAlpha(flag6 ? k : k & 16777215 | 570425344, f), false);
      Render2D.drawRoundedOutline(context, i8, l6, 66.0F, 22.0F, 11.0F, 1.5F, this.scaleAlpha(k, f * (flag6 ? 1.0F : 0.5F)), false);
      this.drawString(context, "SAVE", i8 + (66 - this.getTextWidth("SAVE")) / 2, l6 + 7, this.scaleAlpha(flag6 ? -16777216 : k, f));
      if (this.cpBlock != null) {
         this.drawColorPicker(context, mouseX, mouseY, f);
      }
   }

   public void drawColorPicker(DrawContext context, int mouseX, int mouseY, float alpha) {
      int i = Math.min(this.cpX, this.getLeft() + 320 - 140 - 6);
      if (i < this.getLeft() + 4) {
         i = this.getLeft() + 4;
      }

      int j = Math.min(this.cpY, this.getTop() + 300 - 130 - 6);
      if (j < this.getTop() + 34 + 4) {
         j = this.getTop() + 34 + 4;
      }

      Color color = this.getBlockColor(this.cpBlock);
      float f = this.toHsb(color)[0];
      float f1 = this.toHsb(color)[1];
      float f2 = this.toHsb(color)[2];
      float f3 = Water.getGuiRoundness();
      int k = Water.getAccentArgb();
      Render2D.drawRoundedRect(context, i - 2, j - 2, 144.0F, 134.0F, f3, this.scaleAlpha(withAlpha(0, 80), alpha), false);
      Render2D.drawRoundedRect(context, i, j, 140.0F, 130.0F, f3, this.scaleAlpha(Water.getBackgroundArgb(), alpha), false);
      Render2D.drawRoundedOutline(context, i, j, 140.0F, 130.0F, f3, 1.0F, this.scaleAlpha(k & 16777215 | -2013265920, alpha), false);
      int l = i + 6;
      int i1 = j + 6;
      int j1 = l + 90 + 6;
      byte b0 = 12;
      float f4 = 90.0F / b0;
      float f5 = 90.0F / b0;

      for (int k1 = 0; k1 < b0; k1++) {
         float f6 = 1.0F - (float)k1 / b0;

         for (int l1 = 0; l1 < b0; l1++) {
            float f7 = (float)l1 / b0;
            context.fill(
               (int)(l + l1 * f4), (int)(i1 + k1 * f5), (int)(l + (l1 + 1) * f4), (int)(i1 + (k1 + 1) * f5), 0xFF000000 | Color.HSBtoRGB(f, f7, f6) & 16777215
            );
         }
      }

      Render2D.drawRoundedOutline(context, l, i1, 90.0F, 90.0F, 3.0F, 1.0F, this.scaleAlpha(1157627903, alpha), false);
      int j2 = l + (int)(f1 * 90.0F);
      int k2 = i1 + (int)((1.0F - f2) * 90.0F);
      Render2D.drawRoundedRect(context, j2 - 4, k2 - 4, 8.0F, 8.0F, 4.0F, this.scaleAlpha(-2013265920, alpha), false);
      Render2D.drawRoundedOutline(context, j2 - 4, k2 - 4, 8.0F, 8.0F, 4.0F, 2.0F, this.scaleAlpha(-1, alpha), false);

      for (int l2 = 0; l2 < 90; l2++) {
         float f8 = l2 / 90.0F;
         context.fill(j1, i1 + l2, j1 + 12, i1 + l2 + 1, 0xFF000000 | Color.HSBtoRGB(f8, 1.0F, 1.0F) & 16777215);
      }

      Render2D.drawRoundedOutline(context, j1, i1, 12.0F, 90.0F, 3.0F, 1.0F, this.scaleAlpha(1157627903, alpha), false);
      int i3 = i1 + (int)(f * 90.0F);
      Render2D.drawRoundedRect(context, j1 - 2, i3 - 1, 16.0F, 3.0F, 1.5F, this.scaleAlpha(-1, alpha), false);
      int j3 = i1 + 90 + 6;
      byte b1 = 52;
      int i2 = 0xFF000000 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
      Render2D.drawRoundedRect(context, l, j3, b1, 10.0F, 3.0F, this.scaleAlpha(i2, alpha), false);
      Render2D.drawRoundedRect(context, l + b1 + 4, j3, b1, 10.0F, 3.0F, this.scaleAlpha(i2, alpha), false);
      Render2D.drawRoundedOutline(context, l, j3, b1, 10.0F, 3.0F, 1.0F, this.scaleAlpha(1157627903, alpha), false);
      Render2D.drawRoundedOutline(context, l + b1 + 4, j3, b1, 10.0F, 3.0F, 1.0F, this.scaleAlpha(1157627903, alpha), false);
      this.drawString(context, this.setting.getDisplayName(this.cpBlock), i + 6, j + 130 - 14, this.scaleAlpha(-7824982, alpha));
   }

   public float[] toHsb(Color color) {
      return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
   }

   public int getColorPickerX() {
      int i = Math.min(this.cpX, this.getLeft() + 320 - 140 - 6);
      if (i < this.getLeft() + 4) {
         i = this.getLeft() + 4;
      }

      return i + 6;
   }

   public int getColorPickerY() {
      int i = Math.min(this.cpY, this.getTop() + 300 - 130 - 6);
      if (i < this.getTop() + 34 + 4) {
         i = this.getTop() + 34 + 4;
      }

      return i + 6;
   }

   public int getHueSliderX() {
      return this.getColorPickerX() + 90 + 6;
   }

   public void pickColorFromField(int mouseX, int mouseY) {
      if (this.cpBlock != null) {
         Color color = this.getBlockColor(this.cpBlock);
         float f = Math.max(0.0F, Math.min(1.0F, (mouseX - this.getColorPickerX()) / 90.0F));
         float f1 = 1.0F - Math.max(0.0F, Math.min(1.0F, (mouseY - this.getColorPickerY()) / 90.0F));
         int i = Color.HSBtoRGB(this.toHsb(color)[0], f, f1);
         this.setBlockColor(this.cpBlock, new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, color.getAlpha()));
      }
   }

   public void pickHueFromSlider(int mouseY) {
      if (this.cpBlock != null) {
         Color color = this.getBlockColor(this.cpBlock);
         float f = Math.max(0.0F, Math.min(1.0F, (mouseY - this.getColorPickerY()) / 90.0F));
         int i = Color.HSBtoRGB(f, this.toHsb(color)[1], this.toHsb(color)[2]);
         this.setBlockColor(this.cpBlock, new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, color.getAlpha()));
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      int i = (int)click.x();
      int j = (int)click.y();
      int k = click.button();
      int l = this.getLeft();
      int i1 = this.getTop();
      if (this.cpBlock != null) {
         int k3 = this.getColorPickerX();
         int l3 = this.getColorPickerY();
         int i4 = this.getHueSliderX();
         if (k == 0) {
            if (this.isInside(i, j, k3, l3, 90, 90)) {
               this.pickColorFromField(i, j);
               this.cpDragSV = true;
               return true;
            }

            if (this.isInside(i, j, i4, l3, 12, 90)) {
               this.pickHueFromSlider(j);
               this.cpDragHue = true;
               return true;
            }
         }

         int j4 = Math.min(this.cpX, l + 320 - 140 - 6);
         if (j4 < l + 4) {
            j4 = l + 4;
         }

         int k4 = Math.min(this.cpY, i1 + 300 - 130 - 6);
         if (k4 < i1 + 34 + 4) {
            k4 = i1 + 34 + 4;
         }

         if (!this.isInside(i, j, j4 - 2, k4 - 2, 144, 134)) {
            this.cpBlock = null;
            this.cpDragSV = false;
            this.cpDragHue = false;
         }

         return true;
      } else {
         int j1 = i1 + 34 + 4;
         short short1 = 145;
         int k1 = l + 10;
         int l1 = k1 + short1 + 10;
         if (this.isInside(i, j, k1, j1, short1, 18)) {
            this.showSel = false;
            this.scroll = 0;
            this.refreshFilter();
            return true;
         } else if (this.isInside(i, j, l1, j1, short1, 18)) {
            this.showSel = true;
            this.scroll = 0;
            this.refreshFilter();
            return true;
         } else if (i >= this.getContentX() && i < this.getContentX() + this.getPanelHeight() && j >= this.getGridY() && j < this.getGridY() + this.getGridHeight()) {
            List list = this.getDisplayedBlocks();
            int l4 = (i - this.getContentX()) / 25;
            int k2 = (j - this.getGridY()) / 25 + this.scroll;
            int l2 = k2 * 9 + l4;
            if (l4 < 9 && l2 >= 0 && l2 < list.size()) {
               Block block = (Block)list.get(l2);
               boolean flag = this.builtinBlocks.contains(block);
               if (k == 0 && !flag) {
                  if (this.sel.contains(block)) {
                     this.sel.remove(block);
                  } else {
                     this.sel.add(block);
                     this.getBlockColor(block);
                  }

                  this.applyFilter(true);
               } else if (k == 1 && this.isBlockSelected(block)) {
                  int i3 = this.getContentX() + l4 * 25;
                  int j3 = this.getGridY() + (k2 - this.scroll) * 25;
                  this.cpX = i3 + 22 + 4;
                  this.cpY = j3 - 4;
                  this.cpBlock = block;
                  this.cpBuiltin = flag;
                  this.cpDragSV = false;
                  this.cpDragHue = false;
               }
            }

            return true;
         } else {
            int i2 = i1 + 300 - 36;
            int j2 = i2 + 7;
            if (this.isInside(i, j, l + 10, j2, 76, 22)) {
               this.sel.clear();
               this.colorMap.clear();
               this.refreshFilter();
               return true;
            } else if (this.isInside(i, j, l + 320 - 10 - 66 - 10 - 66, j2, 66, 22)) {
               this.client.setScreen(this.parent);
               return true;
            } else if (this.isInside(i, j, l + 320 - 10 - 66, j2, 66, 22)) {
               this.saveAndApply();
               return true;
            } else {
               return super.mouseClicked(click, doubled);
            }
         }
      }
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      int i = (int)click.x();
      int j = (int)click.y();
      if (this.cpDragSV) {
         this.pickColorFromField(i, j);
         return true;
      } else if (this.cpDragHue) {
         this.pickHueFromSlider(j);
         return true;
      } else {
         return super.mouseDragged(click, offsetX, offsetY);
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.cpDragSV = false;
      this.cpDragHue = false;
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (mouseX >= this.getContentX() && mouseX < this.getContentX() + this.getPanelHeight() && mouseY >= this.getGridY() && mouseY < this.getGridY() + this.getGridHeight()) {
         this.scroll = Math.max(0, Math.min(this.getMaxScroll(), this.scroll + (verticalAmount > 0.0 ? -1 : 1)));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      if (this.cpBlock != null) {
         return true;
      } else {
         String s = input.asString();
         if (s != null && !s.isEmpty() && !this.showSel) {
            this.search = this.search + s;
            this.refreshFilter();
            return true;
         } else {
            return super.charTyped(input);
         }
      }
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (this.cpBlock != null) {
         if (input.isEscape()) {
            this.cpBlock = null;
         }

         return true;
      } else if (input.getKeycode() == 259 && !this.search.isEmpty()) {
         this.search = this.search.substring(0, this.search.length() - 1);
         this.refreshFilter();
         return true;
      } else if (input.isEscape()) {
         this.saveAndApply();
         return true;
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

   public void drawBlockCell(DrawContext context, int x, int y, int width, int height, String text, boolean selected, boolean hovered, float alpha, int accent) {
      int i = selected ? this.scaleAlpha(accent & 16777215 | 436207616, alpha) : (hovered ? this.scaleAlpha(285212671, alpha) : this.scaleAlpha(150994943, alpha));
      int j = selected ? this.scaleAlpha(accent & 16777215 | 1426063360, alpha) : this.scaleAlpha(-15327184, alpha);
      int k = selected ? this.scaleAlpha(accent, alpha) : this.scaleAlpha(-7824982, alpha);
      Render2D.drawRoundedRect(context, x, y, width, height, height / 2.0F, i, false);
      Render2D.drawRoundedOutline(context, x, y, width, height, height / 2.0F, 1.0F, j, false);
      this.drawString(context, text, x + (width - this.getTextWidth(text)) / 2, y + height / 2 - 5, k);
   }

   public void drawString(DrawContext context, String text, int x, int y, int color) {
      FontRenderer.INSTANCE.drawString(context, text, x, y, color);
   }

   public int getTextWidth(String text) {
      return FontRenderer.INSTANCE.getWidth(text);
   }

   public int scaleAlpha(int argb, float factor) {
      return Math.max(0, Math.min(255, (int)((argb >> 24 & 0xFF) * factor))) << 24 | argb & 16777215;
   }

   public static int withAlpha(int argb, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | argb & 16777215;
   }

   public float easeOutCubic(float t) {
      return 1.0F - (float)Math.pow(1.0F - Math.min(1.0F, t), 3.0);
   }

   public boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public void saveAndApply() {
      this.setting.setValue(new LinkedHashSet<>(this.sel));
      if (this.colorSaveCallback != null) {
         this.colorSaveCallback.accept(new LinkedHashMap<>(this.colorMap));
      }

      if (this.module instanceof StorageESP storageESP) {
         for (Entry entry : this.builtinColors.entrySet()) {
            storageESP.setBuiltinBlockColor((Block)entry.getKey(), (Color)entry.getValue());
         }
      }

      ModuleManager.INSTANCE.saveConfig();
      this.client.setScreen(this.parent);
   }
}
