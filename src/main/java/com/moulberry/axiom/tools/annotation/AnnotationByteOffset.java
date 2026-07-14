package com.moulberry.axiom.tools.annotation;

import net.minecraft.core.Direction.Axis;

public record AnnotationByteOffset(int dx, int dy, int dz, Axis axis) {
   private static final AnnotationByteOffset[] offsetLookup = new AnnotationByteOffset[243];

   public static int offsetToId(int dx, int dy, int dz, Axis axis) {
      if (Math.abs(dx) > 4 || Math.abs(dy) > 4 || Math.abs(dz) > 4) {
         return -1;
      } else if (dx == 0 && axis == Axis.X) {
         return dy + 4 + (dz + 4) * 9;
      } else if (dy == 0 && axis == Axis.Y) {
         return dx + 4 + (dz + 4) * 9 + 81;
      } else {
         return dz == 0 && axis == Axis.Z ? dx + 4 + (dy + 4) * 9 + 162 : -1;
      }
   }

   public static AnnotationByteOffset idToOffset(int id) {
      return offsetLookup[id & 0xFF];
   }

   static {
      for (Axis axis : Axis.values()) {
         for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
               for (int z = -4; z <= 4; z++) {
                  int offsetId = offsetToId(x, y, z, axis);
                  if (offsetId >= 0) {
                     offsetLookup[offsetId] = new AnnotationByteOffset(x, y, z, axis);
                  }
               }
            }
         }
      }
   }
}
