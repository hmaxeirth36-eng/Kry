package com.water.gui;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.client.HUD;
import com.water.module.modules.client.HudElement;
import com.water.module.modules.client.SpotifyHUD;
import com.water.module.modules.client.Water;
import com.water.render.Render2D;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class HudEditor {
   public static final HudEditor INSTANCE = new HudEditor();
   public static final int HANDLE_SIZE = 8;
   public String dragging = null;
   public int grabOffsetX;
   public int grabOffsetY;
   public boolean isDragging = false;
   public String resizing = null;
   public int resizeStartX;
   public int resizeStartY;
   public float resizeStartScale;
   public static volatile boolean isEditing = false;

   public HudEditor() {
   }

   public static float getHudScale() {
      return HUD.getHudScale();
   }

   public boolean onMouseClick(double mouseX, double mouseY, int button) {
      if (button != 0 && button != 1) {
         return false;
      } else {
         for (HudElement hudElement : HudElement.values()) {
            if (HUD.isElementEnabled(hudElement) && isElementVisible(hudElement)) {
               int[] aint = getElementPosition(hudElement);
               int i = getElementWidth(hudElement);
               int j = getElementHeight(hudElement);
               if (button == 0 && isOnResizeHandle(mouseX, mouseY, aint, i, j)) {
                  this.resizing = hudElement.name();
                  this.resizeStartX = (int)mouseX;
                  this.resizeStartY = (int)mouseY;
                  this.resizeStartScale = getElementScale(hudElement);
                  this.isDragging = false;
                  this.dragging = null;
                  isEditing = true;
                  return true;
               }

               if (mouseX >= aint[0] && mouseX <= aint[0] + i && mouseY >= aint[1] && mouseY <= aint[1] + j) {
                  if (button == 1 && hudElement == HudElement.SPOTIFY_HUD) {
                     MinecraftClient minecraftClient = MinecraftClient.getInstance();
                     if (minecraftClient != null) {
                        minecraftClient.setScreen(new ModuleSearchScreen(minecraftClient.currentScreen));
                     }

                     return true;
                  }

                  if (button == 0) {
                     this.dragging = hudElement.name();
                     this.grabOffsetX = (int)(mouseX - aint[0]);
                     this.grabOffsetY = (int)(mouseY - aint[1]);
                     this.isDragging = true;
                     this.resizing = null;
                     isEditing = true;
                     return true;
                  }
               }
            }
         }

         if (button == 0) {
            this.dragging = null;
            this.isDragging = false;
            this.resizing = null;
         }

         return false;
      }
   }

   public boolean isDragging() {
      return this.dragging != null && this.isDragging || this.resizing != null;
   }

   public boolean isResizing() {
      return this.resizing != null;
   }

   public void onMouseDrag(double mouseX, double mouseY) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      boolean flag = minecraftClient != null && minecraftClient.getWindow() != null && GLFW.glfwGetMouseButton(minecraftClient.getWindow().getHandle(), 0) == 1;
      if (!flag) {
         this.onMouseRelease();
      } else if (this.resizing != null) {
         try {
            HudElement hudElement1 = HudElement.valueOf(this.resizing);
            double d0 = mouseX - this.resizeStartX;
            double d1 = mouseY - this.resizeStartY;
            double d2 = d0 + d1;
            float f1 = Math.max(0.5F, Math.min(3.0F, this.resizeStartScale + (float)(d2 * 0.006F)));
            HUD.setElementScale(hudElement1, f1);
         } catch (Exception exception) {
         }
      } else if (this.dragging != null && this.isDragging) {
         try {
            HudElement hudElement = HudElement.valueOf(this.dragging);
            float f = HUD.getElementScale(hudElement);
            if (hudElement == HudElement.SPOTIFY_HUD) {
               int i = (int)Math.round(mouseX - this.grabOffsetX);
               int j = (int)Math.round(mouseY - this.grabOffsetY);
               if (minecraftClient != null && minecraftClient.getWindow() != null) {
                  int k = minecraftClient.getWindow().getScaledWidth();
                  int l = minecraftClient.getWindow().getScaledHeight();
                  int i1 = SpotifyHUD.getWidth();
                  int j1 = SpotifyHUD.getHeight();
                  i = Math.max(0, Math.min(i, k - i1));
                  j = Math.max(0, Math.min(j, l - j1));
               }

               HUD.setPosition(hudElement, i, j);
            } else {
               int l1 = (int)Math.round((mouseX - this.grabOffsetX) / f);
               int i2 = (int)Math.round((mouseY - this.grabOffsetY) / f);
               if (minecraftClient != null && minecraftClient.getWindow() != null) {
                  int j2 = minecraftClient.getWindow().getScaledWidth();
                  int k2 = minecraftClient.getWindow().getScaledHeight();
                  int[] aint1 = hudElement == HudElement.MODULE_LIST ? HUD.getModuleListBounds() : HUD.getElementBounds(hudElement);
                  int l2 = aint1[2];
                  int k1 = aint1[3];
                  if (l2 < 1) {
                     l2 = 1;
                  }

                  if (k1 < 1) {
                     k1 = 1;
                  }

                  if (j2 > 0) {
                     l1 = Math.max(0, Math.min(l1, (int)((j2 - l2 * f) / f)));
                  }

                  if (k2 > 0) {
                     i2 = Math.max(0, Math.min(i2, (int)((k2 - k1 * f) / f)));
                  }
               }

               if (hudElement == HudElement.MODULE_LIST) {
                  int[] aint = HUD.getModuleListBounds();
                  HUD.setPosition(hudElement, l1 + aint[2], i2);
               } else {
                  HUD.setPosition(hudElement, l1, i2);
               }
            }
         } catch (Exception exception1) {
         }
      }
   }

   public void onMouseRelease() {
      this.dragging = null;
      this.isDragging = false;
      this.resizing = null;
      isEditing = false;
   }

   public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
      if (amount == 0.0) {
         return false;
      } else if (!HUD.isElementEnabled(HudElement.SPOTIFY_HUD)) {
         return false;
      } else {
         int[] aint = HUD.getPosition(HudElement.SPOTIFY_HUD);
         int i = SpotifyHUD.getWidth();
         int j = SpotifyHUD.getHeight();
         if (!(mouseX < aint[0]) && !(mouseX > aint[0] + i) && !(mouseY < aint[1]) && !(mouseY > aint[1] + j)) {
            SpotifyHUD.setScale(SpotifyHUD.getScale() + (float)(amount * 0.08));
            return true;
         } else {
            return false;
         }
      }
   }

   public void render(DrawContext context, int mouseX, int mouseY) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         int i = Water.getAccentArgb();
         float f = Water.getGuiRoundness();

         for (HudElement hudElement : HudElement.values()) {
            if (HUD.isElementEnabled(hudElement) && (isElementVisible(hudElement) || hudElement == HudElement.SPOTIFY_HUD)) {
               int[] aint = getElementPosition(hudElement);
               int j = getElementWidth(hudElement);
               int k = getElementHeight(hudElement);
               if (hudElement == HudElement.MODULE_LIST) {
                  TextRenderer textrenderer = minecraftClient.textRenderer;
                  ArrayList arrayList = new ArrayList();

                  for (Module module : ModuleManager.INSTANCE.getModules()) {
                     if (module.isEnabled() && module.getCategory() != Category.CLIENT) {
                        arrayList.add(module);
                     }
                  }

                  if (arrayList.isEmpty()) {
                     continue;
                  }

                  arrayList.sort(Comparator.<Module>comparingInt(var1x -> textrenderer.getWidth(var1x.getName())).reversed());
                  int[] aint1 = HUD.getPosition(HudElement.MODULE_LIST);
                  int k1 = aint1[1];

                  for (Module module1 : (Iterable<Module>)arrayList) {
                     int l = textrenderer.getWidth(module1.getName()) + 12;
                     byte b0 = 13;
                     int i1 = aint1[0] - l;
                     Render2D.drawRoundedOutline(context, i1, k1, l, b0, 3.0F, 1.0F, i, false);
                     k1 += b0 + 1;
                  }
               } else if (hudElement == HudElement.POTION_EFFECTS) {
                  if (minecraftClient.player == null) {
                     continue;
                  }

                  TextRenderer textRenderer1 = minecraftClient.textRenderer;
                  ArrayList<StatusEffectInstance> arrayList1 = new ArrayList<>(minecraftClient.player.getStatusEffects());
                  arrayList1.sort(Comparator.comparingInt(var1x -> textRenderer1.getWidth(HUD.getEffectDisplayName(var1x))));
                  int[] aint2 = HUD.getPosition(HudElement.POTION_EFFECTS);
                  int l1 = aint2[1];

                  for (StatusEffectInstance statusEffectInstance : (Iterable<StatusEffectInstance>)arrayList1) {
                     String s = HUD.getEffectDisplayName(statusEffectInstance);
                     int j2 = textRenderer1.getWidth(s) + 14;
                     byte b1 = 14;
                     Render2D.drawRoundedOutline(context, aint2[0], l1, j2, b1, 5.0F, 1.0F, i, false);
                     l1 += b1 + 3;
                  }
               } else {
                  Render2D.drawRoundedOutline(context, aint[0], aint[1], j, k, f, 1.0F, i, false);
               }

               if (hudElement != HudElement.MODULE_LIST && hudElement != HudElement.POTION_EFFECTS) {
                  boolean flag = isOnResizeHandle(mouseX, mouseY, aint, j, k);
                  boolean flag1 = this.resizing != null && this.resizing.equals(hudElement.name());
                  int j1 = flag1 ? i : (flag ? lerpColor(i, -1, 0.3F) : i & 16777215 | -1728053248);
                  float f1 = aint[0] + j - 8;
                  float f2 = aint[1] + k - 8;
                  Render2D.drawRoundedRect(context, f1, f2, 8.0F, 8.0F, Math.min(f, 4.0F), j1, false);
                  int i2 = -855638017;
                  context.fill((int)(f1 + 2.0F), (int)(f2 + 8.0F - 3.0F), (int)(f1 + 8.0F - 1.0F), (int)(f2 + 8.0F - 2.0F), i2);
                  context.fill((int)(f1 + 2.0F), (int)(f2 + 8.0F - 5.0F), (int)(f1 + 8.0F - 3.0F), (int)(f2 + 8.0F - 4.0F), i2);
               }
            }
         }
      }
   }

   public static boolean isOnResizeHandle(double mouseX, double mouseY, int[] var4, int width, int height) {
      return mouseX >= var4[0] + width - 8 && mouseX <= var4[0] + width && mouseY >= var4[1] + height - 8 && mouseY <= var4[1] + height;
   }

   public static float getElementScale(HudElement element) {
      return HUD.getElementScale(element);
   }

   public static int lerpColor(int from, int to, float t) {
      int i = from >> 16 & 0xFF;
      int j = from >> 8 & 0xFF;
      int k = from & 0xFF;
      int l = from >> 24 & 0xFF;
      int i1 = to >> 16 & 0xFF;
      int j1 = to >> 8 & 0xFF;
      int k1 = to & 0xFF;
      int l1 = to >> 24 & 0xFF;
      return (int)(l + (l1 - l) * t) << 24 | (int)(i + (i1 - i) * t) << 16 | (int)(j + (j1 - j) * t) << 8 | (int)(k + (k1 - k) * t);
   }

   public static boolean isElementVisible(HudElement element) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient == null) {
         return false;
      } else {
         return switch (element) {
            case ARMOR -> {
               if (minecraftClient.player == null) {
                  yield false;
               } else {
                  EquipmentSlot[] aequipmentslot = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

                  for (EquipmentSlot equipmentSlot : aequipmentslot) {
                     ItemStack itemStack = minecraftClient.player.getEquippedStack(equipmentSlot);
                     if (itemStack != null && !itemStack.isEmpty()) {
                        yield true;
                     }
                  }

                  yield false;
               }
            }
            case POTION_EFFECTS -> minecraftClient.player == null ? false : !minecraftClient.player.getStatusEffects().isEmpty();
            case MODULE_LIST -> {
               for (Module module1 : ModuleManager.INSTANCE.getModules()) {
                  if (module1.isEnabled() && module1.getCategory() != Category.CLIENT) {
                     yield true;
                  }
               }

               yield false;
            }
            case KEYBINDS -> {
               for (Module module : ModuleManager.INSTANCE.getModules()) {
                  if (module.getBind() != 0) {
                     yield true;
                  }
               }

               yield false;
            }
            default -> true;
         };
      }
   }

   public static int[] getElementPosition(HudElement element) {
      float f = HUD.getElementScale(element);
      if (element == HudElement.MODULE_LIST) {
         int[] aint1 = HUD.getModuleListBounds();
         return new int[]{aint1[0], aint1[1]};
      } else {
         int[] aint = HUD.getPosition(element);
         return new int[]{Math.round(aint[0] * f), Math.round(aint[1] * f)};
      }
   }

   public static int getElementWidth(HudElement element) {
      float f = HUD.getElementScale(element);
      if (element == HudElement.MODULE_LIST) {
         return Math.round(HUD.getModuleListBounds()[2] * f);
      } else {
         return element == HudElement.SPOTIFY_HUD ? SpotifyHUD.getWidth() : Math.round(HUD.getElementBounds(element)[2] * f);
      }
   }

   public static int getElementHeight(HudElement element) {
      float f = HUD.getElementScale(element);
      if (element == HudElement.MODULE_LIST) {
         return Math.round(HUD.getModuleListBounds()[3] * f);
      } else {
         return element == HudElement.SPOTIFY_HUD ? SpotifyHUD.getHeight() : Math.round(HUD.getElementBounds(element)[3] * f);
      }
   }

   public static String watermarkFragment() {
      return "A";
   }
}
