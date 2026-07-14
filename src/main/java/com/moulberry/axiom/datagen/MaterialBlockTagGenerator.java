package com.moulberry.axiom.datagen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.RootsBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

public class MaterialBlockTagGenerator {
   public static final Map<Block, String> blockToMaterialMap = new LinkedHashMap<>();
   public static final Map<BlockSetType, String> setTypeToMaterialMap = Map.ofEntries(
      Map.entry(BlockSetType.IRON, "iron"),
      Map.entry(BlockSetType.COPPER, "copper"),
      Map.entry(BlockSetType.GOLD, "gold"),
      Map.entry(BlockSetType.STONE, "stone"),
      Map.entry(BlockSetType.POLISHED_BLACKSTONE, "blackstone"),
      Map.entry(BlockSetType.OAK, "oak"),
      Map.entry(BlockSetType.SPRUCE, "spruce"),
      Map.entry(BlockSetType.BIRCH, "birch"),
      Map.entry(BlockSetType.ACACIA, "acacia"),
      Map.entry(BlockSetType.CHERRY, "cherry"),
      Map.entry(BlockSetType.JUNGLE, "jungle"),
      Map.entry(BlockSetType.DARK_OAK, "dark_oak"),
      Map.entry(BlockSetType.CRIMSON, "crimson"),
      Map.entry(BlockSetType.WARPED, "warped"),
      Map.entry(BlockSetType.MANGROVE, "mangrove"),
      Map.entry(BlockSetType.BAMBOO, "bamboo")
   );
   public static final Map<WoodType, String> woodTypeToMaterialMap = Map.ofEntries(
      Map.entry(WoodType.OAK, "oak"),
      Map.entry(WoodType.SPRUCE, "spruce"),
      Map.entry(WoodType.BIRCH, "birch"),
      Map.entry(WoodType.ACACIA, "acacia"),
      Map.entry(WoodType.CHERRY, "cherry"),
      Map.entry(WoodType.JUNGLE, "jungle"),
      Map.entry(WoodType.DARK_OAK, "dark_oak"),
      Map.entry(WoodType.CRIMSON, "crimson"),
      Map.entry(WoodType.WARPED, "warped"),
      Map.entry(WoodType.MANGROVE, "mangrove"),
      Map.entry(WoodType.BAMBOO, "bamboo")
   );
   private static final Map<String, LinkedHashSet<Block>> materialMap = new HashMap<>();

   public static void addTags(Provider arg, AxiomTagCreator<Block> builderFunction) {
      for (Block block : BuiltInRegistries.BLOCK) {
         ResourceLocation location = ((ResourceKey)block.builtInRegistryHolder().unwrapKey().get()).location();
         if (block instanceof DoorBlock doorBlock) {
            String material = setTypeToMaterialMap.get(doorBlock.type);
            forMaterial(material).add(block);
         } else if (block instanceof TrapDoorBlock trapDoorBlock) {
            String material = setTypeToMaterialMap.get(trapDoorBlock.type);
            forMaterial(material).add(block);
         } else if (block instanceof BasePressurePlateBlock pressurePlateBlock) {
            String material = setTypeToMaterialMap.get(pressurePlateBlock.type);
            forMaterial(material).add(block);
         } else if (block instanceof StairBlock stairBlock) {
            String material = blockToMaterialMap.get(stairBlock.base);
            if (material == null) {
               throw new RuntimeException("Missing material for: " + stairBlock.base);
            }

            Set<Block> set = forMaterial(material);
            set.add(block);
            String ns = location.getNamespace();
            String path = location.getPath();
            BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(ns, path.replace("_stairs", "_slab"))).ifPresent(set::add);
            BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(ns, path.replace("_stairs", "_wall"))).ifPresent(set::add);
            BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(ns, path.replace("_stairs", "_fence"))).ifPresent(set::add);
         } else if (blockToMaterialMap.containsKey(block)) {
            forMaterial(blockToMaterialMap.get(block)).add(block);
         } else if (block instanceof GlazedTerracottaBlock || location.getPath().endsWith("_terracotta")) {
            forMaterial("terracotta").add(block);
         }
      }

      for (Entry<Block, String> entry : blockToMaterialMap.entrySet()) {
         ResourceLocation location = ((ResourceKey)entry.getKey().builtInRegistryHolder().unwrapKey().get()).location();
         String ns = location.getNamespace();
         String path = location.getPath();
         BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath(ns, path + "_slab"))
            .ifPresent(blockx -> forMaterial(entry.getValue()).add(blockx));
         BuiltInRegistries.BLOCK
            .getOptional(ResourceLocation.fromNamespaceAndPath(ns, path + "_wall"))
            .ifPresent(blockx -> forMaterial(entry.getValue()).add(blockx));
      }

      BlockSetType.values().forEach(type -> {
         String materialx = setTypeToMaterialMap.get(type);
         String name = type.name();
         List<Optional<Block>> blocks = new ArrayList<>();
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_stairs")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_slab")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_fence")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_fence_gate")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_door")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_trapdoor")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_pressure_plate")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_button")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_sign")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_wall_sign")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_hanging_sign")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_wall_hanging_sign")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_shelf")));
         LinkedHashSet<Block> builder = forMaterial(materialx);
         blocks.forEach(optional -> optional.ifPresent(builder::add));
      });
      WoodType.values().forEach(type -> {
         String materialx = woodTypeToMaterialMap.get(type);
         String name = type.name();
         List<Optional<Block>> blocks = new ArrayList<>();
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_log")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_wood")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", "stripped_" + name + "_log")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", "stripped_" + name + "_wood")));
         blocks.add(BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath("minecraft", name + "_planks")));
         LinkedHashSet<Block> builder = forMaterial(materialx);
         blocks.forEach(optional -> optional.ifPresent(builder::add));
      });

      for (Entry<String, LinkedHashSet<Block>> entry : materialMap.entrySet()) {
         boolean containsStairVariant = false;
         boolean containsSlabVariant = false;

         for (Block blockx : entry.getValue()) {
            containsStairVariant |= blockx instanceof StairBlock;
            containsSlabVariant |= blockx instanceof SlabBlock;
         }

         if ((containsStairVariant || containsSlabVariant) && (!containsStairVariant || !containsSlabVariant)) {
            throw new RuntimeException(
               entry.getKey()
                  + " contained one variant, but not the other\ncontainsStairVariant = "
                  + containsStairVariant
                  + "\ncontainsSlabVariant = "
                  + containsSlabVariant
                  + "\n"
            );
         }

         AxiomTagAppender<Block> builder = builderFunction.create(
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("material", entry.getKey()))
         );

         for (Block blockx : entry.getValue()) {
            builder.add(blockx);
         }
      }

      AxiomTagAppender<Block> builder = builderFunction.create(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("material", "wood")));
      WoodType.values().forEach(type -> {
         String materialx = woodTypeToMaterialMap.get(type);
         builder.addTag(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("material", materialx)));
      });

      for (Block blockx : BuiltInRegistries.BLOCK) {
         boolean inMaterial = false;

         for (LinkedHashSet<Block> set : materialMap.values()) {
            if (set.contains(blockx)) {
               if (inMaterial) {
                  throw new RuntimeException(blockx + " is in two materials at once!");
               }

               inMaterial = true;
            }
         }

         if (!inMaterial) {
            if (blockx instanceof StairBlock || blockx instanceof SlabBlock || blockx instanceof WallBlock) {
               throw new RuntimeException(blockx + " is missing a material!");
            }

            if (!(blockx instanceof SaplingBlock)
               && !(blockx instanceof BambooSaplingBlock)
               && !(blockx instanceof FlowerPotBlock)
               && !(blockx instanceof LeavesBlock)
               && !(blockx instanceof GrindstoneBlock)
               && !(blockx instanceof StonecutterBlock)
               && !(blockx instanceof NyliumBlock)
               && !(blockx instanceof FungusBlock)
               && !(blockx instanceof RootsBlock)) {
               String path = ((ResourceKey)blockx.builtInRegistryHolder().unwrapKey().get()).location().getPath();
               if (!path.endsWith("_ore") && !path.equals("warped_wart_block") && !path.equals("crimson_wart_block") && !path.equals("lodestone")) {
                  for (String material : blockToMaterialMap.values()) {
                     if ((!material.equals("stone") || !path.contains("redstone"))
                        && (!material.equals("stone") || !path.contains("glowstone"))
                        && (!material.equals("stone") || !path.contains("sandstone"))
                        && (!material.equals("stone") || !path.contains("dripstone"))
                        && (!material.equals("copper") || !path.equals("copper_torch") && !path.equals("copper_wall_torch"))
                        && (!material.equals("gold") || !path.contains("dandelion"))
                        && path.contains(material)) {
                        throw new RuntimeException(blockx + " contains '" + material + "'. Should it be part of the '" + material + "' material?");
                     }
                  }
               }
            }
         }
      }
   }

   private static LinkedHashSet<Block> forMaterial(String material) {
      if (!blockToMaterialMap.values().contains(material)) {
         throw new RuntimeException("Unknown material: " + material);
      } else {
         return materialMap.computeIfAbsent(material, type -> new LinkedHashSet<>());
      }
   }

   static {
      blockToMaterialMap.put(Blocks.STONE, "stone");
      blockToMaterialMap.put(Blocks.INFESTED_STONE, "stone");
      blockToMaterialMap.put(Blocks.SMOOTH_STONE, "stone");
      blockToMaterialMap.put(Blocks.COBBLESTONE, "cobblestone");
      blockToMaterialMap.put(Blocks.MOSSY_COBBLESTONE, "cobblestone");
      blockToMaterialMap.put(Blocks.INFESTED_COBBLESTONE, "cobblestone");
      blockToMaterialMap.put(Blocks.DEEPSLATE, "deepslate");
      blockToMaterialMap.put(Blocks.INFESTED_DEEPSLATE, "deepslate");
      blockToMaterialMap.put(Blocks.CHISELED_DEEPSLATE, "deepslate");
      blockToMaterialMap.put(Blocks.REINFORCED_DEEPSLATE, "deepslate");
      blockToMaterialMap.put(Blocks.COBBLED_DEEPSLATE, "deepslate");
      blockToMaterialMap.put(Blocks.POLISHED_DEEPSLATE, "deepslate");
      blockToMaterialMap.put(Blocks.DEEPSLATE_TILES, "deepslate");
      blockToMaterialMap.put(Blocks.CRACKED_DEEPSLATE_TILES, "deepslate");
      blockToMaterialMap.put(Blocks.DEEPSLATE_BRICKS, "deepslate");
      blockToMaterialMap.put(Blocks.CRACKED_DEEPSLATE_BRICKS, "deepslate");
      blockToMaterialMap.put(Blocks.BRICKS, "bricks");
      blockToMaterialMap.put(Blocks.STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.MOSSY_STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.CRACKED_STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.CHISELED_STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.INFESTED_STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.INFESTED_MOSSY_STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.INFESTED_CRACKED_STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.INFESTED_CHISELED_STONE_BRICKS, "stone_brick");
      blockToMaterialMap.put(Blocks.MUD_BRICKS, "mud_brick");
      blockToMaterialMap.put(Blocks.NETHER_BRICKS, "nether_brick");
      blockToMaterialMap.put(Blocks.CHISELED_NETHER_BRICKS, "nether_brick");
      blockToMaterialMap.put(Blocks.CRACKED_NETHER_BRICKS, "nether_brick");
      blockToMaterialMap.put(Blocks.RED_NETHER_BRICKS, "nether_brick");
      blockToMaterialMap.put(Blocks.SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.SMOOTH_SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.CHISELED_SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.CUT_SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.RED_SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.SMOOTH_RED_SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.CHISELED_RED_SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.CUT_RED_SANDSTONE, "sandstone");
      blockToMaterialMap.put(Blocks.OAK_PLANKS, "oak");
      blockToMaterialMap.put(Blocks.PETRIFIED_OAK_SLAB, "oak");
      blockToMaterialMap.put(Blocks.SPRUCE_PLANKS, "spruce");
      blockToMaterialMap.put(Blocks.BIRCH_PLANKS, "birch");
      blockToMaterialMap.put(Blocks.ACACIA_PLANKS, "acacia");
      blockToMaterialMap.put(Blocks.BAMBOO, "bamboo");
      blockToMaterialMap.put(Blocks.BAMBOO_PLANKS, "bamboo");
      blockToMaterialMap.put(Blocks.BAMBOO_BLOCK, "bamboo");
      blockToMaterialMap.put(Blocks.BAMBOO_MOSAIC, "bamboo");
      blockToMaterialMap.put(Blocks.STRIPPED_BAMBOO_BLOCK, "bamboo");
      blockToMaterialMap.put(Blocks.CHERRY_PLANKS, "cherry");
      blockToMaterialMap.put(Blocks.CRIMSON_PLANKS, "crimson");
      blockToMaterialMap.put(Blocks.CRIMSON_STEM, "crimson");
      blockToMaterialMap.put(Blocks.STRIPPED_CRIMSON_STEM, "crimson");
      blockToMaterialMap.put(Blocks.CRIMSON_HYPHAE, "crimson");
      blockToMaterialMap.put(Blocks.STRIPPED_CRIMSON_HYPHAE, "crimson");
      blockToMaterialMap.put(Blocks.WARPED_PLANKS, "warped");
      blockToMaterialMap.put(Blocks.WARPED_STEM, "warped");
      blockToMaterialMap.put(Blocks.STRIPPED_WARPED_STEM, "warped");
      blockToMaterialMap.put(Blocks.WARPED_HYPHAE, "warped");
      blockToMaterialMap.put(Blocks.STRIPPED_WARPED_HYPHAE, "warped");
      blockToMaterialMap.put(Blocks.DARK_OAK_PLANKS, "dark_oak");
      blockToMaterialMap.put(Blocks.JUNGLE_PLANKS, "jungle");
      blockToMaterialMap.put(Blocks.MANGROVE_PLANKS, "mangrove");
      blockToMaterialMap.put(Blocks.MANGROVE_ROOTS, "mangrove");
      blockToMaterialMap.put(Blocks.MUDDY_MANGROVE_ROOTS, "mangrove");
      blockToMaterialMap.put(Blocks.QUARTZ_BLOCK, "quartz");
      blockToMaterialMap.put(Blocks.SMOOTH_QUARTZ, "quartz");
      blockToMaterialMap.put(Blocks.CHISELED_QUARTZ_BLOCK, "quartz");
      blockToMaterialMap.put(Blocks.QUARTZ_PILLAR, "quartz");
      blockToMaterialMap.put(Blocks.QUARTZ_BRICKS, "quartz");
      blockToMaterialMap.put(Blocks.PRISMARINE, "prismarine");
      blockToMaterialMap.put(Blocks.PRISMARINE_BRICKS, "prismarine");
      blockToMaterialMap.put(Blocks.DARK_PRISMARINE, "prismarine");
      blockToMaterialMap.put(Blocks.PURPUR_BLOCK, "purpur");
      blockToMaterialMap.put(Blocks.PURPUR_PILLAR, "purpur");
      blockToMaterialMap.put(Blocks.GRANITE, "granite");
      blockToMaterialMap.put(Blocks.POLISHED_GRANITE, "granite");
      blockToMaterialMap.put(Blocks.DIORITE, "diorite");
      blockToMaterialMap.put(Blocks.POLISHED_DIORITE, "diorite");
      blockToMaterialMap.put(Blocks.ANDESITE, "andesite");
      blockToMaterialMap.put(Blocks.POLISHED_ANDESITE, "andesite");
      blockToMaterialMap.put(Blocks.BLACKSTONE, "blackstone");
      blockToMaterialMap.put(Blocks.GILDED_BLACKSTONE, "blackstone");
      blockToMaterialMap.put(Blocks.POLISHED_BLACKSTONE, "blackstone");
      blockToMaterialMap.put(Blocks.CHISELED_POLISHED_BLACKSTONE, "blackstone");
      blockToMaterialMap.put(Blocks.POLISHED_BLACKSTONE_BRICKS, "blackstone");
      blockToMaterialMap.put(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, "blackstone");
      blockToMaterialMap.put(Blocks.END_STONE, "end_stone");
      blockToMaterialMap.put(Blocks.END_STONE_BRICKS, "end_stone");
      blockToMaterialMap.put(Blocks.COPPER_BLOCK, "copper");
      blockToMaterialMap.put(Blocks.RAW_COPPER_BLOCK, "copper");
      blockToMaterialMap.put(Blocks.CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.OXIDIZED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.OXIDIZED_CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WEATHERED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WEATHERED_CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.EXPOSED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.EXPOSED_CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_COPPER_BLOCK, "copper");
      blockToMaterialMap.put(Blocks.WAXED_WEATHERED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_EXPOSED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_OXIDIZED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_OXIDIZED_CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_WEATHERED_CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_EXPOSED_CUT_COPPER, "copper");
      blockToMaterialMap.put(Blocks.OXIDIZED_CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WEATHERED_CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.EXPOSED_CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_OXIDIZED_CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_WEATHERED_CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_EXPOSED_CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.WAXED_CHISELED_COPPER, "copper");
      blockToMaterialMap.put(Blocks.COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.EXPOSED_COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.WEATHERED_COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.OXIDIZED_COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.WAXED_COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.WAXED_EXPOSED_COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.WAXED_WEATHERED_COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.WAXED_OXIDIZED_COPPER_BULB, "copper");
      blockToMaterialMap.put(Blocks.COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.EXPOSED_COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.WEATHERED_COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.OXIDIZED_COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.WAXED_COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.WAXED_EXPOSED_COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.WAXED_WEATHERED_COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.WAXED_OXIDIZED_COPPER_GRATE, "copper");
      blockToMaterialMap.put(Blocks.IRON_BLOCK, "iron");
      blockToMaterialMap.put(Blocks.RAW_IRON_BLOCK, "iron");
      blockToMaterialMap.put(Blocks.IRON_BARS, "iron");
      blockToMaterialMap.put(Blocks.GOLD_BLOCK, "gold");
      blockToMaterialMap.put(Blocks.RAW_GOLD_BLOCK, "gold");
      blockToMaterialMap.put(Blocks.TERRACOTTA, "terracotta");
      blockToMaterialMap.put(Blocks.TUFF, "tuff");
      blockToMaterialMap.put(Blocks.CHISELED_TUFF, "tuff");
      blockToMaterialMap.put(Blocks.POLISHED_TUFF, "tuff");
      blockToMaterialMap.put(Blocks.TUFF_BRICKS, "tuff");
      blockToMaterialMap.put(Blocks.CHISELED_TUFF_BRICKS, "tuff");
      blockToMaterialMap.put(Blocks.CHAIN, "iron");
   }
}
