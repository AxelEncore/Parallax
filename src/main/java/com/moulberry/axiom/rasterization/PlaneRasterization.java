package com.moulberry.axiom.rasterization;

import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class PlaneRasterization {
   public static void plane(
      Vector3i center, float diameterX, float diameterZ, boolean hollow, boolean centerEvenDiameters, Quaternionf quaternionf, TriIntConsumer consumer
   ) {
      if (!(diameterX <= 0.0F) && !(diameterZ <= 0.0F)) {
         int centerX = center.x;
         int centerY = center.y;
         int centerZ = center.z;
         double radiusX = (diameterX - 1.0F) / 2.0;
         double radiusZ = (diameterZ - 1.0F) / 2.0;
         if (hollow) {
            double offsetX = (centerEvenDiameters ? radiusX % 1.0 : 0.0) + 0.5;
            double offsetZ = (centerEvenDiameters ? radiusZ % 1.0 : 0.0) + 0.5;
            Vector3d one = new Vector3d(-(radiusX - 0.15) + offsetX, 0.5, -(radiusZ - 0.15) + offsetZ);
            quaternionf.transformInverse(one);
            Vector3d two = new Vector3d(-(radiusX - 0.15) + offsetX, 0.5, radiusZ - 0.15 + offsetZ);
            quaternionf.transformInverse(two);
            Vector3d three = new Vector3d(radiusX - 0.15 + offsetX, 0.5, radiusZ - 0.15 + offsetZ);
            quaternionf.transformInverse(three);
            Vector3d four = new Vector3d(radiusX - 0.15 + offsetX, 0.5, -(radiusZ - 0.15) + offsetZ);
            quaternionf.transformInverse(four);
            TriIntConsumer offsetConsumer = (x, y, z) -> consumer.accept(x + centerX, y + centerY, z + centerZ);
            Rasterization3D.dda(one, two, offsetConsumer);
            Rasterization3D.dda(two, three, offsetConsumer);
            Rasterization3D.dda(three, four, offsetConsumer);
            Rasterization3D.dda(four, one, offsetConsumer);
         } else {
            RasterizationHelper.planeCondition(
               center, diameterX, diameterZ, centerEvenDiameters, quaternionf, (x, y) -> Math.abs(x) <= radiusX && Math.abs(y) <= radiusZ, consumer
            );
         }
      }
   }
}
