package com.moulberry.axiom.i18n;

import com.moulberry.axiom.utils.ResourcePackUtils;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;

public class LocalizationSource implements RepositorySource {
   public static LocalizationSource INSTANCE = new LocalizationSource();
   private boolean attemptedLoadImmediate = false;
   private PackResources packResources = null;

   public void setResources(PackResources packResources) {
      this.packResources = packResources;
   }

   public void loadPacks(Consumer<Pack> consumer) {
      if (this.packResources != null) {
         Pack pack = ResourcePackUtils.createBuiltIn("axiom_translations", Component.literal("Parallax Translations"), this.packResources);
         consumer.accept(pack);
      } else if (!this.attemptedLoadImmediate) {
         this.attemptedLoadImmediate = true;
         LocalizationLoader.applyCachedTranslations();
         if (this.packResources != null) {
            Pack pack = ResourcePackUtils.createBuiltIn("axiom_translations", Component.literal("Parallax Translations"), this.packResources);
            consumer.accept(pack);
         }
      }
   }
}
