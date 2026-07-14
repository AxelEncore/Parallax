package com.moulberry.axiom.mask.antlr;

import org.antlr.axiom.v4.runtime.tree.AbstractParseTreeVisitor;

public class MaskBaseVisitor<T> extends AbstractParseTreeVisitor<T> implements MaskVisitor<T> {
   @Override
   public T visitMask(MaskParser.MaskContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementCmpBlock(MaskParser.MaskElementCmpBlockContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementOr(MaskParser.MaskElementOrContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementParen(MaskParser.MaskElementParenContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementSingle(MaskParser.MaskElementSingleContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementAnd(MaskParser.MaskElementAndContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementCmpNumeric(MaskParser.MaskElementCmpNumericContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementOffset(MaskParser.MaskElementOffsetContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementCmpBiome(MaskParser.MaskElementCmpBiomeContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMaskElementNot(MaskParser.MaskElementNotContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMultiBiomeMatch(MaskParser.MultiBiomeMatchContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitMultiBlockMatch(MaskParser.MultiBlockMatchContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitBlockMatch(MaskParser.BlockMatchContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitProperty(MaskParser.PropertyContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitNear(MaskParser.NearContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitSingle(MaskParser.SingleContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitCmpBlock(MaskParser.CmpBlockContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitCmpBiome(MaskParser.CmpBiomeContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitCmpNumeric(MaskParser.CmpNumericContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitNumericPow(MaskParser.NumericPowContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitNumericMultOrDiv(MaskParser.NumericMultOrDivContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitNumericParen(MaskParser.NumericParenContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitNumericAddOrSubtract(MaskParser.NumericAddOrSubtractContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitNumericLiteral(MaskParser.NumericLiteralContext ctx) {
      return (T)this.visitChildren(ctx);
   }

   @Override
   public T visitIdentifier(MaskParser.IdentifierContext ctx) {
      return (T)this.visitChildren(ctx);
   }
}
