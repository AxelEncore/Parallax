package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.clipboard_installation.ClipboardInstallationTask;
import com.moulberry.axiom.i18n.AxiomI18n;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import java.util.ArrayList;
import java.util.List;

public class ClipboardInstallationWindow {
   private static List<ClipboardInstallationTask> tasks = new ArrayList<>();
   private static ClipboardInstallationTask runningTask = null;

   public static void render() {
      if (runningTask != null && runningTask.isFinished()) {
         tasks.remove(runningTask);
         runningTask = null;
      }

      if (!tasks.isEmpty() || runningTask != null) {
         String title = AxiomI18n.get("axiom.editorui.window.clipboard_installation");
         ImVec2 center = ImGui.getMainViewport().getCenter();
         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         ImGui.setNextWindowSize(300.0F, 0.0F);
         if (!ImGui.isPopupOpen("###ClipboardInstallation")) {
            ImGui.openPopup("###ClipboardInstallation");
         }

         if (ImGuiHelper.beginPopupModal(title + "###ClipboardInstallation", 68)) {
            if (runningTask != null) {
               Exception exception = runningTask.getException();
               if (exception != null) {
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.exception_occurred") + exception.getMessage());
                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.close"))) {
                     tasks.remove(runningTask);
                     runningTask = null;
                     ImGui.closeCurrentPopup();
                  }
               } else {
                  int progressInt = Math.min(99, (int)(runningTask.progress() * 100.0F));
                  ImGui.text(AxiomI18n.get("axiom.hardcoded.progress") + progressInt + "%");
               }
            } else {
               ClipboardInstallationTask task = tasks.get(0);
               task.renderConfirmationPopup();
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
                  tasks.remove(task);
               }

               ImGui.sameLine();
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.install"))) {
                  runningTask = task;
                  runningTask.start();
                  if (runningTask.isFinished()) {
                     tasks.remove(runningTask);
                     runningTask = null;
                  }
               }
            }

            ImGui.endPopup();
         }
      }
   }

   public static void addTask(ClipboardInstallationTask task) {
      if (!tasks.contains(task)) {
         tasks.add(task);
      }
   }
}
