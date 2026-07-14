package com.moulberry.axiom.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybind;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.palette.EditorPalette;
import com.moulberry.axiom.editor.styles.StyleHelper;
import com.moulberry.axiom.editor.styles.StyleManager;
import com.moulberry.lattice.WidgetFunction;
import com.moulberry.lattice.annotation.LatticeCategory;
import com.moulberry.lattice.annotation.LatticeFormatValues;
import com.moulberry.lattice.annotation.LatticeOption;
import com.moulberry.lattice.annotation.constraint.LatticeFloatRange;
import com.moulberry.lattice.annotation.constraint.LatticeIntRange;
import com.moulberry.lattice.annotation.widget.LatticeWidgetButton;
import com.moulberry.lattice.annotation.widget.LatticeWidgetCustom;
import com.moulberry.lattice.annotation.widget.LatticeWidgetKeybind;
import com.moulberry.lattice.annotation.widget.LatticeWidgetSlider;
import com.moulberry.lattice.annotation.widget.LatticeWidgetTextArea;
import com.moulberry.lattice.annotation.widget.LatticeWidgetTextField;
import com.moulberry.lattice.element.LatticeElements;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AxiomConfig {
   private static final Gson GSON = new GsonBuilder().registerTypeAdapter(EditorPalette.class, new EditorPalette.TypeAdapter()).setPrettyPrinting().create();
   @LatticeCategory(
      name = "axiom.editorui.window.keybinds"
   )
   public AxiomConfig.SubcategoryKeybinds keybinds = new AxiomConfig.SubcategoryKeybinds();
   @LatticeCategory(
      name = "axiom.config.visuals"
   )
   public AxiomConfig.SubcategoryVisuals visuals = new AxiomConfig.SubcategoryVisuals();
   @LatticeCategory(
      name = "axiom.config.context_menu"
   )
   public AxiomConfig.SubcategoryContextMenu contextMenu = new AxiomConfig.SubcategoryContextMenu();
   @LatticeCategory(
      name = "axiom.config.capabilities"
   )
   public AxiomConfig.SubcategoryCapabilities capabilities = new AxiomConfig.SubcategoryCapabilities();
   @LatticeCategory(
      name = "axiom.config.block_attributes"
   )
   public AxiomConfig.SubcategoryBlockAttributes blockAttributes = new AxiomConfig.SubcategoryBlockAttributes();
   @LatticeCategory(
      name = "axiom.config.movement"
   )
   public AxiomConfig.SubcategoryMovement movement = new AxiomConfig.SubcategoryMovement();
   @LatticeCategory(
      name = "axiom.config.builder_tools"
   )
   public AxiomConfig.SubcategoryBuilderTools builderTools = new AxiomConfig.SubcategoryBuilderTools();
   @LatticeCategory(
      name = "axiom.config.creative_menu"
   )
   public AxiomConfig.SubcategoryCreativeMenu creativeMenu = new AxiomConfig.SubcategoryCreativeMenu();
   @LatticeCategory(
      name = "axiom.config.entity_manipulation"
   )
   public AxiomConfig.SubcategoryEntityManipulation entityManipulation = new AxiomConfig.SubcategoryEntityManipulation();
   @LatticeCategory(
      name = "axiom.config.editor"
   )
   public AxiomConfig.SubcategoryEditor editor = new AxiomConfig.SubcategoryEditor();
   @LatticeCategory(
      name = "axiom.config.blueprint"
   )
   public AxiomConfig.SubcategoryBlueprint blueprint = new AxiomConfig.SubcategoryBlueprint();
   public AxiomConfig.SubcategoryInternal internal = new AxiomConfig.SubcategoryInternal();
   public static final String DEFAULT_LAYOUT = "[Window][###Tools]\nPos=0,0\nSize=300,250\nCollapsed=0\nDockId=0x00000003,0\n\n[Window][###Tool Options]\nPos=0,250\nSize=300,750\nCollapsed=0\nDockId=0x00000004,0\n\n[Window][###Clipboard]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,0\n\n[Window][###TargetInfo]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,1\n\n[Window][###Palette]\nPos=1700,200\nSize=300,200\nCollapsed=0\nDockId=0x0000000E,0\n\n[Window][###ActiveBlock]\nPos=1700,400\nSize=300,100\nCollapsed=0\nDockId=0x0000000C,0\n\n[Window][###History]\nPos=1700,500\nSize=300,300\nCollapsed=0\nDockId=0x0000000A,0\n\n[Window][###WorldProperties]\nPos=1700,800\nSize=300,200\nCollapsed=0\nDockId=0x00000008,0\n\n[Docking][Data]\nDockSpace           ID=0x8B93E3BD Window=0xA787BDB4 Pos=0,0 Size=2000,1000 Split=X\nDockNode          ID=0x00000005 Parent=0x8B93E3BD SizeRef=1700,1000 Split=X\n DockNode        ID=0x00000001 Parent=0x00000005 SizeRef=300,1000 Split=Y\n   DockNode      ID=0x00000003 Parent=0x00000001 SizeRef=300,250 Selected=0x80AFE82B\n   DockNode      ID=0x00000004 Parent=0x00000001 SizeRef=300,750 Selected=0xECA27DCB\n DockNode        ID=0x00000002 Parent=0x00000005 SizeRef=1400,1000 CentralNode=1 Selected=0x1F1A625A\nDockNode          ID=0x00000006 Parent=0x8B93E3BD SizeRef=300,1000 Split=Y Selected=0x34064FA7\n DockNode        ID=0x00000007 Parent=0x00000006 SizeRef=300,800 Split=Y Selected=0x34064FA7\n   DockNode      ID=0x00000009 Parent=0x00000007 SizeRef=300,500 Split=Y Selected=0x34064FA7\n     DockNode    ID=0x0000000B Parent=0x00000009 SizeRef=300,400 Split=Y Selected=0x34064FA7\n       DockNode  ID=0x0000000D Parent=0x0000000B SizeRef=300,200 Selected=0x34064FA7\n       DockNode  ID=0x0000000E Parent=0x0000000B SizeRef=300,200 Selected=0x1E514AEA\n     DockNode    ID=0x0000000C Parent=0x00000009 SizeRef=300,100 Selected=0x1D216E21\n   DockNode      ID=0x0000000A Parent=0x00000007 SizeRef=300,300 Selected=0xFE0E9DDF\n DockNode        ID=0x00000008 Parent=0x00000006 SizeRef=300,200 Selected=0x602D8B84";

   public String serialize() {
      return GSON.toJson(this);
   }

   public static AxiomConfig tryLoadFromFolder(Path configFolder) {
      Path primary = configFolder.resolve("axiom.json");
      Path backup = configFolder.resolve(".axiom.json.backup");
      if (Files.exists(primary)) {
         try {
            return load(primary);
         } catch (Exception var5) {
            Axiom.LOGGER.error("Failed to load config from {}", primary, var5);
         }
      }

      if (Files.exists(backup)) {
         try {
            return load(backup);
         } catch (Exception var4) {
            Axiom.LOGGER.error("Failed to load config from {}", backup, var4);
         }
      }

      return new AxiomConfig();
   }

   public void saveToDefaultFolder() {
      this.saveToFolder(Axiom.getInstance().getConfigDirectory());
   }

   public synchronized void saveToFolder(Path configFolder) {
      this.updateInternalValues();
      Path primary = configFolder.resolve("axiom.json");
      Path backup = configFolder.resolve(".axiom.json.backup");
      if (Files.exists(primary)) {
         try {
            load(primary);

            try {
               Files.move(primary, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException var5) {
               Axiom.LOGGER.error("Failed to backup config", var5);
            }
         } catch (Exception var6) {
         }
      }

      this.save(primary);
   }

   private static AxiomConfig load(Path path) throws IOException {
      String serialized = Files.readString(path);
      return (AxiomConfig)GSON.fromJson(serialized, AxiomConfig.class);
   }

   private void save(Path path) {
      String serialized = GSON.toJson(this, AxiomConfig.class);

      try {
         Files.writeString(
            path, serialized, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.DSYNC
         );
      } catch (IOException var4) {
         Axiom.LOGGER.error("Failed to save config", var4);
      }
   }

   private void updateInternalValues() {
      this.internal.openEditorWindowTypes = EditorWindowType.getOpenByName();
      this.updateTheme();
      Keybinds.save(this);
   }

   private void updateTheme() {
      if (EditorUI.hasImGuiContext()) {
         long oldContext = EditorUI.pushImGuiContext();
         ImGuiHelper.popAllStyleColors();
         ImGuiHelper.popAllStyleVars();
         StyleHelper.Theme theme = StyleManager.createTheme();
         if (theme != null) {
            this.internal.savedCustomTheme = theme.convertToBase64();
         }

         EditorUI.popImGuiContext(oldContext);
      }
   }

   public LatticeElements createElements() {
      return LatticeElements.fromAnnotations(Component.translatable("axiom.config"), this);
   }

   public static class SubcategoryBlockAttributes {
      @LatticeOption(
         title = "axiom.block_attributes.show_collision_mesh",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showCollisionMesh = false;
      @LatticeOption(
         title = "axiom.block_attributes.show_light_blocks",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showLightBlocks = false;
      @LatticeOption(
         title = "axiom.block_attributes.show_structure_void_blocks",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showStructureVoidBlocks = false;
      @LatticeOption(
         title = "axiom.block_attributes.show_moving_piston_blocks",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showMovingPistonBlocks = false;
      @LatticeOption(
         title = "axiom.block_attributes.expand_hitboxes_to_full_cube",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean expandHitboxesToFullCube = false;
      @LatticeOption(
         title = "axiom.block_attributes.make_fluid_hitboxes_solid",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean makeFluidHitboxesSolid = false;
      @LatticeOption(
         title = "axiom.block_attributes.prevent_interactions",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean preventInteractions = false;
   }

   public static class SubcategoryBlueprint {
      @LatticeOption(
         title = "axiom.editorui.window.blueprint_browser.automatically_refresh",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean automaticRefreshing = true;
      @LatticeOption(
         title = "axiom.editorui.window.blueprint_browser.search_inside_folders",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean recursiveSearch = false;
      @LatticeOption(
         title = "axiom.config.blueprint.custom_path",
         description = "!!.description"
      )
      @LatticeWidgetTextField
      public String customPath = "";
      @LatticeOption(
         title = "axiom.config.blueprint.default_tags",
         description = "!!.description"
      )
      @LatticeWidgetTextArea
      public String defaultTags = "small,medium,large,massive,organic,structure,terrain,interior,house,tower,bridge,castle,statue,temple,monument,barn,stable,windmill,store,watermill,ship,airship,balloon,palace,watchtower,mansion,grave,marketplace,mine,obelisk,warehouse,silo,shipwreck,mausoleum,cemetery,bunker,airplane,helicopter,car,truck,vehicle,blacksmith,crypt,factory,mountain,cliff,rock,iceberg,spike,stone,wood,brick,natural,sand,metal,winter,spring,summer,autumn,tree,bush,mushroom,spruce,oak,birch,coniferous,deciduous,acacia,mangrove,cherryblossom,darkoak,jungle,baobab,azalea,cypress,coral,sapling,grass,seagrass,bamboo,flowers,animal,creature,dead,lamp,streetlight,brazier,bed,bookshelf,closet,table,chair,fireplace,carpet,fountain,clock,banner,flag,bell,modern,medieval,steampunk,gothic,oriental,victorian,fantasy,sci-fi,elven,dwarven,futuristic,retro,classic,rustic,baroque,rococo,industrial,artnouveau,artdeco,cyberpunk,space,arabic,indian,egyptian,greek,roman,norse,mesoamerican,japanese,western,spanish,tudor,spooky,pirate,dungeons,rubble,crates,redstone,wall,window,roof,stairs,pillar,arch,stairs,chimney,well";
   }

   public static class SubcategoryBuilderTools {
      @LatticeOption(
         title = "axiom.config.builder_tools.show_builder_tool_slot",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showBuilderToolSlot = true;
      @LatticeOption(
         title = "axiom.config.builder_tools.direction_lock",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean directionLock = true;
      @LatticeOption(
         title = "axiom.config.builder_tools.symmetry_range",
         description = "!!.description"
      )
      @LatticeIntRange(
         min = 64,
         max = 1024,
         clampMin = 0
      )
      @LatticeWidgetSlider
      public int symmetryRange = 128;
      @LatticeOption(
         title = "axiom.config.builder_tools.show_symmetry_enabled_icon",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showSymmetryEnabledIcon = true;
      @LatticeOption(
         title = "axiom.config.builder_tools.magic_select_count",
         description = "!!.description"
      )
      @LatticeIntRange(
         min = 16,
         max = 1024,
         clampMin = 1
      )
      @LatticeWidgetSlider
      public int magicSelectCount = 128;
      @LatticeOption(
         title = "axiom.config.builder_tools.middle_click",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public BuilderToolMiddleClick middleClick = BuilderToolMiddleClick.EXTEND_SELECT;
   }

   public static class SubcategoryCapabilities {
      @LatticeOption(
         title = "axiom.capability.bulldozer.title",
         description = "axiom.capability.bulldozer.description_alt"
      )
      @LatticeWidgetButton
      public boolean bulldozer = true;
      @LatticeOption(
         title = "axiom.capability.replace_mode.title",
         description = "axiom.capability.replace_mode.description"
      )
      @LatticeWidgetButton
      public boolean replaceMode = false;
      @LatticeOption(
         title = "axiom.config.capabilities.type_replace",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean typeReplace = false;
      @LatticeOption(
         title = "axiom.capability.force_place.title",
         description = "axiom.capability.force_place.description"
      )
      @LatticeWidgetButton
      public boolean forcePlace = false;
      @LatticeOption(
         title = "axiom.capability.no_updates.title",
         description = "axiom.capability.no_updates.description"
      )
      @LatticeWidgetButton
      public boolean noUpdates = false;
      @LatticeOption(
         title = "axiom.capability.tinker.title",
         description = "axiom.capability.tinker.description"
      )
      @LatticeWidgetButton
      public boolean tinker = true;
      @LatticeOption(
         title = "axiom.capability.infinite_reach.title",
         description = "axiom.capability.infinite_reach.description"
      )
      @LatticeWidgetButton
      public boolean infiniteReach = false;
      @LatticeOption(
         title = "axiom.contextmenu.infinite_reach_limit_none",
         description = "axiom.config.capabilities.infinite_reach_limit.description"
      )
      @LatticeWidgetCustom(
         function = "createInfiniteReachLimitSlider"
      )
      public int infiniteReachLimit = -1;
      @LatticeOption(
         title = "axiom.capability.fast_place.title",
         description = "axiom.capability.fast_place.description"
      )
      @LatticeWidgetButton
      public boolean fastPlace = false;
      @LatticeOption(
         title = "axiom.capability.angel_placement.title",
         description = "axiom.capability.angel_placement.description"
      )
      @LatticeWidgetButton
      public boolean angelPlacement = false;
      @LatticeOption(
         title = "axiom.capability.no_clip.title",
         description = "axiom.capability.no_clip.description"
      )
      @LatticeWidgetButton
      public boolean noClip = false;
      @LatticeOption(
         title = "axiom.capability.phantom.title",
         description = "axiom.capability.phantom.description"
      )
      @LatticeWidgetButton
      public boolean phantom = false;

      private WidgetFunction createInfiniteReachLimitSlider(IntSupplier supplier, IntConsumer consumer) {
         return new WidgetFunction() {
            public AbstractWidget createWidget(Font arg, @NotNull Component title, @Nullable Component description, int width) {
               float infiniteReachLimit = supplier.getAsInt();
               if (infiniteReachLimit < 5.0F) {
                  infiniteReachLimit = 1.0F;
               } else {
                  infiniteReachLimit = (infiniteReachLimit - 5.0F) / 96.0F;
               }

               return new AbstractSliderButton(0, 0, width, 20, CommonComponents.EMPTY, infiniteReachLimit) {
                  {
                     this.updateMessage();
                  }

                  protected void updateMessage() {
                     int infiniteReachLimitx = supplier.getAsInt();
                     if (infiniteReachLimitx < 0) {
                        this.setMessage(Component.translatable("axiom.contextmenu.infinite_reach_limit_none"));
                     } else {
                        this.setMessage(Component.translatable("axiom.contextmenu.infinite_reach_limit", new Object[]{infiniteReachLimitx}));
                     }
                  }

                  protected void applyValue() {
                     int value = (int)(this.value * 96.0 + 5.0);
                     if (value < 5) {
                        value = 5;
                     }

                     if (value > 100) {
                        consumer.accept(-1);
                     } else {
                        consumer.accept(value);
                     }
                  }
               };
            }
         };
      }
   }

   public static class SubcategoryContextMenu {
      @LatticeOption(
         title = "axiom.config.context_menu.keybind"
      )
      @LatticeWidgetKeybind
      private transient KeyMapping contextMenuKeybind = ClientEvents.contextMenuKeyBind;
      @LatticeOption(
         title = "axiom.config.context_menu.keybind_mode"
      )
      @LatticeWidgetButton
      public AltMenuKeybindMode keybindMode = AltMenuKeybindMode.HOLD;
      @LatticeOption(
         title = "axiom.config.context_menu.auto_swap_to_creative",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean autoSwapToCreative = true;
      @LatticeOption(
         title = "axiom.config.context_menu.auto_swap_to_hotbar_with_item",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean autoSwapToOtherHotbarWithItem = false;
      @LatticeOption(
         title = "axiom.config.context_menu.global_hotbars",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean globalHotbars = false;
   }

   public static class SubcategoryCreativeMenu {
      @LatticeOption(
         title = "axiom.config.creative_menu.show_color_picker",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showColorPicker = true;
      @LatticeOption(
         title = "axiom.config.creative_menu.show_gradient_helper",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showGradientHelper = true;
      @LatticeOption(
         title = "axiom.config.creative_menu.search_quick_add",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean searchQuickAdd = true;
   }

   public static class SubcategoryEditor {
      @LatticeOption(
         title = "axiom.keybind.toggle_editor_ui"
      )
      @LatticeWidgetKeybind
      private transient KeyMapping toggleEditorUiKeyBind;
      @LatticeOption(
         title = "axiom.editorui.tool_stabilization"
      )
      @LatticeFloatRange(
         min = 0.0F,
         max = 16.0F,
         step = "0.1",
         clampMin = 0.0F,
         clampMax = 64.0F
      )
      @LatticeWidgetSlider
      public float toolStabilization;
      @LatticeOption(
         title = "axiom.config.editor.clear_tool_when_swapping",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean clearToolWhenSwapping;

      public SubcategoryEditor() {
         this.toggleEditorUiKeyBind = ClientEvents.toggleEditorUiKeyBind;
         this.toolStabilization = 0.0F;
         this.clearToolWhenSwapping = true;
      }
   }

   public static class SubcategoryEntityManipulation {
      @LatticeOption(
         title = "axiom.contextmenu.show_display_entity_gizmos",
         description = "axiom.config.entity_manipulation.show_display_entity_gizmos.description"
      )
      @LatticeWidgetButton
      public boolean showDisplayEntities = true;
      @LatticeOption(
         title = "axiom.config.entity_manipulation.display_entity_range",
         description = "!!.description"
      )
      @LatticeIntRange(
         min = 1,
         max = 128,
         clampMin = 1
      )
      @LatticeWidgetSlider
      public int displayEntityRange = 8;
      @LatticeOption(
         title = "axiom.contextmenu.show_marker_entity_gizmos",
         description = "axiom.config.entity_manipulation.show_marker_entity_gizmos.description"
      )
      @LatticeWidgetButton
      public boolean showMarkerEntities = true;
      @LatticeOption(
         title = "axiom.config.entity_manipulation.marker_entity_range",
         description = "!!.description"
      )
      @LatticeIntRange(
         min = 1,
         max = 128,
         clampMin = 1
      )
      @LatticeWidgetSlider
      public int markerEntityRange = 24;
      @LatticeOption(
         title = "axiom.config.entity_manipulation.swap_left_right_click",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean swapLeftRightClick = false;
   }

   public static class SubcategoryInternal {
      public long nextUpdateNag = 0L;
      public boolean shownIntroduction = false;
      public boolean showOpenConfigTip = true;
      public boolean showOpenContextMenuTip = true;
      public boolean showOpenEditorTip = true;
      public Set<String> completedTutorials = new HashSet<>();
      public boolean askedTutorialPreference = false;
      public List<String> openEditorWindowTypes = EditorWindowType.getOpenByName();
      public String defaultLayout = "[Window][###Tools]\nPos=0,0\nSize=300,250\nCollapsed=0\nDockId=0x00000003,0\n\n[Window][###Tool Options]\nPos=0,250\nSize=300,750\nCollapsed=0\nDockId=0x00000004,0\n\n[Window][###Clipboard]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,0\n\n[Window][###TargetInfo]\nPos=1700,0\nSize=300,200\nCollapsed=0\nDockId=0x0000000D,1\n\n[Window][###Palette]\nPos=1700,200\nSize=300,200\nCollapsed=0\nDockId=0x0000000E,0\n\n[Window][###ActiveBlock]\nPos=1700,400\nSize=300,100\nCollapsed=0\nDockId=0x0000000C,0\n\n[Window][###History]\nPos=1700,500\nSize=300,300\nCollapsed=0\nDockId=0x0000000A,0\n\n[Window][###WorldProperties]\nPos=1700,800\nSize=300,200\nCollapsed=0\nDockId=0x00000008,0\n\n[Docking][Data]\nDockSpace           ID=0x8B93E3BD Window=0xA787BDB4 Pos=0,0 Size=2000,1000 Split=X\nDockNode          ID=0x00000005 Parent=0x8B93E3BD SizeRef=1700,1000 Split=X\n DockNode        ID=0x00000001 Parent=0x00000005 SizeRef=300,1000 Split=Y\n   DockNode      ID=0x00000003 Parent=0x00000001 SizeRef=300,250 Selected=0x80AFE82B\n   DockNode      ID=0x00000004 Parent=0x00000001 SizeRef=300,750 Selected=0xECA27DCB\n DockNode        ID=0x00000002 Parent=0x00000005 SizeRef=1400,1000 CentralNode=1 Selected=0x1F1A625A\nDockNode          ID=0x00000006 Parent=0x8B93E3BD SizeRef=300,1000 Split=Y Selected=0x34064FA7\n DockNode        ID=0x00000007 Parent=0x00000006 SizeRef=300,800 Split=Y Selected=0x34064FA7\n   DockNode      ID=0x00000009 Parent=0x00000007 SizeRef=300,500 Split=Y Selected=0x34064FA7\n     DockNode    ID=0x0000000B Parent=0x00000009 SizeRef=300,400 Split=Y Selected=0x34064FA7\n       DockNode  ID=0x0000000D Parent=0x0000000B SizeRef=300,200 Selected=0x34064FA7\n       DockNode  ID=0x0000000E Parent=0x0000000B SizeRef=300,200 Selected=0x1E514AEA\n     DockNode    ID=0x0000000C Parent=0x00000009 SizeRef=300,100 Selected=0x1D216E21\n   DockNode      ID=0x0000000A Parent=0x00000007 SizeRef=300,300 Selected=0xFE0E9DDF\n DockNode        ID=0x00000008 Parent=0x00000006 SizeRef=300,200 Selected=0x602D8B84";
      public int lastTranslationCount = 0;
      public float globalScale = 1.0F;
      public String savedCustomTheme = "";
      public boolean showCloseWindowButton = false;
      public boolean showToolMaskOpenWarning = true;
      public boolean showNon90DegreeRotationWarning = true;
      public boolean showQuickReplaceCtrlClickTip = true;
      public boolean dockedInventoryWithPalette = false;
      public EditorPalette rootEditorPalette = new EditorPalette("");
      public Map<String, String> customDowngradeSuggestions = new LinkedHashMap<>();
      public boolean invertCameraRotate = false;
      public boolean useCenterOfScreenForArcball = false;
      public boolean pickBlockDrag = true;
      public boolean cutAlsoCopiesToClipboard = false;
      public boolean useVanillaMovementForEditor = true;
      public boolean tallGrassIsActuallyNotTall = false;
      public boolean disableChunkRenderOverrider = false;
      public boolean hadEditorUIOpen = false;
      public int blueprintBrowserDirSize = 175;
   }

   public static class SubcategoryKeybinds {
      @LatticeOption(
         title = "axiom.config.keybinds.view_all_ingame_keybinds"
      )
      @LatticeWidgetButton
      private transient Runnable viewAllIngameKeybinds = () -> Minecraft.getInstance()
         .setScreen(new KeyBindsScreen(Minecraft.getInstance().screen, Minecraft.getInstance().options));
      @LatticeOption(
         title = "axiom.config.keybinds.view_editor_keybinds"
      )
      @LatticeWidgetButton
      private transient Runnable viewEditorKeybinds = () -> {
         if (!EditorUI.isActive()) {
            Minecraft.getInstance().setScreen(null);
            EditorUI.enable();
         }

         EditorWindowType.KEYBINDS.setOpen(true);
      };
      @LatticeOption(
         title = "axiom.keybinds.copy_ingame",
         description = "axiom.config.keybinds.copy_ingame.description"
      )
      @LatticeWidgetKeybind(
         allowModifiers = true
      )
      private transient Keybind copyIngameKeybind = Keybinds.COPY_INGAME;
      @LatticeOption(
         title = "axiom.keybinds.rotate_placement",
         description = "axiom.config.keybinds.rotate_placement.description"
      )
      @LatticeWidgetKeybind(
         allowModifiers = true
      )
      private transient Keybind rotatePlacementKeybind = Keybinds.ROTATE_PLACEMENT;
      @LatticeOption(
         title = "axiom.keybinds.flip_placement",
         description = "axiom.config.keybinds.flip_placement.description"
      )
      @LatticeWidgetKeybind(
         allowModifiers = true
      )
      private transient Keybind flipPlacementKeybind = Keybinds.FLIP_PLACEMENT;
      public Map<String, String> regularKeybinds = new LinkedHashMap<>();
      public Map<String, String> toolKeybinds = new LinkedHashMap<>();
   }

   public static class SubcategoryMovement {
      @LatticeOption(
         title = "axiom.config.movement.flight_momentum",
         description = "!!.description"
      )
      @LatticeFormatValues(
         formattingString = "%d%%"
      )
      @LatticeIntRange(
         min = 0,
         max = 100,
         clampMin = 0,
         clampMax = 100
      )
      @LatticeWidgetSlider
      public int flightMomentum = 100;
      @LatticeOption(
         title = "axiom.contextmenu.flight_direction",
         description = "axiom.config.movement.flight_direction.description"
      )
      @LatticeWidgetButton
      public FlightDirection flightDirection = FlightDirection.HORIZONTAL;
      @LatticeOption(
         title = "axiom.config.movement.sync_with_editor_ui",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean syncIngameMovementWithEditorUI = false;
      @LatticeOption(
         title = "axiom.config.movement.separate_flight_speed",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean separateFlightSpeeds = false;
   }

   public static class SubcategoryVisuals {
      @LatticeOption(
         title = "axiom.editorui.mainmenu.view.min_brightness",
         description = "axiom.config.visuals.min_brightness.description"
      )
      @LatticeFormatValues(
         formattingString = "%d%%"
      )
      @LatticeIntRange(
         min = 0,
         max = 100,
         clampMin = 0,
         clampMax = 100
      )
      @LatticeWidgetSlider
      public int minBrightness = 0;
      @LatticeOption(
         title = "axiom.editorui.mainmenu.view.liquid_opacity",
         description = "axiom.config.visuals.liquid_opacity.description"
      )
      @LatticeFormatValues(
         formattingString = "%d%%"
      )
      @LatticeIntRange(
         min = 0,
         max = 100,
         clampMin = 0,
         clampMax = 100
      )
      @LatticeWidgetSlider
      public int liquidOpacity = 100;
      @LatticeOption(
         title = "axiom.editorui.mainmenu.view.show_biomes",
         description = "axiom.config.visuals.show_biomes.description"
      )
      @LatticeWidgetButton
      public boolean showBiomes = false;
      @LatticeOption(
         title = "axiom.editorui.mainmenu.view.show_annotations",
         description = "axiom.config.visuals.show_annotations.description"
      )
      @LatticeWidgetButton
      public boolean showAnnotations = true;
      @LatticeOption(
         title = "axiom.editorui.mainmenu.view.show_ruler",
         description = "axiom.config.visuals.show_ruler.description"
      )
      @LatticeWidgetButton
      public boolean showRuler = true;
      @LatticeOption(
         title = "axiom.editorui.mainmenu.view.show_key_presses",
         description = "axiom.config.visuals.show_key_presses.description"
      )
      @LatticeWidgetButton
      public boolean keypressOverlay = false;
      @LatticeOption(
         title = "axiom.contextmenu.show_key_hints",
         description = "axiom.config.visuals.show_key_hints.description"
      )
      @LatticeWidgetButton
      public boolean showKeyHints = true;
      @LatticeOption(
         title = "axiom.config.visuals.add_blockstate_to_tooltip",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean showBlockStateTooltip = true;
      @LatticeOption(
         title = "axiom.config.visuals.status_background",
         description = "!!.description"
      )
      @LatticeWidgetButton
      public boolean statusBackground = true;
   }
}
