package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.configuration.AxiomConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader.Builder;

public class LegacyConfiguration {
   public static void tryLoadAndApplyLegacy(Path mainPath, Path internalPath, AxiomConfig newConfiguration) {
      boolean appliedMainConfig = false;

      try {
         if (Files.exists(mainPath) && Files.size(mainPath) > 1L) {
            HoconConfigurationLoader mainLoader = ((Builder)HoconConfigurationLoader.builder().path(mainPath)).build();
            CommentedConfigurationNode mainRoot = (CommentedConfigurationNode)mainLoader.load();
            CapabilitiesConfigurationLegacy capabilities = new CapabilitiesConfigurationLegacy(
               (CommentedConfigurationNode)mainRoot.node(new Object[]{"capabilities"})
            );
            BlockAttributesConfigurationLegacy blockAttributes = new BlockAttributesConfigurationLegacy(
               (CommentedConfigurationNode)mainRoot.node(new Object[]{"blockAttributes"})
            );
            RenderingConfigurationLegacy rendering = new RenderingConfigurationLegacy((CommentedConfigurationNode)mainRoot.node(new Object[]{"rendering"}));
            KeybindConfigurationLegacy keybind = new KeybindConfigurationLegacy((CommentedConfigurationNode)mainRoot.node(new Object[]{"keybinds"}));
            BlueprintConfigurationLegacy blueprint = new BlueprintConfigurationLegacy((CommentedConfigurationNode)mainRoot.node(new Object[]{"blueprint"}));
            capabilities.applyToNewConfiguration(newConfiguration);
            blockAttributes.applyToNewConfiguration(newConfiguration);
            rendering.applyToNewConfiguration(newConfiguration);
            keybind.applyToNewConfiguration(newConfiguration);
            blueprint.applyToNewConfiguration(newConfiguration);
            appliedMainConfig = true;
         }
      } catch (Exception var14) {
         Axiom.LOGGER.error("Failed to load and apply legacy main config", var14);
      }

      boolean appliedInternalConfig = false;

      try {
         if (Files.exists(internalPath) && Files.size(internalPath) > 1L) {
            HoconConfigurationLoader internalLoader = ((Builder)HoconConfigurationLoader.builder().path(internalPath)).build();
            CommentedConfigurationNode internalRoot = (CommentedConfigurationNode)internalLoader.load();
            InternalConfigurationLegacy internal = new InternalConfigurationLegacy(internalRoot);
            internal.applyToNewConfiguration(newConfiguration);
            appliedInternalConfig = true;
         }
      } catch (Exception var13) {
         Axiom.LOGGER.error("Failed to load and apply legacy internal config", var13);
      }

      if (appliedMainConfig || appliedInternalConfig) {
         newConfiguration.saveToDefaultFolder();
      }

      if (appliedMainConfig) {
         try {
            Files.deleteIfExists(mainPath);
         } catch (IOException var12) {
            Axiom.LOGGER.error("Failed to delete legacy main config", var12);
         }
      }

      if (appliedInternalConfig) {
         try {
            Files.deleteIfExists(internalPath);
         } catch (IOException var11) {
            Axiom.LOGGER.error("Failed to delete legacy internal config", var11);
         }
      }
   }

   private static void init() {
   }
}
