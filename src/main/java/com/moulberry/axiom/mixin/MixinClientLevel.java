package com.moulberry.axiom.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.capabilities.SpecialPlace;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.hooks.ClientLevelExt;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientLevel.class})
public abstract class MixinClientLevel implements LevelHeightAccessor, ClientLevelExt {
   @Unique
   private boolean playedBlockBreakThisTick = false;

   @Shadow
   public abstract void addDestroyBlockEffect(BlockPos var1, BlockState var2);

   @Override
   public boolean axiom$isTimeFrozen() {
      return !((Level)(Object)this).getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
   }

   @Inject(
      method = {"setBlock"},
      at = {@At("HEAD")}
   )
   public void setBlock(BlockPos blockPos, BlockState blockState, int i, int j, CallbackInfoReturnable<Boolean> cir) {
      SpecialPlace.tryCollectBlockChange((Level)(Object)this, blockPos, blockState);
   }

   @WrapOperation(
      method = {"removeEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;onClientRemoval()V"
      )}
   )
   public void onRemoveEntity(Entity instance, Operation<Void> original) {
      original.call(new Object[]{instance});
      DisplayEntityManipulator.onEntityRemoved(instance);
   }

   @ModifyReturnValue(
      method = {"getMarkerParticleTarget"},
      at = {@At("RETURN")}
   )
   public Block getMarkerParticleTarget(Block block) {
      if (AxiomClient.isAxiomActive()) {
         if (Axiom.configuration.blockAttributes.showLightBlocks && block == Blocks.LIGHT) {
            return null;
         }

         if (Axiom.configuration.blockAttributes.showCollisionMesh && block == Blocks.BARRIER) {
            return null;
         }
      }

      return block;
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void onTick(CallbackInfo ci) {
      this.playedBlockBreakThisTick = false;
   }

   @Inject(
      method = {"levelEvent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void onLevelEvent(@Nullable Player player, int event, BlockPos blockPos, int j, CallbackInfo ci) {
      if (event == 2001 && AxiomClient.isAxiomActive()) {
         if (this.playedBlockBreakThisTick) {
            BlockState blockState = Block.stateById(j);
            this.addDestroyBlockEffect(blockPos, blockState);
            ci.cancel();
         }

         this.playedBlockBreakThisTick = true;
      }
   }
}
