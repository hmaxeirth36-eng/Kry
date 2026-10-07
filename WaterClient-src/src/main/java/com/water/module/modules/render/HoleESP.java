package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.util.ChunkMark;
import com.water.util.HoleBox;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class HoleESP extends Module {
   public static final int MAX_CHUNKS_PER_TICK = 200;
   public static final int FIXED_MIN_DEPTH = 7;
   public final Setting<Double> alpha = new Setting<>("Fill Alpha", 60.0, 0.0, 255.0);
   public final Setting<Color> color = new Setting<>("Color", new Color(255, 100, 0));
   public final Setting<Double> range = new Setting<>("Range", 64.0, 16.0, 128.0);
   public final Setting<Boolean> gradientFill = new Setting<>("Gradient Fill", true);
   public final Map<Long, ChunkMark> chunks = new ConcurrentHashMap<>();
   public final Queue<Long> chunkQueue = new ArrayDeque<>();
   public final Set<Long> queuedChunks = ConcurrentHashMap.newKeySet();
   public final Set<HoleBox> holes = ConcurrentHashMap.newKeySet();
   public ExecutorService executor;
   public ClientWorld currentWorld;

   public HoleESP() {
      super("Hole ESP", Category.RENDER);
      this.addSetting(this.alpha);
      this.addSetting(this.color);
      this.addSetting(this.range);
      this.addSetting(this.gradientFill);
   }

   @Override
   public void onEnable() {
      this.currentWorld = mc.world;
      this.startExecutor();
      this.clearCaches();
   }

   @Override
   public void onDisable() {
      this.shutdownExecutor();
      this.clearCaches();
      this.currentWorld = null;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if (mc.world != this.currentWorld) {
            this.currentWorld = mc.world;
            this.clearCaches();
         }

         this.startExecutor();
         this.updateChunkQueue();
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.holes.isEmpty()) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            int i = this.clampAlpha(this.alpha.getValue());
            boolean flag = this.gradientFill.getValue();
            BufferAllocator bufferAllocator = new BufferAllocator(2097152);
            Immediate immediate = VertexConsumerProvider.immediate(bufferAllocator);
            VertexConsumer vertexConsumer = immediate.getBuffer(RenderLayers.debugFilledBox());
            Entry entry = matrices.peek();
            boolean flag1 = false;

            for (HoleBox holeBox : this.holes) {
               if (holeBox.rS()) {
                  Box box = holeBox.box;
                  if (RenderUtils.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ)) {
                     Color colorx = this.color.getValue();
                     Color color1 = this.withAlpha(colorx, i);
                     Box box1 = new Box(box.minX - vec3d.x, box.minY - vec3d.y, box.minZ - vec3d.z, box.maxX - vec3d.x, box.maxY - vec3d.y, box.maxZ - vec3d.z);
                     if (flag) {
                        this.drawBoxFill(vertexConsumer, entry, box1, colorx, i);
                     } else {
                        this.drawBoxOutline(vertexConsumer, entry, box1, this.toArgb(color1));
                     }

                     flag1 = true;
                  }
               }
            }

            if (!flag1) {
               bufferAllocator.close();
            } else {
               boolean flag2 = GL11.glIsEnabled(2929);
               GL11.glDisable(2929);
               GL11.glDepthMask(false);
               immediate.draw();
               GL11.glDepthMask(true);
               if (flag2) {
                  GL11.glEnable(2929);
               }

               bufferAllocator.close();
            }
         }
      }
   }

   public void drawBoxOutline(VertexConsumer buffer, Entry entry, Box box, int argb) {
      float f = (float)box.minX;
      float f1 = (float)box.minY;
      float f2 = (float)box.minZ;
      float f3 = (float)box.maxX;
      float f4 = (float)box.maxY;
      float f5 = (float)box.maxZ;
      this.sc(buffer, entry, f, f1, f2, f3, f1, f2, f3, f1, f5, f, f1, f5, argb);
      this.sc(buffer, entry, f, f4, f2, f, f4, f5, f3, f4, f5, f3, f4, f2, argb);
      this.sc(buffer, entry, f, f1, f2, f, f4, f2, f3, f4, f2, f3, f1, f2, argb);
      this.sc(buffer, entry, f, f1, f5, f3, f1, f5, f3, f4, f5, f, f4, f5, argb);
      this.sc(buffer, entry, f, f1, f2, f, f1, f5, f, f4, f5, f, f4, f2, argb);
      this.sc(buffer, entry, f3, f1, f2, f3, f4, f2, f3, f4, f5, f3, f1, f5, argb);
   }

   public void drawBoxFill(VertexConsumer buffer, Entry entry, Box box, Color color, int argb) {
      double d0 = Math.max(0.001, box.maxY - box.minY);
      int i = Math.max(1, MathHelper.ceil(d0));
      int j = Math.max(6, Math.round(argb * 0.18F));
      float f = (float)box.minX;
      float f1 = (float)box.minZ;
      float f2 = (float)box.maxX;
      float f3 = (float)box.maxZ;
      int k = 0;
      int l = 0;

      for (int i1 = 0; i1 < i; i1++) {
         double d1 = (double)i1 / i;
         double d2 = (double)(i1 + 1) / i;
         float f4 = (float)MathHelper.lerp(d1, box.minY, box.maxY);
         float f5 = (float)MathHelper.lerp(d2, box.minY, box.maxY);
         float f6 = 1.0F - (float)i1 / Math.max(1, i - 1);
         float f7 = 1.0F - (float)(i1 + 1) / Math.max(1, i);
         int j1 = this.toArgb(this.withAlpha(color, Math.max(j, Math.round(argb * f6))));
         int k1 = this.toArgb(this.withAlpha(color, Math.max(j, Math.round(argb * f7))));
         if (i1 == 0) {
            l = j1;
         }

         if (i1 == i - 1) {
            k = k1;
         }

         this.sd(buffer, entry, f, f4, f1, f, f5, f1, f2, f5, f1, f2, f4, f1, j1, k1);
         this.sd(buffer, entry, f, f4, f3, f2, f4, f3, f2, f5, f3, f, f5, f3, j1, k1);
         this.sd(buffer, entry, f, f4, f1, f, f4, f3, f, f5, f3, f, f5, f1, j1, k1);
         this.sd(buffer, entry, f2, f4, f1, f2, f5, f1, f2, f5, f3, f2, f4, f3, j1, k1);
      }

      this.sc(buffer, entry, f, (float)box.maxY, f1, f, (float)box.maxY, f3, f2, (float)box.maxY, f3, f2, (float)box.maxY, f1, k);
      this.sc(buffer, entry, f, (float)box.minY, f1, f2, (float)box.minY, f1, f2, (float)box.minY, f3, f, (float)box.minY, f3, l);
   }

   public void sd(
      VertexConsumer buffer,
      Entry entry,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      float var14,
      int var15,
      int var16
   ) {
      buffer.vertex(entry, var3, var4, var5).color(var15);
      buffer.vertex(entry, var6, var7, var8).color(var16);
      buffer.vertex(entry, var9, var10, var11).color(var16);
      buffer.vertex(entry, var12, var13, var14).color(var15);
   }

   public void sc(
      VertexConsumer buffer,
      Entry entry,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      float var14,
      int var15
   ) {
      buffer.vertex(entry, var3, var4, var5).color(var15);
      buffer.vertex(entry, var6, var7, var8).color(var15);
      buffer.vertex(entry, var9, var10, var11).color(var15);
      buffer.vertex(entry, var12, var13, var14).color(var15);
   }

   public void updateChunkQueue() {
      if (mc.world != null && mc.player != null) {
         for (ChunkMark chunkMark : this.chunks.values()) {
            chunkMark.marked = false;
         }

         int i1 = Math.max(1, this.getScanRadius() / 16);
         int j1 = mc.player.getChunkPos().x;
         int i = mc.player.getChunkPos().z;

         for (int j = j1 - i1; j <= j1 + i1; j++) {
            for (int k = i - i1; k <= i + i1; k++) {
               WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(j, k, false);
               if (worldChunk != null) {
                  long l = ChunkPos.toLong(j, k);
                  ChunkMark chunkMark1 = this.chunks.get(l);
                  if (chunkMark1 != null) {
                     chunkMark1.marked = true;
                  } else if (this.queuedChunks.add(l)) {
                     this.chunkQueue.add(l);
                  }
               }
            }
         }

         this.processChunkQueue();
         this.chunks.entrySet().removeIf(var0 -> !var0.getValue().marked);
         Set set = this.chunks.keySet();
         this.holes.removeIf(var2x -> !this.isBoxInChunks(var2x.box, set));
      }
   }

   public boolean isBoxInChunks(Box box, Set<Long> set) {
      int i = (int)Math.floor(box.getCenter().x) >> 4;
      int j = (int)Math.floor(box.getCenter().z) >> 4;
      return set.contains(ChunkPos.toLong(i, j));
   }

   public void processChunkQueue() {
      if (this.executor != null && mc.world != null) {
         int i = 0;

         while (!this.chunkQueue.isEmpty() && i < 200) {
            Long olong = this.chunkQueue.poll();
            if (olong != null) {
               this.queuedChunks.remove(olong);
               int j = ChunkPos.getPackedX(olong);
               int k = ChunkPos.getPackedZ(olong);
               WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(j, k, false);
               if (worldChunk != null) {
                  this.chunks.put(olong, new ChunkMark(j, k));
                  this.executor.execute(() -> this.scanChunk(worldChunk));
                  i++;
               }
            }
         }
      }
   }

   public void scanChunk(WorldChunk chunk) {
      ClientWorld clientWorld = mc.world;
      if (clientWorld != null && clientWorld == this.currentWorld && this.isEnabled()) {
         ChunkSection[] achunksection = chunk.getSectionArray();
         int i = clientWorld.getBottomY();
         int j = clientWorld.getBottomY() + clientWorld.getHeight();
         int k = i;

         for (ChunkSection chunkSection : achunksection) {
            if (chunkSection != null && !chunkSection.isEmpty()) {
               for (int l = 0; l < 16; l++) {
                  for (int i1 = 0; i1 < 16; i1++) {
                     for (int j1 = 0; j1 < 16; j1++) {
                        int k1 = k + j1;
                        if (k1 > i && k1 < j) {
                           BlockPos blockPos = new BlockPos(chunk.getPos().getStartX() + i1, k1, chunk.getPos().getStartZ() + l);
                           this.checkSingleHole(blockPos);
                           this.checkDoubleHole(blockPos);
                        }
                     }
                  }
               }
            }

            k += 16;
         }
      }
   }

   public void checkSingleHole(BlockPos pos) {
      if (this.isOneByOneHole(pos) && !this.isOneByOneHole(pos.up())) {
         Mutable mutable = pos.mutableCopy();

         while (this.isOneByOneHole(mutable)) {
            mutable.move(Direction.DOWN);
         }

         int i = pos.getY() - mutable.getY();
         if (i >= this.getMinDepth()) {
            Box box = new Box(pos.getX(), mutable.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
            if (!this.overlapsExistingHole(box)) {
               this.holes.add(new HoleBox(box, i, true));
            }
         }
      }
   }

   public void checkDoubleHole(BlockPos pos) {
      if (this.isTwoByOneHoleEast(pos) && !this.isTwoByOneHoleEast(pos.up())) {
         Mutable mutable = pos.mutableCopy();

         while (this.isTwoByOneHoleEast(mutable)) {
            mutable.move(Direction.DOWN);
         }

         int i = pos.getY() - mutable.getY();
         if (i >= this.getMinDepth()) {
            Box box = new Box(pos.getX(), mutable.getY() + 1, pos.getZ(), pos.getX() + 3, pos.getY() + 1, pos.getZ() + 1);
            if (!this.overlapsExistingHole(box)) {
               this.holes.add(new HoleBox(box, i, false));
            }
         }
      }

      if (this.isTwoByOneHoleSouth(pos) && !this.isTwoByOneHoleSouth(pos.up())) {
         Mutable mutable1 = pos.mutableCopy();

         while (this.isTwoByOneHoleSouth(mutable1)) {
            mutable1.move(Direction.DOWN);
         }

         int j = pos.getY() - mutable1.getY();
         if (j >= this.getMinDepth()) {
            Box box1 = new Box(pos.getX(), mutable1.getY() + 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 3);
            if (!this.overlapsExistingHole(box1)) {
               this.holes.add(new HoleBox(box1, j, false));
            }
         }
      }
   }

   public boolean overlapsExistingHole(Box box) {
      for (HoleBox holeBox : this.holes) {
         if (holeBox.box.equals(box) || holeBox.box.intersects(box)) {
            return true;
         }
      }

      return false;
   }

   public boolean isLeaves(BlockState state) {
      return state.getBlock() == Blocks.OAK_LEAVES
         || state.getBlock() == Blocks.SPRUCE_LEAVES
         || state.getBlock() == Blocks.BIRCH_LEAVES
         || state.getBlock() == Blocks.JUNGLE_LEAVES
         || state.getBlock() == Blocks.ACACIA_LEAVES
         || state.getBlock() == Blocks.DARK_OAK_LEAVES
         || state.getBlock() == Blocks.CHERRY_LEAVES
         || state.getBlock() == Blocks.MANGROVE_LEAVES
         || state.getBlock() == Blocks.AZALEA_LEAVES
         || state.getBlock() == Blocks.FLOWERING_AZALEA_LEAVES
         || state.getBlock() == Blocks.GLASS
         || state.getBlock() == Blocks.GLASS_PANE
         || state.getBlock() == Blocks.VINE
         || state.getBlock() == Blocks.CAVE_VINES
         || state.getBlock() == Blocks.CAVE_VINES_PLANT
         || state.getBlock() == Blocks.WEEPING_VINES
         || state.getBlock() == Blocks.WEEPING_VINES_PLANT
         || state.getBlock() == Blocks.TWISTING_VINES
         || state.getBlock() == Blocks.TWISTING_VINES_PLANT
         || state.getBlock() == Blocks.GLOW_LICHEN
         || state.getBlock() == Blocks.HANGING_ROOTS
         || state.getBlock() == Blocks.SPORE_BLOSSOM
         || state.getBlock() == Blocks.BAMBOO
         || state.getBlock() == Blocks.BAMBOO_SAPLING
         || state.getBlock() == Blocks.KELP
         || state.getBlock() == Blocks.KELP_PLANT
         || state.getBlock() == Blocks.SEAGRASS
         || state.getBlock() == Blocks.TALL_SEAGRASS
         || state.getBlock() == Blocks.SHORT_GRASS
         || state.getBlock() == Blocks.TALL_GRASS
         || state.getBlock() == Blocks.FERN
         || state.getBlock() == Blocks.LARGE_FERN
         || state.getBlock() == Blocks.SUGAR_CANE
         || state.getBlock() == Blocks.DEAD_BUSH
         || state.getBlock() == Blocks.SWEET_BERRY_BUSH;
   }

   public boolean isSolidWall(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         BlockState blockState = mc.world.getBlockState(pos);
         return !blockState.isAir() && !this.isLeaves(blockState);
      }
   }

   public boolean isOneByOneHole(BlockPos pos) {
      return this.isReplaceable(pos) && this.isSolidWall(pos.north()) && this.isSolidWall(pos.south()) && this.isSolidWall(pos.east()) && this.isSolidWall(pos.west());
   }

   public boolean isTwoByOneHoleEast(BlockPos pos) {
      return this.isReplaceable(pos)
         && this.isReplaceable(pos.east())
         && this.isReplaceable(pos.east(2))
         && this.isSolidWall(pos.north())
         && this.isSolidWall(pos.south())
         && this.isSolidWall(pos.west())
         && this.isSolidWall(pos.east(3));
   }

   public boolean isTwoByOneHoleSouth(BlockPos pos) {
      return this.isReplaceable(pos)
         && this.isReplaceable(pos.south())
         && this.isReplaceable(pos.south(2))
         && this.isSolidWall(pos.east())
         && this.isSolidWall(pos.west())
         && this.isSolidWall(pos.north())
         && this.isSolidWall(pos.south(3));
   }

   public boolean isReplaceable(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         BlockState blockState = mc.world.getBlockState(pos);
         if (!blockState.isAir()) {
            return false;
         } else {
            BlockState blockState1 = mc.world.getBlockState(pos.down());
            BlockState blockState2 = mc.world.getBlockState(pos.up());
            return !this.isPlantOrVine(blockState1) && !this.isPlantOrVine(blockState2) && !this.isRailOrFence(blockState1) && !this.isRailOrFence(blockState2);
         }
      }
   }

   public boolean isPlantOrVine(BlockState state) {
      return state.getBlock() == Blocks.KELP
         || state.getBlock() == Blocks.KELP_PLANT
         || state.getBlock() == Blocks.SEAGRASS
         || state.getBlock() == Blocks.TALL_SEAGRASS
         || state.getBlock() == Blocks.VINE
         || state.getBlock() == Blocks.CAVE_VINES
         || state.getBlock() == Blocks.CAVE_VINES_PLANT
         || state.getBlock() == Blocks.WEEPING_VINES
         || state.getBlock() == Blocks.WEEPING_VINES_PLANT
         || state.getBlock() == Blocks.TWISTING_VINES
         || state.getBlock() == Blocks.TWISTING_VINES_PLANT
         || state.getBlock() == Blocks.GLOW_LICHEN
         || state.getBlock() == Blocks.HANGING_ROOTS
         || state.getBlock() == Blocks.SPORE_BLOSSOM;
   }

   public boolean isRailOrFence(BlockState state) {
      return state.getBlock() == Blocks.RAIL
         || state.getBlock() == Blocks.POWERED_RAIL
         || state.getBlock() == Blocks.DETECTOR_RAIL
         || state.getBlock() == Blocks.ACTIVATOR_RAIL
         || state.getBlock() == Blocks.OAK_FENCE
         || state.getBlock() == Blocks.DARK_OAK_FENCE
         || state.getBlock() == Blocks.SPRUCE_FENCE
         || state.getBlock() == Blocks.COBWEB;
   }

   public void clearCaches() {
      this.chunks.clear();
      this.chunkQueue.clear();
      this.queuedChunks.clear();
      this.holes.clear();
   }

   public void startExecutor() {
      if (this.executor == null || this.executor.isShutdown()) {
         this.executor = Executors.newFixedThreadPool(2, var0 -> {
            Thread thread = new Thread(var0, "water-hole-esp");
            thread.setDaemon(true);
            return thread;
         });
      }
   }

   public void shutdownExecutor() {
      ExecutorService executorService = this.executor;
      this.executor = null;
      if (executorService != null) {
         executorService.shutdown();

         try {
            if (!executorService.awaitTermination(500L, TimeUnit.MILLISECONDS)) {
               executorService.shutdownNow();
            }
         } catch (InterruptedException interruptedException) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
         }
      }
   }

   public int getScanRadius() {
      return MathHelper.clamp((int)Math.round(this.range.getValue()), 16, 128);
   }

   public int getMinDepth() {
      return 7;
   }

   public int clampAlpha(double alpha) {
      return MathHelper.clamp((int)Math.round(alpha), 0, 255);
   }

   public Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
   }

   public int toArgb(Color color) {
      return color.getAlpha() << 24 | color.getRed() << 16 | color.getGreen() << 8 | color.getBlue();
   }
}
