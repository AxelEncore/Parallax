package com.moulberry.axiom;

import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.ModifySelection;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.world_modification.Dispatcher;
import java.util.List;
import net.minecraft.client.Minecraft;

public enum UserAction {
   ENTER,
   ESCAPE,
   CUT,
   DUPLICATE,
   COPY,
   PASTE,
   DELETE,
   UNDO,
   REDO,
   SAVE,
   EXTRUDE,
   ROTATE_PLACEMENT,
   FLIP_PLACEMENT,
   LEFT_MOUSE,
   RIGHT_MOUSE,
   MIDDLE_MOUSE,
   SCROLL;

   private static final List<UserAction.UserActionCallable> CALLABLES = List.of(
      ModifySelection::callAction,
      Placement.INSTANCE::callAction,
      ToolManager::callAction,
      Selection::callAction,
      Clipboard.INSTANCE::callAction,
      BuilderToolManager::callAction,
      DisplayEntityManipulator::callAction,
      MarkerEntityManipulator::callAction,
      GeneralGameFeatures::callAction,
      Dispatcher::callAction
   );

   public UserAction.ActionResult call(Object object) {
      if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
         UserAction.ActionResult result = UserAction.ActionResult.NOT_HANDLED;

         for (UserAction.UserActionCallable callable : CALLABLES) {
            switch (callable.callAction(this, object)) {
               case USED_STOP:
                  return UserAction.ActionResult.USED_STOP;
               case USED_CONT:
                  result = UserAction.ActionResult.USED_CONT;
            }
         }

         return result;
      } else {
         return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   public static enum ActionResult {
      USED_STOP,
      USED_CONT,
      NOT_HANDLED;
   }

   public record ScrollAmount(int scrollX, int scrollY) {
   }

   public interface UserActionCallable {
      UserAction.ActionResult callAction(UserAction var1, Object var2);
   }
}
