package com.moulberry.axiom.mask.antlr;

import org.antlr.axiom.v4.runtime.tree.ParseTreeVisitor;

public interface MaskVisitor<T> extends ParseTreeVisitor<T> {
   T visitMask(MaskParser.MaskContext var1);

   T visitMaskElementCmpBlock(MaskParser.MaskElementCmpBlockContext var1);

   T visitMaskElementOr(MaskParser.MaskElementOrContext var1);

   T visitMaskElementParen(MaskParser.MaskElementParenContext var1);

   T visitMaskElementSingle(MaskParser.MaskElementSingleContext var1);

   T visitMaskElementAnd(MaskParser.MaskElementAndContext var1);

   T visitMaskElementCmpNumeric(MaskParser.MaskElementCmpNumericContext var1);

   T visitMaskElementOffset(MaskParser.MaskElementOffsetContext var1);

   T visitMaskElementCmpBiome(MaskParser.MaskElementCmpBiomeContext var1);

   T visitMaskElementNot(MaskParser.MaskElementNotContext var1);

   T visitMultiBiomeMatch(MaskParser.MultiBiomeMatchContext var1);

   T visitMultiBlockMatch(MaskParser.MultiBlockMatchContext var1);

   T visitBlockMatch(MaskParser.BlockMatchContext var1);

   T visitProperty(MaskParser.PropertyContext var1);

   T visitNear(MaskParser.NearContext var1);

   T visitSingle(MaskParser.SingleContext var1);

   T visitCmpBlock(MaskParser.CmpBlockContext var1);

   T visitCmpBiome(MaskParser.CmpBiomeContext var1);

   T visitCmpNumeric(MaskParser.CmpNumericContext var1);

   T visitNumericPow(MaskParser.NumericPowContext var1);

   T visitNumericMultOrDiv(MaskParser.NumericMultOrDivContext var1);

   T visitNumericParen(MaskParser.NumericParenContext var1);

   T visitNumericAddOrSubtract(MaskParser.NumericAddOrSubtractContext var1);

   T visitNumericLiteral(MaskParser.NumericLiteralContext var1);

   T visitIdentifier(MaskParser.IdentifierContext var1);
}
