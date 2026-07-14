package com.moulberry.axiom.noise.generic;

import net.minecraft.util.Mth;

public class PositionalRandom {
   private long seedLow;
   private long seedHigh;
   private final long originalSeedLow;
   private final long originalSeedHigh;

   public PositionalRandom(long seedLow, long seedHigh) {
      if ((seedLow | seedHigh) == 0L) {
         this.originalSeedLow = this.seedLow = -7046029254386353131L;
         this.originalSeedHigh = this.seedHigh = 7640891576956012809L;
      } else {
         this.originalSeedLow = this.seedLow = seedLow;
         this.originalSeedHigh = this.seedHigh = seedHigh;
      }
   }

   public void at(int x, int y, int z) {
      this.seedLow = Mth.getSeed(x, y, z) ^ this.originalSeedLow;
      this.seedHigh = this.originalSeedHigh;
   }

   public long nextLong() {
      long low = this.seedLow;
      long high = this.seedHigh;
      long value = Long.rotateLeft(low + high, 17) + low;
      long var7;
      this.seedLow = Long.rotateLeft(low, 49) ^ (var7 = high ^ low) ^ var7 << 21;
      this.seedHigh = Long.rotateLeft(var7, 28);
      return value;
   }

   public int nextInt() {
      return (int)this.next(32);
   }

   public int nextInt(int bound) {
      long value = this.nextInt() % bound;
      if (value < 0L) {
         value += bound;
      }

      return (int)value;
   }

   public float nextFloat() {
      return (float)this.next(24) / 1.6777216E7F;
   }

   public boolean nextBoolean() {
      return this.nextLong() >= 0L;
   }

   private long next(int bits) {
      return this.nextLong() >>> 64 - bits;
   }
}
