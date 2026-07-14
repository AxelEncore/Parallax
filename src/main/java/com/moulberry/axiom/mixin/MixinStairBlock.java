package com.moulberry.axiom.mixin;

import com.moulberry.axiom.utils.BlockHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({StairBlock.class})
public class MixinStairBlock {
   @Shadow
   @Final
   public static EnumProperty<StairsShape> SHAPE;

   @Inject(
      method = {"mirror"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void mirror(BlockState blockState, Mirror mirror, CallbackInfoReturnable<BlockState> cir) {
      Direction direction = (Direction)blockState.getValue(HorizontalDirectionalBlock.FACING);
      StairsShape stairsShape = (StairsShape)blockState.getValue(SHAPE);
      switch (mirror) {
         case LEFT_RIGHT:
            if (direction.getAxis() == Axis.Z) {
               blockState = BlockHelper.rotateY(blockState, Rotation.CLOCKWISE_180);
            }
            blockState = (BlockState)blockState.setValue(SHAPE, switch (stairsShape) {
               case INNER_LEFT -> StairsShape.INNER_RIGHT;
               case INNER_RIGHT -> StairsShape.INNER_LEFT;
               case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
               case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
               case STRAIGHT -> StairsShape.STRAIGHT;
               default -> throw new IncompatibleClassChangeError();
            });
            cir.setReturnValue(blockState);
            break;
         case FRONT_BACK:
            if (direction.getAxis() == Axis.X) {
               blockState = BlockHelper.rotateY(blockState, Rotation.CLOCKWISE_180);
            }
            blockState = (BlockState)blockState.setValue(SHAPE, switch (stairsShape) {
               case INNER_LEFT -> StairsShape.INNER_RIGHT;
               case INNER_RIGHT -> StairsShape.INNER_LEFT;
               case OUTER_LEFT -> StairsShape.OUTER_RIGHT;
               case OUTER_RIGHT -> StairsShape.OUTER_LEFT;
               case STRAIGHT -> StairsShape.STRAIGHT;
               default -> throw new IncompatibleClassChangeError();
            });
            cir.setReturnValue(blockState);
      }
   }
}
