package com.moulberry.axiom.tools.weld;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.ConcurrentLevelPredicateSet;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.GaussianBlurTable;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class WeldTool implements Tool {
   private boolean usingTool = false;
   private final Position2FloatMap weights = new Position2FloatMap();
   protected GaussianBlurTable blurTable = null;
   protected ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   protected final ChunkedBooleanRegion removeRegion = new ChunkedBooleanRegion();
   protected ConcurrentLevelPredicateSet blocksMotionPredicateSet = null;
   protected final List<Future<Position2FloatMap>> futures = new ArrayList<>();
   private AsyncToolPathProvider pathProvider = null;
   private PositionSet inputSet = new PositionSet();
   private final BrushWidget brushWidget = new BrushWidget();
   private final float[] blurStddev = new float[]{2.0F};
   protected final float[] threshold = new float[]{0.1F};
   private final PresetWidget presetWidget = new PresetWidget(this, "weld");
   private boolean replaceSolidBlocks = false;
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();

   @Override
   public void reset() {
      if (this.usingTool) {
         ChunkRenderOverrider.release("Weld Tool");
         this.usingTool = false;
      }

      for (Future<?> future : this.futures) {
         future.cancel(true);
      }

      this.futures.clear();
      this.blurTable = null;
      this.weights.clear();
      this.chunkedBlockRegion = null;
      this.removeRegion.clear();
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }

      this.blocksMotionPredicateSet = null;
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.reset();
            this.blurTable = new GaussianBlurTable(this.blurStddev[0], 0.01F);
            if (this.blurTable.isSingleValued()) {
               this.blurTable = null;
               return UserAction.ActionResult.USED_STOP;
            }

            this.usingTool = true;
            ChunkRenderOverrider.acquire("Weld Tool");
            this.chunkedBlockRegion = new ChunkedBlockRegion();
            this.blocksMotionPredicateSet = new ConcurrentLevelPredicateSet(Minecraft.getInstance().level, BlockStateBase::blocksMotion);
            this.pathProvider = new AsyncToolPathProvider(new AsyncToolPatherUnique(this.brushWidget.getBrushShape(), (x, y, z) -> this.inputSet.add(x, y, z)));
            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
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
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 1);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         this.addInputsToTool();
         float threshold = this.threshold[0];
         BlockState block = Tool.getActiveBlock();

         for (Future<Position2FloatMap> future : this.futures) {
            try {
               Position2FloatMap map = future.get();
               if (map != null) {
                  map.forEachEntry((x, y, z, v) -> {
                     float newValue = this.weights.add(x, y, z, v);
                     if (newValue >= threshold && newValue - v < threshold) {
                        this.chunkedBlockRegion.addBlock(x, y, z, block);
                     }
                  });
               }
            } catch (InterruptedException | ExecutionException var8) {
               throw new RuntimeException(var8);
            }
         }

         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.welded", countString);
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            this.reset();
            return;
         }

         Selection.render(rc, 4);
         if (this.futures.size() > 0) {
            Iterator<Future<Position2FloatMap>> iterator = this.futures.iterator();
            float threshold = this.threshold[0];
            BlockState block = Tool.getActiveBlock();

            while (iterator.hasNext()) {
               Future<Position2FloatMap> future = iterator.next();
               if (future.isDone()) {
                  try {
                     Position2FloatMap map = future.get();
                     if (map != null) {
                        if (!block.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                           map.forEachEntry((x, y, z, v) -> {
                              float newValue = this.weights.add(x, y, z, v);
                              if (newValue >= threshold && newValue - v < threshold) {
                                 this.chunkedBlockRegion.addBlock(x, y, z, block);
                                 ChunkRenderOverrider.setBlock(x, y, z, AIR);
                                 if (block == AIR) {
                                    this.removeRegion.add(x, y, z);
                                 }
                              }
                           });
                        } else {
                           map.forEachEntry((x, y, z, v) -> {
                              float newValue = this.weights.add(x, y, z, v);
                              if (newValue >= threshold && newValue - v < threshold) {
                                 this.chunkedBlockRegion.addBlock(x, y, z, block);
                              }
                           });
                        }
                     }
                  } catch (InterruptedException | ExecutionException var9) {
                     throw new RuntimeException(var9);
                  }

                  iterator.remove();
               }
            }
         }

         this.updateTool();
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
         this.removeRegion.render(rc, Vec3.ZERO, 8);
      }
   }

   private void updateTool() {
      this.pathProvider.update();
      this.addInputsToTool();
   }

   private void addInputsToTool() {
      if (!this.inputSet.isEmpty()) {
         PositionSet input = this.inputSet;
         this.inputSet = new PositionSet();
         Future<Position2FloatMap> future = Tool.sharedPoolThreadExecutor
            .submit(() -> this.updateToolTask(input, new ConcurrentLevelPredicateSet(this.blocksMotionPredicateSet), this.blurTable));
         this.futures.add(future);
      }
   }

   protected Position2FloatMap updateToolTask(PositionSet input, ConcurrentLevelPredicateSet blocksMotionPredicateSet, GaussianBlurTable blurTable) {
      float[] table = blurTable.getTable();
      int radius = table.length / 2;
      if (radius == 0) {
         throw new FaultyImplementationError();
      } else {
         MaskElement sourceMaskElement = MaskManager.getSourceMask();
         MaskElement destMaskElement = MaskManager.getDestMask();
         MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
         Position2FloatMap smoothedX = new Position2FloatMap();
         input.forEach((x, y, z) -> {
            if (sourceMaskElement.test(maskContext.reset(), x, y, z) && blocksMotionPredicateSet.contains(x, y, z)) {
               for (int i = -radius; i <= radius; i++) {
                  smoothedX.add(x + i, y, z, table[i + radius]);
               }
            }
         });
         Position2FloatMap smoothedZ = new Position2FloatMap();
         smoothedX.forEachEntry((x, y, z, v) -> {
            for (int i = -radius; i <= radius; i++) {
               smoothedZ.add(x, y, z + i, v * table[i + radius]);
            }
         });
         Position2FloatMap output = new Position2FloatMap();
         smoothedZ.forEachEntry((x, y, z, v) -> {
            for (int i = -radius; i <= radius; i++) {
               if (destMaskElement.test(maskContext.reset(), x, y + i, z) && (this.replaceSolidBlocks || !blocksMotionPredicateSet.contains(x, y + i, z))) {
                  output.add(x, y + i, z, v * table[i + radius]);
               }
            }
         });
         return output;
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.smoothing_options"));
      changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.weld.strength"), this.blurStddev, 0.75F, 10.0F, "%.2f", 32);
      changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.weld.threshold"), this.threshold, 0.01F, 1.0F, "%.2f", 32);
      ImGuiHelper.separatorWithText("Other");
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.replace_solid_blocks"), this.replaceSolidBlocks)) {
         this.replaceSolidBlocks = !this.replaceSolidBlocks;
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
      return AxiomI18n.get("axiom.tool.weld");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putFloat("SmoothStrength", this.blurStddev[0]);
      tag.putFloat("SmoothThreshold", this.threshold[0]);
      tag.putBoolean("ReplaceSolidBlocks", this.replaceSolidBlocks);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.blurStddev[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SmoothStrength", 2.0F);
      this.threshold[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SmoothThreshold", 0.1F);
      this.replaceSolidBlocks = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "ReplaceSolidBlocks", false);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue90b';
   }

   @Override
   public String keybindId() {
      return "weld";
   }

   @Override
   public int defaultKeybind() {
      return 74;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_WELD, AxiomPermission.BUILD_SECTION);
   }
}
