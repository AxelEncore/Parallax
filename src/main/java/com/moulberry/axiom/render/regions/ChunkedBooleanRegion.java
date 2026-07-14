package com.moulberry.axiom.render.regions;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.GlobalCleaner;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.collections.Position2ByteMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.collections.list.IntrusiveLinkedElement;
import com.moulberry.axiom.collections.list.IntrusiveLinkedList;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDraw;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.EffectRenderer;
import com.moulberry.axiom.render.MeshDataHelper;
import com.moulberry.axiom.render.SortStateWrapper;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.ExpandOffsets;
import com.moulberry.axiom.utils.FramebufferUtils;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import com.moulberry.axiomclientapi.pathers.BallShape;
import com.moulberry.axiomclientapi.regions.BooleanRegion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;

public class ChunkedBooleanRegion implements BooleanRegion {
   private boolean closed = false;
   private GlobalCleaner.LeakChecker leakChecker;
   private PositionSet positionSet;
   private final MutableBlockPos min = new MutableBlockPos();
   private final MutableBlockPos max = new MutableBlockPos();
   private float scaleX = 1.0F;
   private float scaleY = 1.0F;
   private float scaleZ = 1.0F;
   private final Long2ObjectMap<ChunkedBooleanRegion.ChunkData> chunkData = new Long2ObjectOpenHashMap();
   private LongSet dirtyChunks = new LongOpenHashSet();
   private boolean clearChunkData = false;
   private long lastSortMillis;
   private float lastSortX;
   private float lastSortY;
   private float lastSortZ;
   private int lastSortChunkX;
   private int lastSortChunkY;
   private int lastSortChunkZ;
   private final IntrusiveLinkedList<ChunkedBooleanRegion.ChunkData> sortedChunkData = new IntrusiveLinkedList<>();
   private static RenderTarget copiedDepth = null;
   private static final float EPS = 1.0E-5F;
   private static final int SHADE_X = 178;
   private static final int SHADE_PLUS_Y = 255;
   private static final int SHADE_MINUS_Y = 153;
   private static final int SHADE_Z = 222;

   public ChunkedBooleanRegion() {
      this(new PositionSet());
   }

   public ChunkedBooleanRegion(PositionSet positionSet) {
      if (BuildConfig.DEBUG) {
         this.leakChecker = GlobalCleaner.createLeakChecker(this, "ChunkedBooleanRegion");
      }

      this.positionSet = positionSet.copy();
      if (!positionSet.isEmpty()) {
         this.updateBoundingMinMax(true, true, true, true, true, true);
         this.positionSet.forEachChunk((cx, cy, cz, array) -> {
            this.dirtyChunks.add(BlockPos.asLong(cx, cy, cz));
            this.dirtyChunks.add(BlockPos.asLong(cx - 1, cy, cz));
            this.dirtyChunks.add(BlockPos.asLong(cx + 1, cy, cz));
            this.dirtyChunks.add(BlockPos.asLong(cx, cy - 1, cz));
            this.dirtyChunks.add(BlockPos.asLong(cx, cy + 1, cz));
            this.dirtyChunks.add(BlockPos.asLong(cx, cy, cz - 1));
            this.dirtyChunks.add(BlockPos.asLong(cx, cy, cz + 1));
         });
      }
   }

   public void render(Camera camera, Vec3 translation, PoseStack matrix, Matrix4f projection, long time, int effects) {
      this.render(
         new AxiomWorldRenderContext(camera, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), matrix, projection), translation, effects
      );
   }

   public void render(AxiomWorldRenderContext rc, Vec3 translation, int effects) {
      this.uploadDirty(rc, translation);
      if (!this.sortedChunkData.isEmpty()) {
         RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
         int width = mainRenderTarget.width;
         int height = mainRenderTarget.height;
         RenderTarget particlesTarget = Minecraft.getInstance().levelRenderer.getParticlesTarget();
         RenderTarget renderTarget;
         if (particlesTarget != null) {
            renderTarget = particlesTarget;
            if (copiedDepth != null) {
               copiedDepth.destroyBuffers();
               copiedDepth = null;
            }
         } else {
            copiedDepth = FramebufferUtils.resizeOrCreateFramebuffer(copiedDepth, width, height);
            FramebufferUtils.clear(copiedDepth, 0);
            FramebufferUtils.copyDepth(mainRenderTarget, copiedDepth);
            renderTarget = mainRenderTarget;
         }

         List<AxiomDraw> axiomDraws = new ArrayList<>(this.sortedChunkData.size());
         ChunkedBooleanRegion.ChunkData firstChunkData = this.sortedChunkData.first();
         double originX = firstChunkData.offsetX;
         double originY = firstChunkData.offsetY;
         double originZ = firstChunkData.offsetZ;
         Matrix4d initialTransform = new Matrix4d();
         initialTransform.translate(-rc.x(), -rc.y(), -rc.z());
         initialTransform.translate(translation.x, translation.y, translation.z);
         initialTransform.scale(this.scaleX, this.scaleY, this.scaleZ);
         initialTransform.translate(originX, originY, originZ);
         PoseStack poseStack = rc.poseStack();
         poseStack.pushPose();
         poseStack.mulPose(VersionUtilsClient.matrix4fcToMatrix4f(new Matrix4f(initialTransform)));
         Matrix4f modelViewMatrix = poseStack.last().pose();

         for (ChunkedBooleanRegion.ChunkData data : this.sortedChunkData) {
            Matrix4f matrix4f = new Matrix4f(modelViewMatrix);
            matrix4f.translate((float)(data.offsetX - originX), (float)(data.offsetY - originY), (float)(data.offsetZ - originZ));
            axiomDraws.add(new AxiomDraw(data.buffer, matrix4f, null, null));
         }

         EffectRenderer.render(
            (pipeline, target) -> AxiomRenderer.renderPipeline(pipeline, target != null ? target : renderTarget, axiomDraws), rc.nanos(), effects
         );
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
         if (copiedDepth != null) {
            FramebufferUtils.copyDepth(copiedDepth, mainRenderTarget);
         }

         poseStack.popPose();
      }
   }

   private void uploadDirty(AxiomWorldRenderContext rc, Vec3 translation) {
      RenderSystem.assertOnRenderThread();
      if (this.clearChunkData) {
         this.clearChunkData = false;
         ObjectIterator chunksToUpload = this.chunkData.values().iterator();

         while (chunksToUpload.hasNext()) {
            ChunkedBooleanRegion.ChunkData chunkDatum = (ChunkedBooleanRegion.ChunkData)chunksToUpload.next();
            chunkDatum.buffer.close();
         }

         this.chunkData.clear();
         this.sortedChunkData.clear();
      }

      LongSet chunksToUpload = this.dirtyChunks;
      this.dirtyChunks = new LongOpenHashSet();
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      float sortX = (float)(rc.x() / this.scaleX);
      float sortY = (float)(rc.y() / this.scaleY);
      float sortZ = (float)(rc.z() / this.scaleZ);
      boolean addedNewChunk = false;
      LongIterator currentTime = chunksToUpload.iterator();

      while (currentTime.hasNext()) {
         long pos = (Long)currentTime.next();
         int chunkX = BlockPos.getX(pos);
         int chunkY = BlockPos.getY(pos);
         int chunkZ = BlockPos.getZ(pos);
         BufferBuilder bufferBuilder = null;
         short[] chunk = this.positionSet.getChunk(pos);
         if (chunk != null) {
            short[] up = this.positionSet.getChunk(BlockPos.asLong(chunkX, chunkY + 1, chunkZ));
            short[] down = this.positionSet.getChunk(BlockPos.asLong(chunkX, chunkY - 1, chunkZ));
            short[] north = this.positionSet.getChunk(BlockPos.asLong(chunkX, chunkY, chunkZ - 1));
            short[] south = this.positionSet.getChunk(BlockPos.asLong(chunkX, chunkY, chunkZ + 1));
            short[] east = this.positionSet.getChunk(BlockPos.asLong(chunkX - 1, chunkY, chunkZ));
            short[] west = this.positionSet.getChunk(BlockPos.asLong(chunkX + 1, chunkY, chunkZ));
            bufferBuilder = buildChunkVanilla(provider, chunk, up, down, north, south, east, west);
         }

         if (bufferBuilder != null && bufferBuilder.vertices > 0) {
            ChunkedBooleanRegion.ChunkData chunkData = (ChunkedBooleanRegion.ChunkData)this.chunkData.get(pos);
            if (chunkData == null) {
               chunkData = new ChunkedBooleanRegion.ChunkData(chunkX * 16, chunkY * 16, chunkZ * 16);
               this.chunkData.put(pos, chunkData);
               float dx = this.lastSortChunkX * 16 - chunkData.offsetX;
               float dy = this.lastSortChunkY * 16 - chunkData.offsetY;
               float dz = this.lastSortChunkZ * 16 - chunkData.offsetZ;
               chunkData.distanceSqToCamera = dx * dx + dy * dy + dz * dz;
               this.sortedChunkData.add(chunkData);
               addedNewChunk = true;
            }

            MeshDataHelper.MeshDataAndSortState meshDataAndSortState = MeshDataHelper.buildAndSort(
               bufferBuilder, InvertedVertexSorting.byDistance(sortX - chunkData.offsetX, sortY - chunkData.offsetY, sortZ - chunkData.offsetZ)
            );
            if (meshDataAndSortState != null) {
               chunkData.buffer.upload(meshDataAndSortState.meshData());
               chunkData.sortState = meshDataAndSortState.sortStateWrapper();
            }
         } else {
            if (bufferBuilder != null) {
               MeshDataHelper.discard(bufferBuilder.build());
            }

            ChunkedBooleanRegion.ChunkData chunkDatax = (ChunkedBooleanRegion.ChunkData)this.chunkData.remove(pos);
            if (chunkDatax != null) {
               this.sortedChunkData.remove(chunkDatax);
               chunkDatax.buffer.close();
            }
         }
      }

      long currentTimex = System.currentTimeMillis();
      if (currentTimex - this.lastSortMillis < 250L) {
         if (addedNewChunk) {
            this.sortedChunkData.sort(Comparator.comparingDouble(c -> c.distanceSqToCamera));
         }
      } else {
         double dX = sortX - this.lastSortX;
         double dY = sortY - this.lastSortY;
         double dZ = sortZ - this.lastSortZ;
         if (dX * dX + dY * dY + dZ * dZ < 1.0) {
            if (addedNewChunk) {
               this.sortedChunkData.sort(Comparator.comparingDouble(c -> c.distanceSqToCamera));
            }
         } else {
            this.lastSortX = sortX;
            this.lastSortY = sortY;
            this.lastSortZ = sortZ;
            this.lastSortMillis = currentTimex;
            int chunkXx = SectionPos.posToSectionCoord(sortX);
            int chunkYx = SectionPos.posToSectionCoord(sortY);
            int chunkZx = SectionPos.posToSectionCoord(sortZ);
            boolean resortChunkList = chunkXx != this.lastSortChunkX || chunkYx != this.lastSortChunkY || chunkZx != this.lastSortChunkZ;

            for (ChunkedBooleanRegion.ChunkData chunkDatax : this.sortedChunkData) {
               BufferBuilder bufferBuilderx = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
               chunkDatax.sortState = MeshDataHelper.resort(
                  bufferBuilderx,
                  chunkDatax.sortState,
                  chunkDatax.buffer,
                  InvertedVertexSorting.byDistance(sortX - chunkDatax.offsetX, sortY - chunkDatax.offsetY, sortZ - chunkDatax.offsetZ)
               );
               if (resortChunkList) {
                  float dx = chunkXx * 16 - chunkDatax.offsetX;
                  float dy = chunkYx * 16 - chunkDatax.offsetY;
                  float dz = chunkZx * 16 - chunkDatax.offsetZ;
                  chunkDatax.distanceSqToCamera = dx * dx + dy * dy + dz * dz;
               }
            }

            if (resortChunkList) {
               this.lastSortChunkX = chunkXx;
               this.lastSortChunkY = chunkYx;
               this.lastSortChunkZ = chunkZx;
               this.sortedChunkData.sort(Comparator.comparingDouble(c -> c.distanceSqToCamera));
            } else if (addedNewChunk) {
               this.sortedChunkData.sort(Comparator.comparingDouble(c -> c.distanceSqToCamera));
            }
         }
      }
   }

   @Nullable
   private static BufferBuilder buildChunkVanilla(
      VertexConsumerProvider provider, short[] chunk, short[] up, short[] down, short[] north, short[] south, short[] east, short[] west
   ) {
      BufferBuilder bufferBuilder = null;
      int index = 0;

      for (int z = 0; z < 16; z++) {
         for (int y = 0; y < 16; y++) {
            short v = chunk[index++];
            if (v != 0) {
               if (bufferBuilder == null) {
                  bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
               }

               for (int x = 0; x < 16; x++) {
                  if ((v & 1 << x) != 0) {
                     boolean plusX;
                     boolean minusX;
                     if (x == 0) {
                        plusX = (v & 2) == 0;
                        minusX = east == null || (east[y + z * 16] & '耀') == 0;
                     } else if (x != 15) {
                        plusX = (v & 1 << x + 1) == 0;
                        minusX = (v & 1 << x - 1) == 0;
                     } else {
                        plusX = west == null || (west[y + z * 16] & 1) == 0;
                        minusX = (v & 16384) == 0;
                     }

                     if (plusX) {
                        bufferBuilder.addVertex(x + 1 + 1.0E-5F, y, z).setColor(178, 178, 178, 255);
                        bufferBuilder.addVertex(x + 1 + 1.0E-5F, y + 1, z).setColor(178, 178, 178, 255);
                        bufferBuilder.addVertex(x + 1 + 1.0E-5F, y + 1, z + 1).setColor(178, 178, 178, 255);
                        bufferBuilder.addVertex(x + 1 + 1.0E-5F, y, z + 1).setColor(178, 178, 178, 255);
                     }

                     if (minusX) {
                        bufferBuilder.addVertex(x - 1.0E-5F, y, z + 1).setColor(178, 178, 178, 255);
                        bufferBuilder.addVertex(x - 1.0E-5F, y + 1, z + 1).setColor(178, 178, 178, 255);
                        bufferBuilder.addVertex(x - 1.0E-5F, y + 1, z).setColor(178, 178, 178, 255);
                        bufferBuilder.addVertex(x - 1.0E-5F, y, z).setColor(178, 178, 178, 255);
                     }

                     boolean plusY;
                     boolean minusY;
                     if (y == 0) {
                        plusY = (chunk[1 + z * 16] & 1 << x) == 0;
                        minusY = down == null || (down[15 + z * 16] & 1 << x) == 0;
                     } else if (y != 15) {
                        plusY = (chunk[y + 1 + z * 16] & 1 << x) == 0;
                        minusY = (chunk[y - 1 + z * 16] & 1 << x) == 0;
                     } else {
                        plusY = up == null || (up[z * 16] & 1 << x) == 0;
                        minusY = (chunk[14 + z * 16] & 1 << x) == 0;
                     }

                     if (plusY) {
                        bufferBuilder.addVertex(x, y + 1 + 1.0E-5F, z + 1).setColor(255, 255, 255, 255);
                        bufferBuilder.addVertex(x + 1, y + 1 + 1.0E-5F, z + 1).setColor(255, 255, 255, 255);
                        bufferBuilder.addVertex(x + 1, y + 1 + 1.0E-5F, z).setColor(255, 255, 255, 255);
                        bufferBuilder.addVertex(x, y + 1 + 1.0E-5F, z).setColor(255, 255, 255, 255);
                     }

                     if (minusY) {
                        bufferBuilder.addVertex(x, y - 1.0E-5F, z).setColor(153, 153, 153, 255);
                        bufferBuilder.addVertex(x + 1, y - 1.0E-5F, z).setColor(153, 153, 153, 255);
                        bufferBuilder.addVertex(x + 1, y - 1.0E-5F, z + 1).setColor(153, 153, 153, 255);
                        bufferBuilder.addVertex(x, y - 1.0E-5F, z + 1).setColor(153, 153, 153, 255);
                     }

                     boolean plusZ;
                     boolean minusZ;
                     if (z == 0) {
                        plusZ = (chunk[y + 16] & 1 << x) == 0;
                        minusZ = north == null || (north[y + 240] & 1 << x) == 0;
                     } else if (z != 15) {
                        plusZ = (chunk[y + (z + 1) * 16] & 1 << x) == 0;
                        minusZ = (chunk[y + (z - 1) * 16] & 1 << x) == 0;
                     } else {
                        plusZ = south == null || (south[y] & 1 << x) == 0;
                        minusZ = (chunk[y + 224] & 1 << x) == 0;
                     }

                     if (plusZ) {
                        bufferBuilder.addVertex(x, y, z + 1 + 1.0E-5F).setColor(222, 222, 222, 255);
                        bufferBuilder.addVertex(x + 1, y, z + 1 + 1.0E-5F).setColor(222, 222, 222, 255);
                        bufferBuilder.addVertex(x + 1, y + 1, z + 1 + 1.0E-5F).setColor(222, 222, 222, 255);
                        bufferBuilder.addVertex(x, y + 1, z + 1 + 1.0E-5F).setColor(222, 222, 222, 255);
                     }

                     if (minusZ) {
                        bufferBuilder.addVertex(x, y + 1, z - 1.0E-5F).setColor(222, 222, 222, 255);
                        bufferBuilder.addVertex(x + 1, y + 1, z - 1.0E-5F).setColor(222, 222, 222, 255);
                        bufferBuilder.addVertex(x + 1, y, z - 1.0E-5F).setColor(222, 222, 222, 255);
                        bufferBuilder.addVertex(x, y, z - 1.0E-5F).setColor(222, 222, 222, 255);
                     }
                  }
               }
            }
         }
      }

      return bufferBuilder;
   }

   public BlockPos min() {
      return this.min;
   }

   public BlockPos max() {
      return this.max;
   }

   public int count() {
      return this.positionSet.count();
   }

   public BlockPos getCenter() {
      return new BlockPos(
         Math.floorDiv(this.max.getX() + this.min.getX(), 2),
         Math.floorDiv(this.max.getY() + this.min.getY(), 2),
         Math.floorDiv(this.max.getZ() + this.min.getZ(), 2)
      );
   }

   public void setScale(float scaleX, float scaleY, float scaleZ) {
      this.scaleX = scaleX;
      this.scaleY = scaleY;
      this.scaleZ = scaleZ;
   }

   @Deprecated
   public PositionSet unsafeGetPositionSet() {
      return this.positionSet;
   }

   public PositionSet copyPositionSet() {
      return this.positionSet.copy();
   }

   public void forEach(TriIntConsumer consumer) {
      this.positionSet.forEach(consumer);
   }

   public void forEachChunk(PositionSet.ChunkConsumer consumer) {
      this.positionSet.forEachChunk(consumer);
   }

   public void close() {
      if (this.closed) {
         throw new FaultyImplementationError();
      } else {
         this.closed = true;
         this.positionSet.clear();
         this.dirtyChunks.clear();
         ObjectIterator var1 = this.chunkData.values().iterator();

         while (var1.hasNext()) {
            ChunkedBooleanRegion.ChunkData chunkDatum = (ChunkedBooleanRegion.ChunkData)var1.next();
            chunkDatum.buffer.close();
         }

         this.chunkData.clear();
         this.sortedChunkData.clear();
         if (this.leakChecker != null) {
            this.leakChecker.disarm();
         }
      }
   }

   public void clear() {
      this.positionSet.clear();
      this.clearChunkData = true;
      this.dirtyChunks.clear();
      this.min.set(0, 0, 0);
      this.max.set(0, 0, 0);
   }

   public boolean contains(int x, int y, int z) {
      return x >= this.min.getX() && x <= this.max.getX() && y >= this.min.getY() && y <= this.max.getY() && z >= this.min.getZ() && z <= this.max.getZ()
         ? this.positionSet.contains(x, y, z)
         : false;
   }

   public boolean subtractAABB(BlockPos min, BlockPos max) {
      if (this.positionSet.count() <= 0) {
         return false;
      } else {
         min = new BlockPos(Math.max(min.getX(), this.min.getX()), Math.max(min.getY(), this.min.getY()), Math.max(min.getZ(), this.min.getZ()));
         max = new BlockPos(Math.min(max.getX(), this.max.getX()), Math.min(max.getY(), this.max.getY()), Math.min(max.getZ(), this.max.getZ()));
         if (min.getX() <= max.getX() && min.getY() <= max.getY() && min.getZ() <= max.getZ()) {
            int oldCount = this.positionSet.count();
            int minRemoveSectionX = min.getX() >> 4;
            int minRemoveSectionY = min.getY() >> 4;
            int minRemoveSectionZ = min.getZ() >> 4;
            int maxRemoveSectionX = max.getX() >> 4;
            int maxRemoveSectionY = max.getY() >> 4;
            int maxRemoveSectionZ = max.getZ() >> 4;

            for (int sx = minRemoveSectionX; sx <= maxRemoveSectionX; sx++) {
               for (int sy = minRemoveSectionY; sy <= maxRemoveSectionY; sy++) {
                  for (int sz = minRemoveSectionZ; sz <= maxRemoveSectionZ; sz++) {
                     long pos = BlockPos.asLong(sx, sy, sz);
                     int lx = Math.max(0, min.getX() - sx * 16);
                     int ux = Math.min(15, max.getX() - sx * 16);
                     int ly = Math.max(0, min.getY() - sy * 16);
                     int uy = Math.min(15, max.getY() - sy * 16);
                     int lz = Math.max(0, min.getZ() - sz * 16);
                     int uz = Math.min(15, max.getZ() - sz * 16);
                     if (lx != 0 || ux != 15 || ly != 0 || uy != 15 || lz != 0 || uz != 15) {
                        short[] section = this.positionSet.getChunk(pos);
                        if (section != null) {
                           int mask = (1 << ux - lx + 1) - 1 << lx;
                           int removedCount = 0;

                           for (int z = lz; z <= uz; z++) {
                              for (int y = ly; y <= uy; y++) {
                                 short value = section[y + z * 16];
                                 removedCount += Integer.bitCount(value & mask);
                                 section[y + z * 16] = (short)(value & ~mask);
                              }
                           }

                           if (removedCount > 0) {
                              this.positionSet.unsafeSetCount(this.positionSet.count() - removedCount);
                              this.dirtyChunks.add(pos);
                              if (ux == 15) {
                                 this.dirtyChunks.add(BlockPos.asLong(sx + 1, sy, sz));
                              }

                              if (uy == 15) {
                                 this.dirtyChunks.add(BlockPos.asLong(sx, sy + 1, sz));
                              }

                              if (uz == 15) {
                                 this.dirtyChunks.add(BlockPos.asLong(sx, sy, sz + 1));
                              }

                              if (lx == 0) {
                                 this.dirtyChunks.add(BlockPos.asLong(sx - 1, sy, sz));
                              }

                              if (ly == 0) {
                                 this.dirtyChunks.add(BlockPos.asLong(sx, sy - 1, sz));
                              }

                              if (lz == 0) {
                                 this.dirtyChunks.add(BlockPos.asLong(sx, sy, sz - 1));
                              }

                              boolean empty = true;

                              for (short s : this.positionSet.getChunk(pos)) {
                                 if (s != 0) {
                                    empty = false;
                                    break;
                                 }
                              }

                              if (empty) {
                                 this.positionSet.removeEmptyChunk(pos);
                              }
                           }
                        }
                     } else if (this.positionSet.removeChunk(pos) > 0) {
                        this.dirtyChunks.add(pos);
                        if (sx == maxRemoveSectionX) {
                           this.dirtyChunks.add(BlockPos.asLong(sx + 1, sy, sz));
                        }

                        if (sy == maxRemoveSectionY) {
                           this.dirtyChunks.add(BlockPos.asLong(sx, sy + 1, sz));
                        }

                        if (sz == maxRemoveSectionZ) {
                           this.dirtyChunks.add(BlockPos.asLong(sx, sy, sz + 1));
                        }

                        if (sx == minRemoveSectionX) {
                           this.dirtyChunks.add(BlockPos.asLong(sx - 1, sy, sz));
                        }

                        if (sy == minRemoveSectionY) {
                           this.dirtyChunks.add(BlockPos.asLong(sx, sy - 1, sz));
                        }

                        if (sz == minRemoveSectionZ) {
                           this.dirtyChunks.add(BlockPos.asLong(sx, sy, sz - 1));
                        }
                     }
                  }
               }
            }

            if (oldCount == this.positionSet.count()) {
               return false;
            } else if (this.positionSet.count() <= 0) {
               this.clear();
               return true;
            } else {
               boolean removeCoversMinX = min.getX() <= this.min.getX();
               boolean removeCoversMinY = min.getY() <= this.min.getY();
               boolean removeCoversMinZ = min.getZ() <= this.min.getZ();
               boolean removeCoversMaxX = max.getX() >= this.max.getX();
               boolean removeCoversMaxY = max.getY() >= this.max.getY();
               boolean removeCoversMaxZ = max.getZ() >= this.max.getZ();
               this.updateBoundingMinMax(removeCoversMinX, removeCoversMinY, removeCoversMinZ, removeCoversMaxX, removeCoversMaxY, removeCoversMaxZ);
               if (BuildConfig.DEBUG) {
                  this.positionSet.validateCount();
                  int changedBlocks = oldCount - this.positionSet.count();
                  if (changedBlocks <= 0) {
                     throw new FaultyImplementationError();
                  }

                  if (changedBlocks > (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1)) {
                     throw new FaultyImplementationError("Too many blocks were removed");
                  }

                  this.validateNoEmpty();
                  this.validateMinMax();
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   public void updateBoundingMinMax(boolean updateMinX, boolean updateMinY, boolean updateMinZ, boolean updateMaxX, boolean updateMaxY, boolean updateMaxZ) {
      if (updateMinX || updateMinY || updateMinZ || updateMaxX || updateMaxY || updateMaxZ) {
         List<short[]> minXChunks = new ArrayList<>();
         int minXChunk = updateMinX ? Integer.MAX_VALUE : Integer.MIN_VALUE;
         List<short[]> minYChunks = new ArrayList<>();
         int minYChunk = updateMinY ? Integer.MAX_VALUE : Integer.MIN_VALUE;
         List<short[]> minZChunks = new ArrayList<>();
         int minZChunk = updateMinZ ? Integer.MAX_VALUE : Integer.MIN_VALUE;
         List<short[]> maxXChunks = new ArrayList<>();
         int maxXChunk = updateMaxX ? Integer.MIN_VALUE : Integer.MAX_VALUE;
         List<short[]> maxYChunks = new ArrayList<>();
         int maxYChunk = updateMaxY ? Integer.MIN_VALUE : Integer.MAX_VALUE;
         List<short[]> maxZChunks = new ArrayList<>();
         int maxZChunk = updateMaxZ ? Integer.MIN_VALUE : Integer.MAX_VALUE;
         ObjectIterator maxZSectionRelative = this.positionSet.unsafeGetRawMap().long2ObjectEntrySet().iterator();

         while (maxZSectionRelative.hasNext()) {
            Entry<short[]> entry = (Entry<short[]>)maxZSectionRelative.next();
            long key = entry.getLongKey();
            int sx = BlockPos.getX(key);
            int sy = BlockPos.getY(key);
            int sz = BlockPos.getZ(key);
            if (sx < minXChunk) {
               minXChunks.clear();
               minXChunks.add((short[])entry.getValue());
               minXChunk = sx;
            } else if (sx == minXChunk) {
               minXChunks.add((short[])entry.getValue());
            }

            if (sy < minYChunk) {
               minYChunks.clear();
               minYChunks.add((short[])entry.getValue());
               minYChunk = sy;
            } else if (sy == minYChunk) {
               minYChunks.add((short[])entry.getValue());
            }

            if (sz < minZChunk) {
               minZChunks.clear();
               minZChunks.add((short[])entry.getValue());
               minZChunk = sz;
            } else if (sz == minZChunk) {
               minZChunks.add((short[])entry.getValue());
            }

            if (sx > maxXChunk) {
               maxXChunks.clear();
               maxXChunks.add((short[])entry.getValue());
               maxXChunk = sx;
            } else if (sx == maxXChunk) {
               maxXChunks.add((short[])entry.getValue());
            }

            if (sy > maxYChunk) {
               maxYChunks.clear();
               maxYChunks.add((short[])entry.getValue());
               maxYChunk = sy;
            } else if (sy == maxYChunk) {
               maxYChunks.add((short[])entry.getValue());
            }

            if (sz > maxZChunk) {
               maxZChunks.clear();
               maxZChunks.add((short[])entry.getValue());
               maxZChunk = sz;
            } else if (sz == maxZChunk) {
               maxZChunks.add((short[])entry.getValue());
            }
         }

         if (updateMinX) {
            int minXSectionRelative = BuildConfig.DEBUG ? Integer.MAX_VALUE : 15;
            Iterator var32 = minXChunks.iterator();

            while (true) {
               if (var32.hasNext()) {
                  short[] chunk = (short[])var32.next();

                  for (short value : chunk) {
                     minXSectionRelative = Math.min(minXSectionRelative, Integer.numberOfTrailingZeros(value & '\uffff'));
                  }

                  if (minXSectionRelative != 0) {
                     continue;
                  }
               }

               if (BuildConfig.DEBUG && (minXSectionRelative < 0 || minXSectionRelative > 15)) {
                  throw new FaultyImplementationError("Sections: " + Arrays.deepToString(minXChunks.toArray()));
               }

               this.min.setX(minXChunk * 16 + minXSectionRelative);
               break;
            }
         }

         if (updateMinY) {
            int minYSectionRelative = BuildConfig.DEBUG ? Integer.MAX_VALUE : 15;
            Iterator var33 = minYChunks.iterator();

            while (true) {
               if (var33.hasNext()) {
                  short[] chunk = (short[])var33.next();

                  for (int y = 0; y < minYSectionRelative; y++) {
                     for (int z = 0; z < 16; z++) {
                        if (chunk[y + z * 16] != 0) {
                           minYSectionRelative = y;
                           break;
                        }
                     }
                  }

                  if (minYSectionRelative != 0) {
                     continue;
                  }
               }

               if (BuildConfig.DEBUG && (minYSectionRelative < 0 || minYSectionRelative > 15)) {
                  throw new FaultyImplementationError("Sections: " + Arrays.deepToString(minYChunks.toArray()));
               }

               this.min.setY(minYChunk * 16 + minYSectionRelative);
               break;
            }
         }

         if (updateMinZ) {
            int minZSectionRelative = BuildConfig.DEBUG ? Integer.MAX_VALUE : 15;
            Iterator var34 = minZChunks.iterator();

            while (true) {
               if (var34.hasNext()) {
                  short[] chunk = (short[])var34.next();

                  for (int zx = 0; zx < minZSectionRelative; zx++) {
                     for (int y = 0; y < 16; y++) {
                        if (chunk[y + zx * 16] != 0) {
                           minZSectionRelative = zx;
                           break;
                        }
                     }
                  }

                  if (minZSectionRelative != 0) {
                     continue;
                  }
               }

               if (BuildConfig.DEBUG && (minZSectionRelative < 0 || minZSectionRelative > 15)) {
                  throw new FaultyImplementationError("Sections: " + Arrays.deepToString(minZChunks.toArray()));
               }

               this.min.setZ(minZChunk * 16 + minZSectionRelative);
               break;
            }
         }

         if (updateMaxX) {
            int maxXSectionRelative = BuildConfig.DEBUG ? Integer.MIN_VALUE : 0;
            Iterator var35 = maxXChunks.iterator();

            while (true) {
               if (var35.hasNext()) {
                  short[] chunk = (short[])var35.next();

                  for (short value : chunk) {
                     maxXSectionRelative = Math.max(maxXSectionRelative, 31 - Integer.numberOfLeadingZeros(value & '\uffff'));
                  }

                  if (maxXSectionRelative != 15) {
                     continue;
                  }
               }

               if (BuildConfig.DEBUG && (maxXSectionRelative < 0 || maxXSectionRelative > 15)) {
                  throw new FaultyImplementationError("Sections: " + Arrays.deepToString(maxXChunks.toArray()));
               }

               this.max.setX(maxXChunk * 16 + maxXSectionRelative);
               break;
            }
         }

         if (updateMaxY) {
            int maxYSectionRelative = BuildConfig.DEBUG ? Integer.MIN_VALUE : 0;
            Iterator var36 = maxYChunks.iterator();

            while (true) {
               if (var36.hasNext()) {
                  short[] chunk = (short[])var36.next();

                  for (int yx = 15; yx > maxYSectionRelative; yx--) {
                     for (int zx = 0; zx < 16; zx++) {
                        if (chunk[yx + zx * 16] != 0) {
                           maxYSectionRelative = yx;
                           break;
                        }
                     }
                  }

                  if (maxYSectionRelative != 15) {
                     continue;
                  }
               }

               if (BuildConfig.DEBUG && (maxYSectionRelative < 0 || maxYSectionRelative > 15)) {
                  throw new FaultyImplementationError("Sections: " + Arrays.deepToString(maxYChunks.toArray()));
               }

               this.max.setY(maxYChunk * 16 + maxYSectionRelative);
               break;
            }
         }

         if (updateMaxZ) {
            int maxZSectionRelativex = BuildConfig.DEBUG ? Integer.MIN_VALUE : 0;
            Iterator var37 = maxZChunks.iterator();

            while (true) {
               if (var37.hasNext()) {
                  short[] chunk = (short[])var37.next();

                  for (int zxx = 15; zxx > maxZSectionRelativex; zxx--) {
                     for (int yx = 0; yx < 16; yx++) {
                        if (chunk[yx + zxx * 16] != 0) {
                           maxZSectionRelativex = zxx;
                           break;
                        }
                     }
                  }

                  if (maxZSectionRelativex != 15) {
                     continue;
                  }
               }

               if (BuildConfig.DEBUG && (maxZSectionRelativex < 0 || maxZSectionRelativex > 15)) {
                  throw new FaultyImplementationError("Sections: " + Arrays.deepToString(maxZChunks.toArray()));
               }

               this.max.setZ(maxZChunk * 16 + maxZSectionRelativex);
               break;
            }
         }
      }
   }

   public boolean intersectAABB(BlockPos min, BlockPos max) {
      if (BuildConfig.DEBUG && this.closed) {
         throw new FaultyImplementationError();
      } else if (this.positionSet.count() <= 0) {
         return false;
      } else {
         min = new BlockPos(Math.max(min.getX(), this.min.getX()), Math.max(min.getY(), this.min.getY()), Math.max(min.getZ(), this.min.getZ()));
         max = new BlockPos(Math.min(max.getX(), this.max.getX()), Math.min(max.getY(), this.max.getY()), Math.min(max.getZ(), this.max.getZ()));
         if (min.getX() <= max.getX() && min.getY() <= max.getY() && min.getZ() <= max.getZ()) {
            if (min.getX() == this.min.getX()
               && min.getY() == this.min.getY()
               && min.getZ() == this.min.getZ()
               && max.getX() == this.max.getX()
               && max.getY() == this.max.getY()
               && max.getZ() == this.max.getZ()) {
               return false;
            } else {
               int minSectionX = min.getX() >> 4;
               int minSectionY = min.getY() >> 4;
               int minSectionZ = min.getZ() >> 4;
               int maxSectionX = max.getX() >> 4;
               int maxSectionY = max.getY() >> 4;
               int maxSectionZ = max.getZ() >> 4;
               this.dirtyChunks.clear();
               this.clearChunkData = true;
               int oldCount = this.positionSet.count();
               int newCount = 0;
               PositionSet oldPositionSet = this.positionSet;
               this.positionSet = new PositionSet();
               Long2ObjectMap<short[]> newPositionSetMap = this.positionSet.unsafeGetRawMap();

               for (int sx = minSectionX; sx <= maxSectionX; sx++) {
                  for (int sy = minSectionY; sy <= maxSectionY; sy++) {
                     for (int sz = minSectionZ; sz <= maxSectionZ; sz++) {
                        long pos = BlockPos.asLong(sx, sy, sz);
                        short[] chunk = oldPositionSet.getChunk(pos);
                        if (chunk != null) {
                           int lx = Math.max(0, min.getX() - sx * 16);
                           int ux = Math.min(15, max.getX() - sx * 16);
                           int ly = Math.max(0, min.getY() - sy * 16);
                           int uy = Math.min(15, max.getY() - sy * 16);
                           int lz = Math.max(0, min.getZ() - sz * 16);
                           int uz = Math.min(15, max.getZ() - sz * 16);
                           if (lx == 0 && ux == 15 && ly == 0 && uy == 15 && lz == 0 && uz == 15) {
                              for (short s : chunk) {
                                 newCount += Integer.bitCount(s & '\uffff');
                              }
                           } else {
                              int mask = (1 << ux - lx + 1) - 1 << lx;

                              for (int z = 0; z < 16; z++) {
                                 for (int y = 0; y < ly; y++) {
                                    chunk[y + z * 16] = 0;
                                 }

                                 for (int y = uy + 1; y < 16; y++) {
                                    chunk[y + z * 16] = 0;
                                 }
                              }

                              boolean empty = true;

                              for (int y = ly; y <= uy; y++) {
                                 for (int z = 0; z < lz; z++) {
                                    chunk[y + z * 16] = 0;
                                 }

                                 for (int z = uz + 1; z < 16; z++) {
                                    chunk[y + z * 16] = 0;
                                 }

                                 for (int z = lz; z <= uz; z++) {
                                    int masked = chunk[y + z * 16] & mask;
                                    if (masked != 0) {
                                       empty = false;
                                       newCount += Integer.bitCount(masked);
                                    }

                                    chunk[y + z * 16] = (short)masked;
                                 }
                              }

                              if (empty) {
                                 continue;
                              }
                           }

                           newPositionSetMap.put(pos, chunk);
                           this.dirtyChunks.add(pos);
                        }
                     }
                  }
               }

               boolean removeCoversMinX = min.getX() <= this.min.getX();
               boolean removeCoversMinY = min.getY() <= this.min.getY();
               boolean removeCoversMinZ = min.getZ() <= this.min.getZ();
               boolean removeCoversMaxX = max.getX() >= this.max.getX();
               boolean removeCoversMaxY = max.getY() >= this.max.getY();
               boolean removeCoversMaxZ = max.getZ() >= this.max.getZ();
               boolean updateMinX = !removeCoversMinX || !removeCoversMinY || !removeCoversMaxY || !removeCoversMinZ || !removeCoversMaxZ;
               boolean updateMinY = !removeCoversMinX || !removeCoversMaxX || !removeCoversMinY || !removeCoversMinZ || !removeCoversMaxZ;
               boolean updateMinZ = !removeCoversMinX || !removeCoversMaxX || !removeCoversMinY || !removeCoversMaxY || !removeCoversMinZ;
               boolean updateMaxX = !removeCoversMaxX || !removeCoversMinY || !removeCoversMaxY || !removeCoversMinZ || !removeCoversMaxZ;
               boolean updateMaxY = !removeCoversMinX || !removeCoversMaxX || !removeCoversMaxY || !removeCoversMinZ || !removeCoversMaxZ;
               boolean updateMaxZ = !removeCoversMinX || !removeCoversMaxX || !removeCoversMinY || !removeCoversMaxY || !removeCoversMaxZ;
               this.updateBoundingMinMax(updateMinX, updateMinY, updateMinZ, updateMaxX, updateMaxY, updateMaxZ);
               if (oldCount <= newCount) {
                  throw new FaultyImplementationError("Count should have decreased. Was " + oldCount + ", now " + newCount);
               } else {
                  this.positionSet.unsafeSetCount(newCount);
                  if (BuildConfig.DEBUG) {
                     this.positionSet.validateCount();
                     if (this.positionSet.count() <= 0) {
                        throw new FaultyImplementationError();
                     }

                     if (this.positionSet.count() > (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1)) {
                        throw new FaultyImplementationError("Too many blocks in set");
                     }

                     this.validateNoEmpty();
                     this.validateMinMax();
                  }

                  return true;
               }
            }
         } else {
            this.clear();
            return true;
         }
      }
   }

   private void validateNoEmpty() {
      ObjectIterator var1 = this.positionSet.unsafeGetRawMap().values().iterator();

      while (var1.hasNext()) {
         short[] chunk = (short[])var1.next();
         boolean empty = true;

         for (short value : chunk) {
            if (value != 0) {
               empty = false;
               break;
            }
         }

         if (empty) {
            throw new FaultyImplementationError("Empty chunk");
         }
      }
   }

   private void validateMinMax() {
      this.positionSet.forEach((x, y, z) -> {
         if (x < this.min.getX()) {
            throw new FaultyImplementationError("MinX: " + this.min.getX() + ", contained x: " + x);
         } else if (y < this.min.getY()) {
            throw new FaultyImplementationError("MinY: " + this.min.getY() + ", contained y: " + y);
         } else if (z < this.min.getZ()) {
            throw new FaultyImplementationError("MinZ: " + this.min.getZ() + ", contained z: " + z);
         } else if (x > this.max.getX()) {
            throw new FaultyImplementationError("MaxX: " + this.max.getX() + ", contained x: " + x);
         } else if (y > this.max.getY()) {
            throw new FaultyImplementationError("MaxY: " + this.max.getY() + ", contained y: " + y);
         } else if (z > this.max.getZ()) {
            throw new FaultyImplementationError("MaxZ: " + this.max.getZ() + ", contained z: " + z);
         }
      });
   }

   public void expand(BallShape ballShape, int amount) {
      if (BuildConfig.DEBUG && this.closed) {
         throw new FaultyImplementationError();
      } else if (amount > 0) {
         if (!this.positionSet.isEmpty()) {
            this.min.setX(this.min.getX() - amount);
            this.min.setY(this.min.getY() - amount);
            this.min.setZ(this.min.getZ() - amount);
            this.max.setX(this.max.getX() + amount);
            this.max.setY(this.max.getY() + amount);
            this.max.setZ(this.max.getZ() + amount);
            int[][] allOffsets = ExpandOffsets.create(ballShape, amount);
            PositionSet copied = this.positionSet.copy();
            copied.forEach((x, y, z) -> {
               int offsetIndex = 63;
               if (copied.contains(x - 1, y, z)) {
                  offsetIndex--;
               }

               if (copied.contains(x, y - 1, z)) {
                  offsetIndex -= 2;
               }

               if (copied.contains(x, y, z - 1)) {
                  offsetIndex -= 4;
               }

               if (copied.contains(x + 1, y, z)) {
                  offsetIndex -= 8;
               }

               if (copied.contains(x, y + 1, z)) {
                  offsetIndex -= 16;
               }

               if (copied.contains(x, y, z + 1)) {
                  offsetIndex -= 32;
               }

               int[] offsets = allOffsets[offsetIndex];

               for (int i = 0; i < offsets.length; i += 3) {
                  this.addSkipMinMax(x + offsets[i], y + offsets[i + 1], z + offsets[i + 2]);
               }
            });
            if (BuildConfig.DEBUG) {
               this.validateMinMax();
            }
         }
      }
   }

   public void shrink(BallShape ballShape, int amount) {
      if (BuildConfig.DEBUG && this.closed) {
         throw new FaultyImplementationError();
      } else if (amount > 0) {
         if (!this.positionSet.isEmpty()) {
            int radiusSquared = amount * amount;
            BlockPos[][] offsets = new BlockPos[64][];

            for (int index = 0; index < 64; index++) {
               int minRadiusX = (index & 1) == 0 ? 0 : -amount;
               int minRadiusY = (index & 2) == 0 ? 0 : -amount;
               int minRadiusZ = (index & 4) == 0 ? 0 : -amount;
               int maxRadiusX = (index & 8) == 0 ? 0 : amount;
               int maxRadiusY = (index & 16) == 0 ? 0 : amount;
               int maxRadiusZ = (index & 32) == 0 ? 0 : amount;
               List<BlockPos> off = new ArrayList<>();

               for (int xo = minRadiusX; xo <= maxRadiusX; xo++) {
                  for (int yo = minRadiusY; yo <= maxRadiusY; yo++) {
                     for (int zo = minRadiusZ; zo <= maxRadiusZ; zo++) {
                        if ((xo != 0 || yo != 0 || zo != 0) && ballShape.distanceSq(xo, yo, zo) <= radiusSquared) {
                           off.add(new BlockPos(xo, yo, zo));
                        }
                     }
                  }
               }

               offsets[index] = off.toArray(new BlockPos[0]);
            }

            Position2ByteMap edges = new Position2ByteMap();
            this.positionSet.forEach((x, y, z) -> {
               if (!this.positionSet.contains(x + 1, y, z)) {
                  edges.add(x + 1, y, z, (byte)1);
               }

               if (!this.positionSet.contains(x - 1, y, z)) {
                  edges.add(x - 1, y, z, (byte)8);
               }

               if (!this.positionSet.contains(x, y + 1, z)) {
                  edges.add(x, y + 1, z, (byte)2);
               }

               if (!this.positionSet.contains(x, y - 1, z)) {
                  edges.add(x, y - 1, z, (byte)16);
               }

               if (!this.positionSet.contains(x, y, z + 1)) {
                  edges.add(x, y, z + 1, (byte)4);
               }

               if (!this.positionSet.contains(x, y, z - 1)) {
                  edges.add(x, y, z - 1, (byte)32);
               }
            });
            PositionSet removedFromChunks = new PositionSet();
            edges.forEachEntry((x, y, z, v) -> {
               for (BlockPos offset : offsets[v]) {
                  int bx = x + offset.getX();
                  int by = y + offset.getY();
                  int bz = z + offset.getZ();
                  if (this.positionSet.remove(bx, by, bz)) {
                     removedFromChunks.add(bx >> 4, by >> 4, bz >> 4);
                  }
               }
            });
            if (this.positionSet.isEmpty()) {
               this.clear();
            } else {
               Long2ObjectMap<short[]> map = this.positionSet.unsafeGetRawMap();
               removedFromChunks.forEach((sx, sy, sz) -> {
                  long key = BlockPos.asLong(sx, sy, sz);
                  this.dirtyChunks.add(key);
                  short[] array = (short[])map.get(key);

                  for (short s : array) {
                     if (s != 0) {
                        return;
                     }
                  }

                  map.remove(key);
               });
               if (BuildConfig.DEBUG) {
                  this.validateNoEmpty();
               }

               this.updateBoundingMinMax(true, true, true, true, true, true);
               if (BuildConfig.DEBUG) {
                  this.validateMinMax();
               }
            }
         }
      }
   }

   public void addAll(PositionSet set) {
      if (BuildConfig.DEBUG && this.closed) {
         throw new FaultyImplementationError();
      } else {
         set.forEach(this::add);
      }
   }

   private void addSkipMinMax(int x, int y, int z) {
      if (BuildConfig.DEBUG && this.closed) {
         throw new FaultyImplementationError();
      } else {
         if (this.positionSet.add(x, y, z)) {
            if (BuildConfig.DEBUG) {
               if (x < this.min.getX()) {
                  throw new FaultyImplementationError();
               }

               if (y < this.min.getY()) {
                  throw new FaultyImplementationError();
               }

               if (z < this.min.getZ()) {
                  throw new FaultyImplementationError();
               }

               if (x > this.max.getX()) {
                  throw new FaultyImplementationError();
               }

               if (y > this.max.getY()) {
                  throw new FaultyImplementationError();
               }

               if (z > this.max.getZ()) {
                  throw new FaultyImplementationError();
               }
            }

            int chunkX = x >> 4;
            int chunkY = y >> 4;
            int chunkZ = z >> 4;
            boolean onNegXBorder = (x & 15) == 0;
            boolean onNegYBorder = (y & 15) == 0;
            boolean onNegZBorder = (z & 15) == 0;
            if (onNegXBorder) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY, chunkZ));
               if (onNegYBorder) {
                  this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY - 1, chunkZ));
               }

               if (onNegZBorder) {
                  this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY, chunkZ - 1));
               }
            } else if ((x & 15) == 15) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX + 1, chunkY, chunkZ));
            }

            if (onNegYBorder) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY - 1, chunkZ));
               if (onNegZBorder) {
                  this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY - 1, chunkZ - 1));
               }
            } else if ((y & 15) == 15) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY + 1, chunkZ));
            }

            if (onNegZBorder) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ - 1));
            } else if ((z & 15) == 15) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ + 1));
            }

            this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ));
         }
      }
   }

   public boolean add(int x, int y, int z) {
      if (BuildConfig.DEBUG && this.closed) {
         throw new FaultyImplementationError();
      } else if (this.positionSet.add(x, y, z)) {
         if (this.positionSet.count() == 1) {
            this.min.set(x, y, z);
            this.max.set(x, y, z);
         } else {
            if (x < this.min.getX()) {
               this.min.setX(x);
            }

            if (y < this.min.getY()) {
               this.min.setY(y);
            }

            if (z < this.min.getZ()) {
               this.min.setZ(z);
            }

            if (x > this.max.getX()) {
               this.max.setX(x);
            }

            if (y > this.max.getY()) {
               this.max.setY(y);
            }

            if (z > this.max.getZ()) {
               this.max.setZ(z);
            }
         }

         int chunkX = x >> 4;
         int chunkY = y >> 4;
         int chunkZ = z >> 4;
         boolean onNegXBorder = (x & 15) == 0;
         boolean onNegYBorder = (y & 15) == 0;
         boolean onNegZBorder = (z & 15) == 0;
         if (onNegXBorder) {
            this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY, chunkZ));
            if (onNegYBorder) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY - 1, chunkZ));
            }

            if (onNegZBorder) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY, chunkZ - 1));
            }
         } else if ((x & 15) == 15) {
            this.dirtyChunks.add(BlockPos.asLong(chunkX + 1, chunkY, chunkZ));
         }

         if (onNegYBorder) {
            this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY - 1, chunkZ));
            if (onNegZBorder) {
               this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY - 1, chunkZ - 1));
            }
         } else if ((y & 15) == 15) {
            this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY + 1, chunkZ));
         }

         if (onNegZBorder) {
            this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ - 1));
         } else if ((z & 15) == 15) {
            this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ + 1));
         }

         this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ));
         return true;
      } else {
         return false;
      }
   }

   private static final class ChunkData extends IntrusiveLinkedElement<ChunkedBooleanRegion.ChunkData> {
      AxiomDrawBuffer buffer = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
      SortStateWrapper sortState = null;
      int offsetX;
      int offsetY;
      int offsetZ;
      float distanceSqToCamera;

      public ChunkData(int offsetX, int offsetY, int offsetZ) {
         this.offsetX = offsetX;
         this.offsetY = offsetY;
         this.offsetZ = offsetZ;
      }
   }
}
