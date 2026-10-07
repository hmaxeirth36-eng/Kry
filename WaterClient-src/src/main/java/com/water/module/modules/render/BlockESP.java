package com.water.module.modules.render;

import com.water.gui.NotificationManager;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.BlockListSetting;
import com.water.module.setting.Setting;
import com.water.render.EspPoint;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public final class BlockESP extends Module {
   public static final int RESCAN_INTERVAL_TICKS = 200;
   public static final int CHUNKS_PER_TICK = 6;
   public static final long NOTIFY_COOLDOWN_MS = 750L;
   public static final double BOX_INSET = 0.0625;
   public final BlockListSetting blocks = new BlockListSetting("Blocks", Blocks.SPAWNER);
   public final Setting<Boolean> notify = new Setting<>("Notification", true);
   public final Setting<Boolean> tracers = new Setting<>("Tracers", true);
   public final Setting<Double> tracerWidth = new Setting<>("Tracer Weight", 1.0, 0.1, 5.0);
   public final Setting<Boolean> filled = new Setting<>("Filled", true);
   public final Setting<Double> alpha = new Setting<>("Opacity", 220.0, 0.0, 255.0);
   public final Setting<Double> fillAlpha = new Setting<>("Fill Alpha", 100.0, 0.0, 255.0);
   public final Setting<Integer> maxRender = new Setting<>("Max Render", 500, 10, 2000);
   public final Map<Long, Set<BlockPos>> cachedBlocks = new ConcurrentHashMap<>();
   public final Map<BlockPos, Block> posTypeMap = new ConcurrentHashMap<>();
   public final Map<Long, Long> lastNotifiedAt = new ConcurrentHashMap<>();
   public final ArrayDeque<Long> scanQueue = new ArrayDeque<>();
   public final Set<Long> queuedChunks = new HashSet<>();
   public final Object queueLock = new Object();
   public final ConcurrentHashMap<Block, Color> customBlockColors = new ConcurrentHashMap<>();
   public volatile Set<Block> targets = Collections.emptySet();
   public long lastBlocksVersion = -1L;
   public int tickCounter = 0;
   public boolean fullRescanRequested = true;
   public ChunkPos lastCenterChunk;
   public int lastChunkRadius = -1;
   public final List<EspPoint> renderList = new ArrayList<>();

   public BlockESP() {
      super("Block ESP", Category.RENDER);
      this.addSetting(this.blocks);
      this.addSetting(this.notify);
      this.addSetting(this.tracers);
      this.addSetting(this.tracerWidth);
      this.addSetting(this.filled);
      this.addSetting(this.alpha);
      this.addSetting(this.fillAlpha);
      this.addSetting(this.maxRender);
   }

   @Override
   public void onEnable() {
      this.clearCaches();
      this.lastBlocksVersion = -1L;
      this.fullRescanRequested = true;
      this.tickCounter = 0;
      this.lastCenterChunk = null;
      this.lastChunkRadius = -1;
   }

   @Override
   public void onDisable() {
      this.clearCaches();
      this.lastCenterChunk = null;
      this.lastChunkRadius = -1;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         this.refreshTargets();
         if (this.targets.isEmpty()) {
            this.clearCaches();
         } else {
            this.tickCounter++;
            ChunkPos chunkpos = mc.player.getChunkPos();
            int i = this.getViewDistance();
            boolean flag = this.fullRescanRequested || this.tickCounter % 200 == 0;
            if (flag || this.lastCenterChunk == null || !this.lastCenterChunk.equals(chunkpos) || this.lastChunkRadius != i) {
               this.queueNearbyChunks(flag);
               this.fullRescanRequested = false;
               this.lastCenterChunk = chunkpos;
               this.lastChunkRadius = i;
            }

            for (int j = 0; j < 6; j++) {
               Long olong;
               synchronized (this.queueLock) {
                  olong = this.scanQueue.poll();
                  if (olong != null) {
                     this.queuedChunks.remove(olong);
                  }
               }

               if (olong == null) {
                  break;
               }

               int l = ChunkPos.getPackedX(olong);
               int k = ChunkPos.getPackedZ(olong);
               WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(l, k, false);
               if (worldChunk != null) {
                  this.scanChunk(worldChunk);
               }
            }
         }
      }
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (mc.world != null) {
         if (packet instanceof ChunkDataS2CPacket chunkDataS2CPacket) {
            this.enqueueChunk(ChunkPos.toLong(chunkDataS2CPacket.getChunkX(), chunkDataS2CPacket.getChunkZ()), true);
         } else if (packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
            chunkDeltaUpdateS2CPacket.visitUpdates((var1x, var2x) -> this.enqueueChunk(new ChunkPos(var1x).toLong(), true));
         } else if (packet instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
            this.enqueueChunk(new ChunkPos(blockUpdateS2CPacket.getPos()).toLong(), true);
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.cachedBlocks.isEmpty()) {
         Set set = this.targets;
         if (!set.isEmpty()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               Vec3d vec3d1 = RenderUtils.getLookVector(camera);
               Vec3d vec3d2 = Freecam.getTracerOrigin(vec3d, tickDelta);
               Vec3d vec3d3 = vec3d2.equals(vec3d) ? vec3d1.multiply(0.1) : vec3d2.subtract(vec3d);
               int i = clampAlpha((int)Math.round(this.alpha.getValue()));
               int j = clampAlpha((int)Math.round(this.fillAlpha.getValue()));
               int k = this.maxRender.getValue();
               double d0 = this.getMaxRenderDistanceSq();
               double d1 = mc.player.getX();
               double d2 = mc.player.getY();
               double d3 = mc.player.getZ();
               this.renderList.clear();

               for (Set set1 : this.cachedBlocks.values()) {
                  for (BlockPos blockPos : (Iterable<BlockPos>)set1) {
                     double d4 = blockPos.getSquaredDistance(d1, d2, d3);
                     if (!(d4 > d0)) {
                        Block block = this.posTypeMap.get(blockPos);
                        if (block != null && set.contains(block)) {
                           this.renderList
                              .add(new EspPoint(blockPos.getX() - vec3d.x, blockPos.getY() - vec3d.y, blockPos.getZ() - vec3d.z, this.getBlockColor(block, 255), d4));
                        }
                     }
                  }
               }

               if (!this.renderList.isEmpty()) {
                  this.renderList.sort(Comparator.comparingDouble(var0 -> var0.distSq));
                  int l = Math.min(k, this.renderList.size());
                  ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

                  for (int i1 = l - 1; i1 >= 0; i1--) {
                     EspPoint espPoint = this.renderList.get(i1);
                     Color color = withAlpha(espPoint.color, i);
                     shapeBatch.renderOutlineBox(
                        espPoint.x + 0.0625,
                        espPoint.y + 0.0625,
                        espPoint.z + 0.0625,
                        espPoint.x + 1.0 - 0.0625,
                        espPoint.y + 1.0 - 0.0625,
                        espPoint.z + 1.0 - 0.0625,
                        color
                     );
                     if (this.tracers.getValue()) {
                        Vec3d vec3d4 = new Vec3d(espPoint.x + 0.5, espPoint.y + 0.5, espPoint.z + 0.5);
                        shapeBatch.renderLine(color, vec3d3, vec3d4, this.tracerWidth.getValue().floatValue());
                     }
                  }

                  shapeBatch.flush();
                  if (this.filled.getValue() && j > 0) {
                     ShapeBatch shapeBatch1 = RenderUtils.beginShapeBatch(matrices);

                     for (int j1 = l - 1; j1 >= 0; j1--) {
                        EspPoint espPoint1 = this.renderList.get(j1);
                        shapeBatch1.renderFilledBox(
                           espPoint1.x + 0.0625,
                           espPoint1.y + 0.0625,
                           espPoint1.z + 0.0625,
                           espPoint1.x + 1.0 - 0.0625,
                           espPoint1.y + 1.0 - 0.0625,
                           espPoint1.z + 1.0 - 0.0625,
                           withAlpha(espPoint1.color, j)
                        );
                     }

                     shapeBatch1.flush();
                  }
               }
            }
         }
      }
   }

   public static int clampAlpha(int alpha) {
      return Math.max(0, Math.min(255, alpha));
   }

   public void refreshTargets() {
      long i = this.blocks.getVersion();
      if (i != this.lastBlocksVersion) {
         this.lastBlocksVersion = i;
         this.targets = Set.copyOf(this.blocks.getSelectedBlocks());
         this.clearCaches();
         this.fullRescanRequested = true;
      }
   }

   public boolean isSelected(Block block) {
      this.refreshTargets();
      return this.blocks.contains(block);
   }

   public int getSelectedCount() {
      this.refreshTargets();
      return this.blocks.size();
   }

   public Set<Block> getSelectedBlocks() {
      return new LinkedHashSet<>(this.blocks.getSelectedBlocks());
   }

   public boolean isNotifyEnabled() {
      return this.notify.getValue();
   }

   public void setNotifyEnabled(boolean enabled) {
      this.notify.setValue(enabled);
   }

   public boolean isTracersEnabled() {
      return this.tracers.getValue();
   }

   public void setTracersEnabled(boolean enabled) {
      this.tracers.setValue(enabled);
   }

   public void setSelectedBlocks(Set<Block> set) {
      this.blocks.clear();

      for (Block block : set) {
         this.blocks.toggle(block);
      }

      this.fullRescanRequested = true;
   }

   public void setBlockColors(Map<Block, Color> map) {
      this.customBlockColors.clear();
      this.customBlockColors.putAll(map);
   }

   public Map<Block, Color> getBlockColors() {
      return new LinkedHashMap<>(this.customBlockColors);
   }

   public void queueNearbyChunks(boolean force) {
      if (mc.world != null && mc.player != null) {
         int i = this.getViewDistance();
         ChunkPos chunkpos = mc.player.getChunkPos();
         ArrayList<WorldChunk> arrayList = new ArrayList<>();
         HashSet hashSet = new HashSet();

         for (int j = -i; j <= i; j++) {
            for (int k = -i; k <= i; k++) {
               WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkpos.x + j, chunkpos.z + k, false);
               if (worldChunk != null) {
                  arrayList.add(worldChunk);
                  hashSet.add(worldChunk.getPos().toLong());
               }
            }
         }

         arrayList.sort(Comparator.comparingInt(var2x -> this.chunkDistanceSq(chunkpos, var2x.getPos())));
         synchronized (this.queueLock) {
            this.scanQueue.removeIf(var1x -> !hashSet.contains(var1x));
            this.queuedChunks.retainAll(hashSet);

            for (WorldChunk worldChunk1 : (Iterable<WorldChunk>)arrayList) {
               long l = worldChunk1.getPos().toLong();
               if ((force || !this.cachedBlocks.containsKey(l)) && this.queuedChunks.add(l)) {
                  this.scanQueue.addLast(l);
               }
            }
         }

         this.pruneDistantChunks(chunkpos, i);
      }
   }

   public void enqueueChunk(long chunkKey, boolean priority) {
      synchronized (this.queueLock) {
         if (priority && this.queuedChunks.contains(chunkKey)) {
            this.scanQueue.remove(chunkKey);
            this.scanQueue.addFirst(chunkKey);
         } else if (this.queuedChunks.add(chunkKey)) {
            if (priority) {
               this.scanQueue.addFirst(chunkKey);
            } else {
               this.scanQueue.add(chunkKey);
            }
         }
      }
   }

   public void scanChunk(WorldChunk chunk) {
      Set set = this.targets;
      if (!set.isEmpty()) {
         int i = mc.world.getBottomY();
         int j = mc.world.getBottomY() + mc.world.getHeight();
         int k = mc.world.getBottomSectionCoord();
         ChunkPos chunkpos = chunk.getPos();
         long l = chunkpos.toLong();
         Set set1 = this.cachedBlocks.get(l);
         HashSet hashSet = new HashSet();
         Block block = null;
         BlockPos blockPos = null;
         ChunkSection[] achunksection = chunk.getSectionArray();

         for (int i1 = 0; i1 < achunksection.length; i1++) {
            ChunkSection chunkSection = achunksection[i1];
            if (chunkSection != null && !chunkSection.isEmpty()) {
               int j1 = (k + i1) * 16;
               if (j1 + 16 > i && j1 < j && chunkSection.getBlockStateContainer().hasAny(var1x -> set.contains(var1x.getBlock()))) {
                  for (int k1 = 0; k1 < 16; k1++) {
                     for (int l1 = 0; l1 < 16; l1++) {
                        for (int i2 = 0; i2 < 16; i2++) {
                           Block block1 = chunkSection.getBlockState(k1, i2, l1).getBlock();
                           if (set.contains(block1)) {
                              BlockPos blockPos1 = new BlockPos(chunkpos.getStartX() + k1, j1 + i2, chunkpos.getStartZ() + l1);
                              hashSet.add(blockPos1);
                              this.posTypeMap.put(blockPos1, block1);
                              if (block == null && (set1 == null || !set1.contains(blockPos1))) {
                                 block = block1;
                                 blockPos = blockPos1;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (set1 != null) {
            for (BlockPos blockPos2 : (Iterable<BlockPos>)set1) {
               if (!hashSet.contains(blockPos2)) {
                  this.posTypeMap.remove(blockPos2);
               }
            }
         }

         if (hashSet.isEmpty()) {
            this.removeChunk(l);
            this.lastNotifiedAt.remove(l);
         } else {
            this.cachedBlocks.put(l, hashSet);
            if (block != null) {
               this.notifyFound(l, block, blockPos, chunkpos);
            }
         }
      }
   }

   public void notifyFound(long chunkKey, Block block, BlockPos pos, ChunkPos chunkPos) {
      if (this.notify.getValue() && mc.player != null) {
         long i = System.currentTimeMillis();
         long j = this.lastNotifiedAt.getOrDefault(chunkKey, 0L);
         if (i - j >= 750L) {
            this.lastNotifiedAt.put(chunkKey, i);
            NotificationManager.INSTANCE
               .push(this.getBlockName(block) + " found", "X " + pos.getX() + "  Y " + pos.getY() + "  Z " + pos.getZ(), this.getIcon(block), this.getBlockColor(block, 255).getRGB());
            mc.world
               .playSound(
                  mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.6F, 0.95F
               );
         }
      }
   }

   public ItemStack getIcon(Block block) {
      ItemStack itemStack = new ItemStack(block.asItem());
      return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
   }

   public int chunkDistanceSq(ChunkPos chunkPos, ChunkPos chunkPos2) {
      int i = chunkPos2.x - chunkPos.x;
      int j = chunkPos2.z - chunkPos.z;
      return i * i + j * j;
   }

   public int getViewDistance() {
      return mc.options.getClampedViewDistance();
   }

   public double getMaxRenderDistanceSq() {
      double d0 = this.getViewDistance() * 16.0 + 16.0;
      return d0 * d0;
   }

   public void pruneDistantChunks(ChunkPos chunkPos, int radius) {
      ArrayList arrayList = new ArrayList();

      for (Long olong : this.cachedBlocks.keySet()) {
         ChunkPos chunkpos = new ChunkPos(ChunkPos.getPackedX(olong), ChunkPos.getPackedZ(olong));
         if (Math.abs(chunkpos.x - chunkPos.x) > radius || Math.abs(chunkpos.z - chunkPos.z) > radius) {
            arrayList.add(olong);
         }
      }

      for (Long olong1 : (Iterable<Long>)arrayList) {
         this.removeChunk(olong1);
         this.lastNotifiedAt.remove(olong1);
      }
   }

   public void removeChunk(long chunkKey) {
      Set set = this.cachedBlocks.remove(chunkKey);
      if (set != null) {
         set.forEach(this.posTypeMap::remove);
      }
   }

   public Color getBlockColor(Block block, int alpha) {
      Color color = this.customBlockColors.get(block);
      if (color != null) {
         return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
      } else {
         Identifier identifier = Registries.BLOCK.getId(block);
         String s = identifier == null ? "" : identifier.getPath();
         if (block == Blocks.SPAWNER) {
            return new Color(138, 126, 166, alpha);
         } else if (s.contains("diamond")) {
            return new Color(0, 255, 255, alpha);
         } else if (s.contains("ancient_debris")) {
            return new Color(196, 120, 72, alpha);
         } else if (s.contains("emerald")) {
            return new Color(0, 255, 127, alpha);
         } else if (s.contains("gold")) {
            return new Color(255, 215, 0, alpha);
         } else if (s.contains("iron")) {
            return new Color(213, 213, 213, alpha);
         } else if (s.contains("redstone")) {
            return new Color(255, 70, 70, alpha);
         } else {
            return s.contains("lapis") ? new Color(70, 110, 255, alpha) : new Color(255, 255, 0, alpha);
         }
      }
   }

   public static Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   public String getBlockName(Block block) {
      try {
         return block.getName().getString();
      } catch (Exception exception) {
         Identifier identifier = Registries.BLOCK.getId(block);
         return identifier == null ? "Block" : identifier.toString();
      }
   }

   public void clearCaches() {
      this.cachedBlocks.clear();
      this.posTypeMap.clear();
      this.lastNotifiedAt.clear();
      synchronized (this.queueLock) {
         this.scanQueue.clear();
         this.queuedChunks.clear();
      }
   }
}
