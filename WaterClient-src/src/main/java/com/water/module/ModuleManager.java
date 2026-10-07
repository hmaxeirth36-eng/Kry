package com.water.module;

import com.water.module.modules.client.ConfigShare;
import com.water.module.modules.client.DiscordRPC;
import com.water.module.modules.client.Friends;
import com.water.module.modules.client.HUD;
import com.water.module.modules.client.HudElement;
import com.water.module.modules.client.SpotifyHUD;
import com.water.module.modules.client.Water;
import com.water.module.modules.combat.AnchorMacro;
import com.water.module.modules.combat.AutoCrystal;
import com.water.module.modules.combat.AutoDoubleHand;
import com.water.module.modules.combat.AutoInvTotem;
import com.water.module.modules.combat.AutoTotem;
import com.water.module.modules.combat.DoubleAnchor;
import com.water.module.modules.combat.Hitbox;
import com.water.module.modules.combat.HoverTotem;
import com.water.module.modules.combat.ShieldBreaker;
import com.water.module.modules.combat.SingleAnchor;
import com.water.module.modules.combat.SpearSwap;
import com.water.module.modules.combat.Triggerbot;
import com.water.module.modules.donut.ActivityDebug;
import com.water.module.modules.donut.AntiTrap;
import com.water.module.modules.donut.AutoChunkLoader;
import com.water.module.modules.donut.BoneDropper;
import com.water.module.modules.donut.FakeRoles;
import com.water.module.modules.donut.FakeStats;
import com.water.module.modules.donut.SUSChunkFinder;
import com.water.module.modules.donut.ChestSusFinder;
import com.water.module.modules.donut.PlayerDebug;
import com.water.module.modules.donut.SpawnerDebug;
import com.water.module.modules.donut.SpawnerProtect;
import com.water.module.modules.misc.AutoLog;
import com.water.module.modules.misc.AutoMine;
import com.water.module.modules.misc.AutoRender;
import com.water.module.modules.misc.AutoTPA;
import com.water.module.modules.misc.AutoTool;
import com.water.module.modules.misc.ChatMacro;
import com.water.module.modules.misc.CoordSnapper;
import com.water.module.modules.misc.FastPlace;
import com.water.module.modules.misc.FreeLook;
import com.water.module.modules.misc.HomeSetter;
import com.water.module.modules.misc.NameProtect;
import com.water.module.modules.misc.NameTags;
import com.water.module.modules.misc.SkinChanger;
import com.water.module.modules.misc.Sprint;
import com.water.module.modules.misc.SwingSpeed;
import com.water.module.modules.misc.TabDetector;
import com.water.module.modules.misc.WeatherNotifier;
import com.water.module.modules.render.BlockESP;
import com.water.module.modules.render.Freecam;
import com.water.module.modules.render.FullBright;
import com.water.module.modules.render.FutureDebug;
import com.water.module.modules.render.HoleESP;
import com.water.module.modules.render.JumpCircles;
import com.water.module.modules.render.LightDebug;
import com.water.module.modules.render.NoRender;
import com.water.module.modules.render.PearlESP;
import com.water.module.modules.render.PlayerESP;
import com.water.module.modules.render.RegionMap;
import com.water.module.modules.render.SpawnerNotifier;
import com.water.module.modules.render.StorageESP;
import com.water.module.modules.render.TuffChunkFinder;
import com.water.module.setting.BlockListSetting;
import com.water.module.setting.EntityListSetting;
import com.water.module.setting.MultiSelectSetting;
import com.water.module.setting.Setting;
import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ModuleManager {
   public static final String CONFIG_HEADER = "WATER_CONFIG_V2";
   public static final String MODULE_PREFIX = "MODULE";
   public static final String SETTING_PREFIX = "SETTING";
   public static final String HUDPOS_PREFIX = "HUDPOS";
   public static final String BLOCKCOLOR_PREFIX = "BLOCKCOLOR";
   public static final String STORAGECOLOR_PREFIX = "STORAGECOLOR";
   public String activeConfigName = "default";
   public static final ModuleManager INSTANCE = new ModuleManager();
   public final List<Module> modules = new ArrayList<>();
   public boolean initialized = false;
   public boolean loadingConfig = false;

   public ModuleManager() {
   }

   public void init() {
      if (!this.initialized) {
         this.modules.add(new Water());
         this.modules.add(new HUD());
         this.modules.add(new SpotifyHUD());
         this.modules.add(new Friends());
         this.modules.add(new ConfigShare());
         this.modules.add(new SimpleModule("Elytra Swap", Category.COMBAT));
         this.modules.add(new AutoTotem());
         this.modules.add(new ShieldBreaker());
         this.modules.add(new AnchorMacro());
         this.modules.add(new SimpleModule("Mace Swap", Category.COMBAT));
         this.modules.add(new Triggerbot());
         this.modules.add(new HoverTotem());
         this.modules.add(new DoubleAnchor());
         this.modules.add(new SingleAnchor());
         this.modules.add(new AutoDoubleHand());
         this.modules.add(new AutoInvTotem());
         this.modules.add(new AutoCrystal());
         this.modules.add(new Hitbox());
         this.modules.add(new SpearSwap());
         this.modules.add(new DiscordRPC());
         this.modules.add(new StorageESP());
         this.modules.add(new Freecam());
         this.modules.add(new FullBright());
         this.modules.add(new PlayerESP());
         this.modules.add(new HoleESP());
         this.modules.add(new NoRender());
         this.modules.add(new JumpCircles());
         this.modules.add(new LightDebug());
         this.modules.add(new AutoRender());
         this.modules.add(new SpawnerNotifier());
         this.modules.add(new FutureDebug());
         this.modules.add(new SUSChunkFinder());
         this.modules.add(new PlayerDebug());
         this.modules.add(new SpawnerDebug());
         this.modules.add(new TuffChunkFinder());
         this.modules.add(new AutoMine());
         this.modules.add(new PearlESP());
         this.modules.add(new SwingSpeed());
         this.modules.add(new NameTags());
         this.modules.add(new RegionMap());
         this.modules.add(new AutoTool());
         this.modules.add(new Sprint());
         this.modules.add(new NameProtect());
         this.modules.add(new FreeLook());
         this.modules.add(new SkinChanger());
         this.modules.add(new HomeSetter());
         this.modules.add(new WeatherNotifier());
         this.modules.add(new CoordSnapper());
         this.modules.add(new FastPlace());
         this.modules.add(new TabDetector());
         this.modules.add(new AutoLog());
         this.modules.add(new AutoTPA());
         this.modules.add(new ChatMacro());
         this.modules.add(new AutoChunkLoader());
         this.modules.add(new FakeRoles());
         this.modules.add(new AntiTrap());
         this.modules.add(new ActivityDebug());
         this.modules.add(new SpawnerProtect());
         this.modules.add(new FakeStats());
         this.modules.add(new BoneDropper());
         this.initialized = true;
         this.loadConfig();
      }
   }

   public void onSettingChanged() {
      if (this.initialized && !this.loadingConfig) {
         this.saveConfig();
      }
   }

   public void saveConfig() {
      if (this.initialized && !this.loadingConfig) {
         Path path = this.getConfigPath();

         try {
            Files.createDirectories(path.getParent());

            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
               bufferedWriter.write("WATER_CONFIG_V2");
               bufferedWriter.newLine();

               for (Module module : this.modules) {
                  bufferedWriter.write("MODULE");
                  bufferedWriter.write(9);
                  bufferedWriter.write(this.encodeBase64(module.getName()));
                  bufferedWriter.write(9);
                  bufferedWriter.write(Integer.toString(module.getBind()));
                  bufferedWriter.write(9);
                  int i = module instanceof ActivatableModule activatableModule ? activatableModule.getActivationKey() : 0;
                  bufferedWriter.write(Integer.toString(i));
                  bufferedWriter.write(9);
                  bufferedWriter.write(Boolean.toString(module.isEnabled()));
                  bufferedWriter.newLine();

                  for (Setting setting : module.getSettings()) {
                     String s = this.serializeSetting(setting);
                     if (s != null) {
                        bufferedWriter.write("SETTING");
                        bufferedWriter.write(9);
                        bufferedWriter.write(this.encodeBase64(module.getName()));
                        bufferedWriter.write(9);
                        bufferedWriter.write(this.encodeBase64(setting.getName()));
                        bufferedWriter.write(9);
                        bufferedWriter.write(this.encodeBase64(s));
                        bufferedWriter.newLine();
                     }
                  }
               }

               for (HudElement hudElement : HudElement.values()) {
                  int[] aint = HUD.getPosition(hudElement);
                  bufferedWriter.write("HUDPOS");
                  bufferedWriter.write(9);
                  bufferedWriter.write(hudElement.name());
                  bufferedWriter.write(9);
                  bufferedWriter.write(Integer.toString(aint[0]));
                  bufferedWriter.write(9);
                  bufferedWriter.write(Integer.toString(aint[1]));
                  bufferedWriter.newLine();
               }

               for (Module module1 : this.modules) {
                  if (module1 instanceof BlockESP blockESP) {
                     Map map = blockESP.getBlockColors();

                     for (Entry entry : (Iterable<Entry>)map.entrySet()) {
                        Identifier identifier = Registries.BLOCK.getId((Block)entry.getKey());
                        if (identifier != null) {
                           Color color = (Color)entry.getValue();
                           bufferedWriter.write("BLOCKCOLOR");
                           bufferedWriter.write(9);
                           bufferedWriter.write(this.encodeBase64(identifier.toString()));
                           bufferedWriter.write(9);
                           bufferedWriter.write(color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha());
                           bufferedWriter.newLine();
                        }
                     }
                  }
               }

               for (Module module2 : this.modules) {
                  if (module2 instanceof StorageESP storageESP) {
                     Map map1 = storageESP.getCustomBlockColors();

                     for (Entry entry1 : (Iterable<Entry>)map1.entrySet()) {
                        Identifier identifier1 = Registries.BLOCK.getId((Block)entry1.getKey());
                        if (identifier1 != null) {
                           Color color1 = (Color)entry1.getValue();
                           bufferedWriter.write("STORAGECOLOR");
                           bufferedWriter.write(9);
                           bufferedWriter.write(this.encodeBase64(identifier1.toString()));
                           bufferedWriter.write(9);
                           bufferedWriter.write(color1.getRed() + "," + color1.getGreen() + "," + color1.getBlue() + "," + color1.getAlpha());
                           bufferedWriter.newLine();
                        }
                     }
                  }
               }
            }
         } catch (IOException ioexception) {
         }
      }
   }

   public void loadConfig() {
      Path path = this.getConfigPath();
      if (Files.exists(path)) {
         this.loadingConfig = true;

         try {
            for (String s : Files.readAllLines(path, StandardCharsets.UTF_8)) {
               if (s != null && !s.isBlank() && !"WATER_CONFIG_V2".equals(s)) {
                  if (s.startsWith("MODULE\t")) {
                     this.parseModuleLine(s);
                  } else if (s.startsWith("SETTING\t")) {
                     this.parseSettingLine(s);
                  } else if (s.startsWith("HUDPOS\t")) {
                     this.parseHudPosLine(s);
                  } else if (s.startsWith("BLOCKCOLOR\t")) {
                     this.parseBlockColorLine(s);
                  } else if (s.startsWith("STORAGECOLOR\t")) {
                     this.parseStorageColorLine(s);
                  } else {
                     this.applyHotkeyLine(s);
                  }
               }
            }
         } catch (IOException ioexception) {
         } finally {
            this.loadingConfig = false;
         }
      }
   }

   public void parseHudPosLine(String text) {
      try {
         String[] astring = text.split("\t");
         if (astring.length < 4) {
            return;
         }

         HudElement hudElement = HudElement.valueOf(astring[1]);
         int i = Integer.parseInt(astring[2]);
         int j = Integer.parseInt(astring[3]);
         HUD.setPosition(hudElement, i, j);
      } catch (Exception exception) {
      }
   }

   public void parseBlockColorLine(String text) {
      try {
         String[] astring = text.split("\t");
         if (astring.length < 3) {
            return;
         }

         String s = this.decodeBase64(astring[1]);
         String[] astring1 = astring[2].split(",");
         if (astring1.length < 4) {
            return;
         }

         int i = Integer.parseInt(astring1[0].trim());
         int j = Integer.parseInt(astring1[1].trim());
         int k = Integer.parseInt(astring1[2].trim());
         int l = Integer.parseInt(astring1[3].trim());
         Identifier identifier = Identifier.tryParse(s);
         if (identifier == null) {
            return;
         }

         Block block = Registries.BLOCK.get(identifier);
         if (block == null || block == Blocks.AIR) {
            return;
         }

         for (Module module : this.modules) {
            if (module instanceof BlockESP blockESP) {
               Map map = blockESP.getBlockColors();
               map.put(block, new Color(i, j, k, l));
               blockESP.setBlockColors(map);
            }
         }
      } catch (Exception exception) {
      }
   }

   public void parseStorageColorLine(String text) {
      try {
         String[] astring = text.split("\t");
         if (astring.length < 3) {
            return;
         }

         String s = this.decodeBase64(astring[1]);
         String[] astring1 = astring[2].split(",");
         if (astring1.length < 4) {
            return;
         }

         int i = Integer.parseInt(astring1[0].trim());
         int j = Integer.parseInt(astring1[1].trim());
         int k = Integer.parseInt(astring1[2].trim());
         int l = Integer.parseInt(astring1[3].trim());
         Identifier identifier = Identifier.tryParse(s);
         if (identifier == null) {
            return;
         }

         Block block = Registries.BLOCK.get(identifier);
         if (block == null || block == Blocks.AIR) {
            return;
         }

         for (Module module : this.modules) {
            if (module instanceof StorageESP storageESP) {
               storageESP.setBuiltinBlockColor(block, new Color(i, j, k, l));
            }
         }
      } catch (Exception exception) {
      }
   }

   public String getActiveConfigName() {
      return this.activeConfigName;
   }

   public void setActiveConfigName(String text) {
      this.activeConfigName = text != null && !text.isBlank() ? text.trim() : "default";
   }

   public boolean isLoadingConfig() {
      return this.loadingConfig;
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public List<Module> getModulesInCategory(Category category) {
      ArrayList arrayList = new ArrayList();

      for (Module module : this.modules) {
         if (module.getCategory() == category) {
            arrayList.add(module);
         }
      }

      return arrayList;
   }

   public Module getModuleByName(String text) {
      for (Module module : this.modules) {
         if (module.getName().equalsIgnoreCase(text)) {
            return module;
         }
      }

      return null;
   }

   public void onTick() {
      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.onTick();
         }
      }
   }

   public void onRender(MatrixStack matrices, float tickDelta) {
      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.onRender(matrices, tickDelta);
         }
      }
   }

   public void onPacketReceive(Packet<?> packet) {
      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.onPacketReceive(packet);
         }
      }
   }

   public boolean onPacketSend(Packet<?> packet) {
      boolean flag = false;

      for (Module module : this.modules) {
         if (module.isEnabled()) {
            try {
               flag |= module.onPacketSend(packet);
            } catch (Exception exception) {
            }
         }
      }

      return flag;
   }

   public Path getConfigPath() {
      String s = this.activeConfigName != null && !this.activeConfigName.isBlank() ? this.activeConfigName : "default";
      String s1 = s.equals("default") ? "water_config.txt" : "water_config_" + s + ".txt";
      return MinecraftClient.getInstance().runDirectory.toPath().resolve(s1);
   }

   public void parseModuleLine(String text) {
      String[] astring = text.split("\t", 5);
      if (astring.length >= 5) {
         Module module = this.getModuleByName(this.decodeBase64(astring[1]));
         if (module != null) {
            try {
               module.applyBind(Integer.parseInt(astring[2]));
               if (module instanceof ActivatableModule activatableModule) {
                  activatableModule.applyActivationKey(Integer.parseInt(astring[3]));
               }

               module.applyEnabled(Boolean.parseBoolean(astring[4]));
            } catch (Exception exception) {
            }
         }
      }
   }

   public void parseSettingLine(String text) {
      String[] astring = text.split("\t", 4);
      if (astring.length >= 4) {
         Module module = this.getModuleByName(this.decodeBase64(astring[1]));
         if (module != null) {
            Setting setting = this.findSetting(module, this.decodeBase64(astring[2]));
            if (setting != null) {
               this.deserializeSetting(setting, this.decodeBase64(astring[3]));
            }
         }
      }
   }

   public void applyHotkeyLine(String text) {
      String[] astring = text.split(":", 4);
      if (astring.length >= 2) {
         Module module = this.getModuleByName(astring[0]);
         if (module != null) {
            try {
               if (astring.length >= 2) {
                  module.applyBind(Integer.parseInt(astring[1]));
               }

               if (astring.length >= 3 && module instanceof ActivatableModule activatableModule) {
                  activatableModule.applyActivationKey(Integer.parseInt(astring[2]));
               }

               if (astring.length >= 4) {
                  module.applyEnabled(Boolean.parseBoolean(astring[3]));
               }
            } catch (Exception exception) {
            }
         }
      }
   }

   public Setting<?> findSetting(Module module, String text) {
      for (Setting setting : module.getSettings()) {
         if (setting.matchesName(text)) {
            return setting;
         }
      }

      return null;
   }

   public String serializeSetting(Setting<?> setting) {
      Object object = setting.getValue();
      if (setting instanceof BlockListSetting blockListSetting) {
         return this.serializeBlocks(blockListSetting);
      } else if (setting instanceof EntityListSetting entityListSetting) {
         return this.serializeMobs(entityListSetting);
      } else if (setting instanceof MultiSelectSetting multiSelectSetting) {
         return this.serializeStringSet(multiSelectSetting.getValue());
      } else if (object instanceof Boolean obool) {
         return Boolean.toString(obool);
      } else if (object instanceof Float f) {
         return Float.toString(f);
      } else if (object instanceof Integer integer) {
         return Integer.toString(integer);
      } else if (object instanceof Double d0) {
         return Double.toString(d0);
      } else if (object instanceof String s) {
         return s;
      } else {
         return object instanceof Color color ? color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() : null;
      }
   }

   public void deserializeSetting(Setting<?> setting, String text) {
      Object object = setting.getValue();

      try {
         if (setting instanceof BlockListSetting blockListSetting) {
            blockListSetting.setValue(this.deserializeBlocks(text));
            return;
         }

         if (setting instanceof EntityListSetting entityListSetting) {
            entityListSetting.setValue(this.deserializeMobs(text));
            return;
         }

         if (setting instanceof MultiSelectSetting multiSelectSetting) {
            multiSelectSetting.setValue(this.deserializeStringSet(text));
            return;
         }

         if (object instanceof Boolean) {
            ((Setting)setting).setValue(Boolean.parseBoolean(text));
            return;
         }

         if (object instanceof Float) {
            ((Setting)setting).setValue(Float.parseFloat(text));
            return;
         }

         if (object instanceof Integer) {
            ((Setting)setting).setValue(Math.round(Float.parseFloat(text)));
            return;
         }

         if (object instanceof Double) {
            ((Setting)setting).setValue(Double.parseDouble(text));
            return;
         }

         if (object instanceof String) {
            ((Setting)setting).setValue(text);
            return;
         }

         if (object instanceof Color) {
            String[] astring = text.split(",", 4);
            if (astring.length == 4) {
               ((Setting)setting).setValue(new Color(Integer.parseInt(astring[0]), Integer.parseInt(astring[1]), Integer.parseInt(astring[2]), Integer.parseInt(astring[3])));
            }
         }
      } catch (Exception exception) {
      }
   }

   public String serializeBlocks(BlockListSetting setting) {
      StringBuilder stringBuilder = new StringBuilder();

      for (Block block : setting.getSelectedBlocks()) {
         Identifier identifier = Registries.BLOCK.getId(block);
         if (identifier != null) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(',');
            }

            stringBuilder.append(identifier);
         }
      }

      return stringBuilder.toString();
   }

   public Set<Block> deserializeBlocks(String text) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (text != null && !text.isBlank()) {
         for (String s : text.split(",")) {
            String s1 = s.trim();
            if (!s1.isEmpty()) {
               Identifier identifier = Identifier.tryParse(s1);
               if (identifier != null) {
                  Block block = Registries.BLOCK.get(identifier);
                  if (block != null) {
                     linkedHashSet.add(block);
                  }
               }
            }
         }

         return linkedHashSet;
      } else {
         return linkedHashSet;
      }
   }

   public String serializeMobs(EntityListSetting setting) {
      StringBuilder stringBuilder = new StringBuilder();

      for (EntityType entityType : setting.getSelectedMobs()) {
         Identifier identifier = Registries.ENTITY_TYPE.getId(entityType);
         if (identifier != null) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(',');
            }

            stringBuilder.append(identifier);
         }
      }

      return stringBuilder.toString();
   }

   public Set<EntityType<?>> deserializeMobs(String text) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (text != null && !text.isBlank()) {
         for (String s : text.split(",")) {
            String s1 = s.trim();
            if (!s1.isEmpty()) {
               Identifier identifier = Identifier.tryParse(s1);
               if (identifier != null && Registries.ENTITY_TYPE.containsId(identifier)) {
                  linkedHashSet.add(Registries.ENTITY_TYPE.get(identifier));
               }
            }
         }

         return linkedHashSet;
      } else {
         return linkedHashSet;
      }
   }

   public String serializeStringSet(Set<String> set) {
      StringBuilder stringBuilder = new StringBuilder();

      for (String s : set) {
         if (s != null && !s.isEmpty()) {
            if (!stringBuilder.isEmpty()) {
               stringBuilder.append(',');
            }

            stringBuilder.append(s);
         }
      }

      return stringBuilder.toString();
   }

   public Set<String> deserializeStringSet(String text) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (text != null && !text.isBlank()) {
         for (String s : text.split(",")) {
            String s1 = s.trim();
            if (!s1.isEmpty()) {
               linkedHashSet.add(s1);
            }
         }

         return linkedHashSet;
      } else {
         return linkedHashSet;
      }
   }

   public String encodeBase64(String text) {
      return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
   }

   public String decodeBase64(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            return new String(Base64.getDecoder().decode(text), StandardCharsets.UTF_8);
         } catch (IllegalArgumentException illegalArgumentException) {
            return text;
         }
      } else {
         return "";
      }
   }

   public static String watermarkFragment() {
      return "L";
   }
}
