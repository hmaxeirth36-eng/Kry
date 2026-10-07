package com.water.mixin;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import net.minecraft.client.Keyboard;

public class TestReflection {
   public TestReflection() {
   }

   public static void main(String[] text) {
      System.out.println("Methods in Keyboard:");

      for (Method method : Keyboard.class.getDeclaredMethods()) {
         if (method.getName().equals("onChar")) {
            System.out.println(method.getName());

            for (Parameter parameter : method.getParameters()) {
               System.out.println("  " + parameter.getType().getName());
            }
         }
      }
   }
}
