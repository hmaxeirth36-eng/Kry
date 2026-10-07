package com.water.util;

import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Collections;

public class HwidUtil {
   private static final int[] _K = new int[]{78, 26, 127, 51, 155, 44, 136, 212};

   public HwidUtil() {
   }

   private static String toHex(byte[] var0) {
      char[] achar = new char[var0.length * 2];

      for (int i = 0; i < var0.length; i++) {
         int j = var0[i] & 255;
         achar[i * 2] = "0123456789ABCDEF".charAt(j >> 4);
         achar[i * 2 + 1] = "0123456789ABCDEF".charAt(j & 15);
      }

      return new String(achar);
   }

   private static String decodePropertyName(int var0) {
      int[][] aint = new int[][]{{97, 117, 116}, {111, 117, 104, 55, 129, 43, 144, 194}, {107, 68, 106, 33}};
      int[] aint1 = aint[var0];
      char[] achar = new char[aint1.length];

      for (int i = 0; i < aint1.length; i++) {
         achar[i] = (char)(aint1[i] ^ _K[i % 8]);
      }

      return new String(achar);
   }

   public static String getHwid() {
      try {
         MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
         MessageDigest messageDigest2 = MessageDigest.getInstance("MD5");

         for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            try {
               if (!networkInterface.isLoopback()) {
                  byte[] abyte = networkInterface.getHardwareAddress();
                  if (abyte != null) {
                     messageDigest.update(abyte);
                     messageDigest2.update(abyte);
                  }

                  byte[] abyte1 = networkInterface.getName().getBytes();
                  messageDigest.update(abyte1);
                  messageDigest2.update(abyte1);
               }
            } catch (Exception exception2) {
            }
         }

         for (int j = 0; j < 3; j++) {
            try {
               String s = System.getProperty(decodePropertyName(j), "?");
               messageDigest.update(s.getBytes());
               messageDigest2.update(s.getBytes());
            } catch (Exception exception1) {
            }
         }

         byte[] abyte2 = String.valueOf(Runtime.getRuntime().availableProcessors() * 2654435769L).getBytes();
         messageDigest.update(abyte2);
         byte[] abyte3 = messageDigest.digest();
         messageDigest2.update(abyte3);
         byte[] abyte4 = messageDigest2.digest();
         byte[] abyte5 = new byte[16];

         for (int i = 0; i < 16; i++) {
            abyte5[i] = (byte)(abyte3[i] ^ abyte4[i] ^ _K[i % 8]);
         }

         return toHex(abyte5).substring(0, 16);
      } catch (Exception exception3) {
         try {
            MessageDigest messageDigest1 = MessageDigest.getInstance("SHA-256");
            messageDigest1.update(String.valueOf(System.nanoTime() ^ 3735928559L).getBytes());
            return toHex(messageDigest1.digest()).substring(0, 16);
         } catch (Exception exception) {
            return Long.toHexString(System.nanoTime()).toUpperCase().substring(0, 16);
         }
      }
   }
}
