package com.water.gui;

import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.render.Render2D;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class ModuleTooltipRenderer {
   public static final ModuleTooltipRenderer INSTANCE = new ModuleTooltipRenderer();
   public static final int PANEL_COLOR = -652993496;
   public static final int OUTLINE_COLOR = -869253035;
   public static final int TEXT_COLOR = -854277;
   public static final int ACCENT_COLOR = -9710683;
   public static final float RADIUS = 6.0F;
   public static final int TOP_OFFSET = 72;

   public ModuleTooltipRenderer() {
   }

   public void render(DrawContext context) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.options != null && !minecraftClient.getDebugHud().shouldShowDebugHud()) {
         if (!(minecraftClient.currentScreen instanceof ClickGuiScreen)) {
            TextRenderer textrenderer = minecraftClient.textRenderer;
            ArrayList arrayList = new ArrayList();

            for (Module module : ModuleManager.INSTANCE.getModules()) {
               if (module.isEnabled()) {
                  arrayList.add(module);
               }
            }

            if (!arrayList.isEmpty()) {
               arrayList.sort(Comparator.<Module>comparingInt(var1x -> textrenderer.getWidth(var1x.getName())).reversed());
               int l = minecraftClient.getWindow().getScaledWidth() - 10;
               int i1 = 72;

               for (Module module1 : (Iterable<Module>)arrayList) {
                  String s = module1.getName();
                  int i = textrenderer.getWidth(s);
                  int j = i + 14;
                  byte b0 = 14;
                  int k = l - j;
                  Render2D.drawRoundedRect(context, k, i1, j, b0, 6.0F, -652993496, false);
                  Render2D.drawRoundedOutline(context, k, i1, j, b0, 6.0F, 1.0F, -869253035, false);
                  Render2D.drawRoundedRect(context, k, i1, 2.0F, b0, 2.0F, -9710683, false);
                  context.drawText(textrenderer, s, k + 6, i1 + 3, -854277, false);
                  i1 += b0 + 4;
               }
            }
         }
      }
   }

   public static String watermarkFragment() {
      return "E";
   }
}
