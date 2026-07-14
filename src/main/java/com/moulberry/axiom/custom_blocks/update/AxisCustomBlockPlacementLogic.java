package com.moulberry.axiom.custom_blocks.update;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Collection;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class AxisCustomBlockPlacementLogic implements CustomBlockPlacementLogic {
   @Override
   public boolean hasRequiredProperties(Collection<Property<?>> properties) {
      return CustomBlockPlacementLogic.findAxisProperty(properties) != null;
   }

   @Nullable
   @Override
   public CustomBlockState getStateForPlacement(CustomBlockState blockState, BlockPlaceContext blockPlaceContext) {
      EnumProperty<Axis> axisProperty = CustomBlockPlacementLogic.findAxisProperty(blockState.getProperties());
      if (axisProperty != null) {
         Direction clicked = blockPlaceContext.getClickedFace();
         if (axisProperty.getPossibleValues().contains(clicked.getAxis())) {
            return blockState.setPropertyUnsafe(axisProperty, clicked.getAxis());
         }

         for (Direction nearest : Direction.orderedByNearest(blockPlaceContext.getPlayer())) {
            if (axisProperty.getPossibleValues().contains(nearest.getAxis())) {
               return blockState.setPropertyUnsafe(axisProperty, nearest.getAxis());
            }
         }
      }

      return null;
   }
}
