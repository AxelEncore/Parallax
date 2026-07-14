package com.moulberry.axiom.tools.smooth;

import com.moulberry.axiom.collections.list.IntrusiveLinkedElement;

public class WeightedPosition extends IntrusiveLinkedElement<WeightedPosition> {
   public int x;
   public int y;
   public int z;
   public boolean renderOverrideWithAir = false;
   public boolean solid = false;
   public float weight;

   public WeightedPosition(int x, int y, int z, float weight) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.weight = weight;
   }
}
