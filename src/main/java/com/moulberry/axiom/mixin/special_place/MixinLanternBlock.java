package com.moulberry.axiom.mixin.special_place;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.capabilities.SpecialPlace;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LanternBlock.class})
public class MixinLanternBlock {
   @ModifyReturnValue(
      method = {"getStateForPlacement"},
      at = {@At("RETURN")}
   )
   public BlockState getStateForPlacementReturn(BlockState blockState, @Local(argsOnly = true) BlockPlaceContext blockPlaceContext) {
      if (blockState == null && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel())) {
         blockState = ((Block)(Object)this).defaultBlockState();
         if (blockPlaceContext.getClickedFace().getAxis() == Axis.Y) {
            blockState = (BlockState)blockState.setValue(BlockStateProperties.HANGING, true);
         }

         FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
         return (BlockState)blockState.setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
      } else {
         return blockState;
      }
   }
}
