package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import imgui.moulberry92.ImGui;
import net.minecraft.resources.ResourceLocation;

public class ClientWorldPropertySlider extends ClientWorldProperty<Integer> {
   private int min;
   private int max;

   public ClientWorldPropertySlider(ResourceLocation id, String name, boolean localizeName, Integer initialValue, int min, int max) {
      super(id, name, localizeName, initialValue);
      this.min = min;
      this.max = max;
   }

   @Override
   public void renderImgui() {
      int localValue = this.getLocalValue();
      int[] wrapper = new int[]{localValue};
      ImGui.setNextItemWidth(ImGui.calcItemWidth() / 2.0F);
      if (ImGui.sliderInt(this.getLocalizedName(), wrapper, this.min, this.max) && wrapper[0] != localValue) {
         this.changeLocalValue(wrapper[0]);
      }
   }

   @Override
   public WorldPropertyDataType<Integer> getType() {
      return WorldPropertyDataType.INTEGER;
   }
}
