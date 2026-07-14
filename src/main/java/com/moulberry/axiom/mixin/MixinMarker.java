package com.moulberry.axiom.mixin;

import com.moulberry.axiom.hooks.MarkerEntityExt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Marker.class})
public abstract class MixinMarker extends Entity implements MarkerEntityExt {
   @Shadow
   private CompoundTag data;

   public MixinMarker() {
      super(null, null);
   }

   @Override
   public CompoundTag axiom$getData() {
      return this.data;
   }
}
