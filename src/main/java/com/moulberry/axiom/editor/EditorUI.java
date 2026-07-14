package com.moulberry.axiom.editor;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.ServerConfig;
import com.moulberry.axiom.StaticValues;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.palette.ActiveBlockHistory;
import com.moulberry.axiom.editor.palette.CustomBlockStateOrTombstone;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.editor.styles.StyleManager;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.editor.tutorial.TutorialManager;
import com.moulberry.axiom.editor.tutorial.TutorialStage;
import com.moulberry.axiom.editor.views.View;
import com.moulberry.axiom.editor.views.ViewManager;
import com.moulberry.axiom.editor.windows.BlueprintCreateWindow;
import com.moulberry.axiom.editor.windows.ClipboardInstallationWindow;
import com.moulberry.axiom.editor.windows.CurrentBlockWindow;
import com.moulberry.axiom.editor.windows.HistoryWindow;
import com.moulberry.axiom.editor.windows.InventoryWindow;
import com.moulberry.axiom.editor.windows.KeybindsWindow;
import com.moulberry.axiom.editor.windows.MainMenuBar;
import com.moulberry.axiom.editor.windows.OpenSourceLicensesWindow;
import com.moulberry.axiom.editor.windows.PaletteWindow;
import com.moulberry.axiom.editor.windows.PlacementOptionsOverlay;
import com.moulberry.axiom.editor.windows.RotatePlacementWindow;
import com.moulberry.axiom.editor.windows.StyleEditorWindow;
import com.moulberry.axiom.editor.windows.TargetInfoWindow;
import com.moulberry.axiom.editor.windows.TextAnnotationListWindow;
import com.moulberry.axiom.editor.windows.TickBlocksModal;
import com.moulberry.axiom.editor.windows.ToolOptionsWindow;
import com.moulberry.axiom.editor.windows.ToolsWindow;
import com.moulberry.axiom.editor.windows.WorldPropertiesWindow;
import com.moulberry.axiom.editor.windows.clipboard.BlueprintBrowserWindow;
import com.moulberry.axiom.editor.windows.clipboard.ClipboardWindow;
import com.moulberry.axiom.editor.windows.global_mask.ToolMaskWindow;
import com.moulberry.axiom.editor.windows.operations.AnalyzeBlocksWindow;
import com.moulberry.axiom.editor.windows.operations.AnimatedRebuildWindow;
import com.moulberry.axiom.editor.windows.operations.AutoshadeWindow;
import com.moulberry.axiom.editor.windows.operations.FillBlocksWindow;
import com.moulberry.axiom.editor.windows.operations.QuickFillWindow;
import com.moulberry.axiom.editor.windows.operations.QuickReplaceWindow;
import com.moulberry.axiom.editor.windows.operations.ReplaceBlocksWindow;
import com.moulberry.axiom.editor.windows.operations.SetBiomeWindow;
import com.moulberry.axiom.editor.windows.operations.TypeReplaceBlocksWindow;
import com.moulberry.axiom.editor.windows.save_world.ExportSchematicWindow;
import com.moulberry.axiom.editor.windows.selection.DistortSelectionWindow;
import com.moulberry.axiom.editor.windows.selection.ExpandSelectionWindow;
import com.moulberry.axiom.editor.windows.selection.MaskSelectionWindow;
import com.moulberry.axiom.editor.windows.selection.ShrinkSelectionWindow;
import com.moulberry.axiom.editor.windows.selection.SmoothSelectionWindow;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.hooks.WindowExt;
import com.moulberry.axiom.hooks.WorldRenderHook;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.integration.ServerIntegration;
import com.moulberry.axiom.operations.FillOperation;
import com.moulberry.axiom.packets.SupportedProtocol;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.magic_select.MagicSelectionFast;
import com.moulberry.axiom.tools.modify.ModifyTool;
import com.moulberry.axiom.tools.ruler.RulerTool;
import com.moulberry.axiom.tools.shape.ShapeTool;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImDrawData;
import imgui.moulberry92.ImDrawList;
import imgui.moulberry92.ImFont;
import imgui.moulberry92.ImFontAtlas;
import imgui.moulberry92.ImFontConfig;
import imgui.moulberry92.ImFontGlyphRangesBuilder;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiIO;
import imgui.moulberry92.ImGuiViewport;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.internal.ImGuiContext;
import imgui.moulberry92.type.ImBoolean;
import imgui.moulberry92.type.ImString;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

public class EditorUI {
   public static final CustomImGuiImplGlfw imguiGlfw = new CustomImGuiImplGlfw();
   private static final CustomImGuiImplGl3 imguiGl3 = new CustomImGuiImplGl3();
   private static boolean initialized = false;
   private static boolean enabled = false;
   private static boolean isFirstFrame = false;
   private static boolean isFrameHovered = false;
   public static int frameX = 0;
   public static int frameY = 0;
   public static int frameWidth = 1;
   public static int frameHeight = 1;
   public static int viewportSizeX = 1;
   public static int viewportSizeY = 1;
   private static boolean activeLastFrame = false;
   public static boolean canProcessKeybinds = false;
   public static EditorMovementControls movementControls = EditorMovementControls.none();
   private static boolean adjustingTool = false;
   private static float adjustingToolOffsetX = 0.0F;
   private static float adjustingToolOffsetY = 0.0F;
   private static float savedEditorFlightSpeed = -1.0F;
   private static float savedGameFlightSpeed = -1.0F;
   public static Matrix4f lastProjectionMatrix = null;
   public static Quaternionf lastViewQuaternion = null;
   private static ImFont font = null;
   public static ImFont icons = null;
   public static ImFont monospace = null;
   private static String languageCode = null;
   private static boolean wasNavClose = false;
   private static boolean navClose = false;
   private static final Lock deferredCloseLock = new ReentrantLock();
   private static final IntList deferredCloseTextureIds = new IntArrayList();
   private static final List<AutoCloseable> deferredClose = new ArrayList<>();
   private static final ActiveBlockHistory activeBlockHistory = new ActiveBlockHistory();
   private static final BlockList blockList = new BlockList();
   public static final EnumSet<EditorWarningType> warnings = EnumSet.noneOf(EditorWarningType.class);
   public static final List<PendingDepthAction> pendingDepthActions = new ArrayList<>();
   public static final Int2ObjectOpenHashMap<ItemStack> inventoryOverrides = new Int2ObjectOpenHashMap();
   private static CustomBlockState draggingBlockFromWorld = null;
   private static float globalScale = 1.0F;
   private static float contentScale = 1.0F;
   private static int focusViewNextFrame = -1;
   private static float viewportX = 0.0F;
   private static float viewportY = 0.0F;
   private static ImGuiContext imGuiContext = null;
   public static ImGuiIO imGuiIO = null;
   private static String cachedClipboard = "";
   private static boolean cachedClipboardRenderedFrame = false;
   private static long cachedClipboardLastMillis = 0L;
   private static boolean isContextActive = false;

   public static void init() {
      if (!initialized) {
         initialized = true;
         Path path = Axiom.getInstance().getConfigDirectory().resolve("imgui.ini");
         if (!Files.exists(path)) {
            try {
               Files.write(
                  path,
                  "[Window][###Tools]\nPos=0,0\nSize=300,250\nCollapsed=0\nDockId=0x00000003,0\n\n[Window][###Tool Options]\nPos=0,250\nSize=300,750\nCollapsed=0\nDockId=0x00000004,0\n\n[Window][###Clipboard]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,0\n\n[Window][###TargetInfo]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,1\n\n[Window][###Palette]\nPos=1700,200\nSize=300,200\nCollapsed=0\nDockId=0x0000000E,0\n\n[Window][###ActiveBlock]\nPos=1700,400\nSize=300,100\nCollapsed=0\nDockId=0x0000000C,0\n\n[Window][###History]\nPos=1700,500\nSize=300,300\nCollapsed=0\nDockId=0x0000000A,0\n\n[Window][###WorldProperties]\nPos=1700,800\nSize=300,200\nCollapsed=0\nDockId=0x00000008,0\n\n[Docking][Data]\nDockSpace           ID=0x8B93E3BD Window=0xA787BDB4 Pos=0,0 Size=2000,1000 Split=X\nDockNode          ID=0x00000005 Parent=0x8B93E3BD SizeRef=1700,1000 Split=X\n DockNode        ID=0x00000001 Parent=0x00000005 SizeRef=300,1000 Split=Y\n   DockNode      ID=0x00000003 Parent=0x00000001 SizeRef=300,250 Selected=0x80AFE82B\n   DockNode      ID=0x00000004 Parent=0x00000001 SizeRef=300,750 Selected=0xECA27DCB\n DockNode        ID=0x00000002 Parent=0x00000005 SizeRef=1400,1000 CentralNode=1 Selected=0x1F1A625A\nDockNode          ID=0x00000006 Parent=0x8B93E3BD SizeRef=300,1000 Split=Y Selected=0x34064FA7\n DockNode        ID=0x00000007 Parent=0x00000006 SizeRef=300,800 Split=Y Selected=0x34064FA7\n   DockNode      ID=0x00000009 Parent=0x00000007 SizeRef=300,500 Split=Y Selected=0x34064FA7\n     DockNode    ID=0x0000000B Parent=0x00000009 SizeRef=300,400 Split=Y Selected=0x34064FA7\n       DockNode  ID=0x0000000D Parent=0x0000000B SizeRef=300,200 Selected=0x34064FA7\n       DockNode  ID=0x0000000E Parent=0x0000000B SizeRef=300,200 Selected=0x1E514AEA\n     DockNode    ID=0x0000000C Parent=0x00000009 SizeRef=300,100 Selected=0x1D216E21\n   DockNode      ID=0x0000000A Parent=0x00000007 SizeRef=300,300 Selected=0xFE0E9DDF\n DockNode        ID=0x00000008 Parent=0x00000006 SizeRef=300,200 Selected=0x602D8B84"
                     .getBytes()
               );
            } catch (IOException var5) {
            }
         }

         long oldImGuiContext = ImGui.getCurrentContext().ptr;
         imGuiContext = new ImGuiContext(ImGui.createContext().ptr);
         ImGui.setCurrentContext(imGuiContext);
         imGuiIO = new ImGuiIO(ImGui.getIO().ptr);
         Path relativePath = AxiomPlatform.gameDir().relativize(path);
         Axiom.dbg("Using DearImGui config: " + relativePath);
         getIO().setIniFilename(relativePath.toString());
         getIO().addConfigFlags(1);
         getIO().addConfigFlags(128);
         getIO().setConfigMacOSXBehaviors(Minecraft.ON_OSX);
         imguiGlfw.init(Minecraft.getInstance().getWindow().getWindow(), true);
         imguiGl3.init("#version 150");
         contentScale = imguiGlfw.contentScale;
         initFonts(languageCode);
         StyleManager.initialize();
         ImGuiContext currentContext = ImGui.getCurrentContext();
         currentContext.ptr = oldImGuiContext;
         ImGui.setCurrentContext(currentContext);
      }
   }

   private static void addRanges(ImFontGlyphRangesBuilder builder, short[] ranges) {
      for (int i = 0; i < ranges.length && ranges[i] != 0; i += 2) {
         int from = ranges[i] & '\uffff';
         int to = ranges[i + 1] & '\uffff';

         for (int k = from; k <= to; k++) {
            builder.addChar((char)k);
         }
      }
   }

   public static void initFonts(String languageCode) {
      if (languageCode != null) {
         EditorUI.languageCode = languageCode;
      } else {
         languageCode = "en_us";
      }

      if (initialized) {
         ImGuiIO io = getIO();
         ImFontAtlas fonts = io.getFonts();
         fonts.clear();
         int size = (int)(16.0F * getUiScale());
         ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder();
         rangesBuilder.addRanges(fonts.getGlyphRangesDefault());
         if (languageCode.startsWith("uk") || languageCode.startsWith("ru") || languageCode.startsWith("bg")) {
            addRanges(rangesBuilder, fonts.getGlyphRangesCyrillic());
         } else if (languageCode.startsWith("tr")) {
            rangesBuilder.addText("ÇçĞğİıÖöŞşÜü");
         } else if (languageCode.startsWith("pl")) {
            addRanges(rangesBuilder, new short[]{256, 383, 0});
         } else if (languageCode.startsWith("cs")) {
            rangesBuilder.addText("ÁáČčĎďÉéĚěÍíŇňÓóŘřŠšŤťÚúŮůÝýŽž");
         } else if (languageCode.startsWith("he")) {
            addRanges(rangesBuilder, new short[]{1424, 1535, -1251, -1201, 0});
         } else if (languageCode.startsWith("ja")) {
            addRanges(rangesBuilder, fonts.getGlyphRangesJapanese());
         } else if (languageCode.startsWith("zh")) {
            addRanges(rangesBuilder, fonts.getGlyphRangesChineseFull());
         } else if (languageCode.startsWith("ko")) {
            addRanges(rangesBuilder, fonts.getGlyphRangesKorean());
         }

         if (Language.getInstance() instanceof ClientLanguage clientLanguage) {
            for (String value : clientLanguage.storage.values()) {
               rangesBuilder.addText(value);
            }
         }

         rangesBuilder.addChar('⌘');
         rangesBuilder.addChar('⌃');
         rangesBuilder.addChar('⎇');
         rangesBuilder.addChar('⇧');
         rangesBuilder.addChar('❖');
         rangesBuilder.addChar('⚠');
         rangesBuilder.addChar('←');
         rangesBuilder.addChar('↑');
         rangesBuilder.addChar('→');
         rangesBuilder.addChar('↓');

         for (int i = 32; i <= 348; i++) {
            int scancode = GLFW.glfwGetKeyScancode(i);
            if (scancode != -1) {
               String key = GLFW.glfwGetKeyName(i, -1);
               if (key != null) {
                  rangesBuilder.addText(key);
                  rangesBuilder.addText(key.toLowerCase());
                  rangesBuilder.addText(key.toUpperCase());
                  rangesBuilder.addText(key.toLowerCase(Locale.ROOT));
                  rangesBuilder.addText(key.toUpperCase(Locale.ROOT));
               }
            }
         }

         ImFontConfig fontConfig = new ImFontConfig();
         fontConfig.setOversampleH(2);
         fontConfig.setOversampleV(2);
         short[] glyphRanges = rangesBuilder.buildRanges();
         fontConfig.setName("Inter (Medium), 16px");
         font = fonts.addFontFromMemoryTTF(loadFont("inter-medium.ttf"), size, fontConfig, glyphRanges);
         fontConfig.setMergeMode(true);
         if (languageCode.startsWith("he")) {
            io.getFonts().addFontFromMemoryTTF(loadFont("heebo-medium.ttf"), size, fontConfig, glyphRanges);
         } else if (languageCode.startsWith("ja")) {
            io.getFonts().addFontFromMemoryTTF(loadFont("notosansjp-medium.ttf"), size * 5 / 4, fontConfig, glyphRanges);
         } else if (languageCode.startsWith("zh")) {
            io.getFonts().addFontFromMemoryTTF(loadFont("notosanstc-medium.ttf"), size * 5 / 4, fontConfig, glyphRanges);
            io.getFonts().addFontFromMemoryTTF(loadFont("notosanssc-medium.ttf"), size * 5 / 4, fontConfig, glyphRanges);
         } else if (languageCode.startsWith("ko")) {
            io.getFonts().addFontFromMemoryTTF(loadFont("notosanskr-medium.ttf"), size * 5 / 4, fontConfig, glyphRanges);
         }

         ImFontGlyphRangesBuilder emojiRangesBuilder = new ImFontGlyphRangesBuilder();
         emojiRangesBuilder.addChar('✔');
         emojiRangesBuilder.addChar('✖');
         emojiRangesBuilder.addChar('⬆');
         emojiRangesBuilder.addChar('⬇');
         emojiRangesBuilder.addChar('➡');
         emojiRangesBuilder.addChar('⬅');
         emojiRangesBuilder.addChar('☠');
         emojiRangesBuilder.addChar('☢');
         emojiRangesBuilder.addChar('❤');
         emojiRangesBuilder.addChar('✏');
         io.getFonts().addFontFromMemoryTTF(loadFont("noto-emoji-stripped-bold.ttf"), size, fontConfig, emojiRangesBuilder.buildRanges());
         fontConfig.setMergeMode(false);
         fontConfig.setName("Monocraft");
         monospace = fonts.addFontFromMemoryTTF(loadFont("monocraft.ttf"), size, fontConfig, glyphRanges);
         glyphRanges = new short[]{-5888, -5847, 0};
         fontConfig.setName("Icons (don't select)");
         icons = fonts.addFontFromMemoryTTF(loadFont("axiomicons.ttf"), size * 2, fontConfig, glyphRanges);
         fonts.build();
         imguiGl3.updateFontsTexture();
         fontConfig.destroy();
         fonts.clearTexData();
      }
   }

   private static byte[] loadFont(String name) {
      try {
         Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("axiom", name));
         if (resource.isEmpty()) {
            throw new MissingResourceException("Missing font: " + name, "Font", "");
         } else {
            byte[] var3;
            try (InputStream is = resource.get().open()) {
               var3 = is.readAllBytes();
            }

            return var3;
         }
      } catch (IOException var7) {
         throw new RuntimeException(var7);
      }
   }

   public static ImGuiIO getIO() {
      if (!initialized) {
         init();
      }

      return imGuiIO;
   }

   public static Vec3 getMouseForwardsVector() {
      return getMouseForwardsVector(getIO().getMousePosX(), getIO().getMousePosY());
   }

   public static Vec3 getMouseForwardsVector(float mouseX, float mouseY) {
      return isActive() && (isFrameHovered || isMovingCamera()) && lastProjectionMatrix != null && lastViewQuaternion != null
         ? getForwardsVector(mouseX, mouseY)
         : null;
   }

   public static Vec2 getMouseViewportFraction() {
      float x = (getIO().getMousePosX() - viewportX - frameX) / frameWidth;
      float y = (getIO().getMousePosY() - viewportY - frameY) / frameHeight;
      return new Vec2(x, y);
   }

   public static Vec2 getMouseViewportFraction(float mouseX, float mouseY) {
      float x = (mouseX - viewportX - frameX) / frameWidth;
      float y = (mouseY - viewportY - frameY) / frameHeight;
      return new Vec2(x, y);
   }

   public static Vec3 getForwardsVector(float mouseX, float mouseY) {
      float x = (mouseX - viewportX - frameX) / frameWidth * 2.0F - 1.0F;
      float y = (mouseY - viewportY - frameY) / frameHeight * 2.0F - 1.0F;
      return getForwardsVectorRaw(x, y);
   }

   public static Vec3 getForwardsVectorRaw(float x, float y) {
      if (isMovingCamera() || !(x < -1.0F) && !(x > 1.0F) && !(y < -1.0F) && !(y > 1.0F)) {
         Matrix4f matrix = new Matrix4f(lastProjectionMatrix);
         matrix.invert();
         Vector4f forwards = new Vector4f(x, y, 0.0F, 1.0F);
         forwards.mul(matrix);
         return new Vec3(forwards.x(), -forwards.y(), forwards.z()).normalize();
      } else {
         return null;
      }
   }

   public static Vec3 getMouseLookVectorFromForwards(Vec3 forwards) {
      if (forwards == null) {
         return null;
      } else {
         Vector3f view = forwards.toVector3f();
         view.rotate(lastViewQuaternion);
         return new Vec3(view.x(), view.y(), view.z()).normalize();
      }
   }

   public static Vec3 getMouseLookVector(float mouseX, float mouseY) {
      return getMouseLookVectorFromForwards(getMouseForwardsVector(mouseX, mouseY));
   }

   public static Vec3 getMouseLookVector() {
      return getMouseLookVectorFromForwards(getMouseForwardsVector());
   }

   public static void clearActiveBlockHistory() {
      activeBlockHistory.clear();
   }

   public static BlockState getActiveBlock() {
      return activeBlockHistory.getActive().getVanillaState();
   }

   public static boolean isMovingCamera() {
      return movementControls != EditorMovementControls.none()
         || imguiGlfw.isGrabbed() && imguiGlfw.getMouseHandledBy() == CustomImGuiImplGlfw.MouseHandledBy.GAME;
   }

   public static boolean allowGameInputWhileCaptureKeyboard() {
      return movementControls.allowGameInputWhileCaptureKeyboard();
   }

   public static void setupMainViewport() {
      Window window = Minecraft.getInstance().getWindow();
      int frameBottom = window.height - (frameY + frameHeight);
      GlStateManager._viewport(
         frameX * window.getWidth() / window.getScreenWidth(),
         frameBottom * window.getHeight() / window.getScreenHeight(),
         Math.max(1, frameWidth * window.getWidth() / window.getScreenWidth()),
         Math.max(1, frameHeight * window.getHeight() / window.getScreenHeight())
      );
   }

   public static float getUiScale() {
      return globalScale * contentScale;
   }

   public static double getNewMouseX(double x) {
      return x - frameX;
   }

   public static double getNewMouseY(double y) {
      return y - frameY;
   }

   public static int getNewGameWidth(float scale) {
      return Math.max(1, Math.round(frameWidth * scale));
   }

   public static int getNewGameHeight(float scale) {
      return Math.max(1, Math.round(frameHeight * scale));
   }

   public static boolean isFirstFrame() {
      return isFirstFrame;
   }

   private static boolean isActiveInternal() {
      LocalPlayer player = Minecraft.getInstance().player;
      return AxiomClient.isAxiomActive(GameType.SPECTATOR)
         && enabled
         && Minecraft.getInstance().level != null
         && player != null
         && player == Minecraft.getInstance().cameraEntity
         && AxiomClient.hasPermission(AxiomPermission.EDITOR_USE);
   }

   public static void enable() {
      if (!enabled) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (!AxiomClient.hasPermission(AxiomPermission.EDITOR_USE)) {
            ChatUtils.error("The server has disallowed the use of the editor");
         } else if (!AxiomClient.hasPermission(AxiomPermission.PLAYER_GAMEMODE_SPECTATOR)) {
            ChatUtils.error("The server has disallowed switching to spectator, which is required for the Editor");
         } else {
            ServerIntegration.changeGameMode(GameType.SPECTATOR);
            if (ServerIntegration.getGameType() == GameType.SPECTATOR) {
               enabled = true;
               Axiom.configuration.internal.hadEditorUIOpen = true;
               inventoryOverrides.clear();
               if (Axiom.configuration.movement.separateFlightSpeeds && player != null) {
                  savedGameFlightSpeed = player.getAbilities().getFlyingSpeed();
                  if (savedEditorFlightSpeed > 0.0F) {
                     ServerIntegration.changeFlySpeed(savedEditorFlightSpeed);
                  }
               }
            }
         }
      }
   }

   public static void disable() {
      if (enabled) {
         ServerIntegration.changeGameMode(GameType.CREATIVE);
         enabled = false;
         Axiom.configuration.internal.hadEditorUIOpen = false;
         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            inventoryOverrides.clear();
         } else {
            if (ServerIntegration.getGameType() == GameType.CREATIVE) {
               ObjectIterator var1 = inventoryOverrides.int2ObjectEntrySet().iterator();

               while (var1.hasNext()) {
                  Entry<ItemStack> entry = (Entry<ItemStack>)var1.next();
                  player.getInventory().setItem(entry.getIntKey(), (ItemStack)entry.getValue());
                  int index = entry.getIntKey();
                  if (index < 9) {
                     index += 36;
                  }

                  ServerIntegration.sendPacketAfterUpdates(new ServerboundSetCreativeModeSlotPacket(index, (ItemStack)entry.getValue()));
               }

               inventoryOverrides.clear();
            }

            if (Axiom.configuration.movement.separateFlightSpeeds) {
               savedEditorFlightSpeed = player.getAbilities().getFlyingSpeed();
               if (savedGameFlightSpeed > 0.0F) {
                  ServerIntegration.changeFlySpeed(savedGameFlightSpeed);
               }
            }
         }
      }
   }

   public static void toggleEnabled() {
      if (enabled) {
         disable();
      } else {
         enable();
      }
   }

   public static void deferredCloseTextureId(int textureId) {
      deferredCloseLock.lock();

      try {
         deferredCloseTextureIds.add(textureId);
      } finally {
         deferredCloseLock.unlock();
      }
   }

   public static void deferredClose(AutoCloseable autoCloseable) {
      deferredCloseLock.lock();

      try {
         deferredClose.add(autoCloseable);
      } finally {
         deferredCloseLock.unlock();
      }
   }

   public static boolean isEnabled() {
      return enabled;
   }

   public static boolean isActive() {
      return activeLastFrame;
   }

   private static void transitionActiveState(boolean active) {
      if (activeLastFrame != active) {
         activeLastFrame = active;
         Minecraft minecraft = Minecraft.getInstance();
         Window window = minecraft.getWindow();
         if (window.getWidth() > 0 && window.getWidth() <= 32768 && window.getHeight() > 0 && window.getHeight() <= 32768) {
            updateWindowSize();
         }

         imguiGlfw.ungrab();
         if (!activeLastFrame) {
            if (minecraft.gameMode != null) {
               if (minecraft.screen == null) {
                  minecraft.mouseHandler.releaseMouse();
                  minecraft.mouseHandler.grabMouse();
               } else {
                  minecraft.mouseHandler.grabMouse();
                  minecraft.mouseHandler.releaseMouse();
               }

               minecraft.mouseHandler.setIgnoreFirstMove();
            }
         } else {
            long handle = ImGui.getMainViewport().getPlatformHandle();
            if (GLFW.glfwGetInputMode(handle, 208897) != 212993) {
               GLFW.glfwSetInputMode(handle, 208897, 212993);
               GLFW.glfwSetCursorPos(handle, ImGui.getMainViewport().getSizeX() / 2.0F, ImGui.getMainViewport().getSizeY() / 2.0F);
            }
         }

         imguiGlfw.setViewportWindowsHidden(!activeLastFrame);
         ServerIntegration.syncFlySpeed();
         BuilderToolManager.setToolSlotActive(false);
         Placement.INSTANCE.updateCutout();
      }
   }

   public static boolean hasImGuiContext() {
      return imGuiContext != null;
   }

   public static boolean isImGuiContextActive() {
      return imGuiContext != null && isContextActive;
   }

   public static long pushImGuiContext() {
      if (imGuiContext == null) {
         return ImGui.getCurrentContext().ptr;
      } else {
         isContextActive = true;
         long oldImGuiContext = ImGui.getCurrentContext().ptr;
         ImGui.setCurrentContext(imGuiContext);
         return oldImGuiContext;
      }
   }

   public static void popImGuiContext(long oldContext) {
      isContextActive = imGuiContext != null && oldContext == imGuiContext.ptr;
      ImGuiContext currentContext = ImGui.getCurrentContext();
      currentContext.ptr = oldContext;
      ImGui.setCurrentContext(currentContext);
   }

   public static void drawOverlay() {
      if (initialized || !(Minecraft.getInstance().getOverlay() instanceof LoadingOverlay)) {
         init();
         if (StaticValues.gameHasTicked) {
            cachedClipboardRenderedFrame = true;
            GlStateManager._disableColorLogicOp();
            long oldImGuiContext = pushImGuiContext();

            try {
               drawOverlayInternal();
            } finally {
               popImGuiContext(oldImGuiContext);
            }
         }
      }
   }

   private static void drawOverlayInternal() {
      int oldFrameX = frameX;
      int oldFrameY = frameY;
      int oldFrameWidth = frameWidth;
      int oldFrameHeight = frameHeight;
      viewportX = ImGui.getMainViewport().getPosX();
      viewportY = ImGui.getMainViewport().getPosY();
      if (!initialized) {
         throw new IllegalStateException("Tried to use EditorUI while it was not initialized");
      } else {
         deferredCloseLock.lock();

         try {
            IntListIterator grabbedMouseDeltaX = deferredCloseTextureIds.iterator();

            while (grabbedMouseDeltaX.hasNext()) {
               int id = (Integer)grabbedMouseDeltaX.next();
               GlStateManager._deleteTexture(id);
            }

            deferredCloseTextureIds.clear();

            for (AutoCloseable closeable : deferredClose) {
               try {
                  closeable.close();
               } catch (Exception var39) {
               }
            }

            deferredClose.clear();
         } finally {
            deferredCloseLock.unlock();
         }

         if (!(Minecraft.getInstance().screen instanceof ProgressScreen) && !(Minecraft.getInstance().screen instanceof ReceivingLevelScreen)) {
            if (!isActiveInternal()) {
               isFirstFrame = true;
               pendingDepthActions.clear();
               transitionActiveState(false);
               if (!AxiomClient.isAxiomActive(GameType.SPECTATOR)) {
                  enabled = false;
               }

               imguiGlfw.updateReleaseAllKeys(true);
               if (ImageReferenceWindows.hasReferenceInGameUI()) {
                  long oldImGuiContext = pushImGuiContext();
                  imguiGlfw.newFrame();
                  ImGui.newFrame();
                  ImageReferenceWindows.render();
                  ImGui.render();
                  ImGuiHelper.endFrame();
                  long ctx = GLFW.glfwGetCurrentContext();
                  ImGui.updatePlatformWindows();
                  ImGui.renderPlatformWindowsDefault();
                  GLFW.glfwMakeContextCurrent(ctx);
                  ImDrawData drawData = ImGui.getDrawData();
                  if (drawData != null) {
                     imguiGl3.renderDrawData(drawData);
                  }

                  popImGuiContext(oldImGuiContext);
               }
            } else {
               imguiGlfw.updateReleaseAllKeys(false);
               isFirstFrame = !activeLastFrame;
               if (WorldRenderHook.hasDistance) {
                  for (PendingDepthAction pendingDepthAction : pendingDepthActions) {
                     if (pendingDepthAction == PendingDepthAction.ARCBALL) {
                        movementControls = EditorMovementControls.arcballFromDepth();
                        if (movementControls != EditorMovementControls.none()) {
                           int key = Keybinds.ARCBALL_CAMERA.getKey();
                           if (key != 0) {
                              imguiGlfw.setGrabbed(false, key, frameX + frameWidth / 2.0F, frameY + frameHeight / 2.0F);
                           }
                        }
                     } else if (pendingDepthAction == PendingDepthAction.PAN) {
                        movementControls = EditorMovementControls.pan();
                     } else {
                        int wheelY = pendingDepthAction == PendingDepthAction.SCROLL_POSITIVE ? 1 : -1;
                        Entity entity = Minecraft.getInstance().player;
                        Vec3 forwards = getMouseForwardsVector();
                        if (entity != null && forwards != null) {
                           Vector3f view = forwards.toVector3f().rotate(lastViewQuaternion).normalize();
                           Vec3 pos = entity.position();
                           double length = WorldRenderHook.distance * 0.05F;
                           if (length > 4.0) {
                              length = 4.0F + (float)Math.sqrt(length - 4.0);
                           }

                           if (length < 0.4F) {
                              length = 0.4F;
                           }

                           length *= wheelY;
                           if (isMoveQuickDown()) {
                              length *= 2.0;
                           }

                           entity.setPos(pos.add(new Vec3(view).multiply(length, length, length)));
                        }
                     }
                  }
               }

               pendingDepthActions.clear();
               double grabbedMouseDeltaX = imguiGlfw.getGrabbedMouseDeltaX();
               double grabbedMouseDeltaY = imguiGlfw.getGrabbedMouseDeltaY();
               if (movementControls.shouldStop(imguiGlfw.isGrabbed())) {
                  movementControls = EditorMovementControls.none();
               } else {
                  movementControls.update(grabbedMouseDeltaX, grabbedMouseDeltaY);
               }

               if (!ImGui.isAnyMouseDown()) {
                  float newGlobalScale = Axiom.configuration.internal.globalScale;
                  newGlobalScale = (int)(newGlobalScale * 16.0F) / 16.0F;
                  if (newGlobalScale < 0.25) {
                     newGlobalScale = 0.25F;
                  }

                  if (newGlobalScale > 4.0F) {
                     newGlobalScale = 4.0F;
                  }

                  float newContentScale = (int)(imguiGlfw.contentScale * 16.0F) / 16.0F;
                  if (newContentScale < 0.125) {
                     newContentScale = 0.125F;
                  }

                  if (newContentScale > 8.0F) {
                     newContentScale = 8.0F;
                  }

                  if (globalScale != newGlobalScale || contentScale != newContentScale) {
                     int oldFontSize = (int)(16.0F * getUiScale());
                     globalScale = newGlobalScale;
                     contentScale = newContentScale;
                     if (oldFontSize != (int)(16.0F * getUiScale())) {
                        Axiom.dbg("Resizing EditorUI fonts from: " + oldFontSize + " to: " + (int)(16.0F * getUiScale()));
                        initFonts(languageCode);
                     }
                  }
               }

               imguiGlfw.newFrame();
               ImGui.newFrame();
               canProcessKeybinds = !ImGui.isPopupOpen("", 3072)
                  && !getIO().getWantTextInput()
                  && !ImGuiHelper.getWantsSpecialInput()
                  && !EditorWindowType.KEYBINDS.isOpen();
               navClose = ImGui.isKeyPressed(526);
               if (wasNavClose != navClose) {
                  wasNavClose = navClose;
               } else if (wasNavClose) {
                  navClose = false;
               }

               if (ImGui.isPopupOpen("", 3072)) {
                  getIO().addConfigFlags(1);
               } else {
                  getIO().removeConfigFlags(1);
               }

               char controlIcon = (char)(Minecraft.ON_OSX ? 8984 : 8963);
               MainMenuBar.render();
               ImGuiViewport viewport = ImGui.getMainViewport();
               ImVec2 workPos = viewport.getWorkPos();
               ImVec2 workSize = viewport.getWorkSize();
               float sizeX = workSize.x;
               float sizeY = Math.min(workSize.y, ImGui.getFrameHeight());
               float posX = workPos.x;
               float posY = workPos.y + workSize.y - sizeY;
               viewport.setWorkSize(workSize.x, workSize.y - sizeY);
               ImGui.setNextWindowViewport(viewport.getID());
               ImGui.setNextWindowPos(posX, posY);
               ImGui.setNextWindowSize(sizeX, sizeY);
               if (ImGui.begin("##MainStatusBar", 1295) && ImGui.beginMenuBar()) {
                  List<String> hintsToShow = new ArrayList<>();
                  boolean holdCtrlForMoreOptions = false;
                  if (ToolManager.isToolActive()) {
                     String esc = ToolManager.getCurrentTool().listenForEsc();
                     String enter = ToolManager.getCurrentTool().listenForEnter();
                     if (esc != null) {
                        hintsToShow.add(AxiomI18n.get("axiom.editorui.escape_key") + " - " + esc);
                     }

                     if (enter != null) {
                        hintsToShow.add(AxiomI18n.get("axiom.editorui.enter_key") + " - " + enter);
                     }
                  }

                  if (hintsToShow.isEmpty()) {
                     Set<Keybind> left = Keybinds.keybindsForKey.get(-1);
                     Set<Keybind> middle = Keybinds.keybindsForKey.get(-3);
                     Set<Keybind> right = Keybinds.keybindsForKey.get(-2);
                     String mmb;
                     String rmb;
                     String lmb;
                     if (isCtrlOrCmdDown()) {
                        lmb = getPrioritizedKeybind(left, true);
                        mmb = getPrioritizedKeybind(middle, true);
                        rmb = getPrioritizedKeybind(right, true);
                     } else {
                        lmb = getPrioritizedKeybind(left, false);
                        mmb = getPrioritizedKeybind(middle, false);
                        rmb = getPrioritizedKeybind(right, false);
                     }

                     hintsToShow.add(AxiomI18n.get("axiom.editorui.left_mouse_short") + " - " + lmb);
                     hintsToShow.add(AxiomI18n.get("axiom.editorui.middle_mouse_short") + " - " + mmb);
                     hintsToShow.add(AxiomI18n.get("axiom.editorui.right_mouse_short") + " - " + rmb);
                     holdCtrlForMoreOptions = true;
                  }

                  sizeX = 150.0F;

                  for (String hint : hintsToShow) {
                     sizeX = Math.max(sizeX, ImGuiHelper.calcTextWidth(hint));
                  }

                  sizeX += 50.0F;

                  for (int i = 0; i < hintsToShow.size(); i++) {
                     ImGui.text(hintsToShow.get(i));
                     ImGui.sameLine(sizeX * (1 + i));
                     ImGui.separator();
                  }

                  int selectedBlocks = Selection.selectedBlockCount();
                  if (selectedBlocks > 0) {
                     ImGui.text(AxiomI18n.get("axiom.history_description.selected", NumberFormat.getInstance().format((long)selectedBlocks)));
                  } else if (!isCtrlOrCmdDown() && holdCtrlForMoreOptions) {
                     ImGui.textDisabled(AxiomI18n.get("axiom.editorui.hold_for_more_options", controlIcon));
                  }

                  boolean showToolSmoothing = ToolManager.isToolActive() && ToolManager.getCurrentTool().showToolSmoothing();
                  boolean showTeleport = !ClientEvents.teleportablePlayers.isEmpty();
                  float sliderWidth = 150.0F * getUiScale();
                  String toolSmoothingText = AxiomI18n.get("axiom.editorui.tool_stabilization");
                  String speedText = AxiomI18n.get("axiom.editorui.speed");
                  String teleport = AxiomI18n.get("axiom.editorui.teleport");
                  float speedWidth = ImGuiHelper.calcTextWidth(speedText);
                  float start = ImGui.getWindowWidth() - sliderWidth - ImGui.getStyle().getItemSpacingX() - speedWidth - 1.0F;
                  if (showToolSmoothing) {
                     float toolSmoothingWidth = ImGuiHelper.calcTextWidth(toolSmoothingText);
                     start -= toolSmoothingWidth + sliderWidth + ImGui.getStyle().getItemSpacingX() * 3.0F;
                  }

                  if (showTeleport) {
                     float teleportWidth = ImGuiHelper.calcTextWidth(teleport);
                     start -= teleportWidth + ImGui.getStyle().getItemSpacingX() * 3.0F;
                  }

                  ImGui.setCursorPosX(start);
                  if (showToolSmoothing) {
                     ImGui.text(toolSmoothingText);
                     ImGui.setNextItemWidth(sliderWidth);
                     float[] toolSmoothing = new float[]{Axiom.configuration.editor.toolStabilization};
                     ImGui.sliderFloat("##ToolSmoothingSlider", toolSmoothing, 0.0F, 16.0F, "%.1f");
                     Axiom.configuration.editor.toolStabilization = toolSmoothing[0];
                     ImGui.separator();
                  }

                  if (showTeleport) {
                     if (ImGui.menuItem(teleport)) {
                        ImGui.openPopup("##TeleportToPlayers");
                     }

                     ImGui.separator();
                  }

                  if (ImGui.beginPopup("##TeleportToPlayers")) {
                     for (PlayerInfo player : ClientEvents.teleportablePlayers) {
                        String name = player.getProfile().getName();
                        if (ImGui.menuItem(name)) {
                           ChatUtils.sendCommand("tp " + player.getProfile().getId());
                           ImGui.closeCurrentPopup();
                        }
                     }

                     ImGui.endPopup();
                  }

                  ImGui.text(speedText);
                  ImGui.setNextItemWidth(sliderWidth);
                  LocalPlayer playerx = Minecraft.getInstance().player;
                  if (playerx != null) {
                     Abilities abilities = playerx.getAbilities();
                     float flyingSpeed = abilities.getFlyingSpeed() / 0.05F;
                     float[] speed = new float[]{flyingSpeed};
                     if (ImGui.sliderFloat("##FlightSpeedSlider", speed, 1.0F, 10.0F, "%.2f") && speed[0] != flyingSpeed) {
                        ServerIntegration.changeFlySpeed(speed[0] * 0.05F);
                     }
                  } else {
                     ImGui.sliderFloat("##FlightSpeedSlider", new float[]{1.0F}, 1.0F, 10.0F);
                  }

                  ImGui.endMenuBar();
               }

               ImGui.end();
               ImGui.setNextWindowBgAlpha(0.0F);
               int mainDock = ImGui.dockSpaceOverViewport(-1953242179, ImGui.getMainViewport(), 4);
               imgui.moulberry92.internal.ImGui.dockBuilderGetCentralNode(mainDock).addLocalFlags(4096);
               isFrameHovered = false;
               ImGui.setNextWindowDockID(mainDock);
               ImGuiHelper.pushStyleVar(2, 0.0F, 0.0F);
               float titleBarHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0F;
               boolean fireCancelNavInput = false;
               if (ImGui.begin("Main", 65966)) {
                  ImGuiHelper.popStyleVar();
                  sizeY = ImGui.getCursorScreenPosY();
                  ServerConfig config = Axiom.getInstance().serverConfig;
                  if (config != null) {
                     ImGui.pushID("Views");
                     List<View> views = ViewManager.getViews();
                     ImDrawList backgroundDrawList = ImGui.getBackgroundDrawList();
                     float cursorScreenPosX = ImGui.getCursorScreenPosX();
                     float cursorScreenPosY = ImGui.getCursorScreenPosY();
                     float tabBarHeight = ImGui.getFontSize() + ImGui.getStyle().getFramePaddingY() * 2.0F;
                     backgroundDrawList.addRectFilled(
                        cursorScreenPosX, cursorScreenPosY, cursorScreenPosX + ImGui.getWindowWidth(), cursorScreenPosY + tabBarHeight, ImGui.getColorU32(10)
                     );
                     ImGui.beginTabBar("##Bar", 3);
                     boolean disabled = !AxiomClient.hasPermission(AxiomPermission.EDITOR_VIEWS)
                        || !ClientEvents.serverSupportsProtocol(SupportedProtocol.TELEPORT);
                     if (disabled) {
                        ImGui.beginDisabled();
                     }

                     if (views.size() < 16) {
                        ImGuiHelper.pushStyleVar(12, ImGui.getStyle().getTabRounding());
                        if (ImGui.tabItemButton("+", 128)) {
                           ViewManager.addNewView();
                        }

                        ImGuiHelper.popStyleVar();
                        if (disabled) {
                           ImGui.endDisabled();
                           ImGuiHelper.tooltip("Server has disallowed the use of Editor views", 1024);
                           ImGui.beginDisabled();
                        }
                     }

                     int removeView = -1;

                     for (int i = 0; i < views.size(); i++) {
                        View viewx = views.get(i);
                        int flags = focusViewNextFrame == i ? 2 : 0;
                        ImBoolean open = new ImBoolean(true);
                        boolean tabActive;
                        if (views.size() == 1) {
                           tabActive = ImGui.beginTabItem(viewx.name + "###" + viewx.uuid, flags);
                        } else {
                           tabActive = ImGui.beginTabItem(viewx.name + "###" + viewx.uuid, open, flags);
                        }

                        if (ImGui.isItemClicked(0)) {
                           viewx.teleportPinned(Minecraft.getInstance().player);
                        }

                        boolean openRename = false;
                        if (ImGui.isItemClicked(1)) {
                           ImGui.openPopup("##" + viewx.uuid + "Edit");
                        }

                        if (ImGuiHelper.beginPopup("##" + viewx.uuid + "Edit")) {
                           ImGui.text(viewx.name);
                           ImGui.separator();
                           if (ImGui.menuItem(AxiomI18n.get("axiom.widget.rename"))) {
                              openRename = true;
                              ImGui.closeCurrentPopup();
                           }

                           if (views.size() > 1 && ImGui.menuItem(AxiomI18n.get("axiom.widget.delete"))) {
                              open.set(false);
                              ImGui.closeCurrentPopup();
                           }

                           ImGui.separator();
                           if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.pin_world"), viewx.pinLevel)) {
                              viewx.pinLevel = !viewx.pinLevel;
                              ViewManager.dirty();
                           }

                           if (!viewx.pinLevel) {
                              ImGui.beginDisabled();
                           }

                           if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.pin_location"), viewx.pinLevel && viewx.pinLocation)) {
                              viewx.pinLocation = !viewx.pinLocation;
                              ViewManager.dirty();
                           }

                           if (!viewx.pinLevel) {
                              ImGui.endDisabled();
                           }

                           ImGui.endPopup();
                        }

                        if (openRename) {
                           ImGui.openPopup("##" + viewx.uuid + "Rename");
                        }

                        if (ImGuiHelper.beginPopup("##" + viewx.uuid + "Rename")) {
                           ImString string = new ImString(viewx.name, 64);
                           ImGui.setKeyboardFocusHere();
                           if (ImGui.inputText("##Input", string)) {
                              viewx.name = ImGuiHelper.getString(string);
                           }

                           if (ImGui.isItemDeactivatedAfterEdit()) {
                              ImGui.closeCurrentPopup();
                              ViewManager.dirty();
                           }

                           ImGui.endPopup();
                        }

                        if (tabActive) {
                           viewx.markActive(Minecraft.getInstance().player);
                           ImGui.endTabItem();
                        } else {
                           viewx.markInactive();
                        }

                        if (!open.get()) {
                           removeView = i;
                        }
                     }

                     if (removeView != -1 && views.size() > 1) {
                        views.remove(removeView);
                     }

                     if (disabled) {
                        ImGui.endDisabled();
                     }

                     ImGui.endTabBar();
                     ImGui.popID();
                  }

                  posY = ImGui.getWindowContentRegionMinX();
                  float maxX = ImGui.getWindowContentRegionMaxX();
                  float minY = ImGui.getWindowContentRegionMinY() + titleBarHeight;
                  float maxY = ImGui.getWindowContentRegionMaxY();
                  if (getIO().hasConfigFlags(1024)) {
                     frameX = (int)(ImGui.getWindowPosX() - ImGui.getWindowViewport().getPosX() + posY);
                     frameY = (int)(ImGui.getWindowPosY() - ImGui.getWindowViewport().getPosY() + minY);
                  } else {
                     frameX = (int)(ImGui.getWindowPosX() + posY);
                     frameY = (int)(ImGui.getWindowPosY() + minY);
                  }

                  frameWidth = (int)Math.max(1.0F, maxX - posY);
                  frameHeight = (int)Math.max(1.0F, maxY - minY);
                  viewportSizeX = (int)ImGui.getMainViewport().getSizeX();
                  viewportSizeY = (int)ImGui.getMainViewport().getSizeY();
                  if (frameX != oldFrameX || frameY != oldFrameY || frameWidth != oldFrameWidth || frameHeight != oldFrameHeight) {
                     updateWindowSize();
                  }

                  if (!Axiom.configuration.internal.askedTutorialPreference) {
                     ImGui.setNextWindowPos(frameX + frameWidth * 0.5F, frameY + frameHeight * 0.5F, 1, 0.5F, 0.5F);
                     ImGui.setNextWindowSizeConstraints(420.0F, 0.0F, 420.0F, frameHeight * 0.6F);
                     ImGuiHelper.pushStyleVar(4, 2.0F);
                     ImGuiHelper.pushStyleColor(5, -16711681);
                     if (ImGui.begin("##ParallaxTutorialPrompt", 528739)) {
                        ImGui.pushTextWrapPos();
                        ImGui.text(AxiomI18n.get("axiom.tutorial.prompt"));
                        ImGui.popTextWrapPos();
                        if (ImGui.button(AxiomI18n.get("axiom.tutorial.prompt.yes"))) {
                           Axiom.configuration.internal.askedTutorialPreference = true;
                           Axiom.configuration.saveToDefaultFolder();
                           TutorialManager.initialize();
                        }

                        ImGui.sameLine();
                        if (ImGui.button(AxiomI18n.get("axiom.tutorial.prompt.no"))) {
                           Axiom.configuration.internal.askedTutorialPreference = true;
                           Axiom.configuration.saveToDefaultFolder();
                           TutorialManager.skip();
                        }
                     }

                     ImGui.end();
                     ImGuiHelper.popStyleColor();
                     ImGuiHelper.popStyleVar();
                  }

                  if (!Selection.getSelectionBuffer().isEmpty()) {
                     Tutorial.SELECTION.initiateIfNotCompleted();
                  }

                  TutorialStage currentStage = TutorialManager.getCurrentStage();
                  if (currentStage != null && currentStage.getLinkedWindow() == null) {
                     ImGui.setNextWindowPos(frameX + frameWidth * 0.5F, frameY + frameHeight * 0.25F, 1, 0.5F, 0.5F);
                     currentStage.render();
                  } else if (ClientEvents.updateMessage != null) {
                     ImGui.setNextWindowPos(frameX + frameWidth * 0.5F, frameY + frameHeight * 0.75F, 1, 0.5F, 0.5F);
                     ImGui.setNextWindowSizeConstraints(550.0F, 0.0F, 550.0F, frameHeight * 0.45F);
                     ImGuiHelper.pushStyleVar(4, 2.0F);
                     ImGuiHelper.pushStyleColor(5, -16711681);
                     if (ImGui.begin("##UpdatePopup", 528739)) {
                        ImGui.pushTextWrapPos();
                        ImGui.text(ClientEvents.updateMessage);
                        ImGui.popTextWrapPos();
                        if (ImGui.button(AxiomI18n.get("axiom.editorui.update"))) {
                           try {
                              Util.getPlatform().openUri(new URI("https://axiom.moulberry.com/download"));
                           } catch (Exception var38) {
                           }

                           ClientEvents.updateMessage = null;
                           Axiom.configuration.internal.nextUpdateNag = System.currentTimeMillis() + ChronoUnit.DAYS.getDuration().getSeconds() * 3000L;
                        }

                        ImGui.sameLine();
                        if (ImGui.button(AxiomI18n.get("axiom.editorui.skip_update"))) {
                           ClientEvents.updateMessage = null;
                           Axiom.configuration.internal.nextUpdateNag = System.currentTimeMillis() + ChronoUnit.DAYS.getDuration().getSeconds() * 3000L;
                        }
                     }

                     ImGui.end();
                     ImGuiHelper.popStyleColor();
                     ImGuiHelper.popStyleVar();
                  }

                  if (!warnings.isEmpty()) {
                     EditorWarningType type = warnings.iterator().next();
                     switch (type) {
                        case SPECTATORS_GENERATE_CHUNKS:
                           ImGui.setNextWindowPos(frameX + frameWidth * 0.5F, frameY + frameHeight * 0.75F, 1, 0.5F, 0.5F);
                           ImGui.setNextWindowSize(350.0F, 0.0F);
                           if (ImGui.begin("##WarningPopup", 528747)) {
                              ImGui.pushTextWrapPos();
                              ImGui.textColored(-7829249, AxiomI18n.get("axiom.editorui.spectators_generate_chunks_warning"));
                              ImGui.popTextWrapPos();
                           }

                           ImGui.end();
                     }
                  }

                  if (!Axiom.configuration.internal.pickBlockDrag) {
                     draggingBlockFromWorld = null;
                  } else {
                     ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX() + 8.0F, sizeY + titleBarHeight + 8.0F);
                     int leftClickId = -1;
                     int middleClickId = -3;
                     int rightClickId = -2;
                     int pickBlockId = Keybinds.PICK_BLOCK.getKey();
                     if ((pickBlockId == leftClickId || pickBlockId == middleClickId || pickBlockId == rightClickId)
                        && (draggingBlockFromWorld != null || Keybinds.PICK_BLOCK.areAllModifiersDown())) {
                        int buttonFlags = 4;
                        if (pickBlockId == leftClickId) {
                           buttonFlags = 1;
                        } else if (pickBlockId == rightClickId) {
                           buttonFlags = 2;
                        }

                        if (ImGui.invisibleButton("##WorldDragDrop", frameWidth - 16, frameHeight - 16, buttonFlags)) {
                           isFrameHovered = true;
                           RayCaster.RaycastResult result = Tool.raycastBlock();
                           if (result != null) {
                              BlockState blockState = Minecraft.getInstance().level.getBlockState(result.blockPos());
                              CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
                              activeBlockHistory.setActive(Objects.requireNonNullElse(customBlockState, (CustomBlockState)blockState));
                           }
                        }

                        boolean stopDragging = !Keybinds.PICK_BLOCK.isDown();
                        if (ImGui.beginDragDropSource()) {
                           if (draggingBlockFromWorld == null) {
                              isFrameHovered = true;
                              RayCaster.RaycastResult result = Tool.raycastBlock();
                              if (result != null) {
                                 BlockState blockState = Minecraft.getInstance().level.getBlockState(result.blockPos());
                                 CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
                                 draggingBlockFromWorld = Objects.requireNonNullElse(customBlockState, (CustomBlockState)blockState);
                              }
                           }

                           if (draggingBlockFromWorld != null) {
                              ImGui.setDragDropPayload("BlockState", draggingBlockFromWorld);
                              ImGuiHelper.drawBlockState(
                                 ImGui.getForegroundDrawList(), draggingBlockFromWorld, ImGui.getCursorScreenPosX(), ImGui.getCursorScreenPosY(), 32.0F
                              );
                              ImGui.dummy(32.0F, 32.0F);
                              stopDragging = false;
                           }

                           ImGui.endDragDropSource();
                        }

                        if (stopDragging) {
                           draggingBlockFromWorld = null;
                        }
                     }

                     ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX() + 8.0F, sizeY + titleBarHeight + 8.0F);
                     ImGui.dummy(frameWidth - 16, frameHeight - 16);
                     if (ImGui.beginDragDropTarget()) {
                        EditorPalette droppedPalette = (EditorPalette)ImGui.acceptDragDropPayload(EditorPalette.class);
                        List<CustomBlockState> blockStates;
                        if (droppedPalette != null) {
                           blockStates = new ArrayList<>();

                           for (CustomBlockStateOrTombstone block : droppedPalette.getBlocks()) {
                              if (block instanceof CustomBlockState customBlockState) {
                                 blockStates.add(customBlockState);
                              }
                           }
                        } else {
                           CustomBlockState droppedBlockState = (CustomBlockState)ImGui.acceptDragDropPayload("BlockState");
                           if (droppedBlockState != null) {
                              blockStates = List.of(droppedBlockState);
                           } else {
                              DragDropPayloads.PaletteBlock droppedPaletteBlock = (DragDropPayloads.PaletteBlock)ImGui.acceptDragDropPayload(
                                 DragDropPayloads.PaletteBlock.class
                              );
                              if (droppedPaletteBlock != null) {
                                 blockStates = List.of(droppedPaletteBlock.state());
                              } else {
                                 blockStates = List.of();
                              }
                           }
                        }

                        if (!blockStates.isEmpty()) {
                           isFrameHovered = true;
                           RayCaster.RaycastResult result = Tool.raycastBlock(true, true, true);
                           if (result != null) {
                              Level level = Minecraft.getInstance().level;
                              if (level != null) {
                                 if (result.isSelection()) {
                                    if (blockStates.size() == 1) {
                                       FillOperation.fill(blockStates.get(0).getVanillaState());
                                    } else {
                                       ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
                                       Random random = ThreadLocalRandom.current();
                                       Selection.getSelectionBuffer()
                                          .forEach(
                                             (x, y, z) -> chunkedBlockRegion.addBlockWithoutDirty(
                                                x, y, z, blockStates.get(random.nextInt(0, blockStates.size())).getVanillaState()
                                             )
                                          );
                                       chunkedBlockRegion.dirtyAll();
                                       String countString = NumberFormat.getInstance().format((long)Selection.getSelectionBuffer().size());
                                       String historyDescription = AxiomI18n.get("axiom.history_description.painted", countString);
                                       RegionHelper.pushBlockRegionChange(chunkedBlockRegion, historyDescription);
                                    }
                                 } else {
                                    BlockState blockx = level.getBlockState(result.blockPos());
                                    if (!blockx.isAir() && (blockStates.size() != 1 || blockx != blockStates.get(0).getVanillaState())) {
                                       ChunkedBooleanRegion region = new ChunkedBooleanRegion();
                                       MagicSelectionFast.MagicSelectionTask task = new MagicSelectionFast.MagicSelectionTask(region, level, result.blockPos());
                                       task.fill(10000000);
                                       if (blockStates.size() == 1) {
                                          String countString = NumberFormat.getInstance().format((long)region.count());
                                          String blockName = AxiomI18n.get(blockStates.get(0).getCustomBlock().axiom$translationKey());
                                          String historyDescription = AxiomI18n.get("axiom.history_description.set_n_blocks_to", countString, blockName);
                                          RegionHelper.pushBooleanRegionChange(region, blockStates.get(0).getVanillaState(), historyDescription);
                                       } else {
                                          ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
                                          Random random = ThreadLocalRandom.current();
                                          region.forEach(
                                             (x, y, z) -> chunkedBlockRegion.addBlockWithoutDirty(
                                                x, y, z, blockStates.get(random.nextInt(blockStates.size())).getVanillaState()
                                             )
                                          );
                                          chunkedBlockRegion.dirtyAll();
                                          String countString = NumberFormat.getInstance().format((long)region.count());
                                          String historyDescription = AxiomI18n.get("axiom.history_description.painted", countString);
                                          RegionHelper.pushBlockRegionChange(chunkedBlockRegion, historyDescription);
                                       }

                                       region.close();
                                    }
                                 }
                              }
                           }
                        }

                        ImGui.endDragDropTarget();
                     }
                  }

                  if (ImGui.isWindowHovered() && imGuiIO.getMousePosY() > ImGui.getWindowPosY() + titleBarHeight) {
                     if (ImGui.isAnyMouseDown()) {
                        ImGui.setWindowFocus();
                     }

                     isFrameHovered = true;
                     if (Minecraft.getInstance().screen != null) {
                        ImGui.setNextFrameWantCaptureMouse(false);
                     } else {
                        boolean adjustSpeed = Keybinds.ADJUST_SPEED.isDownUsingGLFW();
                        if (adjustSpeed) {
                           int wheelY = (int)Math.signum(getIO().getMouseWheel());
                           if (wheelY != 0) {
                              Abilities abilities = Minecraft.getInstance().player.getAbilities();
                              ServerIntegration.changeFlySpeed(Mth.clamp(abilities.getFlyingSpeed() + wheelY * 0.01F, 0.05F, 0.5F));
                              ScreenRenderHook.setOverlayText(
                                 Component.literal(AxiomI18n.get("axiom.hardcoded.set_fly_speed"))
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(String.format("%.2fx", abilities.getFlyingSpeed() * 20.0F)).withStyle(ChatFormatting.GREEN))
                              );
                           }
                        }

                        if (getIO().getWantCaptureMouse() && !isMovingCamera()) {
                           if (!adjustSpeed) {
                              handleScroll();
                           }

                           fireCancelNavInput |= handleBasicInputs();
                        }
                     }
                  }
               } else {
                  ImGuiHelper.popStyleVar();
               }

               ImGui.end();
               if (adjustingTool) {
                  if (!imguiGlfw.isGrabbed()) {
                     adjustingTool = false;
                     adjustingToolOffsetX = 0.0F;
                     adjustingToolOffsetY = 0.0F;
                  } else if (!ToolManager.isToolActive()) {
                     imguiGlfw.ungrab();
                     adjustingTool = false;
                     adjustingToolOffsetX = 0.0F;
                     adjustingToolOffsetY = 0.0F;
                  } else {
                     sizeY = imGuiIO.getMousePosX();
                     posX = imGuiIO.getMousePosY();
                     adjustingToolOffsetX = (float)(adjustingToolOffsetX + grabbedMouseDeltaX);
                     adjustingToolOffsetY = (float)(adjustingToolOffsetY + grabbedMouseDeltaY);
                     Tool tool = ToolManager.getCurrentTool();
                     Vec2 offset = new Vec2(adjustingToolOffsetX, adjustingToolOffsetY);
                     Vec2 result = tool.renderAdjustment(sizeY, posX, offset);
                     if (result != offset) {
                        adjustingToolOffsetX = result.x;
                        adjustingToolOffsetY = result.y;
                     }
                  }
               }

               ToolsWindow.render(icons);
               ToolOptionsWindow.render();
               ClipboardInstallationWindow.render();
               TargetInfoWindow.render();
               WorldPropertiesWindow.render();
               HistoryWindow.render();
               ClipboardWindow.render();
               PlacementOptionsOverlay.render(frameX + frameWidth, frameY);
               PaletteWindow.render(activeBlockHistory, blockList);
               InventoryWindow.render(activeBlockHistory, blockList);
               CurrentBlockWindow.render(activeBlockHistory, blockList);
               TextAnnotationListWindow.render();
               BlueprintCreateWindow.render();
               BlueprintBrowserWindow.render();
               MaskSelectionWindow.render(blockList);
               ExpandSelectionWindow.render();
               ShrinkSelectionWindow.render();
               DistortSelectionWindow.render();
               SmoothSelectionWindow.render();
               FillBlocksWindow.render(blockList);
               ReplaceBlocksWindow.render(blockList);
               TypeReplaceBlocksWindow.render(blockList);
               SetBiomeWindow.render();
               AutoshadeWindow.render();
               QuickFillWindow.render(blockList);
               QuickReplaceWindow.render(blockList);
               AnalyzeBlocksWindow.render();
               AnimatedRebuildWindow.render();
               KeybindsWindow.render();
               ToolMaskWindow.render();
               OpenSourceLicensesWindow.render();
               StyleEditorWindow.render();
               RotatePlacementWindow.render();
               ExportSchematicWindow.render();
               ImageReferenceWindows.render();
               TickBlocksModal.render();
               focusViewNextFrame = -1;
               canProcessKeybinds = !ImGui.isPopupOpen("", 3072)
                  && !getIO().getWantTextInput()
                  && !ImGuiHelper.getWantsSpecialInput()
                  && !EditorWindowType.KEYBINDS.isOpen();
               if (canProcessKeybinds) {
                  IntSet usedKeys = new IntOpenHashSet();
                  if (Keybinds.COPY.isPressed(false) && UserAction.COPY.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.COPY.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.COPY_WITH_AIR.getKey()) && Keybinds.COPY_WITH_AIR.isPressed(false)) {
                     Selection.makeNextCopyIncludeAir = true;
                     if (UserAction.COPY.call(null) == UserAction.ActionResult.USED_STOP) {
                        usedKeys.add(Keybinds.COPY_WITH_AIR.getKey());
                     }

                     Selection.makeNextCopyIncludeAir = false;
                  }

                  if (!usedKeys.contains(Keybinds.PASTE.getKey())
                     && Keybinds.PASTE.isPressed(false)
                     && UserAction.PASTE.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.PASTE.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.CUT.getKey())
                     && Keybinds.CUT.isPressed(false)
                     && UserAction.CUT.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.CUT.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.DUPLICATE.getKey())
                     && Keybinds.DUPLICATE.isPressed(false)
                     && UserAction.DUPLICATE.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.DUPLICATE.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.UNDO.getKey())
                     && Keybinds.UNDO.isPressed(false)
                     && UserAction.UNDO.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.UNDO.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.REDO.getKey())
                     && Keybinds.REDO.isPressed(false)
                     && UserAction.REDO.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.REDO.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SAVE_BLUEPRINT.getKey())
                     && Keybinds.SAVE_BLUEPRINT.isPressed(false)
                     && UserAction.SAVE.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.SAVE_BLUEPRINT.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SWAP_TO_LAST_TOOL.getKey()) && Keybinds.SWAP_TO_LAST_TOOL.isPressed(false)) {
                     ToolManager.swapToLastTool();
                     usedKeys.add(Keybinds.SWAP_TO_LAST_TOOL.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_SELECTION.getKey()) && Keybinds.SHOW_SELECTION.isPressed(false)) {
                     Selection.setShouldRenderSelection(!Selection.shouldRenderSelection());
                     usedKeys.add(Keybinds.SHOW_SELECTION.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_BIOMES.getKey()) && Keybinds.SHOW_BIOMES.isPressed(false)) {
                     Axiom.configuration.visuals.showBiomes = !Axiom.configuration.visuals.showBiomes;
                     usedKeys.add(Keybinds.SHOW_BIOMES.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_ANNOTATIONS.getKey()) && Keybinds.SHOW_ANNOTATIONS.isPressed(false)) {
                     Axiom.configuration.visuals.showAnnotations = !Axiom.configuration.visuals.showAnnotations;
                     usedKeys.add(Keybinds.SHOW_ANNOTATIONS.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_DISPLAY_ENTITY_GIZMOS.getKey()) && Keybinds.SHOW_DISPLAY_ENTITY_GIZMOS.isPressed(false)) {
                     Axiom.configuration.entityManipulation.showDisplayEntities = !Axiom.configuration.entityManipulation.showDisplayEntities;
                     usedKeys.add(Keybinds.SHOW_DISPLAY_ENTITY_GIZMOS.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_MARKER_ENTITY_GIZMOS.getKey()) && Keybinds.SHOW_MARKER_ENTITY_GIZMOS.isPressed(false)) {
                     Axiom.configuration.entityManipulation.showMarkerEntities = !Axiom.configuration.entityManipulation.showMarkerEntities;
                     usedKeys.add(Keybinds.SHOW_MARKER_ENTITY_GIZMOS.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_COLLISION_MESH.getKey()) && Keybinds.SHOW_COLLISION_MESH.isPressed(false)) {
                     Axiom.configuration.blockAttributes.showCollisionMesh = !Axiom.configuration.blockAttributes.showCollisionMesh;
                     usedKeys.add(Keybinds.SHOW_COLLISION_MESH.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_LIGHT_BLOCKS.getKey()) && Keybinds.SHOW_LIGHT_BLOCKS.isPressed(false)) {
                     Axiom.configuration.blockAttributes.showLightBlocks = !Axiom.configuration.blockAttributes.showLightBlocks;
                     usedKeys.add(Keybinds.SHOW_LIGHT_BLOCKS.getKey());
                     Minecraft.getInstance().levelRenderer.allChanged();
                  }

                  if (!usedKeys.contains(Keybinds.SHOW_STRUCTURE_VOID_BLOCKS.getKey()) && Keybinds.SHOW_STRUCTURE_VOID_BLOCKS.isPressed(false)) {
                     Axiom.configuration.blockAttributes.showStructureVoidBlocks = !Axiom.configuration.blockAttributes.showStructureVoidBlocks;
                     usedKeys.add(Keybinds.SHOW_STRUCTURE_VOID_BLOCKS.getKey());
                     Minecraft.getInstance().levelRenderer.allChanged();
                  }

                  if (!usedKeys.contains(Keybinds.ROTATE_PLACEMENT.getKey())
                     && Keybinds.ROTATE_PLACEMENT.isPressed(false)
                     && UserAction.ROTATE_PLACEMENT.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.ROTATE_PLACEMENT.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.FLIP_PLACEMENT.getKey())
                     && Keybinds.FLIP_PLACEMENT.isPressed(false)
                     && UserAction.FLIP_PLACEMENT.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.FLIP_PLACEMENT.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.PASTE_AND_SELECT.getKey()) && Keybinds.PASTE_AND_SELECT.isPressed(false)) {
                     if (Placement.INSTANCE.isPlacing()) {
                        Placement.INSTANCE.pastePlacement(true);
                        usedKeys.add(Keybinds.PASTE_AND_SELECT.getKey());
                     } else if (ToolManager.isToolActive() && ToolManager.getCurrentTool() instanceof ShapeTool shapeTool) {
                        shapeTool.pasteShape(true);
                        shapeTool.reset();
                     }
                  }

                  if (!usedKeys.contains(Keybinds.CONFIRM.getKey())
                     && Keybinds.CONFIRM.isPressed(false)
                     && UserAction.ENTER.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.CONFIRM.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.EXTRUDE_POINT.getKey())
                     && Keybinds.EXTRUDE_POINT.isPressed(false)
                     && UserAction.EXTRUDE.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(Keybinds.EXTRUDE_POINT.getKey());
                  }

                  if (!usedKeys.contains(Keybinds.DELETE.getKey()) && Keybinds.DELETE.isPressed(false)) {
                     if (UserAction.DELETE.call(null) == UserAction.ActionResult.USED_STOP) {
                        usedKeys.add(Keybinds.DELETE.getKey());
                     }
                  } else if (Keybinds.DELETE.getKey() == 261
                     && !usedKeys.contains(259)
                     && ImGui.isKeyPressed(523, false)
                     && UserAction.DELETE.call(null) == UserAction.ActionResult.USED_STOP) {
                     usedKeys.add(259);
                  }

                  for (java.util.Map.Entry<Tool, Keybind> entry : ToolManager.keybindMap.entrySet()) {
                     if (!usedKeys.contains(entry.getValue().getKey()) && entry.getValue().isPressed(false)) {
                        if (ToolManager.isToolActive() && ToolManager.getCurrentTool().getClass() == entry.getKey().getClass()) {
                           ToolManager.setToolSelected(false);
                        } else {
                           ToolManager.setTool((Class<? extends Tool>)entry.getKey().getClass());
                           ToolManager.setToolSelected(true);
                        }

                        usedKeys.add(entry.getValue().getKey());
                     }
                  }

                  List<View> viewsx = ViewManager.getViews();

                  for (int i = 0; i < 9 && i < viewsx.size(); i++) {
                     Keybind keybind = Keybinds.SELECT_VIEW_KEYBINDS.get(i);
                     if (keybind.isPressed(false) && usedKeys.add(keybind.getKey())) {
                        focusViewNextFrame = i;
                        break;
                     }
                  }
               }

               ImGui.render();
               ImGuiHelper.endFrame();
               if (fireCancelNavInput) {
                  getIO().addKeyEvent(526, true);
                  getIO().addKeyEvent(526, false);
               }

               long ctx = GLFW.glfwGetCurrentContext();
               ImGui.updatePlatformWindows();
               ImGui.renderPlatformWindowsDefault();
               GLFW.glfwMakeContextCurrent(ctx);
               ImDrawData drawData = ImGui.getDrawData();
               if (drawData != null) {
                  imguiGl3.renderDrawData(drawData);
               }

               transitionActiveState(true);
            }
         }
      }
   }

   private static void updateWindowSize() {
      Window window = Minecraft.getInstance().getWindow();
      int[] width = new int[1];
      int[] height = new int[1];
      GLFW.glfwGetFramebufferSize(window.getWindow(), width, height);
      if (width[0] > 0 && height[0] > 0) {
         ((WindowExt)(Object)window).axiom$resizeFramebuffer(window.getWindow(), width[0], height[0]);
      }

      GLFW.glfwGetWindowSize(window.getWindow(), width, height);
      if (width[0] > 0 && height[0] > 0) {
         ((WindowExt)(Object)window).axiom$resize(window.getWindow(), width[0], height[0]);
      }

      Minecraft.getInstance().resizeDisplay();
   }

   public static boolean isMainFrameHovered() {
      return isFrameHovered;
   }

   private static void handleScroll() {
      int wheelY = (int)Math.signum(getIO().getMouseWheel());
      boolean sprintMod = isMoveQuickDown();
      if (!sprintMod) {
         int wheelX = (int)Math.signum(getIO().getMouseWheelH());
         if (wheelX == 0 && wheelY == 0) {
            return;
         }

         UserAction.ScrollAmount scrollAmount = new UserAction.ScrollAmount(wheelX, wheelY);
         if (UserAction.SCROLL.call(scrollAmount) == UserAction.ActionResult.USED_STOP) {
            return;
         }
      }

      if (wheelY >= 1) {
         pendingDepthActions.add(PendingDepthAction.SCROLL_POSITIVE);
      } else if (wheelY <= -1) {
         pendingDepthActions.add(PendingDepthAction.SCROLL_NEGATIVE);
      }
   }

   private static String getPrioritizedKeybind(Set<Keybind> keybinds, boolean ctrlMod) {
      if (keybinds == null) {
         return AxiomI18n.get("axiom.editorui.no_keybind");
      } else {
         Set<Keybind> applicable = new HashSet<>(keybinds);
         applicable.removeIf(keybind -> keybind.isCtrlMod() != ctrlMod);
         if (applicable.isEmpty()) {
            return AxiomI18n.get("axiom.editorui.no_keybind");
         } else if (applicable.contains(Keybinds.ROTATE_CAMERA)) {
            return Keybinds.ROTATE_CAMERA.getDescription();
         } else if (applicable.contains(Keybinds.PICK_BLOCK)) {
            return Keybinds.PICK_BLOCK.getDescription();
         } else if (applicable.contains(Keybinds.USE_TOOL) && ToolManager.isToolActive()) {
            return Keybinds.USE_TOOL.getDescription();
         } else if (applicable.contains(Keybinds.CROSSHAIR_CAMERA)) {
            return Keybinds.CROSSHAIR_CAMERA.getDescription();
         } else if (applicable.contains(Keybinds.ARCBALL_CAMERA)) {
            return Keybinds.ARCBALL_CAMERA.getDescription();
         } else if (applicable.contains(Keybinds.PAN_CAMERA)) {
            return Keybinds.PAN_CAMERA.getDescription();
         } else {
            return applicable.size() == 1 ? applicable.iterator().next().getDescription() : AxiomI18n.get("axiom.editorui.multiple_keybinds");
         }
      }
   }

   private static boolean handleBasicInputs() {
      if (ImGui.isMouseClicked(0) && UserAction.LEFT_MOUSE.call(null) == UserAction.ActionResult.USED_STOP) {
         return true;
      } else {
         if (Keybinds.ADJUST_RADIUS.isPressed(false)) {
            if (ToolManager.isToolActive() && ToolManager.getCurrentTool().initiateAdjustment()) {
               int key = Keybinds.ADJUST_RADIUS.getKey();
               if (key != 0) {
                  imguiGlfw.setGrabbed(false, key, -1.0, -1.0);
               }

               adjustingTool = true;
               adjustingToolOffsetX = 0.0F;
               adjustingToolOffsetY = 0.0F;
               return true;
            }
         } else {
            if (Keybinds.ROTATE_CAMERA.isPressed(false)) {
               movementControls = EditorMovementControls.rotate();
               return true;
            }

            if (Keybinds.PICK_BLOCK.isPressed(false)) {
               RayCaster.RaycastResult result = Tool.raycastBlock();
               if (result != null) {
                  BlockState blockState = Minecraft.getInstance().level.getBlockState(result.blockPos());
                  CustomBlockState customBlockState = ServerCustomBlocks.getCustomStateFor(blockState);
                  activeBlockHistory.setActive(Objects.requireNonNullElse(customBlockState, (CustomBlockState)blockState));
                  return true;
               }
            } else if (Keybinds.USE_TOOL.isPressed(false)) {
               if (UserAction.RIGHT_MOUSE.call(null) != UserAction.ActionResult.NOT_HANDLED || ToolManager.isToolActive()) {
                  return true;
               }
            } else if (Keybinds.ARCBALL_CAMERA.isPressed(false)) {
               Tool currentTool = ToolManager.isToolActive() ? ToolManager.getCurrentTool() : null;
               if (!(currentTool instanceof RulerTool) && !(currentTool instanceof ModifyTool) && !Axiom.configuration.internal.useCenterOfScreenForArcball) {
                  pendingDepthActions.add(PendingDepthAction.ARCBALL);
                  return true;
               }

               movementControls = EditorMovementControls.arcballFromRaycast();
               if (movementControls != EditorMovementControls.none()) {
                  int key = Keybinds.ARCBALL_CAMERA.getKey();
                  if (key != 0) {
                     imguiGlfw.setGrabbed(false, key, frameX + frameWidth / 2.0F, frameY + frameHeight / 2.0F);
                  }

                  return true;
               }
            } else if (Keybinds.PAN_CAMERA.isPressed(false)) {
               pendingDepthActions.add(PendingDepthAction.PAN);
               return true;
            }
         }

         if (Keybinds.CROSSHAIR_CAMERA.isPressed(false)) {
            int key = Keybinds.CROSSHAIR_CAMERA.getKey();
            if (key != 0) {
               imguiGlfw.setGrabbed(true, key, frameX + frameWidth / 2.0F, frameY + frameHeight / 2.0F);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public static BlockList getBlockList() {
      return blockList;
   }

   public static boolean consumeNavClose() {
      boolean navClose = EditorUI.navClose;
      EditorUI.navClose = false;
      return navClose;
   }

   public static boolean isMoveQuickDown() {
      if (Keybinds.useVanillaMovement) {
         return isActive()
            ? ImGuiHelper.isGlfwBindingDown(Minecraft.getInstance().options.keySprint.key.getValue())
            : Minecraft.getInstance().options.keySprint.isDown();
      } else {
         return Keybinds.MOVE_QUICK.isDownIgnoreMods();
      }
   }

   public static boolean isCtrlOrCmdDown() {
      return getIO().getKeyCtrl();
   }

   public static String getClipboard() {
      if (!cachedClipboardRenderedFrame) {
         return cachedClipboard;
      } else {
         long millis = System.currentTimeMillis();
         if (Math.abs(millis - cachedClipboardLastMillis) > 250L) {
            cachedClipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            cachedClipboardRenderedFrame = false;
            cachedClipboardLastMillis = millis;
         }

         return cachedClipboard;
      }
   }
}
