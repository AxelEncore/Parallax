package com.moulberry.axiom.mixin.world_properties;

import com.moulberry.axiom.world_properties.AxiomGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Block.class})
public class MixinBlock {
   @Inject(
      method = {"dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void dropResources(BlockState blockState, Level level, BlockPos blockPos, CallbackInfo ci) {
      if (level instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKDROPS)).get()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void dropResources(BlockState blockState, LevelAccessor levelAccessor, BlockPos blockPos, BlockEntity blockEntity, CallbackInfo ci) {
      if (levelAccessor instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKDROPS)).get()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void dropResources(
      BlockState blockState, Level level, BlockPos blockPos, BlockEntity blockEntity, Entity entity, ItemStack itemStack, CallbackInfo ci
   ) {
      if (level instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKDROPS)).get()) {
         ci.cancel();
      }
   }
}
