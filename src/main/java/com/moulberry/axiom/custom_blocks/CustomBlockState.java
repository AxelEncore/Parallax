package com.moulberry.axiom.custom_blocks;

import com.moulberry.axiom.editor.palette.CustomBlockStateOrTombstone;
import java.util.Collection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public interface CustomBlockState extends CustomBlockStateOrTombstone {
   BlockState getVanillaState();

   CustomBlock getCustomBlock();

   <T extends Comparable<T>> boolean axiomHasProperty(Property<T> var1);

   <T extends Comparable<T>> T getProperty(Property<T> var1);

   <T extends Comparable<T>> CustomBlockState setPropertyUnsafe(Property<T> var1, Comparable<?> var2);

   default Collection<Property<?>> getProperties() {
      return this.getCustomBlock().axiom$getProperties();
   }
}
