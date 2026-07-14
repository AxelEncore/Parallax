package com.moulberry.axiom.restrictions;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.stamp.StampTool;
import com.moulberry.axiom.utils.Box;
import com.moulberry.axiom.utils.ChatUtils;
import java.util.EnumSet;

public class ClientRestrictions {
   public static Restrictions restrictions = new Restrictions();
   public static AxiomPermissionSet permissions = AxiomPermissionSet.DEFAULT;
   private static boolean warnedAboutNonDefaultPermissions = false;
   private static boolean warnedAboutNoChunkRequestPermissions = false;
   private static boolean warnedAboutNoMovementBypassPermissions = false;
   private static final EnumSet<AxiomPermission> IGNORE_FOR_WARNING = EnumSet.of(
      AxiomPermission.ALLOW_COPYING_OTHER_PLOTS,
      AxiomPermission.CAN_IMPORT_BLOCKS,
      AxiomPermission.CAN_EXPORT_BLOCKS,
      AxiomPermission.PLAYER_GAMEMODE_SURVIVAL,
      AxiomPermission.PLAYER_GAMEMODE_ADVENTURE
   );

   public static void update(Restrictions newRestrictions) {
      AxiomPermissionSet newPermissions = new AxiomPermissionSet(newRestrictions.allowedPermissions, newRestrictions.deniedPermissions);
      boolean gaveWarning = false;
      if (!warnedAboutNonDefaultPermissions && !newPermissions.containsAllButIgnore(AxiomPermissionSet.DEFAULT, IGNORE_FOR_WARNING)) {
         ChatUtils.warning("Server is using non-default permissions. Some features in Axiom may be unavailable or broken!");
         warnedAboutNonDefaultPermissions = true;
         gaveWarning = true;
      }

      if (!warnedAboutNoChunkRequestPermissions) {
         if (!newPermissions.contains(AxiomPermission.CHUNK_REQUEST)) {
            ChatUtils.warning(
               "Server does not allow requesting chunk data. Editing outside render distance will not work and some block entities might not be copyable!"
            );
            warnedAboutNoChunkRequestPermissions = true;
            gaveWarning = true;
         } else if (!newPermissions.contains(AxiomPermission.CHUNK_REQUESTBLOCKENTITY)) {
            ChatUtils.warning("Server does not allow requesting block entity data. Some block entities might not be copyable!");
            warnedAboutNoChunkRequestPermissions = true;
            gaveWarning = true;
         }
      }

      if (!warnedAboutNoMovementBypassPermissions && !newPermissions.contains(AxiomPermission.PLAYER_BYPASS_MOVEMENT_RESTRICTIONS)) {
         ChatUtils.warning("Server does not allow for noclip or fast movement. You might get lagged back!");
         warnedAboutNoMovementBypassPermissions = true;
         gaveWarning = true;
      }

      if (gaveWarning) {
         ChatUtils.warning("If you encounter any issues related to the above, please contact your server administrator");
      }

      if (permissions.contains(AxiomPermission.CAN_IMPORT_BLOCKS) && !newPermissions.contains(AxiomPermission.CAN_IMPORT_BLOCKS)) {
         clearCrossWorldBlockInformation();
      }

      if (EditorUI.isActive() && !newPermissions.contains(AxiomPermission.EDITOR_USE)) {
         EditorUI.disable();
      }

      if (ToolManager.isToolActive() && !newPermissions.containsAll(ToolManager.getCurrentTool().requiredPermissions())) {
         ToolManager.setToolSelected(false);
      }

      BuilderToolManager.ensureSelectedToolIsUsable();
      if (DisplayEntityManipulator.hasActiveGizmo() && !newPermissions.contains(AxiomPermission.ENTITY_MANIPULATE)) {
         DisplayEntityManipulator.disableActive();
      }

      permissions = newPermissions;
      restrictions = newRestrictions;
   }

   public static SelectionBuffer constrainSelection(SelectionBuffer selectionBuffer) {
      if (!restrictions.bounds.isEmpty()) {
         Object var2;
         if (restrictions.bounds.size() == 1) {
            Box bounds = restrictions.bounds.get(0);
            var2 = selectionBuffer.intersectAABB(bounds.pos1(), bounds.pos2(), false);
         } else {
            ChunkedBooleanRegion chunkedBooleanRegion = new ChunkedBooleanRegion();
            selectionBuffer.forEach(
               (x, y, z) -> {
                  for (Box bound : restrictions.bounds) {
                     if (bound.pos1().getX() <= x
                        && bound.pos2().getX() >= x
                        && bound.pos1().getY() <= y
                        && bound.pos2().getY() >= y
                        && bound.pos1().getZ() <= z
                        && bound.pos2().getZ() >= z) {
                        chunkedBooleanRegion.add(x, y, z);
                        break;
                     }
                  }
               }
            );
            selectionBuffer.close();
            var2 = new SelectionBuffer.Set(chunkedBooleanRegion);
         }

         return ((SelectionBuffer)var2).optimize();
      } else {
         return selectionBuffer;
      }
   }

   public static void clearCrossWorldBlockInformation() {
      Clipboard.INSTANCE.clearClipboard();
      Placement.INSTANCE.stopPlacement();

      for (Tool tool : ToolManager.getTools()) {
         if (tool instanceof StampTool stampTool) {
            stampTool.clearBlueprints();
         }
      }
   }

   public static void reset() {
      if (!permissions.contains(AxiomPermission.CAN_EXPORT_BLOCKS)) {
         clearCrossWorldBlockInformation();
      }

      restrictions = new Restrictions();
      permissions = AxiomPermissionSet.DEFAULT;
      warnedAboutNonDefaultPermissions = false;
      warnedAboutNoChunkRequestPermissions = false;
      warnedAboutNoMovementBypassPermissions = false;
   }

   public static int getInfiniteReachLimit() {
      int infiniteReachLimit = Axiom.configuration.capabilities.infiniteReachLimit;
      if (infiniteReachLimit < 5 || infiniteReachLimit > 100) {
         infiniteReachLimit = -1;
      }

      return restrictions.infiniteReachLimit <= 0 || infiniteReachLimit > 0 && restrictions.infiniteReachLimit >= infiniteReachLimit
         ? infiniteReachLimit
         : restrictions.infiniteReachLimit;
   }
}
