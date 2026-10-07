package com.water.mixin;

import com.mojang.authlib.GameProfile;
import com.water.module.modules.donut.FakeRoles;
import com.water.module.modules.misc.SkinChanger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlayerListEntry.class})
public abstract class PlayerListEntryMixin {
   public PlayerListEntryMixin() {
   }

   @Shadow
   public abstract GameProfile getProfile();

   @Inject(
      method = {"getSkinTextures"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void water$overrideListEntrySkin(CallbackInfoReturnable<SkinTextures> cir) {
      GameProfile gameProfile = this.getProfile();
      if (gameProfile != null && gameProfile.id() != null) {
         SkinTextures skinTextures = SkinChanger.getOverrideSkinFor(gameProfile.id());
         if (skinTextures != null) {
            cir.setReturnValue(skinTextures);
         }
      }
   }

   @Inject(
      method = {"getDisplayName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void water$fakeRoleDisplayName(CallbackInfoReturnable<Text> cir) {
      if (FakeRoles.isActive()) {
         GameProfile gameProfile = this.getProfile();
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (gameProfile != null && gameProfile.id() != null && minecraftClient != null && minecraftClient.player != null) {
            if (gameProfile.id().equals(minecraftClient.player.getUuid())) {
               Text text = FakeRoles.buildRoleName(gameProfile.name());
               if (text != null) {
                  cir.setReturnValue(text);
               }
            }
         }
      }
   }
}
