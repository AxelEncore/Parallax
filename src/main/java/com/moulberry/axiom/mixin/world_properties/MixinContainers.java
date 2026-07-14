package com.moulberry.axiom.mixin.world_properties;

import com.moulberry.axiom.world_properties.AxiomGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameRules.BooleanValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Containers.class})
public class MixinContainers {
   @Inject(
      method = {"dropContents(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/Container;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void dropContents(Level level, BlockPos blockPos, Container container, CallbackInfo ci) {
      if (level instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKDROPS)).get()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"dropContents(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/NonNullList;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void dropContents(Level level, BlockPos blockPos, NonNullList<ItemStack> nonNullList, CallbackInfo ci) {
      if (level instanceof ServerLevel serverLevel && !((BooleanValue)serverLevel.getGameRules().getRule(AxiomGameRules.RULE_DOBLOCKDROPS)).get()) {
         ci.cancel();
      }
   }
}
