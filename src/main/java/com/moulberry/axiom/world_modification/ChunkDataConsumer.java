package com.moulberry.axiom.world_modification;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

@FunctionalInterface
public interface ChunkDataConsumer {
   void accept(Long2ObjectMap<CompressedBlockEntity> var1, Long2ObjectMap<PalettedContainer<BlockState>> var2);
}
