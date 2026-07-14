package com.moulberry.axiom.custom_blocks.update;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Collection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class WaterloggedCustomBlockPlacementLogic implements CustomBlockPlacementLogic {
   @Override
   public boolean hasRequiredProperties(Collection<Property<?>> properties) {
      return properties.contains(BlockStateProperties.WATERLOGGED);
   }

   @Nullable
   @Override
   public CustomBlockState getStateForPlacement(CustomBlockState blockState, BlockPlaceContext blockPlaceContext) {
      FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
      return blockState.setPropertyUnsafe(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
   }
}
