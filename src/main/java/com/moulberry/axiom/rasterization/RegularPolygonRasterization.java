package com.moulberry.axiom.rasterization;

import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class RegularPolygonRasterization {
   public static void regularPolygon(
      Vector3i center,
      float diameterX,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      int vertices,
      Quaternionf quaternionf,
      TriIntConsumer consumer
   ) {
      if (!(diameterX <= 0.0F) && !(diameterZ <= 0.0F)) {
         if (vertices >= 3) {
            int centerX = center.x;
            int centerY = center.y;
            int centerZ = center.z;
            double radiusX = (diameterX - 1.0F) / 2.0;
            double radiusZ = (diameterZ - 1.0F) / 2.0;
            if (hollow) {
               double offsetX = (centerEvenDiameters ? radiusX % 1.0 : 0.0) + 0.5;
               double offsetZ = (centerEvenDiameters ? radiusZ % 1.0 : 0.0) + 0.5;
               Vector3d from = new Vector3d(offsetX, 0.5, radiusZ + offsetZ);
               quaternionf.transformInverse(from);
               Vector3d to = new Vector3d();

               for (int a = 1; a <= vertices; a++) {
                  double currX = Math.sin(a * Math.PI * 2.0 / vertices) * (radiusX - 0.15) + offsetX;
                  double currZ = Math.cos(a * Math.PI * 2.0 / vertices) * (radiusZ - 0.15) + offsetZ;
                  to.set(currX, 0.5, currZ);
                  quaternionf.transformInverse(to);
                  Rasterization3D.dda(from, to, (xx, y, zx) -> consumer.accept(xx + centerX, y + centerY, zx + centerZ));
                  Vector3d temp = from;
                  from = to;
                  to = temp;
               }
            } else {
               double[] lineSegments = new double[2 * (vertices + 1)];

               for (int a = 0; a <= vertices; a++) {
                  double x = (float)Math.sin(a * Math.PI * 2.0 / vertices) * (radiusX + 0.5);
                  double z = (float)Math.cos(a * Math.PI * 2.0 / vertices) * (radiusZ + 0.5);
                  lineSegments[a * 2] = x;
                  lineSegments[a * 2 + 1] = z;
               }

               RasterizationHelper.planeCondition(center, diameterX, diameterZ, centerEvenDiameters, quaternionf, (xx, y) -> {
                  for (int i = 0; i < lineSegments.length - 2; i += 2) {
                     double x1 = lineSegments[i];
                     double y1 = lineSegments[i + 1];
                     double x2 = lineSegments[i + 2];
                     double y2 = lineSegments[i + 3];
                     if ((x2 - x1) * (y - y1) - (y2 - y1) * (xx - x1) > 0.0) {
                        return false;
                     }
                  }

                  return true;
               }, consumer);
            }
         }
      }
   }
}
