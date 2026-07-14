package com.moulberry.axiom.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({MultiPlayerGameMode.class})
public interface MultiPlayerGameModeAccessor {
   @Accessor("destroyDelay")
   void setDestroyDelay(int var1);

   @Accessor("destroyDelay")
   int getDestroyDelay();
}
