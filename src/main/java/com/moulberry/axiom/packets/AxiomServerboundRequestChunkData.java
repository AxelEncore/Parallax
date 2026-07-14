package com.moulberry.axiom.packets;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomServer;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class AxiomServerboundRequestChunkData implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:request_chunk_data");
   private final long id;
   private final ResourceKey<Level> world;
   private final LongSet blockEntities;
   private final LongSet chunkSections;
   private final boolean sendBlockEntitiesInChunks;
   private static final TicketType<ChunkPos> CHUNK_REQUEST = TicketType.create("axiom_chunk_request", Comparator.comparingLong(ChunkPos::toLong), 5);

   public AxiomServerboundRequestChunkData(long id, ResourceKey<Level> world, LongSet blockEntities, LongSet chunkSections, boolean sendBlockEntitiesInChunks) {
      this.id = id;
      this.world = world;
      this.blockEntities = blockEntities;
      this.chunkSections = chunkSections;
      this.sendBlockEntitiesInChunks = sendBlockEntitiesInChunks;
   }

   public static AxiomServerboundRequestChunkData read(FriendlyByteBuf friendlyByteBuf) {
      long id = friendlyByteBuf.readLong();
      ResourceKey<Level> world = friendlyByteBuf.readResourceKey(Registries.DIMENSION);
      boolean sendBlockEntitiesInChunks = friendlyByteBuf.readBoolean();
      LongSet blockEntities = new LongOpenHashSet();
      int count = friendlyByteBuf.readVarInt();

      for (int i = 0; i < count; i++) {
         blockEntities.add(friendlyByteBuf.readLong());
      }

      LongSet chunkSections = new LongOpenHashSet();
      count = friendlyByteBuf.readVarInt();

      for (int i = 0; i < count; i++) {
         chunkSections.add(friendlyByteBuf.readLong());
      }

      return new AxiomServerboundRequestChunkData(id, world, blockEntities, chunkSections, sendBlockEntitiesInChunks);
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeLong(this.id);
      friendlyByteBuf.writeResourceKey(this.world);
      friendlyByteBuf.writeBoolean(this.sendBlockEntitiesInChunks);
      if (this.blockEntities == null) {
         friendlyByteBuf.writeVarInt(0);
      } else {
         friendlyByteBuf.writeVarInt(this.blockEntities.size());
         LongIterator iterator = this.blockEntities.longIterator();

         while (iterator.hasNext()) {
            friendlyByteBuf.writeLong(iterator.nextLong());
         }
      }

      if (this.chunkSections == null) {
         friendlyByteBuf.writeVarInt(0);
      } else {
         friendlyByteBuf.writeVarInt(this.chunkSections.size());
         LongIterator iterator = this.chunkSections.longIterator();

         while (iterator.hasNext()) {
            friendlyByteBuf.writeLong(iterator.nextLong());
         }
      }
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (!AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.CHUNK_REQUEST)) {
         new AxiomClientboundResponseChunkData(this.id, true, new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap()).send(player);
      } else {
         ServerLevel level = server.getLevel(this.world);
         if (level != null && level == player.serverLevel()) {
            boolean shouldSendBlockEntities = AxiomServer.hasPermission(player, AxiomPermission.CHUNK_REQUESTBLOCKENTITY);
            boolean warnedAboutNonExistant = false;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            Long2ObjectMap<CompressedBlockEntity> blockEntityMap = new Long2ObjectOpenHashMap();
            if (shouldSendBlockEntities) {
               LongIterator iterator = this.blockEntities.iterator();

               while (iterator.hasNext()) {
                  long pos = iterator.nextLong();
                  BlockEntity blockEntity = level.getBlockEntity(mutableBlockPos.set(pos));
                  if (blockEntity != null) {
                     CompoundTag tag = blockEntity.saveWithoutMetadata(server.registryAccess());
                     blockEntityMap.put(pos, CompressedBlockEntity.compress(tag, baos));
                  } else if (!warnedAboutNonExistant) {
                     warnedAboutNonExistant = true;
                     Axiom.LOGGER
                        .warn("Client requested block entity data at {}, but block was {}", BlockPos.of(pos), level.getBlockState(mutableBlockPos.set(pos)));
                  }
               }
            }

            int playerSectionX = player.getBlockX() >> 4;
            int playerSectionZ = player.getBlockZ() >> 4;
            LongIterator iterator = this.chunkSections.iterator();
            LongSet addedTicketSet = new LongOpenHashSet();

            while (iterator.hasNext()) {
               long pos = iterator.nextLong();
               int sx = BlockPos.getX(pos);
               int sz = BlockPos.getZ(pos);
               int distance = Math.abs(playerSectionX - sx) + Math.abs(playerSectionZ - sz);
               if (distance <= 256 && addedTicketSet.add(ChunkPos.asLong(sx, sz))) {
                  ChunkPos chunkPos = new ChunkPos(sx, sz);
                  level.getChunkSource().chunkMap.getDistanceManager().addTicket(CHUNK_REQUEST, chunkPos, ChunkLevel.byStatus(ChunkStatus.FULL), chunkPos);
               }
            }

            Long2ObjectMap<PalettedContainer<BlockState>> sections = new Long2ObjectOpenHashMap();
            iterator = this.chunkSections.iterator();

            while (iterator.hasNext()) {
               long pos = iterator.nextLong();
               int sx = BlockPos.getX(pos);
               int sy = BlockPos.getY(pos);
               int sz = BlockPos.getZ(pos);
               int distance = Math.abs(playerSectionX - sx) + Math.abs(playerSectionZ - sz);
               if (distance <= 256) {
                  LevelChunk chunk = level.getChunk(sx, sz);
                  int sectionIndex = chunk.getSectionIndexFromSectionY(sy);
                  if (sectionIndex >= 0 && sectionIndex < chunk.getSectionsCount()) {
                     LevelChunkSection section = chunk.getSection(sectionIndex);
                     if (section.hasOnlyAir()) {
                        sections.put(pos, null);
                     } else {
                        PalettedContainer<BlockState> container = section.getStates();
                        sections.put(pos, container);
                        if (this.sendBlockEntitiesInChunks && shouldSendBlockEntities && section.maybeHas(BlockStateBase::hasBlockEntity)) {
                           for (int x = 0; x < 16; x++) {
                              for (int y = 0; y < 16; y++) {
                                 for (int z = 0; z < 16; z++) {
                                    BlockState blockState = (BlockState)container.get(x, y, z);
                                    if (blockState.hasBlockEntity()) {
                                       mutableBlockPos.set(sx * 16 + x, sy * 16 + y, sz * 16 + z);
                                       BlockEntity blockEntity = chunk.getBlockEntity(mutableBlockPos, EntityCreationType.CHECK);
                                       if (blockEntity != null) {
                                          CompoundTag tag = blockEntity.saveWithoutMetadata(server.registryAccess());
                                          blockEntityMap.put(mutableBlockPos.asLong(), CompressedBlockEntity.compress(tag, baos));
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

            boolean firstPart = true;
            int maxSize = 1048512;
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeLong(this.id);
            ObjectIterator var41 = blockEntityMap.long2ObjectEntrySet().iterator();

            while (var41.hasNext()) {
               Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var41.next();
               int beforeWriterIndex = buf.writerIndex();
               buf.writeLong(entry.getLongKey());
               ((CompressedBlockEntity)entry.getValue()).write(buf);
               if (buf.writerIndex() >= maxSize) {
                  if (firstPart) {
                     Axiom.dbg(
                        "AxiomServerboundRequestChunkData - Warning: More than "
                           + NumberFormat.getInstance().format((long)maxSize)
                           + " bytes in a single part. Forcing send..."
                     );
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     buf.writeBoolean(false);
                     new AxiomClientboundResponseChunkData(buf).send(player);
                     buf = new FriendlyByteBuf(Unpooled.buffer());
                     buf.writeLong(this.id);
                  } else {
                     int copiedSize = buf.writerIndex() - beforeWriterIndex;
                     byte[] copied = new byte[copiedSize];
                     buf.getBytes(beforeWriterIndex, copied);
                     buf.writerIndex(beforeWriterIndex);
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     buf.writeBoolean(false);
                     new AxiomClientboundResponseChunkData(buf).send(player);
                     buf = new FriendlyByteBuf(Unpooled.buffer());
                     buf.writeLong(this.id);
                     buf.writeBytes(copied);
                     firstPart = true;
                  }
               } else {
                  firstPart = false;
               }
            }

            buf.writeLong(PositionUtils.MIN_POSITION_LONG);
            var41 = sections.long2ObjectEntrySet().iterator();

            while (var41.hasNext()) {
               Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)var41.next();
               int beforeWriterIndex = buf.writerIndex();
               buf.writeLong(entry.getLongKey());
               PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)entry.getValue();
               if (container == null) {
                  buf.writeBoolean(false);
               } else {
                  buf.writeBoolean(true);
                  ((PalettedContainer)entry.getValue()).write(buf);
               }

               if (buf.writerIndex() >= maxSize) {
                  if (firstPart) {
                     Axiom.dbg(
                        "AxiomServerboundRequestChunkData - Warning: More than "
                           + NumberFormat.getInstance().format((long)maxSize)
                           + " bytes in a single part. Forcing send..."
                     );
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     buf.writeBoolean(false);
                     new AxiomClientboundResponseChunkData(buf).send(player);
                     buf = new FriendlyByteBuf(Unpooled.buffer());
                     buf.writeLong(this.id);
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                  } else {
                     int copiedSize = buf.writerIndex() - beforeWriterIndex;
                     byte[] copied = new byte[copiedSize];
                     buf.getBytes(beforeWriterIndex, copied);
                     buf.writerIndex(beforeWriterIndex);
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     buf.writeBoolean(false);
                     new AxiomClientboundResponseChunkData(buf).send(player);
                     buf = new FriendlyByteBuf(Unpooled.buffer());
                     buf.writeLong(this.id);
                     buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     buf.writeBytes(copied);
                     firstPart = true;
                  }
               } else {
                  firstPart = false;
               }
            }

            buf.writeLong(PositionUtils.MIN_POSITION_LONG);
            buf.writeBoolean(true);
            new AxiomClientboundResponseChunkData(buf).send(player);
         } else {
            new AxiomClientboundResponseChunkData(this.id, true, new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap()).send(player);
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundRequestChunkData::read);
   }
}
