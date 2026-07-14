package com.moulberry.axiom;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

public class VersionUtilsNbt {
   public static Optional<Byte> CompoundTag_getByte(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetByte(compoundTag, key);
   }

   public static Optional<Byte> helperCompoundTagGetByte(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 99) ? Optional.of(compoundTag.getByte(key)) : Optional.empty();
   }

   public static byte CompoundTag_getByteOr(CompoundTag compoundTag, String key, byte defaultValue) {
      return helperCompoundTagGetByteOr(compoundTag, key, defaultValue);
   }

   public static byte helperCompoundTagGetByteOr(CompoundTag compoundTag, String key, byte defaultValue) {
      return compoundTag.contains(key, 99) ? compoundTag.getByte(key) : defaultValue;
   }

   public static Optional<Short> CompoundTag_getShort(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetShort(compoundTag, key);
   }

   public static Optional<Short> helperCompoundTagGetShort(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 99) ? Optional.of(compoundTag.getShort(key)) : Optional.empty();
   }

   public static short CompoundTag_getShortOr(CompoundTag compoundTag, String key, short defaultValue) {
      return helperCompoundTagGetShortOr(compoundTag, key, defaultValue);
   }

   public static short helperCompoundTagGetShortOr(CompoundTag compoundTag, String key, short defaultValue) {
      return compoundTag.contains(key, 99) ? compoundTag.getShort(key) : defaultValue;
   }

   public static Optional<Integer> CompoundTag_getInt(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetInt(compoundTag, key);
   }

   public static Optional<Integer> helperCompoundTagGetInt(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 99) ? Optional.of(compoundTag.getInt(key)) : Optional.empty();
   }

   public static int CompoundTag_getIntOr(CompoundTag compoundTag, String key, int defaultValue) {
      return helperCompoundTagGetIntOr(compoundTag, key, defaultValue);
   }

   public static int helperCompoundTagGetIntOr(CompoundTag compoundTag, String key, int defaultValue) {
      return compoundTag.contains(key, 99) ? compoundTag.getInt(key) : defaultValue;
   }

   public static int ListTag_getIntOr(ListTag listTag, int index, int defaultValue) {
      return helperListTagGetIntOr(listTag, index, defaultValue);
   }

   public static int helperListTagGetIntOr(ListTag listTag, int index, int defaultValue) {
      return helperTagAsInt(listTag.get(index)).orElse(defaultValue);
   }

   public static Optional<Integer> Tag_asInt(Tag tag) {
      return helperTagAsInt(tag);
   }

   public static Optional<Integer> helperTagAsInt(Tag tag) {
      return tag instanceof NumericTag numericTag ? Optional.of(numericTag.getAsInt()) : Optional.empty();
   }

   public static Optional<Long> CompoundTag_getLong(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetLong(compoundTag, key);
   }

   public static Optional<Long> helperCompoundTagGetLong(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 99) ? Optional.of(compoundTag.getLong(key)) : Optional.empty();
   }

   public static long CompoundTag_getLongOr(CompoundTag compoundTag, String key, long defaultValue) {
      return helperCompoundTagGetLongOr(compoundTag, key, defaultValue);
   }

   public static long helperCompoundTagGetLongOr(CompoundTag compoundTag, String key, long defaultValue) {
      return compoundTag.contains(key, 99) ? compoundTag.getLong(key) : defaultValue;
   }

   public static Optional<Float> CompoundTag_getFloat(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetFloat(compoundTag, key);
   }

   public static Optional<Float> helperCompoundTagGetFloat(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 99) ? Optional.of(compoundTag.getFloat(key)) : Optional.empty();
   }

   public static float CompoundTag_getFloatOr(CompoundTag compoundTag, String key, float defaultValue) {
      return helperCompoundTagGetFloatOr(compoundTag, key, defaultValue);
   }

   public static float helperCompoundTagGetFloatOr(CompoundTag compoundTag, String key, float defaultValue) {
      return compoundTag.contains(key, 99) ? compoundTag.getFloat(key) : defaultValue;
   }

   public static float ListTag_getFloatOr(ListTag listTag, int index, float defaultValue) {
      return helperListTagGetFloatOr(listTag, index, defaultValue);
   }

   public static float helperListTagGetFloatOr(ListTag listTag, int index, float defaultValue) {
      return helperTagAsFloat(listTag.get(index)).orElse(defaultValue);
   }

   public static Optional<Float> Tag_asFloat(Tag tag) {
      return helperTagAsFloat(tag);
   }

   public static Optional<Float> helperTagAsFloat(Tag tag) {
      return tag instanceof NumericTag numericTag ? Optional.of(numericTag.getAsFloat()) : Optional.empty();
   }

   public static Optional<Double> CompoundTag_getDouble(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetDouble(compoundTag, key);
   }

   public static Optional<Double> helperCompoundTagGetDouble(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 99) ? Optional.of(compoundTag.getDouble(key)) : Optional.empty();
   }

   public static double CompoundTag_getDoubleOr(CompoundTag compoundTag, String key, double defaultValue) {
      return helperCompoundTagGetDoubleOr(compoundTag, key, defaultValue);
   }

   public static double helperCompoundTagGetDoubleOr(CompoundTag compoundTag, String key, double defaultValue) {
      return compoundTag.contains(key, 99) ? compoundTag.getDouble(key) : defaultValue;
   }

   public static double ListTag_getDoubleOr(ListTag listTag, int index, double defaultValue) {
      return helperListTagGetDoubleOr(listTag, index, defaultValue);
   }

   public static double helperListTagGetDoubleOr(ListTag listTag, int index, double defaultValue) {
      return helperTagAsDouble(listTag.get(index)).orElse(defaultValue);
   }

   public static Optional<Double> Tag_asDouble(Tag tag) {
      return helperTagAsDouble(tag);
   }

   public static Optional<Double> helperTagAsDouble(Tag tag) {
      return tag instanceof NumericTag numericTag ? Optional.of(numericTag.getAsDouble()) : Optional.empty();
   }

   public static Optional<String> CompoundTag_getString(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetString(compoundTag, key);
   }

   public static Optional<String> helperCompoundTagGetString(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 8) ? Optional.of(compoundTag.getString(key)) : Optional.empty();
   }

   public static String CompoundTag_getStringOr(CompoundTag compoundTag, String key, String defaultValue) {
      return helperCompoundTagGetStringOr(compoundTag, key, defaultValue);
   }

   public static String helperCompoundTagGetStringOr(CompoundTag compoundTag, String key, String defaultValue) {
      return compoundTag.contains(key, 8) ? compoundTag.getString(key) : defaultValue;
   }

   public static String ListTag_getStringOr(ListTag listTag, int index, String defaultValue) {
      return helperListTagGetStringOr(listTag, index, defaultValue);
   }

   public static String helperListTagGetStringOr(ListTag listTag, int index, String defaultValue) {
      return helperTagAsString(listTag.get(index)).orElse(defaultValue);
   }

   public static Optional<String> Tag_asString(Tag tag) {
      return helperTagAsString(tag);
   }

   public static Optional<String> helperTagAsString(Tag tag) {
      return tag.getId() == 8 ? Optional.of(tag.getAsString()) : Optional.empty();
   }

   public static Optional<byte[]> CompoundTag_getByteArray(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetByteArray(compoundTag, key);
   }

   public static Optional<byte[]> helperCompoundTagGetByteArray(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 7) ? Optional.of(compoundTag.getByteArray(key)) : Optional.empty();
   }

   public static Optional<int[]> CompoundTag_getIntArray(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetIntArray(compoundTag, key);
   }

   public static Optional<int[]> helperCompoundTagGetIntArray(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 11) ? Optional.of(compoundTag.getIntArray(key)) : Optional.empty();
   }

   public static Optional<long[]> CompoundTag_getLongArray(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetLongArray(compoundTag, key);
   }

   public static Optional<long[]> helperCompoundTagGetLongArray(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 12) ? Optional.of(compoundTag.getLongArray(key)) : Optional.empty();
   }

   public static Optional<CompoundTag> CompoundTag_getCompound(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetCompound(compoundTag, key);
   }

   public static Optional<CompoundTag> helperCompoundTagGetCompound(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 10) ? Optional.of(compoundTag.getCompound(key)) : Optional.empty();
   }

   public static CompoundTag CompoundTag_getCompoundOrEmpty(CompoundTag compoundTag, String key) {
      return compoundTag.getCompound(key);
   }

   public static CompoundTag ListTag_getCompoundOrEmpty(ListTag listTag, int index) {
      return listTag.getCompound(index);
   }

   public static Optional<Boolean> CompoundTag_getBoolean(CompoundTag compoundTag, String key) {
      return helperCompoundTagGetBoolean(compoundTag, key);
   }

   public static Optional<Boolean> helperCompoundTagGetBoolean(CompoundTag compoundTag, String key) {
      return compoundTag.contains(key, 99) ? Optional.of(compoundTag.getBoolean(key)) : Optional.empty();
   }

   public static boolean CompoundTag_getBooleanOr(CompoundTag compoundTag, String key, boolean defaultValue) {
      return helperCompoundTagGetBooleanOr(compoundTag, key, defaultValue);
   }

   public static boolean helperCompoundTagGetBooleanOr(CompoundTag compoundTag, String key, boolean defaultValue) {
      return compoundTag.contains(key, 99) ? compoundTag.getBoolean(key) : defaultValue;
   }
}
