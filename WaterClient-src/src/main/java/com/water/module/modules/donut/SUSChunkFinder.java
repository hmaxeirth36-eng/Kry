package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

/**
 * Krypton-style Sus Chunk Finder for DonutSMP.
 * Detects player activity via surface-visible anomalies:
 * kelp, vines, cave vines, bamboo, cocoa, bee nests, amethyst, rotated deepslate.
 * No percentage system — just highlight suspicious chunks.
 */
public final class SUSChunkFinder extends Module {
   public static SUSChunkFinder INSTANCE;

   // --- Krypton-like settings ---
   public final Setting<Integer> simulationDist = new Setting<>("Simulation Dist", 4, 1, 12);
   public final Setting<Integer> sensitivity = new Setting<>("Sensitivity", 3, 1, 20);
   public final Setting<Boolean> smartAdjustment = new Setting<>("Smart Adjustment", true);
   public final Setting<Integer> alpha = new Setting<>("Alpha", 55, 0, 255);
   public final Setting<Integer> displayY = new Setting<>("Display Y", 64, -64, 320);
   public final Setting<Color> fillColor = new Setting<>("Fill Color", new Color(255, 40, 40, 55));
   public final Setting<Boolean> showOutline = new Setting<>("Outline", true);

   // Block toggles (same as Krypton screenshot)
   public final Setting<Boolean> kelp = new Setting<>("Kelp", true);
   public final Setting<Boolean> caveVines = new Setting<>("Cave Vines", true);
   public final Setting<Boolean> vines = new Setting<>("Vines", true);
   public final Setting<Boolean> amethyst = new Setting<>("Amethyst", true);
   public final Setting<Boolean> bamboo = new Setting<>("Bamboo", true);
   public final Setting<Boolean> cocoa = new Setting<>("Cocoa", true);
   public final Setting<Boolean> beeNest = new Setting<>("Bee Nest", true);
   public final Setting<Boolean> rotatedDeepslate = new Setting<>("Rotated Deepslate", true);

   /** Stable set of suspicious chunks — only replaced when a scan batch finishes */
   public volatile Set<ChunkPos> hits = Collections.emptySet();

   private ExecutorService scanExec;
   private final AtomicBoolean scanning = new AtomicBoolean(false);
   private int tickCount = 0;
   private int scanCursor = 0;

   public SUSChunkFinder() {
      super("Sus Chunk Finder", Category.DONUT);
      INSTANCE = this;
      this.addSetting(this.simulationDist);
      this.addSetting(this.sensitivity);
      this.addSetting(this.smartAdjustment);
      this.addSetting(this.alpha);
      this.addSetting(this.displayY);
      this.addSetting(this.fillColor);
      this.addSetting(this.showOutline);
      this.addSetting(this.kelp);
      this.addSetting(this.caveVines);
      this.addSetting(this.vines);
      this.addSetting(this.amethyst);
      this.addSetting(this.bamboo);
      this.addSetting(this.cocoa);
      this.addSetting(this.beeNest);
      this.addSetting(this.rotatedDeepslate);
   }

   @Override
   public void onEnable() {
      this.hits = Collections.emptySet();
      this.tickCount = 0;
      this.scanCursor = 0;
   }

   @Override
   public void onDisable() {
      this.hits = Collections.emptySet();
      this.scanning.set(false);
      if (this.scanExec != null) {
         this.scanExec.shutdownNow();
         this.scanExec = null;
      }
   }

   @Override
   public void onTick() {
      if (mc.world == null || mc.player == null) return;
      if (++this.tickCount % 4 != 0) return;
      if (!this.scanning.compareAndSet(false, true)) return;

      if (this.scanExec == null || this.scanExec.isShutdown()) {
         this.scanExec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sus-chunk-krypton");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
         });
      }

      final ChunkPos origin = mc.player.getChunkPos();
      final int radius = this.simulationDist.getValue();
      final int threshold = this.sensitivity.getValue();
      final boolean smart = this.smartAdjustment.getValue();

      // Snapshot which detectors are on
      final boolean doKelp = this.kelp.getValue();
      final boolean doCaveVines = this.caveVines.getValue();
      final boolean doVines = this.vines.getValue();
      final boolean doAm = this.amethyst.getValue();
      final boolean doBamboo = this.bamboo.getValue();
      final boolean doCocoa = this.cocoa.getValue();
      final boolean doBee = this.beeNest.getValue();
      final boolean doRot = this.rotatedDeepslate.getValue();

      final List<ChunkPos> positions = new ArrayList<>();
      final List<WorldChunk> chunks = new ArrayList<>();
      for (int dx = -radius; dx <= radius; dx++) {
         for (int dz = -radius; dz <= radius; dz++) {
            ChunkPos cp = new ChunkPos(origin.x + dx, origin.z + dz);
            WorldChunk wc = mc.world.getChunkManager().getWorldChunk(cp.x, cp.z, false);
            if (wc != null && !wc.isEmpty()) {
               positions.add(cp);
               chunks.add(wc);
            }
         }
      }

      if (positions.isEmpty()) {
         this.scanning.set(false);
         return;
      }

      // Progressive batch
      final int batch = 10;
      final int start = this.scanCursor % positions.size();
      this.scanCursor = start + batch;

      // Keep previous hits outside this batch so list doesn't flicker
      final Map<ChunkPos, Boolean> prev = new HashMap<>();
      for (ChunkPos cp : this.hits) prev.put(cp, Boolean.TRUE);

      this.scanExec.submit(() -> {
         try {
            Set<ChunkPos> next = new HashSet<>();
            // retain far hits still in radius from previous
            for (ChunkPos cp : prev.keySet()) {
               if (Math.abs(cp.x - origin.x) <= radius && Math.abs(cp.z - origin.z) <= radius) {
                  next.add(cp);
               }
            }

            int end = Math.min(positions.size(), start + batch);
            List<Integer> idxs = new ArrayList<>();
            for (int i = start; i < end; i++) idxs.add(i);
            if (start + batch > positions.size()) {
               for (int i = 0; i < Math.min(batch - (positions.size() - start), positions.size()); i++) {
                  idxs.add(i);
               }
            }

            for (int idx : idxs) {
               if (idx < 0 || idx >= positions.size()) continue;
               ChunkPos cp = positions.get(idx);
               WorldChunk wc = chunks.get(idx);

               int score = scoreChunk(wc, doKelp, doCaveVines, doVines, doAm, doBamboo, doCocoa, doBee, doRot);

               int need = threshold;
               if (smart) {
                  // fewer loaded sections → lower bar slightly (Donut partial load)
                  int sections = countNonEmptySections(wc);
                  if (sections < 8) need = Math.max(1, threshold - 1);
                  if (sections < 4) need = Math.max(1, threshold - 2);
               }

               if (score >= need) next.add(cp);
               else next.remove(cp);
            }

            this.hits = Collections.unmodifiableSet(next);
         } catch (Throwable ignored) {
         } finally {
            this.scanning.set(false);
         }
      });
   }

   private static int countNonEmptySections(WorldChunk chunk) {
      int n = 0;
      for (ChunkSection s : chunk.getSectionArray()) {
         if (s != null && !s.isEmpty()) n++;
      }
      return n;
   }

   /**
    * Score a chunk. Each signal type contributes points.
    * Designed so surface-visible blocks alone can flag a chunk on Donut.
    */
   private static int scoreChunk(
         WorldChunk chunk,
         boolean doKelp, boolean doCaveVines, boolean doVines, boolean doAm,
         boolean doBamboo, boolean doCocoa, boolean doBee, boolean doRot
   ) {
      int kelpN = 0, caveN = 0, vineN = 0, amN = 0;
      int bambooN = 0, cocoaN = 0, beeN = 0, rotN = 0;

      ChunkSection[] sections = chunk.getSectionArray();
      for (ChunkSection section : sections) {
         if (section == null || section.isEmpty()) continue;

         // cheap prefilter: skip section if none of our blocks possible
         if (!section.hasAny(st -> isTracked(st))) continue;

         for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  BlockState st = section.getBlockState(x, y, z);
                  Block b = st.getBlock();

                  if (doKelp && (b == Blocks.KELP || b == Blocks.KELP_PLANT)) kelpN++;
                  else if (doCaveVines && (b == Blocks.CAVE_VINES || b == Blocks.CAVE_VINES_PLANT)) caveN++;
                  else if (doVines && b == Blocks.VINE) vineN++;
                  else if (doAm && isAmethyst(st)) amN++;
                  else if (doBamboo && (b == Blocks.BAMBOO || b == Blocks.BAMBOO_SAPLING)) bambooN++;
                  else if (doCocoa && b == Blocks.COCOA) cocoaN++;
                  else if (doBee && (b == Blocks.BEE_NEST || b == Blocks.BEEHIVE)) beeN++;
                  else if (doRot && isRotatedDeepslate(st)) rotN++;
               }
            }
         }
      }

      int score = 0;
      // Weighted like typical Krypton-style detectors
      if (doKelp && kelpN >= 8) score += 1 + kelpN / 24;
      if (doCaveVines && caveN >= 6) score += 1 + caveN / 20;
      if (doVines && vineN >= 10) score += 1 + vineN / 30;
      if (doAm && amN >= 4) score += 2 + amN / 12;          // amethyst strong signal
      if (doBamboo && bambooN >= 6) score += 1 + bambooN / 20;
      if (doCocoa && cocoaN >= 3) score += 1 + cocoaN / 8;
      if (doBee && beeN >= 1) score += 2 + beeN;             // rare = strong
      if (doRot && rotN >= 12) score += 2 + rotN / 40;       // rotated deepslate cluster

      return score;
   }

   private static boolean isTracked(BlockState st) {
      Block b = st.getBlock();
      return b == Blocks.KELP || b == Blocks.KELP_PLANT
         || b == Blocks.CAVE_VINES || b == Blocks.CAVE_VINES_PLANT
         || b == Blocks.VINE
         || isAmethyst(st)
         || b == Blocks.BAMBOO || b == Blocks.BAMBOO_SAPLING
         || b == Blocks.COCOA
         || b == Blocks.BEE_NEST || b == Blocks.BEEHIVE
         || isRotatedDeepslate(st);
   }

   private static boolean isAmethyst(BlockState st) {
      Block b = st.getBlock();
      return b == Blocks.AMETHYST_BLOCK || b == Blocks.BUDDING_AMETHYST
         || b == Blocks.AMETHYST_CLUSTER
         || b == Blocks.SMALL_AMETHYST_BUD || b == Blocks.MEDIUM_AMETHYST_BUD || b == Blocks.LARGE_AMETHYST_BUD;
   }

   /**
    * "Rotated deepslate" heuristic used by many Donut clients:
    * deepslate (or polished) whose AXIS is horizontal (X/Z), which is far more
    * common in player builds than in natural generation patterns at volume.
    * Also counts cobbled / brick / tile variants (always player-placed).
    */
   private static boolean isRotatedDeepslate(BlockState st) {
      Block b = st.getBlock();
      if (b == Blocks.COBBLED_DEEPSLATE || b == Blocks.COBBLED_DEEPSLATE_SLAB
            || b == Blocks.COBBLED_DEEPSLATE_STAIRS || b == Blocks.COBBLED_DEEPSLATE_WALL
            || b == Blocks.POLISHED_DEEPSLATE || b == Blocks.POLISHED_DEEPSLATE_SLAB
            || b == Blocks.POLISHED_DEEPSLATE_STAIRS || b == Blocks.POLISHED_DEEPSLATE_WALL
            || b == Blocks.DEEPSLATE_BRICKS || b == Blocks.DEEPSLATE_BRICK_SLAB
            || b == Blocks.DEEPSLATE_BRICK_STAIRS || b == Blocks.DEEPSLATE_BRICK_WALL
            || b == Blocks.DEEPSLATE_TILES || b == Blocks.DEEPSLATE_TILE_SLAB
            || b == Blocks.DEEPSLATE_TILE_STAIRS || b == Blocks.DEEPSLATE_TILE_WALL
            || b == Blocks.CHISELED_DEEPSLATE || b == Blocks.REINFORCED_DEEPSLATE) {
         return true;
      }
      if (b == Blocks.DEEPSLATE || b == Blocks.POLISHED_DEEPSLATE) {
         if (st.contains(Properties.AXIS)) {
            Direction.Axis axis = st.get(Properties.AXIS);
            return axis != Direction.Axis.Y;
         }
      }
      return false;
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      Set<ChunkPos> set = this.hits;
      if (set.isEmpty() || mc.world == null || mc.player == null) return;

      Camera camera = RenderUtils.getCamera();
      if (camera == null) return;
      Vec3d cam = RenderUtils.getCameraPos(camera);

      double y = this.displayY.getValue() - cam.y;
      double y2 = y + 0.25;

      Color base = this.fillColor.getValue();
      int a = this.alpha.getValue();
      Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
      Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, a + 120));

      matrices.push();
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      ShapeBatch batch = RenderUtils.beginShapeBatch(matrices);
      for (ChunkPos cp : set) {
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

   /** No percentage HUD — Krypton style is visual boxes only */
   public static void renderHud(DrawContext context, float tickDelta) {
      // intentionally empty
   }
}
