package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

/**
 * Chest Sus for Donut SMP:
 * - Scans Y <= Scan Max Y when deep layers are loaded
 * - Uses block-entity scan (chests/spawners often visible this way)
 * - Session cache: once found, stays marked even if deepslate unloads
 * - Player boost: chunk with underground player gets marked as interest
 */
public final class ChestSusFinder extends Module {
   public static ChestSusFinder INSTANCE;

   public final Setting<Integer> scanRadius = new Setting<>("Scan Radius", 4, 1, 12);
   public final Setting<Integer> scanMaxY = new Setting<>("Scan Max Y", -5, -64, 64);
   public final Setting<Integer> displayY = new Setting<>("Display Y", 64, -64, 320);
   public final Setting<Integer> minScore = new Setting<>("Min Score", 8, 1, 50);
   public final Setting<Boolean> useCache = new Setting<>("Session Cache", true);
   public final Setting<Boolean> playerBoost = new Setting<>("Player Boost", true);
   public final Setting<Color> fillColor = new Setting<>("Fill Color", new Color(255, 80, 40, 55));
   public final Setting<Integer> fillAlpha = new Setting<>("Fill Alpha", 55, 0, 255);
   public final Setting<Boolean> showInfo = new Setting<>("Show % List", true);
   public final Setting<Boolean> showOutline = new Setting<>("Outline", true);
   public final Setting<Boolean> clearCacheBtn = new Setting<>("Clear Cache", false);

   private ExecutorService scanExec;
   private final AtomicBoolean scanning = new AtomicBoolean(false);
   private int tickCount = 0;

   public ChestSusFinder() {
      super("Chest Sus Finder", Category.DONUT);
      INSTANCE = this;
      this.addSetting(this.scanRadius);
      this.addSetting(this.scanMaxY);
      this.addSetting(this.displayY);
      this.addSetting(this.minScore);
      this.addSetting(this.useCache);
      this.addSetting(this.playerBoost);
      this.addSetting(this.fillColor);
      this.addSetting(this.fillAlpha);
      this.addSetting(this.showInfo);
      this.addSetting(this.showOutline);
      this.addSetting(this.clearCacheBtn);
   }

   @Override
   public void onDisable() {
      this.scanning.set(false);
      if (this.scanExec != null) {
         this.scanExec.shutdownNow();
         this.scanExec = null;
      }
   }

   @Override
   public void onTick() {
      if (mc.world == null || mc.player == null) return;

      if (this.clearCacheBtn.getValue()) {
         SusCache.CHESTS.clear();
         this.clearCacheBtn.setValue(false);
      }

      // Player boost: underground players mark their chunk as interesting
      if (this.playerBoost.getValue() && PlayerDebug.INSTANCE != null && PlayerDebug.INSTANCE.isEnabled()) {
         for (Map.Entry<ChunkPos, PlayerDebug.Hit> e : PlayerDebug.INSTANCE.stableHits.entrySet()) {
            SusCache.ChestEntry ce = SusCache.CHESTS.computeIfAbsent(e.getKey(), k -> new SusCache.ChestEntry());
            if (ce.percent < 15) {
               ce.percent = 15;
               ce.tag = "P:" + e.getValue().name;
               ce.lastSeenMs = System.currentTimeMillis();
               ce.live = true;
            }
         }
      }

      if (++this.tickCount % 20 != 0) return;
      if (!this.scanning.compareAndSet(false, true)) return;

      if (this.scanExec == null || this.scanExec.isShutdown()) {
         this.scanExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "chest-sus");
            t.setDaemon(true);
            return t;
         });
      }

      final ChunkPos origin = mc.player.getChunkPos();
      final int range = this.scanRadius.getValue();
      final int maxY = this.scanMaxY.getValue();
      final int threshold = this.minScore.getValue();

      final List<ChunkPos> positions = new ArrayList<>();
      final List<WorldChunk> chunks = new ArrayList<>();
      for (int dx = -range; dx <= range; dx++) {
         for (int dz = -range; dz <= range; dz++) {
            ChunkPos cp = new ChunkPos(origin.x + dx, origin.z + dz);
            WorldChunk wc = mc.world.getChunkManager().getWorldChunk(cp.x, cp.z, false);
            if (wc != null && !wc.isEmpty()) {
               positions.add(cp);
               chunks.add(wc);
            }
         }
      }

      this.scanExec.submit(() -> {
         try {
            Set<ChunkPos> liveFound = new HashSet<>();
            for (int i = 0; i < positions.size(); i++) {
               int[] r = scanChunk(chunks.get(i), maxY);
               int chests = r[0], redstone = r[1], spawners = r[2];
               int score = chests * 12 + redstone * 2 + spawners * 30;
               if (score >= threshold) {
                  SusCache.putChest(positions.get(i), chests, redstone, spawners);
                  liveFound.add(positions.get(i));
               }
            }
            SusCache.markChestStaleOutside(liveFound);
         } catch (Throwable ignored) {
         } finally {
            this.scanning.set(false);
         }
      });
   }

   /** returns [chests, redstone, spawners] */
   private static int[] scanChunk(WorldChunk chunk, int maxY) {
      int chests = 0, redstone = 0, spawners = 0;

      // 1) Block entities — more reliable when sections are partially obfuscated
      try {
         for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
            BlockPos pos = e.getKey();
            if (pos.getY() > maxY) continue;
            BlockEntity be = e.getValue();
            if (be instanceof MobSpawnerBlockEntity) spawners++;
            else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity
                  || be instanceof HopperBlockEntity || be instanceof ShulkerBoxBlockEntity
                  || be instanceof DispenserBlockEntity || be instanceof DropperBlockEntity) {
               chests++;
            }
         }
      } catch (Throwable ignored) {}

      // 2) Full block scan of sections that actually exist client-side
      try {
         ChunkSection[] sections = chunk.getSectionArray();
         int bottom = chunk.getBottomY();
         for (int s = 0; s < sections.length; s++) {
            int sectionY = bottom + s * 16;
            if (sectionY > maxY) break;
            ChunkSection section = sections[s];
            if (section == null || section.isEmpty()) continue;
            int localMax = Math.min(15, maxY - sectionY);
            if (localMax < 0) continue;
            for (int x = 0; x < 16; x++)
               for (int z = 0; z < 16; z++)
                  for (int y = 0; y <= localMax; y++) {
                     BlockState st = section.getBlockState(x, y, z);
                     if (st.isOf(Blocks.SPAWNER)) spawners++;
                     else if (isStorage(st)) chests++;
                     else if (isRedstone(st)) redstone++;
                  }
         }
      } catch (Throwable ignored) {}

      return new int[]{chests, redstone, spawners};
   }

   private static boolean isStorage(BlockState st) {
      return st.isOf(Blocks.CHEST) || st.isOf(Blocks.TRAPPED_CHEST)
         || st.isOf(Blocks.BARREL) || st.isOf(Blocks.HOPPER)
         || st.isOf(Blocks.DROPPER) || st.isOf(Blocks.DISPENSER)
         || st.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock;
   }

   private static boolean isRedstone(BlockState st) {
      return st.isOf(Blocks.REDSTONE_WIRE) || st.isOf(Blocks.REPEATER) || st.isOf(Blocks.COMPARATOR)
         || st.isOf(Blocks.REDSTONE_TORCH) || st.isOf(Blocks.REDSTONE_WALL_TORCH) || st.isOf(Blocks.REDSTONE_BLOCK)
         || st.isOf(Blocks.PISTON) || st.isOf(Blocks.STICKY_PISTON) || st.isOf(Blocks.OBSERVER)
         || st.isOf(Blocks.TARGET) || st.isOf(Blocks.NOTE_BLOCK) || st.isOf(Blocks.DAYLIGHT_DETECTOR);
   }

   private List<Map.Entry<ChunkPos, SusCache.ChestEntry>> visibleEntries() {
      List<Map.Entry<ChunkPos, SusCache.ChestEntry>> list = new ArrayList<>();
      int threshold = this.minScore.getValue();
      boolean cache = this.useCache.getValue();
      for (Map.Entry<ChunkPos, SusCache.ChestEntry> e : SusCache.CHESTS.entrySet()) {
         SusCache.ChestEntry ce = e.getValue();
         if (ce.percent < threshold) continue;
         if (!cache && !ce.live) continue;
         list.add(e);
      }
      return list;
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world == null || mc.player == null) return;
      List<Map.Entry<ChunkPos, SusCache.ChestEntry>> entries = visibleEntries();
      if (entries.isEmpty()) return;

      Camera camera = RenderUtils.getCamera();
      if (camera == null) return;
      Vec3d cam = RenderUtils.getCameraPos(camera);

      double y = this.displayY.getValue() - cam.y;
      double y2 = y + 0.25;

      Color base = this.fillColor.getValue();
      int alpha = this.fillAlpha.getValue();
      Color fillLive = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
      Color fillCached = new Color(base.getRed(), base.getGreen(), Math.min(255, base.getBlue() + 40), Math.max(30, alpha - 15));
      Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, alpha + 120));

      matrices.push();
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      ShapeBatch batch = RenderUtils.beginShapeBatch(matrices);
      for (Map.Entry<ChunkPos, SusCache.ChestEntry> e : entries) {
         ChunkPos cp = e.getKey();
         Color fill = e.getValue().live ? fillLive : fillCached;
         double x1 = (cp.x << 4) - cam.x;
         double z1 = (cp.z << 4) - cam.z;
         batch.renderFilledBox(x1, y, z1, x1 + 16.0, y2, z1 + 16.0, fill);
         if (this.showOutline.getValue()) {
            batch.renderOutlineBox(x1, y, z1, x1 + 16.0, y2, z1 + 16.0, outline);
         }
      }
      batch.flush();
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      matrices.pop();
   }

   public static void renderHud(DrawContext context, float tickDelta) {
      ChestSusFinder self = INSTANCE;
      if (self == null || !self.isEnabled() || !self.showInfo.getValue()) return;
      List<Map.Entry<ChunkPos, SusCache.ChestEntry>> entries = self.visibleEntries();
      if (entries.isEmpty()) return;

      int y = 36;
      if (SUSChunkFinder.INSTANCE != null && SUSChunkFinder.INSTANCE.isEnabled()
            && !SUSChunkFinder.INSTANCE.hits.isEmpty()) {
         y += 12 + 11 * Math.min(12, SUSChunkFinder.INSTANCE.hits.size()) + 6;
      }

      FontRenderer.INSTANCE.drawString(context,
         "Chest Sus (cache " + SusCache.CHESTS.size() + ")", 4, y, 0xFFFF8040);
      y += 12;
      int n = 0;
      for (Map.Entry<ChunkPos, SusCache.ChestEntry> e : entries) {
         if (n >= 12) break;
         SusCache.ChestEntry h = e.getValue();
         ChunkPos cp = e.getKey();
         int color = h.percent >= 70 ? 0xFFFF5555 : (h.percent >= 40 ? 0xFFFFAA00 : 0xFF55FF55);
         String live = h.live ? "" : " *";
         FontRenderer.INSTANCE.drawString(context,
            String.format("  [%d %d] %d%% %s%s", cp.x, cp.z, h.percent, h.tag == null ? "" : h.tag, live),
            4, y, color);
         y += 11;
         n++;
      }
   }
}
