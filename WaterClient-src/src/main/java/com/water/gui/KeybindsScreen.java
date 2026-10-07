package com.water.gui;

import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.client.Water;
import com.water.module.modules.misc.ChatMacro;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindsScreen extends Screen {
   public static final int W = 420;
   public static final int H = 380;
   public static final int PAD = 12;
   public static final int HEAD_H = 36;
   public static final int FOOT_H = 44;
   public static final int ROW_H = 52;
   public static final int ROW_GAP = 6;
   public static final int KEY_W = 72;
   public static final float R = 14.0F;
   public static final float R_SM = 8.0F;
   public static final float R_XS = 5.0F;
   public static final int C_BD_IN = -15327184;
   public static final int C_TEXT = -2234128;
   public static final int C_TEXT_DIM = -7824982;
   public static final int C_MUTED = -12298906;
   public static final int C_RED = -2539435;
   public static final int C_GREEN = -11751558;
   public static final int C_WHITE_10 = 285212671;
   public static final int C_GRID_BG = -16183270;
   public final Screen parent;
   public final ChatMacro module;
   public long openNs = 0L;
   public final List<String[]> macros = new ArrayList<>();
   public int editingText = -1;
   public int listeningKey = -1;
   public int scroll = 0;
   public int px;
   public int py;

   public KeybindsScreen(Screen parent) {
      super(Text.literal(""));
      this.parent = parent;
      this.module = this.findChatMacroModule();
      this.loadMacros();
   }

   public ChatMacro findChatMacroModule() {
      for (Module modulex : ModuleManager.INSTANCE.getModules()) {
         if (modulex instanceof ChatMacro chatMacro) {
            return chatMacro;
         }
      }

      return null;
   }

   public void loadMacros() {
      this.macros.clear();
      if (this.module == null) {
         this.macros.add(new String[]{"", "0"});
      } else {
         List list = this.module.getSettings();

         for (byte b0 = 0; b0 + 1 < list.size(); b0 += 2) {
            String s = (String)((Setting)list.get(b0)).getValue();
            String s1 = String.valueOf(((Setting)list.get(b0 + 1)).getValue());
            this.macros.add(new String[]{s, s1});
         }

         if (this.macros.isEmpty()) {
            this.macros.add(new String[]{"", "0"});
         }
      }
   }

   public void saveMacros() {
      if (this.module != null) {
         List list = this.module.getSettings();

         for (int i = 0; i < this.macros.size() && i * 2 + 1 < list.size(); i++) {
            ((Setting)list.get(i * 2)).setValue(this.macros.get(i)[0]);
            ((Setting)list.get(i * 2 + 1)).setValue(this.parseIntSafe(this.macros.get(i)[1]));
         }
      }
   }

   public int parseIntSafe(String text) {
      try {
         return Integer.parseInt(text);
      } catch (Exception exception) {
         return 0;
      }
   }

   public int getLeft() {
      return (this.width - 420) / 2;
   }

   public int getTop() {
      return (this.height - 380) / 2;
   }

   public int getListY() {
      return this.getTop() + 36 + 12;
   }

   public int getListHeight() {
      return 276;
   }

   public int getVisibleRows() {
      return Math.max(1, this.getListHeight() / 58);
   }

   public int getMaxScroll() {
      return Math.max(0, this.macros.size() - this.getVisibleRows());
   }

   @Override
   public void init() {
      this.px = this.getLeft();
      this.py = this.getTop();
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.openNs == 0L) {
         this.openNs = System.nanoTime();
      }

      float f = this.easeOutCubic(Math.min(1.0F, (float)(System.nanoTime() - this.openNs) / 1.6E8F));
      this.px = this.getLeft();
      this.py = this.getTop();
      int i = Water.getAccentArgb();
      int j = Water.getBackgroundArgb();
      float f1 = Water.getGuiRoundness();
      float f2 = Math.max(5.0F, f1 * 0.6F);
      float f3 = Math.max(4.0F, f1 * 0.4F);
      float f4 = Water.getGlassIntensity();
      Render2D.drawRoundedRect(context, this.px - 4, this.py - 4, 428.0F, 388.0F, f1 + 3.0F, this.scaleAlpha(withAlpha(i & 16777215, (int)(25.0F * f)), 1.0F), false);
      Render2D.drawRoundedRect(context, this.px, this.py, 420.0F, 380.0F, f1, this.scaleAlpha(j, f), false);
      if (f4 > 0.01F) {
         Render2D.drawRoundedRect(context, this.px + 1, this.py + 1, 418.0F, 38.0F, f1, this.scaleAlpha(withAlpha(16777215, (int)(15.0F * f4)), f), false);
         Render2D.drawRoundedOutline(context, this.px, this.py, 420.0F, 380.0F, f1, 1.0F, this.scaleAlpha(withAlpha(16777215, (int)(50.0F * f4)), f), false);
      } else {
         Render2D.drawRoundedOutline(context, this.px, this.py, 420.0F, 380.0F, f1, 1.0F, this.scaleAlpha(i & 16777215 | 1426063360, f), false);
      }

      Render2D.G(context, this.px, this.py, 420.0F, 36.0F, f1, f1, 0.0F, 0.0F, false, this.scaleAlpha(withAlpha(0, 55), f));
      context.fill(this.px, this.py + 36, this.px + 420, this.py + 36 + 1, this.scaleAlpha(-15327184, f));
      Render2D.drawRoundedRect(context, this.px, this.py + 8, 3.0F, 20.0F, 1.5F, this.scaleAlpha(i, f), false);
      this.drawString(context, "CHAT MACROS", this.px + 12 + 8, this.py + 11, this.scaleAlpha(-2234128, f));
      this.drawString(context, this.macros.size() + " macros", this.px + 12 + 8, this.py + 23, this.scaleAlpha(-7824982, f));
      int k = this.px + 420 - 12 - 60;
      int l = this.py + 9;
      boolean flag = this.isInside(mouseX, mouseY, k, l, 60, 18);
      Render2D.drawRoundedRect(context, k, l, 60.0F, 18.0F, 9.0F, this.scaleAlpha(flag ? i & 16777215 | 855638016 : i & 16777215 | 402653184, f), false);
      Render2D.drawRoundedOutline(context, k, l, 60.0F, 18.0F, 9.0F, 1.0F, this.scaleAlpha(i & 16777215 | 1711276032, f), false);
      this.drawString(context, "+ ADD", k + (60 - this.getTextWidth("+ ADD")) / 2, l + 4, this.scaleAlpha(i, f));
      int i1 = this.px + 12;
      short short1 = 396;
      int j1 = this.getListY();
      int k1 = this.getVisibleRows();
      this.scroll = Math.max(0, Math.min(this.getMaxScroll(), this.scroll));

      for (int l1 = 0; l1 < k1; l1++) {
         int i2 = l1 + this.scroll;
         if (i2 >= this.macros.size()) {
            break;
         }

         String[] astring = this.macros.get(i2);
         String s = astring[0];
         int j2 = this.parseIntSafe(astring[1]);
         int k2 = j1 + l1 * 58;
         boolean flag1 = this.editingText == i2;
         boolean flag2 = this.listeningKey == i2;
         boolean flag3 = flag1 || flag2;
         int l2 = flag3 ? i & 16777215 | 369098752 : this.scaleAlpha(419430399, f);
         int i3 = flag3 ? i & 16777215 | 1426063360 : this.scaleAlpha(587202559, f);
         Render2D.drawRoundedRect(context, i1, k2, short1, 52.0F, f2, l2, false);
         Render2D.drawRoundedOutline(context, i1, k2, short1, 52.0F, f2, 1.0F, i3, false);
         this.drawString(context, "#" + (i2 + 1), i1 + 8, k2 + 6, this.scaleAlpha(flag3 ? i : -12298906, f));
         int j3 = i1 + 8;
         int k3 = short1 - 72 - 48;
         boolean flag4 = flag1 && System.currentTimeMillis() / 500L % 2L == 0L;
         String s1 = s + (flag4 ? "|" : "");
         if (s1.isEmpty()) {
            s1 = flag1 ? "|" : "";
         }

         int l3 = flag1 ? this.scaleAlpha(i & 16777215 | 570425344, f) : this.scaleAlpha(570425344, f);
         int i4 = flag1 ? this.scaleAlpha(i & 16777215 | -2013265920, f) : this.scaleAlpha(872415231, f);
         Render2D.drawRoundedRect(context, j3 - 2, k2 + 22, k3 + 4, 20.0F, f3, l3, false);
         Render2D.drawRoundedOutline(context, j3 - 2, k2 + 22, k3 + 4, 20.0F, f3, 1.0F, i4, false);
         String s2 = "Type message or /command...";
         int j4 = s.isEmpty() && !flag1 ? this.scaleAlpha(-12298906, f) : this.scaleAlpha(-2234128, f);
         String s3 = s.isEmpty() && !flag1 ? s2 : s1;
         int k4 = k3 - 4;
         if (this.getTextWidth(s3) > k4) {
            while (s3.length() > 1 && this.getTextWidth(s3) > k4) {
               s3 = s3.substring(1);
            }
         }

         this.drawString(context, s3, j3 + 2, k2 + 28, j4);
         int l4 = i1 + short1 - 72 - 34;
         int i5 = k2 + 22;
         boolean flag5 = this.isInside(mouseX, mouseY, l4, i5, 72, 20);
         String s4 = flag2 ? "PRESS..." : (j2 <= 0 ? "NO KEY" : this.getKeyLabel(j2));
         int j5 = flag2 ? this.scaleAlpha(i & 16777215 | 1140850688, f) : (j2 > 0 ? this.scaleAlpha(i & 16777215 | 671088640, f) : this.scaleAlpha(570425344, f));
         int k5 = flag2 ? this.scaleAlpha(i, f) : (j2 > 0 ? this.scaleAlpha(i & 16777215 | 1996488704, f) : this.scaleAlpha(872415231, f));
         Render2D.drawRoundedRect(context, l4, i5, 72.0F, 20.0F, f3, j5, false);
         Render2D.drawRoundedOutline(context, l4, i5, 72.0F, 20.0F, f3, 1.0F, k5, false);
         this.drawString(context, s4, l4 + (72 - this.getTextWidth(s4)) / 2, i5 + 5, this.scaleAlpha(flag2 ? i : (j2 > 0 ? -2234128 : -12298906), f));
         int l5 = i1 + short1 - 26;
         int i6 = k2 + 22;
         boolean flag6 = this.isInside(mouseX, mouseY, l5, i6, 20, 20);
         Render2D.drawRoundedRect(context, l5, i6, 20.0F, 20.0F, f3, this.scaleAlpha(flag6 ? 1155088469 : 584663125, f), false);
         Render2D.drawRoundedOutline(context, l5, i6, 20.0F, 20.0F, f3, 1.0F, this.scaleAlpha(-2539435, f * (flag6 ? 0.9F : 0.4F)), false);
         this.drawString(context, "\u2715", l5 + (20 - this.getTextWidth("\u2715")) / 2, i6 + 5, this.scaleAlpha(-2539435, f));
      }

      if (this.macros.isEmpty()) {
         this.drawString(
            context,
            "No macros yet \u2014 click + ADD",
            this.px + 210 - this.getTextWidth("No macros yet \u2014 click + ADD") / 2,
            this.py + 190 - 5,
            this.scaleAlpha(-12298906, f)
         );
      }

      if (this.macros.size() > this.getVisibleRows()) {
         int j6 = this.px + 420 - 8;
         int l6 = this.getListY();
         int j7 = this.getListHeight();
         float f5 = (float)this.getVisibleRows() / this.macros.size();
         float f6 = Math.max(20.0F, j7 * f5);
         float f7 = l6 + (j7 - f6) * ((float)this.scroll / Math.max(1, this.getMaxScroll()));
         Render2D.drawRoundedRect(context, j6, l6, 4.0F, j7, 2.0F, this.scaleAlpha(-15327184, f), false);
         Render2D.drawRoundedRect(context, j6, (int)f7, 4.0F, (int)f6, 2.0F, this.scaleAlpha(i & 16777215 | -1442840576, f), false);
      }

      int k6 = this.py + 380 - 44;
      context.fill(this.px, k6, this.px + 420, k6 + 1, this.scaleAlpha(-15327184, f));
      Render2D.G(context, this.px, k6, 420.0F, 44.0F, 0.0F, 0.0F, f1, f1, false, this.scaleAlpha(withAlpha(0, 50), f));
      int i7 = k6 + 11;
      int k7 = this.px + 12;
      boolean flag7 = this.isInside(mouseX, mouseY, k7, i7, 80, 22);
      Render2D.drawRoundedRect(context, k7, i7, 80.0F, 22.0F, 11.0F, this.scaleAlpha(flag7 ? 419430399 : 150994943, f), false);
      Render2D.drawRoundedOutline(context, k7, i7, 80.0F, 22.0F, 11.0F, 1.0F, this.scaleAlpha(-15327184, f), false);
      this.drawString(context, "CANCEL", k7 + (80 - this.getTextWidth("CANCEL")) / 2, i7 + 7, this.scaleAlpha(-7824982, f));
      int l7 = this.px + 420 - 12 - 80;
      boolean flag8 = this.isInside(mouseX, mouseY, l7, i7, 80, 22);
      Render2D.drawRoundedRect(context, l7, i7, 80.0F, 22.0F, 11.0F, this.scaleAlpha(flag8 ? i : i & 16777215 | 570425344, f), false);
      Render2D.drawRoundedOutline(context, l7, i7, 80.0F, 22.0F, 11.0F, 1.5F, this.scaleAlpha(i, f * (flag8 ? 1.0F : 0.5F)), false);
      this.drawString(context, "SAVE", l7 + (80 - this.getTextWidth("SAVE")) / 2, i7 + 7, this.scaleAlpha(flag8 ? -16777216 : i, f));
      super.render(context, mouseX, mouseY, deltaTicks);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      int i = (int)click.x();
      int j = (int)click.y();
      int k = click.button();
      this.px = this.getLeft();
      this.py = this.getTop();
      int l = Water.getAccentArgb();
      int i1 = this.py + 380 - 44;
      int j1 = i1 + 11;
      if (this.isInside(i, j, this.px + 12, j1, 80, 22)) {
         MinecraftClient.getInstance().setScreen(this.parent);
         return true;
      } else if (this.isInside(i, j, this.px + 420 - 12 - 80, j1, 80, 22)) {
         this.saveMacros();
         MinecraftClient.getInstance().setScreen(this.parent);
         return true;
      } else {
         int k1 = this.px + 420 - 12 - 60;
         int l1 = this.py + 9;
         if (this.isInside(i, j, k1, l1, 60, 18)) {
            this.macros.add(new String[]{"", "0"});
            this.scroll = this.getMaxScroll();
            this.editingText = this.macros.size() - 1;
            this.listeningKey = -1;
            return true;
         } else {
            this.editingText = -1;
            this.listeningKey = -1;
            int i2 = this.px + 12;
            short short1 = 396;
            int j2 = this.getListY();
            int k2 = this.getVisibleRows();

            for (int l2 = 0; l2 < k2; l2++) {
               int i3 = l2 + this.scroll;
               if (i3 >= this.macros.size()) {
                  break;
               }

               int j3 = j2 + l2 * 58;
               int k3 = i2 + 8;
               int l3 = short1 - 72 - 48;
               if (this.isInside(i, j, k3 - 2, j3 + 22, l3 + 4, 20)) {
                  this.editingText = i3;
                  return true;
               }

               int i4 = i2 + short1 - 72 - 34;
               int j4 = j3 + 22;
               if (this.isInside(i, j, i4, j4, 72, 20)) {
                  this.listeningKey = i3;
                  return true;
               }

               int k4 = i2 + short1 - 26;
               int l4 = j3 + 22;
               if (this.isInside(i, j, k4, l4, 20, 20)) {
                  this.macros.remove(i3);
                  this.scroll = Math.max(0, Math.min(this.getMaxScroll(), this.scroll));
                  this.editingText = -1;
                  this.listeningKey = -1;
                  return true;
               }
            }

            return super.mouseClicked(click, doubled);
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.scroll = Math.max(0, Math.min(this.getMaxScroll(), this.scroll + (verticalAmount > 0.0 ? -1 : 1)));
      return true;
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      int i = input.getKeycode();
      if (this.listeningKey < 0) {
         if (this.editingText >= 0) {
            if (i != 256 && i != 257) {
               if (i == 259 && !this.macros.get(this.editingText)[0].isEmpty()) {
                  String s = this.macros.get(this.editingText)[0];
                  this.macros.get(this.editingText)[0] = s.substring(0, s.length() - 1);
                  return true;
               } else if (input.isPaste()) {
                  String[] astring = this.macros.get(this.editingText);
                  astring[0] = astring[0] + MinecraftClient.getInstance().keyboard.getClipboard().trim();
                  return true;
               } else {
                  return true;
               }
            } else {
               this.editingText = -1;
               return true;
            }
         } else if (i == 256) {
            MinecraftClient.getInstance().setScreen(this.parent);
            return true;
         } else {
            return super.keyPressed(input);
         }
      } else {
         if (i != 256 && i != 259) {
            this.macros.get(this.listeningKey)[1] = String.valueOf(i);
         } else {
            this.macros.get(this.listeningKey)[1] = "0";
         }

         this.listeningKey = -1;
         return true;
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      if (this.editingText >= 0) {
         String[] astring = this.macros.get(this.editingText);
         astring[0] = astring[0] + input.asString();
         return true;
      } else {
         return super.charTyped(input);
      }
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

   public String getKeyLabel(int keyCode) {
      if (keyCode <= 0) {
         return "None";
      } else {
         String s = GLFW.glfwGetKeyName(keyCode, 0);
         return s != null && !s.isBlank() ? s.toUpperCase() : ClickGuiScreen.keyCodeToName(keyCode);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }
}
