package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import java.util.LinkedHashMap;
import java.util.Map;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class KeybindConfigurationLegacy extends LegacyAbstractConfigurationCategory {
   public boolean swapLeftRightClickDisplayEntities;
   public boolean invertCameraRotate;
   public boolean useCenterOfScreenForArcball;
   public boolean pickBlockDrag;
   public boolean cutAlsoCopiesToClipboard;
   public boolean builderToolDirectionLock;
   public int toolSmoothSteps;
   public boolean forceEnhancedFlight;
   public boolean useVanillaMovement;
   public Map<String, String> regularKeybinds = new LinkedHashMap<>();
   public Map<String, String> toolKeybinds;

   public KeybindConfigurationLegacy(CommentedConfigurationNode node) {
      super(node);
      this.swapLeftRightClickDisplayEntities = this.load(Boolean.class, "swapLeftRightClickDisplayEntities", false);
      this.invertCameraRotate = this.load(Boolean.class, "invertCameraRotate", false);
      this.useCenterOfScreenForArcball = this.load(Boolean.class, "useCenterOfScreenForArcball", false);
      this.pickBlockDrag = this.load(Boolean.class, "pickBlockDrag", true);
      this.cutAlsoCopiesToClipboard = this.load(Boolean.class, "cutAlsoCopiesToClipboard", false);
      this.builderToolDirectionLock = this.load(Boolean.class, "builderToolDirectionLock", true);
      this.toolSmoothSteps = this.load(Integer.class, "toolSmoothSteps", 0);
      this.forceEnhancedFlight = this.load(Boolean.class, "useEnhancedFlight", true);
      this.useVanillaMovement = this.load(Boolean.class, "useVanillaMovement", true);
      this.toolKeybinds = this.load(Map.class, "toolKeybinds", new LinkedHashMap<>());
      Keybinds.loadLegacy(this);
   }

   @Override
   public void applyToNewConfiguration(AxiomConfig newConfiguration) {
      newConfiguration.entityManipulation.swapLeftRightClick = this.swapLeftRightClickDisplayEntities;
      newConfiguration.internal.invertCameraRotate = this.invertCameraRotate;
      newConfiguration.internal.useCenterOfScreenForArcball = this.useCenterOfScreenForArcball;
      newConfiguration.internal.pickBlockDrag = this.pickBlockDrag;
      newConfiguration.internal.cutAlsoCopiesToClipboard = this.cutAlsoCopiesToClipboard;
      newConfiguration.builderTools.directionLock = this.builderToolDirectionLock;
      newConfiguration.editor.toolStabilization = this.toolSmoothSteps / 10.0F;
      newConfiguration.movement.syncIngameMovementWithEditorUI = !this.forceEnhancedFlight;
      newConfiguration.internal.useVanillaMovementForEditor = this.useVanillaMovement;
      newConfiguration.keybinds.regularKeybinds = this.regularKeybinds;
      newConfiguration.keybinds.toolKeybinds = this.toolKeybinds;
   }
}
