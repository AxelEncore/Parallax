package com.moulberry.axiom.packets;

import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.Dispatcher;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AxiomClientboundResponseChunkData implements AxiomClientboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:response_chunk_data");
   private final long id;
   private final boolean finished;
   private final Long2ObjectMap<CompressedBlockEntity> compressedBlockEntities;
   private final Long2ObjectMap<PalettedContainer<BlockState>> chunkSections;
   @Nullable
   private final FriendlyByteBuf rawByteBuf;

   public AxiomClientboundResponseChunkData(
      long id, boolean finished, Long2ObjectMap<CompressedBlockEntity> compressedBlockEntities, Long2ObjectMap<PalettedContainer<BlockState>> chunkSections
   ) {
      this.id = id;
      this.finished = finished;
      this.compressedBlockEntities = compressedBlockEntities;
      this.chunkSections = chunkSections;
      this.rawByteBuf = null;
   }

   public AxiomClientboundResponseChunkData(@NotNull FriendlyByteBuf rawByteBuf) {
      this.id = 0L;
      this.finished = false;
      this.compressedBlockEntities = null;
      this.chunkSections = null;
      this.rawByteBuf = rawByteBuf;
   }

   public static AxiomClientboundResponseChunkData read(FriendlyByteBuf friendlyByteBuf) {
      long id = friendlyByteBuf.readLong();
      Long2ObjectOpenHashMap<CompressedBlockEntity> compressedBlockEntities = new Long2ObjectOpenHashMap();

      while (true) {
         long pos = friendlyByteBuf.readLong();
         if (pos == PositionUtils.MIN_POSITION_LONG) {
            Long2ObjectOpenHashMap<PalettedContainer<BlockState>> chunkSections = new Long2ObjectOpenHashMap();

            while (true) {
               long posx = friendlyByteBuf.readLong();
               if (posx == PositionUtils.MIN_POSITION_LONG) {
                  boolean finished = friendlyByteBuf.readBoolean();
                  return new AxiomClientboundResponseChunkData(id, finished, compressedBlockEntities, chunkSections);
               }

               if (friendlyByteBuf.readBoolean()) {
                  PalettedContainer<BlockState> container = DFUHelper.createPalettedContainer(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState());
                  container.read(friendlyByteBuf);
                  chunkSections.put(posx, container);
               } else {
                  chunkSections.put(posx, null);
               }
            }
         }

         compressedBlockEntities.put(pos, CompressedBlockEntity.read(friendlyByteBuf));
      }
   }

   @Override
   public ResourceLocation packetIdentifier() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      if (this.rawByteBuf != null) {
         friendlyByteBuf.writeBytes(this.rawByteBuf);
      } else {
         friendlyByteBuf.writeLong(this.id);
         ObjectIterator var2 = this.compressedBlockEntities.long2ObjectEntrySet().iterator();

         while (var2.hasNext()) {
            Entry<CompressedBlockEntity> entry = (Entry<CompressedBlockEntity>)var2.next();
            friendlyByteBuf.writeLong(entry.getLongKey());
            ((CompressedBlockEntity)entry.getValue()).write(friendlyByteBuf);
         }

         friendlyByteBuf.writeLong(PositionUtils.MIN_POSITION_LONG);
         var2 = this.chunkSections.long2ObjectEntrySet().iterator();

         while (var2.hasNext()) {
            Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)var2.next();
            friendlyByteBuf.writeLong(entry.getLongKey());
            PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)entry.getValue();
            if (container == null) {
               friendlyByteBuf.writeBoolean(false);
            } else {
               friendlyByteBuf.writeBoolean(true);
               ((PalettedContainer)entry.getValue()).write(friendlyByteBuf);
            }
         }

         friendlyByteBuf.writeLong(PositionUtils.MIN_POSITION_LONG);
         friendlyByteBuf.writeBoolean(this.finished);
      }
   }

   @Override
   public void handle(Minecraft client, RegistryAccess registryAccess) {
      Dispatcher.finishRequestChunkData(this.id, this.finished, this.compressedBlockEntities, this.chunkSections);
   }

   public static void register() {
      AxiomClientboundPacket.register(IDENTIFIER, AxiomClientboundResponseChunkData::read);
   }
}
