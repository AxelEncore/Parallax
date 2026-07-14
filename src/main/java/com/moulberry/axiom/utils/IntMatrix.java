package com.moulberry.axiom.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;

public class IntMatrix {
   public int m00;
   public int m01;
   public int m02;
   public int m10;
   public int m11;
   public int m12;
   public int m20;
   public int m21;
   public int m22;

   public IntMatrix() {
      this.m00 = 1;
      this.m11 = 1;
      this.m22 = 1;
   }

   public IntMatrix(IntMatrix other) {
      this.m00 = other.m00;
      this.m01 = other.m01;
      this.m02 = other.m02;
      this.m10 = other.m10;
      this.m11 = other.m11;
      this.m12 = other.m12;
      this.m20 = other.m20;
      this.m21 = other.m21;
      this.m22 = other.m22;
   }

   public boolean isIdentity() {
      if (this.m00 != 1) {
         return false;
      } else if (this.m01 != 0) {
         return false;
      } else if (this.m02 != 0) {
         return false;
      } else if (this.m10 != 0) {
         return false;
      } else if (this.m11 != 1) {
         return false;
      } else if (this.m12 != 0) {
         return false;
      } else if (this.m20 != 0) {
         return false;
      } else {
         return this.m21 != 0 ? false : this.m22 == 1;
      }
   }

   public void identity() {
      this.m00 = 1;
      this.m01 = 0;
      this.m02 = 0;
      this.m10 = 0;
      this.m11 = 1;
      this.m12 = 0;
      this.m20 = 0;
      this.m21 = 0;
      this.m22 = 1;
   }

   public IntMatrix copy() {
      return new IntMatrix(this);
   }

   public void invert() {
      int a = this.m00 * this.m11 - this.m01 * this.m10;
      int b = this.m02 * this.m10 - this.m00 * this.m12;
      int c = this.m01 * this.m12 - this.m02 * this.m11;
      int d = a * this.m22 + b * this.m21 + c * this.m20;
      int nm00 = (this.m11 * this.m22 - this.m21 * this.m12) / d;
      int nm01 = (this.m21 * this.m02 - this.m01 * this.m22) / d;
      int nm02 = c / d;
      int nm10 = (this.m20 * this.m12 - this.m10 * this.m22) / d;
      int nm11 = (this.m00 * this.m22 - this.m20 * this.m02) / d;
      int nm12 = b / d;
      int nm20 = (this.m10 * this.m21 - this.m20 * this.m11) / d;
      int nm21 = (this.m20 * this.m01 - this.m00 * this.m21) / d;
      int nm22 = a / d;
      this.m00 = nm00;
      this.m01 = nm01;
      this.m02 = nm02;
      this.m10 = nm10;
      this.m11 = nm11;
      this.m12 = nm12;
      this.m20 = nm20;
      this.m21 = nm21;
      this.m22 = nm22;
   }

   public BlockPos transform(BlockPos pos) {
      int lx = pos.getX();
      int ly = pos.getY();
      int lz = pos.getZ();
      return new BlockPos(
         lx * this.m00 + ly * this.m10 + lz * this.m20, lx * this.m01 + ly * this.m11 + lz * this.m21, lx * this.m02 + ly * this.m12 + lz * this.m22
      );
   }

   public int transformX(int x, int y, int z) {
      return x * this.m00 + y * this.m10 + z * this.m20;
   }

   public int transformY(int x, int y, int z) {
      return x * this.m01 + y * this.m11 + z * this.m21;
   }

   public int transformZ(int x, int y, int z) {
      return x * this.m02 + y * this.m12 + z * this.m22;
   }

   public float transformFloatX(float x, float y, float z) {
      return x * this.m00 + y * this.m10 + z * this.m20;
   }

   public float transformFloatY(float x, float y, float z) {
      return x * this.m01 + y * this.m11 + z * this.m21;
   }

   public float transformFloatZ(float x, float y, float z) {
      return x * this.m02 + y * this.m12 + z * this.m22;
   }

   public double transformDoubleX(double x, double y, double z) {
      return x * this.m00 + y * this.m10 + z * this.m20;
   }

   public double transformDoubleY(double x, double y, double z) {
      return x * this.m01 + y * this.m11 + z * this.m21;
   }

   public double transformDoubleZ(double x, double y, double z) {
      return x * this.m02 + y * this.m12 + z * this.m22;
   }

   public void rotate(Axis axis, int count) {
      switch (axis) {
         case X:
            this.rotateX(count);
            break;
         case Y:
            this.rotateY(count);
            break;
         case Z:
            this.rotateZ(count);
      }
   }

   public void rotateX(int count) {
      count %= 4;
      if (count < 0) {
         count += 4;
      }

      if (count == 1) {
         int nm01 = -this.m02;
         int nm11 = -this.m12;
         int nm21 = -this.m22;
         this.m02 = this.m01;
         this.m12 = this.m11;
         this.m22 = this.m21;
         this.m01 = nm01;
         this.m11 = nm11;
         this.m21 = nm21;
      } else if (count == 2) {
         this.m02 *= -1;
         this.m12 *= -1;
         this.m22 *= -1;
         this.m01 *= -1;
         this.m11 *= -1;
         this.m21 *= -1;
      } else if (count == 3) {
         int nm01 = this.m02;
         int nm11 = this.m12;
         int nm21 = this.m22;
         this.m02 = -this.m01;
         this.m12 = -this.m11;
         this.m22 = -this.m21;
         this.m01 = nm01;
         this.m11 = nm11;
         this.m21 = nm21;
      }
   }

   public void rotateY(int count) {
      count %= 4;
      if (count < 0) {
         count += 4;
      }

      if (count == 1) {
         int nm00 = this.m02;
         int nm10 = this.m12;
         int nm20 = this.m22;
         this.m02 = -this.m00;
         this.m12 = -this.m10;
         this.m22 = -this.m20;
         this.m00 = nm00;
         this.m10 = nm10;
         this.m20 = nm20;
      } else if (count == 2) {
         this.m02 *= -1;
         this.m12 *= -1;
         this.m22 *= -1;
         this.m00 *= -1;
         this.m10 *= -1;
         this.m20 *= -1;
      } else if (count == 3) {
         int nm00 = -this.m02;
         int nm10 = -this.m12;
         int nm20 = -this.m22;
         this.m02 = this.m00;
         this.m12 = this.m10;
         this.m22 = this.m20;
         this.m00 = nm00;
         this.m10 = nm10;
         this.m20 = nm20;
      }
   }

   public void rotateZ(int count) {
      count %= 4;
      if (count < 0) {
         count += 4;
      }

      if (count == 1) {
         int nm00 = -this.m01;
         int nm10 = -this.m11;
         int nm20 = -this.m21;
         this.m01 = this.m00;
         this.m11 = this.m10;
         this.m21 = this.m20;
         this.m00 = nm00;
         this.m10 = nm10;
         this.m20 = nm20;
      } else if (count == 2) {
         this.m01 *= -1;
         this.m11 *= -1;
         this.m21 *= -1;
         this.m00 *= -1;
         this.m10 *= -1;
         this.m20 *= -1;
      } else if (count == 3) {
         int nm00 = this.m01;
         int nm10 = this.m11;
         int nm20 = this.m21;
         this.m01 = -this.m00;
         this.m11 = -this.m10;
         this.m21 = -this.m20;
         this.m00 = nm00;
         this.m10 = nm10;
         this.m20 = nm20;
      }
   }

   public void flip(Axis axis) {
      switch (axis) {
         case X:
            this.m00 *= -1;
            this.m10 *= -1;
            this.m20 *= -1;
            break;
         case Y:
            this.m01 *= -1;
            this.m11 *= -1;
            this.m21 *= -1;
            break;
         case Z:
            this.m02 *= -1;
            this.m12 *= -1;
            this.m22 *= -1;
      }
   }

   @Override
   public String toString() {
      return "IntMatrix{m00="
         + this.m00
         + ", m01="
         + this.m01
         + ", m02="
         + this.m02
         + ", m10="
         + this.m10
         + ", m11="
         + this.m11
         + ", m12="
         + this.m12
         + ", m20="
         + this.m20
         + ", m21="
         + this.m21
         + ", m22="
         + this.m22
         + "}";
   }
}
