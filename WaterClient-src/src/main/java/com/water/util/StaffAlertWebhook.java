package com.water.util;

public record StaffAlertWebhook(
   String webhook,
   String title,
   String description,
   int color,
   String playerName,
   String threatName,
   String distance,
   int spawnersInInventory,
   boolean allMined,
   String serverIp,
   String time,
   String skinRenderUrl,
   String disconnectReason
) {
}
