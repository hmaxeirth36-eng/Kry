package com.water.module.setting;

import java.util.List;

public final class ModeSetting extends Setting<String> {
   public final List<String> modes;
   public final List<String> legacyNames;

   public ModeSetting(String text, String text2, String... text3) {
      this(text, text2, new String[0], text3);
   }

   public ModeSetting(String text, String text2, String[] text3, String... text4) {
      super(text, text2);
      if (text4 != null && text4.length != 0) {
         this.modes = List.of(text4);
         this.legacyNames = text3 == null ? List.of() : List.of(text3);
         this.setValue(text2);
      } else {
         throw new IllegalArgumentException("ModeSetting requires at least one mode");
      }
   }

   public List<String> getModes() {
      return this.modes;
   }

   public void cycleNext() {
      this.setValue(this.cycle(1));
   }

   public void cyclePrevious() {
      this.setValue(this.cycle(-1));
   }

   public boolean is(String text) {
      return this.trimOrEmpty(text).equalsIgnoreCase(this.getValue());
   }

   public void setValue(String text) {
      super.setValue(this.normalizeToKnownMode(text));
   }

   @Override
   public boolean matchesName(String text) {
      if (super.matchesName(text)) {
         return true;
      } else {
         String s = this.trimOrEmpty(text);

         for (String s1 : this.legacyNames) {
            if (s1.equalsIgnoreCase(s)) {
               return true;
            }
         }

         return false;
      }
   }

   public String cycle(int var1) {
      int i = this.modes.size();
      if (i == 0) {
         return "";
      } else {
         String s = this.getValue();

         for (int j = 0; j < i; j++) {
            if (this.modes.get(j).equalsIgnoreCase(s)) {
               int k = Math.floorMod(j + var1, i);
               return this.modes.get(k);
            }
         }

         return this.modes.getFirst();
      }
   }

   public String normalizeToKnownMode(String text) {
      String s = this.trimOrEmpty(text);

      for (String s1 : this.modes) {
         if (s1.equalsIgnoreCase(s)) {
            return s1;
         }
      }

      return this.modes.getFirst();
   }

   public String trimOrEmpty(String text) {
      return text == null ? "" : text.trim();
   }
}
