package com.moulberry.axiom.world_properties;

import com.moulberry.axiom.utils.NetworkHelper;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public abstract class WorldPropertyDataType<T> {
   public static WorldPropertyDataType<Boolean> BOOLEAN = new WorldPropertyDataType<Boolean>() {
      @Override
      public int getTypeId() {
         return 0;
      }

      public byte[] serialize(Boolean value) {
         return new byte[]{(byte)(value ? 1 : 0)};
      }

      public Boolean deserialize(byte[] bytes) {
         return bytes[0] != 0;
      }
   };
   public static WorldPropertyDataType<Integer> INTEGER = new WorldPropertyDataType<Integer>() {
      @Override
      public int getTypeId() {
         return 1;
      }

      public byte[] serialize(Integer value) {
         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(8));
         buf.writeVarInt(value);
         byte[] bytes = new byte[buf.writerIndex()];
         buf.getBytes(0, bytes);
         return bytes;
      }

      public Integer deserialize(byte[] bytes) {
         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
         return buf.readVarInt();
      }
   };
   public static WorldPropertyDataType<String> STRING = new WorldPropertyDataType<String>() {
      @Override
      public int getTypeId() {
         return 2;
      }

      public byte[] serialize(String value) {
         return value.getBytes(StandardCharsets.UTF_8);
      }

      public String deserialize(byte[] bytes) {
         return new String(bytes, StandardCharsets.UTF_8);
      }
   };
   public static WorldPropertyDataType<Item> ITEM = new WorldPropertyDataType<Item>() {
      @Override
      public int getTypeId() {
         return 3;
      }

      public byte[] serialize(Item value) {
         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(8));
         NetworkHelper.writeItem(buf, value);
         byte[] bytes = new byte[buf.writerIndex()];
         buf.getBytes(0, bytes);
         return bytes;
      }

      public Item deserialize(byte[] bytes) {
         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
         return NetworkHelper.readItem(buf);
      }
   };
   public static WorldPropertyDataType<Block> BLOCK = new WorldPropertyDataType<Block>() {
      @Override
      public int getTypeId() {
         return 4;
      }

      public byte[] serialize(Block value) {
         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(8));
         NetworkHelper.writeBlock(buf, value);
         byte[] bytes = new byte[buf.writerIndex()];
         buf.getBytes(0, bytes);
         return bytes;
      }

      public Block deserialize(byte[] bytes) {
         FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
         return NetworkHelper.readBlock(buf);
      }
   };
   public static WorldPropertyDataType<Unit> EMPTY = new WorldPropertyDataType<Unit>() {
      @Override
      public int getTypeId() {
         return 5;
      }

      public byte[] serialize(Unit value) {
         return new byte[0];
      }

      public Unit deserialize(byte[] bytes) {
         return Unit.INSTANCE;
      }
   };

   public abstract int getTypeId();

   public abstract byte[] serialize(T var1);

   public abstract T deserialize(byte[] var1);
}
