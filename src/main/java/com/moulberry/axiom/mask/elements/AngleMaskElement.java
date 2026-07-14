package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import net.minecraft.world.phys.Vec3;

public class AngleMaskElement implements MaskElement {
   private final int angle;
   private final int comparison;

   public AngleMaskElement(int angle, int comparison) {
      this.angle = angle;
      this.comparison = comparison;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      Vec3 vec = context.getAngleVector(x, y, z);
      if (vec == Vec3.ZERO) {
         return false;
      } else {
         double horz = Math.sqrt(vec.x * vec.x + vec.z * vec.z);
         int angle = (int)Math.round(Math.toDegrees(Math.atan2(vec.y, horz)));

         return switch (this.comparison) {
            case 0 -> angle == this.angle;
            case 1 -> angle != this.angle;
            case 2 -> angle < this.angle;
            case 3 -> angle > this.angle;
            case 4 -> angle <= this.angle;
            case 5 -> angle >= this.angle;
            default -> throw new FaultyImplementationError();
         };
      }
   }

   public int getComparison() {
      return this.comparison;
   }

   public int getAngle() {
      return this.angle;
   }
}
