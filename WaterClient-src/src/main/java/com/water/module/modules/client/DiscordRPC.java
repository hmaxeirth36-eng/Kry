package com.water.module.modules.client;

import com.water.module.Category;
import com.water.module.Module;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;

public final class DiscordRPC extends Module {
   public static final String APP_ID = "1529221242077450381";
   public static DiscordRPC INSTANCE;
   public ScheduledExecutorService scheduler;
   public volatile boolean connected = false;
   public volatile long startEpoch = 0L;
   public Socket socket;
   public OutputStream out;
   public InputStream in;
   public static final int OP_HANDSHAKE = 0;
   public static final int OP_FRAME = 1;
   public static final int OP_CLOSE = 2;

   public DiscordRPC() {
      super("DiscordRPC", Category.CLIENT);
      INSTANCE = this;
   }

   public static boolean isActive() {
      return INSTANCE != null && INSTANCE.isEnabled();
   }

   @Override
   public void onEnable() {
      this.startEpoch = System.currentTimeMillis() / 1000L;
      this.scheduler = Executors.newSingleThreadScheduledExecutor(var0 -> {
         Thread thread = new Thread(var0, "water-drpc");
         thread.setDaemon(true);
         return thread;
      });
      this.scheduler.submit(this::connectToDiscord);
      this.scheduler.scheduleAtFixedRate(this::updatePresence, 5L, 15L, TimeUnit.SECONDS);
   }

   @Override
   public void onDisable() {
      if (this.scheduler != null) {
         this.scheduler.shutdownNow();
      }

      this.clearPresence();
      this.closeConnection();
   }

   public void connectToDiscord() {
      for (int i = 0; i <= 9; i++) {
         try {
            String s = this.getIpcPath(i);
            if (s == null) {
               break;
            }

            this.openIpcPipe(s);
            if (this.connected) {
               this.sendHandshake();
               this.updatePresence();
               return;
            }
         } catch (Exception exception) {
         }
      }
   }

   public String getIpcPath(int var1) {
      String s = System.getProperty("os.name", "").toLowerCase();
      if (s.contains("win")) {
         return "\\\\.\\pipe\\discord-ipc-" + var1;
      } else {
         String[] astring = new String[]{System.getenv("XDG_RUNTIME_DIR"), System.getenv("TMPDIR"), System.getenv("TMP"), System.getenv("TEMP"), "/tmp"};

         for (String s1 : astring) {
            if (s1 != null) {
               File file1 = new File(s1, "discord-ipc-" + var1);
               if (file1.exists()) {
                  return file1.getAbsolutePath();
               }
            }
         }

         return null;
      }
   }

   public void openIpcPipe(String text) {
      try {
         String s = System.getProperty("os.name", "").toLowerCase();
         if (s.contains("win")) {
            this.openWindowsPipe(text);
         } else {
            this.openUnixSocket(text);
         }
      } catch (Exception exception) {
         this.connected = false;
      }
   }

   public void openWindowsPipe(String text) throws Exception {
      final RandomAccessFile randomAccessFile = new RandomAccessFile(text, "rw");
      this.out = new FileOutputStream(randomAccessFile.getFD());
      this.in = new InputStream() {
         @Override
         public int read() throws IOException {
            return randomAccessFile.read();
         }

         @Override
         public int read(byte[] text, int var2, int var3) throws IOException {
            return randomAccessFile.read(text, var2, var3);
         }
      };
      this.connected = true;
   }

   public void openUnixSocket(String text) throws Exception {
      try {
         Class oclass = Class.forName("java.net.UnixDomainSocketAddress");
         Object object = oclass.getMethod("of", String.class).invoke(null, text);
         Class oclass1 = Class.forName("java.nio.channels.SocketChannel");
         Object object1 = oclass1.getMethod("open", Class.forName("java.net.ProtocolFamily"))
            .invoke(null, Enum.valueOf((Class)Class.forName("java.net.StandardProtocolFamily"), "UNIX"));
         oclass1.getMethod("connect", Class.forName("java.net.SocketAddress")).invoke(object1, object);
         this.socket = (Socket)oclass1.getMethod("socket").invoke(object1);
         this.out = this.socket.getOutputStream();
         this.in = this.socket.getInputStream();
         this.connected = true;
      } catch (Exception exception) {
         this.connected = false;
      }
   }

   public void closeConnection() {
      this.connected = false;

      try {
         if (this.out != null) {
            this.out.close();
         }
      } catch (Exception exception2) {
      }

      try {
         if (this.in != null) {
            this.in.close();
         }
      } catch (Exception exception1) {
      }

      try {
         if (this.socket != null) {
            this.socket.close();
         }
      } catch (Exception exception) {
      }

      this.out = null;
      this.in = null;
      this.socket = null;
   }

   public void sendHandshake() throws Exception {
      String s = "{\"v\":1,\"client_id\":\"1529221242077450381\"}";
      this.sendFrame(0, s);
      this.readFrame();
   }

   public void sendFrame(int var1, String text) throws Exception {
      byte[] abyte = text.getBytes(StandardCharsets.UTF_8);
      byte[] abyte1 = new byte[8];
      abyte1[0] = (byte)(var1 & 0xFF);
      abyte1[1] = (byte)(var1 >> 8 & 0xFF);
      abyte1[2] = (byte)(var1 >> 16 & 0xFF);
      abyte1[3] = (byte)(var1 >> 24 & 0xFF);
      int i = abyte.length;
      abyte1[4] = (byte)(i & 0xFF);
      abyte1[5] = (byte)(i >> 8 & 0xFF);
      abyte1[6] = (byte)(i >> 16 & 0xFF);
      abyte1[7] = (byte)(i >> 24 & 0xFF);
      this.out.write(abyte1);
      this.out.write(abyte);
      this.out.flush();
   }

   public String readFrame() throws Exception {
      byte[] abyte = new byte[8];
      int i = 0;

      while (i < 8) {
         int j = this.in.read(abyte, i, 8 - i);
         if (j < 0) {
            throw new EOFException();
         }

         i += j;
      }

      int l = abyte[4] & 255 | (abyte[5] & 255) << 8 | (abyte[6] & 255) << 16 | (abyte[7] & 255) << 24;
      byte[] abyte1 = new byte[l];
      i = 0;

      while (i < l) {
         int k = this.in.read(abyte1, i, l - i);
         if (k < 0) {
            throw new EOFException();
         }

         i += k;
      }

      return new String(abyte1, StandardCharsets.UTF_8);
   }

   public void updatePresence() {
      if (!this.connected) {
         this.connectToDiscord();
      } else {
         try {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            String s = "Playing on Water Client";
            String s1 = minecraftClient != null && minecraftClient.getCurrentServerEntry() != null
               ? minecraftClient.getCurrentServerEntry().address
               : "Singleplayer";
            String s2 = String.valueOf(System.currentTimeMillis());
            String s3 = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":"
               + ProcessHandle.current().pid()
               + ",\"activity\":{\"details\":\""
               + escapeJson(s)
               + "\",\"state\":\""
               + escapeJson(s1)
               + "\",\"timestamps\":{\"start\":"
               + this.startEpoch
               + "},\"assets\":{\"large_image\":\"content\",\"large_text\":\"Water Client\",\"small_image\":\"minecraft\",\"small_text\":\"Minecraft\"}}},\"nonce\":\""
               + s2
               + "\"}";
            this.sendFrame(1, s3);
            this.readFrame();
         } catch (Exception exception) {
            this.closeConnection();
         }
      }
   }

   public void clearPresence() {
      if (this.connected) {
         try {
            String s = String.valueOf(System.currentTimeMillis());
            String s1 = "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":" + ProcessHandle.current().pid() + ",\"activity\":null},\"nonce\":\"" + s + "\"}";
            this.sendFrame(1, s1);
            this.readFrame();
         } catch (Exception exception) {
         }
      }
   }

   public static String escapeJson(String text) {
      return text == null ? "" : text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
   }

   public static String hH() {
      return "T";
   }
}
