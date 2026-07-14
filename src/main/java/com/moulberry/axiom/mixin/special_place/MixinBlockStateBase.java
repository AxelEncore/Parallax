package com.moulberry.axiom.mixin.special_place;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.StaticValues;
import com.moulberry.axiom.capabilities.SpecialPlace;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockStateBase.class})
public abstract class MixinBlockStateBase {
   @Shadow
   protected abstract BlockState asState();

   @Inject(
      method = {"updateShape"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void updateShapeHead(
      Direction direction, BlockState blockState, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2, CallbackInfoReturnable<BlockState> cir
   ) {
      if (SpecialPlace.isCancellingUpdates(levelAccessor)) {
         cir.setReturnValue(this.asState());
      }
   }

   @Inject(
      method = {"updateShape"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void updateShapeRet(
      Direction direction, BlockState blockState, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2, CallbackInfoReturnable<BlockState> cir
   ) {
      if (SpecialPlace.isSpecialPlacing(levelAccessor)) {
         BlockState currentState = this.asState();
         BlockState vanillaUpdatedShape = (BlockState)cir.getReturnValue();
         if (vanillaUpdatedShape != currentState) {
            if (SpecialPlace.isCancellingUpdates(levelAccessor)) {
               cir.setReturnValue(currentState);
            } else {
               CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(currentState);
               if (customBlockState != null && customBlockState.getCustomBlock().preventShapeUpdates()) {
                  SpecialPlace.markCustomShapeUpdated(levelAccessor, blockPos);
                  cir.setReturnValue(currentState);
               }
            }
         }
      }
   }

   @Inject(
      method = {"useWithoutItem"},
      at = {@At("HEAD")}
   )
   public void useWithoutItemHead(Level level, Player player, BlockHitResult res, CallbackInfoReturnable<InteractionResult> cir) {
      if (SpecialPlace.isUseWithoutItemCancellable(level)) {
         SpecialPlace.startDoingUseWithoutItem();
      }
   }

   @Inject(
      method = {"useWithoutItem"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void useWithoutItemReturn(Level level, Player player, BlockHitResult res, CallbackInfoReturnable<InteractionResult> cir) {
      if (SpecialPlace.isDoingUseWithoutItemChanges(level)) {
         InteractionResult previousResult = (InteractionResult)cir.getReturnValue();
         if (previousResult != null && previousResult.consumesAction()) {
            if (Axiom.configuration.blockAttributes.preventInteractions && player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof BlockItem) {
               SpecialPlace.markPreventedDefaultInteraction(level);
               SpecialPlace.finishDoingUseWithoutItem(level);
               cir.setReturnValue(InteractionResult.PASS);
               return;
            }

            CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(this.asState());
            if (customBlockState != null && customBlockState.getCustomBlock().preventRightClickInteraction()) {
               SpecialPlace.markPreventedDefaultInteraction(level);
               SpecialPlace.finishDoingUseWithoutItem(level);
               cir.setReturnValue(InteractionResult.PASS);
               return;
            }
         }

         SpecialPlace.finishDoingUseWithoutItem(level);
      }
   }

   @ModifyReturnValue(
      method = {"getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"},
      at = {@At("RETURN")}
   )
   public VoxelShape getShape(VoxelShape shape, @Local(argsOnly = true) CollisionContext context) {
      return StaticValues.gameHasTicked
            && AxiomClient.isAxiomActive()
            && Axiom.configuration.blockAttributes.expandHitboxesToFullCube
            && context instanceof EntityCollisionContext entityCollisionContext
            && entityCollisionContext.getEntity() instanceof LocalPlayer
            && !shape.isEmpty()
         ? Shapes.block()
         : shape;
   }
}
