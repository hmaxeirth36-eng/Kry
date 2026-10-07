package com.water.gui;

import com.water.module.modules.client.Water;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;

public final class NotificationManager {
   public static final NotificationManager INSTANCE = new NotificationManager();
   public static final long DISPLAY_MS = 3500L;
   public static final long ANIMATION_MS = 220L;
   public static final int MAX_NOTIFICATIONS = 5;
   public static final float CARD_WIDTH = 170.0F;
   public static final float CARD_HEIGHT = 36.0F;
   public static final float CARD_GAP = 5.0F;
   public static final float ICON_SIZE = 16.0F;
   public static final float ICON_PAD = 7.0F;
   public static final float ACCENT_BAR = 3.0F;
   public final List<Notification> notifications = new CopyOnWriteArrayList<>();

   public NotificationManager() {
   }

   public void pushToggle(String text, boolean enabled) {
      this.pushToggle(text, enabled, ItemStack.EMPTY);
   }

   public void pushToggle(String text, boolean enabled, ItemStack stack) {
      this.push(text, enabled ? "Enabled" : "Disabled", stack, enabled ? Water.getAccentArgb() : -2076576);
   }

   public void renderToasts(DrawContext context) {
      this.render(context);
   }

   public void push(String text, String text2, ItemStack stack, int accentColor) {
      this.notifications
         .add(
            0,
            new Notification(text == null ? "" : text, text2 == null ? "" : text2, stack == null ? ItemStack.EMPTY : stack.copy(), accentColor, Util.getMeasuringTimeMs())
         );

      while (this.notifications.size() > 5) {
         this.notifications.remove(this.notifications.size() - 1);
      }
   }

   public void render(DrawContext context) {
      if (!this.notifications.isEmpty()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         long i = Util.getMeasuringTimeMs();
         this.notifications.removeIf(var2x -> var2x.isExpired(i));
         if (!this.notifications.isEmpty()) {
            context.createNewRootLayer();
            float f = minecraftClient.getWindow().getScaledWidth() - 170.0F - 8.0F;
            float f1 = minecraftClient.getWindow().getScaledHeight() - 36.0F - 8.0F;
            float f2 = Water.getGuiRoundness();

            for (int j = 0; j < this.notifications.size(); j++) {
               Notification notification = this.notifications.get(j);
               float f3 = notification.getAlpha(i);
               if (!(f3 <= 0.0F)) {
                  float f4 = f + (1.0F - f3) * 184.0F;
                  float f5 = f1 - j * 41.0F;
                  int k = scaleAlpha(notification.accentColor, f3);
                  int l = scaleAlpha(Water.getBackgroundArgb(), f3 * 0.97F);
                  int i1 = scaleAlpha(-16777216, f3 * 0.45F);
                  int j1 = scaleAlpha(lerpColor(Water.getBackgroundArgb(), notification.accentColor, 0.25F), f3);
                  Render2D.drawRoundedRect(context, f4 + 2.0F, f5 + 2.0F, 170.0F, 36.0F, f2, i1, false);
                  Render2D.drawRoundedRect(context, f4, f5, 170.0F, 36.0F, f2, l, false);
                  Render2D.drawRoundedOutline(context, f4, f5, 170.0F, 36.0F, f2, 1.0F, j1, false);
                  float f6 = f4 + 7.0F + 3.0F;
                  float f7 = f5 + 10.0F;
                  int k1 = scaleAlpha(-15724528, f3 * 0.7F);
                  Render2D.drawRoundedRect(context, f6 - 2.0F, f7 - 2.0F, 20.0F, 20.0F, f2 * 0.5F, k1, false);
                  if (!notification.stack.isEmpty()) {
                     context.drawItem(notification.stack, (int)f6, (int)f7);
                  }

                  FontRenderer fontRenderer = FontRenderer.INSTANCE;
                  int l1 = scaleAlpha(-1, f3);
                  int i2 = scaleAlpha(notification.accentColor | 0xFF000000, f3);
                  float f8 = f6 + 16.0F + 6.0F;
                  float f9 = f5 + 18.0F - fontRenderer.getWidth("A") * 0.5F - 3.0F;
                  float f10 = f9 + 11.0F;
                  fontRenderer.drawString(context, notification.message, f8, f5 + 8.0F, l1);
                  fontRenderer.drawString(context, notification.details, f8, f5 + 20.0F, i2);
               }
            }
         }
      }
   }

   public static int scaleAlpha(int argb, float factor) {
      int i = Math.max(0, Math.min(255, Math.round((argb >>> 24 & 0xFF) * factor)));
      return argb & 16777215 | i << 24;
   }

   public static int lerpColor(int from, int to, float t) {
      int i = from >> 16 & 0xFF;
      int j = from >> 8 & 0xFF;
      int k = from & 0xFF;
      int l = to >> 16 & 0xFF;
      int i1 = to >> 8 & 0xFF;
      int j1 = to & 0xFF;
      int k1 = (int)(i + (l - i) * t);
      int l1 = (int)(j + (i1 - j) * t);
      int i2 = (int)(k + (j1 - k) * t);
      return 0xFF000000 | k1 << 16 | l1 << 8 | i2;
   }

   public static String watermarkFragment() {
      return "R";
   }
}
