package com.water.gui;

import com.water.module.modules.client.Water;
import com.water.render.Render2D;
import com.water.util.SpotifyApi;
import com.water.util.SpotifyAuth;
import com.water.util.SpotifyTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class ModuleSearchScreen extends Screen {
   public static final int W = 420;
   public static final int H = 360;
   public static final int R = 10;
   public static final int PAD = 10;
   public static final int HEAD_H = 44;
   public static final int FOOT_H = 44;
   public static final int ROW_H = 22;
   public static final int SRCH_H = 24;
   public static final int TABS_H = 28;
   public final Screen parent;
   public String search = "";
   public int scroll = 0;
   public int hoverRow = -1;
   public long openNs = 0L;
   public int tab = 0;
   public static final List<SpotifyTrack> QUEUE_CACHE = new ArrayList<>();
   public static final List<String[]> PLAYLISTS_CACHE = new ArrayList<>();
   public static final List<SpotifyTrack> TRACKS_CACHE = new ArrayList<>();
   public static volatile String selectedPlaylist = null;
   public static volatile String selectedPlaylistName = "";
   public List<Object> filtered = new ArrayList<>();
   public volatile boolean loading = false;
   public volatile String status = "";
   public final ExecutorService exec = Executors.newSingleThreadExecutor(var0 -> {
      Thread thread = new Thread(var0, "spotify-browser");
      thread.setDaemon(true);
      return thread;
   });
   public boolean enteringClientId = false;
   public String clientIdInput = "";

   public ModuleSearchScreen(Screen parent) {
      super(Text.literal(""));
      this.parent = parent;
   }

   public int getLeft() {
      return (this.width - 420) / 2;
   }

   public int getTop() {
      return (this.height - 360) / 2;
   }

   public int getListY() {
      return this.getTop() + 44 + 28 + 24 + 10;
   }

   public int getListHeight() {
      return 200;
   }

   public int getVisibleRows() {
      return this.getListHeight() / 22;
   }

   public int getMaxScroll() {
      return Math.max(0, this.filtered.size() - this.getVisibleRows());
   }

   @Override
   public void init() {
      String s = Water.getSpotifyClientId();
      if (s != null && !s.isBlank()) {
         SpotifyAuth.setClientId(s);
      }

      if (!SpotifyAuth.hasClientId()) {
         this.enteringClientId = true;
         this.clientIdInput = "";
         this.status = "Enter your Spotify Client ID";
      } else if (!SpotifyAuth.ensureValidToken()) {
         this.status = "Click CONNECT to login";
      } else {
         this.loadQueue();
      }
   }

   public void loadQueue() {
      this.loading = true;
      this.status = "Loading queue...";
      this.exec.submit(() -> {
         List list = SpotifyApi.getQueue();
         synchronized (QUEUE_CACHE) {
            QUEUE_CACHE.clear();
            QUEUE_CACHE.addAll(list);
         }

         this.status = list.isEmpty() ? "Queue is empty" : list.size() + " tracks";
         this.loading = false;
         this.client.execute(this::applyFilter);
      });
   }

   public void loadPlaylists() {
      this.loading = true;
      this.status = "Loading playlists...";
      this.exec.submit(() -> {
         List list = SpotifyApi.getPlaylists();
         synchronized (PLAYLISTS_CACHE) {
            PLAYLISTS_CACHE.clear();
            PLAYLISTS_CACHE.addAll(list);
         }

         this.status = list.size() + " playlists";
         this.loading = false;
         this.client.execute(this::applyFilter);
      });
   }

   public void openPlaylist(String text, String text2) {
      selectedPlaylist = text;
      selectedPlaylistName = text2;
      this.loading = true;
      this.status = "Loading " + text2 + "...";
      this.tab = 2;
      this.exec.submit(() -> {
         List list = SpotifyApi.getPlaylistTracks(text);
         synchronized (TRACKS_CACHE) {
            TRACKS_CACHE.clear();
            TRACKS_CACHE.addAll(list);
         }

         this.status = list.size() + " tracks in " + text2;
         this.loading = false;
         this.client.execute(this::applyFilter);
      });
   }

   public void applyFilter() {
      String s = this.search.trim().toLowerCase();
      this.filtered = new ArrayList<>();

      Object object = switch (this.tab) {
         case 0 -> QUEUE_CACHE;
         case 1 -> PLAYLISTS_CACHE;
         case 2 -> TRACKS_CACHE;
         default -> new ArrayList();
      };
      synchronized (object) {
         for (Object object1 : (Iterable<Object>)object) {
            String s1 = object1 instanceof SpotifyTrack spotifyTrack ? spotifyTrack.display() : (object1 instanceof String[] astring ? astring[1] : "");
            if (s.isEmpty() || s1.toLowerCase().contains(s)) {
               this.filtered.add(object1);
            }
         }
      }

      this.scroll = Math.max(0, Math.min(this.getMaxScroll(), this.scroll));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      if (this.openNs == 0L) {
         this.openNs = System.nanoTime();
      }

      float f = this.easeOutCubic(Math.min(1.0F, (float)(System.nanoTime() - this.openNs) / 1.8E8F));
      int i = this.getLeft();
      int j = this.getTop();
      int k = Water.getAccentArgb();
      int l = k & 16777215;
      context.fill(0, 0, this.width, this.height, this.scaleAlpha(-2013265920, f));
      Render2D.drawRoundedRect(context, i, j, 420.0F, 360.0F, 10.0F, this.scaleAlpha(-234353645, f), false);
      Render2D.drawRoundedOutline(context, i, j, 420.0F, 360.0F, 10.0F, 1.5F, this.withAlpha(l, (int)(80.0F * f)), false);
      Render2D.G(context, i, j, 420.0F, 44.0F, 10.0F, 10.0F, 0.0F, 0.0F, false, this.scaleAlpha(-871756264, f));
      context.fill(i, j + 44, i + 420, j + 44 + 1, this.scaleAlpha(553648127, f));
      context.fill(i, j + 10, i + 3, j + 44 - 10, this.withAlpha(l, (int)(255.0F * f)));
      context.drawText(this.textRenderer, "SPOTIFY", i + 10 + 8, j + 10, this.withAlpha(15659767, (int)(255.0F * f)), false);
      String s = this.enteringClientId
         ? "Enter Client ID to connect"
         : (!SpotifyAuth.hasClientId() ? "Not configured" : (!SpotifyAuth.ensureValidToken() ? (SpotifyAuth.isAuthInProgress() ? "Connecting..." : "Click CONNECT") : this.status));
      context.drawText(this.textRenderer, s, i + 10 + 8, j + 25, this.withAlpha(6320256, (int)(180.0F * f)), false);
      if (!SpotifyAuth.ensureValidToken() && !this.enteringClientId) {
         int i1 = i + 420 - 10 - 80;
         int j1 = j + 22 - 9;
         boolean flag = this.isInside(mouseX, mouseY, i1, j1, 80, 18);
         Render2D.drawRoundedRect(context, i1, j1, 80.0F, 18.0F, 4.0F, this.withAlpha(l, flag ? (int)(50.0F * f) : (int)(20.0F * f)), false);
         Render2D.drawRoundedOutline(context, i1, j1, 80.0F, 18.0F, 4.0F, 1.0F, this.withAlpha(l, (int)(150.0F * f)), false);
         String s1 = SpotifyAuth.hasClientId() ? "CONNECT" : "SETUP";
         context.drawText(this.textRenderer, s1, i1 + (80 - this.textRenderer.getWidth(s1)) / 2, j1 + 5, this.withAlpha(l, (int)(220.0F * f)), false);
      }

      if (this.enteringClientId) {
         this.drawSetupHint(context, i, j, mouseX, mouseY, f, l);
      } else if (!SpotifyAuth.ensureValidToken()) {
         context.drawText(
            this.textRenderer,
            "Click CONNECT to login with Spotify",
            i + (420 - this.textRenderer.getWidth("Click CONNECT to login with Spotify")) / 2,
            j + 180 - 4,
            this.withAlpha(6320256, (int)(180.0F * f)),
            false
         );
         this.drawFooter(context, i, j, mouseX, mouseY, f, l);
      } else {
         this.drawTabs(context, i, j, mouseX, mouseY, f, l);
         int l2 = j + 44 + 28 + 4;
         int i3 = i + 10;
         short short2 = 400;
         boolean flag3 = !this.search.isEmpty();
         Render2D.drawRoundedRect(context, i3, l2, short2, 24.0F, 6.0F, this.scaleAlpha(-16183784, f), false);
         Render2D.drawRoundedOutline(context, i3, l2, short2, 24.0F, 6.0F, 1.0F, this.withAlpha(flag3 ? l : 822083583, flag3 ? (int)(120.0F * f) : (int)(50.0F * f)), false);
         context.drawText(
            this.textRenderer,
            flag3 ? "\u2315  " + this.search + "_" : "\u2315  Search...",
            i3 + 8,
            l2 + 12 - 4,
            this.withAlpha(flag3 ? 15659767 : 6320256, (int)(200.0F * f)),
            false
         );
         int k1 = i + 10;
         int l1 = this.getListY();
         short short1 = 400;
         int i2 = this.getListHeight();
         Render2D.drawRoundedRect(context, k1, l1 - 2, short1, i2 + 4, 6.0F, this.scaleAlpha(-1442444784, f), false);
         this.hoverRow = -1;
         if (this.loading && this.filtered.isEmpty()) {
            context.drawText(
               this.textRenderer,
               this.status,
               k1 + (short1 - this.textRenderer.getWidth(this.status)) / 2,
               l1 + i2 / 2 - 4,
               this.withAlpha(6320256, (int)(180.0F * f)),
               false
            );
         } else if (this.filtered.isEmpty()) {
            String s2 = this.search.isEmpty() ? "Nothing here yet" : "No results for \"" + this.search + "\"";
            context.drawText(
               this.textRenderer, s2, k1 + (short1 - this.textRenderer.getWidth(s2)) / 2, l1 + i2 / 2 - 4, this.withAlpha(6320256, (int)(180.0F * f)), false
            );
         } else {
            int j3 = Math.min(this.scroll + this.getVisibleRows(), this.filtered.size());

            for (int j2 = this.scroll; j2 < j3; j2++) {
               Object object = this.filtered.get(j2);
               int k2 = l1 + (j2 - this.scroll) * 22;
               boolean flag1 = this.isInside(mouseX, mouseY, k1, k2, short1, 22);
               if (flag1) {
                  this.hoverRow = j2;
               }

               boolean flag2 = object instanceof SpotifyTrack spotifyTrack && spotifyTrack == this.filtered.get(0) && this.tab == 0;
               if (flag2) {
                  Render2D.drawRoundedRect(context, k1, k2, short1, 21.0F, 4.0F, this.withAlpha(l, (int)(25.0F * f)), false);
                  Render2D.drawRoundedOutline(context, k1, k2, short1, 21.0F, 4.0F, 1.0F, this.withAlpha(l, (int)(80.0F * f)), false);
               } else if (flag1) {
                  Render2D.drawRoundedRect(context, k1, k2, short1, 21.0F, 4.0F, this.scaleAlpha(369098751, f), false);
               }

               int l3 = k1 + 8;
               if (flag2) {
                  context.fill(k1 + 3, k2 + 5, k1 + 5, k2 + 22 - 5, this.withAlpha(l, (int)(255.0F * f)));
                  l3 = k1 + 10;
               }

               String s3 = j2 + 1 + ".";
               context.drawText(this.textRenderer, s3, l3, k2 + 11 - 4, this.withAlpha(5267568, (int)(160.0F * f)), false);
               l3 += this.textRenderer.getWidth(s3) + 4;
               if (object instanceof String[]) {
                  String s4 = ">";
                  context.drawText(this.textRenderer, s4, k1 + short1 - 14, k2 + 11 - 4, this.withAlpha(l, (int)(160.0F * f)), false);
               }

               String s5 = object instanceof SpotifyTrack spotifyTrack1
                  ? this.textRenderer.trimToWidth(spotifyTrack1.display(), short1 - l3 - 18)
                  : (object instanceof String[] astring ? this.textRenderer.trimToWidth(astring[1], short1 - l3 - 18) : "");
               int i4 = flag2 ? this.withAlpha(l, (int)(255.0F * f)) : this.withAlpha(flag1 ? 15659767 : 10531008, (int)(220.0F * f));
               context.drawText(this.textRenderer, s5, l3, k2 + 11 - 4, i4, false);
            }

            if (this.filtered.size() > this.getVisibleRows()) {
               int k3 = k1 + short1 + 2;
               float f1 = Math.max(20.0F, i2 * ((float)this.getVisibleRows() / this.filtered.size()));
               float f2 = l1 + (i2 - f1) * ((float)this.scroll / Math.max(1, this.getMaxScroll()));
               Render2D.drawRoundedRect(context, k3, l1, 3.0F, i2, 1.5F, this.scaleAlpha(318767103, f), false);
               Render2D.drawRoundedRect(context, k3, (int)f2, 3.0F, (int)f1, 1.5F, this.withAlpha(l, (int)(150.0F * f)), false);
            }
         }

         this.drawFooter(context, i, j, mouseX, mouseY, f, l);
      }
   }

   public void drawTabs(DrawContext context, int x, int y, int mouseX, int mouseY, float alpha, int accent) {
      int i = y + 44 + 4;
      short short1 = 133;
      String[] astring = new String[]{"QUEUE", "PLAYLISTS", this.tab == 2 ? selectedPlaylistName : "TRACKS"};

      for (int j = 0; j < 3; j++) {
         int k = x + 10 + j * (short1 + 2);
         boolean flag = this.tab == j;
         boolean flag1 = this.isInside(mouseX, mouseY, k, i, short1, 22);
         int l = flag ? this.withAlpha(accent, (int)(35.0F * alpha)) : this.withAlpha(16777215, flag1 ? (int)(18.0F * alpha) : (int)(8.0F * alpha));
         int i1 = flag ? this.withAlpha(accent, (int)(150.0F * alpha)) : this.withAlpha(16777215, (int)(30.0F * alpha));
         Render2D.drawRoundedRect(context, k, i, short1, 22.0F, 5.0F, l, false);
         Render2D.drawRoundedOutline(context, k, i, short1, 22.0F, 5.0F, 1.0F, i1, false);
         String s = this.textRenderer.trimToWidth(astring[j], short1 - 8);
         context.drawText(
            this.textRenderer,
            s,
            k + (short1 - this.textRenderer.getWidth(s)) / 2,
            i + 11 - 4,
            flag ? this.withAlpha(accent, (int)(255.0F * alpha)) : this.withAlpha(10531008, (int)(200.0F * alpha)),
            false
         );
      }
   }

   public void drawSetupHint(DrawContext context, int x, int y, int mouseX, int mouseY, float alpha, int accent) {
      int i = y + 180 - 40;
      context.drawText(
         this.textRenderer,
         "Setup Spotify (free on developer.spotify.com)",
         x + (420 - this.textRenderer.getWidth("Setup Spotify (free on developer.spotify.com)")) / 2,
         i - 20,
         this.withAlpha(10531008, (int)(200.0F * alpha)),
         false
      );
      context.drawText(this.textRenderer, "Enter Client ID:", x + 10, i, this.withAlpha(15659767, (int)(255.0F * alpha)), false);
      short short1 = 400;
      byte b0 = 22;
      Render2D.drawRoundedRect(context, x + 10, i + 14, short1, b0, 5.0F, this.scaleAlpha(-16183784, alpha), false);
      Render2D.drawRoundedOutline(context, x + 10, i + 14, short1, b0, 5.0F, 1.0F, this.withAlpha(accent, (int)(150.0F * alpha)), false);
      String s = this.clientIdInput.isEmpty() ? "Paste Client ID here..." : this.clientIdInput + "_";
      context.drawText(this.textRenderer, s, x + 10 + 6, i + 19, this.withAlpha(this.clientIdInput.isEmpty() ? 6320256 : 15659767, (int)(200.0F * alpha)), false);
      byte b1 = 80;
      byte b2 = 22;
      int j = i + 46;
      boolean flag = this.isInside(mouseX, mouseY, x + 10, j, b1, b2);
      boolean flag1 = this.isInside(mouseX, mouseY, x + 420 - 10 - b1, j, b1, b2);
      Render2D.drawRoundedRect(context, x + 10, j, b1, b2, 5.0F, this.withAlpha(accent, flag ? (int)(50.0F * alpha) : (int)(20.0F * alpha)), false);
      Render2D.drawRoundedOutline(context, x + 10, j, b1, b2, 5.0F, 1.0F, this.withAlpha(accent, (int)(150.0F * alpha)), false);
      context.drawText(this.textRenderer, "SAVE & CONNECT", x + 10 + 6, j + 7, this.withAlpha(accent, (int)(220.0F * alpha)), false);
      context.drawText(
         this.textRenderer,
         "developer.spotify.com \u2192",
         x + 420 - 10 - this.textRenderer.getWidth("developer.spotify.com \u2192"),
         j + 7,
         this.withAlpha(6320256, (int)(160.0F * alpha)),
         false
      );
   }

   public void drawFooter(DrawContext context, int x, int y, int mouseX, int mouseY, float alpha, int accent) {
      Render2D.G(context, x, y + 360 - 44, 420.0F, 44.0F, 0.0F, 0.0F, 10.0F, 10.0F, false, this.scaleAlpha(-871756264, alpha));
      context.fill(x, y + 360 - 44, x + 420, y + 360 - 44 + 1, this.scaleAlpha(553648127, alpha));
      int i = y + 360 - 44 + 9;
      this.drawButton(context, x + 10, i, 64, 26, "\u23ee PREV", mouseX, mouseY, alpha, accent);
      this.drawButton(context, x + 10 + 68, i, 80, 26, "\u23f8 PAUSE", mouseX, mouseY, alpha, accent);
      this.drawButton(context, x + 10 + 152, i, 64, 26, "NEXT \u23ed", mouseX, mouseY, alpha, accent);
      int j = x + 420 - 10 - 64;
      boolean flag = this.isInside(mouseX, mouseY, j, i, 64, 26);
      Render2D.drawRoundedRect(context, j, i, 64.0F, 26.0F, 6.0F, this.scaleAlpha(flag ? 637534207 : 285212671, alpha), false);
      Render2D.drawRoundedOutline(context, j, i, 64.0F, 26.0F, 6.0F, 1.0F, this.scaleAlpha(553648127, alpha), false);
      context.drawText(this.textRenderer, "CLOSE", j + (64 - this.textRenderer.getWidth("CLOSE")) / 2, i + 9, this.withAlpha(10531008, (int)(200.0F * alpha)), false);
   }

   public void drawButton(DrawContext context, int x, int y, int width, int height, String text, int mouseX, int mouseY, float alpha, int accent) {
      boolean flag = this.isInside(mouseX, mouseY, x, y, width, height);
      Render2D.drawRoundedRect(context, x, y, width, height, 6.0F, this.withAlpha(accent, flag ? (int)(45.0F * alpha) : (int)(18.0F * alpha)), false);
      Render2D.drawRoundedOutline(context, x, y, width, height, 6.0F, 1.0F, this.withAlpha(accent, flag ? (int)(180.0F * alpha) : (int)(60.0F * alpha)), false);
      context.drawText(
         this.textRenderer,
         text,
         x + (width - this.textRenderer.getWidth(text)) / 2,
         y + height / 2 - 4,
         this.withAlpha(flag ? accent : 10531008, (int)(220.0F * alpha)),
         false
      );
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      int i = (int)click.x();
      int j = (int)click.y();
      int k = this.getLeft();
      int l = this.getTop();
      if (this.enteringClientId) {
         int j2 = l + 180 - 40;
         int l2 = j2 + 46;
         byte b0 = 80;
         byte b1 = 22;
         if (this.isInside(i, j, k + 10, l2, b0, b1) && !this.clientIdInput.isBlank()) {
            SpotifyAuth.setClientId(this.clientIdInput);
            this.enteringClientId = false;
            this.init();
         }

         return true;
      } else if (!SpotifyAuth.ensureValidToken()) {
         int i2 = k + 420 - 10 - 80;
         int k2 = l + 22 - 9;
         if (this.isInside(i, j, i2, k2, 80, 18)) {
            if (!SpotifyAuth.hasClientId()) {
               this.enteringClientId = true;
               this.clientIdInput = "";
            } else {
               SpotifyAuth.beginAuthFlow(() -> {
                  this.status = "Connected!";
                  this.loadQueue();
               }, () -> this.status = "Auth failed.");
            }
         }

         this.handleFooterClick(i, j, k, l);
         return true;
      } else {
         int i1 = l + 44 + 4;
         short short1 = 133;

         for (int j1 = 0; j1 < 3; j1++) {
            int k1 = k + 10 + j1 * (short1 + 2);
            if (this.isInside(i, j, k1, i1, short1, 22)) {
               if (j1 == 0 && this.tab != 0) {
                  this.tab = 0;
                  this.search = "";
                  this.loadQueue();
               } else if (j1 == 1 && this.tab != 1) {
                  this.tab = 1;
                  this.search = "";
                  if (PLAYLISTS_CACHE.isEmpty()) {
                     this.loadPlaylists();
                  } else {
                     this.applyFilter();
                  }
               } else if (j1 == 2 && this.tab != 2 && selectedPlaylist != null) {
                  this.tab = 2;
                  this.search = "";
                  this.applyFilter();
               }

               return true;
            }
         }

         if (this.handleFooterClick(i, j, k, l)) {
            return true;
         } else {
            int i3 = k + 10;
            int j3 = this.getListY();
            short short2 = 400;
            if (i >= i3 && i < i3 + short2 && j >= j3 && j < j3 + this.getListHeight()) {
               int l1 = (j - j3) / 22 + this.scroll;
               if (l1 >= 0 && l1 < this.filtered.size()) {
                  Object object = this.filtered.get(l1);
                  if (object instanceof SpotifyTrack spotifyTrack) {
                     this.exec.submit(() -> {
                        boolean flag = SpotifyApi.playTrack(spotifyTrack.uri);
                        if (!flag && selectedPlaylist != null) {
                           SpotifyApi.playContext("spotify:playlist:" + selectedPlaylist, spotifyTrack.uri);
                        }

                        this.client.execute(() -> {
                           this.status = "Playing: " + spotifyTrack.name;
                           this.loadQueue();
                        });
                     });
                  } else if (object instanceof String[] astring) {
                     this.openPlaylist(astring[0], astring[1]);
                  }
               }

               return true;
            } else {
               return super.mouseClicked(click, doubled);
            }
         }
      }
   }

   public boolean handleFooterClick(int mouseX, int mouseY, int x, int y) {
      int i = y + 360 - 44 + 9;
      if (this.isInside(mouseX, mouseY, x + 420 - 10 - 64, i, 64, 26)) {
         this.client.setScreen(this.parent);
         return true;
      } else if (this.isInside(mouseX, mouseY, x + 10, i, 64, 26)) {
         this.exec.submit(SpotifyApi::skipPrevious);
         return true;
      } else if (this.isInside(mouseX, mouseY, x + 10 + 68, i, 80, 26)) {
         this.exec.submit(SpotifyApi::pause);
         return true;
      } else if (this.isInside(mouseX, mouseY, x + 10 + 152, i, 64, 26)) {
         this.exec.submit(SpotifyApi::skipNext);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean charTyped(CharInput input) {
      String s = input.asString();
      if (s != null && !s.isEmpty()) {
         if (this.enteringClientId) {
            this.clientIdInput = this.clientIdInput + s;
         } else {
            this.search = this.search + s;
            this.applyFilter();
         }

         return true;
      } else {
         return super.charTyped(input);
      }
   }

   @Override
   public boolean keyPressed(KeyInput input) {
      if (input.getKeycode() != 259) {
         if (input.isEscape()) {
            this.client.setScreen(this.parent);
            return true;
         } else {
            return super.keyPressed(input);
         }
      } else {
         if (this.enteringClientId && !this.clientIdInput.isEmpty()) {
            this.clientIdInput = this.clientIdInput.substring(0, this.clientIdInput.length() - 1);
         } else if (!this.search.isEmpty()) {
            this.search = this.search.substring(0, this.search.length() - 1);
            this.applyFilter();
         }

         return true;
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      int i = this.getLeft() + 10;
      int j = this.getListY();
      short short1 = 400;
      if (mouseX >= i && mouseX < i + short1 && mouseY >= j && mouseY < j + this.getListHeight()) {
         this.scroll = Math.max(0, Math.min(this.getMaxScroll(), this.scroll + (verticalAmount > 0.0 ? -1 : 1)));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
   }

   public int scaleAlpha(int argb, float factor) {
      return Math.max(0, Math.min(255, (int)((argb >> 24 & 0xFF) * factor))) << 24 | argb & 16777215;
   }

   public int withAlpha(int argb, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | argb & 16777215;
   }

   public float easeOutCubic(float t) {
      return 1.0F - (float)Math.pow(1.0F - Math.min(1.0F, t), 3.0);
   }

   public boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }
}
