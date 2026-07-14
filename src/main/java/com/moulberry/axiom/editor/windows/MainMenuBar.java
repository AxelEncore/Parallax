package com.moulberry.axiom.editor.windows;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ServerConfig;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.clipboard.ModifySelection;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.clipboard.SelectionSerialization;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.downgrade.BlockVersionCompatibility;
import com.moulberry.axiom.downgrade.DowngradeVersion;
import com.moulberry.axiom.downgrade.DowngradeVersionList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.ImageReferenceWindows;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.schematic.SchematicLoader;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.editor.views.ViewManager;
import com.moulberry.axiom.editor.windows.operations.FillBlocksWindow;
import com.moulberry.axiom.editor.windows.save_world.ExportSchematicWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.operations.DrainOperation;
import com.moulberry.axiom.operations.FillNearestOperation;
import com.moulberry.axiom.operations.GenerateColourFieldOperation;
import com.moulberry.axiom.operations.HollowOperation;
import com.moulberry.axiom.operations.SimulateGravityOperation;
import com.moulberry.axiom.operations.UpdateBlocksOperation;
import com.moulberry.axiom.operations.WaterlogOperation;
import com.moulberry.axiom.operations.smooth.SmoothSnow;
import com.moulberry.axiom.packets.AxiomServerboundFixArea;
import com.moulberry.axiom.rasterization.HullRasterization;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.AsyncFileDialogs;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.vanilla_structure_file.VanillaStructureHelper;
import com.moulberry.axiom.world_modification.Dispatcher;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImBoolean;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class MainMenuBar {
   public static void render() {
      if (ImGui.beginMainMenuBar()) {
         ServerConfig config = Axiom.getInstance().serverConfig;
         boolean selectionActive = !Selection.getSelectionBuffer().isEmpty();
         boolean clipboardEmpty = Clipboard.INSTANCE.getClipboard() == null;
         boolean clipboardNonEmpty = !clipboardEmpty;
         if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.file"))) {
            if (!AxiomClient.hasPermission(AxiomPermission.CAN_IMPORT_BLOCKS)) {
               ImGuiHelper.disabledMenuItem(AxiomI18n.get("axiom.editorui.mainmenu.file.import_schematic"), "Server has disallowed importing");
            } else if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.file.import_schematic"))) {
               Path schematicDir = SchematicLoader.getDefaultSchematicDir();
               String schematicDirStr = schematicDir.toString();
               String separator = schematicDir.getFileSystem().getSeparator();
               if (!schematicDirStr.endsWith(separator)) {
                  schematicDirStr = schematicDirStr + separator;
               }

               CompletableFuture<String> future = AsyncFileDialogs.openFileDialog(schematicDirStr, "Schematic Files", "schem", "schematic", "litematic");
               future.thenAccept(path -> {
                  if (path == null) {
                     Axiom.LOGGER.info("Skipping schematic load since file dialog was cancelled");
                  } else {
                     Axiom.LOGGER.info("Loading schematic from file: {}", path);

                     try {
                        Path file = Path.of(path);
                        CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());

                        while (tag.getAllKeys().size() == 1) {
                           String key = (String)tag.getAllKeys().iterator().next();
                           CompoundTag inner = tag.getCompound(key);
                           if (!inner.isEmpty()) {
                              tag = inner;
                           }
                        }

                        ClipboardObject clipboardObject;
                        if (tag.contains("Regions")) {
                           Minecraft.getInstance().submit(() -> ChatUtils.info("Loading Litematic schematic"));
                           clipboardObject = SchematicLoader.loadLitematic(tag);
                        } else if (tag.contains("Version")) {
                           int version = VersionUtilsNbt.helperCompoundTagGetInt(tag, "Version").get();
                           Minecraft.getInstance().submit(() -> ChatUtils.info("Loading SpongeV" + version + " schematic"));
                           clipboardObject = SchematicLoader.loadSponge(tag);
                        } else {
                           if (!tag.contains("Materials")) {
                              throw new SchematicLoader.SchematicLoadException("Unknown format");
                           }

                           Minecraft.getInstance().submit(() -> ChatUtils.info("Loading Legacy1.12 schematic"));
                           clipboardObject = SchematicLoader.loadLegacy(tag);
                        }

                        Minecraft.getInstance().submit(() -> {
                           if (!clipboardObject.blockRegion().isEmpty()) {
                              Clipboard.INSTANCE.setClipboard(clipboardObject);
                           } else {
                              ChatUtils.error("Schematic appears to be empty :(");
                           }
                        });
                     } catch (Throwable var5x) {
                        var5x.printStackTrace();
                        String message = var5x.getMessage();
                        Minecraft.getInstance().submit(() -> ChatUtils.error(message));
                     }
                  }
               });
            }

            if (!AxiomClient.hasPermission(AxiomPermission.CAN_EXPORT_BLOCKS)) {
               ImGuiHelper.disabledMenuItem(AxiomI18n.get("axiom.editorui.mainmenu.export_schematic"), "Server has disallowed exporting");
            } else if (clipboardEmpty) {
               ImGuiHelper.disabledMenuItem(
                  AxiomI18n.get("axiom.editorui.mainmenu.export_schematic"), AxiomI18n.get("axiom.tool.painter.clipboard_empty_warning")
               );
            } else if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.export_schematic"))) {
               ClipboardObject clipboardObject = Clipboard.INSTANCE.getClipboard();
               if (clipboardObject instanceof ClipboardObject.Anonymous anonymous) {
                  ExportSchematicWindow.open(clipboardObject.blockRegion(), clipboardObject.blockEntities(), anonymous.additionalSchematicData());
               } else {
                  ExportSchematicWindow.open(clipboardObject.blockRegion(), clipboardObject.blockEntities(), null);
               }
            }

            if (!AxiomClient.hasPermission(AxiomPermission.CAN_EXPORT_BLOCKS)) {
               ImGuiHelper.disabledMenuItem("Export Minecraft Structure NBT", "Server has disallowed exporting");
            } else if (clipboardEmpty) {
               ImGuiHelper.disabledMenuItem("Export Minecraft Structure NBT", "Clipboard is empty");
            } else if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.export_structure_nbt"))) {
               CompoundTag structureNbt = VanillaStructureHelper.toStructureNbt(Clipboard.INSTANCE.getClipboard());
               CompletableFuture<String> future = AsyncFileDialogs.saveFileDialog(
                  AxiomPlatform.gameDir().toString(), "structure.nbt", "NBT", "nbt"
               );
               future.thenAccept(filePath -> {
                  if (filePath == null) {
                     Axiom.LOGGER.info("Skipping export structure nbt since file dialog was cancelled");
                     ChatUtils.warning("Cancelled file save, aborting");
                  } else {
                     Axiom.LOGGER.info("Saving structure nbt to file: {}", filePath);
                     Minecraft.getInstance().submit(() -> {
                        try {
                           Path file = Path.of(filePath);
                           NbtIo.writeCompressed(structureNbt, file);
                        } catch (Throwable var3x) {
                           var3x.printStackTrace();
                           ChatUtils.error(var3x.getMessage());
                        }
                     });
                  }
               });
            }

            if (!AxiomClient.hasPermission(AxiomPermission.CAN_EXPORT_BLOCKS)) {
               ImGuiHelper.disabledMenuItem("Export as CSV", "Server has disallowed exporting");
            } else if (Selection.getSelectionBuffer().isEmpty()) {
               ImGuiHelper.disabledMenuItem("Export as CSV", "Selection is empty");
            } else if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.export_as_csv"))) {
               ClientLevel level = Minecraft.getInstance().level;
               MutableBlockPos mutableBlockPos = new MutableBlockPos();
               StringBuilder stringBuilder = new StringBuilder();
               Selection.getSelectionBuffer().forEach((x, y, z) -> {
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                  if (!blockState.isAir()) {
                     stringBuilder.append(BlockStateParser.serialize(blockState).replace(",", ";"));
                     stringBuilder.append(',');
                     stringBuilder.append(x);
                     stringBuilder.append(',');
                     stringBuilder.append(y);
                     stringBuilder.append(',');
                     stringBuilder.append(z);
                     stringBuilder.append('\n');
                  }
               });
               CompletableFuture<String> future = AsyncFileDialogs.saveFileDialog(
                  AxiomPlatform.gameDir().toString(), "blocks.csv", "Comma Separated Values", "csv"
               );
               future.thenAccept(filePath -> {
                  if (filePath == null) {
                     Axiom.LOGGER.info("Skipping export as csv since file dialog was cancelled");
                     ChatUtils.warning("Cancelled file save, aborting");
                  } else {
                     Axiom.LOGGER.info("Saving blocks as csv to file: {}", filePath);
                     Minecraft.getInstance().submit(() -> {
                        try {
                           Path file = Path.of(filePath);
                           Files.writeString(file, stringBuilder);
                        } catch (Throwable var3x) {
                           var3x.printStackTrace();
                           ChatUtils.error(var3x.getMessage());
                        }
                     });
                  }
               });
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.open_reference_image"))) {
               Path path = AxiomPlatform.gameDir();
               String dirStr = path.toString();
               String separator = path.getFileSystem().getSeparator();
               if (!dirStr.endsWith(separator)) {
                  dirStr = dirStr + separator;
               }

               CompletableFuture<String> future = AsyncFileDialogs.openFileDialog(dirStr, "Image Files", "png", "jpeg", "jpg");
               future.thenAccept(imagePath -> {
                  if (imagePath == null) {
                     Axiom.LOGGER.info("Skipping open reference image since file dialog was cancelled");
                     ChatUtils.warning("Cancelled open file, aborting");
                  } else {
                     Axiom.LOGGER.info("Opening reference image from file: {}", imagePath);
                     Minecraft.getInstance().submit(() -> {
                        Path file = Path.of(imagePath);

                        try {
                           BufferedImage image = ImageIO.read(Files.newInputStream(file));
                           int width = image.getWidth();
                           int height = image.getHeight();
                           NativeImage nativeImage = new NativeImage(width, height, true);

                           for (int x = 0; x < width; x++) {
                              for (int y = 0; y < height; y++) {
                                 int argb = image.getRGB(x, y);
                                 int alpha = argb >> 24 & 0xFF;
                                 int red = argb >> 16 & 0xFF;
                                 int green = argb >> 8 & 0xFF;
                                 int blue = argb & 0xFF;
                                 nativeImage.setPixelRGBA(x, y, ColourUtils.argbToAbgr(alpha << 24 | red << 16 | green << 8 | blue));
                              }
                           }

                           ImageReferenceWindows.add(nativeImage);
                        } catch (Throwable var13x) {
                           var13x.printStackTrace();
                           ChatUtils.error(var13x.getMessage());
                        }
                     });
                  }
               });
            }

            ImGui.endMenu();
         }

         if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.edit"))) {
            int historyPosition = Dispatcher.getHistoryPosition(false);
            if (historyPosition < 0) {
               ImGui.beginDisabled();
               ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.edit.undo"), Keybinds.UNDO.longKeyIdentifier());
               ImGui.endDisabled();
            } else {
               Dispatcher.HistoryData data = Dispatcher.getHistoryData(historyPosition);
               String undoMessage = AxiomI18n.get("axiom.editorui.mainmenu.edit.undo_named", data.entry().description());
               if (ImGui.menuItem(undoMessage, Keybinds.UNDO.longKeyIdentifier())) {
                  Dispatcher.callAction(UserAction.UNDO, null);
               }
            }

            if (historyPosition + 1 >= Dispatcher.getHistoryDataCount()) {
               ImGui.beginDisabled();
               ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.edit.redo"), Keybinds.REDO.longKeyIdentifier());
               ImGui.endDisabled();
            } else {
               Dispatcher.HistoryData datax = Dispatcher.getHistoryData(historyPosition + 1);
               String redoMessage = AxiomI18n.get("axiom.editorui.mainmenu.edit.redo_named", datax.entry().description());
               if (ImGui.menuItem(redoMessage, Keybinds.REDO.longKeyIdentifier())) {
                  Dispatcher.callAction(UserAction.REDO, null);
               }
            }

            ImGui.separator();
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.edit.cut"), Keybinds.CUT.longKeyIdentifier(), false, selectionActive)) {
               UserAction.CUT.call(null);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.edit.copy"), Keybinds.COPY.longKeyIdentifier(), false, selectionActive)) {
               UserAction.COPY.call(null);
            }

            ImGui.separator();
            if (!AxiomClient.hasPermission(AxiomPermission.CAN_EXPORT_BLOCKS)) {
               ImGuiHelper.disabledMenuItem(AxiomI18n.get("axiom.editorui.mainmenu.edit.save_blueprint"), "Server has disallowed saving blueprints");
            } else if (ImGui.menuItem(
               AxiomI18n.get("axiom.editorui.mainmenu.edit.save_blueprint"), Keybinds.SAVE_BLUEPRINT.longKeyIdentifier(), false, clipboardNonEmpty
            )) {
               UserAction.SAVE.call(null);
            }

            ImGui.endMenu();
         }

         if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.select"))) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.clear"), AxiomI18n.get("key.keyboard.enter"), false, selectionActive)) {
               Selection.clearSelection();
            }

            if (!selectionActive) {
               ImGui.beginDisabled();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.move_selection"))) {
               ModifySelection.start();
            }

            if (!selectionActive) {
               ImGui.endDisabled();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.filter"))) {
               EditorWindowType.FILTER_SELECTION.setOpen(true);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.expand"))) {
               EditorWindowType.EXPAND_SELECTION.setOpen(true);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.shrink"))) {
               EditorWindowType.SHRINK_SELECTION.setOpen(true);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.distort"))) {
               EditorWindowType.DISTORT_SELECTION.setOpen(true);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.smooth"))) {
               EditorWindowType.SMOOTH_SELECTION.setOpen(true);
            }

            if (Selection.getSelectionBuffer() instanceof SelectionBuffer.Set set && set.selectionRegion.count() > 0) {
               if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.bounding_box"))) {
                  Selection.addAABB(set.min(), set.max());
               }

               if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.convex_hull"))) {
                  DoubleArrayList doubleArrayList = new DoubleArrayList(set.selectionRegion.count() * 3);
                  set.selectionRegion.forEach((x, y, z) -> {
                     doubleArrayList.add(x);
                     doubleArrayList.add(y);
                     doubleArrayList.add(z);
                  });
                  PositionSet newSet = new PositionSet();
                  double[] positions = doubleArrayList.elements();
                  if (positions.length != doubleArrayList.size()) {
                     positions = doubleArrayList.toDoubleArray();
                  }

                  HullRasterization.quickHullPositionSet(newSet, positions);
                  Selection.addSet(newSet);
               }
            } else {
               ImGui.beginDisabled();
               ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.select.bounding_box"));
               ImGui.endDisabled();
               ImGui.beginDisabled();
               ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.convex_hull"));
               ImGui.endDisabled();
            }

            if (Selection.getSelectionBuffer().isEmpty()) {
               ImGuiHelper.disabledMenuItem("Save...", "Selection is empty");
            } else {
               if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.save_ellipsis"))) {
                  CompletableFuture<String> future = AsyncFileDialogs.saveFileDialog(
                     AxiomPlatform.gameDir().toString(), "selection.csv", "Comma Separated Values", "csv"
                  );
                  future.thenAccept(
                     filePath -> {
                        if (filePath == null) {
                           Axiom.LOGGER.info("Skipping save selection since file dialog was cancelled");
                           ChatUtils.warning("Cancelled file save, aborting");
                        } else {
                           Axiom.LOGGER.info("Saving selection to file: {}", filePath);
                           Minecraft.getInstance()
                              .submit(
                                 () -> {
                                    try {
                                       Path file = Path.of(filePath);

                                       try (BufferedWriter writer = Files.newBufferedWriter(
                                             file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
                                          )) {
                                          SelectionSerialization.writeSelection(writer, Selection.getSelectionBuffer());
                                       }
                                    } catch (Throwable var7x) {
                                       var7x.printStackTrace();
                                       ChatUtils.error(var7x.getMessage());
                                    }
                                 }
                              );
                        }
                     }
                  );
               }

               ImGuiHelper.tooltip("Save selection region to a file. Can be used in external programs, or loaded through the Load... button below");
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.load_ellipsis"))) {
               CompletableFuture<String> future = AsyncFileDialogs.openFileDialog(
                  AxiomPlatform.gameDir().toString(), "Comma Separated Values", "csv"
               );
               future.thenAccept(filePath -> {
                  if (filePath == null) {
                     Axiom.LOGGER.info("Skipping load selection since file dialog was cancelled");
                     ChatUtils.warning("Cancelled open file, aborting");
                  } else {
                     Axiom.LOGGER.info("Loading selection from file: {}", filePath);
                     Minecraft.getInstance().submit(() -> {
                        Path file = Path.of(filePath);

                        try (BufferedReader reader = Files.newBufferedReader(file)) {
                           BooleanWrapper malformed = new BooleanWrapper(false);
                           SelectionBuffer buffer = SelectionSerialization.loadSelection(reader, malformed);
                           if (malformed.value) {
                              ChatUtils.warning("Selection file contained unexpected data. Selection may be malformed");
                           }

                           Selection.setBufferWithHistory(buffer);
                        } catch (Throwable var7x) {
                           var7x.printStackTrace();
                           ChatUtils.error(var7x.getMessage());
                        }
                     });
                  }
               });
            }

            ImGuiHelper.tooltip("Load a selection region from a file");
            ImGui.endMenu();
         }

         if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.view"))) {
            boolean canAddView = ViewManager.getViews().size() < 16 && AxiomClient.hasPermission(AxiomPermission.EDITOR_VIEWS);
            if (!canAddView) {
               ImGui.beginDisabled();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.view.create_new"))) {
               ViewManager.addNewView();
            }

            if (!canAddView) {
               ImGui.endDisabled();
            }

            ImGui.separator();
            if (ImGui.menuItem(
               AxiomI18n.get("axiom.editorui.mainmenu.view.show_selection"), Keybinds.SHOW_SELECTION.longKeyIdentifier(), Selection.shouldRenderSelection()
            )) {
               Selection.setShouldRenderSelection(!Selection.shouldRenderSelection());
            }

            if (ImGui.menuItem(
               AxiomI18n.get("axiom.editorui.mainmenu.view.show_biomes"), Keybinds.SHOW_BIOMES.longKeyIdentifier(), Axiom.configuration.visuals.showBiomes
            )) {
               Axiom.configuration.visuals.showBiomes = !Axiom.configuration.visuals.showBiomes;
            }

            if (ImGui.menuItem(
               AxiomI18n.get("axiom.editorui.mainmenu.view.show_annotations"),
               Keybinds.SHOW_ANNOTATIONS.longKeyIdentifier(),
               Axiom.configuration.visuals.showAnnotations
            )) {
               Axiom.configuration.visuals.showAnnotations = !Axiom.configuration.visuals.showAnnotations;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.view.show_ruler"), "", Axiom.configuration.visuals.showRuler)) {
               Axiom.configuration.visuals.showRuler = !Axiom.configuration.visuals.showRuler;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.view.show_key_presses"), "", Axiom.configuration.visuals.keypressOverlay)) {
               Axiom.configuration.visuals.keypressOverlay = !Axiom.configuration.visuals.keypressOverlay;
            }

            float[] uiScale = new float[]{Axiom.configuration.internal.globalScale};
            ImGui.setNextItemWidth(100.0F * EditorUI.getUiScale());
            if (ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.mainmenu.view.ui_scale"), uiScale, 0.5F, 2.0F)) {
               Axiom.configuration.internal.globalScale = uiScale[0];
            }

            float[] minBrightness = new float[]{Axiom.configuration.visuals.minBrightness / 100.0F};
            ImGui.setNextItemWidth(100.0F * EditorUI.getUiScale());
            if (ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.mainmenu.view.min_brightness"), minBrightness, 0.0F, 1.0F)) {
               Axiom.configuration.visuals.minBrightness = (int)(minBrightness[0] * 100.0F);
            }

            float[] liquidOpacity = new float[]{Axiom.configuration.visuals.liquidOpacity / 100.0F};
            ImGui.setNextItemWidth(100.0F * EditorUI.getUiScale());
            if (ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.mainmenu.view.liquid_opacity"), liquidOpacity, 0.0F, 1.0F)) {
               Axiom.configuration.visuals.liquidOpacity = (int)(liquidOpacity[0] * 100.0F);
            }

            DowngradeVersion compatibilityLevel = BlockVersionCompatibility.getCompatibilityLevel();
            if (compatibilityLevel != null) {
               String[] versionStrings = DowngradeVersionList.getVersionStrings();
               if (versionStrings != null) {
                  int currentIndex = DowngradeVersionList.getVersions().indexOf(compatibilityLevel);
                  int[] indexArray = new int[]{currentIndex};
                  ImGui.setNextItemWidth(100.0F * EditorUI.getUiScale());
                  if (ImGuiHelper.combo("Block Version", indexArray, versionStrings) && indexArray[0] != currentIndex) {
                     BlockVersionCompatibility.setCompatibilityLevel(DowngradeVersionList.getVersions().get(indexArray[0]));
                  }

                  ImGuiHelper.tooltip("Set the compatibility version for Blocks. Incompatible newer blocks will be highlighted with a red background");
               }
            }

            ImGui.endMenu();
         }

         renderOperationsImgui(true);
         if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.toolmasks"))) {
            boolean canClear = MaskManager.hasConfiguredMask();
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.toolmasks.clear"), null, false, canClear)) {
               MaskManager.setConfiguredMask(null, true, true);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.toolmasks.edit"))) {
               EditorWindowType.TOOL_MASKS.setOpen(true);
            }

            ImGui.endMenu();
         }

         if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.window"))) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.store_layout_as_default"))) {
               Axiom.configuration.internal.defaultLayout = storeLayout();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.restore_default_layout"))) {
               restoreLayout(Axiom.configuration.internal.defaultLayout);
            }

            ImGui.separator();
            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.copy_layout_to_clipboard"))) {
               Minecraft.getInstance().keyboardHandler.setClipboard(storeLayout());
            }

            String clipboard = EditorUI.getClipboard();
            boolean allowLoadingLayout = clipboard.contains("[Docking][Data]");
            if (!allowLoadingLayout) {
               ImGui.beginDisabled();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.load_layout_from_clipboard"))) {
               restoreLayout(clipboard);
            }

            if (!allowLoadingLayout) {
               ImGui.endDisabled();
            }

            ImGui.separator();
            AxiomConfig.SubcategoryInternal internal = Axiom.configuration.internal;
            if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.mainmenu.window.show_close_window_button"), internal.showCloseWindowButton)) {
               internal.showCloseWindowButton = !internal.showCloseWindowButton;
            }

            ImGui.separator();
            if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.window.windows"))) {
               for (EditorWindowType windowType : EditorWindowType.values()) {
                  if (windowType.isImportant()) {
                     ImBoolean open = new ImBoolean(windowType.isOpen());
                     ImGui.checkbox(windowType.getName(), open);
                     windowType.setOpen(open.get());
                  }
               }

               ImGui.endMenu();
            }

            ImGui.endMenu();
         }

         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.help.keybinds"))) {
            EditorWindowType.KEYBINDS.setOpen(true);
         }

         if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.help"))) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.help.discord_server"))) {
               String discord = "https://discord.gg/axiomtool/";
               Minecraft.getInstance().setScreen(new ConfirmLinkScreen(openx -> {
                  if (openx) {
                     try {
                        Util.getPlatform().openUri(new URI(discord));
                     } catch (Exception var3x) {
                     }
                  }

                  Minecraft.getInstance().setScreen(null);
               }, discord, false));
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.help.documentation"))) {
               String docs = "https://axiomdocs.moulberry.com/";
               Minecraft.getInstance().setScreen(new ConfirmLinkScreen(openx -> {
                  if (openx) {
                     try {
                        Util.getPlatform().openUri(new URI(docs));
                     } catch (Exception var3x) {
                     }
                  }

                  Minecraft.getInstance().setScreen(null);
               }, docs, false));
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.help.style_editor"))) {
               EditorWindowType.STYLE_EDITOR.setOpen(true);
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.skip_tutorial"))) {
               TutorialManager.skip();
            }

            if (AxiomPlatform.isDevelopment() && ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.help.reset_tutorial"))) {
               if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.introduction"))) {
                  TutorialManager.reset(Tutorial.INTRODUCTION);
               }

               if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.selection"))) {
                  TutorialManager.reset(Tutorial.SELECTION);
               }

               if (ImGui.beginMenu(AxiomI18n.get("axiom.hardcoded.tools_lbl"))) {
                  if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.magic_select"))) {
                     TutorialManager.reset(Tutorial.MAGIC_SELECT_TOOL);
                  }

                  if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.box_select"))) {
                     TutorialManager.reset(Tutorial.BOX_SELECT_TOOL);
                  }

                  if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.slope"))) {
                     TutorialManager.reset(Tutorial.SLOPE_TOOL);
                  }

                  if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.sculpt_draw"))) {
                     TutorialManager.reset(Tutorial.SCULPT_DRAW_TOOL);
                  }

                  ImGui.endMenu();
               }

               ImGui.endMenu();
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.open_source_licenses"))) {
               OpenSourceLicensesWindow.open = true;
            }

            ImGui.endMenu();
         }

         ImGui.endMainMenuBar();
      }
   }

   @NotNull
   private static String storeLayout() {
      String iniSettingsStr = ImGui.saveIniSettingsToMemory();
      boolean addNewline = !iniSettingsStr.endsWith("\n");
      StringBuilder iniSettings = new StringBuilder(iniSettingsStr);
      if (addNewline) {
         iniSettings.append("\n");
      }

      iniSettings.append("[AxiomWindows]\n");

      for (String openWindow : EditorWindowType.getOpenByName()) {
         iniSettings.append(openWindow).append("\n");
      }

      return iniSettings.toString();
   }

   private static void restoreLayout(String layout) {
      if (layout.contains("[Docking][Data]")) {
         String[] split = layout.split("\\[AxiomWindows\\]");
         String imgui = split[0].trim();
         ImGui.loadIniSettingsFromMemory(imgui);
         if (split.length >= 2) {
            String axiom = split[1].trim();
            String[] windows = axiom.split("\n");
            List<String> openByName = new ArrayList<>();

            for (String window : windows) {
               openByName.add(window.trim());
            }

            EditorWindowType.setOpenByName(openByName);
         } else {
            EditorWindowType.setOpenToDefault();
         }
      }
   }

   public static void renderOperationsImgui(boolean menu) {
      if (menu) {
         if (!ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.operations"))) {
            return;
         }
      } else {
         if (ImGui.button(AxiomI18n.get("axiom.editorui.mainmenu.operations") + "...")) {
            ImGui.openPopup("OperationsPopup");
         }

         if (!ImGuiHelper.beginPopup("OperationsPopup")) {
            return;
         }
      }

      if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_category"))) {
         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill"), Keybinds.QUICK_FILL.longKeyIdentifier())) {
            FillBlocksWindow.setFillType(0);
            EditorWindowType.FILL.setOpen(true);
         }

         ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_description"));
         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_outline"))) {
            FillBlocksWindow.setFillType(2);
            EditorWindowType.FILL.setOpen(true);
         }

         ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_outline_description"));
         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_walls"))) {
            FillBlocksWindow.setFillType(3);
            EditorWindowType.FILL.setOpen(true);
         }

         ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_walls_description"));
         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_top"))) {
            FillBlocksWindow.setFillType(4);
            EditorWindowType.FILL.setOpen(true);
         }

         ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_top_description"));
         if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_bottom"))) {
            FillBlocksWindow.setFillType(5);
            EditorWindowType.FILL.setOpen(true);
         }

         ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_bottom_description"));
         ImGui.endMenu();
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_nearest"))) {
         FillNearestOperation.fillNearest();
      }

      ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_nearest_description"));
      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.replace"), Keybinds.QUICK_REPLACE.longKeyIdentifier())) {
         EditorWindowType.REPLACE.setOpen(true);
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.type_replace"))) {
         EditorWindowType.TYPE_REPLACE.setOpen(true);
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.set_biome"))) {
         EditorWindowType.SET_BIOME.setOpen(true);
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.autoshade"))) {
         EditorWindowType.AUTOSHADE.setOpen(true);
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.drain"))) {
         DrainOperation.drain();
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.waterlog"))) {
         WaterlogOperation.waterlog();
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.smoothsnow"))) {
         SmoothSnow.smoothSnow();
      }

      ImGui.separator();
      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.simulate_gravity"))) {
         SimulateGravityOperation.gravity();
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.trigger_updates"))) {
         UpdateBlocksOperation.updateBlocks();
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.trigger_updates_and_tick"))) {
         TickBlocksModal.open();
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.fix_lighting_data"))) {
         ClientLevel level = Minecraft.getInstance().level;
         SelectionBuffer buffer = Selection.getSelectionBuffer();
         if (buffer instanceof SelectionBuffer.AABB aabb) {
            new AxiomServerboundFixArea(level.dimension(), aabb.min(), aabb.max()).send();
         } else if (buffer instanceof SelectionBuffer.Set set) {
            new AxiomServerboundFixArea(level.dimension(), set.selectionRegion.unsafeGetPositionSet()).send();
         }

         Selection.clearSelection();
      }

      ImGui.separator();
      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.hollow"))) {
         HollowOperation.hollow();
      }

      ImGuiHelper.tooltip("Hollow replaces all unreachable blocks inside your build with air");
      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_gaps"))) {
         HollowOperation.fillGaps(Tool.getActiveBlock());
      }

      ImGuiHelper.tooltip("Fill gaps replaces all unreachable air blocks inside your build with your active block");
      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.fill_unreachable"))) {
         HollowOperation.fillUnreachable(Tool.getActiveBlock());
      }

      ImGuiHelper.tooltip("Fill unreachable replaces all unreachable non-air blocks inside your build with your active block");
      ImGui.separator();
      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.generate_colour_field"))) {
         GenerateColourFieldOperation.generateColourField();
      }

      ImGui.separator();
      if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.mainmenu.operations.analyze"))) {
         EditorWindowType.ANALYZE.setOpen(true);
      }

      if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.animated_rebuild_ellipsis"))) {
         EditorWindowType.ANIMATED_REBUILD.setOpen(true);
      }

      ImGuiHelper.tooltip("Gradually rebuilds a selection block-by-block. This feature is intended for content creators making video showcases of a build");
      if (menu) {
         ImGui.endMenu();
      } else {
         ImGui.endPopup();
      }
   }
}
