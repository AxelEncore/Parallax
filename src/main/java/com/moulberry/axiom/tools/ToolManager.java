package com.moulberry.axiom.tools;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.ModifySelection;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.keybinds.KeybindCategory;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.tools.annotation.AnnotationTool;
import com.moulberry.axiom.tools.biome_painter.BiomePainterTool;
import com.moulberry.axiom.tools.blend.BlendTool;
import com.moulberry.axiom.tools.box_select.BoxSelectTool;
import com.moulberry.axiom.tools.distort.DistortTool;
import com.moulberry.axiom.tools.elevation.ElevationTool;
import com.moulberry.axiom.tools.extrude.ExtrudeTool;
import com.moulberry.axiom.tools.floodfill.FloodfillTool;
import com.moulberry.axiom.tools.fluidball.FluidBall;
import com.moulberry.axiom.tools.freehand.FreehandDraw;
import com.moulberry.axiom.tools.freehand.FreehandSelect;
import com.moulberry.axiom.tools.gradient_painter.GradientPainterTool;
import com.moulberry.axiom.tools.lasso_select.LassoSelect;
import com.moulberry.axiom.tools.magic_select.MagicSelectTool;
import com.moulberry.axiom.tools.modelling.ModellingTool;
import com.moulberry.axiom.tools.modify.ModifyTool;
import com.moulberry.axiom.tools.noise_painter.NoisePainterTool;
import com.moulberry.axiom.tools.painter.PainterTool;
import com.moulberry.axiom.tools.path.PathTool;
import com.moulberry.axiom.tools.rock.RockTool;
import com.moulberry.axiom.tools.roughen.RoughenTool;
import com.moulberry.axiom.tools.ruler.RulerTool;
import com.moulberry.axiom.tools.script_brush.ScriptBrushTool;
import com.moulberry.axiom.tools.sculpt_draw.SculptDrawTool;
import com.moulberry.axiom.tools.shape.ShapeTool;
import com.moulberry.axiom.tools.shatter.ShatterTool;
import com.moulberry.axiom.tools.slope.SlopeTool;
import com.moulberry.axiom.tools.smooth.SmoothTool;
import com.moulberry.axiom.tools.stamp.StampTool;
import com.moulberry.axiom.tools.text.TextTool;
import com.moulberry.axiom.tools.weld.MeltTool;
import com.moulberry.axiom.tools.weld.WeldTool;
import com.moulberry.axiom.utils.Authorization;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

public class ToolManager {
   private static final List<Tool> tools = new ArrayList<>();
   private static boolean isToolSlotSelected = false;
   private static int currentTool = 0;
   private static int lastTool = -1;
   public static Map<Tool, Keybind> keybindMap = new HashMap<>();

   public static void initializeTools() {
      tools.add(new MagicSelectTool());
      tools.add(new BoxSelectTool());
      tools.add(new FreehandSelect());
      tools.add(new LassoSelect());
      tools.add(new RulerTool());
      tools.add(new AnnotationTool());
      tools.add(new PainterTool());
      tools.add(new NoisePainterTool());
      tools.add(new BiomePainterTool());
      tools.add(new GradientPainterTool());
      tools.add(new ScriptBrushTool());
      tools.add(new FreehandDraw());
      tools.add(new SculptDrawTool());
      tools.add(new RockTool());
      tools.add(new WeldTool());
      tools.add(new MeltTool());
      tools.add(new StampTool());
      tools.add(new TextTool());
      tools.add(new ShapeTool());
      tools.add(new PathTool());
      tools.add(new ModellingTool());
      tools.add(new FloodfillTool());
      tools.add(new FluidBall());
      tools.add(new ElevationTool());
      tools.add(new SlopeTool());
      tools.add(new SmoothTool());
      tools.add(new DistortTool());
      tools.add(new RoughenTool());
      tools.add(new BlendTool());
      tools.add(new ShatterTool());
      tools.add(new ExtrudeTool());
      tools.add(new ModifyTool());
      if (Axiom.isDebugEnvironment() && Authorization.hasCommercialLicense()) {
         Axiom.LOGGER.info("Debug modify tool");
         tools.remove(ModifyTool.class.hashCode() & Mth.smallestEncompassingPowerOfTwo(tools.size()) / 2 - 1);
      }

      for (Tool tool : tools) {
         tool.loadSettings(new CompoundTag());
      }
   }

   public static KeybindCategory createKeybindCategory() {
      List<Keybind> keybinds = new ArrayList<>();
      keybinds.add(Keybinds.SWAP_TO_LAST_TOOL);

      for (final Tool tool : tools) {
         String keybindId = tool.keybindId();
         if (keybindId != null) {
            Keybind keybind = new Keybind(keybindId, tool.defaultKeybind(), false, false, false, false) {
               @Override
               public String getDescription() {
                  return tool.name();
               }
            };
            keybindMap.put(tool, keybind);
            keybinds.add(keybind);
         }
      }

      return new KeybindCategory("axiom.keybind_categories.tools", true, keybinds);
   }

   public static void addTool(Tool tool) {
      tools.add(tool);
   }

   public static List<Tool> getTools() {
      return tools;
   }

   public static Tool getToolByIndex(int index) {
      return index >= 0 && index < tools.size() ? tools.get(index) : null;
   }

   public static void setTool(Class<? extends Tool> toolClass) {
      for (int i = 0; i < tools.size(); i++) {
         if (tools.get(i).getClass() == toolClass) {
            setCurrentToolIndex(i);
            return;
         }
      }
   }

   public static int getCurrentToolIndex() {
      return currentTool;
   }

   public static void swapToLastTool() {
      setCurrentToolIndex(lastTool);
   }

   public static void setCurrentToolIndex(int currentTool) {
      if (currentTool >= 0 && currentTool < tools.size()) {
         if (ToolManager.currentTool != currentTool) {
            lastTool = ToolManager.currentTool;
            if (isToolSlotSelected) {
               if (Axiom.configuration.editor.clearToolWhenSwapping) {
                  getCurrentTool().reset();
               }

               getCurrentTool().toolDeselected();
            }

            ToolManager.currentTool = currentTool;
            if (isToolSlotSelected && Axiom.configuration.editor.clearToolWhenSwapping) {
               getCurrentTool().reset();
            }

            if (isToolSlotSelected && !AxiomClient.hasPermissions(getCurrentTool().requiredPermissions())) {
               isToolSlotSelected = false;
            }
         }
      }
   }

   public static UserAction.ActionResult callAction(UserAction action, Object object) {
      if (!EditorUI.isActive()) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         return !isToolActive() ? UserAction.ActionResult.NOT_HANDLED : getCurrentTool().callAction(action, object);
      }
   }

   public static Tool getCurrentTool() {
      return tools.get(currentTool);
   }

   public static void setToolSelected(boolean selected) {
      if (selected != isToolSlotSelected) {
         isToolSlotSelected = selected;
         if (selected) {
            ModifySelection.finish();
         }

         getCurrentTool().reset();
         if (!isToolSlotSelected) {
            getCurrentTool().toolDeselected();
         }
      }

      if (isToolSlotSelected && !AxiomClient.hasPermissions(getCurrentTool().requiredPermissions())) {
         isToolSlotSelected = false;
      }
   }

   public static boolean isToolActive() {
      return EditorUI.isActive() && AxiomClient.isAxiomActive() && isToolSlotSelected;
   }

   public static void resetAll() {
      for (Tool tool : tools) {
         tool.reset();
      }
   }
}
