package com.moulberry.axiom.datagen;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.MushroomBlock;

public class ColourBlockTagGenerator {
   private static Block get(ResourceLocation resourceLocation) {
      return (Block)BuiltInRegistries.BLOCK.get(resourceLocation);
   }

   public static void addTags(Provider arg, AxiomTagCreator<Block> builderFunction) {
      Map<DyeColor, LinkedHashSet<Block>> map = new LinkedHashMap<>();

      for (DyeColor dyeColor : DyeColor.values()) {
         LinkedHashSet<Block> set = map.computeIfAbsent(dyeColor, k -> new LinkedHashSet<>());
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_banner")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_concrete_powder")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_concrete")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_bed")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_stained_glass")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_stained_glass_pane")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_wool")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_carpet")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_terracotta")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_banner")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_wall_banner")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_shulker_box")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_glazed_terracotta")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_candle")));
         set.add(get(ResourceLocation.fromNamespaceAndPath("minecraft", dyeColor + "_candle_cake")));
      }

      label138:
      for (Block block : BuiltInRegistries.BLOCK) {
         for (LinkedHashSet<Block> value : map.values()) {
            if (value.contains(block)) {
               continue label138;
            }
         }

         String path = block.builtInRegistryHolder().key().location().getPath();
         if (!path.contains("red_sand")
            && !path.contains("red_nether")
            && !path.equals("blue_ice")
            && !path.equals("pink_petals")
            && !(block instanceof FlowerBlock)
            && !(block instanceof FlowerPotBlock)
            && !(block instanceof MushroomBlock)
            && !(block instanceof HugeMushroomBlock)) {
            for (DyeColor dyeColor : DyeColor.values()) {
               String color = dyeColor.getName();
               if ((!color.equals("red") || !path.contains("powered"))
                  && (!color.equals("red") || !path.contains("redstone"))
                  && (!color.equals("lime") || !path.contains("slime"))
                  && (!color.equals("black") || !path.contains("blackstone"))
                  && (!color.equals("red") || !path.contains("weathered"))
                  && path.contains(color)) {
                  throw new RuntimeException(block + " contains '" + dyeColor + "' are you sure it's not part of the color:" + dyeColor + " tag?");
               }
            }
         }
      }

      AxiomTagAppender<Block> coloredBlocksBuilder = builderFunction.create(
         TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("axiom", "colored_blocks"))
      );

      for (Entry<DyeColor, LinkedHashSet<Block>> entry : map.entrySet()) {
         TagKey<Block> key = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("color", entry.getKey().getName()));
         AxiomTagAppender<Block> builder = builderFunction.create(key);

         for (Block block : entry.getValue()) {
            builder.add(block);
         }

         coloredBlocksBuilder.addTag(key);
      }
   }
}
