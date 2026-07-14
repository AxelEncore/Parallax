package com.moulberry.axiom.editor.schematic;

import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.BlockEntityMap;
import com.moulberry.axiom.block_maps.LegacyBlocks;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.blocks.BlockStateParser.BlockResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.UpgradeData;

public class SchematicLoader {
   public static Path getDefaultSchematicDir() {
      Path worldeditSchematicsDir = AxiomPlatform.configDir().resolve("worldedit").resolve("schematics");
      return Files.exists(worldeditSchematicsDir) ? worldeditSchematicsDir : AxiomPlatform.gameDir();
   }

   public static ClipboardObject loadSponge(CompoundTag compoundTag) throws SchematicLoader.SchematicLoadException {
      int currentDataVersion = DFUHelper.DATA_VERSION;
      SchematicLoader.SpongeSchematic schem = SchematicLoader.SpongeSchematic.parse(compoundTag);
      CompoundTag palette = schem.palette;
      Int2ObjectMap<BlockState> paletteMap = new Int2ObjectOpenHashMap();

      for (String key : palette.getAllKeys()) {
         try {
            BlockState blockState;
            if (currentDataVersion <= schem.dataVersion) {
               BlockResult result = BlockStateParser.parseForBlock(VersionUtils.createLookup(BuiltInRegistries.BLOCK), key, false);
               blockState = result.blockState();
            } else {
               CompoundTag blockTag = DFUHelper.createBlockTag(key);
               blockState = DFUHelper.updateBlockState(blockTag, schem.dataVersion).result().orElse(Blocks.AIR.defaultBlockState());
            }

            paletteMap.put(VersionUtilsNbt.helperCompoundTagGetIntOr(palette, key, -1), blockState);
         } catch (Exception var21) {
            var21.printStackTrace();
            throw new SchematicLoader.SchematicLoadException("Unable to parse BlockState: " + key);
         }
      }

      ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
      FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(schem.blockData));

      for (int y = 0; y < schem.height; y++) {
         for (int z = 0; z < schem.length; z++) {
            for (int x = 0; x < schem.width; x++) {
               int blockId = buf.readVarInt();
               BlockState blockState = (BlockState)paletteMap.get(blockId);
               if (blockState == null) {
                  blockState = Blocks.AIR.defaultBlockState();
               }

               blockRegion.addBlockWithoutDirty(x - schem.width / 2, y - schem.height / 2, z - schem.length / 2, blockState);
            }
         }
      }

      blockRegion.dirtyAll();
      Long2ObjectMap<CompressedBlockEntity> blockEntityMap = new Long2ObjectOpenHashMap();
      if (schem.blockEntities != null && !schem.blockEntities.isEmpty()) {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();

         for (Tag blockEntityTag : schem.blockEntities) {
            CompoundTag blockEntity = (CompoundTag)blockEntityTag;
            Optional<int[]> posOptional = VersionUtilsNbt.helperCompoundTagGetIntArray(blockEntity, "Pos");
            if (!posOptional.isEmpty()) {
               Optional<String> idOptional = VersionUtilsNbt.helperCompoundTagGetString(blockEntity, "Id");
               if (!idOptional.isEmpty()) {
                  int[] pos = posOptional.get();
                  String id = idOptional.get();
                  int x = pos[0] - schem.width / 2;
                  int y = pos[1] - schem.height / 2;
                  int z = pos[2] - schem.length / 2;
                  BlockEntityType<?> type = (BlockEntityType<?>)BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null);
                  if (type != null && type.isValid(blockRegion.getBlockStateOrAir(x, y, z))) {
                     blockEntity.remove("Pos");
                     blockEntity.remove("Id");
                     if (blockEntity.getAllKeys().size() == 1) {
                        String key = (String)blockEntity.getAllKeys().iterator().next();
                        if (key.equals("Data")) {
                           blockEntity = blockEntity.getCompound("Data");
                        }
                     }

                     CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(blockEntity, baos);
                     blockEntityMap.put(BlockPos.asLong(x, y, z), compressedBlockEntity);
                  }
               }
            }
         }
      }

      return new ClipboardObject.Anonymous(blockRegion, blockEntityMap, List.of(), schem.name, 45.0F, true, compoundTag);
   }

   public static ClipboardObject loadLitematic(CompoundTag compoundTag) throws SchematicLoader.SchematicLoadException {
      int litematicDataVersion = VersionUtilsNbt.helperCompoundTagGetIntOr(compoundTag, "MinecraftDataVersion", 0);
      String name = "";
      Optional<CompoundTag> metadataOptional = VersionUtilsNbt.helperCompoundTagGetCompound(compoundTag, "Metadata");
      if (metadataOptional.isPresent()) {
         CompoundTag metadata = metadataOptional.get();
         name = VersionUtilsNbt.helperCompoundTagGetString(metadata, "Name").orElse("");
      }

      ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
      Long2ObjectMap<CompressedBlockEntity> blockEntityMap = new Long2ObjectOpenHashMap();
      CompoundTag regions = compoundTag.getCompound("Regions");

      for (String regionName : regions.getAllKeys()) {
         CompoundTag region = regions.getCompound(regionName);
         loadLitematicRegion(region, blockRegion, blockEntityMap, litematicDataVersion);
      }

      blockRegion.dirtyAll();
      return new ClipboardObject.Anonymous(blockRegion, blockEntityMap, List.of(), name, 45.0F, true, null);
   }

   private static void loadLitematicRegion(
      CompoundTag region, ChunkedBlockRegion blockRegion, Long2ObjectMap<CompressedBlockEntity> blockEntityMap, int schemDataVersion
   ) {
      int currentDataVersion = DFUHelper.DATA_VERSION;
      Int2ObjectMap<BlockState> paletteMap = new Int2ObjectOpenHashMap();
      ListTag palette = NbtHelper.getList(region, "BlockStatePalette", 10);

      for (int i = 0; i < palette.size(); i++) {
         CompoundTag entryTag = palette.getCompound(i);
         BlockState blockState;
         if (currentDataVersion <= schemDataVersion) {
            blockState = BlockState.CODEC.parse(NbtOps.INSTANCE, entryTag).result().orElse(Blocks.AIR.defaultBlockState());
         } else {
            blockState = DFUHelper.updateBlockState(entryTag, schemDataVersion).result().orElse(Blocks.AIR.defaultBlockState());
         }

         paletteMap.put(i, blockState);
      }

      CompoundTag size = region.getCompound("Size");
      int width = VersionUtilsNbt.helperCompoundTagGetIntOr(size, "x", 0);
      int height = VersionUtilsNbt.helperCompoundTagGetIntOr(size, "y", 0);
      int length = VersionUtilsNbt.helperCompoundTagGetIntOr(size, "z", 0);
      CompoundTag position = region.getCompound("Position");
      int offsetX = VersionUtilsNbt.helperCompoundTagGetIntOr(position, "x", 0);
      int offsetY = VersionUtilsNbt.helperCompoundTagGetIntOr(position, "y", 0);
      int offsetZ = VersionUtilsNbt.helperCompoundTagGetIntOr(position, "z", 0);
      if (width < 0) {
         offsetX += width + 1;
         width = -width;
      }

      if (height < 0) {
         offsetY += height + 1;
         height = -height;
      }

      if (length < 0) {
         offsetZ += length + 1;
         length = -length;
      }

      int bitsPerEntry = Math.max(2, 32 - Integer.numberOfLeadingZeros(palette.size() - 1));
      int entryMask = (1 << bitsPerEntry) - 1;
      long[] blockStateData = VersionUtilsNbt.helperCompoundTagGetLongArray(region, "BlockStates").orElse(new long[0]);
      int currentLongIndex = 0;
      int currentLongBitPosition = 0;

      for (int y = 0; y < height; y++) {
         for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++) {
               long value = blockStateData[currentLongIndex] >>> currentLongBitPosition & entryMask;
               int overflowBits = currentLongBitPosition + bitsPerEntry - 64;
               if (overflowBits > 0) {
                  value |= (blockStateData[currentLongIndex + 1] & (1L << overflowBits) - 1L) << bitsPerEntry - overflowBits;
               }

               currentLongBitPosition += bitsPerEntry;
               if (currentLongBitPosition >= 64) {
                  currentLongIndex++;
                  currentLongBitPosition -= 64;
               }

               BlockState blockState = (BlockState)paletteMap.get((int)value);
               blockRegion.addBlockWithoutDirty(x + offsetX, y + offsetY, z + offsetZ, blockState);
            }
         }
      }

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      for (Tag blockEntityTag : NbtHelper.getList(region, "TileEntities", 10)) {
         CompoundTag blockEntity = (CompoundTag)blockEntityTag;
         int x = VersionUtilsNbt.helperCompoundTagGetIntOr(blockEntity, "x", 0);
         int y = VersionUtilsNbt.helperCompoundTagGetIntOr(blockEntity, "y", 0);
         int z = VersionUtilsNbt.helperCompoundTagGetIntOr(blockEntity, "z", 0);
         blockEntity.remove("x");
         blockEntity.remove("y");
         blockEntity.remove("z");
         if (!blockEntity.isEmpty()) {
            BlockState blockState = blockRegion.getBlockStateOrAir(x + offsetX, y + offsetY, z + offsetZ);
            BlockEntityType<?> blockEntityType = BlockEntityMap.get(blockState);
            if (blockEntityType != null) {
               CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(blockEntity, baos);
               blockEntityMap.put(BlockPos.asLong(x + offsetX, y + offsetY, z + offsetZ), compressedBlockEntity);
            }
         }
      }
   }

   public static ClipboardObject loadLegacy(CompoundTag compoundTag) throws SchematicLoader.SchematicLoadException {
      Optional<byte[]> blocksOptional = VersionUtilsNbt.helperCompoundTagGetByteArray(compoundTag, "Blocks");
      if (blocksOptional.isEmpty()) {
         throw new SchematicLoader.SchematicLoadException("Missing 'Blocks' byte array tag");
      } else {
         Optional<byte[]> dataOptional = VersionUtilsNbt.helperCompoundTagGetByteArray(compoundTag, "Data");
         if (dataOptional.isEmpty()) {
            throw new SchematicLoader.SchematicLoadException("Missing 'Data' byte array tag");
         } else {
            Optional<Short> widthOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Width");
            if (widthOptional.isEmpty()) {
               throw new SchematicLoader.SchematicLoadException("Missing 'Width' short tag");
            } else {
               Optional<Short> heightOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Height");
               if (heightOptional.isEmpty()) {
                  throw new SchematicLoader.SchematicLoadException("Missing 'Height' short tag");
               } else {
                  Optional<Short> lengthOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Length");
                  if (lengthOptional.isEmpty()) {
                     throw new SchematicLoader.SchematicLoadException("Missing 'Length' short tag");
                  } else {
                     int width = widthOptional.get();
                     int height = heightOptional.get();
                     int length = lengthOptional.get();
                     int maxIndex = width * height * length;
                     int minX = -Math.floorDiv(width, 2);
                     int minY = -Math.floorDiv(height, 2);
                     int minZ = -Math.floorDiv(length, 2);
                     byte[] legacyBlockIds = blocksOptional.get();
                     byte[] legacyBlockData = dataOptional.get();
                     ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
                     BlockState[] legacyBlocks = LegacyBlocks.getLegacyBlocks();

                     for (int index = 0; index < maxIndex; index++) {
                        int blockId = legacyBlockIds[index] & 255;
                        int blockData = legacyBlockData[index] & 255;
                        BlockState blockState = legacyBlocks[blockId * 16 + blockData];
                        if (blockState == null) {
                           blockState = legacyBlocks[blockId * 16];
                           if (blockState == null) {
                              continue;
                           }
                        }

                        int x = index % width + minX;
                        int z = index % (width * length) / width + minZ;
                        int y = index / (width * length) + minY;
                        chunkedBlockRegion.addBlockWithoutDirty(x, y, z, blockState);
                     }

                     Direction[] directions = Direction.values();
                     MutableBlockPos mutableBlockPos1 = new MutableBlockPos();
                     MutableBlockPos mutableBlockPos2 = new MutableBlockPos();
                     ChunkedBlockRegion updatedRegion = new ChunkedBlockRegion();
                     chunkedBlockRegion.forEachEntry(
                        (xx, yx, zx, block) -> {
                           if (block.getBlock() instanceof DoublePlantBlock) {
                              DoubleBlockHalf half = (DoubleBlockHalf)block.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                              if (half == DoubleBlockHalf.UPPER) {
                                 BlockState below = chunkedBlockRegion.getBlockStateOrAir(xx, yx - 1, zx);
                                 if (below.getBlock() instanceof DoublePlantBlock) {
                                    block = (BlockState)below.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
                                 }
                              }
                           } else if (block.getBlock() instanceof DoorBlock) {
                              DoubleBlockHalf half = (DoubleBlockHalf)block.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
                              if (half == DoubleBlockHalf.UPPER) {
                                 BlockState below = chunkedBlockRegion.getBlockStateOrAir(xx, yx - 1, zx);
                                 if (below.getBlock() instanceof DoorBlock) {
                                    block = (BlockState)block.setValue(
                                       BlockStateProperties.HORIZONTAL_FACING, (Direction)below.getValue(BlockStateProperties.HORIZONTAL_FACING)
                                    );
                                    block = (BlockState)block.setValue(BlockStateProperties.OPEN, (Boolean)below.getValue(BlockStateProperties.OPEN));
                                 }
                              } else {
                                 BlockState above = chunkedBlockRegion.getBlockStateOrAir(xx, yx + 1, zx);
                                 if (above.getBlock() instanceof DoorBlock) {
                                    block = (BlockState)block.setValue(
                                       BlockStateProperties.DOOR_HINGE, (DoorHingeSide)above.getValue(BlockStateProperties.DOOR_HINGE)
                                    );
                                    block = (BlockState)block.setValue(BlockStateProperties.POWERED, (Boolean)above.getValue(BlockStateProperties.POWERED));
                                 }
                              }
                           }

                           updatedRegion.addBlock(xx, yx, zx, block);
                        }
                     );
                     updatedRegion.forEachEntry((xx, yx, zx, block) -> {
                        try {
                           for (Direction direction : directions) {
                              mutableBlockPos1.set(xx, yx, zx);
                              mutableBlockPos2.setWithOffset(mutableBlockPos1, direction);
                              block = UpgradeData.updateState(block, direction, updatedRegion, mutableBlockPos1, mutableBlockPos2);
                           }
                        } catch (Exception var13x) {
                        }

                        chunkedBlockRegion.addBlockWithoutDirty(xx, yx, zx, block);
                     });
                     chunkedBlockRegion.dirtyAll();
                     return new ClipboardObject.Anonymous(chunkedBlockRegion, new Long2ObjectOpenHashMap(), List.of(), "", 45.0F, true, compoundTag);
                  }
               }
            }
         }
      }
   }

   public static class SchematicLoadException extends RuntimeException {
      public SchematicLoadException(String message) {
         super(message);
      }
   }

   record SpongeSchematic(
      int dataVersion, int width, int height, int length, String name, CompoundTag palette, byte[] blockData, ListTag blockEntities, CompoundTag remainingTag
   ) {
      public static SchematicLoader.SpongeSchematic parse(CompoundTag compoundTag) {
         Optional<Integer> versionOptional = VersionUtilsNbt.helperCompoundTagGetInt(compoundTag, "Version");
         if (versionOptional.isEmpty()) {
            throw new SchematicLoader.SchematicLoadException("Missing 'Version' int tag");
         } else {
            int version = versionOptional.get();
            if (version == 2) {
               return parseV2(compoundTag);
            } else if (version == 3) {
               return parseV3(compoundTag);
            } else {
               throw new SchematicLoader.SchematicLoadException("Unsupported version, expected 2 or 3, but got " + version);
            }
         }
      }

      public static SchematicLoader.SpongeSchematic parseV2(CompoundTag compoundTag) {
         Optional<Integer> dataVersionOptional = VersionUtilsNbt.helperCompoundTagGetInt(compoundTag, "DataVersion");
         if (dataVersionOptional.isEmpty()) {
            throw new SchematicLoader.SchematicLoadException("Missing 'DataVersion' int tag");
         } else {
            Optional<Short> widthOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Width");
            if (widthOptional.isEmpty()) {
               throw new SchematicLoader.SchematicLoadException("Missing 'Width' short tag");
            } else {
               Optional<Short> heightOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Height");
               if (heightOptional.isEmpty()) {
                  throw new SchematicLoader.SchematicLoadException("Missing 'Height' short tag");
               } else {
                  Optional<Short> lengthOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Length");
                  if (lengthOptional.isEmpty()) {
                     throw new SchematicLoader.SchematicLoadException("Missing 'Length' short tag");
                  } else {
                     Optional<CompoundTag> paletteOptional = VersionUtilsNbt.helperCompoundTagGetCompound(compoundTag, "Palette");
                     if (paletteOptional.isEmpty()) {
                        throw new SchematicLoader.SchematicLoadException("Missing 'Palette' compound tag");
                     } else {
                        Optional<byte[]> blockDataOptional = VersionUtilsNbt.helperCompoundTagGetByteArray(compoundTag, "BlockData");
                        if (blockDataOptional.isEmpty()) {
                           throw new SchematicLoader.SchematicLoadException("Missing 'BlockData' byte array tag");
                        } else {
                           int schematicDataVersion = dataVersionOptional.get();
                           CompoundTag palette = paletteOptional.get();
                           byte[] blockData = blockDataOptional.get();
                           String name = "";
                           Optional<CompoundTag> metadataOptional = VersionUtilsNbt.helperCompoundTagGetCompound(compoundTag, "Metadata");
                           if (metadataOptional.isPresent()) {
                              CompoundTag metadata = metadataOptional.get();
                              name = VersionUtilsNbt.helperCompoundTagGetString(metadata, "Name").orElse("");
                           }

                           int width = widthOptional.get() & '\uffff';
                           int height = heightOptional.get() & '\uffff';
                           int length = lengthOptional.get() & '\uffff';
                           ListTag blockEntities = NbtHelper.getList(compoundTag, "BlockEntities", 10);
                           CompoundTag remaining = compoundTag.copy();
                           compoundTag.remove("Version");
                           compoundTag.remove("DataVersion");
                           compoundTag.remove("Palette");
                           compoundTag.remove("PaletteMax");
                           compoundTag.remove("BlockData");
                           compoundTag.remove("Width");
                           compoundTag.remove("Height");
                           compoundTag.remove("Length");
                           compoundTag.remove("BlockEntities");
                           return new SchematicLoader.SpongeSchematic(
                              schematicDataVersion, width, height, length, name, palette, blockData, blockEntities, remaining
                           );
                        }
                     }
                  }
               }
            }
         }
      }

      public static SchematicLoader.SpongeSchematic parseV3(CompoundTag compoundTag) {
         Optional<Integer> dataVersionOptional = VersionUtilsNbt.helperCompoundTagGetInt(compoundTag, "DataVersion");
         if (dataVersionOptional.isEmpty()) {
            throw new SchematicLoader.SchematicLoadException("Missing 'DataVersion' int tag");
         } else {
            Optional<Short> widthOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Width");
            if (widthOptional.isEmpty()) {
               throw new SchematicLoader.SchematicLoadException("Missing 'Width' short tag");
            } else {
               Optional<Short> heightOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Height");
               if (heightOptional.isEmpty()) {
                  throw new SchematicLoader.SchematicLoadException("Missing 'Height' short tag");
               } else {
                  Optional<Short> lengthOptional = VersionUtilsNbt.helperCompoundTagGetShort(compoundTag, "Length");
                  if (lengthOptional.isEmpty()) {
                     throw new SchematicLoader.SchematicLoadException("Missing 'Length' short tag");
                  } else {
                     Optional<CompoundTag> blocksOptional = VersionUtilsNbt.helperCompoundTagGetCompound(compoundTag, "Blocks");
                     if (blocksOptional.isEmpty()) {
                        throw new SchematicLoader.SchematicLoadException("Missing 'Blocks' compound tag");
                     } else {
                        CompoundTag blocks = blocksOptional.get();
                        Optional<CompoundTag> paletteOptional = VersionUtilsNbt.helperCompoundTagGetCompound(blocks, "Palette");
                        if (paletteOptional.isEmpty()) {
                           throw new SchematicLoader.SchematicLoadException("Missing 'Palette' compound tag inside 'Blocks'");
                        } else {
                           Optional<byte[]> dataOptional = VersionUtilsNbt.helperCompoundTagGetByteArray(blocks, "Data");
                           if (dataOptional.isEmpty()) {
                              throw new SchematicLoader.SchematicLoadException("Missing 'Data' byte array tag inside 'Blocks'");
                           } else {
                              int schematicDataVersion = dataVersionOptional.get();
                              CompoundTag palette = paletteOptional.get();
                              byte[] blockData = dataOptional.get();
                              String name = "";
                              Optional<CompoundTag> metadataOptional = VersionUtilsNbt.helperCompoundTagGetCompound(compoundTag, "Metadata");
                              if (metadataOptional.isPresent()) {
                                 CompoundTag metadata = metadataOptional.get();
                                 name = VersionUtilsNbt.helperCompoundTagGetString(metadata, "Name").orElse("");
                              }

                              int width = widthOptional.get() & '\uffff';
                              int height = heightOptional.get() & '\uffff';
                              int length = lengthOptional.get() & '\uffff';
                              ListTag blockEntities = NbtHelper.getList(compoundTag, "BlockEntities", 10);
                              CompoundTag remaining = compoundTag.copy();
                              compoundTag.remove("Version");
                              compoundTag.remove("DataVersion");
                              compoundTag.remove("BlockData");
                              compoundTag.remove("Width");
                              compoundTag.remove("Height");
                              compoundTag.remove("Length");
                              compoundTag.remove("Blocks");
                              return new SchematicLoader.SpongeSchematic(
                                 schematicDataVersion, width, height, length, name, palette, blockData, blockEntities, remaining
                              );
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
