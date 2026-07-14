package com.moulberry.axiom.tools.modify;

import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.funcinterfaces.TriIntPredicate;
import com.moulberry.axiom.rasterization.Rasterization2D;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class ModifyRevolve {
   public static ChunkedBlockRegion revolve(
      Level level, SelectionBuffer selectionBuffer, BlockPos point, double angle, int axis, BlockPos translation, TriIntPredicate mask
   ) {
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         return revolveAABB(level, aabb, point, angle, axis, translation, mask);
      } else {
         return selectionBuffer instanceof SelectionBuffer.Set set ? revolveSet(level, set, point, angle, axis, translation, mask) : new ChunkedBlockRegion();
      }
   }

   private static ChunkedBlockRegion revolveAABB(
      Level level, SelectionBuffer.AABB aabb, BlockPos point, double angle, int axis, BlockPos translation, TriIntPredicate mask
   ) {
      int minX = aabb.min().getX();
      int minY = aabb.min().getY();
      int minZ = aabb.min().getZ();
      int maxX = aabb.max().getX();
      int maxY = aabb.max().getY();
      int maxZ = aabb.max().getZ();
      int revolveX = point.getX();
      int revolveY = point.getY();
      int revolveZ = point.getZ();
      ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
      int negativeMult = angle < 0.0 ? -1 : 1;
      angle = Math.abs(angle);
      boolean translate = !translation.equals(BlockPos.ZERO);

      int duplicates;
      for (duplicates = 0; Math.toDegrees(angle) > 360.5; duplicates++) {
         angle -= Math.PI * 2;
      }

      int duplicatesF = translate ? duplicates : 0;
      MutableBlockPos mutableBlockPos = new MutableBlockPos();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
               if (!blockState.isAir()) {
                  if (axis == 0) {
                     int finalX = x;
                     int dy = y - revolveY;
                     int dz = z - revolveZ;
                     int radius = (int)Math.round(Math.sqrt(dy * dy + dz * dz));
                     double fromAngle = negativeMult * Math.atan2(dy, dz);
                     double toAngle = fromAngle + angle;
                     Rasterization2D.ddaCircle(radius, (y1, z1) -> {
                        double blockAngle = negativeMult * Math.atan2(y1, z1);
                        if (blockAngle <= fromAngle) {
                           blockAngle += Math.PI * 2;
                        }

                        Rotation rotation = BlockHelper.rotationFromRadians(blockAngle - fromAngle);
                        BlockState rotated = BlockHelper.rotateX(blockState, rotation);

                        for (int i = 0; i <= duplicatesF; i++) {
                           if (i == duplicatesF && blockAngle > toAngle + 1.0E-5) {
                              return;
                           }

                           int x1 = 0;
                           if (translate) {
                              double percentage = (blockAngle + i * Math.PI * 2.0 - fromAngle) / (toAngle + duplicatesF * Math.PI * 2.0 - fromAngle);
                              x1 += (int)Math.round(translation.getX() * percentage);
                              y1 += (int)Math.round(translation.getY() * percentage);
                              z1 += (int)Math.round(translation.getZ() * percentage);
                           }

                           if (mask.test(finalX + x1, revolveY + y1, revolveZ + z1)) {
                              chunkedBlockRegion.addBlockWithoutDirty(finalX + x1, revolveY + y1, revolveZ + z1, rotated);
                           }
                        }
                     });
                  } else if (axis == 1) {
                     int dx = x - revolveX;
                     int dz = z - revolveZ;
                     int finalY = y;
                     int radius = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
                     double fromAngle = negativeMult * Math.atan2(dx, -dz);
                     double toAngle = fromAngle + angle;
                     Rasterization2D.ddaCircle(radius, (x1, z1) -> {
                        double blockAngle = negativeMult * Math.atan2(x1, -z1);
                        if (blockAngle <= fromAngle) {
                           blockAngle += Math.PI * 2;
                        }

                        Rotation rotation = BlockHelper.rotationFromRadians(blockAngle - fromAngle);
                        BlockState rotated = BlockHelper.rotateY(blockState, rotation);

                        for (int i = 0; i <= duplicatesF; i++) {
                           if (i == duplicatesF && blockAngle > toAngle + 1.0E-5) {
                              return;
                           }

                           int y1 = 0;
                           if (translate) {
                              double percentage = (blockAngle + i * Math.PI * 2.0 - fromAngle) / (toAngle + duplicatesF * Math.PI * 2.0 - fromAngle);
                              x1 += (int)Math.round(translation.getX() * percentage);
                              y1 += (int)Math.round(translation.getY() * percentage);
                              z1 += (int)Math.round(translation.getZ() * percentage);
                           }

                           if (mask.test(revolveX + x1, finalY + y1, revolveZ + z1)) {
                              chunkedBlockRegion.addBlockWithoutDirty(revolveX + x1, finalY + y1, revolveZ + z1, rotated);
                           }
                        }
                     });
                  } else if (axis == 2) {
                     int dx = x - revolveX;
                     int dy = y - revolveY;
                     int finalZ = z;
                     int radius = (int)Math.round(Math.sqrt(dx * dx + dy * dy));
                     double fromAngle = negativeMult * Math.atan2(dx, dy);
                     double toAngle = fromAngle + angle;
                     Rasterization2D.ddaCircle(radius, (x1, y1) -> {
                        double blockAngle = negativeMult * Math.atan2(x1, y1);
                        if (blockAngle <= fromAngle) {
                           blockAngle += Math.PI * 2;
                        }

                        Rotation rotation = BlockHelper.rotationFromRadians(blockAngle - fromAngle);
                        BlockState rotated = BlockHelper.rotateZ(blockState, rotation);

                        for (int i = 0; i <= duplicatesF; i++) {
                           if (i == duplicatesF && blockAngle > toAngle + 1.0E-5) {
                              return;
                           }

                           int z1 = 0;
                           if (translate) {
                              double percentage = (blockAngle + i * Math.PI * 2.0 - fromAngle) / (toAngle + duplicatesF * Math.PI * 2.0 - fromAngle);
                              x1 += (int)Math.round(translation.getX() * percentage);
                              y1 += (int)Math.round(translation.getY() * percentage);
                              z1 += (int)Math.round(translation.getZ() * percentage);
                           }

                           if (mask.test(revolveX + x1, revolveY + y1, finalZ + z1)) {
                              chunkedBlockRegion.addBlockWithoutDirty(revolveX + x1, revolveY + y1, finalZ + z1, rotated);
                           }
                        }
                     });
                  }
               }
            }
         }
      }

      return chunkedBlockRegion;
   }

   private static ChunkedBlockRegion revolveSet(
      Level level, SelectionBuffer.Set set, BlockPos point, double angle, int axis, BlockPos translation, TriIntPredicate mask
   ) {
      int revolveX = point.getX();
      int revolveY = point.getY();
      int revolveZ = point.getZ();
      ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
      angle = Math.abs(angle);
      boolean translate = !translation.equals(BlockPos.ZERO);

      int duplicates;
      for (duplicates = 0; Math.toDegrees(angle) > 360.5; duplicates++) {
         angle -= Math.PI * 2;
      }

      double angleF = angle;
      int duplicatesF = translate ? duplicates : 0;
      Rotation[] rotations = Rotation.values();
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      set.forEach((x, y, z) -> {
         BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
         if (!blockState.isAir()) {
            if (axis == 0) {
               int dy = y - revolveY;
               int dz = z - revolveZ;
               int radius = (int)Math.round(Math.sqrt(dy * dy + dz * dz));
               double fromAngle = Math.atan2(dy, dz);
               double toAngle = fromAngle + angleF;
               Rasterization2D.ddaCircle(radius, (y1, z1) -> {
                  double blockAngle = Math.atan2(y1, z1);
                  if (blockAngle <= fromAngle) {
                     blockAngle += Math.PI * 2;
                  }

                  for (int i = 0; i <= duplicatesF; i++) {
                     if (i == duplicatesF && blockAngle > toAngle + 1.0E-5) {
                        return;
                     }

                     int x1 = 0;
                     if (translate) {
                        double percentage = (blockAngle + i * Math.PI * 2.0 - fromAngle) / (toAngle + duplicatesF * Math.PI * 2.0 - fromAngle);
                        x1 += (int)Math.round(translation.getX() * percentage);
                        y1 += (int)Math.round(translation.getY() * percentage);
                        z1 += (int)Math.round(translation.getZ() * percentage);
                     }

                     if (mask.test(x + x1, revolveY + y1, revolveZ + z1)) {
                        chunkedBlockRegion.addBlockWithoutDirty(x + x1, revolveY + y1, revolveZ + z1, blockState);
                     }
                  }
               });
            } else if (axis == 1) {
               int dx = x - revolveX;
               int dz = z - revolveZ;
               int radius = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
               double fromAngle = Math.atan2(dx, -dz);
               double toAngle = fromAngle + angleF;
               Rasterization2D.ddaCircle(radius, (x1, z1) -> {
                  double blockAngle = Math.atan2(x1, -z1);
                  if (blockAngle <= fromAngle) {
                     blockAngle += Math.PI * 2;
                  }

                  int rotationCount = (int)Math.round((blockAngle - fromAngle) / (Math.PI / 2));
                  rotationCount %= 4;
                  if (rotationCount < 0) {
                     rotationCount += 4;
                  }

                  BlockState rotated = BlockHelper.rotateY(blockState, rotations[rotationCount]);

                  for (int i = 0; i <= duplicatesF; i++) {
                     if (i == duplicatesF && blockAngle > toAngle + 1.0E-5) {
                        return;
                     }

                     int y1 = 0;
                     if (translate) {
                        double percentage = (blockAngle + i * Math.PI * 2.0 - fromAngle) / (toAngle + duplicatesF * Math.PI * 2.0 - fromAngle);
                        x1 += (int)Math.round(translation.getX() * percentage);
                        y1 += (int)Math.round(translation.getY() * percentage);
                        z1 += (int)Math.round(translation.getZ() * percentage);
                     }

                     if (mask.test(revolveX + x1, y + y1, revolveZ + z1)) {
                        chunkedBlockRegion.addBlockWithoutDirty(revolveX + x1, y + y1, revolveZ + z1, rotated);
                     }
                  }
               });
            } else if (axis == 2) {
               int dx = x - revolveX;
               int dy = y - revolveY;
               int radius = (int)Math.round(Math.sqrt(dx * dx + dy * dy));
               double fromAngle = Math.atan2(dx, dy);
               double toAngle = fromAngle + angleF;
               Rasterization2D.ddaCircle(radius, (x1, y1) -> {
                  float blockAngle = (float)Math.atan2(x1, y1);
                  if (blockAngle <= fromAngle) {
                     blockAngle = (float)(blockAngle + (Math.PI * 2));
                  }

                  for (int i = 0; i <= duplicatesF; i++) {
                     if (i == duplicatesF && blockAngle > toAngle + 1.0E-5) {
                        return;
                     }

                     int z1 = 0;
                     if (translate) {
                        double percentage = (blockAngle + i * Math.PI * 2.0 - fromAngle) / (toAngle + duplicatesF * Math.PI * 2.0 - fromAngle);
                        x1 += (int)Math.round(translation.getX() * percentage);
                        y1 += (int)Math.round(translation.getY() * percentage);
                        z1 += (int)Math.round(translation.getZ() * percentage);
                     }

                     if (mask.test(revolveX + x1, revolveY + y1, z + z1)) {
                        chunkedBlockRegion.addBlockWithoutDirty(revolveX + x1, revolveY + y1, z + z1, blockState);
                     }
                  }
               });
            }
         }
      });
      return chunkedBlockRegion;
   }
}
