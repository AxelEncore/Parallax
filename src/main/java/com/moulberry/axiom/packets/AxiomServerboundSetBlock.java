package com.moulberry.axiom.packets;

import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.NetworkHelper;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.ChunkSkyLightSources;
import net.minecraft.world.phys.BlockHitResult;

public class AxiomServerboundSetBlock implements AxiomServerboundPacket {
   private static final ResourceLocation IDENTIFIER = ResourceLocation.parse("axiom:set_block");
   private final Map<BlockPos, BlockState> blocks;
   private final boolean updateNeighbors;
   private final Set<BlockPos> preventUpdatesAt;
   private final int reason;
   private final boolean breaking;
   private final BlockHitResult blockHit;
   private final InteractionHand hand;
   private final int sequenceId;
   public static final int REASON_REPLACEMODE = 1;
   public static final int REASON_TINKER = 2;
   public static final int REASON_FORCEPLACE = 4;
   public static final int REASON_NOUPDATES = 8;
   public static final int REASON_CUSTOMSHAPEUPDATE = 16;
   public static final int REASON_CUSTOMPLACEMENT = 32;
   public static final int REASON_INFINITEREACH = 64;
   public static final int REASON_ANGEL = 128;
   public static final int REASON_SYMMETRY = 256;
   public static final int REASON_LIQUIDPLACE = 512;
   public static final int REASON_SKIPINTERACTION = 1024;

   public AxiomServerboundSetBlock(
      Map<BlockPos, BlockState> blocks,
      boolean updateNeighbors,
      int reason,
      boolean breaking,
      BlockHitResult blockHitResult,
      InteractionHand interactionHand,
      int sequenceId
   ) {
      this(blocks, updateNeighbors, Set.of(), reason, breaking, blockHitResult, interactionHand, sequenceId);
   }

   public AxiomServerboundSetBlock(
      Map<BlockPos, BlockState> blocks,
      boolean updateNeighbors,
      Set<BlockPos> preventUpdatesAt,
      int reason,
      boolean breaking,
      BlockHitResult blockHitResult,
      InteractionHand interactionHand,
      int sequenceId
   ) {
      this.blocks = new LinkedHashMap<>(blocks);
      this.updateNeighbors = updateNeighbors;
      this.preventUpdatesAt = (Set<BlockPos>)(updateNeighbors ? new HashSet<>(preventUpdatesAt) : Set.of());
      this.reason = reason;
      this.breaking = breaking;
      this.blockHit = blockHitResult;
      this.hand = interactionHand;
      this.sequenceId = sequenceId;
   }

   public AxiomServerboundSetBlock(FriendlyByteBuf friendlyByteBuf) {
      this.blocks = friendlyByteBuf.readMap(AxiomServerboundSetBlock::createMap, buf -> buf.readBlockPos(), NetworkHelper::readBlockState);
      this.updateNeighbors = friendlyByteBuf.readBoolean();
      if (this.updateNeighbors) {
         this.preventUpdatesAt = (Set<BlockPos>)friendlyByteBuf.readCollection(AxiomServerboundSetBlock::createSet, buf -> buf.readBlockPos());
      } else {
         this.preventUpdatesAt = Set.of();
      }

      this.reason = friendlyByteBuf.readVarInt();
      this.breaking = friendlyByteBuf.readBoolean();
      this.blockHit = friendlyByteBuf.readBlockHitResult();
      this.hand = (InteractionHand)friendlyByteBuf.readEnum(InteractionHand.class);
      this.sequenceId = friendlyByteBuf.readVarInt();
   }

   public static <K, V> LinkedHashMap<K, V> createMap(int expectedSize) {
      if (expectedSize >= 0 && expectedSize <= 4096) {
         return new LinkedHashMap<>(expectedSize);
      } else {
         throw new RuntimeException("Invalid size: " + expectedSize);
      }
   }

   public static <V> HashSet<V> createSet(int expectedSize) {
      if (expectedSize >= 0 && expectedSize <= 4096) {
         return new HashSet<>(expectedSize);
      } else {
         throw new RuntimeException("Invalid size: " + expectedSize);
      }
   }

   @Override
   public ResourceLocation id() {
      return IDENTIFIER;
   }

   @Override
   public void write(FriendlyByteBuf friendlyByteBuf) {
      friendlyByteBuf.writeMap(this.blocks, (buf, pos) -> buf.writeBlockPos(pos), NetworkHelper::writeBlockState);
      friendlyByteBuf.writeBoolean(this.updateNeighbors);
      if (this.updateNeighbors) {
         friendlyByteBuf.writeCollection(this.preventUpdatesAt, (buf, pos) -> buf.writeBlockPos(pos));
      }

      friendlyByteBuf.writeVarInt(this.reason);
      friendlyByteBuf.writeBoolean(this.breaking);
      friendlyByteBuf.writeBlockHitResult(this.blockHit);
      friendlyByteBuf.writeEnum(this.hand);
      friendlyByteBuf.writeVarInt(this.sequenceId);
   }

   @Override
   public void handle(MinecraftServer server, ServerPlayer player) {
      if (AxiomServerboundPacket.canUseAxiom(player, AxiomPermission.BUILD_PLACE)) {
         if (this.sequenceId >= 0) {
            player.connection.ackBlockChangesUpTo(this.sequenceId);
         }

         if (this.breaking) {
            ServerLevel serverLevel = player.serverLevel();
            BlockState block = serverLevel.getBlockState(this.blockHit.getBlockPos());
            // Respect protection mods: fire the break event and abort if any listener cancels it.
            BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(serverLevel, this.blockHit.getBlockPos(), block, player);
            NeoForge.EVENT_BUS.post(breakEvent);
            if (breakEvent.isCanceled()) {
               return;
            }
         } else {
            // Respect protection mods: fire the right-click event and abort if the use is denied.
            PlayerInteractEvent.RightClickBlock useEvent =
               new PlayerInteractEvent.RightClickBlock(player, this.hand, this.blockHit.getBlockPos(), this.blockHit);
            NeoForge.EVENT_BUS.post(useEvent);
            if (useEvent.isCanceled() || useEvent.getUseBlock() == net.neoforged.neoforge.common.util.TriState.FALSE) {
               return;
            }
         }

         BlockPlaceContext blockPlaceContext = new BlockPlaceContext(player, this.hand, player.getItemInHand(this.hand), this.blockHit);
         ServerLevel level = player.serverLevel();
         if (this.updateNeighbors) {
            if (this.preventUpdatesAt.isEmpty()) {
               for (Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
                  level.setBlock(entry.getKey(), entry.getValue(), 3);
               }
            } else {
               Direction[] directions = Direction.values();
               MutableBlockPos mutable = new MutableBlockPos();
               Map<BlockPos, BlockState> delayedSetWithoutUpdates = new LinkedHashMap<>(Math.min(this.blocks.size(), this.preventUpdatesAt.size()));

               for (Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
                  boolean updateNeighborsForThisBlock = true;

                  for (Direction direction : directions) {
                     if (this.preventUpdatesAt.contains(mutable.setWithOffset((Vec3i)entry.getKey(), direction))) {
                        updateNeighborsForThisBlock = false;
                        break;
                     }
                  }

                  if (this.preventUpdatesAt.contains(entry.getKey())) {
                     delayedSetWithoutUpdates.put(entry.getKey(), entry.getValue());
                     if (!updateNeighborsForThisBlock) {
                        continue;
                     }
                  }

                  level.setBlock(entry.getKey(), entry.getValue(), updateNeighborsForThisBlock ? 3 : 18);
               }

               for (Entry<BlockPos, BlockState> entry : delayedSetWithoutUpdates.entrySet()) {
                  setWithoutUpdates(level, entry.getKey(), entry.getValue());
               }
            }
         } else {
            for (Entry<BlockPos, BlockState> entry : this.blocks.entrySet()) {
               setWithoutUpdates(level, entry.getKey(), entry.getValue());
            }
         }

         if (!this.breaking) {
            BlockPos clickedPos = blockPlaceContext.getClickedPos();
            if (this.blocks.containsKey(clickedPos)) {
               ItemStack inHand = player.getItemInHand(this.hand);
               BlockState blockState = player.serverLevel().getBlockState(clickedPos);
               BlockItem.updateCustomBlockEntityTag(player.serverLevel(), player, clickedPos, inHand);
               BlockEntity blockEntityx = player.serverLevel().getBlockEntity(clickedPos);
               if (blockEntityx != null) {
                  blockEntityx.applyComponentsFromItemStack(inHand);
               }

               Block blockx = blockState.getBlock();
               if (!(blockx instanceof BedBlock) && !(blockx instanceof DoublePlantBlock) && !(blockx instanceof DoorBlock)) {
                  blockState.getBlock().setPlacedBy(player.serverLevel(), clickedPos, blockState, player, inHand);
               }
            }
         }
      }
   }

   private static void setWithoutUpdates(ServerLevel level, BlockPos blockPos, BlockState blockState) {
      int bx = blockPos.getX();
      int by = blockPos.getY();
      int bz = blockPos.getZ();
      int x = bx & 15;
      int y = by & 15;
      int z = bz & 15;
      int cx = bx >> 4;
      int cy = by >> 4;
      int cz = bz >> 4;
      LevelChunk chunk = level.getChunk(cx, cz);
      chunk.setUnsaved(true);
      int sectionIndex = level.getSectionIndexFromSectionY(cy);
      if (sectionIndex >= 0 && sectionIndex < level.getSectionsCount()) {
         LevelChunkSection section = chunk.getSection(sectionIndex);
         boolean hasOnlyAir = section.hasOnlyAir();
         Heightmap worldSurface = null;
         Heightmap oceanFloor = null;
         Heightmap motionBlocking = null;
         Heightmap motionBlockingNoLeaves = null;

         for (Entry<Types, Heightmap> heightmap : chunk.getHeightmaps()) {
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

         BlockState old = section.setBlockState(x, y, z, blockState, true);
         if (blockState != old) {
            Block block = blockState.getBlock();
            motionBlocking.update(x, by, z, blockState);
            motionBlockingNoLeaves.update(x, by, z, blockState);
            oceanFloor.update(x, by, z, blockState);
            worldSurface.update(x, by, z, blockState);
            if (blockState.hasBlockEntity()) {
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
            } else if (old.hasBlockEntity()) {
               chunk.removeBlockEntity(blockPos);
            }

            level.getChunkSource().blockChanged(blockPos);
            if (VersionUtils.hasDifferentLightProperties(chunk, blockPos, old, blockState)) {
               ChunkSkyLightSources sources = chunk.getSkyLightSources();
               if (sources != null) {
                  sources.update(chunk, x, by, z);
               }

               level.getChunkSource().getLightEngine().checkBlock(blockPos);
            }

            Optional<Holder<PoiType>> newPoi = PoiTypes.forState(blockState);
            Optional<Holder<PoiType>> oldPoi = PoiTypes.forState(old);
            if (!Objects.equals(oldPoi, newPoi)) {
               if (oldPoi.isPresent()) {
                  level.getPoiManager().remove(blockPos);
               }

               newPoi.ifPresent(holder -> level.getPoiManager().add(blockPos, holder));
            }
         }

         boolean nowHasOnlyAir = section.hasOnlyAir();
         if (hasOnlyAir != nowHasOnlyAir) {
            level.getChunkSource().getLightEngine().updateSectionStatus(SectionPos.of(cx, cy, cz), nowHasOnlyAir);
         }
      }
   }

   public static void register() {
      AxiomServerboundPacket.register(IDENTIFIER, AxiomServerboundSetBlock::new);
   }
}
