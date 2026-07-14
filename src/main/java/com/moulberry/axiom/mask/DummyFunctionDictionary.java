package com.moulberry.axiom.mask;

import com.ezylang.axiom.evalex.config.FunctionDictionaryIfc;
import com.ezylang.axiom.evalex.functions.FunctionIfc;

public class DummyFunctionDictionary implements FunctionDictionaryIfc {
   public void addFunction(String functionName, FunctionIfc function) {
      throw new UnsupportedOperationException();
   }

   public boolean hasFunction(String functionName) {
      return FunctionDictionaryIfc.super.hasFunction(functionName);
   }

   public FunctionIfc getFunction(String functionName) {
      return new DummyFunction(functionName);
   }
}
