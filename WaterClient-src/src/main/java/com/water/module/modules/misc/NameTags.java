package com.water.module.modules.misc;

import com.water.gui.ClickGuiScreen;
import com.water.module.Category;
import com.water.module.Module;
import com.water.module.modules.client.Water;
import com.water.module.modules.render.Freecam;
import com.water.module.setting.Setting;
import com.water.render.ProjectedPoint;
import com.water.render.Render2D;
import com.water.render.Render3D;
import com.water.render.RenderUtils;
import com.water.util.HealthData;
import com.water.util.ItemEntry;
import com.water.util.NameTagData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.EntityAttachmentType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

public final class NameTags extends Module {
   public static final float MAX_RENDER_DISTANCE = 64.0F;
   public static final boolean SHOW_ABSORPTION = true;
   public static final int HUD_TEXT_COLOR = -1;
   public static final int HUD_OUTLINE_COLOR = -16777216;
   public static final float HUD_WORLD_SCALE = 0.025F;
   public static final int HUD_OUTLINE_RADIUS = 1;
   public static final int HUD_NAME_OFFSET = 0;
   public static final int HUD_HEALTH_OFFSET_WITH_NAME = 10;
   public static final int HUD_HEALTH_OFFSET_NO_NAME = 0;
   public static final int HUD_ITEM_SIZE = 16;
   public static final int HUD_ITEM_GAP = 2;
   public static final int HUD_ITEM_ROW_OFFSET_WITH_HEALTH = 34;
   public static final int HUD_ITEM_ROW_OFFSET_WITH_NAME = 16;
   public static final int HUD_ITEM_ROW_OFFSET_NO_TEXT = 0;
   public static final double HUD_ANCHOR_Y_ADJUST = 0.62;
   public static final double HUD_FRUSTUM_Y_PADDING = 1.25;
   public static final long HUD_CACHE_DURATION_MS = 125L;
   public static final int HEART_ICON_SIZE = 9;
   public static final int HEART_ICON_SPACING = 8;
   // RECOVERY NOTE: the obfuscator stripped this class's static initialiser. The values below
   // are taken verbatim from the string constants left in the obfuscated class file.
   public static final Identifier HEART_CONTAINER_TEXTURE = Identifier.ofVanilla("hud/heart/container");
   public static final Identifier HEART_FULL_TEXTURE = Identifier.ofVanilla("hud/heart/full");
   public static final Identifier HEART_HALF_TEXTURE = Identifier.ofVanilla("hud/heart/half");
   public static final Identifier HEART_ABS_FULL_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_full");
   public static final Identifier HEART_ABS_HALF_TEXTURE = Identifier.ofVanilla("hud/heart/absorbing_half");
   public static final Pattern MINECRAFT_COLOR_CODE_PATTERN = Pattern.compile("§.");
   public static final float PANEL_RADIUS = 3.0F;
   public static final float PANEL_PAD_X = 3.0F;
   public static final float PANEL_PAD_Y = 2.0F;
   public static final float PANEL_OUTLINE_THICKNESS = 1.0F;
   public static final int PANEL_BG_ALPHA = 150;
   public static final int PANEL_OUTLINE_ALPHA = 90;
   public static NameTags instance;
   public final Setting<Boolean> self = new Setting<>("Self", true);
   public final Setting<Boolean> name = new Setting<>("Name", true);
   public final Setting<Boolean> ping = new Setting<>("Ping", true);
   public final Setting<Boolean> health = new Setting<>("Health", true);
   public final Setting<Boolean> mainHand = new Setting<>("MainHand", true);
   public final Setting<Boolean> offHand = new Setting<>("OffHand", true);
   public final Setting<Boolean> armor = new Setting<>("Armor", true);
   public final Setting<Boolean> panel = new Setting<>("Panel", true);
   public final Map<UUID, NameTagData> hudCache = new HashMap<>();
   public final ProjectedPoint screenProjection = new ProjectedPoint();
   public int hudConfigSignature = Integer.MIN_VALUE;

   public NameTags() {
      super("NameTags", Category.MISC);
      instance = this;
      this.addSetting(this.self);
      this.addSetting(this.name);
      this.addSetting(this.ping);
      this.addSetting(this.health);
      this.addSetting(this.mainHand);
      this.addSetting(this.offHand);
      this.addSetting(this.armor);
      this.addSetting(this.panel);
   }

   public static boolean isActive() {
      return instance != null && instance.isEnabled() && mc != null && mc.player != null;
   }

   public static void renderHud(DrawContext context, float tickDelta) {
      if (isActive() && mc.world != null && !mc.options.hudHidden && !isClickGuiOpen()) {
         NameTags nameTags = instance;
         if (nameTags != null) {
            nameTags.invalidateOnConfigChange();
            nameTags.pruneCache();
            long i = System.currentTimeMillis();
            Camera camera = RenderUtils.getCamera();
            if (camera != null) {
               Vec3d vec3d = RenderUtils.getCameraPos(camera);
               double d0 = vec3d.x;
               double d1 = vec3d.y;
               double d2 = vec3d.z;
               double d3 = 4096.0;
               double d4 = mc.getWindow().getScaledWidth() * 0.5 * Math.abs(Render3D.projectionMatrix.m00()) * 0.025F;
               double d5 = mc.getWindow().getScaledHeight() * 0.5 * Math.abs(Render3D.projectionMatrix.m11()) * 0.025F;
               Matrix3x2fStack matrix3x2fStack = context.getMatrices();

               for (PlayerEntity playerEntity : mc.world.getPlayers()) {
                  if (nameTags.shouldRenderFor(playerEntity)) {
                     double d6 = MathHelper.lerp((double)tickDelta, playerEntity.lastRenderX, playerEntity.getX());
                     double d7 = MathHelper.lerp((double)tickDelta, playerEntity.lastRenderY, playerEntity.getY());
                     double d8 = MathHelper.lerp((double)tickDelta, playerEntity.lastRenderZ, playerEntity.getZ());
                     double d9 = d6 - d0;
                     double d10 = d7 - d1;
                     double d11 = d8 - d2;
                     double d12 = d9 * d9 + d10 * d10 + d11 * d11;
                     if (!(d12 > d3)) {
                        NameTagData nameTagData = nameTags.getOrBuildTagData(playerEntity, i);
                        if (!nameTagData.pu()) {
                           double d13 = Math.max(0.35, playerEntity.getWidth() * 0.5);
                           if (RenderUtils.isBoxVisible(d6 - d13, d7, d8 - d13, d6 + d13, d7 + playerEntity.getHeight() + 1.25, d8 + d13)) {
                              float f = nameTags.projectTagPosition(playerEntity, tickDelta, d6, d7, d8, d4, d5);
                              if (!(f <= 0.0F)) {
                                 matrix3x2fStack.pushMatrix();
                                 translateAndScale(matrix3x2fStack, (float)nameTags.screenProjection.x, (float)nameTags.screenProjection.y, f);
                                 if (nameTags.panel.getValue()) {
                                    drawNameTag(context, nameTagData);
                                 }

                                 drawNameLabel(context, nameTagData.nameLabel(), nameTagData.nameWidth(), 0, false);
                                 drawHealthBar(context, nameTagData.healthData(), nameTagData.nameLabel() != null ? 10 : 0);
                                 drawItems(context, nameTagData.items(), nameTagData.itemRowWidth(), nameTagData.nameLabel() != null, nameTagData.healthData() != null);
                                 matrix3x2fStack.popMatrix();
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void drawNameTag(DrawContext context, NameTagData data) {
      boolean flag = data.nameLabel() != null;
      boolean flag1 = data.healthData() != null;
      boolean flag2 = !data.items().isEmpty();
      if (flag || flag1 || flag2) {
         int i = flag ? 10 : 0;
         int j = flag1 ? 34 : (flag ? 16 : 0);
         float f = 9.0F;
         float f1 = Float.MAX_VALUE;
         float f2 = -Float.MAX_VALUE;
         float f3 = 0.0F;
         if (flag) {
            f1 = Math.min(f1, 0.0F);
            f2 = Math.max(f2, 0.0F + f);
            f3 = Math.max(f3, (float)data.nameWidth());
         }

         if (flag1) {
            f1 = Math.min(f1, (float)(-i));
            f2 = Math.max(f2, (float)(-i + 9));
            f3 = Math.max(f3, (float)data.healthData().totalWidth());
         }

         if (flag2) {
            f1 = Math.min(f1, (float)(-j));
            f2 = Math.max(f2, (float)(-j + 16));
            f3 = Math.max(f3, (float)data.itemRowWidth());
         }

         float f4 = f3 + 6.0F;
         float f5 = f2 - f1 + 4.0F;
         float f6 = -(f4 / 2.0F);
         float f7 = f1 - 2.0F;
         int k = withAlpha(Water.getBackgroundArgb(), 150);
         int l = withAlpha(Water.getAccentArgb(), 90);
         Render2D.drawRoundedRect(context, f6, f7, f4, f5, 3.0F, k, false);
         Render2D.drawRoundedOutline(context, f6, f7, f4, f5, 3.0F, 1.0F, l, false);
      }
   }

   public static int withAlpha(int argb, int alpha) {
      return (alpha & 0xFF) << 24 | argb & 16777215;
   }

   public boolean shouldRenderFor(LivingEntity entity) {
      if (!entity.isAlive() || entity instanceof ArmorStandEntity || !(entity instanceof PlayerEntity)) {
         return false;
      } else if (entity != mc.player && entity.isInvisibleTo(mc.player)) {
         return false;
      } else {
         return entity != mc.player
            ? true
            : this.self.getValue() && (!mc.options.getPerspective().isFirstPerson() || Freecam.instance != null && Freecam.instance.isEnabled());
      }
   }

   public boolean shouldRenderForState(LivingEntity entity, double distanceSq) {
      return this.shouldRenderFor(entity) && !isClickGuiOpen() ? distanceSq <= 4096.0 : false;
   }

   @Override
   public void onEnable() {
      this.hudCache.clear();
      this.hudConfigSignature = Integer.MIN_VALUE;
   }

   @Override
   public void onDisable() {
      this.hudCache.clear();
   }

   @Override
   public void onTick() {
   }

   public Text buildNameLabel(LivingEntity entity) {
      MutableText mutableText = Text.empty();
      boolean flag = false;
      if (this.name.getValue()) {
         mutableText.append(Text.literal("| ").formatted(Formatting.DARK_AQUA));
         mutableText.append(entity.getDisplayName().copy().formatted(Formatting.WHITE));
         flag = true;
      }

      if (this.ping.getValue() && entity instanceof PlayerEntity playerEntity) {
         int i = this.getPing(playerEntity);
         if (i >= 0) {
            if (flag) {
               mutableText.append(Text.literal(" ").formatted(Formatting.GRAY));
            }

            mutableText.append(Text.literal("[").formatted(Formatting.DARK_GRAY));
            mutableText.append(Text.literal(i + " ms").formatted(this.getPingColor(i)));
            mutableText.append(Text.literal("]").formatted(Formatting.DARK_GRAY));
            flag = true;
         }
      }

      if (this.health.getValue()) {
         float f = Math.max(0.0F, entity.getAbsorptionAmount());
         if (f > 0.0F) {
            int j = Math.max(1, MathHelper.ceil(f));
            if (flag) {
               mutableText.append(Text.literal(" ").formatted(Formatting.GRAY));
            }

            mutableText.append(Text.literal("+" + j).formatted(Formatting.GOLD));
            flag = true;
         }
      }

      return flag ? mutableText : null;
   }

   public HealthData buildHealthData(LivingEntity entity) {
      if (!this.health.getValue()) {
         return null;
      } else {
         float f = Math.max(1.0F, entity.getMaxHealth());
         float f1 = MathHelper.clamp(entity.getHealth(), 0.0F, f);
         float f2 = Math.max(0.0F, entity.getAbsorptionAmount());
         int i = Math.max(1, MathHelper.ceil(f / 2.0F));
         if (i > 10) {
            float f3 = 10.0F / i;
            f1 *= f3;
            f2 *= f3;
            i = 10;
         }

         int l1 = MathHelper.clamp(Math.round(f1), 0, i * 2);
         int j = l1 / 2;
         boolean flag = (l1 & 1) != 0;
         int k = Math.max(0, i - j - (flag ? 1 : 0));
         int l = Math.max(0, Math.round(f2));
         int i1 = l / 2;
         boolean flag1 = (l & 1) != 0;
         int j1 = i + i1 + (flag1 ? 1 : 0);
         if (j <= 0 && !flag && i1 <= 0 && !flag1 && k <= 0) {
            return null;
         } else {
            int k1 = (j1 - 1) * 8 + 9;
            return new HealthData(i, j, flag, k, i1, flag1, k1);
         }
      }
   }

   public List<ItemEntry> buildItemEntries(LivingEntity entity) {
      ArrayList arrayList = new ArrayList(6);
      if (this.offHand.getValue()) {
         this.addItemEntry(arrayList, entity.getOffHandStack());
      }

      if (this.armor.getValue()) {
         this.addItemEntry(arrayList, entity.getEquippedStack(EquipmentSlot.FEET));
         this.addItemEntry(arrayList, entity.getEquippedStack(EquipmentSlot.LEGS));
         this.addItemEntry(arrayList, entity.getEquippedStack(EquipmentSlot.CHEST));
         this.addItemEntry(arrayList, entity.getEquippedStack(EquipmentSlot.HEAD));
      }

      if (this.mainHand.getValue()) {
         this.addItemEntry(arrayList, entity.getMainHandStack());
      }

      return arrayList;
   }

   public void addItemEntry(List<ItemEntry> list, ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         list.add(new ItemEntry(stack.copy()));
      }
   }

   public static void drawItems(DrawContext context, List<ItemEntry> list, int totalWidth, boolean hasName, boolean hasHealth) {
      if (!list.isEmpty()) {
         int i = -(totalWidth / 2);
         int j = hasHealth ? 34 : (hasName ? 16 : 0);
         int k = -j;

         for (int l = 0; l < list.size(); l++) {
            ItemStack itemStack = ((ItemEntry)list.get(l)).stack();
            int i1 = i + l * 18;
            context.drawItem(itemStack, i1, k);
            context.drawStackOverlay(mc.textRenderer, itemStack, i1, k, null);
         }
      }
   }

   public static void drawNameLabel(DrawContext context, Text text, int width, int yOffset, boolean var4) {
      if (text != null) {
         int i = -(width / 2);
         int j = -yOffset;
         if (var4) {
            String s = text.getString();

            for (int k = -1; k <= 1; k++) {
               for (int l = -1; l <= 1; l++) {
                  if (k != 0 || l != 0) {
                     context.drawText(mc.textRenderer, s, i + k, j + l, -16777216, false);
                  }
               }
            }
         }

         context.drawText(mc.textRenderer, text, i, j, -1, false);
      }
   }

   public static void drawHealthBar(DrawContext context, HealthData health, int yOffset) {
      if (health != null) {
         int i = -(health.totalWidth() / 2);
         int j = -yOffset;

         for (int k = 0; k < health.baseHeartCount(); k++) {
            int l = i + k * 8;
            drawHeartIcon(context, HEART_CONTAINER_TEXTURE, l, j);
         }

         for (int k1 = 0; k1 < health.fullHearts(); k1++) {
            int j2 = i + k1 * 8;
            drawHeartIcon(context, HEART_FULL_TEXTURE, j2, j);
         }

         if (health.halfHeart()) {
            int l1 = i + health.fullHearts() * 8;
            drawHeartIcon(context, HEART_HALF_TEXTURE, l1, j);
         }

         int i2 = i + health.baseHeartCount() * 8;
         int k2 = health.absorptionFullHearts() + (health.absorptionHalfHeart() ? 1 : 0);

         for (int i1 = 0; i1 < k2; i1++) {
            int j1 = i2 + i1 * 8;
            drawHeartIcon(context, HEART_CONTAINER_TEXTURE, j1, j);
         }

         for (int l2 = 0; l2 < health.absorptionFullHearts(); l2++) {
            int j3 = i2 + l2 * 8;
            drawHeartIcon(context, HEART_ABS_FULL_TEXTURE, j3, j);
         }

         if (health.absorptionHalfHeart()) {
            int i3 = i2 + health.absorptionFullHearts() * 8;
            drawHeartIcon(context, HEART_ABS_HALF_TEXTURE, i3, j);
         }
      }
   }

   public static void drawHeartIcon(DrawContext context, Identifier id, int x, int y) {
      context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, id, x, y, 9, 9);
   }

   public void invalidateOnConfigChange() {
      int i = this.getConfigSignature();
      if (i != this.hudConfigSignature) {
         this.hudConfigSignature = i;
         this.hudCache.clear();
      }
   }

   public void pruneCache() {
      if (mc.world != null && this.hudCache.size() > mc.world.getPlayers().size() + 8) {
         this.hudCache.keySet().removeIf(var0 -> mc.world.getPlayerByUuid(var0) == null);
      }
   }

   public int getConfigSignature() {
      short short1 = 0;
      if (this.self.getValue()) {
         short1 |= 1;
      }

      if (this.name.getValue()) {
         short1 |= 2;
      }

      if (this.ping.getValue()) {
         short1 |= 4;
      }

      if (this.health.getValue()) {
         short1 |= 8;
      }

      if (this.mainHand.getValue()) {
         short1 |= 16;
      }

      if (this.offHand.getValue()) {
         short1 |= 32;
      }

      if (this.armor.getValue()) {
         short1 |= 64;
      }

      if (this.panel.getValue()) {
         short1 |= 128;
      }

      return short1;
   }

   public NameTagData getOrBuildTagData(PlayerEntity player, long now) {
      NameTagData nameTagData = this.hudCache.get(player.getUuid());
      if (nameTagData != null && nameTagData.expiresAtMs() > now) {
         return nameTagData;
      } else {
         Text text = this.buildNameLabel(player);
         List list = this.buildItemEntries(player);
         NameTagData nameTagData1 = new NameTagData(
            now + 125L,
            text,
            text != null ? mc.textRenderer.getWidth(text) : 0,
            this.buildHealthData(player),
            list,
            list.isEmpty() ? 0 : list.size() * 16 + (list.size() - 1) * 2
         );
         this.hudCache.put(player.getUuid(), nameTagData1);
         return nameTagData1;
      }
   }

   public static void translateAndScale(Matrix3x2fStack matrices, float x, float y, float scale) {
      matrices.translate(x, y);
      matrices.scale(scale, scale);
   }

   public float projectTagPosition(PlayerEntity player, float tickDelta, double x, double y, double z, double scaleX, double scaleY) {
      Vec3d vec3d = player.getAttachments().getPointNullable(EntityAttachmentType.NAME_TAG, 0, player.getLerpedYaw(tickDelta));
      double d0;
      double d1;
      double d2;
      if (vec3d == null) {
         d0 = x;
         d1 = y + player.getHeight() + 0.5 + 0.62;
         d2 = z;
      } else {
         d0 = x + vec3d.x;
         d1 = y + vec3d.y + 0.62;
         d2 = z + vec3d.z;
      }

      if (!Render3D.projectToPoint(Render3D.modelViewMatrix, Render3D.projectionMatrix, d0, d1, d2, this.screenProjection)) {
         return 0.0F;
      } else if (this.screenProjection.visible && !(this.screenProjection.z < 0.0) && !(this.screenProjection.z > 1.0) && !(this.screenProjection.w <= 0.0)) {
         double d3 = scaleX / this.screenProjection.w;
         double d4 = scaleY / this.screenProjection.w;
         float f = (float)((d3 + d4) * 0.5);
         return Float.isFinite(f) && f > 0.0F ? f : 0.0F;
      } else {
         return 0.0F;
      }
   }

   public static boolean isClickGuiOpen() {
      return mc.currentScreen instanceof ClickGuiScreen;
   }

   public int getPing(PlayerEntity player) {
      if (mc.getNetworkHandler() == null) {
         return -1;
      } else {
         PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
         return playerListEntry != null ? playerListEntry.getLatency() : -1;
      }
   }

   public Formatting getPingColor(int ping) {
      if (ping < 75) {
         return Formatting.GREEN;
      } else {
         return ping < 150 ? Formatting.YELLOW : Formatting.RED;
      }
   }

   public String stripColorCodes(String text) {
      return text != null && !text.isEmpty() ? MINECRAFT_COLOR_CODE_PATTERN.matcher(text).replaceAll("").trim() : "";
   }

   public boolean containsLetter(String text) {
      for (int i = 0; i < text.length(); i++) {
         if (Character.isLetter(text.charAt(i))) {
            return true;
         }
      }

      return false;
   }

   public String stripNewlines(String text) {
      return text == null ? "" : text.replace('\n', ' ').replace('\r', ' ');
   }

   public String normalize(String text) {
      return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
   }

   public static String watermarkFragment() {
      return "5";
   }
}
