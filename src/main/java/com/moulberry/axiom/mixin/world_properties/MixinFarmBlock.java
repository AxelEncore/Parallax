package com.moulberry.axiom.mixin.world_properties;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.world_properties.AxiomGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({FarmBlock.class})
public class MixinFarmBlock {
   @WrapOperation(
      method = {"fallOn"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
      )}
   )
   public void fallOnTurnToDirt(@Nullable Entity entity, BlockState blockState, Level level, BlockPos blockPos, Operation<Void> original) {
      if (!(level instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOTRAMPLEFARMLAND)).get())) {
         original.call(new Object[]{entity, blockState, level, blockPos});
      }
   }
}
