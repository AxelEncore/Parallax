package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.Dispatcher;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RebuildOperation {
   private static final Position2FloatMap priorityMap = new Position2FloatMap(Float.MAX_VALUE);
   private static final PositionSet processed = new PositionSet();
   private static final ObjectHeapPriorityQueue<RebuildOperation.QueueObject> queue = new ObjectHeapPriorityQueue<RebuildOperation.QueueObject>(Comparator.comparingDouble(k -> k.priority));
   private static ChunkedBlockRegion rebuildRegion = null;
   private static double currentPriority = 0.0;
   private static RebuildOperation.RebuildParameters rebuildParameters = null;

   public static void rebuild(RebuildOperation.RebuildParameters parameters) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      ChunkedBlockRegion region = new ChunkedBlockRegion();
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         ChunkedBlockRegion clear = new ChunkedBlockRegion();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         selectionBuffer.forEach((x, y, z) -> {
            BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
            if (blockState.getBlock() != Blocks.VOID_AIR) {
               region.addBlockWithoutDirty(x, y, z, blockState);
               clear.addBlockWithoutDirty(x, y, z, Blocks.AIR.defaultBlockState());
            }
         });
         if (!region.isEmpty()) {
            RegionHelper.pushBlockRegionChange(clear, "Animated Rebuild");
            rebuild(region, parameters);
         }
      }
   }

   public static void resetRebuildState() {
      priorityMap.clear();
      queue.clear();
      processed.clear();
      rebuildRegion = null;
      rebuildParameters = null;
      currentPriority = 0.0;
   }

   private static void rebuild(ChunkedBlockRegion region, RebuildOperation.RebuildParameters parameters) {
      resetRebuildState();
      rebuildRegion = region;
      rebuildParameters = parameters;
      region.forEachEntry((xx, yx, zx, blockState) -> {
         if (blockState.getBlock() == Blocks.STRUCTURE_BLOCK) {
            queue.enqueue(new RebuildOperation.QueueObject(xx, yx, zx, parameters.startDelay * 20.0F));
            priorityMap.put(xx, yx, zx, parameters.startDelay * 20.0F);
         }
      });
      if (queue.isEmpty()) {
         BlockPos min = region.min();
         BlockPos max = region.max();
         int x = Math.floorDiv(min.getX() + max.getX(), 2);
         int y = Math.floorDiv(min.getY() + max.getY(), 2);
         int z = Math.floorDiv(min.getZ() + max.getZ(), 2);
         queue.enqueue(new RebuildOperation.QueueObject(x, y, z, parameters.startDelay * 20.0F));
         priorityMap.put(x, y, z, parameters.startDelay * 20.0F);
      }
   }

   public static void tick() {
      if (rebuildRegion != null && rebuildParameters != null) {
         if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
            while (!queue.isEmpty()) {
               RebuildOperation.QueueObject object = (RebuildOperation.QueueObject)queue.dequeue();
               if (!processed.contains(object.x, object.y, object.z)) {
                  queue.enqueue(object);
                  currentPriority++;
                  object = (RebuildOperation.QueueObject)queue.first();
                  if (rebuildParameters.maxDelay >= 0.0F && object.priority > currentPriority + rebuildParameters.maxDelay * 20.0F) {
                     currentPriority = object.priority - rebuildParameters.maxDelay * 20.0F;
                  }

                  BlockBuffer blockBuffer = null;

                  while (true) {
                     if (queue.isEmpty()) {
                        resetRebuildState();
                        break;
                     }

                     object = (RebuildOperation.QueueObject)queue.dequeue();
                     if (object.priority > currentPriority) {
                        queue.enqueue(object);
                        break;
                     }

                     if (processed.add(object.x, object.y, object.z)) {
                        BlockState blockState = rebuildRegion.getBlockStateOrAir(object.x, object.y, object.z);

                        for (Direction direction : Direction.values()) {
                           int neighborX = object.x + direction.getStepX();
                           int neighborY = object.y + direction.getStepY();
                           int neighborZ = object.z + direction.getStepZ();
                           BlockState neighbor = rebuildRegion.getBlockStateOrNull(neighborX, neighborY, neighborZ);
                           if (neighbor != null) {
                              float penalty;
                              if (neighbor.isAir()) {
                                 penalty = rebuildParameters.airDelay * 20.0F;
                              } else if (neighbor.getBlock() == blockState.getBlock()) {
                                 penalty = rebuildParameters.sameBlockDelay * 20.0F;
                              } else {
                                 penalty = rebuildParameters.differentBlockDelay * 20.0F;
                              }

                              if (rebuildParameters.randomExtraDelay > 0.0F) {
                                 penalty += ThreadLocalRandom.current().nextFloat(rebuildParameters.randomExtraDelay * 20.0F);
                              }

                              if (direction.getStepY() != 0) {
                                 penalty *= rebuildParameters.verticalDelayMultiplier;
                              } else {
                                 penalty *= rebuildParameters.horizontalDelayMultiplier;
                              }

                              float priority = object.priority + penalty;
                              if (priority < priorityMap.get(neighborX, neighborY, neighborZ)) {
                                 priorityMap.put(neighborX, neighborY, neighborZ, priority);
                                 queue.enqueue(new RebuildOperation.QueueObject(neighborX, neighborY, neighborZ, priority));
                              }
                           }
                        }

                        if (blockState.getBlock() != Blocks.STRUCTURE_BLOCK) {
                           if (blockBuffer == null) {
                              blockBuffer = new BlockBuffer();
                           }

                           blockBuffer.set(object.x, object.y, object.z, blockState);
                        }
                     }
                  }

                  if (blockBuffer != null) {
                     Dispatcher.apply(blockBuffer, false);
                  }

                  return;
               }
            }

            resetRebuildState();
         } else {
            resetRebuildState();
         }
      }
   }

   private record QueueObject(int x, int y, int z, float priority) {
   }

   public record RebuildParameters(
      float startDelay,
      float maxDelay,
      float airDelay,
      float sameBlockDelay,
      float differentBlockDelay,
      float randomExtraDelay,
      float horizontalDelayMultiplier,
      float verticalDelayMultiplier
   ) {
   }
}
