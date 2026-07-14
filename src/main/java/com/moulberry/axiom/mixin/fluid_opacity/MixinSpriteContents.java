package com.moulberry.axiom.mixin.fluid_opacity;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.hooks.SpriteContentsExt;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({SpriteContents.class})
public abstract class MixinSpriteContents implements SpriteContentsExt {
   @Shadow
   private NativeImage[] byMipLevel;
   @Shadow
   @Final
   private NativeImage originalImage;
   @Unique
   private int opacity = 255;

   @Shadow
   public abstract void increaseMipLevel(int var1);

   @Override
   public void axiom$setOpacity(int opacity, int x, int y) {
      if (this.opacity != opacity) {
         this.opacity = opacity;
         int originalMipLevels = this.byMipLevel.length;
         NativeImage newImage;
         if (opacity >= 255) {
            newImage = this.originalImage;
         } else if (opacity <= 0) {
            newImage = new NativeImage(this.originalImage.getWidth(), this.originalImage.getHeight(), true);
         } else {
            IntUnaryOperator operator = rgba -> {
               int alpha = rgba >> 24 & 0xFF;
               alpha = alpha * opacity / 255;
               return rgba & 16777215 | alpha << 24;
            };
            newImage = new NativeImage(this.originalImage.getWidth(), this.originalImage.getHeight(), false);
            newImage.copyFrom(this.originalImage);
            newImage.applyToAllPixels(operator);
         }

         this.byMipLevel = MipmapGenerator.generateMipLevels(new NativeImage[]{newImage}, originalMipLevels - 1);
      }
   }

   @Mixin({Dummy.class})
   private static class MixinSpriteContentsAnimatedTexture {
   }

   @Mixin({Dummy.class})
   private static class MixinSpriteContentsAnimationState {
   }
}
