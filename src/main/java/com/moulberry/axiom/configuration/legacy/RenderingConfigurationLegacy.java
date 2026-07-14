package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.configuration.AxiomConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;

class RenderingConfigurationLegacy extends LegacyAbstractConfigurationCategory {
   public float minBrightness = this.load(Number.class, "minBrightness", 0.0F).floatValue();
   public float liquidOpacity = this.load(Number.class, "liquidOpacity", 1.0F).floatValue();
   public boolean keypressOverlay = this.load(Boolean.class, "keypressOverlay", false);
   public boolean showKeyHints = this.load(Boolean.class, "showKeyHints", true);
   public boolean showAnnotations = this.load(Boolean.class, "showAnnotations", true);
   public boolean showRuler = this.load(Boolean.class, "showRuler", true);
   public boolean showDisplayEntities = this.load(Boolean.class, "showDisplayEntities", true);
   public boolean showMarkerEntities = this.load(Boolean.class, "showMarkerEntities", true);
   public boolean showBuilderToolSlot = this.load(Boolean.class, "showBuilderToolSlot", true);
   public boolean disableChunkRenderOverrider = this.load(Boolean.class, "disableChunkRenderOverrider", false);

   public RenderingConfigurationLegacy(CommentedConfigurationNode node) {
      super(node);
   }

   @Override
   public void applyToNewConfiguration(AxiomConfig newConfiguration) {
      newConfiguration.visuals.minBrightness = (int)(this.minBrightness * 100.0F);
      newConfiguration.visuals.liquidOpacity = (int)(this.liquidOpacity * 100.0F);
      newConfiguration.visuals.keypressOverlay = this.keypressOverlay;
      newConfiguration.visuals.showKeyHints = this.showKeyHints;
      newConfiguration.visuals.showAnnotations = this.showAnnotations;
      newConfiguration.visuals.showRuler = this.showRuler;
      newConfiguration.entityManipulation.showDisplayEntities = this.showDisplayEntities;
      newConfiguration.entityManipulation.showMarkerEntities = this.showMarkerEntities;
      newConfiguration.builderTools.showBuilderToolSlot = this.showBuilderToolSlot;
      newConfiguration.internal.disableChunkRenderOverrider = this.disableChunkRenderOverrider;
   }
}
