package com.moulberry.axiom.utils;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class WeirdTags {
   private static final Set<ResourceLocation> WEIRD_TAGS = Set.of(
      ResourceLocation.fromNamespaceAndPath("minecraft", "lava_pool_stone_cannot_replace"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "sword_efficient"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "wall_post_override"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "sculk_replaceable_world_gen"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "completes_find_tree_tutorial"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "parrots_spawnable_on"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "overworld_carver_replaceables"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "sculk_replaceable"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "needs_stone_tool"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "enderman_holdable"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "azalea_root_replaceable"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "dampens_vibrations"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "azalea_grows_on"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "dead_bush_may_place_on"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "replaceable_by_trees"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "guarded_by_piglins"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "enchantment_power_transmitter"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "nether_carver_replaceables"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "combination_step_sound_blocks"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "moss_replaceable"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "bamboo_plantable_on"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "occludes_vibration_signals"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "ancient_city_replaceable"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "big_dripleaf_placeable"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "fall_damage_resetting"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "mangrove_logs_can_grow_through"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "sniffer_diggable_block"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "mangrove_roots_can_grow_through"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "dripstone_replaceable_blocks"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "geode_invalid_blocks"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "goats_spawnable_on"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "inside_step_sound_blocks"),
      ResourceLocation.fromNamespaceAndPath("minecraft", "lush_ground_replaceable")
   );

   public static boolean isWeird(TagKey<Block> key) {
      return WEIRD_TAGS.contains(key.location());
   }
}
