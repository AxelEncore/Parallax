package com.moulberry.axiom.mixin.special_place;

import com.moulberry.axiom.capabilities.SpecialPlace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({FaceAttachedHorizontalDirectionalBlock.class})
public class MixinFaceAttachedHorizontalDirectionalBlock {
   @Redirect(
      method = {"getStateForPlacement"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/state/BlockState;canSurvive(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
      )
   )
   public boolean getStateForPlacementCanSurvive(BlockState instance, LevelReader levelReader, BlockPos blockPos) {
      boolean canSurvive = instance.canSurvive(levelReader, blockPos);
      return !canSurvive && SpecialPlace.isForcePlacing(levelReader) ? true : canSurvive;
   }
}
