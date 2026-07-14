package com.moulberry.axiom.gizmo;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.PositionUtils;
import com.moulberry.axiom.utils.RenderHelper;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Intersectiond;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class Gizmo {
   private Vec3 targetPositionVec;
   private Vec3 interpFromPosition;
   private float interpAmount = 0.0F;
   private long lastTime = 0L;
   private float axisMarkerTime = 0.0F;
   private GizmoTarget clickedTarget = null;
   private GizmoTarget hoveredTarget = null;
   private Vec3 offsetFromBlockPos;
   public boolean enableRotation = false;
   public boolean yAxisRotationOnly = false;
   public boolean enableScale = false;
   public float rotationSnapRadians = (float) (Math.PI / 2);
   public boolean enableAxes = true;
   public Quaternionf localRotation = null;
   public int translationSnapping = 1;
   public float minVisualScale = 10.0F;
   public int centerColour = 16777215;
   private float lockedDistanceMultiplier = -1.0F;
   private boolean plusX = true;
   private boolean plusY = true;
   private boolean plusZ = true;
   private boolean targetPlusX = true;
   private boolean targetPlusY = true;
   private boolean targetPlusZ = true;
   private Gizmo.GizmoRotation rotation = null;
   private Gizmo.GizmoScale scale = null;
   private static final float SELECT_THRESHOLD = (float)Math.cos(Math.toRadians(1.5));
   private static final float TOO_CLOSE_THRESHOLD = (float)Math.cos(Math.toRadians(1.0));
   private static final float SUPER_TOO_CLOSE_THRESHOLD = (float)Math.cos(Math.toRadians(0.35F));

   public Gizmo(BlockPos target) {
      this(target, Vec3.ZERO);
   }

   public Gizmo(BlockPos target, Vec3 offsetFromBlockPos) {
      this.targetPositionVec = Vec3.atCenterOf(target);
      this.interpFromPosition = Vec3.atLowerCornerOf(target);
      this.offsetFromBlockPos = offsetFromBlockPos;
   }

   public Gizmo(Vec3 target) {
      this(target, Vec3.ZERO);
   }

   public Gizmo(Vec3 target, Vec3 offsetFromBlockPos) {
      this.targetPositionVec = target;
      this.interpFromPosition = target.subtract(0.5, 0.5, 0.5);
      this.offsetFromBlockPos = offsetFromBlockPos;
   }

   public BlockPos getTargetPosition() {
      return BlockPos.containing(this.targetPositionVec);
   }

   public Vec3 getTargetVec() {
      return this.targetPositionVec;
   }

   @Nullable
   public Quaternionf peekRotation() {
      return this.rotation == null ? null : this.rotation.toQuaternion();
   }

   @Nullable
   public Gizmo.GizmoRotation peekGizmoRotation() {
      return this.rotation;
   }

   public Gizmo.GizmoRotation popRotation() {
      if (!this.isGrabbed() && this.rotation != null) {
         Gizmo.GizmoRotation rotation = this.rotation;
         this.rotation = null;
         this.clickedTarget = null;
         return rotation;
      } else {
         return null;
      }
   }

   @Nullable
   public Gizmo.GizmoScale peekScale() {
      return this.scale;
   }

   public Gizmo.GizmoScale popScale() {
      if (!this.isGrabbed() && this.scale != null) {
         Gizmo.GizmoScale scale = this.scale;
         this.scale = null;
         this.clickedTarget = null;
         return scale;
      } else {
         return null;
      }
   }

   public boolean isGrabbed() {
      return this.clickedTarget != null;
   }

   public boolean isScaleGrabbed() {
      return this.clickedTarget instanceof GizmoTarget.Scale1D;
   }

   public boolean isHovered() {
      return this.hoveredTarget != null;
   }

   public boolean isCenterGrabbed() {
      return this.clickedTarget instanceof GizmoTarget.Move3D;
   }

   public boolean isCenterHovered() {
      return this.hoveredTarget instanceof GizmoTarget.Move3D;
   }

   public Vec3 snapPosition(Vec3 position, boolean increaseSnap, boolean snapX, boolean snapY, boolean snapZ) {
      if (Tool.isShiftDown()) {
         return position;
      } else {
         double x;
         if (snapX) {
            if (increaseSnap) {
               x = Math.floor(position.x);
            } else {
               x = (double)Math.round((position.x - 0.5) * this.translationSnapping) / this.translationSnapping;
            }
         } else {
            x = position.x - 0.5;
         }

         double y;
         if (snapY) {
            if (increaseSnap) {
               y = Math.floor(position.y);
            } else {
               y = (double)Math.round((position.y - 0.5) * this.translationSnapping) / this.translationSnapping;
            }
         } else {
            y = position.y - 0.5;
         }

         double z;
         if (snapZ) {
            if (increaseSnap) {
               z = Math.floor(position.z);
            } else {
               z = (double)Math.round((position.z - 0.5) * this.translationSnapping) / this.translationSnapping;
            }
         } else {
            z = position.z - 0.5;
         }

         return new Vec3(x + 0.5, y + 0.5, z + 0.5);
      }
   }

   public Vec3 getLowerCornerOfTargetPosition() {
      return new Vec3(this.targetPositionVec.x - 0.5, this.targetPositionVec.y - 0.5, this.targetPositionVec.z - 0.5);
   }

   public Vec3 getInterpPosition() {
      return this.interpFromPosition.lerp(this.getLowerCornerOfTargetPosition(), this.interpAmount).add(0.5, 0.5, 0.5).add(this.offsetFromBlockPos);
   }

   public void handleScroll(int xScroll, int yScroll, boolean isCtrlDown, Vec3 look) {
      if (look != null) {
         this.axisMarkerTime = 40.0F;
         if (this.clickedTarget == null) {
            Direction[] directions = PositionUtils.orderedByNearest(look);
            Direction mainDirection = directions[0];
            Direction secondaryDirection = findSecondaryAxis(directions);
            int translationSnapping = isCtrlDown ? 1 : this.translationSnapping;
            Vec3 newPosition = this.targetPositionVec
               .relative(mainDirection, (float)yScroll / translationSnapping)
               .relative(secondaryDirection, (float)xScroll / translationSnapping);
            this.moveToWithoutChangingAxisTarget(newPosition);
         } else if (this.clickedTarget instanceof GizmoTarget.Move3D move3D) {
            float translationSnapping = isCtrlDown ? 1.0F : this.translationSnapping;
            move3D.distance += yScroll / translationSnapping;
            if (move3D.distance < 1.0F) {
               move3D.distance = 1.0F;
            }
         }
      }
   }

   private void moveToWithoutChangingAxisTarget(Vec3 newPos) {
      if (!newPos.equals(this.targetPositionVec)) {
         this.interpFromPosition = this.interpFromPosition.lerp(this.getLowerCornerOfTargetPosition(), this.interpAmount);
         this.targetPositionVec = newPos;
         this.interpAmount = 0.0F;
      }
   }

   public void moveToVec(Vec3 newPosVec) {
      this.moveToWithoutChangingAxisTarget(newPosVec);
      if (this.clickedTarget instanceof GizmoTarget.Move1D move1D) {
         move1D.vector = move1D.vector.add(newPosVec.subtract(this.targetPositionVec));
      } else if (this.clickedTarget instanceof GizmoTarget.Move2D move2D) {
         move2D.origin = move2D.origin.add(newPosVec.subtract(this.targetPositionVec));
      }
   }

   public void moveToVecInstantly(Vec3 newPosVec) {
      this.moveToVec(newPosVec);
      this.interpAmount = 1.0F;
   }

   public void moveTo(BlockPos newPos) {
      this.moveToVec(Vec3.atCenterOf(newPos));
   }

   public void moveToInstantly(BlockPos newPos) {
      this.moveTo(newPos);
      this.interpAmount = 1.0F;
   }

   public void setAxisDirections(boolean plusX, boolean plusY, boolean plusZ) {
      this.targetPlusX = plusX;
      this.targetPlusY = plusY;
      this.targetPlusZ = plusZ;
   }

   public void setOffset(Vec3 offsetFromBlockPos) {
      this.interpFromPosition = this.interpFromPosition.lerp(this.getLowerCornerOfTargetPosition(), this.interpAmount);
      this.interpAmount = 0.0F;
      Vec3 delta = offsetFromBlockPos.subtract(this.offsetFromBlockPos);
      this.interpFromPosition = this.interpFromPosition.subtract(delta);
      this.offsetFromBlockPos = offsetFromBlockPos;
   }

   public void setOffsetInstantly(Vec3 offsetFromBlockPos) {
      this.offsetFromBlockPos = offsetFromBlockPos;
      this.interpAmount = 1.0F;
   }

   public Vec3 getOffset() {
      return this.offsetFromBlockPos;
   }

   private static Direction findSecondaryAxis(Direction[] directions) {
      for (Direction direction : directions) {
         switch (direction) {
            case NORTH:
               return Direction.WEST;
            case SOUTH:
               return Direction.EAST;
            case WEST:
               return Direction.SOUTH;
            case EAST:
               return Direction.NORTH;
         }
      }

      return Direction.NORTH;
   }

   public boolean leftClick() {
      if (this.hoveredTarget == null) {
         return false;
      } else {
         if (!this.enableAxes) {
            this.enableAxes = true;
         } else {
            this.clickedTarget = this.hoveredTarget;
         }

         return true;
      }
   }

   public void update(long time, Vec3 lookDirection, boolean holdingLmb, boolean pressingCtrl, boolean showHover) {
      LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
      if (this.enableAxes && EditorUI.isActive() && EditorUI.canProcessKeybinds) {
         Direction[] directions = Direction.orderedByNearest(player);
         Direction forwards = null;

         for (Direction direction : directions) {
            if (direction.getAxis() != Axis.Y) {
               forwards = direction;
               break;
            }
         }

         if (forwards != null) {
            if (Keybinds.NUDGE_FORWARDS.isPressed(true)) {
               this.moveToVecInstantly(this.getTargetVec().relative(forwards, 1.0));
            }

            if (Keybinds.NUDGE_BACKWARDS.isPressed(true)) {
               this.moveToVecInstantly(this.getTargetVec().relative(forwards.getOpposite(), 1.0));
            }

            if (Keybinds.NUDGE_RIGHT.isPressed(true)) {
               this.moveToVecInstantly(this.getTargetVec().relative(forwards.getClockWise(), 1.0));
            }

            if (Keybinds.NUDGE_LEFT.isPressed(true)) {
               this.moveToVecInstantly(this.getTargetVec().relative(forwards.getCounterClockWise(), 1.0));
            }
         }

         if (Keybinds.NUDGE_PLUS_Y.isPressed(true)) {
            this.moveToVecInstantly(this.getTargetVec().relative(Direction.UP, 1.0));
         }

         if (Keybinds.NUDGE_MINUS_Y.isPressed(true)) {
            this.moveToVecInstantly(this.getTargetVec().relative(Direction.DOWN, 1.0));
         }
      }

      if (!holdingLmb) {
         this.clickedTarget = null;
      }

      if (this.clickedTarget == null) {
         this.lockedDistanceMultiplier = -1.0F;
      } else if (this.lockedDistanceMultiplier < 0.0F) {
         this.lockedDistanceMultiplier = this.getDistanceMultiplier(player.getEyePosition());
      }

      if (this.clickedTarget != null && lookDirection != null) {
         if (this.clickedTarget instanceof GizmoTarget.Move3D move3D) {
            if (this.axisMarkerTime < 20.0F) {
               this.axisMarkerTime = 20.0F;
            }

            Vec3 offset = lookDirection.scale(move3D.distance);
            offset = player.getEyePosition().subtract(this.offsetFromBlockPos).add(offset);
            this.moveToWithoutChangingAxisTarget(this.snapPosition(offset, pressingCtrl, true, true, true));
         } else if (this.clickedTarget instanceof GizmoTarget.Move1D move1D && this.enableAxes) {
            if (this.axisMarkerTime < 20.0F) {
               this.axisMarkerTime = 20.0F;
            }

            Vec3 l1 = player.getEyePosition().subtract(this.offsetFromBlockPos);
            Vec3 l2 = l1.add(lookDirection.scale(2000.0));
            Vector3d intersection = new Vector3d();
            Intersectiond.findClosestPointsLineSegments(
               move1D.origin.x - move1D.vector.x * 500.0,
               move1D.origin.y - move1D.vector.y * 500.0,
               move1D.origin.z - move1D.vector.z * 500.0,
               move1D.origin.x + move1D.vector.x * 500.0,
               move1D.origin.y + move1D.vector.y * 500.0,
               move1D.origin.z + move1D.vector.z * 500.0,
               l1.x,
               l1.y,
               l1.z,
               l2.x,
               l2.y,
               l2.z,
               intersection,
               new Vector3d()
            );
            boolean snapX = move1D.clickedAxis == Axis.X;
            boolean snapY = move1D.clickedAxis == Axis.Y;
            boolean snapZ = move1D.clickedAxis == Axis.Z;
            this.moveToWithoutChangingAxisTarget(
               this.snapPosition(
                  new Vec3(intersection.x - move1D.vector.x, intersection.y - move1D.vector.y, intersection.z - move1D.vector.z),
                  pressingCtrl,
                  snapX,
                  snapY,
                  snapZ
               )
            );
         } else {
            label298:
            if (this.clickedTarget instanceof GizmoTarget.Move2D move2D && this.enableAxes) {
               if (this.axisMarkerTime < 20.0F) {
                  this.axisMarkerTime = 20.0F;
               }

               Vec3 l1 = player.getEyePosition();
               Vec3 l2 = l1.add(lookDirection.scale(2000.0));
               Vec3 targetPositionD = this.targetPositionVec.add(this.offsetFromBlockPos);
               Vector3d intersection = new Vector3d();
               switch (move2D.axis) {
                  case X:
                     Vector3d vector3dxx = new Vector3d(1.0, 0.0, 0.0);
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector3dxx);
                     }

                     double planeConstant = vector3dxx.x * targetPositionD.x + vector3dxx.y * targetPositionD.y + vector3dxx.z * targetPositionD.z;
                     if (!Intersectiond.intersectLineSegmentPlane(
                        l1.x, l1.y, l1.z, l2.x, l2.y, l2.z, vector3dxx.x, vector3dxx.y, vector3dxx.z, -planeConstant, intersection
                     )) {
                        break label298;
                     }
                     break;
                  case Y:
                     Vector3d vector3dx = new Vector3d(0.0, 1.0, 0.0);
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector3dx);
                     }

                     planeConstant = vector3dx.x * targetPositionD.x + vector3dx.y * targetPositionD.y + vector3dx.z * targetPositionD.z;
                     if (!Intersectiond.intersectLineSegmentPlane(
                        l1.x, l1.y, l1.z, l2.x, l2.y, l2.z, vector3dx.x, vector3dx.y, vector3dx.z, -planeConstant, intersection
                     )) {
                        break label298;
                     }
                     break;
                  case Z:
                     Vector3d vector3d = new Vector3d(0.0, 0.0, 1.0);
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector3d);
                     }

                     planeConstant = vector3d.x * targetPositionD.x + vector3d.y * targetPositionD.y + vector3d.z * targetPositionD.z;
                     if (!Intersectiond.intersectLineSegmentPlane(
                        l1.x, l1.y, l1.z, l2.x, l2.y, l2.z, vector3d.x, vector3d.y, vector3d.z, -planeConstant, intersection
                     )) {
                        break label298;
                     }
                     break;
                  default:
                     throw new IncompatibleClassChangeError();
               }

               boolean snapX = move2D.axis != Axis.X;
               boolean snapY = move2D.axis != Axis.Y;
               boolean snapZ = move2D.axis != Axis.Z;
               this.moveToWithoutChangingAxisTarget(
                  this.snapPosition(
                     new Vec3(
                        intersection.x - move2D.origin.x - this.offsetFromBlockPos.x,
                        intersection.y - move2D.origin.y - this.offsetFromBlockPos.y,
                        intersection.z - move2D.origin.z - this.offsetFromBlockPos.z
                     ),
                     pressingCtrl,
                     snapX,
                     snapY,
                     snapZ
                  )
               );
            } else if (this.clickedTarget instanceof GizmoTarget.Rotate rotate && this.enableAxes && this.enableRotation) {
               Vec3 l1 = player.getEyePosition().subtract(this.offsetFromBlockPos);
               Vec3 l2 = l1.add(lookDirection.scale(2000.0));
               Vec3 targetPositionD = this.targetPositionVec.add(this.offsetFromBlockPos);
               Vector3d intersection = new Vector3d();
               float tau = (float) (Math.PI * 2);
               float snap = pressingCtrl ? Math.max(this.rotationSnapRadians, (float) (Math.PI / 12)) : this.rotationSnapRadians;
               switch (rotate.axis) {
                  case X:
                     Vector3d vector3dxxxxx = new Vector3d(1.0, 0.0, 0.0);
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector3dxxxxx);
                     }

                     double planeConstant = vector3dxxxxx.x * targetPositionD.x + vector3dxxxxx.y * targetPositionD.y + vector3dxxxxx.z * targetPositionD.z;
                     if (Intersectiond.intersectLineSegmentPlane(
                        l1.x, l1.y, l1.z, l2.x, l2.y, l2.z, vector3dxxxxx.x, vector3dxxxxx.y, vector3dxxxxx.z, -planeConstant, intersection
                     )) {
                        Vector3d diffxx = new Vector3d(
                           intersection.x - targetPositionD.x, intersection.y - targetPositionD.y, intersection.z - targetPositionD.z
                        );
                        if (this.localRotation != null) {
                           this.localRotation.transformInverse(diffxx);
                        }

                        float angle = (float)Math.atan2(diffxx.z, diffxx.y);
                        if (rotate.firstTime) {
                           rotate.startAngle = angle;
                           rotate.firstTime = false;
                        } else {
                           float angleFromStartxx = angle - rotate.startAngle;
                           if (!Tool.isShiftDown()) {
                              angleFromStartxx = Math.round(angleFromStartxx / snap) * snap;
                           }

                           angleFromStartxx %= tau;
                           if (this.rotation != null && this.rotation.axis == Axis.X) {
                              if (this.rotation.radians <= -tau / 4.0F) {
                                 if (angleFromStartxx > 0.0F) {
                                    angleFromStartxx -= tau;
                                 }
                              } else if (this.rotation.radians >= tau / 4.0F) {
                                 if (angleFromStartxx < 0.0F) {
                                    angleFromStartxx += tau;
                                 }
                              } else {
                                 if (angleFromStartxx < -Math.PI) {
                                    angleFromStartxx += tau;
                                 }

                                 if (angleFromStartxx > Math.PI) {
                                    angleFromStartxx -= tau;
                                 }
                              }
                           }

                           this.rotation = new Gizmo.GizmoRotation(Axis.X, angleFromStartxx);
                        }
                     }
                     break;
                  case Y:
                     Vector3d vector3dxxxx = new Vector3d(0.0, 1.0, 0.0);
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector3dxxxx);
                     }

                     planeConstant = vector3dxxxx.x * targetPositionD.x + vector3dxxxx.y * targetPositionD.y + vector3dxxxx.z * targetPositionD.z;
                     if (Intersectiond.intersectLineSegmentPlane(
                        l1.x, l1.y, l1.z, l2.x, l2.y, l2.z, vector3dxxxx.x, vector3dxxxx.y, vector3dxxxx.z, -planeConstant, intersection
                     )) {
                        Vector3d diffx = new Vector3d(
                           intersection.x - targetPositionD.x, intersection.y - targetPositionD.y, intersection.z - targetPositionD.z
                        );
                        if (this.localRotation != null) {
                           this.localRotation.transformInverse(diffx);
                        }

                        float angle = (float)Math.atan2(diffx.x, diffx.z);
                        if (rotate.firstTime) {
                           rotate.startAngle = angle;
                           rotate.firstTime = false;
                        } else {
                           float angleFromStartx = angle - rotate.startAngle;
                           if (!Tool.isShiftDown()) {
                              angleFromStartx = Math.round(angleFromStartx / snap) * snap;
                           }

                           angleFromStartx %= tau;
                           if (this.rotation != null && this.rotation.axis == Axis.Y) {
                              if (this.rotation.radians <= -tau / 4.0F) {
                                 if (angleFromStartx > 0.0F) {
                                    angleFromStartx -= tau;
                                 }
                              } else if (this.rotation.radians >= tau / 4.0F) {
                                 if (angleFromStartx < 0.0F) {
                                    angleFromStartx += tau;
                                 }
                              } else {
                                 if (angleFromStartx < -Math.PI) {
                                    angleFromStartx += tau;
                                 }

                                 if (angleFromStartx > Math.PI) {
                                    angleFromStartx -= tau;
                                 }
                              }
                           }

                           this.rotation = new Gizmo.GizmoRotation(Axis.Y, angleFromStartx);
                        }
                     }
                     break;
                  case Z:
                     Vector3d vector3dxxx = new Vector3d(0.0, 0.0, 1.0);
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector3dxxx);
                     }

                     planeConstant = vector3dxxx.x * targetPositionD.x + vector3dxxx.y * targetPositionD.y + vector3dxxx.z * targetPositionD.z;
                     if (Intersectiond.intersectLineSegmentPlane(
                        l1.x, l1.y, l1.z, l2.x, l2.y, l2.z, vector3dxxx.x, vector3dxxx.y, vector3dxxx.z, -planeConstant, intersection
                     )) {
                        Vector3d diff = new Vector3d(intersection.x - targetPositionD.x, intersection.y - targetPositionD.y, intersection.z - targetPositionD.z);
                        if (this.localRotation != null) {
                           this.localRotation.transformInverse(diff);
                        }

                        float angle = (float)Math.atan2(diff.y, diff.x);
                        if (rotate.firstTime) {
                           rotate.startAngle = angle;
                           rotate.firstTime = false;
                        } else {
                           float angleFromStart = angle - rotate.startAngle;
                           if (!Tool.isShiftDown()) {
                              angleFromStart = Math.round(angleFromStart / snap) * snap;
                           }

                           angleFromStart %= tau;
                           if (this.rotation != null && this.rotation.axis == Axis.Z) {
                              if (this.rotation.radians < -tau / 4.0F) {
                                 if (angleFromStart > 0.0F) {
                                    angleFromStart -= tau;
                                 }
                              } else if (this.rotation.radians > tau / 4.0F) {
                                 if (angleFromStart < 0.0F) {
                                    angleFromStart += tau;
                                 }
                              } else {
                                 if (angleFromStart < -Math.PI) {
                                    angleFromStart += tau;
                                 }

                                 if (angleFromStart > Math.PI) {
                                    angleFromStart -= tau;
                                 }
                              }
                           }

                           this.rotation = new Gizmo.GizmoRotation(Axis.Z, angleFromStart);
                        }
                     }
               }
            } else if (this.clickedTarget instanceof GizmoTarget.Scale1D scale1D && this.enableAxes && this.enableScale) {
               Vec3 target = this.targetPositionVec;
               Vec3 l1 = player.getEyePosition().subtract(this.offsetFromBlockPos);
               Vec3 l2 = l1.add(lookDirection.scale(2000.0));
               Vector3d intersection = new Vector3d();
               Intersectiond.findClosestPointsLineSegments(
                  target.x - scale1D.vector.x * 500.0,
                  target.y - scale1D.vector.y * 500.0,
                  target.z - scale1D.vector.z * 500.0,
                  target.x + scale1D.vector.x * 500.0,
                  target.y + scale1D.vector.y * 500.0,
                  target.z + scale1D.vector.z * 500.0,
                  l1.x,
                  l1.y,
                  l1.z,
                  l2.x,
                  l2.y,
                  l2.z,
                  intersection,
                  new Vector3d()
               );
               double distanceToCenter = intersection.distance(target.x, target.y, target.z);
               this.scale = new Gizmo.GizmoScale(scale1D.clickedAxis, pressingCtrl, (float)(distanceToCenter / scale1D.clickedLength));
            } else {
               this.hoveredTarget = this.clickedTarget = null;
            }
         }
      } else if (showHover && lookDirection != null) {
         this.hoveredTarget = this.calculateAxisTarget(lookDirection);
         this.plusX = this.targetPlusX;
         this.plusY = this.targetPlusY;
         this.plusZ = this.targetPlusZ;
      } else if (lookDirection != null) {
         this.hoveredTarget = null;
         this.plusX = this.targetPlusX;
         this.plusY = this.targetPlusY;
         this.plusZ = this.targetPlusZ;
      }

      if (this.lastTime < time) {
         float delta = (float)(time - this.lastTime) / 1000000.0F / 50.0F;
         this.axisMarkerTime -= delta;
         if (this.axisMarkerTime < 0.0F) {
            this.axisMarkerTime = 0.0F;
         }

         this.interpAmount += delta;
         if (this.interpAmount > 1.0F) {
            this.interpAmount = 1.0F;
         }
      }

      this.lastTime = time;
   }

   public void render(AxiomWorldRenderContext rc, boolean pressedCtrl) {
      Vec3 renderPos = this.interpFromPosition.lerp(this.getLowerCornerOfTargetPosition(), this.interpAmount);
      renderPos = renderPos.add(this.offsetFromBlockPos);
      rc.poseStack().pushPose();
      rc.poseStack().translate(renderPos.x() + 0.5 - rc.x(), renderPos.y() + 0.5 - rc.y(), renderPos.z() + 0.5 - rc.z());
      if (this.localRotation != null) {
         rc.poseStack().mulPose(this.localRotation);
      }

      if (this.axisMarkerTime > 0.0F) {
         this.renderAxisMarkers(rc.poseStack());
      }

      this.renderPlacementHandles(rc, pressedCtrl);
      rc.poseStack().popPose();
   }

   private void renderAxisMarkers(PoseStack matrices) {
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      this.renderAxisMarker(matrices, bufferBuilder, 1, 0, 0);
      this.renderAxisMarker(matrices, bufferBuilder, 0, 1, 0);
      this.renderAxisMarker(matrices, bufferBuilder, 0, 0, 1);
      AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(provider.build());
   }

   private void renderAxisMarker(PoseStack matrices, BufferBuilder builder, int r, int g, int b) {
      float opacity = 1.0F;
      if (this.axisMarkerTime < 10.0F) {
         opacity = this.axisMarkerTime / 10.0F;
      }

      Pose pose = matrices.last();

      for (int i = -5; i <= 4; i++) {
         VersionUtilsClient.legacySetLineWidthIgnored(
            builder.addVertex(pose, i * 20 * r, i * 20 * g, i * 20 * b).setColor(r, g, b, 0.5F * opacity).setNormal(pose, r, g, b), RenderHelper.baseLineWidth
         );
         VersionUtilsClient.legacySetLineWidthIgnored(
            builder.addVertex(pose, (i + 1) * 20 * r, (i + 1) * 20 * g, (i + 1) * 20 * b).setColor(r, g, b, 0.5F * opacity).setNormal(pose, r, g, b),
            RenderHelper.baseLineWidth
         );
      }
   }

   private float getDistanceMultiplier(Vec3 cameraPosition) {
      if (this.lockedDistanceMultiplier > 0.5F) {
         return this.lockedDistanceMultiplier;
      } else {
         float distanceMultiplier = (float)cameraPosition.distanceTo(this.targetPositionVec);
         if (distanceMultiplier < this.minVisualScale) {
            distanceMultiplier = this.minVisualScale;
         }

         return distanceMultiplier / 20.0F;
      }
   }

   private void renderPlacementHandles(AxiomWorldRenderContext rc, boolean pressedCtrl) {
      PoseStack matrices = rc.poseStack();
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      float distanceMultiplier = this.getDistanceMultiplier(rc.position());
      float axisLen = 4.0F * distanceMultiplier;
      float boxSize = 0.3F * distanceMultiplier;
      if (this.enableAxes) {
         int mX = this.plusX ? 1 : -1;
         int mY = this.plusY ? 1 : -1;
         int mZ = this.plusZ ? 1 : -1;
         boolean clickedAny = this.clickedTarget != null;
         boolean click2DX = this.clickedTarget instanceof GizmoTarget.Move2D move2D && move2D.axis == Axis.X;
         boolean click2DY = this.clickedTarget instanceof GizmoTarget.Move2D move2Dx && move2Dx.axis == Axis.Y;
         boolean click2DZ = this.clickedTarget instanceof GizmoTarget.Move2D move2Dxx && move2Dxx.axis == Axis.Z;
         boolean hoverAxisX = this.hoveredTarget instanceof GizmoTarget.Move1D move1D && move1D.clickedAxis == Axis.X || click2DY || click2DZ;
         boolean hoverAxisY = this.hoveredTarget instanceof GizmoTarget.Move1D move1Dx && move1Dx.clickedAxis == Axis.Y || click2DX || click2DZ;
         boolean hoverAxisZ = this.hoveredTarget instanceof GizmoTarget.Move1D move1Dxx && move1Dxx.clickedAxis == Axis.Z || click2DX || click2DY;
         boolean clickAnyAxis = this.clickedTarget instanceof GizmoTarget.Move1D || this.clickedTarget instanceof GizmoTarget.Move2D;
         boolean hover2DX = this.hoveredTarget instanceof GizmoTarget.Move2D move2Dxxx && move2Dxxx.axis == Axis.X;
         boolean hover2DY = this.hoveredTarget instanceof GizmoTarget.Move2D move2Dxxxx && move2Dxxxx.axis == Axis.Y;
         boolean hover2DZ = this.hoveredTarget instanceof GizmoTarget.Move2D move2Dxxxxx && move2Dxxxxx.axis == Axis.Z;
         boolean clickAny2D = this.clickedTarget instanceof GizmoTarget.Move2D;
         boolean hoverRotX = this.hoveredTarget instanceof GizmoTarget.Rotate rotate && rotate.axis == Axis.X;
         boolean hoverRotY = this.hoveredTarget instanceof GizmoTarget.Rotate rotatex && rotatex.axis == Axis.Y;
         boolean hoverRotZ = this.hoveredTarget instanceof GizmoTarget.Rotate rotatexx && rotatexx.axis == Axis.Z;
         boolean clickAnyRot = this.clickedTarget instanceof GizmoTarget.Rotate;
         int UNSELECTED_RED = -1430323200;
         int UNSELECTED_GREEN = -1442791680;
         int UNSELECTED_BLUE = -1442840385;
         int SELECTED_RED = -26215;
         int SELECTED_GREEN = -4194369;
         int SELECTED_BLUE = -8355585;
         float LINE_WIDTH = RenderHelper.baseLineWidth * 1.5F;
         int colourAxisX = hoverAxisX ? -26215 : -1430323200;
         int colourAxisY = hoverAxisY ? -4194369 : -1442791680;
         int colourAxisZ = hoverAxisZ ? -8355585 : -1442840385;
         int colour2DX = hover2DX ? -26215 : -1430323200;
         int colour2DY = hover2DY ? -4194369 : -1442791680;
         int colour2DZ = hover2DZ ? -8355585 : -1442840385;
         int colourRotX = hoverRotX ? -26215 : -1430323200;
         int colourRotY = hoverRotY ? -4194369 : -1442791680;
         int colourRotZ = hoverRotZ ? -8355585 : -1442840385;
         Pose pose = matrices.last();
         if (clickAnyAxis || !clickedAny) {
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            AxiomRenderer.setLineWidthLegacy(LINE_WIDTH);
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, mX * boxSize, 0.0F, 0.0F).setColor(colourAxisX).setNormal(pose, 1.0F, 0.0F, 0.0F), LINE_WIDTH
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, mX * axisLen, 0.0F, 0.0F).setColor(colourAxisX).setNormal(pose, 1.0F, 0.0F, 0.0F), LINE_WIDTH
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, 0.0F, mY * boxSize, 0.0F).setColor(colourAxisY).setNormal(pose, 0.0F, 1.0F, 0.0F), LINE_WIDTH
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, 0.0F, mY * axisLen, 0.0F).setColor(colourAxisY).setNormal(pose, 0.0F, 1.0F, 0.0F), LINE_WIDTH
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, 0.0F, 0.0F, mZ * boxSize).setColor(colourAxisZ).setNormal(pose, 0.0F, 0.0F, 1.0F), LINE_WIDTH
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, 0.0F, 0.0F, mZ * axisLen).setColor(colourAxisZ).setNormal(pose, 0.0F, 0.0F, 1.0F), LINE_WIDTH
            );
            AxiomRenderPipelines.GIZMO_LINES.render(provider.build());
         }

         float coneLen = axisLen / 5.0F;
         float coneRadius = coneLen / 3.0F;
         if (clickAnyAxis || !clickedAny) {
            Shapes.drawCone(
               provider,
               pose.pose(),
               new Vec3(mX * axisLen, 0.0, 0.0),
               new Vec3(mX * (axisLen + coneLen), 0.0, 0.0),
               0,
               coneRadius,
               colourAxisX,
               AxiomRenderPipelines.GIZMO_POSITION_COLOR
            );
            Shapes.drawCone(
               provider,
               pose.pose(),
               new Vec3(0.0, mY * axisLen, 0.0),
               new Vec3(0.0, mY * (axisLen + coneLen), 0.0),
               1,
               coneRadius,
               colourAxisY,
               AxiomRenderPipelines.GIZMO_POSITION_COLOR
            );
            Shapes.drawCone(
               provider,
               pose.pose(),
               new Vec3(0.0, 0.0, mZ * axisLen),
               new Vec3(0.0, 0.0, mZ * (axisLen + coneLen)),
               2,
               coneRadius,
               colourAxisZ,
               AxiomRenderPipelines.GIZMO_POSITION_COLOR
            );
         }

         if (this.enableRotation && (clickAnyRot || !clickedAny)) {
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            AxiomRenderer.setLineWidthLegacy(LINE_WIDTH);
            Vec3 cameraPosition = Minecraft.getInstance().cameraEntity.getEyePosition();
            Vec3 target = this.targetPositionVec;
            Vec3 deltaToCamera = target.subtract(cameraPosition);
            Vec3 offsetTarget = target.add(deltaToCamera.scale(0.05));
            Vector3d intersection = new Vector3d();
            Vector3d vector1 = new Vector3d();
            Vector3d vector2 = new Vector3d();
            deltaToCamera = deltaToCamera.normalize();
            double planarK = offsetTarget.x * deltaToCamera.x + offsetTarget.y * deltaToCamera.y + offsetTarget.z * deltaToCamera.z;

            for (int t = 0; t < 32; t++) {
               float x1 = 5.0F * distanceMultiplier * Mth.sin((float)(t * Math.PI * 2.0 / 32.0));
               float x2 = 5.0F * distanceMultiplier * Mth.sin((float)((t + 1) * Math.PI * 2.0 / 32.0));
               float z1 = 5.0F * distanceMultiplier * Mth.cos((float)(t * Math.PI * 2.0 / 32.0));
               float z2 = 5.0F * distanceMultiplier * Mth.cos((float)((t + 1) * Math.PI * 2.0 / 32.0));
               float length = (float)Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
               if ((!clickAnyRot || hoverRotX) && !this.yAxisRotationOnly) {
                  vector1.set(0.0, x1, z1);
                  vector2.set(0.0, x2, z2);
                  if (this.localRotation != null) {
                     this.localRotation.transform(vector1);
                     this.localRotation.transform(vector2);
                  }

                  boolean firstPointInside = clickAnyRot
                     || (target.x + vector1.x) * deltaToCamera.x + (target.y + vector1.y) * deltaToCamera.y + (target.z + vector1.z) * deltaToCamera.z
                        < planarK;
                  boolean secondPointInside = clickAnyRot
                     || (target.x + vector2.x) * deltaToCamera.x + (target.y + vector2.y) * deltaToCamera.y + (target.z + vector1.z) * deltaToCamera.z
                        < planarK;
                  if (firstPointInside) {
                     if (secondPointInside) {
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, x1, z1).setColor(colourRotX).setNormal(pose, 0.0F, (x2 - x1) / length, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, x2, z2).setColor(colourRotX).setNormal(pose, 0.0F, (x2 - x1) / length, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                     } else {
                        boolean intersected = Intersectiond.intersectLineSegmentPlane(
                           target.x + vector1.x,
                           target.y + vector1.y,
                           target.z + vector1.z,
                           target.x + vector2.x,
                           target.y + vector2.y,
                           target.z + vector2.z,
                           deltaToCamera.x,
                           deltaToCamera.y,
                           deltaToCamera.z,
                           -planarK,
                           intersection
                        );
                        if (intersected) {
                           intersection.sub(target.x, target.y, target.z);
                           if (this.localRotation != null) {
                              this.localRotation.transformInverse(intersection);
                           }

                           float intersectY = (float)intersection.y;
                           float intersectZ = (float)intersection.z;
                           VersionUtilsClient.legacySetLineWidthIgnored(
                              bufferBuilder.addVertex(pose, 0.0F, x1, z1).setColor(colourRotX).setNormal(pose, 0.0F, (x2 - x1) / length, (z2 - z1) / length),
                              LINE_WIDTH
                           );
                           VersionUtilsClient.legacySetLineWidthIgnored(
                              bufferBuilder.addVertex(pose, 0.0F, intersectY, intersectZ)
                                 .setColor(colourRotX)
                                 .setNormal(pose, 0.0F, (x2 - x1) / length, (z2 - z1) / length),
                              LINE_WIDTH
                           );
                        }
                     }
                  } else if (secondPointInside) {
                     boolean intersected = Intersectiond.intersectLineSegmentPlane(
                        target.x + vector1.x,
                        target.y + vector1.y,
                        target.z + vector1.z,
                        target.x + vector2.x,
                        target.y + vector2.y,
                        target.z + vector2.z,
                        deltaToCamera.x,
                        deltaToCamera.y,
                        deltaToCamera.z,
                        -planarK,
                        intersection
                     );
                     if (intersected) {
                        intersection.sub(target.x, target.y, target.z);
                        if (this.localRotation != null) {
                           this.localRotation.transformInverse(intersection);
                        }

                        float intersectY = (float)intersection.y;
                        float intersectZ = (float)intersection.z;
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, intersectY, intersectZ)
                              .setColor(colourRotX)
                              .setNormal(pose, 0.0F, (x2 - x1) / length, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, x2, z2).setColor(colourRotX).setNormal(pose, 0.0F, (x2 - x1) / length, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                     }
                  }
               }

               if (!clickAnyRot || hoverRotY) {
                  vector1.set(x1, 0.0, z1);
                  vector2.set(x2, 0.0, z2);
                  if (this.localRotation != null) {
                     this.localRotation.transform(vector1);
                     this.localRotation.transform(vector2);
                  }

                  boolean firstPointInside = clickAnyRot
                     || (target.x + vector1.x) * deltaToCamera.x + (target.y + vector1.y) * deltaToCamera.y + (target.z + vector1.z) * deltaToCamera.z
                        < planarK;
                  boolean secondPointInside = clickAnyRot
                     || (target.x + vector2.x) * deltaToCamera.x + (target.y + vector2.y) * deltaToCamera.y + (target.z + vector1.z) * deltaToCamera.z
                        < planarK;
                  if (firstPointInside) {
                     if (secondPointInside) {
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, x1, 0.0F, z1).setColor(colourRotY).setNormal(pose, (x2 - x1) / length, 0.0F, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, x2, 0.0F, z2).setColor(colourRotY).setNormal(pose, (x2 - x1) / length, 0.0F, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                     } else {
                        boolean intersected = Intersectiond.intersectLineSegmentPlane(
                           target.x + vector1.x,
                           target.y + vector1.y,
                           target.z + vector1.z,
                           target.x + vector2.x,
                           target.y + vector2.y,
                           target.z + vector2.z,
                           deltaToCamera.x,
                           deltaToCamera.y,
                           deltaToCamera.z,
                           -planarK,
                           intersection
                        );
                        if (intersected) {
                           intersection.sub(target.x, target.y, target.z);
                           if (this.localRotation != null) {
                              this.localRotation.transformInverse(intersection);
                           }

                           float intersectX = (float)intersection.x;
                           float intersectZ = (float)intersection.z;
                           VersionUtilsClient.legacySetLineWidthIgnored(
                              bufferBuilder.addVertex(pose, x1, 0.0F, z1).setColor(colourRotY).setNormal(pose, (x2 - x1) / length, 0.0F, (z2 - z1) / length),
                              LINE_WIDTH
                           );
                           VersionUtilsClient.legacySetLineWidthIgnored(
                              bufferBuilder.addVertex(pose, intersectX, 0.0F, intersectZ)
                                 .setColor(colourRotY)
                                 .setNormal(pose, (x2 - x1) / length, 0.0F, (z2 - z1) / length),
                              LINE_WIDTH
                           );
                        }
                     }
                  } else if (secondPointInside) {
                     boolean intersected = Intersectiond.intersectLineSegmentPlane(
                        target.x + vector1.x,
                        target.y + vector1.y,
                        target.z + vector1.z,
                        target.x + vector2.x,
                        target.y + vector2.y,
                        target.z + vector2.z,
                        deltaToCamera.x,
                        deltaToCamera.y,
                        deltaToCamera.z,
                        -planarK,
                        intersection
                     );
                     if (intersected) {
                        intersection.sub(target.x, target.y, target.z);
                        if (this.localRotation != null) {
                           this.localRotation.transformInverse(intersection);
                        }

                        float intersectX = (float)intersection.x;
                        float intersectZ = (float)intersection.z;
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, intersectX, 0.0F, intersectZ)
                              .setColor(colourRotY)
                              .setNormal(pose, (x2 - x1) / length, 0.0F, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, x2, 0.0F, z2).setColor(colourRotY).setNormal(pose, (x2 - x1) / length, 0.0F, (z2 - z1) / length),
                           LINE_WIDTH
                        );
                     }
                  }
               }

               if ((!clickAnyRot || hoverRotZ) && !this.yAxisRotationOnly) {
                  vector1.set(x1, z1, 0.0);
                  vector2.set(x2, z2, 0.0);
                  if (this.localRotation != null) {
                     this.localRotation.transform(vector1);
                     this.localRotation.transform(vector2);
                  }

                  boolean firstPointInside = clickAnyRot
                     || (target.x + vector1.x) * deltaToCamera.x + (target.y + vector1.y) * deltaToCamera.y + (target.z + vector1.z) * deltaToCamera.z
                        < planarK;
                  boolean secondPointInside = clickAnyRot
                     || (target.x + vector2.x) * deltaToCamera.x + (target.y + vector2.y) * deltaToCamera.y + (target.z + vector1.z) * deltaToCamera.z
                        < planarK;
                  if (firstPointInside) {
                     if (secondPointInside) {
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, x1, z1, 0.0F).setColor(colourRotZ).setNormal(pose, (x2 - x1) / length, (z2 - z1) / length, 0.0F),
                           LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, x2, z2, 0.0F).setColor(colourRotZ).setNormal(pose, (x2 - x1) / length, (z2 - z1) / length, 0.0F),
                           LINE_WIDTH
                        );
                     } else {
                        boolean intersected = Intersectiond.intersectLineSegmentPlane(
                           target.x + vector1.x,
                           target.y + vector1.y,
                           target.z + vector1.z,
                           target.x + vector2.x,
                           target.y + vector2.y,
                           target.z + vector2.z,
                           deltaToCamera.x,
                           deltaToCamera.y,
                           deltaToCamera.z,
                           -planarK,
                           intersection
                        );
                        if (intersected) {
                           intersection.sub(target.x, target.y, target.z);
                           if (this.localRotation != null) {
                              this.localRotation.transformInverse(intersection);
                           }

                           float intersectX = (float)intersection.x;
                           float intersectY = (float)intersection.y;
                           VersionUtilsClient.legacySetLineWidthIgnored(
                              bufferBuilder.addVertex(pose, x1, z1, 0.0F).setColor(colourRotZ).setNormal(pose, (x2 - x1) / length, (z2 - z1) / length, 0.0F),
                              LINE_WIDTH
                           );
                           VersionUtilsClient.legacySetLineWidthIgnored(
                              bufferBuilder.addVertex(pose, intersectX, intersectY, 0.0F)
                                 .setColor(colourRotZ)
                                 .setNormal(pose, (x2 - x1) / length, (z2 - z1) / length, 0.0F),
                              LINE_WIDTH
                           );
                        }
                     }
                  } else if (secondPointInside) {
                     boolean intersected = Intersectiond.intersectLineSegmentPlane(
                        target.x + vector1.x,
                        target.y + vector1.y,
                        target.z + vector1.z,
                        target.x + vector2.x,
                        target.y + vector2.y,
                        target.z + vector2.z,
                        deltaToCamera.x,
                        deltaToCamera.y,
                        deltaToCamera.z,
                        -planarK,
                        intersection
                     );
                     if (intersected) {
                        intersection.sub(target.x, target.y, target.z);
                        if (this.localRotation != null) {
                           this.localRotation.transformInverse(intersection);
                        }

                        float intersectX = (float)intersection.x;
                        float intersectY = (float)intersection.y;
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, intersectX, intersectY, 0.0F)
                              .setColor(colourRotZ)
                              .setNormal(pose, (x2 - x1) / length, (z2 - z1) / length, 0.0F),
                           LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, x2, z2, 0.0F).setColor(colourRotZ).setNormal(pose, (x2 - x1) / length, (z2 - z1) / length, 0.0F),
                           LINE_WIDTH
                        );
                     }
                  }
               }
            }

            AxiomRenderPipelines.GIZMO_LINES.render(provider.build());
         }

         if (this.enableRotation && clickAnyRot && this.rotation != null) {
            GizmoTarget.Rotate rotatexxx = (GizmoTarget.Rotate)this.clickedTarget;
            BufferBuilder bufferBuilder = provider.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bufferBuilder.addVertex(pose, 0.0F, 0.0F, 0.0F).setColor(0);
            float tau = (float) (Math.PI * 2);
            float start = rotatexxx.startAngle;
            float end = rotatexxx.startAngle + this.rotation.radians;
            if (end < start) {
               float temp = start;
               start = end;
               end = temp;
            }

            int divisions = Math.max(32, (int)(tau / this.rotationSnapRadians));
            int endT = Math.round(end / tau * divisions);

            for (int t = Math.round(start / tau * divisions); t < endT; t++) {
               float x1x = 5.0F * distanceMultiplier * Mth.sin((float)(t * Math.PI * 2.0 / divisions));
               float x2x = 5.0F * distanceMultiplier * Mth.sin((float)((t + 1) * Math.PI * 2.0 / divisions));
               float z1x = 5.0F * distanceMultiplier * Mth.cos((float)(t * Math.PI * 2.0 / divisions));
               float z2x = 5.0F * distanceMultiplier * Mth.cos((float)((t + 1) * Math.PI * 2.0 / divisions));
               if (hoverRotX) {
                  bufferBuilder.addVertex(pose, 0.0F, z1x, x1x).setColor(colourRotX);
                  bufferBuilder.addVertex(pose, 0.0F, z2x, x2x).setColor(colourRotX);
               } else if (hoverRotY) {
                  bufferBuilder.addVertex(pose, x1x, 0.0F, z1x).setColor(colourRotY);
                  bufferBuilder.addVertex(pose, x2x, 0.0F, z2x).setColor(colourRotY);
               } else if (hoverRotZ) {
                  bufferBuilder.addVertex(pose, z1x, x1x, 0.0F).setColor(colourRotZ);
                  bufferBuilder.addVertex(pose, z2x, x2x, 0.0F).setColor(colourRotZ);
               }
            }

            AxiomRenderPipelines.GIZMO_POSITION_COLOR.render(provider.build());
         }

         if (clickAny2D || !clickedAny) {
            BufferBuilder bufferBuilder = provider.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bufferBuilder.addVertex(pose, 0.0F, mY * boxSize * 5.5F, mZ * boxSize * 5.5F).setColor(colour2DX);
            bufferBuilder.addVertex(pose, 0.0F, mY * boxSize * 5.5F, mZ * boxSize * 8.5F).setColor(colour2DX);
            bufferBuilder.addVertex(pose, 0.0F, mY * boxSize * 8.5F, mZ * boxSize * 8.5F).setColor(colour2DX);
            bufferBuilder.addVertex(pose, 0.0F, mY * boxSize * 8.5F, mZ * boxSize * 5.5F).setColor(colour2DX);
            bufferBuilder.addVertex(pose, mX * boxSize * 5.5F, 0.0F, mZ * boxSize * 5.5F).setColor(colour2DY);
            bufferBuilder.addVertex(pose, mX * boxSize * 5.5F, 0.0F, mZ * boxSize * 8.5F).setColor(colour2DY);
            bufferBuilder.addVertex(pose, mX * boxSize * 8.5F, 0.0F, mZ * boxSize * 8.5F).setColor(colour2DY);
            bufferBuilder.addVertex(pose, mX * boxSize * 8.5F, 0.0F, mZ * boxSize * 5.5F).setColor(colour2DY);
            bufferBuilder.addVertex(pose, mX * boxSize * 5.5F, mY * boxSize * 5.5F, 0.0F).setColor(colour2DZ);
            bufferBuilder.addVertex(pose, mX * boxSize * 5.5F, mY * boxSize * 8.5F, 0.0F).setColor(colour2DZ);
            bufferBuilder.addVertex(pose, mX * boxSize * 8.5F, mY * boxSize * 8.5F, 0.0F).setColor(colour2DZ);
            bufferBuilder.addVertex(pose, mX * boxSize * 8.5F, mY * boxSize * 5.5F, 0.0F).setColor(colour2DZ);
            AxiomRenderPipelines.GIZMO_POSITION_COLOR.render(provider.build());
         }

         boolean clickAnyScale = this.clickedTarget instanceof GizmoTarget.Scale1D;
         if (this.enableScale && (clickAnyScale || !clickedAny)) {
            if (this.clickedTarget instanceof GizmoTarget.Scale1D scale1D && this.scale != null) {
               BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
               AxiomRenderer.setLineWidthLegacy(LINE_WIDTH);
               float end = (axisLen + coneLen + axisLen / 5.0F) * this.scale.scale - boxSize;
               if (pressedCtrl) {
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, mX * boxSize, 0.0F, 0.0F).setColor(colourAxisX).setNormal(pose, 1.0F, 0.0F, 0.0F), LINE_WIDTH
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, mX * end, 0.0F, 0.0F).setColor(colourAxisX).setNormal(pose, 1.0F, 0.0F, 0.0F), LINE_WIDTH
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, 0.0F, mY * boxSize, 0.0F).setColor(colourAxisY).setNormal(pose, 0.0F, 1.0F, 0.0F), LINE_WIDTH
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, 0.0F, mY * end, 0.0F).setColor(colourAxisY).setNormal(pose, 0.0F, 1.0F, 0.0F), LINE_WIDTH
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, 0.0F, 0.0F, mZ * boxSize).setColor(colourAxisZ).setNormal(pose, 0.0F, 0.0F, 1.0F), LINE_WIDTH
                  );
                  VersionUtilsClient.legacySetLineWidthIgnored(
                     bufferBuilder.addVertex(pose, 0.0F, 0.0F, mZ * end).setColor(colourAxisZ).setNormal(pose, 0.0F, 0.0F, 1.0F), LINE_WIDTH
                  );
               } else {
                  switch (scale1D.clickedAxis) {
                     case X:
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, mX * boxSize, 0.0F, 0.0F).setColor(colourAxisX).setNormal(pose, 1.0F, 0.0F, 0.0F), LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, mX * end, 0.0F, 0.0F).setColor(colourAxisX).setNormal(pose, 1.0F, 0.0F, 0.0F), LINE_WIDTH
                        );
                        break;
                     case Y:
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, mY * boxSize, 0.0F).setColor(colourAxisY).setNormal(pose, 0.0F, 1.0F, 0.0F), LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, mY * end, 0.0F).setColor(colourAxisY).setNormal(pose, 0.0F, 1.0F, 0.0F), LINE_WIDTH
                        );
                        break;
                     case Z:
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, 0.0F, mZ * boxSize).setColor(colourAxisZ).setNormal(pose, 0.0F, 0.0F, 1.0F), LINE_WIDTH
                        );
                        VersionUtilsClient.legacySetLineWidthIgnored(
                           bufferBuilder.addVertex(pose, 0.0F, 0.0F, mZ * end).setColor(colourAxisZ).setNormal(pose, 0.0F, 0.0F, 1.0F), LINE_WIDTH
                        );
                  }
               }

               AxiomRenderPipelines.GIZMO_LINES.render(provider.build());
            }

            BufferBuilder bufferBuilder = provider.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            if (this.clickedTarget instanceof GizmoTarget.Scale1D scale1D && this.scale != null) {
               boolean x = pressedCtrl || scale1D.clickedAxis == Axis.X;
               boolean y = pressedCtrl || scale1D.clickedAxis == Axis.Y;
               boolean z = pressedCtrl || scale1D.clickedAxis == Axis.Z;
               if (x) {
                  matrices.pushPose();
                  matrices.translate(mX * (axisLen + coneLen + axisLen / 5.0F) * this.scale.scale, 0.0F, 0.0F);
                  drawBox(bufferBuilder, matrices.last().pose(), boxSize, -65536);
                  matrices.popPose();
               }

               if (y) {
                  matrices.pushPose();
                  matrices.translate(0.0F, mY * (axisLen + coneLen + axisLen / 5.0F) * this.scale.scale, 0.0F);
                  drawBox(bufferBuilder, matrices.last().pose(), boxSize, -16711936);
                  matrices.popPose();
               }

               if (z) {
                  matrices.pushPose();
                  matrices.translate(0.0F, 0.0F, mZ * (axisLen + coneLen + axisLen / 5.0F) * this.scale.scale);
                  drawBox(bufferBuilder, matrices.last().pose(), boxSize, -16776961);
                  matrices.popPose();
               }
            } else {
               int colourX = this.hoveredTarget instanceof GizmoTarget.Scale1D scale1D && scale1D.clickedAxis == Axis.X ? -65536 : -2130771968;
               int colourY = this.hoveredTarget instanceof GizmoTarget.Scale1D scale1Dx && scale1Dx.clickedAxis == Axis.Y ? -16711936 : -2147418368;
               int colourZ = this.hoveredTarget instanceof GizmoTarget.Scale1D scale1Dxx && scale1Dxx.clickedAxis == Axis.Z ? -16776961 : -2147483393;
               matrices.pushPose();
               matrices.translate(mX * (axisLen + coneLen + axisLen / 5.0F), 0.0F, 0.0F);
               drawBox(bufferBuilder, matrices.last().pose(), boxSize, colourX);
               matrices.popPose();
               matrices.pushPose();
               matrices.translate(0.0F, mY * (axisLen + coneLen + axisLen / 5.0F), 0.0F);
               drawBox(bufferBuilder, matrices.last().pose(), boxSize, colourY);
               matrices.popPose();
               matrices.pushPose();
               matrices.translate(0.0F, 0.0F, mZ * (axisLen + coneLen + axisLen / 5.0F));
               drawBox(bufferBuilder, matrices.last().pose(), boxSize, colourZ);
               matrices.popPose();
            }

            MeshData meshData = bufferBuilder.build();
            if (meshData != null) {
               AxiomRenderPipelines.GIZMO_POSITION_COLOR_WITH_BLENDING.render(meshData);
            }
         }
      }

      BufferBuilder bufferBuilderx = provider.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      drawBox(
         bufferBuilderx,
         matrices.last().pose(),
         boxSize,
         this.hoveredTarget instanceof GizmoTarget.Move3D ? 0xFF000000 | this.centerColour : -2147483648 | this.centerColour
      );
      MeshData meshData = bufferBuilderx.build();
      if (meshData != null) {
         AxiomRenderPipelines.GIZMO_POSITION_COLOR_WITH_BLENDING.render(meshData);
      }
   }

   private GizmoTarget calculateAxisTarget(Vec3 lookDirection) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return null;
      } else {
         Vec3 targetPositionD = this.targetPositionVec.add(this.offsetFromBlockPos);
         float distanceMultiplier = this.getDistanceMultiplier(player.getEyePosition());
         float axisLen = 4.0F * distanceMultiplier;
         float boxSize = 0.32F * distanceMultiplier;
         Vec3 b1 = targetPositionD.add(-boxSize, -boxSize, -boxSize);
         Vec3 b2 = targetPositionD.add(boxSize, boxSize, boxSize);
         Vec3 l1 = player.getEyePosition();
         Vec3 l2 = l1.add(lookDirection.scale(2000.0));
         Vector3d r1 = new Vector3d(l1.x - targetPositionD.x, l1.y - targetPositionD.y, l1.z - targetPositionD.z);
         if (this.localRotation != null) {
            this.localRotation.transformInverse(r1);
         }

         r1.add(targetPositionD.x, targetPositionD.y, targetPositionD.z);
         Vector3d r2 = new Vector3d(l2.x - targetPositionD.x, l2.y - targetPositionD.y, l2.z - targetPositionD.z);
         if (this.localRotation != null) {
            this.localRotation.transformInverse(r2);
         }

         r2.add(targetPositionD.x, targetPositionD.y, targetPositionD.z);
         int mX = this.plusX ? 1 : -1;
         int mY = this.plusY ? 1 : -1;
         int mZ = this.plusZ ? 1 : -1;
         if (Intersectiond.intersectLineSegmentAab(r1.x, r1.y, r1.z, r2.x, r2.y, r2.z, b1.x, b1.y, b1.z, b2.x, b2.y, b2.z, new Vector2d()) != -1) {
            float distance = (float)Math.sqrt(player.distanceToSqr(targetPositionD.add(0.0, -player.getEyeHeight(), 0.0)));
            return new GizmoTarget.Move3D(distance);
         } else {
            if (this.enableAxes) {
               if (this.enableScale) {
                  Axis axis = null;
                  float offset = axisLen + 2.0F * axisLen / 5.0F;
                  if (Intersectiond.intersectLineSegmentAab(
                        r1.x, r1.y, r1.z, r2.x, r2.y, r2.z, b1.x + mX * offset, b1.y, b1.z, b2.x + mX * offset, b2.y, b2.z, new Vector2d()
                     )
                     != -1) {
                     axis = Axis.X;
                  } else if (Intersectiond.intersectLineSegmentAab(
                        r1.x, r1.y, r1.z, r2.x, r2.y, r2.z, b1.x, b1.y + mY * offset, b1.z, b2.x, b2.y + mY * offset, b2.z, new Vector2d()
                     )
                     != -1) {
                     axis = Axis.Y;
                  } else if (Intersectiond.intersectLineSegmentAab(
                        r1.x, r1.y, r1.z, r2.x, r2.y, r2.z, b1.x, b1.y, b1.z + mZ * offset, b2.x, b2.y, b2.z + mZ * offset, new Vector2d()
                     )
                     != -1) {
                     axis = Axis.Z;
                  }

                  if (axis != null) {
                     Vector3f vector3f = new Vector3f(axis.choose(1, 0, 0), axis.choose(0, 1, 0), axis.choose(0, 0, 1));
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector3f);
                     }

                     Vector3d intersection = new Vector3d();
                     Intersectiond.findClosestPointsLineSegments(
                        targetPositionD.x - vector3f.x * 500.0F,
                        targetPositionD.y - vector3f.y * 500.0F,
                        targetPositionD.z - vector3f.z * 500.0F,
                        targetPositionD.x + vector3f.x * 500.0F,
                        targetPositionD.y + vector3f.y * 500.0F,
                        targetPositionD.z + vector3f.z * 500.0F,
                        l1.x,
                        l1.y,
                        l1.z,
                        l2.x,
                        l2.y,
                        l2.z,
                        intersection,
                        new Vector3d()
                     );
                     double distanceToCenter = intersection.distance(targetPositionD.x, targetPositionD.y, targetPositionD.z);
                     return new GizmoTarget.Scale1D(new Vec3(vector3f), (float)distanceToCenter, axis);
                  }
               }

               float mX1 = this.plusX ? 5.5F : -8.5F;
               float mY1 = this.plusY ? 5.5F : -8.5F;
               float mZ1 = this.plusZ ? 5.5F : -8.5F;
               float mX2 = this.plusX ? 8.5F : -5.5F;
               float mY2 = this.plusY ? 8.5F : -5.5F;
               float mZ2 = this.plusZ ? 8.5F : -5.5F;
               b1 = targetPositionD.add(0.0, mY1 * boxSize, mZ1 * boxSize);
               b2 = targetPositionD.add(0.0, mY2 * boxSize, mZ2 * boxSize);
               Vector2d result = new Vector2d();
               if (Intersectiond.intersectLineSegmentAab(r1.x, r1.y, r1.z, r2.x, r2.y, r2.z, b1.x, b1.y, b1.z, b2.x, b2.y, b2.z, result) != -1) {
                  Vec3 origin = l1.add(l2.subtract(l1).multiply(result.x, result.x, result.x)).subtract(targetPositionD);
                  return new GizmoTarget.Move2D(origin, Axis.X);
               }

               b1 = targetPositionD.add(mX1 * boxSize, 0.0, mZ1 * boxSize);
               b2 = targetPositionD.add(mX2 * boxSize, 0.0, mZ2 * boxSize);
               if (Intersectiond.intersectLineSegmentAab(r1.x, r1.y, r1.z, r2.x, r2.y, r2.z, b1.x, b1.y, b1.z, b2.x, b2.y, b2.z, result) != -1) {
                  Vec3 origin = l1.add(l2.subtract(l1).multiply(result.x, result.x, result.x)).subtract(targetPositionD);
                  return new GizmoTarget.Move2D(origin, Axis.Y);
               }

               b1 = targetPositionD.add(mX1 * boxSize, mY1 * boxSize, 0.0);
               b2 = targetPositionD.add(mX2 * boxSize, mY2 * boxSize, 0.0);
               if (Intersectiond.intersectLineSegmentAab(r1.x, r1.y, r1.z, r2.x, r2.y, r2.z, b1.x, b1.y, b1.z, b2.x, b2.y, b2.z, result) != -1) {
                  Vec3 origin = l1.add(l2.subtract(l1).multiply(result.x, result.x, result.x)).subtract(targetPositionD);
                  return new GizmoTarget.Move2D(origin, Axis.Z);
               }

               float closestDot = 0.0F;
               Vector3d closestIntersectionDir = null;
               Vector3d xIntersection = new Vector3d();
               Vector3d yIntersection = new Vector3d();
               Vector3d zIntersection = new Vector3d();
               int intersectionId = -1;
               Vector3d intersection = null;
               Axis clickedAxis = null;
               Vector3d vector = new Vector3d((this.plusX ? 1.3F : -1.3F) * axisLen, 0.0, 0.0);
               if (this.localRotation != null) {
                  this.localRotation.transform(vector);
               }

               Intersectiond.findClosestPointsLineSegments(
                  targetPositionD.x,
                  targetPositionD.y,
                  targetPositionD.z,
                  targetPositionD.x + vector.x,
                  targetPositionD.y + vector.y,
                  targetPositionD.z + vector.z,
                  l1.x,
                  l1.y,
                  l1.z,
                  l2.x,
                  l2.y,
                  l2.z,
                  xIntersection,
                  new Vector3d()
               );
               Vector3d xIntersectionDir = xIntersection.sub(l1.x, l1.y, l1.z, new Vector3d()).normalize();
               float dotX = (float)xIntersectionDir.dot(lookDirection.x, lookDirection.y, lookDirection.z);
               if (dotX >= SELECT_THRESHOLD) {
                  intersection = new Vector3d(xIntersection);
                  closestIntersectionDir = xIntersectionDir;
                  closestDot = dotX;
                  intersectionId = 0;
                  clickedAxis = Axis.X;
               }

               vector.set(0.0, (this.plusY ? 1.3F : -1.3F) * axisLen, 0.0);
               if (this.localRotation != null) {
                  this.localRotation.transform(vector);
               }

               Intersectiond.findClosestPointsLineSegments(
                  targetPositionD.x,
                  targetPositionD.y,
                  targetPositionD.z,
                  targetPositionD.x + vector.x,
                  targetPositionD.y + vector.y,
                  targetPositionD.z + vector.z,
                  l1.x,
                  l1.y,
                  l1.z,
                  l2.x,
                  l2.y,
                  l2.z,
                  yIntersection,
                  new Vector3d()
               );
               Vector3d yIntersectionDir = yIntersection.sub(l1.x, l1.y, l1.z, new Vector3d()).normalize();
               float dotY = (float)yIntersectionDir.dot(lookDirection.x, lookDirection.y, lookDirection.z);
               if (this.shouldSelectHandle(closestIntersectionDir, intersection, closestDot, yIntersectionDir, yIntersection, dotY, l1)) {
                  intersection = new Vector3d(yIntersection);
                  closestIntersectionDir = yIntersectionDir;
                  closestDot = dotY;
                  intersectionId = 0;
                  clickedAxis = Axis.Y;
               }

               vector.set(0.0, 0.0, (this.plusZ ? 1.3F : -1.3F) * axisLen);
               if (this.localRotation != null) {
                  this.localRotation.transform(vector);
               }

               Intersectiond.findClosestPointsLineSegments(
                  targetPositionD.x,
                  targetPositionD.y,
                  targetPositionD.z,
                  targetPositionD.x + vector.x,
                  targetPositionD.y + vector.y,
                  targetPositionD.z + vector.z,
                  l1.x,
                  l1.y,
                  l1.z,
                  l2.x,
                  l2.y,
                  l2.z,
                  zIntersection,
                  new Vector3d()
               );
               Vector3d zIntersectionDir = zIntersection.sub(l1.x, l1.y, l1.z, new Vector3d()).normalize();
               float dotZ = (float)zIntersectionDir.dot(lookDirection.x, lookDirection.y, lookDirection.z);
               if (this.shouldSelectHandle(closestIntersectionDir, intersection, closestDot, zIntersectionDir, zIntersection, dotZ, l1)) {
                  intersection = new Vector3d(zIntersection);
                  closestIntersectionDir = zIntersectionDir;
                  closestDot = dotZ;
                  intersectionId = 0;
                  clickedAxis = Axis.Z;
               }

               if (this.enableRotation) {
                  double xIntersectionMin = Double.MAX_VALUE;
                  double yIntersectionMin = Double.MAX_VALUE;
                  double zIntersectionMin = Double.MAX_VALUE;
                  Vector3d xIntersectionNew = new Vector3d();
                  Vector3d yIntersectionNew = new Vector3d();
                  Vector3d zIntersectionNew = new Vector3d();
                  Vector3d vector1 = new Vector3d();
                  Vector3d vector2 = new Vector3d();
                  Vec3 cameraPosition = Minecraft.getInstance().cameraEntity.getEyePosition();
                  Vec3 target = this.targetPositionVec;
                  Vec3 deltaToCamera = target.subtract(cameraPosition);
                  Vec3 offsetTarget = target.add(deltaToCamera.scale(0.05));
                  deltaToCamera = deltaToCamera.normalize();
                  double planarK = offsetTarget.x * deltaToCamera.x + offsetTarget.y * deltaToCamera.y + offsetTarget.z * deltaToCamera.z;

                  for (int t = 0; t < 32; t++) {
                     float x1 = 5.0F * distanceMultiplier * Mth.sin((float)(t * Math.PI * 2.0 / 32.0));
                     float x2 = 5.0F * distanceMultiplier * Mth.sin((float)((t + 1) * Math.PI * 2.0 / 32.0));
                     float z1 = 5.0F * distanceMultiplier * Mth.cos((float)(t * Math.PI * 2.0 / 32.0));
                     float z2 = 5.0F * distanceMultiplier * Mth.cos((float)((t + 1) * Math.PI * 2.0 / 32.0));
                     if (!this.yAxisRotationOnly) {
                        vector1.set(0.0, x1, z1);
                        vector2.set(0.0, x2, z2);
                        if (this.localRotation != null) {
                           this.localRotation.transform(vector1);
                           this.localRotation.transform(vector2);
                        }

                        double xIntersectionDist = Intersectiond.findClosestPointsLineSegments(
                           targetPositionD.x + vector1.x,
                           targetPositionD.y + vector1.y,
                           targetPositionD.z + vector1.z,
                           targetPositionD.x + vector2.x,
                           targetPositionD.y + vector2.y,
                           targetPositionD.z + vector2.z,
                           l1.x,
                           l1.y,
                           l1.z,
                           l2.x,
                           l2.y,
                           l2.z,
                           xIntersectionNew,
                           new Vector3d()
                        );
                        if (xIntersectionNew.x * deltaToCamera.x + xIntersectionNew.y * deltaToCamera.y + xIntersectionNew.z * deltaToCamera.z < planarK
                           && xIntersectionDist < xIntersectionMin) {
                           xIntersectionMin = xIntersectionDist;
                           xIntersection = new Vector3d(xIntersectionNew);
                        }
                     }

                     vector1.set(x1, 0.0, z1);
                     vector2.set(x2, 0.0, z2);
                     if (this.localRotation != null) {
                        this.localRotation.transform(vector1);
                        this.localRotation.transform(vector2);
                     }

                     double yIntersectionDist = Intersectiond.findClosestPointsLineSegments(
                        targetPositionD.x + vector1.x,
                        targetPositionD.y + vector1.y,
                        targetPositionD.z + vector1.z,
                        targetPositionD.x + vector2.x,
                        targetPositionD.y + vector2.y,
                        targetPositionD.z + vector2.z,
                        l1.x,
                        l1.y,
                        l1.z,
                        l2.x,
                        l2.y,
                        l2.z,
                        yIntersectionNew,
                        new Vector3d()
                     );
                     if (yIntersectionNew.x * deltaToCamera.x + yIntersectionNew.y * deltaToCamera.y + yIntersectionNew.z * deltaToCamera.z < planarK
                        && yIntersectionDist < yIntersectionMin) {
                        yIntersectionMin = yIntersectionDist;
                        yIntersection = new Vector3d(yIntersectionNew);
                     }

                     if (!this.yAxisRotationOnly) {
                        vector1.set(x1, z1, 0.0);
                        vector2.set(x2, z2, 0.0);
                        if (this.localRotation != null) {
                           this.localRotation.transform(vector1);
                           this.localRotation.transform(vector2);
                        }

                        double zIntersectionDist = Intersectiond.findClosestPointsLineSegments(
                           targetPositionD.x + vector1.x,
                           targetPositionD.y + vector1.y,
                           targetPositionD.z + vector1.z,
                           targetPositionD.x + vector2.x,
                           targetPositionD.y + vector2.y,
                           targetPositionD.z + vector2.z,
                           l1.x,
                           l1.y,
                           l1.z,
                           l2.x,
                           l2.y,
                           l2.z,
                           zIntersectionNew,
                           new Vector3d()
                        );
                        if (zIntersectionNew.x * deltaToCamera.x + zIntersectionNew.y * deltaToCamera.y + zIntersectionNew.z * deltaToCamera.z < planarK
                           && zIntersectionDist < zIntersectionMin) {
                           zIntersectionMin = zIntersectionDist;
                           zIntersection = new Vector3d(zIntersectionNew);
                        }
                     }
                  }

                  Vector3d toIntersection = xIntersection.sub(l1.x, l1.y, l1.z, new Vector3d()).normalize();
                  float dot = (float)toIntersection.dot(lookDirection.x, lookDirection.y, lookDirection.z);
                  if (this.shouldSelectHandle(closestIntersectionDir, intersection, closestDot, toIntersection, xIntersection, dot, l1)) {
                     intersection = xIntersection;
                     closestIntersectionDir = toIntersection;
                     closestDot = dot;
                     intersectionId = 1;
                  }

                  toIntersection = yIntersection.sub(l1.x, l1.y, l1.z, new Vector3d()).normalize();
                  dot = (float)toIntersection.dot(lookDirection.x, lookDirection.y, lookDirection.z);
                  if (this.shouldSelectHandle(closestIntersectionDir, intersection, closestDot, toIntersection, yIntersection, dot, l1)) {
                     intersection = yIntersection;
                     closestIntersectionDir = toIntersection;
                     closestDot = dot;
                     intersectionId = 2;
                  }

                  toIntersection = zIntersection.sub(l1.x, l1.y, l1.z, new Vector3d()).normalize();
                  dot = (float)toIntersection.dot(lookDirection.x, lookDirection.y, lookDirection.z);
                  if (this.shouldSelectHandle(closestIntersectionDir, intersection, closestDot, toIntersection, zIntersection, dot, l1)) {
                     intersection = zIntersection;
                     intersectionId = 3;
                  }
               }

               if (intersection != null) {
                  if (intersectionId == 0) {
                     return new GizmoTarget.Move1D(
                        new Vec3(intersection.x, intersection.y, intersection.z).subtract(targetPositionD), this.targetPositionVec, clickedAxis
                     );
                  }

                  Vector3d diff = new Vector3d(intersection.x - targetPositionD.x, intersection.y - targetPositionD.y, intersection.z - targetPositionD.z);
                  if (this.localRotation != null) {
                     this.localRotation.transformInverse(diff);
                  }

                  if (intersectionId == 1 && !this.yAxisRotationOnly) {
                     return new GizmoTarget.Rotate((float)Math.atan2(diff.z, diff.y), Axis.X);
                  }

                  if (intersectionId == 2) {
                     return new GizmoTarget.Rotate((float)Math.atan2(diff.x, diff.z), Axis.Y);
                  }

                  if (intersectionId == 3 && !this.yAxisRotationOnly) {
                     return new GizmoTarget.Rotate((float)Math.atan2(diff.y, diff.x), Axis.Z);
                  }
               }
            }

            return null;
         }
      }
   }

   private boolean shouldSelectHandle(
      Vector3d closestIntersectionDir,
      Vector3d closestIntersection,
      float closestDot,
      Vector3d newIntersectionDir,
      Vector3d newIntersection,
      float newDot,
      Vec3 origin
   ) {
      if (newDot < SELECT_THRESHOLD) {
         return false;
      } else {
         if (closestIntersectionDir != null) {
            double tooCloseDot = closestIntersectionDir.dot(newIntersectionDir);
            if (tooCloseDot > SUPER_TOO_CLOSE_THRESHOLD) {
               return newIntersection.distanceSquared(origin.x, origin.y, origin.z) < closestIntersection.distanceSquared(origin.x, origin.y, origin.z);
            }

            if (tooCloseDot > TOO_CLOSE_THRESHOLD) {
               double tooCloseFactor = (tooCloseDot - TOO_CLOSE_THRESHOLD) / (SUPER_TOO_CLOSE_THRESHOLD - TOO_CLOSE_THRESHOLD);
               if (newIntersection.distanceSquared(origin.x, origin.y, origin.z) < closestIntersection.distanceSquared(origin.x, origin.y, origin.z)) {
                  newDot = 1.0F - (1.0F - newDot) * (1.0F - (float)tooCloseFactor);
               } else {
                  closestDot = 1.0F - (1.0F - closestDot) * (1.0F - (float)tooCloseFactor);
               }
            }
         }

         return newDot > closestDot;
      }
   }

   private static void drawBox(BufferBuilder bufferBuilder, Matrix4f matrix4f, float boxSize, int boxColour) {
      float alpha = (boxColour >> 24 & 0xFF) / 255.0F;
      float red = (boxColour >> 16 & 0xFF) / 255.0F;
      float green = (boxColour >> 8 & 0xFF) / 255.0F;
      float blue = (boxColour & 0xFF) / 255.0F;
      float XF = 0.7F;
      float YPF = 1.0F;
      float YNF = 0.6F;
      float ZF = 0.87F;
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, -boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, -boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, -boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, -boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, -boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, -boxSize).setColor(red * 0.7F, green * 0.7F, blue * 0.7F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, -boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, -boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, -boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, -boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, -boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, -boxSize).setColor(red * 0.87F, green * 0.87F, blue * 0.87F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, -boxSize).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, -boxSize).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, boxSize).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, boxSize).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, -boxSize, boxSize).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, -boxSize, -boxSize).setColor(red * 0.6F, green * 0.6F, blue * 0.6F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, -boxSize).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, boxSize).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, -boxSize).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, boxSize).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, boxSize, boxSize, -boxSize).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
      bufferBuilder.addVertex(matrix4f, -boxSize, boxSize, boxSize).setColor(red * 1.0F, green * 1.0F, blue * 1.0F, alpha);
   }

   public record GizmoRotation(Axis axis, float radians) {
      public Quaternionf toQuaternion() {
         return switch (this.axis) {
            case X -> new Quaternionf().rotateX(this.radians);
            case Y -> new Quaternionf().rotateY(this.radians);
            case Z -> new Quaternionf().rotateZ(this.radians);
            default -> throw new IncompatibleClassChangeError();
         };
      }
   }

   public record GizmoScale(Axis axis, boolean scaleAll, float scale) {
      public float getScaleX() {
         return !this.scaleAll && this.axis != Axis.X ? 1.0F : this.scale;
      }

      public float getScaleY() {
         return !this.scaleAll && this.axis != Axis.Y ? 1.0F : this.scale;
      }

      public float getScaleZ() {
         return !this.scaleAll && this.axis != Axis.Z ? 1.0F : this.scale;
      }

      public float getScaleXZ() {
         return !this.scaleAll && this.axis != Axis.X && this.axis != Axis.Z ? 1.0F : this.scale;
      }

      public boolean isIdentity() {
         return Math.abs(this.scale - 1.0F) < 1.0E-5;
      }
   }
}
