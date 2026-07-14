package com.moulberry.axiom.mixin.world_properties;

import com.moulberry.axiom.world_properties.AxiomGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BrushableBlock.class})
public class MixinBrushableBlock {
   @Inject(
      method = {"onPlace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl, CallbackInfo ci) {
      if (level instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKGRAVITY)).get()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"updateShape"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void updateShape(
      BlockState blockState,
      Direction direction,
      BlockState blockState2,
      LevelAccessor levelAccessor,
      BlockPos blockPos,
      BlockPos blockPos2,
      CallbackInfoReturnable<BlockState> cir
   ) {
      if (levelAccessor instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKGRAVITY)).get()) {
         cir.setReturnValue(blockState);
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;fall(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/entity/item/FallingBlockEntity;",
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource, CallbackInfo ci) {
      if (!((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKGRAVITY)).get()) {
         ci.cancel();
      }
   }
}
