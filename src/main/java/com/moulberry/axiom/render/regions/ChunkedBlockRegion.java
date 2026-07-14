package com.moulberry.axiom.render.regions;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.serialization.Codec;
import com.moulberry.axiom.GlobalCleaner;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionConsumer;
import com.moulberry.axiom.collections.list.IntrusiveLinkedElement;
import com.moulberry.axiom.collections.list.IntrusiveLinkedList;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDraw;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipeline;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.BlockTessellator;
import com.moulberry.axiom.render.MeshDataHelper;
import com.moulberry.axiom.render.SortStateWrapper;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.cache.TessellatedBlockCache;
import com.moulberry.axiom.utils.AxiomVertexFormats;
import com.moulberry.axiom.utils.BlockHelper;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.RenderHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiomclientapi.regions.BlockRegion;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Predicate;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.DimensionType.MonsterSettings;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class ChunkedBlockRegion implements BlockAndTintGetter, LevelAccessor, BlockRegion {
   public static final int SOLID_RENDER_LIMIT = 16777216;
   public static final int TRANSLUCENT_RENDER_LIMIT = 262144;
   public static int staticPackReloadIndex = 0;
   private int packReloadIndex;
   private MutableBlockPos min = null;
   private MutableBlockPos max = null;
   public BlockState uniqueBlockState = null;
   private int count;
   private final Position2ObjectMap<BlockState> blockData;
   private LongSet dirtyChunks = new LongOpenHashSet();
   private long lastSortMillis;
   private float lastSortX;
   private float lastSortY;
   private float lastSortZ;
   private int lastSortChunkX;
   private int lastSortChunkY;
   private int lastSortChunkZ;
   private final boolean drawOutlineForNonBlockingMotion;
   @Nullable
   private ChunkedBlockRegion.RenderData renderData = null;
   private float lastBlockOpacity = 1.0F;
   private final IntrusiveLinkedList<ChunkedBlockRegion.ChunkData> sortedChunkData = new IntrusiveLinkedList<>();
   private static final DimensionType DUMMY_DIMENSION_TYPE = new DimensionType(
      OptionalLong.empty(),
      true,
      false,
      false,
      true,
      1.0,
      true,
      false,
      -64,
      384,
      384,
      BlockTags.INFINIBURN_OVERWORLD,
      BuiltinDimensionTypes.OVERWORLD_EFFECTS,
      0.0F,
      new MonsterSettings(false, true, UniformInt.of(0, 7), 0)
   );
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private final EmptyChunkSource emptyChunkSource = new EmptyChunkSource(this);
   private final RandomSource randomSource = RandomSource.create(0L);

   public ChunkedBlockRegion() {
      this(true);
   }

   public ChunkedBlockRegion(boolean drawOutlineForNonBlockingMotion) {
      this.drawOutlineForNonBlockingMotion = drawOutlineForNonBlockingMotion;
      this.blockData = new Position2ObjectMap<>(k -> new BlockState[4096]);
      this.packReloadIndex = staticPackReloadIndex;
   }

   public void render(Camera camera, Vec3 translation, PoseStack matrix, Matrix4f projection, float blockOpacity, float outlineOpacity) {
      this.render(
         new AxiomWorldRenderContext(camera, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true), matrix, projection),
         translation,
         blockOpacity,
         outlineOpacity
      );
   }

   public void render(AxiomWorldRenderContext rc, Vec3 translation, float blockOpacity, float outlineOpacity) {
      this.render(rc, translation, null, blockOpacity, outlineOpacity);
   }

   public void render(AxiomWorldRenderContext rc, Vec3 translation, Quaternionf rotation, float blockOpacity, float outlineOpacity) {
      this.render(rc, translation, rotation, blockOpacity, outlineOpacity, true);
   }

   public void render(AxiomWorldRenderContext rc, Vec3 translation, Quaternionf rotation, float blockOpacity, float outlineOpacity, boolean polygonOffset) {
      this.render(rc, translation, rotation, blockOpacity, outlineOpacity, polygonOffset, null);
   }

   public void render(
      AxiomWorldRenderContext rc,
      Vec3 translation,
      Quaternionf rotation,
      float blockOpacity,
      float outlineOpacity,
      boolean polygonOffset,
      RenderTarget renderTarget
   ) {
      RenderSystem.assertOnRenderThread();
      if (!(blockOpacity <= 0.01F) || !(outlineOpacity <= 0.01F)) {
         if (this.count() > 0 && this.count() < 16777216) {
            boolean canUseAmbientOcclusion = true;
            boolean canResort = blockOpacity < 0.99;
            if (this.count() > 262144) {
               canUseAmbientOcclusion = false;
               if (this.lastBlockOpacity < 1.0F) {
                  this.lastBlockOpacity = this.lastBlockOpacity + Math.max(0.001F, VersionUtilsClient.getPartialTick(Minecraft.getInstance()) / 50.0F);
               }

               if (this.lastBlockOpacity > 1.0F) {
                  this.lastBlockOpacity = 1.0F;
               }

               blockOpacity = this.lastBlockOpacity;
               canResort = false;
            } else {
               this.lastBlockOpacity = blockOpacity;
            }

            if (this.packReloadIndex != staticPackReloadIndex) {
               this.packReloadIndex = staticPackReloadIndex;
               this.dirtyAll();
            }

            this.uploadDirty(rc, translation, canResort, canUseAmbientOcclusion);
            if (!this.sortedChunkData.isEmpty()) {
               ChunkedBlockRegion.ChunkData firstChunkData = this.sortedChunkData.first();
               double originX = firstChunkData.offsetX;
               double originY = firstChunkData.offsetY;
               double originZ = firstChunkData.offsetZ;
               Matrix4d initialTransform = new Matrix4d();
               initialTransform.translate(translation.x, translation.y, translation.z);
               initialTransform.translate(-rc.x(), -rc.y(), -rc.z());
               if (rotation != null) {
                  initialTransform.rotate(rotation);
               }

               initialTransform.translate(originX, originY, originZ);
               PoseStack poseStack = rc.poseStack();
               poseStack.pushPose();
               poseStack.mulPose(VersionUtilsClient.matrix4fcToMatrix4f(new Matrix4f(initialTransform)));
               Matrix4f modelViewMatrix = poseStack.last().pose();
               RenderHelper.pushModelViewMatrix(modelViewMatrix);
               List<AxiomDraw> blockDraws = new ArrayList<>(this.sortedChunkData.size());
               List<AxiomDraw> outlineDraws = new ArrayList<>(this.sortedChunkData.size());

               for (ChunkedBlockRegion.ChunkData data : this.sortedChunkData) {
                  Matrix4f translated = new Matrix4f(modelViewMatrix);
                  translated = translated.translate((float)(data.offsetX - originX), (float)(data.offsetY - originY), (float)(data.offsetZ - originZ));
                  if (data.block != null && blockOpacity > 0.01F) {
                     blockDraws.add(new AxiomDraw(data.block, translated, null, null));
                  }

                  if (data.outline != null && outlineOpacity > 0.01F) {
                     outlineDraws.add(new AxiomDraw(data.outline, translated, null, null));
                  }
               }

               if (blockOpacity > 0.01F && !blockDraws.isEmpty()) {
                  AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, blockOpacity);
                  AxiomRenderPipeline pipeline = polygonOffset ? AxiomRenderPipelines.AXIOM_BLOCK_WITH_OFFSET : AxiomRenderPipelines.AXIOM_BLOCK;
                  AxiomRenderer.renderPipeline(pipeline, renderTarget, blockDraws);
               }

               if (outlineOpacity > 0.01F && !outlineDraws.isEmpty()) {
                  AxiomRenderer.setShaderColour(0.7F, 0.7F, 1.0F, outlineOpacity);
                  AxiomRenderPipeline pipeline = polygonOffset
                     ? AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH_WITH_OFFSET
                     : AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH;
                  AxiomRenderer.renderPipeline(pipeline, renderTarget, outlineDraws);
               }

               RenderHelper.popModelViewStack();
               poseStack.popPose();
               AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
            }
         }
      }
   }

   private void uploadDirty(AxiomWorldRenderContext rc, Vec3 translation, boolean canResort, boolean canUseAmbientOcclusion) {
      if (this.renderData == null) {
         this.renderData = new ChunkedBlockRegion.RenderData(new Long2ObjectOpenHashMap(), new TessellatedBlockCache(), VertexConsumerProvider.owned(256));
         GlobalCleaner.INSTANCE.register(this, new ChunkedBlockRegion.CleanState(this.renderData));
      }

      LongSet chunksToUpload = this.dirtyChunks;
      this.dirtyChunks = new LongOpenHashSet();
      VertexConsumerProvider blockProvider = VertexConsumerProvider.shared();
      PoseStack matrices = new PoseStack();
      MutableBlockPos blockPos = new MutableBlockPos();
      float sortX = (float)(rc.x() - translation.x);
      float sortY = (float)(rc.y() - translation.y);
      float sortZ = (float)(rc.z() - translation.z);
      boolean addedNewChunk = false;
      TessellatedBlockCache tessellatedBlockCache = null;
      if (this.uniqueBlockState != null && this.blockData.chunkKeySet().size() > 8) {
         this.renderData.tessellatedBlockCache.setBlockState(this.uniqueBlockState);
         tessellatedBlockCache = this.renderData.tessellatedBlockCache;
      }

      BlockTessellator blockTessellator = new BlockTessellator(canUseAmbientOcclusion, true);
      LongIterator longIterator = chunksToUpload.longIterator();

      while (longIterator.hasNext()) {
         long pos = longIterator.nextLong();
         int chunkX = BlockPos.getX(pos);
         int chunkY = BlockPos.getY(pos);
         int chunkZ = BlockPos.getZ(pos);
         int offsetX = chunkX * 16;
         int offsetY = chunkY * 16;
         int offsetZ = chunkZ * 16;
         BufferBuilder blockBuilder = blockProvider.begin(Mode.QUADS, AxiomVertexFormats.AXIOM_BLOCK);
         BufferBuilder outlineBuilder = this.renderData.outlineProvider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         this.renderData.tessellatedBlockCache.setVertexFormat(AxiomVertexFormats.AXIOM_BLOCK);
         BlockState[] blocks = this.blockData.getChunk(pos);
         BlockState[] plusX = this.blockData.getChunk(BlockPos.asLong(chunkX + 1, chunkY, chunkZ));
         BlockState[] plusY = this.blockData.getChunk(BlockPos.asLong(chunkX, chunkY + 1, chunkZ));
         BlockState[] plusXY = this.blockData.getChunk(BlockPos.asLong(chunkX + 1, chunkY + 1, chunkZ));
         BlockState[] plusZ = this.blockData.getChunk(BlockPos.asLong(chunkX, chunkY, chunkZ + 1));
         BlockState[] plusXZ = this.blockData.getChunk(BlockPos.asLong(chunkX + 1, chunkY, chunkZ + 1));
         BlockState[] plusYZ = this.blockData.getChunk(BlockPos.asLong(chunkX, chunkY + 1, chunkZ + 1));
         Matrix4f currentPoseMatrix = matrices.last().pose();
         Matrix4f basePoseMatrix = new Matrix4f(currentPoseMatrix);

         for (int z = 0; z < 15; z++) {
            for (int y = 0; y < 15; y++) {
               if (blocks != null) {
                  for (int x = 0; x < 15; x++) {
                     int index = z * 16 * 16 + y * 16 + x;
                     BlockState dataState = blocks[index];
                     if (dataState != null) {
                        blockPos.set(offsetX + x, offsetY + y, offsetZ + z);
                        if (tessellatedBlockCache != null) {
                           tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
                        } else {
                           blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
                        }
                     }

                     boolean blockOutline = this.shouldBlockBeOutlined(dataState);
                     boolean blockPlusX = this.shouldBlockBeOutlined(blocks[index + 1]);
                     boolean blockPlusY = this.shouldBlockBeOutlined(blocks[index + 16]);
                     boolean blockPlusZ = this.shouldBlockBeOutlined(blocks[index + 256]);
                     boolean blockPlusXY = this.shouldBlockBeOutlined(blocks[index + 17]);
                     boolean blockPlusXZ = this.shouldBlockBeOutlined(blocks[index + 257]);
                     boolean blockPlusYZ = this.shouldBlockBeOutlined(blocks[index + 272]);
                     renderOutline(outlineBuilder, x, y, z, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
                  }
               }

               int index = z * 16 * 16 + y * 16 + 15;
               BlockState dataState = blocks == null ? null : blocks[index];
               if (dataState != null) {
                  blockPos.set(offsetX + 15, offsetY + y, offsetZ + z);
                  if (tessellatedBlockCache != null) {
                     tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
                  } else {
                     blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
                  }
               }

               boolean blockOutline = this.shouldBlockBeOutlined(dataState);
               boolean blockPlusX = plusX != null && this.shouldBlockBeOutlined(plusX[y * 16 + z * 16 * 16]);
               boolean blockPlusY = blocks != null && this.shouldBlockBeOutlined(blocks[index + 16]);
               boolean blockPlusZ = blocks != null && this.shouldBlockBeOutlined(blocks[index + 256]);
               boolean blockPlusXY = plusX != null && this.shouldBlockBeOutlined(plusX[(y + 1) * 16 + z * 16 * 16]);
               boolean blockPlusXZ = plusX != null && this.shouldBlockBeOutlined(plusX[y * 16 + (z + 1) * 16 * 16]);
               boolean blockPlusYZ = blocks != null && this.shouldBlockBeOutlined(blocks[index + 272]);
               renderOutline(outlineBuilder, 15, y, z, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
            }

            for (int x = 0; x < 15; x++) {
               int index = z * 16 * 16 + 240 + x;
               BlockState dataState = blocks == null ? null : blocks[index];
               if (dataState != null) {
                  blockPos.set(offsetX + x, offsetY + 15, offsetZ + z);
                  if (tessellatedBlockCache != null) {
                     tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
                  } else {
                     blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
                  }
               }

               boolean blockOutline = this.shouldBlockBeOutlined(dataState);
               boolean blockPlusX = blocks != null && this.shouldBlockBeOutlined(blocks[index + 1]);
               boolean blockPlusY = plusY != null && this.shouldBlockBeOutlined(plusY[x + z * 16 * 16]);
               boolean blockPlusZ = blocks != null && this.shouldBlockBeOutlined(blocks[index + 256]);
               boolean blockPlusXY = plusY != null && this.shouldBlockBeOutlined(plusY[x + 1 + z * 16 * 16]);
               boolean blockPlusXZ = blocks != null && this.shouldBlockBeOutlined(blocks[index + 257]);
               boolean blockPlusYZ = plusY != null && this.shouldBlockBeOutlined(plusY[x + (z + 1) * 16 * 16]);
               renderOutline(outlineBuilder, x, 15, z, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
            }

            int index = z * 16 * 16 + 240 + 15;
            BlockState dataState = blocks == null ? null : blocks[index];
            if (dataState != null) {
               blockPos.set(offsetX + 15, offsetY + 15, offsetZ + z);
               if (tessellatedBlockCache != null) {
                  tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
               } else {
                  blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
               }
            }

            boolean blockOutline = this.shouldBlockBeOutlined(dataState);
            boolean blockPlusX = plusX != null && this.shouldBlockBeOutlined(plusX[240 + z * 16 * 16]);
            boolean blockPlusY = plusY != null && this.shouldBlockBeOutlined(plusY[15 + z * 16 * 16]);
            boolean blockPlusZ = blocks != null && this.shouldBlockBeOutlined(blocks[index + 256]);
            boolean blockPlusXY = plusXY != null && this.shouldBlockBeOutlined(plusXY[z * 16 * 16]);
            boolean blockPlusXZ = plusX != null && this.shouldBlockBeOutlined(plusX[240 + (z + 1) * 16 * 16]);
            boolean blockPlusYZ = plusY != null && this.shouldBlockBeOutlined(plusY[15 + (z + 1) * 16 * 16]);
            renderOutline(outlineBuilder, 15, 15, z, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
         }

         for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 15; x++) {
               int index = 3840 + y * 16 + x;
               BlockState dataState = blocks == null ? null : blocks[index];
               if (dataState != null) {
                  blockPos.set(offsetX + x, offsetY + y, offsetZ + 15);
                  if (tessellatedBlockCache != null) {
                     tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
                  } else {
                     blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
                  }
               }

               boolean blockOutline = this.shouldBlockBeOutlined(dataState);
               boolean blockPlusX = blocks != null && this.shouldBlockBeOutlined(blocks[index + 1]);
               boolean blockPlusY = blocks != null && this.shouldBlockBeOutlined(blocks[index + 16]);
               boolean blockPlusZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[x + y * 16]);
               boolean blockPlusXY = blocks != null && this.shouldBlockBeOutlined(blocks[index + 17]);
               boolean blockPlusXZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[x + 1 + y * 16]);
               boolean blockPlusYZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[x + (y + 1) * 16]);
               renderOutline(outlineBuilder, x, y, 15, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
            }

            int index = 3840 + y * 16 + 15;
            BlockState dataState = blocks == null ? null : blocks[index];
            if (dataState != null) {
               blockPos.set(offsetX + 15, offsetY + y, offsetZ + 15);
               if (tessellatedBlockCache != null) {
                  tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
               } else {
                  blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
               }
            }

            boolean blockOutline = this.shouldBlockBeOutlined(dataState);
            boolean blockPlusX = plusX != null && this.shouldBlockBeOutlined(plusX[y * 16 + 3840]);
            boolean blockPlusY = blocks != null && this.shouldBlockBeOutlined(blocks[index + 16]);
            boolean blockPlusZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[15 + y * 16]);
            boolean blockPlusXY = plusX != null && this.shouldBlockBeOutlined(plusX[(y + 1) * 16 + 3840]);
            boolean blockPlusXZ = plusXZ != null && this.shouldBlockBeOutlined(plusXZ[y * 16]);
            boolean blockPlusYZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[15 + (y + 1) * 16]);
            renderOutline(outlineBuilder, 15, y, 15, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
         }

         for (int x = 0; x < 15; x++) {
            int index = 4080 + x;
            BlockState dataState = blocks == null ? null : blocks[index];
            if (dataState != null) {
               blockPos.set(offsetX + x, offsetY + 15, offsetZ + 15);
               if (tessellatedBlockCache != null) {
                  tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
               } else {
                  blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
               }
            }

            boolean blockOutline = this.shouldBlockBeOutlined(dataState);
            boolean blockPlusX = blocks != null && this.shouldBlockBeOutlined(blocks[index + 1]);
            boolean blockPlusY = plusY != null && this.shouldBlockBeOutlined(plusY[x + 3840]);
            boolean blockPlusZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[x + 240]);
            boolean blockPlusXY = plusY != null && this.shouldBlockBeOutlined(plusY[x + 1 + 3840]);
            boolean blockPlusXZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[x + 1 + 240]);
            boolean blockPlusYZ = plusYZ != null && this.shouldBlockBeOutlined(plusYZ[x]);
            renderOutline(outlineBuilder, x, 15, 15, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
         }

         int index = 4095;
         BlockState dataState = blocks == null ? null : blocks[index];
         if (dataState != null) {
            blockPos.set(offsetX + 15, offsetY + 15, offsetZ + 15);
            if (tessellatedBlockCache != null) {
               tessellatedBlockCache.renderBlock(blockBuilder, this, blockPos);
            } else {
               blockTessellator.tessellateBlockAndLiquidOffsetMod16(blockBuilder, blockPos, this, dataState);
            }
         }

         boolean blockOutline = this.shouldBlockBeOutlined(dataState);
         boolean blockPlusX = plusX != null && this.shouldBlockBeOutlined(plusX[4080]);
         boolean blockPlusY = plusY != null && this.shouldBlockBeOutlined(plusY[3855]);
         boolean blockPlusZ = plusZ != null && this.shouldBlockBeOutlined(plusZ[255]);
         boolean blockPlusXY = plusXY != null && this.shouldBlockBeOutlined(plusXY[3840]);
         boolean blockPlusXZ = plusXZ != null && this.shouldBlockBeOutlined(plusXZ[240]);
         boolean blockPlusYZ = plusYZ != null && this.shouldBlockBeOutlined(plusYZ[15]);
         renderOutline(outlineBuilder, 15, 15, 15, blockOutline, blockPlusX, blockPlusY, blockPlusZ, blockPlusXY, blockPlusXZ, blockPlusYZ);
         currentPoseMatrix.set(basePoseMatrix);
         if (blockBuilder.vertices == 0 && outlineBuilder.vertices == 0) {
            MeshDataHelper.discard(blockBuilder.build());
            MeshDataHelper.discard(outlineBuilder.build());
            ChunkedBlockRegion.ChunkData chunkData = (ChunkedBlockRegion.ChunkData)this.renderData.chunkData.remove(pos);
            if (chunkData != null) {
               this.sortedChunkData.remove(chunkData);
               if (chunkData.block != null) {
                  EditorUI.deferredClose(chunkData.block);
                  chunkData.block = null;
               }

               if (chunkData.outline != null) {
                  EditorUI.deferredClose(chunkData.outline);
                  chunkData.outline = null;
               }
            }
         } else {
            ChunkedBlockRegion.ChunkData chunkData = (ChunkedBlockRegion.ChunkData)this.renderData.chunkData.get(pos);
            if (chunkData == null) {
               chunkData = new ChunkedBlockRegion.ChunkData(offsetX, offsetY, offsetZ);
               this.renderData.chunkData.put(pos, chunkData);
               float dx = this.lastSortChunkX * 16 - chunkData.offsetX;
               float dy = this.lastSortChunkY * 16 - chunkData.offsetY;
               float dz = this.lastSortChunkZ * 16 - chunkData.offsetZ;
               chunkData.distanceSqToCamera = dx * dx + dy * dy + dz * dz;
               this.sortedChunkData.add(chunkData);
               addedNewChunk = true;
            }

            if (blockBuilder.vertices > 0) {
               MeshDataHelper.MeshDataAndSortState meshDataAndSortState = MeshDataHelper.buildAndSort(
                  blockBuilder, VertexSorting.byDistance(sortX - chunkData.offsetX, sortY - chunkData.offsetY, sortZ - chunkData.offsetZ)
               );
               if (meshDataAndSortState != null) {
                  chunkData.sortState = meshDataAndSortState.sortStateWrapper();
                  if (chunkData.block == null) {
                     chunkData.block = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
                  }

                  chunkData.block.upload(meshDataAndSortState.meshData());
               }
            } else {
               if (chunkData.block != null) {
                  EditorUI.deferredClose(chunkData.block);
                  chunkData.block = null;
               }

               MeshDataHelper.discard(blockBuilder.build());
            }

            if (outlineBuilder.vertices > 0) {
               MeshData meshData = outlineBuilder.build();
               if (meshData != null) {
                  if (chunkData.outline == null) {
                     chunkData.outline = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
                  }

                  chunkData.outline.upload(meshData);
               }
            } else {
               if (chunkData.outline != null) {
                  EditorUI.deferredClose(chunkData.outline);
                  chunkData.outline = null;
               }

               MeshDataHelper.discard(outlineBuilder.build());
            }
         }
      }

      if (!canResort) {
         if (addedNewChunk) {
            this.sortedChunkData.sort(Comparator.comparingDouble(c -> -c.distanceSqToCamera));
         }
      } else {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastSortMillis < 250L) {
            if (addedNewChunk) {
               this.sortedChunkData.sort(Comparator.comparingDouble(c -> -c.distanceSqToCamera));
            }
         } else {
            this.lastSortMillis = currentTime;
            double dX = sortX - this.lastSortX;
            double dY = sortY - this.lastSortY;
            double dZ = sortZ - this.lastSortZ;
            if (dX * dX + dY * dY + dZ * dZ < 1.0) {
               if (addedNewChunk) {
                  this.sortedChunkData.sort(Comparator.comparingDouble(c -> -c.distanceSqToCamera));
               }
            } else {
               this.lastSortX = sortX;
               this.lastSortY = sortY;
               this.lastSortZ = sortZ;
               int chunkX = SectionPos.posToSectionCoord(sortX);
               int chunkY = SectionPos.posToSectionCoord(sortY);
               int chunkZ = SectionPos.posToSectionCoord(sortZ);
               boolean resortChunkList = chunkX != this.lastSortChunkX || chunkY != this.lastSortChunkY || chunkZ != this.lastSortChunkZ;

               for (ChunkedBlockRegion.ChunkData chunkDatax : this.sortedChunkData) {
                  float relSortX = sortX - chunkDatax.offsetX;
                  float relSortY = sortY - chunkDatax.offsetY;
                  float relSortZ = sortZ - chunkDatax.offsetZ;
                  if (chunkDatax.sortState != null && chunkDatax.block != null) {
                     BufferBuilder bufferBuilder = blockProvider.begin(Mode.QUADS, AxiomVertexFormats.AXIOM_BLOCK);
                     chunkDatax.sortState = MeshDataHelper.resort(
                        bufferBuilder, chunkDatax.sortState, chunkDatax.block, VertexSorting.byDistance(relSortX, relSortY, relSortZ)
                     );
                  }

                  if (resortChunkList) {
                     float dx = chunkX * 16 - chunkDatax.offsetX;
                     float dy = chunkY * 16 - chunkDatax.offsetY;
                     float dz = chunkZ * 16 - chunkDatax.offsetZ;
                     chunkDatax.distanceSqToCamera = dx * dx + dy * dy + dz * dz;
                  }
               }

               if (resortChunkList) {
                  this.lastSortChunkX = chunkX;
                  this.lastSortChunkY = chunkY;
                  this.lastSortChunkZ = chunkZ;
                  this.sortedChunkData.sort(Comparator.comparingDouble(c -> -c.distanceSqToCamera));
               } else if (addedNewChunk) {
                  this.sortedChunkData.sort(Comparator.comparingDouble(c -> -c.distanceSqToCamera));
               }
            }
         }
      }
   }

   private static void renderOutline(
      BufferBuilder outlineBuilder,
      int x,
      int y,
      int z,
      boolean blockOutline,
      boolean blockPlusX,
      boolean blockPlusY,
      boolean blockPlusZ,
      boolean blockPlusXY,
      boolean blockPlusXZ,
      boolean blockPlusYZ
   ) {
      if (blockOutline ^ blockPlusZ ^ blockPlusY ^ blockPlusYZ) {
         VersionUtilsClient.legacySetLineWidthIgnored(
            outlineBuilder.addVertex(x, y + 1, z + 1).setColor(-1).setNormal(1.0F, 0.0F, 0.0F), RenderHelper.baseLineWidth
         );
         VersionUtilsClient.legacySetLineWidthIgnored(
            outlineBuilder.addVertex(x + 1, y + 1, z + 1).setColor(-1).setNormal(1.0F, 0.0F, 0.0F), RenderHelper.baseLineWidth
         );
      }

      if (blockOutline ^ blockPlusZ ^ blockPlusX ^ blockPlusXZ) {
         VersionUtilsClient.legacySetLineWidthIgnored(
            outlineBuilder.addVertex(x + 1, y, z + 1).setColor(-1).setNormal(0.0F, 1.0F, 0.0F), RenderHelper.baseLineWidth
         );
         VersionUtilsClient.legacySetLineWidthIgnored(
            outlineBuilder.addVertex(x + 1, y + 1, z + 1).setColor(-1).setNormal(0.0F, 1.0F, 0.0F), RenderHelper.baseLineWidth
         );
      }

      if (blockOutline ^ blockPlusX ^ blockPlusY ^ blockPlusXY) {
         VersionUtilsClient.legacySetLineWidthIgnored(
            outlineBuilder.addVertex(x + 1, y + 1, z).setColor(-1).setNormal(0.0F, 0.0F, 1.0F), RenderHelper.baseLineWidth
         );
         VersionUtilsClient.legacySetLineWidthIgnored(
            outlineBuilder.addVertex(x + 1, y + 1, z + 1).setColor(-1).setNormal(0.0F, 0.0F, 1.0F), RenderHelper.baseLineWidth
         );
      }
   }

   public BlockPos min() {
      return this.min;
   }

   public BlockPos max() {
      return this.max;
   }

   public void forEachEntry(PositionConsumer<BlockState> consumer) {
      this.blockData.forEachEntry(consumer);
   }

   public void forEachEntryLowestFirst(PositionConsumer<BlockState> consumer) {
      LongList chunks = new LongArrayList(this.blockData.chunkKeySet());
      chunks.sort((first, second) -> Integer.compare(BlockPos.getY(first), BlockPos.getY(second)));
      LongIterator longIterator = chunks.longIterator();

      while (longIterator.hasNext()) {
         long pos = longIterator.nextLong();
         BlockState[] chunk = this.blockData.getChunk(pos);
         int cx = BlockPos.getX(pos) * 16;
         int cy = BlockPos.getY(pos) * 16;
         int cz = BlockPos.getZ(pos) * 16;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  BlockState t = chunk[index++];
                  if (t != null) {
                     consumer.accept(cx + x, cy + y, cz + z, t);
                  }
               }
            }
         }
      }
   }

   public void forEachChunk(PositionConsumer<BlockState[]> consumer) {
      this.blockData.forEachChunk(consumer);
   }

   public ChunkedBlockRegion rotate(Axis axis, int count) {
      count %= 4;
      if (count < 0) {
         count += 4;
      }

      if (count == 0) {
         return this;
      } else {
         ChunkedBlockRegion rotated = new ChunkedBlockRegion();
         switch (axis) {
            case X:
               switch (count) {
                  case 1:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(x, -z, y, BlockHelper.rotateX(block, Rotation.COUNTERCLOCKWISE_90)));
                     return rotated;
                  case 2:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(x, -y, -z, BlockHelper.rotateX(block, Rotation.CLOCKWISE_180)));
                     return rotated;
                  case 3:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(x, z, -y, BlockHelper.rotateX(block, Rotation.CLOCKWISE_90)));
                     return rotated;
                  default:
                     throw new FaultyImplementationError();
               }
            case Y:
               switch (count) {
                  case 1:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(z, y, -x, BlockHelper.rotateY(block, Rotation.COUNTERCLOCKWISE_90)));
                     return rotated;
                  case 2:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(-x, y, -z, BlockHelper.rotateY(block, Rotation.CLOCKWISE_180)));
                     return rotated;
                  case 3:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(-z, y, x, BlockHelper.rotateY(block, Rotation.CLOCKWISE_90)));
                     return rotated;
                  default:
                     throw new FaultyImplementationError();
               }
            case Z:
               switch (count) {
                  case 1:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(-y, x, z, BlockHelper.rotateZ(block, Rotation.COUNTERCLOCKWISE_90)));
                     break;
                  case 2:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(-x, -y, z, BlockHelper.rotateZ(block, Rotation.CLOCKWISE_180)));
                     break;
                  case 3:
                     this.forEachEntry((x, y, z, block) -> rotated.addBlock(y, -x, z, BlockHelper.rotateZ(block, Rotation.CLOCKWISE_90)));
                     break;
                  default:
                     throw new FaultyImplementationError();
               }
         }

         return rotated;
      }
   }

   public ChunkedBlockRegion flip(Axis axis) {
      ChunkedBlockRegion flipped = new ChunkedBlockRegion();
      switch (axis) {
         case X:
            this.forEachEntry((x, y, z, block) -> flipped.addBlock(-x, y, z, BlockHelper.flipX(block)));
            break;
         case Y:
            this.forEachEntry((x, y, z, block) -> flipped.addBlock(x, -y, z, BlockHelper.flipY(block)));
            break;
         case Z:
            this.forEachEntry((x, y, z, block) -> flipped.addBlock(x, y, -z, BlockHelper.flipZ(block)));
      }

      return flipped;
   }

   public void save(ListTag list) {
      Codec<PalettedContainer<BlockState>> codec = BlockBuffer.getCodecForEmptyBlockState(BlockBuffer.EMPTY_STATE);
      this.blockData.forEachChunk((cx, cy, cz, data) -> {
         PalettedContainer<BlockState> container = DFUHelper.createPalettedContainer(Block.BLOCK_STATE_REGISTRY, BlockBuffer.EMPTY_STATE);
         boolean containsBlock = false;
         int index = 0;

         for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 16; y++) {
               for (int x = 0; x < 16; x++) {
                  BlockState blockState = data[index++];
                  if (blockState != null) {
                     container.set(x, y, z, blockState);
                     containsBlock = true;
                  }
               }
            }
         }

         if (containsBlock) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("X", cx);
            tag.putInt("Y", cy);
            tag.putInt("Z", cz);
            Tag encoded = VersionUtils.getOrThrow(codec.encodeStart(NbtOps.INSTANCE, container));
            tag.put("BlockStates", encoded);
            list.add(tag);
         }
      });
   }

   public LongSet chunkKeySet() {
      return this.blockData.chunkKeySet();
   }

   public BlockState[] getChunk(int cx, int cy, int cz) {
      return this.blockData.getChunk(cx, cy, cz);
   }

   public Position2ObjectMap<BlockState> copyBlockData() {
      return this.blockData.copy();
   }

   public boolean isEmpty() {
      return this.count == 0;
   }

   public int count() {
      return this.count;
   }

   public void clear() {
      if (this.renderData != null) {
         ObjectIterator var1 = this.renderData.chunkData.values().iterator();

         while (var1.hasNext()) {
            ChunkedBlockRegion.ChunkData chunkDatum = (ChunkedBlockRegion.ChunkData)var1.next();
            if (chunkDatum.block != null) {
               chunkDatum.block.close();
            }

            if (chunkDatum.outline != null) {
               chunkDatum.outline.close();
            }
         }

         this.renderData.chunkData.clear();
         this.renderData.tessellatedBlockCache.clear();
      }

      this.blockData.clear();
      this.sortedChunkData.clear();
      this.dirtyChunks.clear();
      this.min = null;
      this.max = null;
      this.uniqueBlockState = null;
      this.count = 0;
   }

   public BlockPos getCenter() {
      return this.min != null && this.max != null
         ? new BlockPos((this.min.getX() + this.max.getX()) / 2, (this.min.getY() + this.max.getY()) / 2, (this.min.getZ() + this.max.getZ()) / 2)
         : BlockPos.ZERO;
   }

   private boolean shouldBlockBeOutlined(BlockState blockState) {
      if (blockState == null) {
         return false;
      } else {
         return this.drawOutlineForNonBlockingMotion ? !blockState.isAir() : blockState.blocksMotion();
      }
   }

   public void addBlockIfNotPresent(int x, int y, int z, BlockState block) {
      if (this.blockData.get(x, y, z) == null) {
         this.addBlock(x, y, z, block);
      }
   }

   public void addBlock(int x, int y, int z, BlockState block) {
      Objects.requireNonNull(block);
      if (this.count == 0) {
         this.min = new MutableBlockPos(x, y, z);
         this.max = new MutableBlockPos(x, y, z);
         this.uniqueBlockState = block;
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

         if (this.uniqueBlockState != block) {
            this.uniqueBlockState = null;
         }
      }

      BlockState oldState = this.blockData.getAndPut(x, y, z, block);
      if (oldState != block) {
         if (oldState == null) {
            this.count++;
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

   public void addBlock(BlockPos pos, BlockState block) {
      this.addBlock(pos.getX(), pos.getY(), pos.getZ(), block);
   }

   public void addBlockWithoutDirty(int x, int y, int z, BlockState block) {
      Objects.requireNonNull(block);
      if (this.min == null) {
         this.min = new MutableBlockPos(x, y, z);
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
      }

      if (this.max == null) {
         this.max = new MutableBlockPos(x, y, z);
      } else {
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

      BlockState oldState = this.blockData.getAndPut(x, y, z, block);
      if (oldState == null) {
         this.count++;
      }
   }

   @Deprecated
   public void unsafeRemoveBlockWithoutDirty(int x, int y, int z) {
      BlockState oldState = this.blockData.getAndPut(x, y, z, null);
      if (oldState != null) {
         this.count--;
      }
   }

   public void dirtyAll() {
      LongIterator iterator = this.blockData.chunkKeySet().iterator();

      while (iterator.hasNext()) {
         long chunkPos = iterator.nextLong();
         int chunkX = BlockPos.getX(chunkPos);
         int chunkY = BlockPos.getY(chunkPos);
         int chunkZ = BlockPos.getZ(chunkPos);
         this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY, chunkZ));
         this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY - 1, chunkZ));
         this.dirtyChunks.add(BlockPos.asLong(chunkX - 1, chunkY, chunkZ - 1));
         this.dirtyChunks.add(BlockPos.asLong(chunkX + 1, chunkY, chunkZ));
         this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY - 1, chunkZ));
         this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY - 1, chunkZ - 1));
         this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY + 1, chunkZ));
         this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ - 1));
         this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ + 1));
         this.dirtyChunks.add(BlockPos.asLong(chunkX, chunkY, chunkZ));
      }
   }

   public BlockState getBlockStateOrDelegate(BlockPos blockPos, Level level) {
      BlockState blockState = this.blockData.get(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      return blockState != null ? blockState : level.getBlockState(blockPos);
   }

   public float getShade(Direction direction, boolean shaded) {
      switch (direction) {
         case DOWN:
            return 0.5F;
         case UP:
            return 1.0F;
         case NORTH:
         case SOUTH:
            return 0.8F;
         case WEST:
         case EAST:
            return 0.6F;
         default:
            return 1.0F;
      }
   }

   public LevelLightEngine getLightEngine() {
      throw new UnsupportedOperationException();
   }

   public int getBrightness(LightLayer type, BlockPos pos) {
      return 15;
   }

   public int getRawBrightness(BlockPos pos, int ambientDarkness) {
      return 15;
   }

   public boolean canSeeSky(BlockPos pos) {
      return false;
   }

   @Nullable
   public ChunkAccess getChunk(int i, int j, ChunkStatus chunkStatus, boolean bl) {
      return null;
   }

   public int getHeight(Types types, int i, int j) {
      return 0;
   }

   public int getSkyDarken() {
      return 0;
   }

   public BiomeManager getBiomeManager() {
      return null;
   }

   public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
      if (colorResolver == BiomeColors.WATER_COLOR_RESOLVER) {
         return -12618012;
      } else if (colorResolver == BiomeColors.GRASS_COLOR_RESOLVER) {
         return GrassColor.get(0.8F, 0.4F);
      } else {
         return colorResolver == BiomeColors.FOLIAGE_COLOR_RESOLVER ? FoliageColor.get(0.8F, 0.4F) : -1;
      }
   }

   public Holder<Biome> getUncachedNoiseBiome(int i, int j, int k) {
      return null;
   }

   public boolean isClientSide() {
      return false;
   }

   public int getSeaLevel() {
      return 70;
   }

   public DimensionType dimensionType() {
      return DUMMY_DIMENSION_TYPE;
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return null;
   }

   public BlockState getBlockStateOrNull(int x, int y, int z) {
      return this.blockData.get(x, y, z);
   }

   public BlockState getBlockStateOrAir(int x, int y, int z) {
      BlockState blockState = this.blockData.get(x, y, z);
      return blockState != null ? blockState : AIR;
   }

   @NotNull
   public BlockState getBlockState(BlockPos pos) {
      BlockState blockState = this.blockData.get(pos.getX(), pos.getY(), pos.getZ());
      return blockState != null ? blockState : AIR;
   }

   @NotNull
   public FluidState getFluidState(BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }

   public int getMinBuildHeight() {
      return this.min.getY();
   }

   public int getHeight() {
      return this.max.getY() - this.min.getY() + 1;
   }

   @NotNull
   public RegistryAccess registryAccess() {
      throw new UnsupportedOperationException();
   }

   @NotNull
   public FeatureFlagSet enabledFeatures() {
      return FeatureFlagSet.of();
   }

   public long nextSubTickCount() {
      return 0L;
   }

   @NotNull
   public LevelTickAccess<Block> getBlockTicks() {
      throw new UnsupportedOperationException();
   }

   @NotNull
   public LevelTickAccess<Fluid> getFluidTicks() {
      throw new UnsupportedOperationException();
   }

   @NotNull
   public LevelData getLevelData() {
      throw new UnsupportedOperationException();
   }

   @NotNull
   public DifficultyInstance getCurrentDifficultyAt(BlockPos blockPos) {
      return new DifficultyInstance(Difficulty.NORMAL, 0L, 0L, 0.0F);
   }

   @Nullable
   public MinecraftServer getServer() {
      return null;
   }

   @NotNull
   public ChunkSource getChunkSource() {
      return this.emptyChunkSource;
   }

   @NotNull
   public RandomSource getRandom() {
      return this.randomSource;
   }

   public void playSound(@Nullable Player player, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
   }

   public void addParticle(ParticleOptions particleOptions, double d, double e, double f, double g, double h, double i) {
   }

   public void levelEvent(@Nullable Player player, int i, BlockPos blockPos, int j) {
   }

   public void gameEvent(Holder<GameEvent> holder, Vec3 vec3, Context context) {
   }

   public WorldBorder getWorldBorder() {
      throw new UnsupportedOperationException();
   }

   public List<Entity> getEntities(@Nullable Entity entity, AABB aABB, Predicate<? super Entity> predicate) {
      return List.of();
   }

   public <T extends Entity> List<T> getEntities(EntityTypeTest<Entity, T> entityTypeTest, AABB aABB, Predicate<? super T> predicate) {
      return List.of();
   }

   public List<? extends Player> players() {
      return List.of();
   }

   public boolean isStateAtPosition(BlockPos blockPos, Predicate<BlockState> predicate) {
      return predicate.test(this.getBlockState(blockPos));
   }

   public boolean isFluidAtPosition(BlockPos blockPos, Predicate<FluidState> predicate) {
      return predicate.test(this.getFluidState(blockPos));
   }

   public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
      this.addBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), blockState);
      return true;
   }

   public boolean removeBlock(BlockPos blockPos, boolean bl) {
      this.addBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), AIR);
      return true;
   }

   public boolean destroyBlock(BlockPos blockPos, boolean bl, @Nullable Entity entity, int i) {
      this.addBlock(blockPos.getX(), blockPos.getY(), blockPos.getZ(), AIR);
      return true;
   }

   public void scheduleTick(BlockPos blockPos, Block block, int i) {
   }

   public void scheduleTick(BlockPos blockPos, Fluid fluid, int i) {
   }

   public void scheduleTick(BlockPos blockPos, Block block, int i, TickPriority tickPriority) {
   }

   public void scheduleTick(BlockPos blockPos, Fluid fluid, int i, TickPriority tickPriority) {
   }

   private static final class ChunkData extends IntrusiveLinkedElement<ChunkedBlockRegion.ChunkData> {
      AxiomDrawBuffer block = null;
      AxiomDrawBuffer outline = null;
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

   private record CleanState(ChunkedBlockRegion.RenderData renderData) implements Runnable {
      @Override
      public void run() {
         ObjectIterator var1 = this.renderData.chunkData.values().iterator();

         while (var1.hasNext()) {
            ChunkedBlockRegion.ChunkData chunkDatum = (ChunkedBlockRegion.ChunkData)var1.next();
            if (chunkDatum.block != null) {
               EditorUI.deferredClose(chunkDatum.block);
            }

            if (chunkDatum.outline != null) {
               EditorUI.deferredClose(chunkDatum.outline);
            }
         }

         this.renderData.chunkData.clear();
         this.renderData.tessellatedBlockCache.close();
         this.renderData.outlineProvider.close();
      }
   }

   private record RenderData(
      Long2ObjectMap<ChunkedBlockRegion.ChunkData> chunkData, TessellatedBlockCache tessellatedBlockCache, VertexConsumerProvider outlineProvider
   ) {
   }
}
