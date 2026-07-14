package com.moulberry.axiom.pather;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ToolPatherPoint implements com.moulberry.axiomclientapi.pathers.ToolPatherPoint {
   private Vec3 lastPosition = null;
   private Vec3 lastLookDirection = null;
   public boolean includeNonSolid;
   public boolean includeFluids;
   private static final Axis[][][] AXIS_PERMUTATIONS = computeAxisPermutationArray();
   private static final Axis[][] AXIS_PERMUTATIONS_NO_X_PRIO_Y = new Axis[][]{{Axis.Y, Axis.Z}, {Axis.Z, Axis.Y}};
   private static final Axis[][] AXIS_PERMUTATIONS_NO_X_PRIO_Z = new Axis[][]{{Axis.Z, Axis.Y}, {Axis.Y, Axis.Z}};
   private static final Axis[][] AXIS_PERMUTATIONS_NO_Y_PRIO_X = new Axis[][]{{Axis.X, Axis.Z}, {Axis.Z, Axis.X}};
   private static final Axis[][] AXIS_PERMUTATIONS_NO_Y_PRIO_Z = new Axis[][]{{Axis.Z, Axis.X}, {Axis.X, Axis.Z}};
   private static final Axis[][] AXIS_PERMUTATIONS_NO_Z_PRIO_X = new Axis[][]{{Axis.X, Axis.Y}, {Axis.Y, Axis.X}};
   private static final Axis[][] AXIS_PERMUTATIONS_NO_Z_PRIO_Y = new Axis[][]{{Axis.Y, Axis.X}, {Axis.X, Axis.Y}};

   public ToolPatherPoint(boolean includeNonSolid) {
      this.includeNonSolid = includeNonSolid;
      this.includeFluids = Tool.defaultIncludeFluids() && this.includeNonSolid;
   }

   public ToolPatherPoint() {
      this(true);
   }

   public void update(TriIntConsumer consumer) {
      Entity entity = Minecraft.getInstance().player;
      if (entity != null && entity == Minecraft.getInstance().cameraEntity) {
         Vec3 lookDirection = null;
         if (EditorUI.isActive()) {
            if (EditorUI.isMovingCamera()) {
               return;
            }

            lookDirection = EditorUI.getMouseLookVector();
         } else if (Minecraft.getInstance().cameraEntity != null) {
            lookDirection = Minecraft.getInstance().cameraEntity.getLookAngle();
         }

         if (lookDirection != null) {
            Vec3 start = entity.getEyePosition();
            if (this.lastLookDirection != null && this.lastPosition != null) {
               double dot = this.lastLookDirection.dot(lookDirection);
               double angleChange = Math.toDegrees(Math.acos(dot));
               int steps = 1;
               if (angleChange > 1.0) {
                  steps = (int)Math.ceil(angleChange);
               }

               for (int i = 1; i <= steps; i++) {
                  float f = (float)i / steps;
                  Vec3 look = this.lastLookDirection.lerp(lookDirection, f);
                  RayCaster.RaycastResult raycastResult = RayCaster.raycast(entity.level(), start, look, false, this.includeFluids, this.includeNonSolid);
                  if (raycastResult != null) {
                     Vec3 withinBlock = raycastResult.getPositionWithinBlock();
                     smartDDASkipFrom(entity.level(), this.lastPosition, withinBlock, this.includeNonSolid, consumer);
                     this.lastPosition = withinBlock;
                  }
               }

               this.lastLookDirection = lookDirection;
            } else {
               RayCaster.RaycastResult raycastResult = RayCaster.raycast(entity.level(), start, lookDirection, false, this.includeFluids, this.includeNonSolid);
               if (raycastResult != null) {
                  this.lastLookDirection = lookDirection;
                  this.lastPosition = raycastResult.getPositionWithinBlock();
                  consumer.accept(raycastResult.blockPos().getX(), raycastResult.blockPos().getY(), raycastResult.blockPos().getZ());
               }
            }
         }
      }
   }

   public static void smartDDASkipFrom(Level world, Vec3 from, Vec3 to, boolean includeNonSolid, TriIntConsumer consumer) {
      int fromX = (int)Math.floor(from.x);
      int fromY = (int)Math.floor(from.y);
      int fromZ = (int)Math.floor(from.z);
      int toX = (int)Math.floor(to.x);
      int toY = (int)Math.floor(to.y);
      int toZ = (int)Math.floor(to.z);
      if (fromX != toX || fromY != toY || fromZ != toZ) {
         int dx = Math.abs(toX - fromX);
         int dy = Math.abs(toY - fromY);
         int dz = Math.abs(toZ - fromZ);
         if (dx + dy + dz == 1) {
            consumer.accept(toX, toY, toZ);
         } else {
            if (dx <= 1 && dy <= 1 && dz <= 1) {
               Vec3 ray = to.subtract(from).normalize();
               double deltaDistX = Math.abs(1.0 / ray.x);
               double deltaDistY = Math.abs(1.0 / ray.y);
               double deltaDistZ = Math.abs(1.0 / ray.z);
               double sideDistX = (ray.x > 0.0 ? 1.0 - from.x + fromX : from.x - fromX) * deltaDistX;
               double sideDistY = (ray.y > 0.0 ? 1.0 - from.y + fromY : from.y - fromY) * deltaDistY;
               double sideDistZ = (ray.z > 0.0 ? 1.0 - from.z + fromZ : from.z - fromZ) * deltaDistZ;
               if (Double.isNaN(sideDistX)) {
                  sideDistX = Double.POSITIVE_INFINITY;
               }

               if (Double.isNaN(sideDistY)) {
                  sideDistY = Double.POSITIVE_INFINITY;
               }

               if (Double.isNaN(sideDistZ)) {
                  sideDistZ = Double.POSITIVE_INFINITY;
               }

               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               Axis[][] axisPermutations;
               if (dx == 0) {
                  if (sideDistY < sideDistZ) {
                     axisPermutations = AXIS_PERMUTATIONS_NO_X_PRIO_Y;
                  } else {
                     axisPermutations = AXIS_PERMUTATIONS_NO_X_PRIO_Z;
                  }
               } else if (dy == 0) {
                  if (sideDistX < sideDistZ) {
                     axisPermutations = AXIS_PERMUTATIONS_NO_Y_PRIO_X;
                  } else {
                     axisPermutations = AXIS_PERMUTATIONS_NO_Y_PRIO_Z;
                  }
               } else if (dz == 0) {
                  if (sideDistX < sideDistY) {
                     axisPermutations = AXIS_PERMUTATIONS_NO_Z_PRIO_X;
                  } else {
                     axisPermutations = AXIS_PERMUTATIONS_NO_Z_PRIO_Y;
                  }
               } else {
                  axisPermutations = AXIS_PERMUTATIONS[calculatePriorityIndex(sideDistX, sideDistY, sideDistZ)];
               }

               for (Axis[] axisPermutation : axisPermutations) {
                  int x = fromX;
                  int y = fromY;
                  int z = fromZ;

                  for (Axis axis : axisPermutation) {
                     switch (axis) {
                        case X:
                           x = toX;
                           break;
                        case Y:
                           y = toY;
                           break;
                        case Z:
                           z = toZ;
                     }

                     if (x == toX && y == toY && z == toZ) {
                        x = fromX;
                        y = fromY;
                        z = fromZ;

                        for (Axis axis2 : axisPermutation) {
                           switch (axis2) {
                              case X:
                                 x = toX;
                                 break;
                              case Y:
                                 y = toY;
                                 break;
                              case Z:
                                 z = toZ;
                           }

                           consumer.accept(x, y, z);
                        }

                        return;
                     }

                     BlockState blockState = world.getBlockState(mutableBlockPos.set(x, y, z));
                     if (blockState.isAir() || !includeNonSolid && blockState.canBeReplaced()) {
                        break;
                     }
                  }
               }
            }

            Rasterization3D.ddaSkipFrom(from, to, consumer);
         }
      }
   }

   private static Axis[][][] computeAxisPermutationArray() {
      Axis[][] axes = new Axis[][]{
         {Axis.X, Axis.Y, Axis.Z},
         {Axis.X, Axis.Z, Axis.Y},
         {Axis.Y, Axis.X, Axis.Z},
         {Axis.Y, Axis.Z, Axis.X},
         {Axis.Z, Axis.X, Axis.Y},
         {Axis.Z, Axis.Y, Axis.X}
      };
      Axis[][][] allAxisByPriority = new Axis[6][][];

      for (int prio = 0; prio < 6; prio++) {
         Axis[][] sortedAxes = Arrays.copyOf(axes, 6);
         Axis[] order = axes[prio];
         Arrays.sort(sortedAxes, Comparator.comparingInt(triple -> {
            int priority = 0;
            if (triple[0] == order[0]) {
               priority -= 1000;
            } else if (triple[0] == order[1]) {
               priority -= 100;
            }

            if (triple[1] == order[0]) {
               priority -= 10;
            } else if (triple[1] == order[1]) {
               priority--;
            }

            return priority;
         }));
         allAxisByPriority[prio] = sortedAxes;
      }

      return allAxisByPriority;
   }

   private static int calculatePriorityIndex(double sideDistX, double sideDistY, double sideDistZ) {
      if (sideDistX < sideDistY && sideDistX < sideDistZ) {
         return sideDistY < sideDistZ ? 0 : 1;
      } else if (sideDistY < sideDistZ) {
         return sideDistX < sideDistZ ? 2 : 3;
      } else {
         return sideDistX < sideDistY ? 4 : 5;
      }
   }
}
