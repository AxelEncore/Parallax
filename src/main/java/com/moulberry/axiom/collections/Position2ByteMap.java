package com.moulberry.axiom.collections;

import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Arrays;
import java.util.function.LongFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class Position2ByteMap {
   private final byte defaultValue;
   private final LongFunction<byte[]> defaultFunction;
   public final Long2ObjectMap<byte[]> map = new Long2ObjectOpenHashMap();
   private long lastChunkPos = PositionUtils.MIN_POSITION_LONG;
   private byte[] lastChunk = null;

   public Position2ByteMap() {
      this((byte)0);
   }

   public Position2ByteMap(byte defaultValue) {
      this.defaultValue = defaultValue;
      if (defaultValue == 0) {
         this.defaultFunction = k -> new byte[4096];
      } else {
         this.defaultFunction = k -> {
            byte[] array = new byte[4096];
            Arrays.fill(array, defaultValue);
            return array;
         };
      }
   }

   public Position2ByteMap(byte defaultValue, LongFunction<byte[]> defaultFunction) {
      this.defaultValue = defaultValue;
      this.defaultFunction = defaultFunction;
   }

   public byte getDefaultValue() {
      return this.defaultValue;
   }

   public void save(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeByte(this.defaultValue);
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<byte[]> entry = (Entry<byte[]>)var2.next();
         friendlyByteBuf.writeLong(entry.getLongKey());
         friendlyByteBuf.writeBytes((byte[])entry.getValue());
      }

      friendlyByteBuf.writeLong(PositionUtils.MIN_POSITION_LONG);
   }

   public static Position2ByteMap load(FriendlyByteBuf friendlyByteBuf) {
      Position2ByteMap map = new Position2ByteMap(friendlyByteBuf.readByte());

      while (true) {
         long pos = friendlyByteBuf.readLong();
         if (pos == PositionUtils.MIN_POSITION_LONG) {
            return map;
         }

         byte[] bytes = new byte[4096];
         friendlyByteBuf.readBytes(bytes);
         map.map.put(pos, bytes);
      }
   }

   public void clear() {
      this.map.clear();
      this.lastChunkPos = PositionUtils.MIN_POSITION_LONG;
      this.lastChunk = null;
   }

   public byte getOrCreate(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      byte[] array = this.getOrCreateChunk(xC, yC, zC);
      return array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16];
   }

   public byte get(int x, int y, int z) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      byte[] array = this.getChunk(xC, yC, zC);
      return array == null ? this.defaultValue : array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16];
   }

   public void put(int x, int y, int z, byte v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      byte[] array = this.getOrCreateChunk(xC, yC, zC);
      array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] = v;
   }

   public byte add(int x, int y, int z, byte v) {
      if (v == 0) {
         return this.get(x, y, z);
      } else {
         int xC = x >> 4;
         int yC = y >> 4;
         int zC = z >> 4;
         byte[] array = this.getOrCreateChunk(xC, yC, zC);
         return array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] = (byte)(array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] + v);
      }
   }

   public byte binaryAnd(int x, int y, int z, byte v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      byte[] array = this.getOrCreateChunk(xC, yC, zC);
      return array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] = (byte)(array[(x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16] & v);
   }

   public boolean min(int x, int y, int z, byte v) {
      int xC = x >> 4;
      int yC = y >> 4;
      int zC = z >> 4;
      byte[] array = this.getOrCreateChunk(xC, yC, zC);
      int index = (x & 15) + (y & 15) * 16 + (z & 15) * 16 * 16;
      if (v < array[index]) {
         array[index] = v;
         return true;
      } else {
         return false;
      }
   }

   public void forEachEntry(Position2ByteMap.EntryConsumer consumer) {
      ObjectIterator var2 = this.map.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<byte[]> entry = (Entry<byte[]>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey()) * 16;
         int cy = BlockPos.getY(entry.getLongKey()) * 16;
         int cz = BlockPos.getZ(entry.getLongKey()) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  byte v = ((byte[])entry.getValue())[index++];
                  if (v != this.defaultValue) {
                     consumer.consume(cx + x, cy + y, cz + z, v);
                  }
               }
            }
         }
      }
   }

   public byte[] getChunk(int xC, int yC, int zC) {
      return this.getChunk(BlockPos.asLong(xC, yC, zC));
   }

   public byte[] getChunk(long pos) {
      if (this.lastChunkPos != pos) {
         byte[] chunk = (byte[])this.map.get(pos);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   public byte[] getOrCreateChunk(int xC, int yC, int zC) {
      return this.getOrCreateChunk(BlockPos.asLong(xC, yC, zC));
   }

   public byte[] getOrCreateChunk(long pos) {
      if (this.lastChunk == null || this.lastChunkPos != pos) {
         byte[] chunk = (byte[])this.map.computeIfAbsent(pos, this.defaultFunction);
         this.lastChunkPos = pos;
         this.lastChunk = chunk;
      }

      return this.lastChunk;
   }

   @FunctionalInterface
   public interface EntryConsumer {
      void consume(int var1, int var2, int var3, byte var4);
   }
}
