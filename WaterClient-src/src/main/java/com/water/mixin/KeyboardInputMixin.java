package com.water.mixin;

import com.water.module.modules.render.Freecam;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public class KeyboardInputMixin {
   public KeyboardInputMixin() {
   }

   @Inject(
      method = {"tick()V"},
      at = {@At("RETURN")}
   )
   private void onTickReturn(CallbackInfo ci) {
      if (Freecam.instance != null && Freecam.instance.isEnabled()) {
         InputAccessor inputAccessor = (InputAccessor)this;
         inputAccessor.water$setPlayerInput(new PlayerInput(false, false, false, false, false, false, false));
         inputAccessor.water$setMovementVector(Vec2f.ZERO);
      }
   }
}
