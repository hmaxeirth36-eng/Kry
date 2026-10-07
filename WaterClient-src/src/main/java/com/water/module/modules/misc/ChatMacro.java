package com.water.module.modules.misc;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.Setting;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public final class ChatMacro extends Module {
   public final Setting<String> text1 = new Setting<>("Macro 1 Text", "");
   public final Setting<Integer> key1 = new Setting<>("Macro 1 Key", 0, 0, 348);
   public final Setting<String> text2 = new Setting<>("Macro 2 Text", "");
   public final Setting<Integer> key2 = new Setting<>("Macro 2 Key", 0, 0, 348);
   public final Setting<String> text3 = new Setting<>("Macro 3 Text", "");
   public final Setting<Integer> key3 = new Setting<>("Macro 3 Key", 0, 0, 348);
   public final Setting<String> text4 = new Setting<>("Macro 4 Text", "");
   public final Setting<Integer> key4 = new Setting<>("Macro 4 Key", 0, 0, 348);
   public final Setting<String> text5 = new Setting<>("Macro 5 Text", "");
   public final Setting<Integer> key5 = new Setting<>("Macro 5 Key", 0, 0, 348);
   public final Map<Integer, Boolean> prevStates = new HashMap<>();

   public ChatMacro() {
      super("Chat Macro", Category.MISC);
      this.addSetting(this.text1);
      this.addSetting(this.key1);
      this.addSetting(this.text2);
      this.addSetting(this.key2);
      this.addSetting(this.text3);
      this.addSetting(this.key3);
      this.addSetting(this.text4);
      this.addSetting(this.key4);
      this.addSetting(this.text5);
      this.addSetting(this.key5);
   }

   @Override
   public void onTick() {
      if (mc.player != null && mc.currentScreen == null) {
         this.handleMacroKey(this.key1.getValue(), this.text1.getValue());
         this.handleMacroKey(this.key2.getValue(), this.text2.getValue());
         this.handleMacroKey(this.key3.getValue(), this.text3.getValue());
         this.handleMacroKey(this.key4.getValue(), this.text4.getValue());
         this.handleMacroKey(this.key5.getValue(), this.text5.getValue());
      }
   }

   public void handleMacroKey(int var1, String text) {
      if (var1 > 0 && text != null && !text.isBlank()) {
         boolean flag = this.isKeyDown(var1);
         boolean flag1 = this.prevStates.getOrDefault(var1, false);
         if (flag && !flag1) {
            this.sendChatOrCommand(text.trim());
         }

         this.prevStates.put(var1, flag);
      }
   }

   public boolean isKeyDown(int var1) {
      if (mc.getWindow() == null) {
         return false;
      } else {
         try {
            return GLFW.glfwGetKey(mc.getWindow().getHandle(), var1) == 1;
         } catch (Exception exception) {
            return false;
         }
      }
   }

   public void sendChatOrCommand(String text) {
      mc.execute(() -> {
         if (mc.player != null && mc.getNetworkHandler() != null) {
            if (text.startsWith("/")) {
               mc.getNetworkHandler().sendChatCommand(text.substring(1));
            } else {
               mc.getNetworkHandler().sendChatMessage(text);
            }
         }
      });
   }
}
