package com.moulberry.axiom.world_modification;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.packets.AxiomServerboundSetBuffer;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.PositionUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

public interface DispatchTask {
   int tick(int var1);

   boolean isDone();

   static DispatchTask blockOrBiome(ResourceKey<Level> dimension, BlockOrBiomeBuffer blockOrBiomeBuffer) {
      if (blockOrBiomeBuffer instanceof BlockBuffer blockBuffer) {
         return new DispatchTask.Block(dimension, UUID.randomUUID(), blockBuffer);
      } else if (blockOrBiomeBuffer instanceof BiomeBuffer biomeBuffer) {
         return new DispatchTask.Biome(dimension, UUID.randomUUID(), biomeBuffer);
      } else {
         throw new FaultyImplementationError();
      }
   }

   public static class Biome implements DispatchTask {
      private final ResourceKey<Level> dimension;
      private final UUID uuid;
      private final BiomeBuffer biomeBuffer;
      private final List<Entry<byte[]>> entries;
      private FriendlyByteBuf buf = null;
      private int pendingSend = 0;

      public Biome(ResourceKey<Level> dimension, UUID uuid, BiomeBuffer biomeBuffer) {
         this.dimension = dimension;
         this.uuid = uuid;
         this.biomeBuffer = biomeBuffer;
         LocalPlayer player = Minecraft.getInstance().player;
         int sortChunkX;
         int sortChunkY;
         int sortChunkZ;
         if (player != null) {
            sortChunkX = player.getBlockX() >> 4;
            sortChunkY = player.getBlockY() >> 4;
            sortChunkZ = player.getBlockZ() >> 4;
         } else {
            sortChunkX = 0;
            sortChunkY = 0;
            sortChunkZ = 0;
         }

         this.entries = new ArrayList<>(biomeBuffer.map.map.long2ObjectEntrySet());
         this.entries.sort(Comparator.comparingLong(entry -> {
            long pos = entry.getLongKey();
            int posX = BlockPos.getX(pos);
            int posY = BlockPos.getY(pos);
            int posZ = BlockPos.getZ(pos);
            return Math.abs(posX - sortChunkX) + Math.abs(posY - sortChunkY) + Math.abs(posZ - sortChunkZ);
         }));
      }

      @Override
      public int tick(int maxSendable) {
         if (maxSendable > 0 && !this.isDone()) {
            int sent = 0;
            int maxSize = Axiom.getInstance().serverConfig.setBufferMaxSize() - 64;
            Iterator<Entry<byte[]>> iterator = this.entries.iterator();

            while (iterator.hasNext() && this.pendingSend < maxSendable) {
               Entry<byte[]> entry = iterator.next();
               iterator.remove();
               if (this.buf != null && this.buf.writerIndex() + 4096 + 8L + 8L >= maxSize) {
                  this.buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                  this.buf.writeVarInt(maxSendable - this.pendingSend);
                  new AxiomServerboundSetBuffer(this.buf).send();
                  this.buf = null;
                  sent += this.pendingSend;
                  maxSendable -= this.pendingSend;
                  this.pendingSend = 0;
               }

               if (this.buf == null) {
                  this.buf = new FriendlyByteBuf(Unpooled.buffer());
                  this.buf.writeResourceKey(this.dimension);
                  this.buf.writeUUID(this.uuid);
                  this.buf.writeByte(1);
                  this.buf.writeByte(this.biomeBuffer.paletteSize);

                  for (int i = 0; i < this.biomeBuffer.paletteSize; i++) {
                     this.buf.writeResourceKey(this.biomeBuffer.palette[i]);
                  }

                  this.buf.writeByte(this.biomeBuffer.map.getDefaultValue());
               }

               this.pendingSend++;
               this.buf.writeLong(entry.getLongKey());
               this.buf.writeBytes((byte[])entry.getValue());
            }

            if (this.buf != null && this.pendingSend <= maxSendable) {
               this.buf.writeLong(PositionUtils.MIN_POSITION_LONG);
               this.buf.writeVarInt(maxSendable - this.pendingSend);
               new AxiomServerboundSetBuffer(this.buf).send();
               this.buf = null;
               sent += this.pendingSend;
               this.pendingSend = 0;
            }

            return sent;
         } else {
            return 0;
         }
      }

      @Override
      public boolean isDone() {
         return this.entries.isEmpty() && this.buf == null;
      }
   }

   public static class Block implements DispatchTask {
      private final ResourceKey<Level> dimension;
      private final UUID uuid;
      private final BlockBuffer blockBuffer;
      private final List<Entry<PalettedContainer<BlockState>>> entries;
      private FriendlyByteBuf buf = null;
      private int pendingSend = 0;

      public Block(ResourceKey<Level> dimension, UUID uuid, BlockBuffer blockBuffer) {
         this.dimension = dimension;
         this.uuid = uuid;
         this.blockBuffer = blockBuffer;
         LocalPlayer player = Minecraft.getInstance().player;
         int sortChunkX;
         int sortChunkZ;
         if (player != null) {
            sortChunkX = player.getBlockX() >> 4;
            sortChunkZ = player.getBlockZ() >> 4;
         } else {
            sortChunkX = 0;
            sortChunkZ = 0;
         }

         this.entries = new ArrayList<>(blockBuffer.entrySet());
         this.entries.sort(AxiomServerboundSetBuffer.createBlockBufferOrderComparator(sortChunkX, sortChunkZ));
      }

      @Override
      public int tick(int maxSendable) {
         if (maxSendable > 0 && !this.isDone()) {
            int sent = 0;
            int maxSize = Axiom.getInstance().serverConfig.setBufferMaxSize() - 64;
            Iterator<Entry<PalettedContainer<BlockState>>> iterator = this.entries.iterator();

            while (iterator.hasNext() && this.pendingSend < maxSendable) {
               Entry<PalettedContainer<BlockState>> entry = iterator.next();
               long pos = entry.getLongKey();
               PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)entry.getValue();
               iterator.remove();
               boolean firstChunk;
               if (this.buf == null) {
                  this.buf = new FriendlyByteBuf(Unpooled.buffer());
                  this.buf.writeResourceKey(this.dimension);
                  this.buf.writeUUID(this.uuid);
                  this.buf.writeByte(0);
                  firstChunk = true;
               } else {
                  firstChunk = false;
               }

               int beforeWriterIndex = this.buf.writerIndex();
               this.pendingSend++;
               this.buf.writeLong(pos);
               container.write(this.buf);
               Short2ObjectMap<CompressedBlockEntity> blockEntities = this.blockBuffer.getBlockEntityChunkMap(entry.getLongKey());
               if (blockEntities != null && AxiomClient.hasPermission(AxiomPermission.BUILD_NBT)) {
                  this.buf.writeVarInt(blockEntities.size());
                  ObjectIterator copiedSize = blockEntities.short2ObjectEntrySet().iterator();

                  while (copiedSize.hasNext()) {
                     it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity> blockEntityEntry = (it.unimi.dsi.fastutil.shorts.Short2ObjectMap.Entry<CompressedBlockEntity>)copiedSize.next();
                     this.buf.writeShort(blockEntityEntry.getShortKey());
                     ((CompressedBlockEntity)blockEntityEntry.getValue()).write(this.buf);
                  }
               } else {
                  this.buf.writeVarInt(0);
               }

               if (this.buf.writerIndex() >= maxSize) {
                  if (firstChunk) {
                     Axiom.dbg(
                        "AxiomServerboundSetBuffer - Warning: More than "
                           + NumberFormat.getInstance().format((long)maxSize)
                           + " bytes in a single section. Forcing send..."
                     );
                     this.buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     this.buf.writeVarInt(maxSendable - this.pendingSend);
                     new AxiomServerboundSetBuffer(this.buf).send();
                     this.buf = null;
                     sent += this.pendingSend;
                     maxSendable -= this.pendingSend;
                     this.pendingSend = 0;
                  } else {
                     int copiedSize = this.buf.writerIndex() - beforeWriterIndex;
                     byte[] copied = new byte[copiedSize];
                     this.buf.getBytes(beforeWriterIndex, copied);
                     this.buf.writerIndex(beforeWriterIndex);
                     this.buf.writeLong(PositionUtils.MIN_POSITION_LONG);
                     this.buf.writeVarInt(maxSendable - (this.pendingSend - 1));
                     new AxiomServerboundSetBuffer(this.buf).send();
                     this.buf = null;
                     sent += this.pendingSend - 1;
                     maxSendable -= this.pendingSend - 1;
                     this.pendingSend = 1;
                     this.buf = new FriendlyByteBuf(Unpooled.buffer());
                     this.buf.writeResourceKey(this.dimension);
                     this.buf.writeUUID(this.uuid);
                     this.buf.writeByte(0);
                     this.buf.writeBytes(copied);
                  }
               }
            }

            if (this.buf != null && this.pendingSend <= maxSendable) {
               this.buf.writeLong(PositionUtils.MIN_POSITION_LONG);
               this.buf.writeVarInt(maxSendable - this.pendingSend);
               new AxiomServerboundSetBuffer(this.buf).send();
               this.buf = null;
               sent += this.pendingSend;
               this.pendingSend = 0;
            }

            return sent;
         } else {
            return 0;
         }
      }

      @Override
      public boolean isDone() {
         return this.entries.isEmpty() && this.buf == null;
      }
   }
}
