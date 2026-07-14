package com.moulberry.axiom.collections;

import com.moulberry.axiom.funcinterfaces.TriIntPredicate;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.function.LongFunction;
import net.minecraft.world.level.ChunkPos;

public class Position2dToIntMap {
   private final int defaultValue;
   private final LongFunction<int[]> defaultFunction;
   private final Long2ObjectMap<int[]> map;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private int[] lastChunk = null;

   public Position2dToIntMap() {
      this(0);
   }

   public Position2dToIntMap(int defaultValue) {
      this(defaultValue, new Long2ObjectOpenHashMap());
   }

   public Position2dToIntMap(int defaultValue, Long2ObjectMap<int[]> map) {
      this.defaultValue = defaultValue;
      this.map = map;
      if (defaultValue == 0) {
         this.defaultFunction = k -> new int[256];
      } else {
         this.defaultFunction = k -> {
            int[] array = new int[256];
            Arrays.fill(array, defaultValue);
            return array;
         };
      }
   }

   public void clear() {
      this.map.clear();
      this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
      this.lastChunk = null;
   }

   public Position2dToIntMap copy() {
      Position2dToIntMap copy = new Position2dToIntMap(this.defaultValue);
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<int[]> entry = (Entry<int[]>)var2.next();
         int[] array = (int[])entry.getValue();
         copy.map.put(entry.getLongKey(), Arrays.copyOf(array, array.length));
      }

      return copy;
   }

   @Deprecated
   public Long2ObjectMap<int[]> unsafeGetMap() {
      return this.map;
   }

   public int get(int x, int z) {
      int xC = x >> 4;
      int zC = z >> 4;
      int[] array = this.getChunk(xC, zC);
      return array == null ? this.defaultValue : array[(x & 15) + (z & 15) * 16];
   }

   public void put(int x, int z, int v) {
      int xC = x >> 4;
      int zC = z >> 4;
      int[] array = this.getOrCreateChunk(xC, zC);
      array[(x & 15) + (z & 15) * 16] = v;
   }

   public int add(int x, int z, int v) {
      if (v == 0) {
         return this.get(x, z);
      } else {
         int xC = x >> 4;
         int zC = z >> 4;
         int[] array = this.getOrCreateChunk(xC, zC);
         return array[(x & 15) + (z & 15) * 16] = array[(x & 15) + (z & 15) * 16] + v;
      }
   }

   public int binaryAnd(int x, int z, int v) {
      int xC = x >> 4;
      int zC = z >> 4;
      int[] array = this.getOrCreateChunk(xC, zC);
      return array[(x & 15) + (z & 15) * 16] = array[(x & 15) + (z & 15) * 16] & v;
   }

   public boolean min(int x, int z, int v) {
      int xC = x >> 4;
      int zC = z >> 4;
      int[] array = this.getOrCreateChunk(xC, zC);
      int index = (x & 15) + (z & 15) * 16;
      if (v < array[index]) {
         array[index] = v;
         return true;
      } else {
         return false;
      }
   }

   public boolean max(int x, int z, int v) {
      int xC = x >> 4;
      int zC = z >> 4;
      int[] array = this.getOrCreateChunk(xC, zC);
      int index = (x & 15) + (z & 15) * 16;
      if (v > array[index]) {
         array[index] = v;
         return true;
      } else {
         return false;
      }
   }

   public void forEachEntry(TriIntConsumer consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<int[]> entry = (Entry<int[]>)var2.next();
         int cx = ChunkPos.getX(entry.getLongKey()) * 16;
         int cz = ChunkPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               int v = ((int[])entry.getValue())[index++];
               if (v != this.defaultValue) {
                  consumer.accept(cx + x, cz + z, v);
               }
            }
         }
      }
   }

   public boolean removeIf(TriIntPredicate predicate) {
      boolean modified = false;
      ObjectIterator var3 = this.map.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<int[]> entry = (Entry<int[]>)var3.next();
         int cx = ChunkPos.getX(entry.getLongKey()) * 16;
         int cz = ChunkPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               int[] array = (int[])entry.getValue();
               int v = array[index];
               if (v != this.defaultValue && predicate.test(cx + x, cz + z, v)) {
                  array[index] = this.defaultValue;
                  modified = true;
               }

               index++;
            }
         }
      }

      return modified;
   }

   public ChunkPos getLastChunk() {
      if (this.lastChunk != null && this.lastChunkPos != PositionUtils.MIN_POSITION_LONG) {
         return new ChunkPos(this.lastChunkPos);
      } else {
         return !this.map.isEmpty() ? new ChunkPos(this.map.keySet().iterator().nextLong()) : new ChunkPos(0, 0);
      }
   }

   public int[] getChunk(int xC, int zC) {
      return this.getChunk(ChunkPos.asLong(xC, zC));
   }

   public int[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         int[] chunk = (int[])this.map.get(pos);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public int[] getOrCreateChunk(int xC, int zC) {
      return this.getOrCreateChunk(ChunkPos.asLong(xC, zC));
   }

   public int[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         int[] chunk = (int[])this.map.computeIfAbsent(pos, this.defaultFunction);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }
}
