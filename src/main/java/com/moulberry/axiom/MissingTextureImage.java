package com.moulberry.axiom;

import com.mojang.blaze3d.systems.RenderSystem;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.utils.ColourUtils;
import net.minecraft.client.renderer.texture.DynamicTexture;

public class MissingTextureImage {
   private static DynamicTexture missingTexture = null;

   public static DynamicTexture getDynamicTexture() {
      if (missingTexture == null && RenderSystem.isOnRenderThread()) {
         missingTexture = VersionUtilsClient.helperCreateNewWithSizeDynamicTexture((String)null, 16, 16, false);
         int width = 16;
         int height = 16;

         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               if (y < height / 2 ^ x < width / 2) {
                  missingTexture.getPixels().setPixelRGBA(x, y, ColourUtils.argbToAbgr(-524040));
               } else {
                  missingTexture.getPixels().setPixelRGBA(x, y, ColourUtils.argbToAbgr(-16777216));
               }
            }
         }

         missingTexture.upload();
      }

      return missingTexture;
   }

   public static AxiomGpuTexture getTextureId() {
      return new AxiomGpuTexture(getDynamicTexture().getId());
   }
}
