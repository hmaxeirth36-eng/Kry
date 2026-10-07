package com.water.module.modules.donut;

import com.water.module.Category;
import com.water.module.Module;
import com.water.module.setting.ModeSetting;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class FakeRoles extends Module {
   public static FakeRoles instance;
   public static final String MODE_NONE = "None";
   public static final String MODE_SRMOD = "SRMOD";
   public static final String MODE_MEDIA = "MEDIA";
   public static final String MODE_SRADMIN = "SRADMIN";
   public static final int TAG_BRACKET = 8355711;
   public static final int TAG_SRMOD = 5635925;
   public static final int TAG_MEDIA = 16733695;
   public static final int TAG_SRADMIN = 16733525;
   public static final int TAG_WHITE = 16777215;
   public final ModeSetting role = new ModeSetting("Role", "None", "None", "SRMOD", "MEDIA", "SRADMIN");

   public FakeRoles() {
      super("FakeRoles", Category.DONUT);
      instance = this;
      this.addSetting(this.role);
   }

   public static boolean isActive() {
      return instance != null && instance.isEnabled() && !instance.role.is("None") && mc != null && mc.player != null;
   }

   public static String getSelectedRole() {
      return !isActive() ? null : instance.role.getValue();
   }

   public static String getSelfUsername() {
      return mc != null && mc.getSession() != null ? mc.getSession().getUsername() : null;
   }

   public static Text applyToText(Text input) {
      if (isActive() && input != null) {
         String s = getSelfUsername();
         if (s != null && !s.isBlank()) {
            String s1 = input.getString();
            if (!s1.contains(s)) {
               return input;
            } else {
               Text text = buildRoleText(s);
               int i = s1.indexOf(s);
               MutableText mutableText = Text.empty();
               if (i > 0) {
                  mutableText.append(Text.literal(s1.substring(0, i)));
               }

               mutableText.append(text);
               int j = i + s.length();
               if (j < s1.length()) {
                  mutableText.append(Text.literal(s1.substring(j)));
               }

               return mutableText;
            }
         } else {
            return input;
         }
      } else {
         return input;
      }
   }

   public static Text buildRoleName(String text) {
      return isActive() && text != null ? buildRoleText(text) : null;
   }

   public static String getDisplayName() {
      if (!isActive()) {
         return null;
      } else {
         String s = getSelfUsername();
         if (s == null) {
            return null;
         } else {
            Text text = buildRoleText(s);
            return text.getString();
         }
      }
   }

   public static Style getRoleStyle() {
      if (!isActive()) {
         return Style.EMPTY;
      } else {
         String s = instance.role.getValue();

         return switch (s) {
            case "SRMOD" -> boldColored(5635925);
            case "MEDIA" -> boldColored(16733695);
            case "SRADMIN" -> boldColored(16733525);
            default -> Style.EMPTY;
         };
      }
   }

   public static Style getBracketStyle() {
      return isActive() ? Style.EMPTY.withColor(TextColor.fromRgb(8355711)).withBold(false) : Style.EMPTY;
   }

   public static Style getStyleForChar(int var0) {
      return var0 != 91 && var0 != 93 && !Character.isWhitespace(var0) ? getRoleStyle() : getBracketStyle();
   }

   public static Style getNameStyle() {
      if (!isActive()) {
         return Style.EMPTY;
      } else {
         String s = instance.role.getValue();

         return switch (s) {
            case "SRMOD" -> boldColored(5635925);
            case "MEDIA" -> Style.EMPTY.withColor(TextColor.fromRgb(16777215)).withBold(false);
            case "SRADMIN" -> boldColored(16733525);
            default -> Style.EMPTY;
         };
      }
   }

   public static String getRolePrefix() {
      if (!isActive()) {
         return null;
      } else {
         String s = instance.role.getValue();

         return switch (s) {
            case "SRMOD" -> "[SR.MOD] ";
            case "MEDIA" -> "[MEDIA] ";
            case "SRADMIN" -> "[SR.ADMIN] ";
            default -> null;
         };
      }
   }

   public static Text buildRoleText(String text) {
      String s = instance.role.getValue();

      return (Text)(switch (s) {
         case "SRMOD" -> buildSrModText(text);
         case "MEDIA" -> buildMediaText(text);
         case "SRADMIN" -> buildSrAdminText(text);
         default -> Text.literal(text);
      });
   }

   public static Text buildSrModText(String text) {
      MutableText mutableText = Text.empty();
      appendTag(mutableText, "SR.MOD", boldColored(5635925));
      mutableText.append(Text.literal(text).setStyle(getNameStyle()));
      return mutableText;
   }

   public static Text buildMediaText(String text) {
      MutableText mutableText = Text.empty();
      appendTag(mutableText, "MEDIA", boldColored(16733695));
      mutableText.append(Text.literal(text).setStyle(getNameStyle()));
      return mutableText;
   }

   public static Text buildSrAdminText(String text) {
      MutableText mutableText = Text.empty();
      appendTag(mutableText, "SR.ADMIN", boldColored(16733525));
      mutableText.append(Text.literal(text).setStyle(getNameStyle()));
      return mutableText;
   }

   public static void appendTag(MutableText out, String label, Style labelStyle) {
      Style bracketStyle = Style.EMPTY.withColor(TextColor.fromRgb(8355711)).withBold(false);
      out.append(Text.literal("[").setStyle(bracketStyle));
      out.append(Text.literal(label).setStyle(labelStyle));
      out.append(Text.literal("] ").setStyle(bracketStyle));
   }

   public static Style boldColored(int var0) {
      return Style.EMPTY.withColor(TextColor.fromRgb(var0)).withBold(true);
   }
}
