package com.moulberry.axiom.world_modification;

import com.mojang.serialization.Codec;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.collections.PositionConsumer;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.PositionUtils;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.IdMap;
import net.minecraft.core.IdMapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.SingleValuePalette;
import net.minecraft.world.level.chunk.PalettedContainer.Data;
import net.minecraft.world.level.chunk.PalettedContainer.Strategy;
import org.jetbrains.annotations.Nullable;

public class BlockBuffer implements BlockOrBiomeBuffer {
   public static final BlockState EMPTY_STATE = Blocks.VOID_AIR.defaultBlockState();
   private static final Map<BlockState, Codec<PalettedContainer<BlockState>>> BLOCK_STATE_CODECS = new HashMap<>();
   private static final Map<BlockState, IdMap<BlockState>> ID_MAPPERS = new HashMap<>();
   private final Long2ObjectMap<PalettedContainer<BlockState>> values;
   private PalettedContainer<BlockState> last = null;
   private long lastId = PositionUtils.MIN_POSITION_LONG;
   private final Long2ObjectMap<Short2ObjectMap<CompressedBlockEntity>> blockEntities = new Long2ObjectOpenHashMap();

   public static PalettedContainer<BlockState> createPalettedContainerForEmptyBlockState(BlockState emptyBlockState) {
      return DFUHelper.createPalettedContainer(getIdMapForEmptyBlockState(emptyBlockState), emptyBlockState);
   }

   public static IdMap<BlockState> getIdMapForEmptyBlockState(BlockState empty) {
      return (IdMap<BlockState>)(empty == EMPTY_STATE ? Block.BLOCK_STATE_REGISTRY : ID_MAPPERS.computeIfAbsent(empty, emptyState -> {
         IdMapper<BlockState> mapper = new IdMapper(Block.BLOCK_STATE_REGISTRY.size());

         for (BlockState blockState : Block.BLOCK_STATE_REGISTRY) {
            mapper.addMapping(blockState, Block.BLOCK_STATE_REGISTRY.getId(blockState));
         }

         mapper.addMapping(EMPTY_STATE, Block.BLOCK_STATE_REGISTRY.getId(emptyState));
         mapper.addMapping(EMPTY_STATE, Block.BLOCK_STATE_REGISTRY.getId(EMPTY_STATE));
         return mapper;
      }));
   }

   public static Codec<PalettedContainer<BlockState>> getCodecForEmptyBlockState(BlockState empty) {
      return BLOCK_STATE_CODECS.computeIfAbsent(empty, emptyState -> {
         IdMap<BlockState> mapping = getIdMapForEmptyBlockState(emptyState);
         Codec<BlockState> blockStateCodec;
         if (emptyState == EMPTY_STATE) {
            blockStateCodec = BlockState.CODEC;
         } else {
            Function<BlockState, BlockState> mapFunction = blockState -> blockState == emptyState ? EMPTY_STATE : blockState;
            blockStateCodec = BlockState.CODEC.xmap(mapFunction, mapFunction);
         }

         return PalettedContainer.codecRW(mapping, blockStateCodec, Strategy.SECTION_STATES, EMPTY_STATE);
      });
   }

   public BlockBuffer() {
      this.values = new Long2ObjectOpenHashMap();
   }

   public BlockBuffer(Long2ObjectMap<PalettedContainer<BlockState>> values) {
      this.values = values;
   }

   public void saveRaw(FriendlyByteBuf friendlyByteBuf) {
      ObjectIterator var2 = this.entrySet().iterator();

      while (var2.hasNext()) {
         Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)var2.next();
         friendlyByteBuf.writeLong(entry.getLongKey());
         ((PalettedContainer)entry.getValue()).write(friendlyByteBuf);
         Short2ObjectMap<CompressedBlockEntity> blockEntities = (Short2ObjectMap<CompressedBlockEntity>)this.blockEntities.get(entry.getLongKey());
         if (blockEntities != null) {
            friendlyByteBuf.writeVarInt(blockEntities.size());
            ObjectIterator var5 = blockEntities.short2ObjectEntrySet().iterator();

            while (var5.hasNext()) {
               it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity> entry2 = (it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity>)var5.next();
               friendlyByteBuf.writeShort(entry2.getShortKey());
               ((CompressedBlockEntity)entry2.getValue()).write(friendlyByteBuf);
            }
         } else {
            friendlyByteBuf.writeVarInt(0);
         }
      }

      friendlyByteBuf.writeLong(PositionUtils.MIN_POSITION_LONG);
   }

   public static BlockBuffer loadRaw(FriendlyByteBuf friendlyByteBuf) {
      BlockBuffer buffer = new BlockBuffer();

      while (true) {
         long index = friendlyByteBuf.readLong();
         if (index == PositionUtils.MIN_POSITION_LONG) {
            return buffer;
         }

         PalettedContainer<BlockState> palettedContainer = buffer.getOrCreateSection(index);
         palettedContainer.read(friendlyByteBuf);
         int blockEntitySize = Math.min(4096, friendlyByteBuf.readVarInt());
         if (blockEntitySize > 0) {
            Short2ObjectMap<CompressedBlockEntity> map = new Short2ObjectOpenHashMap(blockEntitySize);

            for (int i = 0; i < blockEntitySize; i++) {
               short offset = friendlyByteBuf.readShort();
               CompressedBlockEntity blockEntity = CompressedBlockEntity.read(friendlyByteBuf);
               map.put(offset, blockEntity);
            }

            buffer.blockEntities.put(index, map);
         }
      }
   }

   public void saveNBT(FriendlyByteBuf friendlyByteBuf, BlockState emptyState) {
      ObjectIterator var3 = this.entrySet().iterator();

      while (var3.hasNext()) {
         Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)var3.next();
         friendlyByteBuf.writeLong(entry.getLongKey());
         Codec<PalettedContainer<BlockState>> codec = getCodecForEmptyBlockState(emptyState);
         CompoundTag encoded = (CompoundTag)VersionUtils.getOrThrow(codec.encodeStart(NbtOps.INSTANCE, (PalettedContainer)entry.getValue()));
         friendlyByteBuf.writeNbt(encoded);
         Short2ObjectMap<CompressedBlockEntity> blockEntities = (Short2ObjectMap<CompressedBlockEntity>)this.blockEntities.get(entry.getLongKey());
         if (blockEntities != null) {
            friendlyByteBuf.writeVarInt(blockEntities.size());
            ObjectIterator var8 = blockEntities.short2ObjectEntrySet().iterator();

            while (var8.hasNext()) {
               it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity> entry2 = (it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity>)var8.next();
               friendlyByteBuf.writeShort(entry2.getShortKey());
               ((CompressedBlockEntity)entry2.getValue()).write(friendlyByteBuf);
            }
         } else {
            friendlyByteBuf.writeVarInt(0);
         }
      }

      friendlyByteBuf.writeLong(PositionUtils.MIN_POSITION_LONG);
   }

   public static BlockBuffer loadNBT(FriendlyByteBuf friendlyByteBuf, int dataVersion, BlockState emptyState) {
      BlockBuffer buffer = new BlockBuffer();

      while (true) {
         long index = friendlyByteBuf.readLong();
         if (index == PositionUtils.MIN_POSITION_LONG) {
            return buffer;
         }

         CompoundTag tag = friendlyByteBuf.readNbt();
         if (tag != null && !tag.isEmpty()) {
            Codec<PalettedContainer<BlockState>> codec = getCodecForEmptyBlockState(emptyState);
            tag = DFUHelper.updatePalettedContainer(tag, dataVersion);
            PalettedContainer<BlockState> palettedContainer = VersionUtils.getOrThrow(codec.parse(NbtOps.INSTANCE, tag));
            buffer.putSection(index, palettedContainer);
         }

         int blockEntitySize = Math.min(4096, friendlyByteBuf.readVarInt());
         if (blockEntitySize > 0) {
            Short2ObjectMap<CompressedBlockEntity> map = new Short2ObjectOpenHashMap(blockEntitySize);

            for (int i = 0; i < blockEntitySize; i++) {
               short offset = friendlyByteBuf.readShort();
               CompressedBlockEntity blockEntity = CompressedBlockEntity.read(friendlyByteBuf);
               map.put(offset, blockEntity);
            }

            buffer.blockEntities.put(index, map);
         }
      }
   }

   public void putBlockEntity(int x, int y, int z, CompressedBlockEntity blockEntity) {
      long cpos = BlockPos.asLong(x >> 4, y >> 4, z >> 4);
      Short2ObjectMap<CompressedBlockEntity> chunkMap = (Short2ObjectMap<CompressedBlockEntity>)this.blockEntities
         .computeIfAbsent(cpos, k -> new Short2ObjectOpenHashMap());
      int key = x & 15 | (y & 15) << 4 | (z & 15) << 8;
      chunkMap.put((short)key, blockEntity);
   }

   @Nullable
   public CompressedBlockEntity getBlockEntity(int x, int y, int z) {
      long cpos = BlockPos.asLong(x >> 4, y >> 4, z >> 4);
      Short2ObjectMap<CompressedBlockEntity> chunkMap = (Short2ObjectMap<CompressedBlockEntity>)this.blockEntities.get(cpos);
      if (chunkMap == null) {
         return null;
      } else {
         int key = x & 15 | (y & 15) << 4 | (z & 15) << 8;
         return (CompressedBlockEntity)chunkMap.get((short)key);
      }
   }

   @Nullable
   public Short2ObjectMap<CompressedBlockEntity> getBlockEntityChunkMap(long cpos) {
      return (Short2ObjectMap<CompressedBlockEntity>)this.blockEntities.get(cpos);
   }

   public BlockState get(int x, int y, int z) {
      PalettedContainer<BlockState> container = this.getSectionForCoord(x, y, z);
      if (container == null) {
         return null;
      } else {
         BlockState state = (BlockState)container.get(x & 15, y & 15, z & 15);
         return state == EMPTY_STATE ? null : state;
      }
   }

   public int getSectionCount() {
      return this.values.size();
   }

   public void set(int x, int y, int z, BlockState state) {
      Objects.requireNonNull(state);
      PalettedContainer<BlockState> container = this.getOrCreateSectionForCoord(x, y, z);
      container.getAndSetUnchecked(x & 15, y & 15, z & 15, state);
   }

   public BlockState remove(int x, int y, int z) {
      PalettedContainer<BlockState> container = this.getSectionForCoord(x, y, z);
      if (container == null) {
         return null;
      } else {
         BlockState state = (BlockState)container.get(x & 15, y & 15, z & 15);
         if (state == EMPTY_STATE) {
            return null;
         } else {
            container.set(x & 15, y & 15, z & 15, EMPTY_STATE);
            return state;
         }
      }
   }

   public void forEach(PositionConsumer<BlockState> consumer) {
      ObjectIterator var2 = this.values.long2ObjectEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)var2.next();
         int cx = BlockPos.getX(entry.getLongKey()) * 16;
         int cy = BlockPos.getY(entry.getLongKey()) * 16;
         int cz = BlockPos.getZ(entry.getLongKey()) * 16;
         PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)entry.getValue();

         for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  BlockState block = (BlockState)container.get(x, y, z);
                  if (block != EMPTY_STATE) {
                     consumer.accept(cx + x, cy + y, cz + z, block);
                  }
               }
            }
         }
      }
   }

   public LongSet keySet() {
      return this.values.keySet();
   }

   public ObjectSet<Entry<PalettedContainer<BlockState>>> entrySet() {
      return this.values.long2ObjectEntrySet();
   }

   public void resetCachedLast() {
      this.lastId = PositionUtils.MIN_POSITION_LONG;
      this.last = null;
   }

   public PalettedContainer<BlockState> getSection(int cx, int cy, int cz) {
      long id = BlockPos.asLong(cx, cy, cz);
      if (id != this.lastId) {
         this.lastId = id;
         this.last = (PalettedContainer<BlockState>)this.values.get(id);
      }

      return this.last;
   }

   public PalettedContainer<BlockState> getSectionForCoord(int x, int y, int z) {
      return this.getSection(x >> 4, y >> 4, z >> 4);
   }

   public PalettedContainer<BlockState> getOrCreateSectionForCoord(int x, int y, int z) {
      long id = BlockPos.asLong(x >> 4, y >> 4, z >> 4);
      return this.getOrCreateSection(id);
   }

   public PalettedContainer<BlockState> getOrCreateSection(long id) {
      if (this.last == null || id != this.lastId) {
         this.lastId = id;
         this.last = (PalettedContainer<BlockState>)this.values
            .computeIfAbsent(id, k -> DFUHelper.createPalettedContainer(Block.BLOCK_STATE_REGISTRY, EMPTY_STATE));
      }

      return this.last;
   }

   public void putSection(long id, PalettedContainer<BlockState> palettedContainer) {
      if (this.last != null && id == this.lastId) {
         this.last = palettedContainer;
      }

      this.values.put(id, palettedContainer);
   }

   private static int pad8(int in) {
      return in + 7 & -8;
   }

   public int estimateSizeInRAM() {
      int size = 32;
      size += 64;
      int mapArrayLength = HashCommon.arraySize(this.values.size(), 0.75F);
      size += 16 + 8 * mapArrayLength;
      size += pad8(16 + 4 * mapArrayLength);
      size += 16;
      size += 16;
      ObjectIterator var3 = this.values.values().iterator();

      while (var3.hasNext()) {
         PalettedContainer<BlockState> palettedContainer = (PalettedContainer<BlockState>)var3.next();
         size += 32;
         size += 128;
         size += 24;
         size += 24;
         Data<BlockState> data = palettedContainer.data;
         BitStorage bitStorage = data.storage();
         if (bitStorage instanceof ZeroBitStorage) {
            size += 16;
         } else if (bitStorage instanceof SimpleBitStorage simpleBitStorage) {
            size += 48;
            size += 16 + 8 * simpleBitStorage.getRaw().length;
         }

         Palette<BlockState> palette = data.palette();
         if (palette instanceof SingleValuePalette) {
            size += 32;
         } else if (palette instanceof LinearPalette) {
            size += 32;
            size += 80;
         } else if (palette instanceof GlobalPalette) {
            size += 16;
         } else if (palette instanceof HashMapPalette<BlockState> hashMapPalette) {
            size += 32;
            size += 32;
            size += 3 * pad8(16 + 4 * (int)((1 << hashMapPalette.bits) / 0.8F));
         }
      }

      size += 64;
      mapArrayLength = HashCommon.arraySize(this.blockEntities.size(), 0.75F);
      size += 16 + 8 * mapArrayLength;
      size += pad8(16 + 4 * mapArrayLength);
      size += 16;
      size += 16;
      var3 = this.blockEntities.long2ObjectEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<Short2ObjectMap<CompressedBlockEntity>> entry = (Entry<Short2ObjectMap<CompressedBlockEntity>>)var3.next();
         size += 64;
         mapArrayLength = HashCommon.arraySize(((Short2ObjectMap)entry.getValue()).size(), 0.75F);
         size += 16 + 2 * mapArrayLength;
         size += pad8(16 + 4 * mapArrayLength);
         size += 16;
         size += 16;
         ObjectIterator var36 = ((Short2ObjectMap)entry.getValue()).short2ObjectEntrySet().iterator();

         while (var36.hasNext()) {
            it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity> entry2 = (it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity>)var36.next();
            size += 4;
            size = ++size + ((CompressedBlockEntity)entry2.getValue()).compressed().length;
         }
      }

      return size;
   }
}
