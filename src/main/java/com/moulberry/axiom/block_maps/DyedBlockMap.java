package com.moulberry.axiom.block_maps;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class DyedBlockMap {
   private static final Map<Block, EnumMap<DyeColor, Block>> ASSOCIATED_DYED_BLOCK_MAP = new HashMap<>();

   private static EnumMap<DyeColor, Block> process(Entry<DyeColor, Block>... entries) {
      EnumMap<DyeColor, Block> enumMap = new EnumMap<>(DyeColor.class);

      for (Entry<DyeColor, Block> entry : entries) {
         enumMap.put(entry.getKey(), entry.getValue());
      }

      for (Entry<DyeColor, Block> entry : entries) {
         ASSOCIATED_DYED_BLOCK_MAP.put(entry.getValue(), enumMap);
      }

      return enumMap;
   }

   public static EnumMap<DyeColor, Block> getAssociatedDyedBlocks(Block block) {
      return ASSOCIATED_DYED_BLOCK_MAP.get(block);
   }

   static {
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_WOOL),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_WOOL),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_WOOL),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_WOOL),
         Map.entry(DyeColor.LIME, Blocks.LIME_WOOL),
         Map.entry(DyeColor.PINK, Blocks.PINK_WOOL),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_WOOL),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WOOL),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_WOOL),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_WOOL),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_WOOL),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_WOOL),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_WOOL),
         Map.entry(DyeColor.RED, Blocks.RED_WOOL),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_WOOL)
      );
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_CARPET),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_CARPET),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_CARPET),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_CARPET),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_CARPET),
         Map.entry(DyeColor.LIME, Blocks.LIME_CARPET),
         Map.entry(DyeColor.PINK, Blocks.PINK_CARPET),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_CARPET),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_CARPET),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_CARPET),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_CARPET),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_CARPET),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_CARPET),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_CARPET),
         Map.entry(DyeColor.RED, Blocks.RED_CARPET),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_CARPET)
      );
      ASSOCIATED_DYED_BLOCK_MAP.put(
         Blocks.TERRACOTTA,
         process(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_TERRACOTTA),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_TERRACOTTA),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_TERRACOTTA),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_TERRACOTTA),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_TERRACOTTA),
            Map.entry(DyeColor.LIME, Blocks.LIME_TERRACOTTA),
            Map.entry(DyeColor.PINK, Blocks.PINK_TERRACOTTA),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_TERRACOTTA),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_TERRACOTTA),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_TERRACOTTA),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_TERRACOTTA),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_TERRACOTTA),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_TERRACOTTA),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_TERRACOTTA),
            Map.entry(DyeColor.RED, Blocks.RED_TERRACOTTA),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_TERRACOTTA)
         )
      );
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_CONCRETE),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_CONCRETE),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_CONCRETE),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_CONCRETE),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_CONCRETE),
         Map.entry(DyeColor.LIME, Blocks.LIME_CONCRETE),
         Map.entry(DyeColor.PINK, Blocks.PINK_CONCRETE),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_CONCRETE),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_CONCRETE),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_CONCRETE),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_CONCRETE),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_CONCRETE),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_CONCRETE),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_CONCRETE),
         Map.entry(DyeColor.RED, Blocks.RED_CONCRETE),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_CONCRETE)
      );
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_CONCRETE_POWDER),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_CONCRETE_POWDER),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_CONCRETE_POWDER),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_CONCRETE_POWDER),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_CONCRETE_POWDER),
         Map.entry(DyeColor.LIME, Blocks.LIME_CONCRETE_POWDER),
         Map.entry(DyeColor.PINK, Blocks.PINK_CONCRETE_POWDER),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_CONCRETE_POWDER),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_CONCRETE_POWDER),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_CONCRETE_POWDER),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_CONCRETE_POWDER),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_CONCRETE_POWDER),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_CONCRETE_POWDER),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_CONCRETE_POWDER),
         Map.entry(DyeColor.RED, Blocks.RED_CONCRETE_POWDER),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_CONCRETE_POWDER)
      );
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.LIME, Blocks.LIME_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.PINK, Blocks.PINK_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.RED, Blocks.RED_GLAZED_TERRACOTTA),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_GLAZED_TERRACOTTA)
      );
      ASSOCIATED_DYED_BLOCK_MAP.put(
         Blocks.GLASS,
         process(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_STAINED_GLASS),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_STAINED_GLASS),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_STAINED_GLASS),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_STAINED_GLASS),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_STAINED_GLASS),
            Map.entry(DyeColor.LIME, Blocks.LIME_STAINED_GLASS),
            Map.entry(DyeColor.PINK, Blocks.PINK_STAINED_GLASS),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_STAINED_GLASS),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_STAINED_GLASS),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_STAINED_GLASS),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_STAINED_GLASS),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_STAINED_GLASS),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_STAINED_GLASS),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_STAINED_GLASS),
            Map.entry(DyeColor.RED, Blocks.RED_STAINED_GLASS),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_STAINED_GLASS)
         )
      );
      ASSOCIATED_DYED_BLOCK_MAP.put(
         Blocks.GLASS_PANE,
         process(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_STAINED_GLASS_PANE),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_STAINED_GLASS_PANE),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_STAINED_GLASS_PANE),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_STAINED_GLASS_PANE),
            Map.entry(DyeColor.LIME, Blocks.LIME_STAINED_GLASS_PANE),
            Map.entry(DyeColor.PINK, Blocks.PINK_STAINED_GLASS_PANE),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_STAINED_GLASS_PANE),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_STAINED_GLASS_PANE),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_STAINED_GLASS_PANE),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_STAINED_GLASS_PANE),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_STAINED_GLASS_PANE),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_STAINED_GLASS_PANE),
            Map.entry(DyeColor.RED, Blocks.RED_STAINED_GLASS_PANE),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_STAINED_GLASS_PANE)
         )
      );
      ASSOCIATED_DYED_BLOCK_MAP.put(
         Blocks.SHULKER_BOX,
         process(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_SHULKER_BOX),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_SHULKER_BOX),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_SHULKER_BOX),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_SHULKER_BOX),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_SHULKER_BOX),
            Map.entry(DyeColor.LIME, Blocks.LIME_SHULKER_BOX),
            Map.entry(DyeColor.PINK, Blocks.PINK_SHULKER_BOX),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_SHULKER_BOX),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_SHULKER_BOX),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_SHULKER_BOX),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_SHULKER_BOX),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_SHULKER_BOX),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_SHULKER_BOX),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_SHULKER_BOX),
            Map.entry(DyeColor.RED, Blocks.RED_SHULKER_BOX),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_SHULKER_BOX)
         )
      );
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_BED),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_BED),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_BED),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_BED),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_BED),
         Map.entry(DyeColor.LIME, Blocks.LIME_BED),
         Map.entry(DyeColor.PINK, Blocks.PINK_BED),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_BED),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_BED),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_BED),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_BED),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_BED),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_BED),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_BED),
         Map.entry(DyeColor.RED, Blocks.RED_BED),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_BED)
      );
      ASSOCIATED_DYED_BLOCK_MAP.put(
         Blocks.CANDLE,
         process(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_CANDLE),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_CANDLE),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_CANDLE),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_CANDLE),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_CANDLE),
            Map.entry(DyeColor.LIME, Blocks.LIME_CANDLE),
            Map.entry(DyeColor.PINK, Blocks.PINK_CANDLE),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_CANDLE),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_CANDLE),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_CANDLE),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_CANDLE),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_CANDLE),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_CANDLE),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_CANDLE),
            Map.entry(DyeColor.RED, Blocks.RED_CANDLE),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_CANDLE)
         )
      );
      ASSOCIATED_DYED_BLOCK_MAP.put(
         Blocks.CANDLE_CAKE,
         process(
            Map.entry(DyeColor.WHITE, Blocks.WHITE_CANDLE_CAKE),
            Map.entry(DyeColor.ORANGE, Blocks.ORANGE_CANDLE_CAKE),
            Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_CANDLE_CAKE),
            Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_CANDLE_CAKE),
            Map.entry(DyeColor.YELLOW, Blocks.YELLOW_CANDLE_CAKE),
            Map.entry(DyeColor.LIME, Blocks.LIME_CANDLE_CAKE),
            Map.entry(DyeColor.PINK, Blocks.PINK_CANDLE_CAKE),
            Map.entry(DyeColor.GRAY, Blocks.GRAY_CANDLE_CAKE),
            Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_CANDLE_CAKE),
            Map.entry(DyeColor.CYAN, Blocks.CYAN_CANDLE_CAKE),
            Map.entry(DyeColor.PURPLE, Blocks.PURPLE_CANDLE_CAKE),
            Map.entry(DyeColor.BLUE, Blocks.BLUE_CANDLE_CAKE),
            Map.entry(DyeColor.BROWN, Blocks.BROWN_CANDLE_CAKE),
            Map.entry(DyeColor.GREEN, Blocks.GREEN_CANDLE_CAKE),
            Map.entry(DyeColor.RED, Blocks.RED_CANDLE_CAKE),
            Map.entry(DyeColor.BLACK, Blocks.BLACK_CANDLE_CAKE)
         )
      );
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_BANNER),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_BANNER),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_BANNER),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_BANNER),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_BANNER),
         Map.entry(DyeColor.LIME, Blocks.LIME_BANNER),
         Map.entry(DyeColor.PINK, Blocks.PINK_BANNER),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_BANNER),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_BANNER),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_BANNER),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_BANNER),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_BANNER),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_BANNER),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_BANNER),
         Map.entry(DyeColor.RED, Blocks.RED_BANNER),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_BANNER)
      );
      process(
         Map.entry(DyeColor.WHITE, Blocks.WHITE_WALL_BANNER),
         Map.entry(DyeColor.ORANGE, Blocks.ORANGE_WALL_BANNER),
         Map.entry(DyeColor.MAGENTA, Blocks.MAGENTA_WALL_BANNER),
         Map.entry(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WALL_BANNER),
         Map.entry(DyeColor.YELLOW, Blocks.YELLOW_WALL_BANNER),
         Map.entry(DyeColor.LIME, Blocks.LIME_WALL_BANNER),
         Map.entry(DyeColor.PINK, Blocks.PINK_WALL_BANNER),
         Map.entry(DyeColor.GRAY, Blocks.GRAY_WALL_BANNER),
         Map.entry(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WALL_BANNER),
         Map.entry(DyeColor.CYAN, Blocks.CYAN_WALL_BANNER),
         Map.entry(DyeColor.PURPLE, Blocks.PURPLE_WALL_BANNER),
         Map.entry(DyeColor.BLUE, Blocks.BLUE_WALL_BANNER),
         Map.entry(DyeColor.BROWN, Blocks.BROWN_WALL_BANNER),
         Map.entry(DyeColor.GREEN, Blocks.GREEN_WALL_BANNER),
         Map.entry(DyeColor.RED, Blocks.RED_WALL_BANNER),
         Map.entry(DyeColor.BLACK, Blocks.BLACK_WALL_BANNER)
      );
   }
}
