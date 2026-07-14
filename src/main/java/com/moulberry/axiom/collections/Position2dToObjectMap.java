package com.moulberry.axiom.collections;

import com.moulberry.axiom.funcinterfaces.IntIntObjectConsumer;
import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.function.LongFunction;
import net.minecraft.world.level.ChunkPos;

public class Position2dToObjectMap<T> {
   private final LongFunction<T[]> defaultFunction;
   private final Long2ObjectMap<T[]> map;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private T[] lastChunk = null;

   public Position2dToObjectMap(LongFunction<T[]> defaultChunkSupplier) {
      this(new Long2ObjectOpenHashMap(), defaultChunkSupplier);
   }

   public Position2dToObjectMap(Long2ObjectMap<T[]> map, LongFunction<T[]> defaultChunkSupplier) {
      this.map = map;
      this.defaultFunction = defaultChunkSupplier;
   }

   public void clear() {
      this.map.clear();
      this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
      this.lastChunk = null;
   }

   public T get(int x, int z) {
      int xC = x >> 4;
      int zC = z >> 4;
      T[] array = this.getChunk(xC, zC);
      return array == null ? null : array[(x & 15) + (z & 15) * 16];
   }

   public void put(int x, int z, T v) {
      int xC = x >> 4;
      int zC = z >> 4;
      T[] array = this.getOrCreateChunk(xC, zC);
      array[(x & 15) + (z & 15) * 16] = v;
   }

   public void forEachEntry(IntIntObjectConsumer<T> consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<T[]> entry = (Entry<T[]>)var2.next();
         int cx = ChunkPos.getX(entry.getLongKey()) * 16;
         int cz = ChunkPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               T v = (T)entry.getValue()[index++];
               if (v != null) {
                  consumer.accept(cx + x, cz + z, v);
               }
            }
         }
      }
   }

   public ChunkPos getLastChunk() {
      if (this.lastChunk != null && this.lastChunkPos != PositionUtils.MIN_POSITION_LONG) {
         return new ChunkPos(this.lastChunkPos);
      } else {
         return !this.map.isEmpty() ? new ChunkPos(this.map.keySet().iterator().nextLong()) : new ChunkPos(0, 0);
      }
   }

   public T[] getChunk(int xC, int zC) {
      return this.getChunk(ChunkPos.asLong(xC, zC));
   }

   public T[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         T[] chunk = (T[])((Object[])this.map.get(pos));
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public T[] getOrCreateChunk(int xC, int zC) {
      return this.getOrCreateChunk(ChunkPos.asLong(xC, zC));
   }

   public T[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         T[] chunk = (T[])((Object[])this.map.computeIfAbsent(pos, this.defaultFunction));
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }
}
