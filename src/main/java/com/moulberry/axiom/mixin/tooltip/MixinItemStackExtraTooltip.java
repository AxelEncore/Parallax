package com.moulberry.axiom.mixin.tooltip;

import com.moulberry.axiom.hooks.BlockItemStatePropertiesExt;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemStack.class})
public abstract class MixinItemStackExtraTooltip implements DataComponentHolder {
   @Inject(
      method = {"addAttributeTooltips"},
      at = {@At("HEAD")}
   )
   public void addAttributeTooltips(Consumer<Component> consumer, Player player, CallbackInfo ci) {
      BlockItemStateProperties properties = (BlockItemStateProperties)(Object)this.get(DataComponents.BLOCK_STATE);
      if (properties != null) {
         ((BlockItemStatePropertiesExt)(Object)properties).axiom$addToTooltip(consumer, Set.of());
      }
   }
}
