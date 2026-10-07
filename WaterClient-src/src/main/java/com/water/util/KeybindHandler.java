package com.water.util;

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
import com.water.module.modules.donut.SUSChunkFinder;
import com.water.module.modules.donut.ChestSusFinder;
import com.water.module.modules.donut.PlayerDebug;
import com.water.module.modules.donut.SpawnerDebug;
import com.water.module.modules.render.RegionMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KeybindHandler {
   public static final Logger LOG = LoggerFactory.getLogger("water");
   public static KeyBinding _k;

   public KeybindHandler() {
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

   public static void init() {
      ModuleManager.INSTANCE.init();
      _k = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.water.toggle_menu", Type.KEYSYM, 344, new Category(Identifier.of("water", "general"))));
      HudRenderCallback.EVENT.register((var0, var1) -> {
         NameTags.renderHud(var0, var1.getTickProgress(false));
         SUSChunkFinder.renderHud(var0, var1.getTickProgress(false));
         PlayerDebug.renderHud(var0, var1.getTickProgress(false));
         SpawnerDebug.renderHud(var0, var1.getTickProgress(false));
         HUD.renderHud(var0);
         SpotifyHUD.renderHud(var0);
         // RECOVERY FIX: RegionMap.render(...) was fully implemented but never invoked anywhere
         // in the original jar, so the module silently did nothing. Wired into the HUD pass here.
         RegionMap regionMap = (RegionMap)ModuleManager.INSTANCE.getModuleByName("Region Map");
         if (regionMap != null && regionMap.isEnabled()) {
            MinecraftClient regionMapClient = MinecraftClient.getInstance();
            if (regionMapClient != null && regionMapClient.player != null) {
               regionMap.render(var0, regionMapClient);
            }
         }
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
      // Module tick + keybinds are handled in WaterClient.onInitializeClient
      // to avoid double onTick (lag) and double toggle.
   }
}
