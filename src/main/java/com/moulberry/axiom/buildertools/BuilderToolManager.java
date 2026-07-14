package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.FlightSpeedAdjustment;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;

public class BuilderToolManager {
   private static final List<BuilderTool> tools = List.of(
      new MoveBuilderTool(),
      new CloneBuilderTool(),
      new StackBuilderTool(),
      new SmearBuilderTool(),
      new ExtrudeBuilderTool(),
      new EraseBuilderTool(),
      new MirrorBuilderTool()
   );
   private static boolean toolSlotActive = false;
   private static int toolSlotSelected = 0;
   public static boolean copyAir = false;
   public static boolean copyEntities = true;
   public static boolean keepExisting = false;

   public static int scroll(int from, int to, int dir) {
      if (FlightSpeedAdjustment.isEnabled()) {
         FlightSpeedAdjustment.scroll(dir);
         return from;
      } else if (!canUseBuilderTools()) {
         return -1;
      } else if (!toolSlotActive && !Axiom.configuration.builderTools.showBuilderToolSlot) {
         return -1;
      } else if (toolSlotActive) {
         if (tools.get(toolSlotSelected).scroll(dir)) {
            return from;
         } else {
            setToolSlotActive(false);
            return dir > 0 ? 8 : 0;
         }
      } else if (from == 8 && to == 0) {
         setToolSlotActive(true);
         return from;
      } else if (from == 0 && to == 8) {
         setToolSlotActive(true);
         return from;
      } else {
         return -1;
      }
   }

   public static boolean canUseBuilderTools() {
      if (!AxiomClient.isAxiomActive()) {
         return false;
      } else {
         for (BuilderTool tool : tools) {
            if (AxiomClient.hasPermissions(tool.requiredPermissions())) {
               return true;
            }
         }

         return false;
      }
   }

   public static void ensureSelectedToolIsUsable() {
      int start = toolSlotSelected;

      do {
         BuilderTool selected = tools.get(toolSlotSelected);
         if (AxiomClient.hasPermissions(selected.requiredPermissions())) {
            return;
         }

         toolSlotSelected++;
         if (toolSlotSelected >= tools.size()) {
            toolSlotSelected = 0;
         }
      } while (toolSlotSelected != start);

      toolSlotActive = false;
   }

   public static void setToolSlotActive(boolean active) {
      if (!active || canUseBuilderTools()) {
         if (toolSlotActive != active) {
            tools.get(toolSlotSelected).reset(true);
            toolSlotActive = active;
         }

         ensureSelectedToolIsUsable();
      }
   }

   public static boolean isToolSlotActive() {
      if (!canUseBuilderTools()) {
         toolSlotActive = false;
         return false;
      } else {
         return !EditorUI.isActive() && toolSlotActive;
      }
   }

   public static boolean selectedToolIs(Class<? extends BuilderTool> clazz) {
      return clazz.isAssignableFrom(tools.get(toolSlotSelected).getClass());
   }

   public static int getToolSlotSelected() {
      return toolSlotSelected;
   }

   public static int getToolCount() {
      return tools.size();
   }

   public static List<BuilderTool> getTools() {
      return tools;
   }

   public static String getToolName(int index) {
      return tools.get(index).getName();
   }

   public static BuilderTool getToolForIndex(int slot) {
      slot %= tools.size();
      return tools.get(slot);
   }

   public static void setToolSlotSelected(int slot) {
      slot %= tools.size();
      int oldSlot = toolSlotSelected;
      toolSlotSelected = slot;
      ensureSelectedToolIsUsable();
      if (oldSlot != toolSlotSelected) {
         BuilderToolSelectionState.Restore restore = tools.get(oldSlot).getSelectionRestore();
         tools.get(oldSlot).reset(true);
         tools.get(toolSlotSelected).reset(true);
         if (restore != null) {
            tools.get(toolSlotSelected).applySelectionRestore(restore);
         }
      }
   }

   public static boolean shouldRenderBlockOutline(BlockPos blockPos) {
      return tools.get(toolSlotSelected).shouldRenderBlockOutline(blockPos);
   }

   public static void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      tools.get(toolSlotSelected).renderScreen(guiGraphics, screenWidth, screenHeight, partialTick);
   }

   public static void renderWorld(AxiomWorldRenderContext rc) {
      tools.get(toolSlotSelected).renderWorld(rc);
   }

   public static List<String> getKeyHints() {
      return tools.get(toolSlotSelected).getKeyHints();
   }

   public static boolean setPos1(BlockPos position) {
      return tools.get(toolSlotSelected).setPos1(position);
   }

   public static boolean setPos2(BlockPos position) {
      return tools.get(toolSlotSelected).setPos2(position);
   }

   public static UserAction.ActionResult callAction(UserAction userAction, Object object) {
      if (!isToolSlotActive()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         switch (userAction) {
            case UNDO:
               if (tools.get(toolSlotSelected).canBeReset()) {
                  tools.get(toolSlotSelected).reset(false);
                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case REDO:
               if (tools.get(toolSlotSelected).canBeReset()) {
                  return UserAction.ActionResult.USED_STOP;
               }

               return UserAction.ActionResult.NOT_HANDLED;
            case LEFT_MOUSE:
               tools.get(toolSlotSelected).leftClick(Minecraft.getInstance().hitResult);
               return UserAction.ActionResult.USED_STOP;
            case RIGHT_MOUSE:
               tools.get(toolSlotSelected).rightClick(Minecraft.getInstance().hitResult);
               return UserAction.ActionResult.USED_STOP;
            default:
               return UserAction.ActionResult.NOT_HANDLED;
         }
      }
   }

   public static void middleClick(HitResult hitResult) {
      tools.get(toolSlotSelected).middleClick(hitResult);
   }

   public static void handleInput(boolean nudgeForwards, boolean nudgeBackwards, boolean delete) {
      if (Minecraft.getInstance().screen == null) {
         tools.get(toolSlotSelected).handleInput(nudgeForwards, nudgeBackwards, delete);
      }
   }
}
