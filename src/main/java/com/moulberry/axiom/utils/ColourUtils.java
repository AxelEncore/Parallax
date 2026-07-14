package com.moulberry.axiom.utils;

public class ColourUtils {
   public static String formatHex(int value) {
      return (value & 0xFF000000) == 0 ? String.format("%06x", value) : String.format("%08x", value);
   }

   public static int argbToAbgr(int argb) {
      int red = argb >> 16 & 0xFF;
      int blue = argb & 0xFF;
      return argb & -16711936 | blue << 16 | red;
   }

   public static int abgrToArgb(int abgr) {
      int blue = abgr >> 16 & 0xFF;
      int red = abgr & 0xFF;
      return abgr & -16711936 | red << 16 | blue;
   }

   public static int blendARGB(int srcColour, int dstColour) {
      float srcA = (srcColour >> 24 & 0xFF) / 255.0F;
      float srcR = (srcColour >> 16 & 0xFF) / 255.0F;
      float srcG = (srcColour >> 8 & 0xFF) / 255.0F;
      float srcB = (srcColour & 0xFF) / 255.0F;
      float dstA = (dstColour >> 24 & 0xFF) / 255.0F;
      float dstR = (dstColour >> 16 & 0xFF) / 255.0F;
      float dstG = (dstColour >> 8 & 0xFF) / 255.0F;
      float dstB = (dstColour & 0xFF) / 255.0F;
      float resultAlpha = 1.0F - (1.0F - dstA) * (1.0F - srcA);
      if (resultAlpha < 1.0E-6) {
         return (int)(resultAlpha * 255.0F) << 24;
      } else {
         float resultRed = dstR * dstA / resultAlpha + srcR * srcA * (1.0F - dstA) / resultAlpha;
         float resultGreen = dstG * dstA / resultAlpha + srcG * srcA * (1.0F - dstA) / resultAlpha;
         float resultBlue = dstB * dstA / resultAlpha + srcB * srcA * (1.0F - dstA) / resultAlpha;
         return (int)(resultAlpha * 255.0F) << 24 | (int)(resultRed * 255.0F) << 16 | (int)(resultGreen * 255.0F) << 8 | (int)(resultBlue * 255.0F);
      }
   }

   public static void HSBtoRGB(float hue, float saturation, float brightness, float[] rgb) {
      float r = 0.0F;
      float g = 0.0F;
      float b = 0.0F;
      if (saturation == 0.0F) {
         r = g = b = brightness * 255.0F;
      } else {
         float h = (hue - (float)Math.floor(hue)) * 6.0F;
         float f = h - (float)Math.floor(h);
         float p = brightness * (1.0F - saturation);
         float q = brightness * (1.0F - saturation * f);
         float t = brightness * (1.0F - saturation * (1.0F - f));
         switch ((int)h) {
            case 0:
               r = brightness * 255.0F;
               g = t * 255.0F;
               b = p * 255.0F;
               break;
            case 1:
               r = q * 255.0F;
               g = brightness * 255.0F;
               b = p * 255.0F;
               break;
            case 2:
               r = p * 255.0F;
               g = brightness * 255.0F;
               b = t * 255.0F;
               break;
            case 3:
               r = p * 255.0F;
               g = q * 255.0F;
               b = brightness * 255.0F;
               break;
            case 4:
               r = t * 255.0F;
               g = p * 255.0F;
               b = brightness * 255.0F;
               break;
            case 5:
               r = brightness * 255.0F;
               g = p * 255.0F;
               b = q * 255.0F;
         }
      }

      rgb[0] = r;
      rgb[1] = g;
      rgb[2] = b;
   }

   public static void RGBtoHSB(float r, float g, float b, float[] hsbvals) {
      float cmax = Math.max(r, g);
      if (b > cmax) {
         cmax = b;
      }

      float cmin = Math.min(r, g);
      if (b < cmin) {
         cmin = b;
      }

      float brightness = cmax / 255.0F;
      float saturation;
      if (cmax != 0.0F) {
         saturation = (cmax - cmin) / cmax;
      } else {
         saturation = 0.0F;
      }

      float hue;
      if (saturation == 0.0F) {
         hue = 0.0F;
      } else {
         float redc = (cmax - r) / (cmax - cmin);
         float greenc = (cmax - g) / (cmax - cmin);
         float bluec = (cmax - b) / (cmax - cmin);
         if (r == cmax) {
            hue = bluec - greenc;
         } else if (g == cmax) {
            hue = 2.0F + redc - bluec;
         } else {
            hue = 4.0F + greenc - redc;
         }

         hue /= 6.0F;
         if (hue < 0.0F) {
            hue++;
         }
      }

      hsbvals[0] = hue;
      hsbvals[1] = saturation;
      hsbvals[2] = brightness;
   }
}
