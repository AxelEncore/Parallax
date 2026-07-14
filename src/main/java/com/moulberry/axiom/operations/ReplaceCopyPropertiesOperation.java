package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
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
import net.minecraft.world.level.block.state.properties.Property;

public class ReplaceCopyPropertiesOperation {
   public static void replace(BlockCondition from, CustomBlockState to) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         replaceAABB(aabb, from, to);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         replaceSet(set, from, to);
      }
   }

   private static void replaceAABB(SelectionBuffer.AABB aabb, BlockCondition from, CustomBlockState to) {
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
                  BlockState blockState = world.getBlockState(mutableBlockPos.set(x, y, z));
                  if (from.matches(blockState) && blockState.getBlock() != Blocks.VOID_AIR) {
                     CustomBlockState newState = to;

                     for (Property<?> property : blockState.getProperties()) {
                        if (newState.axiomHasProperty(property)) {
                           newState = newState.setPropertyUnsafe(property, blockState.getValue(property));
                        }
                     }

                     setOperation.set(x, y, z, newState.getVanillaState());
                     changeCount++;
                  }
               }
            }
         }

         if (changeCount != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount);
            String blockName = AxiomI18n.get(to.getCustomBlock().axiom$translationKey());
            String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
            RegionHelper.pushBlockBufferChange(setOperation, aabb.center(), historyDescription, null);
         }
      }
   }

   private static void replaceSet(SelectionBuffer.Set set, BlockCondition from, CustomBlockState to) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         IntWrapper changeCount = new IntWrapper();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         set.selectionRegion.forEach((x, y, z) -> {
            BlockState blockState = world.getBlockState(mutableBlockPos.set(x, y, z));
            if (from.matches(blockState) && blockState.getBlock() != Blocks.VOID_AIR) {
               CustomBlockState newState = to;

               for (Property<?> property : blockState.getProperties()) {
                  if (newState.axiomHasProperty(property)) {
                     newState = newState.setPropertyUnsafe(property, blockState.getValue(property));
                  }
               }

               setOperation.set(x, y, z, newState.getVanillaState());
               changeCount.value++;
            }
         });
         if (changeCount.value != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount.value);
            String blockName = AxiomI18n.get(to.getCustomBlock().axiom$translationKey());
            String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
            RegionHelper.pushBlockBufferChange(setOperation, set.selectionRegion.getCenter(), historyDescription, null);
         }
      }
   }
}
