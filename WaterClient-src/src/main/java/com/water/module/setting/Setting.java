package com.water.module.setting;

import com.water.module.ModuleManager;
import java.util.function.Supplier;

public class Setting<T> {
   public final String name;
   public final T defaultValue;
   public T value;
   public T min;
   public T max;
   public Supplier<Boolean> visibility = () -> true;

   public Setting(String text, T t) {
      this.name = text;
      this.value = (T)t;
      this.defaultValue = (T)t;
   }

   public Setting(String text, T t, T t2, T t3) {
      this.name = text;
      this.value = (T)t;
      this.defaultValue = (T)t;
      this.min = (T)t2;
      this.max = (T)t3;
   }

   public String getName() {
      return this.name;
   }

   public T getValue() {
      return this.value;
   }

   public T getDefaultValue() {
      return this.defaultValue;
   }

   public boolean matchesName(String text) {
      return this.name.equalsIgnoreCase(text);
   }

   public Setting<T> visibleWhen(Supplier<Boolean> supplier) {
      this.visibility = supplier == null ? () -> true : supplier;
      return this;
   }

   public boolean isVisible() {
      try {
         return this.visibility == null || this.visibility.get();
      } catch (Exception exception) {
         return true;
      }
   }

   public void setValue(T t) {
      this.value = (T)t;
      ModuleManager.INSTANCE.onSettingChanged();
   }

   public T getMin() {
      return this.min;
   }

   public T getMax() {
      return this.max;
   }

   public static String watermarkFragment() {
      return "1";
   }
}
