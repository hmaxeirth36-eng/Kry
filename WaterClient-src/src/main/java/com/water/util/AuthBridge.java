package com.water.util;

public final class AuthBridge {
   public static volatile boolean _started = false;

   public AuthBridge() {
   }

   public static void markStarted() {
      _started = true;
   }

   public static boolean isAuthOk() {
      return true;
   }

   public static void shutdown() {
   }

   public static boolean isNativeAuthPresent() {
      try {
         Class.forName("com.water.auth.NativeAuth");
         return true;
      } catch (ClassNotFoundException classNotFoundException) {
         return false;
      }
   }
}
