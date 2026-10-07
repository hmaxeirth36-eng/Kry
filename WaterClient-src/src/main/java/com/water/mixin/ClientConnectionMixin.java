package com.water.mixin;

import com.water.module.Module;
import com.water.module.ModuleManager;
import io.netty.channel.ChannelFutureListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class ClientConnectionMixin {
   public ClientConnectionMixin() {
   }

   @Inject(
      method = {"handlePacket"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void onHandlePacket(Packet<?> packet, PacketListener packetListener, CallbackInfo ci) {
      try {
         Module module = ModuleManager.INSTANCE.getModuleByName("RTP Home Reset");
         if (module != null && module.isEnabled() && containsBlockedText(packet)) {
            ci.cancel();
            return;
         }

         ModuleManager.INSTANCE.onPacketReceive(packet);
      } catch (Exception exception) {
      }
   }

   private static boolean containsBlockedText(Object value) {
      if (value == null) {
         return false;
      } else {
         for (Method method : value.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0) {
               try {
                  method.setAccessible(true);
                  Object object = method.invoke(value);
                  if (object instanceof Text text) {
                     if (isBlocked(text.getString())) {
                        return true;
                     }

                     if (isBlocked(text.getString())) {
                        return true;
                     }
                  }

                  if (object instanceof String s && isBlocked(s)) {
                     return true;
                  }
               } catch (Throwable throwable1) {
               }
            }
         }

         for (Class oclass = value.getClass(); oclass != null; oclass = oclass.getSuperclass()) {
            for (Field field : oclass.getDeclaredFields()) {
               try {
                  field.setAccessible(true);
                  Object object1 = field.get(value);
                  if (object1 instanceof Text text1 && isBlocked(text1.getString())) {
                     return true;
                  }

                  if (object1 instanceof String s1 && isBlocked(s1)) {
                     return true;
                  }
               } catch (Throwable throwable) {
               }
            }
         }

         return false;
      }
   }

   private static boolean isBlocked(String text) {
      if (text == null) {
         return false;
      } else {
         String s = text.toLowerCase();
         return s.contains("home deleted") || s.contains("home set") || s.contains("teleported to a random location") || s.contains("teleported to your home");
      }
   }

   @Inject(
      method = {"send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onSend(Packet<?> packet, ChannelFutureListener channelFutureListener, boolean var3, CallbackInfo ci) {
      try {
         if (ModuleManager.INSTANCE.onPacketSend(packet)) {
            ci.cancel();
         }
      } catch (Exception exception) {
      }
   }
}
