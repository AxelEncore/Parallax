package com.moulberry.axiom.tools.stamp;

import java.util.Arrays;

public record StampBlueprintConfig(boolean[] automaticOffset, int[] offset, float[] chance) {
   public StampBlueprintConfig copy() {
      return new StampBlueprintConfig(
         Arrays.copyOf(this.automaticOffset, this.automaticOffset.length),
         Arrays.copyOf(this.offset, this.offset.length),
         Arrays.copyOf(this.chance, this.chance.length)
      );
   }
}
