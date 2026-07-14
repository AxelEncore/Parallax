package com.moulberry.axiom.render;

import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.Box;
import net.minecraft.world.phys.Vec3;

public class SharedBrushPreviewRenderer {
   private static BrushShape lastBrushShape = null;
   private static final ChunkedBooleanRegion booleanRegion = new ChunkedBooleanRegion();

   public static void render(AxiomWorldRenderContext rc, BrushShape brushShape, Vec3 translation, int effects) {
      if (!brushShape.equals(lastBrushShape)) {
         lastBrushShape = brushShape;
         booleanRegion.clear();
         Box bounding = brushShape.boundingBox();
         int minX = bounding.pos1().getX();
         int minY = bounding.pos1().getY();
         int minZ = bounding.pos1().getZ();
         int maxX = bounding.pos2().getX();
         int maxY = bounding.pos2().getY();
         int maxZ = bounding.pos2().getZ();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  if (brushShape.isInsideShape(x, y, z)) {
                     booleanRegion.add(x, y, z);
                  }
               }
            }
         }
      }

      booleanRegion.render(rc, translation, effects);
   }
}
