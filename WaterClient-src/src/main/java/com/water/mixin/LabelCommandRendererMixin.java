package com.water.mixin;

import com.water.util.LabelRenderOverride;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LabelCommandRenderer.class})
public class LabelCommandRendererMixin {
   public LabelCommandRendererMixin() {
   }

   @Redirect(
      method = {"render"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
         ordinal = 1
      ),
      require = 0
   )
   private void water$drawHealthLabelsWithOutline(
      TextRenderer textRenderer,
      Text text,
      float var3,
      float var4,
      int var5,
      boolean var6,
      Matrix4f matrix,
      VertexConsumerProvider vertexConsumerProvider,
      TextLayerType textLayerType,
      int var10,
      int var11
   ) {
      if (!LabelRenderOverride.shouldHideLabel(text)) {
         textRenderer.draw(text, var3, var4, var5, var6, matrix, vertexConsumerProvider, textLayerType, var10, var11);
      } else {
         textRenderer.drawWithOutline(text.asOrderedText(), var3, var4, var5, -16777216, matrix, vertexConsumerProvider, var11);
      }
   }
}
