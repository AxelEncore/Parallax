package com.moulberry.axiom.tools.magic_select;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.utils.IntWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class MagicSelectionPreciseTask {
   protected final PositionSet alreadyChecked = new PositionSet();
   protected final LongArrayFIFOQueue toCheck = new LongArrayFIFOQueue();
   protected final Level world;
   public PositionSet positionSet;
   public final int compareMode;
   public Block originalBlock;
   public BlockState originalBlockState;
   public final IntWrapper fillCount = new IntWrapper();
   protected int until = 0;

   public MagicSelectionPreciseTask(PositionSet positionSet, Level world, BlockPos position, int compareMode) {
      this.originalBlockState = world.getBlockState(position);
      this.originalBlock = this.originalBlockState.getBlock();
      this.positionSet = positionSet;
      this.world = world;
      this.compareMode = compareMode;
      if (this.compareMode != 2 || this.originalBlockState.blocksMotion()) {
         this.positionSet.add(position.getX(), position.getY(), position.getZ());
         this.fillCount.value++;
         this.toCheck.enqueue(BlockPos.asLong(position.getX(), position.getY(), position.getZ()));
         this.alreadyChecked.add(position.getX(), position.getY(), position.getZ());
      }
   }

   public void fill(int until) {
      if (until < 0) {
         throw new IllegalArgumentException();
      } else {
         this.until = until;
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         IntWrapper count = this.fillCount;
         LongArrayFIFOQueue toCheck = this.toCheck;

         while (!toCheck.isEmpty() && count.value < until) {
            long pos = toCheck.dequeueLong();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            this.tryQueue(mutableBlockPos, x - 1, y, z);
            this.tryQueue(mutableBlockPos, x + 1, y, z);
            this.tryQueue(mutableBlockPos, x, y, z - 1);
            this.tryQueue(mutableBlockPos, x, y, z + 1);
            this.tryQueue(mutableBlockPos, x, y - 1, z);
            this.tryQueue(mutableBlockPos, x, y + 1, z);
            this.tryQueue(mutableBlockPos, x - 1, y, z - 1);
            this.tryQueue(mutableBlockPos, x - 1, y, z + 1);
            this.tryQueue(mutableBlockPos, x - 1, y - 1, z);
            this.tryQueue(mutableBlockPos, x - 1, y + 1, z);
            this.tryQueue(mutableBlockPos, x + 1, y, z - 1);
            this.tryQueue(mutableBlockPos, x + 1, y, z + 1);
            this.tryQueue(mutableBlockPos, x + 1, y - 1, z);
            this.tryQueue(mutableBlockPos, x + 1, y + 1, z);
            this.tryQueue(mutableBlockPos, x, y - 1, z - 1);
            this.tryQueue(mutableBlockPos, x, y + 1, z - 1);
            this.tryQueue(mutableBlockPos, x, y - 1, z + 1);
            this.tryQueue(mutableBlockPos, x, y + 1, z + 1);
            this.tryQueue(mutableBlockPos, x - 1, y - 1, z - 1);
            this.tryQueue(mutableBlockPos, x - 1, y + 1, z - 1);
            this.tryQueue(mutableBlockPos, x - 1, y - 1, z + 1);
            this.tryQueue(mutableBlockPos, x - 1, y + 1, z + 1);
            this.tryQueue(mutableBlockPos, x + 1, y - 1, z - 1);
            this.tryQueue(mutableBlockPos, x + 1, y + 1, z - 1);
            this.tryQueue(mutableBlockPos, x + 1, y - 1, z + 1);
            this.tryQueue(mutableBlockPos, x + 1, y + 1, z + 1);
         }
      }
   }

   private void tryQueue(MutableBlockPos mutableBlockPos, int x1, int y1, int z1) {
      if (this.fillCount.value < this.until) {
         if (this.alreadyChecked.add(x1, y1, z1)) {
            BlockState blockState = this.world.getBlockState(mutableBlockPos.set(x1, y1, z1));
            switch (this.compareMode) {
               case 1:
                  if (blockState != this.originalBlockState) {
                     return;
                  }
                  break;
               case 2:
                  if (!blockState.blocksMotion()) {
                     return;
                  }
                  break;
               default:
                  if (blockState.getBlock() != this.originalBlock) {
                     return;
                  }
            }

            this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
            this.positionSet.add(x1, y1, z1);
            this.fillCount.value++;
         }
      }
   }
}
