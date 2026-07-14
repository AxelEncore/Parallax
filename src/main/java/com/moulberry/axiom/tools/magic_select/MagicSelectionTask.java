package com.moulberry.axiom.tools.magic_select;

import com.moulberry.axiom.AsyncChunkProvider;
import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.utils.IntWrapper;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class MagicSelectionTask {
   public static int PROPAGATION_FLAG_UP = 1;
   public static int PROPAGATION_FLAG_DOWN = 2;
   public static int PROPAGATION_FLAG_HORIZONTAL = 4;
   public static int PROPAGATION_FLAG_CORNERS = 8;
   protected final PositionSet alreadyChecked = new PositionSet();
   protected final LongArrayFIFOQueue toCheck = new LongArrayFIFOQueue();
   protected final AsyncChunkProvider chunkProvider;
   public PositionSet positionSet;
   private final int compareMode;
   private Block originalBlock;
   protected BlockState originalBlockState;
   private FamilyMap.AxiomBlockFamily originalBlockFamily;
   protected MaskContext maskContext;
   protected MaskElement maskElement;
   protected int propagationFlags = 0;
   public final IntWrapper fillCount = new IntWrapper();
   protected int until = 0;

   public MagicSelectionTask(PositionSet positionSet, Level world, BlockPos position, int compareMode, MaskElement maskElement, int propagationFlags) {
      this.originalBlockState = world.getBlockState(position);
      this.originalBlock = this.originalBlockState.getBlock();
      this.originalBlockFamily = FamilyMap.getFamilyFor(this.originalBlockState.getBlock());
      this.positionSet = positionSet;
      this.chunkProvider = new AsyncChunkProvider(world);
      this.compareMode = compareMode;
      this.propagationFlags = propagationFlags;
      if (this.compareMode != 2 || this.originalBlockState.blocksMotion()) {
         this.maskContext = new MaskContext(this.chunkProvider);
         this.maskElement = maskElement;
         if (this.maskElement.test(this.maskContext.reset(), position.getX(), position.getY(), position.getZ())) {
            this.positionSet.add(position.getX(), position.getY(), position.getZ());
            this.fillCount.value++;
            this.toCheck.enqueue(BlockPos.asLong(position.getX(), position.getY(), position.getZ()));
            this.alreadyChecked.add(position.getX(), position.getY(), position.getZ());
         }
      }
   }

   protected boolean matches(BlockState blockState) {
      return switch (this.compareMode) {
         case 1 -> blockState == this.originalBlockState;
         case 2 -> blockState.blocksMotion();
         case 3 -> blockState.getBlock() == this.originalBlock
            ? true
            : (this.originalBlockFamily != null ? FamilyMap.getFamilyFor(blockState.getBlock()) == this.originalBlockFamily : false);
         case 4 -> !blockState.isAir();
         default -> blockState.getBlock() == this.originalBlock;
      };
   }

   public void fill(int until) {
      if (until < 0) {
         throw new IllegalArgumentException();
      } else {
         this.until = until;
         IntWrapper count = this.fillCount;
         LongArrayFIFOQueue toCheck = this.toCheck;
         boolean checkUp = (this.propagationFlags & PROPAGATION_FLAG_UP) != 0;
         boolean checkDown = (this.propagationFlags & PROPAGATION_FLAG_DOWN) != 0;
         if ((this.propagationFlags & PROPAGATION_FLAG_HORIZONTAL) == 0) {
            if (checkUp || checkDown) {
               while (!toCheck.isEmpty() && count.value < until) {
                  long pos = toCheck.dequeueLong();
                  int x = BlockPos.getX(pos);
                  int y = BlockPos.getY(pos);
                  int z = BlockPos.getZ(pos);
                  if (checkDown) {
                     this.tryQueueCautious(x, y - 1, z);
                  }

                  if (checkUp) {
                     this.tryQueueCautious(x, y + 1, z);
                  }
               }
            }
         } else {
            boolean checkCorner = (this.propagationFlags & PROPAGATION_FLAG_CORNERS) != 0;

            while (!toCheck.isEmpty() && count.value < until - 27) {
               long posx = toCheck.dequeueLong();
               int xx = BlockPos.getX(posx);
               int yx = BlockPos.getY(posx);
               int zx = BlockPos.getZ(posx);
               boolean hasMinusX = this.tryQueueContains(xx - 1, yx, zx);
               boolean hasPlusX = this.tryQueueContains(xx + 1, yx, zx);
               boolean hasMinusZ = this.tryQueueContains(xx, yx, zx - 1);
               boolean hasPlusZ = this.tryQueueContains(xx, yx, zx + 1);
               boolean hasMinusY = checkDown ? this.tryQueueContains(xx, yx - 1, zx) : true;
               boolean hasPlusY = checkUp ? this.tryQueueContains(xx, yx + 1, zx) : true;
               if (checkCorner && (!hasMinusX || !hasPlusX || !hasMinusZ || !hasPlusZ || !hasMinusY || !hasPlusY)) {
                  if (!hasMinusX) {
                     if (!hasMinusZ && !this.tryQueueContains(xx - 1, yx, zx - 1)) {
                        if (!hasMinusY) {
                           this.tryQueue(xx - 1, yx - 1, zx - 1);
                        }

                        if (!hasPlusY) {
                           this.tryQueue(xx - 1, yx + 1, zx - 1);
                        }
                     }

                     if (!hasPlusZ && !this.tryQueueContains(xx - 1, yx, zx + 1)) {
                        if (!hasMinusY) {
                           this.tryQueue(xx - 1, yx - 1, zx + 1);
                        }

                        if (!hasPlusY) {
                           this.tryQueue(xx - 1, yx + 1, zx + 1);
                        }
                     }

                     if (!hasMinusY) {
                        this.tryQueue(xx - 1, yx - 1, zx);
                     }

                     if (!hasPlusY) {
                        this.tryQueue(xx - 1, yx + 1, zx);
                     }
                  }

                  if (!hasPlusX) {
                     if (!hasMinusZ && !this.tryQueueContains(xx + 1, yx, zx - 1)) {
                        if (!hasMinusY) {
                           this.tryQueue(xx + 1, yx - 1, zx - 1);
                        }

                        if (!hasPlusY) {
                           this.tryQueue(xx + 1, yx + 1, zx - 1);
                        }
                     }

                     if (!hasPlusZ && !this.tryQueueContains(xx + 1, yx, zx + 1)) {
                        if (!hasMinusY) {
                           this.tryQueue(xx + 1, yx - 1, zx + 1);
                        }

                        if (!hasPlusY) {
                           this.tryQueue(xx + 1, yx + 1, zx + 1);
                        }
                     }

                     if (!hasMinusY) {
                        this.tryQueue(xx + 1, yx - 1, zx);
                     }

                     if (!hasPlusY) {
                        this.tryQueue(xx + 1, yx + 1, zx);
                     }
                  }

                  if (!hasMinusZ) {
                     if (!hasMinusY) {
                        this.tryQueue(xx, yx - 1, zx - 1);
                     }

                     if (!hasPlusY) {
                        this.tryQueue(xx, yx + 1, zx - 1);
                     }
                  }

                  if (!hasPlusZ) {
                     if (!hasMinusY) {
                        this.tryQueue(xx, yx - 1, zx + 1);
                     }

                     if (!hasPlusY) {
                        this.tryQueue(xx, yx + 1, zx + 1);
                     }
                  }
               }
            }

            while (!toCheck.isEmpty() && count.value < until) {
               long posx = toCheck.dequeueLong();
               int xx = BlockPos.getX(posx);
               int yx = BlockPos.getY(posx);
               int zx = BlockPos.getZ(posx);
               this.tryQueueCautious(xx - 1, yx, zx);
               this.tryQueueCautious(xx + 1, yx, zx);
               this.tryQueueCautious(xx, yx, zx - 1);
               this.tryQueueCautious(xx, yx, zx + 1);
               if (checkDown) {
                  this.tryQueueCautious(xx, yx - 1, zx);
               }

               if (checkUp) {
                  this.tryQueueCautious(xx, yx + 1, zx);
               }

               if (checkCorner) {
                  this.tryQueueCautious(xx - 1, yx, zx - 1);
                  this.tryQueueCautious(xx - 1, yx, zx + 1);
                  this.tryQueueCautious(xx + 1, yx, zx - 1);
                  this.tryQueueCautious(xx + 1, yx, zx + 1);
                  if (checkDown) {
                     this.tryQueueCautious(xx - 1, yx - 1, zx);
                     this.tryQueueCautious(xx + 1, yx - 1, zx);
                  }

                  if (checkUp) {
                     this.tryQueueCautious(xx - 1, yx + 1, zx);
                     this.tryQueueCautious(xx + 1, yx + 1, zx);
                  }

                  if (checkDown) {
                     this.tryQueueCautious(xx, yx - 1, zx - 1);
                     this.tryQueueCautious(xx, yx - 1, zx + 1);
                  }

                  if (checkUp) {
                     this.tryQueueCautious(xx, yx + 1, zx - 1);
                     this.tryQueueCautious(xx, yx + 1, zx + 1);
                  }

                  if (checkDown) {
                     this.tryQueueCautious(xx - 1, yx - 1, zx - 1);
                     this.tryQueueCautious(xx + 1, yx - 1, zx - 1);
                     this.tryQueueCautious(xx - 1, yx - 1, zx + 1);
                     this.tryQueueCautious(xx + 1, yx - 1, zx + 1);
                  }

                  if (checkUp) {
                     this.tryQueueCautious(xx - 1, yx + 1, zx - 1);
                     this.tryQueueCautious(xx + 1, yx + 1, zx - 1);
                     this.tryQueueCautious(xx - 1, yx + 1, zx + 1);
                     this.tryQueueCautious(xx + 1, yx + 1, zx + 1);
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
            this.positionSet.add(x1, y1, z1);
            this.fillCount.value++;
         }
      }
   }

   private void tryQueueCautious(int x1, int y1, int z1) {
      if (this.fillCount.value < this.until) {
         if (this.alreadyChecked.add(x1, y1, z1)) {
            BlockState blockState = this.chunkProvider.get(x1, y1, z1);
            if (this.matches(blockState) && this.maskElement.test(this.maskContext.reset(), x1, y1, z1)) {
               this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
               this.positionSet.add(x1, y1, z1);
               this.fillCount.value++;
            }
         }
      }
   }

   private boolean tryQueueContains(int x1, int y1, int z1) {
      if (this.alreadyChecked.add(x1, y1, z1)) {
         BlockState blockState = this.chunkProvider.get(x1, y1, z1);
         if (this.matches(blockState) && this.maskElement.test(this.maskContext.reset(), x1, y1, z1)) {
            this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
            this.positionSet.add(x1, y1, z1);
            this.fillCount.value++;
            return true;
         } else {
            return false;
         }
      } else {
         return this.positionSet.contains(x1, y1, z1);
      }
   }
}
