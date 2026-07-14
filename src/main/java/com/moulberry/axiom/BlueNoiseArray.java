package com.moulberry.axiom;

import io.netty.buffer.Unpooled;
import java.io.InputStream;
import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;

public class BlueNoiseArray {
   public static float[] NOISE = new float[32768];

   static {
      InputStream inputStream = BlueNoiseArray.class.getClassLoader().getResourceAsStream("bluenoise.bin");

      try {
         byte[] bytes = inputStream.readAllBytes();
         FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

         for (int i = 0; i < NOISE.length; i++) {
            NOISE[i] = friendlyByteBuf.readFloat();
            if (NOISE[i] < 0.0F || NOISE[i] > 1.0F) {
               throw new RuntimeException();
            }
         }
      } catch (Exception var4) {
         Arrays.fill(NOISE, 0.5F);
         var4.printStackTrace();
      }
   }
}
