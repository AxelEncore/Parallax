package com.moulberry.axiom.utils;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockShapeUpdater {
   private static final VoxelShape TEST_SHAPE_POST = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 9.0);
   private static final VoxelShape TEST_SHAPE_NORTH = Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 9.0);
   private static final VoxelShape TEST_SHAPE_SOUTH = Block.box(7.0, 0.0, 7.0, 9.0, 16.0, 16.0);
   private static final VoxelShape TEST_SHAPE_WEST = Block.box(0.0, 0.0, 7.0, 9.0, 16.0, 9.0);
   private static final VoxelShape TEST_SHAPE_EAST = Block.box(7.0, 0.0, 7.0, 16.0, 16.0, 9.0);

   public static BlockState updateWallSides(BlockState current, VoxelShape aboveVoxelShape, boolean north, boolean east, boolean south, boolean west) {
      current = (BlockState)current.setValue(BlockStateProperties.NORTH_WALL, getWallSide(north, aboveVoxelShape, TEST_SHAPE_NORTH));
      current = (BlockState)current.setValue(BlockStateProperties.EAST_WALL, getWallSide(east, aboveVoxelShape, TEST_SHAPE_EAST));
      current = (BlockState)current.setValue(BlockStateProperties.SOUTH_WALL, getWallSide(south, aboveVoxelShape, TEST_SHAPE_SOUTH));
      return (BlockState)current.setValue(BlockStateProperties.WEST_WALL, getWallSide(west, aboveVoxelShape, TEST_SHAPE_WEST));
   }

   private static WallSide getWallSide(boolean isSide, VoxelShape aboveVoxelShape, VoxelShape testShape) {
      if (isSide) {
         return isCovered(aboveVoxelShape, testShape) ? WallSide.TALL : WallSide.LOW;
      } else {
         return WallSide.NONE;
      }
   }

   public static BlockState updateWallUp(BlockState current, BlockState above, VoxelShape aboveVoxelShape) {
      if (above.getBlock() instanceof WallBlock && (Boolean)above.getValue(BlockStateProperties.UP)) {
         return (BlockState)current.setValue(BlockStateProperties.UP, true);
      } else {
         WallSide northWall = (WallSide)current.getValue(BlockStateProperties.NORTH_WALL);
         WallSide southWall = (WallSide)current.getValue(BlockStateProperties.SOUTH_WALL);
         WallSide eastWall = (WallSide)current.getValue(BlockStateProperties.EAST_WALL);
         WallSide westWall = (WallSide)current.getValue(BlockStateProperties.WEST_WALL);
         boolean noSouth = southWall == WallSide.NONE;
         boolean noWest = westWall == WallSide.NONE;
         boolean noEast = eastWall == WallSide.NONE;
         boolean noNorth = northWall == WallSide.NONE;
         if ((!noNorth || !noSouth || !noWest || !noEast) && noNorth == noSouth && noWest == noEast) {
            boolean centerWall = northWall == WallSide.TALL && southWall == WallSide.TALL || eastWall == WallSide.TALL && westWall == WallSide.TALL;
            if (centerWall) {
               return (BlockState)current.setValue(BlockStateProperties.UP, false);
            } else {
               boolean raised = above.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(aboveVoxelShape, TEST_SHAPE_POST);
               return (BlockState)current.setValue(BlockStateProperties.UP, raised);
            }
         } else {
            return (BlockState)current.setValue(BlockStateProperties.UP, true);
         }
      }
   }

   private static boolean isCovered(VoxelShape voxelShape, VoxelShape testShape) {
      return !Shapes.joinIsNotEmpty(testShape, voxelShape, BooleanOp.ONLY_FIRST);
   }
}
