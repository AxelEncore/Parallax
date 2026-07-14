package com.moulberry.axiom.utils;

import com.moulberry.axiom.VersionUtilsNbt;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemStackDataHelper {
   public static Tag save(ItemStack itemStack, RegistryAccess registryAccess) {
      return itemStack.saveOptional(registryAccess);
   }

   @NotNull
   public static ItemStack loadOrEmpty(CompoundTag tag, RegistryAccess registryAccess) {
      return Objects.requireNonNullElse(ItemStack.parseOptional(registryAccess, tag), ItemStack.EMPTY);
   }

   public static void setName(ItemStack itemStack, Component name) {
      itemStack.set(DataComponents.ITEM_NAME, name);
   }

   public static void setLore(ItemStack itemStack, List<Component> lore) {
      itemStack.set(DataComponents.LORE, new ItemLore(lore));
   }

   public static boolean hasBlockStateTag(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem ? itemStack.has(DataComponents.BLOCK_STATE) : false;
   }

   public static BlockState updateBlockStateFromTag(BlockState blockState, ItemStack itemStack) {
      if (!itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem) {
         BlockItemStateProperties properties = (BlockItemStateProperties)itemStack.get(DataComponents.BLOCK_STATE);
         return properties == null ? blockState : properties.apply(blockState);
      } else {
         return blockState;
      }
   }

   public static <T extends Comparable<T>> BlockState updateStateString(BlockState blockState, Property<T> property, String string) {
      return property.getValue(string).map(comparable -> (BlockState)blockState.setValue(property, comparable)).orElse(blockState);
   }

   public static void setBlockStateTag(ItemStack itemStack, Map<String, String> map) {
      if (!itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem) {
         itemStack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(map));
      }
   }

   @Nullable
   public static String getCustomBlockPlacer(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return null;
      } else {
         CustomData customData = (CustomData)itemStack.get(DataComponents.CUSTOM_DATA);
         if (customData == null) {
            return null;
         } else {
            CompoundTag tag = customData.getUnsafe();
            CompoundTag axiom = tag.getCompound("Axiom");
            if (axiom.isEmpty()) {
               return null;
            } else {
               String customBlockPlacer = VersionUtilsNbt.helperCompoundTagGetStringOr(axiom, "CustomBlockPlacer", "");
               return customBlockPlacer.isEmpty() ? null : customBlockPlacer;
            }
         }
      }
   }

   public static void setCustomBlockPlacer(@NotNull ItemStack itemStack, String customBlockPlacer) {
      if (!itemStack.isEmpty()) {
         CustomData.update(DataComponents.CUSTOM_DATA, itemStack, tag -> {
            CompoundTag axiom = new CompoundTag();
            axiom.putString("CustomBlockPlacer", customBlockPlacer);
            tag.put("Axiom", axiom);
         });
      }
   }

   @Nullable
   public static CompoundTag getEntityPlacer(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return null;
      } else {
         CustomData customData = (CustomData)itemStack.get(DataComponents.CUSTOM_DATA);
         if (customData == null) {
            return null;
         } else {
            CompoundTag tag = customData.getUnsafe();
            CompoundTag axiom = tag.getCompound("Axiom");
            if (axiom.isEmpty()) {
               return null;
            } else {
               CompoundTag entityPlacer = axiom.getCompound("EntityPlacer");
               return entityPlacer.isEmpty() ? null : entityPlacer;
            }
         }
      }
   }

   public static void setEntityPlacer(ItemStack itemStack, CompoundTag entityPlacer) {
      CustomData.update(DataComponents.CUSTOM_DATA, itemStack, tag -> {
         CompoundTag axiom = new CompoundTag();
         axiom.put("EntityPlacer", entityPlacer);
         tag.put("Axiom", axiom);
      });
      itemStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
   }

   public static void setCustomModelData(ItemStack itemStack, int customModelData) {
      itemStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
   }

   public static void setHoverName(ItemStack itemStack, Component component) {
      itemStack.set(DataComponents.CUSTOM_NAME, component);
   }

   public static boolean canBeDyed(ItemStack itemStack) {
      return itemStack.has(DataComponents.DYED_COLOR);
   }

   public static int getDyeColor(ItemStack itemStack) {
      return DyedItemColor.getOrDefault(itemStack, -6265536) & 16777215;
   }

   public static void setDyeColor(ItemStack itemStack, int rgb) {
      boolean showInTooltip = true;
      DyedItemColor dyedColor = (DyedItemColor)itemStack.get(DataComponents.DYED_COLOR);
      if (dyedColor != null) {
         showInTooltip = dyedColor.showInTooltip();
      }

      itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(rgb & 16777215, showInTooltip));
   }
}
