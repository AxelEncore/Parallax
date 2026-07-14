package com.moulberry.axiom.hooks;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface MinecraftExt {
   void axiom$setRightClickDelay(int var1);

   int axiom$getRightClickDelay();

   void axiom$addCustomNbtData(ItemStack var1, BlockEntity var2, RegistryAccess var3);

   void axiom$pushMainRenderTarget(RenderTarget var1);

   void axiom$popMainRenderTarget();
}
