package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
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

public final class FutureDebug extends Module {
   public static final int BASE_CHEST_THRESHOLD = 10;
   public static final double CHUNK_THICKNESS = 0.1;
   public final Setting<Integer> scanRadius = new Setting<>("Scan Radius", 1, 1, 5);
   public final Setting<Boolean> smartCheck = new Setting<>("Smart Check", true);
   public final Setting<Integer> sensitivity = new Setting<>("Sensitivity", 5, 1, 10);
   public final Setting<Color> fillColor = new Setting<>("Fill Color", new Color(180, 60, 60, 40));
   public final Setting<Integer> fillAlpha = new Setting<>("Fill Alpha", 40, 0, 255);
   public final Map<Long, Long> confirmedChunks = new ConcurrentHashMap<>();
   public final Set<ChunkPos> baseHits = ConcurrentHashMap.newKeySet();
   public ExecutorService scanExec;
   public final AtomicBoolean scanning = new AtomicBoolean(false);
   public int tickCount = 0;

   public FutureDebug() {
      super("FutureDebug", Category.RENDER);
      this.addSetting(this.scanRadius);
      this.addSetting(this.smartCheck);
      this.addSetting(this.sensitivity);
      this.addSetting(this.fillColor);
      this.addSetting(this.fillAlpha);
   }

   @Override
   public void onEnable() {
      this.confirmedChunks.clear();
      this.baseHits.clear();
      this.tickCount = 0;
      this.scanning.set(false);
   }

   @Override
   public void onDisable() {
      this.confirmedChunks.clear();
      this.baseHits.clear();
      if (this.scanExec != null) {
         this.scanExec.shutdownNow();
      }
   }

   public int getMinClusterSize() {
      return (int)(40.0 - (this.sensitivity.getValue() - 1) * 3.5555555555555554);
   }

   public float getDensityThreshold() {
      return 0.015F - (this.sensitivity.getValue() - 1) * 0.0013333333F;
   }

   public int getMinBlockCount() {
      return (int)(15.0 - (this.sensitivity.getValue() - 1) * 1.3333333333333333);
   }

   public float getRatioThreshold() {
      return 0.08F + (this.sensitivity.getValue() - 1) * 0.027F;
   }

   public float getScoreThreshold() {
      return 6.0F + (this.sensitivity.getValue() - 1) * 0.44444445F;
   }

   public boolean hasAmethystCluster(WorldChunk chunk) {
      ChunkSection[] achunksection = chunk.getSectionArray();
      int i = chunk.getBottomY();
      int j = 0;
      int k = 0;
      int l = 0;
      int i1 = 0;
      int j1 = 0;
      int k1 = 0;
      int l1 = 0;
      int i2 = 0;
      ArrayList arrayList = new ArrayList();
      byte[][][] abyte = new byte[16][128][16];

      for (int j2 = 0; j2 < achunksection.length; j2++) {
         int k2 = i + j2 * 16;
         if (k2 <= 48 && k2 + 16 >= -64) {
            ChunkSection chunkSection = achunksection[j2];
            if (chunkSection != null && !chunkSection.isEmpty()) {
               for (int l2 = 0; l2 < 16; l2++) {
                  for (int i3 = 0; i3 < 16; i3++) {
                     int j3 = k2 + i3;
                     if (j3 >= -64 && j3 <= 48) {
                        int k3 = j3 + 64;
                        if (k3 >= 0 && k3 < 128) {
                           for (int l3 = 0; l3 < 16; l3++) {
                              BlockState blockState = chunkSection.getBlockState(l2, i3, l3);
                              if (blockState.isOf(Blocks.BUDDING_AMETHYST)) {
                                 return false;
                              }

                              if (this.isAmethyst(blockState)) {
                                 abyte[l2][k3][l3] = 1;
                                 j++;
                                 k1 += l2;
                                 l1 += j3;
                                 i2 += l3;
                                 arrayList.add(new int[]{l2, j3, l3});
                              } else if (blockState.isOf(Blocks.CALCITE)) {
                                 abyte[l2][k3][l3] = 2;
                                 k++;
                              } else if (blockState.isOf(Blocks.SMOOTH_BASALT)) {
                                 abyte[l2][k3][l3] = 3;
                                 l++;
                              } else if (!blockState.isAir() && !blockState.isOf(Blocks.CAVE_AIR) && !blockState.isOf(Blocks.VOID_AIR)) {
                                 abyte[l2][k3][l3] = 5;
                              } else {
                                 abyte[l2][k3][l3] = 4;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      if (j == 0) {
         return false;
      } else if (!this.smartCheck.getValue()) {
         return j >= this.getMinClusterSize();
      } else {
         int l5 = Integer.MAX_VALUE;
         int i6 = Integer.MIN_VALUE;

         for (int[] aint : (Iterable<int[]>)arrayList) {
            if (aint[1] < l5) {
               l5 = aint[1];
            }

            if (aint[1] > i6) {
               i6 = aint[1];
            }
         }

         float f3 = j / (256.0F * Math.max(1, i6 - l5 + 1));
         float f4 = (float)k1 / j;
         float f5 = (float)l1 / j;
         float f6 = (float)i2 / j;
         float f7 = 0.0F;

         for (int[] aint2 : (Iterable<int[]>)arrayList) {
            float f = aint2[0] - f4;
            float f1 = aint2[1] - f5;
            float f2 = aint2[2] - f6;
            f7 += (float)Math.sqrt(f * f + f1 * f1 + f2 * f2);
         }

         f7 /= j;
         int[] aint1 = new int[]{1, -1, 0, 0, 0, 0};
         int[] aint3 = new int[]{0, 0, 1, -1, 0, 0};
         int[] aint4 = new int[]{0, 0, 0, 0, 1, -1};

         for (int[] aint5 : (Iterable<int[]>)arrayList) {
            int i4 = aint5[0];
            int j4 = aint5[1] + 64;
            int k4 = aint5[2];

            for (int l4 = 0; l4 < 6; l4++) {
               int i5 = i4 + aint1[l4];
               int j5 = j4 + aint3[l4];
               int k5 = k4 + aint4[l4];
               if (i5 >= 0 && i5 <= 15 && j5 >= 0 && j5 < 128 && k5 >= 0 && k5 <= 15) {
                  byte b0 = abyte[i5][j5][k5];
                  if (b0 == 4) {
                     i1++;
                  } else if (b0 >= 2) {
                     j1++;
                  }
               }
            }
         }

         int j6 = i1 + j1;
         float f8 = j6 > 0 ? (float)i1 / j6 : 0.0F;
         return j >= this.getMinClusterSize() && f3 >= this.getDensityThreshold() && k + l >= this.getMinBlockCount() && f7 <= this.getScoreThreshold() && f8 <= this.getRatioThreshold();
      }
   }

   public boolean isAmethyst(BlockState state) {
      return state.isOf(Blocks.AMETHYST_CLUSTER)
         || state.isOf(Blocks.LARGE_AMETHYST_BUD)
         || state.isOf(Blocks.MEDIUM_AMETHYST_BUD)
         || state.isOf(Blocks.SMALL_AMETHYST_BUD)
         || state.isOf(Blocks.AMETHYST_BLOCK);
   }

   public boolean hasEnoughAmethyst(WorldChunk chunk) {
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
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         ChunkPos chunkPos = mc.player.getChunkPos();
         int i = this.scanRadius.getValue() * 5;
         this.confirmedChunks.keySet().removeIf(var2x -> {
            int i1 = ChunkPos.getPackedX(var2x);
            int j1 = ChunkPos.getPackedZ(var2x);
            return Math.abs(i1 - chunkPos.x) > i + 3 || Math.abs(j1 - chunkPos.z) > i + 3;
         });
         this.baseHits.removeIf(var2x -> Math.abs(var2x.x - chunkPos.x) > i + 2 || Math.abs(var2x.z - chunkPos.z) > i + 2);
         if (++this.tickCount % 5 == 0) {
            if (this.scanning.compareAndSet(false, true)) {
               if (this.scanExec == null || this.scanExec.isShutdown()) {
                  this.scanExec = Executors.newSingleThreadExecutor(var0 -> {
                     Thread thread = new Thread(var0, "futuredebug-scan");
                     thread.setDaemon(true);
                     return thread;
                  });
               }

               ArrayList arrayList = new ArrayList();
               ArrayList arrayList1 = new ArrayList();

               for (int j = -i; j <= i; j++) {
                  for (int k = -i; k <= i; k++) {
                     ChunkPos chunkPos1 = new ChunkPos(chunkPos.x + j, chunkPos.z + k);
                     WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos1.x, chunkPos1.z, false);
                     if (worldChunk != null && !worldChunk.isEmpty()) {
                        arrayList.add(chunkPos1);
                        arrayList1.add(worldChunk);
                     }
                  }
               }

               long l = System.currentTimeMillis();
               this.scanExec.submit(() -> {
                  try {
                     for (int i1 = 0; i1 < arrayList.size(); i1++) {
                        ChunkPos chunkPos2 = (ChunkPos)arrayList.get(i1);
                        WorldChunk worldChunk1 = (WorldChunk)arrayList1.get(i1);
                        long j1 = chunkPos2.toLong();
                        if (this.hasAmethystCluster(worldChunk1) && this.confirmedChunks.isEmpty()) {
                           this.confirmedChunks.put(j1, l);
                           break;
                        }

                        if (this.hasEnoughAmethyst(worldChunk1)) {
                           this.baseHits.add(chunkPos2);
                        }
                     }
                  } catch (Exception exception) {
                  } finally {
                     this.scanning.set(false);
                  }
               });
            }
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         if (!this.confirmedChunks.isEmpty() || !this.baseHits.isEmpty()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = camera.getCameraPos();
               double d0 = 63.0 - vec3d.y;
               double d1 = d0 + 0.1;
               Color color = this.fillColor.getValue();
               Color color1 = new Color(color.getRed(), color.getGreen(), color.getBlue(), this.fillAlpha.getValue());
               matrices.push();
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

               for (long i : this.confirmedChunks.keySet()) {
                  ChunkPos chunkPos = new ChunkPos(ChunkPos.getPackedX(i), ChunkPos.getPackedZ(i));
                  double d2 = (chunkPos.x << 4) + 8.0 - vec3d.x;
                  double d3 = (chunkPos.z << 4) + 8.0 - vec3d.z;
                  double d4 = 42.0;
                  shapeBatch.renderFilledBox(d2 - d4, d0, d3 - d4, d2 + d4, d1, d3 + d4, color1);
               }

               shapeBatch.flush();
               matrices.pop();
            }
         }
      }
   }
}
