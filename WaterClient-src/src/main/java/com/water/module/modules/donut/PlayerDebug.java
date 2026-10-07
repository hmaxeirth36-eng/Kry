package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import com.water.render.FontRenderer;
import com.water.render.RenderUtils;
import com.water.render.ShapeBatch;
import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

/**
 * Detects other players below Max Y (underground).
 * Draws a flat chunk highlight at Display Y so you can see it
 * from the surface without going down — same style as Amethyst Sus.
 */
public final class PlayerDebug extends Module {
   public static PlayerDebug INSTANCE;

   public final Setting<Integer> maxY = new Setting<>("Max Y", -1, -64, 320);
   public final Setting<Integer> displayY = new Setting<>("Display Y", 70, -64, 320);
   public final Setting<Color> fillColor = new Setting<>("Fill Color", new Color(40, 255, 80, 55));
   public final Setting<Integer> fillAlpha = new Setting<>("Fill Alpha", 55, 0, 255);
   public final Setting<Boolean> showInfo = new Setting<>("Show Name", true);
   public final Setting<Boolean> showOutline = new Setting<>("Outline", true);

   public static final class Hit {
      public final String name;
      public final double playerY;
      public Hit(String name, double playerY) {
         this.name = name;
         this.playerY = playerY;
      }
   }

   public volatile Map<ChunkPos, Hit> stableHits = Collections.emptyMap();

   public PlayerDebug() {
      super("Player Debug", Category.DONUT);
      INSTANCE = this;
      this.addSetting(this.maxY);
      this.addSetting(this.displayY);
      this.addSetting(this.fillColor);
      this.addSetting(this.fillAlpha);
      this.addSetting(this.showInfo);
      this.addSetting(this.showOutline);
   }

   @Override
   public void onTick() {
      if (mc.world == null || mc.player == null) {
         this.stableHits = Collections.emptyMap();
         return;
      }

      ConcurrentHashMap<ChunkPos, Hit> next = new ConcurrentHashMap<>();
      int limitY = this.maxY.getValue();

      for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
         if (player == mc.player) continue;
         // only players underground (Y <= Max Y)
         if (player.getY() > limitY) continue;

         ChunkPos cp = player.getChunkPos();
         Hit existing = next.get(cp);
         if (existing == null || player.getY() < existing.playerY) {
            next.put(cp, new Hit(player.getName().getString(), player.getY()));
         }
      }
      this.stableHits = Collections.unmodifiableMap(next);
   }

   @Override
   public void onRender(MatrixStack matrices, float tickDelta) {
      Map<ChunkPos, Hit> hits = this.stableHits;
      if (hits.isEmpty() || mc.world == null || mc.player == null) return;

      Camera camera = RenderUtils.getCamera();
      if (camera == null) return;
      Vec3d cam = RenderUtils.getCameraPos(camera);

      // Flat plane at Display Y — visible from surface (like Amethyst)
      double y = this.displayY.getValue() - cam.y;
      double y2 = y + 0.25;

      Color base = this.fillColor.getValue();
      int alpha = this.fillAlpha.getValue();
      Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
      Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.min(255, alpha + 120));

      matrices.push();
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      ShapeBatch batch = RenderUtils.beginShapeBatch(matrices);
      for (ChunkPos cp : hits.keySet()) {
         double x1 = (cp.x << 4) - cam.x;
         double z1 = (cp.z << 4) - cam.z;
         batch.renderFilledBox(x1, y, z1, x1 + 16.0, y2, z1 + 16.0, fill);
         if (this.showOutline.getValue()) {
            batch.renderOutlineBox(x1, y, z1, x1 + 16.0, y2, z1 + 16.0, outline);
         }
      }
      batch.flush();
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      matrices.pop();
   }

   public static void renderHud(DrawContext context, float tickDelta) {
      PlayerDebug self = INSTANCE;
      if (self == null || !self.isEnabled() || !self.showInfo.getValue()) return;
      Map<ChunkPos, Hit> hits = self.stableHits;
      if (hits.isEmpty()) return;

      int y = 36;
      // labels are 3D now — keep list offset minimal
      if (SpawnerDebug.INSTANCE != null && SpawnerDebug.INSTANCE.isEnabled()
            && !SusCache.SPAWNERS.isEmpty()) {
         y += 12 + 11 * Math.min(10, SusCache.SPAWNERS.size()) + 6;
      }

      FontRenderer.INSTANCE.drawString(context, "Player Debug (under Y" + self.maxY.getValue() + ")", 4, y, 0xFF40FF80);
      y += 12;
      int n = 0;
      for (Map.Entry<ChunkPos, Hit> e : hits.entrySet()) {
         if (n >= 8) break;
         ChunkPos cp = e.getKey();
         Hit hit = e.getValue();
         FontRenderer.INSTANCE.drawString(context,
            String.format("  [%d %d] %s  Y%d", cp.x, cp.z, hit.name, (int) hit.playerY),
            4, y, 0xFF55FF55);
         y += 11;
         n++;
      }
   }
}
