package com.water.module.modules.client;

import com.water.gui.FontType;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.ModeSetting;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.util.SpotifyAuth;
import java.awt.Color;
import org.lwjgl.glfw.GLFW;

public final class Water extends Module {
   public static Water INSTANCE;
   public final Setting<Color> accentColor = new Setting<>("Color", new Color(255, 0, 200, 255));
   public final Setting<Color> bgColor = new Setting<>("Background", new Color(30, 15, 40, 255));
   public final Setting<Boolean> animations = new Setting<>("Animations", true);
   public final Setting<Double> tracerWidth = new Setting<>("Tracer Width", 2.0, 1.0, 6.0);
   public final Setting<Double> guiRoundness = new Setting<>("GUI Roundness", 8.0, 0.0, 20.0);
   public final Setting<Integer> glassIntensity = new Setting<>("Glass Intensity", 60, 0, 100);
   public final Setting<Integer> accentGlow = new Setting<>("Accent Glow", 50, 0, 100);
   public final ModeSetting rowStyle = new ModeSetting("Row Style", "Filled", "Filled", "Minimal", "Outlined");
   public final ModeSetting headerStyle = new ModeSetting("Header Style", "Solid", "Solid", "Gradient", "Transparent");
   public final ModeSetting panelShape = new ModeSetting("Panel Shape", "Rounded", "Rounded", "Sharp", "Pill");
   public final ModeSetting animSpeed = new ModeSetting("Anim Speed", "Normal", "Slow", "Normal", "Fast", "Off");
   public final ModeSetting layoutMode = new ModeSetting("Layout", "Columns", "Columns", "Single Panel");
   public final Setting<Boolean> notifications = new Setting<>("Notifications", true);
   public final Setting<String> guiKeyName = new Setting<>("GUI Key", "RShift");
   public final ModeSetting menuSize = new ModeSetting("Menu Size", "3", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
   public final Setting<String> spotifyClientId = new Setting<>("Spotify Client ID", "");
   public final ModeSetting fontSetting = new ModeSetting("Font", "Inter", "Inter", "Dekatron", "Minecraft Ten", "Basketball", "Vanilla");
   public static volatile int resolvedGuiKey = 344;
   public static volatile boolean listeningGuiKey = false;

   public Water() {
      super("Water +", Category.CLIENT);
      this.addSetting(this.accentColor);
      this.addSetting(this.bgColor);
      this.addSetting(this.glassIntensity);
      this.addSetting(this.accentGlow);
      this.addSetting(this.rowStyle);
      this.addSetting(this.headerStyle);
      this.addSetting(this.panelShape);
      this.addSetting(this.animSpeed);
      this.addSetting(this.layoutMode);
      this.addSetting(this.animations);
      this.addSetting(this.guiRoundness);
      this.addSetting(this.guiKeyName);
      this.addSetting(this.spotifyClientId);
      this.addSetting(this.fontSetting);
      INSTANCE = this;
   }

   @Override
   public void onTick() {
      if (INSTANCE != null) {
         String s = this.fontSetting.getValue();

         FontType fontType = switch (s) {
            case "Dekatron" -> FontType.DEKATRON;
            case "Minecraft Ten" -> FontType.MINECRAFT_TEN;
            case "Basketball" -> FontType.BASKETBALL;
            case "Vanilla" -> FontType.VANILLA;
            default -> FontType.INTER;
         };
         if (FontRenderer.getFont() != fontType) {
            FontRenderer.setFont(fontType);
         }

         String s1 = this.spotifyClientId.getValue();
         if (s1 != null && !s1.isBlank() && !s1.equals(SpotifyAuth.getClientId())) {
            SpotifyAuth.setClientId(s1);
         }

         resolvedGuiKey = keyNameToCode(this.guiKeyName.getValue());
      }
   }

   public static void setGuiKey(int var0, String text) {
      if (INSTANCE != null) {
         INSTANCE.guiKeyName.setValue(text);
         resolvedGuiKey = var0;
         listeningGuiKey = false;
      }
   }

   public static String getGuiKeyName() {
      if (INSTANCE == null) {
         return "RShift";
      } else {
         String s = INSTANCE.guiKeyName.getValue();
         return s != null && !s.isBlank() ? s : "RShift";
      }
   }

   public static float getAccentGlow() {
      return INSTANCE == null ? 0.5F : Math.max(0, Math.min(100, INSTANCE.accentGlow.getValue())) / 100.0F;
   }

   public static String getRowStyle() {
      return INSTANCE == null ? "Filled" : INSTANCE.rowStyle.getValue();
   }

   public static String getHeaderStyle() {
      return INSTANCE == null ? "Solid" : INSTANCE.headerStyle.getValue();
   }

   public static float getPanelRadius() {
      if (INSTANCE == null) {
         return 8.0F;
      } else {
         String s = INSTANCE.panelShape.getValue();

         return switch (s) {
            case "Sharp" -> 0.0F;
            case "Pill" -> 20.0F;
            default -> getGuiRoundness();
         };
      }
   }

   public static float getAnimSpeed() {
      if (INSTANCE == null) {
         return 1.0F;
      } else {
         String s = INSTANCE.animSpeed.getValue();

         return switch (s) {
            case "Slow" -> 0.4F;
            case "Fast" -> 2.5F;
            case "Off" -> 999.0F;
            default -> 1.0F;
         };
      }
   }

   public static boolean areAnimationsActive() {
      return !areAnimationsEnabled() ? false : !"Off".equals(INSTANCE == null ? "Normal" : INSTANCE.animSpeed.getValue());
   }

   public static String getLayoutMode() {
      return INSTANCE == null ? "Columns" : INSTANCE.layoutMode.getValue();
   }

   public static int getGuiKeyCode() {
      return resolvedGuiKey > 0 ? resolvedGuiKey : 344;
   }

   public static boolean areNotificationsEnabled() {
      return INSTANCE == null ? true : INSTANCE.notifications.getValue();
   }

   public static float getGuiRoundness() {
      if (INSTANCE == null) {
         return 8.0F;
      } else {
         double d0 = INSTANCE.guiRoundness.getValue();
         if (Double.isNaN(d0) || Double.isInfinite(d0)) {
            d0 = 8.0;
         }

         return (float)Math.max(0.0, Math.min(20.0, d0));
      }
   }

   public static float getGlassIntensity() {
      if (INSTANCE == null) {
         return 0.6F;
      } else {
         int i = INSTANCE.glassIntensity.getValue();
         return Math.max(0, Math.min(100, i)) / 100.0F;
      }
   }

   public static int getAccentArgb() {
      Color color = getAccentColor();
      return color.getAlpha() << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }

   public static Color getAccentColor() {
      return INSTANCE == null ? new Color(255, 0, 200, 255) : INSTANCE.accentColor.getValue();
   }

   public static int getBackgroundArgb() {
      Color color = getBackgroundColor();
      float f = getGlassIntensity();
      int i = color.getAlpha();
      int j = (int)(i * (1.0F - f * 0.82F));
      j = Math.max(15, Math.min(255, j));
      return j << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }

   public static Color getBackgroundColor() {
      if (INSTANCE == null) {
         return new Color(30, 15, 40, 255);
      } else {
         Color color = INSTANCE.bgColor.getValue();
         if (color == null) {
            return new Color(30, 15, 40, 255);
         } else {
            return color.getAlpha() < 200 ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 255) : color;
         }
      }
   }

   public static boolean areAnimationsEnabled() {
      return INSTANCE == null ? true : INSTANCE.animations.getValue();
   }

   public static int getMenuSize() {
      if (INSTANCE == null) {
         return 5;
      } else {
         try {
            return Integer.parseInt(INSTANCE.menuSize.getValue());
         } catch (Exception exception) {
            return 5;
         }
      }
   }

   public static String getSpotifyClientId() {
      if (INSTANCE == null) {
         return "";
      } else {
         String s = INSTANCE.spotifyClientId.getValue();
         return s != null ? s : "";
      }
   }

   public static float getTracerWidth() {
      if (INSTANCE == null) {
         return 2.0F;
      } else {
         double d0 = 2.0;

         try {
            d0 = INSTANCE.tracerWidth.getValue();
         } catch (Exception exception) {
         }

         if (Double.isNaN(d0) || Double.isInfinite(d0)) {
            d0 = 1.0;
         }

         d0 = Math.max(1.0, Math.min(6.0, d0));
         return (float)d0;
      }
   }

   public static int keyNameToCode(String text) {
      if (text == null) {
         return 344;
      }
      return switch (text) {
         case "RShift" -> 344;
         case "LShift" -> 340;
         case "RCtrl" -> 345;
         case "LCtrl" -> 341;
         case "RAlt" -> 346;
         case "LAlt" -> 342;
         case "Enter" -> 257;
         case "Tab" -> 258;
         case "Insert" -> 260;
         case "Delete" -> 261;
         case "Home" -> 268;
         case "End" -> 269;
         case "PageUp" -> 266;
         case "PageDn" -> 267;
         default -> {
            int var3_20;
            if (text.length() == 1) {
               int var3_17;
               yield var3_17 = GLFW.glfwGetKeyScancode((int)text.charAt(0)) > 0 ? (int)text.toUpperCase().charAt(0) : 344;
            }
            if (text.startsWith("F") && text.length() <= 3) {
               try {
                  int var3_18;
                  int var4_21 = Integer.parseInt(text.substring(1));
                  yield var3_18 = 290 + (var4_21 - 1);
               }
               catch (Exception var4_22) {
                  // empty catch block
               }
            }
            if (text.startsWith("KP")) {
               try {
                  int var3_19;
                  int var4_23 = Integer.parseInt(text.substring(2));
                  yield var3_19 = 320 + var4_23;
               }
               catch (Exception var4_24) {
                  // empty catch block
               }
            }
            yield var3_20 = 344;
         }
      };
   }

   public static String watermarkFragment() {
      return "I";
   }
}
