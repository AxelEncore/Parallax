package com.moulberry.axiom.custom_blocks.update;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Collection;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class FacingClickedOppositeCustomBlockPlacementLogic implements CustomBlockPlacementLogic {
   @Override
   public boolean hasRequiredProperties(Collection<Property<?>> properties) {
      return CustomBlockPlacementLogic.findFacingProperty(properties) != null || CustomBlockPlacementLogic.findAxisProperty(properties) != null;
   }

   @Nullable
   @Override
   public CustomBlockState getStateForPlacement(CustomBlockState blockState, BlockPlaceContext blockPlaceContext) {
      EnumProperty<Direction> facingProperty = CustomBlockPlacementLogic.findFacingProperty(blockState.getProperties());
      if (facingProperty != null) {
         Direction clicked = blockPlaceContext.getClickedFace();
         if (facingProperty.getPossibleValues().contains(clicked.getOpposite())) {
            return blockState.setPropertyUnsafe(facingProperty, clicked.getOpposite());
         }

         for (Direction nearest : Direction.orderedByNearest(blockPlaceContext.getPlayer())) {
            if (facingProperty.getPossibleValues().contains(nearest)) {
               return blockState.setPropertyUnsafe(facingProperty, nearest);
            }
         }
      }

      EnumProperty<Axis> axisProperty = CustomBlockPlacementLogic.findAxisProperty(blockState.getProperties());
      if (axisProperty != null) {
         Direction clicked = blockPlaceContext.getClickedFace();
         if (axisProperty.getPossibleValues().contains(clicked.getAxis())) {
            return blockState.setPropertyUnsafe(axisProperty, clicked.getAxis());
         }

         for (Direction nearestx : Direction.orderedByNearest(blockPlaceContext.getPlayer())) {
            if (axisProperty.getPossibleValues().contains(nearestx.getAxis())) {
               return blockState.setPropertyUnsafe(axisProperty, nearestx.getAxis());
            }
         }
      }

      return null;
   }
}
