package com.moulberry.axiom.world_properties.client;

import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.hooks.ClientLevelExt;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.integration.ServerIntegration;
import com.moulberry.axiom.world_properties.WorldPropertyDataType;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;

public class ClientWorldPropertyTime extends ClientWorldProperty<Unit> {
   public ClientWorldPropertyTime(ResourceLocation id, String name, boolean localizeName) {
      super(id, name, localizeName, Unit.INSTANCE);
   }

   @Override
   public void renderImgui() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         boolean isFrozen = ((ClientLevelExt)level).axiom$isTimeFrozen();
         if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.world_properties.freeze_time"), isFrozen)) {
            ServerIntegration.changeTimeFrozen(!isFrozen);
         }

         int currentTime = (int)(level.getDayTime() % 24000L);
         int[] time = new int[]{currentTime};
         ImGui.sliderInt(this.getLocalizedName() + "##TimeSlider", time, 0, 24000);
         switch (ImGuiHelper.buttons(
            AxiomI18n.get("axiom.editorui.window.world_properties.day"),
            AxiomI18n.get("axiom.editorui.window.world_properties.noon"),
            AxiomI18n.get("axiom.editorui.window.world_properties.night"),
            AxiomI18n.get("axiom.editorui.window.world_properties.midnight")
         )) {
            case 0:
               time[0] = 1000;
               break;
            case 1:
               time[0] = 6000;
               break;
            case 2:
               time[0] = 13000;
               break;
            case 3:
               time[0] = 18000;
         }

         if (time[0] != currentTime) {
            ServerIntegration.changeTime(time[0]);
         }
      }
   }

   @Override
   public WorldPropertyDataType<Unit> getType() {
      return WorldPropertyDataType.EMPTY;
   }
}
