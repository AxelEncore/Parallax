package com.moulberry.axiom;

import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.blueprint.ServerBlueprintManager;
import com.moulberry.axiom.capabilities.BuildSymmetry;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.configuration.legacy.LegacyConfiguration;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.displayentity.ItemList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.hooks.ServerLevelExt;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.packets.AxiomClientboundAckWorldProperties;
import com.moulberry.axiom.packets.AxiomClientboundAddServerHeightmap;
import com.moulberry.axiom.packets.AxiomClientboundAnnotationUpdate;
import com.moulberry.axiom.packets.AxiomClientboundCustomBlocks;
import com.moulberry.axiom.packets.AxiomClientboundEditorWarning;
import com.moulberry.axiom.packets.AxiomClientboundEnable;
import com.moulberry.axiom.packets.AxiomClientboundIgnoreDisplayEntities;
import com.moulberry.axiom.packets.AxiomClientboundMarkerData;
import com.moulberry.axiom.packets.AxiomClientboundMarkerNbtResponse;
import com.moulberry.axiom.packets.AxiomClientboundRedoHandshake;
import com.moulberry.axiom.packets.AxiomClientboundRegisterCustomBlockV2;
import com.moulberry.axiom.packets.AxiomClientboundRegisterCustomItems;
import com.moulberry.axiom.packets.AxiomClientboundRegisterWorldProperties;
import com.moulberry.axiom.packets.AxiomClientboundResponseChunkData;
import com.moulberry.axiom.packets.AxiomClientboundResponseEntityData;
import com.moulberry.axiom.packets.AxiomClientboundRestrictions;
import com.moulberry.axiom.packets.AxiomClientboundSetWorldProperty;
import com.moulberry.axiom.packets.AxiomClientboundUpdateAvailableDispatchSends;
import com.moulberry.axiom.packets.AxiomServerboundAnnotationUpdate;
import com.moulberry.axiom.packets.AxiomServerboundDeleteEntity;
import com.moulberry.axiom.packets.AxiomServerboundFixArea;
import com.moulberry.axiom.packets.AxiomServerboundHello;
import com.moulberry.axiom.packets.AxiomServerboundManipulateEntity;
import com.moulberry.axiom.packets.AxiomServerboundMarkerNbtRequest;
import com.moulberry.axiom.packets.AxiomServerboundRequestChunkData;
import com.moulberry.axiom.packets.AxiomServerboundRequestEntityData;
import com.moulberry.axiom.packets.AxiomServerboundSetBlock;
import com.moulberry.axiom.packets.AxiomServerboundSetBuffer;
import com.moulberry.axiom.packets.AxiomServerboundSetFlySpeed;
import com.moulberry.axiom.packets.AxiomServerboundSetGameMode;
import com.moulberry.axiom.packets.AxiomServerboundSetNoPhysicalTrigger;
import com.moulberry.axiom.packets.AxiomServerboundSetTime;
import com.moulberry.axiom.packets.AxiomServerboundSetWorldProperty;
import com.moulberry.axiom.packets.AxiomServerboundSpawnEntity;
import com.moulberry.axiom.packets.AxiomServerboundTeleport;
import com.moulberry.axiom.packets.AxiomServerboundTickBlocks;
import com.moulberry.axiom.packets.blueprint.AxiomClientboundBlueprintManifest;
import com.moulberry.axiom.packets.blueprint.AxiomClientboundResponseBlueprint;
import com.moulberry.axiom.packets.blueprint.AxiomServerboundRequestBlueprint;
import com.moulberry.axiom.packets.blueprint.AxiomServerboundUploadBlueprint;
import com.moulberry.axiom.render.BiomeOverlayRenderer;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.CollisionMeshOverlayRenderer;
import com.moulberry.axiom.render.ShaderManager;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.IrisApiWrapper;
import com.moulberry.axiom.utils.NvidiumApiWrapper;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_properties.AxiomGameRules;
import com.moulberry.axiom.utils.Authorization;
import com.moulberry.lattice.Lattice;
import com.moulberry.lattice.element.LatticeElements;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Axiom {
   public static final Logger LOGGER = LoggerFactory.getLogger("axiom");
   private static Axiom INSTANCE;
   public ServerConfig serverConfig = null;
   private Path configDirectory;
   private Path blueprintDirectory;
   private boolean shouldInitializeServerBlueprints = false;
   public static boolean enableAssertions = AxiomPlatform.isDevelopment();
   public static AxiomConfig configuration = null;
   private static LatticeElements configElements = null;

   public static Axiom getInstance() {
      return INSTANCE;
   }

   public Path getConfigDirectory() {
      return this.configDirectory;
   }

   public Path getBlueprintDirectory() {
      return this.blueprintDirectory;
   }

   public void initCommon() {
      INSTANCE = this;
      LOGGER.info("Initializing Parallax/" + AxiomPlatform.modVersion());
      AxiomGameRules.register();
      this.configDirectory = AxiomPlatform.configDir().resolve("axiom");
      this.registerServerLifecycle();
   }

   public void initClient() {
      DFUHelper.checkContainerFormat();
         IrisApiWrapper.setupIfIrisInstalled();
         NvidiumApiWrapper.setupIfNvidiumInstalled();
         ToolManager.initializeTools();
         configuration = AxiomConfig.tryLoadFromFolder(this.configDirectory);
         LegacyConfiguration.tryLoadAndApplyLegacy(
            this.configDirectory.resolve("axiom.hocon"), this.configDirectory.resolve(".axiominternal.hocon"), configuration
         );
         Keybinds.load(configuration);
         EditorWindowType.setOpenByName(configuration.internal.openEditorWindowTypes);
         configElements = configuration.createElements();
         if (AxiomPlatform.isDevelopment()) {
            Minecraft.getInstance().tell(() -> Lattice.performTest(configElements));
         }

         this.blueprintDirectory = this.configDirectory.resolve("blueprints");
         this.clearEmptyHistory();
         String customBlueprintPath = configuration.blueprint.customPath.trim();
         if (!customBlueprintPath.isEmpty()) {
            Path path = Path.of(customBlueprintPath);
            if (Files.exists(path)) {
               if (Files.isDirectory(path)) {
                  this.blueprintDirectory = path;
               } else {
                  LOGGER.error("Unable to set custom blueprint path, {} is not a directory", path);
               }
            } else {
               LOGGER.error("Unable to set custom blueprint path, folder {} does not exist", path);
            }
         }

         try {
            Files.createDirectories(this.blueprintDirectory);
         } catch (Exception var4) {
         }

         Authorization.checkCommercial(Minecraft.getInstance().getUser().getProfileId());
         // ShaderManager registers its core shaders via RegisterShadersEvent on the mod bus
         // (see neoforge/AxiomClientBusEvents.registerShaders).
         TutorialManager.initialize();
         ClientEvents.register();
         AxiomClient.updateItemBlockRenderTypes();
   }

   private void registerServerLifecycle() {
      if (AxiomServer.supportsServerBlueprints()) {
         this.blueprintDirectory = this.configDirectory.resolve("blueprints");

         try {
            Files.createDirectories(this.blueprintDirectory);
         } catch (Exception var3) {
         }

         this.shouldInitializeServerBlueprints = true;
      }

      AxiomServer.register();
      NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> {
         if (event.getLevel() instanceof ServerLevel world) {
            ((ServerLevelExt)world).axiom$processTasks();
            if (this.shouldInitializeServerBlueprints) {
               this.shouldInitializeServerBlueprints = false;
               ServerBlueprintManager.initialize(this.blueprintDirectory);
            }
         }
      });
   }

   /** Client resource-reload listeners (registered on the mod bus, client only). */
   public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(ShaderManager.INSTANCE);
      event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
         EditorUI.getBlockList().markNeedsReload();
         ItemList.INSTANCE.markDirty();
         BlockColourMap.invalidateCache();
         ChunkedBlockRegion.staticPackReloadIndex++;
         CollisionMeshOverlayRenderer.clearBlockStateShouldRenderCache();
      });
   }

   /** Custom pick-block stack for Axiom custom blocks; invoked by the pick-block mixin. */
   public static ItemStack getCustomPickBlock(Player player, HitResult result) {
      if (result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult blockHitResult) {
         BlockState blockState = player.level().getBlockState(blockHitResult.getBlockPos());
         CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
         if (customBlockState != null) {
            return customBlockState.getCustomBlock().axiom$customPickBlockStack();
         }
      }

      return ItemStack.EMPTY;
   }

   public static Screen createConfigScreen(Screen oldScreen) {
      return Lattice.createConfigScreen(configElements, configuration::saveToDefaultFolder, oldScreen);
   }

   public static void openConfigScreen(Screen oldScreen) {
      Minecraft.getInstance().setScreen(createConfigScreen(oldScreen));
   }

   private void clearEmptyHistory() {
      Path historyFolder = this.configDirectory.resolve("history");
      if (Files.exists(historyFolder)) {
         try (DirectoryStream<Path> folders = Files.newDirectoryStream(historyFolder)) {
            for (Path folder : folders) {
               boolean isEmpty;
               try (DirectoryStream<Path> children = Files.newDirectoryStream(folder)) {
                  isEmpty = !children.iterator().hasNext();
               }

               if (isEmpty) {
                  Files.delete(folder);
               }
            }
         } catch (IOException var13) {
            LOGGER.error("Failed to clear empty history folders", var13);
         }
      }
   }

   public void dimensionChanged(ResourceKey<Level> resourceKey) {
      ChunkRenderOverrider.clear();
      BiomeOverlayRenderer.INSTANCE.clear();
      CollisionMeshOverlayRenderer.INSTANCE.clear();
      Dispatcher.reset();
      BuildSymmetry.clear();
      MarkerEntityManipulator.clear();
      if (Authorization.hasCommercialLicense()) {
         AxiomClient.loadCommercialLicenseHistory(resourceKey);
      }
   }

   public static boolean isDebugEnvironment() {
      return true;
   }

   public static void dbg(String string) {
      LOGGER.info(string);
   }

   public static boolean hasStarlight() {
      return AxiomPlatform.isModLoaded("starlight") || AxiomPlatform.isModLoaded("scalablelux");
   }
}
