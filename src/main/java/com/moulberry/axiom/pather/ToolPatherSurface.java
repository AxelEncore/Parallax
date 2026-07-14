package com.moulberry.axiom.pather;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.collections.Position2dToIntMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import com.moulberry.axiomclientapi.pathers.BallShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ToolPatherSurface implements com.moulberry.axiomclientapi.pathers.ToolPatherUnique {
   private final int[] sphere;
   private final int[][] cardinal;
   private final Position2dToIntMap heightMap = new Position2dToIntMap(Integer.MIN_VALUE);
   private final PositionSet checked = new PositionSet();
   private MaskContext maskContext = null;
   private MutableBlockPos lastBlockPosition = null;
   private Vec3 lastPosition = null;
   private Vec3 lastLookDirection = null;
   private final MaskElement maskElement;
   private final int surfaceDirection;
   private final AxisCycle axisCycle;
   private final AxisCycle axisCycleInverse;

   public ToolPatherSurface(int radius, BallShape ballShape, MaskElement mask, Direction direction) {
      radius = Math.max(0, radius);
      this.maskElement = mask;
      this.surfaceDirection = direction.getAxisDirection() == AxisDirection.NEGATIVE ? -1 : 1;
      this.axisCycle = AxisCycle.between(direction.getAxis(), Axis.Y);
      this.axisCycleInverse = this.axisCycle.inverse();
      IntList sphere = new IntArrayList();
      List<IntList> cardinal = List.of(new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList());
      float radiusSq = (radius + 0.5F) * (radius + 0.5F);

      for (int x = -radius; x <= radius; x++) {
         for (int z = -radius; z <= radius; z++) {
            for (int y = radius; y >= 0; y--) {
               float distanceSq = ballShape.distanceSq(x, y, z);
               if (distanceSq <= radiusSq) {
                  sphere.add(x);
                  sphere.add(y);
                  sphere.add(z);
                  if (x <= 0) {
                     IntList list = cardinal.get(0);
                     list.add(x);
                     list.add(y);
                     list.add(z);
                  }

                  if (z <= 0) {
                     IntList list = cardinal.get(1);
                     list.add(x);
                     list.add(y);
                     list.add(z);
                  }

                  if (x >= 0) {
                     IntList list = cardinal.get(2);
                     list.add(x);
                     list.add(y);
                     list.add(z);
                  }

                  if (z >= 0) {
                     IntList list = cardinal.get(3);
                     list.add(x);
                     list.add(y);
                     list.add(z);
                  }
                  break;
               }
            }
         }
      }

      this.sphere = sphere.toIntArray();
      this.cardinal = new int[4][];

      for (int i = 0; i < 4; i++) {
         this.cardinal[i] = cardinal.get(i).toIntArray();
      }
   }

   public boolean update(TriIntConsumer consumer) {
      Entity entity = Minecraft.getInstance().player;
      if (entity != null && entity == Minecraft.getInstance().cameraEntity) {
         Vec3 lookDirection = null;
         if (EditorUI.isActive()) {
            if (EditorUI.isMovingCamera()) {
               return false;
            }

            lookDirection = EditorUI.getMouseLookVector();
         } else if (Minecraft.getInstance().cameraEntity != null) {
            lookDirection = Minecraft.getInstance().cameraEntity.getLookAngle();
         }

         if (lookDirection == null) {
            return false;
         } else {
            Vec3 start = entity.getEyePosition();
            boolean includeFluids = Tool.defaultIncludeFluids();
            Level level = entity.level();
            if (this.maskContext == null) {
               this.maskContext = new MaskContext(level);
            }

            int xo = this.axisCycleInverse.cycle(0, this.surfaceDirection, 0, Axis.X);
            int yo = this.axisCycleInverse.cycle(0, this.surfaceDirection, 0, Axis.Y);
            int zo = this.axisCycleInverse.cycle(0, this.surfaceDirection, 0, Axis.Z);
            if (this.lastLookDirection != null && this.lastBlockPosition != null) {
               BooleanWrapper changed = new BooleanWrapper(false);
               double dot = this.lastLookDirection.dot(lookDirection);
               double angleChange = Math.toDegrees(Math.acos(dot));
               int steps = 1;
               if (angleChange > 1.0) {
                  steps = (int)Math.ceil(angleChange);
               }

               for (int i = 1; i <= steps; i++) {
                  float f = (float)i / steps;
                  Vec3 look = this.lastLookDirection.lerp(lookDirection, f);
                  RayCaster.RaycastResult raycastResult = RayCaster.raycast(level, start, look, false, includeFluids, false);
                  if (raycastResult != null) {
                     Vec3 withinBlock = raycastResult.getPositionWithinBlock();
                     if (raycastResult.getBlockPos().equals(this.lastBlockPosition)) {
                        this.lastPosition = withinBlock;
                     } else {
                        ToolPatherPoint.smartDDASkipFrom(
                           level,
                           this.lastPosition,
                           withinBlock,
                           false,
                           (x, yx, z) -> {
                              int rawDx = x - this.lastBlockPosition.getX();
                              int rawDy = yx - this.lastBlockPosition.getY();
                              int rawDz = z - this.lastBlockPosition.getZ();
                              int dx = this.axisCycleInverse.cycle(rawDx, rawDy, rawDz, Axis.X);
                              int dy = this.axisCycleInverse.cycle(rawDx, rawDy, rawDz, Axis.Y);
                              int dz = this.axisCycleInverse.cycle(rawDx, rawDy, rawDz, Axis.Z);
                              int[] offsets;
                              if (dy != 0) {
                                 offsets = this.sphere;
                              } else if (dx == 0) {
                                 if (dz == 0) {
                                    return;
                                 }

                                 if (dz == 1) {
                                    offsets = this.cardinal[3];
                                 } else {
                                    if (dz != -1) {
                                       throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                    }

                                    offsets = this.cardinal[1];
                                 }
                              } else {
                                 if (dz != 0) {
                                    throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                 }

                                 if (dx == 1) {
                                    offsets = this.cardinal[2];
                                 } else {
                                    if (dx != -1) {
                                       throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                    }

                                    offsets = this.cardinal[0];
                                 }
                              }

                              for (int j = 0; j < offsets.length; j += 3) {
                                 int a = offsets[j] + this.axisCycle.cycle(x, yx, z, Axis.X);
                                 int height = offsets[j + 1];
                                 int c = offsets[j + 2] + this.axisCycle.cycle(x, yx, z, Axis.Z);
                                 int lastB = this.axisCycle.cycle(x, yx, z, Axis.Y) * this.surfaceDirection;
                                 int fromB = lastB - height;
                                 int toB = lastB + height;
                                 int oldBx = this.heightMap.get(a, c);
                                 if (fromB < oldBx) {
                                    fromB = oldBx;
                                 }

                                 if (oldBx <= toB) {
                                    for (int bx = toB; bx >= fromB; bx--) {
                                       int x1x = this.axisCycleInverse.cycle(a, bx * this.surfaceDirection, c, Axis.X);
                                       int y1x = this.axisCycleInverse.cycle(a, bx * this.surfaceDirection, c, Axis.Y);
                                       int z1x = this.axisCycleInverse.cycle(a, bx * this.surfaceDirection, c, Axis.Z);
                                       if (this.checked.add(x1x, y1x, z1x)) {
                                          this.maskContext.reset();
                                          if (this.maskContext.getBlockState(x1x, y1x, z1x).blocksMotion()
                                             && !this.maskContext.getBlockState(x1x, y1x, z1x, xo, yo, zo).blocksMotion()
                                             && (this.maskElement == null || this.maskElement.test(this.maskContext, x1x, y1x, z1x))) {
                                             this.heightMap.put(a, c, bx);
                                             changed.value = true;
                                             consumer.accept(x1x, y1x, z1x);
                                             break;
                                          }
                                       }
                                    }
                                 }
                              }

                              this.lastBlockPosition.set(x, yx, z);
                           }
                        );
                        this.lastPosition = withinBlock;
                     }
                  }
               }

               this.lastLookDirection = lookDirection;
               return changed.value;
            } else {
               RayCaster.RaycastResult raycastResult = RayCaster.raycast(level, start, lookDirection, false, includeFluids, false);
               if (raycastResult != null) {
                  this.lastLookDirection = lookDirection;
                  this.lastBlockPosition = raycastResult.getBlockPos().mutable();
                  this.lastPosition = raycastResult.getPositionWithinBlock();
                  int x = this.lastBlockPosition.getX();
                  int y = this.lastBlockPosition.getY();
                  int z = this.lastBlockPosition.getZ();

                  for (int ix = 0; ix < this.sphere.length; ix += 3) {
                     int a = this.sphere[ix] + this.axisCycle.cycle(x, y, z, Axis.X);
                     int height = this.sphere[ix + 1];
                     int c = this.sphere[ix + 2] + this.axisCycle.cycle(x, y, z, Axis.Z);
                     int lastB = this.axisCycle.cycle(x, y, z, Axis.Y) * this.surfaceDirection;
                     int fromB = lastB - height;
                     int toB = lastB + height;
                     int oldB = this.heightMap.get(a, c);
                     if (fromB < oldB) {
                        fromB = oldB;
                     }

                     if (oldB <= toB) {
                        for (int b = toB; b >= fromB; b--) {
                           int x1 = this.axisCycleInverse.cycle(a, b * this.surfaceDirection, c, Axis.X);
                           int y1 = this.axisCycleInverse.cycle(a, b * this.surfaceDirection, c, Axis.Y);
                           int z1 = this.axisCycleInverse.cycle(a, b * this.surfaceDirection, c, Axis.Z);
                           if (this.checked.add(x1, y1, z1)) {
                              this.maskContext.reset();
                              if (this.maskContext.getBlockState(x1, y1, z1).blocksMotion()
                                 && !this.maskContext.getBlockState(x1, y1, z1, xo, yo, zo).blocksMotion()
                                 && (this.maskElement == null || this.maskElement.test(this.maskContext, x1, y1, z1))) {
                                 this.heightMap.put(a, c, b);
                                 consumer.accept(x1, y1, z1);
                                 break;
                              }
                           }
                        }
                     }
                  }
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }
}
