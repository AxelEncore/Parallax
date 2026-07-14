package com.moulberry.axiom.buildertools;

import net.minecraft.core.Direction;

public class PendingAction {
   public int moveX = 0;
   public int moveY = 0;
   public int moveZ = 0;
   public int rotateY = 0;
   public boolean flipX = false;
   public boolean flipY = false;
   public boolean flipZ = false;

   public void reset() {
      this.moveX = 0;
      this.moveY = 0;
      this.moveZ = 0;
      this.rotateY = 0;
      this.flipX = false;
      this.flipY = false;
      this.flipZ = false;
   }

   public void move(Direction direction, int amount) {
      this.moveX = this.moveX + direction.getStepX() * amount;
      this.moveY = this.moveY + direction.getStepY() * amount;
      this.moveZ = this.moveZ + direction.getStepZ() * amount;
   }

   public void move(int x, int y, int z) {
      this.moveX += x;
      this.moveY += y;
      this.moveZ += z;
   }

   public void rotateY(int count) {
      if (this.flipX != this.flipZ) {
         this.rotateY -= count;
      } else {
         this.rotateY += count;
      }
   }

   public void flipX() {
      this.flipX = !this.flipX;
   }

   public void flipY() {
      this.flipY = !this.flipY;
   }

   public void flipZ() {
      this.flipZ = !this.flipZ;
   }
}
