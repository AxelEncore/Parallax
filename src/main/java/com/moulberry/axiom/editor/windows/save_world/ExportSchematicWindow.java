package com.moulberry.axiom.editor.windows.save_world;

import com.moulberry.axiom.downgrade.DowngradeVersion;
import com.moulberry.axiom.downgrade.DowngradeVersionList;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.schematic.SchematicLoader;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.AsyncFileDialogs;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.StringUtils;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.utils.Authorization;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.type.ImString;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class ExportSchematicWindow {
   private static boolean showingWindow = false;
   private static ChunkedBlockRegion blockRegion = null;
   private static Long2ObjectMap<CompressedBlockEntity> blockEntities = null;
   private static CompoundTag additionalSchematicData = null;
   private static CompletableFuture<String> saveFileCompletableFuture = null;
   private static SaveSchematicAction saveSchematicAction = null;
   private static final ImString name = new ImString();
   private static final ImString author = new ImString();
   private static final int[] downgradeVersion = new int[]{0};

   public static void render() {
      String exportSchematic = "Export Schematic";
      if (!showingWindow) {
         if (ImGui.isPopupOpen(exportSchematic + "###ExportSchematic")) {
            close();
         }
      } else {
         ImVec2 center = ImGui.getMainViewport().getCenter();
         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         if (!ImGui.isPopupOpen(exportSchematic + "###ExportSchematic")) {
            ImGui.openPopup("###ExportSchematic");
         }

         if (ImGuiHelper.beginPopupModalCloseable(exportSchematic + "###ExportSchematic", 68)) {
            if (saveFileCompletableFuture != null && saveFileCompletableFuture.isDone()) {
               String filePath = saveFileCompletableFuture.join();
               saveFileCompletableFuture = null;
               if (filePath != null) {
                  save(filePath);
                  if (saveSchematicAction == null) {
                     close();
                     ImGui.closeCurrentPopup();
                     ImGui.endPopup();
                     return;
                  }
               }
            }

            if (saveSchematicAction != null && saveSchematicAction.render()) {
               saveSchematicAction = null;
               close();
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               return;
            }

            String[] versionStrings = DowngradeVersionList.getVersionStrings();
            if (versionStrings != null && versionStrings.length > 0) {
               if (!Authorization.hasCommercialLicense()) {
                  ImGui.beginDisabled();
                  ImGuiHelper.combo("Version", downgradeVersion, versionStrings);
                  ImGui.endDisabled();
                  ImGuiHelper.tooltip("Exporting to old versions is a Commercial License feature", 1024);
               } else {
                  ImGuiHelper.combo("Version", downgradeVersion, versionStrings);
               }
            } else if (Authorization.hasCommercialLicense()) {
               ImGui.text("⚠ Missing data for current Minecraft version. Version exporting is unavailable.");
            }

            String username = Minecraft.getInstance().player.getScoreboardName();
            ImGui.inputText(AxiomI18n.get("axiom.editorui.window.create_blueprint.name"), name);
            ImGui.inputTextWithHint(AxiomI18n.get("axiom.editorui.window.create_blueprint.author"), username, author);
            boolean save = ImGui.button(AxiomI18n.get("axiom.editorui.window.create_blueprint.save"));
            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
               close();
               ImGui.closeCurrentPopup();
               ImGui.endPopup();
               return;
            }

            if (save) {
               try {
                  Path schematicDir = SchematicLoader.getDefaultSchematicDir();
                  DowngradeVersion version = getSelectedDowngradeVersion();
                  String extension;
                  if (version != null && version.getMaxDataVersion() < 1631) {
                     extension = "schematic";
                  } else {
                     extension = "schem";
                  }

                  String blueprintNameString = ImGuiHelper.getString(name).trim();
                  String snakeName;
                  if (blueprintNameString.isEmpty()) {
                     snakeName = "unnamed." + extension;
                  } else {
                     snakeName = blueprintNameString.toLowerCase(Locale.ROOT).replace(' ', '_') + "." + extension;
                  }

                  snakeName = StringUtils.sanitizePath(snakeName);
                  saveFileCompletableFuture = AsyncFileDialogs.saveFileDialog(schematicDir.toString(), snakeName, "Schematic Files", extension);
               } catch (Exception var10) {
                  var10.printStackTrace();
               }
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         if (!ImGui.isPopupOpen(exportSchematic + "###ExportSchematic")) {
            close();
         }
      }
   }

   @Nullable
   private static DowngradeVersion getSelectedDowngradeVersion() {
      DowngradeVersion version = null;
      if (Authorization.hasCommercialLicense()) {
         List<DowngradeVersion> versions = DowngradeVersionList.getVersions();
         if (!versions.isEmpty()) {
            version = versions.get(downgradeVersion[0]);
            int currentDataVersion = DFUHelper.DATA_VERSION;
            if (currentDataVersion >= version.getMinDataVersion() && currentDataVersion <= version.getMaxDataVersion()) {
               version = null;
            }
         }
      }

      return version;
   }

   private static void save(String filePathName) {
      filePathName = filePathName.replace(".schem.schem", ".schem");
      filePathName = filePathName.replace(".schematic.schematic", ".schematic");
      Path path = Path.of(filePathName);
      DowngradeVersion version = getSelectedDowngradeVersion();
      String authorNameStr = ImGuiHelper.getString(author).trim();
      if (authorNameStr.isEmpty()) {
         authorNameStr = Minecraft.getInstance().player == null ? "Unknown" : Minecraft.getInstance().player.getScoreboardName();
      }

      String schematicNameStr = ImGuiHelper.getString(name);
      saveSchematicAction = new SaveSchematicAction(path, blockRegion, blockEntities, version, schematicNameStr, authorNameStr, additionalSchematicData);
      if (saveSchematicAction.run()) {
         saveSchematicAction = null;
      }
   }

   public static void open(ChunkedBlockRegion blockRegion, Long2ObjectMap<CompressedBlockEntity> blockEntities, CompoundTag additionalSchematicData) {
      showingWindow = true;
      ExportSchematicWindow.blockRegion = blockRegion;
      ExportSchematicWindow.blockEntities = blockEntities;
      ExportSchematicWindow.additionalSchematicData = additionalSchematicData;
      saveFileCompletableFuture = null;
   }

   private static void close() {
      saveFileCompletableFuture = null;
      showingWindow = false;
      blockRegion = null;
      blockEntities = null;
      additionalSchematicData = null;
   }
}
