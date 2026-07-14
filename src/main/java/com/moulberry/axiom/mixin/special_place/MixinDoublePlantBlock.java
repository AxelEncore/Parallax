package com.moulberry.axiom.mixin.special_place;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.capabilities.SpecialPlace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DoublePlantBlock.class})
public abstract class MixinDoublePlantBlock extends Block {
   public MixinDoublePlantBlock(Properties properties) {
      super(properties);
      throw new UnsupportedOperationException();
   }

   @Inject(
      method = {"getStateForPlacement"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void getStateForPlacementReturn(BlockPlaceContext blockPlaceContext, CallbackInfoReturnable<BlockState> cir) {
      if (cir.getReturnValue() == null && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel())) {
         cir.setReturnValue(super.getStateForPlacement(blockPlaceContext));
      }
   }

   @Inject(
      method = {"setPlacedBy"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, LivingEntity livingEntity, ItemStack itemStack, CallbackInfo ci) {
      if (level.isClientSide && Axiom.configuration.internal.tallGrassIsActuallyNotTall && this.stateDefinition.getOwner() == Blocks.TALL_GRASS) {
         ci.cancel();
      }
   }
}
