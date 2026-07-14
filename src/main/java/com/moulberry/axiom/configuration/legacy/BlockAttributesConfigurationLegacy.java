package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.configuration.AxiomConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;

class BlockAttributesConfigurationLegacy extends LegacyAbstractConfigurationCategory {
   public boolean showCollisionMesh = this.load(Boolean.class, "showCollisionMesh", false);
   public boolean showLightBlocks = this.load(Boolean.class, "showLightBlocks", false);
   public boolean showStructureVoidBlocks = this.load(Boolean.class, "showStructureVoidBlocks", false);
   public boolean expandHitboxesToFullCube = this.load(Boolean.class, "expandHitboxesToFullCube", false);
   public boolean makeFluidHitboxesSolid = this.load(Boolean.class, "makeFluidHitboxesSolid", false);
   public boolean preventInteractions = this.load(Boolean.class, "preventInteractions", false);

   public BlockAttributesConfigurationLegacy(CommentedConfigurationNode node) {
      super(node);
   }

   @Override
   public void applyToNewConfiguration(AxiomConfig newConfiguration) {
      newConfiguration.blockAttributes.showCollisionMesh = this.showCollisionMesh;
      newConfiguration.blockAttributes.showLightBlocks = this.showLightBlocks;
      newConfiguration.blockAttributes.showStructureVoidBlocks = this.showStructureVoidBlocks;
      newConfiguration.blockAttributes.expandHitboxesToFullCube = this.expandHitboxesToFullCube;
      newConfiguration.blockAttributes.makeFluidHitboxesSolid = this.makeFluidHitboxesSolid;
      newConfiguration.blockAttributes.preventInteractions = this.preventInteractions;
   }
}
