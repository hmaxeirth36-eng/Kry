package com.water.util;

import com.water.module.Module;
import com.water.module.ModuleManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;

public final class ConfigFileManager {
   public static final String EXT = ".txt";
   public static final Pattern SAFE = Pattern.compile("[^A-Za-z0-9_\\- ]");

   public ConfigFileManager() {
   }

   public static Path getConfigsDir() {
      Path path = MinecraftClient.getInstance().runDirectory.toPath().resolve("water_configs");

      try {
         Files.createDirectories(path);
      } catch (IOException ioexception) {
      }

      return path;
   }

   public static Path getActiveConfigPath() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("water_config.txt");
   }

   public static String sanitizeName(String text) {
      return text == null ? "" : SAFE.matcher(text.trim()).replaceAll("_");
   }

   public static List<String> listConfigs() {
      ArrayList arrayList = new ArrayList();
      Path path = getConfigsDir();
      if (!Files.isDirectory(path)) {
         return arrayList;
      } else {
         try {
            Files.list(path).forEach(var1x -> {
               String s = var1x.getFileName().toString();
               if (s.endsWith(".txt")) {
                  arrayList.add(s.substring(0, s.length() - ".txt".length()));
               }
            });
         } catch (IOException ioexception) {
         }

         Collections.sort(arrayList, String.CASE_INSENSITIVE_ORDER);
         return arrayList;
      }
   }

   public static boolean saveConfigAs(String text) {
      String s = sanitizeName(text);
      if (s.isEmpty()) {
         return false;
      } else {
         ModuleManager.INSTANCE.saveConfig();
         Path path = getActiveConfigPath();
         Path path1 = getConfigsDir().resolve(s + ".txt");

         try {
            Files.copy(path, path1, StandardCopyOption.REPLACE_EXISTING);
            return true;
         } catch (IOException ioexception) {
            return false;
         }
      }
   }

   public static boolean loadConfigByName(String text) {
      Path path = getConfigsDir().resolve(sanitizeName(text) + ".txt");
      return !Files.isRegularFile(path) ? false : loadConfigFile(path);
   }

   public static boolean deleteConfig(String text) {
      try {
         return Files.deleteIfExists(getConfigsDir().resolve(sanitizeName(text) + ".txt"));
      } catch (IOException ioexception) {
         return false;
      }
   }

   public static String exportToCode() {
      try {
         ModuleManager.INSTANCE.saveConfig();
         byte[] abyte = Files.readAllBytes(getActiveConfigPath());
         return "WCFG-" + Base64.getUrlEncoder().withoutPadding().encodeToString(abyte);
      } catch (IOException ioexception) {
         return null;
      }
   }

   public static boolean importFromCode(String text) {
      if (text == null) {
         return false;
      } else {
         String s = text.trim();
         if (s.startsWith("WCFG-")) {
            s = s.substring(5);
         }

         try {
            byte[] abyte = Base64.getUrlDecoder().decode(s);
            Path path = getConfigsDir().resolve(".__shared_tmp.txt");
            Files.write(path, abyte);
            boolean flag = loadConfigFile(path);

            try {
               Files.deleteIfExists(path);
            } catch (IOException ioexception) {
            }

            return flag;
         } catch (IOException | IllegalArgumentException illegalArgumentException) {
            return false;
         }
      }
   }

   public static boolean loadConfigFile(Path path) {
      Map<String, Boolean> map = snapshotEnabledStates();

      try {
         Files.copy(path, getActiveConfigPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException ioexception) {
         return false;
      }

      ModuleManager.INSTANCE.loadConfig();
      Map<String, Boolean> map1 = snapshotEnabledStates();

      for (Module module : ModuleManager.INSTANCE.getModules()) {
         boolean flag = map.getOrDefault(module.getName(), false);
         boolean flag1 = map1.getOrDefault(module.getName(), false);
         if (flag != flag1) {
            try {
               if (flag1) {
                  module.onEnable();
               } else {
                  module.onDisable();
               }
            } catch (Throwable throwable) {
            }
         }
      }

      ModuleManager.INSTANCE.saveConfig();
      return true;
   }

   public static Map<String, Boolean> snapshotEnabledStates() {
      HashMap hashMap = new HashMap();

      for (Module module : ModuleManager.INSTANCE.getModules()) {
         hashMap.put(module.getName(), module.isEnabled());
      }

      return hashMap;
   }

   public static String readClipboard() {
      try {
         return MinecraftClient.getInstance().keyboard.getClipboard();
      } catch (Throwable throwable) {
         return "";
      }
   }

   public static void writeClipboard(String text) {
      try {
         MinecraftClient.getInstance().keyboard.setClipboard(text == null ? "" : text);
      } catch (Throwable throwable) {
      }
   }

   public static byte[] toUtf8Bytes(String text) {
      return text.getBytes(StandardCharsets.UTF_8);
   }
}
