package com.water.module.setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.item.ItemStack;

public final class DropdownSetting extends Setting<String> {
   public final String defaultValue;
   public final List<DropdownOption> options;

   public DropdownSetting(String text, String text2, DropdownOption... dropdownOption) {
      super(text, text2);
      this.defaultValue = text2;
      this.options = List.of(dropdownOption);
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

   public void select(String text) {
      if (text == null) {
         this.reset();
      } else {
         for (DropdownOption dropdownoption : this.options) {
            if (dropdownoption.value().equalsIgnoreCase(text)) {
               this.setValue(dropdownoption.value());
               return;
            }
         }
      }
   }

   public void reset() {
      this.setValue(this.defaultValue);
   }

   public boolean isSelected(DropdownOption dropdownOption) {
      return dropdownOption != null && this.getValue() != null && dropdownOption.value().equalsIgnoreCase(this.getValue());
   }

   public DropdownOption getSelectedOption() {
      for (DropdownOption dropdownoption : this.options) {
         if (this.isSelected(dropdownoption)) {
            return dropdownoption;
         }
      }

      return this.options.isEmpty() ? null : this.options.getFirst();
   }

   public String getSummary() {
      DropdownOption dropdownoption = this.getSelectedOption();
      return dropdownoption == null ? "Choose" : dropdownoption.label();
   }

   public ItemStack getPreviewStack() {
      DropdownOption dropdownoption = this.getSelectedOption();
      return dropdownoption == null ? ItemStack.EMPTY : dropdownoption.getPreviewStack();
   }
}
