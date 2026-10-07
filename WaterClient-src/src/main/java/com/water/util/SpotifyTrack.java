package com.water.util;

public final class SpotifyTrack {
   public final String id;
   public final String name;
   public final String artist;
   public final String uri;

   public SpotifyTrack(String text, String text2, String text3, String text4) {
      this.id = text;
      this.name = text2;
      this.artist = text3;
      this.uri = text4;
   }

   public String display() {
      return this.artist.isEmpty() ? this.name : this.artist + " - " + this.name;
   }
}
