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
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class WaterlogOperation {
   private static final BlockState WATER = Blocks.WATER.defaultBlockState();

   public static void waterlog() {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         waterlogAABB(aabb);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         waterlogSet(set);
      }
   }

   private static void waterlogAABB(SelectionBuffer.AABB aabb) {
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
                        if (block.getBlock() != Blocks.VOID_AIR) {
                           if (!block.isAir() && (!(block.getBlock() instanceof LiquidBlock) || !block.canBeReplaced())) {
                              if (block.hasProperty(BlockStateProperties.WATERLOGGED)) {
                                 setOperation.set(x, y, z, (BlockState)block.setValue(BlockStateProperties.WATERLOGGED, true));
                                 previousBlocksForUndo.set(x, y, z, block);
                                 changeCount++;
                              }
                           } else {
                              setOperation.set(x, y, z, WATER);
                              previousBlocksForUndo.set(x, y, z, block);
                              changeCount++;
                           }
                        }
                     }
                  }
               }
            }
         }

         String countString = NumberFormat.getInstance().format((long)changeCount);
         String historyDescription = AxiomI18n.get("axiom.history_description.waterlogged_n", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, aabb.center(), historyDescription, 0));
      }
   }

   private static void waterlogSet(SelectionBuffer.Set set) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         IntWrapper changeCount = new IntWrapper();
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
                           if (!block.isAir() && (!(block.getBlock() instanceof LiquidBlock) || !block.canBeReplaced())) {
                              if (block.hasProperty(BlockStateProperties.WATERLOGGED)) {
                                 setOperation.set(x, y, z, (BlockState)block.setValue(BlockStateProperties.WATERLOGGED, true));
                                 previousBlocksForUndo.set(x, y, z, block);
                                 changeCount.value++;
                              }
                           } else {
                              setOperation.set(x, y, z, WATER);
                              previousBlocksForUndo.set(x, y, z, block);
                              changeCount.value++;
                           }
                        }
                     }
                  }
               }
            );
         String countString = NumberFormat.getInstance().format((long)changeCount.value);
         String historyDescription = AxiomI18n.get("axiom.history_description.waterlogged_n", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, set.selectionRegion.getCenter(), historyDescription, 0));
      }
   }
}
