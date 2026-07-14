package com.moulberry.axiom.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.joml.FrustumIntersection;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Frustum.class})
public class MixinFrustum {
   @Shadow
   private double camX;
   @Shadow
   private double camY;
   @Shadow
   private double camZ;
   @Shadow
   @Final
   private FrustumIntersection intersection;
   @Shadow
   private Vector4f viewVector;

   @Inject(
      method = {"offsetToFullyIncludeCameraCube"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void offsetToFullyIncludeCameraCube(int i, CallbackInfoReturnable<Frustum> cir) {
      double d = Math.floor(this.camX / i) * i;
      double e = Math.floor(this.camY / i) * i;
      double f = Math.floor(this.camZ / i) * i;
      double g = Math.ceil(this.camX / i) * i;
      double h = Math.ceil(this.camY / i) * i;
      double j = Math.ceil(this.camZ / i) * i;
      int count = 0;

      while (
         this.intersection
               .intersectAab(
                  (float)(d - this.camX),
                  (float)(e - this.camY),
                  (float)(f - this.camZ),
                  (float)(g - this.camX),
                  (float)(h - this.camY),
                  (float)(j - this.camZ)
               )
            != -2
      ) {
         this.camX = this.camX - this.viewVector.x() * 4.0F;
         this.camY = this.camY - this.viewVector.y() * 4.0F;
         this.camZ = this.camZ - this.viewVector.z() * 4.0F;
         if (count++ > 64) {
            cir.setReturnValue((Frustum)(Object)this);
            return;
         }
      }

      cir.setReturnValue((Frustum)(Object)this);
   }
}
