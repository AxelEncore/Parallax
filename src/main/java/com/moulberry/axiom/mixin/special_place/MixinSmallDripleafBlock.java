package com.moulberry.axiom.mixin.special_place;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SmallDripleafBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SmallDripleafBlock.class})
public class MixinSmallDripleafBlock {
   @Redirect(
      method = {"setPlacedBy"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;isClientSide()Z"
      )
   )
   public boolean setPlacedByIsClientSide(Level instance) {
      return false;
   }
}
