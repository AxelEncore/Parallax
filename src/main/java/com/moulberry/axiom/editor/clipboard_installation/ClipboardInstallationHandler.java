package com.moulberry.axiom.editor.clipboard_installation;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.windows.ClipboardInstallationWindow;
import java.util.Objects;
import net.minecraft.client.Minecraft;

public class ClipboardInstallationHandler {
   private static String lastClipboard = null;

   public static void tick() {
      if (EditorUI.isActive()) {
         String clipboard = EditorUI.getClipboard();
         if (!Objects.equals(lastClipboard, clipboard)) {
            lastClipboard = clipboard;
            String[] split = clipboard.split("~");
            if (split.length >= 2) {
               if (split[0].equals("AxiomInstall")) {
                  Minecraft.getInstance().keyboardHandler.setClipboard("AxiomInstallDone");
                  String var2 = split[1];
                  switch (var2) {
                     case "DownloadBlueprint":
                        if (split.length >= 4) {
                           String name = split[2];
                           String downloadUrl = split[3];
                           ClipboardInstallationWindow.addTask(new ClipboardInstallBlueprintTask(name, downloadUrl));
                        }
                        break;
                     case "SetTheme":
                        if (split.length >= 4) {
                           String name = split[2];
                           String theme = split[3];
                           ClipboardInstallationWindow.addTask(new ClipboardInstallThemeTask(name, theme));
                        }
                  }
               }
            }
         }
      }
   }
}
