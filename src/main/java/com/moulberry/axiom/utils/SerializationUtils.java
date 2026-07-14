package com.moulberry.axiom.utils;

import com.google.gson.Gson;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SerializationUtils {
   private static final Gson GSON = new Gson();

   public static String componentToJson(Component component, RegistryAccess registryAccess) {
      return Serializer.toJson(component, registryAccess);
   }

   public static Component jsonToComponent(String json, RegistryAccess registryAccess) {
      return Serializer.fromJson(json, registryAccess);
   }

   public static void loadBlockEntity(BlockEntity blockEntity, CompoundTag compoundTag, RegistryAccess registryAccess) {
      blockEntity.loadWithComponents(compoundTag, registryAccess);
   }
}
