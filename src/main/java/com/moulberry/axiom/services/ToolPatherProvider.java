package com.moulberry.axiom.services;

import com.moulberry.axiomclientapi.pathers.BallShape;
import com.moulberry.axiomclientapi.pathers.ToolPatherPoint;
import com.moulberry.axiomclientapi.pathers.ToolPatherUnique;

public class ToolPatherProvider implements com.moulberry.axiomclientapi.service.ToolPatherProvider {
   public ToolPatherPoint createPoint(boolean includeNonSolid) {
      return new com.moulberry.axiom.pather.ToolPatherPoint(includeNonSolid);
   }

   public ToolPatherUnique createUnique(int radius, BallShape ballShape) {
      return new com.moulberry.axiom.pather.ToolPatherUnique(radius, ballShape);
   }
}
