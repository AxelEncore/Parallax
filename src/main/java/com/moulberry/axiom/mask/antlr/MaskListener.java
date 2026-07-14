package com.moulberry.axiom.mask.antlr;

import org.antlr.axiom.v4.runtime.tree.ParseTreeListener;

public interface MaskListener extends ParseTreeListener {
   void enterMask(MaskParser.MaskContext var1);

   void exitMask(MaskParser.MaskContext var1);

   void enterMaskElementCmpBlock(MaskParser.MaskElementCmpBlockContext var1);

   void exitMaskElementCmpBlock(MaskParser.MaskElementCmpBlockContext var1);

   void enterMaskElementOr(MaskParser.MaskElementOrContext var1);

   void exitMaskElementOr(MaskParser.MaskElementOrContext var1);

   void enterMaskElementParen(MaskParser.MaskElementParenContext var1);

   void exitMaskElementParen(MaskParser.MaskElementParenContext var1);

   void enterMaskElementSingle(MaskParser.MaskElementSingleContext var1);

   void exitMaskElementSingle(MaskParser.MaskElementSingleContext var1);

   void enterMaskElementAnd(MaskParser.MaskElementAndContext var1);

   void exitMaskElementAnd(MaskParser.MaskElementAndContext var1);

   void enterMaskElementCmpNumeric(MaskParser.MaskElementCmpNumericContext var1);

   void exitMaskElementCmpNumeric(MaskParser.MaskElementCmpNumericContext var1);

   void enterMaskElementOffset(MaskParser.MaskElementOffsetContext var1);

   void exitMaskElementOffset(MaskParser.MaskElementOffsetContext var1);

   void enterMaskElementCmpBiome(MaskParser.MaskElementCmpBiomeContext var1);

   void exitMaskElementCmpBiome(MaskParser.MaskElementCmpBiomeContext var1);

   void enterMaskElementNot(MaskParser.MaskElementNotContext var1);

   void exitMaskElementNot(MaskParser.MaskElementNotContext var1);

   void enterMultiBiomeMatch(MaskParser.MultiBiomeMatchContext var1);

   void exitMultiBiomeMatch(MaskParser.MultiBiomeMatchContext var1);

   void enterMultiBlockMatch(MaskParser.MultiBlockMatchContext var1);

   void exitMultiBlockMatch(MaskParser.MultiBlockMatchContext var1);

   void enterBlockMatch(MaskParser.BlockMatchContext var1);

   void exitBlockMatch(MaskParser.BlockMatchContext var1);

   void enterProperty(MaskParser.PropertyContext var1);

   void exitProperty(MaskParser.PropertyContext var1);

   void enterNear(MaskParser.NearContext var1);

   void exitNear(MaskParser.NearContext var1);

   void enterSingle(MaskParser.SingleContext var1);

   void exitSingle(MaskParser.SingleContext var1);

   void enterCmpBlock(MaskParser.CmpBlockContext var1);

   void exitCmpBlock(MaskParser.CmpBlockContext var1);

   void enterCmpBiome(MaskParser.CmpBiomeContext var1);

   void exitCmpBiome(MaskParser.CmpBiomeContext var1);

   void enterCmpNumeric(MaskParser.CmpNumericContext var1);

   void exitCmpNumeric(MaskParser.CmpNumericContext var1);

   void enterNumericPow(MaskParser.NumericPowContext var1);

   void exitNumericPow(MaskParser.NumericPowContext var1);

   void enterNumericMultOrDiv(MaskParser.NumericMultOrDivContext var1);

   void exitNumericMultOrDiv(MaskParser.NumericMultOrDivContext var1);

   void enterNumericParen(MaskParser.NumericParenContext var1);

   void exitNumericParen(MaskParser.NumericParenContext var1);

   void enterNumericAddOrSubtract(MaskParser.NumericAddOrSubtractContext var1);

   void exitNumericAddOrSubtract(MaskParser.NumericAddOrSubtractContext var1);

   void enterNumericLiteral(MaskParser.NumericLiteralContext var1);

   void exitNumericLiteral(MaskParser.NumericLiteralContext var1);

   void enterIdentifier(MaskParser.IdentifierContext var1);

   void exitIdentifier(MaskParser.IdentifierContext var1);
}
