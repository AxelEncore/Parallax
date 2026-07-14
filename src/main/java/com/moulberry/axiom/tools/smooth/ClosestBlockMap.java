package com.moulberry.axiom.tools.smooth;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ClosestBlockMap {
   private static final int STRIDE_X = 2304;
   private static final int STRIDE_Y = 48;
   private static final int STRIDE_Z = 1;
   private final Predicate<BlockState> blockFilter;
   private final BlockState[] emptyArray;
   private final Long2ObjectMap<BlockState[]> map = new Long2ObjectOpenHashMap();
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private BlockState[] lastChunk = null;
   private final ClosestBlockMap.PropagateData[] propagateDataArray = new ClosestBlockMap.PropagateData[110592];

   public ClosestBlockMap(Predicate<BlockState> blockFilter, BlockState emptyState) {
      this.blockFilter = blockFilter;
      if (!blockFilter.test(emptyState)) {
         throw new FaultyImplementationError();
      } else {
         this.emptyArray = new BlockState[4096];
         Arrays.fill(this.emptyArray, emptyState);
      }
   }

   public BlockState get(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      BlockState[] array = this.getOrCreateChunk(xC, yC, zC);
      return array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16];
   }

   private BlockState[] getOrCreateChunk(int xC, int yC, int zC) {
      return this.getOrCreateChunk(BlockPos.asLong(xC, yC, zC));
   }

   private BlockState[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         BlockState[] chunk = (BlockState[])this.map.computeIfAbsent(pos, this::calculateBlocks);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   private BlockState[] calculateBlocks(long pos) {
      int cx = BlockPos.getX(pos);
      int cy = BlockPos.getY(pos);
      int cz = BlockPos.getZ(pos);
      Level world = Minecraft.getInstance().level;
      if (world == null) {
         return this.emptyArray;
      } else {
         Arrays.fill(this.propagateDataArray, null);
         IntArrayFIFOQueue propagateQueue = new IntArrayFIFOQueue();
         LevelChunk chunk = (LevelChunk)world.getChunk(cx, cz, ChunkStatus.FULL, false);
         if (chunk == null) {
            return this.emptyArray;
         } else {
            int sectionIndex = chunk.getSectionIndexFromSectionY(cy);
            if (sectionIndex >= 0 && sectionIndex < chunk.getSectionsCount()) {
               LevelChunkSection section = chunk.getSection(sectionIndex);
               if (section.hasOnlyAir()) {
                  if (this.blockFilter.test(Blocks.AIR.defaultBlockState())) {
                     return this.emptyArray;
                  }
               } else if (section.maybeHas(this.blockFilter)) {
                  this.initialQueueChunkSection(propagateQueue, 0, 0, 0, section);
               }

               for (int cxo = -1; cxo <= 1; cxo++) {
                  for (int czo = -1; czo <= 1; czo++) {
                     LevelChunk chunkx = (LevelChunk)world.getChunk(cx + cxo, cz + czo, ChunkStatus.FULL, false);
                     if (chunkx != null) {
                        for (int cyo = -1; cyo <= 1; cyo++) {
                           if (cxo != 0 || cyo != 0 || czo != 0) {
                              int sectionIndexx = chunkx.getSectionIndexFromSectionY(cy + cyo);
                              if (sectionIndexx >= 0 && sectionIndexx < chunkx.getSectionsCount()) {
                                 LevelChunkSection sectionx = chunkx.getSection(sectionIndexx);
                                 if (!sectionx.hasOnlyAir()) {
                                    if (sectionx.maybeHas(this.blockFilter)) {
                                       this.initialQueueChunkSection(propagateQueue, cxo, czo, cyo, sectionx);
                                    }
                                 } else if (this.blockFilter.test(Blocks.AIR.defaultBlockState())) {
                                    BlockState air = Blocks.AIR.defaultBlockState();
                                    int baseIndex = (cxo + 1) * 16 * 2304 + (cyo + 1) * 16 * 48 + (czo + 1) * 16 * 1;
                                    int minX = 0;
                                    int minY = 0;
                                    int minZ = 0;
                                    int maxX = 16;
                                    int maxY = 16;
                                    int maxZ = 16;
                                    if (cxo == -1) {
                                       minX = 15;
                                    } else if (cxo == 1) {
                                       maxX = 1;
                                    }

                                    if (cyo == -1) {
                                       minY = 15;
                                    } else if (cyo == 1) {
                                       maxY = 1;
                                    }

                                    if (czo == -1) {
                                       minZ = 15;
                                    } else if (czo == 1) {
                                       maxZ = 1;
                                    }

                                    for (int x = minX; x < maxX; x++) {
                                       for (int y = minY; y < maxY; y++) {
                                          for (int z = minZ; z < maxZ; z++) {
                                             int index = baseIndex + x * 2304 + y * 48 + z * 1;
                                             ClosestBlockMap.PropagateDataBlock propagateDataBlock = new ClosestBlockMap.PropagateDataBlock(index, air);
                                             ClosestBlockMap.PropagateData propagateData = new ClosestBlockMap.PropagateData(0, List.of(propagateDataBlock));
                                             propagateData.queued = true;
                                             this.propagateDataArray[index] = propagateData;
                                             propagateQueue.enqueue(index);
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (propagateQueue.isEmpty()) {
                  return this.emptyArray;
               } else {
                  while (!propagateQueue.isEmpty()) {
                     int index = propagateQueue.dequeueInt();
                     sectionIndex = index / 2304;
                     int y = (index - sectionIndex * 2304) / 48;
                     int z = (index - sectionIndex * 2304 - y * 48) / 1;
                     ClosestBlockMap.PropagateData propagateData = this.propagateDataArray[index];
                     propagateData.queued = false;
                     if (sectionIndex < 31) {
                        propagateTo(index + 2304, this.propagateDataArray, propagateQueue, propagateData.blocks);
                     }

                     if (sectionIndex > 16) {
                        propagateTo(index - 2304, this.propagateDataArray, propagateQueue, propagateData.blocks);
                     }

                     if (y < 31) {
                        propagateTo(index + 48, this.propagateDataArray, propagateQueue, propagateData.blocks);
                     }

                     if (y > 16) {
                        propagateTo(index - 48, this.propagateDataArray, propagateQueue, propagateData.blocks);
                     }

                     if (z < 31) {
                        propagateTo(index + 1, this.propagateDataArray, propagateQueue, propagateData.blocks);
                     }

                     if (z > 16) {
                        propagateTo(index - 1, this.propagateDataArray, propagateQueue, propagateData.blocks);
                     }
                  }

                  BlockState[] finalBlocks = new BlockState[4096];
                  int indexx = 0;

                  for (int zx = 16; zx < 32; zx++) {
                     for (int yx = 16; yx < 32; yx++) {
                        for (int x = 16; x < 32; x++) {
                           List<ClosestBlockMap.PropagateDataBlock> blockList = this.propagateDataArray[x * 2304 + yx * 48 + zx * 1].blocks;
                           if (blockList.size() == 1) {
                              finalBlocks[indexx++] = blockList.get(0).blockState;
                           } else {
                              BlockState bestBlock = null;
                              int leastVerticalDistance = Integer.MAX_VALUE;

                              for (ClosestBlockMap.PropagateDataBlock block : blockList) {
                                 int oy = (block.origin - block.origin / 2304 * 2304) / 48;
                                 int dy = Math.abs(oy - yx);
                                 if (dy < leastVerticalDistance) {
                                    leastVerticalDistance = dy;
                                    bestBlock = block.blockState;
                                 }
                              }

                              finalBlocks[indexx++] = bestBlock;
                           }
                        }
                     }
                  }

                  return finalBlocks;
               }
            } else {
               return this.emptyArray;
            }
         }
      }
   }

   private void initialQueueChunkSection(IntArrayFIFOQueue propagateQueue, int cxo, int czo, int cyo, LevelChunkSection section) {
      PalettedContainer<BlockState> container = section.getStates();
      int index = (cxo + 1) * 16 * 2304 + (cyo + 1) * 16 * 48 + (czo + 1) * 16 * 1;

      for (int x = 0; x < 16; x++) {
         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               BlockState block = (BlockState)container.get(x, y, z);
               if (this.blockFilter.test(block)) {
                  ClosestBlockMap.PropagateDataBlock propagateDataBlock = new ClosestBlockMap.PropagateDataBlock(index, block);
                  ClosestBlockMap.PropagateData propagateData = new ClosestBlockMap.PropagateData(0, List.of(propagateDataBlock));
                  propagateData.queued = true;
                  this.propagateDataArray[index] = propagateData;
                  propagateQueue.enqueue(index);
               }

               index++;
            }

            index += 32;
         }

         index += 1536;
      }
   }

   private static void propagateTo(
      int index, ClosestBlockMap.PropagateData[] propagateDataArray, IntArrayFIFOQueue propagateQueue, List<ClosestBlockMap.PropagateDataBlock> blocks
   ) {
      ClosestBlockMap.PropagateData propagateData = propagateDataArray[index];
      if (propagateData == null || propagateData.minDistanceSq != 0) {
         int minDistanceSq = Integer.MAX_VALUE;
         ClosestBlockMap.PropagateDataBlock minPropagate = null;
         int x = index / 2304;
         int y = (index - x * 2304) / 48;
         int z = (index - x * 2304 - y * 48) / 1;

         for (ClosestBlockMap.PropagateDataBlock block : blocks) {
            int ox = block.origin / 2304;
            int oy = (block.origin - ox * 2304) / 48;
            int oz = (block.origin - ox * 2304 - oy * 48) / 1;
            int dx = ox - x;
            int dy = oy - y;
            int dz = oz - z;
            int distanceSq = dx * dx + dy * dy + dz * dz;
            if (distanceSq < minDistanceSq) {
               minDistanceSq = distanceSq;
               minPropagate = block;
            }
         }

         if (propagateData == null) {
            List<ClosestBlockMap.PropagateDataBlock> list = new ArrayList<>();
            list.add(minPropagate);
            propagateData = new ClosestBlockMap.PropagateData(minDistanceSq, list);
            propagateData.queued = true;
            propagateDataArray[index] = propagateData;
            propagateQueue.enqueue(index);
         } else if (minDistanceSq < propagateData.minDistanceSq) {
            propagateData.minDistanceSq = minDistanceSq;
            propagateData.blocks.clear();
            propagateData.blocks.add(minPropagate);
            if (!propagateData.queued) {
               propagateQueue.enqueue(index);
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
      private final List<ClosestBlockMap.PropagateDataBlock> blocks;

      private PropagateData(int minDistanceSq, List<ClosestBlockMap.PropagateDataBlock> blocks) {
         this.minDistanceSq = minDistanceSq;
         this.blocks = blocks;
      }
   }

   private record PropagateDataBlock(int origin, BlockState blockState) {
   }
}
