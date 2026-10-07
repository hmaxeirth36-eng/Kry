package com.water.util;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SpotifyAuth {
   public static String CLIENT_ID = "";
   public static final String REDIRECT_URI = "http://localhost:8888/callback";
   public static final String SCOPES = "user-read-playback-state user-modify-playback-state user-read-currently-playing playlist-read-private playlist-read-collaborative";
   public static final Path TOKEN_FILE = Paths.get(System.getProperty("user.home"), ".water_spotify_token");
   public static volatile String accessToken = null;
   public static volatile String refreshToken = null;
   public static volatile long expiresAt = 0L;
   public static volatile boolean authInProgress = false;
   public static final ExecutorService exec = Executors.newSingleThreadExecutor(var0 -> {
      Thread thread = new Thread(var0, "spotify-auth");
      thread.setDaemon(true);
      return thread;
   });

   public SpotifyAuth() {
   }

   public static void setClientId(String text) {
      if (text != null && !text.isBlank() && !text.equals(CLIENT_ID)) {
         CLIENT_ID = text.trim();

         try {
            Files.deleteIfExists(TOKEN_FILE);
         } catch (Exception exception) {
         }

         accessToken = null;
         refreshToken = null;
         expiresAt = 0L;
      }
   }

   public static String getClientId() {
      return CLIENT_ID;
   }

   public static boolean hasClientId() {
      return !CLIENT_ID.isBlank();
   }

   public static boolean ensureValidToken() {
      if (accessToken != null && System.currentTimeMillis() < expiresAt - 30000L) {
         return true;
      } else {
         loadTokens();
         return accessToken != null && System.currentTimeMillis() < expiresAt - 30000L;
      }
   }

   public static String getAccessToken() {
      if (!ensureValidToken()) {
         return null;
      } else {
         if (System.currentTimeMillis() > expiresAt - 60000L) {
            exec.submit(SpotifyAuth::refreshAccessToken);
         }

         return accessToken;
      }
   }

   public static void beginAuthFlow(Runnable task, Runnable task2) {
      if (hasClientId() && !authInProgress) {
         authInProgress = true;
         exec.submit(
            () -> {
               try {
                  byte[] abyte = new byte[64];
                  new SecureRandom().nextBytes(abyte);
                  String s = Base64.getUrlEncoder().withoutPadding().encodeToString(abyte);
                  MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                  byte[] abyte1 = messageDigest.digest(s.getBytes(StandardCharsets.US_ASCII));
                  String s1 = Base64.getUrlEncoder().withoutPadding().encodeToString(abyte1);
                  byte[] abyte2 = new byte[16];
                  new SecureRandom().nextBytes(abyte2);
                  String s2 = Base64.getUrlEncoder().withoutPadding().encodeToString(abyte2);
                  String s3 = "https://accounts.spotify.com/authorize?client_id="
                     + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                     + "&response_type=code&redirect_uri="
                     + URLEncoder.encode("http://localhost:8888/callback", StandardCharsets.UTF_8)
                     + "&scope="
                     + URLEncoder.encode(
                        "user-read-playback-state user-modify-playback-state user-read-currently-playing playlist-read-private playlist-read-collaborative",
                        StandardCharsets.UTF_8
                     )
                     + "&state="
                     + s2
                     + "&code_challenge_method=S256&code_challenge="
                     + s1;
                  Desktop.getDesktop().browse(new URI(s3));
                  String s4 = waitForCallbackCode(s2);
                  if (s4 == null) {
                     if (task2 != null) {
                        task2.run();
                     }

                     return;
                  }

                  String s5 = exchangeCodeForToken(s4, s);
                  if (s5 != null) {
                     parseTokenResponse(s5);
                     if (task != null) {
                        task.run();
                     }

                     return;
                  }

                  if (task2 != null) {
                     task2.run();
                  }
               } catch (Exception exception) {
                  if (task2 != null) {
                     task2.run();
                  }

                  return;
               } finally {
                  authInProgress = false;
               }
            }
         );
      }
   }

   public static String waitForCallbackCode(String text) throws Exception {
      String s7;
      try (ServerSocket serverSocket = new ServerSocket(8888)) {
         serverSocket.setSoTimeout(120000);

         try (Socket socket = serverSocket.accept()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String s = bufferedReader.readLine();
            if (s == null) {
               return null;
            }

            String s1 = s.split(" ")[1];
            String s2 = s1.contains("?") ? s1.split("\\?", 2)[1] : "";
            String s3 = null;
            String s4 = null;

            for (String s5 : s2.split("&")) {
               String[] astring = s5.split("=", 2);
               if (astring.length == 2) {
                  if (astring[0].equals("code")) {
                     s3 = URLDecoder.decode(astring[1], StandardCharsets.UTF_8);
                  }

                  if (astring[0].equals("state")) {
                     s4 = URLDecoder.decode(astring[1], StandardCharsets.UTF_8);
                  }
               }
            }

            String s6 = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<html><body style='background:#0a0e18;color:#5be6d0;font-family:sans-serif;text-align:center;padding:50px'><h2>\u2713 Water Client connected to Spotify</h2><p>You can close this tab and return to Minecraft.</p></body></html>";
            socket.getOutputStream().write(s6.getBytes(StandardCharsets.UTF_8));
            if (!text.equals(s4)) {
               return null;
            }

            s7 = s3;
         }
      }

      return s7;
   }

   public static String exchangeCodeForToken(String text, String text2) throws Exception {
      String s = "grant_type=authorization_code&code="
         + URLEncoder.encode(text, StandardCharsets.UTF_8)
         + "&redirect_uri="
         + URLEncoder.encode("http://localhost:8888/callback", StandardCharsets.UTF_8)
         + "&client_id="
         + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
         + "&code_verifier="
         + URLEncoder.encode(text2, StandardCharsets.UTF_8);
      URL url = new URL("https://accounts.spotify.com/api/token");
      HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
      httpURLConnection.setRequestMethod("POST");
      httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      httpURLConnection.setDoOutput(true);
      httpURLConnection.setConnectTimeout(10000);
      httpURLConnection.setReadTimeout(10000);

      try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
         outputStream.write(s.getBytes(StandardCharsets.UTF_8));
      }

      if (httpURLConnection.getResponseCode() != 200) {
         return null;
      } else {
         String s2;
         try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();

            String s1;
            while ((s1 = bufferedReader.readLine()) != null) {
               stringBuilder.append(s1);
            }

            s2 = stringBuilder.toString();
         }

         return s2;
      }
   }

   public static void refreshAccessToken() {
      if (refreshToken != null) {
         try {
            String s = "grant_type=refresh_token&refresh_token="
               + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
               + "&client_id="
               + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8);
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setReadTimeout(10000);

            try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
               outputStream.write(s.getBytes(StandardCharsets.UTF_8));
            }

            if (httpURLConnection.getResponseCode() != 200) {
               return;
            }

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))) {
               StringBuilder stringBuilder = new StringBuilder();

               String s1;
               while ((s1 = bufferedReader.readLine()) != null) {
                  stringBuilder.append(s1);
               }

               parseTokenResponse(stringBuilder.toString());
            }
         } catch (Exception exception) {
         }
      }
   }

   public static void parseTokenResponse(String text) {
      accessToken = extractJsonString(text, "access_token");
      String s = extractJsonString(text, "refresh_token");
      if (s != null) {
         refreshToken = s;
      }

      String s1 = extractJsonString(text, "expires_in");
      long i = s1 != null ? Long.parseLong(s1) : 3600L;
      expiresAt = System.currentTimeMillis() + i * 1000L;
      saveTokens();
   }

   public static void saveTokens() {
      try {
         String s = accessToken + "\n" + refreshToken + "\n" + expiresAt;
         Files.writeString(TOKEN_FILE, s, StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public static void loadTokens() {
      try {
         if (!Files.exists(TOKEN_FILE)) {
            return;
         }

         String[] astring = Files.readString(TOKEN_FILE, StandardCharsets.UTF_8).split("\n");
         if (astring.length >= 3) {
            accessToken = astring[0].trim();
            refreshToken = astring[1].trim();
            expiresAt = Long.parseLong(astring[2].trim());
            if (System.currentTimeMillis() > expiresAt - 30000L && refreshToken != null) {
               refreshAccessToken();
            }
         }
      } catch (Exception exception) {
      }
   }

   public static String extractJsonString(String text, String text2) {
      String s = "\"" + text2 + "\":";
      int i = text.indexOf(s);
      if (i < 0) {
         return null;
      } else {
         i += s.length();

         while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '"')) {
            i++;
         }

         int j = i;

         while (j < text.length() && text.charAt(j) != '"' && text.charAt(j) != ',' && text.charAt(j) != '}') {
            j++;
         }

         return text.substring(i, j).trim();
      }
   }

   public static boolean isAuthInProgress() {
      return authInProgress;
   }
}
