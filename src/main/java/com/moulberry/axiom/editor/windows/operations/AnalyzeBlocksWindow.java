package com.moulberry.axiom.editor.windows.operations;

import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.AnalyzeBlocksOperation;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;

public class AnalyzeBlocksWindow {
   private static AnalyzeBlocksOperation.Information blockCountInformation = null;
   private static boolean filterBlockEntities = false;

   public static void render() {
      if (EditorWindowType.ANALYZE.isOpen()) {
         if (EditorWindowType.ANALYZE.begin("###AnalyzeBlocks", true)) {
            if (blockCountInformation != null && ImGui.beginTable("##BlockCounts", 3, 1920)) {
               ImGui.tableNextColumn();
               ImGui.text(AxiomI18n.get("axiom.editorui.window.analyze.block_type"));
               ImGui.tableNextColumn();
               ImGui.text(AxiomI18n.get("axiom.editorui.window.analyze.block_count"));
               ImGui.tableNextColumn();
               ImGui.text(AxiomI18n.get("axiom.editorui.window.analyze.block_distribution"));
               ImGui.tableNextColumn();
               ImGui.tableNextColumn();
               ImGui.tableNextColumn();
               ImGui.tableNextColumn();

               for (AnalyzeBlocksOperation.InformationEntry entry : blockCountInformation.list()) {
                  String countString = NumberFormat.getInstance().format((long)entry.count());
                  String blockName = AxiomI18n.get(entry.block().axiom$translationKey());
                  float ratio = entry.count() * 100.0F / blockCountInformation.total();
                  ImGui.text(blockName);
                  ImGui.tableNextColumn();
                  ImGui.text(countString);
                  ImGui.tableNextColumn();
                  ImGui.text(String.format("%.1f%%", ratio));
                  ImGui.tableNextColumn();
               }

               ImGui.tableNextColumn();
               ImGui.tableNextColumn();
               ImGui.tableNextColumn();
               ImGui.text(AxiomI18n.get("axiom.editorui.window.block_distribution_total"));
               ImGui.tableNextColumn();
               ImGui.text(NumberFormat.getInstance().format((long)blockCountInformation.total()));
               ImGui.tableNextColumn();
               ImGui.text("100.0%");
               ImGui.endTable();
            }

            if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.analyze.filter_block_entities"), filterBlockEntities)) {
               filterBlockEntities = !filterBlockEntities;
            }

            boolean disable = Selection.getSelectionBuffer().isEmpty();
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.analyze.do_analyze"))) {
               blockCountInformation = AnalyzeBlocksOperation.analyze(filterBlockEntities);
            }

            if (disable) {
               ImGui.endDisabled();
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.analyze.clear_information"))) {
               blockCountInformation = null;
            }
         }

         EditorWindowType.ANALYZE.end();
      }
   }
}
