package com.water.module.modules.client;

import com.water.gui.ConfigScreen;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.ModuleManager;
import com.water.module.setting.Setting;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.minecraft.client.MinecraftClient;

public final class ConfigShare extends Module {
   public static ConfigShare INSTANCE;
   public volatile String lastCode = "";
   public volatile String lastError = "";
   public volatile boolean uploading = false;
   public volatile boolean downloading = false;

   public ConfigShare() {
      super("Config Share", Category.CLIENT);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         minecraftClient.execute(() -> {
            minecraftClient.setScreen(new ConfigScreen());
            this.setEnabled(false);
         });
      }
   }

   public static ConfigShare getInstance() {
      return INSTANCE;
   }

   public void uploadConfig(Runnable task) {
      this.uploading = true;
      this.lastCode = "";
      this.lastError = "";
      new Thread(() -> {
         try {
            String s = this.serializeModules();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            try (GZIPOutputStream gzipoutputstream = new GZIPOutputStream(byteArrayOutputStream)) {
               gzipoutputstream.write(s.getBytes(StandardCharsets.UTF_8));
            }

            String s3 = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOutputStream.toByteArray());
            HttpURLConnection httpURLConnection = (HttpURLConnection)new URI("https://dpaste.com/api/v2/").toURL().openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setConnectTimeout(6000);
            httpURLConnection.setReadTimeout(6000);
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String s1 = "content=" + URLEncoder.encode(s3, StandardCharsets.UTF_8) + "&syntax=text&expiry_days=365";

            try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
               outputStream.write(s1.getBytes(StandardCharsets.UTF_8));
            }

            String s4 = new String(httpURLConnection.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            httpURLConnection.disconnect();
            String s2 = s4.replaceAll("https?://dpaste\\.com/", "").replace("/", "").trim();
            this.lastCode = s2;
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (minecraftClient != null) {
               minecraftClient.execute(() -> minecraftClient.keyboard.setClipboard(s2));
            }
         } catch (Exception exception) {
            this.lastError = "Upload failed: " + exception.getMessage();
         } finally {
            this.uploading = false;
            MinecraftClient minecraftClient1 = MinecraftClient.getInstance();
            if (task != null && minecraftClient1 != null) {
               minecraftClient1.execute(task);
            }
         }
      }, "water-config-export").start();
   }

   public void downloadConfig(String text, Runnable task) {
      this.downloading = true;
      this.lastError = "";
      new Thread(() -> {
         try {
            try {
               String s = text.trim().replaceAll("\\s+", "");
               if (s.isEmpty()) {
                  this.lastError = "Enter a code first!";
                  return;
               }

               HttpURLConnection httpURLConnection = (HttpURLConnection)new URI("https://dpaste.com/" + s + ".txt").toURL().openConnection();
               httpURLConnection.setConnectTimeout(6000);
               httpURLConnection.setReadTimeout(6000);
               String s1 = new String(httpURLConnection.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
               httpURLConnection.disconnect();

               byte[] abyte;
               try {
                  abyte = Base64.getUrlDecoder().decode(s1);
               } catch (Exception exception2) {
                  try {
                     abyte = Base64.getDecoder().decode(s1);
                  } catch (Exception exception1) {
                     this.lastError = "Invalid code!";
                     return;
                  }
               }

               String s2;
               try (GZIPInputStream gzipinputstream = new GZIPInputStream(new ByteArrayInputStream(abyte))) {
                  s2 = new String(gzipinputstream.readAllBytes(), StandardCharsets.UTF_8);
               } catch (Exception exception) {
                  this.lastError = "Invalid code!";
                  return;
               }

               this.applySerialized(s2);
               ModuleManager.INSTANCE.saveConfig();
            } catch (Exception exception3) {
               this.lastError = "Load error: " + exception3.getMessage();
            }
         } finally {
            this.downloading = false;
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            if (task != null && minecraftClient != null) {
               minecraftClient.execute(task);
            }
         }
      }, "water-config-import").start();
   }

   public String serializeModules() {
      StringBuilder stringBuilder = new StringBuilder();

      for (Module module : ModuleManager.INSTANCE.getModules()) {
         if (module.getCategory() != Category.CLIENT) {
            stringBuilder.append(module.getName()).append(":").append(module.isEnabled() ? "1" : "0").append(":").append(module.getBind()).append(":");

            for (Setting setting : module.getSettings()) {
               String s = this.serializeSettingValue(setting);
               stringBuilder.append(s != null ? s : "").append(",");
            }

            stringBuilder.append("\n");
         }
      }

      return stringBuilder.toString();
   }

   public void applySerialized(String text) {
      for (String s : text.split("\n")) {
         String[] astring = s.split(":", 4);
         if (astring.length >= 4) {
            Module module = ModuleManager.INSTANCE.getModuleByName(astring[0]);
            if (module != null) {
               boolean flag = astring[1].equals("1");
               if (flag != module.isEnabled()) {
                  module.toggle();
               }

               try {
                  module.setBind(Integer.parseInt(astring[2]));
               } catch (Exception exception) {
               }

               String[] astring1 = astring[3].split(",", -1);
               int i = 0;

               for (Setting setting : module.getSettings()) {
                  if (i >= astring1.length) {
                     break;
                  }

                  if (!astring1[i].isEmpty()) {
                     this.applySettingValue(setting, astring1[i]);
                  }

                  i++;
               }
            }
         }
      }
   }

   public String serializeSettingValue(Setting<?> setting) {
      Object object = setting.getValue();
      if (object instanceof Boolean) {
         return (Boolean)object ? "1" : "0";
      } else if (object instanceof Float) {
         return String.valueOf((Float)object);
      } else if (object instanceof Double) {
         return String.valueOf((Double)object);
      } else if (object instanceof Integer) {
         return String.valueOf((Integer)object);
      } else if (object instanceof String) {
         return ((String)object).replace(",", "").replace("\n", "");
      } else {
         return object instanceof Color color ? String.format("%02x%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()) : "";
      }
   }

   public void applySettingValue(Setting<?> setting, String text) {
      try {
         Object object = setting.getValue();
         if (object instanceof Boolean) {
            ((Setting)setting).setValue(text.equals("1"));
         } else if (object instanceof Float) {
            ((Setting)setting).setValue(Float.parseFloat(text));
         } else if (object instanceof Double) {
            ((Setting)setting).setValue(Double.parseDouble(text));
         } else if (object instanceof Integer) {
            ((Setting)setting).setValue(Integer.parseInt(text));
         } else if (object instanceof String) {
            ((Setting)setting).setValue(text);
         } else if (object instanceof Color && text.length() == 8) {
            ((Setting)setting).setValue(
               new Color(
                  Integer.parseInt(text.substring(0, 2), 16),
                  Integer.parseInt(text.substring(2, 4), 16),
                  Integer.parseInt(text.substring(4, 6), 16),
                  Integer.parseInt(text.substring(6, 8), 16)
               )
            );
         }
      } catch (Exception exception) {
      }
   }
}
