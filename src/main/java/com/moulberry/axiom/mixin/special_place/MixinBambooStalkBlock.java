package com.moulberry.axiom.mixin.special_place;

import com.moulberry.axiom.capabilities.SpecialPlace;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BambooStalkBlock.class})
public class MixinBambooStalkBlock {
   @Inject(
      method = {"getStateForPlacement"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void getStateForPlacementReturn(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<BlockState> cir) {
      if (cir.getReturnValue() == null && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel())) {
         cir.setReturnValue(Blocks.BAMBOO_SAPLING.defaultBlockState());
      }
   }
}
