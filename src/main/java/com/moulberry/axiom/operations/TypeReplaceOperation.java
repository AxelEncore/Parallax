package com.moulberry.axiom.operations;

import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class TypeReplaceOperation {
   public static void replace(FamilyMap.AxiomBlockFamily from, FamilyMap.AxiomBlockFamily to) {
      SelectionBuffer selectionBuffer = Selection.getSelectionBuffer();
      if (selectionBuffer instanceof SelectionBuffer.AABB aabb) {
         replaceAABB(aabb, from, to);
      } else if (selectionBuffer instanceof SelectionBuffer.Set set) {
         replaceSet(set, from, to);
      }
   }

   private static <T extends Comparable<T>> BlockState copyProperty(BlockState blockState, BlockState blockState2, Property<T> property) {
      return (BlockState)blockState2.setValue(property, blockState.getValue(property));
   }

   private static void replaceAABB(SelectionBuffer.AABB aabb, FamilyMap.AxiomBlockFamily from, FamilyMap.AxiomBlockFamily to) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         int changeCount = 0;
         int minX = aabb.min().getX();
         int minY = aabb.min().getY();
         int minZ = aabb.min().getZ();
         int maxX = aabb.max().getX();
         int maxY = aabb.max().getY();
         int maxZ = aabb.max().getZ();
         minY = Math.max(world.getMinBuildHeight(), minY);
         maxY = Math.min(world.getMaxBuildHeight() - 1, maxY);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
               for (int z = minZ; z <= maxZ; z++) {
                  BlockState blockState = world.getBlockState(mutableBlockPos.set(x, y, z));
                  if (blockState.getBlock() != Blocks.VOID_AIR) {
                     if (blockState.getBlock() == from.getBaseBlock()) {
                        setOperation.set(x, y, z, to.getBaseBlock().defaultBlockState());
                        previousBlocksForUndo.set(x, y, z, blockState);
                        changeCount++;
                     } else if (FamilyMap.getFamilyFor(blockState.getBlock()) == from) {
                        FamilyMap.AxiomBlockVariant variant = FamilyMap.getVariantFor(blockState.getBlock());
                        BlockState toState = null;
                        Block toBlock = to.getVariant(variant);
                        if (toBlock == null) {
                           Map<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> converters = FamilyMap.getVariantConverters(variant);
                           if (converters != null) {
                              for (Entry<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> entry : converters.entrySet()) {
                                 toBlock = to.getVariant(entry.getKey());
                                 if (toBlock != null) {
                                    toState = toBlock.defaultBlockState();

                                    for (Property<?> property : blockState.getProperties()) {
                                       if (toState.hasProperty(property)) {
                                          toState = copyProperty(blockState, toState, property);
                                       }
                                    }

                                    toState = entry.getValue().convert(blockState, toState, mutableBlockPos.immutable(), world);
                                    break;
                                 }
                              }
                           }
                        } else {
                           toState = toBlock.defaultBlockState();

                           for (Property<?> propertyx : blockState.getProperties()) {
                              if (toState.hasProperty(propertyx)) {
                                 toState = copyProperty(blockState, toState, propertyx);
                              }
                           }
                        }

                        if (toState != null) {
                           setOperation.set(x, y, z, toState);
                           previousBlocksForUndo.set(x, y, z, blockState);
                           changeCount++;
                        }
                     }
                  }
               }
            }
         }

         if (changeCount != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount);
            String blockName = AxiomI18n.get(to.getBaseBlock().getDescriptionId());
            String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
            RegionHelper.pushBlockBufferChange(setOperation, aabb.center(), historyDescription, null);
         }
      }
   }

   private static void replaceSet(SelectionBuffer.Set set, FamilyMap.AxiomBlockFamily from, FamilyMap.AxiomBlockFamily to) {
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         BlockBuffer setOperation = new BlockBuffer();
         BlockBuffer previousBlocksForUndo = new BlockBuffer();
         IntWrapper changeCount = new IntWrapper();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         set.selectionRegion.forEach((x, y, z) -> {
            BlockState blockState = world.getBlockState(mutableBlockPos.set(x, y, z));
            if (blockState.getBlock() != Blocks.VOID_AIR) {
               if (blockState.getBlock() == from.getBaseBlock()) {
                  setOperation.set(x, y, z, to.getBaseBlock().defaultBlockState());
                  previousBlocksForUndo.set(x, y, z, blockState);
                  changeCount.value++;
               } else if (FamilyMap.getFamilyFor(blockState.getBlock()) == from) {
                  FamilyMap.AxiomBlockVariant variant = FamilyMap.getVariantFor(blockState.getBlock());
                  BlockState toState = null;
                  Block toBlock = to.getVariant(variant);
                  if (toBlock != null) {
                     toState = toBlock.defaultBlockState();

                     for (Property<?> property : blockState.getProperties()) {
                        if (toState.hasProperty(property)) {
                           toState = copyProperty(blockState, toState, property);
                        }
                     }
                  } else {
                     Map<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> converters = FamilyMap.getVariantConverters(variant);
                     if (converters != null) {
                        for (Entry<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> entry : converters.entrySet()) {
                           toBlock = to.getVariant(entry.getKey());
                           if (toBlock != null) {
                              toState = toBlock.defaultBlockState();

                              for (Property<?> propertyx : blockState.getProperties()) {
                                 if (toState.hasProperty(propertyx)) {
                                    toState = copyProperty(blockState, toState, propertyx);
                                 }
                              }

                              toState = entry.getValue().convert(blockState, toState, mutableBlockPos.immutable(), world);
                              break;
                           }
                        }
                     }
                  }

                  if (toState == null) {
                     return;
                  }

                  setOperation.set(x, y, z, toState);
                  previousBlocksForUndo.set(x, y, z, blockState);
                  changeCount.value++;
               }
            }
         });
         if (changeCount.value != 0) {
            String countString = NumberFormat.getInstance().format((long)changeCount.value);
            String blockName = AxiomI18n.get(to.getBaseBlock().getDescriptionId());
            String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
            RegionHelper.pushBlockBufferChange(setOperation, set.selectionRegion.getCenter(), historyDescription, null);
         }
      }
   }
}
