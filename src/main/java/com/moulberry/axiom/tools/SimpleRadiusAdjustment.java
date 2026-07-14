package com.moulberry.axiom.tools;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.editor.EditorUI;
import imgui.moulberry92.ImDrawList;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public class SimpleRadiusAdjustment {
   public static boolean initiateAdjustment(int[] radius, float[] baseRadiusAdjustment) {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result == null) {
         return false;
      } else {
         float raycastDistance = (float)result.getLocation().distanceTo(Minecraft.getInstance().player.getEyePosition());
         float m11 = EditorUI.lastProjectionMatrix.m11();
         baseRadiusAdjustment[0] = radius[0] / raycastDistance / 2.0F * m11 * ImGui.getMainViewport().getSizeY();
         return true;
      }
   }

   public static Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta, int maxRadius, int[] radius, float[] baseRadiusAdjustment) {
      ImDrawList drawList = ImGui.getForegroundDrawList();
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result == null) {
         return mouseDelta;
      } else {
         float deltaX = mouseDelta.x + baseRadiusAdjustment[0];
         float deltaY = mouseDelta.y;
         double raycastDistance = result.getLocation().distanceTo(Minecraft.getInstance().player.getEyePosition());
         float actualLength = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
         float m11 = EditorUI.lastProjectionMatrix.m11();
         int newRadius = (int)Math.round(actualLength / ImGui.getMainViewport().getSizeY() * raycastDistance * 2.0 / m11);
         if (newRadius < 0) {
            newRadius = 0;
         }

         if (newRadius > maxRadius) {
            newRadius = maxRadius;
         }

         radius[0] = newRadius;
         if (deltaX != 0.0F || deltaY != 0.0F) {
            float targetLength = 6.0F;
            float divisions = actualLength / targetLength;
            if (divisions < 1.0F) {
               divisions = 1.0F;
            }

            if (divisions > 100.0F) {
               divisions = 100.0F;
            }

            for (int i = 1; i < divisions; i += 2) {
               float x1 = deltaX - deltaX * i / divisions;
               float y1 = deltaY - deltaY * i / divisions;
               float x2 = deltaX - deltaX * Math.min(1.0F, (i + 1) / divisions);
               float y2 = deltaY - deltaY * Math.min(1.0F, (i + 1) / divisions);
               drawList.addLine(mouseX + x1, mouseY + y1, mouseX + x2, mouseY + y2, -16777216, 1.0F);
            }

            float angle = (float)Math.atan2(deltaX, deltaY);
            float arrowMainX = Mth.sin(angle);
            float arrowMainY = Mth.cos(angle);
            float arrowLeftX = Mth.sin(angle + (float) (Math.PI * 3.0 / 4.0));
            float arrowLeftY = Mth.cos(angle + (float) (Math.PI * 3.0 / 4.0));
            float arrowRightX = Mth.sin(angle - (float) (Math.PI * 3.0 / 4.0));
            float arrowRightY = Mth.cos(angle - (float) (Math.PI * 3.0 / 4.0));
            drawList.addLine(
               mouseX + deltaX + arrowMainX * 4.0F,
               mouseY + deltaY + arrowMainY * 4.0F,
               mouseX + deltaX + arrowMainX * 15.0F,
               mouseY + deltaY + arrowMainY * 15.0F,
               -16777216,
               2.0F
            );
            drawList.addLine(
               mouseX + deltaX + arrowMainX * 15.0F - arrowLeftX,
               mouseY + deltaY + arrowMainY * 15.0F - arrowLeftY,
               mouseX + deltaX + arrowMainX * 15.0F + arrowLeftX * 7.0F,
               mouseY + deltaY + arrowMainY * 15.0F + arrowLeftY * 7.0F,
               -16777216,
               2.0F
            );
            drawList.addLine(
               mouseX + deltaX + arrowMainX * 15.0F - arrowRightX,
               mouseY + deltaY + arrowMainY * 15.0F - arrowRightY,
               mouseX + deltaX + arrowMainX * 15.0F + arrowRightX * 7.0F,
               mouseY + deltaY + arrowMainY * 15.0F + arrowRightY * 7.0F,
               -16777216,
               2.0F
            );
            drawList.addLine(
               mouseX + deltaX - arrowMainX * 4.0F,
               mouseY + deltaY - arrowMainY * 4.0F,
               mouseX + deltaX - arrowMainX * 15.0F,
               mouseY + deltaY - arrowMainY * 15.0F,
               -16777216,
               2.0F
            );
            drawList.addLine(
               mouseX + deltaX - arrowMainX * 15.0F + arrowLeftX,
               mouseY + deltaY - arrowMainY * 15.0F + arrowLeftY,
               mouseX + deltaX - arrowMainX * 15.0F - arrowLeftX * 7.0F,
               mouseY + deltaY - arrowMainY * 15.0F - arrowLeftY * 7.0F,
               -16777216,
               2.0F
            );
            drawList.addLine(
               mouseX + deltaX - arrowMainX * 15.0F + arrowRightX,
               mouseY + deltaY - arrowMainY * 15.0F + arrowRightY,
               mouseX + deltaX - arrowMainX * 15.0F - arrowRightX * 7.0F,
               mouseY + deltaY - arrowMainY * 15.0F - arrowRightY * 7.0F,
               -16777216,
               2.0F
            );
         }

         return mouseDelta;
      }
   }
}
