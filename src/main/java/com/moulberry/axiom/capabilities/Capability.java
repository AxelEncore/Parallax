package com.moulberry.axiom.capabilities;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.restrictions.AxiomPermission;
import java.util.EnumSet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum Capability {
   BULLDOZER(
      Component.translatable("axiom.capability.bulldozer.title"),
      Component.translatable("axiom.capability.bulldozer.description", new Object[]{Component.keybind("key.attack")}).withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.BULLDOZER),
      null
   ),
   REPLACE_MODE(
      Component.translatable("axiom.capability.replace_mode.title"),
      Component.translatable("axiom.capability.replace_mode.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.REPLACE_MODE, AxiomPermission.BUILD_PLACE),
      null
   ),
   FORCE_PLACE(
      Component.translatable("axiom.capability.force_place.title"),
      Component.translatable("axiom.capability.force_place.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.FORCE_PLACE, AxiomPermission.BUILD_PLACE),
      null
   ),
   NO_UPDATES(
      Component.translatable("axiom.capability.no_updates.title"),
      Component.translatable("axiom.capability.no_updates.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.NO_UPDATES, AxiomPermission.BUILD_PLACE),
      null
   ),
   TINKER(
      Component.translatable("axiom.capability.tinker.title"),
      Component.translatable("axiom.capability.tinker.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.TINKER, AxiomPermission.BUILD_PLACE),
      null
   ),
   INFINITE_REACH(
      Component.translatable("axiom.capability.infinite_reach.title"),
      Component.translatable("axiom.capability.infinite_reach.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.INFINITE_REACH, AxiomPermission.BUILD_PLACE),
      null
   ),
   FAST_PLACE(
      Component.translatable("axiom.capability.fast_place.title"),
      Component.translatable("axiom.capability.fast_place.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.FAST_PLACE),
      null
   ),
   ANGEL_PLACEMENT(
      Component.translatable("axiom.capability.angel_placement.title"),
      Component.translatable("axiom.capability.angel_placement.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.ANGEL_PLACEMENT, AxiomPermission.BUILD_PLACE),
      null
   ),
   NO_CLIP(
      Component.translatable("axiom.capability.no_clip.title"),
      Component.translatable("axiom.capability.no_clip.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.NO_CLIP, AxiomPermission.PLAYER_BYPASS_MOVEMENT_RESTRICTIONS),
      null
   ),
   PHANTOM(
      Component.translatable("axiom.capability.phantom.title"),
      Component.translatable("axiom.capability.phantom.description").withStyle(ChatFormatting.GRAY),
      EnumSet.of(AxiomPermission.PHANTOM, AxiomPermission.PLAYER_SETNOPHYSICALTRIGGER),
      SupportedProtocol.SET_NO_PHYSICAL_TRIGGER
   );

   public final Component title;
   public final Component description;
   public final EnumSet<AxiomPermission> permissions;
   public final SupportedProtocol requiredProtocol;

   private Capability(Component title, Component description, EnumSet<AxiomPermission> permissions, SupportedProtocol requiredProtocol) {
      this.title = title;
      this.description = description;
      this.permissions = permissions;
      this.requiredProtocol = requiredProtocol;
   }

   public boolean allowedByServer() {
      return AxiomClient.hasPermissions(this.permissions) && (this.requiredProtocol == null || ClientEvents.serverSupportsProtocol(this.requiredProtocol));
   }

   public boolean isEnabled() {
      if (AxiomClient.isAxiomActive() && this.allowedByServer()) {
         return switch (this) {
            case BULLDOZER -> Axiom.configuration.capabilities.bulldozer;
            case REPLACE_MODE -> Axiom.configuration.capabilities.replaceMode;
            case FORCE_PLACE -> Axiom.configuration.capabilities.forcePlace;
            case NO_UPDATES -> Axiom.configuration.capabilities.noUpdates;
            case TINKER -> Axiom.configuration.capabilities.tinker;
            case INFINITE_REACH -> Axiom.configuration.capabilities.infiniteReach;
            case FAST_PLACE -> Axiom.configuration.capabilities.fastPlace;
            case ANGEL_PLACEMENT -> Axiom.configuration.capabilities.angelPlacement;
            case NO_CLIP -> Axiom.configuration.capabilities.noClip;
            case PHANTOM -> Axiom.configuration.capabilities.phantom;
         };
      } else {
         return false;
      }
   }

   public void setEnabled(boolean enabled) {
      if (!enabled || this.allowedByServer()) {
         AxiomConfig.SubcategoryCapabilities capabilities = Axiom.configuration.capabilities;
         switch (this) {
            case BULLDOZER:
               capabilities.bulldozer = enabled;
               break;
            case REPLACE_MODE:
               capabilities.replaceMode = enabled;
               break;
            case FORCE_PLACE:
               capabilities.forcePlace = enabled;
               break;
            case NO_UPDATES:
               capabilities.noUpdates = enabled;
               break;
            case TINKER:
               capabilities.tinker = enabled;
               break;
            case INFINITE_REACH:
               capabilities.infiniteReach = enabled;
               break;
            case FAST_PLACE:
               capabilities.fastPlace = enabled;
               break;
            case ANGEL_PLACEMENT:
               capabilities.angelPlacement = enabled;
               break;
            case NO_CLIP:
               capabilities.noClip = enabled;
               break;
            case PHANTOM:
               capabilities.phantom = enabled;
         }
      }
   }

   public void toggle() {
      this.setEnabled(!this.isEnabled());
   }
}
