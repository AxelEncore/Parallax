package com.moulberry.axiom.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.world_modification.BlockBuffer;
import net.minecraft.SharedConstants;
import net.minecraft.core.IdMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;

public class DFUHelper {
   public static final int DATA_VERSION = SharedConstants.getCurrentVersion().getDataVersion().getVersion();

   public static PalettedContainer<BlockState> createPalettedContainer(IdMap<BlockState> registry, BlockState defaultBlockState) {
      return new PalettedContainer(registry, defaultBlockState, Strategy.SECTION_STATES);
   }

   public static void checkContainerFormat() {
      PalettedContainer<BlockState> container = createPalettedContainer(Block.BLOCK_STATE_REGISTRY, Blocks.PISTON.defaultBlockState());
      Tag encoded = VersionUtils.getOrThrow(BlockBuffer.getCodecForEmptyBlockState(BlockBuffer.EMPTY_STATE).encodeStart(NbtOps.INSTANCE, container));
      if (encoded instanceof CompoundTag compoundTag) {
         if (!hasExpectedPaletteTag(compoundTag)) {
            throw new RuntimeException("Encoded PalettedContainer doesn't have palette");
         } else {
            ListTag paletteList = NbtHelper.getList(compoundTag, "palette", 10);
            if (paletteList.isEmpty()) {
               throw new RuntimeException("Encoded PalettedContainer's palette is empty");
            } else {
               for (Tag entry : paletteList) {
                  if (!(entry instanceof CompoundTag entryCompound)) {
                     throw new RuntimeException("Encoded PalettedContainer's palette element isn't a CompoundTag");
                  }

                  if (!entryCompound.contains("Name")) {
                     throw new RuntimeException("Encoded PalettedContainer's palette element doesn't contain 'Name'");
                  }

                  if (!entryCompound.contains("Properties")) {
                     throw new RuntimeException("Encoded PalettedContainer's palette element doesn't contain 'Properties'");
                  }
               }
            }
         }
      } else {
         throw new RuntimeException("Encoded PalettedContainer isn't a CompoundTag");
      }
   }

   public static CompoundTag updatePalettedContainer(CompoundTag tag, int fromVersion) {
      if (!hasExpectedPaletteTag(tag)) {
         Axiom.LOGGER.warn("'palette' tag missing from PalettedContainer NBT, unable to upgrade...");
         return tag;
      } else if (fromVersion == DATA_VERSION) {
         return tag;
      } else {
         tag = tag.copy();
         ListTag newPalette = new ListTag();

         for (Tag entry : NbtHelper.getList(tag, "palette", 10)) {
            Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, entry);
            Dynamic<Tag> output = DataFixers.getDataFixer().update(References.BLOCK_STATE, dynamic, fromVersion, DATA_VERSION);
            newPalette.add((Tag)output.getValue());
         }

         tag.put("palette", newPalette);
         return tag;
      }
   }

   public static DataResult<BlockState> updateBlockState(CompoundTag tag, int fromVersion) {
      Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, tag);
      Dynamic<Tag> output = DataFixers.getDataFixer().update(References.BLOCK_STATE, dynamic, fromVersion, DATA_VERSION);
      return BlockState.CODEC.parse(output);
   }

   public static CompoundTag createBlockTag(String input) throws CommandSyntaxException {
      CompoundTag blockTag = new CompoundTag();
      StringReader reader = new StringReader(input);
      ResourceLocation blockIdentifier = ResourceLocation.read(reader);
      blockTag.putString("Name", blockIdentifier.toString());
      CompoundTag properties = new CompoundTag();
      if (reader.canRead() && reader.peek() == '[') {
         reader.skip();
         reader.skipWhitespace();

         while (reader.canRead() && reader.peek() != ']') {
            reader.skipWhitespace();
            String property = reader.readString();
            if (!reader.canRead() || reader.peek() != '=') {
               throw new RuntimeException("Expected =");
            }

            reader.skip();
            reader.skipWhitespace();
            String value = reader.readString();
            properties.putString(property, value);
            reader.skipWhitespace();
            if (!reader.canRead()) {
               break;
            }

            if (reader.peek() == ',') {
               reader.skip();
            } else if (reader.peek() == ']') {
               break;
            }
         }
      }

      blockTag.put("Properties", properties);
      return blockTag;
   }

   private static boolean hasExpectedPaletteTag(CompoundTag tag) {
      if (!tag.contains("palette")) {
         return false;
      } else {
         ListTag listTag = (ListTag)tag.get("palette");
         return listTag == null ? false : listTag.isEmpty() || listTag.get(0).getId() == 10;
      }
   }
}
