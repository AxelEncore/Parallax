package com.moulberry.axiom.operations;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

public class AutoshadeShading {
   public List<Vec3> vectors;
   public List<AutoshadeShading.PositionWithIntensity> positions;

   public AutoshadeShading(List<Vec3> vectors, List<AutoshadeShading.PositionWithIntensity> positions) {
      List<Vec3> normalizedVectors = new ArrayList<>();

      for (Vec3 vector : vectors) {
         normalizedVectors.add(vector.normalize());
      }

      this.vectors = normalizedVectors;
      this.positions = positions;
   }

   public record PositionWithIntensity(float x, float y, float z, float intensity) {
   }
}
