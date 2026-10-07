package com.water.render;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class EspBlurFramebuffer {
   public static int fboGeometry = -1;
   public static int texGeometry = -1;
   public static int fboBlurH = -1;
   public static int texBlurH = -1;
   public static int fboBlurV = -1;
   public static int texBlurV = -1;
   public static int progBlurH = -1;
   public static int progBlurV = -1;
   public static int progComposite = -1;
   public static int lastW = -1;
   public static int lastH = -1;
   public static boolean active = false;
   public static int savedFbo = 0;
   public static boolean shadersOk = false;
   public static float glowStrength = 2.0F;

   public EspBlurFramebuffer() {
   }

   public static void begin() {
      if (!active) {
         MinecraftClient minecraftClient = MinecraftClient.getInstance();
         int i = minecraftClient.getFramebuffer().textureWidth;
         int j = minecraftClient.getFramebuffer().textureHeight;

         try {
            ensureBuffers(i, j);
         } catch (Exception exception) {
            System.err.println("[ESPGlowRenderer] Shader init failed: " + exception.getMessage());
            shadersOk = false;
            return;
         }

         if (shadersOk) {
            savedFbo = GL11.glGetInteger(36006);
            GL30.glBindFramebuffer(36160, fboGeometry);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClear(16384);
            active = true;
         }
      }
   }

   public static void end() {
      if (active && shadersOk) {
         active = false;
         int i = lastW;
         int j = lastH;
         boolean flag = GL11.glIsEnabled(3042);
         boolean flag1 = GL11.glIsEnabled(2929);
         boolean flag2 = GL11.glGetBoolean(2930);
         int k = GL11.glGetInteger(32969);
         int l = GL11.glGetInteger(32968);
         GL11.glDisable(2929);
         GL11.glDepthMask(false);
         GL30.glBindFramebuffer(36160, fboBlurH);
         GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glClear(16384);
         GL11.glDisable(3042);
         GL20.glUseProgram(progBlurH);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, texGeometry);
         GL20.glUniform1i(GL20.glGetUniformLocation(progBlurH, "Sampler0"), 0);
         GL20.glUniform2f(GL20.glGetUniformLocation(progBlurH, "uResolution"), i, j);
         GL11.glDrawArrays(4, 0, 6);
         GL30.glBindFramebuffer(36160, fboBlurV);
         GL11.glClear(16384);
         GL20.glUseProgram(progBlurV);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, texBlurH);
         GL20.glUniform1i(GL20.glGetUniformLocation(progBlurV, "Sampler0"), 0);
         GL20.glUniform2f(GL20.glGetUniformLocation(progBlurV, "uResolution"), i, j);
         GL11.glDrawArrays(4, 0, 6);
         GL30.glBindFramebuffer(36160, savedFbo);
         GL11.glEnable(3042);
         GL14.glBlendFuncSeparate(770, 771, 1, 771);
         GL20.glUseProgram(progComposite);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, texGeometry);
         GL20.glUniform1i(GL20.glGetUniformLocation(progComposite, "Sampler0"), 0);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, texBlurV);
         GL20.glUniform1i(GL20.glGetUniformLocation(progComposite, "Sampler1"), 1);
         GL20.glUniform1f(GL20.glGetUniformLocation(progComposite, "uGlowStrength"), glowStrength);
         GL11.glDrawArrays(4, 0, 6);
         GL20.glUseProgram(0);
         GL13.glActiveTexture(33985);
         GL11.glBindTexture(3553, 0);
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, 0);
         GL14.glBlendFuncSeparate(k, l, k, l);
         if (!flag) {
            GL11.glDisable(3042);
         }

         if (flag1) {
            GL11.glEnable(2929);
         }

         GL11.glDepthMask(flag2);
      }
   }

   public static boolean isActive() {
      return active;
   }

   public static void ensureBuffers(int var0, int var1) {
      if (var0 != lastW || var1 != lastH || fboGeometry == -1 || !shadersOk) {
         lastW = var0;
         lastH = var1;
         deleteBuffers();
         texGeometry = createTexture(var0, var1);
         texBlurH = createTexture(var0, var1);
         texBlurV = createTexture(var0, var1);
         fboGeometry = createFramebuffer(texGeometry);
         fboBlurH = createFramebuffer(texBlurH);
         fboBlurV = createFramebuffer(texBlurV);
         String s = readShaderSource("esp_glow_vertex.vsh");
         progBlurH = compileProgram(s, readShaderSource("esp_blur_h_fragment.fsh"));
         progBlurV = compileProgram(s, readShaderSource("esp_blur_v_fragment.fsh"));
         progComposite = compileProgram(s, readShaderSource("esp_composite_fragment.fsh"));
         shadersOk = true;
      }
   }

   public static int createTexture(int var0, int var1) {
      int i = GL11.glGenTextures();
      GL11.glBindTexture(3553, i);
      GL11.glTexImage2D(3553, 0, 6408, var0, var1, 0, 6408, 5121, (ByteBuffer)null);
      GL11.glTexParameteri(3553, 10241, 9729);
      GL11.glTexParameteri(3553, 10240, 9729);
      GL11.glTexParameteri(3553, 10242, 33071);
      GL11.glTexParameteri(3553, 10243, 33071);
      return i;
   }

   public static int createFramebuffer(int var0) {
      int i = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(36160, i);
      GL30.glFramebufferTexture2D(36160, 36064, 3553, var0, 0);
      GL30.glBindFramebuffer(36160, 0);
      return i;
   }

   public static void deleteBuffers() {
      if (fboGeometry != -1) {
         GL30.glDeleteFramebuffers(fboGeometry);
         fboGeometry = -1;
      }

      if (fboBlurH != -1) {
         GL30.glDeleteFramebuffers(fboBlurH);
         fboBlurH = -1;
      }

      if (fboBlurV != -1) {
         GL30.glDeleteFramebuffers(fboBlurV);
         fboBlurV = -1;
      }

      if (texGeometry != -1) {
         GL11.glDeleteTextures(texGeometry);
         texGeometry = -1;
      }

      if (texBlurH != -1) {
         GL11.glDeleteTextures(texBlurH);
         texBlurH = -1;
      }

      if (texBlurV != -1) {
         GL11.glDeleteTextures(texBlurV);
         texBlurV = -1;
      }

      shadersOk = false;
   }

   public static String readShaderSource(String text) {
      try {
         String s;
         try (InputStream inputStream = EspBlurFramebuffer.class.getResourceAsStream("/assets/water/shaders/" + text)) {
            if (inputStream == null) {
               throw new RuntimeException("Not found: " + text);
            }

            s = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
         }

         return s;
      } catch (Exception exception) {
         throw new RuntimeException("Load failed: " + text, exception);
      }
   }

   public static int compileProgram(String text, String text2) {
      int i = GL20.glCreateShader(35633);
      GL20.glShaderSource(i, text);
      GL20.glCompileShader(i);
      if (GL20.glGetShaderi(i, 35713) == 0) {
         throw new RuntimeException("VSH: " + GL20.glGetShaderInfoLog(i));
      } else {
         int j = GL20.glCreateShader(35632);
         GL20.glShaderSource(j, text2);
         GL20.glCompileShader(j);
         if (GL20.glGetShaderi(j, 35713) == 0) {
            throw new RuntimeException("FSH: " + GL20.glGetShaderInfoLog(j));
         } else {
            int k = GL20.glCreateProgram();
            GL20.glAttachShader(k, i);
            GL20.glAttachShader(k, j);
            GL20.glLinkProgram(k);
            GL20.glDeleteShader(i);
            GL20.glDeleteShader(j);
            return k;
         }
      }
   }
}
