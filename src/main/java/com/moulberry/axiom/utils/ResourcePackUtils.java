package com.moulberry.axiom.utils;

import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Metadata;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import net.minecraft.world.flag.FeatureFlagSet;

public class ResourcePackUtils {
   public static Pack createBuiltIn(String id, Component title, PackResources resources) {
      PackLocationInfo packLocationInfo = new PackLocationInfo(id, title, PackSource.BUILT_IN, Optional.empty());
      Metadata metadata = new Metadata(title, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
      PackSelectionConfig packSelectionConfig = new PackSelectionConfig(true, Position.BOTTOM, true);
      return new Pack(packLocationInfo, new ResourcesSupplier() {
         public PackResources openPrimary(PackLocationInfo packLocationInfo) {
            return resources;
         }

         public PackResources openFull(PackLocationInfo packLocationInfo, Metadata metadatax) {
            return resources;
         }
      }, metadata, packSelectionConfig);
   }
}
