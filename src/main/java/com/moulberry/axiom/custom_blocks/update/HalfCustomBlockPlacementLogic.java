package com.moulberry.axiom.custom_blocks.update;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class HalfCustomBlockPlacementLogic implements CustomBlockPlacementLogic {
   @Override
   public boolean hasRequiredProperties(Collection<Property<?>> properties) {
      return properties.contains(BlockStateProperties.HALF);
   }

   @Nullable
   @Override
   public CustomBlockState getStateForPlacement(CustomBlockState blockState, BlockPlaceContext blockPlaceContext) {
      Direction direction = blockPlaceContext.getClickedFace();
      BlockPos blockPos = blockPlaceContext.getClickedPos();
      if (direction == Direction.DOWN) {
         return blockState.setPropertyUnsafe(BlockStateProperties.HALF, Half.TOP);
      } else {
         return direction == Direction.UP
            ? blockState.setPropertyUnsafe(BlockStateProperties.HALF, Half.BOTTOM)
            : blockState.setPropertyUnsafe(BlockStateProperties.HALF, blockPlaceContext.getClickLocation().y - blockPos.getY() > 0.5 ? Half.TOP : Half.BOTTOM);
      }
   }
}
