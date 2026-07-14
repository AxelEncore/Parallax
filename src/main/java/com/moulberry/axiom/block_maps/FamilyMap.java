package com.moulberry.axiom.block_maps;

import com.moulberry.axiom.capabilities.ReplaceMode;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.utils.BlockShapeUpdater;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.BlockFamily.Variant;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FamilyMap {
   private static final Map<Block, FamilyMap.AxiomBlockFamily> ANY_BLOCK_TO_FAMILY_MAP = new HashMap<>();
   private static final Map<Block, FamilyMap.AxiomBlockFamily> BASE_BLOCK_TO_FAMILY_MAP = new HashMap<>();
   private static final Map<Block, FamilyMap.AxiomBlockVariant> BLOCK_TO_VARIANT_MAP = new HashMap<>();
   private static final Map<Variant, FamilyMap.AxiomBlockVariant> AXIOM_VARIANTS = new HashMap<>();
   private static final Map<FamilyMap.AxiomBlockVariant, Map<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter>> variantConverters;

   public static FamilyMap.AxiomBlockFamily getFamilyFor(Block block) {
      return ANY_BLOCK_TO_FAMILY_MAP.get(block);
   }

   public static FamilyMap.AxiomBlockFamily getFamilyForBase(Block block) {
      return BASE_BLOCK_TO_FAMILY_MAP.get(block);
   }

   public static FamilyMap.AxiomBlockVariant getVariantFor(Block block) {
      return BLOCK_TO_VARIANT_MAP.get(block);
   }

   public static BlockState typeReplace(BlockState from, FamilyMap.AxiomBlockFamily to, BlockPos pos, Level level) {
      FamilyMap.AxiomBlockVariant variant = getVariantFor(from.getBlock());
      if (!BASE_BLOCK_TO_FAMILY_MAP.containsKey(from.getBlock()) && variant != null) {
         BlockState toState = null;
         Block toBlock = to.getVariant(variant);
         if (toBlock == null) {
            Map<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> converters = getVariantConverters(variant);
            if (converters != null) {
               for (Entry<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> entry : converters.entrySet()) {
                  toBlock = to.getVariant(entry.getKey());
                  if (toBlock != null) {
                     toState = toBlock.defaultBlockState();

                     for (Property<?> property : from.getProperties()) {
                        if (toState.hasProperty(property)) {
                           toState = ReplaceMode.copyProperty((CustomBlockState)from, toState, property);
                        }
                     }

                     toState = entry.getValue().convert(from, toState, pos, level);
                     break;
                  }
               }
            }
         } else {
            toState = toBlock.defaultBlockState();

            for (Property<?> propertyx : from.getProperties()) {
               if (toState.hasProperty(propertyx)) {
                  toState = ReplaceMode.copyProperty((CustomBlockState)from, toState, propertyx);
               }
            }
         }

         return toState;
      } else {
         BlockState toState = to.getBaseBlock().defaultBlockState();

         for (Property<?> propertyxx : from.getProperties()) {
            if (toState.hasProperty(propertyxx)) {
               toState = ReplaceMode.copyProperty((CustomBlockState)from, toState, propertyxx);
            }
         }

         return toState;
      }
   }

   public static Map<FamilyMap.AxiomBlockVariant, FamilyMap.VariantConverter> getVariantConverters(FamilyMap.AxiomBlockVariant variant) {
      return variantConverters.get(variant);
   }

   static {
      boolean throwErrors = AxiomPlatform.isDevelopment();

      for (Variant variant : Variant.values()) {
         AXIOM_VARIANTS.put(variant, new FamilyMap.VanillaBlockVariant(variant));
      }

      Map<WoodType, FamilyMap.VanillaBlockFamily> familiesForSignType = new HashMap<>();
      TreeMap<Block, FamilyMap.AxiomBlockFamily> familiesToRegister = new TreeMap<>(Comparator.comparingInt(BuiltInRegistries.BLOCK::getId));
      BlockFamilies.getAllFamilies().forEach(blockFamily -> {
         FamilyMap.VanillaBlockFamily vanillaBlockFamily = new FamilyMap.VanillaBlockFamily(blockFamily, new HashMap<>());
         if (blockFamily.get(Variant.SIGN) instanceof SignBlock signBlock) {
            familiesForSignType.put(signBlock.type(), vanillaBlockFamily);
         }

         familiesToRegister.put(blockFamily.getBaseBlock(), vanillaBlockFamily);
      });
      EnumMap<DyeColor, Block> fullGlassBlocks = new EnumMap<>(DyeColor.class);
      EnumMap<DyeColor, Block> glassPaneBlocks = new EnumMap<>(DyeColor.class);
      EnumMap<DyeColor, Block> woolBlocks = new EnumMap<>(DyeColor.class);
      woolBlocks.put(DyeColor.WHITE, Blocks.WHITE_WOOL);
      woolBlocks.put(DyeColor.ORANGE, Blocks.ORANGE_WOOL);
      woolBlocks.put(DyeColor.MAGENTA, Blocks.MAGENTA_WOOL);
      woolBlocks.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL);
      woolBlocks.put(DyeColor.YELLOW, Blocks.YELLOW_WOOL);
      woolBlocks.put(DyeColor.LIME, Blocks.LIME_WOOL);
      woolBlocks.put(DyeColor.PINK, Blocks.PINK_WOOL);
      woolBlocks.put(DyeColor.GRAY, Blocks.GRAY_WOOL);
      woolBlocks.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL);
      woolBlocks.put(DyeColor.CYAN, Blocks.CYAN_WOOL);
      woolBlocks.put(DyeColor.PURPLE, Blocks.PURPLE_WOOL);
      woolBlocks.put(DyeColor.BLUE, Blocks.BLUE_WOOL);
      woolBlocks.put(DyeColor.BROWN, Blocks.BROWN_WOOL);
      woolBlocks.put(DyeColor.GREEN, Blocks.GREEN_WOOL);
      woolBlocks.put(DyeColor.RED, Blocks.RED_WOOL);
      woolBlocks.put(DyeColor.BLACK, Blocks.BLACK_WOOL);
      EnumMap<DyeColor, Block> woolCarpetBlocks = new EnumMap<>(DyeColor.class);
      EnumMap<DyeColor, Block> bedBlocks = new EnumMap<>(DyeColor.class);

      for (Block block : BuiltInRegistries.BLOCK) {
         try {
            if (block instanceof StainedGlassBlock stainedGlassBlock) {
               if (stainedGlassBlock.getColor() != null) {
                  fullGlassBlocks.put(stainedGlassBlock.getColor(), block);
               }
            } else if (block instanceof StainedGlassPaneBlock stainedGlassPaneBlock) {
               if (stainedGlassPaneBlock.getColor() != null) {
                  glassPaneBlocks.put(stainedGlassPaneBlock.getColor(), block);
               }
            } else if (block instanceof CeilingHangingSignBlock ceilingHangingSignBlock) {
               FamilyMap.VanillaBlockFamily family = familiesForSignType.get(ceilingHangingSignBlock.type());
               if (family != null) {
                  family.additionalVariants.put(FamilyMap.CustomVariants.CEILING_HANGING_SIGN, block);
               }
            } else if (block instanceof WallHangingSignBlock wallHangingSignBlock) {
               FamilyMap.VanillaBlockFamily family = familiesForSignType.get(wallHangingSignBlock.type());
               if (family != null) {
                  family.additionalVariants.put(FamilyMap.CustomVariants.WALL_HANGING_SIGN, block);
               }
            } else if (block instanceof WoolCarpetBlock woolCarpetBlock) {
               if (woolCarpetBlock.getColor() != null) {
                  woolCarpetBlocks.put(woolCarpetBlock.getColor(), block);
               }
            } else if (block instanceof BedBlock bedBlock && bedBlock.getColor() != null) {
               bedBlocks.put(bedBlock.getColor(), block);
            }
         } catch (Exception var17) {
         }
      }

      for (DyeColor dyeColor : DyeColor.values()) {
         Block base = fullGlassBlocks.get(dyeColor);
         Block pane = glassPaneBlocks.get(dyeColor);
         if (base != null && pane != null && !familiesToRegister.containsKey(base)) {
            FamilyMap.CustomBlockFamily family = new FamilyMap.CustomBlockFamily(base, Map.of(FamilyMap.CustomVariants.PANE, pane));
            familiesToRegister.put(base, family);
         }
      }

      for (DyeColor dyeColorx : DyeColor.values()) {
         Block base = woolBlocks.get(dyeColorx);
         Block carpet = woolCarpetBlocks.get(dyeColorx);
         Block bed = bedBlocks.get(dyeColorx);
         if (base != null && carpet != null && bed != null && !familiesToRegister.containsKey(base)) {
            FamilyMap.CustomBlockFamily family = new FamilyMap.CustomBlockFamily(
               base, Map.of(FamilyMap.CustomVariants.CARPET, carpet, FamilyMap.CustomVariants.BED, bed)
            );
            familiesToRegister.put(base, family);
         }
      }

      if (!familiesToRegister.containsKey(Blocks.SMOOTH_STONE)) {
         familiesToRegister.put(
            Blocks.SMOOTH_STONE, new FamilyMap.CustomBlockFamily(Blocks.SMOOTH_STONE, Map.of(AXIOM_VARIANTS.get(Variant.SLAB), Blocks.SMOOTH_STONE_SLAB))
         );
      }

      if (!familiesToRegister.containsKey(Blocks.GLASS)) {
         familiesToRegister.put(Blocks.GLASS, new FamilyMap.CustomBlockFamily(Blocks.GLASS, Map.of(FamilyMap.CustomVariants.PANE, Blocks.GLASS_PANE)));
      }

      List<BaseWoodSet> woods = new ArrayList<>();
      woods.add(new BaseWoodSet(Blocks.OAK_WOOD, Blocks.OAK_LOG, Blocks.OAK_PLANKS));
      woods.add(new BaseWoodSet(Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS));
      woods.add(new BaseWoodSet(Blocks.ACACIA_WOOD, Blocks.ACACIA_LOG, Blocks.ACACIA_PLANKS));
      woods.add(new BaseWoodSet(Blocks.CHERRY_WOOD, Blocks.CHERRY_LOG, Blocks.CHERRY_PLANKS));
      woods.add(new BaseWoodSet(Blocks.BIRCH_WOOD, Blocks.BIRCH_LOG, Blocks.BIRCH_PLANKS));
      woods.add(new BaseWoodSet(Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LOG, Blocks.JUNGLE_PLANKS));
      woods.add(new BaseWoodSet(Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LOG, Blocks.SPRUCE_PLANKS));
      woods.add(new BaseWoodSet(Blocks.WARPED_HYPHAE, Blocks.WARPED_STEM, Blocks.WARPED_PLANKS));
      woods.add(new BaseWoodSet(Blocks.CRIMSON_HYPHAE, Blocks.CRIMSON_STEM, Blocks.CRIMSON_PLANKS));
      woods.add(new BaseWoodSet(Blocks.MANGROVE_WOOD, Blocks.MANGROVE_LOG, Blocks.MANGROVE_PLANKS));

      for (BaseWoodSet woodSet : woods) {
         HashMap<FamilyMap.AxiomBlockVariant, Block> variants = new HashMap<>();
         variants.put(FamilyMap.CustomVariants.LOG, woodSet.log);
         Block strippedLog = (Block)AxeItem.STRIPPABLES.get(woodSet.log);
         if (strippedLog != null) {
            variants.put(FamilyMap.CustomVariants.STRIPPED_LOG, strippedLog);
         }

         Block strippedWood = (Block)AxeItem.STRIPPABLES.get(woodSet.wood);
         if (strippedWood != null) {
            variants.put(FamilyMap.CustomVariants.STRIPPED_WOOD, strippedWood);
         }

         if (familiesToRegister.get(woodSet.plank) instanceof FamilyMap.VanillaBlockFamily vanillaBlockFamily) {
            for (Entry<Variant, Block> entry : vanillaBlockFamily.family.getVariants().entrySet()) {
               variants.put(AXIOM_VARIANTS.get(entry.getKey()), entry.getValue());
            }

            variants.putAll(vanillaBlockFamily.additionalVariants);
         }

         FamilyMap.CustomBlockFamily family = new FamilyMap.CustomBlockFamily(woodSet.wood, variants);
         familiesToRegister.put(woodSet.wood, family);
      }

      if (throwErrors) {
         for (Entry<Block, Block> entry : AxeItem.STRIPPABLES.entrySet()) {
            if (entry.getKey() != Blocks.BAMBOO_BLOCK && entry.getKey().builtInRegistryHolder().key().location().getNamespace().equals("minecraft")) {
               boolean present = false;

               for (BaseWoodSet wood : woods) {
                  if (wood.wood == entry.getKey() || wood.log == entry.getKey()) {
                     present = true;
                     break;
                  }
               }

               if (!present) {
                  throw new RuntimeException("Missing base wood set for " + entry.getKey());
               }
            }
         }
      }

      for (FamilyMap.AxiomBlockFamily family : familiesToRegister.values()) {
         if (family instanceof FamilyMap.VanillaBlockFamily vanillaBlockFamily) {
            if (!BASE_BLOCK_TO_FAMILY_MAP.containsKey(family.getBaseBlock())) {
               ANY_BLOCK_TO_FAMILY_MAP.put(family.getBaseBlock(), family);
               BASE_BLOCK_TO_FAMILY_MAP.put(family.getBaseBlock(), family);
               BLOCK_TO_VARIANT_MAP.remove(family.getBaseBlock());
            }

            for (Entry<Variant, Block> entryx : vanillaBlockFamily.family.getVariants().entrySet()) {
               if (!ANY_BLOCK_TO_FAMILY_MAP.containsKey(entryx.getValue())) {
                  ANY_BLOCK_TO_FAMILY_MAP.put(entryx.getValue(), family);
                  BLOCK_TO_VARIANT_MAP.put(entryx.getValue(), AXIOM_VARIANTS.get(entryx.getKey()));
               }
            }

            for (Entry<FamilyMap.AxiomBlockVariant, Block> entryxx : vanillaBlockFamily.additionalVariants.entrySet()) {
               if (!ANY_BLOCK_TO_FAMILY_MAP.containsKey(entryxx.getValue())) {
                  ANY_BLOCK_TO_FAMILY_MAP.put(entryxx.getValue(), family);
                  BLOCK_TO_VARIANT_MAP.put(entryxx.getValue(), entryxx.getKey());
               }
            }
         }
      }

      for (FamilyMap.AxiomBlockFamily familyx : familiesToRegister.values()) {
         if (familyx instanceof FamilyMap.CustomBlockFamily custom) {
            if (!BASE_BLOCK_TO_FAMILY_MAP.containsKey(familyx.getBaseBlock())) {
               ANY_BLOCK_TO_FAMILY_MAP.put(familyx.getBaseBlock(), familyx);
               BASE_BLOCK_TO_FAMILY_MAP.put(familyx.getBaseBlock(), familyx);
               BLOCK_TO_VARIANT_MAP.remove(familyx.getBaseBlock());
            }

            for (Entry<FamilyMap.AxiomBlockVariant, Block> entryxxx : custom.variants.entrySet()) {
               if (!ANY_BLOCK_TO_FAMILY_MAP.containsKey(entryxxx.getValue())) {
                  ANY_BLOCK_TO_FAMILY_MAP.put(entryxxx.getValue(), familyx);
                  BLOCK_TO_VARIANT_MAP.put(entryxxx.getValue(), entryxxx.getKey());
               }
            }
         }
      }

      variantConverters = new HashMap<>();
      FamilyMap.VariantConverter identity = (from, to, pos, blockGetter) -> to;
      FamilyMap.VariantConverter fenceToWall = (from, to, pos, blockGetter) -> {
         boolean north = (Boolean)from.getValue(BlockStateProperties.NORTH);
         boolean east = (Boolean)from.getValue(BlockStateProperties.EAST);
         boolean south = (Boolean)from.getValue(BlockStateProperties.SOUTH);
         boolean west = (Boolean)from.getValue(BlockStateProperties.WEST);
         BlockPos abovePos = pos.above();
         BlockState above = blockGetter.getBlockState(abovePos);
         VoxelShape aboveVoxelShape = above.getCollisionShape(blockGetter, abovePos).getFaceShape(Direction.DOWN);
         to = BlockShapeUpdater.updateWallSides(to, aboveVoxelShape, north, east, south, west);
         return BlockShapeUpdater.updateWallUp(to, above, aboveVoxelShape);
      };
      FamilyMap.VariantConverter wallToFence = (from, to, pos, blockGetter) -> {
         boolean north = from.getValue(BlockStateProperties.NORTH_WALL) != WallSide.NONE;
         boolean east = from.getValue(BlockStateProperties.EAST_WALL) != WallSide.NONE;
         boolean south = from.getValue(BlockStateProperties.SOUTH_WALL) != WallSide.NONE;
         boolean west = from.getValue(BlockStateProperties.WEST_WALL) != WallSide.NONE;
         to = (BlockState)to.setValue(BlockStateProperties.NORTH, north);
         to = (BlockState)to.setValue(BlockStateProperties.EAST, east);
         to = (BlockState)to.setValue(BlockStateProperties.SOUTH, south);
         return (BlockState)to.setValue(BlockStateProperties.WEST, west);
      };
      variantConverters.put(
         AXIOM_VARIANTS.get(Variant.FENCE), Map.of(AXIOM_VARIANTS.get(Variant.CUSTOM_FENCE), identity, AXIOM_VARIANTS.get(Variant.WALL), fenceToWall)
      );
      variantConverters.put(
         AXIOM_VARIANTS.get(Variant.WALL), Map.of(AXIOM_VARIANTS.get(Variant.FENCE), wallToFence, AXIOM_VARIANTS.get(Variant.CUSTOM_FENCE), wallToFence)
      );
      variantConverters.put(AXIOM_VARIANTS.get(Variant.CUSTOM_FENCE), Map.of(AXIOM_VARIANTS.get(Variant.FENCE), identity));
      variantConverters.put(AXIOM_VARIANTS.get(Variant.CUSTOM_FENCE_GATE), Map.of(AXIOM_VARIANTS.get(Variant.FENCE_GATE), identity));
      variantConverters.put(AXIOM_VARIANTS.get(Variant.FENCE_GATE), Map.of(AXIOM_VARIANTS.get(Variant.CUSTOM_FENCE_GATE), identity));
   }

   public interface AxiomBlockFamily {
      Block getBaseBlock();

      @Nullable
      Block getVariant(FamilyMap.AxiomBlockVariant var1);
   }

   public interface AxiomBlockVariant {
   }

   private record CustomBlockFamily(Block base, Map<FamilyMap.AxiomBlockVariant, Block> variants) implements FamilyMap.AxiomBlockFamily {
      @Override
      public Block getBaseBlock() {
         return this.base;
      }

      @Nullable
      @Override
      public Block getVariant(FamilyMap.AxiomBlockVariant variant) {
         return this.variants.get(variant);
      }
   }

   private static enum CustomVariants implements FamilyMap.AxiomBlockVariant {
      PANE,
      CEILING_HANGING_SIGN,
      WALL_HANGING_SIGN,
      CARPET,
      BED,
      LOG,
      WOOD,
      STRIPPED_LOG,
      STRIPPED_WOOD;
   }

   private record VanillaBlockFamily(BlockFamily family, Map<FamilyMap.AxiomBlockVariant, Block> additionalVariants) implements FamilyMap.AxiomBlockFamily {
      @Override
      public Block getBaseBlock() {
         return this.family.getBaseBlock();
      }

      @Nullable
      @Override
      public Block getVariant(FamilyMap.AxiomBlockVariant variant) {
         if (variant instanceof FamilyMap.VanillaBlockVariant vanilla) {
            Block block = this.family.get(vanilla.variant);
            if (block != null) {
               return block;
            }
         }

         return this.additionalVariants.get(variant);
      }
   }

   private record VanillaBlockVariant(Variant variant) implements FamilyMap.AxiomBlockVariant {
   }

   @FunctionalInterface
   public interface VariantConverter {
      BlockState convert(BlockState var1, BlockState var2, BlockPos var3, BlockGetter var4);
   }

   // Reconstructed: Vineflower dropped this local class body, leaving only its usages.
   private static final class BaseWoodSet {
      final Block wood;
      final Block log;
      final Block plank;

      BaseWoodSet(Block wood, Block log, Block plank) {
         this.wood = wood;
         this.log = log;
         this.plank = plank;
      }
   }
}
