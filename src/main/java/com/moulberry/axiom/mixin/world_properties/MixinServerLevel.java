package com.moulberry.axiom.mixin.world_properties;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.annotations.ServerAnnotations;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.hooks.ServerLevelExt;
import com.moulberry.axiom.hooks.ThreadedLevelLightEngineExt;
import com.moulberry.axiom.marker.MarkerData;
import com.moulberry.axiom.packets.AxiomClientboundMarkerData;
import com.moulberry.axiom.utils.StarlightHelper;
import com.moulberry.axiom.world_properties.server.ServerWorldPropertiesRegistry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerLevel.class})
public abstract class MixinServerLevel extends Level implements ServerLevelExt {
   @Shadow
   @Final
   private List<ServerPlayer> players;
   @Unique
   private ServerWorldPropertiesRegistry worldPropertiesRegistry;
   @Unique
   private final Map<UUID, MarkerData> previousMarkerData = new HashMap<>();
   @Unique
   private final LongSet pendingChunksToSend = new LongOpenHashSet();
   @Unique
   private final Long2ObjectMap<short[][]> pendingLightSectionUpdates = new Long2ObjectOpenHashMap();
   @Unique
   private final LongSet pendingLightChunks = new LongOpenHashSet();
   @Unique
   private final List<CompletableFuture<?>> waitForPendingLightTasks = new ArrayList<>();
   @Unique
   private final LongSet starlightRelightChunks = new LongOpenHashSet();
   private static boolean promptedForStarlightInstall = false;
   @Unique
   private int totalLightUpdates = 0;
   @Unique
   private int completedLightUpdates = 0;
   @Unique
   private int ticksSinceLightUpdate = 0;
   private static final TicketType<ChunkPos> RELIGHT = TicketType.create("axiom_relight", Comparator.comparingLong(ChunkPos::toLong), 300);

   protected MixinServerLevel(
      WritableLevelData writableLevelData,
      ResourceKey<Level> resourceKey,
      RegistryAccess registryAccess,
      Holder<DimensionType> holder,
      Supplier<ProfilerFiller> supplier,
      boolean bl,
      boolean bl2,
      long l,
      int i
   ) {
      super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
   }

   @Shadow
   public abstract ServerChunkCache getChunkSource();

   @Shadow
   public abstract List<ServerPlayer> players();

   @Shadow
   protected abstract LevelEntityGetter<Entity> getEntities();

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void afterInit(CallbackInfo ci) {
      this.worldPropertiesRegistry = new ServerWorldPropertiesRegistry((ServerLevel)(Object)this);
   }

   @Override
   public ServerWorldPropertiesRegistry axiom$getWorldProperties() {
      return this.worldPropertiesRegistry;
   }

   @Inject(
      method = {"addPlayer"},
      at = {@At("HEAD")}
   )
   public void onPlayerJoinedWorld(ServerPlayer serverPlayer, CallbackInfo ci) {
      this.worldPropertiesRegistry.registerFor(serverPlayer);
      if (!this.previousMarkerData.isEmpty()) {
         List<MarkerData> markerData = new ArrayList<>(this.previousMarkerData.values());
         new AxiomClientboundMarkerData(markerData, Set.of()).send(serverPlayer);
      }

      ServerAnnotations.sendAll((ServerLevel)(Object)this, serverPlayer);
   }

   @Override
   public void axiom$processTasks() {
      this.updateMarkerEntities();
      this.updateLighting();
   }

   @Unique
   private void updateMarkerEntities() {
      List<MarkerData> changedData = new ArrayList<>();
      Set<UUID> allMarkers = new HashSet<>();

      for (Entity entity : this.getEntities().getAll()) {
         if (entity instanceof Marker marker) {
            MarkerData currentData = MarkerData.createFrom(marker);
            MarkerData previousData = this.previousMarkerData.get(marker.getUUID());
            if (!Objects.equals(currentData, previousData)) {
               this.previousMarkerData.put(marker.getUUID(), currentData);
               changedData.add(currentData);
            }

            allMarkers.add(marker.getUUID());
         }
      }

      Set<UUID> oldUuids = new HashSet<>(this.previousMarkerData.keySet());
      oldUuids.removeAll(allMarkers);
      this.previousMarkerData.keySet().removeAll(oldUuids);
      if (!changedData.isEmpty() || !oldUuids.isEmpty()) {
         AxiomClientboundMarkerData dataPacket = new AxiomClientboundMarkerData(changedData, oldUuids);

         for (ServerPlayer player : this.players) {
            dataPacket.send(player);
         }
      }
   }

   @Override
   public void axiom$markChunkDirty(int cx, int cz) {
      this.pendingChunksToSend.add(ChunkPos.asLong(cx, cz));
   }

   @Override
   public short[] axiom$getPendingLightUpdates(int cx, int cy, int cz) {
      short[][] allUpdates = (short[][])this.pendingLightSectionUpdates.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new short[this.getSectionsCount()][]);
      short[] section = allUpdates[cy - this.getMinSection()];
      if (section == null) {
         section = new short[256];
         allUpdates[cy - this.getMinSection()] = section;
         this.totalLightUpdates++;
      }

      return section;
   }

   @Override
   public void axiom$relightChunkStarlight(int cx, int cz) {
      this.starlightRelightChunks.add(ChunkPos.asLong(cx, cz));
   }

   @Unique
   private void updateLighting() {
      ChunkMap chunkMap = this.getChunkSource().chunkMap;
      LongIterator longIterator = this.pendingChunksToSend.longIterator();

      while (longIterator.hasNext()) {
         ChunkPos chunkPos = new ChunkPos(longIterator.nextLong());
         List<ServerPlayer> players = chunkMap.getPlayers(chunkPos, false);
         if (!players.isEmpty()) {
            LevelChunk chunk = this.getChunk(chunkPos.x, chunkPos.z);
            ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, this.getLightEngine(), null, null);

            for (ServerPlayer player : players) {
               player.connection.send(packet);
            }
         }
      }

      this.pendingChunksToSend.clear();
      this.waitForPendingLightTasks.removeIf(CompletableFuture::isDone);
      if (!this.waitForPendingLightTasks.isEmpty()) {
         this.ticksSinceLightUpdate++;
      } else if (this.pendingLightSectionUpdates.isEmpty() && this.starlightRelightChunks.isEmpty()) {
         this.completedLightUpdates = 0;
         this.totalLightUpdates = 0;
         this.ticksSinceLightUpdate = 0;
         if (!this.pendingLightChunks.isEmpty()) {
            LongSet sentLitChunks = new LongOpenHashSet();
            longIterator = this.pendingLightChunks.longIterator();

            while (longIterator.hasNext()) {
               ChunkPos centerChunkPos = new ChunkPos(longIterator.nextLong());
               this.getChunkSource()
                  .chunkMap
                  .getDistanceManager()
                  .removeTicket(RELIGHT, centerChunkPos, ChunkLevel.byStatus(ChunkStatus.LIGHT) - 1, centerChunkPos);

               for (int x = -1; x <= 1; x++) {
                  for (int z = -1; z <= 1; z++) {
                     ChunkPos chunkPos = new ChunkPos(centerChunkPos.x + x, centerChunkPos.z + z);
                     if (sentLitChunks.add(chunkPos.toLong())) {
                        List<ServerPlayer> players = chunkMap.getPlayers(chunkPos, false);
                        if (!players.isEmpty()) {
                           ClientboundLightUpdatePacket packet = new ClientboundLightUpdatePacket(chunkPos, this.getLightEngine(), null, null);

                           for (ServerPlayer player : players) {
                              player.connection.send(packet);
                           }
                        }
                     }
                  }
               }
            }

            this.pendingLightChunks.clear();
         }
      } else {
         this.ticksSinceLightUpdate++;
         ThreadedLevelLightEngine lightEngine = this.getChunkSource().getLightEngine();
         boolean hasStarlight = Axiom.hasStarlight();
         if (hasStarlight) {
            if (!this.pendingLightSectionUpdates.isEmpty()) {
               throw new FaultyImplementationError();
            }

            Set<ChunkPos> chunksToRelight = new HashSet<>();
            LongIterator iterator = this.starlightRelightChunks.iterator();

            while (iterator.hasNext()) {
               long pos = iterator.nextLong();
               ChunkPos chunkPos = new ChunkPos(pos);
               chunksToRelight.add(chunkPos);
               if (this.pendingLightChunks.add(pos)) {
                  this.getChunkSource().chunkMap.getDistanceManager().addTicket(RELIGHT, chunkPos, ChunkLevel.byStatus(ChunkStatus.LIGHT) - 1, chunkPos);
               }
            }

            this.starlightRelightChunks.clear();
            CompletableFuture<Void> future = new CompletableFuture<>();
            this.waitForPendingLightTasks.add(future);
            StarlightHelper.relightChunks(lightEngine, chunksToRelight, chunkPos -> {}, num -> future.complete(null));
         } else {
            if (!this.starlightRelightChunks.isEmpty()) {
               throw new FaultyImplementationError();
            }

            int doTaskCount = Math.max((this.totalLightUpdates - this.completedLightUpdates) / 100, 8);
            ObjectIterator<Entry<short[][]>> iterator = this.pendingLightSectionUpdates.long2ObjectEntrySet().iterator();

            while (iterator.hasNext()) {
               Entry<short[][]> entry = (Entry<short[][]>)iterator.next();
               long pos = entry.getLongKey();
               short[][] all = (short[][])entry.getValue();
               int cx = ChunkPos.getX(pos);
               int cz = ChunkPos.getZ(pos);
               boolean allEmpty = true;
               boolean finished = false;

               for (int i = all.length - 1; i >= 0; i--) {
                  if (i == 0) {
                     finished = true;
                  }

                  short[] array = all[i];
                  if (array != null) {
                     all[i] = null;
                     boolean empty = true;

                     for (short v : array) {
                        if (v != 0) {
                           empty = false;
                           allEmpty = false;
                           break;
                        }
                     }

                     this.completedLightUpdates++;
                     if (!empty) {
                        ((ThreadedLevelLightEngineExt)lightEngine).axiom$checkSectionBlocks(cx, i + this.getMinSection(), cz, array);
                        if (--doTaskCount <= 0) {
                           break;
                        }
                     }
                  }
               }

               if (!allEmpty) {
                  if (this.pendingLightChunks.add(ChunkPos.asLong(cx, cz))) {
                     ChunkPos chunkPos = new ChunkPos(cx, cz);
                     this.getChunkSource().chunkMap.getDistanceManager().addTicket(RELIGHT, chunkPos, ChunkLevel.byStatus(ChunkStatus.LIGHT) - 1, chunkPos);
                  }

                  CompletableFuture<?> future = ((ThreadedLevelLightEngineExt)lightEngine).axiom$waitForPendingTasks(cx, cz);
                  this.waitForPendingLightTasks.add(future);
               }

               if (finished) {
                  iterator.remove();
               }

               if (doTaskCount == 0) {
                  break;
               }
            }
         }

         lightEngine.tryScheduleUpdate();
         if (this.totalLightUpdates > 64 && this.ticksSinceLightUpdate > 20) {
            int percentage = 100 * this.completedLightUpdates / this.totalLightUpdates;
            String text = "Lighting: " + this.completedLightUpdates + "/" + this.totalLightUpdates + " (" + percentage + "%)";
            Component component = Component.literal(text).withStyle(ChatFormatting.YELLOW);

            for (ServerPlayer player : this.players) {
               if (player.isSpectator()) {
                  player.displayClientMessage(component, true);
               }
            }
         }
      }
   }
}
