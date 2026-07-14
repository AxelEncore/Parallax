package com.moulberry.axiom.tools.magic_select;

import com.moulberry.axiom.collections.Position2ByteMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.utils.IntWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MagicSelectionRangedTask extends MagicSelectionTask {
   private final Position2ByteMap distanceMap = new Position2ByteMap();
   private final byte baseDistance;

   public MagicSelectionRangedTask(
      PositionSet positionSet, Level world, BlockPos position, int compareMode, MaskElement maskElement, int distance, int propagationFlags
   ) {
      super(positionSet, world, position, compareMode, maskElement, propagationFlags);
      if (distance > 5) {
         distance = 5;
      }

      this.baseDistance = (byte)(distance + 1);
      this.distanceMap.put(position.getX(), position.getY(), position.getZ(), this.baseDistance);
   }

   @Override
   public void fill(int until) {
      if (until < 0) {
         throw new IllegalArgumentException();
      } else {
         this.until = until;
         IntWrapper count = this.fillCount;
         LongArrayFIFOQueue toCheck = this.toCheck;
         boolean checkUp = (this.propagationFlags & PROPAGATION_FLAG_UP) != 0;
         boolean checkDown = (this.propagationFlags & PROPAGATION_FLAG_DOWN) != 0;
         boolean checkHorz = (this.propagationFlags & PROPAGATION_FLAG_HORIZONTAL) != 0;
         if (!checkHorz) {
            while (!toCheck.isEmpty() && count.value < until) {
               long pos = toCheck.dequeueLong();
               int x = BlockPos.getX(pos);
               int y = BlockPos.getY(pos);
               int z = BlockPos.getZ(pos);
               byte distance = this.distanceMap.get(x, y, z);
               byte neighborDistance = (byte)(distance - 1);
               if (checkDown) {
                  this.tryQueue(x, y - 1, z, neighborDistance);
               }

               if (checkUp) {
                  this.tryQueue(x, y + 1, z, neighborDistance);
               }
            }
         } else {
            while (!toCheck.isEmpty() && count.value < until) {
               long posx = toCheck.dequeueLong();
               int xx = BlockPos.getX(posx);
               int yx = BlockPos.getY(posx);
               int zx = BlockPos.getZ(posx);
               byte distancex = this.distanceMap.get(xx, yx, zx);
               byte neighborDistancex = (byte)(distancex - 1);
               this.tryQueue(xx - 1, yx, zx, neighborDistancex);
               this.tryQueue(xx + 1, yx, zx, neighborDistancex);
               this.tryQueue(xx, yx, zx - 1, neighborDistancex);
               this.tryQueue(xx, yx, zx + 1, neighborDistancex);
               if (checkDown) {
                  this.tryQueue(xx, yx - 1, zx, neighborDistancex);
               }

               if (checkUp) {
                  this.tryQueue(xx, yx + 1, zx, neighborDistancex);
               }
            }
         }
      }
   }

   private void tryQueue(int x1, int y1, int z1, byte distance) {
      byte previousDistance = this.distanceMap.get(x1, y1, z1);
      if (distance > previousDistance) {
         if (previousDistance == 0) {
            BlockState blockState = this.chunkProvider.get(x1, y1, z1);
            if (this.matches(blockState) && this.maskElement.test(this.maskContext.reset(), x1, y1, z1)) {
               if (this.fillCount.value >= this.until) {
                  return;
               }

               this.distanceMap.put(x1, y1, z1, this.baseDistance);
               this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
               this.positionSet.add(x1, y1, z1);
               this.fillCount.value++;
               return;
            }
         }

         if (distance > 1) {
            this.distanceMap.put(x1, y1, z1, distance);
            this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
         }
      }
   }
}
