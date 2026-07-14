package com.moulberry.axiom;

import com.moulberry.axiom.utils.ARGB32;
import com.moulberry.axiom.utils.StringUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public class BiomeDataManager {
   private static final BiomeDataManager INSTANCE = new BiomeDataManager();
   private WeakReference<Registry<Biome>> biomeRegistryReference = null;
   private List<BiomeDataManager.BiomeDataEntry> biomes = new ArrayList<>();
   private Map<ResourceKey<Biome>, BiomeDataManager.BiomeDataEntry> biomeMap = new HashMap<>();
   private boolean justRefreshed = false;

   public static BiomeDataManager get() {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      Optional<Registry<Biome>> registry = connection == null ? Optional.empty() : connection.registryAccess().registry(Registries.BIOME);
      if (registry.isEmpty()) {
         return null;
      } else {
         INSTANCE.justRefreshed = false;
         Registry<Biome> currentRegistry = registry.get();
         if (INSTANCE.biomeRegistryReference == null || !INSTANCE.biomeRegistryReference.refersTo(currentRegistry)) {
            INSTANCE.refresh(currentRegistry);
         }

         return INSTANCE;
      }
   }

   private static int getFogColor(Biome biome) {
      return biome.getFogColor();
   }

   private void refresh(Registry<Biome> registry) {
      this.biomes.clear();
      this.biomeMap.clear();
      this.biomeRegistryReference = new WeakReference<>(registry);
      int plainsFog = getFogColor((Biome)((Reference)registry.getHolder(Biomes.PLAINS).orElseThrow()).value());
      int plainsFogRed = ARGB32.red(plainsFog);
      int plainsFogGreen = ARGB32.green(plainsFog);
      int plainsFogBlue = ARGB32.blue(plainsFog);

      for (Entry<ResourceKey<Biome>, Biome> entry : registry.entrySet()) {
         ResourceKey<Biome> biomeKey = entry.getKey();
         String biomeKeyPath = biomeKey.location().getPath();
         ResourceLocation location = biomeKey.location();
         String biomeTranslationKey = "biome." + location.getNamespace() + "." + location.getPath();
         String biomeString = Language.getInstance().getOrDefault(biomeTranslationKey, StringUtils.convertResourceToPretty(location));
         Biome biome = entry.getValue();
         int biomeFog = getFogColor(biome);
         int biomeFogRed = ARGB32.red(biomeFog);
         int biomeFogGreen = ARGB32.green(biomeFog);
         int biomeFogBlue = ARGB32.blue(biomeFog);
         int redDistance = plainsFogRed - biomeFogRed;
         int greenDistance = plainsFogGreen - biomeFogGreen;
         int blueDistance = plainsFogBlue - biomeFogBlue;
         double biomeFogDistance = Math.sqrt(redDistance * redDistance + greenDistance * greenDistance + blueDistance * blueDistance);
         int biomeColour;
         if (biomeFogDistance > 200.0) {
            biomeColour = biomeFog;
         } else if (!biomeKeyPath.contains("ocean") && !biomeKeyPath.contains("river")) {
            biomeColour = biome.getFoliageColor();
         } else {
            biomeColour = biome.getWaterColor();
         }

         if (biomeKey == Biomes.THE_VOID) {
            biomeColour = 0;
         } else if (biomeKey == Biomes.BEACH) {
            biomeColour = 16177072;
         } else if (biomeKey == Biomes.DRIPSTONE_CAVES) {
            biomeColour = 8743515;
         } else if (biomeKey == Biomes.LUSH_CAVES) {
            biomeColour = 3800852;
         } else if (biomeKey == Biomes.DEEP_DARK) {
            biomeColour = 1052688;
         } else if (biomeKey == Biomes.MUSHROOM_FIELDS) {
            biomeColour = 8676244;
         } else if (biomeKey == Biomes.CHERRY_GROVE) {
            biomeColour = 16764128;
         } else if (biomeKey == Biomes.DESERT) {
            biomeColour = 16772039;
         } else if (biomeKey == Biomes.FLOWER_FOREST) {
            biomeColour = 12641781;
         } else if (biomeKey == Biomes.BAMBOO_JUNGLE) {
            biomeColour = 8388352;
         } else if (biomeKey == Biomes.SWAMP) {
            biomeColour = 5664066;
         } else if (biomeKey == Biomes.MANGROVE_SWAMP) {
            biomeColour = 5217576;
         } else if (biomeKey == Biomes.BADLANDS) {
            biomeColour = 16753920;
         } else if (biomeKey == Biomes.NETHER_WASTES) {
            biomeColour = 6949386;
         } else if (biomeKey == Biomes.SOUL_SAND_VALLEY) {
            biomeColour = 14666;
         } else if (biomeKey == Biomes.CRIMSON_FOREST) {
            biomeColour = 9109504;
         } else if (biomeKey == Biomes.WARPED_FOREST) {
            biomeColour = 4251856;
         } else if (biomeKey == Biomes.BASALT_DELTAS) {
            biomeColour = 6908265;
         } else if (biomeKey == Biomes.THE_END
            || biomeKey == Biomes.END_BARRENS
            || biomeKey == Biomes.END_HIGHLANDS
            || biomeKey == Biomes.END_MIDLANDS
            || biomeKey == Biomes.SMALL_END_ISLANDS) {
            biomeColour = 13148872;
         } else if (biomeKey == Biomes.SNOWY_SLOPES) {
            biomeColour = 13428479;
         } else if (biomeKey == Biomes.GROVE) {
            biomeColour = 11788543;
         } else if (biomeKey == Biomes.FROZEN_PEAKS) {
            biomeColour = 10079487;
         } else if (biomeKey == Biomes.JAGGED_PEAKS) {
            biomeColour = 8437759;
         } else if (biomeKey == Biomes.SNOWY_TAIGA) {
            biomeColour = 6730495;
         } else if (biomeKey == Biomes.SNOWY_BEACH) {
            biomeColour = 5089023;
         } else if (biomeKey == Biomes.ICE_SPIKES) {
            biomeColour = 1674751;
         }

         BiomeDataManager.BiomeDataEntry biomeDataEntry = new BiomeDataManager.BiomeDataEntry(biomeKey, biomeString, biomeColour);
         this.biomes.add(biomeDataEntry);
         this.biomeMap.put(biomeKey, biomeDataEntry);
      }

      this.biomes.sort(Comparator.comparing(k -> {
         ResourceKey<Biome> key = k.key();
         return key.location().toString();
      }));
      this.justRefreshed = true;
   }

   public BiomeDataManager.BiomeDataEntry getData(ResourceKey<Biome> key) {
      return this.biomeMap.get(key);
   }

   public List<BiomeDataManager.BiomeDataEntry> biomes() {
      return this.biomes;
   }

   public boolean justRefreshed() {
      return this.justRefreshed;
   }

   public record BiomeDataEntry(ResourceKey<Biome> key, String prettyName, int colour) {
   }
}
