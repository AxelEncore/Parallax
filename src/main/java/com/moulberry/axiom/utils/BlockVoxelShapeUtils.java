package com.moulberry.axiom.utils;

import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockVoxelShapeUtils {
   public static boolean firstCompletelyOverlapsSecond(VoxelShape a, VoxelShape b) {
      return a == b ? true : !Shapes.joinIsNotEmpty(a, b, BooleanOp.ONLY_SECOND);
   }
}
