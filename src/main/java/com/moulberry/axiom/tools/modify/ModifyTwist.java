package com.moulberry.axiom.tools.modify;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.funcinterfaces.TriIntPredicate;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ModifyTwist {
   public static ChunkedBlockRegion twist(float angleDegrees, float axisX, float axisY, float axisZ, TriIntPredicate mask) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      BlockPos max = selectionBuffer.max();
      BlockPos min = selectionBuffer.min();
      if (!selectionBuffer.isEmpty() && max != null && min != null) {
         int offsetX = Math.floorDiv(max.getX() + min.getX(), 2);
         int offsetY = Math.floorDiv(max.getY() + min.getY(), 2);
         int offsetZ = Math.floorDiv(max.getZ() + min.getZ(), 2);
         float minDot = Float.MAX_VALUE;
         float maxDot = Float.MIN_VALUE;
         int minX = min.getX();
         int minY = min.getY();
         int minZ = min.getZ();
         int maxX = max.getX();
         int maxY = max.getY();
         int maxZ = max.getZ();

         for (int x = minX; x <= maxX; x = maxX) {
            for (int y = minY; y <= maxY; y = maxY) {
               for (int z = minZ; z <= maxZ; z = maxZ) {
                  float dot = (x - offsetX) * axisX + (y - offsetY) * axisY + (z - offsetZ) * axisZ;
                  minDot = Math.min(minDot, dot);
                  maxDot = Math.max(maxDot, dot);
                  if (z == maxZ) {
                     break;
                  }
               }

               if (y == maxY) {
                  break;
               }
            }

            if (x == maxX) {
               break;
            }
         }

         if (maxDot - minDot < 1.0E-4) {
            return null;
         } else if (selectionBuffer instanceof SelectionBuffer.AABB) {
            return twistAABB(angleDegrees, axisX, axisY, axisZ, offsetX, offsetY, offsetZ, minDot, maxDot, mask);
         } else {
            return selectionBuffer instanceof SelectionBuffer.Set set
               ? twistSet(angleDegrees, axisX, axisY, axisZ, offsetX, offsetY, offsetZ, minDot, maxDot, mask, set)
               : null;
         }
      } else {
         return null;
      }
   }

   private static ChunkedBlockRegion twistAABB(
      float angleDegrees, float axisX, float axisY, float axisZ, int offsetX, int offsetY, int offsetZ, float minDot, float maxDot, TriIntPredicate mask
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
         BlockPos max = selectionBuffer.max();
         BlockPos min = selectionBuffer.min();
         if (!selectionBuffer.isEmpty() && max != null && min != null) {
            int minX = min.getX();
            int minY = min.getY();
            int minZ = min.getZ();
            int maxX = max.getX();
            int maxY = max.getY();
            int maxZ = max.getZ();
            ChunkedBlockRegion out = new ChunkedBlockRegion();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            int newMinX = Integer.MAX_VALUE;
            int newMinY = Integer.MAX_VALUE;
            int newMinZ = Integer.MAX_VALUE;
            int newMaxX = Integer.MIN_VALUE;
            int newMaxY = Integer.MIN_VALUE;
            int newMaxZ = Integer.MIN_VALUE;
            Vector4f vector4f = new Vector4f();
            Matrix4f matrix = new Matrix4f();

            for (int x = minX; x <= maxX; x++) {
               for (int y = minY; y <= maxY; y++) {
                  for (int z = minZ; z <= maxZ; z++) {
                     float dot = (x - offsetX) * axisX + (y - offsetY) * axisY + (z - offsetZ) * axisZ;
                     float angle = (float)Math.toRadians(angleDegrees) * (dot - minDot) / (maxDot - minDot);
                     matrix.rotation(angle, axisX, axisY, axisZ);
                     if (mask.test(x, y, z)) {
                        out.addBlockIfNotPresent(x, y, z, Blocks.AIR.defaultBlockState());
                     }

                     vector4f.set(x - offsetX, y - offsetY, z - offsetZ, 1.0F);
                     matrix.transform(vector4f);
                     newMinX = Math.min(newMinX, (int)Math.floor(vector4f.x));
                     newMinY = Math.min(newMinY, (int)Math.floor(vector4f.y));
                     newMinZ = Math.min(newMinZ, (int)Math.floor(vector4f.z));
                     newMaxX = Math.max(newMaxX, (int)Math.ceil(vector4f.x));
                     newMaxY = Math.max(newMaxY, (int)Math.ceil(vector4f.y));
                     newMaxZ = Math.max(newMaxZ, (int)Math.ceil(vector4f.z));
                  }
               }
            }

            Direction[] directions = Direction.values();

            for (int x = newMinX; x <= newMaxX; x++) {
               for (int y = newMinY; y <= newMaxY; y++) {
                  for (int z = newMinZ; z <= newMaxZ; z++) {
                     float dot = x * axisX + y * axisY + z * axisZ;
                     float angle = (float)Math.toRadians(angleDegrees) * (dot - minDot) / (maxDot - minDot);
                     matrix.rotation(angle, axisX, axisY, axisZ);
                     matrix.invert();
                     vector4f.set(x, y, z, 1.0F);
                     matrix.transform(vector4f);
                     int fromX = Math.round(vector4f.x) + offsetX;
                     int fromY = Math.round(vector4f.y) + offsetY;
                     int fromZ = Math.round(vector4f.z) + offsetZ;
                     if (fromX >= minX && fromX <= maxX && fromY >= minY && fromY <= maxY && fromZ >= minZ && fromZ <= maxZ) {
                        BlockState blockState = level.getBlockState(mutableBlockPos.set(fromX, fromY, fromZ));
                        if (!blockState.isAir() && mask.test(x + offsetX, y + offsetY, z + offsetZ)) {
                           out.addBlockWithoutDirty(x + offsetX, y + offsetY, z + offsetZ, blockState);
                           continue;
                        }
                     }

                     float currFloatX = vector4f.x;
                     float currFloatY = vector4f.y;
                     float currFloatZ = vector4f.z;

                     for (Direction direction : directions) {
                        int nx = direction.getStepX();
                        int ny = direction.getStepY();
                        int nz = direction.getStepZ();
                        vector4f.set(x + nx, y + ny, z + nz, 1.0F);
                        matrix.transform(vector4f);
                        int neighborFromX = Math.round(vector4f.x) + offsetX;
                        int neighborFromY = Math.round(vector4f.y) + offsetY;
                        int neighborFromZ = Math.round(vector4f.z) + offsetZ;
                        int manhattanDistance = Math.abs(neighborFromX - fromX) + Math.abs(neighborFromY - fromY) + Math.abs(neighborFromZ - fromZ);
                        if (manhattanDistance == 2) {
                           float avgX = x + nx / 2.0F;
                           float avgY = y + ny / 2.0F;
                           float avgZ = z + nz / 2.0F;
                           float dx = currFloatX - avgX;
                           float dy = currFloatY - avgY;
                           float dz = currFloatZ - avgZ;
                           float currDistanceSq = dx * dx + dy * dy + dz * dz;
                           dx = vector4f.x - avgX;
                           dy = vector4f.y - avgY;
                           dz = vector4f.z - avgZ;
                           if (!(currDistanceSq > dx * dx + dy * dy + dz * dz)) {
                              BlockState neighborState = level.getBlockState(mutableBlockPos.set(neighborFromX, neighborFromY, neighborFromZ));
                              if (neighborState.isAir()) {
                                 int iterMinX = Math.min(fromX, neighborFromX);
                                 int iterMaxX = Math.max(fromX, neighborFromX);
                                 int iterMinY = Math.min(fromY, neighborFromY);
                                 int iterMaxY = Math.max(fromY, neighborFromY);
                                 int iterMinZ = Math.min(fromZ, neighborFromZ);
                                 int iterMaxZ = Math.max(fromZ, neighborFromZ);
                                 iterMinX = Math.max(iterMinX, minX);
                                 iterMaxX = Math.min(iterMaxX, maxX);
                                 iterMinY = Math.max(iterMinY, minY);
                                 iterMaxY = Math.min(iterMaxY, maxY);
                                 iterMinZ = Math.max(iterMinZ, minZ);
                                 iterMaxZ = Math.min(iterMaxZ, maxZ);
                                 BlockState closestBlock = null;
                                 float closestDistanceSq = Float.MAX_VALUE;

                                 for (int x1 = iterMinX; x1 <= iterMaxX; x1++) {
                                    for (int y1 = iterMinY; y1 <= iterMaxY; y1++) {
                                       for (int z1 = iterMinZ; z1 <= iterMaxZ; z1++) {
                                          if ((x1 != fromX || y1 != fromY || z1 != fromZ)
                                             && (x1 != neighborFromX || y1 != neighborFromY || z1 != neighborFromZ)) {
                                             BlockState intermediate = level.getBlockState(mutableBlockPos.set(x1, y1, z1));
                                             if (!intermediate.isAir()) {
                                                dx = currFloatX - x1;
                                                dy = currFloatY - y1;
                                                dz = currFloatZ - z1;
                                                float intermediateDistanceSq = dx * dx + dy * dy + dz * dz;
                                                if (closestBlock == null || intermediateDistanceSq < closestDistanceSq) {
                                                   closestBlock = intermediate;
                                                   closestDistanceSq = intermediateDistanceSq;
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }

                                 if (closestBlock != null && !closestBlock.isAir() && mask.test(x + offsetX, y + offsetY, z + offsetZ)) {
                                    out.addBlockWithoutDirty(x + offsetX, y + offsetY, z + offsetZ, closestBlock);
                                    break;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            return out;
         } else {
            return null;
         }
      }
   }

   private static ChunkedBlockRegion twistSet(
      float angleDegrees,
      float axisX,
      float axisY,
      float axisZ,
      int offsetX,
      int offsetY,
      int offsetZ,
      float minDot,
      float maxDot,
      TriIntPredicate mask,
      SelectionBuffer.Set set
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
         BlockPos max = selectionBuffer.max();
         BlockPos min = selectionBuffer.min();
         if (!selectionBuffer.isEmpty() && max != null && min != null) {
            int minX = min.getX();
            int minY = min.getY();
            int minZ = min.getZ();
            int maxX = max.getX();
            int maxY = max.getY();
            int maxZ = max.getZ();
            ChunkedBlockRegion out = new ChunkedBlockRegion();
            MutableBlockPos mutableBlockPos = new MutableBlockPos();

            class MinMaxWrapper {
               int newMinX = Integer.MAX_VALUE;
               int newMinY = Integer.MAX_VALUE;
               int newMinZ = Integer.MAX_VALUE;
               int newMaxX = Integer.MIN_VALUE;
               int newMaxY = Integer.MIN_VALUE;
               int newMaxZ = Integer.MIN_VALUE;
            }

            MinMaxWrapper wrapper = new MinMaxWrapper();
            Vector4f vector4f = new Vector4f();
            Matrix4f matrix = new Matrix4f();
            set.forEach((xx, yx, zx) -> {
               float dotx = (xx - offsetX) * axisX + (yx - offsetY) * axisY + (zx - offsetZ) * axisZ;
               float anglex = (float)Math.toRadians(angleDegrees) * (dotx - minDot) / (maxDot - minDot);
               matrix.rotation(anglex, axisX, axisY, axisZ);
               if (mask.test(xx, yx, zx)) {
                  out.addBlockIfNotPresent(xx, yx, zx, Blocks.AIR.defaultBlockState());
               }

               vector4f.set(xx - offsetX, yx - offsetY, zx - offsetZ, 1.0F);
               matrix.transform(vector4f);
               wrapper.newMinX = Math.min(wrapper.newMinX, (int)Math.floor(vector4f.x));
               wrapper.newMinY = Math.min(wrapper.newMinY, (int)Math.floor(vector4f.y));
               wrapper.newMinZ = Math.min(wrapper.newMinZ, (int)Math.floor(vector4f.z));
               wrapper.newMaxX = Math.max(wrapper.newMaxX, (int)Math.ceil(vector4f.x));
               wrapper.newMaxY = Math.max(wrapper.newMaxY, (int)Math.ceil(vector4f.y));
               wrapper.newMaxZ = Math.max(wrapper.newMaxZ, (int)Math.ceil(vector4f.z));
            });
            int newMinX = wrapper.newMinX;
            int newMinY = wrapper.newMinY;
            int newMinZ = wrapper.newMinZ;
            int newMaxX = wrapper.newMaxX;
            int newMaxY = wrapper.newMaxY;
            int newMaxZ = wrapper.newMaxZ;
            Direction[] directions = Direction.values();

            for (int x = newMinX; x <= newMaxX; x++) {
               for (int y = newMinY; y <= newMaxY; y++) {
                  for (int z = newMinZ; z <= newMaxZ; z++) {
                     float dot = x * axisX + y * axisY + z * axisZ;
                     float angle = (float)Math.toRadians(angleDegrees) * (dot - minDot) / (maxDot - minDot);
                     matrix.rotation(angle, axisX, axisY, axisZ);
                     matrix.invert();
                     vector4f.set(x, y, z, 1.0F);
                     matrix.transform(vector4f);
                     int fromX = Math.round(vector4f.x) + offsetX;
                     int fromY = Math.round(vector4f.y) + offsetY;
                     int fromZ = Math.round(vector4f.z) + offsetZ;
                     if (set.contains(fromX, fromY, fromZ)) {
                        BlockState blockState = level.getBlockState(mutableBlockPos.set(fromX, fromY, fromZ));
                        if (!blockState.isAir() && mask.test(x + offsetX, y + offsetY, z + offsetZ)) {
                           out.addBlockWithoutDirty(x + offsetX, y + offsetY, z + offsetZ, blockState);
                           continue;
                        }
                     }

                     float currFloatX = vector4f.x;
                     float currFloatY = vector4f.y;
                     float currFloatZ = vector4f.z;

                     for (Direction direction : directions) {
                        int nx = direction.getStepX();
                        int ny = direction.getStepY();
                        int nz = direction.getStepZ();
                        vector4f.set(x + nx, y + ny, z + nz, 1.0F);
                        matrix.transform(vector4f);
                        int neighborFromX = Math.round(vector4f.x) + offsetX;
                        int neighborFromY = Math.round(vector4f.y) + offsetY;
                        int neighborFromZ = Math.round(vector4f.z) + offsetZ;
                        int manhattanDistance = Math.abs(neighborFromX - fromX) + Math.abs(neighborFromY - fromY) + Math.abs(neighborFromZ - fromZ);
                        if (manhattanDistance == 2) {
                           float avgX = x + nx / 2.0F;
                           float avgY = y + ny / 2.0F;
                           float avgZ = z + nz / 2.0F;
                           float dx = currFloatX - avgX;
                           float dy = currFloatY - avgY;
                           float dz = currFloatZ - avgZ;
                           float currDistanceSq = dx * dx + dy * dy + dz * dz;
                           dx = vector4f.x - avgX;
                           dy = vector4f.y - avgY;
                           dz = vector4f.z - avgZ;
                           if (!(currDistanceSq > dx * dx + dy * dy + dz * dz)) {
                              BlockState neighborState = level.getBlockState(mutableBlockPos.set(neighborFromX, neighborFromY, neighborFromZ));
                              if (neighborState.isAir()) {
                                 int iterMinX = Math.min(fromX, neighborFromX);
                                 int iterMaxX = Math.max(fromX, neighborFromX);
                                 int iterMinY = Math.min(fromY, neighborFromY);
                                 int iterMaxY = Math.max(fromY, neighborFromY);
                                 int iterMinZ = Math.min(fromZ, neighborFromZ);
                                 int iterMaxZ = Math.max(fromZ, neighborFromZ);
                                 BlockState closestBlock = null;
                                 float closestDistanceSq = Float.MAX_VALUE;

                                 for (int x1 = iterMinX; x1 <= iterMaxX; x1++) {
                                    for (int y1 = iterMinY; y1 <= iterMaxY; y1++) {
                                       for (int z1 = iterMinZ; z1 <= iterMaxZ; z1++) {
                                          if ((x1 != fromX || y1 != fromY || z1 != fromZ)
                                             && (x1 != neighborFromX || y1 != neighborFromY || z1 != neighborFromZ)
                                             && set.contains(x1, y1, z1)) {
                                             BlockState intermediate = level.getBlockState(mutableBlockPos.set(x1, y1, z1));
                                             if (!intermediate.isAir()) {
                                                dx = currFloatX - x1;
                                                dy = currFloatY - y1;
                                                dz = currFloatZ - z1;
                                                float intermediateDistanceSq = dx * dx + dy * dy + dz * dz;
                                                if (closestBlock == null || intermediateDistanceSq < closestDistanceSq) {
                                                   closestBlock = intermediate;
                                                   closestDistanceSq = intermediateDistanceSq;
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }

                                 if (closestBlock != null && !closestBlock.isAir() && mask.test(x + offsetX, y + offsetY, z + offsetZ)) {
                                    out.addBlockWithoutDirty(x + offsetX, y + offsetY, z + offsetZ, closestBlock);
                                    break;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            return out;
         } else {
            return null;
         }
      }
   }
}
