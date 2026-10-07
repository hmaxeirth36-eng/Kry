package com.water.module.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.item.ItemStack;

public final class MultiSelectSetting extends Setting<Set<String>> {
   public final List<DropdownOption> options;

   public MultiSelectSetting(String text, DropdownOption... dropdownOption) {
      super(text, new LinkedHashSet<>());
      this.options = List.of(dropdownOption);
   }

   public void setValue(Set<String> set) {
      LinkedHashSet linkedHashSet = new LinkedHashSet();
      if (set != null) {
         for (String s : set) {
            if (s != null) {
               for (DropdownOption dropdownoption : this.options) {
                  if (dropdownoption.value().equalsIgnoreCase(s)) {
                     linkedHashSet.add(dropdownoption.value());
                     break;
                  }
               }
            }
         }
      }

      super.setValue(linkedHashSet);
   }

   public List<DropdownOption> getOptions() {
      return this.options;
   }

   public List<DropdownOption> filter(String text) {
      String s = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
      if (s.isEmpty()) {
         return this.options;
      } else {
         ArrayList arrayList = new ArrayList();

         for (DropdownOption dropdownoption : this.options) {
            String s1 = dropdownoption.value() == null ? "" : dropdownoption.value().toLowerCase(Locale.ROOT);
            String s2 = dropdownoption.label() == null ? "" : dropdownoption.label().toLowerCase(Locale.ROOT);
            if (s1.contains(s) || s2.contains(s)) {
               arrayList.add(dropdownoption);
            }
         }

         return arrayList;
      }
   }

   public void toggle(String text) {
      if (text != null) {
         LinkedHashSet linkedHashSet = new LinkedHashSet<>(this.getValue());
         String s = null;

         for (DropdownOption dropdownoption : this.options) {
            if (dropdownoption.value().equalsIgnoreCase(text)) {
               s = dropdownoption.value();
               break;
            }
         }

         if (s != null) {
            if (!linkedHashSet.add(s)) {
               linkedHashSet.remove(s);
            }

            this.setValue(linkedHashSet);
         }
      }
   }

   public void clear() {
      if (!this.getValue().isEmpty()) {
         this.setValue(Collections.emptySet());
      }
   }

   public boolean contains(String text) {
      if (text == null) {
         return false;
      } else {
         for (String s : this.getValue()) {
            if (s.equalsIgnoreCase(text)) {
               return true;
            }
         }

         return false;
      }
   }

   public boolean isSelected(DropdownOption dropdownOption) {
      return dropdownOption != null && this.contains(dropdownOption.value());
   }

   public int size() {
      return this.getValue().size();
   }

   public List<DropdownOption> getSelectedOptions() {
      ArrayList arrayList = new ArrayList();

      for (DropdownOption dropdownoption : this.options) {
         if (this.contains(dropdownoption.value())) {
            arrayList.add(dropdownoption);
         }
      }

      return arrayList;
   }

   public String getSummary() {
      List list = this.getSelectedOptions();
      if (list.isEmpty()) {
         return "Choose";
      } else {
         DropdownOption dropdownoption = (DropdownOption)list.getFirst();
         int i = list.size() - 1;
         return i > 0 ? dropdownoption.label() + " +" + i : dropdownoption.label();
      }
   }

   public ItemStack getPreviewStack() {
      List list = this.getSelectedOptions();
      return list.isEmpty() ? ItemStack.EMPTY : ((DropdownOption)list.getFirst()).getPreviewStack();
   }
}
