package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.hooks.ServerLevelExt;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.SerializationUtils;
import com.moulberry.axiom.world_modification.BiomeBuffer;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.BlockOrBiomeBuffer;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.SkipPacketException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import org.jetbrains.annotations.Nullable;

public class AxiomServerboundSetBuffer implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_buffer");
   private final ResourceKey<Level> world;
   private final BlockOrBiomeBuffer buffer;
   private final int clientAvailableDispatchSends;
   private final FriendlyByteBuf rawByteBuf;
   private final int rawByteBufReaderIndex;
   private static final TicketType<ChunkPos> CHUNK_UPDATE = TicketType.create("axiom_chunk_update", Comparator.comparingLong(ChunkPos::toLong), 5);

   public AxiomServerboundSetBuffer(ResourceKey<Level> world, BlockOrBiomeBuffer buffer, int clientAvailableDispatchSends) {
      this.world = world;
      this.buffer = buffer;
      this.clientAvailableDispatchSends = clientAvailableDispatchSends;
      this.rawByteBuf = null;
      this.rawByteBufReaderIndex = 0;
   }

   public AxiomServerboundSetBuffer(FriendlyByteBuf friendlyByteBuf) {
      this.world = null;
      this.buffer = null;
      this.clientAvailableDispatchSends = Integer.MAX_VALUE;
      this.rawByteBuf = friendlyByteBuf;
      this.rawByteBufReaderIndex = friendlyByteBuf.readerIndex();
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   public static AxiomServerboundSetBuffer read(FriendlyByteBuf friendlyByteBuf) {
      ResourceKey<Level> world = friendlyByteBuf.readResourceKey(Registries.DIMENSION);
      friendlyByteBuf.readUUID();
      byte type = friendlyByteBuf.readByte();
      BlockOrBiomeBuffer buffer;
      if (type == 0) {
         buffer = BlockBuffer.loadRaw(friendlyByteBuf);
      } else {
         if (type != 1) {
            throw new RuntimeException("Unknown buffer type: " + type);
         }

         buffer = BiomeBuffer.load(friendlyByteBuf);
      }

      int clientAvailableDispatchSends = friendlyByteBuf.readVarInt();
      return new AxiomServerboundSetBuffer(world, buffer, clientAvailableDispatchSends);
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      if (this.rawByteBuf != null) {
         this.rawByteBuf.readerIndex(this.rawByteBufReaderIndex);
         friendlyByteBuf.writeBytes(this.rawByteBuf);
      } else {
         throw new SkipPacketException(new RuntimeException("Not a serializable AxiomServerboundSetBuffer"));
      }
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.BUILD_SECTION)) {
         ServerLevel level = server.getLevel(this.world);
         if (level != null) {
            if (this.buffer instanceof BlockBuffer blockBuffer) {
               if (!AxiomServer.consumeDispatchSends(player, blockBuffer.getSectionCount(), this.clientAvailableDispatchSends)) {
                  return;
               }

               applyBlockBufferServer(blockBuffer, level, null, player);
            } else {
               if (!(this.buffer instanceof BiomeBuffer biomeBuffer)) {
                  throw new RuntimeException("Unknown buffer type: " + this.buffer.getClass());
               }

               if (!AxiomServer.consumeDispatchSends(player, biomeBuffer.map.map.size(), this.clientAvailableDispatchSends)) {
                  return;
               }

               applyBiomeBufferServer(biomeBuffer, level);
            }
         }
      }
   }

   public static Comparator<Entry<PalettedContainer<BlockState>>> createBlockBufferOrderComparator(int sortChunkX, int sortChunkZ) {
      return (e1, e2) -> {
         long pos1 = e1.getLongKey();
         long pos2 = e2.getLongKey();
         int pos1X = BlockPos.getX(pos1);
         int pos1Y = BlockPos.getY(pos1);
         int pos1Z = BlockPos.getZ(pos1);
         int pos2X = BlockPos.getX(pos2);
         int pos2Y = BlockPos.getY(pos2);
         int pos2Z = BlockPos.getZ(pos2);
         int chunkDistanceXZ1 = Math.abs(pos1X - sortChunkX) + Math.abs(pos1Z - sortChunkZ);
         int chunkDistanceXZ2 = Math.abs(pos2X - sortChunkX) + Math.abs(pos2Z - sortChunkZ);
         int chunkDistanceComparison = Integer.compare(chunkDistanceXZ1, chunkDistanceXZ2);
         if (chunkDistanceComparison != 0) {
            return chunkDistanceComparison;
         } else {
            int xComparison = Integer.compare(pos1X, pos2X);
            if (xComparison != 0) {
               return xComparison;
            } else {
               int zComparison = Integer.compare(pos1Z, pos2Z);
               return zComparison != 0 ? zComparison : Integer.compare(pos1Y, pos2Y);
            }
         }
      };
   }

   public static void applyBlockBufferServer(BlockBuffer buffer, ServerLevel world, @Nullable ChunkedBooleanRegion selection, @Nullable ServerPlayer source) {
      MutableBlockPos blockPos = new MutableBlockPos();
      BlockState emptyState = BlockBuffer.EMPTY_STATE;
      boolean hasStarlight = Axiom.hasStarlight();
      boolean sendGameMasterBlockWarning = false;
      boolean canEditNbt = source == null || AxiomServer.canUseAxiom(source, AxiomPermission.BUILD_NBT);
      int sortChunkX;
      int sortChunkZ;
      if (source != null) {
         sortChunkX = source.getBlockX() >> 4;
         sortChunkZ = source.getBlockZ() >> 4;
      } else {
         sortChunkX = 0;
         sortChunkZ = 0;
      }

      List<Entry<PalettedContainer<BlockState>>> entries = new ArrayList<>(buffer.entrySet());
      entries.sort(createBlockBufferOrderComparator(sortChunkX, sortChunkZ));
      LongIterator iterator = buffer.keySet().iterator();
      LongSet addedTicketSet = new LongOpenHashSet();

      while (iterator.hasNext()) {
         long pos = iterator.nextLong();
         int sx = BlockPos.getX(pos);
         int sz = BlockPos.getZ(pos);
         if (addedTicketSet.add(ChunkPos.asLong(sx, sz))) {
            ChunkPos chunkPos = new ChunkPos(sx, sz);
            world.getChunkSource().chunkMap.getDistanceManager().addTicket(CHUNK_UPDATE, chunkPos, ChunkLevel.byStatus(ChunkStatus.FULL), chunkPos);
         }
      }

      for (Entry<PalettedContainer<BlockState>> entry : entries) {
         int cx = BlockPos.getX(entry.getLongKey());
         int cy = BlockPos.getY(entry.getLongKey());
         int cz = BlockPos.getZ(entry.getLongKey());
         PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)entry.getValue();
         if (cy >= world.getMinSection() && cy <= world.getMaxSection() - 1) {
            LevelChunk chunk = world.getChunk(cx, cz);
            LevelChunkSection section = chunk.getSection(world.getSectionIndexFromSectionY(cy));
            boolean hasOnlyAir = section.hasOnlyAir();
            Heightmap worldSurface = null;
            Heightmap oceanFloor = null;
            Heightmap motionBlocking = null;
            Heightmap motionBlockingNoLeaves = null;

            for (java.util.Map.Entry<Types, Heightmap> heightmap : chunk.getHeightmaps()) {
               switch ((Types)heightmap.getKey()) {
                  case WORLD_SURFACE:
                     worldSurface = heightmap.getValue();
                     break;
                  case OCEAN_FLOOR:
                     oceanFloor = heightmap.getValue();
                     break;
                  case MOTION_BLOCKING:
                     motionBlocking = heightmap.getValue();
                     break;
                  case MOTION_BLOCKING_NO_LEAVES:
                     motionBlockingNoLeaves = heightmap.getValue();
               }
            }

            short[] lightUpdates = hasStarlight ? null : ((ServerLevelExt)world).axiom$getPendingLightUpdates(cx, cy, cz);
            boolean sectionChanged = false;
            boolean relightStarlight = false;
            boolean containerMaybeHasPoi = container.maybeHas(PoiTypes::hasPoi);
            boolean sectionMaybeHasPoi = section.maybeHas(PoiTypes::hasPoi);
            Short2ObjectMap<CompressedBlockEntity> blockEntityChunkMap = canEditNbt ? buffer.getBlockEntityChunkMap(entry.getLongKey()) : null;

            for (int x = 0; x < 16; x++) {
               for (int y = 0; y < 16; y++) {
                  for (int z = 0; z < 16; z++) {
                     BlockState blockState = (BlockState)container.get(x, y, z);
                     if (blockState != emptyState) {
                        int bx = cx * 16 + x;
                        int by = cy * 16 + y;
                        int bz = cz * 16 + z;
                        if (selection != null) {
                           selection.add(bx, by, bz);
                        }

                        if (!hasOnlyAir || !blockState.isAir()) {
                           Block block = blockState.getBlock();
                           BlockState old = section.setBlockState(x, y, z, blockState, true);
                           if (blockState != old) {
                              sectionChanged = true;
                              blockPos.set(bx, by, bz);
                              motionBlocking.update(x, by, z, blockState);
                              motionBlockingNoLeaves.update(x, by, z, blockState);
                              oceanFloor.update(x, by, z, blockState);
                              worldSurface.update(x, by, z, blockState);
                              if (VersionUtils.hasDifferentLightProperties(chunk, blockPos, old, blockState)) {
                                 if (hasStarlight) {
                                    relightStarlight = true;
                                 } else {
                                    ChunkSkyLightSources sources = chunk.getSkyLightSources();
                                    if (sources != null) {
                                       sources.update(chunk, x, by, z);
                                    }

                                    lightUpdates[y + z * 16] = (short)(lightUpdates[y + z * 16] | 1 << x);
                                 }
                              }

                              Optional<Holder<PoiType>> newPoi = containerMaybeHasPoi ? PoiTypes.forState(blockState) : Optional.empty();
                              Optional<Holder<PoiType>> oldPoi = sectionMaybeHasPoi ? PoiTypes.forState(old) : Optional.empty();
                              if (!Objects.equals(oldPoi, newPoi)) {
                                 if (oldPoi.isPresent()) {
                                    world.getPoiManager().remove(blockPos);
                                 }

                                 if (newPoi.isPresent()) {
                                    world.getPoiManager().add(blockPos, newPoi.get());
                                 }
                              }
                           }

                           if (blockState.hasBlockEntity()) {
                              blockPos.set(bx, by, bz);
                              BlockEntity blockEntity = chunk.getBlockEntity(blockPos, EntityCreationType.CHECK);
                              if (blockEntity == null) {
                                 blockEntity = ((EntityBlock)block).newBlockEntity(blockPos, blockState);
                                 if (blockEntity != null) {
                                    chunk.addAndRegisterBlockEntity(blockEntity);
                                 }
                              } else if (blockEntity.getType().isValid(blockState)) {
                                 blockEntity.setBlockState(blockState);
                                 chunk.updateBlockEntityTicker(blockEntity);
                              } else {
                                 chunk.removeBlockEntity(blockPos);
                                 blockEntity = ((EntityBlock)block).newBlockEntity(blockPos, blockState);
                                 if (blockEntity != null) {
                                    chunk.addAndRegisterBlockEntity(blockEntity);
                                 }
                              }

                              if (blockEntity != null && blockEntityChunkMap != null) {
                                 if (blockEntity instanceof GameMasterBlock && source != null && !source.canUseGameMasterBlocks()) {
                                    sendGameMasterBlockWarning = true;
                                 } else {
                                    int key = x | y << 4 | z << 8;
                                    CompressedBlockEntity savedBlockEntity = (CompressedBlockEntity)blockEntityChunkMap.get((short)key);
                                    if (savedBlockEntity != null) {
                                       SerializationUtils.loadBlockEntity(blockEntity, savedBlockEntity.decompress(), world.registryAccess());
                                       sectionChanged = true;
                                    }
                                 }
                              }
                           } else if (old.hasBlockEntity()) {
                              chunk.removeBlockEntity(blockPos);
                           }
                        }
                     }
                  }
               }
            }

            boolean nowHasOnlyAir = section.hasOnlyAir();
            if (hasOnlyAir != nowHasOnlyAir) {
               world.getChunkSource().getLightEngine().updateSectionStatus(SectionPos.of(cx, cy, cz), nowHasOnlyAir);
            }

            if (sectionChanged) {
               ((ServerLevelExt)world).axiom$markChunkDirty(cx, cz);
               chunk.setUnsaved(true);
            }

            if (relightStarlight) {
               ((ServerLevelExt)world).axiom$relightChunkStarlight(cx, cz);
            }
         }
      }

      if (sendGameMasterBlockWarning && source != null) {
         source.sendSystemMessage(Component.literal(AxiomI18n.get("axiom.hardcoded.unable_gamemaster")).withStyle(ChatFormatting.RED));
      }
   }

   private static void applyBiomeBufferServer(BiomeBuffer biomeBuffer, ServerLevel world) {
      Set<LevelChunk> changedChunks = new HashSet<>();
      int minSection = world.getMinSection();
      int maxSection = world.getMaxSection() - 1;
      Optional<Registry<Biome>> registryOptional = world.registryAccess().registry(Registries.BIOME);
      if (!registryOptional.isEmpty()) {
         Registry<Biome> registry = registryOptional.get();
         biomeBuffer.forEachEntry((x, y, z, biome) -> {
            int cy = y >> 2;
            if (cy >= minSection && cy <= maxSection) {
               LevelChunk chunkx = world.getChunk(x >> 2, z >> 2);
               LevelChunkSection section = chunkx.getSection(cy - minSection);
               PalettedContainer<Holder<Biome>> container = (PalettedContainer<Holder<Biome>>)section.getBiomes();
               Optional<Reference<Biome>> holder = registry.getHolder(biome);
               if (holder.isPresent()) {
                  container.set(x & 3, y & 3, z & 3, holder.get());
                  changedChunks.add(chunkx);
               }
            }
         });
         ChunkMap chunkMap = world.getChunkSource().chunkMap;
         HashMap<ServerPlayer, List<LevelChunk>> map = new HashMap<>();

         for (LevelChunk chunk : changedChunks) {
            chunk.setUnsaved(true);
            ChunkPos chunkPos = chunk.getPos();

            for (ServerPlayer serverPlayer2 : chunkMap.getPlayers(chunkPos, false)) {
               map.computeIfAbsent(serverPlayer2, serverPlayer -> new ArrayList<>()).add(chunk);
            }
         }

         map.forEach((serverPlayer, list) -> serverPlayer.connection.send(ClientboundChunksBiomesPacket.forChunks(list)));
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSetBuffer::read);
   }
}
