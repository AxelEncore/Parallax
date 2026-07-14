package com.moulberry.axiom.tools.magic_select;

import com.moulberry.axiom.collections.LevelPredicateSet;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.utils.IntWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;

public class MagicSelectionSurfaceTask extends MagicSelectionTask {
   private final LevelPredicateSet airBlockPredicateSet;

   public MagicSelectionSurfaceTask(PositionSet positionSet, Level world, BlockPos position, int compareMode, MaskElement maskElement, int propagationFlags) {
      super(positionSet, world, position, compareMode, maskElement, propagationFlags);
      if (this.originalBlockState.blocksMotion()) {
         this.airBlockPredicateSet = new LevelPredicateSet(this.chunkProvider, state -> !state.blocksMotion());
      } else {
         this.airBlockPredicateSet = new LevelPredicateSet(this.chunkProvider, BlockStateBase::isAir);
      }
   }

   @Override
   public void fill(int until) {
      if (until < 0) {
         throw new IllegalArgumentException();
      } else {
         IntWrapper count = this.fillCount;
         count.value--;
         LongArrayFIFOQueue toCheck = this.toCheck;
         boolean checkUp = (this.propagationFlags & PROPAGATION_FLAG_UP) != 0;
         boolean checkDown = (this.propagationFlags & PROPAGATION_FLAG_DOWN) != 0;
         boolean checkHorz = (this.propagationFlags & PROPAGATION_FLAG_HORIZONTAL) != 0;
         boolean checkCorners = (this.propagationFlags & PROPAGATION_FLAG_CORNERS) != 0;

         while (!toCheck.isEmpty() && count.value < until) {
            long pos = toCheck.dequeueLong();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            if (this.airBlockPredicateSet.contains(x + 1, y, z)
               || this.airBlockPredicateSet.contains(x - 1, y, z)
               || this.airBlockPredicateSet.contains(x, y + 1, z)
               || this.airBlockPredicateSet.contains(x, y - 1, z)
               || this.airBlockPredicateSet.contains(x, y, z + 1)
               || this.airBlockPredicateSet.contains(x, y, z - 1)) {
               this.positionSet.add(x, y, z);
               count.value++;
               if (!checkHorz) {
                  if (checkDown) {
                     this.tryQueue(x, y - 1, z);
                  }

                  if (checkUp) {
                     this.tryQueue(x, y + 1, z);
                  }
               } else {
                  this.tryQueue(x - 1, y, z);
                  this.tryQueue(x + 1, y, z);
                  this.tryQueue(x, y, z - 1);
                  this.tryQueue(x, y, z + 1);
                  if (checkDown) {
                     this.tryQueue(x, y - 1, z);
                  }

                  if (checkUp) {
                     this.tryQueue(x, y + 1, z);
                  }

                  if (checkCorners) {
                     this.tryQueue(x - 1, y, z - 1);
                     this.tryQueue(x - 1, y, z + 1);
                     this.tryQueue(x + 1, y, z - 1);
                     this.tryQueue(x + 1, y, z + 1);
                     if (checkDown) {
                        this.tryQueue(x - 1, y - 1, z);
                        this.tryQueue(x + 1, y - 1, z);
                     }

                     if (checkUp) {
                        this.tryQueue(x - 1, y + 1, z);
                        this.tryQueue(x + 1, y + 1, z);
                     }

                     if (checkDown) {
                        this.tryQueue(x, y - 1, z - 1);
                        this.tryQueue(x, y - 1, z + 1);
                     }

                     if (checkUp) {
                        this.tryQueue(x, y + 1, z - 1);
                        this.tryQueue(x, y + 1, z + 1);
                     }

                     if (checkDown) {
                        this.tryQueue(x - 1, y - 1, z - 1);
                        this.tryQueue(x + 1, y - 1, z - 1);
                        this.tryQueue(x - 1, y - 1, z + 1);
                        this.tryQueue(x + 1, y - 1, z + 1);
                     }

                     if (checkUp) {
                        this.tryQueue(x - 1, y + 1, z - 1);
                        this.tryQueue(x + 1, y + 1, z - 1);
                        this.tryQueue(x - 1, y + 1, z + 1);
                        this.tryQueue(x + 1, y + 1, z + 1);
                     }
                  }
               }
            }
         }
      }
   }

   private void tryQueue(int x1, int y1, int z1) {
      if (this.alreadyChecked.add(x1, y1, z1)) {
         BlockState blockState = this.chunkProvider.get(x1, y1, z1);
         if (this.matches(blockState) && this.maskElement.test(this.maskContext.reset(), x1, y1, z1)) {
            this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
         }
      }
   }
}
