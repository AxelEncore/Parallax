package com.moulberry.axiom.mixin.render;

import com.moulberry.axiom.hooks.WorldRenderHook;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.mixinconstraints.annotations.IfModAbsent;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.CompiledSection;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@IfModAbsent(
   value = "sodium",
   aliases = {"embeddium"}
)
@Mixin({SectionOcclusionGraph.class})
public class MixinSectionOcclusionGraphWithoutSodium {
   @Redirect(
      method = {"runUpdates"},
      require = 1,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;getCompiled()Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$CompiledSection;"
      )
   )
   public CompiledSection updateRenderChunks_checkEmpty(RenderSection instance) {
      BlockPos origin = instance.getOrigin();
      return ChunkRenderOverrider.isOverridingSection(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4)
         ? WorldRenderHook.VERY_VISIBLE_CHUNK
         : instance.getCompiled();
   }
}
