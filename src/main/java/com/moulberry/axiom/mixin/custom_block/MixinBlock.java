package com.moulberry.axiom.mixin.custom_block;

import com.moulberry.axiom.DefaultBlocks;
import com.moulberry.axiom.custom_blocks.CustomBlock;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({Block.class})
public abstract class MixinBlock extends BlockBehaviour implements CustomBlock {
   @Shadow
   @Final
   private Reference<Block> builtInRegistryHolder;
   @Shadow
   @Final
   protected StateDefinition<Block, BlockState> stateDefinition;

   public MixinBlock(Properties properties) {
      super(properties);
   }

   @Shadow
   public abstract BlockState defaultBlockState();

   @Shadow
   public abstract StateDefinition<Block, BlockState> getStateDefinition();

   @Shadow
   public abstract String getDescriptionId();

   @Shadow
   public abstract Item asItem();

   @Override
   public ResourceLocation axiom$getIdentifier() {
      return this.builtInRegistryHolder.key().location();
   }

   @Override
   public String axiom$translationKey() {
      return this.getDescriptionId();
   }

   @Override
   public CustomBlockState axiom$defaultCustomState() {
      return (CustomBlockState)DefaultBlocks.forBlock((Block)(Object)this);
   }

   @Override
   public Collection<Property<?>> axiom$getProperties() {
      return this.stateDefinition.getProperties();
   }

   @Override
   public ItemStack axiom$asItemStack() {
      return new ItemStack(this.asItem());
   }

   @Override
   public List<CustomBlockState> axiom$getPossibleCustomStates() {
      return (List<CustomBlockState>)(List<?>)this.getStateDefinition().getPossibleStates();
   }
}
