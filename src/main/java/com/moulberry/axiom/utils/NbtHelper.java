package com.moulberry.axiom.utils;

import com.moulberry.axiom.VersionUtilsNbt;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class NbtHelper {
   public static ListTag getList(CompoundTag tag, String key, int type) {
      return tag.getList(key, type);
   }

   public static void putUUID(CompoundTag tag, String key, UUID uuid) {
      tag.putIntArray(key, UUIDUtil.uuidToIntArray(uuid));
   }

   public static UUID getUUID(CompoundTag tag, String key) {
      return UUIDUtil.uuidFromIntArray(VersionUtilsNbt.helperCompoundTagGetIntArray(tag, key).orElse(new int[]{0, 0, 0, 0}));
   }
}
