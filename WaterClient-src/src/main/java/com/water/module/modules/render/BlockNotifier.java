package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.BlockListSetting;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class BlockNotifier extends Module {
   public static final int CHUNKS_PER_TICK = 32;
   public static final int RESCAN_INTERVAL_TICKS = 5;
   public static final double BOX_INSET = 0.035;
   public final BlockListSetting blocks = new BlockListSetting("Blocks", Blocks.HOPPER);
   public final Setting<Boolean> chatNotify = new Setting<>("Chat Notify", true);
   public final Setting<Boolean> sound = new Setting<>("Sound", true);
   public final Setting<Boolean> esp = new Setting<>("ESP", true);
   public final Setting<Boolean> tracers = new Setting<>("Tracers", false);
   public final Setting<Integer> scanRadius = new Setting<>("Scan Radius", 8, 1, 16);
   public final Setting<Integer> fillAlpha = new Setting<>("Fill Alpha", 80, 0, 255);
   public final Map<Long, Set<BlockPos>> cached = new ConcurrentHashMap<>();
   public final Map<BlockPos, Block> blockTypes = new ConcurrentHashMap<>();
   public final Set<BlockPos> announced = ConcurrentHashMap.newKeySet();
   public final ArrayDeque<Long> queue = new ArrayDeque<>();
   public final Set<Long> queued = new HashSet<>();
   public final Object queueLock = new Object();
   public volatile Set<Block> targets = Collections.emptySet();
   public long lastBlocksVersion = -1L;
   public int ticks = 0;
   public ChunkPos lastCenter = null;
   public int lastRadius = -1;
   public int lastLoadedCount = -1;
   public int lastTargetHash = 0;

   public BlockNotifier() {
      super("Block Notifier", Category.RENDER);
      this.addSetting(this.blocks);
      this.addSetting(this.chatNotify);
      this.addSetting(this.sound);
      this.addSetting(this.esp);
      this.addSetting(this.tracers);
      this.addSetting(this.scanRadius);
      this.addSetting(this.fillAlpha);
   }

   @Override
   public void onEnable() {
      this.clearCaches();
      this.lastBlocksVersion = -1L;
      this.ticks = 0;
      this.lastCenter = null;
      this.lastRadius = -1;
      this.lastLoadedCount = -1;
      this.lastTargetHash = 0;
      this.rescanAroundPlayer();
   }

   @Override
   public void onDisable() {
      this.clearCaches();
      this.lastCenter = null;
      this.lastRadius = -1;
      this.lastLoadedCount = -1;
      this.lastTargetHash = 0;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         this.refreshTargets();
         if (this.targets.isEmpty()) {
            this.clearCaches();
         } else {
            ChunkPos chunkpos = mc.player.getChunkPos();
            int i = Math.min(this.scanRadius.getValue(), mc.options.getClampedViewDistance());
            int j = this.countLoadedChunks(chunkpos, i);
            int k = this.targets.hashCode();
            boolean flag = this.lastCenter == null || !this.lastCenter.equals(chunkpos);
            boolean flag1 = this.lastRadius != i;
            boolean flag2 = this.lastLoadedCount != j;
            boolean flag3 = this.lastTargetHash != k;
            boolean flag4 = ++this.ticks >= 5;
            if (flag || flag1 || flag2 || flag3 || flag4 || this.queue.isEmpty()) {
               this.ticks = 0;
               this.queueChunks(chunkpos, i);
               this.lastCenter = chunkpos;
               this.lastRadius = i;
               this.lastLoadedCount = j;
               this.lastTargetHash = k;
            }

            for (int l = 0; l < 32; l++) {
               Long olong;
               synchronized (this.queueLock) {
                  olong = this.queue.pollFirst();
                  if (olong != null) {
                     this.queued.remove(olong);
                  }
               }

               if (olong == null) {
                  break;
               }

               int j1 = ChunkPos.getPackedX(olong);
               int i1 = ChunkPos.getPackedZ(olong);
               WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(j1, i1, false);
               if (worldChunk != null) {
                  this.scanChunk(worldChunk);
               }
            }
         }
      }
   }

   public void refreshTargets() {
      long i = this.blocks.getVersion();
      if (i != this.lastBlocksVersion) {
         this.lastBlocksVersion = i;
         this.targets = Set.copyOf(this.blocks.getSelectedBlocks());
         this.clearCaches();
         if (mc.world != null && mc.player != null) {
            this.rescanAroundPlayer();
         }
      }
   }

   public void rescanAroundPlayer() {
      if (mc.world != null && mc.player != null) {
         ChunkPos chunkpos = mc.player.getChunkPos();
         int i = Math.min(this.scanRadius.getValue(), mc.options.getClampedViewDistance());
         this.queueChunks(chunkpos, i);
         this.lastCenter = chunkpos;
         this.lastRadius = i;
         this.lastLoadedCount = this.countLoadedChunks(chunkpos, i);
      }
   }

   public int countLoadedChunks(ChunkPos chunkPos, int radius) {
      int i = 0;

      for (int j = -radius; j <= radius; j++) {
         for (int k = -radius; k <= radius; k++) {
            WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos.x + j, chunkPos.z + k, false);
            if (worldChunk != null && !worldChunk.isEmpty()) {
               i++;
            }
         }
      }

      return i;
   }

   public void queueChunks(ChunkPos chunkPos, int radius) {
      ArrayList<WorldChunk> arrayList = new ArrayList<>();
      HashSet hashSet = new HashSet();

      for (int i = -radius; i <= radius; i++) {
         for (int j = -radius; j <= radius; j++) {
            WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos.x + i, chunkPos.z + j, false);
            if (worldChunk != null && !worldChunk.isEmpty()) {
               arrayList.add(worldChunk);
               hashSet.add(worldChunk.getPos().toLong());
            }
         }
      }

      arrayList.sort(Comparator.comparingInt(var2x -> this.chunkDistanceSq(chunkPos, var2x.getPos())));
      synchronized (this.queueLock) {
         this.queue.removeIf(var1x -> !hashSet.contains(var1x));
         this.queued.retainAll(hashSet);

         for (WorldChunk worldChunk1 : (Iterable<WorldChunk>)arrayList) {
            long k = worldChunk1.getPos().toLong();
            if (this.queued.add(k)) {
               this.queue.addLast(k);
            }
         }
      }

      this.pruneDistantChunks(chunkPos, radius);
   }

   public int chunkDistanceSq(ChunkPos chunkPos, ChunkPos chunkPos2) {
      int i = chunkPos.x - chunkPos2.x;
      int j = chunkPos.z - chunkPos2.z;
      return i * i + j * j;
   }

   public void scanChunk(WorldChunk chunk) {
      Set set = this.targets;
      if (!set.isEmpty() && mc.world != null) {
         ChunkPos chunkpos = chunk.getPos();
         long i = chunkpos.toLong();
         Set set1 = this.cached.get(i);
         HashSet hashSet = new HashSet();
         int j = mc.world.getBottomSectionCoord();
         ChunkSection[] achunksection = chunk.getSectionArray();

         for (int k = 0; k < achunksection.length; k++) {
            int l = (j + k) * 16;
            int i1 = l + 15;
            if (l > -2) {
               break;
            }

            if (i1 >= mc.world.getBottomY()) {
               ChunkSection chunkSection = achunksection[k];
               if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(var1x -> set.contains(var1x.getBlock()))) {
                  for (int j1 = 0; j1 < 16; j1++) {
                     int k1 = l + j1;
                     if (k1 <= -2) {
                        for (int l1 = 0; l1 < 16; l1++) {
                           for (int i2 = 0; i2 < 16; i2++) {
                              BlockState blockState = chunkSection.getBlockState(l1, j1, i2);
                              Block block = blockState.getBlock();
                              if (set.contains(block)) {
                                 BlockPos blockPos = new BlockPos(chunkpos.getStartX() + l1, k1, chunkpos.getStartZ() + i2);
                                 hashSet.add(blockPos);
                                 this.blockTypes.put(blockPos, block);
                                 if (this.announced.add(blockPos)) {
                                    this.announceFound(block, blockPos);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (set1 != null) {
            for (BlockPos blockPos1 : (Iterable<BlockPos>)set1) {
               if (!hashSet.contains(blockPos1)) {
                  this.blockTypes.remove(blockPos1);
                  this.announced.remove(blockPos1);
               }
            }
         }

         if (hashSet.isEmpty()) {
            this.cached.remove(i);
         } else {
            this.cached.put(i, hashSet);
         }
      }
   }

   public void announceFound(Block block, BlockPos pos) {
      String s = this.getBlockName(block) + " found (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
      if (this.chatNotify.getValue() && mc.inGameHud != null) {
         mc.inGameHud.getChatHud().addMessage(Text.literal("\u00a7 \u00a7f" + s));
      }

      if (this.sound.getValue() && mc.world != null && mc.player != null) {
         mc.world
            .playSound(
               mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.55F, 1.1F
            );
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.cached.isEmpty()) {
         if (this.esp.getValue() || this.tracers.getValue()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);
               boolean flag = false;
               matrices.push();
               GL11.glDisable(2929);

               try {
                  for (Set set : this.cached.values()) {
                     for (BlockPos blockPos : (Iterable<BlockPos>)set) {
                        Block block = this.blockTypes.get(blockPos);
                        if (block != null && this.targets.contains(block) && mc.world.getBlockState(blockPos).isOf(block)) {
                           Color color = this.getBlockColor(block, this.fillAlpha.getValue());
                           Color color1 = this.getBlockColor(block, 255);
                           double d0 = blockPos.getX() - vec3d.x;
                           double d1 = blockPos.getY() - vec3d.y;
                           double d2 = blockPos.getZ() - vec3d.z;
                           if (this.esp.getValue()) {
                              shapeBatch.renderFilledBox(d0 + 0.035, d1 + 0.035, d2 + 0.035, d0 + 1.0 - 0.035, d1 + 1.0 - 0.035, d2 + 1.0 - 0.035, color);
                              shapeBatch.renderOutlineBox(d0 + 0.035, d1 + 0.035, d2 + 0.035, d0 + 1.0 - 0.035, d1 + 1.0 - 0.035, d2 + 1.0 - 0.035, color1);
                           }

                           if (this.tracers.getValue()) {
                              shapeBatch.renderLine(color1, new Vec3d(0.0, 0.0, 0.0), new Vec3d(d0 + 0.5, d1 + 0.5, d2 + 0.5), 1.0F);
                           }

                           flag = true;
                        }
                     }
                  }

                  if (flag) {
                     shapeBatch.flush();
                  }
               } finally {
                  GL11.glEnable(2929);
                  matrices.pop();
               }
            }
         }
      }
   }

   public void pruneDistantChunks(ChunkPos chunkPos, int radius) {
      ArrayList arrayList = new ArrayList();

      for (Long olong : this.cached.keySet()) {
         ChunkPos chunkpos = new ChunkPos(ChunkPos.getPackedX(olong), ChunkPos.getPackedZ(olong));
         if (Math.abs(chunkpos.x - chunkPos.x) > radius || Math.abs(chunkpos.z - chunkPos.z) > radius) {
            arrayList.add(olong);
         }
      }

      for (Long olong1 : (Iterable<Long>)arrayList) {
         Set set = this.cached.remove(olong1);
         if (set != null) {
            for (BlockPos blockPos : (Iterable<BlockPos>)set) {
               this.blockTypes.remove(blockPos);
               this.announced.remove(blockPos);
            }
         }
      }
   }

   public void clearCaches() {
      this.cached.clear();
      this.blockTypes.clear();
      this.announced.clear();
      synchronized (this.queueLock) {
         this.queue.clear();
         this.queued.clear();
      }
   }

   public Color getBlockColor(Block block, int alpha) {
      return new Color(255, 255, 255, alpha);
   }

   public String getBlockName(Block block) {
      try {
         return block.getName().getString();
      } catch (Throwable throwable) {
         Identifier identifier = Registries.BLOCK.getId(block);
         return identifier == null ? "Block" : identifier.toString();
      }
   }
}
