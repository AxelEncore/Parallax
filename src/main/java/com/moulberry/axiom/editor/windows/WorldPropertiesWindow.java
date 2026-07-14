package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.world_properties.WorldPropertyCategory;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertiesRegistry;
import com.moulberry.axiom.world_properties.client.ClientWorldProperty;
import imgui.moulberry92.ImGui;
import java.util.List;
import java.util.Map.Entry;

public class WorldPropertiesWindow {
   public static void render() {
      if (Axiom.getInstance().serverConfig != null) {
         if (EditorWindowType.WORLD_PROPERTIES.isOpen()) {
            if (EditorWindowType.WORLD_PROPERTIES.begin("###WorldProperties", true)) {
               if (ClientWorldPropertiesRegistry.PROPERTY_MAP.isEmpty()) {
                  ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.world_properties.unavailable"));
                  ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.window.world_properties.unavailable_description"));
                  EditorWindowType.WORLD_PROPERTIES.end();
                  return;
               }

               for (Entry<WorldPropertyCategory, List<ClientWorldProperty<?>>> entry : ClientWorldPropertiesRegistry.PROPERTY_LIST.entrySet()) {
                  if (ImGui.collapsingHeader(entry.getKey().getLocalizedName())) {
                     ImGui.indent(8.0F * EditorUI.getUiScale());

                     for (ClientWorldProperty<?> property : entry.getValue()) {
                        property.renderImgui();
                     }

                     ImGui.unindent(8.0F * EditorUI.getUiScale());
                  }
               }
            }

            EditorWindowType.WORLD_PROPERTIES.end();
         }
      }
   }
}
