package com.moulberry.axiom.custom_blocks;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

public class CustomBlockStateImplementation extends StateHolder<CustomBlock, CustomBlockStateImplementation> implements CustomBlockState {
   private final BlockState vanillaState;
   private final CustomBlock block;

   public CustomBlockStateImplementation(
      CustomBlock block,
      BlockState vanillaState,
      Reference2ObjectArrayMap<Property<?>, Comparable<?>> immutableMap,
      MapCodec<CustomBlockStateImplementation> mapCodec
   ) {
      super(block, immutableMap, mapCodec);
      this.vanillaState = vanillaState;
      this.block = block;
   }

   @Override
   public BlockState getVanillaState() {
      return this.vanillaState;
   }

   @Override
   public CustomBlock getCustomBlock() {
      return this.block;
   }

   @Override
   public <T extends Comparable<T>> boolean axiomHasProperty(Property<T> property) {
      return this.hasProperty(property);
   }

   @Override
   public <T extends Comparable<T>> T getProperty(Property<T> property) {
      return (T)this.getValue(property);
   }

   @Override
   public <T extends Comparable<T>> CustomBlockState setPropertyUnsafe(Property<T> property, Comparable<?> value) {
      return (CustomBlockState)this.setValue(property, (T)value);
   }
}
