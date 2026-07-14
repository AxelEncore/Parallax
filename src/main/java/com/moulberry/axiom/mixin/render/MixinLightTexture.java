package com.moulberry.axiom.mixin.render;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LightTexture.class})
public class MixinLightTexture {
   @Redirect(
      method = {"updateLightTexture"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LightTexture;clampColor(Lorg/joml/Vector3f;)V"
      )
   )
   public void updateLightTextureClamp(Vector3f vector3f) {
      float r = vector3f.x;
      float g = vector3f.y;
      float b = vector3f.z;
      if (AxiomClient.isAxiomActive()) {
         float minBrightness = Axiom.configuration.visuals.minBrightness / 100.0F;
         float sqr = minBrightness * minBrightness;
         float factor = 0.2F * minBrightness + 0.8F * sqr * sqr;
         r = r * (1.0F - factor) + factor;
         g = g * (1.0F - factor) + factor;
         b = b * (1.0F - factor) + factor;
      }

      vector3f.set(Mth.clamp(r, 0.0F, 1.0F), Mth.clamp(g, 0.0F, 1.0F), Mth.clamp(b, 0.0F, 1.0F));
   }
}
