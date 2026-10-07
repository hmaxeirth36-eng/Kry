package com.water.module.modules.render;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

public final class TNTExplosionMarker extends Module {
   public static final double CHUNK_THICKNESS = 0.12;
   public final Setting<Integer> alpha = new Setting<>("Alpha", 80, 0, 255);
   public final Setting<Boolean> chatNotify = new Setting<>("Chat Notify", true);
   public final Map<ChunkPos, Double> markedChunks = new ConcurrentHashMap<>();

   public TNTExplosionMarker() {
      super("TNT Explosion Marker", Category.RENDER);
      this.addSetting(this.alpha);
      this.addSetting(this.chatNotify);
   }

   @Override
   public void onDisable() {
      this.markedChunks.clear();
   }

   @Override
   public void onPacketReceive(Packet<?> packet) {
      if (mc.world != null && mc.player != null && packet != null) {
         String s = packet.getClass().getSimpleName();
         if (s.equals("class_2673")) {
            this.dumpFields(packet);
         }

         if (s.toLowerCase().contains("explosion") || s.equals("class_2673")) {
            Vec3d vec3d = this.readCenterVec(packet);
            if (vec3d == null) {
               System.out.println("[TNT] Position null f\u00fcr: " + s);
            } else if (!Double.isNaN(vec3d.x) && !Double.isNaN(vec3d.y) && !Double.isNaN(vec3d.z)) {
               ChunkPos chunkPos = new ChunkPos((int)Math.floor(vec3d.x) >> 4, (int)Math.floor(vec3d.z) >> 4);
               this.markedChunks.put(chunkPos, vec3d.y);
               System.out.println("[TNT] Chunk markiert: " + chunkPos.x + ", " + chunkPos.z);
               if (this.chatNotify.getValue() && mc.inGameHud != null) {
                  mc.inGameHud.getChatHud().addMessage(Text.literal("\u00a7c[TNT] \u00a7fChunk markiert \u00a77(" + chunkPos.x + ", " + chunkPos.z + ")"));
               }
            }
         }
      }
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      if (mc.world != null && mc.player != null) {
         if (!this.markedChunks.isEmpty()) {
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               Color color = new Color(255, 60, 60, this.alpha.getValue());
               Color color1 = new Color(255, 60, 60, 255);
               GL11.glDisable(2929);
               matrices.push();

               try {
                  ShapeBatch shapeBatch = RenderUtils.beginShapeBatch(matrices);

                  for (Entry entry : this.markedChunks.entrySet()) {
                     ChunkPos chunkPos = (ChunkPos)entry.getKey();
                     double d0 = (Double)entry.getValue();
                     double d1 = (chunkPos.x << 4) - vec3d.x;
                     double d2 = (chunkPos.z << 4) - vec3d.z;
                     double d3 = d1 + 16.0;
                     double d4 = d2 + 16.0;
                     double d5 = d0 - vec3d.y;
                     double d6 = d5 + 0.12;
                     shapeBatch.renderFilledBox(d1, d5, d2, d3, d6, d4, color);
                     shapeBatch.renderOutlineBox(d1, d5, d2, d3, d6, d4, color1);
                  }

                  shapeBatch.flush();
               } catch (Exception exception) {
                  System.out.println("[TNT] Render-Fehler: " + exception.getMessage());
                  exception.printStackTrace();
               } finally {
                  matrices.pop();
                  GL11.glEnable(2929);
               }
            }
         }
      }
   }

   public Vec3d readCenterVec(Object value) {
      Object object = this.invokeNoArg(value, "center");
      if (object == null) {
         object = this.invokeNoArg(value, "getCenter");
      }

      if (object instanceof Vec3d vec3d1) {
         return vec3d1;
      } else {
         Double d0 = this.readDouble(value, "x", "getX");
         Double d1 = this.readDouble(value, "y", "getY");
         Double d2 = this.readDouble(value, "z", "getZ");
         if (d0 != null && d1 != null && d2 != null) {
            return new Vec3d(d0, d1, d2);
         } else {
            Float f = this.readFloat(value, "x", "getX");
            Float f1 = this.readFloat(value, "y", "getY");
            Float f2 = this.readFloat(value, "z", "getZ");
            if (f != null && f1 != null && f2 != null) {
               return new Vec3d(f.floatValue(), f1.floatValue(), f2.floatValue());
            } else {
               Vec3d vec3d = this.findFirstVec3d(value);
               if (vec3d != null) {
                  return vec3d;
               } else {
                  d0 = this.findDoubleField(value, "x");
                  d1 = this.findDoubleField(value, "y");
                  d2 = this.findDoubleField(value, "z");
                  return d0 != null && d1 != null && d2 != null ? new Vec3d(d0, d1, d2) : null;
               }
            }
         }
      }
   }

   public void dumpFields(Object value) {
      System.out.println("[TNT-DEBUG] Felder in " + value.getClass().getSimpleName() + ":");

      for (Class oclass = value.getClass(); oclass != null; oclass = oclass.getSuperclass()) {
         for (Field field : oclass.getDeclaredFields()) {
            try {
               field.setAccessible(true);
               System.out.println("  " + field.getName() + " (" + field.getType().getSimpleName() + ") = " + field.get(value));
            } catch (Throwable throwable) {
            }
         }
      }
   }

   public Object invokeNoArg(Object value, String text) {
      for (Class oclass = value.getClass(); oclass != null; oclass = oclass.getSuperclass()) {
         for (Method method : oclass.getDeclaredMethods()) {
            if (method.getName().equals(text) && method.getParameterCount() == 0) {
               try {
                  method.setAccessible(true);
                  return method.invoke(value);
               } catch (Throwable throwable) {
               }
            }
         }
      }

      return null;
   }

   public Double readDouble(Object value, String text, String text2) {
      Object object = this.invokeNoArg(value, text);
      if (!(object instanceof Number)) {
         object = this.invokeNoArg(value, text2);
      }

      return object instanceof Number number ? number.doubleValue() : null;
   }

   public Float readFloat(Object value, String text, String text2) {
      Object object = this.invokeNoArg(value, text);
      if (!(object instanceof Float)) {
         object = this.invokeNoArg(value, text2);
      }

      return object instanceof Float f ? f : null;
   }

   public Vec3d findFirstVec3d(Object value) {
      for (Class oclass = value.getClass(); oclass != null; oclass = oclass.getSuperclass()) {
         for (Field field : oclass.getDeclaredFields()) {
            try {
               field.setAccessible(true);
               if (field.get(value) instanceof Vec3d vec3d) {
                  return vec3d;
               }
            } catch (Throwable throwable) {
            }
         }
      }

      return null;
   }

   public Double findDoubleField(Object value, String text) {
      for (Class oclass = value.getClass(); oclass != null; oclass = oclass.getSuperclass()) {
         for (Field field : oclass.getDeclaredFields()) {
            if (field.getName().toLowerCase().contains(text)) {
               try {
                  field.setAccessible(true);
                  if (field.get(value) instanceof Number number) {
                     return number.doubleValue();
                  }
               } catch (Throwable throwable) {
               }
            }
         }
      }

      return null;
   }
}
