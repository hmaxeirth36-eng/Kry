package com.water.module.modules.donut;

import net.minecraft.util.math.ChunkPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session-long memory of found chunks.
 * On Donut SMP, deepslate layers often only load when a player goes near Y0.
 * Once we see a chest/spawner we keep the chunk marked even after data unloads.
 */
public final class SusCache {
   private SusCache() {}

   public static final class ChestEntry {
      public int chests, redstone, spawners, percent;
      public String tag;
      public long lastSeenMs;
      public boolean live; // true = currently visible in loaded data
   }

   public static final class SpawnerEntry {
      public int count;
      public int minY;
      public long lastSeenMs;
      public boolean live;
   }

   public static final Map<ChunkPos, ChestEntry> CHESTS = new ConcurrentHashMap<>();
   public static final Map<ChunkPos, SpawnerEntry> SPAWNERS = new ConcurrentHashMap<>();

   public static void clearAll() {
      CHESTS.clear();
      SPAWNERS.clear();
   }

   public static void putChest(ChunkPos cp, int chests, int redstone, int spawners) {
      if (chests == 0 && redstone == 0 && spawners == 0) return;
      ChestEntry e = CHESTS.computeIfAbsent(cp, k -> new ChestEntry());
      e.chests = Math.max(e.chests, chests);
      e.redstone = Math.max(e.redstone, redstone);
      e.spawners = Math.max(e.spawners, spawners);
      e.percent = Math.min(100, e.chests * 12 + e.redstone * 2 + e.spawners * 30);
      StringBuilder sb = new StringBuilder();
      if (e.spawners > 0) sb.append("S").append(e.spawners);
      if (e.chests > 0) { if (!sb.isEmpty()) sb.append(' '); sb.append("C").append(e.chests); }
      if (e.redstone > 0) { if (!sb.isEmpty()) sb.append(' '); sb.append("R").append(e.redstone); }
      e.tag = sb.toString();
      e.lastSeenMs = System.currentTimeMillis();
      e.live = true;
   }

   public static void putSpawner(ChunkPos cp, int count, int minY) {
      if (count <= 0) return;
      SpawnerEntry e = SPAWNERS.computeIfAbsent(cp, k -> new SpawnerEntry());
      e.count = Math.max(e.count, count);
      if (minY < e.minY || e.minY == 0 && e.count == count) e.minY = minY;
      if (e.minY == 0 && minY != 0) e.minY = minY;
      e.lastSeenMs = System.currentTimeMillis();
      e.live = true;
   }

   public static void markChestStaleOutside(java.util.Set<ChunkPos> liveSet) {
      for (Map.Entry<ChunkPos, ChestEntry> e : CHESTS.entrySet()) {
         e.getValue().live = liveSet.contains(e.getKey());
      }
   }

   public static void markSpawnerStaleOutside(java.util.Set<ChunkPos> liveSet) {
      for (Map.Entry<ChunkPos, SpawnerEntry> e : SPAWNERS.entrySet()) {
         e.getValue().live = liveSet.contains(e.getKey());
      }
   }
}
