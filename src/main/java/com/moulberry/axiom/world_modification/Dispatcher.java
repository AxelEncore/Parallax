package com.moulberry.axiom.world_modification;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionHistoryElement;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.packets.AxiomServerboundRequestChunkData;
import com.moulberry.axiom.packets.AxiomServerboundRequestEntityData;
import com.moulberry.axiom.packets.AxiomServerboundSetBuffer;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.modelling.ModellingTool;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.StringUtils;
import com.moulberry.axiom.world_modification.undo.ModellingAdditionalUndoOperation;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

public class Dispatcher {
   private static final long DATA_REQUEST_TIMEOUT_MILLIS = 60000L;
   private static final HistoryBuffer<SelectionHistoryElement> activeSelectionHistory = new HistoryBuffer<>();
   private static HistoryBuffer<BlockOrBiomeBuffer> history = new HistoryBuffer<>();
   private static int activeSelectionHistoryStart = 0;
   private static final Long2ObjectMap<ChunkDataConsumer> chunkDataCallbacks = new Long2ObjectOpenHashMap();
   private static final Long2LongOpenHashMap chunkDataTime = new Long2LongOpenHashMap();
   private static final Long2ObjectMap<Dispatcher.PendingChunkData> pendingChunkData = new Long2ObjectOpenHashMap();
   private static final Long2ObjectMap<EntityDataConsumer> entityDataCallbacks = new Long2ObjectOpenHashMap();
   private static final Long2LongOpenHashMap entityDataTime = new Long2LongOpenHashMap();
   private static final Long2ObjectMap<Dispatcher.PendingEntityData> pendingEntityData = new Long2ObjectOpenHashMap();
   private static final List<UndoRedoTracer> tracers = new ArrayList<>();
   private static boolean shouldRemovePlacementOnStep;
   private static final List<DispatchTask> dispatchTasks = new ArrayList<>();
   private static int availableDispatchSends = 0;
   private static int delayDueToExhausedDispatchSends = 0;
   public static String historyIdentifier = null;
   private static String pendingHistoryIdentifier = null;
   private static final boolean DO_DIRECT_SINGLEPLAYER_UPDATE = true;

   public static void reset() {
      pendingHistoryIdentifier = null;
      historyIdentifier = null;
      tracers.clear();
      history.clear();
      chunkDataCallbacks.clear();
      pendingChunkData.clear();
      activeSelectionHistory.clear();
      activeSelectionHistoryStart = 0;
      shouldRemovePlacementOnStep = false;
      dispatchTasks.clear();
      availableDispatchSends = 1;
      delayDueToExhausedDispatchSends = 0;
   }

   private static Path getHistoryPath() {
      return Axiom.getInstance().getConfigDirectory().resolve("history").resolve(StringUtils.sanitizePath(historyIdentifier));
   }

   public static void tryLoadHistory(String newHistoryIdentifier) {
      pendingHistoryIdentifier = newHistoryIdentifier;
   }

   private static void applyPendingHistory() {
      if (!Objects.equals(pendingHistoryIdentifier, historyIdentifier)) {
         historyIdentifier = pendingHistoryIdentifier;
         if (historyIdentifier != null) {
            Path path = getHistoryPath();
            history = HistoryIO.loadHistory(path);
         }
      }
   }

   public static void addTracer(UndoRedoTracer tracer) {
      tracers.add(tracer);
   }

   public static void tick() {
      applyPendingHistory();

      for (int i = 0; i < tracers.size(); i++) {
         UndoRedoTracer tracer = tracers.get(i);
         if (tracer.tick()) {
            tracers.remove(i);
            i--;
         }
      }

      if (!chunkDataTime.isEmpty()) {
         long currentTime = System.currentTimeMillis();
         LongSet removeWaitingChunks = new LongArraySet();
         ObjectIterator removeWaitingEntities = chunkDataTime.long2LongEntrySet().iterator();

         while (removeWaitingEntities.hasNext()) {
            it.unimi.dsi.fastutil.longs.Long2LongMap.Entry entry = (it.unimi.dsi.fastutil.longs.Long2LongMap.Entry)removeWaitingEntities.next();
            long id = entry.getLongKey();
            if (!chunkDataCallbacks.containsKey(id)) {
               removeWaitingChunks.add(id);
            } else {
               long time = entry.getLongValue();
               if (currentTime > time) {
                  removeWaitingChunks.add(id);
               }
            }
         }

         LongIterator var11 = removeWaitingChunks.iterator();

         while (var11.hasNext()) {
            long id = (Long)var11.next();
            pendingChunkData.remove(id);
            chunkDataCallbacks.remove(id);
            chunkDataTime.remove(id);
         }

         LongSet removeWaitingEntitiesx = new LongArraySet();
         ObjectIterator var14 = entityDataTime.long2LongEntrySet().iterator();

         while (var14.hasNext()) {
            it.unimi.dsi.fastutil.longs.Long2LongMap.Entry entry = (it.unimi.dsi.fastutil.longs.Long2LongMap.Entry)var14.next();
            long id = entry.getLongKey();
            if (!entityDataCallbacks.containsKey(id)) {
               removeWaitingEntitiesx.add(id);
            } else {
               long time = entry.getLongValue();
               if (currentTime > time) {
                  removeWaitingEntitiesx.add(id);
               }
            }
         }

         LongIterator var15 = removeWaitingEntitiesx.iterator();

         while (var15.hasNext()) {
            long id = (Long)var15.next();
            pendingEntityData.remove(id);
            entityDataCallbacks.remove(id);
            entityDataTime.remove(id);
         }
      }

      if (delayDueToExhausedDispatchSends > 0) {
         delayDueToExhausedDispatchSends--;
      } else {
         runTasks();
      }
   }

   private static void addTask(DispatchTask dispatchTask) {
      dispatchTasks.add(dispatchTask);
      runTasks();
   }

   public static void updateAvailableDispatchSends(int add, int max) {
      availableDispatchSends = (int)Math.min((long)availableDispatchSends + add, (long)max);
   }

   private static void runTasks() {
      if (availableDispatchSends > 0 && !dispatchTasks.isEmpty()) {
         delayDueToExhausedDispatchSends = 0;
         Iterator<DispatchTask> iterator = dispatchTasks.iterator();

         while (iterator.hasNext() && availableDispatchSends > 0) {
            DispatchTask dispatchTask = iterator.next();
            int sent = dispatchTask.tick(availableDispatchSends);
            availableDispatchSends -= sent;
            if (!dispatchTask.isDone()) {
               break;
            }

            iterator.remove();
         }

         if (availableDispatchSends <= 0) {
            delayDueToExhausedDispatchSends = 20;
         }
      }
   }

   public static boolean exhaustedDispatchSends() {
      return availableDispatchSends <= 0 || delayDueToExhausedDispatchSends > 0;
   }

   public static void requestChunkData(LongSet blockEntities, LongSet chunkSections, boolean sendBlockEntitiesInChunks, ChunkDataConsumer callback) {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.REQUEST_CHUNK)) {
         callback.accept(new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap());
      } else {
         if (!AxiomClient.hasPermission(AxiomPermission.CHUNK_REQUESTBLOCKENTITY)) {
            blockEntities = LongSet.of();
            sendBlockEntitiesInChunks = false;
         }

         if (blockEntities.isEmpty() && chunkSections.isEmpty()) {
            callback.accept(new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap());
         } else {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
               long id;
               do {
                  id = ThreadLocalRandom.current().nextLong();
               } while (chunkDataCallbacks.containsKey(id));

               new AxiomServerboundRequestChunkData(id, level.dimension(), blockEntities, chunkSections, sendBlockEntitiesInChunks).send();
               chunkDataCallbacks.put(id, callback);
               chunkDataTime.put(id, System.currentTimeMillis() + 60000L);
            }
         }
      }
   }

   public static void finishRequestChunkData(
      long id, boolean finished, Long2ObjectMap<CompressedBlockEntity> compressedBlockEntityMap, Long2ObjectMap<PalettedContainer<BlockState>> chunkSections
   ) {
      if (finished) {
         Dispatcher.PendingChunkData pending = (Dispatcher.PendingChunkData)pendingChunkData.remove(id);
         ChunkDataConsumer callback = (ChunkDataConsumer)chunkDataCallbacks.remove(id);
         chunkDataTime.remove(id);
         if (callback == null) {
            return;
         }

         if (pending != null) {
            pending.compressedBlockEntityMap.putAll(compressedBlockEntityMap);
            pending.chunkSections.putAll(chunkSections);
            callback.accept(pending.compressedBlockEntityMap, pending.chunkSections);
         } else {
            callback.accept(compressedBlockEntityMap, chunkSections);
         }
      } else if (pendingChunkData.containsKey(id)) {
         Dispatcher.PendingChunkData pendingx = (Dispatcher.PendingChunkData)pendingChunkData.get(id);
         pendingx.compressedBlockEntityMap.putAll(compressedBlockEntityMap);
         pendingx.chunkSections.putAll(chunkSections);
         chunkDataTime.put(id, System.currentTimeMillis() + 60000L);
      } else {
         pendingChunkData.put(id, new Dispatcher.PendingChunkData(compressedBlockEntityMap, chunkSections));
         chunkDataTime.put(id, System.currentTimeMillis() + 60000L);
      }
   }

   public static void requestEntityData(List<UUID> entities, EntityDataConsumer callback) {
      if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.REQUEST_ENTITY)) {
         callback.accept(new HashMap<>());
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            long id;
            do {
               id = ThreadLocalRandom.current().nextLong();
            } while (entityDataCallbacks.containsKey(id));

            new AxiomServerboundRequestEntityData(id, entities).send();
            entityDataCallbacks.put(id, callback);
            entityDataTime.put(id, System.currentTimeMillis() + 60000L);
         }
      }
   }

   public static void finishRequestEntityData(long id, boolean finished, Map<UUID, CompoundTag> entityData) {
      if (finished) {
         Dispatcher.PendingEntityData pending = (Dispatcher.PendingEntityData)pendingEntityData.remove(id);
         EntityDataConsumer callback = (EntityDataConsumer)entityDataCallbacks.remove(id);
         entityDataTime.remove(id);
         if (callback == null) {
            return;
         }

         if (pending != null) {
            pending.entityData.putAll(entityData);
            callback.accept(pending.entityData);
         } else {
            callback.accept(entityData);
         }
      } else if (pendingEntityData.containsKey(id)) {
         Dispatcher.PendingEntityData pendingx = (Dispatcher.PendingEntityData)pendingEntityData.get(id);
         pendingx.entityData.putAll(entityData);
         entityDataTime.put(id, System.currentTimeMillis() + 60000L);
      } else {
         pendingEntityData.put(id, new Dispatcher.PendingEntityData(new HashMap<>(entityData)));
         entityDataTime.put(id, System.currentTimeMillis() + 60000L);
      }
   }

   public static void clear() {
      applyPendingHistory();
      if (historyIdentifier != null) {
         Path path = getHistoryPath();
         HistoryIO.clear(path);
      }

      history.clear();
      clearActiveSelectionHistory();
   }

   public static void push(HistoryEntry<BlockOrBiomeBuffer> entry) {
      applyPendingHistory();
      Level level = Minecraft.getInstance().level;
      if (level != null) {
         if (ClientEvents.c == 0) {
            String c = new String(
               new byte[]{
                  104,
                  116,
                  116,
                  112,
                  115,
                  58,
                  47,
                  47,
                  97,
                  120,
                  105,
                  111,
                  109,
                  46,
                  109,
                  111,
                  117,
                  108,
                  98,
                  101,
                  114,
                  114,
                  121,
                  46,
                  99,
                  111,
                  109,
                  47,
                  97,
                  112,
                  105,
                  47,
                  109,
                  99,
                  97,
                  117,
                  116,
                  104,
                  47,
                  104,
                  97,
                  115,
                  95,
                  99,
                  111,
                  109,
                  109,
                  101,
                  114,
                  99,
                  105,
                  97,
                  108,
                  95,
                  108,
                  105,
                  99,
                  101,
                  110,
                  115,
                  101
               }
            );
            if (c.intern() == c) {
               ClientEvents.c = 18000;
            } else {
               ClientEvents.c = -1;
            }
         }

         if (!AxiomClient.hasPermission(AxiomPermission.BUILD_SECTION)) {
            ChatUtils.error("Server hasn't given you permission to place blocks with Axiom");
         } else {
            int oldSize = history.getSize();
            history.push(entry);
            if (historyIdentifier != null) {
               Path path = getHistoryPath();
               HistoryIO.pushEntry(path, entry, history.getPosition(), oldSize);
            }

            shouldRemovePlacementOnStep = entry.hasModifier(HistoryEntry.MODIFIER_CUT);
            apply(entry.forwards(), false);
            clearActiveSelectionHistory();
         }
      }
   }

   public static void pushActiveSelection(HistoryEntry<SelectionHistoryElement> entry) {
      if (activeSelectionHistory.getSize() == 0) {
         activeSelectionHistoryStart = history.getPosition();
      }

      activeSelectionHistory.push(entry);
      Selection.setShouldRenderSelection(true);
   }

   public static void clearActiveSelectionHistory() {
      activeSelectionHistory.clear();
   }

   private static int getActiveSelectionHistoryStart() {
      if (activeSelectionHistoryStart >= history.getSize()) {
         activeSelectionHistoryStart = history.getSize() - 1;
      }

      return activeSelectionHistoryStart;
   }

   public static int getHistoryPosition(boolean includeToolHistory) {
      int selectionPosition = activeSelectionHistory.getPosition();
      if (selectionPosition >= 0) {
         return getActiveSelectionHistoryStart() + selectionPosition + 1;
      } else {
         if (includeToolHistory && ToolManager.isToolActive()) {
            Tool tool = ToolManager.getCurrentTool();
            if (tool != null) {
               HistoryBuffer<?> toolHistoryBuffer = tool.historyBuffer();
               if (toolHistoryBuffer != null) {
                  return history.getPosition() + toolHistoryBuffer.getPosition() + 1;
               }
            }
         }

         return history.getPosition();
      }
   }

   public static void setHistoryPosition(int position) {
      int delta = position - getHistoryPosition(false);
      if (delta < 0) {
         if (Placement.INSTANCE.isPlacing() && !ToolManager.isToolActive()) {
            Placement.INSTANCE.stopPlacement();
         }

         for (int i = 0; i < -delta; i++) {
            undo(false, false, false);
         }
      } else if (delta > 0) {
         if (Placement.INSTANCE.isPlacing() && !ToolManager.isToolActive()) {
            Placement.INSTANCE.stopPlacement();
         }

         for (int i = 0; i < delta; i++) {
            redo(false, false);
         }
      }
   }

   public static long getHistoryBytes() {
      return history.getSizeInBytes();
   }

   public static int getHistoryDataCount() {
      int toolCount = 0;
      if (ToolManager.isToolActive()) {
         Tool tool = ToolManager.getCurrentTool();
         if (tool != null) {
            HistoryBuffer<?> toolHistoryBuffer = tool.historyBuffer();
            if (toolHistoryBuffer != null) {
               toolCount = toolHistoryBuffer.getSize();
            }
         }
      }

      return activeSelectionHistory.getSize() + history.getSize() + toolCount;
   }

   public static Dispatcher.HistoryData getHistoryData(int index) {
      if (index < 0) {
         return null;
      } else {
         int activeStart = getActiveSelectionHistoryStart();
         if (index > activeStart) {
            int selectionIndex = index - activeStart - 1;
            if (selectionIndex < activeSelectionHistory.getSize()) {
               return new Dispatcher.HistoryData(activeSelectionHistory.getHistoryEntry(selectionIndex), selectionIndex, true);
            }

            index -= activeSelectionHistory.getSize();
         }

         if (index > history.getPosition() && ToolManager.isToolActive()) {
            Tool tool = ToolManager.getCurrentTool();
            if (tool != null) {
               HistoryBuffer<?> toolHistoryBuffer = tool.historyBuffer();
               if (toolHistoryBuffer != null) {
                  int toolHistoryIndex = index - history.getPosition() - 1;
                  if (toolHistoryIndex < toolHistoryBuffer.getSize()) {
                     return new Dispatcher.HistoryData(toolHistoryBuffer.getHistoryEntry(toolHistoryIndex), toolHistoryIndex, true);
                  }

                  index -= toolHistoryBuffer.getSize();
               }
            }
         }

         return index >= history.getSize() ? null : new Dispatcher.HistoryData(history.getHistoryEntry(index), index, false);
      }
   }

   public static void renderTracers(AxiomWorldRenderContext rc) {
      for (UndoRedoTracer tracer : tracers) {
         tracer.render(rc);
      }
   }

   public static UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case UNDO:
            undo(true, true, !EditorUI.isActive());
            return UserAction.ActionResult.USED_STOP;
         case REDO:
            redo(true, !EditorUI.isActive());
            return UserAction.ActionResult.USED_STOP;
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   private static void undo(boolean allowPlacement, boolean allowSelection, boolean fromIngame) {
      applyPendingHistory();
      if (!AxiomClient.hasPermission(AxiomPermission.BUILD_SECTION)) {
         ChatUtils.error("Server hasn't given you permission to place blocks with Axiom");
      } else {
         if (shouldRemovePlacementOnStep) {
            Placement.INSTANCE.stopPlacement();
            shouldRemovePlacementOnStep = false;
         } else if (Placement.INSTANCE.isPlacing() && !ToolManager.isToolActive()) {
            Placement.INSTANCE.stopPlacement();
            return;
         }

         HistoryEntry<SelectionHistoryElement> selectionEntry = activeSelectionHistory.undo(fromIngame ? HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME : 0);
         if (selectionEntry != null) {
            selectionEntry.backwards().applyToSelection();
         } else {
            HistoryEntry<BlockOrBiomeBuffer> entry = history.undo(fromIngame ? HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME : 0);
            if (entry != null) {
               if (activeSelectionHistory.getSize() > 0) {
                  Selection.clearSelection();
               }

               if (historyIdentifier != null) {
                  Path path = getHistoryPath();
                  HistoryIO.setPosition(path, history.getPosition(), history.getSize());
               }

               if (entry.backwards() instanceof DummyBuffer) {
                  return;
               }

               tracers.add(new UndoRedoTracer(Vec3.atCenterOf(entry.origin()), 16711680, true));
               apply(entry.backwards(), allowSelection && entry.hasModifier(HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP));
               if (entry.additionalUndoOperation() != null) {
                  entry.additionalUndoOperation().perform();
               }

               if (allowPlacement
                  && entry.hasModifier(HistoryEntry.MODIFIER_PASTE)
                  && !Placement.INSTANCE.isPlacing()
                  && entry.forwards() instanceof BlockBuffer blockBuffer
                  && blockBuffer.getSectionCount() < 4096) {
                  Placement.INSTANCE.startPlacement(entry.origin(), blockBuffer, entry.description());
                  shouldRemovePlacementOnStep = true;
               }

               if (ToolManager.isToolActive()) {
                  ToolManager.getCurrentTool().afterBlockBufferUndo();
               }
            }
         }
      }
   }

   private static void redo(boolean allowPlacement, boolean fromIngame) {
      applyPendingHistory();
      if (!AxiomClient.hasPermission(AxiomPermission.BUILD_SECTION)) {
         ChatUtils.error("Server hasn't given you permission to place blocks with Axiom");
      } else {
         if (shouldRemovePlacementOnStep) {
            Placement.INSTANCE.stopPlacement();
            shouldRemovePlacementOnStep = false;
         } else if (Placement.INSTANCE.isPlacing() && !ToolManager.isToolActive()) {
            Placement.INSTANCE.stopPlacement();
            return;
         }

         HistoryEntry<SelectionHistoryElement> selectionEntry = activeSelectionHistory.redo(fromIngame ? HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME : 0);
         if (selectionEntry != null) {
            selectionEntry.forwards().applyToSelection();
         } else {
            HistoryEntry<BlockOrBiomeBuffer> entry = history.redo(fromIngame ? HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME : 0);
            if (entry != null) {
               if (activeSelectionHistory.getSize() > 0) {
                  Selection.clearSelection();
               }

               if (historyIdentifier != null) {
                  Path path = getHistoryPath();
                  HistoryIO.setPosition(path, history.getPosition(), history.getSize());
               }

               if (entry.forwards() instanceof DummyBuffer) {
                  return;
               }

               tracers.add(new UndoRedoTracer(Vec3.atCenterOf(entry.origin()), 65280, false));
               if (entry.additionalUndoOperation() instanceof ModellingAdditionalUndoOperation
                  && ToolManager.isToolActive()
                  && ToolManager.getCurrentTool() instanceof ModellingTool modellingTool) {
                  modellingTool.reset();
               }

               apply(entry.forwards(), false);
               if (allowPlacement
                  && entry.hasModifier(HistoryEntry.MODIFIER_CUT)
                  && !Placement.INSTANCE.isPlacing()
                  && entry.backwards() instanceof BlockBuffer blockBuffer
                  && blockBuffer.getSectionCount() < 4096) {
                  Placement.INSTANCE.startPlacement(entry.origin(), blockBuffer, entry.description());
                  shouldRemovePlacementOnStep = true;
               }

               if (ToolManager.isToolActive()) {
                  ToolManager.getCurrentTool().afterBlockBufferRedo();
               }
            }
         }
      }
   }

   public static void resetShouldRemovePlacementOnStep() {
      shouldRemovePlacementOnStep = false;
   }

   public static void apply(BlockOrBiomeBuffer buffer, boolean select) {
      if (buffer instanceof BlockBuffer blockBuffer) {
         applyBlockBuffer(blockBuffer, select);
      } else {
         if (!(buffer instanceof BiomeBuffer biomeBuffer)) {
            throw new FaultyImplementationError();
         }

         applyBiomeBuffer(biomeBuffer);
      }
   }

   private static void applyBiomeBuffer(BiomeBuffer biomeBuffer) {
      if (Minecraft.getInstance().hasSingleplayerServer() && dispatchTasks.isEmpty()) {
         ResourceKey<Level> worldKey = Minecraft.getInstance().level.dimension();
         ServerLevel world = Minecraft.getInstance().getSingleplayerServer().getLevel(worldKey);
         Objects.requireNonNull(world);
         Minecraft.getInstance().getSingleplayerServer().submit(() -> {
            try {
               applyBiomeBufferSingleplayer(biomeBuffer, world);
            } catch (Exception var3x) {
               var3x.printStackTrace();
            }
         });
      } else {
         ResourceKey<Level> worldKey = Minecraft.getInstance().level.dimension();
         addTask(DispatchTask.blockOrBiome(worldKey, biomeBuffer));
      }
   }

   private static void applyBiomeBufferSingleplayer(BiomeBuffer buffer, ServerLevel world) {
      Set<LevelChunk> changedChunks = new HashSet<>();
      int minSection = world.getMinSection();
      int maxSection = world.getMaxSection() - 1;
      Optional<Registry<Biome>> registryOptional = world.registryAccess().registry(Registries.BIOME);
      if (!registryOptional.isEmpty()) {
         Registry<Biome> registry = registryOptional.get();
         buffer.forEachEntry((x, y, z, biome) -> {
            int cy = y >> 2;
            if (cy >= minSection && cy <= maxSection) {
               LevelChunk chunkx = (LevelChunk)world.getChunk(x >> 2, z >> 2, ChunkStatus.FULL, false);
               if (chunkx != null) {
                  LevelChunkSection section = chunkx.getSection(cy - minSection);
                  PalettedContainer<Holder<Biome>> container = (PalettedContainer<Holder<Biome>>)section.getBiomes();
                  Optional<Reference<Biome>> holder = registry.getHolder(biome);
                  if (holder.isPresent()) {
                     container.set(x & 3, y & 3, z & 3, holder.get());
                     changedChunks.add(chunkx);
                  }
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

   public static void applyBlockBuffer(BlockBuffer buffer, boolean select) {
      if (Minecraft.getInstance().hasSingleplayerServer() && dispatchTasks.isEmpty()) {
         ResourceKey<Level> worldKey = Minecraft.getInstance().level.dimension();
         ChunkedBooleanRegion selection = select ? new ChunkedBooleanRegion() : null;
         UUID localPlayerUuid = Minecraft.getInstance().player.getUUID();
         IntegratedServer integratedServer = Minecraft.getInstance().getSingleplayerServer();
         integratedServer.submit(() -> {
            ServerLevel world = integratedServer.getLevel(worldKey);
            if (world != null) {
               ServerPlayer player = integratedServer.getPlayerList().getPlayer(localPlayerUuid);

               try {
                  AxiomServerboundSetBuffer.applyBlockBufferServer(buffer, world, selection, player);
                  if (selection != null) {
                     Minecraft.getInstance().submit(() -> {
                        Selection.clearSelection();
                        if (selection.count() <= 0) {
                           selection.close();
                        } else {
                           Selection.set(selection);
                        }
                     });
                  }
               } catch (Exception var8x) {
                  var8x.printStackTrace();
               }
            }
         });
      } else {
         if (select) {
            BlockState emptyState = BlockBuffer.EMPTY_STATE;
            ChunkedBooleanRegion selection = new ChunkedBooleanRegion();
            ObjectIterator localPlayerUuid = buffer.entrySet().iterator();

            while (localPlayerUuid.hasNext()) {
               Entry<PalettedContainer<BlockState>> entry = (Entry<PalettedContainer<BlockState>>)localPlayerUuid.next();
               int cx = BlockPos.getX(entry.getLongKey()) * 16;
               int cy = BlockPos.getY(entry.getLongKey()) * 16;
               int cz = BlockPos.getZ(entry.getLongKey()) * 16;
               PalettedContainer<BlockState> container = (PalettedContainer<BlockState>)entry.getValue();

               for (int x = 0; x < 16; x++) {
                  for (int y = 0; y < 16; y++) {
                     for (int z = 0; z < 16; z++) {
                        BlockState blockState = (BlockState)container.get(x, y, z);
                        if (blockState != emptyState) {
                           selection.add(cx + x, cy + y, cz + z);
                        }
                     }
                  }
               }
            }

            Selection.clearSelection();
            Selection.set(selection);
         }

         ResourceKey<Level> worldKey = Minecraft.getInstance().level.dimension();
         addTask(DispatchTask.blockOrBiome(worldKey, buffer));
      }
   }

   public record HistoryData(HistoryEntry<?> entry, int position, boolean isActiveSelection) {
      public String getIndexIdentifier() {
         if (!this.isActiveSelection) {
            return this.position + "";
         } else if (this.position == 0) {
            return "a";
         } else {
            int position = this.position;

            StringBuilder identifier;
            for (identifier = new StringBuilder(); position > 0; position /= 26) {
               int modulo = position % 26;
               identifier.insert(0, (char)(97 + modulo));
            }

            return identifier.toString();
         }
      }
   }

   private record PendingChunkData(Long2ObjectMap<CompressedBlockEntity> compressedBlockEntityMap, Long2ObjectMap<PalettedContainer<BlockState>> chunkSections) {
   }

   private record PendingEntityData(Map<UUID, CompoundTag> entityData) {
   }
}
