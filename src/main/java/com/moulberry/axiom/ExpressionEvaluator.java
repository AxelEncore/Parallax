package com.moulberry.axiom;

import com.ezylang.axiom.evalex.EvaluationException;
import com.ezylang.axiom.evalex.Expression;
import com.ezylang.axiom.evalex.parser.ParseException;

public class ExpressionEvaluator {
   public static double evaluateFallible(String input) {
      try {
         return Double.parseDouble(input);
      } catch (NumberFormatException var5) {
         input = input.replace("^", "**").replace("−", "-").replace("×", "*").replace("÷", "/");
         Expression expression = new Expression(input);

         try {
            return expression.evaluate().getNumberValue().doubleValue();
         } catch (ParseException | EvaluationException var4) {
            throw new RuntimeException(var4);
         }
      }
   }

   public static double evaluate(String input) {
      try {
         return evaluateFallible(input);
      } catch (Exception var2) {
         var2.printStackTrace();
         return 0.0;
      }
   }
}
