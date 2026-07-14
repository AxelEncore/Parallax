package com.moulberry.axiom.tools.extrude;

import com.moulberry.axiom.AsyncChunkProvider;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ExtrudeExpandTask {
   public Position2ObjectMap<BlockState> expandBlocks = new Position2ObjectMap<>(k -> new BlockState[4096]);
   public final BlockPos initialPosition;
   public final Direction direction;
   public final AsyncChunkProvider chunkProvider;
   public final BlockState initialBlockState;
   public final int includeMode;
   private final boolean displace;
   private final int count;
   private final boolean corners;

   public ExtrudeExpandTask(Level world, BlockPos initialPosition, Direction direction, int includeMode, boolean displace, int count, boolean corners) {
      this.chunkProvider = new AsyncChunkProvider(world);
      this.initialPosition = initialPosition;
      this.initialBlockState = this.chunkProvider.getBlockState(initialPosition);
      this.direction = direction;
      this.includeMode = includeMode;
      this.displace = displace;
      this.count = Math.max(1, count);
      this.corners = corners;
   }

   public static int[] calculatePerpendicularFaces(Direction direction, boolean corners) {
      int[] perpendicular = corners ? new int[24] : new int[12];
      int perpendicularIndex = 0;

      for (Direction face : Direction.values()) {
         if (face.getAxis() != direction.getAxis()) {
            perpendicular[perpendicularIndex++] = face.getStepX();
            perpendicular[perpendicularIndex++] = face.getStepY();
            perpendicular[perpendicularIndex++] = face.getStepZ();
            if (corners) {
               for (Direction corner : Direction.values()) {
                  if (corner.ordinal() > face.ordinal() && corner.getAxis() != direction.getAxis() && corner.getAxis() != face.getAxis()) {
                     perpendicular[perpendicularIndex++] = face.getStepX() + corner.getStepX();
                     perpendicular[perpendicularIndex++] = face.getStepY() + corner.getStepY();
                     perpendicular[perpendicularIndex++] = face.getStepZ() + corner.getStepZ();
                  }
               }
            }
         }
      }

      return perpendicular;
   }

   public void perform(int until) {
      int[] perpendicular = calculatePerpendicularFaces(this.direction, this.corners);
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
      if (sourceMaskElement.test(maskContext.reset(), this.initialPosition.getX(), this.initialPosition.getY(), this.initialPosition.getZ())) {
         BlockPos offset = this.initialPosition.relative(this.direction);
         BlockState offsetBlock = this.chunkProvider.getBlockState(offset);
         if (!offsetBlock.blocksMotion()) {
            int xo = offset.getX();
            int yo = offset.getY();
            int zo = offset.getZ();
            this.expandBlocks.put(xo, yo, zo, this.initialBlockState);
            if (this.displace) {
               int xn = xo - hoverDirX;
               int yn = yo - hoverDirY;
               int zn = zo - hoverDirZ;
               BlockState negativeBlock = this.chunkProvider.get(xn, yn, zn);
               if (!negativeBlock.blocksMotion()) {
                  this.expandBlocks.put(xo, yo, zo, negativeBlock);
               }
            } else {
               for (int i = 1; i < this.count; i++) {
                  int xn = xo + hoverDirX * i;
                  int yn = yo + hoverDirY * i;
                  int zn = zo + hoverDirZ * i;
                  BlockState blockState = this.chunkProvider.get(xn, yn, zn);
                  if (blockState.blocksMotion()) {
                     break;
                  }

                  this.expandBlocks.put(xn, yn, zn, this.initialBlockState);
               }
            }

            checked.add(xo, yo, zo);
            xo = 1;
            LongArrayFIFOQueue checkQueue = new LongArrayFIFOQueue();
            checkQueue.enqueue(this.initialPosition.asLong());
            zo = this.includeMode;
            if (zo == 0 && this.initialBlockState.getBlock().getStateDefinition().getPossibleStates().size() == 1) {
               zo = 1;
            }

            Block initialBlock = this.initialBlockState.getBlock();

            while (!checkQueue.isEmpty() && xo < until) {
               long pos = checkQueue.dequeueLong();
               int x = BlockPos.getX(pos);
               int y = BlockPos.getY(pos);
               int z = BlockPos.getZ(pos);

               for (int index = 0; index < perpendicular.length; index += 3) {
                  int xox = x + perpendicular[index];
                  int yox = y + perpendicular[index + 1];
                  int zox = z + perpendicular[index + 2];
                  if (checked.add(xox, yox, zox)) {
                     BlockState block = this.chunkProvider.get(xox, yox, zox);
                     switch (zo) {
                        case 1:
                           if (block != this.initialBlockState) {
                              continue;
                           }
                           break;
                        case 2:
                           if (!block.blocksMotion()) {
                              continue;
                           }
                           break;
                        case 3:
                           if (block.isAir()) {
                              continue;
                           }
                           break;
                        default:
                           if (block.getBlock() != initialBlock) {
                              continue;
                           }
                     }

                     if (sourceMaskElement.test(maskContext.reset(), xox, yox, zox)) {
                        int xr = xox + hoverDirX;
                        int yr = yox + hoverDirY;
                        int zr = zox + hoverDirZ;
                        offsetBlock = this.chunkProvider.get(xr, yr, zr);
                        if (!offsetBlock.blocksMotion()) {
                           this.expandBlocks.put(xr, yr, zr, block);
                           if (this.displace) {
                              int xn = xox - hoverDirX;
                              int yn = yox - hoverDirY;
                              int zn = zox - hoverDirZ;
                              BlockState negativeBlock = this.chunkProvider.get(xn, yn, zn);
                              if (!negativeBlock.blocksMotion()) {
                                 this.expandBlocks.put(xox, yox, zox, negativeBlock);
                              }
                           } else {
                              for (int i = 1; i < this.count; i++) {
                                 int xn = xr + hoverDirX * i;
                                 int yn = yr + hoverDirY * i;
                                 int zn = zr + hoverDirZ * i;
                                 BlockState blockState = this.chunkProvider.get(xn, yn, zn);
                                 if (blockState.blocksMotion()) {
                                    break;
                                 }

                                 this.expandBlocks.put(xn, yn, zn, block);
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
   }
}
