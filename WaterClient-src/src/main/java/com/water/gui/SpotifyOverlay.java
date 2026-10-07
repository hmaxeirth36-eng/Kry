package com.water.gui;

import com.water.module.modules.client.SpotifyHUD;
import com.water.module.modules.client.Water;
import com.water.render.Render2D;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class SpotifyOverlay {
   public static int posX = 10;
   public static int posY = 200;
   public static boolean visible = true;
   public static boolean dragging = false;
   public static double dragOffX = 0.0;
   public static double dragOffY = 0.0;
   public static final int W = 220;
   public static final int HEAD_H = 18;
   public static final int ROW_H = 13;
   public static final int MAX_ROWS = 12;
   public static final int PAD = 5;
   public static final int MAX_KNOWN = 300;
   public static final List<String[]> ALL_KNOWN = new ArrayList<>();
   public static volatile String currentTitle = "";
   public static volatile String currentArtist = "";
   public static volatile int currentIdx = 0;
   public static int scroll = 0;
   public static final ExecutorService exec = Executors.newSingleThreadExecutor(var0 -> {
      Thread thread = new Thread(var0, "spotify-queue");
      thread.setDaemon(true);
      return thread;
   });
   public static final AtomicBoolean historyLoaded = new AtomicBoolean(false);
   public static volatile long lastClickMs = 0L;
   public static final long CLICK_DEBOUNCE_MS = 500L;

   public SpotifyOverlay() {
   }

   public static void ensureHistoryLoaded() {
      if (historyLoaded.compareAndSet(false, true)) {
         loadHistory();
      }
   }

   public static void onTrackChanged(String text, String text2) {
      ensureHistoryLoaded();
      if (text2 != null && !text2.isEmpty()) {
         if (!text2.equals(currentTitle) || !text.equals(currentArtist)) {
            currentTitle = text2;
            currentArtist = text;
            synchronized (ALL_KNOWN) {
               boolean flag = false;

               for (int i = 0; i < ALL_KNOWN.size(); i++) {
                  if (ALL_KNOWN.get(i)[1].equals(text2) && ALL_KNOWN.get(i)[0].equals(text)) {
                     currentIdx = i;
                     flag = true;
                     break;
                  }
               }

               if (!flag) {
                  ALL_KNOWN.add(new String[]{text, text2});
                  currentIdx = ALL_KNOWN.size() - 1;
                  if (ALL_KNOWN.size() > 300) {
                     int k = ALL_KNOWN.size() - 300;

                     for (int j = 0; j < k; j++) {
                        ALL_KNOWN.remove(0);
                     }

                     currentIdx = Math.max(0, currentIdx - k);
                     scroll = Math.max(0, scroll - k);
                  }

                  exec.submit(SpotifyOverlay::saveHistory);
               }
            }

            clampScrollToCurrent();
         }
      }
   }

   public static void clampScrollToCurrent() {
      if (currentIdx < scroll) {
         scroll = Math.max(0, currentIdx);
      } else if (currentIdx >= scroll + 12) {
         scroll = currentIdx - 12 + 1;
      }
   }

   public static int getMaxScroll() {
      synchronized (ALL_KNOWN) {
         return Math.max(0, ALL_KNOWN.size() - 12);
      }
   }

   public static void render(DrawContext context, double mouseX, double mouseY) {
      if (visible) {
         ensureHistoryLoaded();
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null) {
            TextRenderer textrenderer = minecraftClient.textRenderer;
            int i = Water.getAccentArgb();
            int j = i & 16777215;
            ArrayList arrayList;
            synchronized (ALL_KNOWN) {
               arrayList = new ArrayList<>(ALL_KNOWN);
            }

            int l1 = Math.max(1, Math.min(12, arrayList.size()));
            int k = 18 + l1 * 13 + 2;
            Render2D.drawRoundedRect(context, posX, posY, 220.0F, k, 8.0F, -871556572, false);
            Render2D.drawRoundedOutline(context, posX, posY, 220.0F, k, 8.0F, 1.0F, 1073741824 | j, false);
            context.fill(posX + 1, posY + 1, posX + 220 - 1, posY + 2, 369098751);
            context.fill(posX, posY, posX + 220, posY + 18, 587202559);
            context.fill(posX, posY + 3, posX + 3, posY + 18 - 3, 0xFF000000 | j);
            String s = "QUEUE  " + (arrayList.isEmpty() ? "play a song" : arrayList.size() + " songs");
            context.drawText(textrenderer, s, posX + 5 + 5, posY + 9 - 4, -1117449, false);
            if (arrayList.isEmpty()) {
               context.drawText(textrenderer, "Play a song to start", posX + 5, posY + 18 + 4, 1442840575, false);
            } else {
               int l = Math.min(scroll + 12, arrayList.size());

               for (int i1 = scroll; i1 < l; i1++) {
                  String[] astring = (String[])arrayList.get(i1);
                  int j1 = posY + 18 + (i1 - scroll) * 13;
                  boolean flag = astring[1].equals(currentTitle) && astring[0].equals(currentArtist);
                  boolean flag1 = mouseX >= posX && mouseX < posX + 220 && mouseY >= j1 && mouseY < j1 + 13;
                  boolean flag2 = i1 < currentIdx;
                  if (flag) {
                     context.fill(posX, j1, posX + 220, j1 + 13, 620756992 | j);
                     context.fill(posX, j1, posX + 3, j1 + 13, 0xFF000000 | j);
                  } else if (flag1) {
                     context.fill(posX, j1, posX + 220, j1 + 13, 369098751);
                  }

                  int k1 = flag ? 0xFF000000 | j : (flag2 ? -10456960 : -5588020);
                  String s1 = (astring[0].isEmpty() ? "" : astring[0] + " - ") + astring[1];
                  String s2 = textrenderer.trimToWidth(s1, 210 - (flag ? 6 : 2));
                  context.drawText(textrenderer, s2, posX + 5 + (flag ? 5 : 2), j1 + 6 - 4, k1, false);
               }

               if (arrayList.size() > 12) {
                  int i2 = posX + 220 - 3;
                  int j2 = posY + 18;
                  int k2 = l1 * 13;
                  float f = Math.max(16.0F, k2 * (12.0F / arrayList.size()));
                  float f1 = j2 + (k2 - f) * ((float)scroll / Math.max(1, getMaxScroll()));
                  context.fill(i2, j2, i2 + 3, j2 + k2, 369098751);
                  context.fill(i2, (int)f1, i2 + 3, (int)(f1 + f), -2147483648 | j);
               }
            }
         }
      }
   }

   public static boolean handleClick(double mouseX, double mouseY, int button) {
      if (!visible) {
         return false;
      } else {
         ArrayList arrayList;
         synchronized (ALL_KNOWN) {
            arrayList = new ArrayList<>(ALL_KNOWN);
         }

         int k = Math.max(1, Math.min(12, arrayList.size()));
         int i = 18 + k * 13 + 2;
         if (mouseX < posX || mouseX > posX + 220 || mouseY < posY || mouseY > posY + i) {
            return false;
         } else if (mouseY < posY + 18) {
            if (button == 0) {
               dragging = true;
               dragOffX = mouseX - posX;
               dragOffY = mouseY - posY;
            }

            return true;
         } else {
            if (button == 0 && mouseY < posY + 18 + k * 13) {
               int j = ((int)mouseY - posY - 18) / 13 + scroll;
               if (j >= 0 && j < arrayList.size()) {
                  playFromHistory(((String[])arrayList.get(j))[0], ((String[])arrayList.get(j))[1], j);
                  return true;
               }
            }

            return true;
         }
      }
   }

   public static boolean handleDrag(double mouseX, double mouseY) {
      if (!dragging) {
         return false;
      } else {
         posX = (int)(mouseX - dragOffX);
         posY = (int)(mouseY - dragOffY);
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null) {
            posX = Math.max(0, Math.min(minecraftClient.getWindow().getScaledWidth() - 220, posX));
            posY = Math.max(0, Math.min(minecraftClient.getWindow().getScaledHeight() - 50, posY));
         }

         return true;
      }
   }

   public static void stopDragging() {
      dragging = false;
   }

   public static boolean isDragging() {
      return dragging;
   }

   public static boolean handleScroll(double mouseX, double mouseY, double amount) {
      if (!visible) {
         return false;
      } else {
         ArrayList arrayList;
         synchronized (ALL_KNOWN) {
            arrayList = new ArrayList<>(ALL_KNOWN);
         }

         int j = Math.max(1, Math.min(12, arrayList.size()));
         int i = 18 + j * 13 + 2;
         if (!(mouseX < posX) && !(mouseX > posX + 220) && !(mouseY < posY) && !(mouseY > posY + i)) {
            scroll = Math.max(0, Math.min(getMaxScroll(), scroll + (amount > 0.0 ? -1 : 1)));
            return true;
         } else {
            return false;
         }
      }
   }

   public static void playFromHistory(String text, String text2, int var2) {
      if (!text2.equals(currentTitle) || !text.equals(currentArtist)) {
         long i = System.currentTimeMillis();
         if (i - lastClickMs >= 500L) {
            lastClickMs = i;
            int j = var2 - currentIdx;
            if (j != 0) {
               SpotifyHUD.sendMediaAction(j > 0 ? "next" : "prev");
            }
         }
      }
   }

   public static Path getHistoryPath() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient != null && minecraftClient.runDirectory != null
         ? minecraftClient.runDirectory.toPath().resolve("water_spotify_history.txt")
         : null;
   }

   public static void loadHistory() {
      try {
         Path path = getHistoryPath();
         if (path == null || !Files.isRegularFile(path)) {
            return;
         }

         List list = Files.readAllLines(path, StandardCharsets.UTF_8);
         synchronized (ALL_KNOWN) {
            ALL_KNOWN.clear();

            for (String s : (Iterable<String>)list) {
               if (s != null && !s.isBlank()) {
                  int i = s.indexOf(9);
                  if (i >= 0) {
                     String s1 = s.substring(0, i);
                     String s2 = s.substring(i + 1);
                     if (!s2.isEmpty()) {
                        ALL_KNOWN.add(new String[]{s1, s2});
                     }
                  }
               }
            }

            if (ALL_KNOWN.size() > 300) {
               int j = ALL_KNOWN.size() - 300;

               for (int k = 0; k < j; k++) {
                  ALL_KNOWN.remove(0);
               }
            }
         }
      } catch (Exception exception) {
      }
   }

   public static void saveHistory() {
      try {
         Path path = getHistoryPath();
         if (path == null) {
            return;
         }

         ArrayList arrayList;
         synchronized (ALL_KNOWN) {
            arrayList = new ArrayList<>(ALL_KNOWN);
         }

         Files.createDirectories(path.getParent());

         try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String[] astring : (Iterable<String[]>)arrayList) {
               String s = sanitizeField(astring[0]);
               String s1 = sanitizeField(astring[1]);
               bufferedWriter.write(s);
               bufferedWriter.write(9);
               bufferedWriter.write(s1);
               bufferedWriter.newLine();
            }
         }
      } catch (IOException ioexception) {
      }
   }

   public static String sanitizeField(String text) {
      return text == null ? "" : text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
   }
}
