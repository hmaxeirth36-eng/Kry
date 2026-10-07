package com.water.mixin;

import com.water.WaterClient;
import com.water.module.modules.client.Water;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class WaterMenuKeyboardMixin {
   @Unique
   private boolean water$menuKeyDown = false;

   public WaterMenuKeyboardMixin() {
   }

   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$openClickGuiFromAnyScreen(long var1, int var3, KeyInput input, CallbackInfo ci) {
      int i = Water.getGuiKeyCode();
      boolean flag = GLFW.glfwGetKey(var1, i) == 1;
      if (flag && !this.water$menuKeyDown) {
         WaterClient.toggleClickGui();
         ci.cancel();
      }

      this.water$menuKeyDown = flag;
   }
}
