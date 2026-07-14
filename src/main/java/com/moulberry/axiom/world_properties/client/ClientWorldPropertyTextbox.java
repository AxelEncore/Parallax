package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import net.minecraft.resources.ResourceLocation;

public class ClientWorldPropertyTextbox extends ClientWorldProperty<String> {
   public ClientWorldPropertyTextbox(ResourceLocation id, String name, boolean localizeName, String initialValue) {
      super(id, name, localizeName, initialValue);
   }

   @Override
   public void renderImgui() {
      ImString imString = new ImString(this.getLocalValue(), 64);
      if (ImGui.inputText(this.getLocalizedName() + "###" + this.getId().toString(), imString, 64)) {
         this.changeLocalValue(ImGuiHelper.getString(imString));
      }
   }

   @Override
   public WorldPropertyDataType<String> getType() {
      return WorldPropertyDataType.STRING;
   }
}
