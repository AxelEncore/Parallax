package com.moulberry.axiom.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDraw;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.utils.AxiomVertexFormats;
import com.moulberry.axiom.utils.FramebufferUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes.DoubleLineConsumer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public enum CollisionMeshOverlayRenderer {
   INSTANCE;

   private final Long2ObjectMap<AxiomDrawBuffer> chunkDataMap = new Long2ObjectOpenHashMap();
   private final LongSet tickDirtyChunkSet = new LongOpenHashSet();
   private final LongSet forgetChunkSet = new LongOpenHashSet();
   private boolean clearChunkData = false;
   private RenderTarget renderTarget;
   private static final int QUEUE_SIZE = 16;
   private final LongSet buildingChunkList = new LongOpenHashSet();
   private final LongSet ignoreBuiltChunkSet = new LongOpenHashSet();
   private final ArrayBlockingQueue<CollisionMeshOverlayRenderer.BuildTask> buildTasks = new ArrayBlockingQueue<>(16);
   private final ArrayBlockingQueue<CollisionMeshOverlayRenderer.BuildResult> buildResults = new ArrayBlockingQueue<>(16);
   private final AtomicBoolean startedMesherThread = new AtomicBoolean(false);
   private static final Object2BooleanMap<BlockState> SHOULD_RENDER_COLLISION_MESH = new Object2BooleanOpenHashMap();

   private void startCollisionMesherIfNeeded() {
      if (this.startedMesherThread.compareAndSet(false, true)) {
         Thread thread = new Thread(() -> {
            try {
               while (true) {
                  CollisionMeshOverlayRenderer.BuildTask task = INSTANCE.buildTasks.take();
                  VertexConsumerProvider bufferBuilder = VertexConsumerProvider.owned(256);
                  renderChunk(task.sections, task.chunkX, task.chunkZ, task.minY, bufferBuilder);
                  INSTANCE.buildResults.put(new CollisionMeshOverlayRenderer.BuildResult(bufferBuilder, task.chunkX, task.chunkZ));
               }
            } catch (Throwable var2) {
               var2.printStackTrace();
            }
         });
         thread.setName("Axiom Collision Mesher");
         thread.start();
      }
   }

   public void render(AxiomWorldRenderContext rc) {
      if (this.clearChunkData) {
         this.clearChunkData = false;
         this.chunkDataMap.values().forEach(AxiomDrawBuffer::close);
         this.chunkDataMap.clear();

         CollisionMeshOverlayRenderer.BuildTask buildTask;
         while ((buildTask = this.buildTasks.poll()) != null) {
            this.buildingChunkList.remove(ChunkPos.asLong(buildTask.chunkX, buildTask.chunkZ));
         }

         CollisionMeshOverlayRenderer.BuildResult buildResult;
         while ((buildResult = this.buildResults.poll()) != null) {
            this.buildingChunkList.remove(ChunkPos.asLong(buildResult.chunkX, buildResult.chunkZ));
            buildResult.bufferBuilder.close();
         }

         this.ignoreBuiltChunkSet.addAll(this.buildingChunkList);
         this.buildingChunkList.clear();
      }

      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         if (Axiom.configuration.blockAttributes.showCollisionMesh) {
            this.uploadDirty();
            float offsetY = level.getMinBuildHeight();
            int mainWidth = Minecraft.getInstance().getMainRenderTarget().width;
            int mainHeight = Minecraft.getInstance().getMainRenderTarget().height;
            this.renderTarget = FramebufferUtils.resizeOrCreateFramebuffer(this.renderTarget, mainWidth, mainHeight);
            FramebufferUtils.clear(this.renderTarget, 0);
            FramebufferUtils.copyDepth(Minecraft.getInstance().getMainRenderTarget(), this.renderTarget);
            List<AxiomDraw> axiomDraws = new ArrayList<>();
            Matrix4f modelViewMatrix = rc.poseStack().last().pose();
            ObjectIterator var8 = this.chunkDataMap.long2ObjectEntrySet().iterator();

            while (var8.hasNext()) {
               Entry<AxiomDrawBuffer> entry = (Entry<AxiomDrawBuffer>)var8.next();
               Matrix4f translated = new Matrix4f(modelViewMatrix);
               int offsetX = ChunkPos.getX(entry.getLongKey()) * 16;
               int offsetZ = ChunkPos.getZ(entry.getLongKey()) * 16;
               translated.translate((float)(offsetX - rc.x()), (float)(offsetY - rc.y()), (float)(offsetZ - rc.z()));
               axiomDraws.add(new AxiomDraw((AxiomDrawBuffer)entry.getValue(), translated, null, null));
            }

            AxiomRenderer.renderPipeline(AxiomRenderPipelines.COLLISION_MESH_OVERLAY_PIPELINE, this.renderTarget, axiomDraws);
            FramebufferUtils.blitToMainBlend(this.renderTarget, mainWidth, mainHeight);
         }
      }
   }

   public void uploadDirty() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         if (!this.forgetChunkSet.isEmpty()) {
            LongSet removeBuilding = new LongOpenHashSet();
            LongIterator longIterator = this.forgetChunkSet.longIterator();

            while (longIterator.hasNext()) {
               long pos = longIterator.nextLong();
               AxiomDrawBuffer vertexBuffer = (AxiomDrawBuffer)this.chunkDataMap.remove(pos);
               if (vertexBuffer != null) {
                  vertexBuffer.close();
               }

               if (this.buildingChunkList.remove(pos)) {
                  removeBuilding.add(pos);
               }
            }

            this.forgetChunkSet.clear();
            this.buildTasks.removeIf(buildTask -> removeBuilding.remove(ChunkPos.asLong(buildTask.chunkX, buildTask.chunkZ)));
            this.buildResults.removeIf(buildResultx -> {
               if (removeBuilding.remove(ChunkPos.asLong(buildResultx.chunkX, buildResultx.chunkZ))) {
                  buildResultx.bufferBuilder.close();
                  return true;
               } else {
                  return false;
               }
            });
            this.ignoreBuiltChunkSet.addAll(removeBuilding);
         }

         CollisionMeshOverlayRenderer.BuildResult buildResult;
         while ((buildResult = this.buildResults.poll()) != null) {
            long posx = ChunkPos.asLong(buildResult.chunkX, buildResult.chunkZ);
            if (!this.ignoreBuiltChunkSet.remove(posx)) {
               MeshData result = buildResult.bufferBuilder.build();
               if (result != null) {
                  AxiomDrawBuffer vertexBufferx = (AxiomDrawBuffer)this.chunkDataMap
                     .computeIfAbsent(posx, k -> new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE));
                  vertexBufferx.upload(result);
               } else {
                  AxiomDrawBuffer vertexBufferx = (AxiomDrawBuffer)this.chunkDataMap.remove(posx);
                  if (vertexBufferx != null) {
                     vertexBufferx.close();
                  }
               }
            }

            this.buildingChunkList.remove(posx);
            buildResult.bufferBuilder.close();
         }

         if (!this.tickDirtyChunkSet.isEmpty()) {
            this.startCollisionMesherIfNeeded();
            LongIterator longIterator = this.tickDirtyChunkSet.longIterator();

            while (longIterator.hasNext()) {
               long posx = longIterator.nextLong();
               if (this.buildTasks.size() == 16) {
                  break;
               }

               if (!this.buildingChunkList.contains(posx) && !this.ignoreBuiltChunkSet.contains(posx)) {
                  longIterator.remove();
                  int chunkX = ChunkPos.getX(posx);
                  int chunkZ = ChunkPos.getZ(posx);
                  LevelChunk chunk = (LevelChunk)level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                  if (chunk != null) {
                     List<PalettedContainer<BlockState>> sections = new ArrayList<>();
                     int sectionsCount = chunk.getSectionsCount();

                     for (int y = 0; y < sectionsCount; y++) {
                        LevelChunkSection section = chunk.getSection(y);
                        if (section.hasOnlyAir()) {
                           sections.add(null);
                        } else {
                           sections.add(section.getStates().copy());
                        }
                     }

                     this.buildTasks.add(new CollisionMeshOverlayRenderer.BuildTask(sections, chunkX, chunkZ, level.getMinBuildHeight()));
                  }
               }
            }
         }
      }
   }

   private static void buildBox(BufferBuilder bufferBuilder, float x1, float y1, float z1, float x2, float y2, float z2) {
      float widthX = 1.0F / Math.abs(x2 - x1);
      float widthY = 1.0F / Math.abs(y2 - y1);
      float widthZ = 1.0F / Math.abs(z2 - z1);
      bufferBuilder.addVertex(x1, y1, z1).setUv(-widthY, -widthZ);
      bufferBuilder.addVertex(x1, y1, z2).setUv(-widthY, widthZ);
      bufferBuilder.addVertex(x1, y2, z2).setUv(widthY, widthZ);
      bufferBuilder.addVertex(x1, y2, z1).setUv(widthY, -widthZ);
      bufferBuilder.addVertex(x2, y2, z1).setUv(widthY, -widthZ);
      bufferBuilder.addVertex(x2, y2, z2).setUv(widthY, widthZ);
      bufferBuilder.addVertex(x2, y1, z2).setUv(-widthY, widthZ);
      bufferBuilder.addVertex(x2, y1, z1).setUv(-widthY, -widthZ);
      bufferBuilder.addVertex(x1, y2, z1).setUv(-widthX, -widthZ);
      bufferBuilder.addVertex(x1, y2, z2).setUv(-widthX, widthZ);
      bufferBuilder.addVertex(x2, y2, z2).setUv(widthX, widthZ);
      bufferBuilder.addVertex(x2, y2, z1).setUv(widthX, -widthZ);
      bufferBuilder.addVertex(x2, y1, z1).setUv(widthX, -widthZ);
      bufferBuilder.addVertex(x2, y1, z2).setUv(widthX, widthZ);
      bufferBuilder.addVertex(x1, y1, z2).setUv(-widthX, widthZ);
      bufferBuilder.addVertex(x1, y1, z1).setUv(-widthX, -widthZ);
      bufferBuilder.addVertex(x1, y1, z2).setUv(-widthY, -widthX);
      bufferBuilder.addVertex(x2, y1, z2).setUv(-widthY, widthX);
      bufferBuilder.addVertex(x2, y2, z2).setUv(widthY, widthX);
      bufferBuilder.addVertex(x1, y2, z2).setUv(widthY, -widthX);
      bufferBuilder.addVertex(x1, y2, z1).setUv(widthY, -widthX);
      bufferBuilder.addVertex(x2, y2, z1).setUv(widthY, widthX);
      bufferBuilder.addVertex(x2, y1, z1).setUv(-widthY, widthX);
      bufferBuilder.addVertex(x1, y1, z1).setUv(-widthY, -widthX);
   }

   private static void renderChunk(List<PalettedContainer<BlockState>> sections, int chunkX, int chunkZ, int minY, VertexConsumerProvider provider) {
      CollisionMeshOverlayRenderer.BoxConsumer consumer = new CollisionMeshOverlayRenderer.BoxConsumer();
      consumer.bufferBuilder = provider.begin(Mode.QUADS, AxiomVertexFormats.COLLISION_MESH_VERTEX_FORMAT);
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      BlockRenderDispatcher renderManager = Minecraft.getInstance().getBlockRenderer();
      Direction[] directions = Direction.values();

      for (int x = 0; x < 16; x++) {
         consumer.x = x;

         for (int z = 0; z < 16; z++) {
            consumer.z = z;

            for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
               PalettedContainer<BlockState> container = sections.get(sectionIndex);
               if (container != null) {
                  for (int y = 0; y < 16; y++) {
                     BlockState blockState = (BlockState)container.get(x, y, z);
                     if (!blockState.isAir()) {
                        boolean shouldRender = SHOULD_RENDER_COLLISION_MESH.computeIfAbsent(
                           blockState,
                           blockState1 -> {
                              VoxelShape collisionx = blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, mutableBlockPos);
                              boolean allSolid = ItemBlockRenderTypes.getChunkRenderType(blockState) == RenderType.solid();
                              if (collisionx == net.minecraft.world.phys.shapes.Shapes.block() && blockState.getRenderShape() == RenderShape.MODEL && allSolid) {
                                 return false;
                              } else if (collisionx != net.minecraft.world.phys.shapes.Shapes.empty() && !collisionx.isEmpty()) {
                                 BakedModel bakedModel = renderManager.getBlockModel(blockState);
                                 Vector3f minModel = new Vector3f(Float.POSITIVE_INFINITY);
                                 Vector3f maxModel = new Vector3f(Float.NEGATIVE_INFINITY);
                                 Consumer<Vector3f> positionConsumer = vector3f -> {
                                    minModel.min(vector3f);
                                    maxModel.max(vector3f);
                                 };
                                 RandomSource rand = RandomSource.create(42L);

                                 for (Direction direction : directions) {
                                    for (BakedQuad quad : bakedModel.getQuads(blockState, direction, rand)) {
                                       int[] vertices = quad.getVertices();

                                       for (int i = 0; i < 4; i++) {
                                          positionConsumer.accept(
                                             new Vector3f(
                                                Float.intBitsToFloat(vertices[8 * i]),
                                                Float.intBitsToFloat(vertices[8 * i + 1]),
                                                Float.intBitsToFloat(vertices[8 * i + 2])
                                             )
                                          );
                                       }
                                    }
                                 }

                                 AABB bounds = collisionx.bounds();
                                 return !Mth.equal(bounds.minX, minModel.x)
                                    || !Mth.equal(bounds.minY, minModel.y)
                                    || !Mth.equal(bounds.minZ, minModel.z)
                                    || !Mth.equal(bounds.maxX, maxModel.x)
                                    || !Mth.equal(bounds.maxY, maxModel.y)
                                    || !Mth.equal(bounds.maxZ, maxModel.z);
                              } else {
                                 return false;
                              }
                           }
                        );
                        if (shouldRender) {
                           mutableBlockPos.set(chunkX * 16 + x, minY + sectionIndex * 16 + y, chunkZ * 16 + z);
                           VoxelShape collision = blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, mutableBlockPos);
                           consumer.y = y + sectionIndex * 16;
                           collision.forAllBoxes(consumer);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void clear() {
      this.tickDirtyChunkSet.clear();
      this.forgetChunkSet.clear();
      this.clearChunkData = true;
   }

   public void markDirty(int chunkX, int chunkZ) {
      long pos = ChunkPos.asLong(chunkX, chunkZ);
      this.tickDirtyChunkSet.add(pos);
   }

   public void forgetChunk(int chunkX, int chunkZ) {
      long pos = ChunkPos.asLong(chunkX, chunkZ);
      this.forgetChunkSet.add(pos);
      this.tickDirtyChunkSet.remove(pos);
   }

   public static void clearBlockStateShouldRenderCache() {
      SHOULD_RENDER_COLLISION_MESH.clear();
   }

   private static class BoxConsumer implements DoubleLineConsumer {
      public BufferBuilder bufferBuilder;
      public int x;
      public int y;
      public int z;

      public void consume(double x1, double y1, double z1, double x2, double y2, double z2) {
         CollisionMeshOverlayRenderer.buildBox(
            this.bufferBuilder, this.x + (float)x1, this.y + (float)y1, this.z + (float)z1, this.x + (float)x2, this.y + (float)y2, this.z + (float)z2
         );
      }
   }

   private static final class BuildResult {
      private final VertexConsumerProvider bufferBuilder;
      private final int chunkX;
      private final int chunkZ;

      private BuildResult(VertexConsumerProvider bufferBuilder, int chunkX, int chunkZ) {
         this.bufferBuilder = bufferBuilder;
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
      }
   }

   private static final class BuildTask {
      private final List<PalettedContainer<BlockState>> sections;
      private final int chunkX;
      private final int chunkZ;
      private final int minY;

      private BuildTask(List<PalettedContainer<BlockState>> sections, int chunkX, int chunkZ, int minY) {
         this.sections = sections;
         this.chunkX = chunkX;
         this.chunkZ = chunkZ;
         this.minY = minY;
      }
   }
}
