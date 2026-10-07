package com.water.module.modules.client;

import com.water.gui.ClickGuiScreen;
import com.water.gui.HudEditor;
import com.water.gui.ModuleSearchScreen;
import com.water.gui.SpotifyOverlay;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.render.Render2D;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class SpotifyHUD extends Module {
   public static SpotifyHUD INSTANCE;
   public final Setting<Float> scale = new Setting<>("Scale", 1.0F, 0.6F, 2.0F);
   public volatile String title = "";
   public volatile String artist = "";
   public volatile boolean isPlaying = false;
   public volatile boolean isRepeating = false;
   public volatile long anchorPosMs = 0L;
   public volatile long anchorTimeMs = 0L;
   public volatile long durMs = 0L;
   public ScheduledExecutorService scheduler;
   public ExecutorService actionExec;
   public File pollScriptFile;
   public File ctrlScriptFile;
   public File artFile;
   public static final Identifier ART_ID = Identifier.of("water", "spotify_art");
   public NativeImageBackedTexture artTex = null;
   public volatile long artLastModified = 0L;
   public volatile boolean hasArt = false;
   public float fade = 0.0F;
   public long lastNanos = 0L;
   public volatile boolean hasPolledOnce = false;
   public final AtomicBoolean polling = new AtomicBoolean(false);
   public final AtomicInteger fastPollCount = new AtomicInteger(0);
   public static final int BASE_W = 270;
   public static final int BASE_H = 60;
   public static final int BASE_ART = 52;
   public static final int BASE_PAD = 10;
   public static final int PROG_H = 3;
   public static final int WAVE_BARS = 20;
   public static final int WAVE_H = 14;
   public volatile float waveSeed = 0.0F;
   public volatile String waveForTitle = null;
   public static final String POLL_SCRIPT = "[void][System.Reflection.Assembly]::LoadFile('C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\System.Runtime.WindowsRuntime.dll')\r\n$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]\r\n$g = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -like 'IAsyncOperation*' })[0]\r\nfunction Aw($op,$t){$m=$g.MakeGenericMethod($t);$task=$m.Invoke($null,@($op));$task.GetAwaiter().GetResult()}\r\n$asStreamForRead = [System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsStreamForRead' -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1\r\ntry {\r\n  $mgr = Aw([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\r\n  $s = $mgr.GetCurrentSession()\r\n  if ($s) {\r\n    $p = Aw($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\r\n    $tl = $s.GetTimelineProperties()\r\n    $pb = $s.GetPlaybackInfo()\r\n    if ($p.Title) {\r\n      if ($p.Thumbnail -and $asStreamForRead) {\r\n        try {\r\n          $stream = Aw($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\r\n          $netStream = $asStreamForRead.Invoke($null, @($stream))\r\n          $outPath = Join-Path $env:TEMP 'water_spotify_art.png'\r\n          $fs = [System.IO.File]::Create($outPath)\r\n          $netStream.CopyTo($fs)\r\n          $fs.Close()\r\n          $netStream.Close()\r\n        } catch {}\r\n      }\r\n      Write-Output ($p.Artist + '|||' + $p.Title + '|||' + [long]$tl.Position.TotalMilliseconds + '|||' + [long]$tl.EndTime.TotalMilliseconds + '|||' + ($pb.PlaybackStatus.ToString() -eq 'Playing'))\r\n    }\r\n  }\r\n} catch {}\r\n";
   public static final String CTRL_SCRIPT = "param([string]$action)\r\n[void][System.Reflection.Assembly]::LoadFile('C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\System.Runtime.WindowsRuntime.dll')\r\n$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]\r\n$g = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -like 'IAsyncOperation*' })[0]\r\nfunction Aw($op,$t){$m=$g.MakeGenericMethod($t);$task=$m.Invoke($null,@($op));$task.GetAwaiter().GetResult()}\r\ntry {\r\n  $mgr = Aw([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\r\n  $s = $mgr.GetCurrentSession()\r\n  if ($s) {\r\n    switch ($action) {\r\n      'next'    { Aw($s.TrySkipNextAsync()) ([bool]) | Out-Null }\r\n      'prev'    { Aw($s.TrySkipPreviousAsync()) ([bool]) | Out-Null }\r\n      'toggle'  { Aw($s.TryTogglePlayPauseAsync()) ([bool]) | Out-Null }\r\n      'repeat'  { try { $pb2 = $s.GetPlaybackInfo(); $cur = $pb2.AutoRepeatMode; $next = if($cur -eq [Windows.Media.MediaPlaybackAutoRepeatMode]::None){'Track'} elseif($cur -eq [Windows.Media.MediaPlaybackAutoRepeatMode]::Track){'List'} else{'None'}; Aw($s.TryChangeAutoRepeatModeAsync([Windows.Media.MediaPlaybackAutoRepeatMode]::$next)) ([bool]) | Out-Null } catch {} }\r\n    }\r\n  }\r\n} catch {}\r\n";

   public SpotifyHUD() {
      super("Spotify HUD", Category.CLIENT);
      this.addSetting(this.scale);
      INSTANCE = this;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   public static float getScale() {
      return INSTANCE == null ? 1.0F : INSTANCE.scale.getValue();
   }

   public static void setScale(float scale) {
      if (INSTANCE != null) {
         float f = INSTANCE.scale.getMin() instanceof Float f1 ? f1 : 0.6F;
         float f2 = INSTANCE.scale.getMax() instanceof Float f3 ? f3 : 2.0F;
         INSTANCE.scale.setValue(Math.max(f, Math.min(f2, scale)));
         int[] aint = HUD.getPosition(HudElement.SPOTIFY_HUD);
         int j = clampX(aint[0]);
         int i = clampY(aint[1]);
         if (j != aint[0] || i != aint[1]) {
            HUD.setPosition(HudElement.SPOTIFY_HUD, j, i);
         }
      }
   }

   public static int getWidth() {
      return Math.round(270.0F * getScale());
   }

   public static int getHeight() {
      return Math.round(60.0F * getScale());
   }

   public static int scaleInt(int value) {
      return Math.round(value * getScale());
   }

   public static float scaleFloat(float value) {
      return value * getScale();
   }

   public static int clampX(int x) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient == null ? x : Math.max(0, Math.min(x, minecraftClient.getWindow().getScaledWidth() - getWidth()));
   }

   public static int clampY(int y) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient == null ? y : Math.max(0, Math.min(y, minecraftClient.getWindow().getScaledHeight() - getHeight()));
   }

   public static int getX() {
      return clampX(HUD.getPosition(HudElement.SPOTIFY_HUD)[0]);
   }

   public static int getY() {
      return clampY(HUD.getPosition(HudElement.SPOTIFY_HUD)[1]);
   }

   @Override
   public void onEnable() {
      this.resetPlaybackState();
   }

   @Override
   public void onTick() {
      if (this.scheduler == null || this.scheduler.isShutdown()) {
         this.resetPlaybackState();
      }
   }

   public void resetPlaybackState() {
      this.fade = 0.0F;
      this.lastNanos = 0L;
      this.hasPolledOnce = false;
      this.fastPollCount.set(0);
      this.writeHelperScripts();
      this.artFile = new File(System.getenv("TEMP"), "water_spotify_art.png");
      Thread thread = new Thread(this::pollMediaSession, "water-spotify-initial");
      thread.setDaemon(true);
      thread.start();
      this.scheduler = Executors.newSingleThreadScheduledExecutor(var0 -> {
         Thread thread1 = new Thread(var0, "water-spotify");
         thread1.setDaemon(true);
         return thread1;
      });
      this.actionExec = Executors.newSingleThreadExecutor(var0 -> {
         Thread thread1 = new Thread(var0, "water-spotify-ctrl");
         thread1.setDaemon(true);
         return thread1;
      });
      this.scheduler.scheduleAtFixedRate(() -> {
         int i = this.fastPollCount.getAndIncrement();
         if (i < 10) {
            this.pollMediaSession();
            if (!this.title.isEmpty()) {
               this.fastPollCount.set(Integer.MAX_VALUE);
            }
         }
      }, 1000L, 800L, TimeUnit.MILLISECONDS);
      this.scheduler.scheduleAtFixedRate(this::pollMediaSession, 2L, 2L, TimeUnit.SECONDS);
   }

   @Override
   public void onDisable() {
      if (this.scheduler != null) {
         this.scheduler.shutdownNow();
      }

      if (this.actionExec != null) {
         this.actionExec.shutdownNow();
      }

      this.title = "";
      this.artist = "";
      this.isPlaying = false;
      this.anchorPosMs = 0L;
      this.anchorTimeMs = 0L;
      this.durMs = 0L;
      this.fade = 0.0F;
      this.hasArt = false;
      this.waveForTitle = null;
      if (this.artTex != null) {
         try {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(ART_ID);
         } catch (Exception exception) {
         }

         this.artTex = null;
      }
   }

   public void writeHelperScripts() {
      try {
         this.pollScriptFile = File.createTempFile("water_smtc_poll_", ".ps1");
         this.pollScriptFile.deleteOnExit();

         try (FileWriter fileWriter = new FileWriter(this.pollScriptFile, StandardCharsets.UTF_8)) {
            fileWriter.write(
               "[void][System.Reflection.Assembly]::LoadFile('C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\System.Runtime.WindowsRuntime.dll')\r\n$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]\r\n$g = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -like 'IAsyncOperation*' })[0]\r\nfunction Aw($op,$t){$m=$g.MakeGenericMethod($t);$task=$m.Invoke($null,@($op));$task.GetAwaiter().GetResult()}\r\n$asStreamForRead = [System.IO.WindowsRuntimeStreamExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsStreamForRead' -and $_.GetParameters().Count -eq 1 } | Select-Object -First 1\r\ntry {\r\n  $mgr = Aw([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\r\n  $s = $mgr.GetCurrentSession()\r\n  if ($s) {\r\n    $p = Aw($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\r\n    $tl = $s.GetTimelineProperties()\r\n    $pb = $s.GetPlaybackInfo()\r\n    if ($p.Title) {\r\n      if ($p.Thumbnail -and $asStreamForRead) {\r\n        try {\r\n          $stream = Aw($p.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])\r\n          $netStream = $asStreamForRead.Invoke($null, @($stream))\r\n          $outPath = Join-Path $env:TEMP 'water_spotify_art.png'\r\n          $fs = [System.IO.File]::Create($outPath)\r\n          $netStream.CopyTo($fs)\r\n          $fs.Close()\r\n          $netStream.Close()\r\n        } catch {}\r\n      }\r\n      Write-Output ($p.Artist + '|||' + $p.Title + '|||' + [long]$tl.Position.TotalMilliseconds + '|||' + [long]$tl.EndTime.TotalMilliseconds + '|||' + ($pb.PlaybackStatus.ToString() -eq 'Playing'))\r\n    }\r\n  }\r\n} catch {}\r\n"
            );
         }

         this.ctrlScriptFile = File.createTempFile("water_smtc_ctrl_", ".ps1");
         this.ctrlScriptFile.deleteOnExit();

         try (FileWriter fileWriter1 = new FileWriter(this.ctrlScriptFile, StandardCharsets.UTF_8)) {
            fileWriter1.write(
               "param([string]$action)\r\n[void][System.Reflection.Assembly]::LoadFile('C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\System.Runtime.WindowsRuntime.dll')\r\n$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime]\r\n$g = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -like 'IAsyncOperation*' })[0]\r\nfunction Aw($op,$t){$m=$g.MakeGenericMethod($t);$task=$m.Invoke($null,@($op));$task.GetAwaiter().GetResult()}\r\ntry {\r\n  $mgr = Aw([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\r\n  $s = $mgr.GetCurrentSession()\r\n  if ($s) {\r\n    switch ($action) {\r\n      'next'    { Aw($s.TrySkipNextAsync()) ([bool]) | Out-Null }\r\n      'prev'    { Aw($s.TrySkipPreviousAsync()) ([bool]) | Out-Null }\r\n      'toggle'  { Aw($s.TryTogglePlayPauseAsync()) ([bool]) | Out-Null }\r\n      'repeat'  { try { $pb2 = $s.GetPlaybackInfo(); $cur = $pb2.AutoRepeatMode; $next = if($cur -eq [Windows.Media.MediaPlaybackAutoRepeatMode]::None){'Track'} elseif($cur -eq [Windows.Media.MediaPlaybackAutoRepeatMode]::Track){'List'} else{'None'}; Aw($s.TryChangeAutoRepeatModeAsync([Windows.Media.MediaPlaybackAutoRepeatMode]::$next)) ([bool]) | Out-Null } catch {} }\r\n    }\r\n  }\r\n} catch {}\r\n"
            );
         }
      } catch (Exception exception) {
         this.pollScriptFile = null;
         this.ctrlScriptFile = null;
      }
   }

   public void pollMediaSession() {
      if (this.pollScriptFile == null || !this.pollScriptFile.exists()) {
         this.writeHelperScripts();
      }

      if (this.pollScriptFile == null) {
         this.hasPolledOnce = true;
      } else if (this.polling.compareAndSet(false, true)) {
         try {
            long i = System.currentTimeMillis();
            Process process = new ProcessBuilder(
                  "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", this.pollScriptFile.getAbsolutePath()
               )
               .redirectErrorStream(true)
               .start();
            String s = null;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            String s1;
            try {
               while ((s1 = bufferedReader.readLine()) != null) {
                  s1 = s1.trim();
                  if (s1.contains("|||")) {
                     s = s1;
                     break;
                  }
               }
            } catch (Throwable throwable1) {
               try {
                  bufferedReader.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }

               throw throwable1;
            }

            bufferedReader.close();
            process.waitFor(8L, TimeUnit.SECONDS);
            long j1 = System.currentTimeMillis();
            long j = (i + j1) / 2L;
            if (s == null) {
               return;
            }

            String[] astring = s.split("\\|\\|\\|", -1);
            if (astring.length < 2 || astring[1].trim().isEmpty()) {
               return;
            }

            String s2 = astring[1].trim();
            String s3 = astring[0].trim();
            long k = astring.length > 2 ? parseLongSafe(astring[2]) : 0L;
            long l = astring.length > 3 ? parseLongSafe(astring[3]) : 0L;
            boolean flag = astring.length > 4 && astring[4].trim().equalsIgnoreCase("True");
            boolean flag1 = !s2.equals(this.title);
            boolean flag2 = flag != this.isPlaying;
            long i1 = this.isPlaying ? this.anchorPosMs + Math.max(0L, j - this.anchorTimeMs) : this.anchorPosMs;
            boolean flag3 = Math.abs(k - i1) > 4000L;
            this.title = s2;
            this.artist = s3;
            this.durMs = l;
            this.isPlaying = flag;
            if (flag1 || flag2 || flag3 || this.anchorTimeMs == 0L) {
               this.anchorPosMs = k;
               this.anchorTimeMs = j;
            }

            if (flag1) {
               SpotifyOverlay.onTrackChanged(s3, s2);
            }
         } catch (Exception exception) {
            return;
         } finally {
            this.hasPolledOnce = true;
            this.polling.set(false);
         }
      }
   }

   public static long parseLongSafe(String text) {
      try {
         return Long.parseLong(text.trim());
      } catch (Exception exception) {
         return 0L;
      }
   }

   public static float hashToUnit(String text) {
      return text == null ? 0.0F : text.hashCode() % 1000 / 1000.0F;
   }

   public static boolean handleClick(double mouseX, double mouseY) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient == null) {
            return false;
         } else if (!HudEditor.isEditing && !HudEditor.INSTANCE.isDragging()) {
            int i = getX();
            int j = getY();
            int k = getWidth();
            int l = getHeight();
            if (mouseX >= i && mouseX <= i + k && mouseY >= j && mouseY <= j + l) {
               minecraftClient.setScreen(new ModuleSearchScreen(minecraftClient.currentScreen));
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void openSearchScreen() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         minecraftClient.setScreen(new ModuleSearchScreen(minecraftClient.currentScreen));
      }
   }

   public static void sendMediaAction(String text) {
      if (INSTANCE != null && INSTANCE.actionExec != null && INSTANCE.ctrlScriptFile != null) {
         long i = System.currentTimeMillis();
         if ("repeat".equals(text)) {
            INSTANCE.isRepeating = !INSTANCE.isRepeating;
         }

         if ("toggle".equals(text)) {
            if (INSTANCE.isPlaying) {
               INSTANCE.anchorPosMs = INSTANCE.anchorPosMs + Math.max(0L, i - INSTANCE.anchorTimeMs);
               INSTANCE.anchorTimeMs = i;
               INSTANCE.isPlaying = false;
            } else {
               INSTANCE.anchorTimeMs = i;
               INSTANCE.isPlaying = true;
            }
         }

         INSTANCE.actionExec
            .submit(
               () -> {
                  try {
                     Process process = new ProcessBuilder(
                           "powershell",
                           "-NoProfile",
                           "-NonInteractive",
                           "-ExecutionPolicy",
                           "Bypass",
                           "-File",
                           INSTANCE.ctrlScriptFile.getAbsolutePath(),
                           "-action",
                           text
                        )
                        .redirectErrorStream(true)
                        .start();
                     process.waitFor(5L, TimeUnit.SECONDS);
                     if (INSTANCE.scheduler != null && !INSTANCE.scheduler.isShutdown()) {
                        INSTANCE.scheduler.submit(INSTANCE::pollMediaSession);
                     }
                  } catch (Exception exception) {
                  }
               }
            );
      }
   }

   public void loadAlbumArt() {
      if (this.artFile != null && this.artFile.exists() && this.artFile.length() >= 200L) {
         long i = this.artFile.lastModified();
         if (i != this.artLastModified || this.artTex == null) {
            try {
               byte[] abyte = Files.readAllBytes(this.artFile.toPath());

               try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(abyte)) {
                  NativeImage nativeImage = NativeImage.read(byteArrayInputStream);
                  if (this.artTex != null) {
                     try {
                        MinecraftClient.getInstance().getTextureManager().destroyTexture(ART_ID);
                     } catch (Exception exception) {
                     }
                  }

                  this.artTex = new NativeImageBackedTexture(() -> "spotify_art", nativeImage);
                  MinecraftClient.getInstance().getTextureManager().registerTexture(ART_ID, this.artTex);
                  this.artLastModified = i;
                  this.hasArt = true;
               }
            } catch (Exception exception1) {
            }
         }
      }
   }

   public static void renderHud(DrawContext context) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         if (minecraftClient != null && minecraftClient.player != null) {
            if (!(minecraftClient.currentScreen instanceof ClickGuiScreen)) {
               if (!minecraftClient.getDebugHud().shouldShowDebugHud()) {
                  long i = System.nanoTime();
                  float f = INSTANCE.lastNanos == 0L ? 0.016F : Math.min(0.1F, (float)(i - INSTANCE.lastNanos) / 1.0E9F);
                  INSTANCE.lastNanos = i;
                  INSTANCE.fade = INSTANCE.fade + (1.0F - INSTANCE.fade) * (1.0F - (float)Math.exp(-12.0F * f));
                  float f1 = INSTANCE.fade;
                  if (!(f1 < 0.01F)) {
                     INSTANCE.loadAlbumArt();
                     if (!INSTANCE.hasPolledOnce || !INSTANCE.title.isEmpty()) {
                        if (!Objects.equals(INSTANCE.waveForTitle, INSTANCE.title)) {
                           INSTANCE.waveSeed = hashToUnit(INSTANCE.title);
                           INSTANCE.waveForTitle = INSTANCE.title;
                        }

                        int j = getWidth();
                        int k = getHeight();
                        int l = scaleInt(52);
                        int i1 = scaleInt(10);
                        int j1 = getX();
                        int k1 = getY();
                        FontRenderer fontrenderer = FontRenderer.INSTANCE;
                        int l1 = Water.getAccentArgb();
                        float f2 = Water.getGuiRoundness();
                        Render2D.drawRoundedRect(context, j1 - 1, k1 - 1, j + 2, k + 2, f2 + 1.0F, withAlpha(l1 & 16777215, (int)(20.0F * f1)), false);
                        Render2D.drawRoundedRect(context, j1, k1, j, k, f2, withAlpha(526344, (int)(145.0F * f1)), false);
                        Render2D.drawRoundedRect(context, j1 + 1, k1 + 1, j - 2, scaleInt(18), f2, withAlpha(16777215, (int)(16.0F * f1)), false);
                        Render2D.drawRoundedRect(context, j1, k1 + k - scaleInt(14), j, scaleInt(14), f2, withAlpha(l1 & 16777215, (int)(9.0F * f1)), false);
                        Render2D.drawRoundedOutline(context, j1, k1, j, k, f2, 1.0F, withAlpha(16777215, (int)(45.0F * f1)), false);
                        Render2D.drawRoundedOutline(context, j1 + 1, k1 + 1, j - 2, k - 2, f2 - 1.0F, 0.5F, withAlpha(0, (int)(35.0F * f1)), false);
                        int i2 = scaleInt(16);
                        int j2 = j1 + j - i2 - scaleInt(5);
                        int k2 = k1 + scaleInt(4);
                        drawCircleButton(context, j2, k2, i2, f1);
                        int l2 = j1 + i1;
                        int i3 = k1 + (k - l) / 2;
                        if (INSTANCE.hasArt) {
                           int j3 = Math.max(0, Math.min(255, (int)(255.0F * f1))) << 24 | 16777215;
                           Render2D.drawTexture(context, l2, i3, l, ART_ID, j3, scaleFloat(4.0F), false);
                           Render2D.drawRoundedOutline(context, l2, i3, l, l, scaleFloat(4.0F), 1.0F, withAlpha(16777215, (int)(55.0F * f1)), false);
                           Render2D.drawRoundedOutline(context, l2 - 1, i3 - 1, l + 2, l + 2, scaleFloat(5.0F), 0.5F, withAlpha(l1 & 16777215, (int)(35.0F * f1)), false);
                        } else {
                           Render2D.drawRoundedRect(context, l2, i3, l, l, scaleFloat(4.0F), withAlpha(16777215, (int)(8.0F * f1)), false);
                           Render2D.drawRoundedOutline(context, l2, i3, l, l, scaleFloat(4.0F), 1.0F, withAlpha(16777215, (int)(35.0F * f1)), false);
                           String s2 = "\u266b";
                           int k3 = fontrenderer.getWidth(s2);
                           fontrenderer.drawString(context, s2, l2 + (l - k3) / 2, i3 + (l - 9) / 2, withAlpha(l1 & 16777215, (int)(180.0F * f1)));
                        }

                        int k6 = l2 + l + i1;
                        int l6 = j - (k6 - j1) - i1 - scaleInt(4);
                        long l3 = System.currentTimeMillis();
                        long i4;
                        if (INSTANCE.isPlaying && INSTANCE.anchorTimeMs > 0L) {
                           long j4 = Math.max(0L, l3 - INSTANCE.anchorTimeMs);
                           i4 = INSTANCE.anchorPosMs + j4;
                           if (INSTANCE.durMs > 0L) {
                              i4 = Math.min(INSTANCE.durMs, i4);
                           }
                        } else {
                           i4 = INSTANCE.anchorPosMs;
                        }

                        float f3 = INSTANCE.durMs > 0L ? Math.min(1.0F, (float)i4 / (float)INSTANCE.durMs) : 0.0F;
                        int k4 = scaleInt(3);
                        int l4 = scaleInt(9) + scaleInt(3) + scaleInt(8) + scaleInt(4) + k4 + scaleInt(4);
                        int i5 = k1 + (k - l4) / 2 + scaleInt(5);
                        String s = !INSTANCE.hasPolledOnce ? "" : (INSTANCE.title.isEmpty() ? "No track playing" : INSTANCE.title);
                        fontrenderer.drawString(context, ellipsize(s, l6, fontrenderer), k6, i5, withAlpha(16777215, (int)(255.0F * f1)));
                        int j5 = i5 + scaleInt(11);
                        fontrenderer.drawString(context, ellipsize(INSTANCE.artist, l6, fontrenderer), k6, j5, withAlpha(11184810, (int)(200.0F * f1)));
                        int k5 = j5 + scaleInt(11);
                        int l5 = Math.max(0, Math.round(l6 * f3));
                        Render2D.drawRoundedRect(context, k6, k5, l6, k4, k4 / 2.0F, withAlpha(16777215, (int)(28.0F * f1)), false);
                        if (l5 > 0) {
                           Render2D.drawRoundedRect(context, k6, k5, l5, k4, k4 / 2.0F, withAlpha(l1 & 16777215, (int)(230.0F * f1)), false);
                        }

                        if (l5 > 0) {
                           int i6 = scaleInt(5);
                           Render2D.drawRoundedRect(context, k6 + l5 - i6 / 2, k5 - (i6 - k4) / 2, i6, i6, i6 / 2.0F, withAlpha(16777215, (int)(235.0F * f1)), false);
                        }

                        String s3 = formatTime(i4);
                        String s1 = INSTANCE.durMs > 0L ? formatTime(INSTANCE.durMs) : "--:--";
                        int j6 = k5 + k4 + scaleInt(2);
                        fontrenderer.drawString(context, s3, k6, j6, withAlpha(8947848, (int)(155.0F * f1)));
                        fontrenderer.drawString(context, s1, k6 + l6 - fontrenderer.getWidth(s1), j6, withAlpha(8947848, (int)(155.0F * f1)));
                     }
                  }
               }
            }
         }
      }
   }

   public static void drawCircleButton(DrawContext context, int var1, int var2, int var3, float var4) {
      int i = (int)(var4 * 255.0F);
      int j = 1947988;
      Render2D.drawRoundedRect(context, var1, var2, var3, var3, var3 / 2.0F, withAlpha(j, i), false);
      Render2D.drawRoundedOutline(context, var1, var2, var3, var3, var3 / 2.0F, 0.8F, withAlpha(16777215, (int)(50.0F * var4)), false);
      float f = var3 * 0.18F;
      float f1 = var3 * 0.09F;
      float f2 = var3 * 0.115F;
      float f3 = f1 / 2.0F;
      int k = withAlpha(16777215, i);
      float f4 = var1 + f * 0.7F;
      float f5 = var2 + var3 * 0.3F;
      float f6 = var3 - f * 1.4F;
      float f7 = var1 + f * 1.1F;
      float f8 = f5 + f1 + f2;
      float f9 = var3 - f * 2.2F;
      float f10 = var1 + f * 1.55F;
      float f11 = f8 + f1 + f2;
      float f12 = var3 - f * 3.1F;
      Render2D.drawRoundedRect(context, (int)f4, (int)f5, (int)f6, (int)f1, f3, k, false);
      Render2D.drawRoundedRect(context, (int)f7, (int)f8, (int)f9, (int)f1, f3, k, false);
      Render2D.drawRoundedRect(context, (int)f10, (int)f11, (int)f12, (int)f1, f3, k, false);
   }

   public static String ellipsize(String text, int maxWidth, FontRenderer fontRenderer) {
      if (text != null && !text.isEmpty()) {
         if (fontRenderer.getWidth(text) <= maxWidth) {
            return text;
         } else {
            String s = "...";
            int i = fontRenderer.getWidth(s);
            if (i >= maxWidth) {
               return s;
            } else {
               StringBuilder stringBuilder = new StringBuilder();
               int[] aint = text.codePoints().toArray();

               for (int j : aint) {
                  String s1 = stringBuilder.toString() + new String(Character.toChars(j));
                  if (fontRenderer.getWidth(s1 + s) > maxWidth) {
                     break;
                  }

                  stringBuilder.appendCodePoint(j);
               }

               return stringBuilder.toString() + s;
            }
         }
      } else {
         return "";
      }
   }

   public static String formatTime(long millis) {
      long i = millis / 1000L;
      return i / 60L + ":" + String.format("%02d", i % 60L);
   }

   public static int withAlpha(int argb, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | argb & 16777215;
   }

   public static String watermarkFragment() {
      return "T";
   }
}
