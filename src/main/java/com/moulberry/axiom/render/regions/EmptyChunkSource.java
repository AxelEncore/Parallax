package com.moulberry.axiom.render.regions;

import java.util.function.BooleanSupplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.Nullable;

public class EmptyChunkSource extends ChunkSource {
   private final BlockGetter level;
   private final LevelLightEngine lightEngine;

   public EmptyChunkSource(BlockGetter level) {
      this.level = level;
      this.lightEngine = new LevelLightEngine(this, false, false);
   }

   @Nullable
   public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
      return null;
   }

   public void tick(BooleanSupplier booleanSupplier, boolean bl) {
   }

   public String gatherStats() {
      return "0, 0";
   }

   public int getLoadedChunksCount() {
      return 0;
   }

   public LevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   public BlockGetter getLevel() {
      return this.level;
   }
}
