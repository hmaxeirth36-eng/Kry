package com.water.module.modules.misc;

import com.water.module.ActivatableModule;
import com.water.module.Category;
import com.water.module.modules.client.Friends;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult.Type;

public final class AutoLog extends ActivatableModule {
   public static final long COMBAT_COOLDOWN_MS = 20000L;
   public static final int COMBAT_COOLDOWN_TICKS = 400;
   public static final double COMBAT_GRACE_RANGE = 8.0;
   public static final long SERVER_COMBAT_GRACE_MS = 1500L;
   public static final int MAX_TEXT_SCAN_DEPTH = 4;
   public long lastHitTime;
   public long lastAttackTime;
   public long lastServerCombatTagTime;
   public float lastCombinedHealth = -1.0F;
   public int lastObservedAttackedTick = -1;
   public int lastObservedAttackTick = -1;
   public String savedHost;
   public int savedPort;
   public String savedName;
   public boolean reconnectPending = false;
   public long reconnectTime = 0L;
   public static final long RECONNECT_DELAY_MS = 3000L;

   public AutoLog() {
      super("AutoLog", Category.MISC);
   }

   @Override
   public void onActivationKeyPressed() {
      if (mc.player != null && mc.world != null) {
         this.saveCurrentServer();
         this.disconnect();
         this.reconnectPending = true;
         this.reconnectTime = System.currentTimeMillis() + 3000L;
      }
   }

   @Override
   public void onEnable() {
      this.lastHitTime = 0L;
      this.lastAttackTime = 0L;
      this.lastServerCombatTagTime = 0L;
      this.lastCombinedHealth = this.getEffectiveHealth();
      this.lastObservedAttackedTick = this.getLastAttackedTime();
      this.lastObservedAttackTick = this.getLastAttackTime();
      if (mc.player != null && mc.world != null && (this.detectCombat() || this.isRecentlyEngaged() || this.isCombatTagged())) {
         this.markCombat(System.currentTimeMillis());
      }
   }

   @Override
   public void onTick() {
      if (this.reconnectPending && mc.player == null && mc.world == null) {
         if (System.currentTimeMillis() >= this.reconnectTime && this.savedHost != null) {
            this.reconnectPending = false;
            this.reconnect();
         }
      } else if (mc.player != null && mc.world != null) {
         long i = System.currentTimeMillis();
         float f = this.getEffectiveHealth();
         boolean flag = this.lastCombinedHealth >= 0.0F && f + 0.001F < this.lastCombinedHealth;
         this.lastCombinedHealth = f;
         if (mc.player.hurtTime > 0 || flag || this.pollAttackedByPlayer()) {
            this.lastHitTime = i;
         }

         boolean flag1 = this.isCombatTagged();
         if (flag1) {
            this.lastServerCombatTagTime = i;
         }

         if (mc.options.attackKey.isPressed()
            && mc.crosshairTarget != null
            && mc.crosshairTarget.getType() == Type.ENTITY
            && mc.crosshairTarget instanceof EntityHitResult entityHitResult
            && entityHitResult.getEntity() instanceof PlayerEntity playerEntity
            && playerEntity != mc.player) {
            this.lastAttackTime = i;
         }

         if (this.pollAttackingPlayer()) {
            this.lastAttackTime = i;
         }

         if (!this.isInCombat(i, flag1)) {
            for (PlayerEntity playerEntity1 : mc.world.getPlayers()) {
               if (playerEntity1 != mc.player && !playerEntity1.isSpectator() && (!Friends.isAutoLogEnabled() || !Friends.isFriend(playerEntity1.getName().getString()))) {
                  if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
                     this.saveCurrentServer();
                     mc.getNetworkHandler().getConnection().disconnect(Text.literal("[AutoLog] Player detected: " + playerEntity1.getName().getString()));
                     this.toggle();
                  }

                  return;
               }
            }
         }
      }
   }

   public void saveCurrentServer() {
      if (mc.getCurrentServerEntry() != null) {
         ServerInfo serverInfo = mc.getCurrentServerEntry();
         ServerAddress serverAddress = ServerAddress.parse(serverInfo.address);
         this.savedHost = serverAddress.getAddress();
         this.savedPort = serverAddress.getPort();
         this.savedName = serverInfo.name;
      }
   }

   public void disconnect() {
      if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
         mc.getNetworkHandler().getConnection().disconnect(Text.literal("[AutoLog] Manual leave"));
      }
   }

   public void reconnect() {
      if (this.savedHost != null) {
         ServerInfo serverInfo = new ServerInfo(
            this.savedName != null ? this.savedName : this.savedHost, this.savedHost + ":" + this.savedPort, ServerType.OTHER
         );
         ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(serverInfo.address), serverInfo, false, null);
      }
   }

   public boolean isInCombat(long now, boolean force) {
      if (force || this.isRecentlyEngaged() || now - this.lastServerCombatTagTime < 1500L) {
         return true;
      } else if (this.lastHitTime <= 0L && this.lastAttackTime <= 0L) {
         return false;
      } else {
         long i = Math.max(this.lastHitTime, this.lastAttackTime);
         return now - i < 20000L;
      }
   }

   public boolean detectCombat() {
      if (mc.player != null && mc.world != null) {
         if (mc.player.hurtTime <= 0 && !this.isRecentlyEngaged()) {
            if (mc.crosshairTarget != null
               && mc.crosshairTarget.getType() == Type.ENTITY
               && mc.crosshairTarget instanceof EntityHitResult entityHitResult
               && entityHitResult.getEntity() instanceof PlayerEntity playerEntity
               && playerEntity != mc.player
               && !playerEntity.isSpectator()) {
               return true;
            } else {
               double d0 = 64.0;

               for (PlayerEntity playerEntity1 : mc.world.getPlayers()) {
                  if (playerEntity1 != mc.player && !playerEntity1.isSpectator() && mc.player.squaredDistanceTo(playerEntity1) <= d0) {
                     return true;
                  }
               }

               return false;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public void markCombat(long now) {
      this.lastHitTime = now;
      this.lastAttackTime = now;
      this.lastServerCombatTagTime = now;
   }

   public boolean isRecentlyEngaged() {
      return this.wasAttackedByPlayer() || this.isAttackingPlayer();
   }

   public boolean pollAttackedByPlayer() {
      int i = this.getLastAttackedTime();
      if (i > 0 && i != this.lastObservedAttackedTick) {
         this.lastObservedAttackedTick = i;
         return this.wasAttackedByPlayer();
      } else {
         return false;
      }
   }

   public boolean pollAttackingPlayer() {
      int i = this.getLastAttackTime();
      if (i > 0 && i != this.lastObservedAttackTick) {
         this.lastObservedAttackTick = i;
         return this.isAttackingPlayer();
      } else {
         return false;
      }
   }

   public boolean wasAttackedByPlayer() {
      if (mc.player == null) {
         return false;
      } else {
         return mc.player.getLastAttacker() instanceof PlayerEntity playerEntity && playerEntity != mc.player && !playerEntity.isSpectator()
            ? this.isRecentTick(mc.player.getLastAttackedTime())
            : false;
      }
   }

   public boolean isAttackingPlayer() {
      if (mc.player == null) {
         return false;
      } else {
         return mc.player.getAttacking() instanceof PlayerEntity playerEntity && playerEntity != mc.player && !playerEntity.isSpectator()
            ? this.isRecentTick(mc.player.getLastAttackTime())
            : false;
      }
   }

   public boolean isRecentTick(int tick) {
      if (mc.player != null && tick > 0) {
         int i = mc.player.age - tick;
         return i >= 0 && i < 400;
      } else {
         return false;
      }
   }

   public int getLastAttackedTime() {
      return mc.player != null ? mc.player.getLastAttackedTime() : -1;
   }

   public int getLastAttackTime() {
      return mc.player != null ? mc.player.getLastAttackTime() : -1;
   }

   public boolean isCombatTagged() {
      if (mc.world != null && this.scoreboardHasCombatTag(mc.world.getScoreboard())) {
         return true;
      } else {
         Set set = Collections.newSetFromMap(new IdentityHashMap());
         return mc.inGameHud != null && this.searchForCombatText(mc.inGameHud, 0, set);
      }
   }

   public boolean scoreboardHasCombatTag(Scoreboard scoreboard) {
      if (scoreboard == null) {
         return false;
      } else {
         for (ScoreboardObjective scoreboardObjective : scoreboard.getObjectives()) {
            if (this.containsCombat(scoreboardObjective.getName()) || this.textContainsCombat(scoreboardObjective.getDisplayName())) {
               return true;
            }
         }

         for (ScoreboardDisplaySlot scoreboardDisplaySlot : ScoreboardDisplaySlot.values()) {
            ScoreboardObjective scoreboardObjective1 = scoreboard.getObjectiveForSlot(scoreboardDisplaySlot);
            if (scoreboardObjective1 != null) {
               if (this.containsCombat(scoreboardObjective1.getName()) || this.textContainsCombat(scoreboardObjective1.getDisplayName())) {
                  return true;
               }

               for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(scoreboardObjective1)) {
                  if (this.containsCombat(scoreboardEntry.owner()) || this.textContainsCombat(scoreboardEntry.name()) || this.textContainsCombat(scoreboardEntry.display())) {
                     return true;
                  }

                  Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
                  if (this.teamHasCombatTag(team)) {
                     return true;
                  }
               }
            }
         }

         for (Team team1 : scoreboard.getTeams()) {
            if (this.teamHasCombatTag(team1)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean teamHasCombatTag(Team team) {
      return team != null && (this.containsCombat(team.getName()) || this.textContainsCombat(team.getDisplayName()) || this.textContainsCombat(team.getPrefix()) || this.textContainsCombat(team.getSuffix()));
   }

   public boolean searchForCombatText(Object value, int depth, Set<Object> set) {
      if (value == null || depth > 4) {
         return false;
      } else if (value instanceof Text text) {
         return this.containsCombat(text.getString());
      } else if (value instanceof String s) {
         return this.containsCombat(s);
      } else if (!set.add(value)) {
         return false;
      } else if (value instanceof Map map) {
         for (Entry entry : (Iterable<Entry>)map.entrySet()) {
            if (this.searchForCombatText(entry.getKey(), depth + 1, set) || this.searchForCombatText(entry.getValue(), depth + 1, set)) {
               return true;
            }
         }

         return false;
      } else if (value instanceof Collection) {
         for (Object object : (Collection)value) {
            if (this.searchForCombatText(object, depth + 1, set)) {
               return true;
            }
         }

         return false;
      } else {
         Class oclass = value.getClass();
         if (!this.isVanillaTextClass(oclass)) {
            return false;
         } else {
            for (Class oclass1 = oclass; oclass1 != null && oclass1 != Object.class; oclass1 = oclass1.getSuperclass()) {
               for (Field field : oclass1.getDeclaredFields()) {
                  if (!Modifier.isStatic(field.getModifiers())
                     && !field.getType().isPrimitive()
                     && !field.getDeclaringClass().getName().startsWith("java.lang")) {
                     try {
                        field.setAccessible(true);
                        if (this.searchForCombatText(field.get(value), depth + 1, set)) {
                           return true;
                        }
                     } catch (Exception exception) {
                     }
                  }
               }
            }

            return false;
         }
      }
   }

   public boolean isVanillaTextClass(Class<?> type) {
      String s = type.getName();
      return s.startsWith("net.minecraft.scoreboard.")
         || s.startsWith("net.minecraft.text.")
         || s.startsWith("net.minecraft.client.gui.hud.")
         || s.startsWith("net.minecraft.client.network.")
         || s.startsWith("java.util.");
   }

   public boolean containsCombat(String text) {
      return text != null && text.toLowerCase().contains("combat");
   }

   public boolean textContainsCombat(Text text) {
      return text != null && this.containsCombat(text.getString());
   }

   public float getEffectiveHealth() {
      return mc.player != null ? mc.player.getHealth() + mc.player.getAbsorptionAmount() : -1.0F;
   }
}
