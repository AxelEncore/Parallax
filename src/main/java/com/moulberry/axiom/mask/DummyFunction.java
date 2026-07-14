package com.moulberry.axiom.mask;

import com.ezylang.axiom.evalex.EvaluationException;
import com.ezylang.axiom.evalex.Expression;
import com.ezylang.axiom.evalex.data.EvaluationValue;
import com.ezylang.axiom.evalex.functions.FunctionIfc;
import com.ezylang.axiom.evalex.functions.FunctionParameterDefinition;
import com.ezylang.axiom.evalex.parser.Token;
import java.util.List;

public record DummyFunction(String name) implements FunctionIfc {
   public List<FunctionParameterDefinition> getFunctionParameterDefinitions() {
      return List.of();
   }

   public EvaluationValue evaluate(Expression expression, Token functionToken, EvaluationValue... parameterValues) throws EvaluationException {
      throw new UnsupportedOperationException();
   }

   public void validatePreEvaluation(Token token, EvaluationValue... parameterValues) {
   }

   public boolean hasVarArgs() {
      return true;
   }
}
