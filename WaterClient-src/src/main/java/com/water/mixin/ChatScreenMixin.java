package com.water.mixin;

import com.water.gui.HudEditor;
import com.water.gui.SpotifyOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ChatScreen.class})
public class ChatScreenMixin {
   public ChatScreenMixin() {
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$mouseClicked(Click click, boolean var2, CallbackInfoReturnable<Boolean> cir) {
      double d0 = click.x();
      double d1 = click.y();
      int i = click.button();
      if (i == 0) {
         if (SpotifyOverlay.handleClick(d0, d1, i)) {
            cir.setReturnValue(true);
            return;
         }

         if (HudEditor.INSTANCE.onMouseClick(d0, d1, i)) {
            HudEditor.isEditing = true;
            cir.setReturnValue(true);
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void water$render(DrawContext context, int var2, int var3, float var4, CallbackInfo ci) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         boolean flag = GLFW.glfwGetMouseButton(minecraftClient.getWindow().getHandle(), 0) == 1;
         if (!flag) {
            SpotifyOverlay.stopDragging();
            HudEditor.INSTANCE.onMouseRelease();
            HudEditor.isEditing = false;
         }
      }

      HudEditor.INSTANCE.render(context, var2, var3);
   }
}
