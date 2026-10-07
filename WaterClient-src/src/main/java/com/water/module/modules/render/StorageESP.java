package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.BlockListSetting;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.SmokerBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

public final class StorageESP extends Module {
   public static StorageESP INSTANCE;
   public final Setting<Boolean> chests = new Setting<>("Chest", true);
   public final Setting<Boolean> enderChests = new Setting<>("Ender Chest", true);
   public final Setting<Boolean> spawners = new Setting<>("Spawner", true);
   public final Setting<Boolean> shulkerBoxes = new Setting<>("Shulker Box", true);
   public final Setting<Boolean> shulkerDye = new Setting<>("Use Shulker Dyes", true);
   public final Setting<Boolean> furnaces = new Setting<>("Furnace", true);
   public final Setting<Boolean> barrels = new Setting<>("Barrel", true);
   public final Setting<Boolean> enchant = new Setting<>("Enchanting Table", true);
   public final Setting<Boolean> pistons = new Setting<>("Moving Piston", true);
   public final Setting<Boolean> hoppers = new Setting<>("Hopper", true);
   public final Setting<Boolean> tracers = new Setting<>("Tracers", true);
   public final Setting<Double> tracerWidth = new Setting<>("Tracer Weight", 1.0, 0.1, 5.0);
   public final Setting<Boolean> filled = new Setting<>("Filled", true);
   public final Setting<Double> alpha = new Setting<>("Opacity", 220.0, 0.0, 255.0);
   public final Setting<Double> fillAlpha = new Setting<>("Fill Alpha", 100.0, 0.0, 255.0);
   public final BlockListSetting customBlocks = new BlockListSetting("Blocks");
   public final Map<Block, Color> customBlockColors = new ConcurrentHashMap<>();
   public final Map<BlockPos, Block> storageBlocks = new ConcurrentHashMap<>();
   public final Map<BlockPos, Color> blockColors = new ConcurrentHashMap<>();
   public final ExecutorService scanner = Executors.newSingleThreadExecutor(var0 -> {
      Thread thread = new Thread(var0, "storageESP-scan");
      thread.setDaemon(true);
      return thread;
   });
   public final AtomicBoolean isScanning = new AtomicBoolean(false);
   public int scanTick = 0;
   public static final int SCAN_INTERVAL_TICKS = 10;

   public StorageESP() {
      super("Storage ESP", Category.RENDER);
      INSTANCE = this;
      this.addSetting(this.chests);
      this.addSetting(this.enderChests);
      this.addSetting(this.spawners);
      this.addSetting(this.shulkerBoxes);
      this.addSetting(this.shulkerDye);
      this.addSetting(this.furnaces);
      this.addSetting(this.barrels);
      this.addSetting(this.enchant);
      this.addSetting(this.pistons);
      this.addSetting(this.hoppers);
      this.addSetting(this.tracers);
      this.addSetting(this.tracerWidth);
      this.addSetting(this.filled);
      this.addSetting(this.alpha);
      this.addSetting(this.fillAlpha);
      this.addSetting(this.customBlocks);
   }

   public Map<Block, Color> getBuiltinBlockColors() {
      LinkedHashMap linkedHashMap = new LinkedHashMap();
      if (this.chests.getValue()) {
         linkedHashMap.put(Blocks.CHEST, this.customBlockColors.getOrDefault(Blocks.CHEST, new Color(156, 91, 0)));
         linkedHashMap.put(Blocks.TRAPPED_CHEST, this.customBlockColors.getOrDefault(Blocks.TRAPPED_CHEST, new Color(200, 91, 0)));
      }

      if (this.enderChests.getValue()) {
         linkedHashMap.put(Blocks.ENDER_CHEST, this.customBlockColors.getOrDefault(Blocks.ENDER_CHEST, new Color(117, 0, 255)));
      }

      if (this.spawners.getValue()) {
         linkedHashMap.put(Blocks.SPAWNER, this.customBlockColors.getOrDefault(Blocks.SPAWNER, new Color(138, 126, 166)));
      }

      if (this.shulkerBoxes.getValue()) {
         linkedHashMap.put(Blocks.PURPLE_SHULKER_BOX, this.customBlockColors.getOrDefault(Blocks.PURPLE_SHULKER_BOX, new Color(134, 0, 158)));
      }

      if (this.furnaces.getValue()) {
         linkedHashMap.put(Blocks.FURNACE, this.customBlockColors.getOrDefault(Blocks.FURNACE, new Color(125, 125, 125)));
      }

      if (this.barrels.getValue()) {
         linkedHashMap.put(Blocks.BARREL, this.customBlockColors.getOrDefault(Blocks.BARREL, new Color(255, 140, 140)));
      }

      if (this.enchant.getValue()) {
         linkedHashMap.put(Blocks.ENCHANTING_TABLE, this.customBlockColors.getOrDefault(Blocks.ENCHANTING_TABLE, new Color(80, 80, 255)));
      }

      if (this.pistons.getValue()) {
         linkedHashMap.put(Blocks.PISTON, this.customBlockColors.getOrDefault(Blocks.PISTON, new Color(35, 226, 0)));
      }

      if (this.hoppers.getValue()) {
         linkedHashMap.put(Blocks.HOPPER, this.customBlockColors.getOrDefault(Blocks.HOPPER, new Color(35, 226, 0)));
      }

      return linkedHashMap;
   }

   public void setBuiltinBlockColor(Block block, Color color) {
      this.customBlockColors.put(block, color);
   }

   public Map<Block, Color> getCustomBlockColors() {
      return this.customBlockColors;
   }

   public void setCustomBlockColors(Map<Block, Color> map) {
      this.customBlockColors.clear();
      this.customBlockColors.putAll(map);
   }

   @Override
   public void onEnable() {
      this.storageBlocks.clear();
      this.blockColors.clear();
      this.scanTick = 0;
   }

   @Override
   public void onDisable() {
      this.storageBlocks.clear();
      this.blockColors.clear();
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if (++this.scanTick >= 10 && !this.isScanning.get()) {
            this.scanTick = 0;
            this.scanForContainers();
         }
      } else {
         this.storageBlocks.clear();
      }
   }

   public void scanForContainers() {
      this.isScanning.set(true);
      if (mc.world != null && mc.player != null) {
         BlockPos blockPos = mc.player.getBlockPos();
         int i = blockPos.getX() >> 4;
         int j = blockPos.getZ() >> 4;
         byte b0 = 4;
         boolean flag = this.customBlocks.size() > 0;
         ArrayList arrayList = new ArrayList();

         try {
            for (int k = i - b0; k <= i + b0; k++) {
               for (int l = j - b0; l <= j + b0; l++) {
                  WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(k, l, false);
                  if (worldChunk != null) {
                     arrayList.add(worldChunk);
                  }
               }
            }
         } catch (Exception exception) {
            this.isScanning.set(false);
            return;
         }

         this.scanner.execute(() -> {
            try {
               HashMap hashMap = new HashMap();
               HashMap hashMap1 = new HashMap();

               for (WorldChunk worldChunk1 : (Iterable<WorldChunk>)arrayList) {
                  try {
                     for (Entry entry : new HashMap<>(worldChunk1.getBlockEntities()).entrySet()) {
                        BlockEntity blockentity = (BlockEntity)entry.getValue();
                        if (blockentity != null && this.isEnabledContainer(blockentity)) {
                           hashMap.put((BlockPos)entry.getKey(), blockentity.getCachedState().getBlock());
                           hashMap1.put((BlockPos)entry.getKey(), this.getContainerColor(blockentity));
                        }
                     }
                  } catch (Exception exception2) {
                  }

                  if (flag) {
                     try {
                        ChunkSection[] achunksection = worldChunk1.getSectionArray();
                        int k2 = worldChunk1.getBottomY();
                        int l2 = worldChunk1.getPos().x << 4;
                        int i1 = worldChunk1.getPos().z << 4;

                        for (int j1 = 0; j1 < achunksection.length; j1++) {
                           ChunkSection chunkSection = achunksection[j1];
                           if (chunkSection != null && !chunkSection.isEmpty()) {
                              int k1 = k2 + j1 * 16;

                              for (int l1 = 0; l1 < 16; l1++) {
                                 for (int i2 = 0; i2 < 16; i2++) {
                                    for (int j2 = 0; j2 < 16; j2++) {
                                       Block block = chunkSection.getBlockState(l1, j2, i2).getBlock();
                                       if (this.customBlocks.contains(block)) {
                                          BlockPos blockPos1 = new BlockPos(l2 + l1, k1 + j2, i1 + i2);
                                          if (!hashMap.containsKey(blockPos1)) {
                                             hashMap.put(blockPos1, block);
                                             hashMap1.put(blockPos1, this.customBlockColors.getOrDefault(block, new Color(0, 200, 255)));
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     } catch (Exception exception1) {
                     }
                  }
               }

               this.storageBlocks.putAll(hashMap);
               this.storageBlocks.keySet().retainAll(hashMap.keySet());
               this.blockColors.putAll(hashMap1);
               this.blockColors.keySet().retainAll(hashMap.keySet());
            } catch (Exception exception3) {
            } finally {
               this.isScanning.set(false);
            }
         });
      } else {
         this.isScanning.set(false);
      }
   }

   public boolean isEnabledContainer(BlockEntity blockEntity) {
      if (blockEntity instanceof ChestBlockEntity && this.chests.getValue()) {
         return true;
      } else if (blockEntity instanceof TrappedChestBlockEntity && this.chests.getValue()) {
         return true;
      } else if (blockEntity instanceof EnderChestBlockEntity && this.enderChests.getValue()) {
         return true;
      } else if (blockEntity instanceof MobSpawnerBlockEntity && this.spawners.getValue()) {
         return true;
      } else if (blockEntity instanceof ShulkerBoxBlockEntity && this.shulkerBoxes.getValue()) {
         return true;
      } else if (blockEntity instanceof FurnaceBlockEntity && this.furnaces.getValue()) {
         return true;
      } else if (blockEntity instanceof BlastFurnaceBlockEntity && this.furnaces.getValue()) {
         return true;
      } else if (blockEntity instanceof SmokerBlockEntity && this.furnaces.getValue()) {
         return true;
      } else if (blockEntity instanceof BarrelBlockEntity && this.barrels.getValue()) {
         return true;
      } else if (blockEntity instanceof EnchantingTableBlockEntity && this.enchant.getValue()) {
         return true;
      } else {
         return blockEntity instanceof PistonBlockEntity && this.pistons.getValue() ? true : blockEntity instanceof HopperBlockEntity && this.hoppers.getValue();
      }
   }

   public Color getContainerColor(BlockEntity blockEntity) {
      Block block = blockEntity.getCachedState().getBlock();
      if (this.customBlockColors.containsKey(block)) {
         return this.customBlockColors.get(block);
      } else if (blockEntity instanceof ShulkerBoxBlockEntity && this.shulkerBoxes.getValue()) {
         if (this.shulkerDye.getValue()) {
            Color color = this.getShulkerDyeColor(block);
            if (color != null) {
               return color;
            }
         }

         return new Color(134, 0, 158);
      } else if (blockEntity instanceof TrappedChestBlockEntity) {
         return new Color(200, 91, 0);
      } else if (blockEntity instanceof ChestBlockEntity) {
         return new Color(156, 91, 0);
      } else if (blockEntity instanceof EnderChestBlockEntity) {
         return new Color(117, 0, 255);
      } else if (blockEntity instanceof MobSpawnerBlockEntity) {
         return new Color(138, 126, 166);
      } else if (blockEntity instanceof FurnaceBlockEntity || blockEntity instanceof BlastFurnaceBlockEntity || blockEntity instanceof SmokerBlockEntity) {
         return new Color(125, 125, 125);
      } else if (blockEntity instanceof BarrelBlockEntity) {
         return new Color(255, 140, 140);
      } else if (blockEntity instanceof EnchantingTableBlockEntity) {
         return new Color(80, 80, 255);
      } else if (blockEntity instanceof PistonBlockEntity) {
         return new Color(35, 226, 0);
      } else {
         return blockEntity instanceof HopperBlockEntity ? new Color(35, 226, 0) : new Color(100, 200, 255);
      }
   }

   public Color getShulkerDyeColor(Block block) {
      if (block == Blocks.WHITE_SHULKER_BOX) {
         return new Color(16052459);
      } else if (block == Blocks.ORANGE_SHULKER_BOX) {
         return new Color(16098851);
      } else if (block == Blocks.MAGENTA_SHULKER_BOX) {
         return new Color(13061821);
      } else if (block == Blocks.LIGHT_BLUE_SHULKER_BOX) {
         return new Color(4043473);
      } else if (block == Blocks.YELLOW_SHULKER_BOX) {
         return new Color(16369177);
      } else if (block == Blocks.LIME_SHULKER_BOX) {
         return new Color(7648811);
      } else if (block == Blocks.PINK_SHULKER_BOX) {
         return new Color(15760568);
      } else if (block == Blocks.GRAY_SHULKER_BOX) {
         return new Color(4869970);
      } else if (block == Blocks.LIGHT_GRAY_SHULKER_BOX) {
         return new Color(10396579);
      } else if (block == Blocks.CYAN_SHULKER_BOX) {
         return new Color(2461326);
      } else if (block == Blocks.PURPLE_SHULKER_BOX) {
         return new Color(8339380);
      } else if (block == Blocks.BLUE_SHULKER_BOX) {
         return new Color(2964907);
      } else if (block == Blocks.BROWN_SHULKER_BOX) {
         return new Color(8343857);
      } else if (block == Blocks.GREEN_SHULKER_BOX) {
         return new Color(5268771);
      } else if (block == Blocks.RED_SHULKER_BOX) {
         return new Color(10495778);
      } else {
         return block == Blocks.BLACK_SHULKER_BOX ? new Color(1842212) : null;
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.storageBlocks.isEmpty()) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            Vec3d vec3d1 = RenderUtils.getLookVector(camera);
            Vec3d vec3d2 = Freecam.getTracerOrigin(vec3d, tickDelta);
            Vec3d vec3d3 = vec3d2.equals(vec3d) ? vec3d1.multiply(0.1) : vec3d2.subtract(vec3d);
            int i = this.clampAlpha((int)Math.round(this.alpha.getValue()));
            int j = this.clampAlpha((int)Math.round(this.fillAlpha.getValue()));
            short short1 = 5000;
            double d0 = mc.player.getX();
            double d1 = mc.player.getY();
            double d2 = mc.player.getZ();
            short short2 = 16384;
            int k = 0;
            ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);
            ShapeBatch shapeBatch1 = this.filled.getValue() && j > 0 ? RenderUtils.beginShapeBatch(matrices) : null;

            for (Entry entry : this.storageBlocks.entrySet()) {
               if (k >= short1) {
                  break;
               }

               BlockPos blockPos = (BlockPos)entry.getKey();
               Color color = this.blockColors.get(blockPos);
               if (color != null) {
                  double d3 = blockPos.getX() - d0;
                  double d4 = blockPos.getZ() - d2;
                  if (!(d3 * d3 + d4 * d4 > short2)) {
                     k++;
                     double d5 = blockPos.getX() - vec3d.x;
                     double d6 = blockPos.getY() - vec3d.y;
                     double d7 = blockPos.getZ() - vec3d.z;
                     if (Double.isFinite(d5) && Double.isFinite(d6) && Double.isFinite(d7)) {
                        Color color1 = withAlpha(color, i);
                        shapeBatch.renderOutlineBox(d5 + 0.0625, d6, d7 + 0.0625, d5 + 0.9375, d6 + 1.0, d7 + 0.9375, color1);
                        if (this.tracers.getValue()) {
                           shapeBatch.renderLine(color1, vec3d3, new Vec3d(d5 + 0.5, d6 + 0.5, d7 + 0.5), this.tracerWidth.getValue().floatValue());
                        }

                        if (shapeBatch1 != null) {
                           shapeBatch1.renderFilledBox(d5 + 0.0625, d6, d7 + 0.0625, d5 + 0.9375, d6 + 1.0, d7 + 0.9375, withAlpha(color, j));
                        }
                     }
                  }
               }
            }

            shapeBatch.flush();
            if (shapeBatch1 != null) {
               shapeBatch1.flush();
            }
         }
      }
   }

   public static Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   public int clampAlpha(int alpha) {
      return Math.max(0, Math.min(255, alpha));
   }
}
