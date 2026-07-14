package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FillNearestOperation {
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();

   public static void fillNearest() {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         fillNearestAABB(aabb);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         fillNearestSet(set);
      }
   }

   private static void fillNearestAABB(SelectionBuffer.AABB aabb) {
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
         Position2ObjectMap<FillNearestOperation.PropagateData> propagateDataMap = new Position2ObjectMap<>(k -> new FillNearestOperation.PropagateData[4096]);
         LongArrayFIFOQueue propagateQueue = new LongArrayFIFOQueue();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockState maxBlock = world.getBlockState(mutableBlockPos.set(x, maxY + 1, z));
               if (!maxBlock.isAir()) {
                  FillNearestOperation.PropagateDataBlock maxPropagateDataBlock = new FillNearestOperation.PropagateDataBlock(
                     BlockPos.asLong(x, maxY + 1, z), maxBlock
                  );
                  initialPropagateTo(world, x, maxY, z, mutableBlockPos, propagateDataMap, propagateQueue, maxPropagateDataBlock);
               }

               BlockState minBlock = world.getBlockState(mutableBlockPos.set(x, minY - 1, z));
               if (!minBlock.isAir()) {
                  FillNearestOperation.PropagateDataBlock minPropagateDataBlock = new FillNearestOperation.PropagateDataBlock(
                     BlockPos.asLong(x, minY - 1, z), minBlock
                  );
                  initialPropagateTo(world, x, minY, z, mutableBlockPos, propagateDataMap, propagateQueue, minPropagateDataBlock);
               }
            }
         }

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               BlockState maxBlockx = world.getBlockState(mutableBlockPos.set(x, y, maxZ + 1));
               if (!maxBlockx.isAir()) {
                  FillNearestOperation.PropagateDataBlock maxPropagateDataBlock = new FillNearestOperation.PropagateDataBlock(
                     BlockPos.asLong(x, y, maxZ + 1), maxBlockx
                  );
                  initialPropagateTo(world, x, y, maxZ, mutableBlockPos, propagateDataMap, propagateQueue, maxPropagateDataBlock);
               }

               BlockState minBlock = world.getBlockState(mutableBlockPos.set(x, y, minZ - 1));
               if (!minBlock.isAir()) {
                  FillNearestOperation.PropagateDataBlock minPropagateDataBlock = new FillNearestOperation.PropagateDataBlock(
                     BlockPos.asLong(x, y, minZ - 1), minBlock
                  );
                  initialPropagateTo(world, x, y, minZ, mutableBlockPos, propagateDataMap, propagateQueue, minPropagateDataBlock);
               }
            }
         }

         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockState maxBlockxx = world.getBlockState(mutableBlockPos.set(maxX + 1, y, z));
               if (!maxBlockxx.isAir()) {
                  FillNearestOperation.PropagateDataBlock maxPropagateDataBlock = new FillNearestOperation.PropagateDataBlock(
                     BlockPos.asLong(maxX + 1, y, z), maxBlockxx
                  );
                  initialPropagateTo(world, maxX, y, z, mutableBlockPos, propagateDataMap, propagateQueue, maxPropagateDataBlock);
               }

               BlockState minBlock = world.getBlockState(mutableBlockPos.set(minX - 1, y, z));
               if (!minBlock.isAir()) {
                  FillNearestOperation.PropagateDataBlock minPropagateDataBlock = new FillNearestOperation.PropagateDataBlock(
                     BlockPos.asLong(minX - 1, y, z), minBlock
                  );
                  initialPropagateTo(world, minX, y, z, mutableBlockPos, propagateDataMap, propagateQueue, minPropagateDataBlock);
               }
            }
         }

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               for (int y = minY; y <= maxY; y++) {
                  BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                  if (!block.isAir()) {
                     FillNearestOperation.PropagateDataBlock propagateDataBlock = new FillNearestOperation.PropagateDataBlock(BlockPos.asLong(x, y, z), block);
                     propagateDataMap.put(x, y, z, new FillNearestOperation.PropagateData(0, null));
                     if (x < maxX) {
                        initialPropagateTo(world, x + 1, y, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                     }

                     if (x > minX) {
                        initialPropagateTo(world, x - 1, y, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                     }

                     if (y < maxY) {
                        initialPropagateTo(world, x, y + 1, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                     }

                     if (y > minY) {
                        initialPropagateTo(world, x, y - 1, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                     }

                     if (z < maxZ) {
                        initialPropagateTo(world, x, y, z + 1, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                     }

                     if (z > minZ) {
                        initialPropagateTo(world, x, y, z - 1, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                     }
                  }
               }
            }
         }

         while (!propagateQueue.isEmpty()) {
            long pos = propagateQueue.dequeueLong();
            int x = BlockPos.getX(pos);
            int yx = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            FillNearestOperation.PropagateData propagateData = propagateDataMap.get(x, yx, z);
            propagateData.queued = false;
            if (x < maxX) {
               propagateTo(x + 1, yx, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (x > minX) {
               propagateTo(x - 1, yx, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (yx < maxY) {
               propagateTo(x, yx + 1, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (yx > minY) {
               propagateTo(x, yx - 1, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (z < maxZ) {
               propagateTo(x, yx, z + 1, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (z > minZ) {
               propagateTo(x, yx, z - 1, propagateDataMap, propagateQueue, propagateData.blocks);
            }
         }

         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         IntWrapper changeCount = new IntWrapper();
         propagateDataMap.forEachEntry((xx, yxx, zx, data) -> {
            if (data.blocks != null) {
               BlockState blockState = data.blocks.get(0).blockState;
               setOperation.set(xx, yxx, zx, blockState);
               previousBlocksForUndo.set(xx, yxx, zx, AIR);
               changeCount.value++;
            }
         });
         if (changeCount.value != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount.value);
            String historyDescription = AxiomI18n.get("axiom.history_description.fill_nearest", countString);
            RegionHelper.pushBlockBufferChange(setOperation, aabb.center(), historyDescription, null);
         }
      }
   }

   private static void fillNearestSet(SelectionBuffer.Set set) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         Position2ObjectMap<FillNearestOperation.PropagateData> propagateDataMap = new Position2ObjectMap<>(k -> new FillNearestOperation.PropagateData[4096]);
         LongArrayFIFOQueue propagateQueue = new LongArrayFIFOQueue();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         ChunkedBooleanRegion region = set.selectionRegion;
         region.forEach(
            (x, y, z) -> {
               BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
               if (!block.isAir()) {
                  FillNearestOperation.PropagateDataBlock propagateDataBlock = new FillNearestOperation.PropagateDataBlock(BlockPos.asLong(x, y, z), block);
                  propagateDataMap.put(x, y, z, new FillNearestOperation.PropagateData(0, null));
                  if (region.contains(x + 1, y, z)) {
                     initialPropagateTo(world, x + 1, y, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                  }

                  if (region.contains(x - 1, y, z)) {
                     initialPropagateTo(world, x - 1, y, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                  }

                  if (region.contains(x, y + 1, z)) {
                     initialPropagateTo(world, x, y + 1, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                  }

                  if (region.contains(x, y - 1, z)) {
                     initialPropagateTo(world, x, y - 1, z, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                  }

                  if (region.contains(x, y, z + 1)) {
                     initialPropagateTo(world, x, y, z + 1, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                  }

                  if (region.contains(x, y, z - 1)) {
                     initialPropagateTo(world, x, y, z - 1, mutableBlockPos, propagateDataMap, propagateQueue, propagateDataBlock);
                  }
               } else {
                  if (!region.contains(x + 1, y, z)) {
                     BlockState neighbor = world.getBlockState(mutableBlockPos.set(x + 1, y, z));
                     if (!neighbor.isAir()) {
                        FillNearestOperation.PropagateDataBlock propagateNeighbor = new FillNearestOperation.PropagateDataBlock(
                           BlockPos.asLong(x + 1, y, z), neighbor
                        );
                        initialPropagateToAssumeAir(x, y, z, propagateDataMap, propagateQueue, propagateNeighbor);
                        return;
                     }
                  }

                  if (!region.contains(x - 1, y, z)) {
                     BlockState neighbor = world.getBlockState(mutableBlockPos.set(x - 1, y, z));
                     if (!neighbor.isAir()) {
                        FillNearestOperation.PropagateDataBlock propagateNeighbor = new FillNearestOperation.PropagateDataBlock(
                           BlockPos.asLong(x - 1, y, z), neighbor
                        );
                        initialPropagateToAssumeAir(x, y, z, propagateDataMap, propagateQueue, propagateNeighbor);
                        return;
                     }
                  }

                  if (!region.contains(x, y + 1, z)) {
                     BlockState neighbor = world.getBlockState(mutableBlockPos.set(x, y + 1, z));
                     if (!neighbor.isAir()) {
                        FillNearestOperation.PropagateDataBlock propagateNeighbor = new FillNearestOperation.PropagateDataBlock(
                           BlockPos.asLong(x, y + 1, z), neighbor
                        );
                        initialPropagateToAssumeAir(x, y, z, propagateDataMap, propagateQueue, propagateNeighbor);
                        return;
                     }
                  }

                  if (!region.contains(x, y - 1, z)) {
                     BlockState neighbor = world.getBlockState(mutableBlockPos.set(x, y - 1, z));
                     if (!neighbor.isAir()) {
                        FillNearestOperation.PropagateDataBlock propagateNeighbor = new FillNearestOperation.PropagateDataBlock(
                           BlockPos.asLong(x, y - 1, z), neighbor
                        );
                        initialPropagateToAssumeAir(x, y, z, propagateDataMap, propagateQueue, propagateNeighbor);
                        return;
                     }
                  }

                  if (!region.contains(x, y, z + 1)) {
                     BlockState neighbor = world.getBlockState(mutableBlockPos.set(x, y, z + 1));
                     if (!neighbor.isAir()) {
                        FillNearestOperation.PropagateDataBlock propagateNeighbor = new FillNearestOperation.PropagateDataBlock(
                           BlockPos.asLong(x, y, z + 1), neighbor
                        );
                        initialPropagateToAssumeAir(x, y, z, propagateDataMap, propagateQueue, propagateNeighbor);
                        return;
                     }
                  }

                  if (!region.contains(x, y, z - 1)) {
                     BlockState neighbor = world.getBlockState(mutableBlockPos.set(x, y, z - 1));
                     if (!neighbor.isAir()) {
                        FillNearestOperation.PropagateDataBlock propagateNeighbor = new FillNearestOperation.PropagateDataBlock(
                           BlockPos.asLong(x, y, z - 1), neighbor
                        );
                        initialPropagateToAssumeAir(x, y, z, propagateDataMap, propagateQueue, propagateNeighbor);
                     }
                  }
               }
            }
         );

         while (!propagateQueue.isEmpty()) {
            long pos = propagateQueue.dequeueLong();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            FillNearestOperation.PropagateData propagateData = propagateDataMap.get(x, y, z);
            propagateData.queued = false;
            if (region.contains(x + 1, y, z)) {
               propagateTo(x + 1, y, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (region.contains(x - 1, y, z)) {
               propagateTo(x - 1, y, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (region.contains(x, y + 1, z)) {
               propagateTo(x, y + 1, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (region.contains(x, y - 1, z)) {
               propagateTo(x, y - 1, z, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (region.contains(x, y, z + 1)) {
               propagateTo(x, y, z + 1, propagateDataMap, propagateQueue, propagateData.blocks);
            }

            if (region.contains(x, y, z - 1)) {
               propagateTo(x, y, z - 1, propagateDataMap, propagateQueue, propagateData.blocks);
            }
         }

         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         IntWrapper changeCount = new IntWrapper();
         propagateDataMap.forEachEntry((xx, yx, zx, data) -> {
            if (data.blocks != null) {
               BlockState blockState = data.blocks.get(0).blockState;
               setOperation.set(xx, yx, zx, blockState);
               previousBlocksForUndo.set(xx, yx, zx, AIR);
               changeCount.value++;
            }
         });
         if (changeCount.value != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount.value);
            String historyDescription = AxiomI18n.get("axiom.history_description.fill_nearest", countString);
            RegionHelper.pushBlockBufferChange(setOperation, set.center(), historyDescription, null);
         }
      }
   }

   private static void initialPropagateTo(
      Level level,
      int x,
      int y,
      int z,
      MutableBlockPos mutableBlockPos,
      Position2ObjectMap<FillNearestOperation.PropagateData> propagateDataMap,
      LongArrayFIFOQueue propagateQueue,
      FillNearestOperation.PropagateDataBlock propagateDataBlock
   ) {
      BlockState state = level.getBlockState(mutableBlockPos.set(x, y, z));
      if (state.isAir()) {
         FillNearestOperation.PropagateData propagateData = propagateDataMap.get(x, y, z);
         if (propagateData == null) {
            List<FillNearestOperation.PropagateDataBlock> list = new ArrayList<>();
            list.add(propagateDataBlock);
            propagateData = new FillNearestOperation.PropagateData(1, list);
            propagateData.queued = true;
            propagateDataMap.put(x, y, z, propagateData);
            propagateQueue.enqueue(BlockPos.asLong(x, y, z));
         } else if (propagateData.minDistanceSq != 0) {
            propagateData.blocks.add(propagateDataBlock);
         }
      }
   }

   private static void initialPropagateToAssumeAir(
      int x,
      int y,
      int z,
      Position2ObjectMap<FillNearestOperation.PropagateData> propagateDataMap,
      LongArrayFIFOQueue propagateQueue,
      FillNearestOperation.PropagateDataBlock propagateDataBlock
   ) {
      FillNearestOperation.PropagateData propagateData = propagateDataMap.get(x, y, z);
      if (propagateData == null) {
         List<FillNearestOperation.PropagateDataBlock> list = new ArrayList<>();
         list.add(propagateDataBlock);
         propagateData = new FillNearestOperation.PropagateData(1, list);
         propagateData.queued = true;
         propagateDataMap.put(x, y, z, propagateData);
         propagateQueue.enqueue(BlockPos.asLong(x, y, z));
      } else if (propagateData.minDistanceSq != 0) {
         propagateData.blocks.add(propagateDataBlock);
      }
   }

   private static void propagateTo(
      int x,
      int y,
      int z,
      Position2ObjectMap<FillNearestOperation.PropagateData> propagateDataMap,
      LongArrayFIFOQueue propagateQueue,
      List<FillNearestOperation.PropagateDataBlock> blocks
   ) {
      FillNearestOperation.PropagateData propagateData = propagateDataMap.get(x, y, z);
      if (propagateData == null || propagateData.minDistanceSq != 0) {
         int minDistanceSq = Integer.MAX_VALUE;
         FillNearestOperation.PropagateDataBlock minPropagate = null;

         for (FillNearestOperation.PropagateDataBlock block : blocks) {
            int dx = BlockPos.getX(block.origin) - x;
            int dy = BlockPos.getY(block.origin) - y;
            int dz = BlockPos.getZ(block.origin) - z;
            int distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq < minDistanceSq) {
               minDistanceSq = distanceSq;
               minPropagate = block;
            }
         }

         if (propagateData == null) {
            List<FillNearestOperation.PropagateDataBlock> list = new ArrayList<>();
            list.add(minPropagate);
            propagateData = new FillNearestOperation.PropagateData(minDistanceSq, list);
            propagateData.queued = true;
            propagateDataMap.put(x, y, z, propagateData);
            propagateQueue.enqueue(BlockPos.asLong(x, y, z));
         } else if (minDistanceSq < propagateData.minDistanceSq) {
            propagateData.minDistanceSq = minDistanceSq;
            propagateData.blocks.clear();
            propagateData.blocks.add(minPropagate);
            if (!propagateData.queued) {
               propagateQueue.enqueue(BlockPos.asLong(x, y, z));
               propagateData.queued = true;
            }
         } else if (minDistanceSq == propagateData.minDistanceSq) {
            propagateData.blocks.add(minPropagate);
         }
      }
   }

   private static final class PropagateData {
      private int minDistanceSq;
      private boolean queued = false;
      private final List<FillNearestOperation.PropagateDataBlock> blocks;

      PropagateData(int minDistanceSq, List<FillNearestOperation.PropagateDataBlock> blocks) {
         this.minDistanceSq = minDistanceSq;
         this.blocks = blocks;
      }
   }

   private record PropagateDataBlock(long origin, BlockState blockState) {
   }
}
