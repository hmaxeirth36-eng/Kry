package com.water.mixin;

import com.water.module.ModuleManager;
import com.water.module.modules.misc.AutoMine;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MinecraftClient.class})
public class AutoMineMixin {
   private BlockPos autoMineLastPos = null;
   private boolean autoMineActive = false;

   public AutoMineMixin() {
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void onTick(CallbackInfo ci) {
      MinecraftClient minecraftClient = (MinecraftClient)(Object)this;
      AutoMine autoMine = (AutoMine)ModuleManager.INSTANCE.getModuleByName("AutoMine");
      if (autoMine != null && autoMine.isEnabled()) {
         if (minecraftClient.player != null && minecraftClient.world != null && minecraftClient.interactionManager != null) {
            if (minecraftClient.currentScreen == null) {
               boolean flag = GLFW.glfwGetMouseButton(minecraftClient.getWindow().getHandle(), 0) == 1;
               if (flag) {
                  this.autoMineActive = false;
                  this.autoMineLastPos = null;
               } else {
                  HitResult hitResult = minecraftClient.crosshairTarget;
                  if (hitResult != null && hitResult.getType() == Type.BLOCK) {
                     BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                     BlockPos blockPos = blockHitResult.getBlockPos();
                     BlockState blockState = minecraftClient.world.getBlockState(blockPos);
                     if (!blockState.isAir() && !(blockState.getHardness(minecraftClient.world, blockPos) < 0.0F)) {
                        if (blockState.getHardness(minecraftClient.world, blockPos) > 0.0F) {
                           minecraftClient.options.attackKey.setPressed(true);
                           this.autoMineActive = true;
                           this.autoMineLastPos = blockPos;
                        } else {
                           minecraftClient.options.attackKey.setPressed(false);
                           this.autoMineActive = false;
                        }
                     } else {
                        this.autoMineLastPos = null;
                        this.autoMineActive = false;
                     }
                  } else {
                     this.autoMineLastPos = null;
                     this.autoMineActive = false;
                  }
               }
            }
         }
      } else {
         if (this.autoMineActive) {
            this.autoMineActive = false;
            this.autoMineLastPos = null;
         }
      }
   }
}
