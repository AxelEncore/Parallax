package com.moulberry.axiom.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ParticleEngine.class})
public class MixinParticleEngine {
   @Shadow
   protected ClientLevel level;

   @Inject(
      method = {"crack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void crack(BlockPos blockPos, Direction direction, CallbackInfo ci) {
      BlockState blockState = this.level.getBlockState(blockPos);
      VoxelShape shape = blockState.getShape(this.level, blockPos);
      if (shape == Shapes.empty()) {
         ci.cancel();
      }
   }
}
