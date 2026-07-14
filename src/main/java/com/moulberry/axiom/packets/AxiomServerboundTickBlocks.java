package com.moulberry.axiom.packets;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

public class AxiomServerboundTickBlocks implements AxiomServerboundPacket {
   public static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:tick_blocks");
   private final ResourceKey<Level> world;
   private final PositionSet positionSet;
   private final BlockPos aabbMin;
   private final BlockPos aabbMax;

   public AxiomServerboundTickBlocks(ResourceKey<Level> world, PositionSet positionSet) {
      this.world = world;
      this.positionSet = positionSet.copy();
      this.aabbMin = null;
      this.aabbMax = null;
   }

   public AxiomServerboundTickBlocks(ResourceKey<Level> world, BlockPos min, BlockPos max) {
      this.world = world;
      this.positionSet = null;
      this.aabbMin = min;
      this.aabbMax = max;
   }

   public AxiomServerboundTickBlocks(FriendlyByteBuf friendlyByteBuf) {
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
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.BUILD_DANGEROUS_TICK)) {
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
               Component msg = Component.literal(player.getScoreboardName() + " updated & ticked " + count + " blocks using Axiom. The server may lag...");
               server.getPlayerList().broadcastSystemMessage(msg, false);
               long estimatedTime = Math.max(1, count / 4194304);
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
            AxiomServerboundTickBlocks.Updater consumer = new AxiomServerboundTickBlocks.Updater(level);
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
               Component msg = Component.literal(AxiomI18n.get("axiom.hardcoded.done_updating") + seconds + "s)");
               server.getPlayerList().broadcastSystemMessage(msg, false);
            }
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundTickBlocks::new);
   }

   private static class Updater implements TriIntConsumer {
      private final MutableBlockPos blockPos = new MutableBlockPos();
      private final ServerLevel level;
      private int lastSectionX = Integer.MIN_VALUE;
      private int lastSectionY = Integer.MIN_VALUE;
      private int lastSectionZ = Integer.MIN_VALUE;
      private LevelChunk chunk;
      private LevelChunkSection section;

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
            }

            if (chunkChanged || cy != this.lastSectionY) {
               this.lastSectionY = cy;
               this.section = this.chunk.getSection(sectionIndex);
            }

            BlockState blockState = this.section.getBlockState(x, y, z);
            if (!blockState.isAir()) {
               FluidState fluidState = blockState.getFluidState();
               if (!fluidState.isEmpty()) {
                  fluidState.tick(this.level, this.blockPos);
               }

               if (blockState.getBlock() instanceof LiquidBlock) {
                  blockState.tick(this.level, this.blockPos, this.level.getRandom());
               } else {
                  BlockState blockStateNew = Block.updateFromNeighbourShapes(blockState, this.level, this.blockPos);
                  if (blockStateNew != blockState) {
                     this.level.setBlock(this.blockPos, blockStateNew, 818);
                  }
               }
            }
         }
      }
   }
}
