package com.moulberry.axiom.editor.widgets;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.brush_shapes.CapsuleBrushShape;
import com.moulberry.axiom.brush_shapes.ConeBrushShape;
import com.moulberry.axiom.brush_shapes.CubeBrushShape;
import com.moulberry.axiom.brush_shapes.CylinderBrushShape;
import com.moulberry.axiom.brush_shapes.OctahedronBrushShape;
import com.moulberry.axiom.brush_shapes.SphereBrushShape;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.SharedBrushPreviewRenderer;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImDrawList;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class BrushWidget {
   private static final int MAX_RADIUS = 50;
   private final int[] shape = new int[]{0};
   private final int[] radius = new int[]{5};
   private final int[] height = new int[]{5};
   private final float[] coneRounding = new float[]{0.0F};
   private final int[] coneDirection = new int[]{0};
   private boolean dirty = false;
   private BrushShape brushShape = null;
   private static final int SHAPE_SPHERE = 0;
   private static final int SHAPE_CUBE = 1;
   private static final int SHAPE_OCTAHEDRON = 2;
   private static final int SHAPE_CYLINDER = 3;
   private static final int SHAPE_ELLIPSOID = 4;
   private static final int SHAPE_CUBOID = 5;
   private static final int SHAPE_CAPSULE = 6;
   private static final int SHAPE_CONE = 7;
   private float baseHeightAdjustment = 0.0F;
   private float baseRadiusAdjustment = 0.0F;
   private float raycastDistance = 16.0F;

   public boolean displayImgui() {
      boolean changed = ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.generic.brush_shape"),
         this.shape,
         new String[]{
            AxiomI18n.get("axiom.tool.shape.sphere"),
            AxiomI18n.get("axiom.tool.shape.cube"),
            AxiomI18n.get("axiom.tool.shape.octahedron"),
            AxiomI18n.get("axiom.tool.shape.cylinder"),
            AxiomI18n.get("axiom.tool.generic.brush_shape.ellipsoid"),
            AxiomI18n.get("axiom.tool.generic.brush_shape.cuboid"),
            AxiomI18n.get("axiom.tool.generic.brush_shape.capsule"),
            AxiomI18n.get("axiom.tool.generic.brush_shape.cone")
         }
      );
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.generic.brush_radius"), this.radius, 0, 50);
      ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
      if (hasHeight(this.shape[0])) {
         changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.generic.brush_height"), this.height, 0, 50);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
      }

      if (this.shape[0] == 7) {
         changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.cone.rounding"), this.coneRounding, 0.0F, 1.0F);
         changed |= ImGuiHelper.combo("Direction", this.coneDirection, new String[]{"Both", "Up", "Down"});
      }

      this.dirty |= changed;
      return changed;
   }

   public void writeSettings(CompoundTag tag) {
      String shapeString = switch (this.shape[0]) {
         case 0 -> "Sphere";
         case 1 -> "Cube";
         case 2 -> "Octahedron";
         case 3 -> "Cylinder";
         case 4 -> "Ellipsoid";
         case 5 -> "Cuboid";
         case 6 -> "Capsule";
         case 7 -> "Bicone";
         default -> throw new FaultyImplementationError();
      };
      tag.putString("BrushShape", shapeString);
      tag.putInt("BrushRadius", this.radius[0]);
      if (hasHeight(this.shape[0])) {
         tag.putInt("BrushHeight", this.height[0]);
      }

      if (this.shape[0] == 7) {
         tag.putFloat("ConeRounding", this.coneRounding[0]);
         tag.putInt("ConeDirection", this.coneDirection[0]);
      }
   }

   public void loadSettings(CompoundTag tag) {
      String brushShape = VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "BrushShape", "Sphere");
      int[] var10000 = this.shape;

      var10000[0] = switch (brushShape) {
         case "Cube" -> 1;
         case "Octahedron" -> 2;
         case "Cylinder" -> 3;
         case "Ellipsoid" -> 4;
         case "Cuboid" -> 5;
         case "Capsule" -> 6;
         case "Bicone" -> 7;
         default -> 0;
      };
      this.radius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BrushRadius", 5);
      this.height[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BrushHeight", 5);
      if (this.shape[0] == 7) {
         this.coneRounding[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "ConeRounding", 0.0F);
         this.coneDirection[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "ConeDirection", 0);
      }

      this.dirty = true;
   }

   private static boolean hasHeight(int shapeType) {
      return switch (shapeType) {
         case 0, 1, 2 -> false;
         case 3, 4, 5, 6, 7 -> true;
         default -> throw new UnsupportedOperationException();
      };
   }

   public BrushShape createBrushShapeWithAdditional(int extra) {
      int extraSqrt3 = (int)Math.ceil(extra * Math.sqrt(3.0));

      return (BrushShape)(switch (this.shape[0]) {
         case 0 -> SphereBrushShape.create(this.radius[0] + extraSqrt3);
         case 1 -> CubeBrushShape.create(this.radius[0] + extra);
         case 2 -> OctahedronBrushShape.create(this.radius[0] + extraSqrt3);
         case 3 -> CylinderBrushShape.create(this.radius[0] + extraSqrt3, this.height[0] + extra);
         case 4 -> SphereBrushShape.create(this.radius[0] + extraSqrt3, this.height[0] + extraSqrt3, this.radius[0] + extraSqrt3);
         case 5 -> CubeBrushShape.create(this.radius[0] + extra, this.height[0] + extra, this.radius[0] + extra);
         case 6 -> CapsuleBrushShape.create(this.radius[0] + extraSqrt3, this.height[0] + extra);
         case 7 -> ConeBrushShape.create(this.radius[0] + extraSqrt3, this.height[0] + extraSqrt3, this.coneRounding[0], this.coneDirection[0]);
         default -> throw new FaultyImplementationError();
      });
   }

   public BrushShape getBrushShape() {
      if (this.brushShape == null || this.dirty) {
         this.brushShape = (BrushShape)(switch (this.shape[0]) {
            case 0 -> SphereBrushShape.create(this.radius[0]);
            case 1 -> CubeBrushShape.create(this.radius[0]);
            case 2 -> OctahedronBrushShape.create(this.radius[0]);
            case 3 -> CylinderBrushShape.create(this.radius[0], this.height[0]);
            case 4 -> SphereBrushShape.create(this.radius[0], this.height[0], this.radius[0]);
            case 5 -> CubeBrushShape.create(this.radius[0], this.height[0], this.radius[0]);
            case 6 -> CapsuleBrushShape.create(this.radius[0], this.height[0]);
            case 7 -> ConeBrushShape.create(this.radius[0], this.height[0], this.coneRounding[0], this.coneDirection[0]);
            default -> throw new FaultyImplementationError();
         });
         this.dirty = false;
      }

      return this.brushShape;
   }

   public void renderPreview(AxiomWorldRenderContext rc, Vec3 translation, int effects) {
      SharedBrushPreviewRenderer.render(rc, this.getBrushShape(), translation, effects);
   }

   public boolean initiateAdjustment() {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result == null) {
         return false;
      } else {
         this.raycastDistance = (float)result.getLocation().distanceTo(Minecraft.getInstance().player.getEyePosition());
         float m11 = EditorUI.lastProjectionMatrix.m11();
         this.baseRadiusAdjustment = this.radius[0] / this.raycastDistance / 2.0F * m11 * ImGui.getMainViewport().getSizeY();
         if (hasHeight(this.shape[0])) {
            int height = this.height[0];
            if (this.shape[0] == 6) {
               height += this.radius[0];
            }

            this.baseHeightAdjustment = height / this.raycastDistance / 2.0F * m11 * ImGui.getMainViewport().getSizeY();
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

         return true;
      }
   }

   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      float deltaX = mouseDelta.x + this.baseRadiusAdjustment;
      float deltaY = mouseDelta.y - this.baseHeightAdjustment;
      float actualLength = (float)Math.sqrt(deltaX * deltaX + deltaY * deltaY);
      float factor = actualLength / Math.max(Math.abs(deltaX), Math.abs(deltaY));
      float m11 = EditorUI.lastProjectionMatrix.m11();
      int newRadius;
      int newHeight;
      if (hasHeight(this.shape[0])) {
         float lengthForHeight = Math.abs(deltaY * factor);
         float lengthForRadius = Math.abs(deltaX * factor);
         newRadius = Math.round(lengthForRadius / ImGui.getMainViewport().getSizeY() * this.raycastDistance * 2.0F / m11);
         newHeight = Math.round(lengthForHeight / ImGui.getMainViewport().getSizeY() * this.raycastDistance * 2.0F / m11);
         if (newRadius < 0) {
            newRadius = 0;
         }

         if (newRadius > 200) {
            newRadius = 200;
         }

         if (newHeight < 0) {
            newHeight = 0;
         }

         if (newHeight > 200) {
            newHeight = 200;
         }

         if (this.shape[0] == 6 && newHeight < newRadius) {
            newHeight = newRadius;
         }

         int maxVolume = 2500;
         if (newRadius * newHeight > maxVolume) {
            float scaleDown = (float)Math.sqrt((float)maxVolume / (newRadius * newHeight));
            newRadius = Math.round(newRadius * scaleDown);
            newHeight = Math.round(newHeight * scaleDown);
         }

         if (this.shape[0] == 6) {
            newHeight = Math.max(0, newHeight - newRadius);
         }
      } else {
         newRadius = Math.round(actualLength / ImGui.getMainViewport().getSizeY() * this.raycastDistance * 2.0F / m11);
         newHeight = this.height[0];
         if (newRadius < 0) {
            newRadius = 0;
         }

         if (newRadius > 50) {
            newRadius = 50;
         }
      }

      if (this.radius[0] != newRadius) {
         this.radius[0] = newRadius;
         this.dirty = true;
      }

      if (this.height[0] != newHeight) {
         this.height[0] = newHeight;
         this.dirty = true;
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

      return mouseDelta;
   }
}
