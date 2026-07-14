package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.hooks.ServerLevelExt;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;

public class AxiomServerboundFixArea implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:fix_area");
   private final ResourceKey<Level> world;
   private final PositionSet positionSet;
   private final BlockPos aabbMin;
   private final BlockPos aabbMax;

   public AxiomServerboundFixArea(ResourceKey<Level> world, PositionSet positionSet) {
      this.world = world;
      this.positionSet = positionSet.copy();
      this.aabbMin = null;
      this.aabbMax = null;
   }

   public AxiomServerboundFixArea(ResourceKey<Level> world, BlockPos min, BlockPos max) {
      this.world = world;
      this.positionSet = null;
      this.aabbMin = min;
      this.aabbMax = max;
   }

   public AxiomServerboundFixArea(FriendlyByteBuf friendlyByteBuf) {
      this.world = friendlyByteBuf.readResourceKey(Registries.DIMENSION);
      byte type = friendlyByteBuf.readByte();
      if (type == 0) {
         this.positionSet = PositionSet.read(friendlyByteBuf);
         this.aabbMin = null;
         this.aabbMax = null;
      } else {
         if (type != 1) {
            throw new RuntimeException("Unknown type: " + type);
         }

         this.positionSet = null;
         this.aabbMin = friendlyByteBuf.readBlockPos();
         this.aabbMax = friendlyByteBuf.readBlockPos();
      }
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeResourceKey(this.world);
      if (this.positionSet != null) {
         friendlyByteBuf.writeByte(0);
         this.positionSet.write(friendlyByteBuf);
      } else {
         friendlyByteBuf.writeByte(1);
         friendlyByteBuf.writeBlockPos(this.aabbMin);
         friendlyByteBuf.writeBlockPos(this.aabbMax);
      }
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.BUILD_DANGEROUS_FIX_AREA)) {
         ServerLevel level = player.serverLevel();
         if (level.dimension() == this.world) {
            int count;
            if (this.positionSet != null) {
               count = this.positionSet.count();
            } else {
               int sizeX = Math.abs(this.aabbMax.getX() - this.aabbMin.getX()) + 1;
               int sizeY = Math.abs(this.aabbMax.getY() - this.aabbMin.getY()) + 1;
               int sizeZ = Math.abs(this.aabbMax.getZ() - this.aabbMin.getZ()) + 1;
               count = sizeX * sizeY * sizeZ;
            }

            boolean showMessage = count > 1048576;
            if (showMessage) {
               Component msg = Component.literal(player.getScoreboardName() + " applied fixes to " + count + " blocks using Axiom. The server may lag...");
               server.getPlayerList().broadcastSystemMessage(msg, false);
               long estimatedTime = Math.max(1, count / 10485760);
               msg = Component.literal(AxiomI18n.get("axiom.hardcoded.estimated_time") + estimatedTime + "s");
               server.getPlayerList().broadcastSystemMessage(msg, false);
               msg = Component.literal(AxiomI18n.get("axiom.hardcoded.may_take_longer"));
               server.getPlayerList().broadcastSystemMessage(msg, false);
               if (estimatedTime > 30L && !Minecraft.getInstance().hasSingleplayerServer()) {
                  msg = Component.literal(AxiomI18n.get("axiom.hardcoded.estimated_time_kick"));
                  server.getPlayerList().broadcastSystemMessage(msg, false);
               }
            }

            long start = System.currentTimeMillis();
            AxiomServerboundFixArea.Updater consumer = new AxiomServerboundFixArea.Updater(level);
            if (this.positionSet != null) {
               this.positionSet.forEach(consumer);
            } else {
               int minX = Math.min(this.aabbMin.getX(), this.aabbMax.getX());
               int minY = Math.min(this.aabbMin.getY(), this.aabbMax.getY());
               int minZ = Math.min(this.aabbMin.getZ(), this.aabbMax.getZ());
               int maxX = Math.max(this.aabbMin.getX(), this.aabbMax.getX());
               int maxY = Math.max(this.aabbMin.getY(), this.aabbMax.getY());
               int maxZ = Math.max(this.aabbMin.getZ(), this.aabbMax.getZ());
               int minSX = minX >> 4;
               int minSY = minY >> 4;
               int minSZ = minZ >> 4;
               int maxSX = maxX >> 4;
               int maxSY = maxY >> 4;
               int maxSZ = maxZ >> 4;

               for (int sx = minSX; sx <= maxSX; sx++) {
                  for (int sz = minSZ; sz <= maxSZ; sz++) {
                     for (int sy = minSY; sy <= maxSY; sy++) {
                        int rx = sx << 4;
                        int ry = sy << 4;
                        int rz = sz << 4;
                        int fromX = Math.max(rx, minX);
                        int fromY = Math.max(ry, minY);
                        int fromZ = Math.max(rz, minZ);
                        int toX = Math.min(rx + 15, maxX);
                        int toY = Math.min(ry + 15, maxY);
                        int toZ = Math.min(rz + 15, maxZ);

                        for (int x = fromX; x <= toX; x++) {
                           for (int y = fromY; y <= toY; y++) {
                              for (int z = fromZ; z <= toZ; z++) {
                                 consumer.accept(x, y, z);
                              }
                           }
                        }
                     }
                  }
               }
            }

            long end = System.currentTimeMillis();
            if (showMessage) {
               long seconds = (end - start + 500L) / 1000L;
               Component msg = Component.literal(AxiomI18n.get("axiom.hardcoded.done_fixing") + seconds + "s)");
               server.getPlayerList().broadcastSystemMessage(msg, false);
            }
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundFixArea::new);
   }

   private static class Updater implements TriIntConsumer {
      private final MutableBlockPos blockPos = new MutableBlockPos();
      private final boolean hasStarlight = Axiom.hasStarlight();
      private final ServerLevel level;
      private int lastSectionX = Integer.MIN_VALUE;
      private int lastSectionY = Integer.MIN_VALUE;
      private int lastSectionZ = Integer.MIN_VALUE;
      private LevelChunk chunk;
      private Heightmap worldSurface;
      private Heightmap oceanFloor;
      private Heightmap motionBlocking;
      private Heightmap motionBlockingNoLeaves;
      private LevelChunkSection section;
      private short[] pendingLightUpdates;

      public Updater(ServerLevel level) {
         this.level = level;
      }

      public void accept(int bx, int by, int bz) {
         this.blockPos.set(bx, by, bz);
         int x = bx & 15;
         int y = by & 15;
         int z = bz & 15;
         int cx = bx >> 4;
         int cy = by >> 4;
         int cz = bz >> 4;
         int sectionIndex = this.level.getSectionIndexFromSectionY(cy);
         if (sectionIndex >= 0 && sectionIndex < this.level.getSectionsCount()) {
            boolean chunkChanged = this.chunk == null || cx != this.lastSectionX || cz != this.lastSectionZ;
            if (chunkChanged) {
               this.lastSectionX = cx;
               this.lastSectionZ = cz;
               this.chunk = this.level.getChunk(cx, cz);
               this.chunk.setUnsaved(true);
               this.worldSurface = null;
               this.oceanFloor = null;
               this.motionBlocking = null;
               this.motionBlockingNoLeaves = null;

               for (Entry<Types, Heightmap> heightmap : this.chunk.getHeightmaps()) {
                  switch ((Types)heightmap.getKey()) {
                     case WORLD_SURFACE:
                        this.worldSurface = heightmap.getValue();
                        break;
                     case OCEAN_FLOOR:
                        this.oceanFloor = heightmap.getValue();
                        break;
                     case MOTION_BLOCKING:
                        this.motionBlocking = heightmap.getValue();
                        break;
                     case MOTION_BLOCKING_NO_LEAVES:
                        this.motionBlockingNoLeaves = heightmap.getValue();
                  }
               }
            }

            if (chunkChanged || cy != this.lastSectionY) {
               this.lastSectionY = cy;
               this.section = this.chunk.getSection(sectionIndex);
               if (!this.hasStarlight) {
                  this.pendingLightUpdates = ((ServerLevelExt)this.level).axiom$getPendingLightUpdates(cx, cy, cz);
               }
            }

            BlockState blockState = this.section.getBlockState(x, y, z);
            Block block = blockState.getBlock();
            this.motionBlocking.update(x, by, z, blockState);
            this.motionBlockingNoLeaves.update(x, by, z, blockState);
            this.oceanFloor.update(x, by, z, blockState);
            this.worldSurface.update(x, by, z, blockState);
            if (blockState.hasBlockEntity()) {
               BlockEntity blockEntity = this.chunk.getBlockEntity(this.blockPos, EntityCreationType.CHECK);
               if (blockEntity == null) {
                  blockEntity = ((EntityBlock)block).newBlockEntity(this.blockPos, blockState);
                  if (blockEntity != null) {
                     this.chunk.addAndRegisterBlockEntity(blockEntity);
                  }
               } else if (blockEntity.getType().isValid(blockState)) {
                  blockEntity.setBlockState(blockState);
                  this.chunk.updateBlockEntityTicker(blockEntity);
               } else {
                  this.chunk.removeBlockEntity(this.blockPos);
                  blockEntity = ((EntityBlock)block).newBlockEntity(this.blockPos, blockState);
                  if (blockEntity != null) {
                     this.chunk.addAndRegisterBlockEntity(blockEntity);
                  }
               }
            } else {
               this.chunk.removeBlockEntity(this.blockPos);
            }

            ChunkSkyLightSources sources = this.chunk.getSkyLightSources();
            if (sources != null) {
               sources.update(this.chunk, x, by, z);
            }

            if (this.hasStarlight) {
               ((ServerLevelExt)this.level).axiom$relightChunkStarlight(cx, cz);
            } else {
               this.pendingLightUpdates[y + z * 16] = (short)(this.pendingLightUpdates[y + z * 16] | 1 << x);
            }

            PoiManager poiManager = this.level.getPoiManager();
            Optional<Holder<PoiType>> poi = PoiTypes.forState(blockState);
            Optional<Holder<PoiType>> registered = poiManager.getType(this.blockPos);
            if (!Objects.equals(poi, registered)) {
               if (registered.isPresent()) {
                  poiManager.remove(this.blockPos);
               }

               if (poi.isPresent()) {
                  poiManager.add(this.blockPos, poi.get());
               }
            }
         }
      }
   }
}
