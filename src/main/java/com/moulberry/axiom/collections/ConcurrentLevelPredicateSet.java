package com.moulberry.axiom.collections;

import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

public class ConcurrentLevelPredicateSet {
   private final StampedLock lock;
   private final Long2ObjectMap<short[]> map;
   private final Level level;
   private final Predicate<BlockState> predicate;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private short[] lastChunk = null;

   public ConcurrentLevelPredicateSet(Level level, Predicate<BlockState> predicate) {
      this.lock = new StampedLock();
      this.map = new Long2ObjectOpenHashMap();
      this.level = level;
      this.predicate = predicate;
   }

   public ConcurrentLevelPredicateSet(ConcurrentLevelPredicateSet other) {
      this.lock = other.lock;
      this.map = other.map;
      this.level = other.level;
      this.predicate = other.predicate;
      this.lastChunkPos = other.lastChunkPos;
      this.lastChunk = other.lastChunk;
   }

   private short[] computeForSection(int cx, int cy, int cz) {
      short[] array = new short[256];
      LevelChunk chunk = this.level.getChunk(cx, cz);
      int sectionIndex = chunk.getSectionIndexFromSectionY(cy);
      if (sectionIndex >= 0 && sectionIndex < chunk.getSectionsCount()) {
         LevelChunkSection section = chunk.getSection(sectionIndex);
         PalettedContainer<BlockState> blocks = section.getStates();
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
      } else {
         return array;
      }
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
         short[] chunk = null;
         long stamp = this.lock.tryOptimisticRead();
         if (stamp != 0L) {
            chunk = (short[])this.map.get(pos);
         }

         if (!this.lock.validate(stamp)) {
            stamp = this.lock.readLock();

            try {
               chunk = (short[])this.map.get(pos);
            } finally {
               this.lock.unlockRead(stamp);
            }
         }

         if (chunk == null) {
            chunk = this.computeForSection(cx, cy, cz);
            stamp = this.lock.writeLock();

            try {
               this.map.put(pos, chunk);
            } finally {
               this.lock.unlockWrite(stamp);
            }
         }

         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }
}
