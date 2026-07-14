package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import imgui.moulberry92.ImGui;
import net.minecraft.resources.ResourceLocation;

public class ClientWorldPropertyCheckbox extends ClientWorldProperty<Boolean> {
   public ClientWorldPropertyCheckbox(ResourceLocation id, String name, boolean localizeName, Boolean initialValue) {
      super(id, name, localizeName, initialValue);
   }

   @Override
   public void renderImgui() {
      if (ImGui.checkbox(this.getLocalizedName(), this.getLocalValue())) {
         this.changeLocalValue(!this.getLocalValue());
      }
   }

   @Override
   public WorldPropertyDataType<Boolean> getType() {
      return WorldPropertyDataType.BOOLEAN;
   }
}
