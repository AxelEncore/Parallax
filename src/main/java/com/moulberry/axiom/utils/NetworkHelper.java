package com.moulberry.axiom.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkHelper {
   public static ItemStack readItemStack(FriendlyByteBuf friendlyByteBuf) {
      return (ItemStack)ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf)friendlyByteBuf);
   }

   public static void writeItemStack(FriendlyByteBuf friendlyByteBuf, ItemStack itemStack) {
      ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf)friendlyByteBuf, itemStack);
   }

   public static BlockState readBlockStateByIdOrString(FriendlyByteBuf friendlyByteBuf) {
      int id = friendlyByteBuf.readVarInt();
      if (id != -1) {
         return Block.stateById(id);
      } else {
         String asString = friendlyByteBuf.readUtf();
         return BlockHelper.readVanillaStateByString(asString);
      }
   }

   public static BlockState readBlockState(FriendlyByteBuf friendlyByteBuf) {
      return (BlockState)friendlyByteBuf.readById(Block.BLOCK_STATE_REGISTRY::byId);
   }

   public static void writeBlockState(FriendlyByteBuf friendlyByteBuf, BlockState blockState) {
      friendlyByteBuf.writeById(Block.BLOCK_STATE_REGISTRY::getIdOrThrow, blockState);
   }

   public static Block readBlock(FriendlyByteBuf friendlyByteBuf) {
      return (Block)friendlyByteBuf.readById(BuiltInRegistries.BLOCK::byId);
   }

   public static void writeBlock(FriendlyByteBuf friendlyByteBuf, Block block) {
      friendlyByteBuf.writeById(BuiltInRegistries.BLOCK::getIdOrThrow, block);
   }

   public static Item readItem(FriendlyByteBuf friendlyByteBuf) {
      return (Item)friendlyByteBuf.readById(BuiltInRegistries.ITEM::byId);
   }

   public static void writeItem(FriendlyByteBuf friendlyByteBuf, Item item) {
      friendlyByteBuf.writeById(BuiltInRegistries.ITEM::getIdOrThrow, item);
   }
}
