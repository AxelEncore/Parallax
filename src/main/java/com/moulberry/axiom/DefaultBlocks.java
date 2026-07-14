package com.moulberry.axiom;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DefaultBlocks {
   public static BlockState forBlock(Block block) {
      return applyDefaultProperties(block.defaultBlockState());
   }

   public static BlockState applyDefaultProperties(BlockState blockState) {
      if (blockState.hasProperty(BlockStateProperties.PERSISTENT)) {
         blockState = (BlockState)blockState.setValue(BlockStateProperties.PERSISTENT, true);
      }

      return blockState;
   }
}
