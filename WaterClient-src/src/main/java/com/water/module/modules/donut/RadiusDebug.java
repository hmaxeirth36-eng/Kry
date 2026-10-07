package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class RadiusDebug extends Module {
   public final Setting<Float> scanRadius = new Setting<>("Scan Radius", 3.0F, 1.0F, 16.0F);
   public final Setting<Float> kolision = new Setting<>("KOLISION", 1.0F, 1.0F, 10.0F);
   public final Setting<Color> color = new Setting<>("Color", new Color(160, 50, 255, 60));
   public final Setting<Float> fillAlpha = new Setting<>("Fill Alpha", 60.0F, 0.0F, 255.0F);
   public final Setting<Float> lineAlpha = new Setting<>("Line Alpha", 180.0F, 0.0F, 255.0F);
   public final Map<ChunkPos, Integer> foundGeodes = new ConcurrentHashMap<>();
   public int scanCursor = 0;

   public RadiusDebug() {
      super("RadiusDebug", Category.DONUT);
      this.addSetting(this.scanRadius);
      this.addSetting(this.kolision);
      this.addSetting(this.color);
      this.addSetting(this.fillAlpha);
      this.addSetting(this.lineAlpha);
   }

   public int getScanBudget() {
      return this.kolision.getValue().intValue() * 40;
   }

   @Override
   public void onEnable() {
      this.foundGeodes.clear();
      this.scanCursor = 0;
   }

   @Override
   public void onDisable() {
      this.foundGeodes.clear();
      this.scanCursor = 0;
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         ChunkPos chunkPos = mc.player.getChunkPos();
         int i = this.scanRadius.getValue().intValue();
         int j = i * 2 + 1;
         int k = j * j;
         int l = this.getScanBudget();
         this.foundGeodes.keySet().removeIf(var2x -> Math.abs(var2x.x - chunkPos.x) > i + 4 || Math.abs(var2x.z - chunkPos.z) > i + 4);

         for (int i1 = 0; i1 < 8; i1++) {
            int j1 = this.scanCursor % k;
            this.scanCursor = (this.scanCursor + 1) % k;
            int k1 = j1 % j - i;
            int l1 = j1 / j - i;
            int i2 = chunkPos.x + k1;
            int j2 = chunkPos.z + l1;
            WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(i2, j2, false);
            if (worldChunk != null && !worldChunk.isEmpty()) {
               ChunkPos chunkPos1 = new ChunkPos(i2, j2);
               int k2 = this.countAmethyst(worldChunk);
               if (k2 >= l) {
                  boolean flag = false;
                  ChunkPos chunkPos2 = null;

                  for (Entry entry : this.foundGeodes.entrySet()) {
                     if (Math.abs(((ChunkPos)entry.getKey()).x - chunkPos1.x) <= 8 && Math.abs(((ChunkPos)entry.getKey()).z - chunkPos1.z) <= 8) {
                        if (k2 > (Integer)entry.getValue()) {
                           chunkPos2 = (ChunkPos)entry.getKey();
                        } else {
                           flag = true;
                        }
                        break;
                     }
                  }

                  if (chunkPos2 != null) {
                     this.foundGeodes.remove(chunkPos2);
                     this.foundGeodes.put(chunkPos1, k2);
                  } else if (!flag) {
                     this.foundGeodes.put(chunkPos1, k2);
                  }
               }
            }
         }

         this.foundGeodes.entrySet().removeIf(var1x -> var1x.getValue() < l);
      }
   }

   public boolean isAmethyst(BlockState state) {
      return state.isOf(Blocks.AMETHYST_BLOCK)
         || state.isOf(Blocks.BUDDING_AMETHYST)
         || state.isOf(Blocks.AMETHYST_CLUSTER)
         || state.isOf(Blocks.LARGE_AMETHYST_BUD)
         || state.isOf(Blocks.MEDIUM_AMETHYST_BUD)
         || state.isOf(Blocks.SMALL_AMETHYST_BUD);
   }

   public int countAmethyst(WorldChunk chunk) {
      int i = 0;

      for (ChunkSection chunkSection : chunk.getSectionArray()) {
         if (chunkSection != null && !chunkSection.isEmpty() && chunkSection.hasAny(this::isAmethyst)) {
            for (int j = 0; j < 16; j++) {
               for (int k = 0; k < 16; k++) {
                  for (int l = 0; l < 16; l++) {
                     if (this.isAmethyst(chunkSection.getBlockState(j, l, k))) {
                        i++;
                     }
                  }
               }
            }
         }
      }

      return i;
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.foundGeodes.isEmpty()) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            Color colorx = this.color.getValue();
            Color color1 = new Color(colorx.getRed(), colorx.getGreen(), colorx.getBlue(), Math.min(255, Math.max(0, this.fillAlpha.getValue().intValue())));
            Color color2 = new Color(colorx.getRed(), colorx.getGreen(), colorx.getBlue(), Math.min(255, Math.max(0, this.lineAlpha.getValue().intValue())));
            matrices.push();
            GL11.glDisable(2929);

            try {
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

               for (ChunkPos chunkPos : this.foundGeodes.keySet()) {
                  double d0 = 49.0 - vec3d.y - 0.05;
                  double d1 = 49.0 - vec3d.y + 0.05;

                  for (int i = -1; i <= 2; i++) {
                     for (int j = -1; j <= 2; j++) {
                        ChunkPos chunkPos1 = new ChunkPos(chunkPos.x + i, chunkPos.z + j);
                        double d2 = chunkPos1.getStartX() - vec3d.x;
                        double d3 = chunkPos1.getStartZ() - vec3d.z;
                        double d4 = chunkPos1.getEndX() - vec3d.x + 1.0;
                        double d5 = chunkPos1.getEndZ() - vec3d.z + 1.0;
                        shapeBatch.renderFilledBox(d2, d0, d3, d4, d1, d5, color1);
                        shapeBatch.renderOutlineBox(d2, d0, d3, d4, d1, d5, color2);
                     }
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
}
