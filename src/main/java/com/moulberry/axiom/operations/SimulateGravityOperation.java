package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.text.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SimulateGravityOperation {
   public static void gravity() {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         gravity(aabb);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         gravity(set);
      }
   }

   private static void gravity(SelectionBuffer.AABB aabb) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         int minX = aabb.min().getX();
         int minY = aabb.min().getY();
         int minZ = aabb.min().getZ();
         int maxX = aabb.max().getX();
         int maxY = aabb.max().getY();
         int maxZ = aabb.max().getZ();
         minY = Math.max(world.getMinBuildHeight(), minY);
         maxY = Math.min(world.getMaxBuildHeight() - 1, maxY);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         int movedCount = 0;
         int minWorldY = world.getMinBuildHeight();

         for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
               int currentY = Integer.MIN_VALUE;

               for (int searchY = minY - 1; searchY >= minWorldY; searchY--) {
                  BlockState block = world.getBlockState(mutableBlockPos.set(x, searchY, z));
                  if (!block.canBeReplaced()) {
                     currentY = searchY + 1;
                     break;
                  }
               }

               if (currentY == Integer.MIN_VALUE) {
                  for (int y = minY; y <= maxY; y++) {
                     BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                     if (!block.isAir()) {
                        setOperation.set(x, y, z, Blocks.AIR.defaultBlockState());
                        previousBlocksForUndo.set(x, y, z, block);
                        movedCount++;
                     }
                  }
               } else {
                  for (int yx = minY; yx <= maxY; yx++) {
                     BlockState block = world.getBlockState(mutableBlockPos.set(x, yx, z));
                     if (!block.isAir()) {
                        if (currentY != yx) {
                           setOperation.set(x, currentY, z, block);
                           previousBlocksForUndo.set(x, currentY, z, world.getBlockState(mutableBlockPos.set(x, currentY, z)));
                           setOperation.set(x, yx, z, Blocks.AIR.defaultBlockState());
                           previousBlocksForUndo.set(x, yx, z, block);
                           movedCount++;
                        }

                        currentY++;
                     }
                  }
               }
            }
         }

         String countString = NumberFormat.getInstance().format((long)movedCount);
         String historyDescription = AxiomI18n.get("axiom.history_description.moved", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, aabb.center(), historyDescription, 0));
      }
   }

   private static void gravity(SelectionBuffer.Set set) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         IntWrapper movedCount = new IntWrapper();
         int minSection = world.getMinSection();
         int maxSection = world.getMaxSection() - 1;
         int sectionCount = world.getSectionsCount();
         int maxWorldY = world.getMaxBuildHeight() - 1;
         int minWorldY = world.getMinBuildHeight();
         LongSet visitedChunks = new LongOpenHashSet();
         PositionSet positionSet = set.selectionRegion.unsafeGetPositionSet();
         positionSet.forEachChunk((cx, cy, cz, blocks) -> {
            long chunkPos = ChunkPos.asLong(cx, cz);
            if (visitedChunks.add(chunkPos)) {
               short[][] sections = new short[sectionCount][];

               for (int i = minSection; i <= maxSection; i++) {
                  short[] section = positionSet.getChunk(cx, i, cz);
                  sections[i - minSection] = section;
               }

               for (int x = 0; x < 16; x++) {
                  for (int z = 0; z < 16; z++) {
                     int minY;
                     for (minY = minWorldY; minY <= maxWorldY; minY++) {
                        short[] section = sections[(minY >> 4) - minSection];
                        if (section == null) {
                           minY |= 15;
                        } else {
                           int offset = (minY & 15) + z * 16;
                           if ((section[offset] & 1 << x) != 0) {
                              break;
                           }
                        }
                     }

                     int currentY = Integer.MIN_VALUE;

                     for (int searchY = minY - 1; searchY >= minWorldY; searchY--) {
                        int wx = cx * 16 + x;
                        int wz = cz * 16 + z;
                        BlockState block = world.getBlockState(mutableBlockPos.set(wx, searchY, wz));
                        if (!block.canBeReplaced()) {
                           currentY = searchY + 1;
                           break;
                        }
                     }

                     if (currentY == Integer.MIN_VALUE) {
                        for (int y = minY; y < maxWorldY; y++) {
                           short[] section = sections[(y >> 4) - minSection];
                           if (section == null) {
                              y |= 15;
                           } else {
                              int offset = (y & 15) + z * 16;
                              if ((section[offset] & 1 << x) != 0) {
                                 int wx = cx * 16 + x;
                                 int wz = cz * 16 + z;
                                 BlockState block = world.getBlockState(mutableBlockPos.set(wx, y, wz));
                                 if (!block.isAir()) {
                                    setOperation.set(wx, y, wz, Blocks.AIR.defaultBlockState());
                                    previousBlocksForUndo.set(wx, y, wz, block);
                                    movedCount.value++;
                                 }
                              }
                           }
                        }
                     } else {
                        for (int yx = minY; yx < maxWorldY; yx++) {
                           short[] section = sections[(yx >> 4) - minSection];
                           if (section == null) {
                              yx |= 15;
                           } else {
                              int offset = (yx & 15) + z * 16;
                              if ((section[offset] & 1 << x) != 0) {
                                 int wx = cx * 16 + x;
                                 int wz = cz * 16 + z;
                                 BlockState block = world.getBlockState(mutableBlockPos.set(wx, yx, wz));
                                 if (!block.isAir()) {
                                    if (currentY != yx) {
                                       setOperation.set(wx, currentY, wz, block);
                                       previousBlocksForUndo.set(wx, currentY, wz, world.getBlockState(mutableBlockPos.set(wx, currentY, wz)));
                                       setOperation.set(wx, yx, wz, Blocks.AIR.defaultBlockState());
                                       previousBlocksForUndo.set(wx, yx, wz, block);
                                       movedCount.value++;
                                    }

                                    currentY++;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         });
         String countString = NumberFormat.getInstance().format((long)movedCount.value);
         String historyDescription = AxiomI18n.get("axiom.history_description.moved", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, set.center(), historyDescription, 0));
      }
   }
}
