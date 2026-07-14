package com.moulberry.axiom.collections;

import com.moulberry.axiom.AsyncChunkProvider;
import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

public class LevelPredicateSet {
   private final Long2ObjectMap<short[]> map = new Long2ObjectOpenHashMap();
   private final AsyncChunkProvider asyncChunkProvider;
   private final Predicate<BlockState> predicate;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private short[] lastChunk = null;

   public LevelPredicateSet(AsyncChunkProvider asyncChunkProvider, Predicate<BlockState> predicate) {
      this.asyncChunkProvider = asyncChunkProvider;
      this.predicate = predicate;
   }

   private short[] computeForSection(long k) {
      short[] array = new short[256];
      int sectionX = BlockPos.getX(k);
      int sectionY = BlockPos.getY(k);
      int sectionZ = BlockPos.getZ(k);
      PalettedContainer<BlockState> blocks = this.asyncChunkProvider.getSection(sectionX, sectionY, sectionZ);
      if (blocks == null) {
         return array;
      } else {
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               short v = 0;

               for (int x = 0; x < 16; x++) {
                  if (this.predicate.test((BlockState)blocks.get(x, y, z))) {
                     v = (short)(v | 1 << x);
                  }
               }

               array[index++] = v;
            }
         }

         return array;
      }
   }

   public void reset() {
      this.map.clear();
      this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
      this.lastChunk = null;
   }

   public boolean contains(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      short[] array = this.getOrCreateChunk(xC, yC, zC);
      int offset = (y & 15) + (z & 15) * 16;
      int bit = 1 << (x & 15);
      return (array[offset] & bit) != 0;
   }

   public short[] getOrCreateChunk(int cx, int cy, int cz) {
      long pos = BlockPos.asLong(cx, cy, cz);
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         short[] chunk = (short[])this.map.computeIfAbsent(pos, this::computeForSection);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }
}
