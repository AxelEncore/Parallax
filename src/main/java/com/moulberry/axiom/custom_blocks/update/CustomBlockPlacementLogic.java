package com.moulberry.axiom.custom_blocks.update;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public interface CustomBlockPlacementLogic {
   boolean hasRequiredProperties(Collection<Property<?>> var1);

   default CustomBlockState customShapeUpdate(CustomBlockState blockState, LevelReader levelReader, BlockPos blockPos) {
      return blockState;
   }

   @Nullable
   default CustomBlockState getStateForPlacement(CustomBlockState blockState, BlockPlaceContext blockPlaceContext) {
      return blockState;
   }

   @Nullable
   static EnumProperty<Direction> findFacingProperty(Collection<Property<?>> collection) {
      for (Property<?> property : collection) {
         if (property.getName().equals("facing")) {
            if (property instanceof DirectionProperty directionProperty) {
               return directionProperty;
            }

            if (property instanceof EnumProperty<?> enumProperty && enumProperty.getValueClass() == Direction.class) {
               return (EnumProperty<Direction>)enumProperty;
            }

            return null;
         }
      }

      return null;
   }

   @Nullable
   static EnumProperty<Axis> findAxisProperty(Collection<Property<?>> collection) {
      for (Property<?> property : collection) {
         if (property.getName().equals("axis")) {
            if (property instanceof EnumProperty<?> enumProperty && enumProperty.getValueClass() == Axis.class) {
               return (EnumProperty<Axis>)enumProperty;
            }

            return null;
         }
      }

      return null;
   }
}
