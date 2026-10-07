package com.water.mixin;

import com.water.util.KeybindHandler;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientLifecycleHandler implements ClientModInitializer {
   public static final Logger LOGGER = LoggerFactory.getLogger("water");

   public ClientLifecycleHandler() {
   }

   public static boolean isMenuKey(int var0, int var1) {
      return KeybindHandler.isMenuKey(var0, var1);
   }

   public static void toggleClickGui() {
      KeybindHandler.toggleClickGui();
   }

   @Override
   public void onInitializeClient() {
      KeybindHandler.init();
   }
}
