package com.moulberry.axiom.tools.biome_painter;

import com.moulberry.axiom.BiomeDataManager;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.IntWrapper;
import com.moulberry.axiom.world_modification.BiomeBuffer;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_modification.HistoryEntry;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class BiomePainterTool implements Tool {
   private final ChunkedBooleanRegion chunkedBooleanRegion = new ChunkedBooleanRegion();
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private boolean biomeOverlay = true;
   private boolean fillVertically = false;
   private static final int[] selectedBiome = new int[]{0};
   private static final List<BiomePainterTool.BiomeWithIndex> filteredBiomes = new ArrayList<>();
   private static String biomeFilter = "";

   @Override
   public void reset() {
      this.usingTool = false;
      this.chunkedBooleanRegion.clear();
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.reset();
            this.usingTool = true;
            this.pathProvider = new AsyncToolPathProvider(this.createToolPather(this.brushWidget.getBrushShape()));
            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
            if (this.usingTool) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (!this.usingTool) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            Selection.render(rc, 7);
            return;
         }

         Selection.render(rc, 4);
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            this.reset();
            return;
         }

         BiomeDataManager biomeDataManager = BiomeDataManager.get();
         if (biomeDataManager == null) {
            this.reset();
            return;
         }

         List<BiomeDataManager.BiomeDataEntry> biomes = biomeDataManager.biomes();
         if (selectedBiome[0] < 0 || selectedBiome[0] >= biomes.size()) {
            this.reset();
            return;
         }

         this.pathProvider.finish();
         ResourceKey<Biome> biome = biomes.get(selectedBiome[0]).key();
         BiomeBuffer setOperation = new BiomeBuffer();
         BiomeBuffer previousBiomesForUndo = new BiomeBuffer();
         IntWrapper count = new IntWrapper();
         if (this.fillVertically) {
            PositionSet alreadyChecked = new PositionSet();
            this.chunkedBooleanRegion.forEach((x, y, z) -> {
               if (alreadyChecked.add(x, 0, z)) {
                  LevelChunk chunk = (LevelChunk)level.getChunk(x >> 2, z >> 2, ChunkStatus.FULL, false);
                  if (chunk == null) {
                     return;
                  }

                  int sectionCount = chunk.getSectionsCount();
                  int minSection = chunk.getMinSection();

                  for (int i = 0; i < sectionCount; i++) {
                     LevelChunkSection section = chunk.getSection(i);

                     for (int ly = 0; ly < 4; ly++) {
                        if (section.getNoiseBiome(x & 3, ly, z & 3) instanceof Reference<Biome> reference) {
                           ResourceKey<Biome> oldBiomeKey = reference.key();
                           if (oldBiomeKey != biome) {
                              int by = (i + minSection << 2) + ly;
                              setOperation.set(x, by, z, biome);
                              previousBiomesForUndo.set(x, by, z, oldBiomeKey);
                              count.value++;
                           }
                        }
                     }
                  }
               }
            });
         } else {
            this.chunkedBooleanRegion.forEach((x, y, z) -> {
               LevelChunk chunk = (LevelChunk)level.getChunk(x >> 2, z >> 2, ChunkStatus.FULL, false);
               if (chunk != null) {
                  int index = level.getSectionIndexFromSectionY(y >> 2);
                  if (index >= 0 && index < level.getSectionsCount()) {
                     LevelChunkSection section = chunk.getSection(index);
                     if (section.getNoiseBiome(x & 3, y & 3, z & 3) instanceof Reference<Biome> reference) {
                        ResourceKey<Biome> oldBiomeKey = reference.key();
                        if (oldBiomeKey != biome) {
                           setOperation.set(x, y, z, biome);
                           previousBiomesForUndo.set(x, y, z, oldBiomeKey);
                           count.value++;
                        }
                     }
                  }
               }
            });
         }

         BlockPos center = this.chunkedBooleanRegion.getCenter();
         center = new BlockPos(center.getX() * 4 + 2, center.getY() * 4 + 2, center.getZ() * 4 + 2);
         String countString = NumberFormat.getInstance().format(count.value * 4L * 4L * 4L);
         String historyDescription = AxiomI18n.get("axiom.history_description.biome_painted", countString);
         Dispatcher.push(new HistoryEntry<>(setOperation, previousBiomesForUndo, center, historyDescription, 0));
         this.reset();
      } else {
         ClientLevel levelx = Minecraft.getInstance().level;
         if (levelx == null) {
            return;
         }

         Selection.render(rc, 4);
         this.pathProvider.update();
         this.chunkedBooleanRegion.setScale(4.0F, 4.0F, 4.0F);
         this.chunkedBooleanRegion.render(rc, Vec3.ZERO, 1);
      }
   }

   private AsyncToolPather createToolPather(BrushShape brushShape) {
      ClientLevel level = Minecraft.getInstance().level;
      MaskElement maskElement = MaskManager.getDestMask();
      MaskContext maskContext = new MaskContext(level);
      return new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
         if (maskElement.test(maskContext.reset(), x, y, z)) {
            this.chunkedBooleanRegion.add(x >> 2, y >> 2, z >> 2);
         }
      });
   }

   public boolean shouldRenderBiomeOverlay() {
      return this.biomeOverlay;
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.biome_painter"));
      renderBiomeDropdown();
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.biome_painter.fill_vertically"), this.fillVertically)) {
         this.fillVertically = !this.fillVertically;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.biome_painter.visualize_biomes"), this.biomeOverlay)) {
         this.biomeOverlay = !this.biomeOverlay;
      }
   }

   private static void renderBiomeDropdown() {
      BiomeDataManager biomeDataManager = BiomeDataManager.get();
      if (biomeDataManager == null) {
         ImGui.text(AxiomI18n.get("axiom.widget.error_biome_registry"));
      } else {
         if (biomeDataManager.justRefreshed()) {
            filteredBiomes.clear();
            biomeFilter = "";
         }

         List<BiomeDataManager.BiomeDataEntry> biomes = biomeDataManager.biomes();
         if (biomes.isEmpty()) {
            ImGui.text(AxiomI18n.get("axiom.widget.error_biome_registry"));
         } else {
            if (selectedBiome[0] >= biomes.size()) {
               selectedBiome[0] = 0;
            }

            String currentBiome = biomes.get(selectedBiome[0]).prettyName();
            if (ImGui.beginCombo("##Biomes", biomeFilter.isEmpty() ? currentBiome : biomeFilter)) {
               ImGui.setNextFrameWantCaptureKeyboard(true);
               String biomeFilterOld = biomeFilter;
               biomeFilter = ImGuiHelper.modifyFromInput(biomeFilter);
               boolean enterPressed = ImGui.isKeyPressed(525);
               if (biomeFilter.isEmpty()) {
                  for (int i = 0; i < biomes.size(); i++) {
                     ImGui.pushID(i);
                     boolean selected = i == selectedBiome[0];
                     if (ImGui.selectable(biomes.get(i).prettyName(), selected) && !selected) {
                        selectedBiome[0] = i;
                     }

                     if (enterPressed && ImGui.isItemFocused()) {
                        selectedBiome[0] = i;
                        ImGui.popID();
                        ImGui.closeCurrentPopup();
                        ImGui.endCombo();
                        return;
                     }

                     if (selected) {
                        ImGui.setItemDefaultFocus();
                     }

                     ImGui.popID();
                  }

                  if (enterPressed) {
                     selectedBiome[0] = 0;
                     ImGui.closeCurrentPopup();
                  }
               } else {
                  if (!biomeFilter.equals(biomeFilterOld)) {
                     String filterLower = biomeFilter.toLowerCase(Locale.ROOT);
                     filteredBiomes.clear();

                     for (int i = 0; i < biomes.size(); i++) {
                        String biome = biomes.get(i).prettyName();
                        if (biome.toLowerCase(Locale.ROOT).contains(filterLower)) {
                           filteredBiomes.add(new BiomePainterTool.BiomeWithIndex(biome, i));
                        }
                     }
                  }

                  for (int ix = 0; ix < filteredBiomes.size(); ix++) {
                     ImGui.pushID(ix);
                     BiomePainterTool.BiomeWithIndex biomeWithIndex = filteredBiomes.get(ix);
                     boolean selectedx = biomeWithIndex.index == selectedBiome[0];
                     if (ImGui.selectable(biomeWithIndex.prettyName, selectedx) && !selectedx) {
                        selectedBiome[0] = biomeWithIndex.index;
                     }

                     if (enterPressed && ImGui.isItemFocused()) {
                        selectedBiome[0] = biomeWithIndex.index;
                        ImGui.popID();
                        ImGui.closeCurrentPopup();
                        ImGui.endCombo();
                        return;
                     }

                     if (selectedx) {
                        ImGui.setItemDefaultFocus();
                     }

                     ImGui.popID();
                  }

                  if (enterPressed && filteredBiomes.size() > 0) {
                     selectedBiome[0] = filteredBiomes.get(0).index;
                     ImGui.closeCurrentPopup();
                  }
               }

               ImGui.endCombo();
            } else {
               biomeFilter = "";
            }
         }
      }
   }

   @Override
   public String listenForEsc() {
      return !this.usingTool ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public boolean initiateAdjustment() {
      return this.brushWidget.initiateAdjustment();
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      return this.brushWidget.renderAdjustment(mouseX, mouseY, mouseDelta);
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.biome_painter");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putBoolean("FillVertically", this.fillVertically);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.fillVertically = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "FillVertically", false);
   }

   @Override
   public char iconChar() {
      return '\ue914';
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public String keybindId() {
      return "biome_painter";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_BIOMEPAINTER, AxiomPermission.BUILD_SECTION);
   }

   private record BiomeWithIndex(String prettyName, int index) {
   }
}
