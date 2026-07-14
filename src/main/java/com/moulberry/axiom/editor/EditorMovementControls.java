package com.moulberry.axiom.editor;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.hooks.WorldRenderHook;
import com.moulberry.axiom.tools.Tool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Intersectionf;
import org.joml.Vector3f;

public abstract sealed class EditorMovementControls
   permits EditorMovementControls.None,
   EditorMovementControls.Rotate,
   EditorMovementControls.RotateInvert,
   EditorMovementControls.Pan,
   EditorMovementControls.Arcball {
   public abstract boolean allowGameInputWhileCaptureKeyboard();

   public abstract boolean shouldStop(boolean var1);

   public abstract void update(double var1, double var3);

   public static EditorMovementControls none() {
      return EditorMovementControls.None.INSTANCE;
   }

   public static EditorMovementControls rotate() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return EditorMovementControls.None.INSTANCE;
      } else {
         return (EditorMovementControls)(Axiom.configuration.internal.invertCameraRotate
            ? new EditorMovementControls.RotateInvert(
               EditorUI.getMouseForwardsVector(), Minecraft.getInstance().player.getXRot(), Minecraft.getInstance().player.getYRot()
            )
            : new EditorMovementControls.Rotate(
               EditorUI.getMouseForwardsVector(), new Vec2(Minecraft.getInstance().player.getXRot(), Minecraft.getInstance().player.getYRot())
            ));
      }
   }

   public static EditorMovementControls pan() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return EditorMovementControls.None.INSTANCE;
      } else {
         Vec3 planePoint;
         if (WorldRenderHook.hasDistance) {
            Vec3 lookDelta = EditorUI.getMouseLookVector().scale(WorldRenderHook.distance);
            planePoint = player.getEyePosition().add(lookDelta);
         } else {
            RayCaster.RaycastResult result = Tool.raycastBlock();
            if (result == null) {
               return EditorMovementControls.None.INSTANCE;
            }

            planePoint = result.getLocation();
         }

         return new EditorMovementControls.Pan(player.position(), planePoint.toVector3f(), player.getLookAngle().toVector3f().negate());
      }
   }

   public static EditorMovementControls arcballFromDepth() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return EditorMovementControls.None.INSTANCE;
      } else {
         Vec3 look = EditorUI.getMouseLookVector();
         if (look == null) {
            return EditorMovementControls.None.INSTANCE;
         } else {
            Vec3 point = player.getEyePosition().add(look.scale(WorldRenderHook.distance));
            int minBuildHeight = player.level().getMinBuildHeight();
            if (point.y < minBuildHeight) {
               double yDelta = minBuildHeight - point.y;
               if (Math.abs(look.y) > 1.0E-4) {
                  double scale = yDelta / look.y;
                  point = point.add(look.scale(scale));
               }
            }

            Vec3 raycastStart = point.add(look.scale(-1.0));
            Vec3 raycastEnd = point.add(look.scale(1.0));
            BlockHitResult result = player.level().clip(new ClipContext(raycastStart, raycastEnd, Block.OUTLINE, Fluid.NONE, player));
            if (!result.isInside() && result.getType() == Type.BLOCK) {
               point = result.getLocation();
            }

            float distance = (float)point.distanceTo(player.getEyePosition());
            return new EditorMovementControls.Arcball(point, distance, true);
         }
      }
   }

   public static EditorMovementControls arcballFromRaycast() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return EditorMovementControls.None.INSTANCE;
      } else {
         Vec3 look = Axiom.configuration.internal.useCenterOfScreenForArcball ? player.getLookAngle() : EditorUI.getMouseLookVector();
         if (look == null) {
            return EditorMovementControls.None.INSTANCE;
         } else {
            RayCaster.RaycastResult result = RayCaster.raycast(Minecraft.getInstance().level, player.getEyePosition(), look, false, false);
            if (result != null) {
               Vec3 point = result.getLocation();
               float distance = (float)point.distanceTo(player.getEyePosition());
               return new EditorMovementControls.Arcball(point, distance, !Axiom.configuration.internal.useCenterOfScreenForArcball);
            } else {
               return EditorMovementControls.None.INSTANCE;
            }
         }
      }
   }

   protected static final class Arcball extends EditorMovementControls {
      private final Vec3 point;
      private float distance;
      private boolean snapLook;
      private float realXRot = 0.0F;
      private float realYRot = 0.0F;

      public Arcball(Vec3 point, float distance, boolean snapLook) {
         this.point = point;
         this.distance = Math.max(1.0F, distance);
         this.snapLook = snapLook;
      }

      @Override
      public boolean allowGameInputWhileCaptureKeyboard() {
         return false;
      }

      @Override
      public boolean shouldStop(boolean isGrabbed) {
         return !isGrabbed;
      }

      @Override
      public void update(double mouseDeltaX, double mouseDeltaY) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            if (this.snapLook) {
               this.snapLook = false;
               Vec3 look = EditorUI.getMouseLookVector();
               double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
               float newXRot = Mth.wrapDegrees((float)Math.toDegrees(-Mth.atan2(look.y, horizontal)));
               float newYRot = Mth.wrapDegrees((float)Math.toDegrees(Mth.atan2(look.z, look.x)) - 90.0F);
               player.setXRot(newXRot);
               player.setYRot(newYRot);
               this.realXRot = newXRot;
               this.realYRot = newYRot;
            } else {
               double baseSensitivity = (Double)Minecraft.getInstance().options.sensitivity().get() * 0.6F + 0.2F;
               double sensitivity = baseSensitivity * baseSensitivity * baseSensitivity * 8.0;
               int invert = Minecraft.getInstance().options.invertYMouse().get() ? -1 : 1;
               if (Keybinds.ARCBALL_CARDINAL_SNAP.isDown()) {
                  player.setYRot(this.realYRot);
                  player.setXRot(this.realXRot);
                  player.turn(mouseDeltaX * sensitivity, mouseDeltaY * sensitivity * invert);
                  this.realXRot = player.getXRot();
                  this.realYRot = player.getYRot();
                  Vec3 look = player.getLookAngle();
                  if (look.y < -0.5) {
                     player.setXRot(90.0F);
                  } else if (look.y > 0.5) {
                     player.setXRot(-90.0F);
                  } else {
                     player.setXRot(0.0F);
                     player.setYRot(Math.round(player.getYRot() / 90.0F) * 90.0F);
                  }
               } else {
                  player.turn(mouseDeltaX * sensitivity, mouseDeltaY * sensitivity * invert);
                  this.realXRot = player.getXRot();
                  this.realYRot = player.getYRot();
               }

               if (EditorUI.getIO().getMouseWheel() != 0.0F) {
                  float wheel = Math.signum(EditorUI.getIO().getMouseWheel());
                  float move = this.distance * 0.05F;
                  if (move > 4.0F) {
                     move = 4.0F + (float)Math.sqrt(move - 4.0F);
                  }

                  move *= wheel;
                  if (EditorUI.isMoveQuickDown()) {
                     move *= 2.0F;
                  }

                  this.distance = Math.max(1.0F, this.distance - move);
               }

               Vec3 position = this.point.subtract(player.getLookAngle().scale(this.distance));
               Minecraft.getInstance().player.setPos(position.subtract(0.0, player.getEyeHeight(), 0.0));
               Minecraft.getInstance().player.setOldPosAndRot();
            }
         }
      }
   }

   protected static final class None extends EditorMovementControls {
      private static final EditorMovementControls.None INSTANCE = new EditorMovementControls.None();

      @Override
      public boolean allowGameInputWhileCaptureKeyboard() {
         return false;
      }

      @Override
      public boolean shouldStop(boolean isGrabbed) {
         return false;
      }

      @Override
      public void update(double mouseDeltaX, double mouseDeltaY) {
      }
   }

   protected static final class Pan extends EditorMovementControls {
      private final Vec3 originalPos;
      private final Vector3f planePoint;
      private final Vector3f planeNormal;

      public Pan(Vec3 originalPos, Vector3f planePoint, Vector3f planeNormal) {
         this.originalPos = originalPos;
         this.planePoint = planePoint;
         this.planeNormal = planeNormal;
      }

      @Override
      public boolean allowGameInputWhileCaptureKeyboard() {
         return false;
      }

      @Override
      public boolean shouldStop(boolean isGrabbed) {
         return !Keybinds.PAN_CAMERA.isDownIgnoreMods();
      }

      @Override
      public void update(double mouseDeltaX, double mouseDeltaY) {
         Vec3 forwards = EditorUI.getMouseForwardsVector();
         LocalPlayer player = Minecraft.getInstance().player;
         if (forwards != null && player != null) {
            Vector3f origin = this.originalPos.add(0.0, player.getEyeHeight(), 0.0).toVector3f();
            Vector3f view = EditorUI.getMouseLookVectorFromForwards(forwards).toVector3f();
            float distance = Intersectionf.intersectRayPlane(origin, view, this.planePoint, this.planeNormal, 1.0E-5F);
            if (!(distance < 0.0F)) {
               Vector3f targetPoint = origin.add(new Vector3f(view).mul(distance));
               Vector3f targetDelta = new Vector3f(targetPoint).sub(this.planePoint);
               Vec3 finalPos = this.originalPos.subtract(targetDelta.x, targetDelta.y, targetDelta.z);
               player.setPos(finalPos);
               player.setOldPosAndRot();
            }
         }
      }
   }

   protected static final class Rotate extends EditorMovementControls {
      private final Vec3 originalForwards;
      private final Vec2 originalAngles;

      public Rotate(Vec3 originalForwards, Vec2 originalAngles) {
         this.originalForwards = originalForwards;
         this.originalAngles = originalAngles;
      }

      @Override
      public boolean allowGameInputWhileCaptureKeyboard() {
         return true;
      }

      @Override
      public boolean shouldStop(boolean isGrabbed) {
         return !Keybinds.ROTATE_CAMERA.isDownIgnoreMods();
      }

      @Override
      public void update(double mouseDeltaX, double mouseDeltaY) {
         Vec3 forwards = EditorUI.getMouseForwardsVector();
         LocalPlayer player = Minecraft.getInstance().player;
         if (forwards != null && player != null) {
            float pitch = this.originalAngles.x;
            float yaw = this.originalAngles.y;
            Vec3 lastVector = this.originalForwards;

            for (int i = 1; i <= 10; i++) {
               Vec3 thisVector = this.originalForwards.lerp(forwards, i / 10.0F);
               double differenceY = lastVector.y - thisVector.y;
               double pitchDelta = Math.toDegrees(Math.asin(differenceY / 2.0)) * 2.0;
               float var24 = Mth.wrapDegrees(pitch - (float)pitchDelta);
               pitch = Mth.clamp(var24, -90.0F, 90.0F);
               double yaw1 = Math.toDegrees(Math.atan2(lastVector.x, lastVector.z));
               double yaw2 = Math.toDegrees(Math.atan2(thisVector.x, thisVector.z));
               double yawDelta = Mth.wrapDegrees(yaw1 - yaw2);
               Vector3f forwards3f = lastVector.lerp(thisVector, 0.5).toVector3f();
               forwards3f.x = -forwards3f.x;
               forwards3f.z = -forwards3f.z;
               forwards3f.rotateX((float)Math.toRadians(pitch));
               forwards3f.normalize();
               float radius = (float)Math.sqrt(1.0F - forwards3f.y() * forwards3f.y()) * 1.08F;
               if (radius < 0.2F) {
                  radius = 0.2F;
               }

               yaw = Mth.wrapDegrees(yaw - (float)yawDelta / radius);
               lastVector = thisVector;
            }

            player.setXRot(pitch);
            player.setYRot(yaw);
         }
      }
   }

   protected static final class RotateInvert extends EditorMovementControls {
      private Vec3 oldForwards;
      private float pitch;
      private float yaw;

      public RotateInvert(Vec3 forwards, float pitch, float yaw) {
         this.oldForwards = forwards;
         this.pitch = pitch;
         this.yaw = yaw;
      }

      @Override
      public boolean allowGameInputWhileCaptureKeyboard() {
         return true;
      }

      @Override
      public boolean shouldStop(boolean isGrabbed) {
         return !Keybinds.ROTATE_CAMERA.isDownIgnoreMods();
      }

      @Override
      public void update(double mouseDeltaX, double mouseDeltaY) {
         Vec3 forwards = EditorUI.getMouseForwardsVector();
         LocalPlayer player = Minecraft.getInstance().player;
         if (forwards != null && player != null) {
            double differenceY = forwards.y - this.oldForwards.y;
            double pitchDelta = Math.toDegrees(Math.asin(differenceY / 2.0)) * 2.0;
            this.pitch = Mth.wrapDegrees(this.pitch - (float)pitchDelta);
            this.pitch = Mth.clamp(this.pitch, -90.0F, 90.0F);
            double yaw1 = Math.toDegrees(Math.atan2(this.oldForwards.x, this.oldForwards.z));
            double yaw2 = Math.toDegrees(Math.atan2(forwards.x, forwards.z));
            double yawDelta = -Mth.wrapDegrees(yaw1 - yaw2);
            this.yaw = Mth.wrapDegrees(this.yaw - (float)yawDelta);
            this.oldForwards = forwards;
            player.setXRot(this.pitch);
            player.setYRot(this.yaw);
         }
      }
   }
}
