package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.BlockVoxelShapeUtils;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import java.text.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HollowOperation {
   public static void hollow() {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         hollowOrFillGapsAABB(aabb, HollowOperation.HollowReplacement.NONAIR, Blocks.AIR.defaultBlockState());
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         hollowOrFillGapsSet(set, HollowOperation.HollowReplacement.NONAIR, Blocks.AIR.defaultBlockState());
      }
   }

   public static void fillGaps(BlockState blockState) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         hollowOrFillGapsAABB(aabb, HollowOperation.HollowReplacement.AIR, blockState);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         hollowOrFillGapsSet(set, HollowOperation.HollowReplacement.AIR, blockState);
      }
   }

   public static void fillUnreachable(BlockState blockState) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         hollowOrFillGapsAABB(aabb, HollowOperation.HollowReplacement.NONAIR, blockState);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         hollowOrFillGapsSet(set, HollowOperation.HollowReplacement.NONAIR, blockState);
      }
   }

   private static void hollowOrFillGapsAABB(SelectionBuffer.AABB aabb, HollowOperation.HollowReplacement replacement, BlockState replaceWith) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         int minX = aabb.min().getX();
         int minY = aabb.min().getY();
         int minZ = aabb.min().getZ();
         int maxX = aabb.max().getX();
         int maxY = aabb.max().getY();
         int maxZ = aabb.max().getZ();
         minY = Math.max(world.getMinBuildHeight(), minY);
         maxY = Math.min(world.getMaxBuildHeight() - 1, maxY);
         LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
         PositionSet visited = new PositionSet();
         PositionSet visible = new PositionSet();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               propagateInitial(world, mutableBlockPos.set(x, maxY + 1, z), Direction.DOWN, queue, visible, visited);
               propagateInitial(world, mutableBlockPos.set(x, minY - 1, z), Direction.UP, queue, visible, visited);
            }
         }

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               propagateInitial(world, mutableBlockPos.set(x, y, maxZ + 1), Direction.NORTH, queue, visible, visited);
               propagateInitial(world, mutableBlockPos.set(x, y, minZ - 1), Direction.SOUTH, queue, visible, visited);
            }
         }

         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               propagateInitial(world, mutableBlockPos.set(maxX + 1, y, z), Direction.WEST, queue, visible, visited);
               propagateInitial(world, mutableBlockPos.set(minX - 1, y, z), Direction.EAST, queue, visible, visited);
            }
         }

         if (replacement == HollowOperation.HollowReplacement.NONAIR && !replaceWith.isAir()) {
            for (int x = minX; x <= maxX; x++) {
               for (int y = minY; y <= maxY; y++) {
                  for (int z = minZ; z <= maxZ; z++) {
                     BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                     if (block.getBlock() != Blocks.VOID_AIR && block.isAir()) {
                        queue.enqueue(mutableBlockPos.asLong());
                        visited.add(x, y, z);
                     }
                  }
               }
            }
         }

         Direction[] directions = Direction.values();

         while (!queue.isEmpty()) {
            long pos = queue.dequeueLong();

            for (Direction direction : directions) {
               mutableBlockPos.set(pos);
               int neighborX = mutableBlockPos.getX() + direction.getStepX();
               int neighborY = mutableBlockPos.getY() + direction.getStepY();
               int neighborZ = mutableBlockPos.getZ() + direction.getStepZ();
               if (neighborX >= minX
                  && neighborY >= minY
                  && neighborZ >= minZ
                  && neighborX <= maxX
                  && neighborY <= maxY
                  && neighborZ <= maxZ
                  && !visited.contains(neighborX, neighborY, neighborZ)) {
                  propagate(world, mutableBlockPos, direction, queue, visible, visited);
               }
            }
         }

         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         int changeCount = 0;

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int zx = minZ; zx <= maxZ; zx++) {
                  BlockState block = world.getBlockState(mutableBlockPos.set(x, y, zx));
                  if (block.getBlock() != Blocks.VOID_AIR && replacement.shouldReplace(block) && !visible.contains(x, y, zx) && !visited.contains(x, y, zx)) {
                     changeCount++;
                     setOperation.set(x, y, zx, replaceWith);
                     previousBlocksForUndo.set(x, y, zx, block);
                  }
               }
            }
         }
         String desc = switch (replacement) {
            case AIR -> "axiom.history_description.filled_gaps";
            case NONAIR -> replaceWith.isAir() ? "axiom.history_description.hollowed" : "axiom.history_description.filled_unreachable";
         };
         String countString = NumberFormat.getInstance().format((long)changeCount);
         String historyDescription = AxiomI18n.get(desc, countString);
         RegionHelper.pushBlockBufferChange(setOperation, aabb.center(), historyDescription, null);
      }
   }

   private static void hollowOrFillGapsSet(SelectionBuffer.Set set, HollowOperation.HollowReplacement replacement, BlockState replaceWith) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         ChunkedBooleanRegion region = set.selectionRegion;
         LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
         PositionSet visited = new PositionSet();
         PositionSet visible = new PositionSet();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         region.forEach((x, y, z) -> {
            if (!region.contains(x + 1, y, z)) {
               propagateInitial(world, mutableBlockPos.set(x + 1, y, z), Direction.WEST, queue, visible, visited);
            }

            if (!region.contains(x - 1, y, z)) {
               propagateInitial(world, mutableBlockPos.set(x - 1, y, z), Direction.EAST, queue, visible, visited);
            }

            if (!region.contains(x, y + 1, z)) {
               propagateInitial(world, mutableBlockPos.set(x, y + 1, z), Direction.DOWN, queue, visible, visited);
            }

            if (!region.contains(x, y - 1, z)) {
               propagateInitial(world, mutableBlockPos.set(x, y - 1, z), Direction.UP, queue, visible, visited);
            }

            if (!region.contains(x, y, z + 1)) {
               propagateInitial(world, mutableBlockPos.set(x, y, z + 1), Direction.NORTH, queue, visible, visited);
            }

            if (!region.contains(x, y, z - 1)) {
               propagateInitial(world, mutableBlockPos.set(x, y, z - 1), Direction.SOUTH, queue, visible, visited);
            }

            if (replacement == HollowOperation.HollowReplacement.NONAIR && !replaceWith.isAir()) {
               BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
               if (block.getBlock() != Blocks.VOID_AIR && block.isAir()) {
                  queue.enqueue(mutableBlockPos.asLong());
                  visited.add(x, y, z);
               }
            }
         });
         Direction[] directions = Direction.values();

         while (!queue.isEmpty()) {
            long pos = queue.dequeueLong();

            for (Direction direction : directions) {
               mutableBlockPos.set(pos);
               int neighborX = mutableBlockPos.getX() + direction.getStepX();
               int neighborY = mutableBlockPos.getY() + direction.getStepY();
               int neighborZ = mutableBlockPos.getZ() + direction.getStepZ();
               if (region.contains(neighborX, neighborY, neighborZ) && !visited.contains(neighborX, neighborY, neighborZ)) {
                  propagate(world, mutableBlockPos, direction, queue, visible, visited);
               }
            }
         }

         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         IntWrapper changeCount = new IntWrapper();
         region.forEach((x, y, z) -> {
            BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
            if (block.getBlock() != Blocks.VOID_AIR) {
               if (replacement.shouldReplace(block)) {
                  if (!visible.contains(x, y, z) && !visited.contains(x, y, z)) {
                     changeCount.value++;
                     setOperation.set(x, y, z, replaceWith);
                     previousBlocksForUndo.set(x, y, z, block);
                  }
               }
            }
         });

         String desc = switch (replacement) {
            case AIR -> "axiom.history_description.filled_gaps";
            case NONAIR -> replaceWith.isAir() ? "axiom.history_description.hollowed" : "axiom.history_description.filled_unreachable";
         };
         String countString = NumberFormat.getInstance().format((long)changeCount.value);
         String historyDescription = AxiomI18n.get(desc, countString);
         RegionHelper.pushBlockBufferChange(setOperation, set.center(), historyDescription, null);
      }
   }

   private static void propagateInitial(
      Level level, MutableBlockPos pos, Direction direction, LongArrayFIFOQueue queue, PositionSet visible, PositionSet visited
   ) {
      BlockState fromBlock = level.getBlockState(pos);
      VoxelShape fromShape = fromBlock.getFaceOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction);
      if (!fromBlock.canOcclude() || !BlockVoxelShapeUtils.firstCompletelyOverlapsSecond(fromShape, Shapes.block())) {
         visited.add(pos.getX(), pos.getY(), pos.getZ());
      }

      pos.move(direction);
      BlockState toBlock = level.getBlockState(pos);
      if (fromBlock.canOcclude()) {
         VoxelShape toShape = toBlock.getFaceOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction.getOpposite());
         if (Shapes.faceShapeOccludes(fromShape, toShape)) {
            if (!BlockVoxelShapeUtils.firstCompletelyOverlapsSecond(fromShape, Shapes.block())) {
               visible.add(pos.getX(), pos.getY(), pos.getZ());
            }

            return;
         }
      }

      queue.enqueue(pos.asLong());
      visited.add(pos.getX(), pos.getY(), pos.getZ());
   }

   private static void propagate(Level level, MutableBlockPos pos, Direction direction, LongArrayFIFOQueue queue, PositionSet visible, PositionSet visited) {
      BlockState fromBlock = level.getBlockState(pos);
      VoxelShape fromShape = fromBlock.getFaceOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction);
      pos.move(direction);
      BlockState toBlock = level.getBlockState(pos);
      if (fromBlock.canOcclude()) {
         VoxelShape toShape = toBlock.getFaceOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, direction.getOpposite());
         if (Shapes.faceShapeOccludes(fromShape, toShape)) {
            if (!BlockVoxelShapeUtils.firstCompletelyOverlapsSecond(fromShape, Shapes.block())) {
               visible.add(pos.getX(), pos.getY(), pos.getZ());
            }

            return;
         }
      }

      queue.enqueue(pos.asLong());
      visited.add(pos.getX(), pos.getY(), pos.getZ());
   }

   private static enum HollowReplacement {
      AIR,
      NONAIR;

      public boolean shouldReplace(BlockState blockState) {
         return switch (this) {
            case AIR -> blockState.isAir();
            case NONAIR -> !blockState.isAir();
         };
      }
   }
}
