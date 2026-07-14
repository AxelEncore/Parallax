package com.moulberry.axiom.noise;

public class WhiteNoise implements NoiseInterface {
   private final int seed;

   public WhiteNoise(long seed) {
      this.seed = Long.hashCode(seed);
   }

   public WhiteNoise(int seed) {
      this.seed = seed;
   }

   @Override
   public float evaluate(double x, double y) {
      int cellX = (int)Math.floor(x);
      int cellY = (int)Math.floor(y);
      int hash = NoiseHelper.hash(this.seed, cellX * 501125321, cellY * 1136930381);
      double v = NoiseHelper.RAND_FLOATS[hash & 0xFF];
      v += (Math.PI / 10) * (cellX * 501125321);
      v += 0.2718281828459045 * (cellY * 1136930381);
      v -= Math.floor(v);
      return (float)v;
   }

   @Override
   public float evaluate(double x, double y, double z) {
      int cellX = (int)Math.floor(x);
      int cellY = (int)Math.floor(y);
      int cellZ = (int)Math.floor(z);
      int hash = NoiseHelper.hash(this.seed, cellX * 501125321, cellY * 1136930381, cellZ * 1720413743);
      double v = NoiseHelper.RAND_FLOATS[hash & 0xFF];
      v += (Math.PI / 10) * (cellX * 501125321);
      v += 0.2718281828459045 * (cellY * 1136930381);
      v += 0.4142135623730951 * (cellZ * 1720413743);
      v -= Math.floor(v);
      return (float)v;
   }
}
