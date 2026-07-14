package com.moulberry.axiom.mixin;

import com.moulberry.axiom.GeneralGameFeatures;
import com.moulberry.axiom.capabilities.ArcballCamera;
import com.moulberry.axiom.capabilities.FastPlace;
import com.moulberry.axiom.capabilities.ReplaceMode;
import com.moulberry.axiom.capabilities.SpecialPlace;
import com.moulberry.axiom.capabilities.Tinker;
import com.moulberry.axiom.editor.EditorUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public abstract class MixinMultiPlayerGameMode {
   @Shadow
   private int destroyDelay;
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private GameType localPlayerMode;

   @Shadow
   protected abstract void startPrediction(ClientLevel var1, PredictiveAction var2);

   @Shadow
   protected abstract InteractionResult performUseItemOn(LocalPlayer var1, InteractionHand var2, BlockHitResult var3);

   @Shadow
   public abstract boolean destroyBlock(BlockPos var1);

   /**
    * Defensive guard: vanilla {@code MultiPlayerGameMode.tick()} dereferences {@code minecraft.player}
    * (via {@code ensureHasSentCarriedItem}) whenever a level is present. During a resource/shader reload
    * or a disconnect race the level can outlive the player for a tick, producing a hard NPE crash
    * (seen with heavy modpacks). Skipping the tick when there is no player is safe — none of its work
    * (carried-item sync, destroy-delay, continued digging) is meaningful without a player.
    */
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void axiom$guardTickWithoutPlayer(CallbackInfo ci) {
      if (this.minecraft.player == null) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"isAlwaysFlying"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void isAlwaysFlying(CallbackInfoReturnable<Boolean> cir) {
      if (ArcballCamera.isLocked() || EditorUI.isActive()) {
         cir.setReturnValue(true);
      }
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("RETURN")}
   )
   public void useItemOnReturn(
      LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir
   ) {
      if (((InteractionResult)cir.getReturnValue()).consumesAction()) {
         FastPlace.afterUseItemOn(blockHitResult, false);
      }
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   public void performUseItemOn(
      LocalPlayer localPlayer, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir
   ) {
      InteractionResult result = GeneralGameFeatures.handleUseCustomItemOn(localPlayer, interactionHand, blockHitResult);
      if (result != InteractionResult.PASS) {
         cir.setReturnValue(result);
      } else {
         result = ReplaceMode.performUseItemOn(localPlayer, interactionHand, blockHitResult);
         if (result != InteractionResult.PASS) {
            cir.setReturnValue(result);
         } else {
            result = Tinker.performUseItemOn(localPlayer, interactionHand, blockHitResult);
            if (result != InteractionResult.PASS) {
               cir.setReturnValue(result);
               FastPlace.disableTemporarily();
            } else {
               result = SpecialPlace.performUseItemOn(localPlayer, interactionHand, blockHitResult, this::performUseItemOn);
               if (result != InteractionResult.PASS) {
                  cir.setReturnValue(result);
                  FastPlace.afterUseItemOn(blockHitResult, false);
               }
            }
         }
      }
   }

   @Inject(
      method = {"startDestroyBlock", "continueDestroyBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;startPrediction(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/multiplayer/prediction/PredictiveAction;)V",
         shift = Shift.BEFORE,
         ordinal = 0
      )},
      cancellable = true
   )
   public void startDestroyBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      if (!this.minecraft.player.blockActionRestricted(this.minecraft.level, blockPos, this.localPlayerMode)) {
         ClientLevel level = this.minecraft.level;
         BlockState blockState = level.getBlockState(blockPos);
         if (this.minecraft.player.getMainHandItem().getItem().canAttackBlock(blockState, level, blockPos, this.minecraft.player)) {
            Block block = blockState.getBlock();
            if (!(block instanceof GameMasterBlock) || this.minecraft.player.canUseGameMasterBlocks()) {
               if (!blockState.isAir()) {
                  BlockHitResult blockHitResult = (BlockHitResult)Minecraft.getInstance().hitResult;
                  if (Tinker.startDestroyBlockCreative(blockHitResult)) {
                     this.destroyDelay = 5;
                     cir.setReturnValue(true);
                  } else {
                     if (SpecialPlace.destroyBlock(this.minecraft.level, blockPos, direction, this::destroyBlock)) {
                        this.destroyDelay = 5;
                        cir.setReturnValue(true);
                     }
                  }
               }
            }
         }
      }
   }
}
