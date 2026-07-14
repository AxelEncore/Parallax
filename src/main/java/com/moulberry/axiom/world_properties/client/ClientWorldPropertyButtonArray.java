package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public class ClientWorldPropertyButtonArray extends ClientWorldProperty<Integer> {
   private final List<String> otherButtons;

   public ClientWorldPropertyButtonArray(ResourceLocation id, String name, boolean localizeName, List<String> otherButtons) {
      super(id, name, localizeName, 0);
      this.otherButtons = otherButtons;
   }

   @Override
   public void renderImgui() {
      String[] array = new String[this.otherButtons.size() + 1];
      array[0] = this.getLocalizedName();

      for (int i = 0; i < this.otherButtons.size(); i++) {
         array[i + 1] = this.localizeName ? AxiomI18n.get(this.otherButtons.get(i)) : this.otherButtons.get(i);
      }

      int pressed = ImGuiHelper.buttons(array);
      if (pressed >= 0) {
         this.changeLocalValue(pressed);
      }
   }

   @Override
   public WorldPropertyDataType<Integer> getType() {
      return WorldPropertyDataType.INTEGER;
   }
}
