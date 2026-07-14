package com.moulberry.axiom.custom_blocks;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.state.properties.Property;

public class StringProperty extends Property<String> {
   private final List<String> values;

   public StringProperty(String name, List<String> values) {
      super(name, String.class);
      this.values = List.copyOf(values);
   }

   public Collection<String> getPossibleValues() {
      return this.values;
   }

   public Optional<String> getValue(String string) {
      return this.values.contains(string) ? Optional.of(string) : Optional.empty();
   }

   public String getName(String comparable) {
      return comparable;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return o instanceof StringProperty stringProperty && super.equals(o) ? this.values.equals(stringProperty.values) : false;
      }
   }

   public int generateHashCode() {
      int i = super.generateHashCode();
      return 31 * i + this.values.hashCode();
   }
}
