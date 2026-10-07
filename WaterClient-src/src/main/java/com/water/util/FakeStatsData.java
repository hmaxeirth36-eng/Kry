package com.water.util;

public record FakeStatsData(String money, String shards, String kills, String deaths, String playtime) {

   public String lQ() {
      return this.money + "|" + this.shards + "|" + this.kills + "|" + this.deaths + "|" + this.playtime;
   }
}
