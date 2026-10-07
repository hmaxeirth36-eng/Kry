package com.water.mixin;

import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.modules.donut.FakeRoles;
import com.water.module.modules.misc.NameProtect;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChatHud.class})
public class ChatHudMixin {
   @Shadow
   private List<?> messages;

   public ChatHudMixin() {
   }

   @Inject(
      method = {"addMessage*"},
      at = {@At("RETURN")}
   )
   private void afterAddMessage(CallbackInfo ci) {
      Module module = ModuleManager.INSTANCE.getModuleByName("RTP Home Reset");
      if (module != null && module.isEnabled()) {
         if (this.messages != null && !this.messages.isEmpty()) {
            this.messages
               .removeIf(
                  var0 -> {
                     if (var0 == null) {
                        return false;
                     } else {
                        try {
                           for (Method method : var0.getClass().getDeclaredMethods()) {
                              if (method.getParameterCount() == 0) {
                                 method.setAccessible(true);
                                 if (method.invoke(var0) instanceof Text text) {
                                    String s = text.getString().toLowerCase();
                                    return s.contains("home deleted")
                                       || s.contains("home set")
                                       || s.contains("teleported to a random location")
                                       || s.contains("teleported to your home");
                                 }
                              }
                           }
                        } catch (Throwable throwable) {
                        }

                        return false;
                     }
                  }
               );
         }
      }
   }

   @ModifyVariable(
      method = {"addMessage"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private Text modifyChatMessage(Text text) {
      if (text != null && NameProtect.instance != null && NameProtect.instance.isEnabled() && MinecraftClient.getInstance().getSession() != null) {
         String s = MinecraftClient.getInstance().getSession().getUsername();
         if (s != null) {
            String s1 = text.getString();
            if (s1.contains(s)) {
               text = Text.literal(s1.replace(s, NameProtect.instance.getFakeName()));
            }
         }
      }

      return FakeRoles.applyToText((Text)text);
   }
}
