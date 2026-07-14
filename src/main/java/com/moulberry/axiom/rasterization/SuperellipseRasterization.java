package com.moulberry.axiom.rasterization;

import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class SuperellipseRasterization {
   public static void superellipse(
      Vector3i center,
      float diameterX,
      float diameterZ,
      boolean hollow,
      boolean centerEvenDiameters,
      float exponent,
      Quaternionf quaternionf,
      TriIntConsumer consumer
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
            double invRadiusX = 1.0 / (float)Math.pow(radiusX - 0.15, exponent);
            double invRadiusZ = 1.0 / (float)Math.pow(radiusZ - 0.15, exponent);
            if (!Double.isFinite(invRadiusX)) {
               invRadiusX = Double.MAX_VALUE;
            }

            if (!Double.isFinite(invRadiusZ)) {
               invRadiusZ = Double.MAX_VALUE;
            }

            Vector3d from = new Vector3d(offsetX, 0.5, radiusZ - 0.15F + offsetZ);
            quaternionf.transformInverse(from);
            Vector3d to = new Vector3d();

            for (int a = 0; a <= 360; a++) {
               double currX = Math.sin(Math.toRadians(a)) * (radiusX - 0.15);
               double currZ = Math.cos(Math.toRadians(a)) * (radiusZ - 0.15);
               double rxPow = Math.pow(Math.abs(currX), exponent);
               double rzPow = Math.pow(Math.abs(currZ), exponent);
               double factor = rxPow * invRadiusX + rzPow * invRadiusZ;
               double invFactor = Math.pow(1.0 / factor, 1.0F / exponent);
               currX *= invFactor;
               currZ *= invFactor;
               to.set(currX + offsetX, 0.5, currZ + offsetZ);
               quaternionf.transformInverse(to);
               Rasterization3D.dda(from, to, (x, y, z) -> consumer.accept(x + centerX, y + centerY, z + centerZ));
               Vector3d temp = from;
               from = to;
               to = temp;
            }
         } else {
            int powLutSize = Math.max((int)Math.ceil(radiusX), (int)Math.ceil(radiusZ)) * 10;
            float[] powLookup = new float[powLutSize];

            for (int i = 0; i < powLookup.length; i++) {
               powLookup[i] = (float)Math.pow(i / 10.0F, exponent);
            }

            float invRadiusXx = 1.0F / (float)Math.pow(radiusX + 0.5, exponent);
            float invRadiusZx = 1.0F / (float)Math.pow(radiusZ + 0.5, exponent);
            if (!Float.isFinite(invRadiusXx)) {
               invRadiusXx = Float.MAX_VALUE;
            }

            if (!Float.isFinite(invRadiusZx)) {
               invRadiusZx = Float.MAX_VALUE;
            }

            float invRadiusXF = invRadiusXx;
            float invRadiusZF = invRadiusZx;
            RasterizationHelper.planeCondition(center, diameterX, diameterZ, centerEvenDiameters, quaternionf, (rx, rz) -> {
               int rxPowIndex = (int)Math.abs(rx * 10.0F);
               int rzPowIndex = (int)Math.abs(rz * 10.0F);
               if (rxPowIndex >= powLutSize) {
                  return false;
               } else if (rzPowIndex >= powLutSize) {
                  return false;
               } else {
                  float rxPowx = powLookup[rxPowIndex];
                  float rzPowx = powLookup[rzPowIndex];
                  return rxPowx * invRadiusXF + rzPowx * invRadiusZF <= 1.0F;
               }
            }, consumer);
         }
      }
   }
}
