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
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class UpdateBlocksOperation {
   public static void updateBlocks() {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         updateBlocksAABB(aabb);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         updateBlocksSet(set);
      }
   }

   private static void updateBlocksAABB(SelectionBuffer.AABB aabb) {
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
         Direction[] directions = Direction.values();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         MutableBlockPos mutableBlockPos2 = new MutableBlockPos();
         RandomSource randomSource = RandomSource.create();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                  if (block.getBlock() != Blocks.VOID_AIR) {
                     BlockState originalBlock = block;

                     for (Direction direction : directions) {
                        mutableBlockPos2.setWithOffset(mutableBlockPos, direction);
                        block = block.updateShape(direction, world.getBlockState(mutableBlockPos2), world, mutableBlockPos, mutableBlockPos2);
                     }

                     if (originalBlock != block) {
                        setOperation.set(x, y, z, block);
                        previousBlocksForUndo.set(x, y, z, originalBlock);
                        changeCount++;
                     }
                  }
               }
            }
         }

         String countString = NumberFormat.getInstance().format((long)changeCount);
         String historyDescription = AxiomI18n.get("axiom.history_description.updated", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, aabb.center(), historyDescription, 0));
      }
   }

   private static void updateBlocksSet(SelectionBuffer.Set set) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         IntWrapper intWrapper = new IntWrapper();
         Direction[] directions = Direction.values();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         MutableBlockPos mutableBlockPos2 = new MutableBlockPos();
         RandomSource randomSource = RandomSource.create();
         set.selectionRegion.forEach((x, y, z) -> {
            BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
            if (block.getBlock() != Blocks.VOID_AIR) {
               BlockState originalBlock = block;

               for (Direction direction : directions) {
                  mutableBlockPos2.setWithOffset(mutableBlockPos, direction);
                  block = block.updateShape(direction, world.getBlockState(mutableBlockPos2), world, mutableBlockPos, mutableBlockPos2);
               }

               if (originalBlock != block) {
                  setOperation.set(x, y, z, block);
                  previousBlocksForUndo.set(x, y, z, originalBlock);
                  intWrapper.value++;
               }
            }
         });
         String countString = NumberFormat.getInstance().format((long)intWrapper.value);
         String historyDescription = AxiomI18n.get("axiom.history_description.updated", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBlocksForUndo, set.selectionRegion.getCenter(), historyDescription, 0));
      }
   }
}
