package com.moulberry.axiom.tools;

import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ChamferClosestBlockMap {
   private static final int STRIDE_X = 324;
   private static final int STRIDE_Y = 18;
   private static final int STRIDE_Z = 1;
   private static final float SQRT_2 = (float)Math.sqrt(2.0);
   private static final float SQRT_3 = (float)Math.sqrt(3.0);
   private final Level level;
   private final Predicate<BlockState> blockFilter;
   private final ChamferClosestBlockMap.PropagateData[] missingChunk;
   private final ChamferClosestBlockMap.PropagateData[] airChunk;
   private final Long2ObjectMap<ChamferClosestBlockMap.PropagateData[]> map = new Long2ObjectOpenHashMap();
   private final Long2IntMap levels = new Long2IntOpenHashMap();
   private final int maxLevel;
   private LongSet maskPositions = null;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private ChamferClosestBlockMap.PropagateData[] lastChunk = null;

   public ChamferClosestBlockMap(Level level, Predicate<BlockState> blockFilter, int maxDistance) {
      this.level = level;
      this.blockFilter = blockFilter;
      this.maxLevel = (maxDistance + 15) / 16 + 3;
      if (blockFilter.test(Blocks.AIR.defaultBlockState())) {
         ChamferClosestBlockMap.PropagateData airData = new ChamferClosestBlockMap.PropagateData(0.0F, Blocks.AIR.defaultBlockState());
         this.airChunk = new ChamferClosestBlockMap.PropagateData[5832];
         Arrays.fill(this.airChunk, airData);
      } else {
         ChamferClosestBlockMap.PropagateData airData = new ChamferClosestBlockMap.PropagateData(Float.MAX_VALUE, Blocks.AIR.defaultBlockState());
         this.airChunk = new ChamferClosestBlockMap.PropagateData[5832];
         Arrays.fill(this.airChunk, airData);
      }

      ChamferClosestBlockMap.PropagateData missingData = new ChamferClosestBlockMap.PropagateData(Float.MAX_VALUE, Blocks.VOID_AIR.defaultBlockState());
      this.missingChunk = new ChamferClosestBlockMap.PropagateData[5832];
      Arrays.fill(this.missingChunk, missingData);
   }

   public void withMaskPositions(LongSet maskPositions) {
      this.maskPositions = maskPositions;
   }

   public BlockState get(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      ChamferClosestBlockMap.PropagateData[] array = this.getOrCreateChunk(xC, yC, zC);
      return array[createIndex(x & 15, y & 15, z & 15)].blockState;
   }

   private ChamferClosestBlockMap.PropagateData[] getOrCreateChunk(int xC, int yC, int zC) {
      return this.getOrCreateChunk(BlockPos.asLong(xC, yC, zC));
   }

   private ChamferClosestBlockMap.PropagateData[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         int level = this.levels.get(pos);
         if (level != this.maxLevel) {
            this.propagateToMaxLevel(pos);
         }

         ChamferClosestBlockMap.PropagateData[] chunk = (ChamferClosestBlockMap.PropagateData[])this.map.get(pos);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   private void preparePropagation(long pos, int level, Long2IntOpenHashMap toPropagate) {
      int currentLevel = Math.max(this.levels.get(pos), toPropagate.get(pos));
      if (currentLevel < level) {
         toPropagate.put(pos, level);
         if (level > 1) {
            this.preparePropagation(BlockPos.offset(pos, -1, 0, 0), level - 1, toPropagate);
            this.preparePropagation(BlockPos.offset(pos, 1, 0, 0), level - 1, toPropagate);
            this.preparePropagation(BlockPos.offset(pos, 0, -1, 0), level - 1, toPropagate);
            this.preparePropagation(BlockPos.offset(pos, 0, 1, 0), level - 1, toPropagate);
            this.preparePropagation(BlockPos.offset(pos, 0, 0, -1), level - 1, toPropagate);
            this.preparePropagation(BlockPos.offset(pos, 0, 0, 1), level - 1, toPropagate);
         }
      }
   }

   private void propagateToMaxLevel(long pos) {
      Long2IntOpenHashMap toPropagate = new Long2IntOpenHashMap();
      this.preparePropagation(pos, this.maxLevel, toPropagate);
      List<LongList> ordered = new ArrayList<>();
      toPropagate.long2IntEntrySet().fastForEach(entry -> {
         int oldLevel = this.levels.get(entry.getLongKey());
         int newLevel = entry.getIntValue();

         while (ordered.size() < newLevel) {
            ordered.add(new LongArrayList());
         }

         for (int levelx = oldLevel + 1; levelx <= newLevel; levelx++) {
            ordered.get(levelx - 1).add(entry.getLongKey());
         }

         this.levels.put(entry.getLongKey(), newLevel);
      });
      int level = 1;

      for (LongList positions : ordered) {
         if (!positions.isEmpty()) {
            LongIterator iterator = positions.longIterator();

            while (iterator.hasNext()) {
               this.propagateToLevel(iterator.nextLong(), level);
            }

            level++;
         }
      }
   }

   private void propagateToLevel(long pos, int level) {
      if (level == 1) {
         this.map.put(pos, this.fillInitial(pos));
      } else {
         ChamferClosestBlockMap.PropagateData[] center = (ChamferClosestBlockMap.PropagateData[])this.map.get(pos);
         copyFace((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, 0, 0)), center, -324, 18, 1);
         copyFace((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, 0, 0)), center, 324, 18, 1);
         copyFace((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, -1, 0)), center, -18, 324, 1);
         copyFace((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, 1, 0)), center, 18, 324, 1);
         copyFace((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, 0, -1)), center, -1, 324, 18);
         copyFace((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, 0, 1)), center, 1, 324, 18);
         if (level > 2) {
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, -1, 0)), center, -324, -18, 1);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, -1, 0)), center, 324, -18, 1);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, 1, 0)), center, -324, 18, 1);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, 1, 0)), center, 324, 18, 1);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, -1, -1)), center, -1, -18, 324);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, -1, 1)), center, 1, -18, 324);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, 1, -1)), center, -1, 18, 324);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 0, 1, 1)), center, 1, 18, 324);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, 0, -1)), center, -1, -324, 18);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, 0, 1)), center, 1, -324, 18);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, 0, -1)), center, -1, 324, 18);
            copyEdge((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, 0, 1)), center, 1, 324, 18);
         }

         if (level > 3) {
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, -1, -1)), center, -324, -18, -1);
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, -1, -1)), center, 324, -18, -1);
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, 1, -1)), center, -324, 18, -1);
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, 1, -1)), center, 324, 18, -1);
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, -1, 1)), center, -324, -18, 1);
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, -1, 1)), center, 324, -18, 1);
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, -1, 1, 1)), center, -324, 18, 1);
            copyCorner((ChamferClosestBlockMap.PropagateData[])this.map.get(BlockPos.offset(pos, 1, 1, 1)), center, 324, 18, 1);
         }

         propagate(center);
      }
   }

   private static void copyFace(ChamferClosestBlockMap.PropagateData[] from, ChamferClosestBlockMap.PropagateData[] to, int strideA, int strideB, int strideC) {
      int fromOffset;
      int toOffset;
      if (strideA < 0) {
         toOffset = 0;
         fromOffset = -16 * strideA;
      } else {
         toOffset = 17 * strideA;
         fromOffset = strideA;
      }

      for (int b = 0; b < 16; b++) {
         for (int c = 0; c < 16; c++) {
            to[toOffset + (b + 1) * strideB + (c + 1) * strideC] = from[fromOffset + (b + 1) * strideB + (c + 1) * strideC];
         }
      }
   }

   private static void copyEdge(ChamferClosestBlockMap.PropagateData[] from, ChamferClosestBlockMap.PropagateData[] to, int strideA, int strideB, int strideC) {
      int fromOffset;
      int toOffset;
      if (strideA < 0) {
         toOffset = 0;
         fromOffset = -16 * strideA;
      } else {
         toOffset = 17 * strideA;
         fromOffset = strideA;
      }

      if (strideB < 0) {
         toOffset += 0;
         fromOffset += -16 * strideB;
      } else {
         toOffset += 17 * strideB;
         fromOffset += strideB;
      }

      for (int c = 0; c < 16; c++) {
         to[toOffset + (c + 1) * strideC] = from[fromOffset + (c + 1) * strideC];
      }
   }

   private static void copyCorner(ChamferClosestBlockMap.PropagateData[] from, ChamferClosestBlockMap.PropagateData[] to, int strideA, int strideB, int strideC) {
      int fromOffset;
      int toOffset;
      if (strideA < 0) {
         toOffset = 0;
         fromOffset = -16 * strideA;
      } else {
         toOffset = 17 * strideA;
         fromOffset = strideA;
      }

      if (strideB < 0) {
         toOffset += 0;
         fromOffset += -16 * strideB;
      } else {
         toOffset += 17 * strideB;
         fromOffset += strideB;
      }

      if (strideC < 0) {
         toOffset += 0;
         fromOffset += -16 * strideC;
      } else {
         toOffset += 17 * strideC;
         fromOffset += strideC;
      }

      to[toOffset] = from[fromOffset];
   }

   private static int createIndex(int x, int y, int z) {
      return (x + 1) * 324 + (y + 1) * 18 + (z + 1) * 1;
   }

   private ChamferClosestBlockMap.PropagateData[] fillInitial(long pos) {
      int cx = BlockPos.getX(pos);
      int cy = BlockPos.getY(pos);
      int cz = BlockPos.getZ(pos);
      LevelChunk chunk = (LevelChunk)this.level.getChunk(cx, cz, ChunkStatus.FULL, false);
      if (chunk == null) {
         return Arrays.copyOf(this.missingChunk, this.missingChunk.length);
      } else {
         int sectionIndex = chunk.getSectionIndexFromSectionY(cy);
         if (sectionIndex >= 0 && sectionIndex < chunk.getSectionsCount()) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            if (section.hasOnlyAir()) {
               return Arrays.copyOf(this.airChunk, this.airChunk.length);
            } else {
               section.acquire();

               ChamferClosestBlockMap.PropagateData[] y;
               try {
                  PalettedContainer<BlockState> states = section.getStates();
                  if (!states.maybeHas(this.blockFilter)) {
                     return Arrays.copyOf(this.missingChunk, this.missingChunk.length);
                  }

                  ChamferClosestBlockMap.PropagateData[] data = Arrays.copyOf(this.missingChunk, this.missingChunk.length);
                  Palette<BlockState> palette = states.data.palette();
                  if (palette.getSize() != 1 || this.maskPositions != null) {
                     boolean allMatching = true;

                     for (int x = 0; x < 16; x++) {
                        for (int yx = 0; yx < 16; yx++) {
                           for (int z = 0; z < 16; z++) {
                              if (this.maskPositions != null && !this.maskPositions.contains(BlockPos.asLong(cx * 16 + x, cy * 16 + yx, cz * 16 + z))) {
                                 allMatching = false;
                              } else {
                                 BlockState blockState = (BlockState)states.get(x, yx, z);
                                 if (this.blockFilter.test(blockState)) {
                                    data[createIndex(x, yx, z)] = new ChamferClosestBlockMap.PropagateData(0.0F, blockState);
                                 } else {
                                    allMatching = false;
                                 }
                              }
                           }
                        }
                     }

                     if (!allMatching) {
                        propagate(data);
                     }

                     return data;
                  }

                  BlockState blockState = (BlockState)palette.valueFor(0);
                  if (!this.blockFilter.test(blockState)) {
                     return Arrays.copyOf(this.missingChunk, this.missingChunk.length);
                  }

                  ChamferClosestBlockMap.PropagateData entry = new ChamferClosestBlockMap.PropagateData(0.0F, blockState);
                  Arrays.fill(data, entry);
                  y = data;
               } finally {
                  section.release();
               }

               return y;
            }
         } else {
            return Arrays.copyOf(this.missingChunk, this.missingChunk.length);
         }
      }
   }

   private static void propagate(ChamferClosestBlockMap.PropagateData[] data) {
      for (int x = 0; x < 16; x++) {
         for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
               int index = createIndex(x, y, z);
               ChamferClosestBlockMap.PropagateData datum = data[index];
               float least = datum.distance;
               BlockState closest = datum.blockState;
               ChamferClosestBlockMap.PropagateData next = data[index - 324 - 18 - 1];
               if (next.distance + SQRT_3 < least) {
                  least = next.distance + SQRT_3;
                  closest = next.blockState;
               }

               next = data[index - 324 - 18];
               if (next.distance + SQRT_2 < least) {
                  least = next.distance + SQRT_2;
                  closest = next.blockState;
               }

               next = data[index - 324 - 18 + 1];
               if (next.distance + SQRT_3 < least) {
                  least = next.distance + SQRT_3;
                  closest = next.blockState;
               }

               next = data[index - 324 - 1];
               if (next.distance + SQRT_2 < least) {
                  least = next.distance + SQRT_2;
                  closest = next.blockState;
               }

               next = data[index - 324];
               if (next.distance + 1.0F < least) {
                  least = next.distance + 1.0F;
                  closest = next.blockState;
               }

               next = data[index - 324 + 1];
               if (next.distance + SQRT_2 < least) {
                  least = next.distance + SQRT_2;
                  closest = next.blockState;
               }

               next = data[index - 324 + 18 - 1];
               if (next.distance + SQRT_3 < least) {
                  least = next.distance + SQRT_3;
                  closest = next.blockState;
               }

               next = data[index - 324 + 18];
               if (next.distance + SQRT_2 < least) {
                  least = next.distance + SQRT_2;
                  closest = next.blockState;
               }

               next = data[index - 324 + 18 + 1];
               if (next.distance + SQRT_3 < least) {
                  least = next.distance + SQRT_3;
                  closest = next.blockState;
               }

               next = data[index - 18 - 1];
               if (next.distance + SQRT_2 < least) {
                  least = next.distance + SQRT_2;
                  closest = next.blockState;
               }

               next = data[index - 18];
               if (next.distance + 1.0F < least) {
                  least = next.distance + 1.0F;
                  closest = next.blockState;
               }

               next = data[index - 18 + 1];
               if (next.distance + SQRT_2 < least) {
                  least = next.distance + SQRT_2;
                  closest = next.blockState;
               }

               next = data[index - 1];
               if (next.distance + 1.0F < least) {
                  least = next.distance + 1.0F;
                  closest = next.blockState;
               }

               if (least < datum.distance) {
                  data[index] = new ChamferClosestBlockMap.PropagateData(least, closest);
               }
            }
         }
      }

      for (int x = 15; x >= 0; x--) {
         for (int y = 15; y >= 0; y--) {
            for (int z = 15; z >= 0; z--) {
               int indexx = createIndex(x, y, z);
               ChamferClosestBlockMap.PropagateData datumx = data[indexx];
               float leastx = datumx.distance;
               BlockState closestx = datumx.blockState;
               ChamferClosestBlockMap.PropagateData nextx = data[indexx + 324 + 18 + 1];
               if (nextx.distance + SQRT_3 < leastx) {
                  leastx = nextx.distance + SQRT_3;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324 + 18];
               if (nextx.distance + SQRT_2 < leastx) {
                  leastx = nextx.distance + SQRT_2;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324 + 18 - 1];
               if (nextx.distance + SQRT_3 < leastx) {
                  leastx = nextx.distance + SQRT_3;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324 + 1];
               if (nextx.distance + SQRT_2 < leastx) {
                  leastx = nextx.distance + SQRT_2;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324];
               if (nextx.distance + 1.0F < leastx) {
                  leastx = nextx.distance + 1.0F;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324 - 1];
               if (nextx.distance + SQRT_2 < leastx) {
                  leastx = nextx.distance + SQRT_2;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324 - 18 + 1];
               if (nextx.distance + SQRT_3 < leastx) {
                  leastx = nextx.distance + SQRT_3;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324 - 18];
               if (nextx.distance + SQRT_2 < leastx) {
                  leastx = nextx.distance + SQRT_2;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 324 - 18 - 1];
               if (nextx.distance + SQRT_3 < leastx) {
                  leastx = nextx.distance + SQRT_3;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 18 + 1];
               if (nextx.distance + SQRT_2 < leastx) {
                  leastx = nextx.distance + SQRT_2;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 18];
               if (nextx.distance + 1.0F < leastx) {
                  leastx = nextx.distance + 1.0F;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 18 - 1];
               if (nextx.distance + SQRT_2 < leastx) {
                  leastx = nextx.distance + SQRT_2;
                  closestx = nextx.blockState;
               }

               nextx = data[indexx + 1];
               if (nextx.distance + 1.0F < leastx) {
                  leastx = nextx.distance + 1.0F;
                  closestx = nextx.blockState;
               }

               if (leastx < datumx.distance) {
                  data[indexx] = new ChamferClosestBlockMap.PropagateData(leastx, closestx);
               }
            }
         }
      }
   }

   private record PropagateData(float distance, BlockState blockState) {
   }
}
