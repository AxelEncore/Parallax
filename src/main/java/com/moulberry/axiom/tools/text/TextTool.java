package com.moulberry.axiom.tools.text;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.HDVoxelMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.AsyncFileDialogs;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.utils.RenderHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.moulberry.axiom.platform.AxiomPlatform;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class TextTool implements Tool {
   private final PositionSet cutout = new PositionSet();
   private final ChunkedBlockRegion blocks = new ChunkedBlockRegion();
   @Nullable
   private Gizmo gizmo = null;
   private BlockState oldActiveBlock = null;
   private Path oldCustomFont = null;
   private final ImString text = new ImString(256);
   private final int[] direction = new int[]{0};
   private final int[] size = new int[]{64};
   private boolean useFullBlocks = false;
   private boolean useBuiltinFont = true;
   private Path customFont = null;

   @Override
   public void reset() {
      if (this.gizmo != null) {
         ChunkRenderOverrider.release("text_tool");
         this.gizmo = null;
      }

      this.blocks.clear();
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            RayCaster.RaycastResult result = Tool.raycastBlock();
            if (result != null) {
               if (this.gizmo == null) {
                  ChunkRenderOverrider.acquire("text_tool");
               }

               this.gizmo = new Gizmo(result.getBlockPos());
               this.drawText();
            }

            return UserAction.ActionResult.USED_STOP;
         case REDO:
            if (this.gizmo != null) {
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case ESCAPE:
         case DELETE:
         case UNDO:
            if (this.gizmo == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.reset();
            return UserAction.ActionResult.USED_STOP;
         case ENTER:
            if (this.gizmo != null && !this.blocks.isEmpty()) {
               String countString = NumberFormat.getInstance().format((long)this.blocks.count());
               String historyDescription = AxiomI18n.get("axiom.history_description.text_tool", countString);
               if (this.direction[0] != 0 && this.direction[0] != 1) {
                  int maxX = this.blocks.max().getX();
                  int maxZ = this.blocks.max().getZ();
                  RegionHelper.pushBlockRegionChangeOffset(this.blocks, this.gizmo.getTargetPosition().offset(-maxX, 0, -maxZ), historyDescription, 0);
               } else {
                  int minX = this.blocks.min().getX();
                  int minZ = this.blocks.min().getZ();
                  RegionHelper.pushBlockRegionChangeOffset(this.blocks, this.gizmo.getTargetPosition().offset(-minX, 0, -minZ), historyDescription, 0);
               }

               this.reset();
            }

            return UserAction.ActionResult.USED_STOP;
         case LEFT_MOUSE:
            if (this.leftClick()) {
               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case SCROLL:
            if (this.gizmo != null) {
               UserAction.ScrollAmount scrollObject = (UserAction.ScrollAmount)object;
               this.handleScroll(scrollObject.scrollX(), scrollObject.scrollY());
               return UserAction.ActionResult.USED_STOP;
            }
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   private boolean leftClick() {
      return this.gizmo == null ? false : this.gizmo.leftClick();
   }

   public void handleScroll(int xScroll, int yScroll) {
      if (this.gizmo != null && (!EditorUI.isCtrlOrCmdDown() || this.gizmo.isGrabbed())) {
         if (this.gizmo != null) {
            Vec3 lookDirection = Tool.getLookDirection();
            if (lookDirection == null) {
               return;
            }

            BlockPos oldPosition = this.gizmo.getTargetPosition();
            this.gizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), lookDirection);
            if (!oldPosition.equals(this.gizmo.getTargetPosition()) && !this.blocks.isEmpty()) {
               if (this.direction[0] != 0 && this.direction[0] != 1) {
                  int maxX = this.blocks.max().getX();
                  int maxZ = this.blocks.max().getZ();
                  ChunkRenderOverrider.cutoutBoolean(this.cutout, this.gizmo.getTargetPosition().offset(-maxX, 0, -maxZ));
               } else {
                  int minX = this.blocks.min().getX();
                  int minZ = this.blocks.min().getZ();
                  ChunkRenderOverrider.cutoutBoolean(this.cutout, this.gizmo.getTargetPosition().offset(-minX, 0, -minZ));
               }
            }
         }
      }
   }

   public void drawText() {
      if (this.gizmo != null) {
         this.oldActiveBlock = Tool.getActiveBlock();
         this.oldCustomFont = this.customFont;
         float size = this.size[0];
         if (size < 8.0F) {
            size = 8.0F;
         }

         if (size > 256.0F) {
            size = 256.0F;
         }

         String text = ImGuiHelper.getString(this.text);
         this.blocks.clear();
         HDVoxelMap.HDVoxelBaseBlocks blocks = HDVoxelMap.getAssociatedBlocks(this.oldActiveBlock.getBlock());
         if (blocks != null && !this.useFullBlocks) {
            TextDrawing.drawFancy(this.blocks, this.useBuiltinFont ? null : this.customFont, blocks, text, this.direction[0], size);
         } else {
            TextDrawing.drawSimple(this.blocks, this.useBuiltinFont ? null : this.customFont, this.oldActiveBlock, text, this.direction[0], size / 2.0F);
         }

         if (this.blocks.isEmpty()) {
            this.cutout.clear();
            ChunkRenderOverrider.clear();
         } else {
            this.cutout.clear();
            this.blocks.forEachEntry((x, y, z, block) -> this.cutout.add(x, y, z));
            if (this.direction[0] != 0 && this.direction[0] != 1) {
               int maxX = this.blocks.max().getX();
               int maxZ = this.blocks.max().getZ();
               ChunkRenderOverrider.cutoutBoolean(this.cutout, this.gizmo.getTargetPosition().offset(-maxX, 0, -maxZ));
            } else {
               int minX = this.blocks.min().getX();
               int minZ = this.blocks.min().getZ();
               ChunkRenderOverrider.cutoutBoolean(this.cutout, this.gizmo.getTargetPosition().offset(-minX, 0, -minZ));
            }
         }
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (this.gizmo == null) {
         Tool.renderRaycastOverlay(rc, Tool.raycastBlock());
      }

      if (this.oldActiveBlock != Tool.getActiveBlock()) {
         this.drawText();
      }

      if (this.customFont != this.oldCustomFont) {
         this.drawText();
      }

      if (this.gizmo != null) {
         Vec3 lookDirection = Tool.getLookDirection();
         boolean isLeftDown = Tool.isMouseDown(0);
         boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
         boolean showGizmo = !EditorUI.isActive() || !isCtrlDown;
         BlockPos oldPosition = this.gizmo.getTargetPosition();
         this.gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
         this.gizmo
            .setAxisDirections(
               rc.x() > this.gizmo.getTargetPosition().getX(), rc.y() > this.gizmo.getTargetPosition().getY(), rc.z() > this.gizmo.getTargetPosition().getZ()
            );
         if (!this.blocks.isEmpty()) {
            if (!oldPosition.equals(this.gizmo.getTargetPosition())) {
               if (this.direction[0] != 0 && this.direction[0] != 1) {
                  int maxX = this.blocks.max().getX();
                  int maxZ = this.blocks.max().getZ();
                  ChunkRenderOverrider.cutoutBoolean(this.cutout, this.gizmo.getTargetPosition().offset(-maxX, 0, -maxZ));
               } else {
                  int minX = this.blocks.min().getX();
                  int minZ = this.blocks.min().getZ();
                  ChunkRenderOverrider.cutoutBoolean(this.cutout, this.gizmo.getTargetPosition().offset(-minX, 0, -minZ));
               }
            }

            int minX = this.blocks.min().getX();
            int minY = this.blocks.min().getY();
            int minZ = this.blocks.min().getZ();
            int maxX = this.blocks.max().getX();
            int maxY = this.blocks.max().getY();
            int maxZ = this.blocks.max().getZ();
            Vec3 blockOffset;
            Vec3 one;
            Vec3 two;
            if (this.direction[0] != 0 && this.direction[0] != 1) {
               blockOffset = this.gizmo.getInterpPosition().subtract(0.5, 0.5, 0.5).subtract(maxX, 0.0, maxZ);
               one = this.gizmo.getInterpPosition().subtract(0.5, 0.5, 0.5).add(minX - maxX, minY, minZ - maxZ);
               two = this.gizmo.getInterpPosition().add(0.5, 0.5, 0.5).add(0.0, maxY, 0.0);
            } else {
               blockOffset = this.gizmo.getInterpPosition().subtract(0.5, 0.5, 0.5).subtract(minX, 0.0, minZ);
               one = this.gizmo.getInterpPosition().subtract(0.5, 0.5, 0.5).add(0.0, minY, 0.0);
               two = this.gizmo.getInterpPosition().add(0.5, 0.5, 0.5).add(maxX - minX, maxY, maxZ - minZ);
            }

            this.blocks.render(rc, blockOffset, null, 1.0F, 0.0F, false);
            this.renderBoundingBox(rc, one, two);
         }

         if (this.gizmo.isGrabbed() || showGizmo) {
            this.gizmo.render(rc, isCtrlDown);
         }
      }
   }

   private void renderBoundingBox(AxiomWorldRenderContext rc, Vec3 one, Vec3 two) {
      double minX = Math.min(one.x(), two.x()) - 1.0E-4;
      double minY = Math.min(one.y(), two.y()) - 1.0E-4;
      double minZ = Math.min(one.z(), two.z()) - 1.0E-4;
      double maxX = Math.max(one.x(), two.x()) + 1.0E-4;
      double maxY = Math.max(one.y(), two.y()) + 1.0E-4;
      double maxZ = Math.max(one.z(), two.z()) + 1.0E-4;
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      matrices.translate(minX - rc.x(), minY - rc.y(), minZ - rc.z());
      RenderHelper.tryApplyModelViewMatrix();
      float sizeX = (float)(maxX - minX);
      float sizeY = (float)(maxY - minY);
      float sizeZ = (float)(maxZ - minZ);
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      Shapes.lineBox(matrices, bufferBuilder, 0.0F, 0.0F, 0.0F, sizeX, sizeY, sizeZ, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, RenderHelper.baseLineWidth);
      MeshData meshData = bufferBuilder.build();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.5F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.15F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      matrices.popPose();
   }

   @Override
   public void displayImguiOptions() {
      boolean changed = false;
      changed |= ImGui.inputText(AxiomI18n.get("axiom.tool.text.text_input"), this.text);
      changed |= ImGuiHelper.combo(AxiomI18n.get("axiom.tool.text.direction"), this.direction, new String[]{"+X", "+Z", "-X", "-Z"});
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.text.font"));
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.text.text_size"), this.size, 12, 256);
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.text.use_builtin_font"), this.useBuiltinFont)) {
         this.useBuiltinFont = !this.useBuiltinFont;
         changed = true;
      }

      if (!this.useBuiltinFont) {
         String selectFontText;
         if (this.customFont != null) {
            selectFontText = AxiomI18n.get("axiom.tool.text.font") + ": " + this.customFont.getFileName().toString();
         } else {
            selectFontText = AxiomI18n.get("axiom.tool.text.select_font");
         }

         if (ImGui.button(selectFontText)) {
            Path fontDirectory = this.getDefaultFontDirectory();
            String defaultPath = fontDirectory.toString();
            String separator = fontDirectory.getFileSystem().getSeparator();
            if (!defaultPath.endsWith(separator)) {
               defaultPath = defaultPath + separator;
            }

            CompletableFuture<String> future = AsyncFileDialogs.openFileDialog(defaultPath, "TrueType Font", "ttf", "otf");
            future.thenAccept(path -> this.customFont = Path.of(path));
         }
      }

      BlockState activeBlock = Tool.getActiveBlock();
      if (HDVoxelMap.getAssociatedBlocks(activeBlock.getBlock()) != null) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.text.rasterization"));
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.text.use_full_blocks"), this.useFullBlocks)) {
            this.useFullBlocks = !this.useFullBlocks;
            changed = true;
         }
      }

      if (changed) {
         this.drawText();
      }
   }

   private Path getDefaultFontDirectory() {
      List<Path> possiblePaths = new ArrayList<>();
      switch (Util.getPlatform()) {
         case LINUX:
         case SOLARIS:
            Path pathxx = Path.of("/usr", "share", "fonts");
            if (Files.exists(pathxx)) {
               possiblePaths.add(pathxx);
            }

            pathxx = Path.of("/usr", "local", "share", "fonts");
            if (Files.exists(pathxx)) {
               possiblePaths.add(pathxx);
            }

            pathxx = Path.of(System.getProperty("user.home"), ".local", "share", "fonts");
            if (Files.exists(pathxx)) {
               possiblePaths.add(pathxx);
            }
            break;
         case WINDOWS:
            Path pathx = Path.of("C:/Windows", "Fonts");
            if (Files.exists(pathx)) {
               possiblePaths.add(pathx);
            }
            break;
         case OSX:
            Path path = Path.of("/Library", "Fonts");
            if (Files.exists(path)) {
               possiblePaths.add(path);
            }

            path = Path.of("/System", "Library", "Fonts");
            if (Files.exists(path)) {
               possiblePaths.add(path);
            }

            path = Path.of(System.getProperty("user.home"), "Library", "Fonts");
            if (Files.exists(path)) {
               possiblePaths.add(path);
            }
      }

      int highestCount = 0;
      Path bestPath = null;

      for (Path possiblePath : possiblePaths) {
         int pathCount = countFontFiles(possiblePath);
         if (bestPath == null || pathCount > highestCount) {
            bestPath = possiblePath;
            highestCount = pathCount;
         }
      }

      return bestPath != null ? bestPath : AxiomPlatform.gameDir();
   }

   private static int countFontFiles(Path path) {
      if (Files.exists(path) && Files.isDirectory(path)) {
         try {
            int count = 0;

            for (Path child : Files.newDirectoryStream(path)) {
               String childName = child.getFileName().toString();
               if (childName.endsWith(".ttf") || childName.endsWith(".otf")) {
                  count++;
               }
            }

            return count;
         } catch (IOException var5) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   @Override
   public String listenForEsc() {
      return this.gizmo == null ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public String listenForEnter() {
      return this.gizmo != null && !this.blocks.isEmpty() ? AxiomI18n.get("axiom.widget.confirm") : null;
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.text");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putInt("Size", this.size[0]);
      tag.putBoolean("UseFullBlocks", this.useFullBlocks);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.size[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Size", 64);
      this.useFullBlocks = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "UseFullBlocks", false);
      this.useBuiltinFont = true;
   }

   @Override
   public char iconChar() {
      return '\ue901';
   }

   @Override
   public String keybindId() {
      return "text";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_TEXT, AxiomPermission.BUILD_SECTION);
   }
}
