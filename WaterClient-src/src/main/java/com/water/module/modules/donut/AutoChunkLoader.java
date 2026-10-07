package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.text.Text;

public final class AutoChunkLoader extends Module {
   public final Setting<Float> homeSlot = new Setting<>("Home Slot", 1.0F, 1.0F, 5.0F);
   public final Setting<Boolean> chatFeedback = new Setting<>("Chat Feedback", true);
   public final Setting<Integer> rtpWait = new Setting<>("Cooldown", 3500, 500, 10000);
   public boolean triggered = false;
   public long cooldownUntil = 0L;

   public AutoChunkLoader() {
      super("AUTO CHUNK LOADER", Category.DONUT);
      this.addSetting(this.homeSlot);
      this.addSetting(this.chatFeedback);
      this.addSetting(this.rtpWait);
   }

   @Override
   public void onDisable() {
      this.triggered = false;
      this.cooldownUntil = 0L;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if (!this.triggered) {
            if (System.currentTimeMillis() >= this.cooldownUntil) {
               double d0 = mc.player.getY();
               if (!(d0 >= -3.0)) {
                  this.triggered = true;
                  int i = Math.round(this.homeSlot.getValue());
                  if (this.chatFeedback.getValue() && mc.inGameHud != null) {
                     mc.inGameHud.getChatHud().addMessage(Text.literal(""));
                  }

                  new Thread(() -> {
                     try {
                        mc.execute(() -> this.sendCommand("/delhome " + i));
                        Thread.sleep(800L);
                        mc.execute(() -> {
                           this.sendCommand("/sethome " + i);
                           if (this.chatFeedback.getValue() && mc.inGameHud != null) {
                              mc.inGameHud.getChatHud().addMessage(Text.literal(""));
                           }
                        });
                        Thread.sleep(300L);
                        mc.execute(() -> {
                           this.sendCommand("/home 3");
                           if (this.chatFeedback.getValue() && mc.inGameHud != null) {
                              mc.inGameHud.getChatHud().addMessage(Text.literal(""));
                           }
                        });
                        Thread.sleep(this.rtpWait.getValue().intValue());
                        mc.execute(() -> {
                           this.sendCommand("/home " + i);
                           if (this.chatFeedback.getValue() && mc.inGameHud != null) {
                              mc.inGameHud.getChatHud().addMessage(Text.literal(""));
                           }

                           this.cooldownUntil = System.currentTimeMillis() + 15000L;
                           this.triggered = false;
                        });
                     } catch (InterruptedException interruptedException) {
                        this.triggered = false;
                     }
                  }, "RtpReset-Thread").start();
               }
            }
         }
      }
   }

   public void sendCommand(String text) {
      if (mc != null && mc.player != null) {
         ClientPlayNetworkHandler clientPlayNetworkHandler = null;

         try {
            clientPlayNetworkHandler = mc.player.networkHandler;
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
