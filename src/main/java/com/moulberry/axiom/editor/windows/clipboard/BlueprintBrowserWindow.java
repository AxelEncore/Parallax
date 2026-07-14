package com.moulberry.axiom.editor.windows.clipboard;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.MissingTextureImage;
import com.moulberry.axiom.blueprint.Blueprint;
import com.moulberry.axiom.blueprint.BlueprintHeader;
import com.moulberry.axiom.blueprint.BlueprintIo;
import com.moulberry.axiom.blueprint.RawBlueprint;
import com.moulberry.axiom.blueprint.ServerBlueprintRegistry;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.collections.FlowCache;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.core_rendering.AxiomGpuTexture;
import com.moulberry.axiom.editor.BlueprintPreview;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.TagListWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.packets.blueprint.AxiomServerboundRequestBlueprint;
import com.moulberry.axiom.packets.blueprint.AxiomServerboundUploadBlueprint;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.DynamicTextureSupplier;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.utils.Authorization;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiListClipper;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.callback.ImListClipperCallback;
import imgui.moulberry92.type.ImString;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.StringConcatFactory;
import java.nio.ByteBuffer;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;

public class BlueprintBrowserWindow {
   public static final BlueprintBrowserWindow.Thumbnail EMPTY_THUMBNAIL = new BlueprintBrowserWindow.Thumbnail(null);
   public static final Set<Path> updatedBlueprintPaths = new HashSet<>();
   private static final ImString searchString = ImGuiHelper.createResizableString(64);
   private static final ImString renameFilenameString = ImGuiHelper.createResizableString(64);
   private static String noLongerExistsErrorFileName = null;
   private static boolean reloadDirectoryStructure = false;
   private static BlueprintDirectory rootDirectory = null;
   private static BlueprintDirectory currentDirectory = null;
   private static List<BlueprintOrDirectory.Bp> recursiveSearchInCurrentDirectory = null;
   private static String lastRecursiveSearch = null;
   private static Set<String> lastRecursiveSearchFilteredTags = null;
   private static BlueprintBrowserWindow.EditBlueprintData editBlueprintData = null;
   private static final Set<String> filteredTags = new TreeSet<>();
   private static boolean filteredTagsChanged = false;
   private static Exception displayedException = null;
   private static BlueprintBrowserWindow.MoveFileOperation moveFileOperation = null;
   public static BlueprintDirectory pendingSelectDirectory = null;
   private static List<BlueprintDirectory> forwardsQueue = new ArrayList<>();
   private static boolean dirStructureToggled = false;
   public static boolean anyOrderUpdated = false;
   private static int updateOrderingCounter = 0;
   private static boolean renderedAtLeastOnce = false;
   private static int tooManyFileChangesCount = -1;
   private static float draggingDirListDividerStartMouseX = -1.0F;
   private static float draggingDirListDividerStartDirWidth = -1.0F;
   private static WatchService watchService = null;
   private static final Map<Path, BlueprintDirectory> dirStructureMap = new HashMap<>();
   private static ServerBlueprintRegistry serverBlueprintRegistry = null;
   private static BlueprintDirectory rootServerBlueprintDirectory = null;
   public static String pendingServerBlueprintDownload = null;
   public static Blueprint pendingServerBlueprintResult = null;
   private static boolean popupBrowserBecauseJustOpened = false;
   private static boolean makeActiveBecauseJustOpened = false;
   public static boolean selectMultiple = false;
   private static Predicate<Blueprint> callback = BlueprintBrowserWindow::defaultCallback;
   private static final FlowCache<PathWrapper, BlueprintBrowserWindow.Thumbnail> loadedBlueprintThumbnails = new FlowCache<>(0, 100, 4, 1, true, path -> {
      if (path.real() != null) {
         if (Files.isDirectory(path.real())) {
            return EMPTY_THUMBNAIL;
         } else {
            try {
               BlueprintBrowserWindow.Thumbnail var13;
               try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path.real()))) {
                  var13 = new BlueprintBrowserWindow.Thumbnail(BlueprintIo.readThumbnail(inputStream));
               }

               return var13;
            } catch (Exception var9) {
               var9.printStackTrace();
               return EMPTY_THUMBNAIL;
            }
         }
      } else {
         RawBlueprint rawBlueprint = serverBlueprintRegistry.blueprints().get(path.fakePath());
         if (rawBlueprint == null) {
            return EMPTY_THUMBNAIL;
         } else {
            try {
               MemoryStack memoryStack = MemoryStack.stackPush();

               NativeImage nativeImage;
               try {
                  ByteBuffer byteBuffer = memoryStack.malloc(rawBlueprint.thumbnail().length);
                  byteBuffer.put(rawBlueprint.thumbnail());
                  byteBuffer.rewind();
                  nativeImage = NativeImage.read(byteBuffer);
               } catch (Throwable var10) {
                  if (memoryStack != null) {
                     try {
                        memoryStack.close();
                     } catch (Throwable var6) {
                        var10.addSuppressed(var6);
                     }
                  }

                  throw var10;
               }

               if (memoryStack != null) {
                  memoryStack.close();
               }

               return new BlueprintBrowserWindow.Thumbnail(new DynamicTextureSupplier(nativeImage));
            } catch (Exception var11) {
               return EMPTY_THUMBNAIL;
            }
         }
      }
   });
   private static final int ICON_SIZE = 100;

   public static void open(@Nullable Predicate<Blueprint> callback, boolean selectMultiple) {
      EditorWindowType.BLUEPRINT_BROWSER.setOpen(true);
      BlueprintBrowserWindow.callback = callback == null ? BlueprintBrowserWindow::defaultCallback : callback;
      BlueprintBrowserWindow.selectMultiple = selectMultiple;
      makeActiveBecauseJustOpened = true;
      if (!EditorWindowType.BLUEPRINT_BROWSER.isDocked()) {
         popupBrowserBecauseJustOpened = true;
      }
   }

   public static void resetCallback() {
      callback = BlueprintBrowserWindow::defaultCallback;
      selectMultiple = false;
   }

   public static void setServerBlueprintRegistry(boolean replace, ServerBlueprintRegistry registry) {
      pendingServerBlueprintDownload = null;
      if (registry == null) {
         serverBlueprintRegistry = null;
      } else if (!replace && serverBlueprintRegistry != null) {
         for (Entry<String, RawBlueprint> entry : registry.blueprints().entrySet()) {
            serverBlueprintRegistry.blueprints().put(entry.getKey(), entry.getValue());
         }
      } else {
         serverBlueprintRegistry = registry;
      }

      if (rootServerBlueprintDirectory != null) {
         BlueprintDirectory currentRoot = currentDirectory;

         while (currentRoot.parent() != null) {
            currentRoot = currentRoot.parent();
         }

         if (rootServerBlueprintDirectory.equals(currentRoot)) {
            currentDirectory = rootDirectory;
            unsetRecursiveSearch();
         }

         rootServerBlueprintDirectory = null;
      }
   }

   private static void unsetRecursiveSearch() {
      recursiveSearchInCurrentDirectory = null;
      lastRecursiveSearch = null;
      lastRecursiveSearchFilteredTags = null;
   }

   private static BlueprintDirectory createHierarchical(Map<String, Object> hierarchical, String name, String path) {
      BlueprintDirectory directory = new BlueprintDirectory(new PathWrapper(null, path), name);

      for (Entry<String, Object> child : hierarchical.entrySet()) {
         if (child.getValue() instanceof RawBlueprint rawBlueprint) {
            directory.addLast(new BlueprintOrDirectory.Bp(new PathWrapper(null, path + child.getKey()), rawBlueprint.header()));
         } else {
            Map<String, Object> map = (Map<String, Object>)child.getValue();
            BlueprintDirectory childDirectory = createHierarchical(map, child.getKey(), path + child.getKey() + "/");
            directory.addLast(new BlueprintOrDirectory.Dir(childDirectory));
         }
      }

      directory.sort(BlueprintDirectory.SortMode.NAME);
      return directory;
   }

   private static boolean defaultCallback(Blueprint blueprint) {
      Clipboard.INSTANCE.setClipboard(blueprint);
      return true;
   }

   private static void updateOrdering(BlueprintDirectory blueprintDirectory) {
      blueprintDirectory.saveOrdering();

      for (BlueprintDirectory child : blueprintDirectory.children()) {
         updateOrdering(child);
      }
   }

   public static void tick() {
      if (renderedAtLeastOnce) {
         if (Authorization.hasCommercialLicense() && serverBlueprintRegistry != null && rootServerBlueprintDirectory == null) {
            Map<String, Object> hierarchical = new HashMap<>();

            for (Entry<String, RawBlueprint> entry : serverBlueprintRegistry.blueprints().entrySet()) {
               String path = entry.getKey();
               if (path.startsWith("/")) {
                  path = path.substring(1);
               }

               String[] paths = path.split("/");
               Map<String, Object> map = hierarchical;

               for (int i = 0; i < paths.length; i++) {
                  String name = paths[i];
                  if (i < paths.length - 1) {
                     map = (Map<String, Object>)map.computeIfAbsent(name, k -> new HashMap());
                  } else {
                     map.put(name, entry.getValue());
                  }
               }
            }

            rootServerBlueprintDirectory = createHierarchical(hierarchical, "Server Blueprints", "/");
         }

         boolean automaticRefreshing = Axiom.configuration.blueprint.automaticRefreshing;
         loadedBlueprintThumbnails.tick();
         if (pendingSelectDirectory != null) {
            setCurrentDirectory(pendingSelectDirectory);
            pendingSelectDirectory = null;
            if (currentDirectory == null) {
               forwardsQueue.clear();
            } else if (!forwardsQueue.isEmpty()) {
               BlueprintDirectory head = forwardsQueue.get(0);
               if (!currentDirectory.children().contains(head)) {
                  forwardsQueue.clear();
               }
            }
         }

         if (moveFileOperation != null) {
            handleMoveFileOperation();
         }

         label87:
         if (watchService == null) {
            for (Path updatedPath : updatedBlueprintPaths) {
               Path parent = updatedPath.getParent();
               if (parent != null && !updatedBlueprintPaths.contains(parent)) {
                  BlueprintDirectory blueprintDirectory = dirStructureMap.get(parent);
                  if (blueprintDirectory != null) {
                     if (!Files.exists(updatedPath)) {
                        blueprintDirectory.remove(new PathWrapper(updatedPath, null));
                     } else {
                        BlueprintBrowserWindow.Thumbnail texture = loadedBlueprintThumbnails.remove(new PathWrapper(updatedPath, null));
                        BlueprintOrDirectory blueprintOrDirectory = BlueprintDirectoryLoader.loadBlueprintOrDirectory(
                           updatedPath, watchService, dirStructureMap
                        );
                        if (blueprintOrDirectory != null) {
                           blueprintDirectory.addLast(blueprintOrDirectory);
                        }
                     }
                  }
               }
            }

            updatedBlueprintPaths.clear();
            if (rootDirectory == null || reloadDirectoryStructure || automaticRefreshing && watchService == null) {
               if (rootDirectory != null && anyOrderUpdated) {
                  updateOrdering(rootDirectory);
                  anyOrderUpdated = false;
               }

               searchString.clear();
               filteredTags.clear();

               try {
                  if (watchService != null) {
                     watchService.close();
                  }

                  if (automaticRefreshing) {
                     watchService = FileSystems.getDefault().newWatchService();
                  } else {
                     watchService = null;
                  }
               } catch (IOException var9) {
                  throw new UncheckedIOException(var9);
               }

               dirStructureMap.clear();
               Path rootPath = Axiom.getInstance().getBlueprintDirectory();
               rootDirectory = currentDirectory = BlueprintDirectoryLoader.loadDirectory("Blueprints", rootPath, watchService, dirStructureMap);
               unsetRecursiveSearch();
               loadedBlueprintThumbnails.clear();
               reloadDirectoryStructure = false;
               anyOrderUpdated = false;
               updateOrderingCounter = 0;
            }

            if (filteredTagsChanged) {
               updateSearch();
               filteredTagsChanged = false;
            }

            if (anyOrderUpdated) {
               if (updateOrderingCounter < 200) {
                  updateOrderingCounter++;
               } else {
                  updateOrdering(rootDirectory);
                  updateOrderingCounter = 0;
                  anyOrderUpdated = false;
               }
            }
         } else if (!automaticRefreshing) {
            try {
               watchService.close();
               watchService = null;
               break label87;
            } catch (IOException var8) {
               throw new UncheckedIOException(var8);
            }
         } else {
            pollAndUpdateWatchService();
            break label87;
         }
      }
   }

   private static void updateSearch() {
      String search = ImGuiHelper.getString(searchString);
      search = search.toLowerCase(Locale.ROOT);
      boolean allowRecursiveSearch = Axiom.configuration.blueprint.recursiveSearch && (!search.isEmpty() || !filteredTags.isEmpty());
      if (allowRecursiveSearch) {
         if (recursiveSearchInCurrentDirectory == null) {
            recursiveSearchInCurrentDirectory = new ArrayList<>();
         } else if (lastRecursiveSearch != null
            && search.startsWith(lastRecursiveSearch)
            && lastRecursiveSearchFilteredTags != null
            && filteredTags.containsAll(lastRecursiveSearchFilteredTags)) {
            if (search.equals(lastRecursiveSearch) && filteredTags.equals(lastRecursiveSearchFilteredTags)) {
               return;
            }

            lastRecursiveSearch = search;
            lastRecursiveSearchFilteredTags = Set.copyOf(filteredTags);
            String searchF = search;
            recursiveSearchInCurrentDirectory.removeIf(bp -> !bp.nameContainsLower(searchF) || !bp.containsAllTags(filteredTags));
         }

         lastRecursiveSearch = search;
         lastRecursiveSearchFilteredTags = Set.copyOf(filteredTags);
         recursiveSearchInCurrentDirectory.clear();
         currentDirectory.addRecursiveSearch(search, recursiveSearchInCurrentDirectory, filteredTags);
      } else {
         unsetRecursiveSearch();
         if (currentDirectory != null) {
            currentDirectory.search(search, filteredTags);
         }
      }
   }

   private static void handleMoveFileOperation() {
      if (moveFileOperation.to.equals(moveFileOperation.from)) {
         if (moveFileOperation.repositionReference != null) {
            moveFileOperation.to.reposition(moveFileOperation.file, moveFileOperation.repositionReference, moveFileOperation.repositionBefore);
         }

         moveFileOperation = null;
      } else if (!moveFileOperation.pendingOverwriteConfirmation || moveFileOperation.overwriteConfirmation) {
         try {
            Path path = getNewPath(moveFileOperation.to, moveFileOperation.file);
            moveBlueprint(moveFileOperation.file, path, moveFileOperation.overwriteConfirmation);
            if (moveFileOperation.from.remove(moveFileOperation.file.path())) {
               moveFileOperation.file.path(path);
               if (moveFileOperation.overwriteConfirmation) {
                  moveFileOperation.to.remove(moveFileOperation.file.path());
               }

               moveFileOperation.to.addLast(moveFileOperation.file);
            } else {
               reloadDirectoryStructure = true;
            }

            moveFileOperation = null;
         } catch (NoSuchFileException | FileNotFoundException var1) {
            noLongerExistsErrorFileName = moveFileOperation.file.path().getFileName();
            moveFileOperation = null;
         } catch (FileAlreadyExistsException var2) {
            if (moveFileOperation.overwriteConfirmation) {
               displayedException = var2;
               moveFileOperation = null;
               var2.printStackTrace();
            } else {
               moveFileOperation.pendingOverwriteConfirmation = true;
            }
         } catch (Exception var3) {
            displayedException = var3;
            moveFileOperation = null;
            var3.printStackTrace();
         }
      }
   }

   private static void pollAndUpdateWatchService() {
      WatchKey key;
      while ((key = watchService.poll()) != null) {
         Path keyPath = (Path)key.watchable();

         for (WatchEvent<?> event : key.pollEvents()) {
            Kind<?> kind = event.kind();
            if (kind == StandardWatchEventKinds.OVERFLOW) {
               if (tooManyFileChangesCount < 0) {
                  tooManyFileChangesCount = 0;
               }

               tooManyFileChangesCount = tooManyFileChangesCount + Math.max(0, event.count());
            } else if (event.context() instanceof Path filename && !filename.getFileName().toString().startsWith(".")) {
               Path absolute = keyPath.resolve(filename);
               updatedBlueprintPaths.add(absolute);
            }
         }

         if (!key.reset()) {
            BlueprintDirectory blueprintDirectory = dirStructureMap.remove(keyPath);
            if (blueprintDirectory != null && blueprintDirectory.parent() != null) {
               blueprintDirectory.parent().remove(new PathWrapper(keyPath, null));
            }

            if (pendingSelectDirectory != null && pendingSelectDirectory.equals(blueprintDirectory)) {
               pendingSelectDirectory = null;
            }

            if (currentDirectory != null && currentDirectory.equals(blueprintDirectory)) {
               currentDirectory = rootDirectory;
               unsetRecursiveSearch();
            }
         }
      }
   }

   private static void setCurrentDirectory(BlueprintDirectory structure) {
      if (currentDirectory == null || !currentDirectory.equals(structure)) {
         if (currentDirectory != null) {
            currentDirectory.search("", Set.of());
            searchString.set("");
         }

         currentDirectory = structure;
         unsetRecursiveSearch();
         structure.search("", filteredTags);
      }
   }

   private static void renderDirectoryOverview(BlueprintDirectory blueprintDirectory, String id) {
      int flags = 4096;
      if (blueprintDirectory.children().isEmpty()) {
         flags |= 512;
      } else if (currentDirectory != blueprintDirectory) {
         flags |= 128;
      }

      if (blueprintDirectory.equals(rootDirectory)) {
         flags |= 32;
      }

      id = id + blueprintDirectory.dirName();
      if (currentDirectory != null && currentDirectory.equals(blueprintDirectory)) {
         flags |= 1;
      }

      String formattedCount = NumberFormat.getInstance().format((long)blueprintDirectory.blueprints().size());
      boolean treeNodeOpen = ImGui.treeNodeEx(blueprintDirectory.dirName() + " (" + formattedCount + ")###DirOverview" + id, flags);
      dirStructureToggled = dirStructureToggled | ImGui.isItemToggledOpen();
      if (ImGui.isItemHovered() && ImGui.isMouseReleased(0) && (!dirStructureToggled || blueprintDirectory.children().isEmpty())) {
         pendingSelectDirectory = blueprintDirectory;
      }

      if (ImGui.beginDragDropTarget()) {
         BlueprintOrDirectory blueprintOrDirectory = (BlueprintOrDirectory)ImGui.acceptDragDropPayload("BlueprintOrDirectory", 3072);
         if (blueprintOrDirectory != null) {
            ImVec2 min = ImGui.getItemRectMin();
            ImVec2 max = ImGui.getItemRectMax();
            ImGui.getForegroundDrawList().addRect(min.x - 3.5F, min.y - 3.5F, max.x + 3.5F, max.y + 3.5F, ImGui.getColorU32(55), 0.0F, 0, 2.0F);
            if (ImGui.isMouseReleased(0)) {
               moveFileOperation = new BlueprintBrowserWindow.MoveFileOperation(currentDirectory, blueprintDirectory, blueprintOrDirectory);
            }
         }

         ImGui.endDragDropTarget();
      }

      if (treeNodeOpen) {
         for (BlueprintDirectory child : blueprintDirectory.children()) {
            renderDirectoryOverview(child, id);
         }

         ImGui.treePop();
      }
   }

   public static void render() {
      if (pendingServerBlueprintDownload != null) {
         if (pendingServerBlueprintResult == null) {
            if (!ImGui.isPopupOpen("###DownloadingModal")) {
               ImGui.openPopup("###DownloadingModal");
            }

            if (ImGuiHelper.beginPopupModal("###DownloadingModal", 325)) {
               ImGui.text(AxiomI18n.get("axiom.hardcoded.downloading_blueprint"));
               if (ImGui.button(AxiomI18n.get("axiom.hardcoded.cancel"))) {
                  pendingServerBlueprintDownload = null;
               }

               ImGui.endPopup();
            }

            return;
         }

         boolean success = callback.test(pendingServerBlueprintResult);
         if (!success) {
            callback = BlueprintBrowserWindow::defaultCallback;
            selectMultiple = false;
            callback.test(pendingServerBlueprintResult);
         }

         if ((!selectMultiple || !EditorUI.getIO().getKeyShift()) && !EditorWindowType.BLUEPRINT_BROWSER.isDocked()) {
            EditorWindowType.BLUEPRINT_BROWSER.setOpen(false);
         }

         pendingServerBlueprintDownload = null;
         pendingServerBlueprintResult = null;
      }

      if (pendingServerBlueprintResult != null) {
         pendingServerBlueprintResult.close();
         pendingServerBlueprintResult = null;
      }

      if (!EditorWindowType.BLUEPRINT_BROWSER.isOpen()) {
         selectMultiple = false;
         callback = BlueprintBrowserWindow::defaultCallback;
         popupBrowserBecauseJustOpened = true;
      } else {
         ImVec2 center = ImGui.getMainViewport().getCenter();
         if (popupBrowserBecauseJustOpened) {
            ImGui.setNextWindowSize(720.0F, 500.0F, 2);
            ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
            popupBrowserBecauseJustOpened = false;
         }

         if (makeActiveBecauseJustOpened) {
            ImGui.setNextWindowFocus();
            makeActiveBecauseJustOpened = false;
         }

         ImGui.setNextWindowSizeConstraints(510.0F, 350.0F, 5000.0F, 3000.0F);
         if (EditorUI.canProcessKeybinds) {
            if (ImGui.isMouseClicked(3)) {
               goBack();
            } else if (ImGui.isMouseClicked(4)) {
               goForwards();
            }
         }

         if (EditorWindowType.BLUEPRINT_BROWSER.begin("###OpenBlueprint", true)) {
            renderInner(center);
         }

         EditorWindowType.BLUEPRINT_BROWSER.end();
         if (!ImGui.isMouseDown(0)) {
            dirStructureToggled = false;
         }
      }
   }

   private static void goBack() {
      if (currentDirectory != null) {
         pendingSelectDirectory = currentDirectory.parent();
         forwardsQueue.add(0, currentDirectory);
      }
   }

   private static void goForwards() {
      if (!forwardsQueue.isEmpty()) {
         BlueprintDirectory head = forwardsQueue.remove(0);
         if (currentDirectory != null && !currentDirectory.children().contains(head)) {
            forwardsQueue.clear();
         } else {
            pendingSelectDirectory = head;
         }
      }
   }

   private static void renderInner(ImVec2 center) {
      renderedAtLeastOnce = true;
      AxiomConfig.SubcategoryBlueprint blueprintConfig = Axiom.configuration.blueprint;
      if (tooManyFileChangesCount >= 0) {
         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModal(AxiomI18n.get("axiom.editorui.window.blueprint_browser.overflow_files") + "###TooManyFileChangesPopup", 64)) {
            ImGui.pushTextWrapPos(600.0F);
            String message = AxiomI18n.get("axiom.editorui.window.blueprint_browser.overflow_files_message");
            if (tooManyFileChangesCount > 0) {
               message = message + " (" + NumberFormat.getInstance().format((long)tooManyFileChangesCount) + "+)";
            }

            ImGui.text(message);
            ImGui.popTextWrapPos();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.reload"))) {
               reloadDirectoryStructure = true;
               tooManyFileChangesCount = -1;
               ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.disable_auto_refresh"))) {
               tooManyFileChangesCount = -1;
            }

            EditorUI.consumeNavClose();
            ImGui.endPopup();
         } else {
            ImGui.openPopup("###TooManyFileChangesPopup");
         }

         moveFileOperation = null;
      } else if (noLongerExistsErrorFileName != null) {
         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModal(AxiomI18n.get("axiom.editorui.window.blueprint_browser.file_missing") + "###NoLongerExistsErrorPopup", 64)) {
            String osFileBrowser = switch (Util.getPlatform()) {
               case OSX -> AxiomI18n.get("axiom.editorui.window.blueprint_browser.file_browser_osx");
               case WINDOWS -> AxiomI18n.get("axiom.editorui.window.blueprint_browser.file_browser_win");
               default -> AxiomI18n.get("axiom.editorui.window.blueprint_browser.file_browser_other");
            };
            ImGui.pushTextWrapPos(600.0F);
            ImGui.text(AxiomI18n.get("axiom.editorui.window.blueprint_browser.file_missing_message1", noLongerExistsErrorFileName));
            ImGui.text(AxiomI18n.get("axiom.editorui.window.blueprint_browser.file_missing_message2", osFileBrowser));
            ImGui.popTextWrapPos();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.reload"))) {
               reloadDirectoryStructure = true;
               noLongerExistsErrorFileName = null;
               ImGui.closeCurrentPopup();
            }

            EditorUI.consumeNavClose();
            ImGui.endPopup();
         } else {
            ImGui.openPopup("###NoLongerExistsErrorPopup");
         }

         moveFileOperation = null;
      } else if (editBlueprintData != null) {
         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         String editBlueprint = AxiomI18n.get("axiom.editorui.window.blueprint_browser.edit_blueprint");
         if (!ImGui.isPopupOpen(editBlueprint + "###EditBlueprintPopup")) {
            ImGui.openPopup("###EditBlueprintPopup");
         }

         if (ImGuiHelper.beginPopupModalCloseable(editBlueprint + "###EditBlueprintPopup", 64)) {
            editBlueprintPopup();
            ImGuiHelper.endPopupModalCloseable();
         }

         if (!ImGui.isPopupOpen(editBlueprint + "###EditBlueprintPopup") && editBlueprintData != null) {
            editBlueprintData.close();
            editBlueprintData = null;
         }
      } else if (displayedException != null) {
         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         if (!ImGui.isPopupOpen("###BrowserErrorPopup")) {
            ImGui.openPopup("###BrowserErrorPopup");
         }

         if (ImGuiHelper.beginPopupModalCloseable(displayedException.getClass().getSimpleName() + "###BrowserErrorPopup", 64)) {
            ImGui.pushTextWrapPos(600.0F);
            ImGui.text(displayedException.getMessage());
            ImGui.popTextWrapPos();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.close"))) {
               displayedException = null;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         if (!ImGui.isPopupOpen("###BrowserErrorPopup")) {
            displayedException = null;
         }
      } else if (moveFileOperation != null && moveFileOperation.pendingOverwriteConfirmation) {
         if (!ImGui.isPopupOpen("###BrowserOverwritePopup")) {
            ImGui.openPopup("###BrowserOverwritePopup");
         }

         ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
         if (ImGuiHelper.beginPopupModalCloseable(AxiomI18n.get("axiom.editorui.window.blueprint_browser.move_file") + "###BrowserOverwritePopup", 64)) {
            ImGui.pushTextWrapPos(600.0F);
            ImGui.text(AxiomI18n.get("axiom.editorui.window.blueprint_browser.move_file_already_exists", moveFileOperation.file.path().getFileName()));
            ImGui.popTextWrapPos();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.move_file_overwrite"))) {
               moveFileOperation.overwriteConfirmation = true;
            }

            ImGui.sameLine();
            if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.close"))) {
               moveFileOperation = null;
               ImGui.closeCurrentPopup();
            }

            ImGuiHelper.endPopupModalCloseable();
         }

         if (!ImGui.isPopupOpen("###BrowserOverwritePopup") && moveFileOperation != null) {
            moveFileOperation = null;
         }
      }

      ImGui.beginGroup();
      float uiScale = EditorUI.getUiScale();
      float contentRegionAvailX = ImGui.getContentRegionAvailX();
      float minDirectorySize = (int)Math.ceil(contentRegionAvailX / 10.0F);
      float maxDirectorySize = (int)Math.ceil(contentRegionAvailX / 2.0F);
      if (draggingDirListDividerStartMouseX >= 0.0F) {
         if (!ImGui.isAnyMouseDown()) {
            draggingDirListDividerStartMouseX = -1.0F;
         } else {
            ImGui.setMouseCursor(4);
            float mouseX = EditorUI.getIO().getMousePosX();
            float deltaX = mouseX - draggingDirListDividerStartMouseX;
            float newDirWidth = draggingDirListDividerStartDirWidth * uiScale + deltaX;
            newDirWidth = Math.max(minDirectorySize, Math.min(maxDirectorySize, newDirWidth));
            Axiom.configuration.internal.blueprintBrowserDirSize = (int)Math.ceil(newDirWidth / uiScale);
         }
      }

      float directorySize = Math.max(minDirectorySize, Math.min(maxDirectorySize, uiScale * Axiom.configuration.internal.blueprintBrowserDirSize));
      if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.reload"), (int)Math.ceil(directorySize), 0.0F)) {
         reloadDirectoryStructure = true;
      }

      float halfButtonSize = (directorySize - ImGui.getStyle().getItemSpacingX()) / 2.0F;
      boolean backDisabled = currentDirectory == null || currentDirectory.parent() == null;
      if (backDisabled) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.go_back"), (int)Math.floor(halfButtonSize), 0.0F)) {
         goBack();
      }

      if (backDisabled) {
         ImGui.endDisabled();
      }

      ImGui.sameLine();
      boolean forwardsDisabled = forwardsQueue.isEmpty();
      if (forwardsDisabled) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.go_forwards"), (int)Math.ceil(halfButtonSize), 0.0F)) {
         goForwards();
      }

      if (forwardsDisabled) {
         ImGui.endDisabled();
      }

      float bottomBarHeight = ImGui.getFrameHeightWithSpacing() + ImGui.getStyle().getItemSpacingY();
      if (ImGui.beginChild("DirList", directorySize, ImGui.getContentRegionAvailY() - bottomBarHeight, false, 2048)) {
         if (rootDirectory != null) {
            renderDirectoryOverview(rootDirectory, "");
         }

         if (rootServerBlueprintDirectory != null) {
            renderDirectoryOverview(rootServerBlueprintDirectory, "");
         } else if (!Authorization.hasCommercialLicense() && !Minecraft.getInstance().hasSingleplayerServer()) {
            int flags = 4608;
            if (ImGui.treeNodeEx(AxiomI18n.get("axiom.hardcoded.server_blueprints"), flags)) {
               ImGui.treePop();
            }

            ImGuiHelper.tooltip("Server Blueprints are a Commercial License feature. Click for more information");
            ImGuiHelper.openCommercialLicenseOnClick();
         }
      }

      ImGui.endChild();
      ImGui.endGroup();
      ImGui.sameLine();
      if (draggingDirListDividerStartMouseX < 0.0F) {
         ImVec2 cursorPos = ImGui.getCursorScreenPos();
         float spacingX = ImGui.getStyle().getItemSpacingX();
         float mouseX = EditorUI.getIO().getMousePosX();
         ImGui.setCursorScreenPos(cursorPos.x - spacingX, cursorPos.y);
         ImGui.invisibleButton("##DirListDrag", spacingX, ImGui.getContentRegionAvailY() - bottomBarHeight);
         if (ImGui.isItemClicked()) {
            ImGui.setMouseCursor(4);
            draggingDirListDividerStartMouseX = mouseX;
            draggingDirListDividerStartDirWidth = directorySize / uiScale;
         } else if (ImGui.isItemHovered()) {
            ImGui.setMouseCursor(4);
         }

         ImGui.setCursorScreenPos(cursorPos.x, cursorPos.y);
      }

      ImGui.beginGroup();
      float tagFilterDropdownSize = ImGui.getContentRegionAvailX() * 0.75F;
      ImGui.setNextItemWidth(ImGui.getContentRegionAvailX() * 0.25F);
      if (ImGui.inputText("##Search", searchString)) {
         updateSearch();
      }

      if (currentDirectory != null) {
         Map<String, Integer> tagsWithCount = currentDirectory.tagsWithCount();
         boolean broke = false;

         for (String tag : filteredTags) {
            if (!tagsWithCount.containsKey(tag)) {
               ImGui.sameLine();
               if (renderTagFilterButton(tag, 0)) {
                  broke = true;
                  break;
               }
            }
         }

         if (!broke) {
            List<Entry<String, Integer>> list = new ArrayList<>(tagsWithCount.entrySet());
            list.sort(Comparator.comparingInt(entry -> -entry.getValue()));

            for (Entry<String, Integer> tagx : list) {
               ImGui.sameLine();
               if (renderTagFilterButton(tagx.getKey(), tagx.getValue())) {
                  broke = true;
                  break;
               }
            }
         }

         ImVec2 buttonPos = ImGui.getCursorScreenPos();
         if (broke && ImGui.arrowButton("##TagFilterDropdownButton", 3)) {
            ImGui.openPopup("##TagFilterDropdown");
         }

         float frameHeight = ImGui.getFrameHeight();
         ImGui.setNextWindowPos(buttonPos.x + frameHeight, buttonPos.y + frameHeight, 1, 1.0F, 0.0F);
         if (ImGuiHelper.beginPopup("##TagFilterDropdown")) {
            renderTagFilterDropdown(tagFilterDropdownSize, tagsWithCount);
            ImGui.endPopup();
         }
      }

      if (ImGui.beginChild("Browser", 0.0F, ImGui.getContentRegionAvailY() - bottomBarHeight)) {
         final int countPerRow = Math.max(
            1, (int)Math.floor((ImGui.getContentRegionAvailX() + ImGui.getStyle().getItemSpacingX() + 2.0F) / (100.0F + ImGui.getStyle().getItemSpacingX()))
         );
         if (ImGui.isWindowHovered() && recursiveSearchInCurrentDirectory == null && ImGui.isMouseClicked(1)) {
            ImGui.openPopup("##BrowserCtxMenu");
         }

         if (currentDirectory != null
            && currentDirectory.path().real() != null
            && recursiveSearchInCurrentDirectory == null
            && ImGuiHelper.beginPopup("##BrowserCtxMenu")) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.add_folder"))) {
               Path currentDir = currentDirectory.path().real();
               int i = 0;

               while (true) {
                  String filename;
                  if (i == 0) {
                     filename = AxiomI18n.get("axiom.editorui.window.blueprint_browser.new_folder");
                  } else {
                     filename = AxiomI18n.get("axiom.editorui.window.blueprint_browser.new_folder_i", Integer.toString(i));
                  }

                  Path folderPath = currentDir.resolve(filename);

                  try {
                     Files.createDirectory(folderPath);
                     updatedBlueprintPaths.add(folderPath);
                     break;
                  } catch (FileAlreadyExistsException var18) {
                     i++;
                  } catch (IOException var19) {
                     var19.printStackTrace();
                     break;
                  }
               }
            }

            if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.window.blueprint_browser.sort_by"))) {
               if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.sort_by.name"))) {
                  currentDirectory.sort(BlueprintDirectory.SortMode.NAME);
               }

               if (ImGui.beginMenu(AxiomI18n.get("axiom.editorui.window.blueprint_browser.sort_by.block_count"))) {
                  if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.sort_by.ascending"))) {
                     currentDirectory.sort(BlueprintDirectory.SortMode.BLOCK_COUNT_ASC);
                  }

                  if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.sort_by.descending"))) {
                     currentDirectory.sort(BlueprintDirectory.SortMode.BLOCK_COUNT_DESC);
                  }

                  ImGui.endMenu();
               }

               ImGui.endMenu();
            }

            ImGui.endPopup();
         }

         if (recursiveSearchInCurrentDirectory != null) {
            int vcount = (int)Math.ceil((float)recursiveSearchInCurrentDirectory.size() / countPerRow);
            float itemHeightWithSpacing = 100.0F + ImGui.getTextLineHeightWithSpacing() * 2.0F + 7.0F + ImGui.getStyle().getItemSpacingY();
            ImGuiListClipper.forEach(vcount, (int)itemHeightWithSpacing, new ImListClipperCallback() {
               public void accept(int index) {
                  int from = index * countPerRow;
                  int to = Math.min(from + countPerRow, BlueprintBrowserWindow.recursiveSearchInCurrentDirectory.size());

                  for (int i = from; i < to; i++) {
                     BlueprintOrDirectory blueprintOrDirectory = BlueprintBrowserWindow.recursiveSearchInCurrentDirectory.get(i);
                     if (blueprintOrDirectory instanceof BlueprintOrDirectory.Dir dir) {
                        ImGui.pushID(i);
                        BlueprintBrowserWindow.renderBlueprintDirectory(dir, i, i == to - 1);
                        ImGui.popID();
                     } else if (blueprintOrDirectory instanceof BlueprintOrDirectory.Bp bp) {
                        ImGui.pushID(i);
                        BlueprintBrowserWindow.renderBlueprintLoaded(bp, i, i == to - 1);
                        ImGui.popID();
                     }

                     if (i < to - 1) {
                        ImGui.sameLine();
                     }
                  }
               }
            });
         } else if (currentDirectory != null && !currentDirectory.searchedBlueprints().isEmpty()) {
            int vcount = (int)Math.ceil((float)currentDirectory.searchedBlueprints().size() / countPerRow);
            float itemHeightWithSpacing = 100.0F + ImGui.getTextLineHeightWithSpacing() * 2.0F + 7.0F + ImGui.getStyle().getItemSpacingY();
            ImGuiListClipper.forEach(vcount, (int)itemHeightWithSpacing, new ImListClipperCallback() {
               private BlueprintOrDirectory currentBlueprintOrDirectory = null;
               private boolean ended = false;

               public void accept(int index) {
                  if (!this.ended) {
                     int from = index * countPerRow;
                     int to = Math.min(from + countPerRow, BlueprintBrowserWindow.currentDirectory.searchedBlueprints().size());
                     if (this.currentBlueprintOrDirectory == null) {
                        this.currentBlueprintOrDirectory = BlueprintBrowserWindow.currentDirectory.setSearchVisibleStart(from);
                        if (this.currentBlueprintOrDirectory == null) {
                           this.ended = true;
                           return;
                        }
                     }

                     for (int i = from; i < to; i++) {
                        BlueprintOrDirectory blueprintOrDirectory = this.currentBlueprintOrDirectory;
                        if (blueprintOrDirectory instanceof BlueprintOrDirectory.Dir dir) {
                           ImGui.pushID(i);
                           BlueprintBrowserWindow.renderBlueprintDirectory(dir, i, i == to - 1);
                           ImGui.popID();
                        } else if (blueprintOrDirectory instanceof BlueprintOrDirectory.Bp bp) {
                           ImGui.pushID(i);
                           BlueprintBrowserWindow.renderBlueprintLoaded(bp, i, i == to - 1);
                           ImGui.popID();
                        }

                        this.currentBlueprintOrDirectory = blueprintOrDirectory.nextSearchNode;
                        if (this.currentBlueprintOrDirectory == null) {
                           this.ended = true;
                           return;
                        }

                        if (i < to - 1) {
                           ImGui.sameLine();
                        }
                     }
                  }
               }
            });
         }
      }

      ImGui.endChild();
      ImGui.endGroup();
      ImGui.separator();
      if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.open_folder"))) {
         Path rootPath = Axiom.getInstance().getBlueprintDirectory();
         Util.getPlatform().openUri(rootPath.toUri());
      }

      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.blueprint_browser.automatically_refresh"), blueprintConfig.automaticRefreshing)) {
         blueprintConfig.automaticRefreshing = !blueprintConfig.automaticRefreshing;
      }

      ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.window.blueprint_browser.automatically_refresh.description"));
      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.blueprint_browser.search_inside_folders"), blueprintConfig.recursiveSearch)) {
         blueprintConfig.recursiveSearch = !blueprintConfig.recursiveSearch;
         updateSearch();
      }

      ImGuiHelper.tooltip(AxiomI18n.get("axiom.editorui.window.blueprint_browser.search_inside_folders.description"));
      ImGui.sameLine();
   }

   private static void renderTagFilterDropdown(float width, Map<String, Integer> tagsWithCount) {
      record Tag(String tag, String tagWithCount) {
      }

      List<Tag> tags = new ArrayList<>();

      for (String tag : filteredTags) {
         if (!tagsWithCount.containsKey(tag)) {
            tags.add(new Tag(tag, tag + " (0)"));
         }
      }

      for (Entry<String, Integer> tagx : tagsWithCount.entrySet()) {
         tags.add(new Tag(tagx.getKey(), tagx.getKey() + " (" + tagx.getValue() + ")"));
      }

      int minLines = 1;
      int maxLines = 8;
      float itemSpacingX = ImGui.getStyle().getItemSpacingX();
      float framePaddingX = ImGui.getStyle().getFramePaddingX();
      float availableSpace = width;
      int lines = 0;
      boolean scrollbar = false;
      if (minLines == maxLines) {
         lines = minLines;
         scrollbar = true;
      } else {
         float consumedWidth = 0.0F;

         for (Tag tagx : tags) {
            float elementWidth = framePaddingX * 2.0F + ImGuiHelper.calcTextWidth(tagx.tagWithCount);
            if (consumedWidth + elementWidth < availableSpace && consumedWidth > 0.0F) {
               consumedWidth += elementWidth + itemSpacingX;
            } else {
               consumedWidth = elementWidth + itemSpacingX;
               if (++lines > maxLines) {
                  lines = maxLines;
                  scrollbar = true;
                  break;
               }
            }
         }

         if (lines < minLines) {
            lines = minLines;
         }
      }

      float lineHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0F + ImGui.getStyle().getItemSpacingY();
      if (ImGui.beginChild("##TagFilterDropdownScroller", width, lineHeight * lines - ImGui.getStyle().getItemSpacingY())) {
         if (scrollbar) {
            availableSpace -= ImGui.getStyle().getScrollbarSize();
         }

         boolean disabledAlpha = false;
         float consumedWidth = 0.0F;

         for (Tag tagxx : tags) {
            float elementWidth = framePaddingX * 2.0F + ImGuiHelper.calcTextWidth(tagxx.tagWithCount);
            if (consumedWidth + elementWidth < availableSpace && consumedWidth > 0.0F) {
               ImGui.sameLine();
               consumedWidth += elementWidth + itemSpacingX;
            } else {
               consumedWidth = elementWidth + itemSpacingX;
            }

            boolean contains = filteredTags.contains(tagxx.tag);
            if (contains != disabledAlpha) {
               disabledAlpha = contains;
               if (contains) {
                  ImGuiHelper.pushStyleVar(0, ImGui.getStyle().getAlpha() * ImGui.getStyle().getDisabledAlpha());
               } else {
                  ImGuiHelper.popStyleVar();
               }
            }

            if (ImGui.button(tagxx.tagWithCount + "##" + tagxx.tag)) {
               if (contains) {
                  filteredTags.remove(tagxx.tag);
               } else {
                  filteredTags.add(tagxx.tag);
               }

               filteredTagsChanged = true;
            }
         }

         if (disabledAlpha) {
            ImGuiHelper.popStyleVar();
         }
      }

      ImGui.endChild();
   }

   private static boolean renderTagFilterButton(String tag, int count) {
      String display = tag + " (" + count + ")";
      float available = ImGui.getContentRegionAvailX();
      float needed = ImGui.getStyle().getFramePaddingX() * 2.0F
         + ImGuiHelper.calcTextWidth(display)
         + ImGui.getStyle().getItemSpacingX()
         + ImGui.getFrameHeight();
      if (available < needed) {
         return true;
      } else {
         boolean contains = filteredTags.contains(tag);
         if (contains) {
            ImGuiHelper.pushStyleVar(0, ImGui.getStyle().getAlpha() * ImGui.getStyle().getDisabledAlpha());
         }

         if (ImGui.button(display + "##" + tag)) {
            if (contains) {
               filteredTags.remove(tag);
            } else {
               filteredTags.add(tag);
            }

            filteredTagsChanged = true;
         }

         if (contains) {
            ImGuiHelper.popStyleVar();
         }

         return false;
      }
   }

   private static void editBlueprintPopup() {
      if (!editBlueprintData.previewLocked && editBlueprintData.blockRegion.count() < 16777216) {
         ImGui.image(editBlueprintData.blueprintPreview.render(512, true, true), 256.0F, 256.0F, 0.0F, 1.0F, 1.0F, 0.0F);
         if (editBlueprintData.isRotating) {
            if (!ImGui.isMouseDown(0)) {
               editBlueprintData.isRotating = false;
               editBlueprintData.blueprintPreview.mouseReleased();
            } else {
               float mouseX = ImGui.getMousePosX();
               float mouseY = ImGui.getMousePosY();
               editBlueprintData.blueprintPreview
                  .mouseMoved(mouseX - editBlueprintData.lastMouseX, mouseY - editBlueprintData.lastMouseY, EditorUI.isCtrlOrCmdDown());
               editBlueprintData.lastMouseX = mouseX;
               editBlueprintData.lastMouseY = mouseY;
            }
         } else if (ImGui.isItemClicked(0)) {
            editBlueprintData.isRotating = true;
            editBlueprintData.lastMouseX = ImGui.getMousePosX();
            editBlueprintData.lastMouseY = ImGui.getMousePosY();
         }
      }

      ImGui.inputText(AxiomI18n.get("axiom.editorui.window.create_blueprint.name"), editBlueprintData.blueprintName);
      ImGui.inputText(AxiomI18n.get("axiom.editorui.window.create_blueprint.author"), editBlueprintData.authorName);
      editBlueprintData.tagListWidget.render(256);
      if (!editBlueprintData.previewLocked) {
         float[] angleArray = new float[]{editBlueprintData.blueprintPreview.getYaw(), editBlueprintData.blueprintPreview.getPitch()};
         if (ImGui.inputFloat2(AxiomI18n.get("axiom.editorui.window.create_blueprint.angle"), angleArray, "%.2f")) {
            editBlueprintData.blueprintPreview.setYaw(angleArray[0], false);
            editBlueprintData.blueprintPreview.setPitch(angleArray[1], false);
         }
      }

      if (ImGui.button(AxiomI18n.get("axiom.editorui.window.create_blueprint.save"))) {
         if (editBlueprintData.previewImageFuture != null) {
            editBlueprintData.previewImageFuture.thenAccept(NativeImage::close);
         }

         if (editBlueprintData.blockRegion.count() < 16777216) {
            editBlueprintData.blueprintPreview.render(960, false, false);
            editBlueprintData.previewImageFuture = editBlueprintData.blueprintPreview.toNativeImage(96, true);
         } else {
            editBlueprintData.previewImageFuture = CompletableFuture.completedFuture(new NativeImage(96, 96, true));
         }
      }

      if (editBlueprintData.previewImageFuture != null && editBlueprintData.previewImageFuture.isDone()) {
         BlueprintHeader header = new BlueprintHeader(
            ImGuiHelper.getString(editBlueprintData.blueprintName).trim(),
            ImGuiHelper.getString(editBlueprintData.authorName).trim(),
            editBlueprintData.tagListWidget.tags(),
            editBlueprintData.blueprintPreview.getYaw(),
            editBlueprintData.blueprintPreview.getPitch(),
            false,
            editBlueprintData.blockRegion.count(),
            editBlueprintData.containsAir
         );
         updatedBlueprintPaths.add(editBlueprintData.path);
         NativeImage nativeImage = editBlueprintData.previewImageFuture.join();
         editBlueprintData.previewImageFuture = null;

         try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(editBlueprintData.path))) {
            BlueprintIo.write(outputStream, header, nativeImage, editBlueprintData.blockRegion, editBlueprintData.blockEntities, editBlueprintData.entities);
         } catch (IOException var7) {
            var7.printStackTrace();
         }

         nativeImage.close();
         ImGui.closeCurrentPopup();
         editBlueprintData.close();
         editBlueprintData = null;
      }

      ImGui.sameLine();
      if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
         ImGui.closeCurrentPopup();
         editBlueprintData.close();
         editBlueprintData = null;
      }
   }

   private static void renderBlueprintDirectory(BlueprintOrDirectory.Dir directory, int index, boolean last) {
      ImVec2 textSize = new ImVec2();
      ImGui.calcTextSize(textSize, directory.blueprintDirectory.dirName(), 100.0F);
      float childHeight = 100.0F + ImGui.getTextLineHeightWithSpacing() * 2.0F + 7.0F;
      boolean openCtxMenu = false;
      ImVec2 startCursorPos = ImGui.getCursorScreenPos();
      float itemSpacingX = ImGui.getStyle().getItemSpacingX();
      ImGui.setCursorScreenPos(startCursorPos.x - itemSpacingX, startCursorPos.y);
      ImGui.dummy(itemSpacingX + 20.0F, childHeight);
      if (ImGui.beginDragDropTarget()) {
         BlueprintOrDirectory blueprintOrDirectory = (BlueprintOrDirectory)ImGui.acceptDragDropPayload("BlueprintOrDirectory", 3072);
         if (blueprintOrDirectory != null && directory.prevNode != blueprintOrDirectory && directory != blueprintOrDirectory) {
            float lineX = startCursorPos.x - ImGui.getStyle().getItemSpacingX() / 2.0F;
            ImGui.getForegroundDrawList().addRectFilled(lineX - 1.0F, startCursorPos.y, lineX + 1.0F, startCursorPos.y + 100.0F + 3.0F, ImGui.getColorU32(55));
            if (ImGui.isMouseReleased(0)) {
               moveFileOperation = new BlueprintBrowserWindow.MoveFileOperation(currentDirectory, currentDirectory, blueprintOrDirectory);
               moveFileOperation.repositionBefore = true;
               moveFileOperation.repositionReference = directory;
            }
         }

         ImGui.endDragDropTarget();
      }

      ImGui.setCursorScreenPos(startCursorPos.x + 80.0F, startCursorPos.y);
      ImGui.dummy(last ? ImGui.getContentRegionAvailX() : 20.0F, childHeight);
      if (ImGui.beginDragDropTarget()) {
         BlueprintOrDirectory blueprintOrDirectory = (BlueprintOrDirectory)ImGui.acceptDragDropPayload("BlueprintOrDirectory", 3072);
         if (blueprintOrDirectory != null && directory.nextNode != blueprintOrDirectory && directory != blueprintOrDirectory) {
            float lineX = startCursorPos.x + 100.0F + ImGui.getStyle().getItemSpacingX() / 2.0F;
            ImGui.getForegroundDrawList().addRectFilled(lineX - 1.0F, startCursorPos.y, lineX + 1.0F, startCursorPos.y + 100.0F + 3.0F, ImGui.getColorU32(55));
            if (ImGui.isMouseReleased(0)) {
               moveFileOperation = new BlueprintBrowserWindow.MoveFileOperation(currentDirectory, currentDirectory, blueprintOrDirectory);
               moveFileOperation.repositionBefore = false;
               moveFileOperation.repositionReference = directory;
            }
         }

         ImGui.endDragDropTarget();
      }

      ImGui.setCursorScreenPos(startCursorPos.x, startCursorPos.y);
      if (ImGui.beginChild("", 100.0F, childHeight, false, 59)) {
         ImVec2 pos = ImGui.getCursorScreenPos();
         ImGuiHelper.pushStyleVar(11, 0.0F, 0.0F);
         ImVec2 buttonStartPos = ImGui.getCursorScreenPos();
         ImGui.setCursorScreenPos(buttonStartPos.x + 20.0F, buttonStartPos.y);
         ImGui.dummy(60.0F, 100.0F);
         if (ImGui.beginDragDropTarget()) {
            BlueprintOrDirectory blueprintOrDirectory = (BlueprintOrDirectory)ImGui.acceptDragDropPayload("BlueprintOrDirectory", 3072);
            if (blueprintOrDirectory != null && directory != blueprintOrDirectory) {
               ImVec2 min = ImGui.getItemRectMin();
               ImVec2 max = ImGui.getItemRectMax();
               ImGui.getForegroundDrawList()
                  .addRect(min.x - 3.5F - 20.0F, min.y - 3.5F, max.x + 3.5F + 20.0F, max.y + 3.5F, ImGui.getColorU32(55), 0.0F, 0, 2.0F);
               if (ImGui.isMouseReleased(0)) {
                  moveFileOperation = new BlueprintBrowserWindow.MoveFileOperation(currentDirectory, directory.blueprintDirectory, blueprintOrDirectory);
               }
            }

            ImGui.endDragDropTarget();
         }

         ImGui.setCursorScreenPos(buttonStartPos.x, buttonStartPos.y);
         ImGui.pushFont(EditorUI.icons, EditorUI.icons.getLegacySize());
         if (ImGui.button("\ue908", 100.0F, 100.0F)) {
            pendingSelectDirectory = directory.blueprintDirectory;
         }

         if (directory.blueprintDirectory.head() instanceof BlueprintOrDirectory.Bp head) {
            BlueprintBrowserWindow.Thumbnail thumbnail = loadedBlueprintThumbnails.get(head.path());
            if (thumbnail != null) {
               DynamicTexture texture = thumbnail.getTextureOrMissing();
               if (texture != null) {
                  ImGui.getWindowDrawList()
                     .addImage(
                        new AxiomGpuTexture(texture.getId()).glId(), buttonStartPos.x, buttonStartPos.y + 100.0F, buttonStartPos.x + 100.0F, buttonStartPos.y
                     );
               }

               ImGui.getWindowDrawList()
                  .addRectFilled(buttonStartPos.x + 17.0F, buttonStartPos.y + 33.0F, buttonStartPos.x + 83.0F, buttonStartPos.y + 81.0F, -1, 5);
            }
         }

         openCtxMenu = ImGui.isItemClicked(1);
         if (ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload("BlueprintOrDirectory", directory);
            ImGui.text("\ue908");
            ImGui.endDragDropSource();
         }

         ImGui.popFont();
         String countText = NumberFormat.getInstance().format((long)directory.blueprintDirectory.blueprints().size());
         ImVec2 countTextSize = new ImVec2();
         ImGui.calcTextSize(countTextSize, countText);
         ImGui.getWindowDrawList().addText(pos.x + 50.0F - countTextSize.x / 2.0F, pos.y + 5.0F + 50.0F - countTextSize.y / 2.0F, -9155541, countText);
         ImVec2 cursorPos = ImGui.getCursorPos();
         ImGui.setCursorPos(cursorPos.x + (100.0F - textSize.x) / 2.0F, cursorPos.y);
         ImGui.pushTextWrapPos(100.0F);
         ImGui.text(directory.blueprintDirectory.dirName());
         ImGui.popTextWrapPos();
         ImGuiHelper.popStyleVar();
      }

      ImGui.endChild();
      if (directory.path().real() != null) {
         Path path = directory.path().real();
         boolean openRename = false;
         if (openCtxMenu) {
            ImGui.openPopup("###BlueprintCtxMenu" + index);
         }

         if (ImGuiHelper.beginPopup("###BlueprintCtxMenu" + index)) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.rename_file"))) {
               openRename = true;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.delete"))) {
               try {
                  Files.delete(path);
                  BlueprintDirectory parent = dirStructureMap.get(path.getParent());
                  if (parent != null) {
                     parent.remove(directory.path());
                  }
               } catch (NoSuchFileException | FileNotFoundException var13) {
                  noLongerExistsErrorFileName = directory.path().getFileName();
               } catch (Exception var14) {
                  displayedException = var14;
                  var14.printStackTrace();
               }
            }

            ImGui.endPopup();
         }

         renameContextMenu(directory, index, openRename);
      }
   }

   private static void renderBlueprintLoaded(BlueprintOrDirectory.Bp blueprint, int index, boolean last) {
      boolean fileBasedBlueprint = blueprint.path().real() != null;
      String textToRender = blueprint.blueprint.name().trim();
      if (textToRender.isBlank()) {
         textToRender = AxiomI18n.get("axiom.editorui.window.blueprint_browser.unnamed_blueprint");
      }

      ImVec2 textSize = new ImVec2();
      ImGui.calcTextSize(textSize, textToRender, 100.0F);
      float childHeight = 100.0F + ImGui.getTextLineHeightWithSpacing() * 2.0F + 7.0F;
      boolean openCtxMenu = false;
      if (fileBasedBlueprint) {
         ImVec2 startCursorPos = ImGui.getCursorScreenPos();
         float itemSpacingX = ImGui.getStyle().getItemSpacingX();
         ImGui.setCursorScreenPos(startCursorPos.x - itemSpacingX, startCursorPos.y);
         ImGui.dummy(itemSpacingX + 50.0F, childHeight);
         if (ImGui.beginDragDropTarget()) {
            BlueprintOrDirectory blueprintOrDirectory = (BlueprintOrDirectory)ImGui.acceptDragDropPayload("BlueprintOrDirectory", 3072);
            if (blueprintOrDirectory != null && blueprint.prevNode != blueprintOrDirectory && blueprint != blueprintOrDirectory) {
               float lineX = startCursorPos.x - ImGui.getStyle().getItemSpacingX() / 2.0F;
               ImGui.getForegroundDrawList()
                  .addRectFilled(lineX - 1.0F, startCursorPos.y, lineX + 1.0F, startCursorPos.y + 100.0F + 3.0F, ImGui.getColorU32(55));
               if (ImGui.isMouseReleased(0)) {
                  moveFileOperation = new BlueprintBrowserWindow.MoveFileOperation(currentDirectory, currentDirectory, blueprintOrDirectory);
                  moveFileOperation.repositionBefore = true;
                  moveFileOperation.repositionReference = blueprint;
               }
            }

            ImGui.endDragDropTarget();
         }

         ImGui.setCursorScreenPos(startCursorPos.x + 50.0F, startCursorPos.y);
         ImGui.dummy(last ? ImGui.getContentRegionAvailX() : 50.0F, childHeight);
         if (ImGui.beginDragDropTarget()) {
            BlueprintOrDirectory blueprintOrDirectory = (BlueprintOrDirectory)ImGui.acceptDragDropPayload("BlueprintOrDirectory", 3072);
            if (blueprintOrDirectory != null && blueprint.nextNode != blueprintOrDirectory && blueprint != blueprintOrDirectory) {
               float lineX = startCursorPos.x + 100.0F + ImGui.getStyle().getItemSpacingX() / 2.0F;
               ImGui.getForegroundDrawList()
                  .addRectFilled(lineX - 1.0F, startCursorPos.y, lineX + 1.0F, startCursorPos.y + 100.0F + 3.0F, ImGui.getColorU32(55));
               if (ImGui.isMouseReleased(0)) {
                  moveFileOperation = new BlueprintBrowserWindow.MoveFileOperation(currentDirectory, currentDirectory, blueprintOrDirectory);
                  moveFileOperation.repositionBefore = false;
                  moveFileOperation.repositionReference = blueprint;
               }
            }

            ImGui.endDragDropTarget();
         }

         ImGui.setCursorScreenPos(startCursorPos.x, startCursorPos.y);
      }

      if (ImGui.beginChild("", 100.0F, childHeight, false, 59)) {
         ImGuiHelper.pushStyleVar(11, 0.0F, 0.0F);
         BlueprintBrowserWindow.Thumbnail thumbnail = loadedBlueprintThumbnails.get(blueprint.path());
         DynamicTexture texture = thumbnail == null ? null : thumbnail.getTextureOrMissing();
         boolean openBlueprint;
         if (texture != null) {
            ImGui.pushStyleVar(11, 2.0F, 2.0F);
            openBlueprint = ImGui.imageButton("##OpenBlueprint", new AxiomGpuTexture(texture.getId()).glId(), 96.0F, 96.0F, 0.0F, 1.0F, 1.0F, 0.0F);
            ImGui.popStyleVar();
         } else {
            openBlueprint = ImGui.button("##OpenBlueprintFallback", 100.0F, 100.0F);
         }

         openCtxMenu = ImGui.isItemClicked(1);
         if (fileBasedBlueprint && ImGui.beginDragDropSource()) {
            ImGui.setDragDropPayload("BlueprintOrDirectory", blueprint);
            if (texture != null) {
               ImGui.image(new AxiomGpuTexture(texture.getId()).glId(), 100.0F, 100.0F, 0.0F, 1.0F, 1.0F, 0.0F);
            }

            ImGui.endDragDropSource();
         }

         if (openBlueprint) {
            if (fileBasedBlueprint) {
               try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(blueprint.path().real()))) {
                  Blueprint fullBlueprint = BlueprintIo.readBlueprint(inputStream);
                  if (fullBlueprint.blockRegion().isEmpty()) {
                     ChatUtils.error("Failed to load blueprint: empty");
                     fullBlueprint.close();
                  } else {
                     boolean success = callback.test(fullBlueprint);
                     if (!success) {
                        callback = BlueprintBrowserWindow::defaultCallback;
                        selectMultiple = false;
                        callback.test(fullBlueprint);
                     }
                  }

                  if ((!selectMultiple || !EditorUI.getIO().getKeyShift()) && !EditorWindowType.BLUEPRINT_BROWSER.isDocked()) {
                     EditorWindowType.BLUEPRINT_BROWSER.setOpen(false);
                  }
               } catch (NoSuchFileException | FileNotFoundException var27) {
                  noLongerExistsErrorFileName = blueprint.path().getFileName();
               } catch (IOException var28) {
                  var28.printStackTrace();
               }
            } else if (Axiom.getInstance().serverConfig == null || Axiom.getInstance().serverConfig.blueprintVersion() != 2) {
               ChatUtils.error(
                  "Unable to request blueprint since server is running a version with a different blueprint format. Please ensure both the client & server are running the most up-to-date versions of Axiom"
               );
            } else if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.BLUEPRINT_REQUEST)) {
               ChatUtils.error("Server has not given you permission to download blueprints");
            } else {
               pendingServerBlueprintDownload = blueprint.path().fakePath();
               if (pendingServerBlueprintResult != null) {
                  pendingServerBlueprintResult.close();
                  pendingServerBlueprintResult = null;
               }

               new AxiomServerboundRequestBlueprint(blueprint.path().fakePath()).send();
            }
         }

         ImVec2 cursorPos = ImGui.getCursorPos();
         ImGui.setCursorPos(cursorPos.x + (100.0F - textSize.x) / 2.0F, cursorPos.y);
         ImGui.pushTextWrapPos(100.0F);
         ImGui.text(textToRender);
         ImGui.popTextWrapPos();
         if (textSize.y > ImGui.getTextLineHeight() + ImGui.getTextLineHeightWithSpacing() && ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.pushTextWrapPos(ImGui.getFontSize() * 20.0F);
            ImGui.textUnformatted(textToRender);
            ImGui.popTextWrapPos();
            ImGui.endTooltip();
         }

         ImGuiHelper.popStyleVar();
      }

      ImGui.endChild();
      if (fileBasedBlueprint) {
         Path path = blueprint.path().real();
         boolean openRename = false;
         if (openCtxMenu) {
            ImGui.openPopup("###BlueprintCtxMenu" + index);
         }

         if (ImGuiHelper.beginPopup("###BlueprintCtxMenu" + index)) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.rename_file"))) {
               openRename = true;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.edit_blueprint"))) {
               try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
                  Blueprint fullBlueprintx = BlueprintIo.readBlueprint(inputStream);
                  if (editBlueprintData != null) {
                     editBlueprintData.close();
                  }

                  editBlueprintData = new BlueprintBrowserWindow.EditBlueprintData(
                     path,
                     fullBlueprintx.thumbnail(),
                     fullBlueprintx.blockRegion(),
                     fullBlueprintx.blockEntities(),
                     fullBlueprintx.entities(),
                     fullBlueprintx.header().containsAir()
                  );
                  editBlueprintData.blueprintName.set(fullBlueprintx.header().name());
                  editBlueprintData.authorName.set(fullBlueprintx.header().author());
                  editBlueprintData.tagListWidget.tags().addAll(fullBlueprintx.header().tags());
                  editBlueprintData.blueprintPreview.setYaw(fullBlueprintx.header().thumbnailYaw(), false);
                  editBlueprintData.blueprintPreview.setPitch(fullBlueprintx.header().thumbnailPitch(), false);
                  editBlueprintData.previewLocked = fullBlueprintx.header().lockedThumbnail();
                  ImGui.closeCurrentPopup();
               } catch (NoSuchFileException | FileNotFoundException var24) {
                  noLongerExistsErrorFileName = path.getFileName().toString();
               } catch (IOException var25) {
                  var25.printStackTrace();
               }
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.delete"))) {
               try {
                  Files.delete(path);
                  BlueprintDirectory parent = dirStructureMap.get(path.getParent());
                  if (parent != null) {
                     parent.remove(blueprint.path());
                  }
               } catch (NoSuchFileException | FileNotFoundException var16) {
                  noLongerExistsErrorFileName = path.getFileName().toString();
               } catch (Exception var17) {
                  displayedException = var17;
                  var17.printStackTrace();
               }
            }

            if (rootServerBlueprintDirectory != null) {
               if (!ClientEvents.serverSupportsProtocol(SupportedProtocol.BLUEPRINT_UPLOAD)) {
                  ImGuiHelper.disabledMenuItem(
                     AxiomI18n.get("axiom.editorui.window.blueprint_browser.upload_to_server"), "Server hasn't given you permission to upload blueprints"
                  );
               } else if (ImGui.menuItem(AxiomI18n.get("axiom.editorui.window.blueprint_browser.upload_to_server"))) {
                  if (Axiom.getInstance().serverConfig != null && Axiom.getInstance().serverConfig.blueprintVersion() == 2) {
                     try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
                        RawBlueprint rawBlueprint = BlueprintIo.readRawBlueprint(inputStream);
                        Path rootPath = Axiom.getInstance().getBlueprintDirectory();
                        Path relative = rootPath.relativize(path);
                        String relativeStr = relative.toString();
                        relativeStr = relativeStr.replace("\\", "/");
                        if (!relativeStr.startsWith("/")) {
                           relativeStr = "/" + relativeStr;
                        }

                        new AxiomServerboundUploadBlueprint(relativeStr, rawBlueprint).send();
                        ImGui.closeCurrentPopup();
                     } catch (NoSuchFileException | FileNotFoundException var21) {
                        noLongerExistsErrorFileName = path.getFileName().toString();
                     } catch (IOException var22) {
                        var22.printStackTrace();
                     }
                  } else {
                     ChatUtils.error(
                        "Unable to upload blueprint since server is running a version with a different blueprint format. Please ensure both the client & server are running the most up-to-date versions of Axiom"
                     );
                  }
               }
            }

            ImGui.endPopup();
         }

         renameContextMenu(blueprint, index, openRename);
      }
   }

   private static void renameContextMenu(BlueprintOrDirectory blueprint, int index, boolean openRename) {
      Path path = blueprint.path().real();
      if (openRename) {
         ImGui.openPopup("###BlueprintCtxMenu" + index + "Rename");
         renameFilenameString.set(path.getFileName().toString());
      }

      ImVec2 center = ImGui.getMainViewport().getCenter();
      ImGui.setNextWindowPos(center.x, center.y, 1, 0.5F, 0.5F);
      if (ImGuiHelper.beginPopupModalCloseable(
         AxiomI18n.get("axiom.editorui.window.blueprint_browser.rename_file") + "###BlueprintCtxMenu" + index + "Rename", 64
      )) {
         boolean confirm = ImGui.inputText("##Filename", renameFilenameString, 64);
         if (ImGui.button(AxiomI18n.get("axiom.editorui.window.blueprint_browser.confirm")) || confirm) {
            String filename = path.getFileName().toString();
            String newFilename = ImGuiHelper.getString(renameFilenameString);
            if (!newFilename.equals(filename)) {
               try {
                  Path newPath = path.getParent().resolve(newFilename);
                  BlueprintDirectory from = dirStructureMap.get(path.getParent());
                  BlueprintDirectory to = dirStructureMap.get(newPath.getParent());
                  Files.move(path, newPath);
                  if (from == to) {
                     from.changeChildPath(path, newPath);
                  }
               } catch (NoSuchFileException | FileNotFoundException var11) {
                  noLongerExistsErrorFileName = path.getFileName().toString();
               } catch (Exception var12) {
                  displayedException = var12;
                  var12.printStackTrace();
               }
            }

            ImGui.closeCurrentPopup();
         }

         ImGui.sameLine();
         if (ImGui.button(AxiomI18n.get("axiom.widget.cancel"))) {
            ImGui.closeCurrentPopup();
         }

         ImGuiHelper.endPopupModalCloseable();
      }
   }

   private static Path getNewPath(BlueprintDirectory newDir, BlueprintOrDirectory blueprintOrDirectory) {
      return newDir.path().real().resolve(blueprintOrDirectory.path().getFileName());
   }

   private static void moveBlueprint(BlueprintOrDirectory blueprintOrDirectory, Path path, boolean overwrite) throws IOException {
      if (!blueprintOrDirectory.path().real().equals(path)) {
         Files.move(blueprintOrDirectory.path().real(), path, overwrite ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[0]);
      }
   }

   public static class EditBlueprintData {
      Path path;
      ChunkedBlockRegion blockRegion;
      Long2ObjectMap<CompressedBlockEntity> blockEntities;
      List<CompoundTag> entities;
      boolean isRotating = false;
      float lastMouseX = 0.0F;
      float lastMouseY = 0.0F;
      boolean previewLocked = false;
      DynamicTextureSupplier fallbackPreview;
      BlueprintPreview blueprintPreview = new BlueprintPreview();
      ImString blueprintName = new ImString();
      ImString authorName = new ImString();
      TagListWidget tagListWidget = new TagListWidget();
      CompletableFuture<NativeImage> previewImageFuture = null;
      boolean containsAir;

      public EditBlueprintData(
         Path path,
         DynamicTextureSupplier fallbackPreview,
         ChunkedBlockRegion blockRegion,
         Long2ObjectMap<CompressedBlockEntity> blockEntities,
         List<CompoundTag> entities,
         boolean containsAir
      ) {
         this.path = path;
         this.blockRegion = blockRegion;
         this.blockEntities = blockEntities;
         this.entities = entities;
         this.fallbackPreview = fallbackPreview;
         this.blueprintPreview.setBlockRegion(blockRegion);
         this.containsAir = containsAir;
      }

      public void close() {
         this.fallbackPreview.close();
         if (this.previewImageFuture != null) {
            this.previewImageFuture.thenAccept(NativeImage::close);
            this.previewImageFuture = null;
         }
      }
   }

   private static class MoveFileOperation {
      boolean pendingOverwriteConfirmation = false;
      boolean overwriteConfirmation = false;
      BlueprintDirectory from;
      BlueprintDirectory to;
      BlueprintOrDirectory file;
      BlueprintOrDirectory repositionReference;
      boolean repositionBefore;

      public MoveFileOperation(BlueprintDirectory from, BlueprintDirectory to, BlueprintOrDirectory file) {
         this.from = from;
         this.to = to;
         this.file = file;
      }
   }

   public record Thumbnail(DynamicTextureSupplier textureSupplier) {
      public DynamicTexture getTextureOrMissing() {
         return this.textureSupplier != null ? this.textureSupplier.get() : MissingTextureImage.getDynamicTexture();
      }
   }
}
