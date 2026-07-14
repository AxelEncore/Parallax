package com.moulberry.axiom.collections;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class PositionSet {
   private final Long2ObjectMap<short[]> map;
   private int count = 0;
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private short[] lastChunk = null;

   public PositionSet() {
      this.map = new Long2ObjectOpenHashMap();
      this.count = 0;
   }

   private PositionSet(Long2ObjectMap<short[]> map, int count) {
      this.map = map;
      this.count = count;
   }

   public int chunkCount() {
      return this.map.size();
   }

   public int count() {
      return this.count;
   }

   @Deprecated
   public void unsafeSetCount(int count) {
      this.count = count;
   }

   public void validateCount() {
      int actualCount = 0;
      ObjectIterator var2 = this.map.values().iterator();

      while (var2.hasNext()) {
         short[] chunk = (short[])var2.next();

         for (short value : chunk) {
            actualCount += Integer.bitCount(value & '\uffff');
         }
      }

      if (actualCount != this.count) {
         throw new FaultyImplementationError("The count of this PositionSet is incorrect");
      }
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

   public PositionSet copy() {
      PositionSet copy = new PositionSet();
      copy.count = this.count;
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var2.next();
         short[] array = (short[])entry.getValue();
         copy.map.put(entry.getLongKey(), Arrays.copyOf(array, array.length));
      }

      return copy;
   }

   public boolean contains(int x, int y, int z) {
      if (this.count == 0) {
         return false;
      } else {
         int xC = x >> 4;
         int yC = y >> 4;
         int zC = z >> 4;
         short[] array = this.getChunk(xC, yC, zC);
         if (array == null) {
            return false;
         } else {
            int offset = (y & 15) + (z & 15) * 16;
            int bit = 1 << (x & 15);
            return (array[offset] & bit) != 0;
         }
      }
   }

   public boolean containsInXRow(int x, int y, int z) {
      if (this.count == 0) {
         return false;
      } else {
         int xC = x >> 4;
         int yC = y >> 4;
         int zC = z >> 4;
         short[] array = this.getChunk(xC, yC, zC);
         if (array == null) {
            return false;
         } else {
            int offset = (y & 15) + (z & 15) * 16;
            return array[offset] != 0;
         }
      }
   }

   public boolean add(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      short[] array = this.getOrCreateChunk(xC, yC, zC);
      int offset = (y & 15) + (z & 15) * 16;
      int bit = 1 << (x & 15);
      if ((array[offset] & bit) == 0) {
         array[offset] = (short)(array[offset] | bit);
         this.count++;
         return true;
      } else {
         return false;
      }
   }

   public boolean remove(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      short[] array = this.getChunk(xC, yC, zC);
      if (array == null) {
         return false;
      } else {
         int offset = (y & 15) + (z & 15) * 16;
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

   public void forEach(TriIntConsumer consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey()) * 16;
         int cy = BlockPos.getY(entry.getLongKey()) * 16;
         int cz = BlockPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               short v = ((short[])entry.getValue())[index++];
               if (v == -1) {
                  consumer.accept(cx + 0, cy + y, cz + z);
                  consumer.accept(cx + 1, cy + y, cz + z);
                  consumer.accept(cx + 2, cy + y, cz + z);
                  consumer.accept(cx + 3, cy + y, cz + z);
                  consumer.accept(cx + 4, cy + y, cz + z);
                  consumer.accept(cx + 5, cy + y, cz + z);
                  consumer.accept(cx + 6, cy + y, cz + z);
                  consumer.accept(cx + 7, cy + y, cz + z);
                  consumer.accept(cx + 8, cy + y, cz + z);
                  consumer.accept(cx + 9, cy + y, cz + z);
                  consumer.accept(cx + 10, cy + y, cz + z);
                  consumer.accept(cx + 11, cy + y, cz + z);
                  consumer.accept(cx + 12, cy + y, cz + z);
                  consumer.accept(cx + 13, cy + y, cz + z);
                  consumer.accept(cx + 14, cy + y, cz + z);
                  consumer.accept(cx + 15, cy + y, cz + z);
               } else if (v != 0) {
                  if ((v & 1) != 0) {
                     consumer.accept(cx + 0, cy + y, cz + z);
                  }

                  if ((v & 2) != 0) {
                     consumer.accept(cx + 1, cy + y, cz + z);
                  }

                  if ((v & 4) != 0) {
                     consumer.accept(cx + 2, cy + y, cz + z);
                  }

                  if ((v & 8) != 0) {
                     consumer.accept(cx + 3, cy + y, cz + z);
                  }

                  if ((v & 16) != 0) {
                     consumer.accept(cx + 4, cy + y, cz + z);
                  }

                  if ((v & 32) != 0) {
                     consumer.accept(cx + 5, cy + y, cz + z);
                  }

                  if ((v & 64) != 0) {
                     consumer.accept(cx + 6, cy + y, cz + z);
                  }

                  if ((v & 128) != 0) {
                     consumer.accept(cx + 7, cy + y, cz + z);
                  }

                  if ((v & 256) != 0) {
                     consumer.accept(cx + 8, cy + y, cz + z);
                  }

                  if ((v & 512) != 0) {
                     consumer.accept(cx + 9, cy + y, cz + z);
                  }

                  if ((v & 1024) != 0) {
                     consumer.accept(cx + 10, cy + y, cz + z);
                  }

                  if ((v & 2048) != 0) {
                     consumer.accept(cx + 11, cy + y, cz + z);
                  }

                  if ((v & 4096) != 0) {
                     consumer.accept(cx + 12, cy + y, cz + z);
                  }

                  if ((v & 8192) != 0) {
                     consumer.accept(cx + 13, cy + y, cz + z);
                  }

                  if ((v & 16384) != 0) {
                     consumer.accept(cx + 14, cy + y, cz + z);
                  }

                  if ((v & '耀') != 0) {
                     consumer.accept(cx + 15, cy + y, cz + z);
                  }
               }
            }
         }
      }
   }

   public void forEachChunk(PositionSet.ChunkConsumer consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey());
         int cy = BlockPos.getY(entry.getLongKey());
         int cz = BlockPos.getZ(entry.getLongKey());
         consumer.accept(cx, cy, cz, (short[])entry.getValue());
      }
   }

   public LongSet calculateChunksChanged(PositionSet other) {
      LongSet changed = new LongOpenHashSet();
      ObjectIterator var3 = this.map.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var3.next();
         short[] otherArray = other.getChunk(entry.getLongKey());
         short[] thisArray = (short[])entry.getValue();
         if (!Arrays.equals(otherArray, thisArray)) {
            changed.add(entry.getLongKey());
         }
      }

      return changed;
   }

   public void write(FriendlyByteBuf buf) {
      buf.writeVarInt(this.map.size());
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<short[]> entry = (Entry<short[]>)var2.next();
         buf.writeLong(entry.getLongKey());
         short[] array = (short[])entry.getValue();
         if (array.length != 256) {
            throw new FaultyImplementationError();
         }

         for (short s : array) {
            buf.writeShort(s);
         }
      }
   }

   public static PositionSet read(FriendlyByteBuf buf) {
      int size = buf.readVarInt();
      Long2ObjectMap<short[]> map = new Long2ObjectOpenHashMap(Math.min(256, size));
      int count = 0;

      for (int i = 0; i < size; i++) {
         long pos = buf.readLong();
         short[] array = new short[256];

         for (int j = 0; j < 256; j++) {
            short s = buf.readShort();
            count += Integer.bitCount(s & '\uffff');
            array[j] = s;
         }

         map.put(pos, array);
      }

      return new PositionSet(map, count);
   }

   public LongSet chunkKeySet() {
      return this.map.keySet();
   }

   @Deprecated
   public Long2ObjectMap<short[]> unsafeGetRawMap() {
      return this.map;
   }

   public void removeEmptyChunk(long pos) {
      if (this.lastChunkPos == pos) {
         this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
         this.lastChunk = null;
      }

      short[] array = (short[])this.map.remove(pos);
      if (BuildConfig.DEBUG) {
         for (short s : array) {
            if (s != 0) {
               throw new FaultyImplementationError("Chunk is not empty");
            }
         }
      }
   }

   public int removeChunk(long pos) {
      if (this.lastChunkPos == pos) {
         this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
         this.lastChunk = null;
      }

      short[] array = (short[])this.map.remove(pos);
      if (array == null) {
         return 0;
      } else {
         int removedCount = 0;

         for (short s : array) {
            removedCount += Integer.bitCount(s & '\uffff');
         }

         this.count -= removedCount;
         return removedCount;
      }
   }

   public short[] getChunk(int xC, int yC, int zC) {
      return this.getChunk(BlockPos.asLong(xC, yC, zC));
   }

   public short[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         short[] chunk = (short[])this.map.get(pos);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public short[] getOrCreateChunk(int xC, int yC, int zC) {
      return this.getOrCreateChunk(BlockPos.asLong(xC, yC, zC));
   }

   public short[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         short[] chunk = (short[])this.map.computeIfAbsent(pos, k -> new short[256]);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public interface ChunkConsumer {
      void accept(int var1, int var2, int var3, short[] var4);
   }
}
