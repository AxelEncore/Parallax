package com.moulberry.axiom.tools.lasso_select;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Intersectiond;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4f;

public record PendingLassoSelect(byte[] data, int width, int height, int depth, boolean includeNonSolid, int mode) {
   public void handle(float[] depth, Matrix4f projection) {
      Entity entity = Minecraft.getInstance().player;
      if (entity != null) {
         Vec3 start = entity.getEyePosition();
         PositionSet blockPositions = new PositionSet();
         float[] offsetDistances = new float[this.width * this.height];
         BlockPos[] raycasts = new BlockPos[this.width * this.height];
         Matrix4f inverseProjection = new Matrix4f(projection);
         inverseProjection.invert();
         Level level = entity.level();
         MaskElement maskElement = MaskManager.getSelectionMask();
         MaskContext maskContext = new MaskContext(level);
         TriIntConsumer consumer = (xx, yx, z) -> {
            if (maskElement.test(maskContext.reset(), xx, yx, z)) {
               blockPositions.add(xx, yx, z);
            }
         };
         if (maskElement instanceof ConstantMaskElement constantMaskElement) {
            if (!constantMaskElement.getConstant()) {
               return;
            }

            consumer = blockPositions::add;
         }

         for (int y = 0; y < this.height; y++) {
            boolean inside = false;
            int countUp = 0;
            int countDown = 0;

            for (int x = 0; x < this.width; x++) {
               byte value = this.data[x + y * this.width];
               if (value != -1) {
                  countDown += value & 15;
                  countUp += value >> 4 & 15;
                  if (countUp > 0 && countDown > 0) {
                     int min = Math.min(countUp, countDown);
                     if ((min & 1) != 0) {
                        inside = !inside;
                     }

                     countUp -= min;
                     countDown -= min;
                  }
               }

               if (inside || value != 0) {
                  float normX = (float)x / (this.width - 1) * 2.0F - 1.0F;
                  float normY = (float)y / (this.height - 1) * 2.0F - 1.0F;
                  Vec3 forwards = EditorUI.getForwardsVectorRaw(normX, normY);
                  Vec3 lookDirection = EditorUI.getMouseLookVectorFromForwards(forwards);
                  if (lookDirection != null) {
                     float pixelDepth = depth[x + (this.height - y - 1) * this.width];
                     Vector4f depthForwards = new Vector4f(normX, normY, pixelDepth * 2.0F - 1.0F, 1.0F);
                     depthForwards.mul(inverseProjection);
                     float a = depthForwards.x / depthForwards.w;
                     float b = depthForwards.y / depthForwards.w;
                     float c = depthForwards.z / depthForwards.w;
                     float depthDistance = (float)Math.sqrt(a * a + b * b + c * c);
                     float depthFar = projection.perspectiveFar();
                     if (!(depthDistance > depthFar / 2.0F)) {
                        float offsetDistance = Math.max(0.0F, depthDistance - 3.0F);
                        Vector3d raycastStart = new Vector3d(
                           start.x + lookDirection.x * offsetDistance, start.y + lookDirection.y * offsetDistance, start.z + lookDirection.z * offsetDistance
                        );
                        RayCaster.RaycastResult raycastResult = RayCaster.raycast(
                           level,
                           raycastStart,
                           new Vector3d(lookDirection.x, lookDirection.y, lookDirection.z),
                           false,
                           Tool.defaultIncludeFluids(),
                           this.includeNonSolid
                        );
                        if (raycastResult != null) {
                           BlockPos blockPos = raycastResult.blockPos();
                           this.handleHit(entity.level(), lookDirection, raycastResult, consumer);
                           offsetDistances[x + y * this.width] = offsetDistance;
                           raycasts[x + y * this.width] = blockPos;
                           if (y > 0) {
                              float upOffsetDistance = offsetDistances[x + (y - 1) * this.width];
                              float normYUp = (float)(y - 1) / (this.height - 1) * 2.0F - 1.0F;
                              this.subdivide(
                                 entity.level(),
                                 consumer,
                                 start,
                                 blockPos,
                                 raycasts[x + (y - 1) * this.width],
                                 normX,
                                 normY,
                                 normX,
                                 normYUp,
                                 Math.min(offsetDistance, upOffsetDistance)
                              );
                           }

                           if (x > 0) {
                              float leftOffsetDistance = offsetDistances[x - 1 + y * this.width];
                              float normXLeft = (float)(x - 1) / (this.width - 1) * 2.0F - 1.0F;
                              this.subdivide(
                                 entity.level(),
                                 consumer,
                                 start,
                                 blockPos,
                                 raycasts[x - 1 + y * this.width],
                                 normX,
                                 normY,
                                 normXLeft,
                                 normY,
                                 Math.min(offsetDistance, leftOffsetDistance)
                              );
                           }
                        }
                     }
                  }
               }
            }
         }

         switch (this.mode) {
            case 0:
               Selection.clearSelection();
               Selection.addSet(blockPositions);
               break;
            case 1:
               Selection.addSet(blockPositions);
               break;
            case 2:
               Selection.subtractSet(blockPositions);
               break;
            case 3:
               Selection.intersectSet(blockPositions);
               break;
            default:
               throw new FaultyImplementationError();
         }
      }
   }

   private void handleHit(Level level, Vec3 lookDirection, RayCaster.RaycastResult raycastResult, TriIntConsumer consumer) {
      if (this.depth == 1) {
         BlockPos pos = raycastResult.getBlockPos();
         consumer.accept(pos.getX(), pos.getY(), pos.getZ());
      } else {
         Vec3 worldPos = raycastResult.worldPos();
         Vector3d from = new Vector3d(worldPos.x - lookDirection.x, worldPos.y - lookDirection.y, worldPos.z - lookDirection.z);
         Vector3d to = new Vector3d(worldPos.x + lookDirection.x, worldPos.y + lookDirection.y, worldPos.z + lookDirection.z);
         BlockPos blockPos = raycastResult.blockPos();
         Vector3d min = new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ());
         Vector3d max = new Vector3d(blockPos.getX() + 1.0, blockPos.getY() + 1.0, blockPos.getZ() + 1.0);
         Vector2d result = new Vector2d();
         double x;
         double y;
         double z;
         if (Intersectiond.intersectLineSegmentAab(from, to, min, max, result) != -1) {
            x = from.x + result.x * (to.x - from.x);
            y = from.y + result.x * (to.y - from.y);
            z = from.z + result.x * (to.z - from.z);
         } else {
            x = worldPos.x;
            y = worldPos.y;
            z = worldPos.z;
         }

         if (this.depth > 0) {
            Vector3d fromVec = new Vector3d(x + lookDirection.x * 0.01, y + lookDirection.y * 0.01, z + lookDirection.z * 0.01);
            Vector3d toVec = new Vector3d(x + lookDirection.x * this.depth, y + lookDirection.y * this.depth, z + lookDirection.z * this.depth);
            if (this.includeNonSolid) {
               Rasterization3D.dda(fromVec, toVec, consumer);
            } else {
               RayCaster.ddaSkip(fromVec, toVec, level, true, consumer);
            }
         } else if (this.depth < 0) {
            Vector3d fromVec = new Vector3d(x - lookDirection.x * 0.01, y - lookDirection.y * 0.01, z - lookDirection.z * 0.01);
            Vector3d toVec = new Vector3d(x + lookDirection.x * this.depth, y + lookDirection.y * this.depth, z + lookDirection.z * this.depth);
            RayCaster.ddaSkip(fromVec, toVec, level, false, consumer);
         }
      }
   }

   private void subdivide(
      Level level, TriIntConsumer consumer, Vec3 start, BlockPos aPos, BlockPos bPos, float aX, float aY, float bX, float bY, float offsetDistance
   ) {
      if (aPos != null && bPos != null && !aPos.equals(bPos)) {
         if (aX != bX || aY != bY) {
            float newX = (aX + bX) / 2.0F;
            float newY = (aY + bY) / 2.0F;
            if ((newX != aX || newY != aY) && (newX != bX || newY != bY)) {
               Vec3 forwards = EditorUI.getForwardsVectorRaw(newX, newY);
               Vec3 lookDirection = EditorUI.getMouseLookVectorFromForwards(forwards);
               if (lookDirection != null) {
                  Vector3d raycastStart = new Vector3d(
                     start.x + lookDirection.x * offsetDistance, start.y + lookDirection.y * offsetDistance, start.z + lookDirection.z * offsetDistance
                  );
                  RayCaster.RaycastResult raycastResult = RayCaster.raycast(
                     level,
                     raycastStart,
                     new Vector3d(lookDirection.x, lookDirection.y, lookDirection.z),
                     false,
                     Tool.defaultIncludeFluids(),
                     this.includeNonSolid
                  );
                  if (raycastResult != null) {
                     BlockPos blockPos = raycastResult.blockPos();
                     this.handleHit(level, lookDirection, raycastResult, consumer);
                     if (!blockPos.equals(aPos) && !blockPos.equals(bPos)) {
                        this.subdivide(level, consumer, start, aPos, blockPos, aX, aY, newX, newY, offsetDistance);
                        this.subdivide(level, consumer, start, blockPos, bPos, newX, newY, bX, bY, offsetDistance);
                     }
                  }
               }
            }
         }
      }
   }
}
