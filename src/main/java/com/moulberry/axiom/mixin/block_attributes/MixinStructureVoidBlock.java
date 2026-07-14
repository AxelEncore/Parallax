package com.moulberry.axiom.mixin.block_attributes;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.StaticValues;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StructureVoidBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({StructureVoidBlock.class})
public class MixinStructureVoidBlock {
   @ModifyReturnValue(
      method = {"getRenderShape"},
      at = {@At("RETURN")}
   )
   public RenderShape getShape(RenderShape voxelShape) {
      return StaticValues.gameHasTicked && AxiomClient.isAxiomActive() && Axiom.configuration.blockAttributes.showStructureVoidBlocks
         ? RenderShape.MODEL
         : voxelShape;
   }
}
