package com.moulberry.axiom.mixin.render;

import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Display.RenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DisplayRenderer.class})
public class MixinDisplayRenderer {
   @Redirect(
      method = {"render(Lnet/minecraft/world/entity/Display;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Display;renderState()Lnet/minecraft/world/entity/Display$RenderState;"
      )
   )
   public RenderState render_renderState(Display instance) {
      return DisplayEntityManipulator.getModifiedRenderState(instance);
   }
}
