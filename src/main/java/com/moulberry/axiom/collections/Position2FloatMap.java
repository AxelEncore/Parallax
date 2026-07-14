package com.moulberry.axiom.collections;

import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.function.LongFunction;
import net.minecraft.core.BlockPos;

public class Position2FloatMap {
   private final float defaultValue;
   private final LongFunction<float[]> defaultFunction;
   private final Long2ObjectMap<float[]> map = new Long2ObjectOpenHashMap();
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private float[] lastChunk = null;

   public Position2FloatMap() {
      this(0.0F);
   }

   public Position2FloatMap(float defaultValue) {
      this.defaultValue = defaultValue;
      if (defaultValue == 0.0F) {
         this.defaultFunction = k -> new float[4096];
      } else {
         this.defaultFunction = k -> {
            float[] array = new float[4096];
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

   public float get(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      float[] array = this.getChunk(xC, yC, zC);
      return array == null ? this.defaultValue : array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16];
   }

   public void put(int x, int y, int z, float v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      float[] array = this.getOrCreateChunk(xC, yC, zC);
      array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] = v;
   }

   public float add(int x, int y, int z, float v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      float[] array = this.getOrCreateChunk(xC, yC, zC);
      return array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] = array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] + v;
   }

   public boolean min(int x, int y, int z, float v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      float[] array = this.getOrCreateChunk(xC, yC, zC);
      int index = (x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16;
      if (v < array[index]) {
         array[index] = v;
         return true;
      } else {
         return false;
      }
   }

   public boolean max(int x, int y, int z, float v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      float[] array = this.getOrCreateChunk(xC, yC, zC);
      int index = (x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16;
      if (v > array[index]) {
         array[index] = v;
         return true;
      } else {
         return false;
      }
   }

   @Deprecated
   public void addAll(Position2FloatMap other) {
      ObjectIterator var2 = other.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<float[]> entry = (Entry<float[]>)var2.next();
         long pos = entry.getLongKey();
         float[] otherArray = (float[])entry.getValue();
         float[] thisArray = (float[])this.map.get(pos);
         if (thisArray == null) {
            this.map.put(pos, otherArray);
         } else {
            for (int i = 0; i < 4096; i++) {
               thisArray[i] += otherArray[i];
            }
         }
      }
   }

   public void forEachEntry(Position2FloatMap.EntryConsumer consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<float[]> entry = (Entry<float[]>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey()) * 16;
         int cy = BlockPos.getY(entry.getLongKey()) * 16;
         int cz = BlockPos.getZ(entry.getLongKey()) * 16;
         float[] array = (float[])entry.getValue();
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  float v = array[index++];
                  if (v != this.defaultValue) {
                     consumer.consume(cx + x, cy + y, cz + z, v);
                  }
               }
            }
         }
      }
   }

   public void forEachEntryUnary(Position2FloatMap.EntryUnary unary) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<float[]> entry = (Entry<float[]>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey()) * 16;
         int cy = BlockPos.getY(entry.getLongKey()) * 16;
         int cz = BlockPos.getZ(entry.getLongKey()) * 16;
         float[] array = (float[])entry.getValue();
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  float v = array[index];
                  if (v != this.defaultValue) {
                     array[index] = unary.apply(cx + x, cy + y, cz + z, v);
                  }

                  index++;
               }
            }
         }
      }
   }

   @Deprecated
   public Long2ObjectMap<float[]> unsafeGetRawMap() {
      return this.map;
   }

   public float[] getChunk(int xC, int yC, int zC) {
      return this.getChunk(BlockPos.asLong(xC, yC, zC));
   }

   public float[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         float[] chunk = (float[])this.map.get(pos);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public float[] getOrCreateChunk(int xC, int yC, int zC) {
      return this.getOrCreateChunk(BlockPos.asLong(xC, yC, zC));
   }

   public float[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         float[] chunk = (float[])this.map.computeIfAbsent(pos, this.defaultFunction);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   @FunctionalInterface
   public interface EntryConsumer {
      void consume(int var1, int var2, int var3, float var4);
   }

   @FunctionalInterface
   public interface EntryUnary {
      float apply(int var1, int var2, int var3, float var4);
   }
}
