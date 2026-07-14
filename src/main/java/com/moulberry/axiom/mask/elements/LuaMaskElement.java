package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;

public class LuaMaskElement implements MaskElement {
   private final String script;

   public LuaMaskElement(String script) {
      this.script = script;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      return context.runScript(this, x, y, z);
   }

   public String getScript() {
      return this.script;
   }
}
