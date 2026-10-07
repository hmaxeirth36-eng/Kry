package com.water.gui;

import com.water.module.modules.client.SpotifyHUD;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;

public class WaterChatScreen extends ChatScreen {
   public WaterChatScreen(String text) {
      super(text, false);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      double d0 = click.x();
      double d1 = click.y();
      int i = click.button();
      if (HudEditor.INSTANCE.onMouseClick(d0, d1, i)) {
         return true;
      } else {
         return i == 0 && SpotifyHUD.handleClick(d0, d1) ? true : super.mouseClicked(click, doubled);
      }
   }

   @Override
   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      if (HudEditor.INSTANCE.isDragging()) {
         HudEditor.INSTANCE.onMouseDrag(click.x(), click.y());
         return true;
      } else {
         return super.mouseDragged(click, offsetX, offsetY);
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      HudEditor.INSTANCE.onMouseRelease();
      return super.mouseReleased(click);
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      HudEditor.INSTANCE.render(context, mouseX, mouseY);
   }

   public static String watermarkFragment() {
      return "T";
   }
}
