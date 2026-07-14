package com.moulberry.axiom.mixin.special_place;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({BedBlock.class})
public class MixinBedBlock {
   @Redirect(
      method = {"setPlacedBy"},
      at = @At(
         value = "FIELD",
         target = "Lnet/minecraft/world/level/Level;isClientSide:Z",
         opcode = 180
      )
   )
   public boolean setPlacedByIsClientSide(Level instance) {
      return false;
   }
}
