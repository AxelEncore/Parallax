package com.moulberry.axiom;

import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.integration.ServerIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public class FlightSpeedAdjustment {
   private static final ResourceLocation FLIGHT_SPEED_ADJUSTMENT_ON = ResourceLocation.fromNamespaceAndPath("axiom", "gui/flight_speed_adjustment_on.png");
   private static final ResourceLocation FLIGHT_SPEED_ADJUSTMENT_OFF = ResourceLocation.fromNamespaceAndPath("axiom", "gui/flight_speed_adjustment_off.png");

   public static boolean isEnabled() {
      return AxiomClient.isAxiomActive() && ClientEvents.toggleFlightSpeedAdjustment.isDown() && !EditorUI.isActive();
   }

   public static void scroll(int dir) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         float flyingSpeed = player.getAbilities().getFlyingSpeed();
         float amount;
         if (flyingSpeed < 0.1 - 1.0E-6 * dir) {
            amount = 0.005F;
         } else if (flyingSpeed < 0.25 - 1.0E-6 * dir) {
            amount = 0.01F;
         } else {
            amount = 0.025F;
         }

         ServerIntegration.changeFlySpeed(Math.max(0.05F, Math.min(0.5F, Math.round(flyingSpeed / amount + dir) * amount)));
      }
   }

   public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         float flySpeed = player.getAbilities().getFlyingSpeed() / 0.05F;
         float flyAmount;
         if (flySpeed <= 1.0F) {
            flyAmount = 0.0F;
         } else {
            flyAmount = (float)Math.sqrt((flySpeed - 1.0F) / 8.99F);
            if (flyAmount > 1.0F) {
               flyAmount = 1.0F;
            }
         }

         String textDisplay;
         if (flySpeed > 9.99999 && flySpeed < 10.00001) {
            textDisplay = "999%";
         } else {
            textDisplay = Math.round(flySpeed * 100.0F) + "%";
         }

         int barX = screenWidth / 2 - 46;
         int barY = screenHeight * 3 / 4 + 9;
         if (flyAmount <= 0.0F) {
            VersionUtilsClient.genericBlit(guiGraphics, Dummy.GUI_TEXTURED, FLIGHT_SPEED_ADJUSTMENT_OFF, barX, barY, 92.0F, 0.0F, 92, 16, 92, 16, 92, 16, -1);
         } else if (flyAmount >= 1.0F) {
            VersionUtilsClient.genericBlit(guiGraphics, Dummy.GUI_TEXTURED, FLIGHT_SPEED_ADJUSTMENT_ON, barX, barY, 92.0F, 0.0F, 92, 16, 92, 16, 92, 16, -1);
         } else {
            int flyAmountI = 14 + (int)Math.floor(flyAmount * 64.0F);
            VersionUtilsClient.genericBlit(
               guiGraphics, Dummy.GUI_TEXTURED, FLIGHT_SPEED_ADJUSTMENT_ON, barX, barY, 0.0F, 0.0F, flyAmountI, 16, flyAmountI, 16, 92, 16, -1
            );
            VersionUtilsClient.genericBlit(
               guiGraphics,
               Dummy.GUI_TEXTURED,
               FLIGHT_SPEED_ADJUSTMENT_OFF,
               barX + flyAmountI,
               barY,
               flyAmountI,
               0.0F,
               92 - flyAmountI,
               16,
               92 - flyAmountI,
               16,
               92,
               16,
               -1
            );
         }

         guiGraphics.drawCenteredString(mc.font, AxiomI18n.get("axiom.flight_speed") + ": " + textDisplay, screenWidth / 2, screenHeight * 3 / 4, -1);
      }
   }
}
