package com.moulberry.axiom.services;

import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiomclientapi.regions.BlockRegion;
import com.moulberry.axiomclientapi.regions.BooleanRegion;

public class RegionProvider implements com.moulberry.axiomclientapi.service.RegionProvider {
   public BlockRegion createBlock() {
      return new ChunkedBlockRegion();
   }

   public BooleanRegion createBoolean() {
      return new ChunkedBooleanRegion();
   }
}
