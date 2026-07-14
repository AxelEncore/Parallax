package com.moulberry.axiom.render.annotations;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtils;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.annotations.data.LinesOutlineAnnotationData;
import com.moulberry.axiom.annotations.data.OutlineAnnotationData;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomBufferUsage;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ShaderManager;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiom.utils.RenderHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes.DoubleLineConsumer;
import org.jetbrains.annotations.Nullable;

public class OutlineAnnotation implements Annotation {
   private final OutlineAnnotationData data;
   private AxiomDrawBuffer vertexBuffer = null;
   private final SectionPos minSectionPos;
   private final SectionPos maxSectionPos;
   private final BlockPos center;
   private boolean empty = false;
   private boolean fullyBuilt = false;
   private static final int SHADE_X = 178;
   private static final int SHADE_PLUS_Y = 255;
   private static final int SHADE_MINUS_Y = 153;
   private static final int SHADE_Z = 222;
   private static final int DIRECTION_MINUS_X = 1 << Direction.WEST.get3DDataValue();
   private static final int DIRECTION_PLUS_X = 1 << Direction.EAST.get3DDataValue();
   private static final int DIRECTION_MINUS_Y = 1 << Direction.DOWN.get3DDataValue();
   private static final int DIRECTION_PLUS_Y = 1 << Direction.UP.get3DDataValue();
   private static final int DIRECTION_MINUS_Z = 1 << Direction.NORTH.get3DDataValue();
   private static final int DIRECTION_PLUS_Z = 1 << Direction.SOUTH.get3DDataValue();
   private static final Direction[] DIRECTIONS = Direction.values();

   public OutlineAnnotation(OutlineAnnotationData data) {
      this.data = data;
      IntWrapper minSectionX = new IntWrapper(Integer.MAX_VALUE);
      IntWrapper minSectionY = new IntWrapper(Integer.MAX_VALUE);
      IntWrapper minSectionZ = new IntWrapper(Integer.MAX_VALUE);
      IntWrapper maxSectionX = new IntWrapper(Integer.MIN_VALUE);
      IntWrapper maxSectionY = new IntWrapper(Integer.MIN_VALUE);
      IntWrapper maxSectionZ = new IntWrapper(Integer.MIN_VALUE);
      this.data.iteratePositions((x, y, z) -> {
         minSectionX.value = Math.min(minSectionX.value, x >> 4);
         minSectionY.value = Math.min(minSectionY.value, y >> 4);
         minSectionZ.value = Math.min(minSectionZ.value, z >> 4);
         maxSectionX.value = Math.max(maxSectionX.value, x >> 4);
         maxSectionY.value = Math.max(maxSectionY.value, y >> 4);
         maxSectionZ.value = Math.max(maxSectionZ.value, z >> 4);
      });
      this.minSectionPos = SectionPos.of(minSectionX.value, minSectionY.value, minSectionZ.value);
      this.maxSectionPos = SectionPos.of(maxSectionX.value, maxSectionY.value, maxSectionZ.value);
      this.center = new BlockPos(
         minSectionX.value * 8 + maxSectionX.value * 8 + 8,
         minSectionY.value * 8 + maxSectionY.value * 8 + 8,
         minSectionZ.value * 8 + maxSectionZ.value * 8 + 8
      );
   }

   @Override
   public AnnotationData getData() {
      return this.data;
   }

   @Override
   public SectionPos getMinSectionY() {
      return this.minSectionPos;
   }

   @Override
   public SectionPos getMaxSection() {
      return this.maxSectionPos;
   }

   @Nullable
   @Override
   public Gizmo getGizmo() {
      return null;
   }

   @Override
   public boolean renderPost() {
      return true;
   }

   @Override
   public void render(AxiomWorldRenderContext rc, UUID uuid, RenderTarget renderTarget) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null && !this.empty) {
         if (this.vertexBuffer == null) {
            VertexConsumerProvider provider = VertexConsumerProvider.shared();
            BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            this.fullyBuilt = build(level, bufferBuilder, this.data, this.center.multiply(-1));
            MeshData meshData = bufferBuilder.build();
            if (meshData == null) {
               this.empty = true;
               return;
            }

            this.vertexBuffer = new AxiomDrawBuffer(AxiomBufferUsage.STATIC_WRITE);
            this.vertexBuffer.upload(meshData);
         }

         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(this.center.getX() - rc.x(), this.center.getY() - rc.y(), this.center.getZ() - rc.z());
         RenderHelper.pushModelViewMatrix(matrices.last().pose());
         int colour = this.data.getColour();
         float red = (colour >> 16 & 0xFF) / 255.0F;
         float green = (colour >> 8 & 0xFF) / 255.0F;
         float blue = (colour & 0xFF) / 255.0F;
         AxiomRenderer.setShaderColour(red, green, blue, 1.0F);
         RenderTarget selectionTarget = ShaderManager.getSelectionOutlineTarget(true);
         AxiomRenderer.renderPipeline(AxiomRenderPipelines.OUTLINE_WITH_DEPTH, selectionTarget, this.vertexBuffer);
         AxiomRenderer.setShaderColour(red, green, blue, 0.25F);
         AxiomRenderPipelines.POSITION_COLOR_POLYGON_OFFSET.render(renderTarget, this.vertexBuffer);
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
         RenderHelper.popModelViewStack();
         matrices.popPose();
      }
   }

   public static void drawStatic(AxiomWorldRenderContext rc, ClientLevel level, OutlineAnnotationData data) {
      BlockPos center = rc.blockPosition();
      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      matrices.translate(center.getX() - rc.x(), center.getY() - rc.y(), center.getZ() - rc.z());
      RenderHelper.pushModelViewMatrix(matrices.last().pose());
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      build(level, bufferBuilder, data, center.multiply(-1));
      MeshData meshData = bufferBuilder.build();
      if (meshData != null) {
         int colour = data.getColour();
         float red = (colour >> 16 & 0xFF) / 255.0F;
         float green = (colour >> 8 & 0xFF) / 255.0F;
         float blue = (colour & 0xFF) / 255.0F;
         AxiomRenderer.setShaderColour(red, green, blue, 1.0F);
         RenderTarget selectionTarget = ShaderManager.getSelectionOutlineTarget(true);
         AxiomRenderer.renderPipeline(AxiomRenderPipelines.OUTLINE_WITH_DEPTH, selectionTarget, meshData, false);
         AxiomRenderer.setShaderColour(red, green, blue, 0.25F);
         AxiomRenderPipelines.POSITION_COLOR_POLYGON_OFFSET.render(meshData);
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      }

      RenderHelper.popModelViewStack();
      matrices.popPose();
   }

   private static void buildBox(BufferBuilder bufferBuilder, int allowedDirections, float x1, float y1, float z1, float x2, float y2, float z2) {
      if ((allowedDirections & DIRECTION_MINUS_X) != 0) {
         bufferBuilder.addVertex(x1, y1, z1).setColor(178, 178, 178, 255);
         bufferBuilder.addVertex(x1, y1, z2).setColor(178, 178, 178, 255);
         bufferBuilder.addVertex(x1, y2, z2).setColor(178, 178, 178, 255);
         bufferBuilder.addVertex(x1, y2, z1).setColor(178, 178, 178, 255);
      }

      if ((allowedDirections & DIRECTION_PLUS_X) != 0) {
         bufferBuilder.addVertex(x2, y2, z1).setColor(178, 178, 178, 255);
         bufferBuilder.addVertex(x2, y2, z2).setColor(178, 178, 178, 255);
         bufferBuilder.addVertex(x2, y1, z2).setColor(178, 178, 178, 255);
         bufferBuilder.addVertex(x2, y1, z1).setColor(178, 178, 178, 255);
      }

      if ((allowedDirections & DIRECTION_PLUS_Y) != 0) {
         bufferBuilder.addVertex(x1, y2, z1).setColor(255, 255, 255, 255);
         bufferBuilder.addVertex(x1, y2, z2).setColor(255, 255, 255, 255);
         bufferBuilder.addVertex(x2, y2, z2).setColor(255, 255, 255, 255);
         bufferBuilder.addVertex(x2, y2, z1).setColor(255, 255, 255, 255);
      }

      if ((allowedDirections & DIRECTION_MINUS_Y) != 0) {
         bufferBuilder.addVertex(x2, y1, z1).setColor(153, 153, 153, 255);
         bufferBuilder.addVertex(x2, y1, z2).setColor(153, 153, 153, 255);
         bufferBuilder.addVertex(x1, y1, z2).setColor(153, 153, 153, 255);
         bufferBuilder.addVertex(x1, y1, z1).setColor(153, 153, 153, 255);
      }

      if ((allowedDirections & DIRECTION_PLUS_Z) != 0) {
         bufferBuilder.addVertex(x1, y1, z2).setColor(222, 222, 222, 255);
         bufferBuilder.addVertex(x2, y1, z2).setColor(222, 222, 222, 255);
         bufferBuilder.addVertex(x2, y2, z2).setColor(222, 222, 222, 255);
         bufferBuilder.addVertex(x1, y2, z2).setColor(222, 222, 222, 255);
      }

      if ((allowedDirections & DIRECTION_MINUS_Z) != 0) {
         bufferBuilder.addVertex(x1, y2, z1).setColor(222, 222, 222, 255);
         bufferBuilder.addVertex(x2, y2, z1).setColor(222, 222, 222, 255);
         bufferBuilder.addVertex(x2, y1, z1).setColor(222, 222, 222, 255);
         bufferBuilder.addVertex(x1, y1, z1).setColor(222, 222, 222, 255);
      }
   }

   private static boolean build(ClientLevel level, BufferBuilder bufferBuilder, OutlineAnnotationData data, BlockPos offset) {
      BooleanWrapper fullyBuilt = new BooleanWrapper(true);
      MutableBlockPos position = new MutableBlockPos();
      MutableBlockPos neighborPosition = new MutableBlockPos();
      OutlineAnnotation.BoxConsumer boxConsumer = new OutlineAnnotation.BoxConsumer();
      boxConsumer.bufferBuilder = bufferBuilder;
      boxConsumer.offsetX = offset.getX();
      boxConsumer.offsetY = offset.getY();
      boxConsumer.offsetZ = offset.getZ();
      List<OutlineAnnotation.BlockPosWithState> lastPositions = new ArrayList<>();
      PositionSet visited = new PositionSet();
      boolean allowAir = data instanceof LinesOutlineAnnotationData;
      data.iteratePositions(
         (x, y, z) -> {
            BlockState newBlockState = level.getBlockState(position.set(x, y, z));
            VoxelShape newVoxelShape = newBlockState.getShape(level, position);
            OutlineAnnotation.BlockPosWithState newState = new OutlineAnnotation.BlockPosWithState(
               x,
               y,
               z,
               newBlockState,
               newVoxelShape,
               newVoxelShape.isEmpty() || newVoxelShape == Shapes.empty() || newBlockState.getRenderShape() == RenderShape.INVISIBLE
            );
            if (lastPositions.isEmpty()) {
               lastPositions.add(newState);
            } else if (lastPositions.size() == 1) {
               OutlineAnnotation.BlockPosWithState lastxx = lastPositions.get(0);
               if (lastxx.blockState.getBlock() == Blocks.VOID_AIR) {
                  fullyBuilt.value = false;
               }

               if (visited.add(lastxx.x, lastxx.y, lastxx.z)) {
                  Direction nextDirection;
                  if (lastxx.isEmptyVoxelShape) {
                     nextDirection = PositionUtils.directionFromDelta(x - lastxx.x, y - lastxx.y, z - lastxx.z);
                  } else {
                     nextDirection = allowAir && newState.isEmptyVoxelShape ? PositionUtils.directionFromDelta(x - lastxx.x, y - lastxx.y, z - lastxx.z) : null;
                  }

                  if (allowAir || !lastxx.blockState.isAir()) {
                     lastxx.renderBlockFace(level, position, neighborPosition, boxConsumer, null, nextDirection);
                  }
               }

               lastPositions.add(newState);
            } else {
               OutlineAnnotation.BlockPosWithState lastLastx = lastPositions.get(0);
               OutlineAnnotation.BlockPosWithState lastx = lastPositions.get(1);
               lastPositions.remove(0);
               lastPositions.add(newState);
               if (lastx.blockState.getBlock() == Blocks.VOID_AIR) {
                  fullyBuilt.value = false;
                  return;
               }

               if (visited.add(lastx.x, lastx.y, lastx.z)) {
                  Direction lastDirectionx;
                  Direction nextDirectionx;
                  if (lastx.isEmptyVoxelShape) {
                     lastDirectionx = PositionUtils.directionFromDelta(lastLastx.x - lastx.x, lastLastx.y - lastx.y, lastLastx.z - lastx.z);
                     nextDirectionx = PositionUtils.directionFromDelta(x - lastx.x, y - lastx.y, z - lastx.z);
                  } else {
                     lastDirectionx = allowAir && lastLastx.isEmptyVoxelShape
                        ? PositionUtils.directionFromDelta(lastLastx.x - lastx.x, lastLastx.y - lastx.y, lastLastx.z - lastx.z)
                        : null;
                     nextDirectionx = allowAir && newState.isEmptyVoxelShape ? PositionUtils.directionFromDelta(x - lastx.x, y - lastx.y, z - lastx.z) : null;
                  }

                  if (allowAir || !lastx.blockState.isAir()) {
                     lastx.renderBlockFace(level, position, neighborPosition, boxConsumer, lastDirectionx, nextDirectionx);
                  }
               }
            }
         }
      );
      if (lastPositions.size() == 1) {
         OutlineAnnotation.BlockPosWithState last = lastPositions.get(0);
         if (last.blockState.getBlock() == Blocks.VOID_AIR) {
            fullyBuilt.value = false;
         }

         if (visited.add(last.x, last.y, last.z) && (allowAir || !last.blockState.isAir())) {
            last.renderBlockFace(level, position, neighborPosition, boxConsumer, null, null);
         }
      } else if (lastPositions.size() == 2) {
         OutlineAnnotation.BlockPosWithState lastLast = lastPositions.get(0);
         OutlineAnnotation.BlockPosWithState lastx = lastPositions.get(1);
         if (lastx.blockState.getBlock() == Blocks.VOID_AIR) {
            fullyBuilt.value = false;
         }

         if (visited.add(lastx.x, lastx.y, lastx.z)) {
            Direction lastDirection;
            if (lastx.isEmptyVoxelShape) {
               lastDirection = PositionUtils.directionFromDelta(lastLast.x - lastx.x, lastLast.y - lastx.y, lastLast.z - lastx.z);
            } else {
               lastDirection = allowAir && lastLast.isEmptyVoxelShape
                  ? PositionUtils.directionFromDelta(lastLast.x - lastx.x, lastLast.y - lastx.y, lastLast.z - lastx.z)
                  : null;
            }

            if (allowAir || !lastx.blockState.isAir()) {
               lastx.renderBlockFace(level, position, neighborPosition, boxConsumer, lastDirection, null);
            }
         }
      }

      return fullyBuilt.value;
   }

   @Override
   public void sectionChanged() {
      if (!this.fullyBuilt) {
         this.empty = false;
         this.close();
      }
   }

   @Override
   public void close() {
      if (this.vertexBuffer != null) {
         this.vertexBuffer.close();
         this.vertexBuffer = null;
      }
   }

   private record BlockPosWithState(int x, int y, int z, BlockState blockState, VoxelShape shape, boolean isEmptyVoxelShape) {
      private void renderBlockFace(
         ClientLevel level,
         MutableBlockPos position,
         MutableBlockPos neighborPosition,
         OutlineAnnotation.BoxConsumer boxConsumer,
         Direction lastDirection,
         Direction nextDirection
      ) {
         if (this.isEmptyVoxelShape) {
            boxConsumer.allowedDirections = 63;
            if (lastDirection != null) {
               boxConsumer.allowedDirections = boxConsumer.allowedDirections & ~(1 << lastDirection.get3DDataValue());
            }

            if (nextDirection != null) {
               boxConsumer.allowedDirections = boxConsumer.allowedDirections & ~(1 << nextDirection.get3DDataValue());
            }

            boxConsumer.x = this.x;
            boxConsumer.y = this.y;
            boxConsumer.z = this.z;
            Shapes.block().forAllBoxes(boxConsumer);
         } else {
            boxConsumer.allowedDirections = 0;
            position.set(this.x, this.y, this.z);

            for (Direction direction : OutlineAnnotation.DIRECTIONS) {
               neighborPosition.setWithOffset(position, direction);
               if (direction != lastDirection
                  && direction != nextDirection
                  && VersionUtils.shouldRenderFace(this.blockState, level, position, direction, neighborPosition)) {
                  boxConsumer.allowedDirections = boxConsumer.allowedDirections | 1 << direction.get3DDataValue();
               }
            }

            if (boxConsumer.allowedDirections != 0) {
               boxConsumer.x = this.x;
               boxConsumer.y = this.y;
               boxConsumer.z = this.z;
               this.shape.forAllBoxes(boxConsumer);
            }
         }
      }
   }

   private static class BoxConsumer implements DoubleLineConsumer {
      public BufferBuilder bufferBuilder;
      public int x;
      public int y;
      public int z;
      public int offsetX;
      public int offsetY;
      public int offsetZ;
      public int allowedDirections = 0;

      public void consume(double x1, double y1, double z1, double x2, double y2, double z2) {
         if (x1 != 0.0) {
            this.allowedDirections = this.allowedDirections | OutlineAnnotation.DIRECTION_MINUS_X;
         }

         if (x2 != 1.0) {
            this.allowedDirections = this.allowedDirections | OutlineAnnotation.DIRECTION_PLUS_X;
         }

         if (y1 != 0.0) {
            this.allowedDirections = this.allowedDirections | OutlineAnnotation.DIRECTION_MINUS_Y;
         }

         if (y2 != 1.0) {
            this.allowedDirections = this.allowedDirections | OutlineAnnotation.DIRECTION_PLUS_Y;
         }

         if (z1 != 0.0) {
            this.allowedDirections = this.allowedDirections | OutlineAnnotation.DIRECTION_MINUS_Z;
         }

         if (z2 != 1.0) {
            this.allowedDirections = this.allowedDirections | OutlineAnnotation.DIRECTION_PLUS_Z;
         }

         int x = this.x + this.offsetX;
         int y = this.y + this.offsetY;
         int z = this.z + this.offsetZ;
         OutlineAnnotation.buildBox(
            this.bufferBuilder, this.allowedDirections, x + (float)x1, y + (float)y1, z + (float)z1, x + (float)x2, y + (float)y2, z + (float)z2
         );
      }
   }
}
