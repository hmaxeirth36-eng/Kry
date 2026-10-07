package com.water.module.modules.render;

import com.water.gui.NotificationManager;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Water;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class TuffChunkFinder extends Module {
   public static final double CHUNK_THICKNESS = 0.01;
   public static final double RENDER_Y = 63.0;
   public static final long COOLDOWN_MS = 50000L;
   public static final long SCAN_INTERVAL_MS = 20000L;
   public static final long REPEATER_SCAN_INTERVAL_MS = 200L;
   public static final float SCAN_DURATION_MS = 800.0F;
   public static final float SCAN_MAX_RADIUS = 140.0F;
   public final ConcurrentHashMap<ChunkPos, String> hiveChunks = new ConcurrentHashMap<>();
   public final AtomicBoolean scanning = new AtomicBoolean(false);
   public ExecutorService executor;
   public long lastMarkTime = 0L;
   public long lastScanTime = 0L;
   public long lastRepScanTime = 0L;
   public long scanStartTime = 0L;
   public boolean scanActive = false;

   public TuffChunkFinder() {
      super("Tuff Chunk Finder", Category.RENDER);
   }

   @Override
   public void onEnable() {
      this.hiveChunks.clear();
      this.lastMarkTime = 0L;
      this.lastScanTime = 0L;
      this.scanActive = false;
   }

   @Override
   public void onDisable() {
      this.hiveChunks.clear();
      if (this.executor != null) {
         this.executor.shutdownNow();
      }

      this.scanning.set(false);
      this.scanActive = false;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if (this.scanActive && (float)(System.currentTimeMillis() - this.scanStartTime) > 800.0F) {
            this.scanActive = false;
         }

         long i = System.currentTimeMillis();
         if (i - this.lastRepScanTime >= 200L && !this.scanning.get()) {
            this.lastRepScanTime = i;
            ChunkPos chunkPos = mc.player.getChunkPos();
            int j = Math.min(mc.options.getClampedViewDistance(), 8);
            ArrayList arrayList = new ArrayList();
            ArrayList arrayList1 = new ArrayList();

            for (int k = -j; k <= j; k++) {
               for (int l = -j; l <= j; l++) {
                  ChunkPos chunkPos1 = new ChunkPos(chunkPos.x + k, chunkPos.z + l);
                  WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos1.x, chunkPos1.z, false);
                  if (worldChunk != null && !worldChunk.isEmpty()) {
                     arrayList.add(chunkPos1);
                     arrayList1.add(worldChunk);
                  }
               }
            }

            if (this.executor == null || this.executor.isShutdown()) {
               this.executor = Executors.newSingleThreadExecutor(var0 -> {
                  Thread thread = new Thread(var0, "tuff-scan");
                  thread.setDaemon(true);
                  return thread;
               });
            }

            this.executor.submit(() -> {
               for (int l1 = 0; l1 < arrayList.size(); l1++) {
                  WorldChunk worldChunk2 = (WorldChunk)arrayList1.get(l1);
                  int i2 = 0;

                  for (ChunkSection chunkSection : worldChunk2.getSectionArray()) {
                     if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(var0 -> var0.isOf(Blocks.REPEATER))) {
                        for (int j2 = 0; j2 < 16; j2++) {
                           for (int k2 = 0; k2 < 16; k2++) {
                              for (int l2 = 0; l2 < 16; l2++) {
                                 BlockState blockState = chunkSection.getBlockState(j2, l2, k2);
                                 if (blockState.isOf(Blocks.REPEATER) && blockState.get(RepeaterBlock.POWERED)) {
                                    if (++i2 >= 3) {
                                       this.hiveChunks.clear();
                                       this.hiveChunks.put((ChunkPos)arrayList.get(l1), "repeater");
                                       return;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            });
         }

         if (i - this.lastScanTime >= 20000L) {
            if (this.scanning.compareAndSet(false, true)) {
               if (this.executor == null || this.executor.isShutdown()) {
                  this.executor = Executors.newSingleThreadExecutor(var0 -> {
                     Thread thread = new Thread(var0, "tuff-chunk-scan");
                     thread.setDaemon(true);
                     return thread;
                  });
               }

               this.lastScanTime = i;
               this.scanActive = true;
               this.scanStartTime = i;
               ChunkPos chunkPos2 = mc.player.getChunkPos();
               int i1 = Math.min(mc.options.getClampedViewDistance(), 8);
               ArrayList arrayList2 = new ArrayList();
               ArrayList arrayList3 = new ArrayList();

               for (int j1 = -i1; j1 <= i1; j1++) {
                  for (int k1 = -i1; k1 <= i1; k1++) {
                     ChunkPos chunkPos3 = new ChunkPos(chunkPos2.x + j1, chunkPos2.z + k1);
                     WorldChunk worldChunk1 = mc.world.getChunkManager().getWorldChunk(chunkPos3.x, chunkPos3.z, false);
                     if (worldChunk1 != null && !worldChunk1.isEmpty()) {
                        arrayList2.add(chunkPos3);
                        arrayList3.add(worldChunk1);
                     }
                  }
               }

               this.executor
                  .submit(
                     () -> {
                        try {
                           ChunkPos chunkPos4 = null;

                           label646:
                           for (int l1 = 0; l1 < arrayList2.size(); l1++) {
                              WorldChunk worldChunk2 = (WorldChunk)arrayList3.get(l1);
                              int i2 = 0;

                              for (ChunkSection chunkSection : worldChunk2.getSectionArray()) {
                                 if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(var0 -> var0.isOf(Blocks.REPEATER))) {
                                    for (int j2 = 0; j2 < 16; j2++) {
                                       for (int k2 = 0; k2 < 16; k2++) {
                                          for (int l2 = 0; l2 < 16; l2++) {
                                             BlockState blockState = chunkSection.getBlockState(j2, l2, k2);
                                             if (blockState.isOf(Blocks.REPEATER) && blockState.get(RepeaterBlock.POWERED)) {
                                                if (++i2 >= 3) {
                                                   chunkPos4 = (ChunkPos)arrayList2.get(l1);
                                                   break label646;
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }

                           if (chunkPos4 != null) {
                              this.lastMarkTime = System.currentTimeMillis();
                              this.hiveChunks.clear();
                              this.hiveChunks.put(chunkPos4, "repeater");
                              return;
                           }

                           ChunkPos chunkPos5 = null;
                           String s = "normal";

                           for (int j4 = 0; j4 < arrayList2.size(); j4++) {
                              ChunkPos chunkPos6 = (ChunkPos)arrayList2.get(j4);
                              WorldChunk worldChunk3 = (WorldChunk)arrayList3.get(j4);
                              boolean flag1 = false;

                              for (BlockEntity blockEntity : worldChunk3.getBlockEntities().values()) {
                                 BlockState blockState2 = worldChunk3.getBlockState(blockEntity.getPos());
                                 if ((blockState2.isOf(Blocks.BEEHIVE) || blockState2.isOf(Blocks.BEE_NEST))
                                    && blockEntity instanceof BeehiveBlockEntity beehiveBlockEntity
                                    && beehiveBlockEntity.getBeeCount() > 0) {
                                    flag1 = true;
                                    break;
                                 }
                              }

                              boolean flag2 = false;
                              if (!flag1) {
                                 int l4 = 0;
                                 ChunkSection[] achunksection = worldChunk3.getSectionArray();
                                 int j5 = worldChunk3.getBottomY();

                                 label591:
                                 for (int l5 = 0; l5 < achunksection.length; l5++) {
                                    int i3 = j5 + l5 * 16;
                                    if (i3 > 20) {
                                       break;
                                    }

                                    if (i3 + 16 >= 0) {
                                       ChunkSection chunkSection1 = achunksection[l5];
                                       if (chunkSection1 != null
                                          && !chunkSection1.isEmpty()
                                          && chunkSection1.hasAny(var0 -> var0.isOf(Blocks.COBBLED_DEEPSLATE))) {
                                          for (int j3 = 0; j3 < 16; j3++) {
                                             for (int k3 = 0; k3 < 16; k3++) {
                                                for (int l3 = 0; l3 < 16; l3++) {
                                                   int i4 = i3 + l3;
                                                   if (i4 >= 0 && i4 <= 20 && chunkSection1.getBlockState(j3, l3, k3).isOf(Blocks.COBBLED_DEEPSLATE)) {
                                                      if (++l4 >= 50) {
                                                         flag2 = true;
                                                         break label591;
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              boolean flag3 = false;
                              if (!flag1) {
                                 int i5 = 0;

                                 for (ChunkSection chunkSection2 : worldChunk3.getSectionArray()) {
                                    if (chunkSection2 != null && !chunkSection2.isEmpty() && chunkSection2.hasAny(var0 -> var0.isOf(Blocks.VINE))) {
                                       for (int l6 = 0; l6 < 16; l6++) {
                                          for (int i7 = 0; i7 < 16; i7++) {
                                             for (int k7 = 0; k7 < 16; k7++) {
                                                if (chunkSection2.getBlockState(l6, k7, i7).isOf(Blocks.VINE)) {
                                                   if (++i5 >= 150) {
                                                      flag3 = true;
                                                      break;
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              boolean flag4 = false;
                              if (!flag1 && !flag3) {
                                 int k5 = 0;
                                 ChunkSection[] achunksection1 = worldChunk3.getSectionArray();
                                 int j6 = worldChunk3.getBottomY();

                                 for (int k6 = 0; k6 < achunksection1.length && j6 + k6 * 16 <= 70; k6++) {
                                    ChunkSection chunkSection3 = achunksection1[k6];
                                    if (chunkSection3 != null
                                       && !chunkSection3.isEmpty()
                                       && chunkSection3.hasAny(var0 -> var0.isOf(Blocks.SEAGRASS) || var0.isOf(Blocks.TALL_SEAGRASS))) {
                                       for (int j7 = 0; j7 < 16; j7++) {
                                          for (int l7 = 0; l7 < 16; l7++) {
                                             for (int j8 = 0; j8 < 16; j8++) {
                                                BlockState blockState1 = chunkSection3.getBlockState(j7, j8, l7);
                                                if (blockState1.isOf(Blocks.SEAGRASS) || blockState1.isOf(Blocks.TALL_SEAGRASS)) {
                                                   if (++k5 >= 30) {
                                                      flag4 = true;
                                                      break;
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              boolean flag5 = false;
                              if (!flag1 && !flag3 && !flag4 && !flag2) {
                                 int i6 = 0;

                                 label473:
                                 for (ChunkSection chunkSection4 : worldChunk3.getSectionArray()) {
                                    if (chunkSection4 != null && !chunkSection4.isEmpty() && chunkSection4.hasAny(var0 -> var0.isOf(Blocks.REPEATER))) {
                                       for (int i8 = 0; i8 < 16; i8++) {
                                          for (int k8 = 0; k8 < 16; k8++) {
                                             for (int l8 = 0; l8 < 16; l8++) {
                                                if (chunkSection4.getBlockState(i8, l8, k8).isOf(Blocks.REPEATER)) {
                                                   if (++i6 >= 3) {
                                                      flag5 = true;
                                                      break label473;
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }

                              if (flag1 || flag3 || flag4 || flag2 || flag5) {
                                 chunkPos5 = chunkPos6;
                                 s = flag5 ? "repeater" : "normal";
                                 break;
                              }
                           }

                           if (chunkPos5 != null && "repeater".equals(s)) {
                              this.lastMarkTime = System.currentTimeMillis();
                              this.hiveChunks.clear();
                              this.hiveChunks.put(chunkPos5, s);
                              MinecraftClient.getInstance().execute(() -> {
                                 MinecraftClient minecraftClient = MinecraftClient.getInstance();
                                 if (minecraftClient.player != null) {
                                    minecraftClient.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                                    minecraftClient.player.sendMessage(Text.literal("\u00a78\u00a77Repeater chunk found!"), false);
                                 }

                                 NotificationManager.INSTANCE.push("Tuff Chunk Finder", "Repeater chunk found!", Items.REPEATER.getDefaultStack(), Water.getAccentArgb());
                              });
                           } else if (chunkPos5 != null) {
                              boolean flag = System.currentTimeMillis() - this.lastMarkTime >= 50000L;
                              if (flag) {
                                 this.lastMarkTime = System.currentTimeMillis();
                                 this.hiveChunks.clear();
                                 this.hiveChunks.put(chunkPos5, s);
                                 int k4 = 40 + new Random().nextInt(61);
                                 MinecraftClient.getInstance()
                                    .execute(
                                       () -> {
                                          MinecraftClient minecraftClient = MinecraftClient.getInstance();
                                          if (minecraftClient.player != null) {
                                             minecraftClient.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                                             minecraftClient.player
                                                .sendMessage(Text.literal("\u00a78\u00a77Chunk found! \u00a7aBase chance: \u00a7f" + k4 + "%"), false);
                                          }

                                          NotificationManager.INSTANCE
                                             .push("Tuff Chunk Finder", "Base chance: " + k4 + "%", Items.LIME_CONCRETE.getDefaultStack(), Water.getAccentArgb());
                                       }
                                    );
                              }
                           }
                        } finally {
                           this.scanning.set(false);
                        }
                     }
                  );
            }
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            double d0 = 63.0 - vec3d.y;
            double d1 = d0 + 0.01;
            matrices.push();
            GL11.glDisable(2929);

            try {
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);
               if (this.scanActive) {
                  float f = Math.min(1.0F, (float)(System.currentTimeMillis() - this.scanStartTime) / 800.0F);
                  float f1 = 1.0F - (1.0F - f) * (1.0F - f);
                  float f2 = f1 * 140.0F;
                  int i = (int)(120.0F * (1.0F - f));
                  if (i > 0 && f2 > 0.0F) {
                     double d2 = mc.player.getX() - vec3d.x;
                     double d3 = mc.player.getZ() - vec3d.z;
                     Color color = new Color(255, 255, 255, i);
                     float f3 = 5.0F;
                     shapeBatch.renderFilledBox(d2 - f2, d0, d3 - f2, d2 + f2, d1, d3 - f2 + f3, color);
                     shapeBatch.renderFilledBox(d2 - f2, d0, d3 + f2 - f3, d2 + f2, d1, d3 + f2, color);
                     shapeBatch.renderFilledBox(d2 - f2, d0, d3 - f2, d2 - f2 + f3, d1, d3 + f2, color);
                     shapeBatch.renderFilledBox(d2 + f2 - f3, d0, d3 - f2, d2 + f2, d1, d3 + f2, color);
                  }
               }

               for (Entry entry : this.hiveChunks.entrySet()) {
                  ChunkPos chunkPos = (ChunkPos)entry.getKey();
                  String s = (String)entry.getValue();
                  double d5 = (chunkPos.x << 4) + 8.0 - vec3d.x;
                  double d6 = (chunkPos.z << 4) + 8.0 - vec3d.z;
                  double d7 = 32.0;
                  if ("repeater".equals(s)) {
                     shapeBatch.renderFilledBox(d5 - d7, d0, d6 - d7, d5 + d7, d1, d6 + d7, new Color(50, 205, 50, 40));
                     shapeBatch.renderOutlineBox(d5 - d7, d0, d6 - d7, d5 + d7, d1, d6 + d7, new Color(50, 205, 50, 180));
                     double d4 = 8.0;
                     shapeBatch.renderFilledBox(d5 - d4, d0, d6 - d4, d5 + d4, d1, d6 + d4, new Color(50, 205, 50, 100));
                     shapeBatch.renderOutlineBox(d5 - d4, d0, d6 - d4, d5 + d4, d1, d6 + d4, new Color(50, 205, 50, 255));
                  } else {
                     shapeBatch.renderFilledBox(d5 - d7, d0, d6 - d7, d5 + d7, d1, d6 + d7, new Color(50, 205, 50, 120));
                     shapeBatch.renderOutlineBox(d5 - d7, d0, d6 - d7, d5 + d7, d1, d6 + d7, new Color(50, 205, 50, 255));
                  }
               }

               shapeBatch.flush();
            } finally {
               GL11.glEnable(2929);
            }

            matrices.pop();
         }
      }
   }

   public static String watermarkFragment() {
      return "7";
   }
}
