package com.moulberry.axiom.tools.stamp;

import com.moulberry.axiom.gizmo.Gizmo;

public final class IntermediatePlacement {
   public final Gizmo gizmo;
   public StampPlacement placement;

   public IntermediatePlacement(Gizmo gizmo, StampPlacement placement) {
      this.gizmo = gizmo;
      this.placement = placement;
   }
}
