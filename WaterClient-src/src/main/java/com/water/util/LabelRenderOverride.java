package com.water.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.text.Text;

public final class LabelRenderOverride {
   public static final Set<EntityRenderState> OVERRIDDEN_LABELS = Collections.newSetFromMap(new WeakHashMap<>());

   public LabelRenderOverride() {
   }

   public static void clearOverride(EntityRenderState entityRenderState) {
      OVERRIDDEN_LABELS.remove(entityRenderState);
   }

   public static void markOverridden(EntityRenderState entityRenderState) {
      OVERRIDDEN_LABELS.add(entityRenderState);
   }

   public static boolean isOverridden(EntityRenderState entityRenderState) {
      return OVERRIDDEN_LABELS.contains(entityRenderState);
   }

   public static boolean shouldHideLabel(Text text) {
      return false;
   }
}
