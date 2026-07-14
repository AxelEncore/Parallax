package com.moulberry.axiom.utils;

import com.google.common.collect.Iterables;
import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class BlockManipulation {
   public static CustomBlockState applyRandomProperties(CustomBlockState blockState, @Nullable Set<Property<?>> randomizedProperties) {
      if (randomizedProperties != null && !randomizedProperties.isEmpty()) {
         Random random = ThreadLocalRandom.current();

         for (Property<?> randomizedProperty : randomizedProperties) {
            if (blockState.axiomHasProperty(randomizedProperty)) {
               Collection<? extends Comparable<?>> possibleValues = randomizedProperty.getPossibleValues();
               Comparable<?> value = (Comparable<?>)Iterables.get(possibleValues, random.nextInt(possibleValues.size()));
               blockState = blockState.setPropertyUnsafe(randomizedProperty, value);
            }
         }
      }

      return blockState;
   }

   public static void setWithCopyPropertiesAndTypeReplace(
      ChunkedBlockRegion chunkedBlockRegion,
      int x,
      int y,
      int z,
      CustomBlockState blockState,
      boolean typeReplace,
      boolean copyProperties,
      @Nullable Set<Property<?>> randomizedProperties,
      MaskContext maskContext,
      MutableBlockPos mutableBlockPos
   ) {
      FamilyMap.AxiomBlockFamily typeReplaceFamily = typeReplace ? FamilyMap.getFamilyForBase(blockState.getVanillaState().getBlock()) : null;
      if (typeReplaceFamily != null) {
         BlockState existing = maskContext.getBlockState(x, y, z);
         BlockState to = FamilyMap.typeReplace(existing, typeReplaceFamily, mutableBlockPos.set(x, y, z), Minecraft.getInstance().level);
         if (to != null) {
            chunkedBlockRegion.addBlock(x, y, z, to);
            return;
         }
      }

      blockState = applyRandomProperties(blockState, randomizedProperties);
      if (copyProperties) {
         BlockState existing = maskContext.getBlockState(x, y, z);

         for (Property<?> property : existing.getProperties()) {
            if (blockState.axiomHasProperty(property)) {
               blockState = blockState.setPropertyUnsafe(property, existing.getValue(property));
            }
         }
      }

      chunkedBlockRegion.addBlock(x, y, z, blockState.getVanillaState());
   }
}
