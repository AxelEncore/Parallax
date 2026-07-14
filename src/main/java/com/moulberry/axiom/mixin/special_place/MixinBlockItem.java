package com.moulberry.axiom.mixin.special_place;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.moulberry.axiom.capabilities.Capability;
import com.moulberry.axiom.capabilities.SpecialPlace;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.ItemStackDataHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BlockItem.class})
public abstract class MixinBlockItem {
   @Shadow
   protected abstract boolean canPlace(BlockPlaceContext var1, BlockState var2);

   @Shadow
   public abstract Block getBlock();

   @WrapOperation(
      method = {"place"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/BlockItem;updatePlacementContext(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/item/context/BlockPlaceContext;"
      )}
   )
   public BlockPlaceContext place$updatePlacementContext(BlockItem instance, BlockPlaceContext blockPlaceContext, Operation<BlockPlaceContext> original) {
      if (!SpecialPlace.isSpecialPlacing(blockPlaceContext.getLevel())) {
         return (BlockPlaceContext)original.call(new Object[]{instance, blockPlaceContext});
      } else {
         BlockPlaceContext originalBlockPlaceContext = (BlockPlaceContext)original.call(new Object[]{instance, blockPlaceContext});
         return originalBlockPlaceContext == null && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel()) ? blockPlaceContext : originalBlockPlaceContext;
      }
   }

   @WrapOperation(
      method = {"place"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"
      )}
   )
   public BlockState place$getPlacementState(BlockItem instance, BlockPlaceContext blockPlaceContext, Operation<BlockState> original) {
      if (!SpecialPlace.isSpecialPlacing(blockPlaceContext.getLevel())) {
         return (BlockState)original.call(new Object[]{instance, blockPlaceContext});
      } else {
         BlockState ret = (BlockState)original.call(new Object[]{instance, blockPlaceContext});
         if (ret == null && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel())) {
            ret = this.getBlock().defaultBlockState();
            if (ret.hasProperty(BlockStateProperties.WATERLOGGED)) {
               FluidState fluidState = blockPlaceContext.getLevel().getFluidState(blockPlaceContext.getClickedPos());
               ret = (BlockState)ret.setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
            }
         }

         ItemStack itemStack = blockPlaceContext.getItemInHand();
         if (itemStack.isEmpty()) {
            return ret;
         } else {
            String customBlockPlacer = ItemStackDataHelper.getCustomBlockPlacer(itemStack);
            if (customBlockPlacer == null) {
               return ret;
            } else {
               CustomBlock customBlock = ServerCustomBlocks.getCustomBlock(ResourceLocation.parse(customBlockPlacer));
               if (customBlock == null) {
                  ChatUtils.error("Unknown custom block: " + customBlockPlacer);
                  return ret;
               } else {
                  CustomBlockState placedState = customBlock.getCustomStateForPlacement(blockPlaceContext, Capability.FORCE_PLACE.isEnabled());
                  BlockState vanillaState = placedState == null ? null : placedState.getVanillaState();
                  if (vanillaState != null && !this.canPlace(blockPlaceContext, vanillaState)) {
                     vanillaState = null;
                  }

                  if (vanillaState == null) {
                     if (ret != null) {
                        SpecialPlace.markCustomPlacement(blockPlaceContext.getLevel());
                        return null;
                     }
                  } else if (ret != vanillaState) {
                     SpecialPlace.markCustomPlacement(blockPlaceContext.getLevel());
                     return vanillaState;
                  }

                  return ret;
               }
            }
         }
      }
   }

   @ModifyReturnValue(
      method = {"canPlace"},
      at = {@At("RETURN")}
   )
   public boolean canPlace(boolean canPlace, @Local(argsOnly = true) BlockPlaceContext blockPlaceContext) {
      return !canPlace && SpecialPlace.isForcePlacing(blockPlaceContext.getLevel()) ? true : canPlace;
   }
}
