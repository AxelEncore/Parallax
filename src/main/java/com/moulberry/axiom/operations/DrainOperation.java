package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import java.text.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class DrainOperation {
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();

   public static void drain() {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         drainAABB(aabb);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         drainSet(set);
      }
   }

   private static void drainAABB(SelectionBuffer.AABB aabb) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         int changeCount = 0;
         int minX = aabb.min().getX();
         int minY = aabb.min().getY();
         int minZ = aabb.min().getZ();
         int maxX = aabb.max().getX();
         int maxY = aabb.max().getY();
         int maxZ = aabb.max().getZ();
         minY = Math.max(world.getMinBuildHeight(), minY);
         maxY = Math.min(world.getMaxBuildHeight() - 1, maxY);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  mutableBlockPos.set(x, y, z);
                  if (!world.isOutsideBuildHeight(mutableBlockPos)) {
                     LevelChunk levelChunk = (LevelChunk)world.getChunk(
                        SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, false
                     );
                     if (levelChunk != null) {
                        BlockState block = levelChunk.getBlockState(mutableBlockPos);
                        if (block.getBlock() != Blocks.VOID_AIR && !block.getFluidState().isEmpty()) {
                           if (block.hasProperty(BlockStateProperties.WATERLOGGED)) {
                              setOperation.set(x, y, z, (BlockState)block.setValue(BlockStateProperties.WATERLOGGED, false));
                           } else {
                              setOperation.set(x, y, z, AIR);
                           }

                           previousBlocksForUndo.set(x, y, z, block);
                           changeCount++;
                        }
                     }
                  }
               }
            }
         }

         String countString = NumberFormat.getInstance().format((long)changeCount);
         String historyDescription = AxiomI18n.get("axiom.history_description.drained_n", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, aabb.center(), historyDescription, 0));
      }
   }

   private static void drainSet(SelectionBuffer.Set set) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         IntWrapper intWrapper = new IntWrapper();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         set.selectionRegion
            .forEach(
               (x, y, z) -> {
                  mutableBlockPos.set(x, y, z);
                  if (!world.isOutsideBuildHeight(mutableBlockPos)) {
                     LevelChunk levelChunk = (LevelChunk)world.getChunk(
                        SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, false
                     );
                     if (levelChunk != null) {
                        BlockState block = levelChunk.getBlockState(mutableBlockPos);
                        if (block.getBlock() != Blocks.VOID_AIR) {
                           if (!block.getFluidState().isEmpty()) {
                              if (block.hasProperty(BlockStateProperties.WATERLOGGED)) {
                                 setOperation.set(x, y, z, (BlockState)block.setValue(BlockStateProperties.WATERLOGGED, false));
                              } else {
                                 setOperation.set(x, y, z, AIR);
                              }

                              previousBlocksForUndo.set(x, y, z, block);
                              intWrapper.value++;
                           }
                        }
                     }
                  }
               }
            );
         String countString = NumberFormat.getInstance().format((long)intWrapper.value);
         String historyDescription = AxiomI18n.get("axiom.history_description.drained_n", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, set.selectionRegion.getCenter(), historyDescription, 0));
      }
   }
}
