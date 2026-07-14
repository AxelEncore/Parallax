package com.moulberry.axiom;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.StringUtils;
import com.moulberry.axiom.world_modification.Dispatcher;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
public class AxiomClient {
   public static Set<UUID> ignoredDisplayEntities = Set.of();

   public static boolean isAxiomActive(GameType gameType) {
      if (!StaticValues.gameHasTicked) {
         return false;
      } else if (Axiom.getInstance() == null || Axiom.getInstance().serverConfig == null) {
         return false;
      } else if (ClientEvents.allowedOnServer
         && ClientEvents.serverSupportsAxiom
         && ClientEvents.processedServerSupportsAxiom
         && ClientEvents.processedAllowedOnServer
         && !ClientEvents.remotelyDisabled) {
         if (com.moulberry.axiom.integration.ReplayModIntegration.isPlayingReplay()) {
            return false;
         } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
               return false;
            } else {
               MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
               return gameMode == null ? false : gameMode.getPlayerMode() == gameType;
            }
         }
      } else {
         return false;
      }
   }

   public static void updateItemBlockRenderTypes() {
      AxiomConfig.SubcategoryBlockAttributes attributes = Axiom.configuration.blockAttributes;
      if (attributes.showStructureVoidBlocks) {
         ItemBlockRenderTypes.TYPE_BY_BLOCK.put(Blocks.STRUCTURE_VOID, RenderType.cutout());
      }

      if (attributes.showLightBlocks) {
         ItemBlockRenderTypes.TYPE_BY_BLOCK.put(Blocks.LIGHT, RenderType.cutout());
      }

      if (attributes.showMovingPistonBlocks) {
         ItemBlockRenderTypes.TYPE_BY_BLOCK.put(Blocks.MOVING_PISTON, RenderType.translucent());
      }
   }

   public static boolean hasPermission(AxiomPermission axiomPermission) {
      return ClientRestrictions.permissions.contains(axiomPermission);
   }

   public static boolean hasPermissions(EnumSet<AxiomPermission> permissions) {
      return ClientRestrictions.permissions.containsAll(permissions);
   }

   public static boolean isAxiomActive() {
      return isAxiomActive(EditorUI.isEnabled() ? GameType.SPECTATOR : GameType.CREATIVE);
   }

   public static void loadCommercialLicenseHistory(ResourceKey<Level> resourceKey) {
      String newHistoryIdentifier;
      if (Minecraft.getInstance().hasSingleplayerServer()) {
         String resource = StringUtils.convertResourceToPretty(resourceKey.location());
         newHistoryIdentifier = "Singleplayer/" + Minecraft.getInstance().getSingleplayerServer().storageSource.getLevelId() + "/" + resource;
      } else {
         SocketAddress address = Minecraft.getInstance().getConnection().getConnection().getRemoteAddress();
         String addressIdentifier;
         if (address instanceof InetSocketAddress inetSocketAddress) {
            addressIdentifier = inetSocketAddress.getAddress().getHostAddress();
         } else {
            addressIdentifier = address.toString();
         }

         String resource = StringUtils.convertResourceToPretty(resourceKey.location());
         newHistoryIdentifier = "Multiplayer/" + addressIdentifier + "/" + resource;
      }

      Dispatcher.tryLoadHistory(newHistoryIdentifier);
   }

   public static void onAxiomEnabled(Minecraft client) {
      if (Axiom.configuration.internal.shownIntroduction) {
         if (Axiom.configuration.internal.hadEditorUIOpen && client.gameMode != null && client.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            EditorUI.enable();
         }

         Component conflicts = getEditorUiKeybindConflicts(client);
         if (conflicts != null) {
            ChatUtils.error(Component.literal(AxiomI18n.get("axiom.hardcoded.editor_keybind_conflicts")).append(conflicts));
         }
         // Startup "Tip:" chat messages intentionally removed — no tips are shown on join.
      }
   }

   @Nullable
   private static Component getEditorUiKeybindConflicts(Minecraft client) {
      KeyMapping toggleEditor = ClientEvents.toggleEditorUiKeyBind;
      if (!toggleEditor.isUnbound()) {
         boolean hasCollision = false;
         MutableComponent mutableComponent = Component.empty();

         for (KeyMapping other : client.options.keyMappings) {
            if (other != toggleEditor && toggleEditor.same(other)) {
               if (hasCollision) {
                  mutableComponent.append(", ");
               }

               hasCollision = true;
               mutableComponent.append(Component.translatable(other.getName()));
            }
         }

         return hasCollision ? mutableComponent : null;
      } else {
         return null;
      }
   }
}
