package com.moulberry.axiom.utils;

import net.minecraft.world.level.block.state.properties.Property;

public record PropertyWithValue<T extends Comparable<T>>(Property<T> property, T value) {
   public static <T extends Comparable<T>> PropertyWithValue<T> create(Property<T> property, Object value) {
      return new PropertyWithValue<>(property, (T)value);
   }
}
