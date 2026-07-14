package com.moulberry.axiom.mixin.special_place;

import com.moulberry.axiom.capabilities.SpecialPlace;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PlaceOnWaterBlockItem.class})
public class MixinPlaceOnWaterBlockItem extends BlockItem {
   public MixinPlaceOnWaterBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Inject(
      method = {"useOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void useOn(UseOnContext useOnContext, CallbackInfoReturnable<InteractionResult> cir) {
      if (SpecialPlace.isForcePlacing(useOnContext.getLevel())) {
         BlockHitResult blockHitResult = PlaceOnWaterBlockItem.getPlayerPOVHitResult(useOnContext.getLevel(), useOnContext.getPlayer(), Fluid.SOURCE_ONLY);
         if (blockHitResult.getType() == Type.MISS || blockHitResult.getBlockPos().equals(useOnContext.getClickedPos())) {
            cir.setReturnValue(super.useOn(useOnContext));
         }
      }
   }
}
