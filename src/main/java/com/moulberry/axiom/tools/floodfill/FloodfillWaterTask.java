package com.moulberry.axiom.tools.floodfill;

import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class FloodfillWaterTask extends FloodfillTask {
   private static final BlockState FULL_WATER_STATE = Blocks.WATER.defaultBlockState();

   public FloodfillWaterTask(PositionSet positionSet, Level world, BlockPos position, int propagationFlags) {
      super(positionSet, world, position, propagationFlags);
   }

   @Override
   protected void tryQueue(int x1, int y1, int z1, MaskElement maskElement, MaskContext maskContext) {
      if (this.alreadyChecked.add(x1, y1, z1)) {
         BlockState blockState = this.chunkProvider.get(x1, y1, z1);
         if (blockState.getBlock() == Blocks.VOID_AIR) {
            return;
         }

         if (blockState == FULL_WATER_STATE) {
            return;
         }

         if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            if (!(Boolean)blockState.getValue(BlockStateProperties.WATERLOGGED)) {
               if (!maskElement.test(maskContext.reset(), x1, y1, z1)) {
                  return;
               }

               this.toCheck.enqueue(BlockPos.asLong(x1, y1, z1));
               this.positionSet.add(x1, y1, z1);
               this.fillCount.value++;
            }
         } else if (!blockState.blocksMotion()) {
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
