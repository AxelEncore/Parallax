package com.moulberry.axiom.mixin.custom_block;

import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BlockState.class})
public abstract class MixinBlockState extends BlockStateBase implements CustomBlockState {
   protected MixinBlockState() {
      super(null, null, null);
   }

   @Override
   public BlockState getVanillaState() {
      return (BlockState)(Object)this;
   }

   @Override
   public CustomBlock getCustomBlock() {
      return (CustomBlock)(Object)this.getBlock();
   }

   @Override
   public <T extends Comparable<T>> T getProperty(Property<T> property) {
      return (T)(Object)this.getValue(property);
   }

   @Override
   public <T extends Comparable<T>> boolean axiomHasProperty(Property<T> property) {
      return this.hasProperty(property);
   }

   @Override
   public <T extends Comparable<T>> CustomBlockState setPropertyUnsafe(Property<T> property, Comparable<?> value) {
      return (CustomBlockState)(Object)this.setValue(property, (T)value);
   }
}
