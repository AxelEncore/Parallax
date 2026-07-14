package com.moulberry.axiom.tools.floodfill;

import com.moulberry.axiom.AsyncChunkProvider;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.tools.magic_select.MagicSelectionTask;
import com.moulberry.axiom.utils.IntWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class FloodfillTask {
   protected final PositionSet alreadyChecked = new PositionSet();
   protected final LongArrayFIFOQueue toCheck = new LongArrayFIFOQueue();
   protected final AsyncChunkProvider chunkProvider;
   public PositionSet positionSet;
   private final int propagationFlags;
   public final IntWrapper fillCount = new IntWrapper();
   protected int until = 0;

   public FloodfillTask(PositionSet positionSet, Level world, BlockPos position, int propagationFlags) {
      this.positionSet = positionSet;
      this.chunkProvider = new AsyncChunkProvider(world);
      this.propagationFlags = propagationFlags;
      this.tryQueue(position.getX(), position.getY(), position.getZ(), MaskManager.getDestMask(), new MaskContext(world));
   }

   public void fill(int until) {
      if (until < 0) {
         throw new IllegalArgumentException();
      } else {
         this.until = until;
         IntWrapper count = this.fillCount;
         LongArrayFIFOQueue toCheck = this.toCheck;
         MaskElement maskElement = MaskManager.getDestMask();
         MaskContext maskContext = new MaskContext(this.chunkProvider);
         boolean checkUp = (this.propagationFlags & MagicSelectionTask.PROPAGATION_FLAG_UP) != 0;
         boolean checkDown = (this.propagationFlags & MagicSelectionTask.PROPAGATION_FLAG_DOWN) != 0;
         boolean checkHorizontal = (this.propagationFlags & MagicSelectionTask.PROPAGATION_FLAG_HORIZONTAL) != 0;
         boolean checkCorners = (this.propagationFlags & MagicSelectionTask.PROPAGATION_FLAG_CORNERS) != 0;

         while (!toCheck.isEmpty() && count.value < until - 8) {
            long pos = toCheck.dequeueLong();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            if (checkDown) {
               this.tryQueue(x, y - 1, z, maskElement, maskContext);
            }

            if (checkUp) {
               this.tryQueue(x, y + 1, z, maskElement, maskContext);
            }

            if (checkHorizontal) {
               this.tryQueue(x - 1, y, z, maskElement, maskContext);
               this.tryQueue(x + 1, y, z, maskElement, maskContext);
               this.tryQueue(x, y, z - 1, maskElement, maskContext);
               this.tryQueue(x, y, z + 1, maskElement, maskContext);
               if (checkCorners) {
                  this.tryQueue(x - 1, y, z - 1, maskElement, maskContext);
                  this.tryQueue(x - 1, y, z + 1, maskElement, maskContext);
                  this.tryQueue(x + 1, y, z - 1, maskElement, maskContext);
                  this.tryQueue(x + 1, y, z + 1, maskElement, maskContext);
                  if (checkDown) {
                     this.tryQueue(x - 1, y - 1, z, maskElement, maskContext);
                     this.tryQueue(x + 1, y - 1, z, maskElement, maskContext);
                  }

                  if (checkUp) {
                     this.tryQueue(x - 1, y + 1, z, maskElement, maskContext);
                     this.tryQueue(x + 1, y + 1, z, maskElement, maskContext);
                  }

                  if (checkDown) {
                     this.tryQueue(x, y - 1, z - 1, maskElement, maskContext);
                     this.tryQueue(x, y - 1, z + 1, maskElement, maskContext);
                  }

                  if (checkUp) {
                     this.tryQueue(x, y + 1, z - 1, maskElement, maskContext);
                     this.tryQueue(x, y + 1, z + 1, maskElement, maskContext);
                  }

                  if (checkDown) {
                     this.tryQueue(x - 1, y - 1, z - 1, maskElement, maskContext);
                     this.tryQueue(x + 1, y - 1, z - 1, maskElement, maskContext);
                     this.tryQueue(x - 1, y - 1, z + 1, maskElement, maskContext);
                     this.tryQueue(x + 1, y - 1, z + 1, maskElement, maskContext);
                  }

                  if (checkUp) {
                     this.tryQueue(x - 1, y + 1, z - 1, maskElement, maskContext);
                     this.tryQueue(x + 1, y + 1, z - 1, maskElement, maskContext);
                     this.tryQueue(x - 1, y + 1, z + 1, maskElement, maskContext);
                     this.tryQueue(x + 1, y + 1, z + 1, maskElement, maskContext);
                  }
               }
            }
         }

         while (!toCheck.isEmpty() && count.value < until) {
            long posx = toCheck.dequeueLong();
            int xx = BlockPos.getX(posx);
            int yx = BlockPos.getY(posx);
            int zx = BlockPos.getZ(posx);
            this.tryQueue(xx, yx - 1, zx, maskElement, maskContext);
            if (count.value >= until) {
               break;
            }

            this.tryQueue(xx - 1, yx, zx, maskElement, maskContext);
            if (count.value >= until) {
               break;
            }

            this.tryQueue(xx + 1, yx, zx, maskElement, maskContext);
            if (count.value >= until) {
               break;
            }

            this.tryQueue(xx, yx, zx - 1, maskElement, maskContext);
            if (count.value >= until) {
               break;
            }

            this.tryQueue(xx, yx, zx + 1, maskElement, maskContext);
         }
      }
   }

   protected void tryQueue(int x1, int y1, int z1, MaskElement maskElement, MaskContext maskContext) {
      if (this.alreadyChecked.add(x1, y1, z1)) {
         BlockState blockState = this.chunkProvider.get(x1, y1, z1);
         if (blockState.getBlock() == Blocks.VOID_AIR) {
            return;
         }

         if (!blockState.blocksMotion()) {
            if (!maskElement.test(maskContext.reset(), x1, y1, z1)) {
               return;
            }

            this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
            this.positionSet.add(x1, y1, z1);
            this.fillCount.value++;
         }
      }
   }
}
