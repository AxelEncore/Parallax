package com.moulberry.axiom.blueprint;

import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.utils.NbtHelper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record BlueprintHeader(
   int version,
   String name,
   String author,
   List<String> tags,
   float thumbnailYaw,
   float thumbnailPitch,
   boolean lockedThumbnail,
   int blockCount,
   boolean containsAir
) {
   public static final int CURRENT_VERSION = 2;

   public BlueprintHeader(
      String name, String author, List<String> tags, float thumbnailYaw, float thumbnailPitch, boolean lockedThumbnail, int blockCount, boolean containsAir
   ) {
      this(2, name, author, tags, thumbnailYaw, thumbnailPitch, lockedThumbnail, blockCount, containsAir);
   }

   public BlockState emptyBlockState() {
      return this.version <= 1 ? Blocks.STRUCTURE_VOID.defaultBlockState() : Blocks.VOID_AIR.defaultBlockState();
   }

   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeVarInt(2);
      friendlyByteBuf.writeUtf(this.name);
      friendlyByteBuf.writeUtf(this.author);
      friendlyByteBuf.writeCollection(this.tags, FriendlyByteBuf::writeUtf);
      friendlyByteBuf.writeInt(this.blockCount);
      friendlyByteBuf.writeBoolean(this.containsAir);
   }

   public static BlueprintHeader read(FriendlyByteBuf friendlyByteBuf) {
      int version = friendlyByteBuf.readVarInt();
      String name = friendlyByteBuf.readUtf();
      String author = friendlyByteBuf.readUtf();
      List<String> tags = friendlyByteBuf.readList(FriendlyByteBuf::readUtf);
      int blockCount = friendlyByteBuf.readInt();
      boolean containsAir = friendlyByteBuf.readBoolean();
      return new BlueprintHeader(version, name, author, tags, 0.0F, 0.0F, true, blockCount, containsAir);
   }

   public static BlueprintHeader load(CompoundTag tag) {
      int version = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Version", 0);
      String name = VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "Name", "");
      String author = VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "Author", "");
      float thumbnailYaw = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "ThumbnailYaw", 135.0F);
      float thumbnailPitch = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "ThumbnailPitch", 30.0F);
      boolean lockedThumbnail = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "LockedThumbnail", false);
      int blockCount = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BlockCount", 0);
      boolean containsAir = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "ContainsAir", false);
      List<String> tags = new ArrayList<>();

      for (Tag string : NbtHelper.getList(tag, "Tags", 8)) {
         VersionUtilsNbt.helperTagAsString(string).ifPresent(tags::add);
      }

      return new BlueprintHeader(version, name, author, tags, thumbnailYaw, thumbnailPitch, lockedThumbnail, blockCount, containsAir);
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag listTag = new ListTag();

      for (String string : this.tags) {
         listTag.add(StringTag.valueOf(string));
      }

      tag.putInt("Version", 2);
      tag.putString("Name", this.name);
      tag.putString("Author", this.author);
      tag.put("Tags", listTag);
      tag.putFloat("ThumbnailYaw", this.thumbnailYaw);
      tag.putFloat("ThumbnailPitch", this.thumbnailPitch);
      tag.putBoolean("LockedThumbnail", this.lockedThumbnail);
      tag.putInt("BlockCount", this.blockCount);
      tag.putBoolean("ContainsAir", this.containsAir);
      return tag;
   }
}
