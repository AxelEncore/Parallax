package com.moulberry.axiom.mixin.special_place;

import com.moulberry.axiom.capabilities.SpecialPlace;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({CocoaBlock.class})
public class MixinCocoaBlock {
   @Inject(
      method = {"getStateForPlacement"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void getStateForPlacementReturn(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<BlockState> cir) {
      if (cir.getReturnValue() == null && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel())) {
         BlockState blockState = ((CocoaBlock)(Object)this).defaultBlockState();

         for (Direction direction : blockPlaceContext.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
               cir.setReturnValue((BlockState)blockState.setValue(HorizontalDirectionalBlock.FACING, direction));
               return;
            }
         }
      }
   }
}
