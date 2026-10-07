package com.water.module.modules.client;

import com.water.gui.ClickGuiScreen;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.donut.StaffDetector;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

public final class HUD extends Module {
   public static HUD INSTANCE;
   public final Setting<Boolean> watermark = new Setting<>("Watermark", true);
   public final Setting<Boolean> coordinates = new Setting<>("Coordinates", true);
   public final Setting<Boolean> info = new Setting<>("Info", true);
   public final Setting<Boolean> moduleList = new Setting<>("Module List", true);
   public final Setting<Boolean> potionEffects = new Setting<>("Potion Effects", true);
   public final Setting<Boolean> armor = new Setting<>("Armor", true);
   public final Setting<Boolean> keybinds = new Setting<>("Keybinds", true);
   public final Setting<Boolean> notifications = new Setting<>("Notifications", true);
   public final Setting<Boolean> radar = new Setting<>("Radar", true);
   public final Setting<Boolean> spotifyQueue = new Setting<>("Spotify Queue", true);
   public final Setting<Float> opacity = new Setting<>("Opacity", 0.8F, 0.0F, 1.0F);
   public final Setting<Boolean> rainbow = new Setting<>("Rainbow", false);
   public final Setting<Float> rainbowSpeed = new Setting<>("Rainbow Speed", 2.0F, 0.1F, 10.0F);
   public final Setting<Integer> radarSize = new Setting<>("Radar Size", 110, 60, 200);
   public final Setting<Integer> radarRange = new Setting<>("Radar Range", 64, 16, 128);
   public final Setting<Boolean> radarPlayers = new Setting<>("Radar Players", true);
   public final Setting<Boolean> radarHostile = new Setting<>("Radar Hostile", false);
   public final Setting<Boolean> radarPassive = new Setting<>("Radar Passive", false);
   public final Setting<Boolean> radarRotate = new Setting<>("Radar Rotate", true);
   public final Setting<Float> hudScale = new Setting<>("HUD Scale", 1.0F, 0.5F, 3.0F);
   public static final Color PRIMARY = new Color(65, 185, 255, 255);
   public static final int BG_RGB = 2302755;
   public static Color cachedAccent = new Color(65, 185, 255);
   public static Matrix4f cachedMatrix = new Matrix4f();
   public static long lastFrameMs = 0L;
   public static List<Entity> cachedEntities = new ArrayList<>();
   public static long lastEntityScanMs = 0L;
   public static final long ENTITY_SCAN_INTERVAL = 100L;
   public static final EnumMap<HudElement, int[]> positions = new EnumMap<>(HudElement.class);
   public static final EnumMap<HudElement, Float> elementScales = new EnumMap<>(HudElement.class);
   public static final int _lc8d9e118640 = 0;
   public static final Map<Identifier, Identifier> HEAD_CACHE = new ConcurrentHashMap<>();
   public static final Set<Identifier> HEAD_PENDING = Collections.newSetFromMap(new ConcurrentHashMap<>());
   public static int headCacheCounter = 0;

   public static int[] getPosition(HudElement element) {
      return positions.computeIfAbsent(element, HUD::getDefaultPosition);
   }

   public static void setPosition(HudElement element, int x, int y) {
      positions.put(element, new int[]{x, y});
      ModuleManager.INSTANCE.onSettingChanged();
   }

   public static float getElementScale(HudElement element) {
      return element == HudElement.SPOTIFY_HUD ? SpotifyHUD.getScale() : elementScales.getOrDefault(element, 1.0F);
   }

   public static void setElementScale(HudElement element, float scale) {
      if (element == HudElement.SPOTIFY_HUD) {
         SpotifyHUD.setScale(Math.max(0.5F, Math.min(3.0F, scale)));
      } else {
         elementScales.put(element, Math.max(0.5F, Math.min(3.0F, scale)));
      }

      ModuleManager.INSTANCE.onSettingChanged();
   }

   public static int getElementWidth(HudElement element) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return 100;
      } else {
         return switch (element) {
            case WATERMARK -> getTextWidth("Water +") + 20;
            case COORDINATES -> getTextWidth("XYZ: -00000 / -256 / -00000") + 14;
            case INFO -> getTextWidth("999 FPS  \u2022  999 ms  \u2022  23:59:59") + 14;
            case MODULE_LIST -> 120;
            case POTION_EFFECTS -> 110;
            case ARMOR -> getTextWidth(" 100%") + 22;
            case KEYBINDS -> 120;
            case SPOTIFY_HUD -> SpotifyHUD.getWidth();
            case RADAR -> INSTANCE != null ? INSTANCE.radarSize.getValue() : 110;
            case STAFF_LIST -> 180;
         };
      }
   }

   public static int[] getElementBounds(HudElement element) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.player != null) {
         int[] aint1 = getPosition(element);

         return switch (element) {
            case WATERMARK -> new int[]{aint1[0], aint1[1], getTextWidth("WATER+") + 20, 20};
            case COORDINATES -> new int[]{
               aint1[0],
               aint1[1],
               getTextWidth(String.format("%.0f X  %.0f Y  %.0f Z", minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ())) + 14,
               20
            };
            case INFO -> new int[]{aint1[0], aint1[1], getTextWidth(minecraftClient.getCurrentFps() + " FPS  \u2022  0 ms  \u2022  00:00:00") + 14, 20};
            case MODULE_LIST -> getModuleListBounds();
            case POTION_EFFECTS -> {
               ArrayList<StatusEffectInstance> arrayList = new ArrayList<>(minecraftClient.player.getStatusEffects());
               if (arrayList.isEmpty()) {
                  yield new int[]{aint1[0], aint1[1], getElementWidth(element), 20};
               } else {
                  int l = 0;

                  for (StatusEffectInstance statuseffectinstance : (Iterable<StatusEffectInstance>)arrayList) {
                     int i1 = getTextWidth(getEffectDisplayName(statuseffectinstance)) + 14;
                     if (i1 > l) {
                        l = i1;
                     }
                  }

                  yield new int[]{aint1[0], aint1[1], l, arrayList.size() * 22};
               }
            }
            case ARMOR -> new int[]{aint1[0], aint1[1], 70, 90};
            case KEYBINDS -> {
               List list = getBoundModules();
               if (list.isEmpty()) {
                  yield new int[]{aint1[0], aint1[1], getElementWidth(element), 20};
               } else {
                  int j = 0;

                  for (Module module : (Iterable<Module>)list) {
                     int k = getTextWidth(module.getName() + "  " + ClickGuiScreen.keyCodeToName(module.getBind()).toUpperCase()) + 14;
                     if (k > j) {
                        j = k;
                     }
                  }

                  yield new int[]{aint1[0], aint1[1], j, list.size() * 22 + 22};
               }
            }
            case SPOTIFY_HUD -> new int[]{aint1[0], aint1[1], SpotifyHUD.getWidth(), SpotifyHUD.getHeight()};
            case RADAR -> {
               int i = INSTANCE != null ? INSTANCE.radarSize.getValue() : 110;
               yield new int[]{aint1[0], aint1[1], i, i};
            }
            case STAFF_LIST -> new int[]{aint1[0], aint1[1], 180, 100};
         };
      } else {
         int[] aint = getPosition(element);
         return new int[]{aint[0], aint[1], getElementWidth(element), 14};
      }
   }

   public static List<Module> getBoundModules() {
      ArrayList arrayList = new ArrayList();

      for (Module module : ModuleManager.INSTANCE.getModules()) {
         if (module.getBind() != 0) {
            arrayList.add(module);
         }
      }

      arrayList.sort(Comparator.comparing(Module::getName));
      return arrayList;
   }

   public static boolean isElementEnabled(HudElement element) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         return switch (element) {
            case WATERMARK -> INSTANCE.watermark.getValue();
            case COORDINATES -> INSTANCE.coordinates.getValue();
            case INFO -> INSTANCE.info.getValue();
            case MODULE_LIST -> INSTANCE.moduleList.getValue();
            case POTION_EFFECTS -> INSTANCE.potionEffects.getValue();
            case ARMOR -> INSTANCE.armor.getValue();
            case KEYBINDS -> INSTANCE.keybinds.getValue();
            case SPOTIFY_HUD -> SpotifyHUD.isActive();
            case RADAR -> INSTANCE.radar.getValue();
            case STAFF_LIST -> true;
         };
      } else {
         return false;
      }
   }

   public static int[] getDefaultPosition(HudElement element) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      int i = minecraftClient != null ? minecraftClient.getWindow().getScaledWidth() : 800;

      return switch (element) {
         case WATERMARK -> new int[]{5, 5};
         case COORDINATES -> new int[]{5, 30};
         case INFO -> new int[]{5, 55};
         case MODULE_LIST -> new int[]{i - 8, 5};
         case POTION_EFFECTS -> new int[]{5, 80};
         case ARMOR -> new int[]{i - 70, 200};
         case KEYBINDS -> new int[]{5, 130};
         case SPOTIFY_HUD -> new int[]{5, 50};
         case RADAR -> new int[]{i - 125, 10};
         case STAFF_LIST -> new int[]{i - 190, 10};
      };
   }

   public HUD() {
      super("Hud", Category.CLIENT);
      this.addSetting(this.watermark);
      this.addSetting(this.coordinates);
      this.addSetting(this.info);
      this.addSetting(this.moduleList);
      this.addSetting(this.potionEffects);
      this.addSetting(this.armor);
      this.addSetting(this.keybinds);
      this.addSetting(this.notifications);
      this.addSetting(this.opacity);
      this.addSetting(this.rainbow);
      this.addSetting(this.rainbowSpeed);
      this.addSetting(this.spotifyQueue);
      this.addSetting(this.radar);
      this.addSetting(this.radarSize);
      this.addSetting(this.radarRange);
      this.addSetting(this.radarPlayers);
      this.addSetting(this.radarHostile);
      this.addSetting(this.radarPassive);
      this.addSetting(this.radarRotate);
      this.addSetting(this.hudScale);
      INSTANCE = this;
   }

   public static boolean isSpotifyQueueEnabled() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.spotifyQueue.getValue();
   }

   public static float getHudScale() {
      return INSTANCE == null ? 1.0F : INSTANCE.hudScale.getValue();
   }

   public static void setHudScale(float scale) {
      if (INSTANCE != null) {
         INSTANCE.hudScale.setValue(Math.max(0.5F, Math.min(3.0F, scale)));
      }
   }

   public static boolean isModuleListEnabled() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.moduleList.getValue();
   }

   public static boolean areNotificationsEnabled() {
      return INSTANCE != null && INSTANCE.isEnabled() && INSTANCE.notifications.getValue();
   }

   public static void renderHud(DrawContext context) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null && minecraftClient.player != null) {
            if (!(minecraftClient.currentScreen instanceof ClickGuiScreen)) {
               if (!minecraftClient.getDebugHud().shouldShowDebugHud()) {
                  long i = System.currentTimeMillis();
                  if (i != lastFrameMs) {
                     lastFrameMs = i;
                     cachedAccent = Water.getAccentColor();
                     cachedMatrix = Render2D.getProjectionMatrix(context);
                  }

                  float f = INSTANCE.opacity.getValue();
                  float f1 = Water.getGuiRoundness();
                  boolean flag = INSTANCE.rainbow.getValue();
                  int j = INSTANCE.rainbowSpeed.getValue().intValue();
                  float f2 = getHudScale();
                  Color color = Water.getBackgroundColor();
                  Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(f * 255.0F));
                  float f3 = elementScales.getOrDefault(HudElement.WATERMARK, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.watermark.getValue()) {
                     int[] aint = getPosition(HudElement.WATERMARK);
                     Color color2 = flag ? lerpColor(j, 0) : cachedAccent;
                     String s = "Water";
                     String s1 = "+";
                     int k = getTextWidth(s);
                     int l = getTextWidth(s1);
                     int i1 = k + l + 16;
                     byte b0 = 20;
                     fillRoundedRect(context, color1, aint[0], aint[1], aint[0] + i1, aint[1] + b0, f1);
                     drawString(context, s, aint[0] + 7, aint[1] + 5, -1117449);
                     drawString(context, s1, aint[0] + 7 + k, aint[1] + 5, color2.getRGB());
                  }

                  context.getMatrices().popMatrix();
                  f3 = elementScales.getOrDefault(HudElement.COORDINATES, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.coordinates.getValue()) {
                     int[] aint1 = getPosition(HudElement.COORDINATES);
                     Color color3 = flag ? lerpColor(j, 0) : cachedAccent;
                     Color color5 = flag ? lerpColor(j, 40) : darken(cachedAccent);
                     Color color8 = flag ? lerpColor(j, 80) : darken(cachedAccent);
                     String s3 = String.format("%.0f", minecraftClient.player.getX());
                     String s5 = String.format("%.0f", minecraftClient.player.getY());
                     String s8 = String.format("%.0f", minecraftClient.player.getZ());
                     int j6 = getTextWidth(s3) + getTextWidth("  X  ") + getTextWidth(s5) + getTextWidth("  Y  ") + getTextWidth(s8) + getTextWidth("  Z") + 16;
                     fillRoundedRect(context, color1, aint1[0], aint1[1], aint1[0] + j6, aint1[1] + 20, f1);
                     int j1 = aint1[0] + 7;
                     drawString(context, s3, j1, aint1[1] + 5, -1117449);
                     j1 += getTextWidth(s3);
                     drawString(context, "  X", j1, aint1[1] + 5, color3.getRGB());
                     j1 += getTextWidth("  X");
                     drawString(context, "  " + s5, j1, aint1[1] + 5, -1117449);
                     j1 += getTextWidth("  " + s5);
                     drawString(context, "  Y", j1, aint1[1] + 5, color5.getRGB());
                     j1 += getTextWidth("  Y");
                     drawString(context, "  " + s8, j1, aint1[1] + 5, -1117449);
                     j1 += getTextWidth("  " + s8);
                     drawString(context, "  Z", j1, aint1[1] + 5, color8.getRGB());
                  }

                  context.getMatrices().popMatrix();
                  f3 = elementScales.getOrDefault(HudElement.INFO, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.info.getValue()) {
                     int[] aint2 = getPosition(HudElement.INFO);
                     Color color4 = flag ? lerpColor(j, 0) : cachedAccent;
                     Color color6 = flag ? lerpColor(j, 40) : darken(cachedAccent);
                     int i4 = minecraftClient.getCurrentFps();
                     int l4 = 0;

                     try {
                        if (minecraftClient.getNetworkHandler() != null) {
                           PlayerListEntry playerListEntry = minecraftClient.getNetworkHandler().getPlayerListEntry(minecraftClient.player.getUuid());
                           if (playerListEntry != null) {
                              l4 = playerListEntry.getLatency();
                           }
                        }
                     } catch (Exception exception) {
                     }

                     String s6 = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                     String s9 = i4 + " FPS";
                     String s10 = l4 + " ms";
                     String s11 = "  \u2022  ";
                     int k1 = getTextWidth(s9) + getTextWidth(s11) + getTextWidth(s10) + getTextWidth(s11) + getTextWidth(s6) + 16;
                     fillRoundedRect(context, color1, aint2[0], aint2[1], aint2[0] + k1, aint2[1] + 20, f1);
                     int l1 = aint2[0] + 7;
                     drawString(context, s9, l1, aint2[1] + 5, -1117449);
                     l1 += getTextWidth(s9);
                     drawString(context, s11, l1, aint2[1] + 5, color4.getRGB());
                     l1 += getTextWidth(s11);
                     drawString(context, s10, l1, aint2[1] + 5, -1117449);
                     l1 += getTextWidth(s10);
                     drawString(context, s11, l1, aint2[1] + 5, color6.getRGB());
                     l1 += getTextWidth(s11);
                     drawString(context, s6, l1, aint2[1] + 5, -5260086);
                  }

                  context.getMatrices().popMatrix();
                  f3 = elementScales.getOrDefault(HudElement.POTION_EFFECTS, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.potionEffects.getValue()) {
                     ArrayList<StatusEffectInstance> arrayList = new ArrayList<>(minecraftClient.player.getStatusEffects());
                     if (!arrayList.isEmpty()) {
                        arrayList.sort(Comparator.comparingInt(var0x -> getTextWidth(getEffectDisplayName(var0x))));
                        int[] aint6 = getPosition(HudElement.POTION_EFFECTS);
                        int i3 = aint6[1];

                        for (int j4 = 0; j4 < arrayList.size(); j4++) {
                           StatusEffectInstance statuseffectinstance = (StatusEffectInstance)arrayList.get(j4);
                           String s7 = getEffectDisplayName(statuseffectinstance);
                           int k5 = getTextWidth(s7) + 16;
                           byte b2 = 20;
                           int k6 = statuseffectinstance.getEffectType().value().getColor();
                           if (flag) {
                              lerpColor(j, j4 * 15);
                           } else {
                              lerpColor(cachedAccent, darken(cachedAccent), (float)j4 / Math.max(1, arrayList.size() - 1));
                           }

                           fillRoundedRect(context, color1, aint6[0], i3, aint6[0] + k5, i3 + b2, f1);
                           drawString(context, s7, aint6[0] + 8, i3 + 5, -5260086);
                           i3 += b2 + 3;
                        }
                     }
                  }

                  context.getMatrices().popMatrix();
                  f3 = elementScales.getOrDefault(HudElement.ARMOR, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.armor.getValue()) {
                     int[] aint3 = getPosition(HudElement.ARMOR);
                     EquipmentSlot[] aequipmentslot = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                     int j3 = aint3[1];

                     for (EquipmentSlot equipmentSlot : aequipmentslot) {
                        ItemStack itemStack = minecraftClient.player.getEquippedStack(equipmentSlot);
                        if (itemStack != null && !itemStack.isEmpty()) {
                           int l6 = itemStack.isDamageable() && itemStack.getMaxDamage() > 0
                              ? (int)Math.round((1.0 - (double)itemStack.getDamage() / itemStack.getMaxDamage()) * 100.0)
                              : 100;
                           Color color10 = l6 >= 66 ? new Color(74, 222, 128) : (l6 >= 33 ? new Color(250, 204, 21) : new Color(239, 68, 68));
                           String s14 = l6 + "%";
                           int i2 = 16 + getTextWidth(s14) + 8;
                           byte b1 = 18;
                           fillRoundedRect(context, color1, aint3[0], j3, aint3[0] + i2, j3 + b1, f1);
                           context.drawItem(itemStack, aint3[0] + 4, j3 + 1);
                           drawString(context, s14, aint3[0] + 22, j3 + 5, color10.getRGB());
                           j3 += b1 + 3;
                        }
                     }
                  }

                  context.getMatrices().popMatrix();
                  f3 = elementScales.getOrDefault(HudElement.KEYBINDS, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.keybinds.getValue()) {
                     List list = getBoundModules();
                     if (!list.isEmpty()) {
                        int[] aint7 = getPosition(HudElement.KEYBINDS);
                        Color color7 = flag ? lerpColor(j, 0) : cachedAccent;
                        String s2 = "HOTKEYS";
                        int i5 = getTextWidth(s2) + 16;
                        fillRoundedRect(context, color1, aint7[0], aint7[1], aint7[0] + i5, aint7[1] + 20, f1);
                        drawString(context, s2, aint7[0] + 8, aint7[1] + 5, color7.getRGB());
                        int j5 = aint7[1] + 23;

                        for (int l5 = 0; l5 < list.size(); l5++) {
                           Module module1 = (Module)list.get(l5);
                           String s12 = module1.getName().toUpperCase();
                           String s13 = ClickGuiScreen.keyCodeToName(module1.getBind()).toUpperCase();
                           Color color12 = flag ? lerpColor(j, l5 * 15) : lerpColor(cachedAccent, darken(cachedAccent), (float)l5 / Math.max(1, list.size() - 1));
                           int j7 = getTextWidth(s12) + getTextWidth("  ") + getTextWidth(s13) + 16;
                           byte b4 = 18;
                           fillRoundedRect(context, color1, aint7[0], j5, aint7[0] + j7, j5 + b4, f1);
                           drawString(context, s12, aint7[0] + 8, j5 + 4, -5260086);
                           drawString(context, s13, aint7[0] + 8 + getTextWidth(s12) + getTextWidth("  "), j5 + 4, color12.getRGB());
                           j5 += b4 + 3;
                        }
                     }
                  }

                  context.getMatrices().popMatrix();
                  f3 = elementScales.getOrDefault(HudElement.MODULE_LIST, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.moduleList.getValue()) {
                     int[] aint4 = getPosition(HudElement.MODULE_LIST);
                     ArrayList arrayList1 = new ArrayList();

                     for (Module module : ModuleManager.INSTANCE.getModules()) {
                        if (module.isEnabled() && module.getCategory() != Category.CLIENT) {
                           arrayList1.add(module);
                        }
                     }

                     arrayList1.sort(Comparator.<Module>comparingInt(var0x -> getTextWidth(var0x.getName())).reversed());
                     int k3 = aint4[1];

                     for (int k4 = 0; k4 < arrayList1.size(); k4++) {
                        String s4 = ((Module)arrayList1.get(k4)).getName().toUpperCase();
                        Color color9 = flag ? lerpColor(j, k4 * 15) : lerpColor(cachedAccent, darken(cachedAccent), (float)k4 / Math.max(1, arrayList1.size() - 1));
                        int i6 = getTextWidth(s4) + 14;
                        byte b3 = 18;
                        int i7 = aint4[0] - i6;
                        fillRoundedRect(context, color1, i7, k3, i7 + i6, k3 + b3, f1);
                        fillRoundedRect(context, color9, i7 + i6 - 3, k3, i7 + i6, k3 + b3, 1.5);
                        drawString(context, s4, i7 + 6, k3 + 4, color9.getRGB());
                        k3 += b3 + 3;
                     }
                  }

                  context.getMatrices().popMatrix();
                  f3 = elementScales.getOrDefault(HudElement.RADAR, 1.0F);
                  context.getMatrices().pushMatrix();
                  context.getMatrices().scale(f3, f3);
                  if (INSTANCE.radar.getValue() && minecraftClient.world != null) {
                     int[] aint5 = getPosition(HudElement.RADAR);
                     int l2 = INSTANCE.radarSize.getValue();
                     int l3 = INSTANCE.radarRange.getValue();
                     float f16 = l2 / 2.0F;
                     float f17 = aint5[0] + f16;
                     float f18 = aint5[1] + f16;
                     float f19 = INSTANCE.radarRotate.getValue() ? minecraftClient.player.getYaw() : 0.0F;
                     float f20 = f16 * 0.55F;
                     float f21 = 2.5F;
                     Color color11 = flag ? lerpColor(j, 0) : cachedAccent;
                     Color color13 = cachedAccent;
                     int k7 = (int)(f * 80.0F) << 24 | color13.getRed() << 16 | color13.getGreen() << 8 | color13.getBlue();
                     Render2D.drawRoundedRect(context, aint5[0], aint5[1], l2, l2, l2 / 2.0F, k7, false);
                     int l7 = 620756992 | color13.getRed() << 16 | color13.getGreen() << 8 | color13.getBlue();
                     Render2D.drawRoundedRect(context, f17 - f20, f18 - f20, f20 * 2.0F, f20 * 2.0F, f20, l7, false);
                     int j2 = -2147483648 | color13.getRed() << 16 | color13.getGreen() << 8 | color13.getBlue();
                     Render2D.drawRoundedOutline(context, aint5[0], aint5[1], l2, l2, l2 / 2.0F, 1.5F, j2, false);
                     context.fill((int)(f17 - f16 + 4.0F), (int)f18, (int)(f17 + f16 - 4.0F), (int)(f18 + 1.0F), 587202559);
                     context.fill((int)f17, (int)(f18 - f16 + 4.0F), (int)(f17 + 1.0F), (int)(f18 + f16 - 4.0F), 587202559);
                     TextRenderer textrenderer = minecraftClient.textRenderer;
                     String[] astring = new String[]{"N", "E", "S", "W"};
                     float[] afloat = new float[]{0.0F, 90.0F, 180.0F, 270.0F};

                     for (int k2 = 0; k2 < 4; k2++) {
                        float f4 = (float)Math.toRadians(afloat[k2] - f19 - 90.0F);
                        float f5 = (float)(f17 + (f16 - 7.0F) * Math.cos(f4));
                        float f6 = (float)(f18 + (f16 - 7.0F) * Math.sin(f4));
                        context.drawText(textrenderer, astring[k2], (int)(f5 - textrenderer.getWidth(astring[k2]) / 2.0F), (int)(f6 - 4.0F), -1427180809, false);
                     }

                     float f22 = (f16 - 5.0F) / l3;
                     if (i - lastEntityScanMs >= 100L) {
                        lastEntityScanMs = i;
                        cachedEntities = new ArrayList<>(
                           minecraftClient.world
                              .getEntitiesByClass(
                                 Entity.class, minecraftClient.player.getBoundingBox().expand(l3, l3, l3), var1x -> var1x != minecraftClient.player
                              )
                        );
                     }

                     for (Entity entity : cachedEntities) {
                        boolean flag1 = entity instanceof PlayerEntity;
                        boolean flag2 = entity instanceof HostileEntity;
                        boolean flag3 = entity instanceof PassiveEntity;
                        if ((!flag1 || INSTANCE.radarPlayers.getValue())
                           && (!flag2 || INSTANCE.radarHostile.getValue())
                           && (!flag3 || INSTANCE.radarPassive.getValue())
                           && (flag1 || flag2 || flag3)) {
                           double d0 = entity.getX() - minecraftClient.player.getX();
                           double d1 = entity.getZ() - minecraftClient.player.getZ();
                           if (!(Math.sqrt(d0 * d0 + d1 * d1) > l3)) {
                              float f7 = (float)Math.toRadians(f19);
                              float f8 = (float)(d0 * Math.cos(f7) - d1 * Math.sin(f7));
                              float f9 = (float)(d0 * Math.sin(f7) + d1 * Math.cos(f7));
                              float f10 = f17 + f8 * f22;
                              float f11 = f18 + f9 * f22;
                              float f12 = (float)Math.sqrt((f10 - f17) * (f10 - f17) + (f11 - f18) * (f11 - f18));
                              if (f12 > f16 - 4.0F) {
                                 float f13 = (f16 - 4.0F) / f12;
                                 f10 = f17 + (f10 - f17) * f13;
                                 f11 = f18 + (f11 - f18) * f13;
                              }

                              if (flag1) {
                                 byte b5 = 9;
                                 float f14 = f10 - b5 / 2.0F;
                                 float f15 = f11 - b5 / 2.0F;
                                 boolean flag4 = false;
                                 StaffDetector staffDetector = (StaffDetector)ModuleManager.INSTANCE.getModuleByName("Staff Detector");
                                 if (staffDetector != null && staffDetector.isEnabled()) {
                                    flag4 = staffDetector.getDetectedStaff().containsKey(((PlayerEntity)entity).getName().getString());
                                 }

                                 AbstractClientPlayerEntity abstractClientPlayerEntity = (AbstractClientPlayerEntity)entity;
                                 Identifier identifier = null;

                                 try {
                                    SkinTextures skinTextures = abstractClientPlayerEntity.getSkin();

                                    for (Method method : skinTextures.getClass().getMethods()) {
                                       if (method.getParameterCount() == 0) {
                                          method.setAccessible(true);
                                          Object object = null;

                                          try {
                                             object = method.invoke(skinTextures);
                                          } catch (Exception exception2) {
                                             continue;
                                          }

                                          if (object != null) {
                                             if (object instanceof Identifier) {
                                                identifier = (Identifier)object;
                                                break;
                                             }

                                             try {
                                                for (Method method1 : object.getClass().getMethods()) {
                                                   if (method1.getParameterCount() == 0 && method1.getReturnType() == Identifier.class) {
                                                      method1.setAccessible(true);
                                                      identifier = (Identifier)method1.invoke(object);
                                                      if (identifier != null) {
                                                         break;
                                                      }
                                                   }
                                                }
                                             } catch (Exception exception1) {
                                             }

                                             if (identifier != null) {
                                                break;
                                             }
                                          }
                                       }
                                    }
                                 } catch (Exception exception3) {
                                 }

                                 Identifier identifier1 = getHeadTexture(identifier);
                                 if (identifier1 != null) {
                                    Render2D.drawTexture(context, f14, f15, b5, identifier1, -1, 0.0F, false);
                                 } else {
                                    Render2D.drawArc(context, f14, f15, b5, b5, 360.0F, 0.0F, color11.getRGB(), false);
                                 }

                                 if (flag4) {
                                    float f23 = (float)(0.5 + 0.5 * Math.sin(i / 250.0));
                                    int i8 = (int)(f23 * 150.0F);
                                    Render2D.drawRoundedRect(context, f14 - 2.0F, f15 - 2.0F, b5 + 4, b5 + 4, 2.0F, i8 << 24 | 16720418, false);
                                 }
                              } else {
                                 Render2D.drawArc(context, f10 - 2.0F, f11 - 2.0F, 4.0F, 4.0F, 360.0F, 0.0F, flag2 ? -50116 : -11477936, false);
                              }
                           }
                        }
                     }

                     Render2D.drawArc(context, f17 - f21, f18 - f21, f21 * 2.0F, f21 * 2.0F, 360.0F, 0.0F, -1, false);
                     context.fill((int)(f17 - 1.0F), (int)(f18 - 6.0F), (int)(f17 + 1.0F), (int)(f18 - 2.0F), -1);
                  }

                  context.getMatrices().popMatrix();
                  StaffDetector.renderHud(context);
               }
            }
         }
      }
   }

   public static Color lerpColor(int from, int to) {
      return Color.getHSBColor((float)((System.currentTimeMillis() * 3L + to * 175L) % 7200L) / 7200.0F * from % 1.0F, 0.6F, 1.0F);
   }

   public static Color darken(Color color) {
      return new Color(Math.max(0, (int)(color.getRed() * 0.6F)), Math.max(0, (int)(color.getGreen() * 0.6F)), Math.max(0, (int)(color.getBlue() * 0.6F)), 255);
   }

   public static Color lerpColor(Color color, Color color2, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      return new Color(
         (int)(color.getRed() + (color2.getRed() - color.getRed()) * t),
         (int)(color.getGreen() + (color2.getGreen() - color.getGreen()) * t),
         (int)(color.getBlue() + (color2.getBlue() - color.getBlue()) * t),
         255
      );
   }

   public static float getScaleFor(HudElement element) {
      return elementScales.getOrDefault(element, 1.0F);
   }

   public static int[] getScaledPosition(HudElement element) {
      float f = getScaleFor(element);
      int[] aint = getPosition(element);
      return new int[]{Math.round((float)aint[0]), Math.round((float)aint[1])};
   }

   public static void fillRoundedRect(DrawContext context, Color color, double var2, double var4, double var6, double var8, double var10) {
      Render2D.drawRoundedRect(context, (float)var2, (float)var4, (float)(var6 - var2), (float)(var8 - var4), (float)var10, color.getRGB(), false);
   }

   public static void drawString(DrawContext context, String text, int x, int y, int color) {
      FontRenderer.INSTANCE.drawString(context, text, x, y, color);
   }

   public static int getTextWidth(String text) {
      return FontRenderer.INSTANCE.getWidth(text);
   }

   public static String getEffectDisplayName(StatusEffectInstance statusEffectInstance) {
      String s = Registries.STATUS_EFFECT.getId(statusEffectInstance.getEffectType().value()).getPath();
      String[] astring = s.split("_");
      StringBuilder stringBuilder = new StringBuilder();

      for (String s1 : astring) {
         if (!s1.isEmpty()) {
            stringBuilder.append(Character.toUpperCase(s1.charAt(0)));
            if (s1.length() > 1) {
               stringBuilder.append(s1.substring(1));
            }

            stringBuilder.append(' ');
         }
      }

      String s2 = stringBuilder.toString().trim();
      int i = statusEffectInstance.getAmplifier();
      if (i > 0) {
         s2 = s2 + " " + toRoman(i + 1);
      }

      int j = statusEffectInstance.getDuration();
      if (j < 32767) {
         int k = j / 20;
         s2 = s2 + " " + String.format("%d:%02d", k / 60, k % 60);
      }

      return s2;
   }

   public static String toRoman(int level) {
      return switch (level) {
         case 1 -> "I";
         case 2 -> "II";
         case 3 -> "III";
         case 4 -> "IV";
         case 5 -> "V";
         case 6 -> "VI";
         case 7 -> "VII";
         case 8 -> "VIII";
         case 9 -> "IX";
         case 10 -> "X";
         default -> String.valueOf(level);
      };
   }

   public static int[] getModuleListBounds() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return new int[]{0, 0, 120, 14};
      } else {
         int[] aint = getPosition(HudElement.MODULE_LIST);
         ArrayList arrayList = new ArrayList();

         for (Module module : ModuleManager.INSTANCE.getModules()) {
            if (module.isEnabled() && module.getCategory() != Category.CLIENT) {
               arrayList.add(module);
            }
         }

         if (arrayList.isEmpty()) {
            return new int[]{aint[0], aint[1], 80, 18};
         } else {
            arrayList.sort(Comparator.<Module>comparingInt(var0x -> getTextWidth(var0x.getName())).reversed());
            int i = getTextWidth(((Module)arrayList.get(0)).getName()) + 14;
            int j = arrayList.size() * 18 + (arrayList.size() - 1) * 3;
            return new int[]{aint[0] - i, aint[1], i, j};
         }
      }
   }

   public static String getInfoLetter() {
      return "E";
   }

   public static Identifier getHeadTexture(Identifier id) {
      if (id == null) {
         return null;
      } else {
         Identifier identifier = HEAD_CACHE.get(id);
         if (identifier != null) {
            return identifier;
         } else if (HEAD_PENDING.contains(id)) {
            return null;
         } else {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            AbstractTexture abstractTexture = minecraftClient.getTextureManager().getTexture(id);
            if (abstractTexture == null) {
               return null;
            } else {
               HEAD_PENDING.add(id);
               minecraftClient.execute(
                  () -> {
                     try {
                        AbstractTexture abstractTexture1 = minecraftClient.getTextureManager().getTexture(id);
                        if (abstractTexture1 == null) {
                           HEAD_PENDING.remove(id);
                           return;
                        }

                        NativeImage nativeImage = null;
                        if (abstractTexture1 instanceof NativeImageBackedTexture nativeImageBackedTexture) {
                           nativeImage = nativeImageBackedTexture.getImage();
                        }

                        if (nativeImage == null) {
                           for (Field field : abstractTexture1.getClass().getDeclaredFields()) {
                              if (field.getType().getSimpleName().equals("NativeImage")) {
                                 field.setAccessible(true);

                                 try {
                                    nativeImage = (NativeImage)field.get(abstractTexture1);
                                 } catch (Exception exception1) {
                                 }

                                 if (nativeImage != null) {
                                    break;
                                 }
                              }
                           }
                        }

                        if (nativeImage == null) {
                           for (Class oclass = abstractTexture1.getClass().getSuperclass();
                              oclass != null && nativeImage == null;
                              oclass = oclass.getSuperclass()
                           ) {
                              for (Field field1 : oclass.getDeclaredFields()) {
                                 if (field1.getType().getSimpleName().equals("NativeImage")) {
                                    field1.setAccessible(true);

                                    try {
                                       nativeImage = (NativeImage)field1.get(abstractTexture1);
                                    } catch (Exception exception) {
                                    }

                                    if (nativeImage != null) {
                                       break;
                                    }
                                 }
                              }
                           }
                        }

                        if (nativeImage == null) {
                           HEAD_PENDING.remove(id);
                           return;
                        }

                        byte b0 = 16;
                        NativeImage nativeImage1 = new NativeImage(b0, b0, false);
                        int l = nativeImage.getWidth();
                        int i1 = nativeImage.getHeight();

                        for (int j1 = 0; j1 < b0; j1++) {
                           for (int i = 0; i < b0; i++) {
                              int j = 8 + i * 8 / b0;
                              int k = 8 + j1 * 8 / b0;
                              if (j < l && k < i1) {
                                 nativeImage1.setColorArgb(i, j1, nativeImage.getColorArgb(j, k));
                              }
                           }
                        }

                        Identifier identifier1 = Identifier.of("water", "player_head_" + headCacheCounter++);
                        minecraftClient.getTextureManager().registerTexture(identifier1, new NativeImageBackedTexture(() -> "ph", nativeImage1));
                        HEAD_CACHE.put(id, identifier1);
                        HEAD_PENDING.remove(id);
                     } catch (Exception exception2) {
                        HEAD_PENDING.remove(id);
                     }
                  }
               );
               return null;
            }
         }
      }
   }
}
