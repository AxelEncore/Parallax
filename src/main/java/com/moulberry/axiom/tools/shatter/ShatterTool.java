package com.moulberry.axiom.tools.shatter;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.noise.VoronoiEdgesNoise;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ShatterTool implements Tool {
   protected final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   protected final ChunkedBooleanRegion removeRegion = new ChunkedBooleanRegion();
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private final int[] scale = new int[]{8};
   private final int[] width = new int[]{1};
   private boolean useActiveBlock = false;
   private final int[] axis = new int[]{0};
   private final ImString noiseSeed = new ImString();
   private final int defaultRandomValue = ThreadLocalRandom.current().nextInt();
   private final PresetWidget presetWidget = new PresetWidget(this, "shatter");

   public ShatterTool() {
      this.noiseSeed.set(String.valueOf(this.defaultRandomValue), false);
   }

   @Override
   public void reset() {
      if (this.usingTool) {
         this.usingTool = false;
         ChunkRenderOverrider.release("shatter");
      }

      this.removeRegion.clear();
      this.chunkedBlockRegion.clear();
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
            if (!this.usingTool) {
               this.usingTool = true;
               ChunkRenderOverrider.acquire("shatter");
            }

            this.pathProvider = new AsyncToolPathProvider(this.createToolPather(this.brushWidget.getBrushShape()));
            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
         case DELETE:
            if (this.usingTool) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (!this.usingTool) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            Selection.render(rc, 7);
            return;
         }

         Selection.render(rc, 4);
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 2);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         BlockState block = this.useActiveBlock ? Tool.getActiveBlock() : Blocks.AIR.defaultBlockState();
         if (block.isAir()) {
            String countString = NumberFormat.getInstance().format((long)this.removeRegion.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.shatter", countString);
            RegionHelper.pushBooleanRegionChange(this.removeRegion, Blocks.AIR.defaultBlockState(), historyDescription);
         } else {
            String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.shatter", countString);
            RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
         }

         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         Selection.render(rc, 4);
         this.pathProvider.update();
         this.removeRegion.render(rc, Vec3.ZERO, 8);
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
      }
   }

   private AsyncToolPather createToolPather(BrushShape brushShape) {
      String seedStr = ImGuiHelper.getString(this.noiseSeed).trim();
      int seed;
      if (seedStr.isEmpty()) {
         seed = this.defaultRandomValue;
      } else {
         try {
            seed = Integer.parseInt(seedStr);
         } catch (NumberFormatException var13) {
            seed = 0;

            for (char c : seedStr.toCharArray()) {
               seed = 31 * seed + c;
            }
         }
      }

      VoronoiEdgesNoise crackNoise = new VoronoiEdgesNoise(seed, 1.0F);
      BlockState setBlock = this.useActiveBlock ? Tool.getActiveBlock() : Blocks.AIR.defaultBlockState();
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      int scale = this.scale[0];
      int width = this.width[0];
      int axis = this.axis[0];
      ClientLevel level = Minecraft.getInstance().level;
      MaskElement maskElement = MaskManager.getSourceMask();
      MaskContext maskContext = new MaskContext(level);
      return setBlock.isAir() ? new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
         BlockState block = level.getBlockState(mutableBlockPos.set(x, y, z));
         if (!block.isAir() && maskElement.test(maskContext.reset(), x, y, z)) {
            float noise = switch (axis) {
               case 1 -> crackNoise.evaluate((y + 0.5) / scale, (z + 0.5) / scale);
               case 2 -> crackNoise.evaluate((x + 0.5) / scale, (z + 0.5) / scale);
               case 3 -> crackNoise.evaluate((x + 0.5) / scale, (y + 0.5) / scale);
               default -> crackNoise.evaluate((x + 0.5) / scale, (y + 0.5) / scale, (z + 0.5) / scale);
            };
            if (noise < width / (scale * 2.0F)) {
               ChunkRenderOverrider.setBlock(x, y, z, Blocks.AIR.defaultBlockState());
               this.removeRegion.add(x, y, z);
            }
         }
      }) : new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
         BlockState block = level.getBlockState(mutableBlockPos.set(x, y, z));
         if (!block.isAir() && maskElement.test(maskContext.reset(), x, y, z)) {
            float noise = switch (axis) {
               case 1 -> crackNoise.evaluate((y + 0.5) / scale, (z + 0.5) / scale);
               case 2 -> crackNoise.evaluate((x + 0.5) / scale, (z + 0.5) / scale);
               case 3 -> crackNoise.evaluate((x + 0.5) / scale, (y + 0.5) / scale);
               default -> crackNoise.evaluate((x + 0.5) / scale, (y + 0.5) / scale, (z + 0.5) / scale);
            };
            if (noise < width / (scale * 2.0F)) {
               this.chunkedBlockRegion.addBlock(x, y, z, setBlock);
            }
         }
      });
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shatter"));
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.shatter.scale"), this.scale, 0, 50);
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.shatter.width"), this.width, 1, 8);
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shatter.use_active_block"), this.useActiveBlock)) {
         this.useActiveBlock = !this.useActiveBlock;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.noise"));
      changed |= ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.shatter.axis"),
         this.axis,
         new String[]{
            AxiomI18n.get("axiom.tool.shatter.axis_3d"),
            AxiomI18n.get("axiom.tool.shatter.axis_x"),
            AxiomI18n.get("axiom.tool.shatter.axis_y"),
            AxiomI18n.get("axiom.tool.shatter.axis_z")
         }
      );
      changed |= ImGui.inputText(AxiomI18n.get("axiom.tool.generic.noise_seed"), this.noiseSeed);
      if (ImGui.button(AxiomI18n.get("axiom.tool.generic.noise_seed_do_randomize"))) {
         this.noiseSeed.set(String.valueOf(ThreadLocalRandom.current().nextInt()), false);
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
   }

   @Override
   public String listenForEsc() {
      return !this.usingTool ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public boolean initiateAdjustment() {
      return this.brushWidget.initiateAdjustment();
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      return this.brushWidget.renderAdjustment(mouseX, mouseY, mouseDelta);
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.shatter");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putInt("ShatterScale", this.scale[0]);
      tag.putInt("ShatterWidth", this.width[0]);
      tag.putBoolean("UseActiveBlock", this.useActiveBlock);
      tag.putByte("Axis", (byte)this.axis[0]);
      tag.putString("NoiseSeed", ImGuiHelper.getString(this.noiseSeed));
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.scale[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "ShatterScale", 8);
      this.width[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "ShatterWidth", 1);
      this.useActiveBlock = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "UseActiveBlock", false);
      this.axis[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Axis", 0);
      this.noiseSeed.set(VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "NoiseSeed", String.valueOf(this.defaultRandomValue)), false);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue911';
   }

   @Override
   public String keybindId() {
      return "shatter";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_SHATTER, AxiomPermission.BUILD_SECTION);
   }
}
