package com.moulberry.axiom.utils;

import java.math.BigInteger;
import java.util.function.DoubleUnaryOperator;

public record BezierOperator(long constant, int index, int count) implements DoubleUnaryOperator {
   public BezierOperator(int index, int count) {
      this(calculateConstant(index, count), index, count);
   }

   public static BigInteger factorial(int number) {
      BigInteger result = BigInteger.ONE;

      for (int factor = 2; factor <= number; factor++) {
         result = result.multiply(BigInteger.valueOf(factor));
      }

      return result;
   }

   private static long calculateConstant(int index, int count) {
      BigInteger a = factorial(count - 1);
      BigInteger b = factorial(index);
      BigInteger c = factorial(count - 1 - index);
      return a.divide(b.multiply(c)).longValueExact();
   }

   @Override
   public double applyAsDouble(double operand) {
      return this.constant * Math.pow(1.0 - operand, this.count - 1 - this.index) * Math.pow(operand, this.index);
   }
}
