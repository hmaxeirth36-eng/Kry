package com.water.util;

public record AutoLogWebhook(String webhook, String playerName, int x, int y, int z, String serverIp, String time, String skinRenderUrl) {

   public String formatCoordsLabeled() {
      return "X: " + this.x + " Y: " + this.y + " Z: " + this.z;
   }

   public String formatCoords() {
      return this.x + ", " + this.y + ", " + this.z;
   }
}
