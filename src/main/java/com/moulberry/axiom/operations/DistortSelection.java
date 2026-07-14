package com.moulberry.axiom.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.utils.GaussianBlurTable;
import de.articdive.jnoise.generators.noise_parameters.fade_functions.FadeFunction;
import de.articdive.jnoise.generators.noise_parameters.interpolation.Interpolation;
import de.articdive.jnoise.pipeline.JNoise;
import java.util.concurrent.ThreadLocalRandom;

public class DistortSelection {
   public static void distort(float noiseRadius, float stddev) {
      Selection.modify(
         region -> {
            GaussianBlurTable blurTable = new GaussianBlurTable(Math.max(1.0F, stddev), 0.01F);
            Position2FloatMap map = new Position2FloatMap();
            blurTable.applyBlur(region.unsafeGetPositionSet(), map);
            JNoise noise = JNoise.newBuilder()
               .scale(0.617F / noiseRadius)
               .perlin(ThreadLocalRandom.current().nextLong(), Interpolation.QUADRATIC, FadeFunction.NONE)
               .build();
            ChunkedBooleanRegion newRegion = new ChunkedBooleanRegion();
            map.forEachEntry((x, y, z, v) -> {
               float noiseValue = (float)noise.evaluateNoise(x + 0.5F, y + 0.5F, z + 0.5F);
               if (v + noiseValue * 0.5F >= 0.5F) {
                  newRegion.add(x, y, z);
               }
            });
            return newRegion;
         }
      );
   }

   public static void smooth(float stddev, float threshold) {
      Selection.modify(region -> {
         GaussianBlurTable blurTable = new GaussianBlurTable(Math.max(1.0F, stddev), 0.01F);
         if (blurTable.isSingleValued()) {
            return region;
         } else {
            float[] table = blurTable.getTable();
            int radius = table.length / 2;
            if (radius == 0) {
               throw new FaultyImplementationError();
            } else {
               Position2FloatMap smoothedX = new Position2FloatMap();
               region.forEach((x, y, z) -> {
                  for (int i = -radius; i <= radius; i++) {
                     smoothedX.add(x + i, y, z, table[i + radius]);
                  }
               });
               Position2FloatMap smoothedZ = new Position2FloatMap();
               smoothedX.forEachEntry((x, y, z, v) -> {
                  for (int i = -radius; i <= radius; i++) {
                     smoothedZ.add(x, y, z + i, v * table[i + radius]);
                  }
               });
               Position2FloatMap output = new Position2FloatMap();
               ChunkedBooleanRegion newRegion = new ChunkedBooleanRegion();
               smoothedZ.forEachEntry((x, y, z, v) -> {
                  for (int i = -radius; i <= radius; i++) {
                     float s = v * table[i + radius];
                     float newValue = output.add(x, y + i, z, s);
                     if (newValue >= threshold && newValue - s < threshold) {
                        newRegion.add(x, y + i, z);
                     }
                  }
               });
               return newRegion;
            }
         }
      });
   }
}
