package com.moulberry.axiom.clipboard;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ServerConfig;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.EffectRenderer;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.utils.RenderHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.ClientBlockEntitySerializer;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.LevelChunk.EntityCreationType;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SelectionBuffer {
   SelectionBuffer.Empty EMPTY = new SelectionBuffer.Empty();

   int size();

   @Nullable
   BlockPos center();

   @Nullable
   BlockPos min();

   @Nullable
   BlockPos max();

   SelectionHistoryElement createHistoryElement();

   SelectionBuffer optimize();

   boolean contains(int var1, int var2, int var3);

   boolean isEmpty();

   SelectionBuffer modify(UnaryOperator<ChunkedBooleanRegion> var1, boolean var2);

   SelectionBuffer addAABB(BlockPos var1, BlockPos var2, boolean var3);

   SelectionBuffer subtractAABB(BlockPos var1, BlockPos var2, boolean var3);

   SelectionBuffer intersectAABB(BlockPos var1, BlockPos var2, boolean var3);

   SelectionBuffer addSet(PositionSet var1, boolean var2);

   SelectionBuffer subtractSet(PositionSet var1, boolean var2);

   SelectionBuffer intersectSet(PositionSet var1, boolean var2);

   SelectionBuffer move(int var1, int var2, int var3, boolean var4);

   default void callDelete() {
      this.callDelete(0);
   }

   void callDelete(int var1);

   CompletableFuture<SelectionBuffer.CopyResult> callCopy(boolean var1, boolean var2);

   void forEach(TriIntConsumer var1);

   void render(AxiomWorldRenderContext var1, int var2);

   default void close() {
   }

   default void pushActiveSelectionHistory(BlockPos oldCenter, int oldCount, SelectionHistoryElement oldElement) {
      int newCount = this.size();
      int selectionDelta = newCount - oldCount;
      SelectionHistoryElement newElement = this.createHistoryElement();
      String historyDescription;
      if (selectionDelta > 0) {
         if (oldCount == 0) {
            historyDescription = AxiomI18n.get("axiom.history_description.selected", NumberFormat.getInstance().format((long)newCount));
         } else {
            historyDescription = AxiomI18n.get("axiom.history_description.selected_additional", NumberFormat.getInstance().format((long)selectionDelta));
         }
      } else if (selectionDelta < 0) {
         if (newCount == 0) {
            historyDescription = AxiomI18n.get("axiom.history_description.deselected_all");
         } else {
            historyDescription = AxiomI18n.get("axiom.history_description.deselected", NumberFormat.getInstance().format((long)(-selectionDelta)));
         }
      } else {
         historyDescription = AxiomI18n.get("axiom.history_description.selected", NumberFormat.getInstance().format((long)newCount));
      }

      BlockPos center = this.center();
      if (center == null) {
         if (oldCenter == null) {
            return;
         }

         center = oldCenter;
      }

      Dispatcher.pushActiveSelection(new HistoryEntry<>(newElement, oldElement, center, historyDescription, 0));
   }

   static boolean addAABBToBooleanRegion(ChunkedBooleanRegion selectionRegion, BlockPos min, BlockPos max) {
      boolean changed = false;
      int minX = min.getX();
      int minY = min.getY();
      int minZ = min.getZ();
      int maxX = max.getX();
      int maxY = max.getY();
      int maxZ = max.getZ();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               changed |= selectionRegion.add(x, y, z);
            }
         }
      }

      return changed;
   }

   public record AABB(BlockPos min, BlockPos max) implements SelectionBuffer {
      public AABB(BlockPos min, BlockPos max) {
         if (min.getX() <= max.getX() && min.getY() <= max.getY() && min.getZ() <= max.getZ()) {
            min = min.immutable();
            max = max.immutable();
         } else {
            int minX = Math.min(min.getX(), max.getX());
            int minY = Math.min(min.getY(), max.getY());
            int minZ = Math.min(min.getZ(), max.getZ());
            int maxX = Math.max(min.getX(), max.getX());
            int maxY = Math.max(min.getY(), max.getY());
            int maxZ = Math.max(min.getZ(), max.getZ());
            min = new BlockPos(minX, minY, minZ);
            max = new BlockPos(maxX, maxY, maxZ);
         }

         this.min = min;
         this.max = max;
      }

      @Override
      public int size() {
         return (this.max.getX() - this.min.getX() + 1) * (this.max.getY() - this.min.getY() + 1) * (this.max.getZ() - this.min.getZ() + 1);
      }

      @Override
      public BlockPos center() {
         return new BlockPos(
            Math.floorDiv(this.max.getX() + this.min.getX(), 2),
            Math.floorDiv(this.max.getY() + this.min.getY(), 2),
            Math.floorDiv(this.max.getZ() + this.min.getZ(), 2)
         );
      }

      @Override
      public SelectionHistoryElement createHistoryElement() {
         return new SelectionHistoryElement.AABB(this.min, this.max);
      }

      @Override
      public boolean contains(int x, int y, int z) {
         return x >= this.min.getX() && x <= this.max.getX() && y >= this.min.getY() && y <= this.max.getY() && z >= this.min.getZ() && z <= this.max.getZ();
      }

      @Override
      public boolean isEmpty() {
         return false;
      }

      @Override
      public SelectionBuffer modify(UnaryOperator<ChunkedBooleanRegion> consumer, boolean history) {
         ChunkedBooleanRegion selectionRegion = new ChunkedBooleanRegion();
         SelectionBuffer.addAABBToBooleanRegion(selectionRegion, this.min, this.max);
         return new SelectionBuffer.Set(selectionRegion).modify(consumer, history);
      }

      @Override
      public SelectionBuffer addAABB(BlockPos min, BlockPos max, boolean history) {
         if (aabbCompletelyOverlapsAabb(this.min, this.max, min, max)) {
            return this;
         } else if (aabbCompletelyOverlapsAabb(min, max, this.min, this.max)) {
            SelectionBuffer buffer = new SelectionBuffer.AABB(min, max);
            if (history) {
               buffer.pushActiveSelectionHistory(this.center(), this.size(), this.createHistoryElement());
            }

            return buffer;
         } else {
            if (min.getX() == this.min.getX() && max.getX() == this.max.getX()) {
               if (min.getY() == this.min.getY() && max.getY() == this.max.getY()) {
                  if (areLineSegmentsContinuous(min.getZ(), max.getZ(), this.min.getZ(), this.max.getZ())) {
                     SelectionBuffer buffer = new SelectionBuffer.AABB(
                        new BlockPos(min.getX(), min.getY(), Math.min(min.getZ(), this.min.getZ())),
                        new BlockPos(max.getX(), max.getY(), Math.max(max.getZ(), this.max.getZ()))
                     );
                     if (history) {
                        buffer.pushActiveSelectionHistory(this.center(), this.size(), this.createHistoryElement());
                     }

                     return buffer;
                  }
               } else if (min.getZ() == this.min.getZ()
                  && max.getZ() == this.max.getZ()
                  && areLineSegmentsContinuous(min.getY(), max.getY(), this.min.getY(), this.max.getY())) {
                  SelectionBuffer buffer = new SelectionBuffer.AABB(
                     new BlockPos(min.getX(), Math.min(min.getY(), this.min.getY()), min.getZ()),
                     new BlockPos(max.getX(), Math.max(max.getY(), this.max.getY()), max.getZ())
                  );
                  if (history) {
                     buffer.pushActiveSelectionHistory(this.center(), this.size(), this.createHistoryElement());
                  }

                  return buffer;
               }
            } else if (min.getY() == this.min.getY()
               && max.getY() == this.max.getY()
               && min.getZ() == this.min.getZ()
               && max.getZ() == this.max.getZ()
               && areLineSegmentsContinuous(min.getX(), max.getX(), this.min.getX(), this.max.getX())) {
               SelectionBuffer buffer = new SelectionBuffer.AABB(
                  new BlockPos(Math.min(min.getX(), this.min.getX()), min.getY(), min.getZ()),
                  new BlockPos(Math.max(max.getX(), this.max.getX()), max.getY(), max.getZ())
               );
               if (history) {
                  buffer.pushActiveSelectionHistory(this.center(), this.size(), this.createHistoryElement());
               }

               return buffer;
            }

            ChunkedBooleanRegion selectionRegion = new ChunkedBooleanRegion();
            SelectionBuffer.addAABBToBooleanRegion(selectionRegion, this.min, this.max);
            return new SelectionBuffer.Set(selectionRegion).addAABB(min, max, history);
         }
      }

      @Override
      public SelectionBuffer subtractAABB(BlockPos min, BlockPos max, boolean history) {
         if (!lineSegmentsIntersect(min.getX(), max.getX(), this.min.getX(), this.max.getX())) {
            return this;
         } else if (!lineSegmentsIntersect(min.getY(), max.getY(), this.min.getY(), this.max.getY())) {
            return this;
         } else if (!lineSegmentsIntersect(min.getZ(), max.getZ(), this.min.getZ(), this.max.getZ())) {
            return this;
         } else {
            boolean overlapsMinX = min.getX() <= this.min.getX();
            boolean overlapsMaxX = max.getX() >= this.max.getX();
            boolean completelyOverlapX = overlapsMinX && overlapsMaxX;
            boolean overlapsMinY = min.getY() <= this.min.getY();
            boolean overlapsMaxY = max.getY() >= this.max.getY();
            boolean completelyOverlapY = overlapsMinY && overlapsMaxY;
            boolean overlapsMinZ = min.getZ() <= this.min.getZ();
            boolean overlapsMaxZ = max.getZ() >= this.max.getZ();
            boolean completelyOverlapZ = overlapsMinZ && overlapsMaxZ;
            if (completelyOverlapX && completelyOverlapY && completelyOverlapZ) {
               if (history) {
                  Dispatcher.clearActiveSelectionHistory();
               }

               return EMPTY;
            } else {
               BlockPos oldCenter = this.center();
               int oldCount = this.size();
               SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
               if ((overlapsMinX || overlapsMaxX) && completelyOverlapY && completelyOverlapZ) {
                  int minX = this.min.getX();
                  int maxX = this.max.getX();
                  if (overlapsMinX) {
                     minX = max.getX() + 1;
                  } else {
                     maxX = min.getX() - 1;
                  }

                  SelectionBuffer buffer = new SelectionBuffer.AABB(
                     new BlockPos(minX, this.min.getY(), this.min.getZ()), new BlockPos(maxX, this.max.getY(), this.max.getZ())
                  );
                  if (history) {
                     buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
                  }

                  return buffer;
               } else if (completelyOverlapX && (overlapsMinY || overlapsMaxY) && completelyOverlapZ) {
                  int minY = this.min.getY();
                  int maxY = this.max.getY();
                  if (overlapsMinY) {
                     minY = max.getY() + 1;
                  } else {
                     maxY = min.getY() - 1;
                  }

                  SelectionBuffer buffer = new SelectionBuffer.AABB(
                     new BlockPos(this.min.getX(), minY, this.min.getZ()), new BlockPos(this.max.getX(), maxY, this.max.getZ())
                  );
                  if (history) {
                     buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
                  }

                  return buffer;
               } else if (completelyOverlapX && completelyOverlapY && (overlapsMinZ || overlapsMaxZ)) {
                  int minZ = this.min.getZ();
                  int maxZ = this.max.getZ();
                  if (overlapsMinZ) {
                     minZ = max.getZ() + 1;
                  } else {
                     maxZ = min.getZ() - 1;
                  }

                  SelectionBuffer buffer = new SelectionBuffer.AABB(
                     new BlockPos(this.min.getX(), this.min.getY(), minZ), new BlockPos(this.max.getX(), this.max.getY(), maxZ)
                  );
                  if (history) {
                     buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
                  }

                  return buffer;
               } else {
                  ChunkedBooleanRegion selectionRegion = new ChunkedBooleanRegion();
                  SelectionBuffer.addAABBToBooleanRegion(selectionRegion, this.min, this.max);
                  return new SelectionBuffer.Set(selectionRegion).subtractAABB(min, max, history);
               }
            }
         }
      }

      @Override
      public SelectionBuffer intersectAABB(BlockPos min, BlockPos max, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         int minX = Math.max(min.getX(), this.min.getX());
         int minY = Math.max(min.getY(), this.min.getY());
         int minZ = Math.max(min.getZ(), this.min.getZ());
         int maxX = Math.min(max.getX(), this.max.getX());
         int maxY = Math.min(max.getY(), this.max.getY());
         int maxZ = Math.min(max.getZ(), this.max.getZ());
         if (minX <= maxX && minY <= maxY && minZ <= maxZ) {
            SelectionBuffer buffer = new SelectionBuffer.AABB(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
            if (history) {
               buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
            }

            return buffer;
         } else {
            if (history) {
               Dispatcher.clearActiveSelectionHistory();
            }

            return EMPTY;
         }
      }

      @Override
      public SelectionBuffer addSet(PositionSet set, boolean history) {
         ChunkedBooleanRegion selectionRegion = new ChunkedBooleanRegion();
         SelectionBuffer.addAABBToBooleanRegion(selectionRegion, this.min, this.max);
         return new SelectionBuffer.Set(selectionRegion).addSet(set, history);
      }

      @Override
      public SelectionBuffer subtractSet(PositionSet set, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         ChunkedBooleanRegion region = new ChunkedBooleanRegion();
         int minX = this.min.getX();
         int minY = this.min.getY();
         int minZ = this.min.getZ();
         int maxX = this.max.getX();
         int maxY = this.max.getY();
         int maxZ = this.max.getZ();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  if (!set.contains(x, y, z)) {
                     region.add(x, y, z);
                  }
               }
            }
         }

         SelectionBuffer buffer = new SelectionBuffer.Set(region).optimize();
         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer intersectSet(PositionSet set, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         ChunkedBooleanRegion region = new ChunkedBooleanRegion();
         int minX = this.min.getX();
         int minY = this.min.getY();
         int minZ = this.min.getZ();
         int maxX = this.max.getX();
         int maxY = this.max.getY();
         int maxZ = this.max.getZ();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  if (set.contains(x, y, z)) {
                     region.add(x, y, z);
                  }
               }
            }
         }

         SelectionBuffer buffer = new SelectionBuffer.Set(region).optimize();
         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer move(int x, int y, int z, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         SelectionBuffer buffer = new SelectionBuffer.AABB(this.min.offset(x, y, z), this.max.offset(x, y, z));
         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public void callDelete(int extraModifiers) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            BlockBuffer setOperation = new BlockBuffer();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            int changeCount = 0;
            BlockState air = Blocks.AIR.defaultBlockState();

            for (int x = this.min.getX(); x <= this.max.getX(); x++) {
               for (int y = this.min.getY(); y <= this.max.getY(); y++) {
                  for (int z = this.min.getZ(); z <= this.max.getZ(); z++) {
                     BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
                     if (!block.isAir()) {
                        setOperation.set(x, y, z, air);
                        changeCount++;
                     }
                  }
               }
            }

            String countString = NumberFormat.getInstance().format((long)changeCount);
            String historyDescription = AxiomI18n.get("axiom.history_description.deleted", countString);
            RegionHelper.pushBlockBufferChange(setOperation, this.center(), historyDescription, HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP | extraModifiers, null);
         }
      }

      @Override
      public CompletableFuture<SelectionBuffer.CopyResult> callCopy(boolean cut, boolean copyAir) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world == null) {
            return CompletableFuture.completedFuture(null);
         } else {
            BlockPos offset = this.center();
            BlockBuffer forwards = cut ? new BlockBuffer() : null;
            BlockBuffer backwards = cut ? new BlockBuffer() : null;
            IntWrapper changeCount = cut ? new IntWrapper() : null;
            BlockState air = Blocks.AIR.defaultBlockState();
            LongSet needNbt = new LongOpenHashSet();
            LongSet needChunks = new LongOpenHashSet();
            ChunkedBlockRegion blockBuffer = new ChunkedBlockRegion();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Long2ObjectMap<CompressedBlockEntity> loadedNbt = new Long2ObjectOpenHashMap();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            int minSectionX = this.min.getX() >> 4;
            int minSectionY = Math.max(world.getMinSection(), this.min.getY() >> 4);
            int minSectionZ = this.min.getZ() >> 4;
            int maxSectionX = this.max.getX() >> 4;
            int maxSectionY = Math.min(world.getMaxSection() - 1, this.max.getY() >> 4);
            int maxSectionZ = this.max.getZ() >> 4;
            ServerConfig serverConfig = Axiom.getInstance().serverConfig;

            for (int sx = minSectionX; sx <= maxSectionX; sx++) {
               for (int sz = minSectionZ; sz <= maxSectionZ; sz++) {
                  LevelChunk chunk = (LevelChunk)world.getChunk(sx, sz, ChunkStatus.FULL, false);
                  if (chunk == null) {
                     for (int sy = minSectionY; sy <= maxSectionY; sy++) {
                        needChunks.add(BlockPos.asLong(sx, sy, sz));
                     }
                  } else {
                     for (int sy = minSectionY; sy <= maxSectionY; sy++) {
                        LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                        if (!section.hasOnlyAir() || copyAir) {
                           int lx = Math.max(0, this.min.getX() - sx * 16);
                           int ux = Math.min(15, this.max.getX() - sx * 16);
                           int ly = Math.max(0, this.min.getY() - sy * 16);
                           int uy = Math.min(15, this.max.getY() - sy * 16);
                           int lz = Math.max(0, this.min.getZ() - sz * 16);
                           int uz = Math.min(15, this.max.getZ() - sz * 16);
                           PalettedContainer<BlockState> container = section.getStates();

                           for (int x = lx; x <= ux; x++) {
                              for (int y = ly; y <= uy; y++) {
                                 for (int z = lz; z <= uz; z++) {
                                    BlockState blockState = (BlockState)container.get(x, y, z);
                                    if (copyAir || !blockState.isAir()) {
                                       if (cut) {
                                          forwards.set(x + sx * 16, y + sy * 16, z + sz * 16, air);
                                          backwards.set(x + sx * 16, y + sy * 16, z + sz * 16, blockState);
                                          changeCount.value++;
                                       }

                                       blockBuffer.addBlock(x + sx * 16 - offset.getX(), y + sy * 16 - offset.getY(), z + sz * 16 - offset.getZ(), blockState);
                                       if (serverConfig.blocksWithCustomData().contains(blockState.getBlock())) {
                                          needNbt.add(BlockPos.asLong(x + sx * 16, y + sy * 16, z + sz * 16));
                                       } else if (blockState.hasBlockEntity()) {
                                          mutableBlockPos.set(x + sx * 16, y + sy * 16, z + sz * 16);
                                          BlockEntity blockEntity = chunk.getBlockEntity(mutableBlockPos, EntityCreationType.CHECK);
                                          if (blockEntity != null) {
                                             CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
                                             if (nbt != null) {
                                                if (!nbt.isEmpty()) {
                                                   CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);
                                                   long pos = BlockPos.asLong(
                                                      x + sx * 16 - offset.getX(), y + sy * 16 - offset.getY(), z + sz * 16 - offset.getZ()
                                                   );
                                                   if (cut) {
                                                      backwards.putBlockEntity(x + sx * 16, y + sy * 16, z + sz * 16, compressedBlockEntity);
                                                   }

                                                   loadedNbt.put(pos, compressedBlockEntity);
                                                }
                                             } else {
                                                needNbt.add(mutableBlockPos.asLong());
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

            if (needNbt.isEmpty() && needChunks.isEmpty()) {
               if (cut && !blockBuffer.isEmpty()) {
                  BlockPos newOffset = offset.offset(
                     (blockBuffer.max().getX() + blockBuffer.min().getX()) / 2,
                     (blockBuffer.max().getY() + blockBuffer.min().getY()) / 2,
                     (blockBuffer.max().getZ() + blockBuffer.min().getZ()) / 2
                  );
                  String countString = NumberFormat.getInstance().format((long)changeCount.value);
                  String historyDescription = AxiomI18n.get("axiom.history_description.cut", countString);
                  Dispatcher.push(
                     new HistoryEntry<>(
                        forwards, backwards, newOffset, historyDescription, HistoryEntry.MODIFIER_CUT | HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP
                     )
                  );
               }

               return CompletableFuture.completedFuture(new SelectionBuffer.CopyResult(blockBuffer, loadedNbt, offset.multiply(-1)));
            } else {
               CompletableFuture<SelectionBuffer.CopyResult> future = new CompletableFuture<>();
               int minX = this.min.getX();
               int minY = this.min.getY();
               int minZ = this.min.getZ();
               int maxX = this.max.getX();
               int maxY = this.max.getY();
               int maxZ = this.max.getZ();
               Dispatcher.requestChunkData(
                  needNbt,
                  needChunks,
                  true,
                  (compressedBlockEntities, chunkSections) -> {
                     chunkSections.forEach(
                        (posx, containerx) -> {
                           if (containerx != null) {
                              int sx = BlockPos.getX(posx);
                              int syx = BlockPos.getY(posx);
                              int szx = BlockPos.getZ(posx);
                              int lx = Math.max(0, minX - sx * 16);
                              int ux = Math.min(15, maxX - sx * 16);
                              int lyx = Math.max(0, minY - syx * 16);
                              int uyx = Math.min(15, maxY - syx * 16);
                              int lzx = Math.max(0, minZ - szx * 16);
                              int uzx = Math.min(15, maxZ - szx * 16);

                              for (int xx = lx; xx <= ux; xx++) {
                                 for (int yx = lyx; yx <= uyx; yx++) {
                                    for (int zx = lzx; zx <= uzx; zx++) {
                                       BlockState blockStatex = (BlockState)containerx.get(xx, yx, zx);
                                       if (copyAir || !blockStatex.isAir()) {
                                          if (cut) {
                                             forwards.set(xx + sx * 16, yx + syx * 16, zx + szx * 16, air);
                                             backwards.set(xx + sx * 16, yx + syx * 16, zx + szx * 16, blockStatex);
                                             changeCount.value++;
                                          }

                                          blockBuffer.addBlock(
                                             xx + sx * 16 - offset.getX(), yx + syx * 16 - offset.getY(), zx + szx * 16 - offset.getZ(), blockStatex
                                          );
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     );
                     compressedBlockEntities.forEach((posx, compressedBlockEntityx) -> {
                        int xx = BlockPos.getX(posx);
                        int yx = BlockPos.getY(posx);
                        int zx = BlockPos.getZ(posx);
                        int xo = xx - offset.getX();
                        int yo = yx - offset.getY();
                        int zo = zx - offset.getZ();
                        if (cut) {
                           backwards.putBlockEntity(xx, yx, zx, compressedBlockEntityx);
                        }

                        loadedNbt.put(BlockPos.asLong(xo, yo, zo), compressedBlockEntityx);
                     });
                     if (cut && !blockBuffer.isEmpty()) {
                        BlockPos newOffset = offset.offset(
                           (blockBuffer.max().getX() + blockBuffer.min().getX()) / 2,
                           (blockBuffer.max().getY() + blockBuffer.min().getY()) / 2,
                           (blockBuffer.max().getZ() + blockBuffer.min().getZ()) / 2
                        );
                        String countString = NumberFormat.getInstance().format((long)changeCount.value);
                        String historyDescription = AxiomI18n.get("axiom.history_description.cut", countString);
                        Dispatcher.push(
                           new HistoryEntry<>(
                              forwards, backwards, newOffset, historyDescription, HistoryEntry.MODIFIER_CUT | HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP
                           )
                        );
                     }

                     future.complete(new SelectionBuffer.CopyResult(blockBuffer, loadedNbt, offset.multiply(-1)));
                  }
               );
               return future;
            }
         }
      }

      @Override
      public void forEach(TriIntConsumer consumer) {
         int minX = this.min.getX();
         int minY = this.min.getY();
         int minZ = this.min.getZ();
         int maxX = this.max.getX();
         int maxY = this.max.getY();
         int maxZ = this.max.getZ();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  consumer.accept(x, y, z);
               }
            }
         }
      }

      @Override
      public void render(AxiomWorldRenderContext rc, int effects) {
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         double minX = this.min.getX() - 1.0E-4;
         double minY = this.min.getY() - 1.0E-4;
         double minZ = this.min.getZ() - 1.0E-4;
         double maxX = this.max.getX() + 1 + 1.0E-4;
         double maxY = this.max.getY() + 1 + 1.0E-4;
         double maxZ = this.max.getZ() + 1 + 1.0E-4;
         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(minX - rc.x(), minY - rc.y(), minZ - rc.z());
         RenderHelper.tryApplyModelViewMatrix();
         float sizeX = (float)(maxX - minX);
         float sizeY = (float)(maxY - minY);
         float sizeZ = (float)(maxZ - minZ);
         if ((effects & 4) != 0) {
            AxiomRenderer.setShaderColour(1.0F, 0.9F, 0.12F, 1.0F);
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            Shapes.lineBox(matrices, bufferBuilder, 0.0F, 0.0F, 0.0F, sizeX, sizeY, sizeZ, 1.0F, 1.0F, 1.0F, 0.5F, 1.0F, 1.0F, 1.0F, RenderHelper.baseLineWidth);
            AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(provider.build());
            bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            Shapes.lineBox(
               matrices, bufferBuilder, 0.0F, 0.0F, 0.0F, sizeX, sizeY, sizeZ, 1.0F, 1.0F, 1.0F, 0.15F, 1.0F, 1.0F, 1.0F, RenderHelper.baseLineWidth
            );
            AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
            AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
         }

         EffectRenderer.render((pipeline, target) -> {
            Shapes.shadedBox(provider, matrices.last().pose(), sizeX, sizeY, sizeZ, -1);
            pipeline.render(target, provider.build());
         }, rc.nanos(), effects & -5);
         matrices.popPose();
      }

      @Override
      public SelectionBuffer optimize() {
         if (this.min.getX() > this.max.getX()) {
            this.close();
            return EMPTY;
         } else if (this.min.getY() > this.max.getY()) {
            this.close();
            return EMPTY;
         } else if (this.min.getZ() > this.max.getZ()) {
            this.close();
            return EMPTY;
         } else {
            return this;
         }
      }

      private static boolean aabbCompletelyOverlapsAabb(BlockPos min1, BlockPos max1, BlockPos min2, BlockPos max2) {
         return min1.getX() <= min2.getX()
            && min1.getY() <= min2.getY()
            && min1.getZ() <= min2.getZ()
            && max1.getX() >= max2.getX()
            && max1.getY() >= max2.getY()
            && max1.getZ() >= max2.getZ();
      }

      private static boolean areLineSegmentsContinuous(int min1, int max1, int min2, int max2) {
         int size1 = max1 - min1 + 1;
         int size2 = max2 - min2 + 1;
         float mid1 = min1 + max1;
         float mid2 = min2 + max2;
         float midDiff = Math.abs(mid1 - mid2);
         return midDiff <= size1 + size2;
      }

      private static boolean lineSegmentsIntersect(int min1, int max1, int min2, int max2) {
         int size1 = max1 - min1 + 1;
         int size2 = max2 - min2 + 1;
         float mid1 = min1 + max1;
         float mid2 = min2 + max2;
         float midDiff = Math.abs(mid1 - mid2);
         return midDiff < size1 + size2;
      }
   }

   public record CopyResult(ChunkedBlockRegion chunkedBlockRegion, Long2ObjectMap<CompressedBlockEntity> blockEntities, BlockPos realOffset) {
   }

   public static class Empty implements SelectionBuffer {
      @Override
      public int size() {
         return 0;
      }

      @Nullable
      @Override
      public BlockPos center() {
         return null;
      }

      @Nullable
      @Override
      public BlockPos min() {
         return null;
      }

      @Nullable
      @Override
      public BlockPos max() {
         return null;
      }

      @Override
      public SelectionHistoryElement createHistoryElement() {
         return SelectionHistoryElement.EMPTY;
      }

      @Override
      public boolean contains(int x, int y, int z) {
         return true;
      }

      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public SelectionBuffer modify(UnaryOperator<ChunkedBooleanRegion> consumer, boolean history) {
         return new SelectionBuffer.Set(new ChunkedBooleanRegion()).modify(consumer, history);
      }

      @Override
      public SelectionBuffer addAABB(BlockPos min, BlockPos max, boolean history) {
         SelectionBuffer buffer = new SelectionBuffer.AABB(min, max);
         if (history) {
            buffer.pushActiveSelectionHistory(null, 0, SelectionHistoryElement.EMPTY);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer subtractAABB(BlockPos min, BlockPos max, boolean history) {
         return this;
      }

      @Override
      public SelectionBuffer intersectAABB(BlockPos min, BlockPos max, boolean history) {
         return this;
      }

      @Override
      public SelectionBuffer addSet(PositionSet set, boolean history) {
         SelectionBuffer buffer = new SelectionBuffer.Set(new ChunkedBooleanRegion(set));
         if (history) {
            buffer.pushActiveSelectionHistory(null, 0, SelectionHistoryElement.EMPTY);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer subtractSet(PositionSet set, boolean history) {
         return this;
      }

      @Override
      public SelectionBuffer intersectSet(PositionSet set, boolean history) {
         return this;
      }

      @Override
      public SelectionBuffer move(int x, int y, int z, boolean history) {
         return this;
      }

      @Override
      public void callDelete(int extraModifiers) {
      }

      @Override
      public CompletableFuture<SelectionBuffer.CopyResult> callCopy(boolean cut, boolean copyAir) {
         return CompletableFuture.completedFuture(null);
      }

      @Override
      public void forEach(TriIntConsumer consumer) {
      }

      @Override
      public void render(AxiomWorldRenderContext rc, int effects) {
      }

      @Override
      public SelectionBuffer optimize() {
         return this;
      }
   }

   public static final class Set implements SelectionBuffer {
      public ChunkedBooleanRegion selectionRegion;
      private SelectionHistoryElement cachedHistoryElement = null;

      public Set(ChunkedBooleanRegion selectionRegion) {
         this.selectionRegion = selectionRegion;
      }

      @Override
      public void close() {
         if (this.selectionRegion != null) {
            this.selectionRegion.close();
            this.selectionRegion = null;
         }
      }

      @Override
      public int size() {
         return this.selectionRegion.count();
      }

      @NotNull
      @Override
      public BlockPos center() {
         return this.selectionRegion.getCenter();
      }

      @Nullable
      @Override
      public BlockPos min() {
         return this.selectionRegion.min();
      }

      @Nullable
      @Override
      public BlockPos max() {
         return this.selectionRegion.max();
      }

      @Override
      public SelectionHistoryElement createHistoryElement() {
         if (this.cachedHistoryElement == null) {
            this.cachedHistoryElement = new SelectionHistoryElement.Set(this.selectionRegion.copyPositionSet());
         }

         return this.cachedHistoryElement;
      }

      @Override
      public boolean contains(int x, int y, int z) {
         return this.selectionRegion.contains(x, y, z);
      }

      @Override
      public boolean isEmpty() {
         assert this.selectionRegion.count() > 0;

         return false;
      }

      @Override
      public SelectionBuffer modify(UnaryOperator<ChunkedBooleanRegion> consumer, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         this.cachedHistoryElement = null;
         ChunkedBooleanRegion chunkedBooleanRegion = consumer.apply(this.selectionRegion);
         if (chunkedBooleanRegion != this.selectionRegion) {
            this.selectionRegion.close();
            this.selectionRegion = chunkedBooleanRegion;
         }

         if (this.selectionRegion.count() == 0) {
            if (history) {
               Dispatcher.clearActiveSelectionHistory();
            }

            this.close();
            return EMPTY;
         } else {
            SelectionBuffer buffer = this.optimize();
            if (history) {
               buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
            }

            return buffer;
         }
      }

      @Override
      public SelectionBuffer addAABB(BlockPos min, BlockPos max, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         SelectionBuffer buffer;
         if (min.getX() <= this.selectionRegion.min().getX()
            && min.getY() <= this.selectionRegion.min().getY()
            && min.getZ() <= this.selectionRegion.min().getZ()
            && max.getX() >= this.selectionRegion.max().getX()
            && max.getY() >= this.selectionRegion.max().getY()
            && max.getZ() >= this.selectionRegion.max().getZ()) {
            this.close();
            buffer = new SelectionBuffer.AABB(min, max);
         } else {
            if (!SelectionBuffer.addAABBToBooleanRegion(this.selectionRegion, min, max)) {
               return this;
            }

            this.cachedHistoryElement = null;
            buffer = this.optimize();
         }

         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer subtractAABB(BlockPos min, BlockPos max, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         if (!this.selectionRegion.subtractAABB(min, max)) {
            return this;
         } else {
            this.cachedHistoryElement = null;
            if (this.selectionRegion.count() == 0) {
               if (history) {
                  Dispatcher.clearActiveSelectionHistory();
               }

               this.close();
               return EMPTY;
            } else {
               SelectionBuffer buffer = this.optimize();
               if (history) {
                  buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
               }

               return buffer;
            }
         }
      }

      @Override
      public SelectionBuffer intersectAABB(BlockPos min, BlockPos max, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         if (!this.selectionRegion.intersectAABB(min, max)) {
            return this;
         } else {
            this.cachedHistoryElement = null;
            if (this.selectionRegion.count() == 0) {
               if (history) {
                  Dispatcher.clearActiveSelectionHistory();
               }

               this.close();
               return EMPTY;
            } else {
               SelectionBuffer buffer = this.optimize();
               if (history) {
                  buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
               }

               return buffer;
            }
         }
      }

      @Override
      public SelectionBuffer addSet(PositionSet set, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         this.cachedHistoryElement = null;
         set.forEach(this.selectionRegion::add);
         SelectionBuffer buffer = this.optimize();
         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer subtractSet(PositionSet set, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         ChunkedBooleanRegion region = new ChunkedBooleanRegion();
         this.selectionRegion.forEach((x, y, z) -> {
            if (!set.contains(x, y, z)) {
               region.add(x, y, z);
            }
         });
         SelectionBuffer buffer = new SelectionBuffer.Set(region).optimize();
         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer intersectSet(PositionSet set, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         ChunkedBooleanRegion region = new ChunkedBooleanRegion();
         if (set.count() < this.selectionRegion.count()) {
            set.forEach((x, y, z) -> {
               if (this.selectionRegion.contains(x, y, z)) {
                  region.add(x, y, z);
               }
            });
         } else {
            this.selectionRegion.forEach((x, y, z) -> {
               if (set.contains(x, y, z)) {
                  region.add(x, y, z);
               }
            });
         }

         SelectionBuffer buffer = new SelectionBuffer.Set(region).optimize();
         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public SelectionBuffer move(int x, int y, int z, boolean history) {
         BlockPos oldCenter = this.center();
         int oldCount = this.size();
         SelectionHistoryElement oldElement = history ? this.createHistoryElement() : null;
         ChunkedBooleanRegion region = new ChunkedBooleanRegion();
         this.forEach((x1, y1, z1) -> region.add(x + x1, y + y1, z + z1));
         SelectionBuffer buffer = new SelectionBuffer.Set(region).optimize();
         if (history) {
            buffer.pushActiveSelectionHistory(oldCenter, oldCount, oldElement);
         }

         return buffer;
      }

      @Override
      public void callDelete(int extraModifiers) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world != null) {
            BlockBuffer setOperation = new BlockBuffer();
            AtomicInteger changeCount = new AtomicInteger();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            this.selectionRegion.forEach((x, y, z) -> {
               BlockState block = world.getBlockState(mutableBlockPos.set(x, y, z));
               if (!block.isAir()) {
                  setOperation.set(x, y, z, Blocks.AIR.defaultBlockState());
                  changeCount.incrementAndGet();
               }
            });
            String countString = NumberFormat.getInstance().format((long)changeCount.get());
            String historyDescription = AxiomI18n.get("axiom.history_description.deleted", countString);
            RegionHelper.pushBlockBufferChange(setOperation, this.center(), historyDescription, HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP | extraModifiers, null);
         }
      }

      @Override
      public CompletableFuture<SelectionBuffer.CopyResult> callCopy(boolean cut, boolean copyAir) {
         ClientLevel world = Minecraft.getInstance().level;
         if (world == null) {
            return CompletableFuture.completedFuture(null);
         } else {
            BlockPos offset = this.center();
            BlockBuffer forwards = cut ? new BlockBuffer() : null;
            BlockBuffer backwards = cut ? new BlockBuffer() : null;
            IntWrapper changeCount = cut ? new IntWrapper() : null;
            BlockState air = Blocks.AIR.defaultBlockState();
            LongSet needNbt = new LongOpenHashSet();
            LongSet needChunks = new LongOpenHashSet();
            ChunkedBlockRegion blockBuffer = new ChunkedBlockRegion();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Long2ObjectMap<CompressedBlockEntity> loadedNbt = new Long2ObjectOpenHashMap();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            Long2ObjectMap<PalettedContainer<BlockState>> containers = new Long2ObjectOpenHashMap();
            PositionSet positionSet = this.selectionRegion.unsafeGetPositionSet();
            ServerConfig serverConfig = Axiom.getInstance().serverConfig;
            LongIterator longIterator = positionSet.chunkKeySet().longIterator();

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
                     if (!section.hasOnlyAir() || copyAir) {
                        containers.put(pos, section.getStates());
                     }
                  }
               }
            }

            containers.forEach((pos, container) -> {
               short[] array = positionSet.getChunk(pos);
               if (array != null) {
                  int wcx = BlockPos.getX(pos) * 16;
                  int wcy = BlockPos.getY(pos) * 16;
                  int wcz = BlockPos.getZ(pos) * 16;
                  int offsetX = wcx - offset.getX();
                  int offsetY = wcy - offset.getY();
                  int offsetZ = wcz - offset.getZ();
                  int index = 0;

                  for (int z = 0; z < 16; z++) {
                     for (int y = 0; y < 16; y++) {
                        short v = array[index++];
                        if (v != 0) {
                           for (int x = 0; x < 16; x++) {
                              if ((v & 1 << x) != 0) {
                                 BlockState blockState = (BlockState)container.get(x, y, z);
                                 if (copyAir || !blockState.isAir()) {
                                    if (cut) {
                                       forwards.set(x + wcx, y + wcy, z + wcz, air);
                                       backwards.set(x + wcx, y + wcy, z + wcz, blockState);
                                       changeCount.value++;
                                    }

                                    blockBuffer.addBlock(x + offsetX, y + offsetY, z + offsetZ, blockState);
                                    if (serverConfig.blocksWithCustomData().contains(blockState.getBlock())) {
                                       needNbt.add(BlockPos.asLong(x + wcx, y + wcy, z + wcz));
                                    } else if (blockState.hasBlockEntity()) {
                                       BlockEntity blockEntity = world.getBlockEntity(mutableBlockPos.set(x + wcx, y + wcy, z + wcz));
                                       if (blockEntity != null) {
                                          CompoundTag nbt = ClientBlockEntitySerializer.serialize(blockEntity, world.registryAccess());
                                          if (nbt != null) {
                                             if (!nbt.isEmpty()) {
                                                CompressedBlockEntity compressedBlockEntity = CompressedBlockEntity.compress(nbt, baos);
                                                long blockEntityPos = BlockPos.asLong(x + offsetX, y + offsetY, z + offsetZ);
                                                if (cut) {
                                                   backwards.putBlockEntity(x + wcx, y + wcy, z + wcz, compressedBlockEntity);
                                                }

                                                loadedNbt.put(blockEntityPos, compressedBlockEntity);
                                             }
                                          } else {
                                             needNbt.add(mutableBlockPos.asLong());
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
            });
            if (needNbt.isEmpty() && needChunks.isEmpty()) {
               if (cut && !blockBuffer.isEmpty()) {
                  BlockPos newOffset = offset.offset(
                     (blockBuffer.max().getX() + blockBuffer.min().getX()) / 2,
                     (blockBuffer.max().getY() + blockBuffer.min().getY()) / 2,
                     (blockBuffer.max().getZ() + blockBuffer.min().getZ()) / 2
                  );
                  String countString = NumberFormat.getInstance().format((long)changeCount.value);
                  String historyDescription = AxiomI18n.get("axiom.history_description.cut", countString);
                  Dispatcher.push(
                     new HistoryEntry<>(
                        forwards, backwards, newOffset, historyDescription, HistoryEntry.MODIFIER_CUT | HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP
                     )
                  );
               }

               return CompletableFuture.completedFuture(new SelectionBuffer.CopyResult(blockBuffer, loadedNbt, offset.multiply(-1)));
            } else {
               PositionSet positionSetCopied = positionSet.copy();
               CompletableFuture<SelectionBuffer.CopyResult> future = new CompletableFuture<>();
               Dispatcher.requestChunkData(
                  needNbt,
                  needChunks,
                  true,
                  (compressedBlockEntities, chunkSections) -> {
                     chunkSections.forEach((pos, container) -> {
                        if (container != null) {
                           short[] array = positionSetCopied.getChunk(pos);
                           if (array != null) {
                              int wcx = BlockPos.getX(pos) * 16;
                              int wcy = BlockPos.getY(pos) * 16;
                              int wcz = BlockPos.getZ(pos) * 16;
                              int offsetX = wcx - offset.getX();
                              int offsetY = wcy - offset.getY();
                              int offsetZ = wcz - offset.getZ();
                              int index = 0;

                              for (int z = 0; z < 16; z++) {
                                 for (int y = 0; y < 16; y++) {
                                    short v = array[index++];
                                    if (v != 0) {
                                       for (int x = 0; x < 16; x++) {
                                          if ((v & 1 << x) != 0) {
                                             BlockState blockState = (BlockState)container.get(x, y, z);
                                             if (copyAir || !blockState.isAir()) {
                                                if (cut) {
                                                   forwards.set(x + wcx, y + wcy, z + wcz, air);
                                                   backwards.set(x + wcx, y + wcy, z + wcz, blockState);
                                                   changeCount.value++;
                                                }

                                                blockBuffer.addBlock(x + offsetX, y + offsetY, z + offsetZ, blockState);
                                             }
                                          }
                                       }
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
                        int xo = x - offset.getX();
                        int yo = y - offset.getY();
                        int zo = z - offset.getZ();
                        if (cut) {
                           backwards.putBlockEntity(x, y, z, compressedBlockEntity);
                        }

                        loadedNbt.put(BlockPos.asLong(xo, yo, zo), compressedBlockEntity);
                     });
                     if (cut && !blockBuffer.isEmpty()) {
                        BlockPos newOffset = offset.offset(
                           (blockBuffer.max().getX() + blockBuffer.min().getX()) / 2,
                           (blockBuffer.max().getY() + blockBuffer.min().getY()) / 2,
                           (blockBuffer.max().getZ() + blockBuffer.min().getZ()) / 2
                        );
                        String countString = NumberFormat.getInstance().format((long)changeCount.value);
                        String historyDescriptionx = AxiomI18n.get("axiom.history_description.cut", countString);
                        Dispatcher.push(
                           new HistoryEntry<>(
                              forwards, backwards, newOffset, historyDescriptionx, HistoryEntry.MODIFIER_CUT | HistoryEntry.MODIFIER_SELECT_ON_BACKSTEP
                           )
                        );
                     }

                     future.complete(new SelectionBuffer.CopyResult(blockBuffer, loadedNbt, offset.multiply(-1)));
                  }
               );
               return future;
            }
         }
      }

      @Override
      public void forEach(TriIntConsumer consumer) {
         this.selectionRegion.forEach(consumer);
      }

      @Override
      public void render(AxiomWorldRenderContext rc, int effects) {
         this.selectionRegion.render(rc, Vec3.ZERO, effects);
      }

      @Override
      public SelectionBuffer optimize() {
         if (this.selectionRegion.count() <= 0) {
            this.close();
            return EMPTY;
         } else {
            if (this.selectionRegion.count() > 0) {
               BlockPos max = this.selectionRegion.max();
               BlockPos min = this.selectionRegion.min();
               int volume = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
               if (volume <= this.selectionRegion.count()) {
                  SelectionBuffer aabb = new SelectionBuffer.AABB(this.selectionRegion.min(), this.selectionRegion.max());
                  this.close();
                  return aabb;
               }
            }

            return this;
         }
      }

      @Override
      public boolean equals(Object obj) {
         if (obj == this) {
            return true;
         } else if (obj != null && obj.getClass() == this.getClass()) {
            SelectionBuffer.Set that = (SelectionBuffer.Set)obj;
            return Objects.equals(this.selectionRegion, that.selectionRegion);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.selectionRegion);
      }
   }
}
