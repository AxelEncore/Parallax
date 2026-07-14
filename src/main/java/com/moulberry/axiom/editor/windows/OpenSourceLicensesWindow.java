package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.ImGuiHelper;
import imgui.moulberry92.ImGui;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class OpenSourceLicensesWindow {
   private static final Map<String, String> LICENSES = new TreeMap<>(
      Map.ofEntries(
         Map.entry("Antlr4", "/licenses/Antlr4-LICENSE"),
         Map.entry("Caffeine", "/licenses/Caffeine-LICENSE"),
         Map.entry("Configurate", "/licenses/Configurate-LICENSE"),
         Map.entry("EvalEx", "/licenses/EvalEx-LICENSE"),
         Map.entry("FastNoiseLite", "/licenses/FastNoiseLite-LICENSE"),
         Map.entry("ImGui", "/licenses/ImGui-LICENSE"),
         Map.entry("ImGuiJava", "/licenses/ImGuiJava-LICENSE"),
         Map.entry("JavaImageScaling", "/licenses/JavaImageScaling-LICENSE"),
         Map.entry("JavaJWT", "/licenses/JavaJWT-LICENSE"),
         Map.entry("JNoise", "/licenses/JNoise-LICENSE"),
         Map.entry("Lattice", "/licenses/Lattice-LICENSE"),
         Map.entry("LuaJ", "/licenses/LuaJ-LICENSE"),
         Map.entry("LWJGL", "/licenses/LWJGL-LICENSE"),
         Map.entry("MixinConstraints", "/licenses/MixinConstraints-LICENSE"),
         Map.entry("Fonts", "/licenses/OFL.txt"),
         Map.entry("QuickHull3D", "/licenses/QuickHull3D-LICENSE"),
         Map.entry("ZstdJni", "/licenses/ZstdJni-LICENSE")
      )
   );
   public static boolean open = false;
   private static final Map<String, String> LICENSE_CONTENT = new HashMap<>();

   public static void render() {
      if (open) {
         open = false;
         ImGui.openPopup("###OpenSourceLicenses");
      }

      ImGui.setNextWindowSize(720.0F, 500.0F);
      if (ImGuiHelper.beginPopupModalCloseable("Open Source Licenses###OpenSourceLicenses")) {
         for (Entry<String, String> entry : LICENSES.entrySet()) {
            if (ImGui.collapsingHeader(entry.getKey())) {
               String content = getLicenseContent(entry.getKey(), entry.getValue());
               ImGui.indent();
               ImGui.textWrapped(content);
               ImGui.unindent();
            }
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   private static String getLicenseContent(String key, String path) {
      String cached = LICENSE_CONTENT.get(key);
      if (cached != null) {
         return cached;
      } else {
         try (InputStream inputStream = OpenSourceLicensesWindow.class.getResourceAsStream(path)) {
            if (inputStream != null) {
               String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
               LICENSE_CONTENT.put(key, content);
               return content;
            }
         } catch (IOException var8) {
            Axiom.LOGGER.error("Unable to load open source license file", var8);
         }

         String errorMsg = "Error: Unable to read " + path;
         LICENSE_CONTENT.put(key, errorMsg);
         return errorMsg;
      }
   }
}
