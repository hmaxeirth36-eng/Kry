package com.water.mixin;

import com.water.module.modules.donut.FakeStats;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PlayerListHud.class})
public class PlayerListHudMixin {
   @Shadow
   private Text footer;

   public PlayerListHudMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void water$fakeFooter(DrawContext context, int var2, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
      FakeStats fakeStats = FakeStats.getInstance();
      if (fakeStats != null && fakeStats.isEnabled() && this.footer != null) {
         Text text = fakeStats.fakeFooterText(this.footer);
         if (text != null) {
            this.footer = text;
         }
      }
   }
}
