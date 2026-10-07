package com.water.util;

import com.water.module.modules.misc.NameProtect;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

public final class ChatUtils {
   public ChatUtils() {
   }

   public static String replaceOwnName(String text) {
      if (text != null && isNameProtectActive()) {
         String s = getRealName();
         String s1 = getFakeName();
         if (s != null && s1 != null && !s.isBlank() && !s.equals(s1)) {
            return text.contains(s) ? text.replace(s, s1) : text;
         } else {
            return text;
         }
      } else {
         return text;
      }
   }

   public static StringVisitable maskVisitable(StringVisitable text) {
      if (text != null && isNameProtectActive()) {
         List list = replaceNameInSegments(toSegments(text));
         if (list == null) {
            return text;
         } else if (list.isEmpty()) {
            return StringVisitable.EMPTY;
         } else {
            ArrayList arrayList = new ArrayList(list.size());

            for (TextSegment textSegment : (Iterable<TextSegment>)list) {
               arrayList.add(StringVisitable.styled(textSegment.text(), textSegment.style()));
            }

            return StringVisitable.concat(arrayList);
         }
      } else {
         return text;
      }
   }

   public static OrderedText maskOrderedText(OrderedText text) {
      if (text != null && isNameProtectActive()) {
         List list = replaceNameInSegments(orderedToSegments(text));
         if (list == null) {
            return text;
         } else if (list.isEmpty()) {
            return OrderedText.empty();
         } else {
            ArrayList arrayList = new ArrayList(list.size());

            for (TextSegment textSegment : (Iterable<TextSegment>)list) {
               arrayList.add(OrderedText.styledForwardsVisitedString(textSegment.text(), textSegment.style()));
            }

            return OrderedText.concat(arrayList);
         }
      } else {
         return text;
      }
   }

   public static boolean isNameProtectActive() {
      return NameProtect.instance != null && NameProtect.instance.isEnabled() && getRealName() != null && !getRealName().isBlank();
   }

   public static String getRealName() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient != null && minecraftClient.getSession() != null ? minecraftClient.getSession().getUsername() : null;
   }

   public static String getFakeName() {
      return NameProtect.instance == null ? null : NameProtect.instance.getFakeName();
   }

   public static List<TextSegment> toSegments(StringVisitable text) {
      ArrayList arrayList = new ArrayList();
      text.visit((var1x, var2) -> {
         appendCodePoints(arrayList, var2, var1x);
         return Optional.empty();
      }, Style.EMPTY);
      return arrayList;
   }

   public static List<TextSegment> orderedToSegments(OrderedText text) {
      ArrayList arrayList = new ArrayList();
      text.accept((var1x, var2, var3) -> {
         arrayList.add(new TextSegment(new String(Character.toChars(var3)), var2));
         return true;
      });
      return arrayList;
   }

   public static void appendCodePoints(List<TextSegment> list, String text, Style style) {
      if (text != null && !text.isEmpty()) {
         int i = 0;

         while (i < text.length()) {
            int j = text.codePointAt(i);
            list.add(new TextSegment(new String(Character.toChars(j)), style));
            i += Character.charCount(j);
         }
      }
   }

   public static List<TextSegment> replaceNameInSegments(List<TextSegment> list) {
      String s = getRealName();
      String s1 = getFakeName();
      if (s != null && s1 != null && !s.isBlank() && !s.equals(s1)) {
         StringBuilder stringBuilder = new StringBuilder();
         ArrayList<Integer> arrayList = new ArrayList<>(list.size());

         for (TextSegment textSegment : list) {
            arrayList.add(stringBuilder.length());
            stringBuilder.append(textSegment.text());
         }

         String s2 = stringBuilder.toString();
         if (!s2.contains(s)) {
            return null;
         } else {
            ArrayList arrayList1 = new ArrayList();
            int i = 0;
            int j = 0;

            int k;
            while ((k = s2.indexOf(s, j)) >= 0) {
               while (i < list.size() && arrayList.get(i) < k) {
                  arrayList1.add((TextSegment)list.get(i++));
               }

               Style style = i < list.size() ? ((TextSegment)list.get(i)).style() : Style.EMPTY;
               arrayList1.add(new TextSegment(s1, style));
               int l = k + s.length();

               while (i < list.size() && arrayList.get(i) < l) {
                  i++;
               }

               j = l;
            }

            while (i < list.size()) {
               arrayList1.add((TextSegment)list.get(i++));
            }

            return mergeAdjacentSegments(arrayList1);
         }
      } else {
         return null;
      }
   }

   public static List<TextSegment> mergeAdjacentSegments(List<TextSegment> list) {
      if (list.isEmpty()) {
         return list;
      } else {
         ArrayList<TextSegment> arrayList = new ArrayList<>(list.size());
         TextSegment textSegment = (TextSegment)list.getFirst();

         for (int i = 1; i < list.size(); i++) {
            TextSegment textSegment1 = (TextSegment)list.get(i);
            if (Objects.equals(textSegment.style(), textSegment1.style())) {
               textSegment = new TextSegment(textSegment.text() + textSegment1.text(), textSegment.style());
            } else {
               arrayList.add(textSegment);
               textSegment = textSegment1;
            }
         }

         arrayList.add(textSegment);
         return arrayList;
      }
   }
}
