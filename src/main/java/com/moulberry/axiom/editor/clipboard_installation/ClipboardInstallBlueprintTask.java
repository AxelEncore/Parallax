package com.moulberry.axiom.editor.clipboard_installation;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.blueprint.Blueprint;
import com.moulberry.axiom.blueprint.BlueprintIo;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.utils.AsyncFileDialogs;
import com.moulberry.axiom.utils.StringUtils;
import imgui.moulberry92.ImGui;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class ClipboardInstallBlueprintTask implements ClipboardInstallationTask {
   private final String name;
   private final String downloadUrl;
   private final String confirmationMessage;
   private boolean started = false;
   private boolean finished = false;
   private float progress = 0.0F;
   private Exception exception = null;

   public ClipboardInstallBlueprintTask(String name, String downloadUrl) {
      this.name = name;
      this.downloadUrl = downloadUrl;
      this.confirmationMessage = "Do you want to download and install the blueprint '" + this.name + "' from " + this.downloadUrl;
   }

   @Override
   public void renderConfirmationPopup() {
      ImGui.textWrapped(this.confirmationMessage);
   }

   @Override
   public void start() {
      if (!this.started) {
         this.started = true;
         new Thread(() -> {
            try {
               byte[] blueprintBytes = ClipboardInstallationTask.downloadWithProgress(new URL(this.downloadUrl), f -> this.progress = f * 0.9F);
               Blueprint blueprint = BlueprintIo.readBlueprint(new ByteArrayInputStream(blueprintBytes));
               Clipboard.INSTANCE.setClipboard(blueprint);
               this.progress = 0.95F;
               Path blueprintDir = Axiom.getInstance().getBlueprintDirectory();
               String separator = blueprintDir.getFileSystem().getSeparator();
               String blueprintNameString = this.name.trim();
               String snakeName;
               if (blueprintNameString.isEmpty()) {
                  snakeName = "unnamed.bp";
               } else {
                  snakeName = blueprintNameString.toLowerCase(Locale.ROOT).replace(' ', '_').replace(separator, "_") + ".bp";
               }

               snakeName = StringUtils.sanitizePath(snakeName);
               AsyncFileDialogs.saveFileDialog(blueprintDir.toString(), snakeName, "Blueprint Files", "bp").thenAccept(str -> {
                  if (str != null) {
                     Path path = Path.of(str);

                     try {
                        Files.write(path, blueprintBytes);
                     } catch (IOException var4x) {
                        throw new RuntimeException(var4x);
                     }
                  }
               }).get();
               this.finished = true;
            } catch (Exception var7) {
               Axiom.LOGGER.error("Error while installing blueprint", var7);
               this.exception = var7;
            }
         }).start();
      }
   }

   @Override
   public float progress() {
      return this.progress;
   }

   @Override
   public boolean isFinished() {
      return this.finished;
   }

   @Override
   public Exception getException() {
      return this.exception;
   }

   @Override
   public boolean equals(Object o) {
      if (o != null && this.getClass() == o.getClass()) {
         ClipboardInstallBlueprintTask that = (ClipboardInstallBlueprintTask)o;
         return Objects.equals(this.name, that.name) && Objects.equals(this.downloadUrl, that.downloadUrl);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(this.name);
      return 31 * result + Objects.hashCode(this.downloadUrl);
   }
}
