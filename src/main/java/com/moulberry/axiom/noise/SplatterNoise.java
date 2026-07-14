package com.moulberry.axiom.noise;

public class SplatterNoise implements NoiseInterface {
   private final SimplexNoise simplexNoise;
   private final WhiteNoise whiteNoise;
   private final float scaleX;
   private final float scaleY;
   private final float scaleZ;

   public SplatterNoise(long seed, float scaleX, float scaleY, float scaleZ) {
      this.simplexNoise = new SimplexNoise(seed);
      this.whiteNoise = new WhiteNoise(seed + 10L);
      this.scaleX = scaleX;
      this.scaleY = scaleY;
      this.scaleZ = scaleZ;
   }

   @Override
   public float evaluate(double x, double y) {
      float simplex = this.simplexNoise.evaluate(x, y);
      float white = this.whiteNoise.evaluate(x * this.scaleX, y * this.scaleY) * 0.4F + 0.3F;
      if (white < 0.5) {
         white *= 2.0F;
         return simplex * white;
      } else {
         white = (white - 0.5F) * 2.0F;
         return simplex * (1.0F - white) + white;
      }
   }

   @Override
   public float evaluate(double x, double y, double z) {
      float simplex = this.simplexNoise.evaluate(x, y, z);
      float white = this.whiteNoise.evaluate(x * this.scaleX, y * this.scaleY, z * this.scaleZ) * 0.4F + 0.3F;
      if (white < 0.5) {
         white *= 2.0F;
         return simplex * white;
      } else {
         white = (white - 0.5F) * 2.0F;
         return simplex * (1.0F - white) + white;
      }
   }
}
