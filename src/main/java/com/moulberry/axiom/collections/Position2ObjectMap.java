package com.moulberry.axiom.collections;

import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.function.LongFunction;
import net.minecraft.core.BlockPos;

public class Position2ObjectMap<T> {
   private final LongFunction<T[]> defaultChunkSupplier;
   private final Long2ObjectMap<T[]> map = new Long2ObjectOpenHashMap();
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private T[] lastChunk = null;

   public Position2ObjectMap(LongFunction<T[]> defaultChunkSupplier) {
      this.defaultChunkSupplier = defaultChunkSupplier;
   }

   public void clear() {
      this.map.clear();
      this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
      this.lastChunk = null;
   }

   public T get(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      T[] array = this.getChunk(xC, yC, zC);
      return array == null ? null : array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16];
   }

   public T getOrCreate(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      T[] array = this.getOrCreateChunk(xC, yC, zC);
      return array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16];
   }

   public void put(int x, int y, int z, T v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      T[] array = this.getOrCreateChunk(xC, yC, zC);
      array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] = v;
   }

   public T getAndPut(int x, int y, int z, T v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      T[] array = this.getOrCreateChunk(xC, yC, zC);
      int index = (x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16;
      T old = array[index];
      array[index] = v;
      return old;
   }

   public void forEachChunk(PositionConsumer<T[]> consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<T[]> entry = (Entry<T[]>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey());
         int cy = BlockPos.getY(entry.getLongKey());
         int cz = BlockPos.getZ(entry.getLongKey());
         consumer.accept(cx, cy, cz, (T[])entry.getValue());
      }
   }

   public void forEachEntry(PositionConsumer<T> consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<T[]> entry = (Entry<T[]>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey()) * 16;
         int cy = BlockPos.getY(entry.getLongKey()) * 16;
         int cz = BlockPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  T t = (T)entry.getValue()[index++];
                  if (t != null) {
                     consumer.accept(cx + x, cy + y, cz + z, t);
                  }
               }
            }
         }
      }
   }

   public void mergeAllFrom(Position2ObjectMap<T> other, LongSet keys) {
      keys.forEach(value -> {
         T[] currentChunk = (T[])((Object[])this.map.computeIfAbsent(value, this.defaultChunkSupplier));
         T[] otherChunk = other.getChunk(value);
         if (otherChunk != null) {
            for (int i = 0; i < otherChunk.length; i++) {
               T t = otherChunk[i];
               if (t != null) {
                  currentChunk[i] = t;
               }
            }
         }
      });
   }

   public Position2ObjectMap<T> copy() {
      Position2ObjectMap<T> newMap = new Position2ObjectMap<>(this.defaultChunkSupplier);
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<T[]> entry = (Entry<T[]>)var2.next();
         newMap.putChunk(entry.getLongKey(), (T[])Arrays.copyOf((T[])((Object[])entry.getValue()), ((Object[])entry.getValue()).length));
      }

      return newMap;
   }

   public LongSet calculateChunksChanged(Position2ObjectMap<T> other) {
      LongSet changed = new LongOpenHashSet();
      ObjectIterator var3 = this.map.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<T[]> entry = (Entry<T[]>)var3.next();
         T[] otherArray = other.getChunk(entry.getLongKey());
         T[] thisArray = (T[])((Object[])entry.getValue());
         if (!Arrays.equals(otherArray, thisArray)) {
            changed.add(entry.getLongKey());
         }
      }

      return changed;
   }

   public LongSet chunkKeySet() {
      return this.map.keySet();
   }

   public void putChunk(int xC, int yC, int zC, T[] array) {
      this.putChunk(BlockPos.asLong(xC, yC, zC), array);
   }

   public void putChunk(long pos, T[] array) {
      this.map.put(pos, array);
      if (this.lastChunkPos == pos) {
         this.lastChunk = array;
      }
   }

   public T[] getChunk(int xC, int yC, int zC) {
      return this.getChunk(BlockPos.asLong(xC, yC, zC));
   }

   public T[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         T[] chunk = (T[])((Object[])this.map.get(pos));
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public T[] getOrCreateChunk(int xC, int yC, int zC) {
      return this.getOrCreateChunk(BlockPos.asLong(xC, yC, zC));
   }

   public T[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         T[] chunk = (T[])((Object[])this.map.computeIfAbsent(pos, this.defaultChunkSupplier));
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public T[] removeChunk(int xC, int yC, int zC) {
      return this.removeChunk(BlockPos.asLong(xC, yC, zC));
   }

   public T[] removeChunk(long pos) {
      T[] values = (T[])((Object[])this.map.remove(pos));
      if (this.lastChunkPos == pos) {
         this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
         this.lastChunk = null;
      }

      return values;
   }
}
