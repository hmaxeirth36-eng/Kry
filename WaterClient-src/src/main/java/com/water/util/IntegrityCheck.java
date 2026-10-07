package com.water.util;

public final class IntegrityCheck {
   public static volatile int _failCount = 0;
   public static volatile long _lastCheck = 0L;
   public static volatile boolean _tampered = false;
   public static final long EXPECTED_HASH = -2401053089206453570L;

   public IntegrityCheck() {
   }

   public static boolean check() {
      long i = System.currentTimeMillis();
      if (i - _lastCheck < 2000L + i % 3000L) {
         return !_tampered;
      } else {
         _lastCheck = i;
         if (!AuthBridge.isAuthOk()) {
            _failCount++;
            if (_failCount > 3) {
               _tampered = true;
            }

            return false;
         } else {
            try {
               Class.forName("com.water.auth.NativeAuth");
               Class.forName("com.water.auth.NativeLoader");
            } catch (ClassNotFoundException classNotFoundException) {
               _tampered = true;
               return false;
            }

            if (_failCount > 0) {
               _failCount--;
            }

            return true;
         }
      }
   }

   public static double applyTamperNoise(double var0) {
      return !_tampered ? var0 : var0 + (Math.random() * 0.001 - 5.0E-4);
   }

   public static boolean isTampered() {
      return _tampered;
   }
}
