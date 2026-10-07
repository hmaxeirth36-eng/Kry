package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import com.water.util.LightBlock;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class LightDebug extends Module {
   public static final int SCAN_RADIUS = 3;
   public static final int SCAN_INTERVAL = 10;
   public static final int MAX_RENDER_BLOCKS = 50000;
   public final Setting<Integer> minLight = new Setting<>("Min Light", 0, 0, 15);
   public final Setting<Integer> maxLight = new Setting<>("Max Light", 7, 0, 15);
   public final Setting<Integer> minY = new Setting<>("Min Y", -51, -64, 20);
   public final Setting<Integer> maxY = new Setting<>("Max Y", 20, -64, 20);
   public final Setting<Color> colorLow = new Setting<>("Color Low", new Color(50, 200, 50, 60));
   public final Setting<Color> colorHigh = new Setting<>("Color High", new Color(255, 50, 50, 60));
   public final Map<ChunkPos, List<LightBlock>> lightBlocks = new ConcurrentHashMap<>();
   public ChunkPos lastPlayerChunk = null;
   public int scanTimer = 0;
   public ExecutorService scanExec;
   public final AtomicBoolean scanning = new AtomicBoolean(false);

   public LightDebug() {
      super("LightDebug", Category.RENDER);
      this.addSetting(this.minLight);
      this.addSetting(this.maxLight);
      this.addSetting(this.minY);
      this.addSetting(this.maxY);
      this.addSetting(this.colorLow);
      this.addSetting(this.colorHigh);
   }

   @Override
   public void onEnable() {
      this.lightBlocks.clear();
      this.lastPlayerChunk = null;
      this.scanTimer = 0;
      this.scanning.set(false);
   }

   @Override
   public void onDisable() {
      this.lightBlocks.clear();
      if (this.scanExec != null) {
         this.scanExec.shutdownNow();
      }
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         ChunkPos chunkpos = mc.player.getChunkPos();
         boolean flag = !chunkpos.equals(this.lastPlayerChunk);
         this.lastPlayerChunk = chunkpos;
         if (++this.scanTimer % 10 == 0 || flag) {
            if (this.scanning.compareAndSet(false, true)) {
               if (this.scanExec == null || this.scanExec.isShutdown()) {
                  this.scanExec = Executors.newSingleThreadExecutor(var0 -> {
                     Thread thread = new Thread(var0, "lightdebug-scan");
                     thread.setDaemon(true);
                     return thread;
                  });
               }

               byte b0 = 3;
               int i = this.minLight.getValue();
               int j = this.maxLight.getValue();
               int k = Math.min(this.minY.getValue(), this.maxY.getValue());
               int l = Math.max(this.minY.getValue(), this.maxY.getValue());
               ArrayList arrayList = new ArrayList();
               ArrayList arrayList1 = new ArrayList();

               for (int i1 = -b0; i1 <= b0; i1++) {
                  for (int j1 = -b0; j1 <= b0; j1++) {
                     ChunkPos chunkPos1 = new ChunkPos(chunkpos.x + i1, chunkpos.z + j1);
                     WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos1.x, chunkPos1.z, false);
                     if (worldChunk != null && !worldChunk.isEmpty()) {
                        arrayList.add(chunkPos1);
                        arrayList1.add(worldChunk);
                     }
                  }
               }

               this.lightBlocks.keySet().removeIf(var2x -> Math.abs(var2x.x - chunkpos.x) > b0 + 1 || Math.abs(var2x.z - chunkpos.z) > b0 + 1);
               this.scanExec.submit(() -> {
                  try {
                     for (int k1 = 0; k1 < arrayList.size(); k1++) {
                        ChunkPos chunkPos2 = (ChunkPos)arrayList.get(k1);
                        List list = this.collectLightBlocks((WorldChunk)arrayList1.get(k1), chunkPos2, i, j, k, l);
                        if (list.isEmpty()) {
                           this.lightBlocks.remove(chunkPos2);
                        } else {
                           this.lightBlocks.put(chunkPos2, list);
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

   public List<LightBlock> collectLightBlocks(WorldChunk chunk, ChunkPos chunkPos, int var3, int var4, int var5, int var6) {
      ArrayList arrayList = new ArrayList();
      int i = chunkPos.x << 4;
      int j = chunkPos.z << 4;

      for (int k = var5; k <= var6; k++) {
         for (int l = 0; l < 16; l++) {
            for (int i1 = 0; i1 < 16; i1++) {
               BlockPos blockPos = new BlockPos(i + l, k, j + i1);
               int j1 = mc.world.getLightLevel(LightType.BLOCK, blockPos);
               if (j1 >= var3 && j1 <= var4) {
                  arrayList.add(new LightBlock(blockPos, j1));
               }
            }
         }
      }

      return arrayList;
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         if (!this.lightBlocks.isEmpty()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               int i = this.maxLight.getValue();
               int j = this.minLight.getValue();
               matrices.push();
               GL11.glDisable(2929);
               ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);
               int k = 0;

               label33:
               for (List list : this.lightBlocks.values()) {
                  for (LightBlock lightBlock : (Iterable<LightBlock>)list) {
                     if (k++ >= 50000) {
                        break label33;
                     }

                     Color color = this.getLightColor(lightBlock.level, j, i);
                     double d0 = lightBlock.pos.getX() - vec3d.x;
                     double d1 = lightBlock.pos.getY() - vec3d.y;
                     double d2 = lightBlock.pos.getZ() - vec3d.z;
                     shapeBatch.renderFilledBox(d0, d1, d2, d0 + 1.0, d1 + 1.0, d2 + 1.0, color);
                  }
               }

               shapeBatch.flush();
               GL11.glEnable(2929);
               matrices.pop();
            }
         }
      }
   }

   public Color getLightColor(int var1, int var2, int var3) {
      float f = Math.max(1, var3 - var2);
      float f1 = Math.max(0.0F, Math.min(1.0F, (var1 - var2) / f));
      Color color = this.colorLow.getValue();
      Color color1 = this.colorHigh.getValue();
      int i = (int)(color.getRed() + (color1.getRed() - color.getRed()) * f1);
      int j = (int)(color.getGreen() + (color1.getGreen() - color.getGreen()) * f1);
      int k = (int)(color.getBlue() + (color1.getBlue() - color.getBlue()) * f1);
      int l = (int)(color.getAlpha() + (color1.getAlpha() - color.getAlpha()) * f1);
      return new Color(i, j, k, l);
   }
}
