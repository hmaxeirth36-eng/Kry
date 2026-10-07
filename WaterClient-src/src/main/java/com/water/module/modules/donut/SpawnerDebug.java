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
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class SpawnerDebug extends Module {
   public static SpawnerDebug INSTANCE;

   public final Setting<Integer> scanRadius = new Setting<>("Scan Radius", 5, 1, 12);
   public final Setting<Integer> displayY = new Setting<>("Display Y", 80, -64, 320);
   public final Setting<Boolean> useCache = new Setting<>("Session Cache", true);
   public final Setting<Color> fillColor = new Setting<>("Fill Color", new Color(255, 200, 40, 55));
   public final Setting<Integer> fillAlpha = new Setting<>("Fill Alpha", 55, 0, 255);
   public final Setting<Boolean> showInfo = new Setting<>("Show List", true);
   public final Setting<Boolean> showOutline = new Setting<>("Outline", true);
   public final Setting<Boolean> clearCacheBtn = new Setting<>("Clear Cache", false);

   private ExecutorService scanExec;
   private final AtomicBoolean scanning = new AtomicBoolean(false);
   private int tickCount = 0;

   public SpawnerDebug() {
      super("Spawner Debug", Category.DONUT);
      INSTANCE = this;
      this.addSetting(this.scanRadius);
      this.addSetting(this.displayY);
      this.addSetting(this.useCache);
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
         SusCache.SPAWNERS.clear();
         this.clearCacheBtn.setValue(false);
      }

      if (++this.tickCount % 20 != 0) return;
      if (!this.scanning.compareAndSet(false, true)) return;

      if (this.scanExec == null || this.scanExec.isShutdown()) {
         this.scanExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "spawner-debug");
            t.setDaemon(true);
            return t;
         });
      }

      final ChunkPos origin = mc.player.getChunkPos();
      final int range = this.scanRadius.getValue();

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
            Set<ChunkPos> live = new HashSet<>();
            for (int i = 0; i < positions.size(); i++) {
               int[] r = scanSpawners(chunks.get(i));
               if (r[0] > 0) {
                  SusCache.putSpawner(positions.get(i), r[0], r[1]);
                  live.add(positions.get(i));
               }
            }
            SusCache.markSpawnerStaleOutside(live);
         } catch (Throwable ignored) {
         } finally {
            this.scanning.set(false);
         }
      });
   }

   /** [count, minY] */
   private static int[] scanSpawners(WorldChunk chunk) {
      int count = 0;
      int minY = Integer.MAX_VALUE;

      try {
         for (Map.Entry<BlockPos, BlockEntity> e : chunk.getBlockEntities().entrySet()) {
            if (e.getValue() instanceof MobSpawnerBlockEntity) {
               count++;
               int wy = e.getKey().getY();
               if (wy < minY) minY = wy;
            }
         }
      } catch (Throwable ignored) {}

      try {
         ChunkSection[] sections = chunk.getSectionArray();
         int bottom = chunk.getBottomY();
         for (int s = 0; s < sections.length; s++) {
            int sectionY = bottom + s * 16;
            ChunkSection section = sections[s];
            if (section == null || section.isEmpty()) continue;
            if (!section.hasAny(st -> st.isOf(Blocks.SPAWNER))) continue;
            for (int x = 0; x < 16; x++)
               for (int y = 0; y < 16; y++)
                  for (int z = 0; z < 16; z++) {
                     if (section.getBlockState(x, y, z).isOf(Blocks.SPAWNER)) {
                        count++;
                        int wy = sectionY + y;
                        if (wy < minY) minY = wy;
                     }
                  }
         }
      } catch (Throwable ignored) {}

      if (count == 0) return new int[]{0, 0};
      return new int[]{count, minY == Integer.MAX_VALUE ? 0 : minY};
   }

   private List<Map.Entry<ChunkPos, SusCache.SpawnerEntry>> visibleEntries() {
      List<Map.Entry<ChunkPos, SusCache.SpawnerEntry>> list = new ArrayList<>();
      boolean cache = this.useCache.getValue();
      for (Map.Entry<ChunkPos, SusCache.SpawnerEntry> e : SusCache.SPAWNERS.entrySet()) {
         if (!cache && !e.getValue().live) continue;
         list.add(e);
      }
      return list;
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world == null || mc.player == null) return;
      List<Map.Entry<ChunkPos, SusCache.SpawnerEntry>> entries = visibleEntries();
      if (entries.isEmpty()) return;

      Camera camera = RenderUtils.getCamera();
      if (camera == null) return;
      Vec3d cam = RenderUtils.getCameraPos(camera);

      double y = this.displayY.getValue() - cam.y;
      double y2 = y + 0.25;

      Color base = this.fillColor.getValue();
      int alpha = this.fillAlpha.getValue();
      Color fillLive = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
      Color fillCached = new Color(Math.min(255, base.getRed() + 20), base.getGreen(), base.getBlue(), Math.max(30, alpha - 15));
      Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, alpha + 120));

      matrices.push();
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      ShapeBatch batch = RenderUtils.beginShapeBatch(matrices);
      for (Map.Entry<ChunkPos, SusCache.SpawnerEntry> e : entries) {
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
      SpawnerDebug self = INSTANCE;
      if (self == null || !self.isEnabled() || !self.showInfo.getValue()) return;
      List<Map.Entry<ChunkPos, SusCache.SpawnerEntry>> entries = self.visibleEntries();
      if (entries.isEmpty()) return;

      int y = 36;
      if (SUSChunkFinder.INSTANCE != null && SUSChunkFinder.INSTANCE.isEnabled()
            && !SUSChunkFinder.INSTANCE.hits.isEmpty()) {
         y += 12 + 11 * Math.min(12, SUSChunkFinder.INSTANCE.hits.size()) + 6;
      }
      if (ChestSusFinder.INSTANCE != null && ChestSusFinder.INSTANCE.isEnabled()) {
         y += 12 + 11 * 8 + 6;
      }

      FontRenderer.INSTANCE.drawString(context,
         "Spawner Debug (cache " + SusCache.SPAWNERS.size() + ")", 4, y, 0xFFFFCC40);
      y += 12;
      int n = 0;
      for (Map.Entry<ChunkPos, SusCache.SpawnerEntry> e : entries) {
         if (n >= 10) break;
         ChunkPos cp = e.getKey();
         SusCache.SpawnerEntry h = e.getValue();
         String live = h.live ? "" : " *";
         FontRenderer.INSTANCE.drawString(context,
            String.format("  [%d %d] x%d Y%d%s", cp.x, cp.z, h.count, h.minY, live),
            4, y, 0xFFFFDD66);
         y += 11;
         n++;
      }
   }
}
