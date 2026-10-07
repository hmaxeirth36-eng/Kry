package com.water.util;

import net.minecraft.text.Style;

public record ChatSegment(String text, Style style) {

   public ChatSegment(String text, Style style) {
      style = style == null ? Style.EMPTY : style;
      this.text = text;
      this.style = style;
   }
}
