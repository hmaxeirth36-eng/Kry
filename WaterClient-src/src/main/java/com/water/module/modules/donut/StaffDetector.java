package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.HUD;
import com.water.module.modules.client.HudElement;
import com.water.module.setting.Setting;
import com.water.render.Render2D;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class StaffDetector extends Module {
   public final Setting<Boolean> showInChat = new Setting<>("Chat Alert", true);
   public final Setting<Boolean> showInHud = new Setting<>("HUD", true);
   public final Setting<Boolean> detectByName = new Setting<>("By Name", true);
   public final Setting<Boolean> detectByRank = new Setting<>("By Rank Tag", true);
   // RECOVERY NOTE: the obfuscator stripped this class's static initialiser entirely, so in the
   // released jar both fields were null and staff detection would have thrown NPE. The entries
   // below are reconstructed from the string constants still present in the obfuscated class
   // file; the exact split and ordering could not be recovered — verify against your own list.
   public static final List<String> RANK_KEYWORDS = List.of(
      "owner", "admin", "developer", "dev", "manager", "moderator", "mod", "sr",
      "helper", "staff", "community", "cm", "guard", "sentinel", "support",
      "operator", "head", "junior"
   );
   public static final Set<String> KNOWN_STAFF = Set.of("notsobot", "donutsmp", "donut");
   public final Map<String, String> detectedStaff = new LinkedHashMap<>();
   public final Set<String> alreadyAlerted = new HashSet<>();
   public final Map<String, Identifier> skinCache = new LinkedHashMap<>();
   public int tickCounter = 0;
   public static StaffDetector INSTANCE;

   public StaffDetector() {
      super("Staff Detector", Category.DONUT);
      this.addSetting(this.showInChat);
      this.addSetting(this.showInHud);
      this.addSetting(this.detectByName);
      this.addSetting(this.detectByRank);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.detectedStaff.clear();
      this.alreadyAlerted.clear();
      this.skinCache.clear();
      this.tickCounter = 0;
   }

   @Override
   public void onDisable() {
      this.detectedStaff.clear();
      this.alreadyAlerted.clear();
      this.skinCache.clear();
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if (++this.tickCounter % 20 == 0) {
            if (mc.getNetworkHandler() != null) {
               HashSet hashSet = new HashSet();

               for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
                  String s = playerListEntry.getProfile().name();
                  String s1 = playerListEntry.getDisplayName() != null ? playerListEntry.getDisplayName().getString() : s;
                  if (playerListEntry.getScoreboardTeam() != null) {
                     s1 = playerListEntry.getScoreboardTeam().getPrefix().getString() + s + playerListEntry.getScoreboardTeam().getSuffix().getString();
                  }

                  String s2 = this.detectStaffTag(s, s1);
                  if (s2 != null) {
                     hashSet.add(s);
                     this.detectedStaff.put(s, s2);
                     if (!this.skinCache.containsKey(s) && mc.world != null) {
                        for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
                           if (abstractClientPlayerEntity.getName().getString().equalsIgnoreCase(s)
                              && abstractClientPlayerEntity instanceof AbstractClientPlayerEntity) {
                              Identifier identifier = getSkinTexture(abstractClientPlayerEntity);
                              if (identifier != null) {
                                 this.skinCache.put(s, identifier);
                              }
                              break;
                           }
                        }
                     }

                     if (!this.alreadyAlerted.contains(s)) {
                        this.alreadyAlerted.add(s);
                        if (this.showInChat.getValue() && mc.player != null) {
                           mc.player
                              .sendMessage(Text.literal("\u00a78[\u00a7cStaff Detector\u00a78] \u00a7c\u26a0 \u00a7f" + s + " \u00a77(" + s2 + ")"), false);
                        }

                        mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.5F);
                     }
                  }
               }

               this.detectedStaff.keySet().retainAll(hashSet);
               this.skinCache.keySet().retainAll(hashSet);
            }
         }
      }
   }

   public static Identifier getSkinTexture(AbstractClientPlayerEntity player) {
      try {
         SkinTextures skinTextures = player.getSkin();

         for (Method method : skinTextures.getClass().getMethods()) {
            if (method.getParameterCount() == 0) {
               method.setAccessible(true);
               Object object = null;

               try {
                  object = method.invoke(skinTextures);
               } catch (Exception exception1) {
                  continue;
               }

               if (object != null) {
                  if (object instanceof Identifier) {
                     return (Identifier)object;
                  }

                  try {
                     for (Method method1 : object.getClass().getMethods()) {
                        if (method1.getParameterCount() == 0 && method1.getReturnType() == Identifier.class) {
                           method1.setAccessible(true);
                           Identifier identifier = (Identifier)method1.invoke(object);
                           if (identifier != null) {
                              return identifier;
                           }
                        }
                     }
                  } catch (Exception exception) {
                  }
               }
            }
         }
      } catch (Exception exception2) {
      }

      return null;
   }

   public static void renderHud(DrawContext context) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         if (INSTANCE.showInHud.getValue()) {
            if (INSTANCE.hasDetectedStaff()) {
               MinecraftClient minecraftClient = MinecraftClient.getInstance();
               if (minecraftClient != null && minecraftClient.player != null) {
                  Map map = INSTANCE.detectedStaff;
                  int i = map.size();
                  byte b0 = 12;
                  byte b1 = 10;
                  byte b2 = 14;
                  byte b3 = 6;
                  byte b4 = 10;
                  int j = 140;

                  for (Entry entry : (Iterable<Entry>)map.entrySet()) {
                     int k = b0
                        + b4
                        + 4
                        + minecraftClient.textRenderer.getWidth(((String)entry.getKey()).toUpperCase())
                        + 16
                        + minecraftClient.textRenderer.getWidth((String)entry.getValue())
                        + b0;
                     if (k > j) {
                        j = k;
                     }
                  }

                  int k2 = b0 + minecraftClient.textRenderer.getWidth("STAFF") + 10 + 18 + b0;
                  if (k2 > j) {
                     j = k2;
                  }

                  int l2 = j;
                  int i3 = b1 + b2 + b3 + i * (b4 + 2) + b1;
                  int[] aint = HUD.getPosition(HudElement.STAFF_LIST);
                  int l = aint[0];
                  int i1 = aint[1];
                  Render2D.drawRoundedRect(context, l, i1, j, i3, 8.0F, -267382768, false);
                  int j1 = i1 + b1;
                  context.drawText(minecraftClient.textRenderer, "STAFF", l + b0, j1, -1, false);
                  String s = String.valueOf(i);
                  int k1 = minecraftClient.textRenderer.getWidth(s) + 8;
                  int l1 = l + j - b0 - k1;
                  int i2 = j1 - 1;
                  Render2D.drawRoundedRect(context, l1, i2, k1, 11.0F, 5.0F, -48982, false);
                  context.drawText(minecraftClient.textRenderer, s, l1 + k1 / 2 - minecraftClient.textRenderer.getWidth(s) / 2, i2 + 2, -1, false);
                  int j2 = j1 + b2 + b3 - 2;

                  for (Entry entry1 : (Iterable<Entry>)map.entrySet()) {
                     String s1 = (String)entry1.getKey();
                     String s2 = (String)entry1.getValue();
                     Identifier identifier = INSTANCE.skinCache.get(s1);
                     if (identifier == null && minecraftClient.world != null) {
                        for (AbstractClientPlayerEntity abstractClientPlayerEntity : minecraftClient.world.getPlayers()) {
                           if (abstractClientPlayerEntity.getName().getString().equalsIgnoreCase(s1)
                              && abstractClientPlayerEntity instanceof AbstractClientPlayerEntity) {
                              identifier = getSkinTexture(abstractClientPlayerEntity);
                              if (identifier != null) {
                                 INSTANCE.skinCache.put(s1, identifier);
                              }
                              break;
                           }
                        }
                     }

                     int j3 = l + b0;
                     int k3 = j2 - 1;
                     Identifier identifier1 = HUD.getHeadTexture(identifier);
                     if (identifier1 != null) {
                        Render2D.drawTexture(context, j3, k3, b4, identifier1, -1, 0.0F, false);
                     } else {
                        Render2D.drawRoundedRect(context, j3 + b4 / 4, k3 + b4 / 4, b4 / 2, b4 / 2, b4 / 4.0F, -48982, false);
                     }

                     context.drawText(minecraftClient.textRenderer, s1.toUpperCase(), j3 + b4 + 4, j2, -48982, false);
                     context.drawText(minecraftClient.textRenderer, s2, l + l2 - b0 - minecraftClient.textRenderer.getWidth(s2), j2, -3377221, false);
                     j2 += b4 + 2;
                  }
               }
            }
         }
      }
   }

   public String detectStaffTag(String text, String text2) {
      String s = text.toLowerCase(Locale.ROOT);
      String s1 = text2.toLowerCase(Locale.ROOT);
      if (this.detectByName.getValue() && KNOWN_STAFF.contains(s)) {
         return "STAFF";
      } else {
         if (this.detectByRank.getValue()) {
            for (String s2 : RANK_KEYWORDS) {
               if (s1.contains("[" + s2 + "]")
                  || s1.contains("(" + s2 + ")")
                  || s1.startsWith(s2 + " ")
                  || s1.contains(" " + s2 + " ")
                  || s1.contains("." + s2)
                  || s1.endsWith(" " + s2)) {
                  String s3 = s2.toUpperCase();
                  if (s3.equals("SR")) {
                     return "SR.HELPER";
                  } else if (s3.equals("DEVELOPER") || s3.equals("DEV")) {
                     return "DEV";
                  } else {
                     return !s3.equals("COMMUNITY") && !s3.equals("CM") ? s3 : "COMMUNITY MGR";
                  }
               }
            }
         }

         if (this.detectByRank.getValue()) {
            for (String s4 : RANK_KEYWORDS) {
               if (s1.contains(s4)) {
                  return s4.toUpperCase();
               }
            }
         }

         for (char c0 : text2.toCharArray()) {
            if (c0 > 9472 && c0 < 10240) {
               return "STAFF";
            }
         }

         return null;
      }
   }

   public Map<String, String> getDetectedStaff() {
      return Collections.unmodifiableMap(this.detectedStaff);
   }

   public boolean hasDetectedStaff() {
      return !this.detectedStaff.isEmpty();
   }

   public static String watermarkFragment() {
      return "9";
   }
}
