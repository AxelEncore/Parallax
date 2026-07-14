package com.moulberry.axiom.tools.extrude;

import com.moulberry.axiom.AsyncChunkProvider;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ExtrudeShrinkTask {
   public ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   public PositionSet shrinkBlocks = new PositionSet();
   public final BlockPos initialPosition;
   public final Direction direction;
   public final AsyncChunkProvider chunkProvider;
   public final BlockState initialBlockState;
   public final int includeMode;
   private final boolean useChunkedBlockRegion;
   private final boolean displace;
   private final int count;
   private final boolean corners;

   public ExtrudeShrinkTask(
      Level world, BlockPos initialPosition, Direction direction, int includeMode, boolean useChunkedBlockRegion, boolean displace, int count, boolean corners
   ) {
      this.chunkProvider = new AsyncChunkProvider(world);
      this.initialPosition = initialPosition;
      this.initialBlockState = this.chunkProvider.getBlockState(initialPosition);
      this.direction = direction;
      this.includeMode = includeMode;
      this.useChunkedBlockRegion = useChunkedBlockRegion;
      this.displace = displace;
      this.count = Math.max(1, count);
      this.corners = corners;
   }

   public void perform(int until) {
      int[] perpendicular = ExtrudeExpandTask.calculatePerpendicularFaces(this.direction, this.corners);
      switch (this.includeMode) {
         case 2:
            if (!this.initialBlockState.blocksMotion()) {
               return;
            }
            break;
         case 3:
            if (this.initialBlockState.isAir()) {
               return;
            }
      }

      int hoverDirX = this.direction.getStepX();
      int hoverDirY = this.direction.getStepY();
      int hoverDirZ = this.direction.getStepZ();
      PositionSet checked = new PositionSet();
      MaskElement sourceMaskElement = MaskManager.getSourceMask();
      MaskContext maskContext = new MaskContext(this.chunkProvider);
      int effectiveMode = this.includeMode;
      if (effectiveMode == 0 && this.initialBlockState.getBlock().getStateDefinition().getPossibleStates().size() == 1) {
         effectiveMode = 1;
      }

      Block initialBlock = this.initialBlockState.getBlock();
      BlockPos offset = this.initialPosition.relative(this.direction);
      BlockState offsetBlock = this.chunkProvider.getBlockState(offset);
      if (!offsetBlock.blocksMotion()
         && sourceMaskElement.test(maskContext.reset(), this.initialPosition.getX(), this.initialPosition.getY(), this.initialPosition.getZ())) {
         int xo = this.initialPosition.getX();
         int yo = this.initialPosition.getY();
         int zo = this.initialPosition.getZ();
         if (this.useChunkedBlockRegion) {
            this.chunkedBlockRegion.addBlock(xo, yo, zo, Blocks.AIR.defaultBlockState());
            if (this.displace) {
               int xn = xo - hoverDirX;
               int yn = yo - hoverDirY;
               int zn = zo - hoverDirZ;
               BlockState negativeBlock = this.chunkProvider.get(xn, yn, zn);
               if (!negativeBlock.blocksMotion()) {
                  BlockState block = this.chunkProvider.get(xo, yo, zo);
                  this.chunkedBlockRegion.addBlock(xn, yn, zn, block);
               }
            } else {
               for (int i = 1; i < this.count; i++) {
                  int xn = xo - hoverDirX * i;
                  int yn = yo - hoverDirY * i;
                  int zn = zo - hoverDirZ * i;
                  BlockState blockState = this.chunkProvider.get(xn, yn, zn);
                  if (!this.matches(effectiveMode, blockState, initialBlock) || !sourceMaskElement.test(maskContext.reset(), xn, yn, zn)) {
                     break;
                  }

                  this.chunkedBlockRegion.addBlock(xn, yn, zn, Blocks.AIR.defaultBlockState());
               }
            }
         } else {
            this.shrinkBlocks.add(xo, yo, zo);

            for (int i = 1; i < this.count; i++) {
               int xn = xo - hoverDirX * i;
               int yn = yo - hoverDirY * i;
               int zn = zo - hoverDirZ * i;
               BlockState blockState = this.chunkProvider.get(xn, yn, zn);
               if (!this.matches(effectiveMode, blockState, initialBlock) || !sourceMaskElement.test(maskContext.reset(), xn, yn, zn)) {
                  break;
               }

               this.shrinkBlocks.add(xn, yn, zn);
            }
         }

         checked.add(xo, yo, zo);
         xo = 1;
         LongArrayFIFOQueue checkQueue = new LongArrayFIFOQueue();
         checkQueue.enqueue(this.initialPosition.asLong());

         while (!checkQueue.isEmpty() && xo < until) {
            long pos = checkQueue.dequeueLong();
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);

            for (int index = 0; index < 12; index += 3) {
               int xox = x + perpendicular[index];
               int yox = y + perpendicular[index + 1];
               int zox = z + perpendicular[index + 2];
               if (checked.add(xox, yox, zox)) {
                  BlockState block = this.chunkProvider.get(xox, yox, zox);
                  if (this.matches(effectiveMode, block, initialBlock) && sourceMaskElement.test(maskContext.reset(), xox, yox, zox)) {
                     int xr = xox + hoverDirX;
                     int yr = yox + hoverDirY;
                     int zr = zox + hoverDirZ;
                     offsetBlock = this.chunkProvider.get(xr, yr, zr);
                     if (!offsetBlock.blocksMotion()) {
                        if (this.useChunkedBlockRegion) {
                           this.chunkedBlockRegion.addBlock(xox, yox, zox, Blocks.AIR.defaultBlockState());
                           if (this.displace) {
                              int xn = xox - hoverDirX;
                              int yn = yox - hoverDirY;
                              int zn = zox - hoverDirZ;
                              BlockState negativeBlock = this.chunkProvider.get(xn, yn, zn);
                              if (!negativeBlock.blocksMotion()) {
                                 this.chunkedBlockRegion.addBlock(xn, yn, zn, block);
                              }
                           } else {
                              for (int i = 1; i < this.count; i++) {
                                 int xn = xox - hoverDirX * i;
                                 int yn = yox - hoverDirY * i;
                                 int zn = zox - hoverDirZ * i;
                                 BlockState blockState = this.chunkProvider.get(xn, yn, zn);
                                 if (!this.matches(effectiveMode, blockState, initialBlock) || !sourceMaskElement.test(maskContext.reset(), xn, yn, zn)) {
                                    break;
                                 }

                                 this.chunkedBlockRegion.addBlock(xn, yn, zn, Blocks.AIR.defaultBlockState());
                              }
                           }
                        } else {
                           this.shrinkBlocks.add(xox, yox, zox);

                           for (int i = 1; i < this.count; i++) {
                              int xn = xox - hoverDirX * i;
                              int yn = yox - hoverDirY * i;
                              int zn = zox - hoverDirZ * i;
                              BlockState blockState = this.chunkProvider.get(xn, yn, zn);
                              if (!this.matches(effectiveMode, blockState, initialBlock) || !sourceMaskElement.test(maskContext.reset(), xn, yn, zn)) {
                                 break;
                              }

                              this.shrinkBlocks.add(xn, yn, zn);
                           }
                        }

                        checkQueue.enqueue(BlockPos.asLong(xox, yox, zox));
                        xo++;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean matches(int effectiveMode, BlockState block, Block initialBlock) {
      return switch (effectiveMode) {
         case 1 -> block == this.initialBlockState;
         case 2 -> block.blocksMotion();
         case 3 -> !block.isAir();
         default -> block.getBlock() == initialBlock;
      };
   }
}
