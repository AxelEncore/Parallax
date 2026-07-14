package com.moulberry.axiom.tools.rock;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.funcinterfaces.IntIntIntFloatConsumer;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.noise.NoiseInterface;
import com.moulberry.axiom.noise.SimplexNoise;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPatherMinSDF;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.GaussianBlurTable;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class RockTool implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private boolean painting = false;
   private final Position2FloatMap influenceMap = new Position2FloatMap();
   private final PositionSet calculatedSet = new PositionSet();
   private final Position2FloatMap smoothedInfluenceMap = new Position2FloatMap();
   private GaussianBlurTable gaussianBlur = null;
   private AsyncToolPathProvider pathProvider = null;
   private NoiseInterface noise = null;
   private MaskElement cachedDestMask = null;
   private MaskContext cachedMaskContext = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private final int[] noiseRadius = new int[]{5};
   private final float[] noisiness = new float[]{0.5F};
   private final float[] smoothingStddev = new float[]{2.0F};
   private final ImString noiseSeed = new ImString();
   private final float[] solidBlockInfluence = new float[]{2.0F};
   private boolean replaceSolidBlocks = false;
   private boolean extendToGround = false;
   private final PresetWidget presetWidget = new PresetWidget(this, "rock");

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case UNDO:
            if (!this.painting) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.reset();
            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
            if (this.painting) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case RIGHT_MOUSE:
            this.rightClick();
            return UserAction.ActionResult.USED_STOP;
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Override
   public void reset() {
      this.painting = false;
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }

      this.cachedDestMask = null;
      this.cachedMaskContext = null;
      this.influenceMap.clear();
      this.calculatedSet.clear();
      this.smoothedInfluenceMap.clear();
      this.chunkedBlockRegion.clear();
   }

   private void rightClick() {
      if (!this.painting) {
         this.influenceMap.clear();
         this.calculatedSet.clear();
         this.chunkedBlockRegion.clear();
         this.painting = true;
         String seedStr = ImGuiHelper.getString(this.noiseSeed).trim();
         long seed;
         if (seedStr.isEmpty()) {
            seed = ThreadLocalRandom.current().nextLong();
         } else {
            try {
               seed = Long.parseLong(seedStr);
            } catch (NumberFormatException var9) {
               seed = 0L;

               for (char c : seedStr.toCharArray()) {
                  seed = 31L * seed + c;
               }
            }
         }

         this.noise = new SimplexNoise(seed);
         BrushShape brushShape = this.brushWidget.getBrushShape();
         this.pathProvider = new AsyncToolPathProvider(new AsyncToolPatherMinSDF(brushShape, this.createPatherTask()));
         float threshold = 0.01F;
         if (this.solidBlockInfluence[0] > 1.5F) {
            float solidBlockReinfluenceFactor = (this.solidBlockInfluence[0] - 1.0F) * 2.0F;
            threshold = 0.01F / solidBlockReinfluenceFactor;
         }

         this.gaussianBlur = new GaussianBlurTable(this.smoothingStddev[0], threshold);
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (!this.painting) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            Selection.render(rc, 7);
            return;
         }

         Selection.render(rc, 4);
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 1);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
         BlockState activeBlock = Tool.getActiveBlock();
         if (this.cachedDestMask == null) {
            this.cachedDestMask = MaskManager.getDestMask();
            this.cachedMaskContext = new MaskContext(world);
         }

         this.pathProvider.finish();
         this.performSmoothing(world, this.cachedDestMask, this.cachedMaskContext, activeBlock);
         this.smoothedInfluenceMap.clear();
         ClientLevel level = Minecraft.getInstance().level;
         if (this.extendToGround && level != null) {
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            Position2ObjectMap<BlockState> newBlocks = new Position2ObjectMap<>(k -> new BlockState[4096]);
            this.chunkedBlockRegion.forEachEntry((x, y, z, blockx) -> {
               for (int yo = 1; yo < 256 && this.chunkedBlockRegion.getBlockStateOrAir(x, y - yo, z).isAir(); yo++) {
                  BlockState below = level.getBlockState(mutableBlockPos.set(x, y - yo, z));
                  if (below.getBlock() == Blocks.VOID_AIR || !below.canBeReplaced()) {
                     break;
                  }

                  newBlocks.put(x, y - yo, z, blockx);
               }
            });
            newBlocks.forEachEntry(this.chunkedBlockRegion::addBlock);
         }

         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.rock_tool", countString);
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
         this.reset();
      } else {
         ClientLevel worldx = Objects.requireNonNull(Minecraft.getInstance().level);
         BlockState block = Tool.getActiveBlock();
         Selection.render(rc, 4);
         if (this.cachedDestMask == null) {
            this.cachedDestMask = MaskManager.getDestMask();
            this.cachedMaskContext = new MaskContext(worldx);
         }

         this.pathProvider.update();
         this.performSmoothing(worldx, this.cachedDestMask, this.cachedMaskContext, block);
         this.smoothedInfluenceMap.clear();
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
      }
   }

   private void performSmoothing(ClientLevel world, MaskElement destMask, MaskContext maskContext, BlockState block) {
      if (!this.gaussianBlur.isSingleValued()) {
         Position2FloatMap reinfluenceMap = new Position2FloatMap();
         float[] table = this.gaussianBlur.getTable();
         float threshold = 0.08F + table[table.length / 2] * 0.92F;
         float solidBlockReinfluenceFactor = this.solidBlockInfluence[0] < 1.1F ? 0.0F : (this.solidBlockInfluence[0] - 1.0F) * 2.0F;
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         this.gaussianBlur.applyBlurCheckThreshold(this.smoothedInfluenceMap, this.influenceMap, threshold, (x, y, z) -> {
            if (destMask.test(maskContext.reset(), x, y, z)) {
               if (!world.getBlockState(mutableBlockPos.set(x, y, z)).blocksMotion()) {
                  this.chunkedBlockRegion.addBlock(x, y, z, block);
               } else if (solidBlockReinfluenceFactor > 0.0F) {
                  if (this.replaceSolidBlocks) {
                     this.chunkedBlockRegion.addBlock(x, y, z, block);
                  }

                  reinfluenceMap.put(x, y, z, solidBlockReinfluenceFactor);
               }
            }
         });
         if (solidBlockReinfluenceFactor > 0.0F) {
            this.gaussianBlur.applyBlurCheckThreshold(reinfluenceMap, this.influenceMap, threshold, (x, y, z) -> {
               if (destMask.test(maskContext.reset(), x, y, z)) {
                  if (this.replaceSolidBlocks || !world.getBlockState(mutableBlockPos.set(x, y, z)).blocksMotion()) {
                     this.chunkedBlockRegion.addBlock(x, y, z, block);
                  }
               }
            });
         }
      }
   }

   private IntIntIntFloatConsumer createPatherTask() {
      ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
      MutableBlockPos mutable = new MutableBlockPos();
      MaskElement destMaskElement = MaskManager.getDestMask();
      MaskContext maskContext = new MaskContext(world);
      float noisiness = this.noisiness[0];
      float scale = 1.0F / this.noiseRadius[0];
      BlockState blockState = Tool.getActiveBlock();
      return (x, y, z, distance) -> {
         if (!this.calculatedSet.contains(x, y, z)) {
            float noise;
            if (noisiness >= 0.01) {
               float v = this.noise.evaluate((x + 0.5) * scale, (y + 0.5) * scale, (z + 0.5) * scale);
               noise = v * noisiness + (1.0F - noisiness);
            } else {
               noise = 1.0F;
            }

            if (distance < noise) {
               this.calculatedSet.add(x, y, z);
               boolean isAir = !world.getBlockState(mutable.set(x, y, z)).blocksMotion();
               if (this.gaussianBlur.isSingleValued()) {
                  if (!isAir && !this.replaceSolidBlocks) {
                     return;
                  }

                  if (!destMaskElement.test(maskContext.reset(), x, y, z)) {
                     return;
                  }

                  this.chunkedBlockRegion.addBlock(x, y, z, blockState);
               } else {
                  this.smoothedInfluenceMap.put(x, y, z, isAir ? 1.0F : 1.0F + Math.min(1.0F, this.solidBlockInfluence[0]));
               }
            }
         }
      };
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.noise"));
      ImGuiHelper.Label noiseRadius = ImGuiHelper.translateLabel("axiom.tool.rock.noise_radius");
      ImGuiHelper.Label noisiness = ImGuiHelper.translateLabel("axiom.tool.rock.noisiness");
      ImGuiHelper.Label noiseFieldSeed = ImGuiHelper.translateLabel("axiom.tool.rock.noise_field_seed");
      ImGuiHelper.LabelPosition labelPosition = ImGuiHelper.calcLabelPosition(noiseRadius, noisiness, noiseFieldSeed);
      changed |= ImGuiHelper.drawLabelledWidget(noiseRadius, labelPosition, label -> ImGui.sliderInt(label, this.noiseRadius, 5, 30));
      changed |= ImGuiHelper.drawLabelledWidget(noisiness, labelPosition, label -> ImGui.sliderFloat(label, this.noisiness, 0.0F, 1.0F, "%.2f"));
      changed |= ImGuiHelper.drawLabelledWidget(
         noiseFieldSeed, labelPosition, label -> ImGui.inputTextWithHint(label, AxiomI18n.get("axiom.tool.rock.noise_field_seed.hint"), this.noiseSeed)
      );
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.smoothing_options"));
      ImGuiHelper.Label smoothingStdDev = ImGuiHelper.translateLabel("axiom.tool.rock.smoothing_stddev");
      ImGuiHelper.Label meldStrength = ImGuiHelper.translateLabel("axiom.tool.rock.meld_strength");
      labelPosition = ImGuiHelper.calcLabelPosition(smoothingStdDev, meldStrength);
      changed |= ImGuiHelper.drawLabelledWidget(smoothingStdDev, labelPosition, label -> ImGui.sliderFloat(label, this.smoothingStddev, 0.0F, 5.0F, "%.2f"));
      changed |= ImGuiHelper.drawLabelledWidget(meldStrength, labelPosition, label -> ImGui.sliderFloat(label, this.solidBlockInfluence, 0.0F, 3.0F, "%.2f"));
      ImGuiHelper.separatorWithText("Other");
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.replace_solid_blocks"), this.replaceSolidBlocks)) {
         this.replaceSolidBlocks = !this.replaceSolidBlocks;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.extend_to_ground"), this.extendToGround)) {
         this.extendToGround = !this.extendToGround;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
   }

   @Override
   public String listenForEsc() {
      return !this.painting ? null : AxiomI18n.get("axiom.widget.cancel");
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
      return AxiomI18n.get("axiom.tool.rock");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putInt("NoiseRadius", this.noiseRadius[0]);
      tag.putFloat("Noisiness", this.noisiness[0]);
      tag.putFloat("SmoothingStddev", this.smoothingStddev[0]);
      tag.putString("NoiseSeed", ImGuiHelper.getString(this.noiseSeed));
      tag.putFloat("SolidBlockInfluence", this.solidBlockInfluence[0]);
      tag.putBoolean("ReplaceSolidBlocks", this.replaceSolidBlocks);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.noiseRadius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "NoiseRadius", 5);
      this.noisiness[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Noisiness", 0.5F);
      this.smoothingStddev[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SmoothingStddev", 2.0F);
      this.noiseSeed.set(VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "NoiseSeed", ""), false);
      this.solidBlockInfluence[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SolidBlockInfluence", 2.0F);
      this.replaceSolidBlocks = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "ReplaceSolidBlocks", false);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue906';
   }

   @Override
   public String keybindId() {
      return "rock";
   }

   @Override
   public int defaultKeybind() {
      return 72;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_ROCK, AxiomPermission.BUILD_SECTION);
   }
}
