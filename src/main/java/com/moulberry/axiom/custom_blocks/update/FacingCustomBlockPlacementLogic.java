package com.moulberry.axiom.custom_blocks.update;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Collection;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class FacingCustomBlockPlacementLogic implements CustomBlockPlacementLogic {
   @Override
   public boolean hasRequiredProperties(Collection<Property<?>> properties) {
      return CustomBlockPlacementLogic.findFacingProperty(properties) != null || CustomBlockPlacementLogic.findAxisProperty(properties) != null;
   }

   @Nullable
   @Override
   public CustomBlockState getStateForPlacement(CustomBlockState blockState, BlockPlaceContext blockPlaceContext) {
      if (blockPlaceContext.getPlayer() == null) {
         return blockState;
      } else {
         Direction[] nearestDirections = Direction.orderedByNearest(blockPlaceContext.getPlayer());
         EnumProperty<Direction> facingProperty = CustomBlockPlacementLogic.findFacingProperty(blockState.getProperties());
         if (facingProperty != null) {
            for (Direction nearest : nearestDirections) {
               if (facingProperty.getPossibleValues().contains(nearest)) {
                  return blockState.setPropertyUnsafe(facingProperty, nearest);
               }
            }
         }

         EnumProperty<Axis> axisProperty = CustomBlockPlacementLogic.findAxisProperty(blockState.getProperties());
         if (axisProperty != null) {
            for (Direction nearestx : nearestDirections) {
               if (axisProperty.getPossibleValues().contains(nearestx.getAxis())) {
                  return blockState.setPropertyUnsafe(axisProperty, nearestx.getAxis());
               }
            }
         }

         return null;
      }
   }
}
