package com.water;

import com.water.gui.ClickGuiScreen;
import com.water.gui.NotificationManager;
import com.water.gui.SpotifyOverlay;
import com.water.module.ActivatableModule;
import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.client.HUD;
import com.water.module.modules.client.SpotifyHUD;
import com.water.module.modules.client.Water;
import com.water.module.modules.misc.NameTags;
import com.water.module.modules.render.PearlESP;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.water.module.modules.donut.SUSChunkFinder;
import com.water.module.modules.donut.ChestSusFinder;
import com.water.module.modules.donut.PlayerDebug;
import com.water.module.modules.donut.SpawnerDebug;
public class WaterClient implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("Water");
   private static KeyBinding rightShiftKey;

   public WaterClient() {
   }

   public static boolean isMenuKey(int var0, int var1) {
      return var0 == Water.getGuiKeyCode();
   }

   public static void toggleClickGui() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         minecraftClient.execute(() -> {
            if (minecraftClient.currentScreen instanceof ClickGuiScreen) {
               minecraftClient.setScreen(null);
            } else {
               ClickGuiScreen.open();
            }
         });
      }
   }

   @Override
   public void onInitializeClient() {
      LOGGER.info("Cracked by github.com/TrilliumSolutions");
      ModuleManager.INSTANCE.init();
      rightShiftKey = KeyBindingHelper.registerKeyBinding(
         new KeyBinding("key.water.toggle_menu", Type.KEYSYM, 344, new Category(Identifier.of("water", "general")))
      );
      HudRenderCallback.EVENT.register((var0, var1) -> {
         NameTags.renderHud(var0, var1.getTickProgress(false));
         SUSChunkFinder.renderHud(var0, var1.getTickProgress(false));
         PlayerDebug.renderHud(var0, var1.getTickProgress(false));
         SpawnerDebug.renderHud(var0, var1.getTickProgress(false));
         PearlESP.renderHud(var0, var1.getTickProgress(false));
         HUD.renderHud(var0);
         SpotifyHUD.renderHud(var0);
         SpotifyOverlay.visible = HUD.isSpotifyQueueEnabled();
         if (SpotifyOverlay.visible) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null) {
               double d0 = minecraftClient.getWindow().getScaleFactor();
               double d1 = minecraftClient.mouse.getX() / d0;
               double d2 = minecraftClient.mouse.getY() / d0;
               SpotifyOverlay.render(var0, d1, d2);
            }
         }

         NotificationManager.INSTANCE.render(var0);
      });
      ClientTickEvents.END_CLIENT_TICK.register(var0 -> {
         ModuleManager.INSTANCE.onTick();
         if (var0.currentScreen == null && var0.getWindow() != null) {
            long handle = var0.getWindow().getHandle();
            for (Module module : ModuleManager.INSTANCE.getModules()) {
               int bind = module.getBind();
               ActivatableModule activatableModule = module instanceof ActivatableModule activatableModule1 ? activatableModule1 : null;
               int actKey = activatableModule != null ? activatableModule.getActivationKey() : 0;

               if (bind != 0) {
                  try {
                     // Support both keyboard (GLFW key) and mouse buttons (0-7 stored as negative or high codes)
                     boolean down;
                     if (bind >= 0 && bind <= 7) {
                        down = GLFW.glfwGetMouseButton(handle, bind) == GLFW.GLFW_PRESS;
                     } else {
                        down = GLFW.glfwGetKey(handle, bind) == GLFW.GLFW_PRESS;
                     }
                     if (down && !module.wasBindPressed) {
                        // avoid conflicting with activation key on same code
                        if (actKey == 0 || actKey != bind) {
                           module.onBindPressed();
                        }
                     }
                     module.wasBindPressed = down;
                  } catch (Exception ignored) {
                     module.wasBindPressed = false;
                  }
               } else {
                  module.wasBindPressed = false;
               }

               if (activatableModule != null && actKey != 0) {
                  try {
                     boolean down1 = GLFW.glfwGetKey(handle, actKey) == GLFW.GLFW_PRESS;
                     if (down1 && !activatableModule.wasActivationKeyPressed) {
                        activatableModule.onActivationKeyPressed();
                     }
                     activatableModule.wasActivationKeyPressed = down1;
                  } catch (Exception ignored) {
                     activatableModule.wasActivationKeyPressed = false;
                  }
               }
            }
         } else {
            // Reset edge state while any screen is open so releasing a key in GUI
            // doesn't leave wasBindPressed stuck true
            for (Module module : ModuleManager.INSTANCE.getModules()) {
               module.wasBindPressed = true; // require full release after GUI closes
               if (module instanceof ActivatableModule am) {
                  am.wasActivationKeyPressed = true;
               }
            }
         }
      });
   }
}
