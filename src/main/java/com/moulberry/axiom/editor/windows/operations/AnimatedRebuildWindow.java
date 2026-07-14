package com.moulberry.axiom.editor.windows.operations;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.operations.RebuildOperation;
import imgui.moulberry92.ImGui;

public class AnimatedRebuildWindow {
   private static final float[] startDelay = new float[]{0.5F};
   private static final float[] maxDelay = new float[]{0.25F};
   private static final float[] airDelay = new float[]{2.0F};
   private static final float[] sameBlockDelay = new float[]{0.2F};
   private static final float[] differentBlockDelay = new float[]{0.5F};
   private static final float[] randomExtraDelay = new float[]{0.1F};
   private static final float[] horizontalDelayMultiplier = new float[]{1.0F};
   private static final float[] verticalDelayMultiplier = new float[]{1.0F};

   public static void render() {
      if (EditorWindowType.ANIMATED_REBUILD.isOpen()) {
         if (EditorWindowType.ANIMATED_REBUILD.begin("###AnimatedRebuild", true)) {
            ImGui.text("⚠ Ensure you create a backup of your build before using this operation on it");
            ImGuiHelper.separatorWithText("Parameters");
            ImGui.sliderFloat("Start Delay (Seconds)", startDelay, 0.0F, 2.0F);
            ImGui.sliderFloat("Max Delay (Seconds)", maxDelay, 0.0F, 2.0F);
            ImGui.sliderFloat("Air Block Delay (Seconds)", airDelay, 0.0F, 2.0F);
            ImGui.sliderFloat("Same Block Delay (Seconds)", sameBlockDelay, 0.0F, 2.0F);
            ImGui.sliderFloat("Different Block Delay (Seconds)", differentBlockDelay, 0.0F, 2.0F);
            ImGui.sliderFloat("Random Extra Delay (Seconds)", randomExtraDelay, 0.0F, 2.0F);
            ImGui.sliderFloat("Horizontal Multiplier", horizontalDelayMultiplier, 0.0F, 2.0F);
            ImGui.sliderFloat("Vertical Multiplier", verticalDelayMultiplier, 0.0F, 2.0F);
            ImGuiHelper.separatorWithText("Starting Point");
            ImGui.textWrapped(
               "Place Structure Blocks to specify the starting points of the animation. If no Structure Blocks are present then the center is used instead"
            );
            ImGuiHelper.separatorWithText("Presets");
            if (ImGui.button(AxiomI18n.get("axiom.hardcoded.default"))) {
               maxDelay[0] = 0.25F;
               airDelay[0] = 2.0F;
               sameBlockDelay[0] = 0.2F;
               differentBlockDelay[0] = 0.5F;
               randomExtraDelay[0] = 0.1F;
               horizontalDelayMultiplier[0] = 1.0F;
               verticalDelayMultiplier[0] = 1.0F;
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.hardcoded.horizontal_sheets"))) {
               maxDelay[0] = 1.0F;
               airDelay[0] = 1.0F;
               sameBlockDelay[0] = 1.0F;
               differentBlockDelay[0] = 1.0F;
               randomExtraDelay[0] = 0.0F;
               horizontalDelayMultiplier[0] = 0.0F;
               verticalDelayMultiplier[0] = 1.0F;
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.hardcoded.very_random"))) {
               maxDelay[0] = 0.25F;
               airDelay[0] = 0.5F;
               sameBlockDelay[0] = 0.1F;
               differentBlockDelay[0] = 0.2F;
               randomExtraDelay[0] = 2.0F;
               horizontalDelayMultiplier[0] = 1.0F;
               verticalDelayMultiplier[0] = 1.0F;
            }

            ImGuiHelper.separatorWithText("Animated Rebuild");
            boolean disabled = Selection.getSelectionBuffer().isEmpty();
            if (disabled) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.hardcoded.do_animated_rebuild")) && !disabled) {
               RebuildOperation.rebuild(
                  new RebuildOperation.RebuildParameters(
                     startDelay[0],
                     maxDelay[0],
                     airDelay[0],
                     sameBlockDelay[0],
                     differentBlockDelay[0],
                     randomExtraDelay[0],
                     horizontalDelayMultiplier[0],
                     verticalDelayMultiplier[0]
                  )
               );
            }

            if (disabled) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.ANIMATED_REBUILD.end();
      }
   }
}
