package com.moulberry.axiom.mask.elements;

import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public class BiomeConditionMaskElement implements MaskElement {
   private final ResourceKey<Biome> matchBiome;

   public BiomeConditionMaskElement(ResourceKey<Biome> matchBiome) {
      this.matchBiome = matchBiome;
   }

   @Override
   public boolean test(MaskContext context, int x, int y, int z) {
      Holder<Biome> biome = context.getBiomeAt(x, y, z);
      return biome == null ? false : biome.unwrapKey().get() == this.matchBiome;
   }

   public ResourceKey<Biome> getMatchBiome() {
      return this.matchBiome;
   }
}
