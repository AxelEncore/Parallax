package com.moulberry.axiom.block_maps;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class FlowerPotBlockMap {
   private static final Map<Block, Block> ASSOCIATED_POT_BLOCK_MAP = new HashMap<>();
   private static final Map<Block, Block> ASSOCIATED_FLOWER_BLOCK_MAP = new HashMap<>();

   private static void process(Block flowerBlock, Block flowerPotBlock) {
      ASSOCIATED_POT_BLOCK_MAP.put(flowerBlock, flowerPotBlock);
      ASSOCIATED_FLOWER_BLOCK_MAP.put(flowerPotBlock, flowerBlock);
   }

   public static Block getAssociatedPotBlock(Block flowerBlock) {
      return ASSOCIATED_POT_BLOCK_MAP.get(flowerBlock);
   }

   public static Block getAssociatedFlowerBlock(Block flowerBlock) {
      return ASSOCIATED_FLOWER_BLOCK_MAP.get(flowerBlock);
   }

   static {
      process(Blocks.TORCHFLOWER, Blocks.POTTED_TORCHFLOWER);
      process(Blocks.OAK_SAPLING, Blocks.POTTED_OAK_SAPLING);
      process(Blocks.SPRUCE_SAPLING, Blocks.POTTED_SPRUCE_SAPLING);
      process(Blocks.BIRCH_SAPLING, Blocks.POTTED_BIRCH_SAPLING);
      process(Blocks.JUNGLE_SAPLING, Blocks.POTTED_JUNGLE_SAPLING);
      process(Blocks.ACACIA_SAPLING, Blocks.POTTED_ACACIA_SAPLING);
      process(Blocks.CHERRY_SAPLING, Blocks.POTTED_CHERRY_SAPLING);
      process(Blocks.DARK_OAK_SAPLING, Blocks.POTTED_DARK_OAK_SAPLING);
      process(Blocks.MANGROVE_PROPAGULE, Blocks.POTTED_MANGROVE_PROPAGULE);
      process(Blocks.FERN, Blocks.POTTED_FERN);
      process(Blocks.DANDELION, Blocks.POTTED_DANDELION);
      process(Blocks.POPPY, Blocks.POTTED_POPPY);
      process(Blocks.BLUE_ORCHID, Blocks.POTTED_BLUE_ORCHID);
      process(Blocks.ALLIUM, Blocks.POTTED_ALLIUM);
      process(Blocks.AZURE_BLUET, Blocks.POTTED_AZURE_BLUET);
      process(Blocks.RED_TULIP, Blocks.POTTED_RED_TULIP);
      process(Blocks.ORANGE_TULIP, Blocks.POTTED_ORANGE_TULIP);
      process(Blocks.WHITE_TULIP, Blocks.POTTED_WHITE_TULIP);
      process(Blocks.PINK_TULIP, Blocks.POTTED_PINK_TULIP);
      process(Blocks.OXEYE_DAISY, Blocks.POTTED_OXEYE_DAISY);
      process(Blocks.CORNFLOWER, Blocks.POTTED_CORNFLOWER);
      process(Blocks.LILY_OF_THE_VALLEY, Blocks.POTTED_LILY_OF_THE_VALLEY);
      process(Blocks.WITHER_ROSE, Blocks.POTTED_WITHER_ROSE);
      process(Blocks.RED_MUSHROOM, Blocks.POTTED_RED_MUSHROOM);
      process(Blocks.BROWN_MUSHROOM, Blocks.POTTED_BROWN_MUSHROOM);
      process(Blocks.DEAD_BUSH, Blocks.POTTED_DEAD_BUSH);
      process(Blocks.CACTUS, Blocks.POTTED_CACTUS);
      process(Blocks.BAMBOO, Blocks.POTTED_BAMBOO);
      process(Blocks.CRIMSON_FUNGUS, Blocks.POTTED_CRIMSON_FUNGUS);
      process(Blocks.WARPED_FUNGUS, Blocks.POTTED_WARPED_FUNGUS);
      process(Blocks.CRIMSON_ROOTS, Blocks.POTTED_CRIMSON_ROOTS);
      process(Blocks.WARPED_ROOTS, Blocks.POTTED_WARPED_ROOTS);
      process(Blocks.AZALEA, Blocks.POTTED_AZALEA);
      process(Blocks.FLOWERING_AZALEA, Blocks.POTTED_FLOWERING_AZALEA);
   }
}
