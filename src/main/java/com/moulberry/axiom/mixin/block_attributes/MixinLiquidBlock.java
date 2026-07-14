package com.moulberry.axiom.mixin.block_attributes;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.StaticValues;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LiquidBlock.class})
public class MixinLiquidBlock {
   @Inject(
      method = {"getShape"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
      if (StaticValues.gameHasTicked
         && AxiomClient.isAxiomActive()
         && Axiom.configuration.blockAttributes.makeFluidHitboxesSolid
         && context instanceof EntityCollisionContext entityCollisionContext
         && entityCollisionContext.getEntity() instanceof LocalPlayer) {
         cir.setReturnValue(Shapes.block());
      }
   }
}
