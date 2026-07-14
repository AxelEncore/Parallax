package com.moulberry.axiom.custom_blocks;

import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public interface CustomBlock {
   ResourceLocation axiom$getIdentifier();

   String axiom$translationKey();

   CustomBlockState axiom$defaultCustomState();

   Collection<Property<?>> axiom$getProperties();

   List<CustomBlockState> axiom$getPossibleCustomStates();

   @Nullable
   default ItemStack axiom$customPickBlockStack() {
      return null;
   }

   ItemStack axiom$asItemStack();

   default boolean sendServerPickBlockIfPossible() {
      return true;
   }

   default boolean preventRightClickInteraction() {
      return false;
   }

   default boolean preventShapeUpdates() {
      return false;
   }

   @Nullable
   default CustomBlockState getCustomStateForPlacement(BlockPlaceContext blockPlaceContext, boolean force) {
      return this.axiom$defaultCustomState();
   }
}
