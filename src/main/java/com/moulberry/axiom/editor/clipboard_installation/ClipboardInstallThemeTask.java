package com.moulberry.axiom.editor.clipboard_installation;

import com.moulberry.axiom.editor.styles.StyleHelper;
import com.moulberry.axiom.editor.styles.StyleManager;
import imgui.moulberry92.ImGui;
import java.util.Objects;

public class ClipboardInstallThemeTask implements ClipboardInstallationTask {
   private final String name;
   private final String value;
   private final String confirmationMessage;
   private boolean finished = false;

   public ClipboardInstallThemeTask(String name, String value) {
      this.name = name;
      this.value = value;
      this.confirmationMessage = "Do you want to install the theme '" + this.name;
   }

   @Override
   public void renderConfirmationPopup() {
      ImGui.textWrapped(this.confirmationMessage);
   }

   @Override
   public void start() {
      if (!this.finished) {
         StyleHelper.Theme theme = StyleHelper.Theme.convertFromBase64(this.value);
         if (theme != null) {
            StyleManager.loadTheme(theme);
         }

         this.finished = true;
      }
   }

   @Override
   public float progress() {
      return 0.0F;
   }

   @Override
   public boolean isFinished() {
      return this.finished;
   }

   @Override
   public Exception getException() {
      return null;
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         ClipboardInstallThemeTask that = (ClipboardInstallThemeTask)o;
         return Objects.equals(this.name, that.name) && Objects.equals(this.value, that.value);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(this.name);
      return 31 * result + Objects.hashCode(this.value);
   }
}
