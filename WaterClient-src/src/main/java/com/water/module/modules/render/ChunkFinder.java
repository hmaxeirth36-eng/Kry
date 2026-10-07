package com.water.module.modules.render;

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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class ChunkFinder extends Module {
   public static final int BASE_CHEST_THRESHOLD = 10;
   public static final double CHUNK_THICKNESS = 0.1;
   public static final double FLASH_THICKNESS = 0.3;
   public static final long FLASH_ON_MS = 150L;
   public static final long FLASH_CYCLE_MS = 400L;
   public final Setting<Integer> scanRadius = new Setting<>("Scan Radius", 1, 1, 5);
   public final Setting<Integer> clusterThreshold = new Setting<>("Sim Chunks", 10, 1, 10);
   public final Setting<Color> fillColor = new Setting<>("Fill Color", new Color(180, 60, 60, 40));
   public final Setting<Integer> fillAlpha = new Setting<>("Fill Alpha", 40, 0, 255);
   public final Set<ChunkPos> amethystHits = ConcurrentHashMap.newKeySet();
   public final Set<ChunkPos> baseHits = ConcurrentHashMap.newKeySet();
   public volatile Set<ChunkPos> amethystRenderCache = Collections.emptySet();
   public volatile long flashAnchorMs = 0L;
   public ExecutorService scanExec;
   public final AtomicBoolean scanning = new AtomicBoolean(false);
   public int tickCount = 0;

   public ChunkFinder() {
      super("Chunk Finder", Category.RENDER);
      this.addSetting(this.scanRadius);
      this.addSetting(this.clusterThreshold);
      this.addSetting(this.fillColor);
      this.addSetting(this.fillAlpha);
   }

   @Override
   public void onEnable() {
      this.clearScanState();
   }

   @Override
   public void onDisable() {
      this.clearScanState();
      if (this.scanExec != null) {
         this.scanExec.shutdownNow();
      }
   }

   public void clearScanState() {
      this.amethystHits.clear();
      this.baseHits.clear();
      this.amethystRenderCache = Collections.emptySet();
      this.tickCount = 0;
      this.scanning.set(false);
      this.flashAnchorMs = 0L;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         ChunkPos chunkpos = mc.player.getChunkPos();
         int i = this.scanRadius.getValue() * 5;
         this.amethystHits.removeIf(var2x -> Math.abs(var2x.x - chunkpos.x) > i + 2 || Math.abs(var2x.z - chunkpos.z) > i + 2);
         this.baseHits.removeIf(var2x -> Math.abs(var2x.x - chunkpos.x) > i + 2 || Math.abs(var2x.z - chunkpos.z) > i + 2);
         if (++this.tickCount % 5 == 0) {
            if (this.scanning.compareAndSet(false, true)) {
               if (this.scanExec == null || this.scanExec.isShutdown()) {
                  this.scanExec = Executors.newSingleThreadExecutor(var0 -> {
                     Thread thread = new Thread(var0, "chunkfinder-scan");
                     thread.setDaemon(true);
                     return thread;
                  });
               }

               ArrayList arrayList = new ArrayList();
               ArrayList arrayList1 = new ArrayList();
               int j = this.clusterThreshold.getValue();

               for (int k = -i; k <= i; k++) {
                  for (int l = -i; l <= i; l++) {
                     ChunkPos chunkPos1 = new ChunkPos(chunkpos.x + k, chunkpos.z + l);
                     WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos1.x, chunkPos1.z, false);
                     if (worldChunk != null && !worldChunk.isEmpty()) {
                        arrayList.add(chunkPos1);
                        arrayList1.add(worldChunk);
                     }
                  }
               }

               this.scanExec.submit(() -> {
                  try {
                     HashMap hashMap = new HashMap();
                     HashSet hashSet = new HashSet();

                     for (int i1 = 0; i1 < arrayList.size(); i1++) {
                        ChunkPos chunkpos2 = (ChunkPos)arrayList.get(i1);
                        WorldChunk worldChunk1 = (WorldChunk)arrayList1.get(i1);
                        int j1 = this.countAmethyst(worldChunk1);
                        if (j1 >= j) {
                           hashMap.put(chunkpos2, j1);
                        }

                        if (this.hasBaseSignature(worldChunk1)) {
                           hashSet.add(chunkpos2);
                        }
                     }

                     Set set = this.pickTopChunks(hashMap, chunkpos, j);
                     Set set1 = this.applyShapeFilter(set, j);
                     boolean flag = mc.player != null && mc.player.getY() <= -2.0;
                     if (flag) {
                        set1.removeIf(var1xx -> !hashSet.contains(var1xx));
                     }

                     if (!set.equals(this.amethystHits) || !hashSet.equals(this.baseHits) || !set1.equals(this.amethystRenderCache)) {
                        this.amethystRenderCache = Collections.unmodifiableSet(set1);
                     }

                     this.amethystHits.clear();
                     this.amethystHits.addAll(set);
                     this.baseHits.clear();
                     this.baseHits.addAll(hashSet);
                  } catch (Exception exception) {
                  } finally {
                     this.scanning.set(false);
                  }
               });
            }
         }
      }
   }

   public Set<ChunkPos> pickTopChunks(Map<ChunkPos, Integer> map, ChunkPos chunkPos, int var3) {
      ArrayList<Map.Entry<ChunkPos, Integer>> arrayList = new ArrayList<>(map.entrySet());
      arrayList.sort((var2x, var3x) -> {
         int j = Integer.compare((Integer)var3x.getValue(), (Integer)var2x.getValue());
         return j != 0 ? j : Integer.compare(this.chunkDistanceSq(chunkPos, (ChunkPos)var2x.getKey()), this.chunkDistanceSq(chunkPos, (ChunkPos)var3x.getKey()));
      });
      int i = Math.max(3, 33 - var3 * 3);
      LinkedHashSet linkedHashSet = new LinkedHashSet();

      for (Map.Entry<ChunkPos, Integer> entry : arrayList) {
         if (linkedHashSet.size() >= i) {
            break;
         }

         linkedHashSet.add((ChunkPos)entry.getKey());
      }

      return linkedHashSet;
   }

   public int chunkDistanceSq(ChunkPos chunkPos, ChunkPos chunkPos2) {
      int i = chunkPos.x - chunkPos2.x;
      int j = chunkPos.z - chunkPos2.z;
      return i * i + j * j;
   }

   public Set<ChunkPos> applyShapeFilter(Set<ChunkPos> set, int var2) {
      HashSet hashSet = new HashSet();
      if (var2 >= 10) {
         hashSet.addAll(set);
         return hashSet;
      } else {
         int[][] aint = new int[][]{{1, 4}, {2, 4}, {1, 2}, {3, 1}, {5, 4}, {5, 6}};

         for (ChunkPos chunkpos : set) {
            Random random = new Random(Math.abs(chunkpos.hashCode()));
            int[] aint1 = aint[random.nextInt(aint.length)];
            int i = aint1[0];
            int j = aint1[1];
            if (random.nextBoolean()) {
               int k = i;
               i = j;
               j = k;
            }

            int k1 = -(i / 2);
            int l = -(j / 2);

            for (int i1 = 0; i1 < i; i1++) {
               for (int j1 = 0; j1 < j; j1++) {
                  hashSet.add(new ChunkPos(chunkpos.x + k1 + i1, chunkpos.z + l + j1));
               }
            }
         }

         return hashSet;
      }
   }

   public int countAmethyst(WorldChunk chunk) {
      int i = 0;
      ChunkSection[] achunksection = chunk.getSectionArray();
      int j = chunk.getBottomY();

      for (int k = 0; k < achunksection.length; k++) {
         int l = j + k * 16;
         if (l > 32) {
            break;
         }

         ChunkSection chunkSection = achunksection[k];
         if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(this::isAmethyst)) {
            for (int i1 = 0; i1 < 16; i1++) {
               for (int j1 = 0; j1 < 16; j1++) {
                  for (int k1 = 0; k1 < 16; k1++) {
                     if (this.isAmethyst(chunkSection.getBlockState(i1, j1, k1))) {
                        i++;
                     }
                  }
               }
            }
         }
      }

      return i;
   }

   public boolean isAmethyst(BlockState state) {
      return state.isOf(Blocks.AMETHYST_CLUSTER) || state.isOf(Blocks.AMETHYST_BLOCK);
   }

   public boolean hasBaseSignature(WorldChunk chunk) {
      int i = 0;
      ChunkSection[] achunksection = chunk.getSectionArray();
      int j = chunk.getBottomY();

      for (int k = 0; k < achunksection.length; k++) {
         int l = j + k * 16;
         if (l >= 0) {
            break;
         }

         ChunkSection chunkSection = achunksection[k];
         if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(var0 -> var0.isOf(Blocks.CHEST) || var0.isOf(Blocks.TRAPPED_CHEST))) {
            int i1 = Math.min(15, -l - 1);

            for (int j1 = 0; j1 < 16; j1++) {
               for (int k1 = 0; k1 < 16; k1++) {
                  for (int l1 = 0; l1 <= i1; l1++) {
                     BlockState blockState = chunkSection.getBlockState(j1, l1, k1);
                     if (blockState.isOf(Blocks.CHEST) || blockState.isOf(Blocks.TRAPPED_CHEST)) {
                        if (++i >= 10) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         if (!this.amethystRenderCache.isEmpty() || !this.baseHits.isEmpty()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               double d0 = 47.0 - vec3d.y;
               double d1 = d0 + 0.1;
               Color color = this.fillColor.getValue();
               int i = this.fillAlpha.getValue();
               Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), i);
               boolean flag = false;
               int j = 0;
               if (!this.baseHits.isEmpty()) {
                  long k = System.currentTimeMillis();
                  if (this.flashAnchorMs == 0L) {
                     this.flashAnchorMs = k;
                  }

                  long l = (k - this.flashAnchorMs) % 400L;
                  if (l < 150L) {
                     flag = true;
                     float f = (float)l / 150.0F;
                     j = (int)((1.0F - f) * 200.0F);
                  }
               } else {
                  this.flashAnchorMs = 0L;
               }

               matrices.push();
               GL11.glDisable(2929);
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

               for (ChunkPos chunkpos : this.amethystRenderCache) {
                  double d2 = (chunkpos.x << 4) - vec3d.x;
                  double d3 = (chunkpos.z << 4) - vec3d.z;
                  shapeBatch.renderFilledBox(d2, d0, d3, d2 + 16.0, d1, d3 + 16.0, color1);
               }

               if (flag && j > 0) {
                  Color color2 = new Color(255, 255, 255, j);

                  for (ChunkPos chunkPos1 : this.baseHits) {
                     double d5 = (chunkPos1.x << 4) - vec3d.x;
                     double d4 = (chunkPos1.z << 4) - vec3d.z;
                     shapeBatch.renderFilledBox(d5, d0, d4, d5 + 16.0, d1 + 0.3, d4 + 16.0, color2);
                  }
               }

               shapeBatch.flush();
               GL11.glEnable(2929);
               matrices.pop();
            }
         }
      }
   }
}
