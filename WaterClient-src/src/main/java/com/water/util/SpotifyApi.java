package com.water.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SpotifyApi {
   public SpotifyApi() {
   }

   public static SpotifyTrack getCurrentlyPlaying() {
      String s = httpGet("https://api.spotify.com/v1/me/player/currently-playing");
      if (s != null && !s.isEmpty()) {
         String s1 = extractNestedString(s, "item", "id");
         String s2 = extractNestedString(s, "item", "name");
         String s3 = parseItemArtist(s);
         String s4 = extractNestedString(s, "item", "uri");
         return s2 == null ? null : new SpotifyTrack(s1, s2, s3 != null ? s3 : "", s4 != null ? s4 : "");
      } else {
         return null;
      }
   }

   public static List<SpotifyTrack> getPlaylistTracks(String text) {
      ArrayList arrayList = new ArrayList();
      String s = "https://api.spotify.com/v1/playlists/" + text + "/tracks?limit=50&fields=items(track(id,name,artists,uri)),next";

      while (s != null) {
         String s1 = httpGet(s);
         if (s1 == null) {
            break;
         }

         arrayList.addAll(parseTracksFromItems(s1));
         String s2 = extractString(s1, "next");
         s = s2 != null && !s2.equals("null") ? s2 : null;
      }

      return arrayList;
   }

   public static List<SpotifyTrack> getQueue() {
      String s = httpGet("https://api.spotify.com/v1/me/player/queue");
      if (s == null) {
         return new ArrayList<>();
      } else {
         ArrayList arrayList = new ArrayList();
         String s1 = extractNestedString(s, "currently_playing", "id");
         String s2 = extractNestedString(s, "currently_playing", "name");
         String s3 = parseFirstArtist(extractObject(s, "currently_playing"));
         String s4 = extractNestedString(s, "currently_playing", "uri");
         if (s2 != null) {
            arrayList.add(new SpotifyTrack(s1, s2, s3 != null ? s3 : "", s4 != null ? s4 : ""));
         }

         String s5 = extractArray(s, "queue");
         if (s5 != null) {
            arrayList.addAll(parseTrackArray(s5));
         }

         return arrayList;
      }
   }

   public static List<String[]> getPlaylists() {
      String s = httpGet("https://api.spotify.com/v1/me/playlists?limit=50");
      if (s == null) {
         return new ArrayList<>();
      } else {
         ArrayList arrayList = new ArrayList();
         String s1 = extractArray(s, "items");
         if (s1 == null) {
            return arrayList;
         } else {
            for (String s2 : splitJsonArray(s1)) {
               String s3 = extractString(s2, "id");
               String s4 = extractString(s2, "name");
               if (s3 != null && s4 != null) {
                  arrayList.add(new String[]{s3, s4});
               }
            }

            return arrayList;
         }
      }
   }

   public static boolean playTrack(String text) {
      String s = "{\"uris\":[\"" + text + "\"]}";
      return httpPut("https://api.spotify.com/v1/me/player/play", s) == 204;
   }

   public static boolean playContext(String text, String text2) {
      String s = "{\"context_uri\":\"" + text + "\",\"offset\":{\"uri\":\"" + text2 + "\"}}";
      return httpPut("https://api.spotify.com/v1/me/player/play", s) == 204;
   }

   public static boolean pause() {
      return httpPost("https://api.spotify.com/v1/me/player/pause", "") == 204;
   }

   public static boolean resume() {
      return httpPut("https://api.spotify.com/v1/me/player/play", "") == 204;
   }

   public static boolean skipNext() {
      return httpPost("https://api.spotify.com/v1/me/player/next", "") == 204;
   }

   public static boolean skipPrevious() {
      return httpPost("https://api.spotify.com/v1/me/player/previous", "") == 204;
   }

   public static String httpGet(String text) {
      String s = SpotifyAuth.getAccessToken();
      if (s == null) {
         return null;
      } else {
         try {
            URL url = new URL(text);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + s);
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.setReadTimeout(8000);
            if (httpURLConnection.getResponseCode() == 401) {
               return null;
            } else if (httpURLConnection.getResponseCode() == 204) {
               return "";
            } else if (httpURLConnection.getResponseCode() != 200) {
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
         } catch (Exception exception) {
            return null;
         }
      }
   }

   public static int httpPut(String text, String text2) {
      return sendRequest(text, "PUT", text2);
   }

   public static int httpPost(String text, String text2) {
      return sendRequest(text, "POST", text2);
   }

   public static int sendRequest(String text, String text2, String text3) {
      String s = SpotifyAuth.getAccessToken();
      if (s == null) {
         return -1;
      } else {
         try {
            URL url = new URL(text);
            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setRequestMethod(text2);
            httpURLConnection.setRequestProperty("Authorization", "Bearer " + s);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.setReadTimeout(8000);
            if (!text3.isEmpty()) {
               httpURLConnection.setDoOutput(true);

               try (OutputStream outputStream = httpURLConnection.getOutputStream()) {
                  outputStream.write(text3.getBytes(StandardCharsets.UTF_8));
               }
            }

            return httpURLConnection.getResponseCode();
         } catch (Exception exception) {
            return -1;
         }
      }
   }

   public static List<SpotifyTrack> parseTracksFromItems(String text) {
      ArrayList arrayList = new ArrayList();
      String s = extractArray(text, "items");
      if (s == null) {
         return arrayList;
      } else {
         for (String s1 : splitJsonArray(s)) {
            String s2 = extractObject(s1, "track");
            if (s2 != null) {
               String s3 = extractString(s2, "id");
               String s4 = extractString(s2, "name");
               String s5 = parseFirstArtist(s2);
               String s6 = extractString(s2, "uri");
               if (s4 != null) {
                  arrayList.add(new SpotifyTrack(s3, s4, s5 != null ? s5 : "", s6 != null ? s6 : ""));
               }
            }
         }

         return arrayList;
      }
   }

   public static List<SpotifyTrack> parseTrackArray(String text) {
      ArrayList arrayList = new ArrayList();

      for (String s : splitJsonArray(text)) {
         String s1 = extractString(s, "id");
         String s2 = extractString(s, "name");
         String s3 = parseFirstArtist(s);
         String s4 = extractString(s, "uri");
         if (s2 != null) {
            arrayList.add(new SpotifyTrack(s1, s2, s3 != null ? s3 : "", s4 != null ? s4 : ""));
         }
      }

      return arrayList;
   }

   public static String parseItemArtist(String text) {
      String s = extractObject(text, "item");
      return s != null ? parseFirstArtist(s) : null;
   }

   public static String parseFirstArtist(String text) {
      if (text == null) {
         return null;
      } else {
         String s = extractArray(text, "artists");
         if (s == null) {
            return null;
         } else {
            List list = splitJsonArray(s);
            return list.isEmpty() ? null : extractString((String)list.get(0), "name");
         }
      }
   }

   public static String extractString(String text, String text2) {
      if (text == null) {
         return null;
      } else {
         String s = "\"" + text2 + "\":";
         int i = text.indexOf(s);
         if (i < 0) {
            return null;
         } else {
            i += s.length();

            while (i < text.length() && text.charAt(i) == ' ') {
               i++;
            }

            if (i >= text.length()) {
               return null;
            } else if (text.charAt(i) == '"') {
               int k;
               for (k = ++i; k < text.length() && text.charAt(k) != '"'; k++) {
                  if (text.charAt(k) == '\\') {
                     k++;
                  }
               }

               return text.substring(i, Math.min(k, text.length()));
            } else if (text.charAt(i) == 'n') {
               return null;
            } else {
               int j = i;

               while (j < text.length() && ",}]".indexOf(text.charAt(j)) < 0) {
                  j++;
               }

               return text.substring(i, j).trim();
            }
         }
      }
   }

   public static String extractNestedString(String text, String text2, String text3) {
      String s = extractObject(text, text2);
      return s != null ? extractString(s, text3) : null;
   }

   public static String extractObject(String text, String text2) {
      if (text == null) {
         return null;
      } else {
         String s = "\"" + text2 + "\":";
         int i = text.indexOf(s);
         if (i < 0) {
            return null;
         } else {
            i += s.length();

            while (i < text.length() && text.charAt(i) == ' ') {
               i++;
            }

            return i < text.length() && text.charAt(i) == '{' ? extractBalanced(text, i, '{', '}') : null;
         }
      }
   }

   public static String extractArray(String text, String text2) {
      if (text == null) {
         return null;
      } else {
         String s = "\"" + text2 + "\":";
         int i = text.indexOf(s);
         if (i < 0) {
            return null;
         } else {
            i += s.length();

            while (i < text.length() && text.charAt(i) == ' ') {
               i++;
            }

            if (i < text.length() && text.charAt(i) == '[') {
               String s1 = extractBalanced(text, i, '[', ']');
               return s1 != null ? s1.substring(1, s1.length() - 1) : null;
            } else {
               return null;
            }
         }
      }
   }

   public static String extractBalanced(String text, int text2, char var2, char var3) {
      int i = 0;
      int j = text2;

      for (boolean flag = false; j < text.length(); j++) {
         char c0 = text.charAt(j);
         if (c0 == '"' && (j == 0 || text.charAt(j - 1) != '\\')) {
            flag = !flag;
         }

         if (!flag) {
            if (c0 == var2) {
               i++;
            }

            if (c0 == var3) {
               if (--i == 0) {
                  return text.substring(text2, j + 1);
               }
            }
         }
      }

      return null;
   }

   public static List<String> splitJsonArray(String text) {
      ArrayList arrayList = new ArrayList();
      if (text != null && !text.isBlank()) {
         int i = 0;
         int j = -1;
         boolean flag = false;

         for (int k = 0; k < text.length(); k++) {
            char c0 = text.charAt(k);
            if (c0 == '"' && (k == 0 || text.charAt(k - 1) != '\\')) {
               flag = !flag;
            }

            if (!flag) {
               if (c0 == '{') {
                  if (i++ == 0) {
                     j = k;
                  }
               } else if (c0 == '}') {
                  if (--i == 0 && j >= 0) {
                     arrayList.add(text.substring(j, k + 1));
                     j = -1;
                  }
               }
            }
         }

         return arrayList;
      } else {
         return arrayList;
      }
   }
}
