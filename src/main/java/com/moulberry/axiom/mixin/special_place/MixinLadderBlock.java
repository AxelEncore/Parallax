package com.moulberry.axiom.mixin.special_place;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.capabilities.SpecialPlace;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LadderBlock.class})
public class MixinLadderBlock {
   @ModifyReturnValue(
      method = {"getStateForPlacement"},
      at = {@At("RETURN")}
   )
   public BlockState getStateForPlacementReturn(BlockState blockState, @Local(argsOnly = true) BlockPlaceContext blockPlaceContext) {
      if (blockState == null && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel())) {
         FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
         blockState = (BlockState)((LadderBlock)(Object)this).defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);

         for (Direction direction : blockPlaceContext.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
               return (BlockState)blockState.setValue(HorizontalDirectionalBlock.FACING, direction.getOpposite());
            }
         }
      }

      return blockState;
   }
}
