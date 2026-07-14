package com.moulberry.axiom.editor.windows;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.blueprint.BlueprintHeader;
import com.moulberry.axiom.blueprint.BlueprintIo;
import com.moulberry.axiom.editor.BlueprintPreview;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.TagListWidget;
import com.moulberry.axiom.editor.windows.clipboard.BlueprintBrowserWindow;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.AsyncFileDialogs;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.StringUtils;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.type.ImString;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

public class BlueprintCreateWindow {
   private static BlueprintPreview blueprintPreview = null;
   private static final ImString blueprintName = new ImString();
   private static final ImString authorName = new ImString();
   private static boolean showingWindow = false;
   private static final TagListWidget tagListWidget = new TagListWidget();
   private static ChunkedBlockRegion blockRegion = null;
   private static Long2ObjectMap<CompressedBlockEntity> blockEntities = null;
   private static List<CompoundTag> entities = null;
   private static boolean containsAir = false;
   private static boolean isRotating = false;
   private static float lastMouseX = 0.0F;
   private static float lastMouseY = 0.0F;
   private static boolean focusFirst = false;
   private static boolean focusLast = false;
   private static CompletableFuture<String> saveFileCompletableFuture = null;
   private static CompletableFuture<NativeImage> blueprintPreviewFuture = null;

   public static void render() {
      String createBlueprint = AxiomI18n.get("axiom.editorui.window.create_blueprint");
      if (!showingWindow) {
         if (ImGui.isPopupOpen(createBlueprint + "###CreateBlueprint")) {
            close();
         }
      } else {
         ImVec2 center = ImGui.getMainViewport().getCenter();
         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         if (!ImGui.isPopupOpen(createBlueprint + "###CreateBlueprint")) {
            ImGui.openPopup("###CreateBlueprint");
         }

         if (ImGuiHelper.beginPopupModalCloseable(createBlueprint + "###CreateBlueprint", 68)) {
            if (EditorUI.consumeNavClose()) {
               close();
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               return;
            }

            if (saveFileCompletableFuture != null && saveFileCompletableFuture.isDone()) {
               NativeImage nativeImage = tryGetImage();
               if (nativeImage != null) {
                  String filePath = saveFileCompletableFuture.join();
                  saveFileCompletableFuture = null;
                  if (filePath != null) {
                     save(filePath, nativeImage);
                     close();
                     ImGui.closeCurrentPopup();
                     ImGui.endPopup();
                     return;
                  }
               }

               ImGui.endPopup();
               return;
            }

            if (blueprintPreview == null) {
               blueprintPreview = new BlueprintPreview();
            }

            if (blockRegion.count() < 16777216) {
               ImGui.image(blueprintPreview.render(512, true, true), 256.0F, 256.0F, 0.0F, 1.0F, 1.0F, 0.0F);
               if (isRotating) {
                  if (!ImGui.isMouseDown(0)) {
                     isRotating = false;
                     blueprintPreview.mouseReleased();
                  } else {
                     float mouseX = ImGui.getMousePosX();
                     float mouseY = ImGui.getMousePosY();
                     blueprintPreview.mouseMoved(mouseX - lastMouseX, mouseY - lastMouseY, EditorUI.isCtrlOrCmdDown());
                     lastMouseX = mouseX;
                     lastMouseY = mouseY;
                  }
               } else if (ImGui.isItemClicked(0)) {
                  isRotating = true;
                  lastMouseX = ImGui.getMousePosX();
                  lastMouseY = ImGui.getMousePosY();
               }
            }

            boolean handledFocusNext = false;
            if (focusFirst) {
               focusFirst = false;
               handledFocusNext = true;
               ImGui.setKeyboardFocusHere();
            }

            String username = Minecraft.getInstance().player.getScoreboardName();
            ImGui.inputText(AxiomI18n.get("axiom.editorui.window.create_blueprint.name"), blueprintName);
            if (!handledFocusNext && ImGui.isItemActive() && ImGui.isKeyPressed(512, false)) {
               handledFocusNext = true;
               ImGui.setKeyboardFocusHere();
            }

            ImGui.inputTextWithHint(AxiomI18n.get("axiom.editorui.window.create_blueprint.author"), username, authorName);
            if (!handledFocusNext && ImGui.isItemActive() && ImGui.isKeyPressed(512, false)) {
               handledFocusNext = true;
               ImGui.setKeyboardFocusHere(1);
            }

            tagListWidget.render(256);
            float[] angleArray = new float[]{blueprintPreview.getYaw(), blueprintPreview.getPitch()};
            if (focusLast) {
               focusLast = false;
               handledFocusNext = true;
               ImGui.setKeyboardFocusHere(1);
            }

            if (ImGui.inputFloat2(AxiomI18n.get("axiom.editorui.window.create_blueprint.angle"), angleArray, "%.2f")) {
               blueprintPreview.setYaw(angleArray[0], false);
               blueprintPreview.setPitch(angleArray[1], false);
            }

            if (!handledFocusNext && ImGui.isItemActive() && ImGui.isKeyPressed(512, false)) {
               handledFocusNext = true;
               focusLast = true;
            }

            if (!handledFocusNext && ImGui.isKeyPressed(512, false)) {
               focusFirst = true;
            }

            boolean save = ImGui.button(AxiomI18n.get("axiom.editorui.window.create_blueprint.save"));
            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
               close();
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               return;
            }

            if (save || ImGui.isKeyPressed(525, false)) {
               Path blueprintDir = Axiom.getInstance().getBlueprintDirectory();
               String separator = blueprintDir.getFileSystem().getSeparator();
               String blueprintNameString = ImGuiHelper.getString(blueprintName).trim();
               String snakeName;
               if (blueprintNameString.isEmpty()) {
                  snakeName = "unnamed.bp";
               } else {
                  snakeName = blueprintNameString.toLowerCase(Locale.ROOT).replace(' ', '_').replace(separator, "_") + ".bp";
               }

               snakeName = StringUtils.sanitizePath(snakeName);

               try {
                  saveFileCompletableFuture = AsyncFileDialogs.saveFileDialog(blueprintDir.toString(), snakeName, "Blueprint Files", "bp");
               } catch (Exception var11) {
                  var11.printStackTrace();
               }
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         if (!ImGui.isPopupOpen(createBlueprint + "###CreateBlueprint")) {
            close();
         }
      }
   }

   private static NativeImage tryGetImage() {
      if (blueprintPreview == null) {
         blueprintPreview = new BlueprintPreview();
      }

      if (blueprintPreviewFuture != null) {
         if (blueprintPreviewFuture.isDone()) {
            NativeImage nativeImage = blueprintPreviewFuture.join();
            blueprintPreviewFuture = null;
            return nativeImage;
         } else {
            return null;
         }
      } else if (blockRegion.count() < 16777216) {
         blueprintPreview.render(960, false, false);
         blueprintPreviewFuture = blueprintPreview.toNativeImage(96, true);
         return null;
      } else {
         return new NativeImage(96, 96, true);
      }
   }

   private static void save(String filePathName, NativeImage nativeImage) {
      if (blueprintPreview == null) {
         blueprintPreview = new BlueprintPreview();
      }

      filePathName = filePathName.replace(".bp.bp", ".bp");
      Path path = Path.of(filePathName);
      String authorNameStr = ImGuiHelper.getString(authorName).trim();
      if (authorNameStr.isEmpty()) {
         authorNameStr = Minecraft.getInstance().player == null ? "Unknown" : Minecraft.getInstance().player.getScoreboardName();
      }

      BlueprintHeader header = new BlueprintHeader(
         ImGuiHelper.getString(blueprintName).trim(),
         authorNameStr,
         tagListWidget.tags(),
         blueprintPreview.getYaw(),
         blueprintPreview.getPitch(),
         false,
         blockRegion.count(),
         containsAir
      );
      BlueprintBrowserWindow.updatedBlueprintPaths.add(path);

      try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
         BlueprintIo.write(outputStream, header, nativeImage, blockRegion, blockEntities, entities);
      } catch (IOException var10) {
         var10.printStackTrace();
      }

      nativeImage.close();
   }

   public static void open(ChunkedBlockRegion blockRegion, Long2ObjectMap<CompressedBlockEntity> blockEntities, List<CompoundTag> entities, boolean containsAir) {
      if (!AxiomClient.hasPermission(AxiomPermission.CAN_EXPORT_BLOCKS)) {
         ChatUtils.error("Server has disallowed saving blueprints");
      } else {
         EditorWindowType.KEYBINDS.setOpen(false);
         if (blueprintPreview == null) {
            blueprintPreview = new BlueprintPreview();
         }

         showingWindow = true;
         blueprintPreview.setBlockRegion(blockRegion);
         BlueprintCreateWindow.blockRegion = blockRegion;
         BlueprintCreateWindow.blockEntities = blockEntities;
         BlueprintCreateWindow.entities = entities;
         BlueprintCreateWindow.containsAir = containsAir;
         saveFileCompletableFuture = null;
      }
   }

   private static void close() {
      saveFileCompletableFuture = null;
      if (blueprintPreviewFuture != null) {
         blueprintPreviewFuture.thenAccept(NativeImage::close);
         blueprintPreviewFuture = null;
      }

      showingWindow = false;
      blockRegion = null;
      blockEntities = null;
      entities = null;
      if (blueprintPreview != null) {
         blueprintPreview.clear();
      }

      blueprintName.clear();
   }
}
