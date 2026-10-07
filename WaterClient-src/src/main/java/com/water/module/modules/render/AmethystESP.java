package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class AmethystESP extends Module {
   public static final double TRACER_START_DISTANCE = 150.0;
   public static final double TRACER_END_DISTANCE = 24.0;
   public static final double TRACER_BEHIND_SPREAD = 2.75;
   public static final double CHUNK_THICKNESS = 0.05;
   public final Setting<Float> simDistance = new Setting<>("Sim Distance", 7.0F, 1.0F, 32.0F);
   public final Setting<Float> clusterThreshold = new Setting<>("Min Cluster Size", 14.0F, 1.0F, 20.0F);
   public final Setting<Boolean> chunkMark = new Setting<>("Chunk Mark", true);
   public final Setting<Color> chunkColor = new Setting<>("Chunk Color", new Color(180, 100, 255));
   public static AmethystESP INSTANCE;
   public final Map<ChunkPos, Set<BlockPos>> foundClusters = new ConcurrentHashMap<>();
   public final Set<ChunkPos> notifiedChunks = ConcurrentHashMap.newKeySet();
   public int tickCounter = 0;

   public AmethystESP() {
      super("Amethyst ESP", Category.RENDER);
      INSTANCE = this;
      this.addSetting(this.simDistance);
      this.addSetting(this.clusterThreshold);
      this.addSetting(this.chunkMark);
      this.addSetting(this.chunkColor);
   }

   public static AmethystESP getInstance() {
      return INSTANCE;
   }

   @Override
   public void onEnable() {
      this.foundClusters.clear();
      this.notifiedChunks.clear();
      this.scanNearbyChunks();
   }

   @Override
   public void onDisable() {
      this.foundClusters.clear();
      this.notifiedChunks.clear();
   }

   public void scanNearbyChunks() {
      if (mc.world != null && mc.player != null) {
         ChunkPos chunkPos = mc.player.getChunkPos();
         int i = this.simDistance.getValue().intValue();
         int j = 0;

         for (int k = -i; k <= i; k++) {
            for (int l = -i; l <= i; l++) {
               WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos.x + k, chunkPos.z + l, false);
               if (worldChunk != null) {
                  this.scanChunk(worldChunk);
                  j++;
               }
            }
         }
      }
   }

   public void scanChunk(WorldChunk chunk) {
      if (mc.world != null) {
         ChunkPos chunkPos = chunk.getPos();
         HashSet hashSet = new HashSet();
         int i = chunkPos.x << 4;
         int j = chunkPos.z << 4;

         for (int k = -64; k <= 70; k++) {
            for (int l = 0; l < 16; l++) {
               for (int i1 = 0; i1 < 16; i1++) {
                  BlockPos blockPos = new BlockPos(i + l, k, j + i1);
                  if (k <= 50 && mc.world.getLightLevel(LightType.BLOCK, blockPos) == 5 && this.hasAmethystNeighbour(blockPos)) {
                     hashSet.add(blockPos.toImmutable());
                  }
               }
            }
         }

         if (hashSet.size() >= this.clusterThreshold.getValue().intValue()) {
            this.foundClusters.put(chunkPos, hashSet);
            this.notifiedChunks.add(chunkPos);
         } else {
            this.foundClusters.remove(chunkPos);
            this.notifiedChunks.remove(chunkPos);
         }
      }
   }

   public boolean hasAmethystNeighbour(BlockPos pos) {
      for (int i = -1; i <= 1; i++) {
         for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
               BlockState blockState = mc.world.getBlockState(pos.add(i, j, k));
               if (blockState.isOf(Blocks.AMETHYST_CLUSTER)
                  || blockState.isOf(Blocks.LARGE_AMETHYST_BUD)
                  || blockState.isOf(Blocks.MEDIUM_AMETHYST_BUD)
                  || blockState.isOf(Blocks.SMALL_AMETHYST_BUD)
                  || blockState.isOf(Blocks.BUDDING_AMETHYST)
                  || blockState.isOf(Blocks.AMETHYST_BLOCK)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         if (++this.tickCounter % 40 == 0) {
            ChunkPos chunkPos = mc.player.getChunkPos();
            int i = this.simDistance.getValue().intValue();

            for (int j = -i; j <= i; j++) {
               for (int k = -i; k <= i; k++) {
                  WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos.x + j, chunkPos.z + k, false);
                  if (worldChunk != null) {
                     this.scanChunk(worldChunk);
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.foundClusters.isEmpty()) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            Vec3d vec3d1 = RenderUtils.getLookVector(camera);
            Vec3d vec3d2 = RenderUtils.getRightVector(camera);
            Color color = this.withAlpha(this.chunkColor.getValue(), 200);
            matrices.push();
            GL11.glDisable(2929);

            try {
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);
               double d0 = 47.0;

               for (Entry entry : this.foundClusters.entrySet()) {
                  ChunkPos chunkPos = (ChunkPos)entry.getKey();
                  Set set = (Set)entry.getValue();
                  if (!set.isEmpty() && this.chunkMark.getValue()) {
                     double d1 = chunkPos.getStartX() - vec3d.x;
                     double d2 = chunkPos.getStartZ() - vec3d.z;
                     double d3 = chunkPos.getEndX() - vec3d.x + 1.0;
                     double d4 = chunkPos.getEndZ() - vec3d.z + 1.0;
                     double d5 = d0 - vec3d.y;
                     double d6 = d5 + 0.05;
                     shapeBatch.renderFilledBox(d1, d5, d2, d3, d6, d4, color);
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

   public Color withAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha));
   }

   public static void onChunkUpdated(int chunkX, int chunkZ) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         WorldChunk worldChunk = MinecraftClient.getInstance().world.getChunkManager().getWorldChunk(chunkX, chunkZ, false);
         if (worldChunk != null) {
            INSTANCE.scanChunk(worldChunk);
         }
      }
   }

   public static void onBlockUpdated(BlockPos pos, BlockState state) {
      onChunkUpdated(pos.getX() >> 4, pos.getZ() >> 4);
   }

   public static void onCoordsReceived(String text, double x, double y, double z) {
   }

   public static void renderHud(DrawContext context, float tickDelta) {
      if (INSTANCE != null && INSTANCE.isEnabled()) {
         context.drawText(MinecraftClient.getInstance().textRenderer, "\u00a7dAmethystESP: \u00a7f" + INSTANCE.foundClusters.size() + " Geoden", 10, 10, -1, true);
      }
   }
}
