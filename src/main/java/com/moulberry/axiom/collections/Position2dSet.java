package com.moulberry.axiom.collections;

import com.moulberry.axiom.funcinterfaces.BiIntConsumer;
import com.moulberry.axiom.funcinterfaces.BiIntPredicate;
import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import net.minecraft.world.level.ChunkPos;

public class Position2dSet {
   private final Long2ObjectMap<short[]> map = new Long2ObjectOpenHashMap();
   private int count = 0;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private short[] lastChunk = null;

   public int count() {
      return this.count;
   }

   public boolean isEmpty() {
      return this.count == 0;
   }

   public void clear() {
      this.map.clear();
      this.count = 0;
      this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
      this.lastChunk = null;
   }

   public Position2dSet copy() {
      Position2dSet copy = new Position2dSet();
      copy.count = this.count;
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var2.next();
         short[] array = (short[])entry.getValue();
         copy.map.put(entry.getLongKey(), Arrays.copyOf(array, array.length));
      }

      return copy;
   }

   public boolean contains(int x, int z) {
      if (this.count == 0) {
         return false;
      } else {
         int xC = x >> 4;
         int zC = z >> 4;
         short[] array = this.getChunk(xC, zC);
         if (array == null) {
            return false;
         } else {
            int offset = z & 15;
            int bit = 1 << (x & 15);
            return (array[offset] & bit) != 0;
         }
      }
   }

   public boolean add(int x, int z) {
      int xC = x >> 4;
      int zC = z >> 4;
      short[] array = this.getOrCreateChunk(xC, zC);
      int offset = z & 15;
      int bit = 1 << (x & 15);
      if ((array[offset] & bit) == 0) {
         array[offset] = (short)(array[offset] | bit);
         this.count++;
         return true;
      } else {
         return false;
      }
   }

   public boolean remove(int x, int z) {
      int xC = x >> 4;
      int zC = z >> 4;
      short[] array = this.getChunk(xC, zC);
      if (array == null) {
         return false;
      } else {
         int offset = z & 15;
         int bit = 1 << (x & 15);
         if ((array[offset] & bit) != 0) {
            array[offset] = (short)(array[offset] & ~bit);
            this.count--;
            return true;
         } else {
            return false;
         }
      }
   }

   public void forEach(BiIntConsumer consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var2.next();
         int cx = ChunkPos.getX(entry.getLongKey()) * 16;
         int cz = ChunkPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            short v = ((short[])entry.getValue())[index++];

            for (int x = 0; x < 16; x++) {
               if ((v & 1 << x) != 0) {
                  consumer.accept(cx + x, cz + z);
               }
            }
         }
      }
   }

   public boolean removeIf(BiIntPredicate predicate) {
      int oldCount = this.count;
      ObjectIterator var3 = this.map.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var3.next();
         int cx = ChunkPos.getX(entry.getLongKey()) * 16;
         int cz = ChunkPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            short[] array = (short[])entry.getValue();
            short v = array[index];

            for (int x = 0; x < 16; x++) {
               int bit = 1 << x;
               if ((v & bit) != 0 && predicate.apply(cx + x, cz + z)) {
                  v = (short)(v & ~bit);
                  this.count--;
               }
            }

            array[index] = v;
            index++;
         }
      }

      return oldCount != this.count;
   }

   public short[] getChunk(int xC, int zC) {
      return this.getChunk(ChunkPos.asLong(xC, zC));
   }

   public short[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         short[] chunk = (short[])this.map.get(pos);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public short[] getOrCreateChunk(int xC, int zC) {
      return this.getOrCreateChunk(ChunkPos.asLong(xC, zC));
   }

   public short[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         short[] chunk = (short[])this.map.computeIfAbsent(pos, k -> new short[16]);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }
}
