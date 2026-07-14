package com.moulberry.axiom.gizmo;

import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.Vec3;

public interface GizmoTarget {
   public static class Move1D implements GizmoTarget {
      public Vec3 vector;
      public Vec3 origin;
      public Axis clickedAxis;

      public Move1D(Vec3 vector, Vec3 origin, Axis clickedAxis) {
         this.vector = vector;
         this.origin = origin;
         this.clickedAxis = clickedAxis;
      }
   }

   public static class Move2D implements GizmoTarget {
      public Vec3 origin;
      public Axis axis;

      public Move2D(Vec3 origin, Axis axis) {
         this.origin = origin;
         this.axis = axis;
      }
   }

   public static class Move3D implements GizmoTarget {
      float distance;

      public Move3D(float distance) {
         this.distance = distance;
      }
   }

   public static class Rotate implements GizmoTarget {
      public boolean firstTime = true;
      public float startAngle;
      public Axis axis;

      public Rotate(float startAngle, Axis axis) {
         this.startAngle = startAngle;
         this.axis = axis;
      }
   }

   public static class Scale1D implements GizmoTarget {
      public Vec3 vector;
      public float clickedLength;
      public Axis clickedAxis;

      public Scale1D(Vec3 vector, float clickedLength, Axis clickedAxis) {
         this.vector = vector;
         this.clickedLength = clickedLength;
         this.clickedAxis = clickedAxis;
      }
   }
}
