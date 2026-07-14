package com.moulberry.axiom.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.hooks.NativeImageExt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import org.lwjgl.stb.STBImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({NativeImage.class})
public abstract class MixinNativeImage implements NativeImageExt {
   @Shadow
   protected abstract boolean writeToChannel(WritableByteChannel var1) throws IOException;

   @Override
   public byte[] axiom$asByteArray() {
      try {
         byte[] var3;
         try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (WritableByteChannel writableByteChannel = Channels.newChannel(byteArrayOutputStream)) {
               if (!this.writeToChannel(writableByteChannel)) {
                  throw new IOException("Could not write image to byte array: " + STBImage.stbi_failure_reason());
               }

               var3 = byteArrayOutputStream.toByteArray();
            } catch (Exception var8) {
               throw new RuntimeException(var8);
            }
         }

         return var3;
      } catch (Exception var10) {
         throw new RuntimeException(var10);
      }
   }
}
