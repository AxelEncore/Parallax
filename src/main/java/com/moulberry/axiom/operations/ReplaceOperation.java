package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.BlockCondition;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import java.text.NumberFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceOperation {
   public static void replace(BlockCondition blockCondition, BlockState to) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         replaceAABB(aabb, blockCondition, to);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         replaceSet(set, blockCondition, to);
      }
   }

   private static void replaceAABB(SelectionBuffer.AABB aabb, BlockCondition blockCondition, BlockState to) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
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
                  BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                  if (blockCondition.matches(block) && block.getBlock() != Blocks.VOID_AIR) {
                     setOperation.set(x, y, z, to);
                     changeCount++;
                  }
               }
            }
         }

         if (changeCount != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount);
            String blockName = AxiomI18n.get(to.getBlock().getDescriptionId());
            String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
            RegionHelper.pushBlockBufferChange(setOperation, aabb.center(), historyDescription, null);
         }
      }
   }

   private static void replaceSet(SelectionBuffer.Set set, BlockCondition blockCondition, BlockState to) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         IntWrapper changeCount = new IntWrapper();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         set.selectionRegion.forEach((x, y, z) -> {
            BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
            if (blockCondition.matches(block) && block.getBlock() != Blocks.VOID_AIR) {
               setOperation.set(x, y, z, to);
               changeCount.value++;
            }
         });
         if (changeCount.value != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount.value);
            String blockName = AxiomI18n.get(to.getBlock().getDescriptionId());
            String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
            RegionHelper.pushBlockBufferChange(setOperation, set.selectionRegion.getCenter(), historyDescription, null);
         }
      }
   }
}
