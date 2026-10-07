package com.water.module.modules.donut;

import com.water.gui.NotificationManager;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public final class ActivityDebug extends Module {
   public static final long NOTIFY_COOLDOWN_MS = 1250L;
   public static final double MARKER_SURFACE_Y = 57.0;
   public static final double MARKER_SURFACE_THICKNESS = 0.05;
   public final Setting<Float> yLevel = new Setting<>("y-level", 16.0F, -64.0F, 320.0F);
   public final Setting<Boolean> notification = new Setting<>("Notification", false);
   public final Set<ChunkPos> susChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
   public final Map<Long, Long> lastNotifiedAt = new ConcurrentHashMap<>();
   public static final Color YELLOW = new Color(255, 220, 0, 180);
   public static final Color YELLOW_OUTLINE = new Color(255, 220, 0, 255);
   public final Map<Class<?>, List<Field>> doubleFields = new ConcurrentHashMap<>();
   public final Map<Class<?>, List<Field>> nestedFields = new ConcurrentHashMap<>();
   public final Map<Class<?>, List<Field>> blockPosFields = new ConcurrentHashMap<>();
   public final Map<Class<?>, List<Field>> vec3Fields = new ConcurrentHashMap<>();
   public final ThreadLocal<Set<Integer>> exploredObjects = ThreadLocal.withInitial(() -> Collections.newSetFromMap(new ConcurrentHashMap<>()));

   public ActivityDebug() {
      super("ActivityDebug", Category.DONUT);
      this.addSetting(this.yLevel);
      this.addSetting(this.notification);
   }

   @Override
   public void onDisable() {
      this.susChunks.clear();
      this.lastNotifiedAt.clear();
      this.doubleFields.clear();
      this.nestedFields.clear();
      this.blockPosFields.clear();
      this.vec3Fields.clear();
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (packet instanceof ChunkDeltaUpdateS2CPacket chunkDeltaUpdateS2CPacket) {
         chunkDeltaUpdateS2CPacket.visitUpdates((var1x, var2x) -> this.recordSuspiciousPos(var1x.getX(), var1x.getY(), var1x.getZ()));
      } else if (packet instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
         BlockPos blockPos = blockUpdateS2CPacket.getPos();
         this.recordSuspiciousPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      } else {
         this.exploredObjects.get().clear();
         this.exploreObjectForPositions(packet, 0);
      }
   }

   public void exploreObjectForPositions(Object value, int depth) {
      if (value != null && depth <= 3) {
         int i = System.identityHashCode(value);
         if (this.exploredObjects.get().add(i)) {
            Class oclass = value.getClass();
            this.blockPosFields.computeIfAbsent(oclass, var1x -> this.collectFieldsOfType((Class<?>)var1x, BlockPos.class));
            this.vec3Fields.computeIfAbsent(oclass, var1x -> this.collectFieldsOfType((Class<?>)var1x, Vec3d.class));
            this.doubleFields.computeIfAbsent(oclass, var1x -> this.collectFieldsOfType((Class<?>)var1x, double.class));
            this.nestedFields
               .computeIfAbsent(
                  oclass,
                  var0 -> {
                     ArrayList arrayList = new ArrayList();

                     while (var0 != null && var0 != Object.class) {
                        for (Field field3 : var0.getDeclaredFields()) {
                           if (!Modifier.isStatic(field3.getModifiers())
                              && !field3.getType().isPrimitive()
                              && !field3.getType().getName().startsWith("java.")
                              && !field3.getType().isEnum()) {
                              field3.setAccessible(true);
                              arrayList.add(field3);
                           }
                        }

                        var0 = var0.getSuperclass();
                     }

                     return arrayList;
                  }
               );

            for (Field field : this.blockPosFields.get(oclass)) {
               try {
                  BlockPos blockPos = (BlockPos)field.get(value);
                  if (blockPos != null) {
                     this.recordSuspiciousPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                  }
               } catch (Exception exception3) {
               }
            }

            for (Field field1 : this.vec3Fields.get(oclass)) {
               try {
                  Vec3d vec3d = (Vec3d)field1.get(value);
                  if (vec3d != null) {
                     this.recordSuspiciousPos(vec3d.x, vec3d.y, vec3d.z);
                  }
               } catch (Exception exception2) {
               }
            }

            List list = this.doubleFields.get(oclass);
            if (list.size() >= 3) {
               try {
                  double d2 = ((Field)list.get(0)).getDouble(value);
                  double d0 = ((Field)list.get(1)).getDouble(value);
                  double d1 = ((Field)list.get(2)).getDouble(value);
                  if (Math.abs(d2) < 3.0E7 && Math.abs(d1) < 3.0E7 && d0 > -2048.0 && d0 < 2048.0) {
                     this.recordSuspiciousPos(d2, d0, d1);
                  }
               } catch (Exception exception1) {
               }
            }

            for (Field field2 : this.nestedFields.get(oclass)) {
               try {
                  Object object = field2.get(value);
                  if (object != null) {
                     this.exploreObjectForPositions(object, depth + 1);
                  }
               } catch (Exception exception) {
               }
            }
         }
      }
   }

   public List<Field> collectFieldsOfType(Class<?> type, Class<?> type2) {
      ArrayList arrayList = new ArrayList();

      while (type != null && type != Object.class) {
         for (Field field : type.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && field.getType() == type2) {
               field.setAccessible(true);
               arrayList.add(field);
            }
         }

         type = type.getSuperclass();
      }

      return arrayList;
   }

   public void recordSuspiciousPos(double x, double y, double z) {
      if (mc.player == null || !(mc.player.getY() < 0.0)) {
         if (y <= this.yLevel.getValue().floatValue()) {
            ChunkPos chunkpos = new ChunkPos((int)Math.floor(x) >> 4, (int)Math.floor(z) >> 4);
            if (this.susChunks.add(chunkpos)) {
               this.notifyChunk(chunkpos, y);
            }
         }
      }
   }

   public void notifyChunk(ChunkPos chunkPos, double y) {
      if (this.notification.getValue() && mc.player != null && mc.world != null) {
         long i = chunkPos.toLong();
         long j = System.currentTimeMillis();
         long k = this.lastNotifiedAt.getOrDefault(i, 0L);
         if (j - k >= 1250L) {
            this.lastNotifiedAt.put(i, j);
            NotificationManager.INSTANCE
               .push(
                  "Activity detected",
                  "Chunk " + chunkPos.x + ", " + chunkPos.z + "  Y " + (int)Math.floor(y),
                  new ItemStack(Items.COMPASS),
                  YELLOW_OUTLINE.getRGB()
               );
            mc.world
               .playSound(
                  mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.6F, 1.05F
               );
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null) {
         Camera camera = RenderUtils.getCamera();
         if (camera != null) {
            Vec3d vec3d = RenderUtils.getCameraPos(camera);
            double d0 = Math.max((double)mc.world.getBottomY(), Math.min(57.0, (double)mc.world.getTopYInclusive()));
            ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

            for (ChunkPos chunkpos : this.susChunks) {
               if (RenderUtils.isBoxVisible(chunkpos.getStartX(), 57.0, chunkpos.getStartZ(), chunkpos.getEndX(), 57.05, chunkpos.getEndZ())) {
                  double d1 = chunkpos.getStartX() - vec3d.x;
                  double d2 = chunkpos.getStartZ() - vec3d.z;
                  double d3 = d0 - vec3d.y;
                  double d4 = d1 + 16.0;
                  double d5 = d3 + 0.05;
                  double d6 = d2 + 16.0;
                  shapeBatch.renderFilledBox(d1, d3, d2, d4, d5, d6, YELLOW);
               }
            }

            shapeBatch.flush();
         }
      }
   }
}
