package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.BlockCondition;
import com.moulberry.axiom.utils.BooleanWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MaskSelection {
   public static void mask(BlockCondition blockCondition) {
      if (!Selection.getSelectionBuffer().isEmpty()) {
         Level level = Minecraft.getInstance().level;
         if (level != null) {
            Selection.modify(region -> {
               ChunkedBooleanRegion chunkedBooleanRegion = new ChunkedBooleanRegion();
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               BooleanWrapper allMatch = new BooleanWrapper(true);
               region.forEach((x, y, z) -> {
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                  if (blockCondition.matches(blockState) && blockState.getBlock() != Blocks.VOID_AIR) {
                     chunkedBooleanRegion.add(x, y, z);
                  } else {
                     allMatch.value = false;
                  }
               });
               if (allMatch.value) {
                  chunkedBooleanRegion.close();
                  return region;
               } else {
                  return chunkedBooleanRegion;
               }
            });
         }
      }
   }
}
