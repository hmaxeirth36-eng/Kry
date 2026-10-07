package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

public final class HomeSetter extends Module {
   public final Setting<Boolean> chatFeedback = new Setting<>("Chat Feedback", true);
   public final Setting<Float> homeSlot = new Setting<>("Home Slot", 1.0F, 1.0F, 5.0F);
   public volatile boolean running = false;

   public HomeSetter() {
      super("HomeSetter", Category.MISC);
      this.addSetting(this.chatFeedback);
      this.addSetting(this.homeSlot);
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (!this.running) {
         if (mc != null && mc.player != null && mc.world != null) {
            this.running = true;
            int i = Math.round(this.homeSlot.getValue());
            short short1 = 750;
            mc.execute(() -> {
               this.sendCommand("/delhome " + i);
               new Thread(() -> {
                  try {
                     Thread.sleep(750L);
                  } catch (InterruptedException interruptedException) {
                  }

                  mc.execute(() -> {
                     this.sendCommand("/sethome " + i);
                     if (this.chatFeedback.getValue()) {
                        try {
                           mc.inGameHud.getChatHud().addMessage(Text.literal("\u00a7aHome " + i + " deleted and set successfully!"));
                        } catch (Exception exception) {
                        }
                     }

                     this.running = false;
                     this.toggle();
                  });
               }, "HomeSetter-DelayThread").start();
            });
         } else {
            this.toggle();
         }
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      this.running = false;
   }

   public void sendCommand(String text) {
      if (mc != null) {
         ClientPlayNetworkHandler clientPlayNetworkHandler = null;

         try {
            if (mc.player != null) {
               clientPlayNetworkHandler = mc.player.networkHandler;
            }
         } catch (Throwable throwable3) {
         }

         if (clientPlayNetworkHandler == null) {
            try {
               clientPlayNetworkHandler = mc.getNetworkHandler();
            } catch (Throwable throwable2) {
            }
         }

         if (clientPlayNetworkHandler != null) {
            String s = text.startsWith("/") ? text.substring(1) : text;

            try {
               clientPlayNetworkHandler.sendChatCommand(s);
            } catch (Throwable throwable1) {
               try {
                  clientPlayNetworkHandler.sendChatMessage(text);
               } catch (Throwable throwable) {
               }
            }
         }
      }
   }
}
