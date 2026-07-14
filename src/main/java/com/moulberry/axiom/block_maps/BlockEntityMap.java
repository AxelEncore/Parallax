package com.moulberry.axiom.block_maps;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityMap {
   private static final Map<BlockState, BlockEntityType<?>> blockBlockEntityTypeMap = new HashMap<>();

   public static BlockEntityType<?> get(BlockState blockState) {
      return blockBlockEntityTypeMap.computeIfAbsent(blockState, state -> {
         for (BlockEntityType<?> blockEntityType : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            if (blockEntityType.isValid(state)) {
               return blockEntityType;
            }
         }

         return null;
      });
   }
}
