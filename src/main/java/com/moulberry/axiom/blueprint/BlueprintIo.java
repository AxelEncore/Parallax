package com.moulberry.axiom.blueprint;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.BlockEntityMap;
import com.moulberry.axiom.hooks.NativeImageExt;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.DynamicTextureSupplier;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.MemoryStack;

public class BlueprintIo {
   private static final int MAGIC = 182827830;
   private static final IOException NOT_VALID_BLUEPRINT = new IOException("Not a valid Blueprint");

   public static BlueprintHeader readHeader(InputStream inputStream) throws IOException {
      if (inputStream.available() < 4) {
         throw NOT_VALID_BLUEPRINT;
      } else {
         DataInputStream dataInputStream = new DataInputStream(inputStream);
         int magic = dataInputStream.readInt();
         if (magic != 182827830) {
            throw NOT_VALID_BLUEPRINT;
         } else {
            dataInputStream.readInt();
            CompoundTag headerTag = NbtIo.read(dataInputStream);
            return BlueprintHeader.load(headerTag);
         }
      }
   }

   public static DynamicTextureSupplier readThumbnail(InputStream inputStream) throws IOException {
      if (inputStream.available() < 4) {
         throw NOT_VALID_BLUEPRINT;
      } else {
         DataInputStream dataInputStream = new DataInputStream(inputStream);
         int magic = dataInputStream.readInt();
         if (magic != 182827830) {
            throw NOT_VALID_BLUEPRINT;
         } else {
            int headerLength = dataInputStream.readInt();
            if (dataInputStream.skip(headerLength) < headerLength) {
               throw NOT_VALID_BLUEPRINT;
            } else {
               int thumbnailLength = dataInputStream.readInt();
               byte[] thumbnailBytes = dataInputStream.readNBytes(thumbnailLength);
               if (thumbnailBytes.length < thumbnailLength) {
                  throw NOT_VALID_BLUEPRINT;
               } else {
                  MemoryStack memoryStack = MemoryStack.stackPush();

                  NativeImage nativeImage;
                  try {
                     ByteBuffer byteBuffer = memoryStack.malloc(thumbnailBytes.length);
                     byteBuffer.put(thumbnailBytes);
                     byteBuffer.rewind();
                     nativeImage = NativeImage.read(byteBuffer);
                  } catch (Throwable var11) {
                     if (memoryStack != null) {
                        try {
                           memoryStack.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  if (memoryStack != null) {
                     memoryStack.close();
                  }

                  return new DynamicTextureSupplier(nativeImage);
               }
            }
         }
      }
   }

   public static RawBlueprint readRawBlueprint(InputStream inputStream) throws IOException {
      if (inputStream.available() < 4) {
         throw NOT_VALID_BLUEPRINT;
      } else {
         DataInputStream dataInputStream = new DataInputStream(inputStream);
         int magic = dataInputStream.readInt();
         if (magic != 182827830) {
            throw NOT_VALID_BLUEPRINT;
         } else {
            dataInputStream.readInt();
            CompoundTag headerTag = NbtIo.read(dataInputStream);
            BlueprintHeader header = BlueprintHeader.load(headerTag);
            int thumbnailLength = dataInputStream.readInt();
            byte[] thumbnailBytes = dataInputStream.readNBytes(thumbnailLength);
            if (thumbnailBytes.length < thumbnailLength) {
               throw NOT_VALID_BLUEPRINT;
            } else {
               int currentDataVersion = DFUHelper.DATA_VERSION;
               dataInputStream.readInt();
               CompoundTag blockDataTag = NbtIo.readCompressed(dataInputStream, NbtAccounter.unlimitedHeap());
               int blueprintDataVersion = VersionUtilsNbt.helperCompoundTagGetIntOr(blockDataTag, "DataVersion", 0);
               if (blueprintDataVersion == 0) {
                  blueprintDataVersion = currentDataVersion;
               }

               ListTag listTag = NbtHelper.getList(blockDataTag, "BlockRegion", 10);
               Long2ObjectMap<PalettedContainer<BlockState>> blockMap = readBlocks(listTag, blueprintDataVersion, header.emptyBlockState());
               ListTag blockEntitiesTag = NbtHelper.getList(blockDataTag, "BlockEntities", 10);
               Long2ObjectMap<CompressedBlockEntity> blockEntities = new Long2ObjectOpenHashMap();
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               Set<String> loggedWarnings = new HashSet<>();

               for (Tag tag : blockEntitiesTag) {
                  CompoundTag blockEntityCompound = (CompoundTag)tag;
                  if (blueprintDataVersion != currentDataVersion) {
                     Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, blockEntityCompound);
                     Dynamic<Tag> output = DataFixers.getDataFixer().update(References.BLOCK_ENTITY, dynamic, blueprintDataVersion, currentDataVersion);
                     blockEntityCompound = (CompoundTag)output.getValue();
                  }

                  int x = VersionUtilsNbt.helperCompoundTagGetIntOr(blockEntityCompound, "x", 0);
                  int y = VersionUtilsNbt.helperCompoundTagGetIntOr(blockEntityCompound, "y", 0);
                  int z = VersionUtilsNbt.helperCompoundTagGetIntOr(blockEntityCompound, "z", 0);
                  BlockPos blockPos = new BlockPos(x, y, z);
                  long pos = blockPos.asLong();
                  String id = VersionUtilsNbt.helperCompoundTagGetStringOr(blockEntityCompound, "id", "");
                  BlockEntityType<?> type = (BlockEntityType<?>)BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null);
                  if (type != null) {
                     PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)blockMap.get(
                        BlockPos.asLong(blockPos.getX() >> 4, blockPos.getY() >> 4, blockPos.getZ() >> 4)
                     );
                     if (container != null) {
                        BlockState blockState = (BlockState)container.get(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
                        if (type.isValid(blockState)) {
                           CompoundTag newTag = blockEntityCompound.copy();
                           newTag.remove("x");
                           newTag.remove("y");
                           newTag.remove("z");
                           newTag.remove("id");
                           CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(newTag, baos);
                           blockEntities.put(pos, compressedBlockEntity);
                        } else if (loggedWarnings.add(id)) {
                           Axiom.LOGGER.info("BlockState " + BlockStateParser.serialize(blockState) + " isn't valid for BlockEntity with id " + id);
                        }
                     }
                  } else if (loggedWarnings.add(id)) {
                     Axiom.LOGGER.info("Unknown id for Block Entity: " + id);
                  }
               }

               ListTag entitiesTag = NbtHelper.getList(blockDataTag, "Entities", 10);
               List<CompoundTag> entities = new ArrayList<>();

               for (Tag tag : entitiesTag) {
                  CompoundTag entityCompound = (CompoundTag)tag;
                  if (blueprintDataVersion != currentDataVersion) {
                     Dynamic<Tag> dynamic = new Dynamic(NbtOps.INSTANCE, entityCompound);
                     Dynamic<Tag> output = DataFixers.getDataFixer().update(References.ENTITY, dynamic, blueprintDataVersion, currentDataVersion);
                     entityCompound = (CompoundTag)output.getValue();
                  }

                  entities.add(entityCompound);
               }

               return new RawBlueprint(header, thumbnailBytes, blockMap, blockEntities, entities);
            }
         }
      }
   }

   public static Long2ObjectMap<PalettedContainer<BlockState>> readBlocks(ListTag list, int dataVersion, BlockState empty) {
      Long2ObjectMap<PalettedContainer<BlockState>> map = new Long2ObjectOpenHashMap();
      Codec<PalettedContainer<BlockState>> containerCodec = BlockBuffer.getCodecForEmptyBlockState(empty);

      for (Tag tag : list) {
         if (tag instanceof CompoundTag compoundTag) {
            int cx = VersionUtilsNbt.helperCompoundTagGetIntOr(compoundTag, "X", 0);
            int cy = VersionUtilsNbt.helperCompoundTagGetIntOr(compoundTag, "Y", 0);
            int cz = VersionUtilsNbt.helperCompoundTagGetIntOr(compoundTag, "Z", 0);
            CompoundTag blockStates = compoundTag.getCompound("BlockStates");
            blockStates = DFUHelper.updatePalettedContainer(blockStates, dataVersion);
            PalettedContainer<BlockState> container = VersionUtils.getOrThrow(containerCodec.parse(NbtOps.INSTANCE, blockStates));
            map.put(BlockPos.asLong(cx, cy, cz), container);
         }
      }

      return map;
   }

   public static Blueprint createFromRaw(RawBlueprint rawBlueprint) throws IOException {
      MemoryStack memoryStack = MemoryStack.stackPush();

      NativeImage nativeImage;
      try {
         ByteBuffer byteBuffer = memoryStack.malloc(rawBlueprint.thumbnail().length);
         byteBuffer.put(rawBlueprint.thumbnail());
         byteBuffer.rewind();
         nativeImage = NativeImage.read(byteBuffer);
      } catch (Throwable var17) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var16) {
               var17.addSuppressed(var16);
            }
         }

         throw var17;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      DynamicTextureSupplier thumbnail = new DynamicTextureSupplier(nativeImage);
      ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
      ObjectIterator var4 = rawBlueprint.blocks().long2ObjectEntrySet().iterator();

      while (var4.hasNext()) {
         Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)var4.next();
         long key = entry.getLongKey();
         PalettedContainer<BlockState> value = (PalettedContainer<BlockState>)entry.getValue();
         int cx = BlockPos.getX(key);
         int cy = BlockPos.getY(key);
         int cz = BlockPos.getZ(key);

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  BlockState state = (BlockState)value.get(x, y, z);
                  if (state != BlockBuffer.EMPTY_STATE) {
                     chunkedBlockRegion.addBlockWithoutDirty(cx * 16 + x, cy * 16 + y, cz * 16 + z, state);
                  }
               }
            }
         }
      }

      chunkedBlockRegion.dirtyAll();
      return new Blueprint(rawBlueprint.header(), thumbnail, chunkedBlockRegion, rawBlueprint.blockEntities(), rawBlueprint.entities());
   }

   public static Blueprint readBlueprint(InputStream inputStream) throws IOException {
      return createFromRaw(readRawBlueprint(inputStream));
   }

   public static void writeHeader(Path inPath, Path outPath, BlueprintHeader newHeader) throws IOException {
      byte[] thumbnailAndBlockBytes;
      try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(inPath))) {
         if (inputStream.available() < 4) {
            throw NOT_VALID_BLUEPRINT;
         }

         DataInputStream dataInputStream = new DataInputStream(inputStream);
         int magic = dataInputStream.readInt();
         if (magic != 182827830) {
            throw NOT_VALID_BLUEPRINT;
         }

         int headerLength = dataInputStream.readInt();
         if (dataInputStream.skip(headerLength) < headerLength) {
            throw NOT_VALID_BLUEPRINT;
         }

         thumbnailAndBlockBytes = dataInputStream.readAllBytes();
      }

      try (BufferedOutputStream var17 = new BufferedOutputStream(Files.newOutputStream(outPath))) {
         DataOutputStream dataOutputStream = new DataOutputStream(var17);
         dataOutputStream.writeInt(182827830);
         CompoundTag headerTag = newHeader.save(new CompoundTag());
         ByteArrayOutputStream baos = new ByteArrayOutputStream();

         try (DataOutputStream os = new DataOutputStream(baos)) {
            NbtIo.write(headerTag, os);
         }

         dataOutputStream.writeInt(baos.size());
         baos.writeTo(dataOutputStream);
         dataOutputStream.write(thumbnailAndBlockBytes);
      }
   }

   public static void writeRaw(OutputStream outputStream, RawBlueprint rawBlueprint) throws IOException {
      DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
      dataOutputStream.writeInt(182827830);
      CompoundTag headerTag = rawBlueprint.header().save(new CompoundTag());
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try (DataOutputStream os = new DataOutputStream(baos)) {
         NbtIo.write(headerTag, os);
      }

      dataOutputStream.writeInt(baos.size());
      baos.writeTo(dataOutputStream);
      dataOutputStream.writeInt(rawBlueprint.thumbnail().length);
      dataOutputStream.write(rawBlueprint.thumbnail());
      CompoundTag var20 = new CompoundTag();
      ListTag savedBlockRegions = new ListTag();
      Codec blockStateCodec = BlockBuffer.getCodecForEmptyBlockState(rawBlueprint.header().emptyBlockState());
      ObjectIterator blockEntitiesTag = rawBlueprint.blocks().long2ObjectEntrySet().iterator();

      while (blockEntitiesTag.hasNext()) {
         Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)blockEntitiesTag.next();
         long pos = entry.getLongKey();
         PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)entry.getValue();
         int cx = BlockPos.getX(pos);
         int cy = BlockPos.getY(pos);
         int cz = BlockPos.getZ(pos);
         CompoundTag tag = new CompoundTag();
         tag.putInt("X", cx);
         tag.putInt("Y", cy);
         tag.putInt("Z", cz);
         Tag encoded = (Tag)VersionUtils.getOrThrow(blockStateCodec.encodeStart(NbtOps.INSTANCE, container));
         tag.put("BlockStates", encoded);
         savedBlockRegions.add(tag);
      }

      var20.putInt("DataVersion", DFUHelper.DATA_VERSION);
      var20.put("BlockRegion", savedBlockRegions);
      ListTag blockEntitiesTagx = new ListTag();
      rawBlueprint.blockEntities().forEach((posx, compressedBlockEntity) -> {
         int x = BlockPos.getX(posx);
         int y = BlockPos.getY(posx);
         int z = BlockPos.getZ(posx);
         PalettedContainer<BlockState> containerx = (PalettedContainer<BlockState>)rawBlueprint.blocks().get(BlockPos.asLong(x >> 4, y >> 4, z >> 4));
         BlockState blockState = (BlockState)containerx.get(x & 15, y & 15, z & 15);
         BlockEntityType<?> type = BlockEntityMap.get(blockState);
         if (type != null) {
            ResourceLocation resourceLocation = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (resourceLocation != null) {
               CompoundTag tagx = compressedBlockEntity.decompress();
               tagx.putInt("x", x);
               tagx.putInt("y", y);
               tagx.putInt("z", z);
               tagx.putString("id", resourceLocation.toString());
               blockEntitiesTagx.add(tagx);
            }
         }
      });
      var20.put("BlockEntities", blockEntitiesTagx);
      ListTag entitiesTag = new ListTag();
      entitiesTag.addAll(rawBlueprint.entities());
      var20.put("Entities", entitiesTag);
      baos.reset();
      NbtIo.writeCompressed(var20, baos);
      dataOutputStream.writeInt(baos.size());
      baos.writeTo(dataOutputStream);
   }

   public static void write(
      OutputStream outputStream,
      @NotNull BlueprintHeader header,
      @NotNull NativeImage thumbnail,
      @NotNull ChunkedBlockRegion chunkedBlockRegion,
      Long2ObjectMap<CompressedBlockEntity> blockEntities,
      List<CompoundTag> entities
   ) throws IOException {
      DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
      dataOutputStream.writeInt(182827830);
      CompoundTag headerTag = header.save(new CompoundTag());
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      try (DataOutputStream os = new DataOutputStream(baos)) {
         NbtIo.write(headerTag, os);
      }

      dataOutputStream.writeInt(baos.size());
      baos.writeTo(dataOutputStream);
      byte[] var14 = ((NativeImageExt)(Object)thumbnail).axiom$asByteArray();
      dataOutputStream.writeInt(var14.length);
      dataOutputStream.write(var14);
      CompoundTag compound = saveBlockData(new CompoundTag(), chunkedBlockRegion, blockEntities, entities);
      baos.reset();
      NbtIo.writeCompressed(compound, baos);
      dataOutputStream.writeInt(baos.size());
      baos.writeTo(dataOutputStream);
   }

   private static CompoundTag saveBlockData(
      CompoundTag compound, ChunkedBlockRegion chunkedBlockRegion, Long2ObjectMap<CompressedBlockEntity> blockEntities, List<CompoundTag> entities
   ) {
      ListTag savedBlockRegions = new ListTag();
      chunkedBlockRegion.save(savedBlockRegions);
      compound.putInt("DataVersion", DFUHelper.DATA_VERSION);
      compound.put("BlockRegion", savedBlockRegions);
      ListTag blockEntitiesTag = new ListTag();
      blockEntities.forEach((pos, compressedBlockEntity) -> {
         int x = BlockPos.getX(pos);
         int y = BlockPos.getY(pos);
         int z = BlockPos.getZ(pos);
         BlockState blockState = chunkedBlockRegion.getBlockStateOrAir(x, y, z);
         BlockEntityType<?> type = BlockEntityMap.get(blockState);
         if (type != null) {
            ResourceLocation resourceLocation = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            if (resourceLocation != null) {
               CompoundTag tag = compressedBlockEntity.decompress();
               tag.putInt("x", x);
               tag.putInt("y", y);
               tag.putInt("z", z);
               tag.putString("id", resourceLocation.toString());
               blockEntitiesTag.add(tag);
            }
         }
      });
      compound.put("BlockEntities", blockEntitiesTag);
      ListTag entitiesTag = new ListTag();
      entitiesTag.addAll(entities);
      compound.put("Entities", entitiesTag);
      return compound;
   }
}
