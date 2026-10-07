package com.water.gui;

import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.client.ConfigShare;
import com.water.module.modules.client.Water;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
   public static final int W = 340;
   public static final int H = 380;
   public static final int PAD = 14;
   public static final int ROW_H = 42;
   public static final int ROWS = 4;
   public static final int MAX_CFG = 5;
   public static int BG;
   public static int SURFACE;
   public static int SURFACE2;
   public static int BORDER;
   public static int ACCENT;
   public static int ACCENT2;
   public static final int TEXT = -1117441;
   public static final int TEXT2 = -7824982;
   public static final int TEXT3 = -12298906;
   public static final int GREEN = -14498466;
   public static final int GREEN_BG = -15914984;
   public static final int RED = -1096636;
   public static final int RED_BG = -13824498;
   public static final int AMBER = -680437;
   public static final int AMBER_BG = -13821184;
   public static int SEL_BG;
   public static int SEL_BD;
   public ConfigTab tab = ConfigTab.CONFIGS;
   public final List<String> configs = new ArrayList<>();
   public int sel = 0;
   public int scroll = 0;
   public boolean newOpen = false;
   public boolean importOpen = false;
   public String newName = "";
   public String importCode = "";
   public String status = "";
   public int statusCol = -7824982;
   public long statusAt = 0L;
   public int px;
   public int py;

   public static void refreshTheme() {
      int i = Water.getBackgroundArgb();
      int j = Water.getAccentArgb();
      BG = darken(i, 0.85F);
      SURFACE = darken(i, 0.95F);
      SURFACE2 = i;
      BORDER = lerpColor(i, j, 0.15F);
      ACCENT = j;
      ACCENT2 = darken(j, 0.75F);
      SEL_BG = lerpColor(i, j, 0.2F);
      SEL_BD = lerpColor(i, j, 0.55F);
   }

   public static int darken(int argb, float factor) {
      int i = Math.max(0, (int)((argb >> 16 & 0xFF) * factor));
      int j = Math.max(0, (int)((argb >> 8 & 0xFF) * factor));
      int k = Math.max(0, (int)((argb & 0xFF) * factor));
      return argb & 0xFF000000 | i << 16 | j << 8 | k;
   }

   public static int lerpColor(int from, int to, float t) {
      int i = from >> 16 & 0xFF;
      int j = from >> 8 & 0xFF;
      int k = from & 0xFF;
      int l = to >> 16 & 0xFF;
      int i1 = to >> 8 & 0xFF;
      int j1 = to & 0xFF;
      return 0xFF000000 | (int)(i + (l - i) * t) << 16 | (int)(j + (i1 - j) * t) << 8 | (int)(k + (j1 - k) * t);
   }

   public static float getPanelRadius() {
      return Water.getGuiRoundness() + 6.0F;
   }

   public static float getInnerRadius() {
      return Math.max(4.0F, Water.getGuiRoundness());
   }

   public void drawString(DrawContext context, String text, int x, int y, int color) {
      FontRenderer.INSTANCE.drawString(context, text, x, y, color);
   }

   public void drawCenteredString(DrawContext context, String text, int centerX, int y, int color) {
      int i = FontRenderer.INSTANCE.getWidth(text);
      FontRenderer.INSTANCE.drawString(context, text, centerX - i / 2, y, color);
   }

   public int getTextWidth(String text) {
      return FontRenderer.INSTANCE.getWidth(text);
   }

   public ConfigScreen() {
      super(Text.literal("Water Configs"));
   }

   @Override
   public void init() {
      this.px = (this.width - 340) / 2;
      this.py = (this.height - 380) / 2;
      this.refreshConfigList();
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      refreshTheme();
      ModuleManager.INSTANCE.getModules().stream().filter(var0 -> var0 instanceof Water).findFirst().ifPresent(var0 -> var0.onTick());
      FontRenderer.clearClip();
      Render2D.drawRoundedRect(context, this.px, this.py, 340.0F, 380.0F, getPanelRadius(), BG, false);
      Render2D.drawRoundedOutline(context, this.px, this.py, 340.0F, 380.0F, getPanelRadius(), 1.0F, BORDER, false);
      this.drawHeader(context, mouseX, mouseY);
      this.drawTabs(context, mouseX, mouseY);
      if (this.tab == ConfigTab.CONFIGS) {
         this.drawConfigList(context, mouseX, mouseY);
      } else {
         this.drawSharePanel(context, mouseX, mouseY);
      }

      this.drawFooter(context, mouseX, mouseY);
      if (!this.status.isEmpty() && System.currentTimeMillis() - this.statusAt < 3000L) {
         int i = this.getTextWidth(this.status) + 24;
         int j = this.px + 170 - i / 2;
         int k = this.py + 380 - 36;
         Render2D.drawRoundedRect(context, j, k, i, 22.0F, 6.0F, -871756268, false);
         Render2D.drawRoundedOutline(context, j, k, i, 22.0F, 6.0F, 1.0F, this.statusCol & 1442840575, false);
         this.drawCenteredString(context, this.status, this.px + 170, k + 7, this.statusCol);
      }

      if (this.newOpen) {
         this.drawNameDialog(context, mouseX, mouseY);
      }

      if (this.importOpen) {
         this.drawImportDialog(context, mouseX, mouseY);
      }
   }

   public void drawHeader(DrawContext context, int mouseX, int mouseY) {
      int i = this.py + 18;
      this.drawString(context, "WATER CONFIGS", this.px + 14, i, -1117441);
      String s = this.configs.size() + " / 5";
      int j = this.getTextWidth(s) + 16;
      Render2D.drawRoundedRect(context, this.px + 340 - 14 - j, i - 4, j, 20.0F, 5.0F, SURFACE2, false);
      this.drawCenteredString(context, s, this.px + 340 - 14 - j / 2, i, -7824982);
      boolean flag = this.isInside(mouseX, mouseY, this.px + 340 - 28, i - 4, 20, 20);
      this.drawString(context, "x", this.px + 340 - 22, i, flag ? -1117441 : -7824982);
      context.fill(this.px + 14, this.py + 46, this.px + 340 - 14, this.py + 47, BORDER);
   }

   public void drawTabs(DrawContext context, int mouseX, int mouseY) {
      int i = this.py + 56;
      String s = "MY CONFIGS";
      String s1 = "SHARE / IMPORT";
      boolean flag = this.tab == ConfigTab.CONFIGS;
      int j = this.px + 14;
      int k = this.px + 14 + 130;
      Render2D.drawRoundedRect(context, j - 6, i - 4, this.getTextWidth(s) + 12, 24.0F, 5.0F, flag ? SURFACE2 : 0, false);
      Render2D.drawRoundedRect(context, k - 6, i - 4, this.getTextWidth(s1) + 12, 24.0F, 5.0F, !flag ? SURFACE2 : 0, false);
      this.drawString(context, s, j, i + 3, flag ? -1117441 : -7824982);
      this.drawString(context, s1, k, i + 3, !flag ? -1117441 : -7824982);
      int l = flag ? j : k;
      int i1 = flag ? this.getTextWidth(s) : this.getTextWidth(s1);
      Render2D.drawRoundedRect(context, l, i + 22, i1, 3.0F, 2.0F, ACCENT, false);
      context.fill(this.px + 14, i + 28, this.px + 340 - 14, i + 29, BORDER);
   }

   public void drawConfigList(DrawContext context, int mouseX, int mouseY) {
      int i = this.py + 100;
      short short1 = 168;
      if (this.configs.isEmpty()) {
         this.drawCenteredString(context, "No configs yet \u2014 create one below", this.px + 170, i + short1 / 2 - 6, -12298906);
      } else {
         for (int j = 0; j < 4; j++) {
            int k = j + this.scroll;
            if (k >= this.configs.size()) {
               if (k == this.configs.size() && this.configs.size() < 5) {
                  int l = i + j * 42 + 4;
                  Render2D.drawRoundedRect(context, this.px + 14, l, 312.0F, 34.0F, getInnerRadius(), 587202559, false);
                  Render2D.drawRoundedOutline(context, this.px + 14, l, 312.0F, 34.0F, getInnerRadius(), 1.0F, 587202559, false);
                  this.drawCenteredString(context, "+ new config slot", this.px + 170, l + 17 - 5, -12298906);
               }
            } else {
               int j2 = i + j * 42 + 4;
               boolean flag = k == this.sel;
               boolean flag1 = this.isInside(mouseX, mouseY, this.px + 14, j2, 312, 34);
               int i1 = flag ? SEL_BG : (flag1 ? SURFACE2 : SURFACE);
               int j1 = flag ? SEL_BD : BORDER;
               Render2D.drawRoundedRect(context, this.px + 14, j2, 312.0F, 34.0F, getInnerRadius(), i1, false);
               Render2D.drawRoundedOutline(context, this.px + 14, j2, 312.0F, 34.0F, getInnerRadius(), 1.0F, j1, false);
               if (flag) {
                  Render2D.drawRoundedRect(context, this.px + 14, j2 + 8, 3.0F, 18.0F, 2.0F, ACCENT, false);
               }

               this.drawString(context, this.configs.get(k), this.px + 14 + (flag ? 14 : 10), j2 + 17 - 5, flag ? -1117441 : -7824982);
               int k1 = j2 + 17 - 10;
               int l1 = this.px + 340 - 14 - 70;
               int i2 = l1 - 80;
               boolean flag2 = this.isInside(mouseX, mouseY, i2, k1, 70, 20);
               Render2D.drawRoundedRect(context, i2, k1, 70.0F, 20.0F, 5.0F, flag2 ? -15914984 : SURFACE, false);
               Render2D.drawRoundedOutline(context, i2, k1, 70.0F, 20.0F, 5.0F, 1.0F, flag2 ? -14498466 : 872415231, false);
               this.drawCenteredString(context, "Reload", i2 + 35, k1 + 5, -14498466);
               boolean flag3 = this.isInside(mouseX, mouseY, l1, k1, 70, 20);
               Render2D.drawRoundedRect(context, l1, k1, 70.0F, 20.0F, 5.0F, flag3 ? -13824498 : SURFACE, false);
               Render2D.drawRoundedOutline(context, l1, k1, 70.0F, 20.0F, 5.0F, 1.0F, flag3 ? -1096636 : 872415231, false);
               this.drawCenteredString(context, "Delete", l1 + 35, k1 + 5, -1096636);
            }
         }
      }
   }

   public void drawSharePanel(DrawContext context, int mouseX, int mouseY) {
      ConfigShare configShare = ConfigShare.getInstance();
      boolean flag = configShare != null && (configShare.uploading || configShare.downloading);
      short short1 = 312;
      byte b0 = 36;
      int i = this.px + 14;
      int j = this.py + 105;
      j += 8;
      boolean flag1 = !flag && this.isInside(mouseX, mouseY, i, j, short1, b0);
      Render2D.drawRoundedRect(context, i, j, short1, b0, getInnerRadius(), flag1 ? ACCENT2 : SURFACE2, false);
      Render2D.drawRoundedOutline(context, i, j, short1, b0, getInnerRadius(), 1.0F, flag1 ? ACCENT : BORDER, false);
      String s = configShare != null && configShare.uploading ? "Uploading..." : "Share Config";
      this.drawCenteredString(context, s, this.px + 170, j + 11, -1117441);
      j += b0 + 8;
      boolean flag2 = !flag && this.isInside(mouseX, mouseY, i, j, short1, b0);
      Render2D.drawRoundedRect(context, i, j, short1, b0, getInnerRadius(), flag2 ? SURFACE2 : SURFACE, false);
      Render2D.drawRoundedOutline(context, i, j, short1, b0, getInnerRadius(), 1.0F, flag2 ? ACCENT : BORDER, false);
      String s1 = configShare != null && configShare.downloading ? "Importing..." : "Import Config";
      this.drawCenteredString(context, s1, this.px + 170, j + 11, flag2 ? -1117441 : -7824982);
      j += b0 + 12;
      if (configShare != null && !configShare.lastCode.isEmpty()) {
         String s2 = "Code: " + configShare.lastCode + "  (copied)";
         int k = this.getTextWidth(s2) + 24;
         int l = this.px + 170 - k / 2;
         Render2D.drawRoundedRect(context, l, j, k, 26.0F, 6.0F, SURFACE2, false);
         Render2D.drawRoundedOutline(context, l, j, k, 26.0F, 6.0F, 1.0F, ACCENT, false);
         this.drawCenteredString(context, s2, this.px + 170, j + 8, ACCENT);
      }

      if (configShare != null && !configShare.lastError.isEmpty()) {
         this.drawCenteredString(context, configShare.lastError, this.px + 170, this.py + 380 - 70, -1096636);
      }
   }

   public void drawFooter(DrawContext context, int mouseX, int mouseY) {
      int i = this.py + 380 - 54;
      context.fill(this.px + 14, i, this.px + 340 - 14, i + 1, BORDER);
      i += 14;
      if (this.tab == ConfigTab.CONFIGS) {
         boolean flag = this.configs.size() >= 5;
         boolean flag1 = !flag && this.isInside(mouseX, mouseY, this.px + 14, i, 110, 18);
         this.drawString(context, "+ New config", this.px + 14, i + 2, flag1 ? ACCENT : (flag ? -12298906 : -7824982));
         byte b0 = 90;
         int j = this.px + 340 - 14 - b0;
         boolean flag2 = this.isInside(mouseX, mouseY, j, i - 4, b0, 26);
         Render2D.drawRoundedRect(context, j, i - 4, b0, 26.0F, 6.0F, flag2 ? -13821184 : SURFACE, false);
         Render2D.drawRoundedOutline(context, j, i - 4, b0, 26.0F, 6.0F, 1.0F, flag2 ? -680437 : BORDER, false);
         this.drawCenteredString(context, "Reset all", j + b0 / 2, i + 2, -680437);
      }
   }

   public void drawNameDialog(DrawContext context, int mouseX, int mouseY) {
      short short1 = 340;
      short short2 = 130;
      int i = this.px + 170 - short1 / 2;
      int j = this.py + 190 - short2 / 2;
      Render2D.drawRoundedRect(context, i, j, short1, short2, 12.0F, BG, false);
      Render2D.drawRoundedOutline(context, i, j, short1, short2, 12.0F, 1.0F, BORDER, false);
      this.drawCenteredString(context, "New Config Name", i + short1 / 2, j + 14, -1117441);
      Render2D.drawRoundedRect(context, i + 16, j + 40, short1 - 32, 30.0F, 6.0F, SURFACE2, false);
      Render2D.drawRoundedOutline(context, i + 16, j + 40, short1 - 32, 30.0F, 6.0F, 1.0F, this.newName.isEmpty() ? BORDER : ACCENT, false);
      String s = this.newName.isEmpty() ? "Enter name..." : this.newName;
      this.drawString(context, s, i + 26, j + 50, this.newName.isEmpty() ? -12298906 : -1117441);
      int k = i + short1 / 2 - 55;
      byte b0 = 110;
      boolean flag = this.isInside(mouseX, mouseY, k, j + 88, b0, 28);
      Render2D.drawRoundedRect(context, k, j + 88, b0, 28.0F, 7.0F, flag ? ACCENT2 : SURFACE2, false);
      Render2D.drawRoundedOutline(context, k, j + 88, b0, 28.0F, 7.0F, 1.0F, flag ? ACCENT : BORDER, false);
      this.drawCenteredString(context, "Create", k + b0 / 2, j + 97, -1117441);
   }

   public void drawImportDialog(DrawContext context, int mouseX, int mouseY) {
      short short1 = 380;
      short short2 = 130;
      int i = this.px + 170 - short1 / 2;
      int j = this.py + 190 - short2 / 2;
      Render2D.drawRoundedRect(context, i, j, short1, short2, 12.0F, BG, false);
      Render2D.drawRoundedOutline(context, i, j, short1, short2, 12.0F, 1.0F, BORDER, false);
      this.drawCenteredString(context, "Enter Config Code", i + short1 / 2, j + 14, -1117441);
      Render2D.drawRoundedRect(context, i + 16, j + 40, short1 - 32, 30.0F, 6.0F, SURFACE2, false);
      Render2D.drawRoundedOutline(context, i + 16, j + 40, short1 - 32, 30.0F, 6.0F, 1.0F, this.importCode.isEmpty() ? BORDER : ACCENT, false);
      String s = this.importCode.isEmpty() ? "Paste code here (Ctrl+V)..." : this.importCode;
      this.drawString(context, s, i + 26, j + 50, this.importCode.isEmpty() ? -12298906 : -1117441);
      int k = i + short1 / 2 - 55;
      byte b0 = 110;
      boolean flag = this.isInside(mouseX, mouseY, k, j + 88, b0, 28);
      Render2D.drawRoundedRect(context, k, j + 88, b0, 28.0F, 7.0F, flag ? ACCENT2 : SURFACE2, false);
      Render2D.drawRoundedOutline(context, k, j + 88, b0, 28.0F, 7.0F, 1.0F, flag ? ACCENT : BORDER, false);
      this.drawCenteredString(context, "Import", k + b0 / 2, j + 97, -1117441);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      int i = (int)click.x();
      int j = (int)click.y();
      int k = this.px;
      int l = this.py;
      if (this.newOpen) {
         short short2 = 340;
         short short4 = 130;
         int k3 = k + 170 - short2 / 2;
         int i4 = l + 190 - short4 / 2;
         int l4 = k3 + short2 / 2 - 55;
         if (this.isInside(i, j, l4, i4 + 88, 110, 28)) {
            this.createConfig();
            return true;
         } else if (!this.isInside(i, j, k3, i4, short2, short4)) {
            this.newOpen = false;
            return true;
         } else {
            return true;
         }
      } else if (this.importOpen) {
         short short1 = 380;
         short short3 = 130;
         int j3 = k + 170 - short1 / 2;
         int l3 = l + 190 - short3 / 2;
         int k4 = j3 + short1 / 2 - 55;
         if (this.isInside(i, j, k4, l3 + 88, 110, 28)) {
            this.importConfig();
            return true;
         } else if (!this.isInside(i, j, j3, l3, short1, short3)) {
            this.importOpen = false;
            return true;
         } else {
            return true;
         }
      } else if (this.isInside(i, j, k + 340 - 28, l + 14, 20, 20)) {
         this.close();
         return true;
      } else if (this.isInside(i, j, k + 14 - 6, l + 52, this.getTextWidth("MY CONFIGS") + 12, 28)) {
         this.tab = ConfigTab.CONFIGS;
         return true;
      } else if (this.isInside(i, j, k + 14 + 124, l + 52, this.getTextWidth("SHARE / IMPORT") + 12, 28)) {
         this.tab = ConfigTab.SHARE;
         return true;
      } else {
         if (this.tab == ConfigTab.CONFIGS) {
            int i1 = l + 100;

            for (int j1 = 0; j1 < 4; j1++) {
               int k1 = j1 + this.scroll;
               if (k1 >= this.configs.size()) {
                  break;
               }

               int l1 = i1 + j1 * 42 + 4;
               int i2 = l1 + 17 - 10;
               int j2 = k + 340 - 14 - 70;
               int k2 = j2 - 80;
               if (this.isInside(i, j, k2, i2, 70, 20)) {
                  this.sel = k1;
                  this.loadSelectedConfig();
                  return true;
               }

               if (this.isInside(i, j, j2, i2, 70, 20)) {
                  this.sel = k1;
                  this.deleteSelectedConfig();
                  return true;
               }

               if (this.isInside(i, j, k + 14, l1, 312, 34)) {
                  this.sel = k1;
                  return true;
               }
            }

            int l2 = l + 380 - 54 + 14;
            boolean flag1 = this.configs.size() >= 5;
            if (!flag1 && this.isInside(i, j, k + 14, l2, 110, 18)) {
               this.newOpen = true;
               this.newName = "";
               return true;
            }

            if (flag1 && this.isInside(i, j, k + 14, l2, 110, 18)) {
               this.setStatus("Max 5 configs!", -1096636);
               return true;
            }

            byte b0 = 90;
            int j4 = k + 340 - 14 - b0;
            if (this.isInside(i, j, j4, l2 - 4, b0, 26)) {
               this.disableAllModules();
               return true;
            }
         } else {
            ConfigShare configShare = ConfigShare.getInstance();
            boolean flag = configShare != null && (configShare.uploading || configShare.downloading);
            int i3 = l + 160;
            short short5 = 312;
            byte b1 = 36;
            int i5 = k + 14;
            int j5 = l + 127;
            if (!flag && this.isInside(i, j, i5, j5, short5, b1)) {
               this.uploadConfig();
               return true;
            }

            if (!flag && this.isInside(i, j, i5, j5 + b1 + 8, short5, b1)) {
               this.importOpen = true;
               this.importCode = "";
               return true;
            }
         }

         return super.mouseClicked(click, doubled);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int i = Math.max(0, this.configs.size() - 4);
      this.scroll = (int)Math.max(0.0, Math.min((double)i, this.scroll - verticalAmount));
      return true;
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      int i = input.getKeycode();
      if (this.newOpen) {
         if (i == 256) {
            this.newOpen = false;
            return true;
         } else if (i == 257) {
            this.createConfig();
            return true;
         } else if (i == 259 && !this.newName.isEmpty()) {
            this.newName = this.newName.substring(0, this.newName.length() - 1);
            return true;
         } else {
            return true;
         }
      } else if (this.importOpen) {
         if (i == 256) {
            this.importOpen = false;
            return true;
         } else if (i == 257) {
            this.importConfig();
            return true;
         } else if (i == 259 && !this.importCode.isEmpty()) {
            this.importCode = this.importCode.substring(0, this.importCode.length() - 1);
            return true;
         } else if (input.isPaste()) {
            String s = MinecraftClient.getInstance().keyboard.getClipboard();
            if (s != null) {
               this.importCode = s.trim();
            }

            return true;
         } else {
            return true;
         }
      } else if (i == 256) {
         this.close();
         return true;
      } else {
         return super.keyPressed(input);
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      String s = input.asString();
      if (this.newOpen && this.newName.length() < 24) {
         this.newName = this.newName + s;
         return true;
      } else if (this.importOpen && this.importCode.length() < 64) {
         this.importCode = this.importCode + s;
         return true;
      } else {
         return super.charTyped(input);
      }
   }

   public void loadSelectedConfig() {
      if (!this.configs.isEmpty() && this.sel < this.configs.size()) {
         ModuleManager.INSTANCE.setActiveConfigName(this.configs.get(this.sel));
      }

      ModuleManager.INSTANCE.loadConfig();
      this.setStatus("Config loaded: " + ModuleManager.INSTANCE.getActiveConfigName(), -14498466);
   }

   public void deleteSelectedConfig() {
      if (!this.configs.isEmpty()) {
         try {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null) {
               File file1 = new File(minecraftClient.runDirectory, "water_config_" + this.configs.get(this.sel) + ".txt");
               if (file1.exists()) {
                  file1.delete();
               }
            }
         } catch (Exception exception) {
         }

         this.configs.remove(this.sel);
         if (this.sel >= this.configs.size()) {
            this.sel = Math.max(0, this.configs.size() - 1);
         }

         this.setStatus("Config deleted.", -1096636);
      }
   }

   public void disableAllModules() {
      ModuleManager.INSTANCE.getModules().forEach(var0 -> {
         if (var0.isEnabled()) {
            var0.toggle();
         }
      });
      this.setStatus("All modules disabled!", -680437);
   }

   public void uploadConfig() {
      ConfigShare configShare = ConfigShare.getInstance();
      if (configShare != null) {
         if (!this.configs.isEmpty() && this.sel < this.configs.size()) {
            ModuleManager.INSTANCE.setActiveConfigName(this.configs.get(this.sel));
         }

         ModuleManager.INSTANCE.saveConfig();
         this.setStatus("Uploading...", -680437);
         configShare.uploadConfig(() -> {
            if (configShare.lastError.isEmpty()) {
               this.setStatus("Code copied: " + configShare.lastCode, -14498466);
            } else {
               this.setStatus(configShare.lastError, -1096636);
            }
         });
      }
   }

   public void importConfig() {
      if (this.importCode.trim().isEmpty()) {
         this.setStatus("Paste a code first!", -1096636);
      } else {
         ConfigShare configShare = ConfigShare.getInstance();
         if (configShare != null) {
            this.setStatus("Loading...", -680437);
            configShare.downloadConfig(this.importCode.trim(), () -> {
               this.importOpen = false;
               if (configShare.lastError.isEmpty()) {
                  this.setStatus("Config imported!", -14498466);
               } else {
                  this.setStatus(configShare.lastError, -1096636);
               }
            });
         }
      }
   }

   public void createConfig() {
      if (this.configs.size() >= 5) {
         this.setStatus("Max 5 configs!", -1096636);
      } else if (this.newName.trim().isEmpty()) {
         this.setStatus("Enter a name first!", -1096636);
      } else {
         String s = this.newName.trim();
         ModuleManager.INSTANCE.setActiveConfigName(s);
         ModuleManager.INSTANCE.saveConfig();
         if (!this.configs.contains(s)) {
            this.configs.add(s);
         }

         this.newOpen = false;
         this.newName = "";
         this.sel = this.configs.indexOf(s);
         this.setStatus("\"" + s + "\" saved!", -14498466);
      }
   }

   public void refreshConfigList() {
      this.configs.clear();

      try {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null) {
            File[] afile = minecraftClient.runDirectory
               .listFiles(var0 -> var0.isFile() && var0.getName().startsWith("water_config_") && var0.getName().endsWith(".txt"));
            if (afile != null) {
               for (File file1 : afile) {
                  this.configs.add(file1.getName().replace("water_config_", "").replace(".txt", ""));
               }
            }
         }
      } catch (Exception exception) {
      }

      this.sel = 0;
   }

   public void setStatus(String text, int color) {
      this.status = text;
      this.statusCol = color;
      this.statusAt = System.currentTimeMillis();
   }

   public boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }
}
