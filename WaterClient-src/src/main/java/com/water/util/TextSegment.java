package com.water.util;

import net.minecraft.text.Style;

public record TextSegment(String text, Style style) {

   public TextSegment(String text, Style style) {
      style = style == null ? Style.EMPTY : style;
      this.text = text;
      this.style = style;
   }
}
