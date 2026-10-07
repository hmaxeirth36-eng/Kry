package com.water.util;

import com.water.module.modules.donut.FakeRoles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;

public final class RoleTextUtil {
   public RoleTextUtil() {
   }

   public static String applyRolePrefix(String text) {
      if (text != null && FakeRoles.isActive()) {
         String s = getSelfUsername();
         if (s == null || s.isBlank()) {
            return text;
         } else if (!text.contains(s)) {
            return text;
         } else {
            String s1 = FakeRoles.getDisplayName();
            return s1 != null && !s1.equals(s) ? text.replace(s, s1) : text;
         }
      } else {
         return text;
      }
   }

   public static OrderedText applyToOrderedText(OrderedText text) {
      if (text != null && FakeRoles.isActive()) {
         List list = insertRolePrefix(orderedToSegments(text));
         if (list == null) {
            return text;
         } else if (list.isEmpty()) {
            return OrderedText.empty();
         } else {
            ArrayList arrayList = new ArrayList(list.size());

            for (ChatSegment chatSegment : (Iterable<ChatSegment>)list) {
               arrayList.add(OrderedText.styledForwardsVisitedString(chatSegment.text(), chatSegment.style()));
            }

            return OrderedText.concat(arrayList);
         }
      } else {
         return text;
      }
   }

   public static StringVisitable applyToVisitable(StringVisitable text) {
      if (text != null && FakeRoles.isActive()) {
         List list = insertRolePrefix(toSegments(text));
         if (list == null) {
            return text;
         } else if (list.isEmpty()) {
            return StringVisitable.EMPTY;
         } else {
            ArrayList arrayList = new ArrayList(list.size());

            for (ChatSegment chatSegment : (Iterable<ChatSegment>)list) {
               arrayList.add(StringVisitable.styled(chatSegment.text(), chatSegment.style()));
            }

            return StringVisitable.concat(arrayList);
         }
      } else {
         return text;
      }
   }

   public static String getSelfUsername() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      return minecraftClient != null && minecraftClient.getSession() != null ? minecraftClient.getSession().getUsername() : null;
   }

   public static List<ChatSegment> orderedToSegments(OrderedText text) {
      ArrayList arrayList = new ArrayList();
      text.accept((var1x, var2, var3) -> {
         arrayList.add(new ChatSegment(new String(Character.toChars(var3)), var2));
         return true;
      });
      return arrayList;
   }

   public static List<ChatSegment> toSegments(StringVisitable text) {
      ArrayList arrayList = new ArrayList();
      text.visit((var1x, var2) -> {
         int i = 0;

         while (i < var2.length()) {
            int j = var2.codePointAt(i);
            arrayList.add(new ChatSegment(new String(Character.toChars(j)), var1x));
            i += Character.charCount(j);
         }

         return Optional.empty();
      }, Style.EMPTY);
      return arrayList;
   }

   public static List<ChatSegment> insertRolePrefix(List<ChatSegment> list) {
      String s = getSelfUsername();
      if (s != null && !s.isBlank()) {
         String s1 = FakeRoles.getRolePrefix();
         if (s1 == null) {
            return null;
         } else {
            StringBuilder stringBuilder = new StringBuilder();
            ArrayList<Integer> arrayList = new ArrayList<>(list.size());

            for (ChatSegment chatSegment : list) {
               arrayList.add(stringBuilder.length());
               stringBuilder.append(chatSegment.text());
            }

            String s2 = stringBuilder.toString();
            if (!s2.contains(s)) {
               return null;
            } else {
               Style style = FakeRoles.getNameStyle();
               ArrayList arrayList1 = new ArrayList();
               int i = 0;
               int j = 0;

               int k;
               while ((k = s2.indexOf(s, j)) >= 0) {
                  while (i < list.size() && arrayList.get(i) < k) {
                     arrayList1.add((ChatSegment)list.get(i++));
                  }

                  int l = 0;

                  while (l < s1.length()) {
                     int i1 = s1.codePointAt(l);
                     arrayList1.add(new ChatSegment(new String(Character.toChars(i1)), FakeRoles.getStyleForChar(i1)));
                     l += Character.charCount(i1);
                  }

                  l = 0;

                  while (l < s.length()) {
                     int j1 = s.codePointAt(l);
                     arrayList1.add(new ChatSegment(new String(Character.toChars(j1)), style));
                     l += Character.charCount(j1);
                  }

                  l = k + s.length();

                  while (i < list.size() && arrayList.get(i) < l) {
                     i++;
                  }

                  j = l;
               }

               while (i < list.size()) {
                  arrayList1.add((ChatSegment)list.get(i++));
               }

               return mergeAdjacentSegments(arrayList1);
            }
         }
      } else {
         return null;
      }
   }

   public static List<ChatSegment> mergeAdjacentSegments(List<ChatSegment> list) {
      if (list.isEmpty()) {
         return list;
      } else {
         ArrayList<ChatSegment> arrayList = new ArrayList<>(list.size());
         ChatSegment chatSegment = (ChatSegment)list.getFirst();

         for (int i = 1; i < list.size(); i++) {
            ChatSegment chatSegment1 = (ChatSegment)list.get(i);
            if (Objects.equals(chatSegment.style(), chatSegment1.style())) {
               chatSegment = new ChatSegment(chatSegment.text() + chatSegment1.text(), chatSegment.style());
            } else {
               arrayList.add(chatSegment);
               chatSegment = chatSegment1;
            }
         }

         arrayList.add(chatSegment);
         return arrayList;
      }
   }
}
