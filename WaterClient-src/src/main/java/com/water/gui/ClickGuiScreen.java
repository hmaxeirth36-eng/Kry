package com.water.gui;

import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.client.ConfigShare;
import com.water.module.modules.client.Water;
import com.water.module.modules.render.StorageESP;
import com.water.module.setting.BlockListSetting;
import com.water.module.setting.EntityListSetting;
import com.water.module.setting.ModeSetting;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import java.util.Map;

public class ClickGuiScreen extends Screen {
   public static final Category[] CACHED_CATEGORIES = Category.values();
   public static final int SLIDER_TRACK_COLOR_ARGB = -15196373;
   public static final int PANEL_W = 155;
   public static final int PANEL_PAD = 10;
   public static final int PANEL_HEADER_H = 22;
   public static final int PANEL_GAP = 12;
   public static final int PANEL_HEADER_SPACING = 6;
   public static final int ROW_H = 17;
   public static final int ROW_STEP = 19;
   public static final int SEARCH_H = 20;
   public static final int COLOR_PICKER_SV_SIZE = 80;
   public static final int COLOR_PICKER_HUE_W = 16;
   public static final int COLOR_PICKER_GAP = 6;
   public static final int COLOR_PICKER_PREVIEW_H = 14;
   public static final int COLOR_PICKER_BOTTOM_PAD = 6;
   public static final int COLOR_PICKER_FIELD_HEIGHT = 80;
   public static final int COLOR_PICKER_ALPHA_HEIGHT = 10;
   public static final int COLOR_PICKER_EXTRA_HEIGHT = 112;
   public static final int BLOCK_PICKER_SEARCH_H = 16;
   public static final int BLOCK_PICKER_ROW_H = 18;
   public static final int BLOCK_PICKER_VISIBLE_ROWS = 5;
   public static final int BLOCK_PICKER_GAP = 6;
   public static final int BLOCK_PICKER_CLEAR_W = 30;
   public static final int BLOCK_PICKER_BOTTOM_PAD = 6;
   public static final float BLOCK_PICKER_SCROLLBAR_W = 4.0F;
   public static final float BLOCK_PICKER_INDICATOR_SIZE = 6.0F;
   public static final float BLOCK_PICKER_TEXT_SCALE = 0.9F;
   public static final int COLOR_SCREEN_BG = -16052460;
   public static int COLOR_PANEL_BG = 1185831;
   public static final int COLOR_PANEL_OUTLINE = 0;
   public static final int COLOR_HEADER_BG = 0;
   public static final int COLOR_ROW_BG = 0;
   public static final int COLOR_ROW_HOVER = 872415231;
   public static final int COLOR_ROW_ACTIVE = 857419306;
   public static final int COLOR_TEXT = -1511950;
   public static final int COLOR_TEXT_MUTED = -6642510;
   public static int COLOR_ACCENT = -9710683;
   public static int COLOR_ACCENT_DIM = -11620474;
   public static final int COLOR_DIVIDER = 0;
   public static final int COLOR_SEARCH_OUTLINE = -14274495;
   public static final int COLOR_ROW_OUTLINE = 0;
   public static final int COLOR_KEY_BG = -15195855;
   public static final int SCROLL_STEP = 24;
   public Module listeningBind = null;
   public ActivatableModule listeningActivationBind = null;
   public Setting<String> listeningString = null;
   public boolean listeningGuiKey = false;
   public Setting<String> expandedStringListSetting = null;
   public boolean stringListAddActive = false;
   public String stringListAddBuffer = "";
   public Setting<Color> expandedColorSetting = null;
   public Setting<Color> activeColorSetting = null;
   public BlockListSetting expandedBlocksSetting = null;
   public EntityListSetting expandedMobsSetting = null;
   public SortMode colorDragMode = SortMode.NONE;
   public boolean searchActive = false;
   public boolean blockSearchActive = false;
   public boolean mobSearchActive = false;
   public String searchQuery = "";
   public String blockSearchQuery = "";
   public String mobSearchQuery = "";
   public int mobPickerScroll = 0;
   public int verticalScroll = 0;
   public int blockPickerScroll = 0;
   public float uiScale = 1.0F;
   public Setting<?> draggingNumericSetting = null;
   public Module draggingNumericModule = null;
   public int draggingNumericCatX = 0;
   public final EnumMap<Category, int[]> categoryOffsets = new EnumMap<>(Category.class);
   public Category draggingCategory = null;
   public int dragGrabOffsetX = 0;
   public int dragGrabOffsetY = 0;
   public final HashMap<String, Float> animValues = new HashMap<>();
   public long lastAnimNanos = 0L;
   public float frameDt = 0.016666668F;
   public final HashMap<String, Long> moduleOpenTime = new HashMap<>();
   public static final long MODULE_STAGGER_MS = 35L;
   public static final long MODULE_SLIDE_DURATION_MS = 220L;
   public static ClickGuiScreen INSTANCE;

   public void drawString(DrawContext context, String text, int x, int y, int color, boolean shadow) {
      FontRenderer.INSTANCE.drawString(context, text, x, y, color);
   }

   public int getTextWidth(String text) {
      return FontRenderer.INSTANCE.getWidth(text);
   }

   public String trimToWidth(String text, int maxWidth) {
      if (this.getTextWidth(text) <= maxWidth) {
         return text;
      } else {
         String s = "...";
         int i = this.getTextWidth(s);

         while (text.length() > 0 && this.getTextWidth(text) + i > maxWidth) {
            text = text.substring(0, text.length() - 1);
         }

         return text + s;
      }
   }

   public void updateFrameDelta() {
      long i = System.nanoTime();
      if (this.lastAnimNanos != 0L) {
         this.frameDt = Math.min(0.1F, (float)(i - this.lastAnimNanos) / 1.0E9F);
      }

      this.lastAnimNanos = i;
   }

   public float animateValue(String text, float target, float speed) {
      if (!Water.areAnimationsActive()) {
         this.animValues.put(text, target);
         return target;
      } else {
         float f = this.animValues.getOrDefault(text, target);
         float f1 = 1.0F - (float)Math.exp(-speed * Water.getAnimSpeed() * this.frameDt);
         float f2 = f + (target - f) * f1;
         this.animValues.put(text, f2);
         return f2;
      }
   }

   public int getModuleRowHeight(Module module) {
      int i = 19;
      if (module instanceof ActivatableModule) {
         i += 19;
      }

      if ("Config Share".equals(module.getName())) {
         i += 19;
      }

      for (Setting setting : module.getSettings()) {
         i += 19;
         if (setting instanceof BlockListSetting blockListSetting && this.expandedBlocksSetting == blockListSetting) {
            i += this.getBlockPickerHeight(blockListSetting);
         }

         if (setting instanceof EntityListSetting entityListSetting && this.expandedMobsSetting == entityListSetting) {
            i += this.getMobPickerHeight(entityListSetting);
         }

         if (setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
            i += 112;
         }

         if (this.isStringListSetting(module, setting) && this.expandedStringListSetting == setting) {
            i += this.getStringListHeight(setting);
         }
      }

      return i;
   }

   public boolean isStringListSetting(Module module, Setting<?> setting) {
      if (module == null || setting == null || !(setting.getValue() instanceof String)) {
         return false;
      } else {
         return "Friends".equalsIgnoreCase(module.getName()) && setting.matchesName("Names")
            ? true
            : "TabDetector".equalsIgnoreCase(module.getName()) && setting.matchesName("Target Players");
      }
   }

   public int getStringListHeight(Setting<?> setting) {
      return (Math.min(6, this.getStringListValues(setting).size()) + 1) * 19;
   }

   public List<String> getStringListValues(Setting<?> setting) {
      if (setting != null && setting.getValue() instanceof String) {
         String s = (String)setting.getValue();
         if (s != null && !s.isBlank()) {
            String s1 = s.replace('\n', ',').replace('\r', ',');
            ArrayList arrayList = new ArrayList();

            for (String s2 : s1.split(",")) {
               String s3 = s2 == null ? "" : s2.trim();
               if (!s3.isEmpty()) {
                  arrayList.add(s3);
               }
            }

            LinkedHashSet linkedHashSet = new LinkedHashSet();

            for (String s4 : (Iterable<String>)arrayList) {
               linkedHashSet.add(s4.toLowerCase(Locale.ROOT));
            }

            return new ArrayList<>(linkedHashSet);
         } else {
            return new ArrayList<>();
         }
      } else {
         return new ArrayList<>();
      }
   }

   public void setStringListValues(Setting<String> setting, List<String> list) {
      if (setting != null) {
         StringBuilder stringBuilder = new StringBuilder();

         for (String s : list) {
            String s1 = s == null ? "" : s.trim();
            if (!s1.isEmpty()) {
               if (!stringBuilder.isEmpty()) {
                  stringBuilder.append(", ");
               }

               stringBuilder.append(s1);
            }
         }

         setting.setValue(stringBuilder.toString());
      }
   }

   public float getUiScaleFactor() {
      int i = Water.getMenuSize();
      i = Math.max(1, Math.min(10, i));
      return 0.8F + (i - 1) * 0.06666667F;
   }

   public double toScaledX(double value) {
      return value / Math.max(1.0E-4F, this.uiScale);
   }

   public double toScaledY(double value) {
      return value / Math.max(1.0E-4F, this.uiScale);
   }

   public int getScaledWidth() {
      return Math.round(this.width / Math.max(1.0E-4F, this.uiScale));
   }

   public int getScaledHeight() {
      return Math.round(this.height / Math.max(1.0E-4F, this.uiScale));
   }

   public float getExpandProgress(Module module, String text) {
      return this.animateValue(text + "/expand", module.isExpanded() ? 1.0F : 0.0F, 20.0F);
   }

   public static int blendColors(int from, int to, float t) {
      if (t <= 0.0F) {
         return from;
      } else if (t >= 1.0F) {
         return to;
      } else {
         int i = from >>> 24 & 0xFF;
         int j = from >>> 16 & 0xFF;
         int k = from >>> 8 & 0xFF;
         int l = from & 0xFF;
         int i1 = to >>> 24 & 0xFF;
         int j1 = to >>> 16 & 0xFF;
         int k1 = to >>> 8 & 0xFF;
         int l1 = to & 0xFF;
         return (int)(i + (i1 - i) * t) << 24 | (int)(j + (j1 - j) * t) << 16 | (int)(k + (k1 - k) * t) << 8 | (int)(l + (l1 - l) * t);
      }
   }

   public int[] getCategoryOffsets(Category category) {
      return this.categoryOffsets.computeIfAbsent(category, var0 -> new int[2]);
   }

   public int getCategoryX(Category category, int columnIndex) {
      int i = CACHED_CATEGORIES.length * 155 + (CACHED_CATEGORIES.length - 1) * 12;
      int j = Math.max(10, (this.getScaledWidth() - i) / 2);
      return j + columnIndex * 167 + this.getCategoryOffsets(category)[0];
   }

   public int getCategoryY(Category category) {
      return this.getHeaderHeight() + this.verticalScroll + this.getCategoryOffsets(category)[1];
   }

   public static void open() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         minecraftClient.setScreen(new ClickGuiScreen());
      }
   }

   public ClickGuiScreen() {
      super(Text.literal("Water Menu"));
      INSTANCE = this;
   }

   @Override
   public void init() {
      super.init();
      this.moduleOpenTime.clear();
   }

   public static int withAlpha(int argb, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | argb & 16777215;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      COLOR_ACCENT = Water.getAccentArgb();
      COLOR_PANEL_BG = Water.getBackgroundArgb();
      Color color = Water.getAccentColor();
      COLOR_ACCENT_DIM = 0xFF000000 | Math.max(0, color.getRed() - 30) << 16 | Math.max(0, color.getGreen() - 35) << 8 | Math.max(0, color.getBlue() - 20);
      this.updateFrameDelta();
      this.uiScale = this.getUiScaleFactor();
      int i = Math.round(mouseX / this.uiScale);
      int j = Math.round(mouseY / this.uiScale);
      context.getMatrices().pushMatrix();
      context.getMatrices().scale(this.uiScale, this.uiScale);
      this.verticalScroll = this.clampVerticalScroll(this.verticalScroll);
      int k = Math.min(260, this.getScaledWidth() - 60);
      int l = (this.getScaledWidth() - k) / 2;
      int i1 = this.getScaledHeight() - 20 - 60;
      int j1 = this.searchActive ? COLOR_ACCENT : -14274495;
      Render2D.drawRoundedRect(context, l, i1, k, 20.0F, 8.0F, COLOR_PANEL_BG, false);
      Render2D.drawRoundedOutline(context, l, i1, k, 20.0F, 8.0F, 1.0F, j1, false);
      String s = this.searchQuery.isEmpty() ? "Search modules..." : this.searchQuery;
      int k1 = this.searchQuery.isEmpty() && !this.searchActive ? -6642510 : -1511950;
      boolean flag = System.currentTimeMillis() / 500L % 2L == 0L;
      if (this.searchActive && flag) {
         s = s + "_";
      }

      this.drawSettingLabel(context, l, i1, k, 20.0F, s, l + 8, i1 + 6, k1);
      String s1 = "Configs";
      byte b0 = 14;
      int l1 = this.getTextWidth(s1) + 18;
      int i2 = (this.getScaledWidth() - l1) / 2;
      int j2 = i1 - b0 - 6;
      boolean flag1 = i >= i2 && i <= i2 + l1 && j >= j2 && j <= j2 + b0;
      Render2D.drawRoundedRect(context, i2, j2, l1, b0, 7.0F, COLOR_PANEL_BG, false);
      Render2D.drawRoundedOutline(context, i2, j2, l1, b0, 7.0F, 1.0F, flag1 ? COLOR_ACCENT : -14274495, false);
      this.drawString(context, s1, i2 + (l1 - this.getTextWidth(s1)) / 2, j2 + 3, flag1 ? -1511950 : -6642510, false);
      Category[] acategory = CACHED_CATEGORIES;

      for (int k2 = 0; k2 < acategory.length; k2++) {
         Category category = acategory[k2];
         int l2 = this.getCategoryX(category, k2);
         int i3 = this.getCategoryY(category);
         int j3 = this.getCategoryHeight(category);
         float f = Water.getGlassIntensity();
         float f1 = Water.getAccentGlow();
         int k3 = Water.getAccentArgb();
         float f2 = Water.getPanelRadius();
         String s2 = Water.getHeaderStyle();
         if (f1 > 0.01F) {
            Render2D.drawRoundedRect(context, l2 - 3, i3 - 3, 161.0F, j3 + 6, f2 + 3.0F, withAlpha(k3 & 16777215, (int)(22.0F * f1)), false);
         }

         Render2D.drawRoundedRect(context, l2, i3, 155.0F, j3, f2, COLOR_PANEL_BG, false);
         if (f > 0.01F) {
            Render2D.drawRoundedRect(context, l2 - 1, i3 - 1, 157.0F, j3 + 2, f2 + 1.0F, withAlpha(k3 & 16777215, (int)(18.0F * f)), false);
            Render2D.drawRoundedRect(context, l2 + 1, i3 + 1, 153.0F, 26.0F, f2, withAlpha(16777215, (int)(20.0F * f)), false);
            Render2D.drawRoundedOutline(context, l2, i3, 155.0F, j3, f2, 1.0F, withAlpha(16777215, (int)(55.0F * f)), false);
         } else {
            Render2D.drawRoundedOutline(context, l2, i3, 155.0F, j3, f2, 1.0F, 0, false);
         }
         int l3 = switch (s2) {
            case "Transparent" -> 0;
            case "Gradient" -> k3 & 16777215 | 855638016;
            default -> -871887596;
         };
         if (l3 != 0) {
            Render2D.G(context, l2, i3, 155.0F, 22.0F, f2, f2, 6.0F, 6.0F, false, l3);
         }

         ItemStack itemStack = category.getIcon();
         float f16 = 0.75F;
         int i4 = l2 + 10;
         int j4 = i3 + (22 - (int)(16.0F * f16)) / 2;
         context.getMatrices().pushMatrix();
         context.getMatrices().translate(i4, j4);
         context.getMatrices().scale(f16, f16);
         context.drawItem(itemStack, 0, 0);
         context.getMatrices().popMatrix();
         int k4 = (int)(16.0F * f16) + 4;
         this.drawString(context, category.getName(), l2 + 10 + k4, i3 + 6, -1511950, false);
         Render2D.drawRoundedRect(context, l2 + 10, i3 + 22 - 2, 135.0F, 1.0F, 0.5F, 0, false);
         int l4 = i3 + j3;
         FontRenderer.setClipBottom(l4);
         int i5 = i3 + 22 + 6;
         List list = ModuleManager.INSTANCE.getModulesInCategory(category);
         int j5 = 0;
         int k5 = 0;
         long l5 = System.currentTimeMillis();

         for (Module module : (Iterable<Module>)list) {
            if (this.matchesSearch(module)) {
               String s3 = category.name() + "/" + module.getName() + "/openTime";
               if (!this.moduleOpenTime.containsKey(s3)) {
                  this.moduleOpenTime.put(s3, l5 + k5 * 35L);
               }

               k5++;
            }
         }

         for (Module module1 : (Iterable<Module>)list) {
            if (this.matchesSearch(module1)) {
               String s7 = category.name() + "/" + module1.getName() + "/openTime";
               Long olong = this.moduleOpenTime.get(s7);
               float f3 = 1.0F;
               if (olong != null) {
                  long i6 = System.currentTimeMillis() - olong;
                  if (i6 < 0L) {
                     f3 = 0.0F;
                  } else {
                     float f4 = Math.min(1.0F, (float)i6 / 220.0F);
                     f3 = 1.0F - (float)Math.pow(1.0F - f4, 3.0);
                  }
               }

               int l11 = (int)((1.0F - f3) * -14.0F);
               j5++;
               boolean flag2 = i >= l2 + 4 && i <= l2 + 155 - 4 && j >= i5 && j <= i5 + 17;
               String s8 = category.name() + "/" + module1.getName();
               float f5 = this.animateValue(s8 + "/hover", flag2 ? 1.0F : 0.0F, 14.0F);
               float f6 = this.animateValue(s8 + "/enabled", module1.isEnabled() ? 1.0F : 0.0F, 12.0F);
               int j6 = blendColors(0, 872415231, f5);
               int k6 = COLOR_ACCENT & 16777215 | (int)(180.0F * f6) << 24;
               int l6 = blendColors(j6, k6, f6);
               int i7 = blendColors(-6642510, -1511950, f5);
               int j7 = blendColors(i7, -1, f6);
               context.getMatrices().pushMatrix();
               context.getMatrices().translate(0.0F, l11);
               String s4 = Water.getRowStyle();
               float f7 = Math.max(0.0F, Water.getPanelRadius() - 3.0F);
               if (!"Minimal".equals(s4)) {
                  Render2D.drawRoundedRect(context, l2 + 4, i5, 147.0F, 17.0F, f7, this.scaleAlpha(l6, f3), false);
               }

               if ("Outlined".equals(s4) || f6 > 0.01F) {
                  Render2D.drawRoundedOutline(context, l2 + 4, i5, 147.0F, 17.0F, f7, 1.0F, f6 > 0.01F ? this.scaleAlpha(COLOR_ACCENT, f6 * f3) : this.scaleAlpha(0, 0.5F), false);
               }

               s4 = this.getBindLabel(module1);
               if (!s4.isEmpty()) {
                  int i12 = this.getTextWidth(s4) + 8;
                  int k7 = l2 + 155 - 10 - i12;
                  int l7 = i5 + 3;
                  int i8 = COLOR_ACCENT & 16777215 | 1140850688;
                  Render2D.drawRoundedRect(context, k7, l7, i12, 10.0F, 4.0F, i8, false);
                  Render2D.drawRoundedOutline(context, k7, l7, i12, 10.0F, 4.0F, 1.0F, COLOR_ACCENT & 16777215 | 1711276032, false);
                  this.drawString(context, s4, k7 + 4, l7 + 2, COLOR_ACCENT, false);
               }

               this.drawString(context, module1.getName(), l2 + 10 + 4, i5 + 4, this.scaleAlpha(j7, f3), false);
               context.getMatrices().popMatrix();
               i5 += 19;
               f7 = this.getExpandProgress(module1, s8);
               int j12 = this.getModuleRowHeight(module1);
               int k12 = Math.round(f7 * j12);
               if (f7 > 0.001F) {
                  int l12 = i5;
                  float f8 = this.easeOutCubic(f7);
                  float f9 = -(1.0F - f8) * 4.0F;
                  boolean flagListening = this.listeningBind == module1;
                  float f10 = this.clamp01((k12 - (i5 - i5)) / 17.0F);
                  float f11 = f8 * f10;
                  if (!this.animValues.containsKey(s8 + "/stagger/bind")) {
                     this.animValues.put(s8 + "/stagger/bind", -10.0F);
                  }

                  float f12 = this.animateValue(s8 + "/stagger/bind", module1.isExpanded() ? 0.0F : -10.0F, 20.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().translate(0.0F, f9 + f12);
                  Render2D.drawRoundedRect(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), this.scaleAlpha(-15196373, f11), false);
                  Render2D.drawRoundedOutline(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), 1.0F, this.scaleAlpha(0, f11), false);
                  this.drawString(context, "Bind", l2 + 10, i5 + 4, this.scaleAlpha(-6642510, f11), false);
                  String s5 = flagListening ? "..." : (module1.getBind() == 0 ? "None" : this.getKeyName(module1.getBind()));
                  int k8 = this.getTextWidth(s5) + 10;
                  int l8 = l2 + 155 - 10 - k8;
                  int i9 = i5 + 3;
                  int j9 = flagListening ? this.scaleAlpha(COLOR_ACCENT & 16777215 | -2013265920, f11) : this.scaleAlpha(COLOR_ACCENT & 16777215 | 855638016, f11);
                  Render2D.drawRoundedRect(context, l8, i9, k8, 10.0F, Math.max(2.0F, Water.getPanelRadius() * 0.5F), j9, false);
                  Render2D.drawRoundedOutline(context, l8, i9, k8, 10.0F, Math.max(2.0F, Water.getPanelRadius() * 0.5F), 1.0F, this.scaleAlpha(COLOR_ACCENT & 16777215 | 1711276032, f11), false);
                  this.drawString(context, s5, l8 + 5, i9 + 2, this.scaleAlpha(COLOR_ACCENT, f11), false);
                  context.getMatrices().popMatrix();
                  i5 += 19;
                  if (module1 instanceof ActivatableModule activatableModule) {
                     boolean flag4 = this.listeningActivationBind == activatableModule;
                     f11 = this.clamp01((k12 - (i5 - i5)) / 17.0F);
                     f12 = f8 * f11;
                     if (!this.animValues.containsKey(s8 + "/stagger/act")) {
                        this.animValues.put(s8 + "/stagger/act", -10.0F);
                     }

                     float f18 = this.animateValue(s8 + "/stagger/act", module1.isExpanded() ? 0.0F : -10.0F, 18.0F);
                     context.getMatrices().pushMatrix();
                     context.getMatrices().translate(0.0F, f9 + f18);
                     Render2D.drawRoundedRect(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), this.scaleAlpha(-15196373, f12), false);
                     Render2D.drawRoundedOutline(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), 1.0F, this.scaleAlpha(0, f12), false);
                     this.drawString(context, "Activation", l2 + 10, i5 + 4, this.scaleAlpha(-6642510, f12), false);
                     String s9 = flag4 ? "..." : (activatableModule.getActivationKey() == 0 ? "None" : this.getKeyName(activatableModule.getActivationKey()));
                     l8 = this.getTextWidth(s9) + 10;
                     i9 = l2 + 155 - 10 - l8;
                     j9 = i5 + 3;
                     int k9 = flag4 ? this.scaleAlpha(COLOR_ACCENT & 16777215 | -2013265920, f12) : this.scaleAlpha(COLOR_ACCENT & 16777215 | 855638016, f12);
                     Render2D.drawRoundedRect(context, i9, j9, l8, 10.0F, Math.max(2.0F, Water.getPanelRadius() * 0.5F), k9, false);
                     Render2D.drawRoundedOutline(context, i9, j9, l8, 10.0F, Math.max(2.0F, Water.getPanelRadius() * 0.5F), 1.0F, this.scaleAlpha(COLOR_ACCENT & 16777215 | 1711276032, f12), false);
                     this.drawString(context, s9, i9 + 5, j9 + 2, this.scaleAlpha(COLOR_ACCENT, f12), false);
                     context.getMatrices().popMatrix();
                     i5 += 19;
                  }

                  if ("Config Share".equals(module1.getName())) {
                     float f17 = this.clamp01((k12 - (i5 - i5)) / 17.0F);
                     f10 = f8 * f17;
                     boolean flag5 = this.isInside(i, j, l2 + 4, i5, 147.0F, 17.0F);
                     int i13 = flag5 ? this.scaleAlpha(COLOR_ACCENT, f10) : this.scaleAlpha(-15722464, f10);
                     context.getMatrices().pushMatrix();
                     context.getMatrices().translate(0.0F, f9);
                     Render2D.drawRoundedRect(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), i13, false);
                     Render2D.drawRoundedOutline(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), 1.0F, this.scaleAlpha(COLOR_ACCENT, f10 * 0.6F), false);
                     this.drawString(context, "Open Config Manager", l2 + 10, i5 + 4, this.scaleAlpha(-1, f10), false);
                     context.getMatrices().popMatrix();
                     i5 += 19;
                  }

                  int j8 = 0;

                  for (Setting setting : module1.getSettings()) {
                     f12 = this.clamp01((k12 - (i5 - l12)) / 17.0F);
                     float f19 = f8 * f12;
                     String s10 = s8 + "/stagger/" + j8;
                     float f20 = module1.isExpanded() ? 0.0F : -8.0F;
                     if (!this.animValues.containsKey(s10)) {
                        this.animValues.put(s10, -8.0F);
                     }

                     float f21 = this.animateValue(s10, f20, Math.max(8.0F, 18.0F - j8 * 1.5F));
                     j8++;
                     context.getMatrices().pushMatrix();
                     context.getMatrices().translate(0.0F, f9 + f21);
                     Render2D.drawRoundedRect(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), this.scaleAlpha(0, f19), false);
                     Render2D.drawRoundedOutline(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), 1.0F, this.scaleAlpha(0, f19), false);
                     Object object = setting.getValue();
                     if (setting instanceof ModeSetting modeSetting) {
                        this.drawModeRow(context, modeSetting, l2 + 4, 147, i5, f19);
                     } else if (object instanceof Boolean) {
                        boolean flag3 = (Boolean)object;
                        int l9 = l2 + 155 - 10 - 20;
                        int i10 = i5 + 4;
                        String s6 = System.identityHashCode(setting) + "/tog";
                        float f13 = this.animateValue(s6, flag3 ? 1.0F : 0.0F, 16.0F);
                        Render2D.drawRoundedRect(context, l9, i10, 20.0F, 8.0F, 4.0F, this.scaleAlpha(blendColors(-13682875, COLOR_ACCENT_DIM, f13), f19), false);
                        Render2D.drawRoundedRect(context, l9 + 2 + Math.round(10.0F * f13), i10 + 1, 6.0F, 6.0F, 3.0F, this.scaleAlpha(blendColors(-1511950, COLOR_ACCENT, f13), f19), false);
                        this.drawString(context, setting.getName(), l2 + 10, i5 + 4, this.scaleAlpha(blendColors(-6642510, COLOR_ACCENT, f13), f19), false);
                     } else if (!(object instanceof Float) && !(object instanceof Double) && !(object instanceof Integer)) {
                        if (object instanceof String) {
                           if ("GUI Key".equals(setting.getName())) {
                              String s14 = this.listeningGuiKey ? "GUI Key: ..." : "GUI Key: " + Water.getGuiKeyName();
                              float f26 = MinecraftClient.getInstance().textRenderer.getWidth(s14) + 10;
                              float f28 = l2 + 155 - 4 - f26;
                              float f30 = i5 + 3.5F;
                              Render2D.drawRoundedRect(context, f28, f30, f26, 10.0F, 4.0F, COLOR_ACCENT & 16777215 | 1140850688, false);
                              this.drawString(context, "GUI Key", l2 + 10, i5 + 4, this.scaleAlpha(-1511950, f19), false);
                              this.drawString(
                                 context, this.listeningGuiKey ? "..." : Water.getGuiKeyName(), (int)(f28 + 5.0F), (int)(f30 + 1.0F), this.scaleAlpha(COLOR_ACCENT, f19), false
                              );
                           } else {
                              String s11;
                              if (this.isStringListSetting(module1, setting)) {
                                 int j13 = this.getStringListValues(setting).size();
                                 s11 = setting.getName() + ": " + j13 + " entries";
                                 if (this.expandedStringListSetting == setting) {
                                    s11 = s11 + " (edit)";
                                 }
                              } else {
                                 String s12 = this.maskSensitiveValue(module1, setting, (String)object);
                                 s11 = setting.getName() + ": " + s12;
                                 if (this.listeningString == setting) {
                                    s11 = s11 + "_";
                                 }
                              }

                              this.drawSettingLabel(context, l2 + 4, i5, 147.0F, 17.0F, s11, l2 + 10, i5 + 4, this.scaleAlpha(-1511950, f19));
                           }
                        } else if (setting instanceof BlockListSetting blockListSetting) {
                           this.drawBlockListRow(context, blockListSetting, l2 + 4, 147.0F, i5, 17, f19);
                        } else if (setting instanceof EntityListSetting entityListSetting) {
                           this.drawMobListRow(context, entityListSetting, l2 + 4, 147.0F, i5, 17, f19);
                        } else if (object instanceof Color color1) {
                           boolean flag6 = i >= l2 + 4 && i <= l2 + 155 - 4 && j >= i5 && j <= i5 + 17;
                           this.drawString(context, setting.getName(), l2 + 10, i5 + 4, this.scaleAlpha(-1511950, f19), false);
                           this.drawColorRow(context, setting, l2 + 4, 147.0F, i5, 17, flag6 ? 1.0F : 0.0F, this.expandedColorSetting == setting ? 1.0F : 0.0F, f19);
                        }
                     } else {
                        float f25;
                        float f27;
                        float f29;
                        String s13;
                        if (object instanceof Integer integer && setting.getMin() instanceof Integer && setting.getMax() instanceof Integer) {
                           f25 = integer.intValue();
                           f29 = ((Integer)setting.getMin()).intValue();
                           f27 = ((Integer)setting.getMax()).intValue();
                           s13 = Integer.toString(integer);
                        } else {
                           f25 = object instanceof Float ? (Float)object : (float)((Double)object).doubleValue();
                           f27 = setting.getMax() instanceof Float ? (Float)setting.getMax() : (float)((Double)setting.getMax()).doubleValue();
                           f29 = setting.getMin() instanceof Float ? (Float)setting.getMin() : (float)((Double)setting.getMin()).doubleValue();
                           if (this.isCompactModule(module1)) {
                              s13 = String.format("%.1f", f25);
                           } else {
                              f25 = Math.round(f25);
                              s13 = Integer.toString(Math.round(f25));
                           }
                        }

                        float f31 = (f25 - f29) / (f27 - f29);
                        int j10 = l2 + 10;
                        int k10 = i5 + 11;
                        short short1 = 135;
                        int l10 = (int)(short1 * Math.max(0.0F, Math.min(1.0F, f31)));
                        Render2D.drawRoundedRect(context, j10, k10, short1, 3.0F, 1.5F, this.scaleAlpha(-14208703, f19), false);
                        Render2D.drawRoundedRect(context, j10, k10, l10, 3.0F, 1.5F, this.scaleAlpha(COLOR_ACCENT_DIM, f19), false);
                        float f14 = j10 + l10 - 3.0F;
                        float f15 = k10 + 1.5F - 3.0F;
                        Render2D.drawRoundedRect(context, f14, f15, 6.0F, 6.0F, 3.0F, this.scaleAlpha(COLOR_ACCENT, f19), false);
                        Render2D.drawRoundedOutline(context, f14, f15, 6.0F, 6.0F, 3.0F, 1.0F, this.scaleAlpha(-1711276033, f19), false);
                        this.drawString(context, setting.getName() + ": " + s13, l2 + 10, i5 + 2, this.scaleAlpha(-1511950, f19), false);
                     }

                     context.getMatrices().popMatrix();
                     i5 += 19;
                     if (this.isStringListSetting(module1, setting) && this.expandedStringListSetting == setting) {
                        float f22 = this.clamp01((k12 - (i5 - l12)) / 17.0F);
                        if (f8 * f22 > 0.01F) {
                           context.getMatrices().pushMatrix();
                           context.getMatrices().translate(0.0F, f9);
                           this.drawStringListRows(context, setting, l2 + 4, i5, 147, f8 * f22);
                           context.getMatrices().popMatrix();
                        }

                        i5 += this.getStringListHeight(setting);
                     }

                     if (setting instanceof BlockListSetting blockListSetting1 && this.expandedBlocksSetting == blockListSetting1) {
                        float f23 = f8 * this.clamp01((k12 - (i5 - l12)) / 17.0F);
                        context.getMatrices().pushMatrix();
                        context.getMatrices().translate(0.0F, f9);
                        if (f23 > 0.01F) {
                           this.drawBlockPicker(context, blockListSetting1, l2 + 4, 147.0F, i5, mouseX, mouseY);
                        }

                        context.getMatrices().popMatrix();
                        i5 += this.getBlockPickerHeight(blockListSetting1);
                     }

                     if (setting instanceof EntityListSetting entityListSetting1 && this.expandedMobsSetting == entityListSetting1) {
                        float f24 = f8 * this.clamp01((k12 - (i5 - l12)) / 17.0F);
                        context.getMatrices().pushMatrix();
                        context.getMatrices().translate(0.0F, f9);
                        if (f24 > 0.01F) {
                           this.drawMobPicker(context, entityListSetting1, l2 + 4, 147.0F, i5, mouseX, mouseY);
                        }

                        context.getMatrices().popMatrix();
                        i5 += this.getMobPickerHeight(entityListSetting1);
                     }

                     if (setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
                        i5 += 112;
                     }
                  }

                  i5 = l12 + k12;
               }
            }
         }

         if (j5 == 0) {
            Render2D.drawRoundedRect(context, l2 + 4, i5, 147.0F, 17.0F, Math.max(0.0F, Water.getPanelRadius() - 3.0F), 0, false);
            this.drawString(context, "No results", l2 + 10, i5 + 4, -6642510, false);
         }

         FontRenderer.clearClip();
      }

      context.getMatrices().popMatrix();
      if (this.listeningBind != null) {
         for (int i11 = 32; i11 <= 348; i11++) {
            if (i11 != 256 && i11 != 259 && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), i11) == 1) {
               this.listeningBind.setBind(i11);
               this.listeningBind = null;
               break;
            }
         }

         if (this.listeningBind != null && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 259) == 1) {
            this.listeningBind.setBind(0);
            this.listeningBind = null;
         }

         if (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 256) == 1) {
            this.listeningBind = null;
         }
      }

      if (this.listeningActivationBind != null) {
         for (int j11 = 32; j11 <= 348; j11++) {
            if (j11 != 256 && j11 != 259 && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), j11) == 1) {
               this.listeningActivationBind.setActivationKey(j11);
               this.listeningActivationBind = null;
               break;
            }
         }

         if (this.listeningActivationBind != null && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 259) == 1) {
            this.listeningActivationBind.setActivationKey(0);
            this.listeningActivationBind = null;
         }

         if (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 256) == 1) {
            this.listeningActivationBind = null;
         }
      }

      if (this.listeningGuiKey) {
         for (int k11 = 32; k11 <= 348; k11++) {
            if (k11 != 256 && k11 != 259 && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), k11) == 1) {
               Water.setGuiKey(k11, keyCodeToName(k11));
               this.listeningGuiKey = false;
               break;
            }
         }

         if (this.listeningGuiKey && GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), 256) == 1) {
            this.listeningGuiKey = false;
         }
      }
   }

   public void drawBlockListRow(DrawContext context, BlockListSetting setting, float x, float width, float y, int rowHeight, float alpha) {
      String s = this.expandedBlocksSetting == setting ? "v" : ">";
      int i = this.getTextWidth(s);
      int j = Math.round(x + width - 6.0F - i);
      int k = Math.max(30, j - (Math.round(x) + 10 + this.getTextWidth(setting.getName()) + 14));
      int l = this.scaleAlpha(this.expandedBlocksSetting != setting && setting.size() <= 0 ? -6642510 : -1511950, alpha);
      String s1 = setting.size() == 0 ? "Choose" : this.getBlockPickerLabel(setting);
      String s2 = this.ellipsize(s1, Math.round((k - 18) / 0.9F));
      int i1 = Math.max(34, Math.min(k, this.getTextWidth(s2) + 22));
      int j1 = j - i1 - 6;
      int k1 = this.scaleAlpha(setting.size() > 0 ? COLOR_ACCENT_DIM : -15195855, alpha);
      ItemStack itemStack = this.getBlockPickerIcon(setting);
      this.drawString(context, setting.getName(), Math.round(x) + 10, Math.round(y) + 4, l, false);
      Render2D.drawRoundedRect(context, j1, y + 2.0F, i1, 12.0F, Math.max(2.0F, Water.getPanelRadius() * 0.5F), k1, false);
      Render2D.drawRoundedOutline(context, j1, y + 2.0F, i1, 12.0F, Math.max(2.0F, Water.getPanelRadius() * 0.5F), 1.0F, this.scaleAlpha(0, alpha), false);
      if (!itemStack.isEmpty()) {
         context.drawItem(itemStack, j1 + 2, Math.round(y) + 1);
      }

      this.drawScaledString(context, s2, j1 + (itemStack.isEmpty() ? 6 : 16), y + 4.0F, 0.9F, this.scaleAlpha(-1511950, alpha));
      this.drawString(context, s, j, Math.round(y) + 4, this.scaleAlpha(-6642510, alpha), false);
   }

   public void drawBlockPicker(DrawContext context, BlockListSetting setting, float x, float width, float y, int var6, int var7) {
      BlockSelectLayout blockSelectLayout = this.layoutBlockPicker(x, width, y, setting);
      List list = this.getFilteredBlocks(setting);
      this.blockPickerScroll = this.clampBlockScroll(list.size(), this.blockPickerScroll);
      float f = Water.getPanelRadius();
      float f1 = Math.max(4.0F, f * 0.5F);
      float f2 = Water.getGlassIntensity();
      int i = Water.getBackgroundArgb();
      Render2D.drawRoundedRect(context, blockSelectLayout.x, blockSelectLayout.y, blockSelectLayout.width, blockSelectLayout.height, f1, i, false);
      if (f2 > 0.01F) {
         Render2D.drawRoundedOutline(
            context, blockSelectLayout.x, blockSelectLayout.y, blockSelectLayout.width, blockSelectLayout.height, f1, 1.0F, withAlpha(16777215, (int)(50.0F * f2)), false
         );
         Render2D.drawRoundedRect(
            context, blockSelectLayout.x + 1.0F, blockSelectLayout.y + 1.0F, blockSelectLayout.width - 2.0F, 6.0F, f1, withAlpha(16777215, (int)(15.0F * f2)), false
         );
      } else {
         Render2D.drawRoundedOutline(
            context,
            blockSelectLayout.x,
            blockSelectLayout.y,
            blockSelectLayout.width,
            blockSelectLayout.height,
            f1,
            1.0F,
            COLOR_ACCENT & 16777215 | 855638016,
            false
         );
      }

      boolean flag = this.blockSearchActive && this.expandedBlocksSetting == setting;
      int j = flag ? -15919840 : -16117736;
      int k = flag ? COLOR_ACCENT : -14799552;
      Render2D.drawRoundedRect(context, blockSelectLayout.searchX, blockSelectLayout.searchY, blockSelectLayout.searchWidth, blockSelectLayout.searchHeight, f1, j, false);
      Render2D.drawRoundedOutline(context, blockSelectLayout.searchX, blockSelectLayout.searchY, blockSelectLayout.searchWidth, blockSelectLayout.searchHeight, f1, 1.0F, k, false);
      String s = this.blockSearchQuery.isEmpty() ? "\u2315  Search..." : "\u2315  " + this.blockSearchQuery;
      if (flag && System.currentTimeMillis() / 500L % 2L == 0L) {
         s = s + "_";
      }

      this.drawSettingLabel(
         context,
         blockSelectLayout.searchX,
         blockSelectLayout.searchY,
         Math.max(0.0F, blockSelectLayout.searchWidth),
         Math.max(0.0F, blockSelectLayout.searchHeight),
         s,
         Math.round(blockSelectLayout.searchX) + 6,
         Math.round(blockSelectLayout.searchY) + 4,
         this.blockSearchQuery.isEmpty() && !flag ? -6642510 : -1511950
      );
      Render2D.drawRoundedRect(context, blockSelectLayout.clearX, blockSelectLayout.clearY, blockSelectLayout.clearWidth, blockSelectLayout.clearHeight, f1, 585125984, false);
      Render2D.drawRoundedOutline(
         context, blockSelectLayout.clearX, blockSelectLayout.clearY, blockSelectLayout.clearWidth, blockSelectLayout.clearHeight, f1, 1.0F, 1725976672, false
      );
      this.drawString(
         context,
         "\u2715",
         Math.round(blockSelectLayout.clearX) + (int)(blockSelectLayout.clearWidth / 2.0F) - 3,
         Math.round(blockSelectLayout.clearY) + 4,
         -2076576,
         false
      );
      if (list.isEmpty()) {
         this.drawString(context, "No blocks found", Math.round(blockSelectLayout.listX) + 6, Math.round(blockSelectLayout.listY) + 4, -6642510, false);
      } else {
         int l = Math.min(5, list.size());
         boolean flag1 = list.size() > l;

         for (int i1 = 0; i1 < l; i1++) {
            int j1 = this.blockPickerScroll + i1;
            if (j1 >= list.size()) {
               break;
            }

            Block block = (Block)list.get(j1);
            float f3 = blockSelectLayout.listY + i1 * 18;
            boolean flag2 = setting.contains(block);
            boolean flag3 = var6 >= blockSelectLayout.listX
               && var6 <= blockSelectLayout.listX + blockSelectLayout.listWidth
               && var7 >= f3
               && var7 <= f3 + 18.0F - 2.0F;
            int k1 = flag2 ? COLOR_ACCENT & 16777215 | 570425344 : (flag3 ? 419430399 : 0);
            if (k1 != 0) {
               Render2D.drawRoundedRect(context, blockSelectLayout.listX, f3, blockSelectLayout.listWidth, 16.0F, f1, k1, false);
            }

            if (flag2) {
               Render2D.drawRoundedRect(context, blockSelectLayout.listX, f3 + 2.0F, 2.0F, 12.0F, 1.0F, COLOR_ACCENT, false);
            }

            ItemStack itemStack = new ItemStack(block);
            if (!itemStack.isEmpty()) {
               context.getMatrices().pushMatrix();
               context.getMatrices().translate(blockSelectLayout.listX + 4.0F, f3 + 1.0F);
               context.getMatrices().scale(0.75F, 0.75F);
               context.drawItem(itemStack, 0, 0);
               context.getMatrices().popMatrix();
            }

            float f4 = blockSelectLayout.listX + blockSelectLayout.listWidth - 10.0F;
            int l1 = Math.round(blockSelectLayout.listX) + 16;
            String s1 = this.ellipsize(setting.getDisplayName(block), Math.round((f4 - l1 - 4.0F) / 0.9F));
            this.drawScaledString(context, s1, l1, f3 + 4.0F, 0.9F, flag2 ? COLOR_ACCENT : (flag3 ? -1511950 : -6642510));
            int i2 = flag2 ? COLOR_ACCENT : -14799552;
            int j2 = flag2 ? COLOR_ACCENT & 16777215 | 1140850688 : 0;
            if (j2 != 0) {
               Render2D.drawRoundedRect(context, f4, f3 + 5.0F, 6.0F, 6.0F, 3.0F, j2, false);
            }

            Render2D.drawRoundedOutline(context, f4, f3 + 5.0F, 6.0F, 6.0F, 3.0F, 1.0F, i2, false);
         }

         if (flag1) {
            int k2 = Math.max(1, list.size() - l);
            float f5 = blockSelectLayout.listX + blockSelectLayout.listWidth - 4.0F;
            float f6 = blockSelectLayout.listY + 1.0F;
            float f7 = blockSelectLayout.listHeight - 2.0F;
            float f8 = Math.max(12.0F, f7 * ((float)l / list.size()));
            float f9 = (f7 - f8) * ((float)this.blockPickerScroll / k2);
            Render2D.drawRoundedRect(context, f5, f6, 4.0F, f7, 2.0F, -16117736, false);
            Render2D.drawRoundedRect(context, f5, f6 + f9, 4.0F, f8, 2.0F, COLOR_ACCENT & 16777215 | -1442840576, false);
         }
      }
   }

   public void drawColorRow(DrawContext context, Setting<Color> setting, float x, float width, float y, int rowHeight, float var7, float var8, float var9) {
      Color color = (Color)setting.getValue();
      float f = Water.getPanelRadius();
      float f1 = Math.max(2.0F, f * 0.5F);
      float f2 = 12.0F;
      float f3 = x + width - 5.0F - f2;
      float f4 = y + (rowHeight - 4.0F - f2) / 2.0F;
      Render2D.drawRoundedRect(context, f3, f4 + 2.0F, f2, f2, f1, this.toArgb((Color)setting.getValue(), var9), false);
      Render2D.drawRoundedOutline(context, f3, f4 + 2.0F, f2, f2, f1, 1.0F, this.scaleAlpha(-1427114245, var9), false);
      if (!(var8 <= 0.01F)) {
         float f5 = this.easeOutCubic(var8) * var9;
         float f6 = x + 4.0F;
         float f7 = y + rowHeight + 6.0F;
         float f8 = Math.max(1.0F, 80.0F * f5);
         float f9 = Math.max(1.0F, 16.0F * f5);
         float f10 = f6 + f8 + 6.0F;
         float f11 = this.getHue(color);
         float f12 = this.getSaturation(color);
         float f13 = this.getBrightness(color);
         byte b0 = 12;
         byte b1 = 12;
         float f14 = f8 / b0;
         float f15 = f8 / b1;

         for (int i = 0; i < b1; i++) {
            float f16 = 1.0F - (float)i / b1;

            for (int j = 0; j < b0; j++) {
               float f17 = (float)j / b0;
               context.fill(
                  (int)(f6 + j * f14), (int)(f7 + i * f15), (int)(f6 + (j + 1) * f14), (int)(f7 + (i + 1) * f15), this.withAlphaFraction(Color.HSBtoRGB(f11, f17, f16), f5)
               );
            }
         }

         Render2D.drawRoundedOutline(context, f6, f7, f8, f8, f1, 1.0F, this.withAlphaFraction(1728053247, f5), false);
         float f19 = f6 + f12 * f8;
         float f20 = f7 + (1.0F - f13) * f8;
         Render2D.drawRoundedRect(context, f19 - 4.0F, f20 - 4.0F, 8.0F, 8.0F, 4.0F, this.withAlphaFraction(-2013265920, f5), false);
         Render2D.drawRoundedOutline(context, f19 - 4.0F, f20 - 4.0F, 8.0F, 8.0F, 4.0F, 2.0F, this.withAlphaFraction(-1, f5), false);

         for (int l = 0; l < (int)f8; l++) {
            float f22 = (float)l / (int)f8;
            context.fill((int)f10, (int)(f7 + l), (int)(f10 + f9), (int)(f7 + l + 1.0F), this.withAlphaFraction(0xFF000000 | Color.HSBtoRGB(f22, 1.0F, 1.0F) & 16777215, f5));
         }

         Render2D.drawRoundedOutline(context, f10, f7, f9, f8, f1, 1.0F, this.withAlphaFraction(1728053247, f5), false);
         Render2D.drawRoundedRect(context, f10 - 2.0F, f7 + f11 * f8 - 1.0F, f9 + 4.0F, 3.0F, 1.5F, this.withAlphaFraction(-1, f5), false);
         float f21 = f7 + f8 + 6.0F;
         float f23 = (f8 + 6.0F + f9) / 2.0F - 2.0F;
         float f18 = Math.max(1.0F, 14.0F * f5);
         int k = this.toArgb(color, f5);
         Render2D.drawRoundedRect(context, f6, f21, f23, f18, f1, k, false);
         Render2D.drawRoundedRect(context, f6 + f23 + 4.0F, f21, f23, f18, f1, k, false);
         Render2D.drawRoundedOutline(context, f6, f21, f23, f18, f1, 1.0F, this.withAlphaFraction(1442840575, f5), false);
         Render2D.drawRoundedOutline(context, f6 + f23 + 4.0F, f21, f23, f18, f1, 1.0F, this.withAlphaFraction(1442840575, f5), false);
         this.drawString(context, "ORIGINAL", (int)(f6 + 2.0F), (int)(f21 + f18 + 2.0F), this.withAlphaFraction(-6642510, f5), false);
         this.drawString(context, "NEW", (int)(f6 + f23 + 6.0F), (int)(f21 + f18 + 2.0F), this.withAlphaFraction(-6642510, f5), false);
      }
   }

   public float getHue(Color color) {
      return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
   }

   public float getSaturation(Color color) {
      return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[1];
   }

   public float getBrightness(Color color) {
      return Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2];
   }

   public float getAlphaFloat(Color color) {
      return color.getAlpha() / 255.0F;
   }

   public void drawGradientBar(DrawContext context, float x, float y, float width, float height, float radius, int segments, IntFunction<Integer> intFunction) {
      if (!(height <= 0.0F)) {
         float f = Math.max(1.0F, width / segments);

         for (int i = 0; i < segments; i++) {
            float f1 = x + f * i;
            float f2 = i == segments - 1 ? x + width - f1 : f + 1.0F;
            float f3 = i == 0 ? radius : 0.0F;
            float f4 = i == segments - 1 ? radius : 0.0F;
            Render2D.G(context, f1, y, f2, height, f3, f4, f4, f3, false, (Integer)intFunction.apply(i));
         }
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      this.uiScale = this.getUiScaleFactor();
      double d0 = this.toScaledX(click.x());
      double d1 = this.toScaledY(click.y());
      int i = click.button();
      this.activeColorSetting = null;
      this.colorDragMode = SortMode.NONE;
      this.draggingNumericSetting = null;
      this.draggingNumericModule = null;
      int j = Math.min(260, this.getScaledWidth() - 60);
      int k = (this.getScaledWidth() - j) / 2;
      int l = this.getScaledHeight() - 20 - 60;
      String s = "Configs";
      byte b0 = 14;
      int i1 = this.getTextWidth(s) + 18;
      int j1 = (this.getScaledWidth() - i1) / 2;
      int k1 = l - b0 - 6;
      if (d0 >= j1 && d0 <= j1 + i1 && d1 >= k1 && d1 <= k1 + b0) {
         this.searchActive = false;
         MinecraftClient.getInstance().setScreen(new ConfigScreen());
         return true;
      } else if (d0 >= k && d0 <= k + j && d1 >= l && d1 <= l + 20) {
         this.searchActive = true;
         this.blockSearchActive = false;
         this.listeningBind = null;
         this.listeningActivationBind = null;
         this.listeningString = null;
         return true;
      } else {
         this.searchActive = false;
         if (i == 0) {
            Category[] acategory = CACHED_CATEGORIES;

            for (int l1 = 0; l1 < acategory.length; l1++) {
               int i2 = this.getCategoryX(acategory[l1], l1);
               int j2 = this.getCategoryY(acategory[l1]);
               if (d0 >= i2 && d0 <= i2 + 155 && d1 >= j2 && d1 <= j2 + 22) {
                  this.draggingCategory = acategory[l1];
                  this.dragGrabOffsetX = (int)(d0 - i2);
                  this.dragGrabOffsetY = (int)(d1 - j2);
                  return true;
               }
            }
         }

         Category[] acategory1 = CACHED_CATEGORIES;

         for (int j5 = 0; j5 < acategory1.length; j5++) {
            Category category = acategory1[j5];
            int k5 = this.getCategoryX(category, j5);
            int k2 = this.getCategoryY(category);
            int l2 = k2 + 22 + 6;

            for (Module module : ModuleManager.INSTANCE.getModulesInCategory(category)) {
               if (this.matchesSearch(module)) {
                  if (d0 >= k5 + 4 && d0 <= k5 + 155 - 4 && d1 >= l2 && d1 <= l2 + 17) {
                     if (i == 1 && module.getName().equals("Chat Macro")) {
                        MinecraftClient.getInstance().setScreen(new KeybindsScreen(this));
                        return true;
                     }

                     if (i == 0) {
                        module.toggle();
                     } else if (i == 1) {
                        boolean flag1 = !module.isExpanded();
                        String s3 = category.name() + "/" + module.getName();
                        if (!flag1) {
                           this.animValues.put(s3 + "/expand", 0.0F);
                        } else {
                           this.animValues.put(s3 + "/expand", 0.0F);

                           for (int l5 = 0; l5 < module.getSettings().size() + 2; l5++) {
                              this.animValues.put(s3 + "/stagger/" + l5, -8.0F);
                           }

                           this.animValues.put(s3 + "/stagger/bind", -10.0F);
                           this.animValues.put(s3 + "/stagger/act", -10.0F);
                        }

                        module.setExpanded(flag1);
                     }

                     return true;
                  }

                  l2 += 19;
                  if (module.isExpanded()) {
                     if (d0 >= k5 + 4 && d0 <= k5 + 155 - 4 && d1 >= l2 && d1 <= l2 + 17) {
                        if (i == 1) {
                           module.setBind(0);
                           this.listeningBind = null;
                           this.listeningActivationBind = null;
                        } else if (i == 0) {
                           this.listeningBind = module;
                           this.listeningActivationBind = null;
                        }

                        return true;
                     }

                     l2 += 19;
                     if ("Config Share".equals(module.getName())) {
                        if (d0 >= k5 + 4 && d0 <= k5 + 155 - 4 && d1 >= l2 && d1 <= l2 + 17) {
                           if (i == 0) {
                              MinecraftClient.getInstance().setScreen(new ConfigScreen());
                           }

                           return true;
                        }

                        l2 += 19;
                     }

                     if (module instanceof ActivatableModule activatableModule) {
                        if (d0 >= k5 + 4 && d0 <= k5 + 155 - 4 && d1 >= l2 && d1 <= l2 + 17) {
                           if (i == 1) {
                              activatableModule.setActivationKey(0);
                              this.listeningActivationBind = null;
                              this.listeningBind = null;
                           } else if (i == 0) {
                              this.listeningActivationBind = activatableModule;
                              this.listeningBind = null;
                           }

                           return true;
                        }

                        l2 += 19;
                     }

                     for (Setting setting : module.getSettings()) {
                        if (d0 >= k5 + 4 && d0 <= k5 + 155 - 4 && d1 >= l2 && d1 <= l2 + 17) {
                           if (setting instanceof ModeSetting modeSetting) {
                              if (i == 1) {
                                 modeSetting.cyclePrevious();
                              } else {
                                 modeSetting.cycleNext();
                              }
                           } else if (setting.getValue() instanceof Boolean) {
                              setting.setValue(!(Boolean)setting.getValue());
                           } else if (setting.getValue() instanceof String) {
                              if ("GUI Key".equals(setting.getName())) {
                                 if (i == 0) {
                                    this.listeningGuiKey = !this.listeningGuiKey;
                                    this.listeningBind = null;
                                    this.listeningActivationBind = null;
                                    this.listeningString = null;
                                 } else if (i == 1) {
                                    this.listeningGuiKey = false;
                                 }
                              } else if (this.isStringListSetting(module, setting)) {
                                 if (i == 0) {
                                    this.expandedStringListSetting = this.expandedStringListSetting == setting ? null : setting;
                                    this.stringListAddActive = this.expandedStringListSetting == setting;
                                    this.stringListAddBuffer = "";
                                    this.listeningString = null;
                                 } else if (i == 1) {
                                    this.expandedStringListSetting = null;
                                    this.stringListAddActive = false;
                                    this.stringListAddBuffer = "";
                                 }
                              } else {
                                 this.expandedStringListSetting = null;
                                 this.stringListAddActive = false;
                                 this.stringListAddBuffer = "";
                                 this.listeningString = setting;
                              }
                           } else if (!(setting.getValue() instanceof Float)
                              && !(setting.getValue() instanceof Double)
                              && !(setting.getValue() instanceof Integer)) {
                              if (setting instanceof BlockListSetting blockListSetting2) {
                                 if (i == 0 || i == 1) {
                                    LinkedHashMap<Block, Color> linkedHashMap = new LinkedHashMap<>();
                                    Consumer<Map<Block, Color>> consumer = null;
                                    if (module instanceof StorageESP storageESP) {
                                       linkedHashMap.putAll(storageESP.getCustomBlockColors());
                                       consumer = storageESP::setCustomBlockColors;
                                    }

                                    MinecraftClient.getInstance().setScreen(new BlockSelectScreen(this, module, blockListSetting2, linkedHashMap, consumer));
                                    return true;
                                 }

                                 if (i == 2) {
                                    blockListSetting2.clear();
                                 }
                              } else if (setting instanceof EntityListSetting entityListSetting2) {
                                 if (i == 0) {
                                    if (this.expandedMobsSetting != entityListSetting2) {
                                       this.mobSearchQuery = "";
                                       this.mobPickerScroll = 0;
                                    }

                                    this.expandedMobsSetting = this.expandedMobsSetting == entityListSetting2 ? null : entityListSetting2;
                                    this.mobSearchActive = this.expandedMobsSetting == entityListSetting2;
                                 } else if (i == 1) {
                                    entityListSetting2.clear();
                                    this.mobPickerScroll = 0;
                                 }
                              } else if (setting.getValue() instanceof Color) {
                                 if (i == 0) {
                                    this.expandedColorSetting = this.expandedColorSetting == setting ? null : setting;
                                 } else if (i == 1) {
                                    this.expandedColorSetting = null;
                                 }
                              }
                           } else if (i == 0) {
                              this.draggingNumericSetting = setting;
                              this.draggingNumericModule = module;
                              this.draggingNumericCatX = k5;
                              this.updateNumericSetting(module, setting, d0, k5);
                           }

                           return true;
                        }

                        if (this.isStringListSetting(module, setting) && this.expandedStringListSetting == setting) {
                           int i3 = l2 + 19;
                           int j3 = this.getStringListHeight(setting);
                           int k3 = k5 + 4;
                           short short1 = 147;
                           if (this.isInside(d0, d1, k3, i3, short1, j3)) {
                              List<String> list2 = this.getStringListValues(setting);
                              int i7 = Math.min(6, list2.size());

                              for (int j4 = 0; j4 < i7; j4++) {
                                 int k4 = i3 + j4 * 19;
                                 int l4 = k3 + short1 - 10 - 12;
                                 int i5 = k4 + 2;
                                 if (this.isInside(d0, d1, l4, i5, 12.0F, 12.0F) && i == 0) {
                                    String s1 = (String)list2.get(j4);
                                    list2.removeIf(var1 -> var1.equalsIgnoreCase(s1));
                                    this.setStringListValues(setting, list2);
                                    return true;
                                 }
                              }

                              int j7 = i3 + i7 * 19;
                              int k7 = k3 + short1 - 10 - 12;
                              int l7 = j7 + 2;
                              if (i == 0 && this.isInside(d0, d1, k7, l7, 12.0F, 12.0F)) {
                                 String s4 = this.stringListAddBuffer == null ? "" : this.stringListAddBuffer.trim();
                                 if (!s4.isEmpty()) {
                                    String s5 = s4.toLowerCase(Locale.ROOT);
                                    boolean flag = false;

                                    for (String s2 : (Iterable<String>)list2) {
                                       if (s2.equalsIgnoreCase(s5)) {
                                          flag = true;
                                          break;
                                       }
                                    }

                                    if (!flag) {
                                       list2.add(s5);
                                       this.setStringListValues(setting, list2);
                                    }
                                 }

                                 this.stringListAddBuffer = "";
                                 this.stringListAddActive = true;
                                 this.listeningString = null;
                                 return true;
                              }

                              if (i == 0 && this.isInside(d0, d1, k3, j7, short1, 17.0F)) {
                                 this.stringListAddActive = true;
                                 this.listeningString = null;
                                 return true;
                              }

                              return true;
                           }
                        }

                        if (setting instanceof BlockListSetting blockListSetting && this.expandedBlocksSetting == blockListSetting) {
                           BlockSelectLayout blockSelectLayout = this.layoutBlockPicker(k5 + 4, 147.0F, l2 + 19, blockListSetting);
                           if (this.isInside(d0, d1, blockSelectLayout.clearX, blockSelectLayout.clearY, blockSelectLayout.clearWidth, blockSelectLayout.clearHeight)
                              )
                            {
                              blockListSetting.clear();
                              this.blockPickerScroll = 0;
                              this.blockSearchQuery = "";
                              return true;
                           }

                           if (this.isInside(
                              d0, d1, blockSelectLayout.searchX, blockSelectLayout.searchY, blockSelectLayout.searchWidth, blockSelectLayout.searchHeight
                           )) {
                              this.blockSearchActive = true;
                              this.searchActive = false;
                              this.listeningString = null;
                              return true;
                           }

                           if (this.isInside(d0, d1, blockSelectLayout.x, blockSelectLayout.y, blockSelectLayout.width, blockSelectLayout.height)) {
                              this.blockSearchActive = false;
                              List list1 = this.getFilteredBlocks(blockListSetting);
                              int j6 = Math.min(5, Math.max(1, list1.size()));

                              for (int k6 = 0; k6 < j6; k6++) {
                                 int l6 = this.clampBlockScroll(list1.size(), this.blockPickerScroll) + k6;
                                 if (l6 >= list1.size()) {
                                    break;
                                 }

                                 float f1 = blockSelectLayout.listY + k6 * 18;
                                 if (this.isInside(d0, d1, blockSelectLayout.listX, f1, blockSelectLayout.listWidth, 16.0F)) {
                                    blockListSetting.toggle((Block)list1.get(l6));
                                    return true;
                                 }
                              }

                              return true;
                           }
                        }

                        if (setting instanceof EntityListSetting entityListSetting && this.expandedMobsSetting == entityListSetting) {
                           BlockSelectLayout blockSelectLayout1 = this.layoutMobPicker(k5 + 4, 147.0F, l2 + 19, entityListSetting);
                           if (this.isInside(
                              d0, d1, blockSelectLayout1.clearX, blockSelectLayout1.clearY, blockSelectLayout1.clearWidth, blockSelectLayout1.clearHeight
                           )) {
                              entityListSetting.clear();
                              this.mobPickerScroll = 0;
                              this.mobSearchQuery = "";
                              return true;
                           }

                           if (this.isInside(
                              d0, d1, blockSelectLayout1.searchX, blockSelectLayout1.searchY, blockSelectLayout1.searchWidth, blockSelectLayout1.searchHeight
                           )) {
                              this.mobSearchActive = true;
                              this.searchActive = false;
                              this.listeningString = null;
                              return true;
                           }

                           if (this.isInside(d0, d1, blockSelectLayout1.x, blockSelectLayout1.y, blockSelectLayout1.width, blockSelectLayout1.height)) {
                              this.mobSearchActive = false;
                              List list = this.getFilteredMobs(entityListSetting);
                              int i6 = Math.min(5, Math.max(1, list.size()));

                              for (int l3 = 0; l3 < i6; l3++) {
                                 int i4 = this.clampMobScroll(list.size(), this.mobPickerScroll) + l3;
                                 if (i4 >= list.size()) {
                                    break;
                                 }

                                 float f = blockSelectLayout1.listY + l3 * 18;
                                 if (this.isInside(d0, d1, blockSelectLayout1.listX, f, blockSelectLayout1.listWidth, 16.0F)) {
                                    entityListSetting.toggle((EntityType<?>)list.get(i4));
                                    return true;
                                 }
                              }

                              return true;
                           }
                        }

                        if (i == 0 && setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
                           ColorPickerLayout colorPickerLayout = this.layoutColorPicker(k5 + 4, 147.0F, l2, 17);
                           if (this.isInside(d0, d1, colorPickerLayout.fieldX, colorPickerLayout.fieldY, colorPickerLayout.fieldWidth, colorPickerLayout.fieldHeight)
                              )
                            {
                              this.pickColorFromField(setting, colorPickerLayout, d0, d1);
                              this.activeColorSetting = setting;
                              this.colorDragMode = SortMode.FIELD;
                              return true;
                           }

                           if (this.isInside(
                              d0,
                              d1,
                              colorPickerLayout.alphaY - 6.0F,
                              colorPickerLayout.fieldY - 4.0F,
                              colorPickerLayout.alphaHeight + 12.0F,
                              colorPickerLayout.fieldHeight + 8.0F
                           )) {
                              this.pickHueFromSlider(setting, colorPickerLayout, d1);
                              this.activeColorSetting = setting;
                              this.colorDragMode = SortMode.ALPHA;
                              return true;
                           }
                        }

                        l2 += 19;
                        if (this.isStringListSetting(module, setting) && this.expandedStringListSetting == setting) {
                           l2 += this.getStringListHeight(setting);
                        }

                        if (setting instanceof BlockListSetting blockListSetting1 && this.expandedBlocksSetting == blockListSetting1) {
                           l2 += this.getBlockPickerHeight(blockListSetting1);
                        }

                        if (setting instanceof EntityListSetting entityListSetting1 && this.expandedMobsSetting == entityListSetting1) {
                           l2 += this.getMobPickerHeight(entityListSetting1);
                        }

                        if (setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
                           l2 += 112;
                        }
                     }
                  }
               }
            }
         }

         this.listeningBind = null;
         this.listeningActivationBind = null;
         this.listeningString = null;
         this.stringListAddActive = false;
         this.blockSearchActive = false;
         return super.mouseClicked(click, doubled);
      }
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      this.uiScale = this.getUiScaleFactor();
      double d0 = this.toScaledX(click.x());
      double d1 = this.toScaledY(click.y());
      int i = click.button();
      if (i != 0) {
         return super.mouseDragged(click, offsetX, offsetY);
      } else if (this.colorDragMode != SortMode.NONE && this.activeColorSetting != null && this.handleCategoryAreaClick(d0, d1)) {
         return true;
      } else if (this.draggingNumericSetting != null && this.draggingNumericModule != null) {
         this.updateNumericSetting(this.draggingNumericModule, this.draggingNumericSetting, d0, this.draggingNumericCatX);
         return true;
      } else if (this.draggingCategory != null) {
         Category[] acategory1 = CACHED_CATEGORIES;
         int j1 = 0;

         for (int k1 = 0; k1 < acategory1.length; k1++) {
            if (acategory1[k1] == this.draggingCategory) {
               j1 = k1;
               break;
            }
         }

         int l1 = CACHED_CATEGORIES.length * 155 + (CACHED_CATEGORIES.length - 1) * 12;
         int i2 = Math.max(10, (this.getScaledWidth() - l1) / 2);
         int j2 = i2 + j1 * 167;
         int k2 = this.getHeaderHeight() + this.verticalScroll;
         int[] aint = this.getCategoryOffsets(this.draggingCategory);
         aint[0] = (int)(d0 - this.dragGrabOffsetX) - j2;
         aint[1] = (int)(d1 - this.dragGrabOffsetY) - k2;
         return true;
      } else {
         Category[] acategory = CACHED_CATEGORIES;

         for (int j = 0; j < acategory.length; j++) {
            Category category = acategory[j];
            int k = this.getCategoryX(category, j);
            int l = this.getCategoryY(category);
            int i1 = l + 22 + 6;

            for (Module module : ModuleManager.INSTANCE.getModulesInCategory(category)) {
               if (this.matchesSearch(module)) {
                  i1 += 19;
                  if (module.isExpanded()) {
                     i1 += 19;
                     i1 += 19;

                     for (Setting setting : module.getSettings()) {
                        if (d0 >= k + 4
                           && d0 <= k + 155 - 4
                           && d1 >= i1
                           && d1 <= i1 + 17
                           && (setting.getValue() instanceof Float || setting.getValue() instanceof Double || setting.getValue() instanceof Integer)) {
                           this.updateNumericSetting(module, setting, d0, k);
                        }

                        i1 += 19;
                        if (setting instanceof BlockListSetting blockListSetting && this.expandedBlocksSetting == blockListSetting) {
                           i1 += this.getBlockPickerHeight(blockListSetting);
                        }

                        if (setting instanceof EntityListSetting entityListSetting && this.expandedMobsSetting == entityListSetting) {
                           i1 += this.getMobPickerHeight(entityListSetting);
                        }

                        if (setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
                           i1 += 112;
                        }
                     }
                  }
               }
            }
         }

         return super.mouseDragged(click, offsetX, offsetY);
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.activeColorSetting = null;
      this.colorDragMode = SortMode.NONE;
      this.draggingCategory = null;
      this.draggingNumericSetting = null;
      this.draggingNumericModule = null;
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.uiScale = this.getUiScaleFactor();
      mouseX = this.toScaledX(mouseX);
      mouseY = this.toScaledY(mouseY);
      double d0 = verticalAmount != 0.0 ? verticalAmount : horizontalAmount;
      if (d0 == 0.0) {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      } else {
         BlockSelectContext blockSelectContext = this.findOpenBlockPicker();
         if (blockSelectContext != null
            && this.isInside(
               mouseX, mouseY, blockSelectContext.layout().x, blockSelectContext.layout().y, blockSelectContext.layout().width, blockSelectContext.layout().height
            )) {
            this.blockPickerScroll = this.clampBlockScroll(this.getFilteredBlocks(blockSelectContext.setting()).size(), this.blockPickerScroll + (d0 > 0.0 ? -1 : 1));
            return true;
         } else {
            EntitySelectContext entitySelectContext = this.findOpenMobPicker();
            if (entitySelectContext != null
               && this.isInside(
                  mouseX,
                  mouseY,
                  entitySelectContext.layout().x,
                  entitySelectContext.layout().y,
                  entitySelectContext.layout().width,
                  entitySelectContext.layout().height
               )) {
               this.mobPickerScroll = this.clampMobScroll(this.getFilteredMobs(entitySelectContext.setting()).size(), this.mobPickerScroll + (d0 > 0.0 ? -1 : 1));
               return true;
            } else {
               this.verticalScroll = this.clampVerticalScroll(this.verticalScroll + (int)Math.round(d0 * 24.0));
               return true;
            }
         }
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      String s = this.stripControlChars(input.asString());
      if (s.isEmpty()) {
         return super.charTyped(input);
      } else if (this.blockSearchActive && this.expandedBlocksSetting != null) {
         this.blockSearchQuery = this.blockSearchQuery + s;
         this.blockPickerScroll = 0;
         return true;
      } else if (this.mobSearchActive && this.expandedMobsSetting != null) {
         this.mobSearchQuery = this.mobSearchQuery + s;
         this.mobPickerScroll = 0;
         return true;
      } else if (this.searchActive && this.listeningString == null) {
         this.searchQuery = this.searchQuery + s;
         return true;
      } else if (this.stringListAddActive && this.expandedStringListSetting != null) {
         this.stringListAddBuffer = (this.stringListAddBuffer == null ? "" : this.stringListAddBuffer) + s;
         return true;
      } else if (this.listeningString != null) {
         this.listeningString.setValue(this.listeningString.getValue() + s);
         return true;
      } else {
         return super.charTyped(input);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      int key = input.getKeycode();

      // --- Module bind listening (fix: key events were not captured here) ---
      if (this.listeningBind != null) {
         if (key == 256) { // Esc cancel
            this.listeningBind = null;
            return true;
         }
         if (key == 259) { // Backspace clear
            this.listeningBind.setBind(0);
            this.listeningBind = null;
            return true;
         }
         if (key != 0 && key != 340 && key != 344) { // ignore pure modifiers alone
            this.listeningBind.setBind(key);
            this.listeningBind = null;
            return true;
         }
         return true;
      }

      if (this.listeningActivationBind != null) {
         if (key == 256) {
            this.listeningActivationBind = null;
            return true;
         }
         if (key == 259) {
            this.listeningActivationBind.setActivationKey(0);
            this.listeningActivationBind = null;
            return true;
         }
         if (key != 0 && key != 340 && key != 344) {
            this.listeningActivationBind.setActivationKey(key);
            this.listeningActivationBind = null;
            return true;
         }
         return true;
      }

      if (this.blockSearchActive && this.expandedBlocksSetting != null && this.handleBlockSearchKey(input)) {
         return true;
      } else if (this.mobSearchActive && this.expandedMobsSetting != null && this.handleMobSearchKey(input)) {
         return true;
      } else if (this.searchActive) {
         if (this.isCloseKey(input)) {
            this.searchActive = false;
            return true;
         } else if (input.getKeycode() == 259) {
            this.searchQuery = this.removeLastChar(this.searchQuery);
            return true;
         } else if (input.isPaste()) {
            this.searchQuery = this.searchQuery + this.getSanitizedClipboard();
            return true;
         } else {
            return true;
         }
      } else if (this.listeningString != null && this.handleStringSettingKey(input)) {
         return true;
      } else {
         return this.stringListAddActive && this.expandedStringListSetting != null && this.handleStringListKey(input) ? true : super.keyPressed(input);
      }
   }

   public boolean handleStringListKey(KeyInput input) {
      if (input.getKeycode() == 259) {
         this.stringListAddBuffer = this.removeLastChar(this.stringListAddBuffer == null ? "" : this.stringListAddBuffer);
         return true;
      } else if (input.isPaste()) {
         this.stringListAddBuffer = (this.stringListAddBuffer == null ? "" : this.stringListAddBuffer) + this.getSanitizedClipboard();
         return true;
      } else if (input.isEscape()) {
         this.stringListAddActive = false;
         this.stringListAddBuffer = "";
         return true;
      } else if (!input.isEnter()) {
         return true;
      } else {
         if (this.expandedStringListSetting != null) {
            List list = this.getStringListValues(this.expandedStringListSetting);
            String s = this.stringListAddBuffer == null ? "" : this.stringListAddBuffer.trim();
            if (!s.isEmpty()) {
               String s1 = s.toLowerCase(Locale.ROOT);
               boolean flag = false;

               for (String s2 : (Iterable<String>)list) {
                  if (s2.equalsIgnoreCase(s1)) {
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  list.add(s1);
                  this.setStringListValues(this.expandedStringListSetting, list);
               }
            }
         }

         this.stringListAddBuffer = "";
         return true;
      }
   }

   public boolean matchesSearch(Module module) {
      if (module instanceof ConfigShare) {
         return false;
      } else {
         return this.searchQuery.isBlank() ? true : module.getName().toLowerCase().contains(this.searchQuery.trim().toLowerCase());
      }
   }

   public String getBindLabel(Module module) {
      String s = this.getKeyName(module.getBind());
      return "None".equals(s) ? "" : s;
   }

   public String getKeyName(int keyCode) {
      return keyCodeToName(keyCode);
   }

   public static String keyCodeToName(int keyCode) {
      if (keyCode == 0) {
         return "None";
      } else {
         String s = GLFW.glfwGetKeyName(keyCode, 0);
         if (s != null && !s.isBlank()) {
            return formatKeyName(s);
         } else {
            return switch (keyCode) {
               case 32 -> "Space";
               case 256 -> "Esc";
               case 257 -> "Enter";
               case 258 -> "Tab";
               case 259 -> "Backspace";
               case 260 -> "Insert";
               case 261 -> "Delete";
               case 262 -> "Right";
               case 263 -> "Left";
               case 264 -> "Down";
               case 265 -> "Up";
               case 266 -> "Page Up";
               case 267 -> "Page Down";
               case 268 -> "Home";
               case 269 -> "End";
               case 280 -> "Caps";
               case 340 -> "LShift";
               case 341 -> "LCtrl";
               case 342 -> "LAlt";
               case 344 -> "RShift";
               case 345 -> "RCtrl";
               case 346 -> "RAlt";
               default -> "Key " + keyCode;
            };
         }
      }
   }

   public static String formatKeyName(String text) {
      if (text == null) {
         return "";
      } else {
         String s = text.trim();
         if (s.isEmpty()) {
            return "";
         } else {
            String s1 = s.toLowerCase();

            return switch (s1) {
               case "right shift" -> "RShift";
               case "left shift" -> "LShift";
               case "right control", "right ctrl" -> "RCtrl";
               case "left control", "left ctrl" -> "LCtrl";
               case "right alt" -> "RAlt";
               case "left alt" -> "LAlt";
               case "escape" -> "Esc";
               case "caps lock" -> "Caps";
               case "page up" -> "Page Up";
               case "page down" -> "Page Down";
               default -> s.length() == 1 ? s.toUpperCase() : s;
            };
         }
      }
   }

   public void drawModeRow(DrawContext context, ModeSetting setting, int x, int width, int y, float alpha) {
      String s = setting.getName();
      String s1 = setting.getValue();
      int i = this.getTextWidth(s1) + 12;
      int j = x + width - 10 - i;
      int k = y + 4;
      int l = x + 10;
      int i1 = Math.max(0, j - 4 - l);
      String s2 = s;
      if (this.getTextWidth(s) > i1) {
         String s3 = "...";
         int j1 = this.getTextWidth(s3);

         while (s2.length() > 0 && this.getTextWidth(s2) + j1 > i1) {
            s2 = s2.substring(0, s2.length() - 1);
         }

         s2 = s2 + s3;
      }

      this.drawString(context, s2, l, k, this.scaleAlpha(-1511950, alpha), false);
      Render2D.drawRoundedRect(context, j, y + 2, i, 12.0F, 5.0F, this.scaleAlpha(-15195855, alpha), false);
      Render2D.drawRoundedOutline(context, j, y + 2, i, 12.0F, 5.0F, 1.0F, this.scaleAlpha(0, alpha), false);
      this.drawString(context, s1, j + 6, k, this.scaleAlpha(COLOR_ACCENT, alpha), false);
   }

   public void drawStringListRows(DrawContext context, Setting<String> setting, int x, int width, int y, float alpha) {
      List list = this.getStringListValues(setting);
      int i = Math.min(6, list.size());
      int j = this.getStringListHeight(setting);
      Render2D.drawRoundedRect(context, x, width, y, j, Math.max(0.0F, Water.getPanelRadius() - 3.0F), this.scaleAlpha(0, alpha), false);
      Render2D.drawRoundedOutline(context, x, width, y, j, Math.max(0.0F, Water.getPanelRadius() - 3.0F), 1.0F, this.scaleAlpha(0, alpha), false);
      int k = width;

      for (int l = 0; l < i; l++) {
         String s = (String)list.get(l);
         this.drawString(context, s, x + 10, k + 4, this.scaleAlpha(-1511950, alpha), false);
         int i1 = x + y - 10 - 12;
         int j1 = k + 2;
         Render2D.drawRoundedRect(context, i1, j1, 12.0F, 12.0F, 4.0F, this.scaleAlpha(-14011323, alpha), false);
         Render2D.drawRoundedOutline(context, i1, j1, 12.0F, 12.0F, 4.0F, 1.0F, this.scaleAlpha(0, alpha), false);
         this.drawString(context, "x", i1 + 4, k + 4, this.scaleAlpha(-1938838, alpha), false);
         k += 19;
      }

      String s1 = "Add: " + (this.stringListAddBuffer == null ? "" : this.stringListAddBuffer);
      if (this.stringListAddActive && this.expandedStringListSetting == setting) {
         s1 = s1 + "_";
      }

      this.drawSettingLabel(context, x, k, y, 17.0F, s1, x + 10, k + 4, this.scaleAlpha(-6642510, alpha));
      int k1 = x + y - 10 - 12;
      int l1 = k + 2;
      Render2D.drawRoundedRect(context, k1, l1, 12.0F, 12.0F, 4.0F, this.scaleAlpha(-15195855, alpha), false);
      Render2D.drawRoundedOutline(context, k1, l1, 12.0F, 12.0F, 4.0F, 1.0F, this.scaleAlpha(0, alpha), false);
      this.drawString(context, "+", k1 + 4, k + 4, this.scaleAlpha(COLOR_ACCENT, alpha), false);
   }

   public int getCategoryHeight(Category category) {
      int i = 38;
      int j = 0;

      for (Module module : ModuleManager.INSTANCE.getModulesInCategory(category)) {
         if (this.matchesSearch(module)) {
            j++;
            i += 19;
            String s = category.name() + "/" + module.getName();
            float f = this.animValues.getOrDefault(s + "/expand", module.isExpanded() ? 1.0F : 0.0F);
            if (f > 0.001F) {
               i += Math.round(this.getModuleRowHeight(module) * f);
            }
         }
      }

      if (j == 0) {
         i += 19;
      }

      return i;
   }

   public BlockSelectLayout layoutBlockPicker(float x, float width, float y, BlockListSetting setting) {
      int i = this.getFilteredBlocks(setting).size();
      int j = Math.min(5, Math.max(1, i));
      float f = x + 6.0F;
      float f1 = y + 6.0F;
      float f2 = x + width - 30.0F - 6.0F;
      float f3 = Math.max(24.0F, f2 - f - 4.0F);
      float f4 = x + 6.0F;
      float f5 = f1 + 16.0F + 6.0F;
      float f6 = width - 12.0F;
      float f7 = j * 18;
      float f8 = 28.0F + f7 + 6.0F;
      return new BlockSelectLayout(x, y, width, f8, f, f1, f3, 16.0F, f2, f1, 30.0F, 16.0F, f4, f5, f6, f7);
   }

   public List<Block> getFilteredBlocks(BlockListSetting setting) {
      ArrayList arrayList = new ArrayList<>(setting.filter(this.blockSearchQuery));
      arrayList.sort(Comparator.<Block, Boolean>comparing(var1x -> !setting.contains(var1x)).thenComparing(setting::getDisplayName, String.CASE_INSENSITIVE_ORDER));
      return arrayList;
   }

   public String getBlockPickerLabel(BlockListSetting setting) {
      return "Choose";
   }

   public ItemStack getBlockPickerIcon(BlockListSetting setting) {
      Block block = setting.getSelectedBlocks().stream().findFirst().orElse(null);
      if (block == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack itemStack = new ItemStack(block);
         return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
      }
   }

   public String ellipsize(String text, int maxWidth) {
      if (text != null && !text.isEmpty() && maxWidth > 0) {
         if (this.getTextWidth(text) <= maxWidth) {
            return text;
         } else {
            int i = this.getTextWidth("...");
            return i >= maxWidth ? this.trimToWidth(text, maxWidth) : this.trimToWidth(text, maxWidth - i) + "...";
         }
      } else {
         return "";
      }
   }

   public String maskSensitiveValue(Module module, Setting<?> setting, String text) {
      if (text == null || text.isEmpty()) {
         return "";
      } else {
         return this.isSensitiveSetting(module, setting) ? this.shortenUrl(text, 10) : text;
      }
   }

   public boolean isSensitiveSetting(Module module, Setting<?> setting) {
      return module != null && setting != null && "CoordSnapper".equalsIgnoreCase(module.getName()) && setting.matchesName("Webhook");
   }

   public String shortenUrl(String text, int maxLength) {
      String s = text == null ? "" : text.trim();
      if (s.isEmpty()) {
         return "";
      } else {
         int i = Math.max(s.lastIndexOf(47), s.lastIndexOf(92));
         String s1 = i >= 0 && i < s.length() - 1 ? s.substring(i + 1) : s;
         return s1.length() <= maxLength ? "..." + s1 : "..." + s1.substring(s1.length() - maxLength);
      }
   }

   public void drawScaledString(DrawContext context, String text, float x, float y, float scale, int color) {
      if (text != null && !text.isEmpty()) {
         Matrix3x2fStack matrix3x2fStack = context.getMatrices();
         matrix3x2fStack.pushMatrix();
         matrix3x2fStack.translate(x, y);
         matrix3x2fStack.scale(scale, scale);
         this.drawString(context, text, 0, 0, color, false);
         matrix3x2fStack.popMatrix();
      }
   }

   public void drawSettingLabel(DrawContext context, float panelX, float panelY, float panelWidth, float panelHeight, String text, int x, int y, int color) {
      if (text == null) {
         text = "";
      }

      int i = (int)Math.max(0.0F, panelWidth) - (x - (int)panelX) - 4;
      String s = i > 0 ? this.trimToWidth(text, i) : text;
      this.drawString(context, s, x, y, color, false);
   }

   public int getBlockPickerHeight(BlockListSetting setting) {
      return Math.round(this.layoutBlockPicker(0.0F, 147.0F, 0.0F, setting).height);
   }

   public int clampBlockScroll(int itemCount, int scroll) {
      return Math.max(0, Math.min(Math.max(0, itemCount - 5), scroll));
   }

   public BlockSelectLayout layoutMobPicker(float x, float width, float y, EntityListSetting setting) {
      int i = this.getFilteredMobs(setting).size();
      int j = Math.min(5, Math.max(1, i));
      float f = x + 6.0F;
      float f1 = y + 6.0F;
      float f2 = x + width - 30.0F - 6.0F;
      float f3 = Math.max(24.0F, f2 - f - 4.0F);
      float f4 = x + 6.0F;
      float f5 = f1 + 16.0F + 6.0F;
      float f6 = width - 12.0F;
      float f7 = j * 18;
      float f8 = 28.0F + f7 + 6.0F;
      return new BlockSelectLayout(x, y, width, f8, f, f1, f3, 16.0F, f2, f1, 30.0F, 16.0F, f4, f5, f6, f7);
   }

   public List<EntityType<?>> getFilteredMobs(EntityListSetting setting) {
      ArrayList arrayList = new ArrayList<>(setting.filter(this.mobSearchQuery));
      arrayList.sort(
         Comparator.<EntityType, Boolean>comparing(var1x -> !setting.contains((EntityType<?>)var1x))
            .thenComparing(setting::getDisplayName, String.CASE_INSENSITIVE_ORDER)
      );
      return arrayList;
   }

   public int getMobPickerHeight(EntityListSetting setting) {
      return Math.round(this.layoutMobPicker(0.0F, 147.0F, 0.0F, setting).height);
   }

   public int clampMobScroll(int itemCount, int scroll) {
      return Math.max(0, Math.min(Math.max(0, itemCount - 5), scroll));
   }

   public String getMobPickerLabel(EntityListSetting setting) {
      EntityType entitytype = setting.getSelectedMobs().stream().findFirst().orElse(null);
      if (entitytype == null) {
         return "Choose";
      } else {
         int i = setting.size() - 1;
         return i > 0 ? setting.getDisplayName(entitytype) + " +" + i : setting.getDisplayName(entitytype);
      }
   }

   public ItemStack getMobPickerIcon(EntityListSetting setting) {
      EntityType entitytype = setting.getSelectedMobs().stream().findFirst().orElse(null);
      return entitytype == null ? ItemStack.EMPTY : this.getSpawnEggStack(entitytype);
   }

   public ItemStack getSpawnEggStack(EntityType<?> entityType) {
      try {
         SpawnEggItem spawnEggItem = SpawnEggItem.forEntity(entityType);
         if (spawnEggItem != null) {
            return new ItemStack(spawnEggItem);
         }
      } catch (Throwable throwable) {
      }

      return new ItemStack(Items.EGG);
   }

   public void drawMobListRow(DrawContext context, EntityListSetting setting, float x, float width, float y, int rowHeight, float alpha) {
      String s = this.expandedMobsSetting == setting ? "v" : ">";
      int i = this.getTextWidth(s);
      int j = Math.round(x + width - 6.0F - i);
      int k = Math.max(30, j - (Math.round(x) + 10 + this.getTextWidth(setting.getName()) + 14));
      int l = this.scaleAlpha(this.expandedMobsSetting != setting && setting.size() <= 0 ? -6642510 : -1511950, alpha);
      String s1 = setting.size() == 0 ? "Choose" : this.getMobPickerLabel(setting);
      String s2 = this.ellipsize(s1, Math.round((k - 18) / 0.9F));
      int i1 = Math.max(34, Math.min(k, this.getTextWidth(s2) + 22));
      int j1 = j - i1 - 6;
      int k1 = this.scaleAlpha(setting.size() > 0 ? COLOR_ACCENT_DIM : -15195855, alpha);
      ItemStack itemStack = this.getMobPickerIcon(setting);
      this.drawString(context, setting.getName(), Math.round(x) + 10, Math.round(y) + 4, l, false);
      Render2D.drawRoundedRect(context, j1, y + 2.0F, i1, 12.0F, 5.0F, k1, false);
      Render2D.drawRoundedOutline(context, j1, y + 2.0F, i1, 12.0F, 5.0F, 1.0F, this.scaleAlpha(0, alpha), false);
      if (!itemStack.isEmpty()) {
         context.drawItem(itemStack, j1 + 2, Math.round(y) + 1);
      }

      this.drawScaledString(context, s2, j1 + (itemStack.isEmpty() ? 6 : 16), y + 4.0F, 0.9F, this.scaleAlpha(-1511950, alpha));
      this.drawString(context, s, j, Math.round(y) + 4, this.scaleAlpha(-6642510, alpha), false);
   }

   public void drawMobPicker(DrawContext context, EntityListSetting setting, float x, float width, float y, int var6, int var7) {
      BlockSelectLayout blockSelectLayout = this.layoutMobPicker(x, width, y, setting);
      List list = this.getFilteredMobs(setting);
      this.mobPickerScroll = this.clampMobScroll(list.size(), this.mobPickerScroll);
      Render2D.drawRoundedRect(context, blockSelectLayout.x, blockSelectLayout.y, blockSelectLayout.width, blockSelectLayout.height, 6.0F, -15195855, false);
      Render2D.drawRoundedOutline(context, blockSelectLayout.x, blockSelectLayout.y, blockSelectLayout.width, blockSelectLayout.height, 6.0F, 1.0F, 0, false);
      int i = this.mobSearchActive && this.expandedMobsSetting == setting ? COLOR_ACCENT : -14274495;
      Render2D.drawRoundedRect(
         context, blockSelectLayout.searchX, blockSelectLayout.searchY, blockSelectLayout.searchWidth, blockSelectLayout.searchHeight, 5.0F, COLOR_PANEL_BG, false
      );
      Render2D.drawRoundedOutline(
         context, blockSelectLayout.searchX, blockSelectLayout.searchY, blockSelectLayout.searchWidth, blockSelectLayout.searchHeight, 5.0F, 1.0F, i, false
      );
      Render2D.drawRoundedRect(context, blockSelectLayout.clearX, blockSelectLayout.clearY, blockSelectLayout.clearWidth, blockSelectLayout.clearHeight, 5.0F, 0, false);
      Render2D.drawRoundedOutline(context, blockSelectLayout.clearX, blockSelectLayout.clearY, blockSelectLayout.clearWidth, blockSelectLayout.clearHeight, 5.0F, 1.0F, 0, false);
      String s = this.mobSearchQuery.isEmpty() ? "Search mobs..." : this.mobSearchQuery;
      if (this.mobSearchActive && this.expandedMobsSetting == setting && System.currentTimeMillis() / 500L % 2L == 0L) {
         s = s + "_";
      }

      this.drawSettingLabel(
         context,
         blockSelectLayout.searchX,
         blockSelectLayout.searchY,
         Math.max(0.0F, blockSelectLayout.searchWidth),
         Math.max(0.0F, blockSelectLayout.searchHeight),
         s,
         Math.round(blockSelectLayout.searchX) + 6,
         Math.round(blockSelectLayout.searchY) + 4,
         this.mobSearchQuery.isEmpty() && !this.mobSearchActive ? -6642510 : -1511950
      );
      this.drawString(context, "Clear", Math.round(blockSelectLayout.clearX) + 4, Math.round(blockSelectLayout.clearY) + 4, -6642510, false);
      if (list.isEmpty()) {
         this.drawString(context, "No mobs found", Math.round(blockSelectLayout.listX) + 6, Math.round(blockSelectLayout.listY) + 4, -6642510, false);
      } else {
         int j = Math.min(5, list.size());
         boolean flag = list.size() > j;

         for (int k = 0; k < j; k++) {
            int l = this.mobPickerScroll + k;
            if (l >= list.size()) {
               break;
            }

            EntityType entitytype = (EntityType)list.get(l);
            float f = blockSelectLayout.listY + k * 18;
            boolean flag1 = var6 >= blockSelectLayout.listX
               && var6 <= blockSelectLayout.listX + blockSelectLayout.listWidth
               && var7 >= f
               && var7 <= f + 18.0F - 2.0F;
            boolean flag2 = setting.contains(entitytype);
            Render2D.drawRoundedRect(context, blockSelectLayout.listX, f, blockSelectLayout.listWidth, 16.0F, 5.0F, flag2 ? 857419306 : (flag1 ? 872415231 : 0), false);
            Render2D.drawRoundedOutline(context, blockSelectLayout.listX, f, blockSelectLayout.listWidth, 16.0F, 5.0F, 1.0F, 0, false);
            ItemStack itemStack = this.getSpawnEggStack(entitytype);
            int i1 = Math.round(blockSelectLayout.listX) + 5;
            if (!itemStack.isEmpty()) {
               context.drawItem(itemStack, Math.round(blockSelectLayout.listX) + 2, Math.round(f) + 1);
               i1 += 16;
            }

            float f1 = blockSelectLayout.listX + blockSelectLayout.listWidth - 10.0F;
            this.drawScaledString(context, this.ellipsize(setting.getDisplayName(entitytype), Math.round((f1 - i1 - 4.0F) / 0.9F)), i1, f + 4.0F, 0.9F, flag2 ? COLOR_ACCENT : -1511950);
            Render2D.drawRoundedRect(context, f1, f + 5.0F, 6.0F, 6.0F, 2.5F, flag2 ? COLOR_ACCENT : -15195855, false);
            Render2D.drawRoundedOutline(context, f1, f + 5.0F, 6.0F, 6.0F, 2.5F, 1.0F, flag2 ? COLOR_ACCENT : -14274495, false);
         }

         if (flag) {
            int j1 = Math.max(1, list.size() - j);
            float f2 = blockSelectLayout.listX + blockSelectLayout.listWidth - 4.0F;
            float f3 = blockSelectLayout.listY + 1.0F;
            float f4 = blockSelectLayout.listHeight - 2.0F;
            float f5 = Math.max(12.0F, f4 * ((float)j / list.size()));
            float f6 = (f4 - f5) * ((float)this.mobPickerScroll / j1);
            Render2D.drawRoundedRect(context, f2, f3, 4.0F, f4, 2.0F, COLOR_PANEL_BG, false);
            Render2D.drawRoundedRect(context, f2, f3 + f6, 4.0F, f5, 2.0F, COLOR_ACCENT_DIM, false);
         }
      }
   }

   public ColorPickerLayout layoutColorPicker(float x, float width, float y, int rowHeight) {
      float f = x + 4.0F;
      float f1 = y + rowHeight + 6.0F;
      float f2 = 80.0F;
      float f3 = 80.0F;
      float f4 = f + f2 + 6.0F;
      float f5 = 24.0F;
      return new ColorPickerLayout(f, f1, f2, f3, f4, f5);
   }

   public boolean handleCategoryAreaClick(double mouseX, double mouseY) {
      int i = this.getHeaderHeight() + this.verticalScroll;

      for (int j = 0; j < CACHED_CATEGORIES.length; j++) {
         Category category = CACHED_CATEGORIES[j];
         int k = this.getCategoryX(category, j);
         int l = i + 22 + 6;

         for (Module module : ModuleManager.INSTANCE.getModulesInCategory(category)) {
            if (this.matchesSearch(module)) {
               l += 19;
               if (module.isExpanded()) {
                  l += 19;
                  l += 19;

                  for (Setting setting : module.getSettings()) {
                     if (setting == this.activeColorSetting && setting.getValue() instanceof Color) {
                        ColorPickerLayout colorPickerLayout = this.layoutColorPicker(k + 4, 147.0F, l, 17);
                        if (this.colorDragMode == SortMode.FIELD) {
                           this.pickColorFromField(setting, colorPickerLayout, mouseX, mouseY);
                        } else if (this.colorDragMode == SortMode.ALPHA) {
                           this.pickHueFromSlider(setting, colorPickerLayout, mouseY);
                        }

                        return true;
                     }

                     l += 19;
                     if (setting instanceof BlockListSetting blockListSetting && this.expandedBlocksSetting == blockListSetting) {
                        l += this.getBlockPickerHeight(blockListSetting);
                     }

                     if (setting instanceof EntityListSetting entityListSetting && this.expandedMobsSetting == entityListSetting) {
                        l += this.getMobPickerHeight(entityListSetting);
                     }

                     if (setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
                        l += 112;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public EntitySelectContext findOpenMobPicker() {
      if (this.expandedMobsSetting == null) {
         return null;
      } else {
         int i = this.getHeaderHeight() + this.verticalScroll;

         for (int j = 0; j < CACHED_CATEGORIES.length; j++) {
            Category category = CACHED_CATEGORIES[j];
            int k = this.getCategoryX(category, j);
            int l = i + 22 + 6;

            for (Module module : ModuleManager.INSTANCE.getModulesInCategory(category)) {
               if (this.matchesSearch(module)) {
                  l += 19;
                  if (module.isExpanded()) {
                     l += 19;
                     l += 19;

                     for (Setting setting : module.getSettings()) {
                        if (setting == this.expandedMobsSetting) {
                           return new EntitySelectContext(this.expandedMobsSetting, this.layoutMobPicker(k + 4, 147.0F, l + 19, this.expandedMobsSetting));
                        }

                        l += 19;
                        if (setting instanceof BlockListSetting blockListSetting && this.expandedBlocksSetting == blockListSetting) {
                           l += this.getBlockPickerHeight(blockListSetting);
                        }

                        if (setting instanceof EntityListSetting entityListSetting && this.expandedMobsSetting == entityListSetting) {
                           l += this.getMobPickerHeight(entityListSetting);
                        }

                        if (setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
                           l += 112;
                        }
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   public BlockSelectContext findOpenBlockPicker() {
      if (this.expandedBlocksSetting == null) {
         return null;
      } else {
         int i = this.getHeaderHeight() + this.verticalScroll;

         for (int j = 0; j < CACHED_CATEGORIES.length; j++) {
            Category category = CACHED_CATEGORIES[j];
            int k = this.getCategoryX(category, j);
            int l = i + 22 + 6;

            for (Module module : ModuleManager.INSTANCE.getModulesInCategory(category)) {
               if (this.matchesSearch(module)) {
                  l += 19;
                  if (module.isExpanded()) {
                     l += 19;
                     l += 19;

                     for (Setting setting : module.getSettings()) {
                        if (setting == this.expandedBlocksSetting) {
                           return new BlockSelectContext(this.expandedBlocksSetting, this.layoutBlockPicker(k + 4, 147.0F, l + 19, this.expandedBlocksSetting));
                        }

                        l += 19;
                        if (setting instanceof BlockListSetting blockListSetting && this.expandedBlocksSetting == blockListSetting) {
                           l += this.getBlockPickerHeight(blockListSetting);
                        }

                        if (setting instanceof EntityListSetting entityListSetting && this.expandedMobsSetting == entityListSetting) {
                           l += this.getMobPickerHeight(entityListSetting);
                        }

                        if (setting.getValue() instanceof Color && this.expandedColorSetting == setting) {
                           l += 112;
                        }
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   public boolean isCloseKey(KeyInput input) {
      return input.isEscape() || input.isEnter();
   }

   public boolean handleBlockSearchKey(KeyInput input) {
      if (input.getKeycode() == 259) {
         this.blockSearchQuery = this.removeLastChar(this.blockSearchQuery);
         this.blockPickerScroll = 0;
         return true;
      } else if (input.isPaste()) {
         this.blockSearchQuery = this.blockSearchQuery + this.getSanitizedClipboard();
         this.blockPickerScroll = 0;
         return true;
      } else if (!input.isEscape() && !input.isEnter()) {
         return true;
      } else {
         this.blockSearchActive = false;
         return true;
      }
   }

   public boolean handleMobSearchKey(KeyInput input) {
      if (input.getKeycode() == 259) {
         this.mobSearchQuery = this.removeLastChar(this.mobSearchQuery);
         this.mobPickerScroll = 0;
         return true;
      } else if (input.isPaste()) {
         this.mobSearchQuery = this.mobSearchQuery + this.getSanitizedClipboard();
         this.mobPickerScroll = 0;
         return true;
      } else if (!input.isEscape() && !input.isEnter()) {
         return true;
      } else {
         this.mobSearchActive = false;
         return true;
      }
   }

   public boolean handleStringSettingKey(KeyInput input) {
      if (input.getKeycode() == 259) {
         this.listeningString.setValue(this.removeLastChar(this.listeningString.getValue()));
         return true;
      } else if (input.isPaste()) {
         this.listeningString.setValue(this.listeningString.getValue() + this.getSanitizedClipboard());
         return true;
      } else if (!input.isEscape() && !input.isEnter()) {
         return true;
      } else {
         this.listeningString = null;
         return true;
      }
   }

   public int getHeaderHeight() {
      return 16;
   }

   public int getMaxCategoryHeight() {
      int i = 0;

      for (Category category : CACHED_CATEGORIES) {
         i = Math.max(i, this.getCategoryHeight(category));
      }

      return i;
   }

   public int clampVerticalScroll(int scroll) {
      int i = Math.max(0, this.getScaledHeight() - this.getHeaderHeight() - 16);
      int j = Math.min(0, i - this.getMaxCategoryHeight());
      return Math.max(j, Math.min(0, scroll));
   }

   public String getSanitizedClipboard() {
      return this.stripControlChars(MinecraftClient.getInstance().keyboard.getClipboard());
   }

   public String stripControlChars(String text) {
      if (text != null && !text.isEmpty()) {
         StringBuilder stringBuilder = new StringBuilder(text.length());
         text.codePoints().filter(var0 -> !Character.isISOControl(var0)).forEach(stringBuilder::appendCodePoint);
         return stringBuilder.toString();
      } else {
         return "";
      }
   }

   public String removeLastChar(String text) {
      return text != null && !text.isEmpty() ? text.substring(0, text.offsetByCodePoints(text.length(), -1)) : "";
   }

   public void pickColorFromField(Setting<Color> setting, ColorPickerLayout layout, double mouseX, double mouseY) {
      Color color = (Color)setting.getValue();
      float f = this.getHue(color);
      float f1 = this.clamp01((float)((mouseX - layout.fieldX) / layout.fieldWidth));
      float f2 = 1.0F - this.clamp01((float)((mouseY - layout.fieldY) / layout.fieldHeight));
      int i = Color.HSBtoRGB(f, f1, f2);
      setting.setValue(new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, color.getAlpha()));
   }

   public void pickHueFromSlider(Setting<Color> setting, ColorPickerLayout layout, double mouseY) {
      float f = this.clamp01((float)((mouseY - layout.fieldY) / layout.fieldHeight));
      Color color = (Color)setting.getValue();
      int i = Color.HSBtoRGB(f, this.getSaturation(color), this.getBrightness(color));
      setting.setValue(new Color(i >> 16 & 0xFF, i >> 8 & 0xFF, i & 0xFF, color.getAlpha()));
   }

   public boolean isInside(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public boolean isCompactModule(Module module) {
      if (module == null) {
         return false;
      } else {
         String s = module.getName() == null ? "" : module.getName().toLowerCase().replace(" ", "");
         return s.equals("swingspeed")
            || s.equals("freelook")
            || s.equals("fastplace")
            || s.equals("playeresp")
            || s.equals("storageESP")
            || s.equals("freecam")
            || s.equals("holeesp")
            || s.equals("jumpcircles")
            || s.equals("autototem")
            || s.equals("autoinvtotem")
            || s.equals("hitbox")
            || s.equals("anchormacro")
            || s.equals("autocrystal")
            || s.equals("doubleanchor")
            || s.equals("triggerbot")
            || s.equals("shieldbreaker")
            || s.equals("spotifyhud")
            || s.equals("water+")
            || s.equals("hud")
            || s.equals("spawnernotifier")
            || s.equals("nametags");
      }
   }

   public void updateNumericSetting(Module module, Setting<?> setting, double mouseX, int rowX) {
      double d0 = Math.max(0.0, Math.min(1.0, (mouseX - (rowX + 10)) / 135.0));
      boolean flag = this.isCompactModule(module);
      if (setting.getValue() instanceof Float && setting.getMin() instanceof Float && setting.getMax() instanceof Float) {
         float f = (Float)setting.getMin();
         float f1 = (Float)setting.getMax();
         float f2 = (float)(f + (f1 - f) * d0);
         if (!flag) {
            f2 = Math.round(f2);
         }

         ((Setting)setting).setValue(f2);
      } else if (setting.getValue() instanceof Integer && setting.getMin() instanceof Integer && setting.getMax() instanceof Integer) {
         int j = (Integer)setting.getMin();
         int i = (Integer)setting.getMax();
         int k = (int)Math.round(j + (i - j) * d0);
         ((Setting)setting).setValue(Math.max(j, Math.min(i, k)));
      } else {
         if (setting.getValue() instanceof Double && setting.getMin() instanceof Double && setting.getMax() instanceof Double) {
            double d1 = (Double)setting.getMin();
            double d2 = (Double)setting.getMax();
            double d3 = d1 + (d2 - d1) * d0;
            if (!flag) {
               d3 = Math.round(d3);
            }

            ((Setting)setting).setValue(d3);
         }
      }
   }

   public float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   public int withAlphaFraction(int argb, float alpha) {
      int i = Math.max(0, Math.min(255, Math.round(alpha * 255.0F)));
      return argb & 16777215 | i << 24;
   }

   public int scaleAlpha(int argb, float factor) {
      int i = argb >> 24 & 0xFF;
      int j = Math.max(0, Math.min(255, Math.round(i * factor)));
      return argb & 16777215 | j << 24;
   }

   public int toArgb(Color color, float alpha) {
      int i = Math.max(0, Math.min(255, Math.round(color.getAlpha() * alpha)));
      return i << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }

   public float easeOutCubic(float t) {
      float f = this.clamp01(t);
      return 1.0F - (float)Math.pow(1.0F - f, 3.0);
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   public static String watermarkFragment() {
      return "W";
   }
}
