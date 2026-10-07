package com.water.module.modules.client;

public enum HudElement {
   WATERMARK("Watermark"),
   COORDINATES("Coordinates"),
   INFO("Info"),
   MODULE_LIST("Module List"),
   POTION_EFFECTS("Potion Effects"),
   ARMOR("Armor"),
   KEYBINDS("Keybinds"),
   SPOTIFY_HUD("Spotify HUD"),
   RADAR("Radar"),
   STAFF_LIST("Staff List");

   public final String label;

    HudElement(String text) {
      this.label = text;
   }
}
