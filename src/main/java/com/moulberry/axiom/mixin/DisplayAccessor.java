package com.moulberry.axiom.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Display.class})
public interface DisplayAccessor {
   @Accessor("DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID")
   static EntityDataAccessor<Integer> getDataTransformationInterpolationDurationId() {
      throw new AssertionError();
   }

   @Accessor("DATA_POS_ROT_INTERPOLATION_DURATION_ID")
   static EntityDataAccessor<Integer> getDataPosRotInterpolationId() {
      throw new AssertionError();
   }
}
