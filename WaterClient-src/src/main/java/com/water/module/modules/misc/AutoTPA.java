package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.ModeSetting;
import com.water.module.setting.Setting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

public final class AutoTPA extends Module {
   public final Setting<String> playerName = new Setting<>("Player", "Player");
   public final ModeSetting mode = new ModeSetting("Mode", "tpahere", "tpa", "tpahere");
   public final Setting<Float> minDelay = new Setting<>("Min Delay", 10.0F, 1.0F, 100.0F);
   public final Setting<Float> maxDelay = new Setting<>("Max Delay", 30.0F, 1.0F, 100.0F);
   public int delayCounter = 0;
   public int pauseCounter = 0;
   public boolean paused = false;
   public int lastHitTick = -1;
   public int lastAttackedTick = -1;
   public static final int PAUSE_TICKS = 400;

   public AutoTPA() {
      super("AutoTPA", Category.MISC);
      this.addSetting(this.playerName);
      this.addSetting(this.mode);
      this.addSetting(this.minDelay);
      this.addSetting(this.maxDelay);
   }

   @Override
   public void onEnable() {
      this.delayCounter = 0;
      this.pauseCounter = 0;
      this.paused = false;
      this.lastHitTick = mc.player != null ? mc.player.getLastAttackedTime() : -1;
      this.lastAttackedTick = mc.player != null ? mc.player.getLastAttackTime() : -1;
   }

   @Override
   public void onDisable() {
      this.delayCounter = 0;
      this.pauseCounter = 0;
      this.paused = false;
      this.lastHitTick = -1;
      this.lastAttackedTick = -1;
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (mc.player != null) {
         if (packet instanceof EntityVelocityUpdateS2CPacket entityVelocityUpdateS2CPacket && entityVelocityUpdateS2CPacket.getEntityId() == mc.player.getId()) {
            this.pauseAfterTeleport();
         }
      }
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         int i = mc.player.getLastAttackedTime();
         if (i > 0 && i != this.lastHitTick) {
            this.lastHitTick = i;
            if (mc.player.getLastAttacker() instanceof PlayerEntity playerEntity && playerEntity != mc.player && !playerEntity.isSpectator()) {
               this.pauseAfterTeleport();
            }
         }

         int k = mc.player.getLastAttackTime();
         if (k > 0 && k != this.lastAttackedTick) {
            this.lastAttackedTick = k;
            this.pauseAfterTeleport();
         }

         if (this.paused) {
            if (this.pauseCounter > 0) {
               this.pauseCounter--;
               return;
            }

            this.paused = false;
            this.delayCounter = 0;
         }

         if (this.delayCounter > 0) {
            this.delayCounter--;
         } else {
            mc.getNetworkHandler().sendChatCommand(this.mode.getValue() + " " + this.playerName.getValue().trim());
            int l = this.minDelay.getValue().intValue();
            int j = Math.max(l, this.maxDelay.getValue().intValue());
            this.delayCounter = l + (int)(Math.random() * (j - l + 1));
         }
      }
   }

   public void pauseAfterTeleport() {
      this.paused = true;
      this.pauseCounter = 400;
      this.delayCounter = 0;
   }
}
