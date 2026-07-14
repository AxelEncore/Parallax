package com.moulberry.axiom.collections;

import com.moulberry.axiom.funcinterfaces.IntIntFloatConsumer;
import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.function.LongFunction;
import net.minecraft.world.level.ChunkPos;

public class Position2dToFloatMap {
   private final float defaultValue;
   private final LongFunction<float[]> defaultFunction;
   private final Long2ObjectMap<float[]> map;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private float[] lastChunk = null;

   public Position2dToFloatMap() {
      this(0.0F);
   }

   public Position2dToFloatMap(float defaultValue) {
      this(defaultValue, new Long2ObjectOpenHashMap());
   }

   public Position2dToFloatMap(float defaultValue, Long2ObjectMap<float[]> map) {
      this.defaultValue = defaultValue;
      this.map = map;
      if (defaultValue == 0.0F) {
         this.defaultFunction = k -> new float[256];
      } else {
         this.defaultFunction = k -> {
            float[] array = new float[256];
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

   public Position2dToFloatMap copy() {
      Position2dToFloatMap copy = new Position2dToFloatMap(this.defaultValue);
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<float[]> entry = (Entry<float[]>)var2.next();
         float[] array = (float[])entry.getValue();
         copy.map.put(entry.getLongKey(), Arrays.copyOf(array, array.length));
      }

      return copy;
   }

   @Deprecated
   public Long2ObjectMap<float[]> unsafeGetMap() {
      return this.map;
   }

   public float get(int x, int z) {
      int xC = x >> 4;
      int zC = z >> 4;
      float[] array = this.getChunk(xC, zC);
      return array == null ? this.defaultValue : array[(x & 15) + (z & 15) * 16];
   }

   public void put(int x, int z, float v) {
      int xC = x >> 4;
      int zC = z >> 4;
      float[] array = this.getOrCreateChunk(xC, zC);
      array[(x & 15) + (z & 15) * 16] = v;
   }

   public float add(int x, int z, float v) {
      if (v == 0.0F) {
         return this.get(x, z);
      } else {
         int xC = x >> 4;
         int zC = z >> 4;
         float[] array = this.getOrCreateChunk(xC, zC);
         return array[(x & 15) + (z & 15) * 16] = array[(x & 15) + (z & 15) * 16] + v;
      }
   }

   public boolean min(int x, int z, float v) {
      int xC = x >> 4;
      int zC = z >> 4;
      float[] array = this.getOrCreateChunk(xC, zC);
      int index = (x & 15) + (z & 15) * 16;
      if (v < array[index]) {
         array[index] = v;
         return true;
      } else {
         return false;
      }
   }

   public boolean max(int x, int z, float v) {
      int xC = x >> 4;
      int zC = z >> 4;
      float[] array = this.getOrCreateChunk(xC, zC);
      int index = (x & 15) + (z & 15) * 16;
      if (v > array[index]) {
         array[index] = v;
         return true;
      } else {
         return false;
      }
   }

   public void forEachEntry(IntIntFloatConsumer consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<float[]> entry = (Entry<float[]>)var2.next();
         int cx = ChunkPos.getX(entry.getLongKey()) * 16;
         int cz = ChunkPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               float v = ((float[])entry.getValue())[index++];
               if (v != this.defaultValue) {
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

   public float[] getChunk(int xC, int zC) {
      return this.getChunk(ChunkPos.asLong(xC, zC));
   }

   public float[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         float[] chunk = (float[])this.map.get(pos);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public float[] getOrCreateChunk(int xC, int zC) {
      return this.getOrCreateChunk(ChunkPos.asLong(xC, zC));
   }

   public float[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         float[] chunk = (float[])this.map.computeIfAbsent(pos, this.defaultFunction);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }
}
