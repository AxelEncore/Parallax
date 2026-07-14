package com.moulberry.axiom.editor.windows.operations;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.operations.AutoshadeOperation;
import com.moulberry.axiom.operations.AutoshadeShading;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.utils.BlockWithFloat;
import com.moulberry.axiom.utils.NbtHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImFloat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class AutoshadeWindow {
   private static boolean sunShade = true;
   private static final int[] sunShadePositionType = new int[]{0};
   private static final List<ImFloat> customSunPositions = new ArrayList<>();
   private static final float[] sunYawRadians = new float[]{(float) (Math.PI / 4)};
   private static final float[] sunPitchRadians = new float[]{(float) (Math.PI / 4)};
   private static boolean ambientShade = true;
   private static final float[] globalIllumination = new float[]{0.5F};
   private static final float[] dither = new float[]{0.1F};
   private static int paletteFlags = BlockColourMap.FLAG_SOLID
      | BlockColourMap.FLAG_OPAQUE
      | BlockColourMap.FLAG_FULL_CUBE
      | BlockColourMap.FLAG_NO_TILE_ENTITIES;
   private static final int[] paletteMode = new int[]{0};
   private static final List<BlockWithFloat> blockPercentages = new ArrayList<>();
   private static final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private static final PresetWidget presetWidget = new PresetWidget(AutoshadeWindow::loadSettings, AutoshadeWindow::writeSettings, "autoshade");

   public static void render() {
      if (EditorWindowType.AUTOSHADE.isOpen()) {
         if (EditorWindowType.AUTOSHADE.begin("###Autoshade", true)) {
            boolean changed = false;
            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.window.autoshade.sun_shading"));
            if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.sun"), sunShade)) {
               sunShade = !sunShade;
               changed = true;
            }

            if (sunShade) {
               changed |= ImGuiHelper.combo("From", sunShadePositionType, new String[]{"Player Position", "Custom Positions", "Sun Angle"});
               if (sunShadePositionType[0] == 1) {
                  if (customSunPositions.isEmpty()) {
                     Entity camera = Minecraft.getInstance().cameraEntity;
                     if (camera != null) {
                        customSunPositions.add(new ImFloat((float)Math.round(camera.getX() * 10.0) / 10.0F));
                        customSunPositions.add(new ImFloat((float)Math.round(camera.getY() * 10.0) / 10.0F));
                        customSunPositions.add(new ImFloat((float)Math.round(camera.getZ() * 10.0) / 10.0F));
                        customSunPositions.add(new ImFloat(1.0F));
                     }
                  }

                  if (ImGui.beginTable("##CustomSunPositionsTable", 5, 32768)) {
                     ImGui.tableNextColumn();
                     ImGui.text("X");
                     ImGui.tableNextColumn();
                     ImGui.text("Y");
                     ImGui.tableNextColumn();
                     ImGui.text("Z");
                     ImGui.tableNextColumn();
                     ImGui.text(AxiomI18n.get("axiom.hardcoded.intensity"));
                     ImGui.tableNextColumn();
                     int removeIndex = -1;

                     for (int i = 0; i < customSunPositions.size(); i += 4) {
                        ImGui.tableNextColumn();
                        ImGui.setNextItemWidth(-1.0F);
                        ImGui.inputFloat("##CustomPosition" + i, customSunPositions.get(i), 0.0F, 0.0F, "%.2f", 0);
                        ImGui.tableNextColumn();
                        ImGui.setNextItemWidth(-1.0F);
                        ImGui.inputFloat("##CustomPosition" + (i + 1), customSunPositions.get(i + 1), 0.0F, 0.0F, "%.2f", 0);
                        ImGui.tableNextColumn();
                        ImGui.setNextItemWidth(-1.0F);
                        ImGui.inputFloat("##CustomPosition" + (i + 2), customSunPositions.get(i + 2), 0.0F, 0.0F, "%.2f", 0);
                        ImGui.tableNextColumn();
                        ImGui.setNextItemWidth(-1.0F);
                        ImGui.inputFloat("##CustomPosition" + (i + 3), customSunPositions.get(i + 3), 0.0F, 0.0F, "%.2f", 0);
                        ImGui.tableNextColumn();
                        if (ImGui.button("X##Remove" + i)) {
                           removeIndex = i;
                        }
                     }

                     if (removeIndex >= 0) {
                        customSunPositions.remove(removeIndex);
                        customSunPositions.remove(removeIndex);
                        customSunPositions.remove(removeIndex);
                        customSunPositions.remove(removeIndex);
                     }

                     ImGui.endTable();
                  }

                  if (ImGui.button(AxiomI18n.get("axiom.hardcoded.add_position"))) {
                     Entity camera = Minecraft.getInstance().cameraEntity;
                     if (camera != null) {
                        customSunPositions.add(new ImFloat((float)Math.round(camera.getX() * 10.0) / 10.0F));
                        customSunPositions.add(new ImFloat((float)Math.round(camera.getY() * 10.0) / 10.0F));
                        customSunPositions.add(new ImFloat((float)Math.round(camera.getZ() * 10.0) / 10.0F));
                        customSunPositions.add(new ImFloat(1.0F));
                     }
                  }
               } else if (sunShadePositionType[0] == 2) {
                  changed |= ImGui.sliderAngle(AxiomI18n.get("axiom.window.autoshade.sun_yaw"), sunYawRadians);
                  changed |= ImGui.sliderAngle(AxiomI18n.get("axiom.window.autoshade.sun_pitch"), sunPitchRadians);
               }
            }

            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.window.autoshade.ambient_occlusion_shading"));
            if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.ambient_occlusion"), ambientShade)) {
               ambientShade = !ambientShade;
               changed = true;
            }

            if (paletteMode[0] == 0) {
               ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.window.autoshade.global_illumination"));
               changed |= ImGui.sliderFloat("##GlobalIllumination", globalIllumination, 0.0F, 1.0F);
            }

            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.window.autoshade.dither"));
            changed |= ImGui.sliderFloat("##Dither", dither, 0.0F, 1.0F);
            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.window.autoshade.palette_options"));
            ImGuiHelper.combo(
               AxiomI18n.get("axiom.window.autoshade.palette_type"),
               paletteMode,
               new String[]{AxiomI18n.get("axiom.window.autoshade.palette_type.automatic"), AxiomI18n.get("axiom.window.autoshade.palette_type.custom")}
            );
            if (paletteMode[0] == 0) {
               if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.palette_solid"), (paletteFlags & BlockColourMap.FLAG_SOLID) != 0)) {
                  paletteFlags = paletteFlags ^ BlockColourMap.FLAG_SOLID;
                  changed = true;
               }

               if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.palette_opaque"), (paletteFlags & BlockColourMap.FLAG_OPAQUE) != 0)) {
                  paletteFlags = paletteFlags ^ BlockColourMap.FLAG_OPAQUE;
                  changed = true;
               }

               if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.palette_full_cube"), (paletteFlags & BlockColourMap.FLAG_FULL_CUBE) != 0)) {
                  paletteFlags = paletteFlags ^ BlockColourMap.FLAG_FULL_CUBE;
                  changed = true;
               }

               if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.palette_same_texture"), (paletteFlags & BlockColourMap.FLAG_SAME_TEXTURE) != 0)) {
                  paletteFlags = paletteFlags ^ BlockColourMap.FLAG_SAME_TEXTURE;
                  changed = true;
               }

               if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.palette_no_ores"), (paletteFlags & BlockColourMap.FLAG_NO_ORES) != 0)) {
                  paletteFlags = paletteFlags ^ BlockColourMap.FLAG_NO_ORES;
                  changed = true;
               }

               if (ImGui.checkbox(
                  AxiomI18n.get("axiom.window.autoshade.palette_no_glazed_terracotta"), (paletteFlags & BlockColourMap.FLAG_NO_GLAZED_TERRACOTTA) != 0
               )) {
                  paletteFlags = paletteFlags ^ BlockColourMap.FLAG_NO_GLAZED_TERRACOTTA;
                  changed = true;
               }

               if (ImGui.checkbox(AxiomI18n.get("axiom.window.autoshade.palette_no_tile_entities"), (paletteFlags & BlockColourMap.FLAG_NO_TILE_ENTITIES) != 0)
                  )
                {
                  paletteFlags = paletteFlags ^ BlockColourMap.FLAG_NO_TILE_ENTITIES;
                  changed = true;
               }
            } else {
               changed |= BlockWithFloat.renderList(blockPercentages, selectBlockWidget, BlockWithFloat.ExtraRenderType.PERCENTAGE, 1, true);
            }

            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
            presetWidget.displayImgui(changed);
            ImGui.separator();
            boolean disable = Selection.getSelectionBuffer().isEmpty() || !sunShade && !ambientShade;
            if (disable) {
               ImGui.beginDisabled();
            }

            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.autoshade.do_autoshade"))) {
               List<Vec3> vectors = new ArrayList<>();
               List<AutoshadeShading.PositionWithIntensity> positions = new ArrayList<>();
               if (sunShadePositionType[0] == 0) {
                  Entity camera = Minecraft.getInstance().cameraEntity;
                  if (camera != null) {
                     positions.add(new AutoshadeShading.PositionWithIntensity((float)camera.getX(), (float)camera.getY(), (float)camera.getZ(), 1.0F));
                  }
               } else if (sunShadePositionType[0] == 1) {
                  for (int ix = 0; ix < customSunPositions.size(); ix += 4) {
                     positions.add(
                        new AutoshadeShading.PositionWithIntensity(
                           customSunPositions.get(ix).get(),
                           customSunPositions.get(ix + 1).get(),
                           customSunPositions.get(ix + 2).get(),
                           customSunPositions.get(ix + 3).get()
                        )
                     );
                  }
               } else if (sunShadePositionType[0] == 2) {
                  float yawRadians = -sunYawRadians[0];
                  float pitchRadians = -sunPitchRadians[0];
                  float cosYaw = Mth.cos(yawRadians);
                  float sinYaw = Mth.sin(yawRadians);
                  float cosPitch = Mth.cos(pitchRadians);
                  float sinPitch = Mth.sin(pitchRadians);
                  vectors.add(new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch));
               }

               if (!vectors.isEmpty() || !positions.isEmpty()) {
                  List<BlockWithFloat> customPalette = paletteMode[0] == 0 ? null : blockPercentages;
                  float globalIlluminationFloat = paletteMode[0] == 0 ? globalIllumination[0] : 0.0F;
                  AutoshadeShading shading = new AutoshadeShading(vectors, positions);
                  AutoshadeOperation.autoshade(sunShade, ambientShade, shading, globalIlluminationFloat, dither[0], customPalette, paletteFlags);
               }
            }

            if (disable) {
               ImGui.endDisabled();
            }
         }

         EditorWindowType.AUTOSHADE.end();
      }
   }

   public static void renderWorld(AxiomWorldRenderContext rc) {
      if (EditorWindowType.AUTOSHADE.isOpen() && EditorUI.isActive() && !customSunPositions.isEmpty()) {
         PoseStack matrices = rc.poseStack();
         matrices.pushPose();
         matrices.translate(-rc.x(), -rc.y(), -rc.z());
         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

         for (int i = 0; i < customSunPositions.size(); i += 4) {
            float x = customSunPositions.get(i).get();
            float y = customSunPositions.get(i + 1).get();
            float z = customSunPositions.get(i + 2).get();
            matrices.pushPose();
            matrices.translate(x - 0.1, y - 0.1, z - 0.1);
            Shapes.shadedBoxTriangles(bufferBuilder, matrices.last().pose(), 0.2F, 0.2F, 0.2F, -2130706688);
            matrices.translate(-0.1, -0.1, -0.1);
            Shapes.shadedBoxTriangles(bufferBuilder, matrices.last().pose(), 0.4F, 0.4F, 0.4F, -2130706688);
            matrices.translate(-0.1, -0.1, -0.1);
            Shapes.shadedBoxTriangles(bufferBuilder, matrices.last().pose(), 0.6F, 0.6F, 0.6F, 1090518784);
            matrices.popPose();
         }

         AxiomRenderPipelines.POSITION_COLOR.render(Minecraft.getInstance().getMainRenderTarget(), provider.build());
         matrices.popPose();
      }
   }

   private static void loadSettings(CompoundTag tag) {
      sunShade = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "DoSunShading", true);
      sunYawRadians[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SunYawRadians", (float) (Math.PI / 4));
      sunPitchRadians[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SunPitchRadians", (float) (Math.PI / 4));
      ambientShade = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "DoAmbientShading", true);
      globalIllumination[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "GlobalIllumination", 0.5F);
      dither[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Dither", 0.5F);
      paletteFlags = VersionUtilsNbt.helperCompoundTagGetIntOr(
         tag, "PaletteFlags", BlockColourMap.FLAG_SOLID | BlockColourMap.FLAG_OPAQUE | BlockColourMap.FLAG_FULL_CUBE | BlockColourMap.FLAG_NO_TILE_ENTITIES
      );
      paletteMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "PaletteMode", 0);
      blockPercentages.clear();

      for (Tag blockPercentageTag : NbtHelper.getList(tag, "BlockPercentages", 10)) {
         CompoundTag blockPercentage = (CompoundTag)blockPercentageTag;
         String block = VersionUtilsNbt.helperCompoundTagGetString(blockPercentage, "Block").get();
         float percentage = VersionUtilsNbt.helperCompoundTagGetFloat(blockPercentage, "Percentage").get();
         blockPercentages.add(
            new BlockWithFloat(
               Objects.requireNonNullElse(ServerCustomBlocks.deserialize(block), (CustomBlockState)Blocks.STONE.defaultBlockState()),
               new float[]{percentage},
               null
            )
         );
      }

      if (blockPercentages.isEmpty()) {
         BlockWithFloat.addNew(blockPercentages, (CustomBlockState)Blocks.MUD.defaultBlockState());
         BlockWithFloat.addNew(blockPercentages, (CustomBlockState)Blocks.DEEPSLATE.defaultBlockState());
         BlockWithFloat.addNew(blockPercentages, (CustomBlockState)Blocks.TUFF.defaultBlockState());
         BlockWithFloat.addNew(blockPercentages, (CustomBlockState)Blocks.ANDESITE.defaultBlockState());
      }
   }

   private static void writeSettings(CompoundTag tag) {
      tag.putBoolean("DoSunShading", sunShade);
      tag.putFloat("SunYawRadians", sunYawRadians[0]);
      tag.putFloat("SunPitchRadians", sunPitchRadians[0]);
      tag.putBoolean("DoAmbientShading", ambientShade);
      tag.putFloat("GlobalIllumination", globalIllumination[0]);
      tag.putFloat("Dither", dither[0]);
      tag.putInt("PaletteFlags", paletteFlags);
      tag.putInt("PaletteMode", paletteMode[0]);
      ListTag blockPercentagesList = new ListTag();

      for (BlockWithFloat blockWithFloat : blockPercentages) {
         CompoundTag blockWithFloatTag = new CompoundTag();
         blockWithFloatTag.putString("Block", ServerCustomBlocks.serialize(blockWithFloat.blockState()));
         blockWithFloatTag.putFloat("Percentage", blockWithFloat.percentage()[0]);
         blockPercentagesList.add(blockWithFloatTag);
      }

      tag.put("BlockPercentages", blockPercentagesList);
   }

   static {
      blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.MUD.defaultBlockState(), new float[]{25.0F}, null));
      blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.DEEPSLATE.defaultBlockState(), new float[]{25.0F}, null));
      blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.TUFF.defaultBlockState(), new float[]{25.0F}, null));
      blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.ANDESITE.defaultBlockState(), new float[]{25.0F}, null));
   }
}
