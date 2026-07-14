package com.moulberry.axiom.tools.stamp;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.blueprint.Blueprint;
import com.moulberry.axiom.blueprint.BlueprintHeader;
import com.moulberry.axiom.blueprint.BlueprintIo;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2dSet;
import com.moulberry.axiom.collections.Position2dToIntMap;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.GenericRadiusAdjustment;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.editor.windows.clipboard.BlueprintBrowserWindow;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.noise.WhiteNoise;
import com.moulberry.axiom.noise.generic.PositionalRandom;
import com.moulberry.axiom.pather.ToolPatherSurface;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.CompressedBlockEntity;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiomclientapi.pathers.BallShape;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class StampTool implements Tool {
   private final List<StampPlacement> brushPlacements = new ArrayList<>();
   private final List<IntermediatePlacement> intermediatePlacements = new ArrayList<>();
   private final ChunkedBooleanRegion previewFailingDestinationMask = new ChunkedBooleanRegion();
   private final ChunkedBooleanRegion previewFailingDestinationMaskForSinglePlacement = new ChunkedBooleanRegion();
   private final Map<ChunkedBlockRegion, List<Vec3>> renderMap = new IdentityHashMap<>();
   private StampPlacement cachedSinglePlacementPreview = null;
   private BlockPos cachedSinglePlacementPreviewLocation = null;
   private final ChunkedBooleanRegion previewSphere = new ChunkedBooleanRegion();
   private final ChunkedBooleanRegion stampBlocksPreview = new ChunkedBooleanRegion();
   private int oldRadius = -1;
   private int oldBrushShape = -1;
   private boolean usingTool = false;
   private ToolPatherSurface toolPather = null;
   private Vec3 lastPlayerPos = Vec3.ZERO;
   private boolean updateRenderMap = false;
   private final Position2dToIntMap heightMap = new Position2dToIntMap(Integer.MIN_VALUE);
   private Future<StampTool.AsyncComputationResult> asyncComputationFuture = null;
   private boolean heightMapDirty = false;
   private boolean selectedGizmo = false;
   private final int[] brushShape = new int[]{0};
   private final int[] radius = new int[]{4};
   private final GenericRadiusAdjustment radiusAdjustment = new GenericRadiusAdjustment(64);
   private final int[] placeMode = new int[]{0};
   private final int[] stampDirection = new int[]{0};
   private final float[] baseChance = new float[]{1.0F};
   private final int[] minSpacingPercentage = new int[]{100};
   private final int[] maskMode = new int[]{0};
   private boolean randomYaw = true;
   private boolean randomXZFlip = true;
   private boolean keepExisting = false;
   private boolean extendFoundationToGround = false;
   private boolean applyGravity = false;
   private final List<ClipboardObject> blueprints = new ArrayList<>();
   private final List<TransformedBlockRegions> transformedBlueprints = new ArrayList<>();
   private final List<StampBlueprintConfig> blueprintSettings = new ArrayList<>();
   private final Map<String, BlockPos> savedOffsets = new HashMap<>();
   private Direction currentDirection = Direction.UP;
   private final PresetWidget presetWidget = new PresetWidget(this, "stamp");
   private boolean settingsChanged = false;

   @Override
   public void reset() {
      this.partialReset();
      this.renderMap.clear();
      this.previewFailingDestinationMask.clear();
      this.intermediatePlacements.clear();
      this.selectedGizmo = false;
   }

   @Override
   public void toolDeselected() {
      BlueprintBrowserWindow.resetCallback();
   }

   private void partialReset() {
      this.usingTool = false;
      this.brushPlacements.clear();
      this.stampBlocksPreview.clear();
      this.cachedSinglePlacementPreview = null;
      this.previewFailingDestinationMaskForSinglePlacement.clear();
      if (this.asyncComputationFuture != null) {
         this.asyncComputationFuture.cancel(true);
         this.asyncComputationFuture = null;
      }

      this.heightMap.clear();
      this.heightMapDirty = false;
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.partialReset();
            switch (this.stampDirection[0]) {
               case 0:
                  this.currentDirection = Direction.UP;
                  break;
               case 1:
                  this.currentDirection = Direction.DOWN;
                  break;
               case 2:
                  this.currentDirection = Direction.NORTH;
                  break;
               case 3:
                  this.currentDirection = Direction.EAST;
                  break;
               case 4:
                  this.currentDirection = Direction.SOUTH;
                  break;
               case 5:
                  this.currentDirection = Direction.WEST;
            }

            if (!this.blueprints.isEmpty()) {
               MaskElement mask;
               if (this.maskMode[0] != 1) {
                  mask = MaskManager.getSourceMask();
               } else {
                  mask = new ConstantMaskElement(true);
               }

               if (this.isSinglePlacement()) {
                  RayCaster.RaycastResult raycastResult = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
                  if (raycastResult != null) {
                     BlockPos hovered = raycastResult.blockPos();
                     if (mask.test(new MaskContext(Minecraft.getInstance().level), hovered.getX(), hovered.getY(), hovered.getZ())) {
                        BlockPos offset = hovered.relative(raycastResult.direction());
                        StampPlacement placement = this.createSinglePlacement(offset.getX(), offset.getY(), offset.getZ(), raycastResult.direction());
                        if (placement != null) {
                           if (this.isImmediatePlacement()) {
                              this.brushPlacements.add(placement);
                              this.doPlacement();
                           } else {
                              Gizmo gizmo = new Gizmo(new BlockPos(placement.baseX(), placement.baseY(), placement.baseZ()));
                              gizmo.enableAxes = false;
                              gizmo.enableRotation = true;
                              gizmo.yAxisRotationOnly = true;
                              this.intermediatePlacements.add(new IntermediatePlacement(gizmo, placement));
                              this.partialReset();
                              this.updateClosestGizmos(Minecraft.getInstance().player.getEyePosition());
                              this.updateRenderMap = true;
                           }
                        }
                     }
                  }
               } else {
                  this.usingTool = true;
                  this.toolPather = new ToolPatherSurface(this.radius[0], BallShape.getByIndex(this.brushShape[0]), mask, this.currentDirection);
               }
            }

            for (IntermediatePlacement intermediatexxx : this.intermediatePlacements) {
               intermediatexxx.gizmo.enableAxes = false;
            }

            this.selectedGizmo = false;
            return UserAction.ActionResult.USED_STOP;
         case LEFT_MOUSE:
            for (IntermediatePlacement intermediate : this.intermediatePlacements) {
               if (intermediate.gizmo.enableAxes && intermediate.gizmo.leftClick()) {
                  this.selectedGizmo = true;
                  return UserAction.ActionResult.USED_STOP;
               }
            }

            Gizmo disableExcept = null;

            for (IntermediatePlacement intermediatex : this.intermediatePlacements) {
               if (!intermediatex.gizmo.enableAxes && intermediatex.gizmo.leftClick()) {
                  this.selectedGizmo = true;
                  disableExcept = intermediatex.gizmo;
                  break;
               }
            }

            if (disableExcept != null) {
               for (IntermediatePlacement intermediatexx : this.intermediatePlacements) {
                  if (intermediatexx.gizmo != disableExcept) {
                     intermediatexx.gizmo.enableAxes = false;
                  }
               }

               return UserAction.ActionResult.USED_STOP;
            }

            if (!EditorUI.isCtrlOrCmdDown()) {
               for (IntermediatePlacement intermediatexxx : this.intermediatePlacements) {
                  intermediatexxx.gizmo.enableAxes = false;
               }

               this.selectedGizmo = false;
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case ENTER:
            if (!this.brushPlacements.isEmpty() || !this.intermediatePlacements.isEmpty()) {
               this.doPlacement();
            }
            break;
         case REDO:
            if (this.usingTool || !this.intermediatePlacements.isEmpty()) {
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case ESCAPE:
            boolean disabled = false;
            this.updateRenderMap = true;

            for (IntermediatePlacement intermediatePlacement : this.intermediatePlacements) {
               if (intermediatePlacement.gizmo.enableAxes) {
                  intermediatePlacement.gizmo.enableAxes = false;
                  disabled = true;
               }
            }

            this.selectedGizmo = false;
            if (disabled) {
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.usingTool) {
               this.partialReset();
               this.updateRenderMap();
               return UserAction.ActionResult.USED_STOP;
            }

            if (!this.intermediatePlacements.isEmpty()) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case DELETE:
         case UNDO:
            this.updateRenderMap = true;
            boolean removed = false;
            int i = 0;

            for (; i < this.intermediatePlacements.size(); i++) {
               if (this.intermediatePlacements.get(i).gizmo.enableAxes) {
                  this.intermediatePlacements.remove(i);
                  i--;
                  removed = true;
               }
            }

            this.selectedGizmo = false;
            if (removed) {
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.usingTool || !this.intermediatePlacements.isEmpty()) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   private void doPlacement() {
      ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
      Long2ObjectMap<CompressedBlockEntity> blockEntities = new Long2ObjectOpenHashMap();
      MaskElement mask;
      if (this.maskMode[0] != 0) {
         mask = MaskManager.getDestMask();
      } else {
         mask = new ConstantMaskElement(true);
      }

      MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
      if (this.extendFoundationToGround) {
         for (StampPlacement placement : this.brushPlacements) {
            placement.pasteIntoExtendToGround(chunkedBlockRegion, blockEntities, maskContext, mask);
         }

         for (IntermediatePlacement intermediate : this.intermediatePlacements) {
            intermediate.placement.pasteIntoExtendToGround(chunkedBlockRegion, blockEntities, maskContext, mask);
         }
      } else if (this.applyGravity) {
         for (StampPlacement placement : this.brushPlacements) {
            placement.pasteIntoApplyGravity(chunkedBlockRegion, blockEntities, Minecraft.getInstance().level, maskContext, mask);
         }

         for (IntermediatePlacement intermediate : this.intermediatePlacements) {
            intermediate.placement.pasteIntoApplyGravity(chunkedBlockRegion, blockEntities, Minecraft.getInstance().level, maskContext, mask);
         }
      } else {
         for (StampPlacement placement : this.brushPlacements) {
            placement.pasteInto(chunkedBlockRegion, blockEntities, maskContext, mask);
         }

         for (IntermediatePlacement intermediate : this.intermediatePlacements) {
            intermediate.placement.pasteInto(chunkedBlockRegion, blockEntities, maskContext, mask);
         }
      }

      int modifiers = this.keepExisting ? HistoryEntry.MODIFIER_KEEP_EXISTING : 0;
      String countString = NumberFormat.getInstance().format((long)chunkedBlockRegion.count());
      String historyDescription = AxiomI18n.get("axiom.history_description.placed", countString);
      RegionHelper.pushBlockRegionChange(chunkedBlockRegion, blockEntities, historyDescription, modifiers);
      this.reset();
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (this.blueprints.isEmpty() && this.intermediatePlacements.isEmpty()) {
         Selection.render(rc, 4);
      } else {
         Vec3 lookDirection = Tool.getLookDirection();
         boolean isLeftDown = Tool.isMouseDown(0);
         boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
         boolean showGizmo = !isCtrlDown;
         boolean renderGizmos = showGizmo;
         boolean gizmoHovered = false;
         boolean placementMoved = false;
         Vec3 playerPos = Minecraft.getInstance().player.getEyePosition();
         if (playerPos.distanceToSqr(this.lastPlayerPos) > 1.0) {
            this.lastPlayerPos = playerPos;
            this.updateClosestGizmos(playerPos);
         }

         int maxPlacements = Math.min(64, this.intermediatePlacements.size());

         for (int i = 0; i < maxPlacements; i++) {
            IntermediatePlacement intermediate = this.intermediatePlacements.get(i);
            Gizmo gizmo = intermediate.gizmo;
            BlockPos previousTarget = gizmo.getTargetPosition();
            gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
            if (gizmo.enableAxes) {
               gizmo.setAxisDirections(
                  rc.x() > gizmo.getTargetPosition().getX(), rc.y() > gizmo.getTargetPosition().getY(), rc.z() > gizmo.getTargetPosition().getZ()
               );
               gizmoHovered = true;
               this.selectedGizmo = true;
               if (!gizmo.isGrabbed()) {
                  Gizmo.GizmoRotation gizmoRotation = gizmo.popRotation();
                  if (gizmoRotation != null) {
                     int rotations = Math.round((float)Math.toDegrees(gizmoRotation.radians()) / 90.0F);
                     if (rotations != 0) {
                        intermediate.placement = intermediate.placement.rotate(rotations);
                        placementMoved = true;
                     }
                  }
               }
            }

            if (gizmo.isGrabbed()) {
               renderGizmos = true;
            }

            if (gizmo.isHovered()) {
               gizmoHovered = true;
            }

            BlockPos newTarget = gizmo.getTargetPosition();
            if (!newTarget.equals(previousTarget)) {
               intermediate.placement = intermediate.placement.withBasePosition(newTarget.getX(), newTarget.getY(), newTarget.getZ());
               placementMoved = true;
            }
         }

         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         if (!this.usingTool) {
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
            if (result == null) {
               Selection.render(rc, this.intermediatePlacements.isEmpty() ? 7 : 4);
            } else {
               Selection.render(rc, 4);
               if (!gizmoHovered) {
                  if (this.isSinglePlacement()) {
                     MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
                     MaskElement sourceMask = null;
                     if (this.maskMode[0] != 1) {
                        sourceMask = MaskManager.getSourceMask();
                     }

                     BlockPos hovered = result.blockPos();
                     if (sourceMask == null || sourceMask.test(maskContext.reset(), hovered.getX(), hovered.getY(), hovered.getZ())) {
                        BlockPos offset = hovered.relative(result.direction());
                        StampPlacement placement;
                        if (this.cachedSinglePlacementPreview != null && Objects.equals(this.cachedSinglePlacementPreviewLocation, offset)) {
                           placement = this.cachedSinglePlacementPreview;
                        } else {
                           this.previewFailingDestinationMaskForSinglePlacement.clear();
                           placement = this.createSinglePlacement(offset.getX(), offset.getY(), offset.getZ(), result.direction());
                           if (placement != null && this.maskMode[0] != 0) {
                              MaskElement destMask = MaskManager.getDestMask();
                              placement.applyFailingDestinationMaskPreview(this.previewFailingDestinationMaskForSinglePlacement, maskContext, destMask);
                           }

                           this.cachedSinglePlacementPreview = placement;
                           this.cachedSinglePlacementPreviewLocation = offset;
                        }

                        if (placement != null) {
                           Tool.renderRaycastOverlay(rc, result);
                           placement.blockRegion().render(rc, placement.getRenderOffset(), 0.35F, 0.15F - opacity * 0.1F);
                           if (this.maskMode[0] != 0) {
                              this.previewFailingDestinationMaskForSinglePlacement.render(rc, Vec3.ZERO, 8);
                           }
                        }
                     }
                  } else {
                     int radius = this.radius[0];
                     int brushShape = this.brushShape[0];
                     if (this.oldRadius != radius || this.oldBrushShape != brushShape) {
                        this.oldRadius = radius;
                        this.oldBrushShape = brushShape;
                        this.previewSphere.clear();
                        BallShape.getByIndex(brushShape).fillRegion(this.previewSphere, radius);
                     }

                     this.previewSphere.render(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 1);
                  }
               }
            }

            if (placementMoved || this.updateRenderMap) {
               this.updateRenderMap();
            }

            for (Entry<ChunkedBlockRegion, List<Vec3>> entry : this.renderMap.entrySet()) {
               for (Vec3 offsetx : entry.getValue()) {
                  entry.getKey().render(rc, offsetx, 1.0F, 0.15F - opacity * 0.1F);
               }
            }

            if (renderGizmos) {
               maxPlacements = Math.min(64, this.intermediatePlacements.size());

               for (int i = 0; i < maxPlacements; i++) {
                  IntermediatePlacement intermediatex = this.intermediatePlacements.get(i);
                  intermediatex.gizmo.render(rc, isCtrlDown);
               }
            }
         } else if (Tool.cancelUsing()) {
            this.partialReset();
            this.updateRenderMap();
         } else if (!Tool.isMouseDown(1)) {
            if (this.isImmediatePlacement()) {
               this.doPlacement();
            } else {
               for (StampPlacement placementx : this.brushPlacements) {
                  Gizmo gizmox = new Gizmo(new BlockPos(placementx.baseX(), placementx.baseY(), placementx.baseZ()));
                  gizmox.enableAxes = false;
                  gizmox.enableRotation = true;
                  gizmox.yAxisRotationOnly = true;
                  this.intermediatePlacements.add(new IntermediatePlacement(gizmox, placementx));
               }

               this.partialReset();
               this.updateClosestGizmos(Minecraft.getInstance().player.getEyePosition());
            }
         } else {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
               return;
            }

            Selection.render(rc, 4);
            WhiteNoise whiteNoise = new WhiteNoise(428362042);
            int surfaceDirection = this.currentDirection.getAxisDirection() == AxisDirection.NEGATIVE ? -1 : 1;
            AxisCycle axisCycle = AxisCycle.between(this.currentDirection.getAxis(), Axis.Y);
            this.heightMapDirty = this.heightMapDirty | this.toolPather.update((x, y, z) -> {
               this.stampBlocksPreview.add(x, y, z);
               if (this.baseChance[0] > 0.99 || whiteNoise.evaluate(x, z) < this.baseChance[0]) {
                  int a = axisCycle.cycle(x, y, z, Axis.X);
                  int b = axisCycle.cycle(x, y, z, Axis.Y);
                  int c = axisCycle.cycle(x, y, z, Axis.Z);
                  this.heightMap.put(a, c, b + surfaceDirection);
               }
            });
            boolean changed = false;
            if (this.asyncComputationFuture != null) {
               if (this.asyncComputationFuture.isDone()) {
                  try {
                     StampTool.AsyncComputationResult resultx = this.asyncComputationFuture.get();
                     this.brushPlacements.clear();
                     this.brushPlacements.addAll(resultx.newBrushPlacements);
                     this.intermediatePlacements.removeIf(intermediatex -> {
                        StampPlacement placementx = intermediatex.placement;
                        return resultx.spacingBlacklistSet.contains(placementx.baseX(), placementx.baseZ());
                     });
                     changed = true;
                  } catch (ExecutionException | InterruptedException var22) {
                     throw new RuntimeException(var22);
                  }

                  this.asyncComputationFuture = null;
               }
            } else if (this.heightMapDirty) {
               Position2dToIntMap heightMap = this.heightMap.copy();
               int minSpacingPercentage = this.minSpacingPercentage[0];
               List<StampBlueprintConfig> blueprintSettings = new ArrayList<>();

               for (StampBlueprintConfig blueprintSetting : this.blueprintSettings) {
                  blueprintSettings.add(blueprintSetting.copy());
               }

               this.updateTransformedBlueprintList();
               this.asyncComputationFuture = Tool.sharedSingleThreadExecutor
                  .submit(
                     () -> update(
                        heightMap,
                        blueprintSettings,
                        new ArrayList<>(this.transformedBlueprints),
                        minSpacingPercentage,
                        this.randomYaw,
                        this.randomXZFlip,
                        this.currentDirection
                     )
                  );
               this.heightMapDirty = false;
            }

            if (changed || placementMoved || this.updateRenderMap) {
               this.updateRenderMap();
            }

            for (Entry<ChunkedBlockRegion, List<Vec3>> entry : this.renderMap.entrySet()) {
               for (Vec3 offsetx : entry.getValue()) {
                  entry.getKey().render(rc, offsetx, 1.0F, 0.15F - opacity * 0.1F);
               }
            }

            if (this.maskMode[0] != 0) {
               this.previewFailingDestinationMask.render(rc, Vec3.ZERO, 8);
            }

            this.stampBlocksPreview.render(rc, Vec3.ZERO, 1);
            if (renderGizmos) {
               maxPlacements = Math.min(64, this.intermediatePlacements.size());

               for (int i = 0; i < maxPlacements; i++) {
                  IntermediatePlacement intermediatex = this.intermediatePlacements.get(i);
                  intermediatex.gizmo.render(rc, isCtrlDown);
               }
            }
         }
      }
   }

   private boolean isSinglePlacement() {
      return this.placeMode[0] == 2 || this.placeMode[0] == 3;
   }

   private boolean isImmediatePlacement() {
      return this.placeMode[0] == 0 || this.placeMode[0] == 2;
   }

   private void updateTransformedBlueprintList() {
      if (this.transformedBlueprints.size() < this.blueprints.size()) {
         for (int i = this.transformedBlueprints.size(); i < this.blueprints.size(); i++) {
            this.transformedBlueprints.add(new TransformedBlockRegions(this.blueprints.get(i).blockRegion(), this.blueprints.get(i).blockEntities()));
         }
      } else if (this.transformedBlueprints.size() > this.blueprints.size()) {
         int toRemove = this.transformedBlueprints.size() - this.blueprints.size();

         for (int i = 0; i < toRemove; i++) {
            this.transformedBlueprints.remove(this.transformedBlueprints.size() - 1);
         }
      }

      for (int i = 0; i < this.transformedBlueprints.size(); i++) {
         TransformedBlockRegions transformed = this.transformedBlueprints.get(i);
         ChunkedBlockRegion blockRegion = this.blueprints.get(i).blockRegion();
         if (transformed.getBlocks(0, false, false) != blockRegion) {
            this.transformedBlueprints.set(i, new TransformedBlockRegions(blockRegion, this.blueprints.get(i).blockEntities()));
         }
      }
   }

   private void updateClosestGizmos(Vec3 playerPos) {
      this.intermediatePlacements
         .sort(
            Comparator.comparingDouble(intermediate -> intermediate.gizmo.enableAxes ? 0.0 : intermediate.gizmo.getTargetPosition().distToCenterSqr(playerPos))
         );
   }

   private void updateRenderMap() {
      this.updateRenderMap = false;
      this.renderMap.clear();
      this.previewFailingDestinationMask.clear();
      MaskContext maskContext = null;
      MaskElement destMask = null;
      if (this.maskMode[0] != 0) {
         maskContext = new MaskContext(Minecraft.getInstance().level);
         destMask = MaskManager.getDestMask();
      }

      for (StampPlacement placement : this.brushPlacements) {
         this.renderMap.computeIfAbsent(placement.blockRegion(), k -> new ArrayList<>()).add(placement.getRenderOffset());
         if (destMask != null) {
            placement.applyFailingDestinationMaskPreview(this.previewFailingDestinationMask, maskContext, destMask);
         }
      }

      for (IntermediatePlacement intermediate : this.intermediatePlacements) {
         StampPlacement placementx = intermediate.placement;
         this.renderMap.computeIfAbsent(placementx.blockRegion(), k -> new ArrayList<>()).add(placementx.getRenderOffset());
         if (destMask != null) {
            placementx.applyFailingDestinationMaskPreview(this.previewFailingDestinationMask, maskContext, destMask);
         }
      }
   }

   private static StampTool.AsyncComputationResult update(
      Position2dToIntMap heightMap,
      List<StampBlueprintConfig> blueprintSettings,
      List<TransformedBlockRegions> transformedBlueprints,
      int minSpacingPercentage,
      boolean randomYaw,
      boolean randomXZFlip,
      Direction direction
   ) {
      AxisCycle axisCycle = AxisCycle.between(direction.getAxis(), Axis.Y);
      AxisCycle axisCycleInverse = axisCycle.inverse();
      PositionalRandom positionalRandom = new PositionalRandom(0L, 0L);
      ChunkPos chunkPos = heightMap.getLastChunk();
      int relA = chunkPos.x * 16 + 8;
      int relC = chunkPos.z * 16 + 8;
      LongList positions = new LongArrayList();
      heightMap.forEachEntry((ax, cx, bx) -> {
         positionalRandom.at(cx, ax, bx);
         int weight = positionalRandom.nextInt();
         long position = (weight & 4294967295L) << 32 | ((short)(ax - relA) & 65535L) << 16 | (short)(cx - relC) & 65535L;
         positions.add(position);
      });
      positions.unstableSort(null);
      Position2dSet spacingBlacklistSet = new Position2dSet();
      List<StampPlacement> newBrushPlacements = new ArrayList<>();
      float totalWeight = 0.0F;

      for (StampBlueprintConfig config : blueprintSettings) {
         totalWeight += config.chance()[0];
      }

      float invTotalWeight = 1.0F / totalWeight;

      for (int index = 0; index < positions.size(); index++) {
         long pos = positions.getLong(index);
         int a = (short)(pos >>> 16 & 65535L) + relA;
         int c = (short)(pos & 65535L) + relC;
         int b = heightMap.get(a, c);
         if (!spacingBlacklistSet.contains(a, c)) {
            positionalRandom.at(a, b, c);
            int blueprintIndex = -1;
            float blueprintFloat = positionalRandom.nextFloat();

            for (int i = 0; i < blueprintSettings.size(); i++) {
               StampBlueprintConfig config = blueprintSettings.get(i);
               blueprintFloat -= config.chance()[0] * invTotalWeight;
               if (blueprintFloat <= 0.0F) {
                  blueprintIndex = i;
                  break;
               }
            }

            if (blueprintIndex < 0) {
               blueprintIndex = blueprintSettings.size() - 1;
            }

            StampBlueprintConfig config = blueprintSettings.get(blueprintIndex);
            int rotation = randomYaw ? positionalRandom.nextInt(4) : 0;
            boolean flipX = randomXZFlip && positionalRandom.nextBoolean();
            boolean flipZ = !randomYaw && randomXZFlip && positionalRandom.nextBoolean();
            int x = axisCycleInverse.cycle(a, b, c, Axis.X);
            int y = axisCycleInverse.cycle(a, b, c, Axis.Y);
            int z = axisCycleInverse.cycle(a, b, c, Axis.Z);
            TransformedBlockRegions transformed = transformedBlueprints.get(blueprintIndex);
            ChunkedBlockRegion blockRegion = transformed.getBlocks(rotation, flipX, flipZ);
            int[] offset = new int[]{0, 0, 0};
            if (config.automaticOffset()[0]) {
               Vec3i min = blockRegion.min();
               Vec3i max = blockRegion.max();
               if (min != null && max != null) {
                  switch (direction) {
                     case UP:
                        offset[1] = (min.getY() + max.getY()) / 2 - min.getY();
                        break;
                     case DOWN:
                        offset[1] = (min.getY() + max.getY()) / 2 - max.getY();
                        break;
                     case NORTH:
                        offset[2] = (min.getZ() + max.getZ()) / 2 - max.getZ();
                        break;
                     case SOUTH:
                        offset[2] = (min.getZ() + max.getZ()) / 2 - min.getZ();
                        break;
                     case EAST:
                        offset[0] = (min.getX() + max.getX()) / 2 - min.getX();
                        break;
                     case WEST:
                        offset[0] = (min.getX() + max.getX()) / 2 - max.getX();
                  }
               }
            } else {
               offset = config.offset();
            }

            StampPlacement stampPlacement = new StampPlacement(
               blockRegion,
               transformed.getBlockEntities(rotation, flipX, flipZ),
               transformed,
               rotation,
               flipX,
               flipZ,
               x + offset[0],
               y + offset[1],
               z + offset[2],
               x,
               y,
               z
            );
            newBrushPlacements.add(stampPlacement);
            float minSpacingA = 0.0F;
            float minSpacingC = 0.0F;
            ChunkedBlockRegion region = stampPlacement.blockRegion();
            BlockPos regionMin = region.min();
            BlockPos regionMax = region.max();
            if (regionMin != null && regionMax != null) {
               Vec3i size = regionMax.subtract(regionMin);
               int sizeA = axisCycle.cycle(size.getX(), size.getY(), size.getZ(), Axis.X) + 1;
               int sizeC = axisCycle.cycle(size.getX(), size.getY(), size.getZ(), Axis.Z) + 1;
               minSpacingA = sizeA * minSpacingPercentage / 100.0F * 1.414F;
               minSpacingC = sizeC * minSpacingPercentage / 100.0F * 1.414F;
            }

            if (minSpacingA > 0.0F && minSpacingC > 0.0F) {
               int halfA = (int)Math.ceil(minSpacingA / 2.0F);
               int halfC = (int)Math.ceil(minSpacingC / 2.0F);

               for (int ao = -halfA; ao <= halfA; ao++) {
                  for (int co = -halfC; co <= halfC; co++) {
                     float ar = ao / (minSpacingA / 2.0F + 1.0F);
                     float cr = co / (minSpacingC / 2.0F + 1.0F);
                     if (ar * ar + cr * cr <= 1.0F) {
                        spacingBlacklistSet.add(a + ao, c + co);
                     }
                  }
               }
            }
         }
      }

      return new StampTool.AsyncComputationResult(spacingBlacklistSet, newBrushPlacements);
   }

   @Nullable
   private StampPlacement createSinglePlacement(int x, int y, int z, Direction direction) {
      if (this.blueprintSettings.isEmpty()) {
         return null;
      } else {
         this.updateTransformedBlueprintList();
         PositionalRandom positionalRandom = new PositionalRandom(0L, 0L);
         float totalWeight = 0.0F;

         for (StampBlueprintConfig config : this.blueprintSettings) {
            totalWeight += config.chance()[0];
         }

         float invTotalWeight = 1.0F / totalWeight;
         positionalRandom.at(x, y, z);
         int blueprintIndex = -1;
         float blueprintFloat = positionalRandom.nextFloat();

         for (int i = 0; i < this.blueprintSettings.size(); i++) {
            StampBlueprintConfig config = this.blueprintSettings.get(i);
            blueprintFloat -= config.chance()[0] * invTotalWeight;
            if (blueprintFloat <= 0.0F) {
               blueprintIndex = i;
               break;
            }
         }

         if (blueprintIndex < 0) {
            blueprintIndex = this.blueprintSettings.size() - 1;
         }

         StampBlueprintConfig config = this.blueprintSettings.get(blueprintIndex);
         int rotation = this.randomYaw ? positionalRandom.nextInt(4) : 0;
         boolean flipX = this.randomXZFlip && positionalRandom.nextBoolean();
         boolean flipZ = !this.randomYaw && this.randomXZFlip && positionalRandom.nextBoolean();
         TransformedBlockRegions transformed = this.transformedBlueprints.get(blueprintIndex);
         ChunkedBlockRegion blockRegion = transformed.getBlocks(rotation, flipX, flipZ);
         int[] offset = new int[]{0, 0, 0};
         if (config.automaticOffset()[0]) {
            Vec3i min = blockRegion.min();
            Vec3i max = blockRegion.max();
            if (min != null && max != null) {
               switch (direction) {
                  case UP:
                     offset[1] = (min.getY() + max.getY()) / 2 - min.getY();
                     break;
                  case DOWN:
                     offset[1] = (min.getY() + max.getY()) / 2 - max.getY();
                     break;
                  case NORTH:
                     offset[2] = (min.getZ() + max.getZ()) / 2 - max.getZ();
                     break;
                  case SOUTH:
                     offset[2] = (min.getZ() + max.getZ()) / 2 - min.getZ();
                     break;
                  case EAST:
                     offset[0] = (min.getX() + max.getX()) / 2 - min.getX();
                     break;
                  case WEST:
                     offset[0] = (min.getX() + max.getX()) / 2 - max.getX();
               }
            }
         } else {
            offset = config.offset();
         }

         return new StampPlacement(
            blockRegion,
            transformed.getBlockEntities(rotation, flipX, flipZ),
            transformed,
            rotation,
            flipX,
            flipZ,
            x + offset[0],
            y + offset[1],
            z + offset[2],
            x,
            y,
            z
         );
      }
   }

   @Override
   public void displayImguiOptions() {
      if (!this.blueprints.isEmpty()) {
         if (!this.isSinglePlacement()) {
            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
            this.settingsChanged = this.settingsChanged
               | ImGuiHelper.combo(AxiomI18n.get("axiom.tool.generic.brush_shape"), this.brushShape, BallShape.getAllNames());
            this.settingsChanged = this.settingsChanged | ImGui.sliderInt(AxiomI18n.get("axiom.tool.generic.brush_radius"), this.radius, 0, 64);
         }

         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.stamp"));
         this.settingsChanged = this.settingsChanged
            | ImGuiHelper.combo(
               AxiomI18n.get("axiom.tool.stamp.place"),
               this.placeMode,
               new String[]{
                  AxiomI18n.get("axiom.tool.stamp.place_immediate"),
                  AxiomI18n.get("axiom.tool.stamp.place_deferred"),
                  "Single (Immediate)",
                  "Single (Deferred)"
               }
            );
         if (!this.isSinglePlacement()) {
            this.settingsChanged = this.settingsChanged
               | ImGuiHelper.combo(
                  AxiomI18n.get("axiom.tool.stamp.direction"), this.stampDirection, new String[]{"Top", "Bottom", "North", "East", "South", "West"}
               );
            this.settingsChanged = this.settingsChanged
               | ImGui.sliderFloat(AxiomI18n.get("axiom.tool.stamp.base_chance"), this.baseChance, 0.001F, 1.0F, "%.3f", 32);
            this.settingsChanged = this.settingsChanged
               | ImGui.sliderInt(AxiomI18n.get("axiom.tool.stamp.min_spacing"), this.minSpacingPercentage, 0, 800, "%d%%");
         }

         if (EditorWindowType.TOOL_MASKS.isOpen() || !Selection.getSelectionBuffer().isEmpty()) {
            ImGuiHelper.separatorWithText("Masking");
            ImGuiHelper.combo("Mask Mode", this.maskMode, new String[]{"Source", "Destination", "Source & Destination"});
            if (this.maskMode[0] == 0) {
               ImGuiHelper.tooltip(
                  "Mask/selection only affects where the blueprints can be positioned, e.g. masking 'block = grass_block' will make it so that the stamp tool can only place on grass blocks"
               );
            } else if (this.maskMode[0] == 1) {
               ImGuiHelper.tooltip("Mask/selection only affects final block placement");
            } else if (this.maskMode[0] == 2) {
               ImGuiHelper.tooltip("Mask/selection affects both blueprint positioning and final block placement");
            }
         }

         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.smooth.modifiers"));
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.stamp.random_yaw"), this.randomYaw)) {
            this.randomYaw = !this.randomYaw;
            this.settingsChanged = true;
         }

         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.stamp.random_xz_flip"), this.randomXZFlip)) {
            this.randomXZFlip = !this.randomXZFlip;
            this.settingsChanged = true;
         }

         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.stamp.paste_options"));
         if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.keep_existing"), this.keepExisting)) {
            this.keepExisting = !this.keepExisting;
            this.settingsChanged = true;
         }

         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.stamp.extend_foundation_to_ground"), this.extendFoundationToGround)) {
            this.extendFoundationToGround = !this.extendFoundationToGround;
            this.settingsChanged = true;
         }

         ImGui.sameLine();
         ImGuiHelper.helpMarker(AxiomI18n.get("axiom.tool.stamp.extend_foundation_to_ground_tooltip"));
         if (!this.extendFoundationToGround && ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.apply_gravity"), this.applyGravity)) {
            this.applyGravity = !this.applyGravity;
            this.settingsChanged = true;
         }
      }

      boolean canCreatePreset = AxiomClient.hasPermission(AxiomPermission.CAN_EXPORT_BLOCKS) && !this.blueprints.isEmpty();
      if (canCreatePreset || this.presetWidget.getPresetCount() > 0) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
         this.presetWidget.displayImgui(this.settingsChanged, canCreatePreset);
      }

      if (this.settingsChanged) {
         this.cachedSinglePlacementPreview = null;
      }

      this.settingsChanged = false;
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.stamp.blueprints"));
      if (!AxiomClient.hasPermission(AxiomPermission.CAN_IMPORT_BLOCKS)) {
         ImGui.beginDisabled();
         ImGui.button(AxiomI18n.get("axiom.tool.stamp.add_blueprint"));
         ImGui.endDisabled();
         ImGuiHelper.tooltip("The server has disallowed the use of blueprints", 1024);
      } else if (ImGui.button(AxiomI18n.get("axiom.tool.stamp.add_blueprint"))) {
         Predicate<Blueprint> callback = blueprintx -> {
            if (ToolManager.isToolActive() && ToolManager.getCurrentTool() == this) {
               this.addBlueprint(new ClipboardObject.FromBlueprint(blueprintx));
               return true;
            } else {
               return false;
            }
         };
         BlueprintBrowserWindow.open(callback, true);
      }

      ImGui.sameLine();
      if (Clipboard.INSTANCE.isEmpty()) {
         ImGui.beginDisabled();
         ImGui.button(AxiomI18n.get("axiom.hardcoded.add_clipboard"));
         ImGui.endDisabled();
         ImGuiHelper.tooltip("Clipboard is empty", 1024);
      } else if (ImGui.button(AxiomI18n.get("axiom.hardcoded.add_clipboard"))) {
         this.addBlueprint(Clipboard.INSTANCE.getClipboard());
      }

      ImGui.sameLine();
      if (ImGui.button(AxiomI18n.get("axiom.editorui.window.clipboard.clear"))) {
         this.blueprints.clear();
         this.blueprintSettings.clear();
         this.settingsChanged = true;
      }

      int removeIndex = -1;

      for (int i = 0; i < Math.min(this.blueprintSettings.size(), this.blueprints.size()); i++) {
         ClipboardObject blueprint = this.blueprints.get(i);
         StampBlueprintConfig blueprintConfig = this.blueprintSettings.get(i);
         ImGui.pushStyleVar(11, 2.0F, 2.0F);
         ImGui.imageButton("##BlueprintThumbnail" + i, blueprint.thumbnailTextureId(), 64.0F, 64.0F, 0.0F, 1.0F, 1.0F, 0.0F);
         ImGui.popStyleVar();
         if (ImGui.isItemClicked(1)) {
            ImGui.openPopup("##EditBlueprint" + i);
         }

         if (ImGuiHelper.beginPopup("##EditBlueprint" + i)) {
            if (ImGui.menuItem(AxiomI18n.get("axiom.tool.path.remove"))) {
               removeIndex = i;
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.rotate_cw_x"))) {
               this.blueprints.set(i, ClipboardObject.rotate(blueprint, Axis.X, -1));
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.rotate_cw_y"))) {
               this.blueprints.set(i, ClipboardObject.rotate(blueprint, Axis.Y, -1));
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.rotate_cw_z"))) {
               this.blueprints.set(i, ClipboardObject.rotate(blueprint, Axis.Z, -1));
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_x_paren"))) {
               this.blueprints.set(i, ClipboardObject.flip(blueprint, Axis.X));
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_y_paren"))) {
               this.blueprints.set(i, ClipboardObject.flip(blueprint, Axis.Y));
            }

            if (ImGui.menuItem(AxiomI18n.get("axiom.hardcoded.flip_z_paren"))) {
               this.blueprints.set(i, ClipboardObject.flip(blueprint, Axis.Z));
            }

            ImGui.endPopup();
         }

         ImGui.sameLine();
         ImGui.beginGroup();
         String name = blueprint.name();
         boolean blankName = name.isBlank();
         String displayName = !blankName ? name : AxiomI18n.get("axiom.editorui.window.blueprint_browser.unnamed_blueprint");
         String formattedCount = NumberFormat.getNumberInstance().format((long)blueprint.blockRegion().count());
         String blockCount = AxiomI18n.get("axiom.editorui.window.clipboard.n_blocks", formattedCount);
         ImGui.text(displayName + " (" + blockCount + ")");
         if (blueprintConfig.automaticOffset()[0]) {
            if (ImGui.checkbox(AxiomI18n.get("axiom.tool.stamp.automatic_offset"), true)) {
               blueprintConfig.automaticOffset()[0] = false;
               Vec3i min = blueprint.blockRegion().min();
               Vec3i max = blueprint.blockRegion().max();
               if (min != null && max != null) {
                  Arrays.fill(blueprintConfig.offset(), 0);
                  switch (this.stampDirection[0]) {
                     case 0:
                        blueprintConfig.offset()[1] = (min.getY() + max.getY()) / 2 - min.getY();
                        break;
                     case 1:
                        blueprintConfig.offset()[1] = (min.getY() + max.getY()) / 2 - max.getY();
                        break;
                     case 2:
                        blueprintConfig.offset()[2] = (min.getZ() + max.getZ()) / 2 - max.getZ();
                        break;
                     case 3:
                        blueprintConfig.offset()[0] = (min.getX() + max.getX()) / 2 - min.getX();
                        break;
                     case 4:
                        blueprintConfig.offset()[2] = (min.getZ() + max.getZ()) / 2 - min.getZ();
                        break;
                     case 5:
                        blueprintConfig.offset()[0] = (min.getX() + max.getX()) / 2 - max.getX();
                  }
               }
            }
         } else if (ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.stamp.offset") + "##" + i, blueprintConfig.offset(), true)) {
            if (!blankName) {
               this.savedOffsets.put(name, new BlockPos(blueprintConfig.offset()[0], blueprintConfig.offset()[1], blueprintConfig.offset()[2]));
            }

            this.settingsChanged = true;
         }

         ImGui.setNextItemWidth(ImGui.calcItemWidth() - (ImGui.getCursorPosX() - ImGui.getStyle().getWindowPaddingX()) * 2.0F / 3.0F);
         this.settingsChanged = this.settingsChanged
            | ImGui.sliderFloat(AxiomI18n.get("axiom.tool.stamp.chance") + "##" + i, blueprintConfig.chance(), 0.0F, 100.0F, "%.1f%%");
         ImGui.endGroup();
      }

      if (removeIndex >= 0) {
         this.blueprints.remove(removeIndex);
         this.blueprintSettings.remove(removeIndex);
         this.settingsChanged = true;
      }

      if (this.settingsChanged) {
         this.cachedSinglePlacementPreview = null;
      }
   }

   private void addBlueprint(ClipboardObject blueprint) {
      float total = 0.0F;

      for (StampBlueprintConfig config : this.blueprintSettings) {
         total += config.chance()[0];
      }

      float newChance;
      if (total < 100.0F) {
         newChance = 100.0F - total;
      } else {
         float factor = (float)this.blueprintSettings.size() / (this.blueprintSettings.size() + 1);

         for (StampBlueprintConfig config : this.blueprintSettings) {
            config.chance()[0] *= factor;
         }

         newChance = 100.0F / (this.blueprintSettings.size() + 1);
      }

      int[] offset = new int[3];
      boolean[] automaticOffset = new boolean[]{true};
      String name = blueprint.name();
      if (!name.isBlank() && this.savedOffsets.containsKey(name)) {
         BlockPos savedOffset = this.savedOffsets.get(name);
         offset[0] = savedOffset.getX();
         offset[1] = savedOffset.getY();
         offset[2] = savedOffset.getZ();
         automaticOffset[0] = false;
      } else {
         Vec3i min = blueprint.blockRegion().min();
         Vec3i max = blueprint.blockRegion().max();
         if (min != null && max != null) {
            switch (this.stampDirection[0]) {
               case 0:
                  offset[1] = (min.getY() + max.getY()) / 2 - min.getY();
                  break;
               case 1:
                  offset[1] = (min.getY() + max.getY()) / 2 - max.getY();
                  break;
               case 2:
                  offset[2] = (min.getZ() + max.getZ()) / 2 - max.getZ();
                  break;
               case 3:
                  offset[0] = (min.getX() + max.getX()) / 2 - min.getX();
                  break;
               case 4:
                  offset[2] = (min.getZ() + max.getZ()) / 2 - min.getZ();
                  break;
               case 5:
                  offset[0] = (min.getX() + max.getX()) / 2 - max.getX();
            }
         }
      }

      this.blueprints.add(blueprint);
      this.blueprintSettings.add(new StampBlueprintConfig(automaticOffset, offset, new float[]{newChance}));
      this.settingsChanged = true;
   }

   @Override
   public String listenForEsc() {
      if (!this.intermediatePlacements.isEmpty() && this.selectedGizmo) {
         return "Deselect Gizmo";
      } else {
         return !this.usingTool && this.intermediatePlacements.isEmpty() ? null : AxiomI18n.get("axiom.widget.cancel");
      }
   }

   @Override
   public boolean initiateAdjustment() {
      return this.radiusAdjustment.initiateAdjustment(this.radius[0], -1);
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      GenericRadiusAdjustment.Result result = this.radiusAdjustment.renderAdjustment(mouseX, mouseY, mouseDelta);
      this.radius[0] = result.radius();
      return result.mouseDelta();
   }

   @Override
   public String listenForEnter() {
      return this.brushPlacements.isEmpty() && this.intermediatePlacements.isEmpty() ? null : AxiomI18n.get("axiom.widget.confirm");
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.stamp");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putByte("BrushShape", (byte)this.brushShape[0]);
      tag.putInt("BrushRadius", this.radius[0]);
      tag.putInt("PlaceMode", this.placeMode[0]);
      tag.putInt("StampDirection", this.stampDirection[0]);
      tag.putFloat("BaseChance", this.baseChance[0]);
      tag.putInt("MinSpacingPercentage", this.minSpacingPercentage[0]);
      tag.putBoolean("RandomYaw", this.randomYaw);
      tag.putBoolean("RandomXZFlip", this.randomXZFlip);
      tag.putInt("MaskMode", this.maskMode[0]);
      ListTag blueprintsTag = new ListTag();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      for (int i = 0; i < this.blueprints.size(); i++) {
         baos.reset();
         ClipboardObject blueprint = this.blueprints.get(i);
         StampBlueprintConfig blueprintConfig = this.blueprintSettings.get(i);

         try {
            CompoundTag stampBlueprint = new CompoundTag();
            NativeImage image;
            BlueprintHeader header;
            if (blueprint instanceof ClipboardObject.FromBlueprint fromBlueprint) {
               image = fromBlueprint.blueprint().thumbnail().getPixels();
               header = fromBlueprint.blueprint().header();
            } else {
               if (blueprint instanceof ClipboardObject.Anonymous anonymous) {
                  image = anonymous.texture == null ? null : anonymous.texture.getPixels();
               } else {
                  image = null;
               }

               header = new BlueprintHeader(
                  blueprint.name(), "", new ArrayList<>(), 0.0F, 0.0F, false, blueprint.blockRegion().count(), blueprint.containsAir()
               );
            }

            boolean closeNativeImage = false;
            if (image == null) {
               image = new NativeImage(96, 96, true);
               closeNativeImage = true;
            }

            BlueprintIo.write(baos, header, image, blueprint.blockRegion(), new Long2ObjectOpenHashMap(), List.of());
            stampBlueprint.putByteArray("Blueprint", baos.toByteArray());
            stampBlueprint.putBoolean("AutomaticOffset", blueprintConfig.automaticOffset()[0]);
            stampBlueprint.putInt("OffsetX", blueprintConfig.offset()[0]);
            stampBlueprint.putInt("OffsetY", blueprintConfig.offset()[1]);
            stampBlueprint.putInt("OffsetZ", blueprintConfig.offset()[2]);
            stampBlueprint.putFloat("Chance", blueprintConfig.chance()[0]);
            blueprintsTag.add(stampBlueprint);
            if (closeNativeImage) {
               image.close();
            }
         } catch (Exception var12) {
            var12.printStackTrace();
         }
      }

      tag.put("Blueprints", blueprintsTag);
   }

   public void clearBlueprints() {
      this.blueprints.clear();
      this.blueprintSettings.clear();
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushShape[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BrushShape", 0);
      this.radius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BrushRadius", 4);
      this.placeMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "PlaceMode", 0);
      this.stampDirection[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "StampDirection", 0);
      this.baseChance[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "BaseChance", 1.0F);
      this.minSpacingPercentage[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "MinSpacingPercentage", 100);
      this.randomYaw = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "RandomYaw", true);
      this.randomXZFlip = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "RandomXZFlip", true);
      this.maskMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "MaskMode", 0);
      this.clearBlueprints();
      if (AxiomClient.hasPermission(AxiomPermission.CAN_IMPORT_BLOCKS)) {
         for (Tag blueprintTag : NbtHelper.getList(tag, "Blueprints", 10)) {
            CompoundTag blueprint = (CompoundTag)blueprintTag;

            try {
               byte[] blueprintData = VersionUtilsNbt.helperCompoundTagGetByteArray(blueprint, "Blueprint").orElse(null);
               Blueprint fullBlueprint = BlueprintIo.readBlueprint(new ByteArrayInputStream(blueprintData));
               boolean[] automaticOffset = new boolean[]{VersionUtilsNbt.helperCompoundTagGetBooleanOr(blueprint, "AutomaticOffset", false)};
               int[] offset = new int[]{
                  VersionUtilsNbt.helperCompoundTagGetIntOr(blueprint, "OffsetX", 0),
                  VersionUtilsNbt.helperCompoundTagGetIntOr(blueprint, "OffsetY", 0),
                  VersionUtilsNbt.helperCompoundTagGetIntOr(blueprint, "OffsetZ", 0)
               };
               float chance = VersionUtilsNbt.helperCompoundTagGetFloatOr(blueprint, "Chance", 0.0F);
               this.blueprints.add(new ClipboardObject.FromBlueprint(fullBlueprint));
               this.blueprintSettings.add(new StampBlueprintConfig(automaticOffset, offset, new float[]{chance}));
            } catch (Exception var11) {
               var11.printStackTrace();
            }
         }
      }
   }

   @Override
   public char iconChar() {
      return '\ue91b';
   }

   @Override
   public String keybindId() {
      return "stamp";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_STAMP, AxiomPermission.BUILD_SECTION);
   }

   record AsyncComputationResult(Position2dSet spacingBlacklistSet, List<StampPlacement> newBrushPlacements) {
   }
}
