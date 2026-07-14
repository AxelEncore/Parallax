package com.moulberry.axiom.noise;

public class FBMNoise implements NoiseInterface {
   private final NoiseInterface[] delegateNoise;
   private final float lacunarity;
   private final float gain;
   private final float startAmplitude;

   public FBMNoise(float lacunarity, float gain, NoiseInterface... delegateNoise) {
      this.delegateNoise = delegateNoise;
      this.lacunarity = lacunarity;
      this.gain = gain;
      float bounds = 0.0F;
      float amplitude = 1.0F;

      for (int i = 0; i < this.delegateNoise.length; i++) {
         bounds += amplitude;
         amplitude *= this.gain;
      }

      this.startAmplitude = 1.0F / bounds;
   }

   @Override
   public float evaluate(double x, double y) {
      float amplitude = this.startAmplitude;
      float result = 0.0F;

      for (NoiseInterface noise : this.delegateNoise) {
         result += amplitude * noise.evaluate(x, y);
         amplitude *= this.gain;
         x *= this.lacunarity;
         y *= this.lacunarity;
      }

      return result;
   }

   @Override
   public float evaluate(double x, double y, double z) {
      float amplitude = this.startAmplitude;
      float result = 0.0F;

      for (NoiseInterface noise : this.delegateNoise) {
         result += amplitude * noise.evaluate(x, y, z);
         amplitude *= this.gain;
         x *= this.lacunarity;
         y *= this.lacunarity;
         z *= this.lacunarity;
      }

      return result;
   }
}
