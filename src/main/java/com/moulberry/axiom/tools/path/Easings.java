package com.moulberry.axiom.tools.path;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;

public class Easings {
   public static final FloatUnaryOperator LINEAR = FloatUnaryOperator.identity();
   public static final FloatUnaryOperator EASE_IN_SLIGHT = x -> (float)Math.sqrt(x * x * x);
   public static final FloatUnaryOperator EASE_OUT_SLIGHT = x -> {
      float inv = 1.0F - x;
      return 1.0F - (float)Math.sqrt(inv * inv * inv);
   };
   public static final FloatUnaryOperator EASE_IN_OUT_SLIGHT = x -> {
      if (x < 0.5) {
         return (float)Math.sqrt(2.0F * x * x * x);
      } else {
         float a = -2.0F * x + 2.0F;
         return 1.0F - (float)Math.sqrt(a * a * a) / 2.0F;
      }
   };
   public static final FloatUnaryOperator EASE_IN_QUAD = x -> x * x;
   public static final FloatUnaryOperator EASE_OUT_QUAD = x -> 1.0F - (1.0F - x) * (1.0F - x);
   public static final FloatUnaryOperator EASE_IN_OUT_QUAD = x -> x < 0.5 ? 2.0F * x * x : 1.0F - 2.0F * x * x + 4.0F * x - 2.0F;
   public static final FloatUnaryOperator EASE_IN_CUBIC = x -> x * x * x;
   public static final FloatUnaryOperator EASE_OUT_CUBIC = x -> {
      float inv = 1.0F - x;
      return 1.0F - inv * inv * inv;
   };
   public static final FloatUnaryOperator EASE_IN_OUT_CUBIC = x -> {
      if (x < 0.5) {
         return 4.0F * x * x * x;
      } else {
         float a = -2.0F * x + 2.0F;
         return 1.0F - a * a * a / 2.0F;
      }
   };
   public static final FloatUnaryOperator EASE_IN_QUARTIC = x -> x * x * x * x;
   public static final FloatUnaryOperator EASE_OUT_QUARTIC = x -> {
      float inv = 1.0F - x;
      return 1.0F - inv * inv * inv * inv;
   };
   public static final FloatUnaryOperator EASE_IN_OUT_QUARTIC = x -> {
      if (x < 0.5) {
         return 8.0F * x * x * x * x;
      } else {
         float a = -2.0F * x + 2.0F;
         return 1.0F - a * a * a * a / 2.0F;
      }
   };
}
