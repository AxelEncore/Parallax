package com.moulberry.axiom.tools.path;

import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.noise.WhiteNoise;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.BlockHelper;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector4d;

public interface PathRasterizer {
   static int[] createOffsetArray(int radius) {
      IntList offsetList = new IntArrayList();
      int radiusSq = radius * radius + radius;

      for (int xo = -radius; xo <= radius; xo++) {
         for (int yo = -radius; yo <= radius; yo++) {
            for (int zo = -radius; zo <= radius; zo++) {
               if ((xo != 0 || yo != 0 || zo != 0) && xo * xo + yo * yo + zo * zo <= radiusSq) {
                  offsetList.add(xo);
                  offsetList.add(yo);
                  offsetList.add(zo);
               }
            }
         }
      }

      return offsetList.toIntArray();
   }

   void rasterize(ChunkedBlockRegion var1, MaskElement var2, MaskContext var3, Vector3d var4, Vector3d var5, int var6, float var7, float var8);

   default void finish(ChunkedBlockRegion chunkedBlockRegion, MaskElement destinationMask, MaskContext maskContext) {
   }

   public record ConstantRadiusConstantBlock(int[] offsetArray, float realRadiusSquared, BlockState constantBlock) implements PathRasterizer {
      public ConstantRadiusConstantBlock(float radius, BlockState constantBlock) {
         this(PathRasterizer.createOffsetArray((int)Math.ceil(radius) + 1), radius * radius + radius, constantBlock);
      }

      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Rasterization3D.ddaPartial(from, to, (x, y, z, f) -> {
            double targetX = from.x * (1.0F - f) + to.x * f;
            double targetY = from.y * (1.0F - f) + to.y * f;
            double targetZ = from.z * (1.0F - f) + to.z * f;
            if (destinationMask.test(maskContext.reset(), x, y, z)) {
               chunkedBlockRegion.addBlockWithoutDirty(x, y, z, this.constantBlock);
            }

            for (int i1 = 0; i1 < this.offsetArray.length; i1 += 3) {
               int xo = this.offsetArray[i1];
               int yo = this.offsetArray[i1 + 1];
               int zo = this.offsetArray[i1 + 2];
               double dx = x + xo + 0.5 - targetX;
               double dy = y + yo + 0.5 - targetY;
               double dz = z + zo + 0.5 - targetZ;
               double realDistanceSq = dx * dx + dy * dy + dz * dz;
               if (realDistanceSq <= this.realRadiusSquared && destinationMask.test(maskContext.reset(), x + xo, y + yo, z + zo)) {
                  chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, this.constantBlock);
               }
            }
         });
      }
   }

   public record ConstantRadiusDynamicBlock(
      FloatUnaryOperator[] easings, int[] offsetArray, float realRadiusSquared, BlockState[] blocks, WhiteNoise whiteNoise, Position2FloatMap closestMap
   ) implements PathRasterizer {
      public ConstantRadiusDynamicBlock(FloatUnaryOperator[] easings, float radius, BlockState[] blocks, WhiteNoise whiteNoise, Position2FloatMap closestMap) {
         this(easings, PathRasterizer.createOffsetArray((int)Math.ceil(radius) + 1), radius * radius + radius, blocks, whiteNoise, closestMap);
      }

      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Rasterization3D.ddaPartial(
            from,
            to,
            (x, y, z, f) -> {
               float partial = minPartial + partialLength * f;
               partial = this.easings[index].apply(partial);
               BlockState fromState = this.blocks[Math.min(this.blocks.length - 1, index)];
               BlockState toState = this.blocks[Math.min(this.blocks.length - 1, index + 1)];
               if (destinationMask.test(maskContext.reset(), x, y, z)) {
                  if (this.whiteNoise.evaluate(x, y, z) > partial) {
                     chunkedBlockRegion.addBlockWithoutDirty(x, y, z, fromState);
                  } else {
                     chunkedBlockRegion.addBlockWithoutDirty(x, y, z, toState);
                  }
               }

               double targetX = from.x * (1.0F - f) + to.x * f;
               double targetY = from.y * (1.0F - f) + to.y * f;
               double targetZ = from.z * (1.0F - f) + to.z * f;

               for (int i1 = 0; i1 < this.offsetArray.length; i1 += 3) {
                  int xo = this.offsetArray[i1];
                  int yo = this.offsetArray[i1 + 1];
                  int zo = this.offsetArray[i1 + 2];
                  double dx = x + xo + 0.5 - targetX;
                  double dy = y + yo + 0.5 - targetY;
                  double dz = z + zo + 0.5 - targetZ;
                  double realDistanceSq = dx * dx + dy * dy + dz * dz;
                  if (realDistanceSq <= this.realRadiusSquared
                     && destinationMask.test(maskContext.reset(), x + xo, y + yo, z + zo)
                     && this.closestMap.min(x + xo, y + yo, z + zo, (float)realDistanceSq)) {
                     if (this.whiteNoise.evaluate(x + xo, y + yo, z + zo) > partial) {
                        chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, fromState);
                     } else {
                        chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, toState);
                     }
                  }
               }
            }
         );
      }
   }

   public record Custom(
      FloatUnaryOperator[] easings,
      float[] nodeAngles,
      float[] lineAngles,
      double[] accumulatedDistance,
      Position2FloatMap minDistance,
      ChunkedBlockRegion sourceRegion
   ) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Vector3d leftBuffer = new Vector3d();
         Vector3d rightBuffer = new Vector3d();
         double distance = from.distance(to);
         float divisions = (float)Math.ceil(distance) * 32.0F;

         for (int i = 0; i <= (int)divisions; i++) {
            float f = i / divisions;
            float partial = minPartial + partialLength * f;
            partial = this.easings[index].apply(partial);
            float angleLeft;
            float angleRight;
            if (partial < 0.5) {
               angleLeft = this.nodeAngles[index];
               angleRight = this.lineAngles[index];
               partial *= 2.0F;
            } else {
               angleLeft = this.lineAngles[index];
               angleRight = this.nodeAngles[index + 1];
               partial = (partial - 0.5F) * 2.0F;
            }

            double delta = angleRight - angleLeft;
            delta %= Math.PI * 2;
            if (delta < -Math.PI) {
               delta += Math.PI * 2;
            }

            if (delta > Math.PI) {
               delta -= Math.PI * 2;
            }

            double realAngle = angleLeft + delta * partial + (Math.PI / 2);
            double targetX = from.x * (1.0F - f) + to.x * f;
            double targetY = from.y * (1.0F - f) + to.y * f;
            double targetZ = from.z * (1.0F - f) + to.z * f;
            BlockPos min = this.sourceRegion.min();
            BlockPos max = this.sourceRegion.max();
            int sizeX = max.getX() - min.getX() + 1;
            int sizeY = max.getY() - min.getY() + 1;
            int sizeZ = max.getZ() - min.getZ() + 1;
            int currentDistance = (int)Math.round(this.accumulatedDistance[0] + distance * f);
            int blockZ = min.getZ() + currentDistance % sizeZ;
            int halfX = (sizeX - 1) / 2;
            int halfXUp = (sizeX - 1) % 2 == 1 ? halfX + 1 : halfX;
            double sideXLeft = Math.sin(realAngle) * halfX;
            double sideZLeft = Math.cos(realAngle) * halfX;
            double sideXRight = Math.sin(realAngle) * halfXUp;
            double sideZRight = Math.cos(realAngle) * halfXUp;
            Rotation blockStateRotation = BlockHelper.rotationFromRadians((Math.PI / 2) - realAngle);
            leftBuffer.set(targetX - sideXLeft, targetY, targetZ - sideZLeft);
            rightBuffer.set(targetX + sideXRight, targetY, targetZ + sideZRight);
            Rasterization3D.ddaPartial(leftBuffer, rightBuffer, (x1, y1, z1, amount) -> {
               Vector3d actualPosition = leftBuffer.lerp(rightBuffer, amount, new Vector3d());
               double errorDistanceSq = actualPosition.distanceSquared(x1 + 0.5, y1 + 0.5, z1 + 0.5);

               for (int y = 0; y < sizeY; y++) {
                  if (destinationMask.test(maskContext.reset(), x1, y1 + y, z1) && this.minDistance.min(x1, y1 + y, z1, (float)errorDistanceSq)) {
                     int blockX = min.getX() + Math.round((sizeX - 1) * amount);
                     int blockY = min.getY() + y;
                     BlockState blockState = this.sourceRegion.getBlockStateOrNull(blockX, blockY, blockZ);
                     if (blockState == null) {
                        chunkedBlockRegion.unsafeRemoveBlockWithoutDirty(x1, y1 + y, z1);
                     } else {
                        chunkedBlockRegion.addBlockWithoutDirty(x1, y1 + y, z1, BlockHelper.rotateY(blockState, blockStateRotation));
                     }
                  }
               }
            });
         }

         this.accumulatedDistance[0] = this.accumulatedDistance[0] + distance;
      }
   }

   public record DynamicRadiusConstantBlock(FloatUnaryOperator[] easings, float[] radii, BlockState constantBlock) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Rasterization3D.ddaPartial(from, to, (x, y, z, f) -> {
            float partial = minPartial + partialLength * f;
            partial = this.easings[index].apply(partial);
            float realRadius = this.radii[index] * (1.0F - partial) + this.radii[index + 1] * partial;
            float realRadiusSquared = realRadius * realRadius + realRadius;
            int bigRadius = (int)realRadius + 1;
            int bigRadiusSquared = bigRadius * bigRadius + bigRadius;
            double targetX = from.x * (1.0F - f) + to.x * f;
            double targetY = from.y * (1.0F - f) + to.y * f;
            double targetZ = from.z * (1.0F - f) + to.z * f;

            for (int xo = -bigRadius; xo <= bigRadius; xo++) {
               for (int yo = -bigRadius; yo <= bigRadius; yo++) {
                  for (int zo = -bigRadius; zo <= bigRadius; zo++) {
                     if (xo == 0 && yo == 0 && zo == 0) {
                        if (destinationMask.test(maskContext.reset(), x, y, z)) {
                           chunkedBlockRegion.addBlockWithoutDirty(x, y, z, this.constantBlock);
                        }
                     } else if (xo * xo + yo * yo + zo * zo <= bigRadiusSquared) {
                        double dx = x + xo + 0.5 - targetX;
                        double dy = y + yo + 0.5 - targetY;
                        double dz = z + zo + 0.5 - targetZ;
                        double realDistanceSq = dx * dx + dy * dy + dz * dz;
                        if (realDistanceSq <= realRadiusSquared && destinationMask.test(maskContext.reset(), x + xo, y + yo, z + zo)) {
                           chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, this.constantBlock);
                        }
                     }
                  }
               }
            }
         });
      }
   }

   public record DynamicRadiusDynamicBlock(
      FloatUnaryOperator[] easings, float[] radii, BlockState[] blocks, WhiteNoise whiteNoise, Position2FloatMap closestMap
   ) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Rasterization3D.ddaPartial(
            from,
            to,
            (x, y, z, f) -> {
               float partial = minPartial + partialLength * f;
               partial = this.easings[index].apply(partial);
               BlockState fromState = this.blocks[Math.min(this.blocks.length - 1, index)];
               BlockState toState = this.blocks[Math.min(this.blocks.length - 1, index + 1)];
               float realRadius = this.radii[index] * (1.0F - partial) + this.radii[index + 1] * partial;
               float realRadiusSquared = realRadius * realRadius + realRadius;
               int bigRadius = (int)realRadius + 1;
               int bigRadiusSquared = bigRadius * bigRadius + bigRadius;
               double targetX = from.x * (1.0F - f) + to.x * f;
               double targetY = from.y * (1.0F - f) + to.y * f;
               double targetZ = from.z * (1.0F - f) + to.z * f;

               for (int xo = -bigRadius; xo <= bigRadius; xo++) {
                  for (int yo = -bigRadius; yo <= bigRadius; yo++) {
                     for (int zo = -bigRadius; zo <= bigRadius; zo++) {
                        if (xo == 0 && yo == 0 && zo == 0) {
                           if (destinationMask.test(maskContext.reset(), x, y, z)) {
                              if (this.whiteNoise.evaluate(x, y, z) > partial) {
                                 chunkedBlockRegion.addBlockWithoutDirty(x, y, z, fromState);
                              } else {
                                 chunkedBlockRegion.addBlockWithoutDirty(x, y, z, toState);
                              }
                           }
                        } else if (xo * xo + yo * yo + zo * zo <= bigRadiusSquared) {
                           double dx = x + xo + 0.5 - targetX;
                           double dy = y + yo + 0.5 - targetY;
                           double dz = z + zo + 0.5 - targetZ;
                           double realDistanceSq = dx * dx + dy * dy + dz * dz;
                           if (realDistanceSq <= realRadiusSquared
                              && destinationMask.test(maskContext.reset(), x + xo, y + yo, z + zo)
                              && this.closestMap.min(x + xo, y + yo, z + zo, (float)realDistanceSq)) {
                              if (this.whiteNoise.evaluate(x + xo, y + yo, z + zo) > partial) {
                                 chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, fromState);
                              } else {
                                 chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, toState);
                              }
                           }
                        }
                     }
                  }
               }
            }
         );
      }
   }

   public record FlatConstantRadiusConstantBlock(
      FloatUnaryOperator[] easings, float[] nodeAngles, float[] lineAngles, float distance, int depth, BlockState constantBlock
   ) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Vector3d leftBuffer = new Vector3d();
         Vector3d rightBuffer = new Vector3d();
         float divisions = (float)Math.ceil(from.distance(to)) * 32.0F;

         for (int i = 0; i <= (int)divisions; i++) {
            float f = i / divisions;
            float partial = minPartial + partialLength * f;
            partial = this.easings[index].apply(partial);
            float angleLeft;
            float angleRight;
            if (partial < 0.5) {
               angleLeft = this.nodeAngles[index];
               angleRight = this.lineAngles[index];
               partial *= 2.0F;
            } else {
               angleLeft = this.lineAngles[index];
               angleRight = this.nodeAngles[index + 1];
               partial = (partial - 0.5F) * 2.0F;
            }

            float delta = angleRight - angleLeft;
            delta = (float)(delta % (Math.PI * 2));
            if (delta < -Math.PI) {
               delta = (float)(delta + (Math.PI * 2));
            }

            if (delta > Math.PI) {
               delta = (float)(delta - (Math.PI * 2));
            }

            float realAngle = angleLeft + delta * partial + (float) (Math.PI / 2);
            double targetX = from.x * (1.0F - f) + to.x * f;
            double targetY = from.y * (1.0F - f) + to.y * f;
            double targetZ = from.z * (1.0F - f) + to.z * f;
            float sideX = (float)Math.sin(realAngle) * this.distance;
            float sideZ = (float)Math.cos(realAngle) * this.distance;
            leftBuffer.set(targetX - sideX, targetY, targetZ - sideZ);
            rightBuffer.set(targetX + sideX, targetY, targetZ + sideZ);
            Rasterization3D.dda(leftBuffer, rightBuffer, (x1, y1, z1) -> {
               for (int d = Math.min(0, this.depth); d <= Math.max(0, this.depth); d++) {
                  if (destinationMask.test(maskContext.reset(), x1, y1 - d, z1)) {
                     chunkedBlockRegion.addBlockWithoutDirty(x1, y1 - d, z1, this.constantBlock);
                  }
               }
            });
         }
      }
   }

   public record FlatConstantRadiusDynamicBlock(
      FloatUnaryOperator[] easings,
      float[] nodeAngles,
      float[] lineAngles,
      float distance,
      int depth,
      BlockState[] blocks,
      WhiteNoise whiteNoise,
      Position2FloatMap closestMap
   ) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Vector3d leftBuffer = new Vector3d();
         Vector3d rightBuffer = new Vector3d();
         float divisions = (float)Math.ceil(from.distance(to)) * 32.0F;
         BlockState fromState = this.blocks[Math.min(this.blocks.length - 1, index)];
         BlockState toState = this.blocks[Math.min(this.blocks.length - 1, index + 1)];

         for (int i = 0; i <= (int)divisions; i++) {
            float f = i / divisions;
            float partial = this.easings[index].apply(minPartial + partialLength * f);
            float angleLeft;
            float angleRight;
            float newPartial;
            if (partial < 0.5) {
               angleLeft = this.nodeAngles[index];
               angleRight = this.lineAngles[index];
               newPartial = partial * 2.0F;
            } else {
               angleLeft = this.lineAngles[index];
               angleRight = this.nodeAngles[index + 1];
               newPartial = (partial - 0.5F) * 2.0F;
            }

            float delta = angleRight - angleLeft;
            delta = (float)(delta % (Math.PI * 2));
            if (delta < -Math.PI) {
               delta = (float)(delta + (Math.PI * 2));
            }

            if (delta > Math.PI) {
               delta = (float)(delta - (Math.PI * 2));
            }

            float realAngle = angleLeft + delta * newPartial + (float) (Math.PI / 2);
            double targetX = from.x * (1.0F - f) + to.x * f;
            double targetY = from.y * (1.0F - f) + to.y * f;
            double targetZ = from.z * (1.0F - f) + to.z * f;
            float sideX = (float)Math.sin(realAngle) * this.distance;
            float sideZ = (float)Math.cos(realAngle) * this.distance;
            leftBuffer.set(targetX - sideX, targetY, targetZ - sideZ);
            rightBuffer.set(targetX + sideX, targetY, targetZ + sideZ);
            Rasterization3D.dda(leftBuffer, rightBuffer, (x1, y1, z1) -> {
               for (int d = Math.min(0, this.depth); d <= Math.max(0, this.depth); d++) {
                  if (destinationMask.test(maskContext.reset(), x1, y1 - d, z1)) {
                     double dx = x1 + 0.5 - targetX;
                     double dy = y1 - d + 0.5 - targetY;
                     double dz = z1 + 0.5 - targetZ;
                     double realDistanceSq = dx * dx + dy * dy + dz * dz;
                     if (this.closestMap.min(x1, y1 - d, z1, (float)realDistanceSq)) {
                        if (this.whiteNoise.evaluate(x1, y1 - d, z1) > partial) {
                           chunkedBlockRegion.addBlockWithoutDirty(x1, y1 - d, z1, fromState);
                        } else {
                           chunkedBlockRegion.addBlockWithoutDirty(x1, y1 - d, z1, toState);
                        }
                     }
                  }
               }
            });
         }
      }
   }

   public record FlatDynamicRadiusConstantBlock(
      FloatUnaryOperator[] easings, float[] nodeAngles, float[] lineAngles, float[] radii, int depth, BlockState constantBlock
   ) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Vector3d leftBuffer = new Vector3d();
         Vector3d rightBuffer = new Vector3d();
         float divisions = (float)Math.ceil(from.distance(to)) * 32.0F;

         for (int i = 0; i <= (int)divisions; i++) {
            float f = i / divisions;
            float partial = minPartial + partialLength * f;
            partial = this.easings[index].apply(partial);
            float realRadius = this.radii[index] * (1.0F - partial) + this.radii[index + 1] * partial;
            float angleLeft;
            float angleRight;
            if (partial < 0.5) {
               angleLeft = this.nodeAngles[index];
               angleRight = this.lineAngles[index];
               partial *= 2.0F;
            } else {
               angleLeft = this.lineAngles[index];
               angleRight = this.nodeAngles[index + 1];
               partial = (partial - 0.5F) * 2.0F;
            }

            float delta = angleRight - angleLeft;
            delta = (float)(delta % (Math.PI * 2));
            if (delta < -Math.PI) {
               delta = (float)(delta + (Math.PI * 2));
            }

            if (delta > Math.PI) {
               delta = (float)(delta - (Math.PI * 2));
            }

            float realAngle = angleLeft + delta * partial + (float) (Math.PI / 2);
            double targetX = from.x * (1.0F - f) + to.x * f;
            double targetY = from.y * (1.0F - f) + to.y * f;
            double targetZ = from.z * (1.0F - f) + to.z * f;
            float sideX = (float)Math.sin(realAngle) * realRadius;
            float sideZ = (float)Math.cos(realAngle) * realRadius;
            leftBuffer.set(targetX - sideX, targetY, targetZ - sideZ);
            rightBuffer.set(targetX + sideX, targetY, targetZ + sideZ);
            Rasterization3D.dda(leftBuffer, rightBuffer, (x1, y1, z1) -> {
               for (int d = Math.min(0, this.depth); d <= Math.max(0, this.depth); d++) {
                  if (destinationMask.test(maskContext.reset(), x1, y1 - d, z1)) {
                     chunkedBlockRegion.addBlockWithoutDirty(x1, y1 - d, z1, this.constantBlock);
                  }
               }
            });
         }
      }
   }

   public record FlatDynamicRadiusDynamicBlock(
      FloatUnaryOperator[] easings,
      float[] nodeAngles,
      float[] lineAngles,
      float[] radii,
      int depth,
      BlockState[] blocks,
      WhiteNoise whiteNoise,
      Position2FloatMap closestMap
   ) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Vector3d leftBuffer = new Vector3d();
         Vector3d rightBuffer = new Vector3d();
         float divisions = (float)Math.ceil(from.distance(to)) * 32.0F;
         BlockState fromState = this.blocks[Math.min(this.blocks.length - 1, index)];
         BlockState toState = this.blocks[Math.min(this.blocks.length - 1, index + 1)];

         for (int i = 0; i <= (int)divisions; i++) {
            float f = i / divisions;
            float partial = this.easings[index].apply(minPartial + partialLength * f);
            float realRadius = this.radii[index] * (1.0F - partial) + this.radii[index + 1] * partial;
            float angleLeft;
            float angleRight;
            float newPartial;
            if (partial < 0.5) {
               angleLeft = this.nodeAngles[index];
               angleRight = this.lineAngles[index];
               newPartial = partial * 2.0F;
            } else {
               angleLeft = this.lineAngles[index];
               angleRight = this.nodeAngles[index + 1];
               newPartial = (partial - 0.5F) * 2.0F;
            }

            float delta = angleRight - angleLeft;
            delta = (float)(delta % (Math.PI * 2));
            if (delta < -Math.PI) {
               delta = (float)(delta + (Math.PI * 2));
            }

            if (delta > Math.PI) {
               delta = (float)(delta - (Math.PI * 2));
            }

            float realAngle = angleLeft + delta * newPartial + (float) (Math.PI / 2);
            double targetX = from.x * (1.0F - f) + to.x * f;
            double targetY = from.y * (1.0F - f) + to.y * f;
            double targetZ = from.z * (1.0F - f) + to.z * f;
            float sideX = (float)Math.sin(realAngle) * realRadius;
            float sideZ = (float)Math.cos(realAngle) * realRadius;
            leftBuffer.set(targetX - sideX, targetY, targetZ - sideZ);
            rightBuffer.set(targetX + sideX, targetY, targetZ + sideZ);
            Rasterization3D.dda(leftBuffer, rightBuffer, (x1, y1, z1) -> {
               for (int d = Math.min(0, this.depth); d <= Math.max(0, this.depth); d++) {
                  if (destinationMask.test(maskContext.reset(), x1, y1 - d, z1)) {
                     double dx = x1 + 0.5 - targetX;
                     double dy = y1 - d + 0.5 - targetY;
                     double dz = z1 + 0.5 - targetZ;
                     double realDistanceSq = dx * dx + dy * dy + dz * dz;
                     if (this.closestMap.min(x1, y1 - d, z1, (float)realDistanceSq)) {
                        if (this.whiteNoise.evaluate(x1, y1 - d, z1) > partial) {
                           chunkedBlockRegion.addBlockWithoutDirty(x1, y1 - d, z1, fromState);
                        } else {
                           chunkedBlockRegion.addBlockWithoutDirty(x1, y1 - d, z1, toState);
                        }
                     }
                  }
               }
            });
         }
      }
   }

   public static final class Spike implements PathRasterizer {
      private final FloatUnaryOperator[] easings;
      private final float startRadius;
      private final float endRadius;
      private final BlockState[] blocks;
      private final WhiteNoise whiteNoise;
      private final Position2FloatMap closestMap;
      private final List<Vector4d> positions = new ArrayList<>();
      private Vector4d lastPosition = new Vector4d();

      public Spike(FloatUnaryOperator[] easings, float startRadius, float endRadius, BlockState[] blocks, WhiteNoise whiteNoise, Position2FloatMap closestMap) {
         this.easings = easings;
         this.startRadius = startRadius;
         this.endRadius = endRadius;
         this.blocks = blocks;
         this.whiteNoise = whiteNoise;
         this.closestMap = closestMap;
      }

      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         this.positions.add(new Vector4d(from.x, from.y, from.z, index + minPartial));
         this.lastPosition.set(to.x, to.y, to.z, index + minPartial + partialLength);
      }

      @Override
      public void finish(ChunkedBlockRegion chunkedBlockRegion, MaskElement destinationMask, MaskContext maskContext) {
         this.positions.add(this.lastPosition);
         this.lastPosition = null;
         Vector3d from = new Vector3d();
         Vector3d to = new Vector3d();
         double totalDistance = 0.0;
         Vector4d last = null;

         for (Vector4d position : this.positions) {
            if (last == null) {
               last = position;
            } else {
               from.set(last.x, last.y, last.z);
               to.set(position.x, position.y, position.z);
               totalDistance += from.distance(to);
               last = position;
            }
         }

         double currentDistance = 0.0;
         last = null;

         for (Vector4d positionx : this.positions) {
            if (last == null) {
               last = positionx;
            } else {
               from.set(last.x, last.y, last.z);
               to.set(positionx.x, positionx.y, positionx.z);
               if (from.equals(to)) {
                  last = positionx;
               } else {
                  int index = (int)Math.floor(last.w);
                  double minPartial = last.w - index;
                  double partialLength = last.w - positionx.w;
                  float fromRadius = this.startRadius + (this.endRadius - this.startRadius) * (float)(currentDistance / totalDistance);
                  currentDistance += from.distance(to);
                  float toRadius = this.startRadius + (this.endRadius - this.startRadius) * (float)(currentDistance / totalDistance);
                  Rasterization3D.ddaPartial(
                     from,
                     to,
                     (x, y, z, f) -> {
                        float partial = (float)(minPartial + partialLength * f);
                        partial = this.easings[index].apply(partial);
                        BlockState fromState = this.blocks[Math.min(this.blocks.length - 1, index)];
                        BlockState toState = this.blocks[Math.min(this.blocks.length - 1, index + 1)];
                        float realRadius = fromRadius + (toRadius - fromRadius) * f;
                        float realRadiusSquared = realRadius * realRadius + realRadius;
                        int bigRadius = (int)realRadius + 1;
                        int bigRadiusSquared = bigRadius * bigRadius + bigRadius;
                        double targetX = from.x * (1.0F - f) + to.x * f;
                        double targetY = from.y * (1.0F - f) + to.y * f;
                        double targetZ = from.z * (1.0F - f) + to.z * f;

                        for (int xo = -bigRadius; xo <= bigRadius; xo++) {
                           for (int yo = -bigRadius; yo <= bigRadius; yo++) {
                              for (int zo = -bigRadius; zo <= bigRadius; zo++) {
                                 if (xo == 0 && yo == 0 && zo == 0) {
                                    if (destinationMask.test(maskContext.reset(), x, y, z)) {
                                       if (this.whiteNoise.evaluate(x, y, z) > partial) {
                                          chunkedBlockRegion.addBlockWithoutDirty(x, y, z, fromState);
                                       } else {
                                          chunkedBlockRegion.addBlockWithoutDirty(x, y, z, toState);
                                       }
                                    }
                                 } else if (xo * xo + yo * yo + zo * zo <= bigRadiusSquared) {
                                    double dx = x + xo + 0.5F - targetX;
                                    double dy = y + yo + 0.5F - targetY;
                                    double dz = z + zo + 0.5F - targetZ;
                                    double realDistanceSq = dx * dx + dy * dy + dz * dz;
                                    if (realDistanceSq <= realRadiusSquared
                                       && destinationMask.test(maskContext.reset(), x + xo, y + yo, z + zo)
                                       && this.closestMap.min(x + xo, y + yo, z + zo, (float)realDistanceSq)) {
                                       if (this.whiteNoise.evaluate(x + xo, y + yo, z + zo) > partial) {
                                          chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, fromState);
                                       } else {
                                          chunkedBlockRegion.addBlockWithoutDirty(x + xo, y + yo, z + zo, toState);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  );
                  last = positionx;
               }
            }
         }
      }
   }

   public record ZeroRadiusConstantBlock(BlockState constantBlock) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Rasterization3D.dda(from, to, (x, y, z) -> {
            if (destinationMask.test(maskContext.reset(), x, y, z)) {
               chunkedBlockRegion.addBlockWithoutDirty(x, y, z, this.constantBlock);
            }
         });
      }
   }

   public record ZeroRadiusDynamicBlock(FloatUnaryOperator[] easings, BlockState[] blocks, WhiteNoise whiteNoise) implements PathRasterizer {
      @Override
      public void rasterize(
         ChunkedBlockRegion chunkedBlockRegion,
         MaskElement destinationMask,
         MaskContext maskContext,
         Vector3d from,
         Vector3d to,
         int index,
         float minPartial,
         float partialLength
      ) {
         Rasterization3D.ddaPartial(from, to, (x, y, z, f) -> {
            if (destinationMask.test(maskContext.reset(), x, y, z)) {
               float partial = minPartial + partialLength * f;
               partial = this.easings[index].apply(partial);
               if (this.whiteNoise.evaluate(x, y, z) > partial) {
                  chunkedBlockRegion.addBlockWithoutDirty(x, y, z, this.blocks[Math.min(this.blocks.length - 1, index)]);
               } else {
                  chunkedBlockRegion.addBlockWithoutDirty(x, y, z, this.blocks[Math.min(this.blocks.length - 1, index + 1)]);
               }
            }
         });
      }
   }
}
