package com.moulberry.axiom.mixin.fluid_opacity;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.hooks.SpriteContentsExt;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TextureAtlas.class})
public abstract class MixinTextureAtlas extends AbstractTexture {
   @Shadow
   @Final
   private ResourceLocation location;
   @Shadow
   @Final
   @Deprecated
   public static ResourceLocation LOCATION_BLOCKS;
   @Unique
   private int lastOpacity = 255;
   @Unique
   private byte ticks = 0;

   @Inject(
      method = {"cycleAnimationFrames"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/texture/TextureAtlas;bind()V",
         shift = Shift.AFTER
      )}
   )
   public void cycleAnimationFrames(CallbackInfo ci) {
      if (this.location.equals(LOCATION_BLOCKS)) {
         if (Minecraft.getInstance().getOverlay() instanceof LoadingOverlay) {
            this.ticks = 0;
            return;
         }

         if (this.ticks < 20) {
            this.ticks++;
            return;
         }

         int opacity = (int)(Axiom.configuration.visuals.liquidOpacity / 100.0F * 255.0F);
         if (this.lastOpacity == opacity && opacity >= 255) {
            return;
         }

         this.lastOpacity = opacity;

         for (Fluid fluid : BuiltInRegistries.FLUID) {
            FluidRenderHandler fluidRenderHandler = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
            if (fluidRenderHandler != null) {
               FluidState fluidState = fluid.defaultFluidState();

               try {
                  if (ChunkRenderOverrider.AxiomChunkOverrideLayer.fromVanilla(ItemBlockRenderTypes.getRenderLayer(fluidState)).isTranslucent) {
                     for (TextureAtlasSprite sprite : fluidRenderHandler.getFluidSprites(null, null, fluidState)) {
                        try {
                           if (sprite != null) {
                              SpriteContents contents = sprite.contents();
                              ((SpriteContentsExt)contents).axiom$setOpacity(opacity, sprite.getX(), sprite.getY());
                              if (contents.getUniqueFrames().count() <= 1L) {
                                 contents.uploadFirstFrame(sprite.getX(), sprite.getY());
                              }
                           }
                        } catch (Exception var12) {
                        }
                     }
                  }
               } catch (Exception var13) {
               }
            }
         }
      }
   }
}
