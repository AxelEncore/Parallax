package com.moulberry.axiom;
import com.moulberry.axiom.i18n.AxiomI18n;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.moulberry.axiom.buildertools.BuilderTool;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.AngelPlacement;
import com.moulberry.axiom.capabilities.ArcballCamera;
import com.moulberry.axiom.capabilities.BuildSymmetry;
import com.moulberry.axiom.capabilities.Bulldozer;
import com.moulberry.axiom.capabilities.Capability;
import com.moulberry.axiom.capabilities.FastPlace;
import com.moulberry.axiom.capabilities.ReplaceMode;
import com.moulberry.axiom.configuration.AltMenuKeybindMode;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.displayentity.ItemList;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.clipboard_installation.ClipboardInstallationHandler;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.views.ViewManager;
import com.moulberry.axiom.editor.windows.clipboard.BlueprintBrowserWindow;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.integration.ServerIntegration;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.operations.RebuildOperation;
import com.moulberry.axiom.packets.AxiomClientboundPacket;
import com.moulberry.axiom.packets.AxiomServerboundHello;
import com.moulberry.axiom.packets.AxiomServerboundSetNoPhysicalTrigger;
import com.moulberry.axiom.packets.AxiomServerboundTeleport;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.BiomeOverlayRenderer;
import com.moulberry.axiom.render.BlockRenderCache;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.CollisionMeshOverlayRenderer;
import com.moulberry.axiom.render.annotations.Annotations;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.screen.AxiomIntroductionScreen;
import com.moulberry.axiom.screen.SwitchBuilderToolScreen;
import com.moulberry.axiom.screen.SwitchHotbarScreen;
import com.moulberry.axiom.tools.ServerHeightmaps;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.annotation.AnnotationTool;
import com.moulberry.axiom.tools.annotation.AnnotationsDisabled;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.DFUHelper;
import com.moulberry.axiom.utils.InputHelper;
import com.moulberry.axiom.utils.IpAddressMatcher;
import com.moulberry.axiom.versioning.input_events.LegacyKeyEvent;
import com.moulberry.axiom.world_modification.Dispatcher;
import com.moulberry.axiom.world_properties.client.ClientWorldPropertiesRegistry;
import com.moulberry.axiom.utils.Authorization;
import com.moulberry.axiom.utils.Authorization.ServerAuthorization;
import io.netty.channel.local.LocalAddress;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import com.moulberry.axiom.platform.TriState;
import com.moulberry.axiom.platform.AxiomPlatform;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.text.WordUtils;
public class ClientEvents {
   private static final String mainCategory = "key.category.axiom.keybind";
   private static final String capabilitiesCategory = "key.category.axiom.keybind_capabilities";
   private static final String builderToolsCategory = "key.category.axiom.keybind_builder_tools";
   public static final KeyMapping contextMenuKeyBind = new KeyMapping("axiom.keybind.context", Type.KEYSYM, 342, "key.category.axiom.keybind");
   private static final KeyMapping builderToolSlotKeyBind = new KeyMapping("axiom.keybind.builder_tool_slot", Type.KEYSYM, 48, "key.category.axiom.keybind");
   private static final KeyMapping orbitCameraKeyBind = new KeyMapping("axiom.keybind.orbit_camera", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind");
   private static final KeyMapping jumpToBlockKeyBind = new KeyMapping("axiom.keybind.jump_to_block", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind");
   public static final KeyMapping toggleEditorUiKeyBind = new KeyMapping("axiom.keybind.toggle_editor_ui", Type.KEYSYM, 344, "key.category.axiom.keybind");
   public static final KeyMapping toggleInvisibleBlocksKeyBind = new KeyMapping("axiom.keybind.toggle_invisible_blocks", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind");
   public static final KeyMapping toggleFlightSpeedAdjustment = new KeyMapping("axiom.keybind.toggle_flight_speed_adjustment", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind");
   private static final KeyMapping toggleNoClipCapKeyBind = new KeyMapping("axiom.keybind.toggle_no_clip_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleAngelPlaceCapKeyBind = new KeyMapping("axiom.keybind.toggle_angel_placement_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleFastPlaceCapKeyBind = new KeyMapping("axiom.keybind.toggle_fast_place_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleInfiniteReachCapKeyBind = new KeyMapping("axiom.keybind.toggle_infinite_reach_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleTinkerCapKeyBind = new KeyMapping("axiom.keybind.toggle_tinker_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleNoUpdatesCapKeyBind = new KeyMapping("axiom.keybind.toggle_no_updates_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleForcePlaceCapKeyBind = new KeyMapping("axiom.keybind.toggle_force_place_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleReplaceModeCapKeyBind = new KeyMapping("axiom.keybind.toggle_replace_mode_cap", Type.KEYSYM, 82, "key.category.axiom.keybind_capabilities");
   private static final KeyMapping toggleBulldozerCapKeyBind = new KeyMapping("axiom.keybind.toggle_bulldozer_cap", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_capabilities");
   public static final KeyMapping builderToolNudgeScrollKeyBind = new KeyMapping("axiom.keybind.nudge_with_scroll", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_builder_tools");
   public static final KeyMapping builderToolNudgeForwardsKeyBind = new KeyMapping("axiom.keybind.nudge_forwards", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_builder_tools");
   public static final KeyMapping builderToolNudgeBackwardsKeyBind = new KeyMapping("axiom.keybind.nudge_backwards", Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.category.axiom.keybind_builder_tools");
   private static final KeyMapping[] allKeybinds = new KeyMapping[]{
      contextMenuKeyBind,
      builderToolSlotKeyBind,
      orbitCameraKeyBind,
      jumpToBlockKeyBind,
      toggleEditorUiKeyBind,
      toggleInvisibleBlocksKeyBind,
      toggleFlightSpeedAdjustment,
      toggleNoClipCapKeyBind,
      toggleAngelPlaceCapKeyBind,
      toggleFastPlaceCapKeyBind,
      toggleInfiniteReachCapKeyBind,
      toggleTinkerCapKeyBind,
      toggleNoUpdatesCapKeyBind,
      toggleForcePlaceCapKeyBind,
      toggleReplaceModeCapKeyBind,
      toggleBulldozerCapKeyBind,
      builderToolNudgeScrollKeyBind,
      builderToolNudgeForwardsKeyBind,
      builderToolNudgeBackwardsKeyBind
   };
   public static boolean serverSupportsAxiom = false;
   public static boolean processedServerSupportsAxiom = false;
   public static boolean allowedOnServer = false;
   private static CompletableFuture<ServerAuthorization> allowedOnServerFuture = null;
   public static boolean processedAllowedOnServer = false;
   private static byte[] messageSignatureOne = null;
   private static byte[] messageSignatureTwo = null;
   private static byte[] messageSignatureThree = null;
   private static boolean loadedMeta = false;
   public static boolean remotelyDisabled = false;
   public static String updateMessage = null;
   public static int c = 0;
   private static boolean hasDisconnected = false;
   private static boolean hasJoined = false;
   private static boolean redoHandshake = false;
   private static final EnumSet<SupportedProtocol> supportedProtocols = EnumSet.noneOf(SupportedProtocol.class);
   private static final EnumSet<SupportedProtocol> receivedProtocols = EnumSet.noneOf(SupportedProtocol.class);
   public static boolean waitingForOrbitCameraDepth = false;
   private static boolean delayedOpenConfig = false;
   public static TriState setNoPhysicalTrigger = TriState.DEFAULT;
   public static AlertScreen pendingAlertScreen = null;
   public static ServerAddress lastServerAddress = null;
   public static ConcurrentLinkedQueue<AxiomClientboundPacket> clientboundPackets = new ConcurrentLinkedQueue<>();
   public static List<PlayerInfo> teleportablePlayers = new ArrayList<>();
   private static String lastSerializedConfiguration = null;
   private static int saveConfigurationTimer = 0;

   public static void handshake() {
      redoHandshake = true;
      receivedProtocols.addAll(supportedProtocols);
      receivedProtocols.add(SupportedProtocol.HELLO);
   }

   public static boolean serverSupportsProtocol(SupportedProtocol protocol) {
      AxiomPermission permission = protocol.relatedPermission;
      return permission != null && !AxiomClient.hasPermission(permission) ? false : supportedProtocols.contains(protocol);
   }

   public static void beforeDisconnect() {
      Minecraft minecraft = Minecraft.getInstance();
      if (EditorUI.isActive() && minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
         ServerIntegration.sendChangeGameModeImmediately(GameType.CREATIVE);
      }
   }

   public static KeyMapping[] getAllKeybinds() {
      return allKeybinds;
   }

   private static void feedback(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, Component message) {
      context.getSource().sendSuccess(() -> message, false);
   }

   private static void error(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, Component message) {
      context.getSource().sendFailure(message);
   }

   /** Invoked by the client packet-listener mixin when the server registers Axiom plugin channels. */
   public static void onServerChannelsRegistered(java.util.Collection<ResourceLocation> channels) {
      for (SupportedProtocol protocol : SupportedProtocol.values()) {
         if (channels.contains(protocol.identifier)) {
            receivedProtocols.add(protocol);
         }
      }
   }

   public static void register() {
      // Client shutdown: save config (NeoForge has no client-stopping event; use a JVM shutdown hook).
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         try {
            Axiom.LOGGER.info("Saving configuration (client stopping)...");
            Axiom.configuration.saveToDefaultFolder();
         } catch (Throwable var2) {
            var2.printStackTrace();
         }
      }, "axiom-config-save"));
      // Server channel registration (protocol detection) is delivered via a mixin -> onServerChannelsRegistered().
      NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingIn event) -> hasJoined = true);
      NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
         LocalPlayer player = event.getPlayer();
         ClientPacketListener handler = Minecraft.getInstance().getConnection();
         if (player != null && handler != null) {
            HotbarManager.trySave(player, handler.registryAccess(), false);
         }
         hasDisconnected = true;
         receivedProtocols.clear();
         clientboundPackets.clear();
      });
      NeoForge.EVENT_BUS.addListener(
            (RegisterClientCommandsEvent event) -> {
               CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
               dispatcher.register((LiteralArgumentBuilder)Commands.literal("axiomconfig").executes(context -> {
                  delayedOpenConfig = true;
                  Axiom.configuration.internal.showOpenConfigTip = false;
                  return 0;
               }));
               dispatcher.register((LiteralArgumentBuilder)Commands.literal("axiomhandshake").executes(context -> {
                  handshake();
                  return 0;
               }));
               LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("axiomtogglecapability");

               for (Capability capability : Capability.values()) {
                  String name = WordUtils.capitalizeFully(capability.name().toLowerCase(Locale.ROOT).replace("_", " ")).replace(" ", "");
                  builder = (LiteralArgumentBuilder<CommandSourceStack>)builder.then(
                     Commands.literal(name)
                        .executes(
                           context -> {
                              capability.toggle();
                              if (capability.isEnabled()) {
                                 Toasts.addToast(
                                    new Toasts.Toast(
                                       Component.translatable("axiom.toasts.enabled", new Object[]{capability.title}),
                                       ResourceLocation.parse("axiom:gui/hotbar_swapper.png"),
                                       -16711936,
                                       100 + capability.ordinal(),
                                       16 * capability.ordinal(),
                                       24,
                                       256,
                                       256
                                    )
                                 );
                              } else {
                                 Toasts.addToast(
                                    new Toasts.Toast(
                                       Component.translatable("axiom.toasts.disabled", new Object[]{capability.title}),
                                       ResourceLocation.parse("axiom:gui/hotbar_swapper.png"),
                                       -65536,
                                       100 + capability.ordinal(),
                                       16 * capability.ordinal(),
                                       40,
                                       256,
                                       256
                                    )
                                 );
                              }

                              return 0;
                           }
                        )
                  );
               }

               dispatcher.register(builder);
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("/pos1")
                        .executes(
                           context -> {
                              if (BuilderToolManager.isToolSlotActive()) {
                                 BlockPos position = BlockPos.containing((context.getSource()).getPosition());
                                 if (BuilderToolManager.setPos1(position)) {
                                    Component posX = Component.literal(" " + position.getX()).withStyle(ChatFormatting.RED);
                                    Component posY = Component.literal(" " + position.getY()).withStyle(ChatFormatting.GREEN);
                                    Component posZ = Component.literal(" " + position.getZ()).withStyle(ChatFormatting.AQUA);
                                    feedback(context, 
                                          Component.literal(AxiomI18n.get("axiom.hardcoded.set_first_pos")).withStyle(ChatFormatting.YELLOW).append(posX).append(posY).append(posZ)
                                       );
                                    return 0;
                                 }
                              }

                              throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
                           }
                        ))
                     .then(Commands.argument("arguments", StringArgumentType.greedyString()))
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("/pos2")
                        .executes(
                           context -> {
                              if (BuilderToolManager.isToolSlotActive()) {
                                 BlockPos position = BlockPos.containing((context.getSource()).getPosition());
                                 if (BuilderToolManager.setPos2(position)) {
                                    Component posX = Component.literal(" " + position.getX()).withStyle(ChatFormatting.RED);
                                    Component posY = Component.literal(" " + position.getY()).withStyle(ChatFormatting.GREEN);
                                    Component posZ = Component.literal(" " + position.getZ()).withStyle(ChatFormatting.AQUA);
                                    feedback(context, 
                                          Component.literal(AxiomI18n.get("axiom.hardcoded.set_second_pos")).withStyle(ChatFormatting.YELLOW).append(posX).append(posY).append(posZ)
                                       );
                                    return 0;
                                 }
                              }

                              throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand().create();
                           }
                        ))
                     .then(Commands.argument("arguments", StringArgumentType.greedyString()))
               );
               dispatcher.register(
                  (LiteralArgumentBuilder)Commands.literal("axiomenablecheats")
                     .executes(
                        context -> {
                           IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                           if (server == null) {
                              feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.cmd_singleplayer_only")));
                           } else if (server.getWorldData() instanceof PrimaryLevelData primaryLevelData) {
                              LevelSettings settings = primaryLevelData.settings;
                              if (primaryLevelData.settings.allowCommands()) {
                                 feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.cheats_already_enabled")));
                              } else {
                                 feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.cheats_now_enabled")));
                                 primaryLevelData.settings = new LevelSettings(
                                    settings.levelName(),
                                    settings.gameType(),
                                    settings.hardcore(),
                                    settings.difficulty(),
                                    true,
                                    settings.gameRules().copy(),
                                    settings.getDataConfiguration()
                                 );
                              }
                           }

                           return 0;
                        }
                     )
               );
               dispatcher.register((LiteralArgumentBuilder)Commands.literal("axiomintro").executes(context -> {
                  Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new AxiomIntroductionScreen()));
                  return 0;
               }));
               dispatcher.register((LiteralArgumentBuilder)Commands.literal("axiomdocs").executes(context -> {
                  Minecraft.getInstance().tell(() -> {
                     String docs = "https://axiomdocs.moulberry.com/";
                     Minecraft.getInstance().setScreen(new ConfirmLinkScreen(open -> {
                        if (open) {
                           try {
                              Util.getPlatform().openUri(new URI(docs));
                           } catch (Exception var3) {
                           }
                        }

                        Minecraft.getInstance().setScreen(null);
                     }, docs, false));
                  });
                  return 0;
               }));
               dispatcher.register(
                  (LiteralArgumentBuilder)Commands.literal("whynoaxiom")
                     .executes(
                        context -> {
                           LocalPlayer player = Minecraft.getInstance().player;
                           if (player == null) {
                              feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.not_ingame_1")));
                              return 0;
                           } else if (remotelyDisabled) {
                              feedback(context, Component.literal("Parallax is temporarily unavailable"));
                              return 0;
                           } else if (!serverSupportsAxiom) {
                              feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.srv_no_axiom")));
                              return 0;
                           } else if (allowedOnServer) {
                              if (Axiom.getInstance().serverConfig == null) {
                                 if (Minecraft.getInstance().hasSingleplayerServer()) {
                                    feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.cheats_disabled_try")));
                                 } else {
                                    feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.srv_no_permission")));
                                    if (AxiomPlatform.isModLoaded("wurst")) {
                                       feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.wurst_warning")));
                                    }
                                 }

                                 return 0;
                              } else {
                                 MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
                                 if (gameMode == null) {
                                    feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.not_ingame_2")));
                                    return 0;
                                 } else {
                                    GameType required = EditorUI.isEnabled() ? GameType.SPECTATOR : GameType.CREATIVE;
                                    if (gameMode.getPlayerMode() != required) {
                                       feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.you_arent_in") + required));
                                       return 0;
                                    } else {
                                       feedback(context, Component.literal("Parallax should be enabled"));
                                       return 0;
                                    }
                                 }
                              }
                           } else {
                              if (!processedServerSupportsAxiom) {
                                 feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.client_no_mp_unproc")));
                              } else if (allowedOnServerFuture != null && !allowedOnServerFuture.isDone()) {
                                 feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.client_no_mp_pending")));

                                 try {
                                    feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.checking_connection")));
                                    InetAddress address = InetAddress.getByName("axiom.moulberry.com");
                                    String ipAddress = address.getHostAddress();
                                    feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.got_address") + ipAddress + "..."));
                                    HttpURLConnection connection = null;

                                    try {
                                       URL url = new URL("https://axiom.moulberry.com/");
                                       connection = (HttpURLConnection)url.openConnection();
                                       connection.setRequestProperty("User-Agent", Authorization.getUserAgent());
                                       connection.setConnectTimeout(10000);
                                       connection.setReadTimeout(10000);
                                       connection.setRequestMethod("GET");
                                       feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.response_code") + connection.getResponseCode()));
                                    } catch (Throwable var11) {
                                       error(context, Component.literal(AxiomI18n.get("axiom.hardcoded.error_checking_conn")));
                                       var11.printStackTrace();
                                    } finally {
                                       if (connection != null) {
                                          connection.disconnect();
                                       }
                                    }
                                 } catch (UnknownHostException var13) {
                                    feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.unable_check_host")));
                                 } catch (Throwable var14) {
                                    error(context, Component.literal(AxiomI18n.get("axiom.hardcoded.error_checking_conn")));
                                    var14.printStackTrace();
                                 }
                              } else {
                                 feedback(context, Component.literal(AxiomI18n.get("axiom.hardcoded.client_no_mp")));
                              }

                              return 0;
                           }
                        }
                     )
               );
            }
         );
      NeoForge.EVENT_BUS.addListener(
            (ClientTickEvent.Pre event) -> {
               Minecraft client = Minecraft.getInstance();
               if (delayedOpenConfig) {
                  delayedOpenConfig = false;
                  Axiom.openConfigScreen(Minecraft.getInstance().screen);
               }

               if (StaticValues.shouldReloadResourcesForLanguage) {
                  StaticValues.shouldReloadResourcesForLanguage = false;
                  client.reloadResourcePacks();
               }

               if (AxiomClient.isAxiomActive()) {
                  LocalPlayer player = client.player;
                  if (player != null) {
                     FastPlace.tick();
                     AngelPlacement.tick();
                     HotbarManager.tickSaving();
                     ViewManager.tickSaving();
                     if (lastSerializedConfiguration == null) {
                        lastSerializedConfiguration = Axiom.configuration.serialize();
                        saveConfigurationTimer = 1200;
                     } else if (saveConfigurationTimer > 0) {
                        saveConfigurationTimer--;
                     } else if (!lastSerializedConfiguration.equals(Axiom.configuration.serialize())) {
                        Axiom.configuration.saveToDefaultFolder();
                        lastSerializedConfiguration = Axiom.configuration.serialize();
                        saveConfigurationTimer = 1200;
                     }

                     if (DisplayEntityManipulator.hasGrabbedNotCenterGizmo()) {
                        KeyMapping keyShift = Minecraft.getInstance().options.keyShift;
                        boolean shiftUsesDisplayEntityManipulatorKey = VersionUtilsClient.legacyKeyMappingMatches(keyShift, new LegacyKeyEvent(340, -1, 0))
                           || VersionUtilsClient.legacyKeyMappingMatches(keyShift, new LegacyKeyEvent(344, -1, 0))
                           || VersionUtilsClient.legacyKeyMappingMatches(keyShift, new LegacyKeyEvent(InputHelper.EDIT_SHORTCUT_KEY_LEFT, -1, 0))
                           || VersionUtilsClient.legacyKeyMappingMatches(keyShift, new LegacyKeyEvent(InputHelper.EDIT_SHORTCUT_KEY_RIGHT, -1, 0));
                        if (shiftUsesDisplayEntityManipulatorKey) {
                           keyShift.setDown(false);

                           while (keyShift.consumeClick()) {
                           }
                        }
                     }
                  }
               }
            }
         );
      BooleanWrapper contextMenuDown = new BooleanWrapper(true);
      NeoForge.EVENT_BUS.addListener(
            (ClientTickEvent.Post event) -> {
               Minecraft client = Minecraft.getInstance();
               if (hasJoined || hasDisconnected || redoHandshake) {
                  Axiom.getInstance().serverConfig = null;
                  if (hasJoined || hasDisconnected) {
                     serverSupportsAxiom = false;
                     supportedProtocols.clear();
                     processedServerSupportsAxiom = false;
                     allowedOnServerFuture = null;
                     allowedOnServer = false;
                     Authorization.hasServerCommercialLicense = false;
                  }

                  processedAllowedOnServer = false;
                  setNoPhysicalTrigger = TriState.DEFAULT;
                  ClientRestrictions.reset();
                  HotbarManager.updateLocation();
                  Annotations.clear();
                  ViewManager.clear();
                  ServerCustomBlocks.clearRegisteredCustomBlocks();
                  ClientWorldPropertiesRegistry.clear();
                  EditorUI.warnings.clear();
                  ToolManager.resetAll();
                  BuildSymmetry.clear();
                  BlueprintBrowserWindow.setServerBlueprintRegistry(true, null);
                  RebuildOperation.resetRebuildState();
                  ServerHeightmaps.clear();
                  ItemList.INSTANCE.customEntriesDefinedByPacket.clear();
                  ItemList.INSTANCE.markDirty();

                  for (Tool tool : ToolManager.getTools()) {
                     try {
                        tool.reset();
                     } catch (Exception var16) {
                        if (AxiomPlatform.isDevelopment()) {
                           Axiom.LOGGER.error("Error while resetting tool options during login/logout", var16);
                        }
                     }
                  }

                  for (BuilderTool builderTool : BuilderToolManager.getTools()) {
                     try {
                        builderTool.reset(false);
                     } catch (Exception var15) {
                        if (AxiomPlatform.isDevelopment()) {
                           Axiom.LOGGER.error("Error while resetting tool options during login/logout", var15);
                        }
                     }
                  }

                  AxiomClient.ignoredDisplayEntities = Set.of();
                  AnnotationTool.annotationsDisabled = EnumSet.noneOf(AnnotationsDisabled.class);
               }

               if (hasDisconnected) {
                  Annotations.clear();
                  ChunkRenderOverrider.clear();
                  BiomeOverlayRenderer.INSTANCE.clear();
                  CollisionMeshOverlayRenderer.INSTANCE.clear();
                  Dispatcher.reset();
                  EditorUI.clearActiveBlockHistory();
               }

               if (Minecraft.getInstance().hasSingleplayerServer()) {
                  supportedProtocols.addAll(Arrays.asList(SupportedProtocol.values()));
                  serverSupportsAxiom = true;
               } else {
                  supportedProtocols.addAll(receivedProtocols);
                  serverSupportsAxiom = supportedProtocols.contains(SupportedProtocol.HELLO);
               }

               hasJoined = false;
               hasDisconnected = false;
               redoHandshake = false;
               receivedProtocols.clear();
               Axiom axiom = Axiom.getInstance();
               LocalPlayer player = client.player;
               if (player != null) {
                  ClientPacketListener connection = Minecraft.getInstance().getConnection();
                  if (connection != null) {
                     StaticValues.gameHasTicked = client.player != null
                        && (c <= 0 || c <= 4 && Minecraft.getInstance().screen == null != c > 2 || --c != 0 || null == (client.player = null));
                     if (com.moulberry.axiom.integration.ReplayModIntegration.isPlayingReplay()) {
                        clientboundPackets.clear();
                     } else {
                        while (!clientboundPackets.isEmpty()) {
                           AxiomClientboundPacket packet = clientboundPackets.poll();
                           packet.handle(Minecraft.getInstance(), connection.registryAccess());
                        }

                        if (pendingAlertScreen != null && Minecraft.getInstance().screen == null) {
                           Minecraft.getInstance().setScreen(pendingAlertScreen);
                           pendingAlertScreen = null;
                        }

                        if (!loadedMeta) {
                           loadedMeta = true;
                           Authorization.getMeta()
                              .thenAccept(
                                 meta -> {
                                    if (meta.modDisabled() != null) {
                                       String message = "Parallax is temporarily unavailable:\n\n" + meta.modDisabled();
                                       remotelyDisabled = true;
                                       Minecraft.getInstance()
                                          .submit(
                                             () -> Minecraft.getInstance()
                                                .setScreen(new AlertScreen(() -> {}, Component.literal("Parallax"), Component.literal(message)))
                                          );
                                    } else {
                                       long currentTime = System.currentTimeMillis();
                                       long nextUpdateNag = Axiom.configuration.internal.nextUpdateNag;
                                       boolean shouldNag = currentTime > nextUpdateNag
                                          || currentTime < nextUpdateNag - ChronoUnit.YEARS.getDuration().getSeconds() * 1000L;
                                       if (false && shouldNag && meta.latestModVersion() != null) {
                                          ArtifactVersion version = new DefaultArtifactVersion(AxiomPlatform.modVersion());

                                          try {
                                             ArtifactVersion latestVersion = new DefaultArtifactVersion(meta.latestModVersion());
                                             if (latestVersion.compareTo(version) > 0) {
                                                StringBuilder message = new StringBuilder();
                                                message.append("New Axiom version available: ").append(meta.latestModVersion()).append("\n\n");
                                                if (meta.latestChangelog() != null && !meta.latestChangelog().isEmpty()) {
                                                   message.append(meta.latestModVersion()).append(" changelog:").append("\n");

                                                   for (String s : meta.latestChangelog()) {
                                                      if (!s.isBlank()) {
                                                         message.append(" - ").append(s).append("\n");
                                                      }
                                                   }
                                                }

                                                updateMessage = message.toString().trim();
                                             }
                                          } catch (Exception var12x) {
                                             StringBuilder message = new StringBuilder();
                                             message.append("Unable to parse latest version: ").append(meta.latestModVersion()).append("\n\n");
                                             message.append("Please report this!");
                                             Minecraft.getInstance()
                                                .submit(
                                                   () -> Minecraft.getInstance()
                                                      .setScreen(new AlertScreen(() -> {}, Component.literal("Parallax"), Component.literal(message.toString())))
                                                );
                                          }
                                       }
                                    }
                                 }
                              );
                        }

                        if (!remotelyDisabled) {
                           if (serverSupportsAxiom && !processedAllowedOnServer) {
                              if (!processedServerSupportsAxiom) {
                                 processedServerSupportsAxiom = true;
                                 if (!Authorization.hasCommercialLicense() && Minecraft.getInstance().getSingleplayerServer() == null) {
                                    SocketAddress address = connection.getConnection().getRemoteAddress();
                                    if (address instanceof LocalAddress) {
                                       allowedOnServer = true;
                                    } else if (!(address instanceof InetSocketAddress inetSocketAddress)) {
                                       allowedOnServer = true;
                                    } else {
                                       IpAddressMatcher privateMatcher1 = new IpAddressMatcher("10.0.0.0/8");
                                       IpAddressMatcher privateMatcher2 = new IpAddressMatcher("172.16.0.0/12");
                                       IpAddressMatcher privateMatcher3 = new IpAddressMatcher("192.168.0.0/16");
                                       String ip = inetSocketAddress.getAddress().getHostAddress().trim();
                                       if (privateMatcher1.matches(ip) || privateMatcher2.matches(ip) || privateMatcher3.matches(ip)) {
                                          allowedOnServer = true;
                                       } else if (inetSocketAddress.getAddress().isLoopbackAddress()) {
                                          allowedOnServer = true;
                                       }

                                       String host = inetSocketAddress.getHostString().trim();
                                       if (lastServerAddress != null) {
                                          String serverAddressHost = lastServerAddress.getHost();
                                          if (hostPriority(serverAddressHost, ip) > hostPriority(host, ip)) {
                                             host = serverAddressHost;
                                          }
                                       }

                                       if (!allowedOnServer || !host.equals("127.0.0.1") && !host.equals("localhost")) {
                                          allowedOnServerFuture = Authorization.checkServer(ip, host, Minecraft.getInstance().getUser().getProfileId());
                                          if (!allowedOnServer) {
                                             AnnotationTool.annotationsDisabled.add(AnnotationsDisabled.UNAVAILABLE);
                                          }
                                       }
                                    }
                                 } else {
                                    allowedOnServer = true;
                                 }
                              }

                              if (allowedOnServerFuture != null && allowedOnServerFuture.isDone()) {
                                 ServerAuthorization authorization = allowedOnServerFuture.join();
                                 if (authorization != ServerAuthorization.NO) {
                                    if (authorization == ServerAuthorization.YES) {
                                       allowedOnServer = true;
                                    } else if (authorization == ServerAuthorization.COMMERCIAL && Authorization.hasCommercialLicense()) {
                                       allowedOnServer = true;
                                       AnnotationTool.annotationsDisabled.remove(AnnotationsDisabled.UNAVAILABLE);
                                       Level level = Minecraft.getInstance().level;
                                       if (level != null) {
                                          AxiomClient.loadCommercialLicenseHistory(level.dimension());
                                       }
                                    }
                                 }

                                 allowedOnServerFuture = null;
                              }

                              if (allowedOnServerFuture == null) {
                                 processedAllowedOnServer = true;
                                 if (allowedOnServer) {
                                    new AxiomServerboundHello(9, DFUHelper.DATA_VERSION, SharedConstants.getProtocolVersion()).send();
                                 } else {
                                    try {
                                       Style commercialUrl = Style.EMPTY
                                          .withClickEvent(new ClickEvent(Action.OPEN_URL, "https://axiom.moulberry.com/commercial"))
                                          .withHoverEvent(
                                             new HoverEvent(
                                                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                                Component.literal("https://axiom.moulberry.com/commercial")
                                             )
                                          );
                                       Style discordUrl = Style.EMPTY
                                          .withClickEvent(new ClickEvent(Action.OPEN_URL, "https://discord.gg/axiomtool"))
                                          .withHoverEvent(
                                             new HoverEvent(
                                                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, Component.literal("https://discord.gg/axiomtool")
                                             )
                                          );
                                       Component one = Component.literal("[Parallax] This server has Parallax, but your client doesn't support multiplayer.")
                                          .withStyle(Style.EMPTY.withColor(16742263));
                                       Component two = Component.literal(
                                             "[Parallax] If you are building on this server for commercial purposes, please support the mod by purchasing a "
                                          )
                                          .append(
                                             Component.literal(AxiomI18n.get("axiom.hardcoded.commercial_license"))
                                                .withStyle(new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.UNDERLINE})
                                                .withStyle(commercialUrl)
                                          )
                                          .withStyle(Style.EMPTY.withColor(16742263));
                                       Component three = Component.literal("[Parallax] Otherwise, you can request a whitelist through the ")
                                          .append(
                                             Component.literal(AxiomI18n.get("axiom.hardcoded.discord_server"))
                                                .withStyle(new ChatFormatting[]{ChatFormatting.BLUE, ChatFormatting.UNDERLINE})
                                                .withStyle(discordUrl)
                                          )
                                          .withStyle(Style.EMPTY.withColor(16742263));
                                       if (messageSignatureOne != null) {
                                          client.gui.getChat().deleteMessage(new MessageSignature(messageSignatureOne));
                                       }

                                       if (messageSignatureTwo != null) {
                                          client.gui.getChat().deleteMessage(new MessageSignature(messageSignatureTwo));
                                       }

                                       if (messageSignatureThree != null) {
                                          client.gui.getChat().deleteMessage(new MessageSignature(messageSignatureThree));
                                       }

                                       Random random = ThreadLocalRandom.current();
                                       byte[] bytesOne = new byte[256];
                                       random.nextBytes(bytesOne);
                                       messageSignatureOne = bytesOne;
                                       byte[] bytesTwo = new byte[256];
                                       random.nextBytes(bytesTwo);
                                       messageSignatureTwo = bytesTwo;
                                       byte[] bytesThree = new byte[256];
                                       random.nextBytes(bytesThree);
                                       messageSignatureThree = bytesThree;
                                       client.gui.getChat().addMessage(one, new MessageSignature(messageSignatureOne), GuiMessageTag.system());
                                       client.gui.getChat().addMessage(two, new MessageSignature(messageSignatureTwo), GuiMessageTag.system());
                                       client.gui.getChat().addMessage(three, new MessageSignature(messageSignatureThree), GuiMessageTag.system());
                                    } catch (Exception var14) {
                                    }
                                 }
                              }
                           }

                           Dispatcher.tick();
                           Toasts.tick();
                           KeyPressOverlay.tick();
                           ServerIntegration.sendPendingUpdates();
                           BlockRenderCache.tick();
                           BlueprintBrowserWindow.tick();
                           DisplayEntityManipulator.tick();
                           MarkerEntityManipulator.tick();
                           ClipboardInstallationHandler.tick();
                           ScreenRenderHook.tick();
                           RebuildOperation.tick();
                           Vec3 view = player.getViewVector(1.0F);
                           ReplaceMode.lastView = view;
                           AngelPlacement.lastView = view;
                           Bulldozer.setLastView(view);
                           FastPlace.setLastView(view);
                           if (axiom.serverConfig != null && allowedOnServer && serverSupportsAxiom) {
                              boolean shouldSendSetNoPhysicalTrigger = switch (setNoPhysicalTrigger) {
                                 case FALSE -> Capability.PHANTOM.isEnabled();
                                 case DEFAULT -> true;
                                 case TRUE -> !Capability.PHANTOM.isEnabled();
                                 default -> throw new IncompatibleClassChangeError();
                              };
                              if (shouldSendSetNoPhysicalTrigger) {
                                 new AxiomServerboundSetNoPhysicalTrigger(Capability.PHANTOM.isEnabled()).send();
                                 setNoPhysicalTrigger = TriState.of(Capability.PHANTOM.isEnabled());
                              }

                              boolean contextMenuPressed = contextMenuKeyBind.consumeClick() && !contextMenuDown.value;
                              contextMenuDown.value = contextMenuPressed || contextMenuKeyBind.isDown();
                              if (client.screen == null) {
                                 if (!Axiom.configuration.internal.shownIntroduction) {
                                    Minecraft.getInstance().setScreen(new AxiomIntroductionScreen());
                                    Axiom.configuration.internal.shownIntroduction = true;
                                 }

                                 if (!EditorUI.isActive() && !player.isCreative() && contextMenuPressed && Axiom.configuration.contextMenu.autoSwapToCreative) {
                                    ServerIntegration.changeGameMode(GameType.CREATIVE);
                                 }

                                 if (toggleEditorUiKeyBind.consumeClick()) {
                                    EditorUI.toggleEnabled();
                                    Axiom.configuration.internal.showOpenEditorTip = false;
                                 }
                              }

                              if (AxiomClient.isAxiomActive() && client.screen == null) {
                                 if (BuilderToolManager.isToolSlotActive()) {
                                    BuilderToolManager.handleInput(
                                       builderToolNudgeForwardsKeyBind.consumeClick(),
                                       builderToolNudgeBackwardsKeyBind.consumeClick(),
                                       Keybinds.BUILDER_TOOL_DELETE.isPressed(false)
                                    );
                                 }

                                 if (toggleInvisibleBlocksKeyBind.consumeClick()) {
                                    AxiomConfig.SubcategoryBlockAttributes attributes = Axiom.configuration.blockAttributes;
                                    boolean enable = !attributes.showCollisionMesh
                                       && !attributes.showLightBlocks
                                       && !attributes.showStructureVoidBlocks
                                       && !attributes.showMovingPistonBlocks;
                                    attributes.showCollisionMesh = enable;
                                    attributes.showLightBlocks = enable;
                                    attributes.showStructureVoidBlocks = enable;
                                    attributes.showMovingPistonBlocks = enable;
                                    if (enable) {
                                       ScreenRenderHook.setOverlayText(
                                          Component.literal(AxiomI18n.get("axiom.hardcoded.enabled_invisible")).withStyle(ChatFormatting.GREEN)
                                       );
                                    } else {
                                       ScreenRenderHook.setOverlayText(
                                          Component.literal(AxiomI18n.get("axiom.hardcoded.disabling_invisible")).withStyle(ChatFormatting.RED)
                                       );
                                    }

                                    AxiomClient.updateItemBlockRenderTypes();
                                    Minecraft.getInstance().levelRenderer.allChanged();
                                 }

                                 if (toggleEditorUiKeyBind.consumeClick()) {
                                    if (!AxiomClient.hasPermission(AxiomPermission.EDITOR_USE)) {
                                       ChatUtils.error("The server has disallowed the use of the editor");
                                    } else {
                                       EditorUI.toggleEnabled();
                                    }
                                 }

                                 if (EditorUI.isActive()) {
                                    ContextMenuManager.getInstance().close();
                                    ArcballCamera.unlock();
                                    teleportablePlayers.clear();
                                    if (serverSupportsProtocol(SupportedProtocol.TELEPORT)) {
                                       for (PlayerInfo playerInfo : connection.getOnlinePlayers()) {
                                          if (playerInfo.getProfile().getId().version() == 4 && !playerInfo.getProfile().getId().equals(player.getUUID())) {
                                             teleportablePlayers.add(playerInfo);
                                             if (teleportablePlayers.size() > 10) {
                                                break;
                                             }
                                          }
                                       }
                                    }
                                 } else {
                                    if (ContextMenuManager.getInstance().isActive()) {
                                       boolean shouldClose;
                                       if (Axiom.configuration.contextMenu.keybindMode == AltMenuKeybindMode.TOGGLE) {
                                          shouldClose = contextMenuPressed;
                                       } else {
                                          shouldClose = !contextMenuDown.value;
                                       }

                                       if (ContextMenuManager.getInstance().getActiveScreen() != null) {
                                          MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
                                          if (gameMode == null || !Minecraft.getInstance().player.hasInfiniteMaterials()) {
                                             shouldClose = true;
                                          }
                                       }

                                       if (!shouldClose) {
                                          for (KeyMapping keyMapping : allKeybinds) {
                                             keyMapping.consumeClick();
                                          }

                                          if (ArcballCamera.isLocked()) {
                                             ArcballCamera.unlock();
                                          }

                                          return;
                                       }

                                       ContextMenuManager.getInstance().close();
                                    } else if (contextMenuPressed) {
                                       Axiom.configuration.internal.showOpenContextMenuTip = false;
                                       if (BuilderToolManager.isToolSlotActive()) {
                                          ContextMenuManager.getInstance().open(new SwitchBuilderToolScreen());
                                       } else {
                                          ContextMenuManager.getInstance().open(new SwitchHotbarScreen());
                                       }
                                    }

                                    while (orbitCameraKeyBind.consumeClick()) {
                                       waitingForOrbitCameraDepth = true;
                                    }

                                    if (!orbitCameraKeyBind.isDown() && ArcballCamera.isLocked()) {
                                       ArcballCamera.unlock();
                                       player.setDeltaMovement(Vec3.ZERO);
                                    }

                                    while (jumpToBlockKeyBind.consumeClick()) {
                                       RayCaster.RaycastResult result = Tool.raycastBlock();
                                       if (result != null) {
                                          ClientLevel level = Minecraft.getInstance().level;
                                          BlockPos blockPosition = result.blockPos().relative(result.direction());
                                          Vec3 position = Vec3.atBottomCenterOf(blockPosition);
                                          new AxiomServerboundTeleport(
                                                level.dimension(), position.x, position.y, position.z, player.getYRot(), player.getXRot()
                                             )
                                             .send();
                                       }
                                    }

                                    if (builderToolSlotKeyBind.consumeClick() && BuilderToolManager.canUseBuilderTools()) {
                                       if (BuilderToolManager.isToolSlotActive()) {
                                          BuilderToolManager.setToolSlotSelected(BuilderToolManager.getToolSlotSelected() + 1);
                                       } else {
                                          BuilderToolManager.setToolSlotActive(true);
                                       }
                                    }

                                    if (Keybinds.UNDO.isPressed(false)) {
                                       UserAction.UNDO.call(null);
                                    }

                                    if (Keybinds.REDO.isPressed(false)) {
                                       UserAction.REDO.call(null);
                                    }

                                    if (Keybinds.COPY_INGAME.isPressed(false)) {
                                       UserAction.COPY.call(null);
                                    }

                                    if (Keybinds.PASTE.isPressed(false)) {
                                       UserAction.PASTE.call(null);
                                    }

                                    if (Keybinds.SHOW_DISPLAY_ENTITY_GIZMOS.isPressed(false)) {
                                       Axiom.configuration.entityManipulation.showDisplayEntities = !Axiom.configuration.entityManipulation.showDisplayEntities;
                                    }

                                    if (Keybinds.SHOW_MARKER_ENTITY_GIZMOS.isPressed(false)) {
                                       Axiom.configuration.entityManipulation.showMarkerEntities = !Axiom.configuration.entityManipulation.showMarkerEntities;
                                    }

                                    if (Keybinds.SHOW_COLLISION_MESH.isPressed(false)) {
                                       Axiom.configuration.blockAttributes.showCollisionMesh = !Axiom.configuration.blockAttributes.showCollisionMesh;
                                    }

                                    if (Keybinds.SHOW_LIGHT_BLOCKS.isPressed(false)) {
                                       Axiom.configuration.blockAttributes.showLightBlocks = !Axiom.configuration.blockAttributes.showLightBlocks;
                                       Minecraft.getInstance().levelRenderer.allChanged();
                                    }

                                    if (Keybinds.SHOW_STRUCTURE_VOID_BLOCKS.isPressed(false)) {
                                       Axiom.configuration.blockAttributes.showStructureVoidBlocks = !Axiom.configuration.blockAttributes.showStructureVoidBlocks;
                                       Minecraft.getInstance().levelRenderer.allChanged();
                                    }

                                    if (!BuilderToolManager.isToolSlotActive()) {
                                       List<Capability> toggled = new ArrayList<>();

                                       while (toggleNoClipCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.NO_CLIP);
                                       }

                                       while (toggleAngelPlaceCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.ANGEL_PLACEMENT);
                                       }

                                       while (toggleFastPlaceCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.FAST_PLACE);
                                       }

                                       while (toggleInfiniteReachCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.INFINITE_REACH);
                                       }

                                       while (toggleTinkerCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.TINKER);
                                       }

                                       while (toggleNoUpdatesCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.NO_UPDATES);
                                       }

                                       while (toggleForcePlaceCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.FORCE_PLACE);
                                       }

                                       while (toggleReplaceModeCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.REPLACE_MODE);
                                       }

                                       while (toggleBulldozerCapKeyBind.consumeClick()) {
                                          toggled.add(Capability.BULLDOZER);
                                       }

                                       if (!toggled.isEmpty()) {
                                          Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

                                          for (Capability capability : toggled) {
                                             capability.toggle();
                                             if (capability.isEnabled()) {
                                                Toasts.addToast(
                                                   new Toasts.Toast(
                                                      Component.translatable("axiom.toasts.enabled", new Object[]{capability.title}),
                                                      ResourceLocation.parse("axiom:gui/hotbar_swapper.png"),
                                                      -16711936,
                                                      100 + capability.ordinal(),
                                                      16 * capability.ordinal(),
                                                      24,
                                                      256,
                                                      256
                                                   )
                                                );
                                             } else {
                                                Toasts.addToast(
                                                   new Toasts.Toast(
                                                      Component.translatable("axiom.toasts.disabled", new Object[]{capability.title}),
                                                      ResourceLocation.parse("axiom:gui/hotbar_swapper.png"),
                                                      -65536,
                                                      100 + capability.ordinal(),
                                                      16 * capability.ordinal(),
                                                      40,
                                                      256,
                                                      256
                                                   )
                                                );
                                             }
                                          }
                                       }
                                    }
                                 }

                                 for (KeyMapping keyMapping : allKeybinds) {
                                    while (keyMapping.consumeClick()) {
                                    }
                                 }
                              } else {
                                 for (KeyMapping keyMapping : allKeybinds) {
                                    while (keyMapping.consumeClick()) {
                                    }
                                 }

                                 if (ArcballCamera.isLocked()) {
                                    ArcballCamera.unlock();
                                 }

                                 if (ContextMenuManager.getInstance().isActive()) {
                                    ContextMenuManager.getInstance().close();
                                 }
                              }
                           } else {
                              for (KeyMapping keyMapping : allKeybinds) {
                                 while (keyMapping.consumeClick()) {
                                 }
                              }

                              if (ArcballCamera.isLocked()) {
                                 ArcballCamera.unlock();
                              }

                              if (EditorUI.isActive()) {
                                 EditorUI.disable();
                              }

                              if (ContextMenuManager.getInstance().isActive()) {
                                 ContextMenuManager.getInstance().close();
                              }
                           }
                        }
                     }
                  }
               }
            }
         );
   }

   private static int hostPriority(String host, String ip) {
      if (host.equals("127.0.0.1")) {
         return 0;
      } else if (host.equals("localhost")) {
         return 1;
      } else if (host.equals(ip)) {
         return 2;
      } else {
         return host.endsWith(".") ? 3 : 4;
      }
   }
}
