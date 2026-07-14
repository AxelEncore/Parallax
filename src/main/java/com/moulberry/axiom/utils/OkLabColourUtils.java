package com.moulberry.axiom.utils;

public class OkLabColourUtils {
   public static float forwardGamma(float component) {
      return component * component;
   }

   public static float reverseGamma(float component) {
      return (float)Math.sqrt(component);
   }

   public static float forwardLight(float L) {
      return (float)Math.sqrt(L * L * L);
   }

   public static float reverseLight(double L) {
      return (float)Math.cbrt(L * L);
   }

   public static double cube(double value) {
      return value * value * value;
   }

   public static void rgb2lab(float R, float G, float B, double[] lab) {
      float r = forwardGamma(R * 0.003921569F);
      float g = forwardGamma(G * 0.003921569F);
      float b = forwardGamma(B * 0.003921569F);
      float l = (float)Math.cbrt(0.4121656F * r + 0.5362752F * g + 0.051457565F * b);
      float m = (float)Math.cbrt(0.2118591F * r + 0.68071896F * g + 0.10740658F * b);
      float s = (float)Math.cbrt(0.088309795F * r + 0.28184742F * g + 0.63026136F * b);
      lab[0] = forwardLight(0.21045426F * l + 0.7936178F * m - 0.004072047F * s) * 100.0F;
      lab[1] = (1.9779985F * l - 2.4285922F * m + 0.4505937F * s) * 127.5F;
      lab[2] = (0.025904037F * l + 0.78277177F * m - 0.80867577F * s) * 127.5F;
   }

   public static int lab2rgb(double L, double A, double B) {
      L = reverseLight(L / 100.0);
      A /= 127.5;
      B /= 127.5;
      float l = (float)cube(L + 0.3963377774 * A + 0.2158037573 * B);
      float m = (float)cube(L - 0.1055613458 * A - 0.0638541728 * B);
      float s = (float)cube(L - 0.0894841775 * A - 1.291485548 * B);
      int r = (int)(reverseGamma(Math.min(Math.max(4.0767245F * l - 3.307217F * m + 0.23075905F * s, 0.0F), 1.0F)) * 255.999F);
      int g = (int)(reverseGamma(Math.min(Math.max(-1.2681438F * l + 2.6093323F * m - 0.34113443F * s, 0.0F), 1.0F)) * 255.999F);
      int b = (int)(reverseGamma(Math.min(Math.max(-0.0041119885F * l - 0.7034763F * m + 1.7068626F * s, 0.0F), 1.0F)) * 255.999F);
      return r << 16 | g << 8 | b;
   }
}
