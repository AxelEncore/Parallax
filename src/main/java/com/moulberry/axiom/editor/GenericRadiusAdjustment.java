package com.moulberry.axiom.editor;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImDrawList;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;

public class GenericRadiusAdjustment {
   private int radius = 0;
   private int height = 0;
   private final int maxRadius;
   private float baseRadiusAdjustment = 0.0F;
   private float baseHeightAdjustment = 0.0F;
   private float distance = 16.0F;

   public GenericRadiusAdjustment(int maxRadius) {
      this.maxRadius = maxRadius;
   }

   public boolean initiateAdjustment(int radius, int height) {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result == null) {
         return false;
      } else {
         float raycastDistance = (float)result.getLocation().distanceTo(Minecraft.getInstance().player.getEyePosition());
         this.initiateAdjustment(radius, height, raycastDistance);
         return true;
      }
   }

   public void initiateAdjustment(int radius, int height, float distance) {
      this.radius = radius;
      this.height = height;
      float m11 = EditorUI.lastProjectionMatrix.m11();
      this.baseRadiusAdjustment = this.radius / distance / 2.0F * m11 * ImGui.getMainViewport().getSizeY();
      if (height >= 0) {
         this.baseHeightAdjustment = this.height / distance / 2.0F * m11 * ImGui.getMainViewport().getSizeY();
         float r = this.baseRadiusAdjustment;
         float h = this.baseHeightAdjustment;
         if (r > h) {
            this.baseHeightAdjustment = r / (float)Math.sqrt(r / h * r / h + 1.0F);
            this.baseRadiusAdjustment = (float)Math.sqrt(r * r - this.baseHeightAdjustment * this.baseHeightAdjustment);
         } else {
            this.baseRadiusAdjustment = h / (float)Math.sqrt(h / r * h / r + 1.0F);
            this.baseHeightAdjustment = (float)Math.sqrt(h * h - this.baseRadiusAdjustment * this.baseRadiusAdjustment);
         }
      } else {
         this.baseHeightAdjustment = 0.0F;
      }

      this.distance = distance;
   }

   public GenericRadiusAdjustment.Result renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      float deltaX = mouseDelta.x + this.baseRadiusAdjustment;
      float deltaY = mouseDelta.y - this.baseHeightAdjustment;
      float actualLength = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
      float factor = actualLength / Math.max(Math.abs(deltaX), Math.abs(deltaY));
      float m11 = EditorUI.lastProjectionMatrix.m11();
      int newRadius;
      int newHeight;
      if (this.height >= 0) {
         float lengthForHeight = Math.abs(deltaY * factor);
         float lengthForRadius = Math.abs(deltaX * factor);
         newRadius = Math.round(lengthForRadius / ImGui.getMainViewport().getSizeY() * this.distance * 2.0F / m11);
         newHeight = Math.round(lengthForHeight / ImGui.getMainViewport().getSizeY() * this.distance * 2.0F / m11);
         if (newRadius < 0) {
            newRadius = 0;
         }

         if (newRadius > this.maxRadius * 4) {
            newRadius = this.maxRadius * 4;
         }

         if (newHeight < 0) {
            newHeight = 0;
         }

         if (newHeight > this.maxRadius * 4) {
            newHeight = this.maxRadius * 4;
         }

         int maxVolume = this.maxRadius * this.maxRadius;
         if (newRadius * newHeight > maxVolume) {
            float scaleDown = (float)Math.sqrt((float)maxVolume / (newRadius * newHeight));
            newRadius = Math.round(newRadius * scaleDown);
            newHeight = Math.round(newHeight * scaleDown);
         }
      } else {
         newRadius = Math.round(actualLength / ImGui.getMainViewport().getSizeY() * this.distance * 2.0F / m11);
         newHeight = this.height;
         if (newRadius < 0) {
            newRadius = 0;
         }

         if (newRadius > this.maxRadius) {
            newRadius = this.maxRadius;
         }
      }

      boolean changed = false;
      if (this.radius != newRadius) {
         this.radius = newRadius;
         changed = true;
      }

      if (this.height != newHeight) {
         this.height = newHeight;
         changed = true;
      }

      if (deltaX != 0.0F || deltaY != 0.0F) {
         ImDrawList drawList = ImGui.getForegroundDrawList();
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

      return new GenericRadiusAdjustment.Result(mouseDelta, this.radius, this.height, changed);
   }

   public record Result(Vec2 mouseDelta, int radius, int height, boolean changed) {
   }
}
