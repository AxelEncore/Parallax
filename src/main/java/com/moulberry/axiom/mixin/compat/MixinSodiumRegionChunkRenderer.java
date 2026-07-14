package com.moulberry.axiom.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import net.caffeinemc.mods.sodium.client.gl.device.MultiDrawBatch;
import net.caffeinemc.mods.sodium.client.render.chunk.region.RenderRegion;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@IfModLoaded("sodium")
@Pseudo
@Mixin(
   targets = {"net/caffeinemc/mods/sodium/client/render/chunk/DefaultChunkRenderer"},
   remap = false
)
public abstract class MixinSodiumRegionChunkRenderer {
   @WrapOperation(
      method = {"fillCommandBuffer"},
      require = 1,
      remap = false,
      at = {@At(
         value = "INVOKE",
         target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/data/SectionRenderDataUnsafe;getSliceMask(J)I",
         remap = false
      )}
   )
   private static int addDrawCommands_sodium(
      long ptr, Operation<Integer> original, @Local(name = {"chunkX"}) int chunkX, @Local(name = {"chunkY"}) int chunkY, @Local(name = {"chunkZ"}) int chunkZ
   ) {
      return ChunkRenderOverrider.isOverridingSection(chunkX, chunkY, chunkZ) ? 0 : (Integer)original.call(new Object[]{ptr});
   }

   @IfModLoaded(
      value = "sodium",
      minVersion = "0.7.0"
   )
   @WrapOperation(
      method = {"render"},
      require = 1,
      remap = false,
      at = {@At(
         value = "INVOKE",
         target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/region/RenderRegion;getCachedBatch(Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;)Lnet/caffeinemc/mods/sodium/client/gl/device/MultiDrawBatch;",
         remap = false
      )}
   )
   private MultiDrawBatch addDrawCommands_sodium(RenderRegion instance, TerrainRenderPass pass, Operation<MultiDrawBatch> original) {
      MultiDrawBatch batch = (MultiDrawBatch)original.call(new Object[]{instance, pass});
      if (ChunkRenderOverrider.hasOverridenChunksChanged()) {
         batch.clear();
      }

      return batch;
   }
}
