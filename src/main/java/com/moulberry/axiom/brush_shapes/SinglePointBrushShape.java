package com.moulberry.axiom.brush_shapes;

import com.moulberry.axiom.utils.Box;

public class SinglePointBrushShape implements BrushShape {
   @Override
   public boolean isInsideShape(int x, int y, int z) {
      return x == 0 && y == 0 && z == 0;
   }

   @Override
   public float sdfSq(int x, int y, int z) {
      return x * x + y * y + z * z;
   }

   @Override
   public Box boundingBox() {
      return new Box(0, 0, 0, 0, 0, 0);
   }
}
