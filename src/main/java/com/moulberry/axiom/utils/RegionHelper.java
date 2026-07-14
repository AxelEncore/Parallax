package com.moulberry.axiom.utils;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ServerConfig;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.ClientBlockEntitySerializer;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiom.world_modification.undo.AdditionalUndoOperation;
import it.unimi.dsi.fastutil.longs.Long2LongFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class RegionHelper {
   public static void pushBlockRegionChange(ChunkedBlockRegion blockRegion, String historyDescription) {
      pushBlockRegionChange(blockRegion, historyDescription, 0);
   }

   public static void pushBlockRegionChange(ChunkedBlockRegion blockRegion, String historyDescription, int modifiers) {
      pushBlockRegionChange(blockRegion, null, historyDescription, modifiers);
   }

   public static void pushBlockRegionChange(
      ChunkedBlockRegion blockRegion, Long2ObjectMap<CompressedBlockEntity> blockEntities, String historyDescription, int modifiers
   ) {
      pushBlockRegionChange(blockRegion, blockEntities, historyDescription, modifiers, null);
   }

   public static void pushBlockRegionChange(
      ChunkedBlockRegion blockRegion,
      Long2ObjectMap<CompressedBlockEntity> blockEntities,
      String historyDescription,
      int modifiers,
      AdditionalUndoOperation additionalUndoOperation
   ) {
      if (!blockRegion.isEmpty()) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            BlockPos center = blockRegion.getCenter();
            LongSet needNbt = new LongOpenHashSet();
            LongSet needChunks = new LongOpenHashSet();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MutableBlockPos mutable = new MutableBlockPos();
            BlockBuffer forwards = new BlockBuffer();
            BlockBuffer backwards = new BlockBuffer();
            boolean keepExisting = (modifiers & HistoryEntry.MODIFIER_KEEP_EXISTING) != 0;
            if (keepExisting) {
               modifiers &= ~HistoryEntry.MODIFIER_KEEP_EXISTING;
            }

            int modifiersFinal = modifiers;
            Long2ObjectMap<PalettedContainer<BlockState>> containers = new Long2ObjectOpenHashMap();
            LongIterator longIterator = blockRegion.chunkKeySet().longIterator();

            while (longIterator.hasNext()) {
               long pos = longIterator.nextLong();
               int cx = BlockPos.getX(pos);
               int cy = BlockPos.getY(pos);
               int cz = BlockPos.getZ(pos);
               int sectionIndex = world.getSectionIndexFromSectionY(cy);
               if (sectionIndex >= 0 && sectionIndex < world.getSectionsCount()) {
                  LevelChunk chunk = (LevelChunk)world.getChunk(cx, cz, ChunkStatus.FULL, false);
                  if (chunk == null) {
                     needChunks.add(pos);
                  } else {
                     LevelChunkSection section = chunk.getSection(sectionIndex);
                     containers.put(pos, section.getStates());
                  }
               }
            }

            ServerConfig serverConfig = Axiom.getInstance().serverConfig;
            containers.forEach((cpos, container) -> {
               int cx = BlockPos.getX(cpos);
               int cy = BlockPos.getY(cpos);
               int czx = BlockPos.getZ(cpos);
               BlockState[] blocks = blockRegion.getChunk(cx, cy, czx);
               if (blocks != null) {
                  PalettedContainer<BlockState> forwardsContainer = forwards.getOrCreateSection(cpos);
                  PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);
                  int index = 0;

                  for (int zx = 0; zx < 16; zx++) {
                     for (int yx = 0; yx < 16; yx++) {
                        for (int xx = 0; xx < 16; xx++) {
                           BlockState pasteState = blocks[index++];
                           if (pasteState != null) {
                              BlockState existingState = (BlockState)container.get(xx, yx, zx);
                              if (!keepExisting || existingState.isAir()) {
                                 int toX = xx + cx * 16;
                                 int toY = yx + cy * 16;
                                 int toZ = zx + czx * 16;
                                 forwardsContainer.getAndSetUnchecked(xx, yx, zx, pasteState);
                                 backwardsContainer.getAndSetUnchecked(xx, yx, zx, existingState);
                                 if (serverConfig.blocksWithCustomData().contains(existingState.getBlock())) {
                                    needNbt.add(BlockPos.asLong(toX, toY, toZ));
                                 } else if (existingState.hasBlockEntity()) {
                                    BlockEntity blockEntity = world.getBlockEntity(mutable.set(toX, toY, toZ));
                                    if (blockEntity != null) {
                                       CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
                                       if (nbt != null) {
                                          if (!nbt.isEmpty()) {
                                             CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);
                                             backwards.putBlockEntity(toX, toY, toZ, compressedBlockEntity);
                                          }
                                       } else {
                                          needNbt.add(mutable.asLong());
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            });
            if (needNbt.isEmpty() && needChunks.isEmpty()) {
               if (blockEntities != null) {
                  ObjectIterator var26 = blockEntities.long2ObjectEntrySet().iterator();

                  while (var26.hasNext()) {
                     Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var26.next();
                     long pos = entry.getLongKey();
                     int x = BlockPos.getX(pos);
                     int y = BlockPos.getY(pos);
                     int z = BlockPos.getZ(pos);
                     if (forwards.get(x, y, z) != BlockBuffer.EMPTY_STATE) {
                        forwards.putBlockEntity(x, y, z, (CompressedBlockEntity)entry.getValue());
                     }
                  }
               }

               Dispatcher.push(new HistoryEntry<>(forwards, backwards, center, historyDescription, modifiers, additionalUndoOperation));
            } else {
               Position2ObjectMap<BlockState> copiedBlocks = blockRegion.copyBlockData();
               Dispatcher.requestChunkData(needNbt, needChunks, true, (compressedBlockEntities, chunkSections) -> {
                  BlockState air = Blocks.AIR.defaultBlockState();
                  Long2ObjectMap<CompressedBlockEntity> copiedBlockEntities = blockEntities == null ? null : new Long2ObjectOpenHashMap(blockEntities);
                  chunkSections.forEach((cpos, container) -> {
                     int cx = BlockPos.getX(cpos);
                     int cy = BlockPos.getY(cpos);
                     int czx = BlockPos.getZ(cpos);
                     BlockState[] blocks = copiedBlocks.getChunk(cx, cy, czx);
                     if (blocks != null) {
                        PalettedContainer<BlockState> forwardsContainer = forwards.getOrCreateSection(cpos);
                        PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);
                        int index = 0;

                        for (int zxx = 0; zxx < 16; zxx++) {
                           for (int yxx = 0; yxx < 16; yxx++) {
                              for (int xxx = 0; xxx < 16; xxx++) {
                                 BlockState pasteState = blocks[index++];
                                 if (pasteState != null) {
                                    BlockState existingState = container == null ? air : (BlockState)container.get(xxx, yxx, zxx);
                                    if (!keepExisting || existingState.isAir()) {
                                       forwardsContainer.getAndSetUnchecked(xxx, yxx, zxx, pasteState);
                                       backwardsContainer.getAndSetUnchecked(xxx, yxx, zxx, existingState);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  });
                  compressedBlockEntities.forEach((posxx, compressedBlockEntity) -> {
                     int xxx = BlockPos.getX(posxx);
                     int yxx = BlockPos.getY(posxx);
                     int zxx = BlockPos.getZ(posxx);
                     if (backwards.get(xxx, yxx, zxx) != BlockBuffer.EMPTY_STATE) {
                        backwards.putBlockEntity(xxx, yxx, zxx, compressedBlockEntity);
                     }
                  });
                  if (copiedBlockEntities != null) {
                     ObjectIterator var13x = copiedBlockEntities.long2ObjectEntrySet().iterator();

                     while (var13x.hasNext()) {
                        Entry<CompressedBlockEntity> entryx = (Entry<CompressedBlockEntity>)var13x.next();
                        long posx = entryx.getLongKey();
                        int xx = BlockPos.getX(posx);
                        int yx = BlockPos.getY(posx);
                        int zx = BlockPos.getZ(posx);
                        if (forwards.get(xx, yx, zx) != BlockBuffer.EMPTY_STATE) {
                           forwards.putBlockEntity(xx, yx, zx, (CompressedBlockEntity)entryx.getValue());
                        }
                     }
                  }

                  Dispatcher.push(new HistoryEntry<>(forwards, backwards, center, historyDescription, modifiersFinal, additionalUndoOperation));
               });
            }
         }
      }
   }

   public static void pushBlockBufferChange(BlockBuffer forwards, BlockPos center, String historyDescription, AdditionalUndoOperation additionalUndoOperation) {
      pushBlockBufferChange(forwards, center, historyDescription, 0, additionalUndoOperation);
   }

   public static void pushBlockBufferChange(
      BlockBuffer forwards, BlockPos center, String historyDescription, int modifiers, AdditionalUndoOperation additionalUndoOperation
   ) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         LongSet needNbt = new LongOpenHashSet();
         LongSet needChunks = new LongOpenHashSet();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         MutableBlockPos mutable = new MutableBlockPos();
         BlockBuffer backwards = new BlockBuffer();
         Long2ObjectMap<PalettedContainer<BlockState>> containers = new Long2ObjectOpenHashMap();
         LongIterator longIterator = forwards.keySet().longIterator();

         while (longIterator.hasNext()) {
            long pos = longIterator.nextLong();
            int cx = BlockPos.getX(pos);
            int cy = BlockPos.getY(pos);
            int cz = BlockPos.getZ(pos);
            int sectionIndex = world.getSectionIndexFromSectionY(cy);
            if (sectionIndex >= 0 && sectionIndex < world.getSectionsCount()) {
               LevelChunk chunk = (LevelChunk)world.getChunk(cx, cz, ChunkStatus.FULL, false);
               if (chunk == null) {
                  needChunks.add(pos);
               } else {
                  LevelChunkSection section = chunk.getSection(sectionIndex);
                  containers.put(pos, section.getStates());
               }
            }
         }

         ServerConfig serverConfig = Axiom.getInstance().serverConfig;
         containers.forEach((cpos, container) -> {
            int cxx = BlockPos.getX(cpos);
            int cyx = BlockPos.getY(cpos);
            int czx = BlockPos.getZ(cpos);
            PalettedContainer<BlockState> blocks = forwards.getSection(cxx, cyx, czx);
            if (blocks != null) {
               PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);

               for (int z = 0; z < 16; z++) {
                  for (int y = 0; y < 16; y++) {
                     for (int x = 0; x < 16; x++) {
                        BlockState pasteState = (BlockState)blocks.get(x, y, z);
                        if (pasteState != BlockBuffer.EMPTY_STATE) {
                           BlockState existingState = (BlockState)container.get(x, y, z);
                           int toX = x + cxx * 16;
                           int toY = y + cyx * 16;
                           int toZ = z + czx * 16;
                           backwardsContainer.getAndSetUnchecked(x, y, z, existingState);
                           if (serverConfig.blocksWithCustomData().contains(existingState.getBlock())) {
                              needNbt.add(BlockPos.asLong(toX, toY, toZ));
                           } else if (existingState.hasBlockEntity()) {
                              BlockEntity blockEntity = world.getBlockEntity(mutable.set(toX, toY, toZ));
                              if (blockEntity != null) {
                                 CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
                                 if (nbt != null) {
                                    if (!nbt.isEmpty()) {
                                       CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);
                                       backwards.putBlockEntity(toX, toY, toZ, compressedBlockEntity);
                                    }
                                 } else {
                                    needNbt.add(mutable.asLong());
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         });
         if (needNbt.isEmpty() && needChunks.isEmpty()) {
            Dispatcher.push(new HistoryEntry<>(forwards, backwards, center, historyDescription, modifiers, additionalUndoOperation));
         } else {
            Dispatcher.requestChunkData(needNbt, needChunks, true, (compressedBlockEntities, chunkSections) -> {
               BlockState air = Blocks.AIR.defaultBlockState();
               chunkSections.forEach((cpos, container) -> {
                  int cxx = BlockPos.getX(cpos);
                  int cyx = BlockPos.getY(cpos);
                  int czx = BlockPos.getZ(cpos);
                  PalettedContainer<BlockState> blocks = forwards.getSection(cxx, cyx, czx);
                  if (blocks != null) {
                     PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);

                     for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 16; y++) {
                           for (int x = 0; x < 16; x++) {
                              BlockState pasteState = (BlockState)blocks.get(x, y, z);
                              if (pasteState != BlockBuffer.EMPTY_STATE) {
                                 BlockState existingState = container == null ? air : (BlockState)container.get(x, y, z);
                                 backwardsContainer.getAndSetUnchecked(x, y, z, existingState);
                              }
                           }
                        }
                     }
                  }
               });
               compressedBlockEntities.forEach((pos, compressedBlockEntity) -> {
                  int x = BlockPos.getX(pos);
                  int y = BlockPos.getY(pos);
                  int z = BlockPos.getZ(pos);
                  if (backwards.get(x, y, z) != BlockBuffer.EMPTY_STATE) {
                     backwards.putBlockEntity(x, y, z, compressedBlockEntity);
                  }
               });
               Dispatcher.push(new HistoryEntry<>(forwards, backwards, center, historyDescription, modifiers, additionalUndoOperation));
            });
         }
      }
   }

   public static void pushBlockRegionChangeOffset(ChunkedBlockRegion blockRegion, BlockPos offset, String historyDescription, int modifiers) {
      pushBlockRegionChangeOffset(blockRegion, null, offset, historyDescription, modifiers);
   }

   public static void pushBlockRegionChangeOffset(
      ChunkedBlockRegion blockRegion, Long2ObjectMap<CompressedBlockEntity> blockEntities, BlockPos offset, String historyDescription, int modifiers
   ) {
      pushBlockRegionChangeOffset(blockRegion, blockEntities, offset, historyDescription, modifiers, null);
   }

   public static void pushBlockRegionChangeOffset(
      ChunkedBlockRegion blockRegion,
      Long2ObjectMap<CompressedBlockEntity> blockEntities,
      BlockPos offset,
      String historyDescription,
      int modifiers,
      AdditionalUndoOperation additionalUndoOperation
   ) {
      if (!blockRegion.isEmpty()) {
         if (offset.getX() == 0 && offset.getY() == 0 && offset.getZ() == 0) {
            pushBlockRegionChange(blockRegion, blockEntities, historyDescription, modifiers, additionalUndoOperation);
         } else {
            ClientLevel world = Minecraft.getInstance().level;
            if (world != null) {
               BlockPos center = blockRegion.getCenter();
               int minX = blockRegion.min().getX();
               int minY = blockRegion.min().getY();
               int minZ = blockRegion.min().getZ();
               int maxX = blockRegion.max().getX();
               int maxY = blockRegion.max().getY();
               int maxZ = blockRegion.max().getZ();
               int worldMinX = minX + offset.getX();
               int worldMinY = minY + offset.getY();
               int worldMinZ = minZ + offset.getZ();
               int worldMaxX = maxX + offset.getX();
               int worldMaxY = maxY + offset.getY();
               int worldMaxZ = maxZ + offset.getZ();
               LongSet needNbt = new LongOpenHashSet();
               LongSet needChunks = new LongOpenHashSet();
               ByteArrayOutputStream baos = new ByteArrayOutputStream();
               MutableBlockPos mutable = new MutableBlockPos();
               BlockBuffer forwards = new BlockBuffer();
               BlockBuffer backwards = new BlockBuffer();
               boolean keepExisting = (modifiers & HistoryEntry.MODIFIER_KEEP_EXISTING) != 0;
               if (keepExisting) {
                  modifiers &= ~HistoryEntry.MODIFIER_KEEP_EXISTING;
               }

               int modifiersFinal = modifiers;
               Long2ObjectMap<PalettedContainer<BlockState>> containers = new Long2ObjectOpenHashMap();
               int chunkOffsetX = offset.getX() >> 4;
               int chunkOffsetY = offset.getY() >> 4;
               int chunkOffsetZ = offset.getZ() >> 4;
               LongIterator longIterator = blockRegion.chunkKeySet().longIterator();

               while (longIterator.hasNext()) {
                  long regionPos = longIterator.nextLong();
                  int cx = BlockPos.getX(regionPos) + chunkOffsetX;
                  int cy = BlockPos.getY(regionPos) + chunkOffsetY;
                  int cz = BlockPos.getZ(regionPos) + chunkOffsetZ;

                  for (int cxo = 0; cxo <= 1; cxo++) {
                     for (int cyo = 0; cyo <= 1; cyo++) {
                        for (int czo = 0; czo <= 1; czo++) {
                           int ncx = cx + cxo;
                           int ncy = cy + cyo;
                           int ncz = cz + czo;
                           long chunkPos = BlockPos.asLong(ncx, ncy, ncz);
                           if (!containers.containsKey(chunkPos)
                              && !needChunks.contains(chunkPos)
                              && ncx * 16 + 15 >= worldMinX
                              && ncy * 16 + 15 >= worldMinY
                              && ncz * 16 + 15 >= worldMinZ
                              && ncx * 16 <= worldMaxX
                              && ncy * 16 <= worldMaxY
                              && ncz * 16 <= worldMaxZ) {
                              int sectionIndex = world.getSectionIndexFromSectionY(ncy);
                              if (sectionIndex >= 0 && sectionIndex < world.getSectionsCount()) {
                                 LevelChunk chunk = (LevelChunk)world.getChunk(ncx, ncz, ChunkStatus.FULL, false);
                                 if (chunk == null) {
                                    needChunks.add(chunkPos);
                                 } else {
                                    LevelChunkSection section = chunk.getSection(sectionIndex);
                                    containers.put(chunkPos, section.getStates());
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               ServerConfig serverConfig = Axiom.getInstance().serverConfig;
               containers.forEach((cpos, container) -> {
                  int cx = BlockPos.getX(cpos);
                  int cy = BlockPos.getY(cpos);
                  int czx = BlockPos.getZ(cpos);
                  int xo = cx * 16 - offset.getX();
                  int yo = cy * 16 - offset.getY();
                  int zo = czx * 16 - offset.getZ();
                  PalettedContainer<BlockState> forwardsContainer = forwards.getOrCreateSection(cpos);
                  PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);

                  for (int zx = 0; zx < 16; zx++) {
                     for (int yx = 0; yx < 16; yx++) {
                        for (int xx = 0; xx < 16; xx++) {
                           BlockState pasteState = blockRegion.getBlockStateOrNull(xx + xo, yx + yo, zx + zo);
                           if (pasteState != null) {
                              BlockState existingState = (BlockState)container.get(xx, yx, zx);
                              if (!keepExisting || existingState.isAir()) {
                                 int toX = xx + cx * 16;
                                 int toY = yx + cy * 16;
                                 int toZ = zx + czx * 16;
                                 forwardsContainer.getAndSetUnchecked(xx, yx, zx, pasteState);
                                 backwardsContainer.getAndSetUnchecked(xx, yx, zx, existingState);
                                 if (serverConfig.blocksWithCustomData().contains(existingState.getBlock())) {
                                    needNbt.add(BlockPos.asLong(toX, toY, toZ));
                                 } else if (existingState.hasBlockEntity()) {
                                    BlockEntity blockEntity = world.getBlockEntity(mutable.set(toX, toY, toZ));
                                    if (blockEntity != null) {
                                       CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
                                       if (nbt != null) {
                                          if (!nbt.isEmpty()) {
                                             CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);
                                             backwards.putBlockEntity(toX, toY, toZ, compressedBlockEntity);
                                          }
                                       } else {
                                          needNbt.add(mutable.asLong());
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               });
               if (needNbt.isEmpty() && needChunks.isEmpty()) {
                  if (blockEntities != null) {
                     ObjectIterator var50 = blockEntities.long2ObjectEntrySet().iterator();

                     while (var50.hasNext()) {
                        Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var50.next();
                        long pos = entry.getLongKey();
                        int x = BlockPos.getX(pos);
                        int y = BlockPos.getY(pos);
                        int z = BlockPos.getZ(pos);
                        if (forwards.get(x + offset.getX(), y + offset.getY(), z + offset.getZ()) != BlockBuffer.EMPTY_STATE) {
                           forwards.putBlockEntity(x + offset.getX(), y + offset.getY(), z + offset.getZ(), (CompressedBlockEntity)entry.getValue());
                        }
                     }
                  }

                  Dispatcher.push(new HistoryEntry<>(forwards, backwards, center.offset(offset), historyDescription, modifiers, additionalUndoOperation));
               } else {
                  Position2ObjectMap<BlockState> copiedBlocks = blockRegion.copyBlockData();
                  Long2ObjectMap<CompressedBlockEntity> copiedBlockEntities = blockEntities == null ? null : new Long2ObjectOpenHashMap(blockEntities);
                  Dispatcher.requestChunkData(
                     needNbt,
                     needChunks,
                     true,
                     (compressedBlockEntities, chunkSections) -> {
                        BlockState air = Blocks.AIR.defaultBlockState();
                        chunkSections.forEach((cpos, container) -> {
                           int cx = BlockPos.getX(cpos);
                           int cy = BlockPos.getY(cpos);
                           int czx = BlockPos.getZ(cpos);
                           int xo = cx * 16 - offset.getX();
                           int yo = cy * 16 - offset.getY();
                           int zo = czx * 16 - offset.getZ();
                           PalettedContainer<BlockState> forwardsContainer = forwards.getOrCreateSection(cpos);
                           PalettedContainer<BlockState> backwardsContainer = backwards.getOrCreateSection(cpos);

                           for (int zxx = 0; zxx < 16; zxx++) {
                              for (int yxx = 0; yxx < 16; yxx++) {
                                 for (int xxx = 0; xxx < 16; xxx++) {
                                    BlockState pasteState = copiedBlocks.get(xxx + xo, yxx + yo, zxx + zo);
                                    if (pasteState != null) {
                                       BlockState existingState = container == null ? air : (BlockState)container.get(xxx, yxx, zxx);
                                       if (!keepExisting || existingState.isAir()) {
                                          forwardsContainer.getAndSetUnchecked(xxx, yxx, zxx, pasteState);
                                          backwardsContainer.getAndSetUnchecked(xxx, yxx, zxx, existingState);
                                       }
                                    }
                                 }
                              }
                           }
                        });
                        compressedBlockEntities.forEach((posxx, compressedBlockEntity) -> {
                           int xxx = BlockPos.getX(posxx);
                           int yxx = BlockPos.getY(posxx);
                           int zxx = BlockPos.getZ(posxx);
                           if (backwards.get(xxx, yxx, zxx) != BlockBuffer.EMPTY_STATE) {
                              backwards.putBlockEntity(xxx, yxx, zxx, compressedBlockEntity);
                           }
                        });
                        if (copiedBlockEntities != null) {
                           ObjectIterator var13x = copiedBlockEntities.long2ObjectEntrySet().iterator();

                           while (var13x.hasNext()) {
                              Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var13x.next();
                              long posx = entry.getLongKey();
                              int xx = BlockPos.getX(posx);
                              int yx = BlockPos.getY(posx);
                              int zx = BlockPos.getZ(posx);
                              if (forwards.get(xx + offset.getX(), yx + offset.getY(), zx + offset.getZ()) != BlockBuffer.EMPTY_STATE) {
                                 forwards.putBlockEntity(xx + offset.getX(), yx + offset.getY(), zx + offset.getZ(), (CompressedBlockEntity)entry.getValue());
                              }
                           }
                        }

                        Dispatcher.push(
                           new HistoryEntry<>(forwards, backwards, center.offset(offset), historyDescription, modifiersFinal, additionalUndoOperation)
                        );
                     }
                  );
               }
            }
         }
      }
   }

   public static void pushBlockRegionChangeWithNBT(
      ChunkedBlockRegion blockRegion, String historyDescription, int modifiers, Long2LongFunction nbtLocationFunction
   ) {
      pushBlockRegionChangeWithNBT(blockRegion, historyDescription, modifiers, nbtLocationFunction, null);
   }

   public static void pushBlockRegionChangeWithNBT(
      ChunkedBlockRegion blockRegion,
      String historyDescription,
      int modifiers,
      Long2LongFunction nbtLocationFunction,
      AdditionalUndoOperation additionalUndoOperation
   ) {
      if (!blockRegion.isEmpty()) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            BlockPos center = blockRegion.getCenter();
            BlockBuffer setOperation = new BlockBuffer();
            BlockBuffer previousBlocksForUndo = new BlockBuffer();
            Long2ObjectMap<List<RegionHelper.NbtTarget>> nbtMap = new Long2ObjectOpenHashMap();
            boolean keepExisting = (modifiers & HistoryEntry.MODIFIER_KEEP_EXISTING) != 0;
            if (keepExisting) {
               modifiers &= ~HistoryEntry.MODIFIER_KEEP_EXISTING;
            }

            blockRegion.forEachChunk(
               (cx, cy, cz, data) -> {
                  if (cy >= world.getMinSection() && cy <= world.getMaxSection() - 1) {
                     LevelChunk chunk = (LevelChunk)world.getChunk(cx, cz, ChunkStatus.FULL, false);
                     if (chunk != null) {
                        LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(cy));
                        PalettedContainer<BlockState> container = section.getStates();
                        int wcx = cx * 16;
                        int wcy = cy * 16;
                        int wcz = cz * 16;
                        int index = 0;

                        for (int z = 0; z < 16; z++) {
                           for (int y = 0; y < 16; y++) {
                              for (int x = 0; x < 16; x++) {
                                 BlockState state = data[index++];
                                 if (state != null) {
                                    BlockState oldState = (BlockState)container.get(x, y, z);
                                    if (!keepExisting || oldState.isAir()) {
                                       setOperation.set(wcx + x, wcy + y, wcz + z, state);
                                       previousBlocksForUndo.set(wcx + x, wcy + y, wcz + z, oldState);
                                       long pos = BlockPos.asLong(wcx + x, wcy + y, wcz + z);
                                       if (oldState.hasBlockEntity()) {
                                          ((List)nbtMap.computeIfAbsent(pos, k -> new ArrayList()))
                                             .add(new RegionHelper.NbtTarget(false, wcx + x, wcy + y, wcz + z));
                                       }

                                       if (state.hasBlockEntity() && nbtLocationFunction != null) {
                                          long fromPos = nbtLocationFunction.applyAsLong(pos);
                                          ((List)nbtMap.computeIfAbsent(fromPos, k -> new ArrayList()))
                                             .add(new RegionHelper.NbtTarget(true, wcx + x, wcy + y, wcz + z));
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            );
            pushBlockBufferChangeWithNBT(
               world, setOperation, previousBlocksForUndo, center, historyDescription, modifiers, nbtMap, true, additionalUndoOperation
            );
         }
      }
   }

   public static void pushBlockBufferChangeWithNBT(
      Level world,
      BlockBuffer forwards,
      BlockBuffer backwards,
      BlockPos center,
      String historyDescription,
      int modifiers,
      Long2ObjectMap<List<RegionHelper.NbtTarget>> nbtMap,
      boolean tryResolve
   ) {
      pushBlockBufferChangeWithNBT(world, forwards, backwards, center, historyDescription, modifiers, nbtMap, tryResolve, null);
   }

   public static void pushBlockBufferChangeWithNBT(
      Level world,
      BlockBuffer forwards,
      BlockBuffer backwards,
      BlockPos center,
      String historyDescription,
      int modifiers,
      Long2ObjectMap<List<RegionHelper.NbtTarget>> nbtMap,
      boolean tryResolve,
      AdditionalUndoOperation additionalUndoOperation
   ) {
      if (tryResolve) {
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ObjectIterator<Entry<List<RegionHelper.NbtTarget>>> iterator = nbtMap.long2ObjectEntrySet().iterator();

         while (iterator.hasNext()) {
            Entry<List<RegionHelper.NbtTarget>> element = (Entry<List<RegionHelper.NbtTarget>>)iterator.next();
            long pos = element.getLongKey();
            mutableBlockPos.set(pos);
            BlockEntity blockEntity = world.getChunkAt(mutableBlockPos).getBlockEntity(mutableBlockPos, EntityCreationType.CHECK);
            if (blockEntity != null) {
               CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
               if (nbt != null) {
                  if (!nbt.isEmpty()) {
                     CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);

                     for (RegionHelper.NbtTarget target : (List<RegionHelper.NbtTarget>)element.getValue()) {
                        if (target.forwards) {
                           if (forwards.get(target.x, target.y, target.z) != BlockBuffer.EMPTY_STATE) {
                              forwards.putBlockEntity(target.x, target.y, target.z, compressedBlockEntity);
                           }
                        } else if (backwards.get(target.x, target.y, target.z) != BlockBuffer.EMPTY_STATE) {
                           backwards.putBlockEntity(target.x, target.y, target.z, compressedBlockEntity);
                        }
                     }
                  }

                  iterator.remove();
               }
            }
         }
      }

      if (nbtMap.isEmpty()) {
         Dispatcher.push(new HistoryEntry<>(forwards, backwards, center, historyDescription, modifiers, additionalUndoOperation));
      } else {
         Dispatcher.requestChunkData(nbtMap.keySet(), null, false, (compressedBlockEntities, chunkSections) -> {
            try {
               ObjectIterator e = compressedBlockEntities.long2ObjectEntrySet().iterator();

               while (e.hasNext()) {
                  Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)e.next();
                  List<RegionHelper.NbtTarget> targets = (List<RegionHelper.NbtTarget>)nbtMap.get(entry.getLongKey());
                  if (targets != null) {
                     for (RegionHelper.NbtTarget targetx : targets) {
                        if (targetx.forwards) {
                           if (forwards.get(targetx.x, targetx.y, targetx.z) != BlockBuffer.EMPTY_STATE) {
                              forwards.putBlockEntity(targetx.x, targetx.y, targetx.z, (CompressedBlockEntity)entry.getValue());
                           }
                        } else if (backwards.get(targetx.x, targetx.y, targetx.z) != BlockBuffer.EMPTY_STATE) {
                           backwards.putBlockEntity(targetx.x, targetx.y, targetx.z, (CompressedBlockEntity)entry.getValue());
                        }
                     }
                  }
               }

               Dispatcher.push(new HistoryEntry<>(forwards, backwards, center, historyDescription, modifiers, additionalUndoOperation));
            } catch (Exception var14) {
               var14.printStackTrace();
            }
         });
      }
   }

   public static void pushBooleanRegionChange(ChunkedBooleanRegion booleanRegion, BlockState fillState, String historyDescription) {
      pushPositionSetRegionChange(booleanRegion.unsafeGetPositionSet(), fillState, booleanRegion.getCenter(), historyDescription, 0);
   }

   public static void pushPositionSetRegionChange(PositionSet positionSet, BlockState fillState, BlockPos center, String historyDescription, int modifiers) {
      if (positionSet.count() > 0) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Long2ObjectMap<List<RegionHelper.NbtTarget>> nbtMap = new Long2ObjectOpenHashMap();
            BlockBuffer forwards = new BlockBuffer();
            BlockBuffer backwards = new BlockBuffer();
            boolean keepExisting = (modifiers & HistoryEntry.MODIFIER_KEEP_EXISTING) != 0;
            if (keepExisting) {
               modifiers &= ~HistoryEntry.MODIFIER_KEEP_EXISTING;
            }

            positionSet.forEachChunk(
               (cx, cy, cz, data) -> {
                  if (cy >= world.getMinSection() && cy <= world.getMaxSection() - 1) {
                     LevelChunk chunk = (LevelChunk)world.getChunk(cx, cz, ChunkStatus.FULL, false);
                     if (chunk != null) {
                        LevelChunkSection section = chunk.getSection(world.getSectionIndexFromSectionY(cy));
                        PalettedContainer<BlockState> container = section.getStates();
                        int wcx = cx * 16;
                        int wcy = cy * 16;
                        int wcz = cz * 16;
                        int index = 0;

                        for (int z = 0; z < 16; z++) {
                           for (int y = 0; y < 16; y++) {
                              short line = data[index++];
                              if (line != 0) {
                                 for (int x = 0; x < 16; x++) {
                                    if ((line & 1 << x) != 0) {
                                       BlockState oldState = (BlockState)container.get(x, y, z);
                                       if (!keepExisting || oldState.isAir()) {
                                          forwards.set(wcx + x, wcy + y, wcz + z, fillState);
                                          backwards.set(wcx + x, wcy + y, wcz + z, oldState);
                                          if (oldState.hasBlockEntity()) {
                                             mutableBlockPos.set(wcx + x, wcy + y, wcz + z);
                                             BlockEntity blockEntity = chunk.getBlockEntity(mutableBlockPos, EntityCreationType.CHECK);
                                             if (blockEntity != null) {
                                                CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
                                                if (nbt != null) {
                                                   if (!nbt.isEmpty()) {
                                                      CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);
                                                      backwards.putBlockEntity(wcx + x, wcy + y, wcz + z, compressedBlockEntity);
                                                   }
                                                } else {
                                                   ((List)nbtMap.computeIfAbsent(mutableBlockPos.asLong(), k -> new ArrayList()))
                                                      .add(new RegionHelper.NbtTarget(false, wcx + x, wcy + y, wcz + z));
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
                  }
               }
            );
            pushBlockBufferChangeWithNBT(world, forwards, backwards, center, historyDescription, modifiers, nbtMap, false);
         }
      }
   }

   public static void pushPositionSetRegionChangeWithNBT(
      PositionSet positionSet, BlockState fillState, BlockPos center, String historyDescription, int modifiers, Long2LongFunction nbtLocationFunction
   ) {
      if (positionSet.count() > 0) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            BlockBuffer setOperation = new BlockBuffer();
            BlockBuffer previousBlocksForUndo = new BlockBuffer();
            Long2ObjectMap<List<RegionHelper.NbtTarget>> nbtMap = new Long2ObjectOpenHashMap();
            positionSet.forEachChunk(
               (cx, cy, cz, data) -> {
                  if (cy >= world.getMinSection() && cy <= world.getMaxSection() - 1) {
                     LevelChunk chunk = (LevelChunk)world.getChunk(cx, cz, ChunkStatus.FULL, false);
                     if (chunk != null) {
                        LevelChunkSection section = chunk.getSection(world.getSectionIndexFromSectionY(cy));
                        PalettedContainer<BlockState> container = section.getStates();
                        int wcx = cx * 16;
                        int wcy = cy * 16;
                        int wcz = cz * 16;
                        int index = 0;

                        for (int z = 0; z < 16; z++) {
                           for (int y = 0; y < 16; y++) {
                              short line = data[index++];
                              if (line != 0) {
                                 for (int x = 0; x < 16; x++) {
                                    if ((line & 1 << x) != 0) {
                                       BlockState oldState = (BlockState)container.get(x, y, z);
                                       setOperation.set(wcx + x, wcy + y, wcz + z, fillState);
                                       previousBlocksForUndo.set(wcx + x, wcy + y, wcz + z, oldState);
                                       long pos = BlockPos.asLong(wcx + x, wcy + y, wcz + z);
                                       if (oldState.hasBlockEntity()) {
                                          ((List)nbtMap.computeIfAbsent(pos, k -> new ArrayList()))
                                             .add(new RegionHelper.NbtTarget(false, wcx + x, wcy + y, wcz + z));
                                       }

                                       if (fillState.hasBlockEntity() && nbtLocationFunction != null) {
                                          long fromPos = nbtLocationFunction.applyAsLong(pos);
                                          ((List)nbtMap.computeIfAbsent(fromPos, k -> new ArrayList()))
                                             .add(new RegionHelper.NbtTarget(true, wcx + x, wcy + y, wcz + z));
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            );
            pushBlockBufferChangeWithNBT(world, setOperation, previousBlocksForUndo, center, historyDescription, modifiers, nbtMap, true);
         }
      }
   }

   public record NbtTarget(boolean forwards, int x, int y, int z) {
   }
}
