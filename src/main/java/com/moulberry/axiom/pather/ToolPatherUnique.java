package com.moulberry.axiom.pather;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import com.moulberry.axiomclientapi.pathers.BallShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class ToolPatherUnique implements com.moulberry.axiomclientapi.pathers.ToolPatherUnique {
   private boolean realtimeUpdates;
   private final int[] sphere;
   private final int[][] cardinal;
   @Nullable
   private final PositionSet positions;
   @Nullable
   private final ChunkedBooleanRegion preview;
   private MutableBlockPos lastBlockPosition = null;
   private Vec3 lastPosition = null;
   private Vec3 lastLookDirection = null;
   private boolean includeNonSolid = true;

   public ToolPatherUnique(int radius, BallShape ballShape) {
      this(radius, true, ballShape);
   }

   public ToolPatherUnique(int radius, boolean realtimeUpdates, BallShape ballShape) {
      this.realtimeUpdates = realtimeUpdates;
      if (realtimeUpdates) {
         this.positions = new PositionSet();
         this.preview = null;
      } else {
         this.positions = null;
         this.preview = new ChunkedBooleanRegion();
      }

      radius = Math.max(0, radius);
      IntList sphere = new IntArrayList();
      List<IntList> cardinal = List.of(new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList(), new IntArrayList());
      float radiusSq = (radius + 0.5F) * (radius + 0.5F);

      for (int x = -radius - 1; x <= radius + 1; x++) {
         for (int y = -radius - 1; y <= radius + 1; y++) {
            for (int z = -radius - 1; z <= radius + 1; z++) {
               float distanceSq = ballShape.distanceSq(x, y, z);
               if (distanceSq <= radiusSq) {
                  sphere.add(x);
                  sphere.add(y);
                  sphere.add(z);
               } else {
                  for (Direction direction : Direction.values()) {
                     int x2 = x - direction.getStepX();
                     int y2 = y - direction.getStepY();
                     int z2 = z - direction.getStepZ();
                     float distanceSq2 = ballShape.distanceSq(x2, y2, z2);
                     if (distanceSq2 <= radiusSq) {
                        IntList list = cardinal.get(direction.get3DDataValue());
                        list.add(x2);
                        list.add(y2);
                        list.add(z2);
                     }
                  }
               }
            }
         }
      }

      this.sphere = sphere.toIntArray();
      this.cardinal = new int[6][];

      for (int i = 0; i < 6; i++) {
         this.cardinal[i] = cardinal.get(i).toIntArray();
      }
   }

   public boolean update(TriIntConsumer consumer) {
      Entity entity = Minecraft.getInstance().player;
      if (entity == null || entity != Minecraft.getInstance().cameraEntity) {
         return false;
      } else if (!EditorUI.isActive()) {
         return false;
      } else if (EditorUI.isMovingCamera()) {
         return false;
      } else {
         Vec3 currentLookDirection = EditorUI.getMouseLookVector();
         if (currentLookDirection == null) {
            return false;
         } else {
            Vec3 start = entity.getEyePosition();
            boolean includeFluids = Tool.defaultIncludeFluids();
            if (this.lastLookDirection != null && this.lastBlockPosition != null) {
               int oldCount = this.preview != null ? this.preview.count() : this.positions.count();
               List<Vec2> mousePositions = EditorUI.imguiGlfw.getCapturedInterframeMousePositions();

               for (int mouseI = 0; mouseI < mousePositions.size(); mouseI++) {
                  Vec2 mousePosition = mousePositions.get(mouseI);
                  Vec3 lookDirection = EditorUI.getMouseLookVector(mousePosition.x, mousePosition.y);
                  if (lookDirection != null) {
                     double dot = this.lastLookDirection.dot(lookDirection);
                     double angleChange = Math.toDegrees(Math.acos(dot));
                     int steps = 1;
                     if (angleChange > 1.0) {
                        steps = (int)Math.ceil(angleChange);
                     }

                     for (int i = 1; i <= steps; i++) {
                        float f = (float)i / steps;
                        Vec3 look = this.lastLookDirection.lerp(lookDirection, f);
                        RayCaster.RaycastResult raycastResult = RayCaster.raycast(entity.level(), start, look, false, includeFluids, true);
                        if (raycastResult != null) {
                           Vec3 withinBlock = raycastResult.getPositionWithinBlock();
                           if (raycastResult.getBlockPos().equals(this.lastBlockPosition)) {
                              this.lastPosition = withinBlock;
                           } else {
                              ToolPatherPoint.smartDDASkipFrom(entity.level(), this.lastPosition, withinBlock, this.includeNonSolid, (x, y, z) -> {
                                 int dx = x - this.lastBlockPosition.getX();
                                 int dy = y - this.lastBlockPosition.getY();
                                 int dz = z - this.lastBlockPosition.getZ();
                                 int[] offsets;
                                 if (dx == 0) {
                                    if (dy == 0) {
                                       if (dz == 0) {
                                          return;
                                       }

                                       if (dz == 1) {
                                          offsets = this.cardinal[3];
                                       } else {
                                          if (dz != -1) {
                                             throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                          }

                                          offsets = this.cardinal[2];
                                       }
                                    } else {
                                       if (dz != 0) {
                                          throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                       }

                                       if (dy == 1) {
                                          offsets = this.cardinal[1];
                                       } else {
                                          if (dy != -1) {
                                             throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                          }

                                          offsets = this.cardinal[0];
                                       }
                                    }
                                 } else {
                                    if (dy != 0 || dz != 0) {
                                       throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                    }

                                    if (dx == 1) {
                                       offsets = this.cardinal[5];
                                    } else {
                                       if (dx != -1) {
                                          throw new FaultyImplementationError("Not a direction: dx=" + dx + " dy=" + dy + " dz=" + dz);
                                       }

                                       offsets = this.cardinal[4];
                                    }
                                 }

                                 if (this.realtimeUpdates) {
                                    if (this.positions == null) {
                                       throw new FaultyImplementationError();
                                    }

                                    for (int j = 0; j < offsets.length; j += 3) {
                                       int offsetX = offsets[j] + x;
                                       int offsetY = offsets[j + 1] + y;
                                       int offsetZ = offsets[j + 2] + z;
                                       if (this.positions.add(offsetX, offsetY, offsetZ)) {
                                          consumer.accept(offsetX, offsetY, offsetZ);
                                       }
                                    }
                                 } else {
                                    if (this.preview == null) {
                                       throw new FaultyImplementationError();
                                    }

                                    for (int jx = 0; jx < offsets.length; jx += 3) {
                                       int offsetX = offsets[jx] + x;
                                       int offsetY = offsets[jx + 1] + y;
                                       int offsetZ = offsets[jx + 2] + z;
                                       this.preview.add(offsetX, offsetY, offsetZ);
                                    }
                                 }

                                 this.lastBlockPosition.set(x, y, z);
                              });
                              this.lastPosition = withinBlock;
                           }
                        }
                     }

                     this.lastLookDirection = lookDirection;
                  }
               }

               int newCount = this.preview != null ? this.preview.count() : this.positions.count();
               if (BuildConfig.DEBUG && newCount < oldCount) {
                  throw new FaultyImplementationError();
               } else {
                  return newCount != oldCount;
               }
            } else if (BuildConfig.DEBUG && this.preview != null ? this.preview.count() == 0 : this.positions.isEmpty()) {
               RayCaster.RaycastResult raycastResult = RayCaster.raycast(
                  entity.level(), start, currentLookDirection, false, includeFluids, this.includeNonSolid
               );
               if (raycastResult != null) {
                  this.lastLookDirection = currentLookDirection;
                  this.lastBlockPosition = raycastResult.getBlockPos().mutable();
                  this.lastPosition = raycastResult.getPositionWithinBlock();
                  if (this.realtimeUpdates) {
                     if (this.positions == null) {
                        throw new FaultyImplementationError();
                     }

                     for (int ix = 0; ix < this.sphere.length; ix += 3) {
                        int x = this.sphere[ix] + this.lastBlockPosition.getX();
                        int y = this.sphere[ix + 1] + this.lastBlockPosition.getY();
                        int z = this.sphere[ix + 2] + this.lastBlockPosition.getZ();
                        if (this.positions.add(x, y, z)) {
                           consumer.accept(x, y, z);
                        }
                     }
                  } else {
                     if (this.preview == null) {
                        throw new FaultyImplementationError();
                     }

                     for (int ixx = 0; ixx < this.sphere.length; ixx += 3) {
                        int x = this.sphere[ixx] + this.lastBlockPosition.getX();
                        int y = this.sphere[ixx + 1] + this.lastBlockPosition.getY();
                        int z = this.sphere[ixx + 2] + this.lastBlockPosition.getZ();
                        this.preview.add(x, y, z);
                     }
                  }
               }

               return true;
            } else {
               throw new FaultyImplementationError();
            }
         }
      }
   }

   public void finish(TriIntConsumer consumer) {
      if (!this.realtimeUpdates) {
         this.realtimeUpdates = true;
         if (this.preview == null) {
            throw new FaultyImplementationError();
         } else {
            this.preview.forEach(consumer);
         }
      }
   }

   public void renderPreview(AxiomWorldRenderContext rc, int effects) {
      if (this.preview != null) {
         this.preview.render(rc, Vec3.ZERO, effects);
      }
   }

   public void close() {
      if (this.preview != null) {
         this.preview.close();
      }
   }
}
