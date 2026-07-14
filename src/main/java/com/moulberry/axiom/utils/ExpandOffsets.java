package com.moulberry.axiom.utils;

import com.moulberry.axiomclientapi.pathers.BallShape;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class ExpandOffsets {
   public static int[][] create(BallShape shape, int radius, int radiusSquared) {
      int[][] offsets = new int[64][];

      for (int index = 0; index < 64; index++) {
         int minRadiusX = (index & 1) == 0 ? 0 : -radius - 1;
         int minRadiusY = (index & 2) == 0 ? 0 : -radius - 1;
         int minRadiusZ = (index & 4) == 0 ? 0 : -radius - 1;
         int maxRadiusX = (index & 8) == 0 ? 0 : radius + 1;
         int maxRadiusY = (index & 16) == 0 ? 0 : radius + 1;
         int maxRadiusZ = (index & 32) == 0 ? 0 : radius + 1;
         IntArrayList off = new IntArrayList();

         for (int xo = minRadiusX; xo <= maxRadiusX; xo++) {
            for (int yo = minRadiusY; yo <= maxRadiusY; yo++) {
               for (int zo = minRadiusZ; zo <= maxRadiusZ; zo++) {
                  if ((xo != 0 || yo != 0 || zo != 0) && shape.distanceSq(xo, yo, zo) <= radiusSquared) {
                     off.add(xo);
                     off.add(yo);
                     off.add(zo);
                  }
               }
            }
         }

         offsets[index] = off.toIntArray();
      }

      return offsets;
   }

   public static int[][] create(BallShape shape, int radius) {
      return create(shape, radius, radius * radius + radius);
   }
}
