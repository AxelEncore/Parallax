package com.moulberry.axiom.rasterization;

import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class SpiralRasterization {
   public static void archimedean(
      Vector3i center, float diameterX, float diameterZ, boolean centerEvenDiameters, float loops, Quaternionf quaternionf, TriIntConsumer consumer
   ) {
      if (!(diameterX <= 0.0F) && !(diameterZ <= 0.0F)) {
         int centerX = center.x;
         int centerY = center.y;
         int centerZ = center.z;
         double radiusX = (diameterX - 1.0F) / 2.0;
         double radiusZ = (diameterZ - 1.0F) / 2.0;
         double offsetX = (centerEvenDiameters ? radiusX % 1.0 : 0.0) + 0.5;
         double offsetZ = (centerEvenDiameters ? radiusZ % 1.0 : 0.0) + 0.5;
         Vector3d from = new Vector3d(offsetX, 0.5, offsetZ);
         quaternionf.transformInverse(from);
         Vector3d to = new Vector3d();

         for (int a = 0; a <= 360.0F * loops; a++) {
            double amount = a / (360.0F * loops);
            double currX = Math.sin(Math.toRadians(a)) * amount * (radiusX - 0.15) + offsetX;
            double currZ = Math.cos(Math.toRadians(a)) * amount * (radiusZ - 0.15) + offsetZ;
            to.set(currX, 0.5, currZ);
            quaternionf.transformInverse(to);
            Rasterization3D.dda(from, to, (x, y, z) -> consumer.accept(x + centerX, y + centerY, z + centerZ));
            Vector3d temp = from;
            from = to;
            to = temp;
         }
      }
   }
}
