package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;
import org.joml.Quaternionfc;

public interface BrushShape {
   static boolean isQuaternionIdentity(Quaternionfc q) {
      return Math.abs(q.x()) < 1.0E-4 && Math.abs(q.y()) < 1.0E-4 && Math.abs(q.z()) < 1.0E-4 && Math.abs(q.w() - 1.0F) < 1.0E-4;
   }

   boolean isInsideShape(int var1, int var2, int var3);

   float sdfSq(int var1, int var2, int var3);

   default float sdf(int x, int y, int z) {
      return (float)Math.sqrt(this.sdfSq(x, y, z));
   }

   Box boundingBox();
}
