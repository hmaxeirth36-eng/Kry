package com.water.mixin;

import com.water.gui.HudEditor;
import com.water.gui.SpotifyOverlay;
import com.water.module.modules.render.Freecam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MouseMixin {
   public MouseMixin() {
   }

   private static double toScaledX(MinecraftClient client, double value) {
      if (client != null && client.getWindow() != null) {
         double d0 = client.getWindow().getWidth();
         return d0 <= 0.0 ? value : value * (client.getWindow().getScaledWidth() / d0);
      } else {
         return value;
      }
   }

   private static double toScaledY(MinecraftClient client, double value) {
      if (client != null && client.getWindow() != null) {
         double d0 = client.getWindow().getHeight();
         return d0 <= 0.0 ? value : value * (client.getWindow().getScaledHeight() / d0);
      } else {
         return value;
      }
   }

   @Inject(
      method = {"onMouseButton"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void water$hudEditorMouseButtonClick(long window, @Coerce Object var3, int action, CallbackInfo ci) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.currentScreen instanceof ChatScreen) {
         if (var3 instanceof Click click) {
            if (action == 1) {
               double d0 = toScaledX(minecraftClient, click.x());
               double d1 = toScaledY(minecraftClient, click.y());
               if (SpotifyOverlay.handleClick(d0, d1, click.button())) {
                  ci.cancel();
                  return;
               }

               if (HudEditor.INSTANCE.onMouseClick(click.x(), click.y(), click.button())) {
                  ci.cancel();
               }
            } else if (action == 0) {
               SpotifyOverlay.stopDragging();
               HudEditor.INSTANCE.onMouseRelease();
            }
         }
      }
   }

   @Inject(
      method = {"onCursorPos"},
      at = {@At("HEAD")},
      require = 0
   )
   private void water$hudEditorCursorPos(long window, double x, double y, CallbackInfo ci) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.player != null) {
         if (minecraftClient.currentScreen instanceof ChatScreen) {
            boolean flag = SpotifyOverlay.isDragging();
            boolean flag1 = HudEditor.INSTANCE.isDragging();
            if (flag || flag1) {
               if (GLFW.glfwGetMouseButton(window, 0) != 1) {
                  SpotifyOverlay.stopDragging();
                  HudEditor.INSTANCE.onMouseRelease();
               } else {
                  if (flag) {
                     SpotifyOverlay.handleDrag(toScaledX(minecraftClient, x), toScaledY(minecraftClient, y));
                  } else {
                     HudEditor.INSTANCE.onMouseDrag(toScaledX(minecraftClient, x), toScaledY(minecraftClient, y));
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$useScrollForFreecamSpeed(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (Freecam.instance != null && Freecam.instance.isEnabled()) {
         if (MinecraftClient.getInstance().currentScreen == null) {
            double d0 = vertical != 0.0 ? vertical : horizontal;
            if (d0 != 0.0) {
               Freecam.instance.adjustSpeed(d0);
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"onMouseScroll"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$hudEditorMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.currentScreen instanceof ChatScreen) {
         double d0 = vertical != 0.0 ? vertical : horizontal;
         if (d0 != 0.0) {
            double d1 = toScaledX(minecraftClient, minecraftClient.mouse.getX());
            double d2 = toScaledY(minecraftClient, minecraftClient.mouse.getY());
            if (SpotifyOverlay.handleScroll(d1, d2, d0)) {
               ci.cancel();
            } else {
               if (HudEditor.INSTANCE.onMouseScroll(d1, d2, d0)) {
                  ci.cancel();
               }
            }
         }
      }
   }
}
