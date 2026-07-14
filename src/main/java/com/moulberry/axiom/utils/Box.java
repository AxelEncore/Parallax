package com.moulberry.axiom.utils;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public record Box(BlockPos pos1, @Nullable BlockPos pos2) {
   public Box(int x1, int y1, int z1, int x2, int y2, int z2) {
      this(new BlockPos(x1, y1, z1), new BlockPos(x2, y2, z2));
   }
}
