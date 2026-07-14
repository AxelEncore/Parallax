package com.moulberry.axiom.mask;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.antlr.MaskBaseVisitor;
import com.moulberry.axiom.mask.antlr.MaskParser;

public class NumericVisitor extends MaskBaseVisitor<Integer> {
   public Integer visitNumericPow(MaskParser.NumericPowContext ctx) {
      return (int)Math.pow(((Integer)ctx.numeric(0).accept(this)).intValue(), ((Integer)ctx.numeric(1).accept(this)).intValue());
   }

   public Integer visitNumericAddOrSubtract(MaskParser.NumericAddOrSubtractContext ctx) {
      if (ctx.op.getType() == 35) {
         return (Integer)ctx.numeric(0).accept(this) - (Integer)ctx.numeric(1).accept(this);
      } else if (ctx.op.getType() == 34) {
         return (Integer)ctx.numeric(0).accept(this) + (Integer)ctx.numeric(1).accept(this);
      } else {
         throw new FaultyImplementationError();
      }
   }

   public Integer visitNumericMultOrDiv(MaskParser.NumericMultOrDivContext ctx) {
      if (ctx.op.getType() == 36) {
         return (Integer)ctx.numeric(0).accept(this) * (Integer)ctx.numeric(1).accept(this);
      } else if (ctx.op.getType() == 37) {
         return (Integer)ctx.numeric(0).accept(this) / (Integer)ctx.numeric(1).accept(this);
      } else {
         throw new FaultyImplementationError();
      }
   }

   public Integer visitNumericLiteral(MaskParser.NumericLiteralContext ctx) {
      return ctx.MINUS().size() % 2 == 1 ? -Integer.parseInt(ctx.UNSIGNED_INTEGER().getText()) : Integer.parseInt(ctx.UNSIGNED_INTEGER().getText());
   }

   public Integer visitNumericParen(MaskParser.NumericParenContext ctx) {
      return (Integer)ctx.numeric().accept(this);
   }
}
