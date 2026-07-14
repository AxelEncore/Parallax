package com.moulberry.axiom.tools.script_brush;

import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.LuaHelper;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.ChatUtils;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.extension.texteditor.TextEditor;
import imgui.moulberry92.extension.texteditor.TextEditorLanguage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

public class ScriptBrushTool implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private final ChunkedBooleanRegion removeRegion = new ChunkedBooleanRegion();
   private boolean painting = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private final PresetWidget presetWidget = new PresetWidget(this::loadSettings, this::writeSettings, "script_brush", ScriptBrushPresets.getDefaultPresets());
   private final TextEditor luaEditor = new TextEditor();
   private String checkedScript = null;
   private Globals luaGlobals = null;
   private LuaFunction compiledScript = null;
   private final List<ScriptArgument> customArguments = new ArrayList<>();
   private boolean executingOnce = false;
   private boolean ignoreMask = false;
   private long lastLuaCompile = 0L;
   private boolean luaDirty = false;
   public String luaExecutionError = null;
   private boolean scriptChanged = false;
   private String lastLuaText = null;
   private static final String LUA_DESCRIPTION = "Create a brush using a Lua script\nFunction must return block\n\n"
      + LuaHelper.getAvailableLuaFunctions(true)
      + "\nCustom directives (put at start of script):\n$once$ -> apply the script to a specific point instead of a brush\n$ignoreMask$ -> affect all blocks regardless of mask (also ignores selection)\n";

   public ScriptBrushTool() {
      this.luaEditor.setLanguage(TextEditorLanguage.Lua());
      this.luaEditor.setShowWhitespacesEnabled(false);
      this.luaEditor.setTabSize(4);
      this.luaEditor.setAutoIndentEnabled(true);
   }

   @Override
   public void reset() {
      if (this.painting) {
         this.painting = false;
         ChunkRenderOverrider.release("script_brush");
      }

      this.chunkedBlockRegion.clear();
      this.removeRegion.clear();
      this.luaGlobals = null;
      this.compiledScript = null;
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
            if (this.checkedScript != null && !this.checkedScript.isBlank()) {
               if (!AxiomClient.hasPermission(AxiomPermission.CAN_IMPORT_BLOCKS)
                  && this.checkedScript.replaceAll("local|function|return|if|for|then|end|and|or|nil|\\s", "").length() > 1000) {
                  ChatUtils.error("The server has limited the maximum script brush size to 1000");
                  return UserAction.ActionResult.USED_STOP;
               }

               this.compileScript();
               if (this.compiledScript == null) {
                  return UserAction.ActionResult.USED_STOP;
               }

               if (this.executingOnce) {
                  RayCaster.RaycastResult result = Tool.raycastBlock();
                  if (result != null) {
                     ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
                     BlockState blockState = level.getBlockState(result.blockPos());
                     if (blockState.getBlock() == Blocks.VOID_AIR) {
                        return UserAction.ActionResult.USED_STOP;
                     }

                     this.executeScriptAt(result.blockPos().getX(), result.blockPos().getY(), result.blockPos().getZ(), null);
                     this.applyBlockRegionChange();
                     this.reset();
                  }
               } else {
                  AsyncToolPather pather = this.createToolPather(this.brushWidget.getBrushShape());
                  if (pather != null) {
                     if (!this.painting) {
                        this.painting = true;
                        ChunkRenderOverrider.acquire("script_brush");
                     }

                     this.pathProvider = new AsyncToolPathProvider(pather);
                  }
               }

               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
            if (this.painting) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (this.luaGlobals != null) {
         LuaHelper.updateExtraVariables(this.luaGlobals);
      }

      if (!this.painting) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            if (!this.ignoreMask) {
               Selection.render(rc, 7);
            }

            return;
         }

         if (!this.ignoreMask) {
            Selection.render(rc, 4);
         }

         if (this.executingOnce) {
            Tool.renderRaycastOverlay(rc, result);
         } else {
            this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
         }
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         this.applyBlockRegionChange();
         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         if (!this.ignoreMask) {
            Selection.render(rc, 4);
         }

         this.pathProvider.update();
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
         this.removeRegion.render(rc, Vec3.ZERO, 8);
      }
   }

   private void compileScript() {
      this.compiledScript = null;
      this.luaGlobals = LuaHelper.createSandboxed();
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      LuaHelper.initializeGeneric(
         this.luaGlobals,
         (x, y, z, blockState) -> {
            if (blockState.isAir()
               && Minecraft.getInstance().level != null
               && !Minecraft.getInstance().level.getBlockState(mutableBlockPos.set(x, y, z)).isAir()) {
               ChunkRenderOverrider.setBlock(x, y, z, blockState);
               this.removeRegion.add(x, y, z);
            }

            this.chunkedBlockRegion.addBlock(x, y, z, blockState);
         }
      );

      try {
         String resolvedScript = this.applyCustomArguments(this.checkedScript);
         this.compiledScript = LuaHelper.compile(resolvedScript, this.luaGlobals);
         this.luaExecutionError = null;
      } catch (LuaError var5) {
         String message = var5.getMessage();
         String[] splitMessage = message.split(":");
         this.luaExecutionError = splitMessage[splitMessage.length - 1];
      }
   }

   private void applyBlockRegionChange() {
      if (!this.chunkedBlockRegion.isEmpty()) {
         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.modified", countString);
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
      }
   }

   private AsyncToolPather createToolPather(BrushShape brushShape) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         MaskElement destMask = (MaskElement)(this.ignoreMask ? new ConstantMaskElement(true) : MaskManager.getDestMask());
         MaskContext maskContext = new MaskContext(level);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         return this.compiledScript != null ? new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
            if (destMask.test(maskContext.reset(), x, y, z) && this.compiledScript != null) {
               BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
               if (blockState.getBlock() != Blocks.VOID_AIR) {
                  this.executeScriptAt(x, y, z, blockState);
               }
            }
         }) : null;
      }
   }

   private void executeScriptAt(int x, int y, int z, BlockState old) {
      LuaHelper.setPosition(this.luaGlobals, x, y, z);

      try {
         LuaValue value = this.compiledScript.call();
         if (value.isint()) {
            BlockState outState = LuaHelper.internalIdToState(value.toint());
            if (outState != null) {
               if (old != null && !old.isAir() && outState.isAir()) {
                  ChunkRenderOverrider.setBlock(x, y, z, outState);
                  this.removeRegion.add(x, y, z);
               }

               this.chunkedBlockRegion.addBlock(x, y, z, outState);
            }
         } else if (!value.isnil()) {
            this.luaExecutionError = "expected block or nil output, got " + value.typename() + " instead";
            this.reset();
         }
      } catch (LuaError var8) {
         String message = var8.getMessage();
         String[] splitMessage = message.split(":");
         this.luaExecutionError = splitMessage[splitMessage.length - 1];
         this.reset();
      }
   }

   @Override
   public void displayImguiOptions() {
      if (!this.executingOnce) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
         this.brushWidget.displayImgui();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(this.scriptChanged);
      this.scriptChanged = false;
      if (!this.customArguments.isEmpty()) {
         ImGuiHelper.separatorWithText("Arguments");
         String lastCategory = null;
         boolean showCategory = true;
         boolean indent = false;

         for (int i = 0; i < this.customArguments.size(); i++) {
            ScriptArgument customArgument = this.customArguments.get(i);
            String category = customArgument.category;
            if (!Objects.equals(lastCategory, category)) {
               if (indent) {
                  ImGui.unindent();
               }

               if (category == null) {
                  showCategory = true;
               } else {
                  showCategory = ImGui.collapsingHeader(category + "##" + i);
                  ImGui.indent();
                  indent = true;
               }

               lastCategory = category;
            }

            if (showCategory) {
               customArgument.displayImgui(this.selectBlockWidget, i);
            }
         }

         if (indent) {
            ImGui.unindent();
         }
      }

      this.renderLuaEditor();
   }

   private void renderLuaEditor() {
      if (this.luaExecutionError != null) {
         ImGui.textColored(-12566273, AxiomI18n.get("axiom.hardcoded.script_error"));
         ImGui.textWrapped(this.luaExecutionError);
         if (ImGui.button(AxiomI18n.get("axiom.generic.close"))) {
            this.luaExecutionError = null;
         }

         ImGui.separator();
      }

      ImGuiHelper.separatorWithText("Lua Script Brush (Hover for help)");
      ImGuiHelper.tooltip(LUA_DESCRIPTION);
      ImGui.pushFont(EditorUI.monospace, EditorUI.monospace.getLegacySize());
      this.luaEditor.render("TextEditor");
      ImGui.popFont();
      String text = this.luaEditor.getText();
      if (!Objects.equals(this.lastLuaText, text)) {
         this.lastLuaText = text;
         this.luaDirty = true;
         this.scriptChanged = true;
      }

      if (this.luaDirty) {
         long time = System.currentTimeMillis();
         if (time > this.lastLuaCompile + 250L) {
            this.lastLuaCompile = time;
            this.luaDirty = false;
            Globals globals = LuaHelper.createSandboxed();
            this.luaEditor.clearMarkers();

            try {
               String script = this.luaEditor.getText();
               this.updateCustomArguments(script);
               String resolvedScript = this.applyCustomArguments(script);
               LuaValue luaValue = globals.load(resolvedScript, "main.lua");
               if (!luaValue.isfunction()) {
                  this.luaEditor.addMarker(1, -2145378176, -2145378176, "not a function");
                  this.checkedScript = null;
               } else {
                  this.checkedScript = script;
               }
            } catch (LuaError var12) {
               String message = var12.getMessage();
               Pattern pattern = Pattern.compile(":(\\d+):");
               Matcher matcher = pattern.matcher(message);
               int line = 1;
               if (matcher.find()) {
                  line = Integer.parseInt(matcher.group(1));
               }

               int totalLines = this.luaEditor.getLineCount();
               if (line > totalLines) {
                  line = totalLines;
               }

               String[] splitMessage = message.split(":");
               message = splitMessage[splitMessage.length - 1];
               this.luaEditor.addMarker(line, -2145378176, -2145378176, message);
               this.checkedScript = null;
            }
         }
      }
   }

   private void updateCustomArguments(String script) {
      this.executingOnce = script.contains("$once$");
      this.ignoreMask = script.contains("$ignoreMask$");
      Pattern pattern = Pattern.compile("\\$([^$()\n]+)(?:\\(([^$\n]*)\\))?\\$");
      Matcher matcher = pattern.matcher(script);
      String category = null;
      int index = 0;

      while (matcher.find()) {
         String id = matcher.group(1);
         String args = matcher.group(2);
         if (id != null && id.equalsIgnoreCase("category")) {
            if (args != null && args.startsWith("'") && args.endsWith("'")) {
               args = args.substring(1, args.length() - 1);
            }

            if (args != null && args.startsWith("\"") && args.endsWith("\"")) {
               args = args.substring(1, args.length() - 1);
            }

            if (args != null && !args.equals("nil") && !args.equals("null")) {
               category = args.replace("#", "");
            } else {
               category = null;
            }
         } else {
            ScriptArgument scriptArgument = ScriptArgument.parse(id, args);
            if (scriptArgument != null) {
               scriptArgument.category = category;
               if (index >= this.customArguments.size()) {
                  this.customArguments.add(scriptArgument);
               } else {
                  ScriptArgument old = this.customArguments.get(index);
                  if (old.type != scriptArgument.type || !old.name.equals(scriptArgument.name) || !Objects.equals(old.category, scriptArgument.category)) {
                     this.customArguments.set(index, scriptArgument);
                  }
               }

               index++;
            }
         }
      }

      while (this.customArguments.size() > index) {
         this.customArguments.remove(this.customArguments.size() - 1);
      }
   }

   private String applyCustomArguments(String script) {
      if (this.executingOnce) {
         script = script.replace("$once$", "-- $once$");
      }

      if (this.ignoreMask) {
         script = script.replace("$ignoreMask$", "-- $ignoreMask$");
      }

      Pattern pattern = Pattern.compile("\\$([^$()\n]+)(?:\\(([^$\n]*)\\))?\\$");
      Matcher matcher = pattern.matcher(script);
      AtomicInteger index = new AtomicInteger(0);
      return matcher.replaceAll(match -> {
         String id = matcher.group(1);
         if (id != null && id.equalsIgnoreCase("category")) {
            return "-- " + matcher.group().replace("$", "\\$");
         } else {
            ScriptArgument defaultArgument = ScriptArgument.parse(id, matcher.group(2));
            if (defaultArgument != null) {
               int indexI = index.getAndIncrement();
               if (indexI >= this.customArguments.size()) {
                  return defaultArgument.toLuaString();
               } else {
                  ScriptArgument customArgument = this.customArguments.get(indexI);
                  return customArgument.type == defaultArgument.type ? customArgument.toLuaString() : defaultArgument.toLuaString();
               }
            } else {
               return matcher.group().replace("$", "\\$");
            }
         }
      });
   }

   @Override
   public String listenForEsc() {
      return !this.painting ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public boolean initiateAdjustment() {
      return this.executingOnce ? false : this.brushWidget.initiateAdjustment();
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      return this.executingOnce ? mouseDelta : this.brushWidget.renderAdjustment(mouseX, mouseY, mouseDelta);
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.script_brush");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putString("Script", this.luaEditor.getText());
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      String script = VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "Script", "");
      this.luaEditor.setText(script);
      this.lastLuaText = script;
      this.customArguments.clear();
      this.executingOnce = false;
      this.luaDirty = true;
   }

   @Override
   public boolean showToolSmoothing() {
      return !this.executingOnce;
   }

   @Override
   public char iconChar() {
      return '\ue91f';
   }

   @Override
   public String keybindId() {
      return "script_brush";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_SCRIPTBRUSH, AxiomPermission.BUILD_SECTION);
   }
}
