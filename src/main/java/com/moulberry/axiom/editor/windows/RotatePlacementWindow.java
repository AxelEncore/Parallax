package com.moulberry.axiom.editor.windows;

import com.google.common.math.DoubleMath;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.scaling.RotSprite;
import com.moulberry.axiom.scaling.Scale2x;
import com.moulberry.axiom.scaling.Scale3x;
import com.moulberry.axiom.utils.BlockHelper;
import imgui.moulberry92.ImGui;
import java.math.RoundingMode;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class RotatePlacementWindow {
   private static final int[] algorithm = new int[]{1};
   private static final float[] rotationAmountX = new float[]{0.0F};
   private static final float[] rotationAmountY = new float[]{0.0F};
   private static final float[] rotationAmountZ = new float[]{0.0F};
   private static final float[] scaleAmountX = new float[]{1.0F};
   private static final float[] scaleAmountY = new float[]{1.0F};
   private static final float[] scaleAmountZ = new float[]{1.0F};

   public static void reset() {
      rotationAmountX[0] = 0.0F;
      rotationAmountY[0] = 0.0F;
      rotationAmountZ[0] = 0.0F;
      scaleAmountX[0] = 1.0F;
      scaleAmountY[0] = 1.0F;
      scaleAmountZ[0] = 1.0F;
   }

   public static void render() {
      if (EditorWindowType.ROTATE_PLACEMENT.isOpen()) {
         if (EditorWindowType.ROTATE_PLACEMENT.begin("###RotatePlacement", true)) {
            ImGuiHelper.combo(
               AxiomI18n.get("axiom.window.rotate_placement.algorithm"),
               RotatePlacementWindow.algorithm,
               new String[]{
                  AxiomI18n.get("axiom.window.rotate_placement.nearest_neighbor"),
                  AxiomI18n.get("axiom.window.rotate_placement.rotsprite"),
                  AxiomI18n.get("axiom.window.rotate_placement.scale2x"),
                  AxiomI18n.get("axiom.window.rotate_placement.scale3x")
               }
            );
            int algorithm = RotatePlacementWindow.algorithm[0];
            float rotationDegreesX = 0.0F;
            float rotationDegreesY = 0.0F;
            float rotationDegreesZ = 0.0F;
            float scaleX = 1.0F;
            float scaleY = 1.0F;
            float scaleZ = 1.0F;
            if (algorithm == 0 || algorithm == 1) {
               String rotation = AxiomI18n.get("axiom.window.rotate_placement.rotation");
               String scale = AxiomI18n.get("axiom.window.rotate_placement.scale");
               ImGui.sliderAngle(rotation + " X", rotationAmountX, -179.0F, 180.0F, "%.1f");
               ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
               ImGui.sliderAngle(rotation + " Y", rotationAmountY, -179.0F, 180.0F, "%.1f");
               ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
               ImGui.sliderAngle(rotation + " Z", rotationAmountZ, -179.0F, 180.0F, "%.1f");
               ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
               rotationDegreesX = (float)Mth.wrapDegrees(Math.toDegrees(rotationAmountX[0]));
               rotationDegreesY = (float)Mth.wrapDegrees(Math.toDegrees(rotationAmountY[0]));
               rotationDegreesZ = (float)Mth.wrapDegrees(Math.toDegrees(rotationAmountZ[0]));
               if (algorithm == 0) {
                  ImGui.sliderFloat(scale + " X", scaleAmountX, 0.1F, 10.0F, "%.2f", 32);
                  ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
                  ImGui.sliderFloat(scale + " Y", scaleAmountY, 0.1F, 10.0F, "%.2f", 32);
                  ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
                  ImGui.sliderFloat(scale + " Z", scaleAmountZ, 0.1F, 10.0F, "%.2f", 32);
                  ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
                  scaleX = Math.round(scaleAmountX[0] * 100.0F) / 100.0F;
                  scaleY = Math.round(scaleAmountY[0] * 100.0F) / 100.0F;
                  scaleZ = Math.round(scaleAmountZ[0] * 100.0F) / 100.0F;
               }
            } else if (algorithm == 2) {
               scaleZ = 2.0F;
               scaleY = 2.0F;
               scaleX = 2.0F;
            } else if (algorithm == 3) {
               scaleZ = 3.0F;
               scaleY = 3.0F;
               scaleX = 3.0F;
            } else if (algorithm == 4) {
               scaleZ = 4.0F;
               scaleY = 4.0F;
               scaleX = 4.0F;
            }

            boolean canRotate = rotationDegreesX != 0.0F || rotationDegreesY != 0.0F || rotationDegreesZ != 0.0F;
            boolean canScale = scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F;
            String buttonText;
            if (canRotate == canScale) {
               if (algorithm == 0) {
                  buttonText = AxiomI18n.get("axiom.window.rotate_placement.do_rotate_and_scale");
               } else if (algorithm == 1) {
                  buttonText = AxiomI18n.get("axiom.window.rotate_placement.do_rotate");
               } else {
                  buttonText = AxiomI18n.get("axiom.window.rotate_placement.do_scale");
               }
            } else if (canRotate) {
               buttonText = AxiomI18n.get("axiom.window.rotate_placement.do_rotate");
            } else {
               buttonText = AxiomI18n.get("axiom.window.rotate_placement.do_scale");
            }

            ChunkedBlockRegion placement = Placement.INSTANCE.getBlockRegion();
            boolean disabled = !canRotate && !canScale || placement == null;
            if (disabled) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(buttonText)) {
               if (algorithm != 0) {
                  if (algorithm == 1) {
                     if (rotationDegreesX % 90.0F == 0.0F && rotationDegreesY % 90.0F == 0.0F && rotationDegreesZ % 90.0F == 0.0F) {
                        placement = placement.rotate(Axis.Y, -Math.round(rotationDegreesY / 90.0F));
                        placement = placement.rotate(Axis.X, -Math.round(rotationDegreesX / 90.0F));
                        placement = placement.rotate(Axis.Z, -Math.round(rotationDegreesZ / 90.0F));
                        Placement.INSTANCE.replacePlacement(placement);
                     } else {
                        ChunkedBlockRegion rotated = RotSprite.rotate(
                           placement, (float)Math.toRadians(rotationDegreesX), (float)Math.toRadians(rotationDegreesY), (float)Math.toRadians(rotationDegreesZ)
                        );
                        Placement.INSTANCE.replacePlacement(rotated);
                     }
                  } else if (algorithm == 2) {
                     ChunkedBlockRegion scaled = Scale2x.scale2x(placement, true);
                     Placement.INSTANCE.replacePlacement(scaled);
                  } else if (algorithm == 3) {
                     ChunkedBlockRegion scaled = Scale3x.scale3x(placement, true);
                     Placement.INSTANCE.replacePlacement(scaled);
                  }
               } else if (!canScale && rotationDegreesX % 90.0F == 0.0F && rotationDegreesY % 90.0F == 0.0F && rotationDegreesZ % 90.0F == 0.0F) {
                  placement = placement.rotate(Axis.Y, -Math.round(rotationDegreesY / 90.0F));
                  placement = placement.rotate(Axis.X, -Math.round(rotationDegreesX / 90.0F));
                  placement = placement.rotate(Axis.Z, -Math.round(rotationDegreesZ / 90.0F));
                  Placement.INSTANCE.replacePlacement(placement);
               } else {
                  Matrix4f matrix4f = new Matrix4f();
                  matrix4f.scale(scaleAmountX[0], scaleAmountY[0], scaleAmountZ[0]);
                  matrix4f.rotateYXZ((float)Math.toRadians(rotationDegreesY), (float)Math.toRadians(rotationDegreesX), (float)Math.toRadians(rotationDegreesZ));
                  int minX = placement.min().getX();
                  int minY = placement.min().getY();
                  int minZ = placement.min().getZ();
                  int maxX = placement.max().getX();
                  int maxY = placement.max().getY();
                  int maxZ = placement.max().getZ();
                  int newMinX = Integer.MAX_VALUE;
                  int newMinY = Integer.MAX_VALUE;
                  int newMinZ = Integer.MAX_VALUE;
                  int newMaxX = Integer.MIN_VALUE;
                  int newMaxY = Integer.MIN_VALUE;
                  int newMaxZ = Integer.MIN_VALUE;
                  Vector4f vector4f = new Vector4f();

                  for (int x = minX; x <= maxX; x = maxX) {
                     for (int y = minY; y <= maxY; y = maxY) {
                        for (int z = minZ; z <= maxZ; z = maxZ) {
                           vector4f.set(x, y, z, 1.0F);
                           matrix4f.transform(vector4f);
                           newMinX = Math.min(newMinX, (int)Math.floor(vector4f.x));
                           newMinY = Math.min(newMinY, (int)Math.floor(vector4f.y));
                           newMinZ = Math.min(newMinZ, (int)Math.floor(vector4f.z));
                           newMaxX = Math.max(newMaxX, (int)Math.ceil(vector4f.x));
                           newMaxY = Math.max(newMaxY, (int)Math.ceil(vector4f.y));
                           newMaxZ = Math.max(newMaxZ, (int)Math.ceil(vector4f.z));
                           if (z == maxZ) {
                              break;
                           }
                        }

                        if (y == maxY) {
                           break;
                        }
                     }

                     if (x == maxX) {
                        break;
                     }
                  }

                  Matrix4f inverted = matrix4f.invert(new Matrix4f());
                  Rotation rotationX = BlockHelper.rotationFromDegrees(-rotationDegreesX);
                  Rotation rotationY = BlockHelper.rotationFromDegrees(-rotationDegreesY);
                  Rotation rotationZ = BlockHelper.rotationFromDegrees(-rotationDegreesZ);
                  ChunkedBlockRegion out = new ChunkedBlockRegion();

                  for (int x = newMinX; x <= newMaxX; x++) {
                     for (int y = newMinY; y <= newMaxY; y++) {
                        for (int zx = newMinZ; zx <= newMaxZ; zx++) {
                           vector4f.set(x, y, zx, 1.0F);
                           inverted.transform(vector4f);
                           int sampleX = DoubleMath.roundToInt(vector4f.x, RoundingMode.HALF_DOWN);
                           int sampleY = DoubleMath.roundToInt(vector4f.y, RoundingMode.HALF_DOWN);
                           int sampleZ = DoubleMath.roundToInt(vector4f.z, RoundingMode.HALF_DOWN);
                           BlockState blockState = placement.getBlockStateOrAir(sampleX, sampleY, sampleZ);
                           if (!blockState.isAir()) {
                              blockState = BlockHelper.rotateX(blockState, rotationX);
                              blockState = BlockHelper.rotateY(blockState, rotationY);
                              blockState = BlockHelper.rotateZ(blockState, rotationZ);
                              out.addBlock(x, y, zx, blockState);
                           }
                        }
                     }
                  }

                  Placement.INSTANCE.replacePlacement(out);
               }
            }

            if (rotationDegreesX % 90.0F != 0.0F || rotationDegreesY % 90.0F != 0.0F || rotationDegreesZ % 90.0F != 0.0F) {
               ImGui.sameLine();
               ImGui.text("⚠ " + AxiomI18n.get("axiom.window.rotate_placement.lossy_rotation_warning"));
            } else if (scaleX % 1.0F != 0.0F || scaleY % 1.0F != 0.0F || scaleZ % 1.0F != 0.0F) {
               ImGui.sameLine();
               ImGui.text("⚠ " + AxiomI18n.get("axiom.window.rotate_placement.lossy_scaling_warning"));
            }

            if (disabled) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.ROTATE_PLACEMENT.end();
      }
   }
}
