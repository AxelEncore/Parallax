package com.moulberry.axiom.i18n;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

public class LoadedPackResources implements PackResources {
   private final String id;
   private final Component title;
   private final Map<String, byte[]> data;

   public LoadedPackResources(String id, Component title, Map<String, byte[]> data) {
      this.id = id;
      this.title = title;
      this.data = data;
   }

   public PackLocationInfo location() {
      return new PackLocationInfo(this.id, this.title, PackSource.BUILT_IN, Optional.empty());
   }

   @Nullable
   public IoSupplier<InputStream> getRootResource(String... strings) {
      return null;
   }

   @Nullable
   public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation resourceLocation) {
      if (!resourceLocation.getNamespace().equals("axiom")) {
         return null;
      } else {
         byte[] bytes = this.data.get(resourceLocation.getPath());
         return bytes != null ? () -> new ByteArrayInputStream(bytes) : null;
      }
   }

   public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
      if (namespace.equals("axiom")) {
         for (Entry<String, byte[]> entry : this.data.entrySet()) {
            if (entry.getKey().startsWith(path)) {
               resourceOutput.accept(
                  ResourceLocation.fromNamespaceAndPath("axiom", entry.getKey()), (IoSupplier)() -> new ByteArrayInputStream(entry.getValue())
               );
            }
         }
      }
   }

   public Set<String> getNamespaces(PackType packType) {
      return Set.of("axiom");
   }

   @Nullable
   public <T> T getMetadataSection(MetadataSectionSerializer<T> metadataSectionSerializer) throws IOException {
      return null;
   }

   public String packId() {
      return this.id;
   }

   public void close() {
   }
}
