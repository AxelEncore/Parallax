package com.moulberry.axiom.tools.smooth;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.collections.list.IntrusiveLinkedList;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherCombined;
import com.moulberry.axiom.pather.async.AsyncToolPatherMinSDF;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.GaussianBlurTable;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SmoothTool implements Tool {
   private final ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
   private final Position2ObjectMap<WeightedPosition> positionMap = new Position2ObjectMap<>(k -> new WeightedPosition[4096]);
   private final IntrusiveLinkedList<WeightedPosition> sortedPositions = new IntrusiveLinkedList<>();
   private final IntrusiveLinkedList<WeightedPosition> newSortedPositions = new IntrusiveLinkedList<>();
   private final PositionSet newlyAddedPositions = new PositionSet();
   private int oldSolidCount = 0;
   private int blockCount = 0;
   private Position2FloatMap[] weights = null;
   private GaussianBlurTable[] blurTables = null;
   private ClosestBlockMap closestBlockMapSolid = null;
   private ClosestBlockMap closestBlockMapReplaceable = null;
   private AsyncToolPathProvider pathProvider = null;
   private boolean usingTool = false;
   private final BrushWidget brushWidget = new BrushWidget();
   private final float[] blurStddev = new float[]{2.0F};
   private final int[] blockRatio = new int[]{100};
   private int meltStableGrowMode = 1;
   private boolean fixEdges = true;
   private final PresetWidget presetWidget = new PresetWidget(this, "smooth");

   @Override
   public void reset() {
      if (this.usingTool) {
         this.usingTool = false;
         ChunkRenderOverrider.release("smooth_tool");
      }

      this.blurTables = null;
      this.oldSolidCount = 0;
      this.blockCount = 0;
      this.blockRegion.clear();
      this.weights = null;
      this.positionMap.clear();
      this.newSortedPositions.clear();
      this.sortedPositions.clear();
      this.newlyAddedPositions.clear();
      this.closestBlockMapSolid = null;
      this.closestBlockMapReplaceable = null;
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
            RayCaster.RaycastResult raycast = Tool.raycastBlock();
            if (raycast == null) {
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.blockRatio[0] <= 0) {
               return UserAction.ActionResult.USED_STOP;
            }

            int blurTableCount = this.fixEdges && !(this.blurStddev[0] <= 1.5) ? (int)Math.ceil((this.blurStddev[0] - 1.0F) / 3.0F) + 1 : 1;
            if (blurTableCount <= 0) {
               return UserAction.ActionResult.USED_STOP;
            }

            float blurStddevDelta = 0.0F;
            if (blurTableCount > 1) {
               blurStddevDelta = (this.blurStddev[0] - 1.0F) / (blurTableCount - 1);
            }

            this.blurTables = new GaussianBlurTable[blurTableCount];

            for (int i = 0; i < blurTableCount; i++) {
               this.blurTables[i] = new GaussianBlurTable(this.blurStddev[0] - blurStddevDelta * i, 0.01F);
            }

            if (this.blurTables[0].isSingleValued()) {
               this.blurTables = null;
               return UserAction.ActionResult.USED_STOP;
            }

            this.weights = new Position2FloatMap[blurTableCount];

            for (int i = 0; i < blurTableCount; i++) {
               this.weights[i] = new Position2FloatMap();
            }

            if (!this.usingTool) {
               this.usingTool = true;
               ChunkRenderOverrider.acquire("smooth_tool");
            }

            this.oldSolidCount = 0;
            this.blockCount = 0;
            this.blockRegion.clear();
            this.positionMap.clear();
            this.sortedPositions.clear();
            this.newSortedPositions.clear();
            this.closestBlockMapSolid = new ClosestBlockMap(state -> !state.canBeReplaced(), Blocks.STONE.defaultBlockState());
            this.closestBlockMapReplaceable = new ClosestBlockMap(
               blockState -> blockState.isAir() || blockState.getBlock() == Blocks.WATER || blockState.getBlock() == Blocks.LAVA,
               Blocks.AIR.defaultBlockState()
            );
            int additionalWeightRadius = this.blurTables[0].getTable().length / 2;
            BrushShape expanded = this.brushWidget.createBrushShapeWithAdditional(additionalWeightRadius);
            BrushShape brushShape = this.brushWidget.getBrushShape();
            if (this.blurTables.length == 1) {
               this.pathProvider = new AsyncToolPathProvider(
                  new AsyncToolPatherCombined(this.createSinglePatherForWeight(raycast.blockPos(), expanded), this.createSinglePatherForUpdate(brushShape))
               );
            } else {
               this.pathProvider = new AsyncToolPathProvider(
                  new AsyncToolPatherCombined(this.createMultiPatherForWeight(raycast.blockPos(), expanded), this.createMultiSmoothPather(brushShape))
               );
            }
            break;
         case ESCAPE:
            if (this.usingTool) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
      }

      return UserAction.ActionResult.NOT_HANDLED;
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
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         this.updateFromNewSortedPositions();
         String countString = NumberFormat.getInstance().format((long)this.blockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.smoothed", countString);
         RegionHelper.pushBlockRegionChange(this.blockRegion, historyDescription);
         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         Selection.render(rc, 4);
         this.pathProvider.update();
         this.updateFromNewSortedPositions();
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.blockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
      }
   }

   private AsyncToolPather createSinglePatherForWeight(BlockPos initialPosition, BrushShape weightShape) {
      final ClientLevel level = Minecraft.getInstance().level;
      final MutableBlockPos mutableBlockPos = new MutableBlockPos();
      final int initialX = initialPosition.getX();
      final int initialY = initialPosition.getY();
      final int initialZ = initialPosition.getZ();
      final Position2FloatMap smoothedX = new Position2FloatMap();
      final Position2FloatMap smoothedZ = new Position2FloatMap();
      final Position2FloatMap weights = this.weights[0];
      final float[] table = this.blurTables[0].getTable();
      final int blurTableRadius = table.length / 2;
      return new AsyncToolPatherUnique(weightShape, null) {
         @Override
         public void update() {
            smoothedX.clear();
            smoothedZ.clear();

            while (true) {
               long[] positions = this.outputPositions.poll();
               if (positions == null) {
                  smoothedX.forEachEntry((xx, yx, zx, v) -> {
                     for (int ix = -blurTableRadius; ix <= blurTableRadius; ix++) {
                        smoothedZ.add(xx, yx, zx + ix, v * table[ix + blurTableRadius]);
                     }
                  });
                  smoothedZ.forEachEntry((xx, yx, zx, v) -> {
                     for (int ix = -blurTableRadius; ix <= blurTableRadius; ix++) {
                        weights.add(xx, yx + ix, zx, v * table[ix + blurTableRadius]);
                     }
                  });
                  return;
               }

               for (long position : positions) {
                  int x = BlockPos.getX(position);
                  int y = BlockPos.getY(position);
                  int z = BlockPos.getZ(position);
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                  if (!blockState.canBeReplaced()) {
                     int dx = initialX - x;
                     int dy = initialY - y;
                     int dz = initialZ - z;
                     int distanceToInitialSq = dx * dx + dy * dy + dz * dz;
                     float factor = 1.0F - (float)Math.log(distanceToInitialSq + 10) * 0.001F;

                     for (int i = -blurTableRadius; i <= blurTableRadius; i++) {
                        smoothedX.add(x + i, y, z, factor * table[i + blurTableRadius]);
                     }
                  }
               }
            }
         }
      };
   }

   private AsyncToolPather createMultiPatherForWeight(BlockPos initialPosition, BrushShape weightShape) {
      final ClientLevel level = Minecraft.getInstance().level;
      final MutableBlockPos mutableBlockPos = new MutableBlockPos();
      final int initialX = initialPosition.getX();
      final int initialY = initialPosition.getY();
      final int initialZ = initialPosition.getZ();
      final Position2FloatMap[] smoothedXMaps = new Position2FloatMap[this.blurTables.length];
      final Position2FloatMap[] smoothedZMaps = new Position2FloatMap[this.blurTables.length];

      for (int i = 0; i < this.blurTables.length; i++) {
         smoothedXMaps[i] = new Position2FloatMap();
         smoothedZMaps[i] = new Position2FloatMap();
      }

      return new AsyncToolPatherUnique(weightShape, null) {
         @Override
         public void update() {
            for (Position2FloatMap map : smoothedXMaps) {
               map.clear();
            }

            for (Position2FloatMap map : smoothedZMaps) {
               map.clear();
            }

            while (true) {
               long[] positions = this.outputPositions.poll();
               if (positions == null) {
                  for (int j = 0; j < SmoothTool.this.blurTables.length; j++) {
                     float[] table = SmoothTool.this.blurTables[j].getTable();
                     Position2FloatMap weightOutput = SmoothTool.this.weights[j];
                     Position2FloatMap smoothedX = smoothedXMaps[j];
                     int radius = table.length / 2;
                     Position2FloatMap smoothedZ = new Position2FloatMap();
                     smoothedX.forEachEntry((xx, yx, zx, v) -> {
                        for (int ix = -radius; ix <= radius; ix++) {
                           smoothedZ.add(xx, yx, zx + ix, v * table[ix + radius]);
                        }
                     });
                     smoothedZ.forEachEntry((xx, yx, zx, v) -> {
                        for (int ix = -radius; ix <= radius; ix++) {
                           weightOutput.add(xx, yx + ix, zx, v * table[ix + radius]);
                        }
                     });
                  }

                  return;
               }

               for (long position : positions) {
                  int x = BlockPos.getX(position);
                  int y = BlockPos.getY(position);
                  int z = BlockPos.getZ(position);
                  BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                  if (!blockState.canBeReplaced()) {
                     int dx = initialX - x;
                     int dy = initialY - y;
                     int dz = initialZ - z;
                     int distanceToInitialSq = dx * dx + dy * dy + dz * dz;
                     float factor = 1.0F - (float)Math.log(distanceToInitialSq + 10) * 0.001F;

                     for (int i = 0; i < SmoothTool.this.blurTables.length; i++) {
                        float[] anotherTable = SmoothTool.this.blurTables[i].getTable();
                        Position2FloatMap map = smoothedXMaps[i];
                        int anotherRadius = anotherTable.length / 2;

                        for (int j = -anotherRadius; j <= anotherRadius; j++) {
                           map.add(x + j, y, z, factor * anotherTable[j + anotherRadius]);
                        }
                     }
                  }
               }
            }
         }
      };
   }

   private AsyncToolPather createSinglePatherForUpdate(BrushShape smoothShape) {
      ClientLevel level = Minecraft.getInstance().level;
      MaskElement sourceMask = MaskManager.getSourceMask();
      MaskContext maskContext = new MaskContext(level);
      Position2FloatMap weights = this.weights[0];
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      return new AsyncToolPatherUnique(smoothShape, (x, y, z) -> {
         BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
         if (blockState.getBlock() != Blocks.VOID_AIR) {
            if (blockState.isAir() || sourceMask.test(maskContext.reset(), x, y, z)) {
               float weight = weights.get(x, y, z);
               this.newSortedPositions.add(new WeightedPosition(x, y, z, weight));
               if (!blockState.canBeReplaced()) {
                  this.blockCount++;
               }
            }
         }
      });
   }

   private AsyncToolPather createMultiSmoothPather(BrushShape smoothShape) {
      final ClientLevel level = Minecraft.getInstance().level;
      final MaskElement sourceMask = MaskManager.getSourceMask();
      final MaskContext maskContext = new MaskContext(level);
      final MutableBlockPos mutableBlockPos = new MutableBlockPos();
      final PositionSet finished = new PositionSet();
      final float blurStddev = this.blurStddev[0];
      final float blurStddevDeltaInv = (this.blurTables.length - 1) / (this.blurStddev[0] - 1.0F);
      return new AsyncToolPatherMinSDF(smoothShape, null) {
         @Override
         public void update() {
            while (true) {
               int[] positions = this.outputData.poll();
               if (positions == null) {
                  return;
               }

               for (int i = 0; i < positions.length; i += 4) {
                  int x = positions[i];
                  int y = positions[i + 1];
                  int z = positions[i + 2];
                  if (!finished.contains(x, y, z)) {
                     BlockState blockState = level.getBlockState(mutableBlockPos.set(x, y, z));
                     if (blockState.getBlock() == Blocks.VOID_AIR) {
                        finished.add(x, y, z);
                     } else if (!blockState.isAir() && !sourceMask.test(maskContext.reset(), x, y, z)) {
                        finished.add(x, y, z);
                     } else {
                        float distance = Float.intBitsToFloat(positions[i + 3]);
                        float falloff = 0.0F;
                        if (distance > 0.6) {
                           falloff = (distance - 0.6F) / 0.4F;
                        } else {
                           finished.add(x, y, z);
                        }

                        if (BuildConfig.DEBUG && (falloff < 0.0F || falloff > 1.0F)) {
                           throw new FaultyImplementationError();
                        }

                        float weight;
                        if (falloff < 0.01) {
                           weight = SmoothTool.this.weights[0].get(x, y, z);
                        } else {
                           float desiredStddev = blurStddev * (1.0F - falloff) + falloff;
                           float deltas = (blurStddev - desiredStddev) * blurStddevDeltaInv;
                           Position2FloatMap firstTable = SmoothTool.this.weights[(int)Math.floor(deltas)];
                           Position2FloatMap secondTable = SmoothTool.this.weights[(int)Math.ceil(deltas)];
                           float firstWeight = firstTable.get(x, y, z);
                           float secondWeight = secondTable.get(x, y, z);
                           float fpart = deltas - (float)Math.floor(deltas);
                           weight = firstWeight * (1.0F - fpart) + secondWeight * fpart;
                        }

                        WeightedPosition old = SmoothTool.this.positionMap.get(x, y, z);
                        if (old != null) {
                           old.weight = weight;
                           if (!SmoothTool.this.newlyAddedPositions.contains(x, y, z)) {
                              SmoothTool.this.sortedPositions.remove(old);
                              if (old.solid) {
                                 SmoothTool.this.oldSolidCount--;
                              }

                              SmoothTool.this.newSortedPositions.add(old);
                              SmoothTool.this.newlyAddedPositions.add(x, y, z);
                           }
                        } else {
                           WeightedPosition weightedPosition = new WeightedPosition(x, y, z, weight);
                           SmoothTool.this.newSortedPositions.add(weightedPosition);
                           SmoothTool.this.newlyAddedPositions.add(x, y, z);
                           SmoothTool.this.positionMap.put(x, y, z, weightedPosition);
                           if (!blockState.canBeReplaced()) {
                              SmoothTool.this.blockCount++;
                           }
                        }
                     }
                  }
               }
            }
         }
      };
   }

   private void updateFromNewSortedPositions() {
      if (this.newSortedPositions.isEmpty()) {
         this.newlyAddedPositions.clear();
      } else {
         this.newSortedPositions.sort(Comparator.comparingDouble(w -> -w.weight));
         int solidCount = this.calculateBlockCount(this.blockRatio[0] / 100.0F, this.blockCount, this.sortedPositions.size() + this.newSortedPositions.size());
         Consumer<WeightedPosition> headSplit = e -> {
            BlockState block = this.closestBlockMapSolid.get(e.x, e.y, e.z);
            this.blockRegion.addBlock(e.x, e.y, e.z, block);
            if (block.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == e.renderOverrideWithAir) {
               if (e.renderOverrideWithAir) {
                  ChunkRenderOverrider.revertBlock(e.x, e.y, e.z);
                  e.renderOverrideWithAir = false;
               } else {
                  ChunkRenderOverrider.setBlock(e.x, e.y, e.z, Blocks.AIR.defaultBlockState());
                  e.renderOverrideWithAir = true;
               }
            }

            e.solid = true;
         };
         Consumer<WeightedPosition> tailSplit = e -> {
            BlockState block = this.closestBlockMapReplaceable.get(e.x, e.y, e.z);
            this.blockRegion.addBlock(e.x, e.y, e.z, block);
            if (!e.renderOverrideWithAir) {
               ChunkRenderOverrider.setBlock(e.x, e.y, e.z, Blocks.AIR.defaultBlockState());
               e.renderOverrideWithAir = true;
            }

            e.solid = false;
         };
         if (this.meltStableGrowMode == 0) {
            headSplit = e -> e.solid = true;
         }

         if (this.meltStableGrowMode == 2) {
            tailSplit = e -> e.solid = false;
         }

         this.sortedPositions
            .mergeSplitPosition(this.newSortedPositions, Comparator.comparingDouble(w -> -w.weight), this.oldSolidCount, solidCount, headSplit, tailSplit);
         this.oldSolidCount = solidCount;
         this.newSortedPositions.clear();
         this.newlyAddedPositions.clear();
      }
   }

   private int calculateBlockCount(float ratio, int blockCount, int patherCount) {
      if (ratio <= 1.0F) {
         return (int)Math.ceil(blockCount * ratio);
      } else {
         float factor = ratio - 1.0F;
         if (factor > 1.0F) {
            factor = 1.0F;
         }

         return (int)Math.floor(blockCount * (1.0F - factor) + patherCount * factor);
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.smoothing_options"));
      changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.smooth.strength"), this.blurStddev, 0.75F, 10.0F, "%.2f", 32);
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.smooth.modifiers"));
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.smooth.block_ratio"), this.blockRatio, 0, 200, "%d%%");
      int newMeltStableGrowMode = -1;
      if (this.meltStableGrowMode == 0) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.smooth.mode_melt"))) {
         newMeltStableGrowMode = 0;
      }

      if (this.meltStableGrowMode == 0) {
         ImGui.endDisabled();
      }

      ImGui.sameLine();
      if (this.meltStableGrowMode == 1) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.smooth.mode_stable"))) {
         newMeltStableGrowMode = 1;
      }

      if (this.meltStableGrowMode == 1) {
         ImGui.endDisabled();
      }

      ImGui.sameLine();
      if (this.meltStableGrowMode == 2) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.smooth.mode_grow"))) {
         newMeltStableGrowMode = 2;
      }

      if (this.meltStableGrowMode == 2) {
         ImGui.endDisabled();
      }

      if (newMeltStableGrowMode != -1) {
         this.meltStableGrowMode = newMeltStableGrowMode;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.advanced_options"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.smooth.fix_edges"), this.fixEdges)) {
         this.fixEdges = !this.fixEdges;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
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
   public String listenForEsc() {
      return !this.usingTool ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.smooth");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putFloat("SmoothStrength", this.blurStddev[0]);
      tag.putFloat("BlockRatio", this.blockRatio[0] / 100.0F);
      tag.putByte("MeltStableGrowMode", (byte)this.meltStableGrowMode);
      tag.putBoolean("FixEdges", this.fixEdges);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.blurStddev[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SmoothStrength", 2.0F);
      this.blockRatio[0] = Math.round(VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "BlockRatio", 1.0F) * 100.0F);
      this.meltStableGrowMode = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "MeltStableGrowMode", 1);
      this.fixEdges = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "FixEdges", true);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue905';
   }

   @Override
   public String keybindId() {
      return "smooth";
   }

   @Override
   public int defaultKeybind() {
      return 85;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_SMOOTH, AxiomPermission.BUILD_SECTION);
   }
}
