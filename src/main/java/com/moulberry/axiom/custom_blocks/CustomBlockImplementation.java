package com.moulberry.axiom.custom_blocks;

import com.moulberry.axiom.custom_blocks.update.CustomBlockPlacementLogic;
import com.moulberry.axiom.utils.IntWrapper;
import java.util.Collection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class CustomBlockImplementation implements CustomBlock {
   private final ResourceLocation resourceLocation;
   private final String translationKey;
   private final ItemStack pickBlockItemStack;
   private final boolean sendServerPickBlockIfPossible;
   private final boolean preventRightClickInteraction;
   private final boolean preventShapeUpdates;
   private final CustomBlockState defaultBlockState;
   private final StateDefinition<CustomBlock, CustomBlockStateImplementation> stateDefinition;
   private final List<CustomBlockPlacementLogic> placementLogics;

   public CustomBlockImplementation(
      ResourceLocation resourceLocation,
      String translationKey,
      List<Property<?>> properties,
      List<BlockState> vanillaStates,
      @Nullable ItemStack pickBlockItemStack,
      boolean sendServerPickBlockIfPossible,
      boolean preventRightClickInteraction,
      boolean preventShapeUpdates,
      List<CustomBlockPlacementLogic> placementLogics
   ) {
      IntWrapper index = new IntWrapper();
      Builder<CustomBlock, CustomBlockStateImplementation> builder = new Builder(this);
      properties.forEach(xva$0 -> builder.add(new Property[]{xva$0}));
      StateDefinition<CustomBlock, CustomBlockStateImplementation> stateDefinition = builder.create(
         customBlock -> (CustomBlockStateImplementation)customBlock.axiom$defaultCustomState(), (block, immutableMap, mapCodec) -> {
            if (index.value >= vanillaStates.size()) {
               throw new RuntimeException(resourceLocation + " - faulty definition: not enough vanilla states");
            } else {
               BlockState vanillaState = vanillaStates.get(index.value);
               index.value++;
               return new CustomBlockStateImplementation(block, vanillaState, immutableMap, mapCodec);
            }
         }
      );
      if (index.value < vanillaStates.size()) {
         throw new RuntimeException(resourceLocation + " - faulty definition: too many vanilla states");
      } else {
         this.resourceLocation = resourceLocation;
         this.translationKey = translationKey;
         this.pickBlockItemStack = pickBlockItemStack;
         this.sendServerPickBlockIfPossible = sendServerPickBlockIfPossible;
         this.preventRightClickInteraction = preventRightClickInteraction;
         this.preventShapeUpdates = preventShapeUpdates;
         this.stateDefinition = stateDefinition;
         this.defaultBlockState = (CustomBlockState)stateDefinition.any();
         this.placementLogics = placementLogics;

         for (CustomBlockPlacementLogic placementLogic : this.placementLogics) {
            if (!placementLogic.hasRequiredProperties(this.stateDefinition.getProperties())) {
               throw new RuntimeException(resourceLocation + " - state definition doesn't contain required property for " + placementLogic.getClass());
            }
         }
      }
   }

   @Override
   public ResourceLocation axiom$getIdentifier() {
      return this.resourceLocation;
   }

   @Override
   public String axiom$translationKey() {
      return this.translationKey;
   }

   @Override
   public ItemStack axiom$customPickBlockStack() {
      return this.pickBlockItemStack == null ? null : this.pickBlockItemStack.copy();
   }

   @Override
   public ItemStack axiom$asItemStack() {
      ItemStack customPickBlockStack = this.axiom$customPickBlockStack();
      return customPickBlockStack != null && !customPickBlockStack.isEmpty()
         ? customPickBlockStack
         : new ItemStack(this.defaultBlockState.getVanillaState().getBlock().asItem());
   }

   @Override
   public boolean sendServerPickBlockIfPossible() {
      return this.sendServerPickBlockIfPossible;
   }

   @Override
   public boolean preventRightClickInteraction() {
      return this.preventRightClickInteraction;
   }

   @Override
   public boolean preventShapeUpdates() {
      return this.preventShapeUpdates;
   }

   public StateDefinition<CustomBlock, CustomBlockStateImplementation> getStateDefinition() {
      return this.stateDefinition;
   }

   @Override
   public Collection<Property<?>> axiom$getProperties() {
      return this.stateDefinition.getProperties();
   }

   @Override
   public List<CustomBlockState> axiom$getPossibleCustomStates() {
      return (List<CustomBlockState>)(List<?>)this.stateDefinition.getPossibleStates();
   }

   @Override
   public CustomBlockState axiom$defaultCustomState() {
      return this.defaultBlockState;
   }

   @Nullable
   @Override
   public CustomBlockState getCustomStateForPlacement(BlockPlaceContext blockPlaceContext, boolean force) {
      CustomBlockState blockState = this.defaultBlockState;

      for (CustomBlockPlacementLogic updater : this.placementLogics) {
         CustomBlockState newBlockState = updater.getStateForPlacement(blockState, blockPlaceContext);
         if (newBlockState == null) {
            if (force) {
               continue;
            }
            break;
         } else {
            blockState = newBlockState;
         }
      }

      return blockState;
   }
}
