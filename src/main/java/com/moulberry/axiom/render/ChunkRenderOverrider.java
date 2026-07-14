package com.moulberry.axiom.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDraw;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipeline;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.utils.IrisApiWrapper;
import com.moulberry.axiom.utils.NvidiumApiWrapper;
import com.moulberry.axiom.utils.RenderHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ChunkRenderOverrider {
   private static final Position2ObjectMap<BlockState> blocksOverride = new Position2ObjectMap<>(k -> new BlockState[4096]);
   private static final AtomicBoolean stopBuilding = new AtomicBoolean(false);
   private static final ExecutorService executor = Executors.newSingleThreadExecutor();
   private static final Lock lock = new ReentrantLock();
   private static String lockedBy = null;
   private static ChunkRenderOverrider.BooleanCutoutMode pendingBooleanCutoutMode = null;
   private static boolean pendingClear;
   private static final SectionBufferBuilderPack chunkBufferBuilderPack = new SectionBufferBuilderPack();
   private static final Long2ObjectMap<ChunkRenderOverrider.RenderedChunkData> chunkData = new Long2ObjectOpenHashMap();
   private static LongSet dirtyChunks = new LongOpenHashSet();
   private static Future<ChunkRenderOverrider.CompileData> compileFuture = null;
   private static final EnumSet<ChunkRenderOverrider.AxiomChunkOverrideLayer> globalHasBlocks = EnumSet.noneOf(
      ChunkRenderOverrider.AxiomChunkOverrideLayer.class
   );
   private static long lastSortMillis;
   private static float lastSortX;
   private static float lastSortY;
   private static float lastSortZ;
   private static int lastSortChunkX;
   private static int lastSortChunkY;
   private static int lastSortChunkZ;
   private static boolean overridenChunksChanged = false;
   private static boolean overridenChunksChangedLast = false;
   private static boolean disabled = false;
   private static final List<ChunkRenderOverrider.RenderedChunkData> sortedChunkData = new ArrayList<>();

   private static BlockState[] loadBlocks(long k) {
      BlockState[] array = new BlockState[4096];
      if (Minecraft.getInstance().level != null) {
         BlockPos pos = BlockPos.of(k);
         ChunkAccess chunk = Minecraft.getInstance().level.getChunk(pos.getX(), pos.getZ(), ChunkStatus.FULL, false);
         if (chunk != null) {
            int sectionIndex = chunk.getSectionIndexFromSectionY(pos.getY());
            PalettedContainer<BlockState> states = chunk.getSection(sectionIndex).getStates();
            int index = 0;

            for (int z = 0; z < 16; z++) {
               for (int y = 0; y < 16; y++) {
                  for (int x = 0; x < 16; x++) {
                     array[index++] = (BlockState)states.get(x, y, z);
                  }
               }
            }

            return array;
         }
      }

      Arrays.fill(array, Blocks.AIR.defaultBlockState());
      return array;
   }

   public static void acquire(String identifier) {
      if (lockedBy == null && lock.tryLock()) {
         lockedBy = identifier;
      } else {
         throw new Error(
            "ChunkRenderOverrider: '"
               + identifier
               + "' tried to acquire() when it shouldn't have OR '"
               + lockedBy
               + "' forgot to call release()... naughty naughty"
         );
      }
   }

   public static void release(String identifier) {
      if (!identifier.equals(lockedBy)) {
         throw new Error("ChunkRenderOverrider: '" + identifier + "' tried to release() even though '" + lockedBy + "' owns it");
      } else {
         lockedBy = null;
         clear();
         lock.unlock();
      }
   }

   public static void invalidateChunkSection(int x, int y, int z) {
      if (!chunkData.isEmpty()) {
         long key = BlockPos.asLong(x, y, z);
         if (chunkData.containsKey(key)) {
            dirtyChunks.add(key);
         }
      }
   }

   public static void clear() {
      if (compileFuture != null) {
         stopBuilding.set(true);
      }

      blocksOverride.clear();
      dirtyChunks.clear();
      pendingBooleanCutoutMode = null;
      pendingClear = true;
      overridenChunksChanged = true;
   }

   private static void unhideAllNvidium() {
      LongIterator longIterator = chunkData.keySet().longIterator();

      while (longIterator.hasNext()) {
         long pos = longIterator.nextLong();
         NvidiumApiWrapper.showSection(BlockPos.getX(pos), BlockPos.getY(pos), BlockPos.getZ(pos));
      }
   }

   public static boolean isOverridingSection(int sx, int sy, int sz) {
      return !disabled && chunkData.containsKey(BlockPos.asLong(sx, sy, sz));
   }

   public static boolean hasOverridenChunksChanged() {
      return overridenChunksChanged || overridenChunksChangedLast;
   }

   public static void afterRenderLevel() {
      overridenChunksChangedLast = overridenChunksChanged;
      overridenChunksChanged = false;
   }

   public static void render(
      ChunkRenderOverrider.AxiomChunkOverrideLayer layer, RenderTarget target, Matrix4f modelViewMatrix, double cameraX, double cameraY, double cameraZ
   ) {
      if (!Axiom.configuration.internal.disableChunkRenderOverrider && !IrisApiWrapper.isUsingShaders()) {
         disabled = false;
         if (!sortedChunkData.isEmpty()) {
            if (globalHasBlocks.contains(layer)) {
               AxiomRenderPipeline pipeline = switch (layer) {
                  case SOLID -> AxiomRenderPipelines.TERRAIN_SOLID_PIPELINE;
                  case CUTOUT_MIPPED -> AxiomRenderPipelines.TERRAIN_CUTOUT_MIPPED_PIPELINE;
                  case TRIPWIRE -> AxiomRenderPipelines.TERRAIN_TRIPWIRE_PIPELINE;
                  case CUTOUT -> AxiomRenderPipelines.TERRAIN_CUTOUT_PIPELINE;
                  case TRANSLUCENT -> AxiomRenderPipelines.TERRAIN_TRANSLUCENT_PIPELINE;
               };
               List<AxiomDraw> axiomDraws = new ArrayList<>(sortedChunkData.size());
               if (layer.needsSorting) {
                  for (int i = sortedChunkData.size() - 1; i >= 0; i--) {
                     ChunkRenderOverrider.RenderedChunkData chunkDatum = sortedChunkData.get(i);
                     AxiomDrawBuffer buffer = chunkDatum.buffers.get(layer);
                     if (buffer != null) {
                        Vector3f modelOffset = new Vector3f(
                           (float)(chunkDatum.offsetX - cameraX), (float)(chunkDatum.offsetY - cameraY), (float)(chunkDatum.offsetZ - cameraZ)
                        );
                        axiomDraws.add(new AxiomDraw(buffer, null, modelOffset, new BlockPos(chunkDatum.offsetX, chunkDatum.offsetY, chunkDatum.offsetZ)));
                     }
                  }
               } else {
                  for (ChunkRenderOverrider.RenderedChunkData chunkDatum : sortedChunkData) {
                     AxiomDrawBuffer buffer = chunkDatum.buffers.get(layer);
                     if (buffer != null) {
                        Vector3f modelOffset = new Vector3f(
                           (float)(chunkDatum.offsetX - cameraX), (float)(chunkDatum.offsetY - cameraY), (float)(chunkDatum.offsetZ - cameraZ)
                        );
                        axiomDraws.add(new AxiomDraw(buffer, null, modelOffset, new BlockPos(chunkDatum.offsetX, chunkDatum.offsetY, chunkDatum.offsetZ)));
                     }
                  }
               }

               RenderHelper.pushModelViewMatrix(modelViewMatrix);
               AxiomRenderer.setMainTexture(TextureAtlas.LOCATION_BLOCKS);
               AxiomRenderer.renderPipeline(pipeline, target, axiomDraws);
               RenderHelper.popModelViewStack();
            }
         }
      } else {
         disabled = true;
      }
   }

   public static void revertBlock(int x, int y, int z) {
      if (!Axiom.configuration.internal.disableChunkRenderOverrider) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && y >= level.getMinBuildHeight() && y <= level.getMaxBuildHeight() - 1) {
            int cx = x >> 4;
            int cy = y >> 4;
            int cz = z >> 4;
            blocksOverride.put(x, y, z, null);
            dirtyChunks.add(BlockPos.asLong(cx, cy, cz));
            int px = x + 1 >> 4;
            if (px != cx) {
               long pos = BlockPos.asLong(px, cy, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int mx = x - 1 >> 4;
            if (mx != cx) {
               long pos = BlockPos.asLong(mx, cy, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int py = y + 1 >> 4;
            if (py != cy && py <= level.getMaxSection() - 1) {
               long pos = BlockPos.asLong(cx, py, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int my = y - 1 >> 4;
            if (my != cy && my >= level.getMinSection()) {
               long pos = BlockPos.asLong(cx, my, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int pz = z + 1 >> 4;
            if (pz != cz) {
               long pos = BlockPos.asLong(cx, cy, pz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int mz = z - 1 >> 4;
            if (mz != cz) {
               long pos = BlockPos.asLong(cx, cy, mz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }
         }
      }
   }

   public static void setBlock(int x, int y, int z, BlockState block) {
      if (!Axiom.configuration.internal.disableChunkRenderOverrider) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null && y >= level.getMinBuildHeight() && y <= level.getMaxBuildHeight() - 1) {
            int cx = x >> 4;
            int cy = y >> 4;
            int cz = z >> 4;
            blocksOverride.put(x, y, z, block);
            dirtyChunks.add(BlockPos.asLong(cx, cy, cz));
            int px = x + 1 >> 4;
            if (px != cx) {
               long pos = BlockPos.asLong(px, cy, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int mx = x - 1 >> 4;
            if (mx != cx) {
               long pos = BlockPos.asLong(mx, cy, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int py = y + 1 >> 4;
            if (py != cy && py <= level.getMaxSection() - 1) {
               long pos = BlockPos.asLong(cx, py, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int my = y - 1 >> 4;
            if (my != cy && my >= level.getMinSection()) {
               long pos = BlockPos.asLong(cx, my, cz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int pz = z + 1 >> 4;
            if (pz != cz) {
               long pos = BlockPos.asLong(cx, cy, pz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }

            int mz = z - 1 >> 4;
            if (mz != cz) {
               long pos = BlockPos.asLong(cx, cy, mz);
               blocksOverride.getOrCreateChunk(pos);
               dirtyChunks.add(pos);
            }
         }
      }
   }

   public static void uploadDirty(double sortX, double sortY, double sortZ) {
      if (Axiom.configuration.internal.disableChunkRenderOverrider) {
         dirtyChunks.clear();
      } else {
         if (compileFuture != null && compileFuture.isDone()) {
            ChunkRenderOverrider.CompileData asyncCompileChunkData;
            try {
               asyncCompileChunkData = compileFuture.get();
            } catch (Exception var29) {
               throw new RuntimeException(var29);
            }

            if (asyncCompileChunkData != null && !stopBuilding.get()) {
               boolean hasNvidium = NvidiumApiWrapper.isNvidiumAvailable();
               if (asyncCompileChunkData.clear()) {
                  if (hasNvidium) {
                     unhideAllNvidium();
                  }

                  overridenChunksChanged = overridenChunksChanged | !chunkData.isEmpty();
                  sortedChunkData.forEach(data -> data.buffers.forEach((type, buffer) -> buffer.close()));
                  sortedChunkData.clear();
                  chunkData.clear();
               }

               for (ChunkRenderOverrider.CompiledChunkData compiled : asyncCompileChunkData.compiledChunks()) {
                  long pos = BlockPos.asLong(compiled.offsetX >> 4, compiled.offsetY >> 4, compiled.offsetZ >> 4);
                  ChunkRenderOverrider.RenderedChunkData chunkDatum = (ChunkRenderOverrider.RenderedChunkData)chunkData.get(pos);
                  if (chunkDatum == null) {
                     chunkDatum = new ChunkRenderOverrider.RenderedChunkData(compiled.offsetX, compiled.offsetY, compiled.offsetZ);
                     chunkData.put(pos, chunkDatum);
                     overridenChunksChanged = true;
                     if (hasNvidium) {
                        NvidiumApiWrapper.hideSection(compiled.offsetX >> 4, compiled.offsetY >> 4, compiled.offsetZ >> 4);
                     }

                     float dx = lastSortChunkX * 16 - chunkDatum.offsetX;
                     float dy = lastSortChunkY * 16 - chunkDatum.offsetY;
                     float dz = lastSortChunkZ * 16 - chunkDatum.offsetZ;
                     chunkDatum.distanceSqToCamera = dx * dx + dy * dy + dz * dz;
                     int min = 0;
                     int max = sortedChunkData.size();

                     int pivot;
                     for (pivot = (min + max) / 2; min != max; pivot = (min + max) / 2) {
                        ChunkRenderOverrider.RenderedChunkData pivotElement = sortedChunkData.get(pivot);
                        if (chunkDatum.distanceSqToCamera < pivotElement.distanceSqToCamera) {
                           max = pivot;
                        } else {
                           min = pivot + 1;
                        }
                     }

                     sortedChunkData.add(pivot, chunkDatum);
                  }

                  assert chunkDatum.offsetX == compiled.offsetX;

                  assert chunkDatum.offsetY == compiled.offsetY;

                  assert chunkDatum.offsetZ == compiled.offsetZ;

                  chunkDatum.hasBlocks.clear();
                  chunkDatum.hasBlocks.addAll(compiled.buffers.keySet());
                  chunkDatum.sortState = compiled.sortState;

                  for (Entry<ChunkRenderOverrider.AxiomChunkOverrideLayer, MeshData> entry : compiled.buffers.entrySet()) {
                     AxiomDrawBuffer buffer = chunkDatum.buffers.computeIfAbsent(entry.getKey(), k -> new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE));
                     buffer.upload(entry.getValue());
                  }

                  globalHasBlocks.addAll(chunkDatum.hasBlocks);
               }

               chunkBufferBuilderPack.clearAll();
            } else {
               chunkBufferBuilderPack.discardAll();
            }

            compileFuture = null;
            stopBuilding.set(false);
         }

         if (compileFuture == null) {
            if (!dirtyChunks.isEmpty()) {
               pendingBooleanCutoutMode = null;
            } else if (pendingBooleanCutoutMode != null) {
               blocksOverride.clear();
               BlockPos offset = pendingBooleanCutoutMode.offset();
               int offsetX = offset.getX();
               int offsetY = offset.getY();
               int offsetZ = offset.getZ();
               pendingBooleanCutoutMode.positionSet().forEach((x, y, z) -> setBlock(x + offsetX, y + offsetY, z + offsetZ, Blocks.AIR.defaultBlockState()));
            }

            if (!dirtyChunks.isEmpty()) {
               Level level = Minecraft.getInstance().level;
               if (level == null) {
                  throw new IllegalStateException();
               }

               LongSet chunksToUpload = dirtyChunks;
               dirtyChunks = new LongOpenHashSet();
               boolean clear = pendingBooleanCutoutMode != null || pendingClear;
               Position2ObjectMap<BlockState> copied = new Position2ObjectMap<>(ChunkRenderOverrider::loadBlocks);
               LongSet chunksToUploadAndNeighbors = new LongOpenHashSet();
               LongIterator iter = chunksToUpload.iterator();

               while (iter.hasNext()) {
                  long l = iter.nextLong();
                  int cx = BlockPos.getX(l);
                  int cy = BlockPos.getY(l);
                  int cz = BlockPos.getZ(l);
                  chunksToUploadAndNeighbors.add(l);
                  int minY = cy > level.getMinSection() ? -1 : 0;
                  int maxY = cy < level.getMaxSection() - 1 ? 1 : 0;

                  for (int oy = minY; oy <= maxY; oy++) {
                     for (int ox = -1; ox <= 1; ox++) {
                        for (int oz = -1; oz <= 1; oz++) {
                           chunksToUploadAndNeighbors.add(BlockPos.asLong(cx + ox, cy + oy, cz + oz));
                        }
                     }
                  }
               }

               copied.mergeAllFrom(blocksOverride, chunksToUploadAndNeighbors);
               BlockAndTintGetter blockAndTintGetter = new ChunkRenderOverrider.MappedBlockAndTintGetter(copied, Minecraft.getInstance().level);
               compileFuture = executor.submit(
                  () -> {
                     List<ChunkRenderOverrider.CompiledChunkData> list = new ArrayList<>(chunksToUpload.size());
                     MutableBlockPos blockPos = new MutableBlockPos();
                     BlockTessellator blockTessellator = new BlockTessellator(true, false);
                     LongIterator var13x = chunksToUpload.iterator();

                     while (var13x.hasNext()) {
                        long pos = (Long)var13x.next();
                        if (stopBuilding.compareAndSet(true, false)) {
                           return null;
                        }

                        int offsetX = BlockPos.getX(pos) * 16;
                        int offsetY = BlockPos.getY(pos) * 16;
                        int offsetZ = BlockPos.getZ(pos) * 16;
                        ChunkRenderOverrider.CompiledChunkData compiled = new ChunkRenderOverrider.CompiledChunkData(offsetX, offsetY, offsetZ);
                        EnumMap<ChunkRenderOverrider.AxiomChunkOverrideLayer, BufferBuilder> bufferBuilderMap = new EnumMap<>(
                           ChunkRenderOverrider.AxiomChunkOverrideLayer.class
                        );
                        BlockState[] blocks = copied.getChunk(pos);
                        if (blocks != null) {
                           RandomSource rand = RandomSource.create();
                           ModelBlockRenderer.enableCaching();
                           int index = 0;

                           for (int z = 0; z < 16; z++) {
                              for (int y = 0; y < 16; y++) {
                                 for (int x = 0; x < 16; x++) {
                                    BlockState blockState = blocks[index++];
                                    blockPos.set(offsetX + x, offsetY + y, offsetZ + z);
                                    blockTessellator.tessellateBlockAndLiquidOffsetMod16WithBuffers(
                                       layerx -> {
                                          if (bufferBuilderMap.containsKey(layerx)) {
                                             return bufferBuilderMap.get(layerx);
                                          } else {
                                             ByteBufferBuilder byteBufferBuilder = chunkBufferBuilderPack.buffer(
                                                ChunkRenderOverrider.AxiomChunkOverrideLayer.toVanilla(layerx)
                                             );
                                             BufferBuilder bufferBuilderx = new BufferBuilder(byteBufferBuilder, Mode.QUADS, DefaultVertexFormat.BLOCK);
                                             bufferBuilderMap.put(layerx, bufferBuilderx);
                                             return bufferBuilderx;
                                          }
                                       },
                                       blockPos,
                                       blockAndTintGetter,
                                       blockState
                                    );
                                 }
                              }
                           }

                           for (Entry<ChunkRenderOverrider.AxiomChunkOverrideLayer, BufferBuilder> entry : bufferBuilderMap.entrySet()) {
                              ChunkRenderOverrider.AxiomChunkOverrideLayer layer = entry.getKey();
                              BufferBuilder builder = entry.getValue();
                              if (layer.needsSorting) {
                                 MeshDataHelper.MeshDataAndSortState meshDataAndSortState = MeshDataHelper.buildAndSort(
                                    builder,
                                    VertexSorting.byDistance(
                                       (float)(sortX - compiled.offsetX), (float)(sortY - compiled.offsetY), (float)(sortZ - compiled.offsetZ)
                                    )
                                 );
                                 if (meshDataAndSortState != null) {
                                    compiled.buffers.put(layer, meshDataAndSortState.meshData());
                                    compiled.sortState = meshDataAndSortState.sortStateWrapper();
                                 }
                              } else {
                                 MeshData meshData = builder.build();
                                 if (meshData != null) {
                                    compiled.buffers.put(layer, meshData);
                                 }
                              }
                           }

                           list.add(compiled);
                           ModelBlockRenderer.clearCache();
                        }
                     }

                     return new ChunkRenderOverrider.CompileData(list, clear);
                  }
               );
            } else if (pendingClear) {
               compileFuture = executor.submit(() -> new ChunkRenderOverrider.CompileData(List.of(), true));
            }

            pendingBooleanCutoutMode = null;
            pendingClear = false;
         }

         boolean hasLayerWithSorting = false;

         for (ChunkRenderOverrider.AxiomChunkOverrideLayer layer : globalHasBlocks) {
            if (layer.needsSorting) {
               hasLayerWithSorting = true;
               break;
            }
         }

         if (hasLayerWithSorting) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSortMillis >= 200L) {
               lastSortMillis = currentTime;
               double dX = sortX - lastSortX;
               double dY = sortY - lastSortY;
               double dZ = sortZ - lastSortZ;
               if (dX * dX + dY * dY + dZ * dZ > 1.0) {
                  lastSortX = (float)sortX;
                  lastSortY = (float)sortY;
                  lastSortZ = (float)sortZ;
                  int chunkX = SectionPos.posToSectionCoord(sortX);
                  int chunkY = SectionPos.posToSectionCoord(sortY);
                  int chunkZ = SectionPos.posToSectionCoord(sortZ);
                  boolean resortChunkList = chunkX != lastSortChunkX || chunkY != lastSortChunkY || chunkZ != lastSortChunkZ;
                  VertexConsumerProvider provider = VertexConsumerProvider.shared();

                  for (ChunkRenderOverrider.RenderedChunkData chunkDatumx : sortedChunkData) {
                     for (Entry<ChunkRenderOverrider.AxiomChunkOverrideLayer, AxiomDrawBuffer> entry : chunkDatumx.buffers.entrySet()) {
                        if (entry.getKey().needsSorting) {
                           float relSortX = (float)(sortX - chunkDatumx.offsetX);
                           float relSortY = (float)(sortY - chunkDatumx.offsetY);
                           float relSortZ = (float)(sortZ - chunkDatumx.offsetZ);
                           BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.BLOCK);
                           AxiomDrawBuffer vertexBuffer = entry.getValue();
                           if (vertexBuffer != null && chunkDatumx.sortState != null) {
                              chunkDatumx.sortState = MeshDataHelper.resort(
                                 bufferBuilder, chunkDatumx.sortState, vertexBuffer, VertexSorting.byDistance(relSortX, relSortY, relSortZ)
                              );
                           }
                        }
                     }

                     if (resortChunkList) {
                        float dx = chunkX * 16 - chunkDatumx.offsetX;
                        float dy = chunkY * 16 - chunkDatumx.offsetY;
                        float dz = chunkZ * 16 - chunkDatumx.offsetZ;
                        chunkDatumx.distanceSqToCamera = dx * dx + dy * dy + dz * dz;
                     }
                  }

                  if (resortChunkList) {
                     lastSortChunkX = chunkX;
                     lastSortChunkY = chunkY;
                     lastSortChunkZ = chunkZ;
                     sortedChunkData.sort(Comparator.comparingDouble(c -> -c.distanceSqToCamera));
                  }
               }
            }
         }
      }
   }

   public static void cutoutBoolean(PositionSet set, BlockPos offset) {
      if (!Axiom.configuration.internal.disableChunkRenderOverrider) {
         if (!set.isEmpty()) {
            pendingBooleanCutoutMode = new ChunkRenderOverrider.BooleanCutoutMode(set, offset);
         }
      }
   }

   public static enum AxiomChunkOverrideLayer {
      SOLID(false, false),
      CUTOUT_MIPPED(false, false),
      TRIPWIRE(false, false),
      CUTOUT(false, false),
      TRANSLUCENT(true, true);

      public final boolean needsSorting;
      public final boolean isTranslucent;

      private AxiomChunkOverrideLayer(boolean needsSorting, boolean isTranslucent) {
         this.needsSorting = needsSorting;
         this.isTranslucent = isTranslucent;
      }

      public static ChunkRenderOverrider.AxiomChunkOverrideLayer fromVanilla(RenderType layer) {
         if (layer == RenderType.solid()) {
            return SOLID;
         } else if (layer == RenderType.cutoutMipped()) {
            return CUTOUT_MIPPED;
         } else if (layer == RenderType.cutout()) {
            return CUTOUT;
         } else {
            return layer == RenderType.tripwire() ? TRIPWIRE : TRANSLUCENT;
         }
      }

      public static RenderType toVanilla(ChunkRenderOverrider.AxiomChunkOverrideLayer layer) {
         return switch (layer) {
            case SOLID -> RenderType.solid();
            case CUTOUT_MIPPED -> RenderType.cutoutMipped();
            case TRIPWIRE -> RenderType.tripwire();
            case CUTOUT -> RenderType.cutout();
            case TRANSLUCENT -> RenderType.translucent();
         };
      }
   }

   private record BooleanCutoutMode(PositionSet positionSet, BlockPos offset) {
   }

   private record CompileData(List<ChunkRenderOverrider.CompiledChunkData> compiledChunks, boolean clear) {
   }

   private static final class CompiledChunkData {
      EnumMap<ChunkRenderOverrider.AxiomChunkOverrideLayer, MeshData> buffers = new EnumMap<>(ChunkRenderOverrider.AxiomChunkOverrideLayer.class);
      SortStateWrapper sortState = null;
      int offsetX;
      int offsetY;
      int offsetZ;

      public CompiledChunkData(int offsetX, int offsetY, int offsetZ) {
         this.offsetX = offsetX;
         this.offsetY = offsetY;
         this.offsetZ = offsetZ;
      }
   }

   private static final class MappedBlockAndTintGetter implements BlockAndTintGetter {
      private final Position2ObjectMap<BlockState> map;
      private final BlockAndTintGetter level;

      private MappedBlockAndTintGetter(Position2ObjectMap<BlockState> map, BlockAndTintGetter level) {
         this.map = map;
         this.level = level;
      }

      public float getShade(Direction direction, boolean hasShade) {
         return this.level.getShade(direction, hasShade);
      }

      @NotNull
      public LevelLightEngine getLightEngine() {
         throw new UnsupportedOperationException();
      }

      public int getBrightness(@NotNull LightLayer lightLayer, @NotNull BlockPos blockPos) {
         return lightLayer == LightLayer.BLOCK ? 0 : 15;
      }

      public int getRawBrightness(@NotNull BlockPos blockPos, int i) {
         return 15;
      }

      public int getBlockTint(@NotNull BlockPos blockPos, @NotNull ColorResolver colorResolver) {
         return this.level.getBlockTint(blockPos, colorResolver);
      }

      @Nullable
      public BlockEntity getBlockEntity(@NotNull BlockPos blockPos) {
         return null;
      }

      @NotNull
      public BlockState getBlockState(BlockPos blockPos) {
         BlockState mapped = this.map.get(blockPos.getX(), blockPos.getY(), blockPos.getZ());
         return Objects.requireNonNullElse(mapped, Blocks.AIR.defaultBlockState());
      }

      @NotNull
      public FluidState getFluidState(@NotNull BlockPos blockPos) {
         return this.getBlockState(blockPos).getFluidState();
      }

      public int getHeight() {
         return this.level.getHeight();
      }

      public int getMinBuildHeight() {
         return this.level.getMinBuildHeight();
      }
   }

   private static final class RenderedChunkData {
      EnumMap<ChunkRenderOverrider.AxiomChunkOverrideLayer, AxiomDrawBuffer> buffers = new EnumMap<>(ChunkRenderOverrider.AxiomChunkOverrideLayer.class);
      EnumSet<ChunkRenderOverrider.AxiomChunkOverrideLayer> hasBlocks = EnumSet.noneOf(ChunkRenderOverrider.AxiomChunkOverrideLayer.class);
      SortStateWrapper sortState = null;
      int offsetX;
      int offsetY;
      int offsetZ;
      float distanceSqToCamera;

      public RenderedChunkData(int offsetX, int offsetY, int offsetZ) {
         this.offsetX = offsetX;
         this.offsetY = offsetY;
         this.offsetZ = offsetZ;
      }
   }
}
