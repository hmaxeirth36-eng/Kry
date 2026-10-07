package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.opengl.GL11;

public final class SpawnerNotifier extends Module {
   public final Setting<Color> color = new Setting<>("Color", new Color(255, 80, 80));
   public final CopyOnWriteArrayList<BlockPos> spawners = new CopyOnWriteArrayList<>();
   public static SpawnerNotifier INSTANCE;
   public final Set<BlockPos> notified = ConcurrentHashMap.newKeySet();
   public long lastSoundTime = 0L;
   public int tickCounter = 0;

   public SpawnerNotifier() {
      super("Spawner Notifier", Category.RENDER);
      this.addSetting(this.color);
      INSTANCE = this;
   }

   @Override
   public void onEnable() {
      this.spawners.clear();
      this.notified.clear();
      this.tickCounter = 0;
   }

   @Override
   public void onDisable() {
      this.spawners.clear();
      this.notified.clear();
   }

   @Override
   public void onTick() {
      if (mc.world != null && mc.player != null) {
         this.tickCounter++;
         if (this.tickCounter % 40 == 0) {
            this.tickCounter = 0;
            ChunkPos chunkPos = mc.player.getChunkPos();
            int i = Math.min(mc.options.getClampedViewDistance(), 8);
            ArrayList arrayList = new ArrayList();

            for (int j = -i; j <= i; j++) {
               for (int k = -i; k <= i; k++) {
                  WorldChunk worldChunk = mc.world.getChunkManager().getWorldChunk(chunkPos.x + j, chunkPos.z + k, false);
                  if (worldChunk != null) {
                     for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
                        if (blockEntity instanceof MobSpawnerBlockEntity mobSpawnerBlockEntity) {
                           BlockPos blockPos = mobSpawnerBlockEntity.getPos();
                           arrayList.add(blockPos);
                           if (!this.notified.contains(blockPos)) {
                              this.notified.add(blockPos);
                              long l = System.currentTimeMillis();
                              if (l - this.lastSoundTime > 5000L) {
                                 this.lastSoundTime = l;
                                 mc.world
                                    .playSound(
                                       mc.player,
                                       mc.player.getX(),
                                       mc.player.getY(),
                                       mc.player.getZ(),
                                       SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                                       SoundCategory.MASTER,
                                       1.0F,
                                       1.2F
                                    );
                              }

                              String s = "\u00a7a[SpawnerNotifier] \u00a7fSpawner found at \u00a7e"
                                 + blockPos.getX()
                                 + ", "
                                 + blockPos.getY()
                                 + ", "
                                 + blockPos.getZ();

                              for (int i1 = 0; i1 < 4; i1++) {
                                 mc.inGameHud.getChatHud().addMessage(Text.literal(s));
                              }
                           }
                        }
                     }
                  }
               }
            }

            this.notified.retainAll(new HashSet(arrayList));
            this.spawners.clear();
            this.spawners.addAll(arrayList);
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null && !this.spawners.isEmpty()) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            ArrayList arrayList = new ArrayList<>(this.spawners);
            matrices.push();
            GL11.glDisable(2929);

            try {
               for (BlockPos blockPos : (Iterable<BlockPos>)arrayList) {
                  double d0 = blockPos.getX() + 0.5 - vec3d.x;
                  double d1 = blockPos.getY() + 0.5 - vec3d.y;
                  double d2 = blockPos.getZ() + 0.5 - vec3d.z;
                  this.drawBeam(matrices, d0, d1, d2);
               }
            } finally {
               GL11.glEnable(2929);
               matrices.pop();
            }
         }
      }
   }

   public void drawBeam(MatrixStack matrices, double var2, double var4, double var6) {
      Color colorx = this.color.getValue();
      Vec3d vec3d = new Vec3d(var2, var4, var6);
      Vec3d vec3d1 = new Vec3d(var2, var4 + 500.0, var6);
      RenderUtils.drawLine(matrices, colorx, vec3d, vec3d1, 8.0F);
   }
}
