package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import com.water.util.BlockCluster;
import com.water.util.CenterPos;
import com.water.util.ChunkSuspicion;
import com.water.util.PosRecord;
import com.water.util.WeightedPos;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.chunk.WorldChunk;

public final class GrowthFinder extends Module {
   public final Setting<Boolean> renderVines = new Setting<>("Render Vines", true);
   public final Setting<Boolean> renderDripstone = new Setting<>("Render Dripstone", true);
   public final Setting<Boolean> renderBerries = new Setting<>("Render Berries", true);
   public final Setting<Boolean> renderStandardChunks = new Setting<>("Render Gray Chunks", true);
   public final Setting<Float> alpha = new Setting<>("Alpha", 80.0F, 0.0F, 255.0F);
   public static final int SCAN_RADIUS = 8;
   public static final int CHUNKS_PER_TICK = 12;
   public static final int MIN_VINE_LENGTH = 6;
   public static final int MAX_VINE_SCAN_PER_CHUNK = 2;
   public static final double PLATE_HEIGHT = 0.08;
   public static final Color HIGH_SUSPICION_GRAY = new Color(135, 135, 135);
   public static final Color LOW_SUSPICION_GRAY = new Color(100, 100, 100);
   public static final Color SOURCE_PLATE_COLOR_BASE = new Color(203, 64, 255);
   public static final Color EXTREME_PLATE_COLOR_BASE = new Color(255, 198, 64);
   public final Set<ChunkPos> scannedChunks = new HashSet<>();
   public final Map<ChunkPos, ChunkSuspicion> suspiciousChunks = new HashMap<>();
   public final List<ChunkPos> scanQueue = new ArrayList<>();
   public ChunkPos lastQueueCenter = null;
   public int scanCursor = 0;
   public final List<ChunkPos> lockedBaseChunks = new ArrayList<>();
   public final Map<ChunkPos, Double> sourceHistory = new HashMap<>();

   public GrowthFinder() {
      super("Growth Finder", Category.DONUT);
      this.addSetting(this.renderVines);
      this.addSetting(this.renderDripstone);
      this.addSetting(this.renderBerries);
      this.addSetting(this.renderStandardChunks);
      this.addSetting(this.alpha);
   }

   @Override
   public void onEnable() {
      this.clearScanState();
   }

   @Override
   public void onDisable() {
      this.clearScanState();
   }

   public void clearScanState() {
      this.suspiciousChunks.clear();
      this.scannedChunks.clear();
      this.scanQueue.clear();
      this.scanCursor = 0;
      this.lastQueueCenter = null;
      this.lockedBaseChunks.clear();
      this.sourceHistory.clear();
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.world != null) {
         ChunkPos chunkpos = new ChunkPos(mc.player.getBlockPos());
         this.rebuildScanQueue(chunkpos);
         this.processScanQueue(mc.world);
         this.pruneDistantResults(chunkpos, 8);
         if (mc.world.getTime() % 20L == 0L) {
            this.updateSourceHistory();
         }
      }
   }

   public void rebuildScanQueue(ChunkPos chunkPos) {
      if (this.lastQueueCenter == null || chunkPos.x != this.lastQueueCenter.x || chunkPos.z != this.lastQueueCenter.z || this.scanCursor >= this.scanQueue.size()) {
         this.scanQueue.clear();

         for (int i = -8; i <= 8; i++) {
            for (int j = -8; j <= 8; j++) {
               this.scanQueue.add(new ChunkPos(chunkPos.x + i, chunkPos.z + j));
            }
         }

         this.scanCursor = 0;
         this.lastQueueCenter = chunkPos;
      }
   }

   public void processScanQueue(World world) {
      int i = 0;

      while (this.scanCursor < this.scanQueue.size() && i < 12) {
         ChunkPos chunkpos = this.scanQueue.get(this.scanCursor++);
         if (!this.scannedChunks.contains(chunkpos)) {
            WorldChunk worldChunk = world.getChunkManager().getWorldChunk(chunkpos.x, chunkpos.z, false);
            if (worldChunk != null) {
               this.analyzeChunk(worldChunk, chunkpos);
               this.scannedChunks.add(chunkpos);
            }

            i++;
         }
      }
   }

   public void pruneDistantResults(ChunkPos chunkPos, int radius) {
      int i = radius + 2;
      int j = chunkPos.x;
      int k = chunkPos.z;
      this.suspiciousChunks.entrySet().removeIf(var3x -> Math.abs(var3x.getKey().x - j) > i || Math.abs(var3x.getKey().z - k) > i);
      this.scannedChunks.removeIf(var3x -> Math.abs(var3x.x - j) > i || Math.abs(var3x.z - k) > i);
   }

   public void updateSourceHistory() {
      if (this.suspiciousChunks.isEmpty()) {
         this.sourceHistory.clear();
      } else {
         for (ChunkSuspicion chunkSuspicion : this.suspiciousChunks.values()) {
            ChunkPos chunkpos = chunkSuspicion.baseChunk();
            this.sourceHistory.merge(chunkpos, (double)chunkSuspicion.suspicionLevel(), Double::sum);
         }

         this.sourceHistory.entrySet().removeIf(var0 -> {
            double d0 = var0.getValue() * 0.95;
            var0.setValue(d0);
            return d0 < 0.5;
         });
         ArrayList<Map.Entry<ChunkPos, Double>> arrayList = new ArrayList<>(this.sourceHistory.entrySet());
         arrayList.sort((var0, var1) -> Double.compare((Double)var1.getValue(), (Double)var0.getValue()));
         this.lockedBaseChunks.clear();

         for (int i = 0; i < Math.min(2, arrayList.size()); i++) {
            this.lockedBaseChunks.add((ChunkPos)((Entry)arrayList.get(i)).getKey());
         }
      }
   }

   public void analyzeChunk(WorldChunk chunk, ChunkPos chunkPos) {
      int i = chunkPos.getStartX();
      int j = chunkPos.getStartZ();
      List list = this.renderVines.getValue() ? this.findVineClusters(chunk, i, j) : Collections.emptyList();
      List list1 = this.renderDripstone.getValue() ? this.findDripstone(chunk, i, j) : Collections.emptyList();
      List list2 = this.renderBerries.getValue() ? this.findBerries(chunk, i, j) : Collections.emptyList();
      ArrayList arrayList = new ArrayList();
      double d0 = 0.0;
      int k = 0;

      for (BlockCluster blockCluster : (Iterable<BlockCluster>)list) {
         double d1 = blockCluster.length;
         d0 += d1;
         if (blockCluster.length > k) {
            k = blockCluster.length;
         }

         arrayList.add(new WeightedPos(blockCluster.centroid(), d1));
      }

      double d2 = 0.0;

      for (CenterPos centerPos : (Iterable<CenterPos>)list1) {
         d2 += 2.5;
         arrayList.add(new WeightedPos(centerPos.center, 2.5));
      }

      double d3 = 0.0;

      for (PosRecord posRecord : (Iterable<PosRecord>)list2) {
         d3++;
         arrayList.add(new WeightedPos(posRecord.pos(), 1.0));
      }

      int l = (int)Math.round(d0 * 0.5 + d2 * 0.75 + d3 * 1.0);
      if (l > 0) {
         BlockPos blockPos = this.weightedCentroid(arrayList, chunkPos);
         boolean flag = k >= 100;
         boolean flag1 = k >= 25;
         this.suspiciousChunks.put(chunkPos, new ChunkSuspicion(chunkPos, l, new ChunkPos(blockPos), flag, flag1, k));
      } else {
         this.suspiciousChunks.remove(chunkPos);
      }
   }

   public List<BlockCluster> findVineClusters(WorldChunk chunk, int startX, int startZ) {
      ArrayList arrayList = new ArrayList();
      HashSet hashSet = new HashSet();
      int i = chunk.getBottomY();
      int j = 0;

      for (int k = startX; k < startX + 16; k += 2) {
         for (int l = startZ; l < startZ + 16 && j < 2; l += 2) {
            int i1 = chunk.getHeightmap(Type.MOTION_BLOCKING).get(k - startX, l - startZ);

            while (i1 >= i) {
               BlockPos blockPos = new BlockPos(k, i1, l);
               if (hashSet.contains(blockPos)) {
                  i1--;
               } else {
                  BlockState blockState = chunk.getBlockState(blockPos);
                  if (!this.isVine(blockState.getBlock())) {
                     i1--;
                  } else {
                     ArrayList arrayList1 = new ArrayList();

                     BlockPos blockPos1;
                     for (blockPos1 = blockPos; blockPos1.getY() >= i && this.isVine(chunk.getBlockState(blockPos1).getBlock()); blockPos1 = blockPos1.down()) {
                        hashSet.add(blockPos1);
                        arrayList1.add(blockPos1);
                     }

                     if (arrayList1.size() >= 6) {
                        arrayList.add(new BlockCluster(arrayList1));
                        j++;
                     }

                     i1 = blockPos1.getY() - 1;
                  }
               }
            }
         }
      }

      return arrayList;
   }

   public boolean isVine(Block block) {
      return block instanceof VineBlock
         || block == Blocks.CAVE_VINES
         || block == Blocks.CAVE_VINES_PLANT
         || block == Blocks.WEEPING_VINES
         || block == Blocks.WEEPING_VINES_PLANT
         || block == Blocks.TWISTING_VINES
         || block == Blocks.TWISTING_VINES_PLANT;
   }

   public List<CenterPos> findDripstone(WorldChunk chunk, int startX, int startZ) {
      ArrayList arrayList = new ArrayList();

      for (int i = startX; i < startX + 16; i += 4) {
         for (int j = startZ; j < startZ + 16; j += 4) {
            int k = chunk.getHeightmap(Type.MOTION_BLOCKING).get(i - startX, j - startZ);

            for (byte b0 = -64; b0 <= k; b0 += 4) {
               BlockPos blockPos = new BlockPos(i, b0, j);
               if (chunk.getBlockState(blockPos).getBlock() instanceof PointedDripstoneBlock) {
                  arrayList.add(new CenterPos(blockPos));
                  break;
               }
            }
         }
      }

      return arrayList;
   }

   public List<PosRecord> findBerries(WorldChunk chunk, int startX, int startZ) {
      ArrayList arrayList = new ArrayList();

      for (int i = startX; i < startX + 16; i += 2) {
         for (int j = startZ; j < startZ + 16; j += 2) {
            int k = chunk.getHeightmap(Type.MOTION_BLOCKING).get(i - startX, j - startZ);

            for (int l = k; l >= k - 5; l--) {
               BlockPos blockPos = new BlockPos(i, l, j);
               BlockState blockState = chunk.getBlockState(blockPos);
               if (blockState.getBlock() instanceof SweetBerryBushBlock) {
                  try {
                     if (blockState.get(SweetBerryBushBlock.AGE) == 3) {
                        arrayList.add(new PosRecord(blockPos));
                     }
                  } catch (Exception exception) {
                  }
               }
            }
         }
      }

      return arrayList;
   }

   public BlockPos weightedCentroid(List<WeightedPos> list, ChunkPos chunkPos) {
      if (list.isEmpty()) {
         return new BlockPos(chunkPos.getStartX() + 8, 30, chunkPos.getStartZ() + 8);
      } else {
         double d0 = 0.0;
         double d1 = 0.0;
         double d2 = 0.0;
         double d3 = 0.0;

         for (WeightedPos weightedPos : list) {
            d0 += weightedPos.pos().getX() * weightedPos.weight();
            d1 += weightedPos.pos().getY() * weightedPos.weight();
            d2 += weightedPos.pos().getZ() * weightedPos.weight();
            d3 += weightedPos.weight();
         }

         return new BlockPos((int)(d0 / d3), (int)(d1 / d3), (int)(d2 / d3));
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         if (!this.suspiciousChunks.isEmpty()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               int i = Math.max(0, Math.min(255, this.alpha.getValue().intValue()));
               matrices.push();
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

               for (ChunkSuspicion chunkSuspicion : this.suspiciousChunks.values()) {
                  boolean flag = !chunkSuspicion.extreme() && !chunkSuspicion.source();
                  if (!flag || this.renderStandardChunks.getValue()) {
                     Color color;
                     if (chunkSuspicion.extreme()) {
                        color = EXTREME_PLATE_COLOR_BASE;
                     } else if (chunkSuspicion.source()) {
                        color = SOURCE_PLATE_COLOR_BASE;
                     } else if (chunkSuspicion.suspicionLevel() < 5 && chunkSuspicion.maxVineLength() < 4) {
                        color = LOW_SUSPICION_GRAY;
                     } else {
                        color = HIGH_SUSPICION_GRAY;
                     }

                     Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), i);
                     double d0 = chunkSuspicion.chunkPos().getStartX();
                     double d1 = chunkSuspicion.chunkPos().getStartZ();
                     double d2 = 30.0;
                     double d3 = d0 - vec3d.x;
                     double d4 = d1 - vec3d.z;
                     double d5 = d0 + 16.0 - vec3d.x;
                     double d6 = d1 + 16.0 - vec3d.z;
                     double d7 = d2 - vec3d.y;
                     double d8 = d2 + 0.08 - vec3d.y;
                     shapeBatch.renderFilledBox(d3, d7, d4, d5, d8, d6, color1);
                  }
               }

               shapeBatch.flush();
               matrices.pop();
            }
         }
      }
   }
}
