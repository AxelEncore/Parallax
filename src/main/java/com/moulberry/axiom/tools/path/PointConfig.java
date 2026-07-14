package com.moulberry.axiom.tools.path;

import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Objects;

public final class PointConfig {
   public boolean overrideBlock;
   public boolean overrideRadius;
   public CustomBlockState block;
   public int[] radius;
   public int[] easing;
   public int[] easingType;

   public PointConfig(PointConfig other) {
      this(other.overrideBlock, other.overrideRadius, other.block, other.radius[0], other.easing[0], other.easingType[0]);
   }

   public PointConfig(boolean overrideBlock, boolean overrideRadius, CustomBlockState block, int radius) {
      this(overrideBlock, overrideRadius, block, radius, 0, 0);
   }

   public PointConfig(boolean overrideBlock, boolean overrideRadius, CustomBlockState block, int radius, int easing, int easingType) {
      this.overrideBlock = overrideBlock;
      this.overrideRadius = overrideRadius;
      this.block = block;
      this.radius = new int[]{radius};
      this.easing = new int[]{easing};
      this.easingType = new int[]{easingType};
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (obj != null && obj.getClass() == this.getClass()) {
         PointConfig that = (PointConfig)obj;
         return this.overrideBlock == that.overrideBlock
            && this.overrideRadius == that.overrideRadius
            && Objects.equals(this.block, that.block)
            && this.radius == that.radius;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.overrideBlock, this.overrideRadius, this.block, this.radius);
   }

   @Override
   public String toString() {
      return "PointConfig[overrideBlock="
         + this.overrideBlock
         + ", overrideRadius="
         + this.overrideRadius
         + ", block="
         + this.block
         + ", radius="
         + this.radius
         + "]";
   }
}
