package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import imgui.moulberry92.ImGui;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

public class ClientWorldPropertyButton extends ClientWorldProperty<Unit> {
   public ClientWorldPropertyButton(ResourceLocation id, String name, boolean localizeName) {
      super(id, name, localizeName, Unit.INSTANCE);
   }

   @Override
   public void renderImgui() {
      if (ImGui.button(this.getLocalizedName())) {
         this.changeLocalValue(Unit.INSTANCE);
      }
   }

   @Override
   public WorldPropertyDataType<Unit> getType() {
      return WorldPropertyDataType.EMPTY;
   }
}
