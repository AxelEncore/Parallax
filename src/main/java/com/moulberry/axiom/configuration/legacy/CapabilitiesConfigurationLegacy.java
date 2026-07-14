package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.configuration.AltMenuKeybindMode;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.configuration.FlightDirection;
import net.minecraft.util.Mth;
import org.spongepowered.configurate.CommentedConfigurationNode;

class CapabilitiesConfigurationLegacy extends LegacyAbstractConfigurationCategory {
   public boolean bulldozer;
   public boolean replaceMode;
   public boolean forcePlace;
   public boolean noUpdates;
   public boolean tinker;
   public boolean infiniteReach;
   public boolean fastPlace;
   public boolean angelPlacement;
   public boolean noClip;
   public boolean phantom;
   public boolean typeReplace;
   public int infiniteReachLimit;
   public float flightMomentum;
   public boolean flightCameraDirection;
   public boolean autoSwapToCreative;
   public boolean tallGrassIsActuallyNotTall;
   public boolean infiniteRangeMarkers;
   public boolean autoSwapToOtherHotbarWithItem;
   public boolean separateEditorAndGameFlightSpeed;
   public boolean allowClickingHistoryInEditor;
   public boolean altMenuToggleInsteadOfHold;

   public CapabilitiesConfigurationLegacy(CommentedConfigurationNode node) {
      super(node);
      node.commentIfAbsent("Capabilities modify the way the player moves and interacts with the world");
      this.bulldozer = this.load(Boolean.class, "bulldozer", true);
      this.replaceMode = this.load(Boolean.class, "replaceMode", false);
      this.forcePlace = this.load(Boolean.class, "forcePlace", false);
      this.noUpdates = this.load(Boolean.class, "noUpdates", false);
      this.tinker = this.load(Boolean.class, "tinker", true);
      this.infiniteReach = this.load(Boolean.class, "infiniteReach", false);
      this.fastPlace = this.load(Boolean.class, "fastPlace", false);
      this.angelPlacement = this.load(Boolean.class, "angelPlacement", false);
      this.noClip = this.load(Boolean.class, "noClip", false);
      this.phantom = this.load(Boolean.class, "phantom", false);
      this.typeReplace = this.load(Boolean.class, "typeReplace", false);
      this.infiniteReachLimit = this.load(Number.class, "infiniteReachLimit", -1).intValue();
      this.flightMomentum = Mth.clamp(this.load(Number.class, "flightMomentum", 1.0F).floatValue(), 0.0F, 1.0F);
      this.flightCameraDirection = this.load(Boolean.class, "flightCameraDirection", false);
      this.autoSwapToCreative = this.load(Boolean.class, "autoSwapToCreative", true);
      this.tallGrassIsActuallyNotTall = Boolean.TRUE.equals(this.load(Boolean.class, "tallGrassIsActuallyNotTall", null));
      this.infiniteRangeMarkers = Boolean.TRUE.equals(this.load(Boolean.class, "infiniteRangeMarkers", null));
      this.autoSwapToOtherHotbarWithItem = this.load(Boolean.class, "autoSwapToOtherHotbarWithItem", false);
      this.separateEditorAndGameFlightSpeed = this.load(Boolean.class, "separateEditorAndGameFlightSpeed", false);
      this.allowClickingHistoryInEditor = this.load(Boolean.class, "allowClickingHistoryInEditor", false);
      this.altMenuToggleInsteadOfHold = this.load(Boolean.class, "altMenuToggleInsteadOfHold", false);
   }

   @Override
   public void applyToNewConfiguration(AxiomConfig newConfiguration) {
      newConfiguration.capabilities.bulldozer = this.bulldozer;
      newConfiguration.capabilities.replaceMode = this.replaceMode;
      newConfiguration.capabilities.forcePlace = this.forcePlace;
      newConfiguration.capabilities.noUpdates = this.noUpdates;
      newConfiguration.capabilities.tinker = this.tinker;
      newConfiguration.capabilities.infiniteReach = this.infiniteReach;
      newConfiguration.capabilities.fastPlace = this.fastPlace;
      newConfiguration.capabilities.angelPlacement = this.angelPlacement;
      newConfiguration.capabilities.noClip = this.noClip;
      newConfiguration.capabilities.phantom = this.phantom;
      newConfiguration.capabilities.typeReplace = this.typeReplace;
      newConfiguration.capabilities.infiniteReachLimit = this.infiniteReachLimit;
      newConfiguration.movement.flightMomentum = Math.max(0, Math.min(100, (int)this.flightMomentum * 100));
      newConfiguration.movement.flightDirection = this.flightCameraDirection ? FlightDirection.CAMERA : FlightDirection.HORIZONTAL;
      newConfiguration.contextMenu.autoSwapToCreative = this.autoSwapToCreative;
      newConfiguration.internal.tallGrassIsActuallyNotTall = this.tallGrassIsActuallyNotTall;
      newConfiguration.contextMenu.autoSwapToOtherHotbarWithItem = this.autoSwapToOtherHotbarWithItem;
      newConfiguration.movement.separateFlightSpeeds = this.separateEditorAndGameFlightSpeed;
      newConfiguration.contextMenu.keybindMode = this.altMenuToggleInsteadOfHold ? AltMenuKeybindMode.TOGGLE : AltMenuKeybindMode.HOLD;
   }
}
