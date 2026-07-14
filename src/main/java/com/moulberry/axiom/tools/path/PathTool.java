package com.moulberry.axiom.tools.path;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.HDVoxelMap;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.gizmo.ExtrudedGizmo;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.noise.WhiteNoise;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.modelling.GizmoList;
import com.moulberry.axiom.utils.BezierOperator;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryBuffer;
import com.moulberry.axiom.world_modification.HistoryEntry;
import com.moulberry.axiom.world_modification.undo.CreatePathAdditionalUndoOperation;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public class PathTool implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private GizmoList gizmoList = new GizmoList();
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private boolean cutout = false;
   private boolean recalculate = false;
   private BlockState lastActiveBlock = null;
   private boolean maskWindowOpen = false;
   private ExtrudedGizmo extrudedGizmo = null;
   private final int[] curveType = new int[]{1};
   private boolean looped = false;
   private boolean useStairsAndSlabs = false;
   private final int[] shape = new int[]{0};
   private final int[] radius = new int[]{0};
   private final int[] endRadius = new int[]{0};
   private final int[] depth = new int[]{0};
   private final List<PointConfig> pointConfigs = new ArrayList<>();
   private boolean inverted = false;
   private final int[] slack = new int[]{20};
   private boolean keepExisting = false;
   private boolean extendToGround = false;
   private final PresetWidget presetWidget = new PresetWidget(this, "path");
   private static final int CURVE_TYPE_BRESENHAM = 0;
   private static final int CURVE_TYPE_DDA = 1;
   private static final int CURVE_TYPE_CATENARY = 2;
   private static final int CURVE_TYPE_CATMULLROM = 3;
   private static final int CURVE_TYPE_BEZIER = 4;

   @Override
   public void reset() {
      this.gizmoList = new GizmoList();
      this.pointConfigs.clear();
      this.chunkedBlockRegion.clear();
      this.recalculate = true;
      if (this.cutout) {
         ChunkRenderOverrider.release("Path Tool");
         this.cutout = false;
      }
   }

   public void restore(
      List<BlockPos> positions,
      List<PointConfig> pointConfigs,
      int curveType,
      boolean looped,
      boolean useStairsAndSlabs,
      int shape,
      int radius,
      int endRadius,
      int depth,
      boolean inverted,
      int slack,
      boolean keepExisting,
      boolean extendToGround
   ) {
      this.reset();

      for (BlockPos position : positions) {
         this.gizmoList.addGizmo(Vec3.atCenterOf(position));
      }

      for (PointConfig pointConfig : pointConfigs) {
         this.pointConfigs.add(new PointConfig(pointConfig));
      }

      this.curveType[0] = curveType;
      this.looped = looped;
      this.useStairsAndSlabs = useStairsAndSlabs;
      this.shape[0] = shape;
      this.radius[0] = radius;
      this.endRadius[0] = endRadius;
      this.depth[0] = depth;
      this.inverted = inverted;
      this.slack[0] = slack;
      this.keepExisting = keepExisting;
      this.extendToGround = extendToGround;
   }

   public void markDirty() {
      this.recalculate = true;
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case EXTRUDE:
            if (this.extrudedGizmo == null) {
               this.extrudedGizmo = this.gizmoList.extrude();
            }

            return UserAction.ActionResult.USED_STOP;
         case ENTER:
            if (this.extrudedGizmo != null) {
               this.finishExtrudeGizmo();
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.gizmoList.isEmpty()) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.pastePath();
            this.gizmoList = new GizmoList();
            this.chunkedBlockRegion.clear();
            this.recalculate = true;
            if (this.cutout) {
               ChunkRenderOverrider.release("Path Tool");
               this.cutout = false;
            }

            return UserAction.ActionResult.USED_STOP;
         case RIGHT_MOUSE:
            if (this.extrudedGizmo != null) {
               this.finishExtrudeGizmo();
               return UserAction.ActionResult.USED_STOP;
            }

            RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
            if (result != null) {
               if (this.gizmoList.size() >= this.getPointLimit()) {
                  return UserAction.ActionResult.USED_STOP;
               }

               this.addPointConfigAtActiveIndex();
               this.recalculate = true;
               this.gizmoList.addGizmo(Vec3.atCenterOf(result.getBlockPos()));
               this.ensurePointConfigsExist();
            }

            return UserAction.ActionResult.USED_STOP;
         case LEFT_MOUSE:
            if (this.extrudedGizmo != null) {
               this.finishExtrudeGizmo();
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.gizmoList.leftClick()) {
               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case ESCAPE:
            if (this.extrudedGizmo != null) {
               this.extrudedGizmo = null;
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.gizmoList.hasActiveGizmo()) {
               this.gizmoList.deselectActiveGizmo();
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case DELETE:
            if (this.extrudedGizmo != null) {
               this.extrudedGizmo = null;
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.gizmoList.isEmpty()) {
               this.reset();
            } else {
               this.gizmoList.delete();
            }

            this.recalculate = true;
            return UserAction.ActionResult.USED_STOP;
         case SCROLL:
            UserAction.ScrollAmount scrollAmount = (UserAction.ScrollAmount)object;
            if (this.gizmoList.handleScroll(scrollAmount.scrollX(), scrollAmount.scrollY())) {
               this.recalculate = true;
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case REDO:
            int oldSizex = this.gizmoList.size();
            if (this.gizmoList.redo()) {
               if (this.gizmoList.size() > oldSizex && this.gizmoList.hasActiveGizmo()) {
                  this.pointConfigs.add(this.gizmoList.getActiveGizmoIndex(), this.createDefaultPointConfig());
               }

               this.recalculate = true;
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case UNDO:
            int oldSize = this.gizmoList.size();
            if (this.gizmoList.undo()) {
               if (this.gizmoList.size() > oldSize && this.gizmoList.hasActiveGizmo()) {
                  this.pointConfigs.add(this.gizmoList.getActiveGizmoIndex(), this.createDefaultPointConfig());
               }

               this.recalculate = true;
               return UserAction.ActionResult.USED_STOP;
            }
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   private void addPointConfigAtActiveIndex() {
      int index = this.gizmoList.getActiveGizmoIndex();
      if (index >= 0 && index < this.pointConfigs.size() - 1) {
         this.pointConfigs.add(index + 1, this.createDefaultPointConfig());
      } else {
         this.ensurePointConfigsExist();
      }
   }

   private void ensurePointConfigsExist() {
      while (this.pointConfigs.size() < this.gizmoList.size()) {
         this.pointConfigs.add(this.createDefaultPointConfig());
      }
   }

   private PointConfig createDefaultPointConfig() {
      return new PointConfig(false, false, (CustomBlockState)Blocks.STONE.defaultBlockState(), this.radius[0]);
   }

   private void pastePath() {
      this.recalculate();
      if (!this.chunkedBlockRegion.isEmpty()) {
         Level level = Minecraft.getInstance().level;
         if (this.extendToGround && level != null) {
            Position2ObjectMap<BlockState> newBlocks = new Position2ObjectMap<>(k -> new BlockState[4096]);
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            BlockState full;
            if (this.useStairsAndSlabs) {
               BlockState activeBlock = Tool.getActiveBlock();
               HDVoxelMap.HDVoxelBaseBlocks blocks = HDVoxelMap.getAssociatedBlocks(activeBlock.getBlock());
               if (blocks == null) {
                  full = Blocks.STONE.defaultBlockState();
               } else {
                  full = blocks.full().defaultBlockState();
               }
            } else {
               full = null;
            }

            this.chunkedBlockRegion.forEachEntry((x, y, z, block) -> {
               for (int yo = 1; yo < 256 && this.chunkedBlockRegion.getBlockStateOrAir(x, y - yo, z).isAir(); yo++) {
                  if (this.useStairsAndSlabs && yo == 1) {
                     if (block.getBlock() instanceof StairBlock && block.getValue(StairBlock.HALF) == Half.TOP) {
                        newBlocks.put(x, y, z, full);
                     } else if (block.getBlock() instanceof SlabBlock && block.getValue(SlabBlock.TYPE) == SlabType.TOP) {
                        newBlocks.put(x, y, z, full);
                     }

                     block = full;
                  }

                  BlockState below = level.getBlockState(mutableBlockPos.set(x, y - yo, z));
                  if (below.getBlock() == Blocks.VOID_AIR || !below.canBeReplaced()) {
                     break;
                  }

                  newBlocks.put(x, y - yo, z, block);
               }
            });
            newBlocks.forEachEntry(this.chunkedBlockRegion::addBlock);
         }

         int modifiers = this.keepExisting ? HistoryEntry.MODIFIER_KEEP_EXISTING : 0;
         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.placed", countString);
         List<BlockPos> positions = new ArrayList<>();
         List<PointConfig> pointConfigs = new ArrayList<>();

         for (Gizmo gizmo : this.gizmoList.getGizmos()) {
            positions.add(gizmo.getTargetPosition());
         }

         for (PointConfig pointConfig : this.pointConfigs) {
            pointConfigs.add(new PointConfig(pointConfig));
         }

         CreatePathAdditionalUndoOperation operation = new CreatePathAdditionalUndoOperation(
            positions,
            pointConfigs,
            this.curveType[0],
            this.looped,
            this.useStairsAndSlabs,
            this.shape[0],
            this.radius[0],
            this.endRadius[0],
            this.depth[0],
            this.inverted,
            this.slack[0],
            this.keepExisting,
            this.extendToGround
         );
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, null, historyDescription, modifiers, operation);
      }
   }

   private void finishExtrudeGizmo() {
      if (this.gizmoList.size() < this.getPointLimit()) {
         this.addPointConfigAtActiveIndex();
         this.gizmoList.finishExtrude(this.extrudedGizmo, Tool.getLookDirection());
         this.recalculate = true;
         this.extrudedGizmo = null;
         this.ensurePointConfigsExist();
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      Vec3 lookDirection = Tool.getLookDirection();
      Selection.render(rc, 4);
      if (lookDirection != null && this.extrudedGizmo != null) {
         this.extrudedGizmo.render(rc, lookDirection);
      }

      this.recalculate = this.recalculate | this.gizmoList.updateGizmos(rc);
      if (!this.gizmoList.hasGrabbedGizmo() && this.extrudedGizmo == null && this.gizmoList.size() < this.getPointLimit()) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
         Tool.renderRaycastOverlay(rc, result);
      }

      if (Tool.getActiveBlock() != this.lastActiveBlock) {
         this.lastActiveBlock = Tool.getActiveBlock();
         this.recalculate = true;
      }

      boolean maskWindowOpen = EditorWindowType.TOOL_MASKS.isOpen();
      if (this.maskWindowOpen != maskWindowOpen) {
         this.maskWindowOpen = maskWindowOpen;
         this.recalculate = true;
      }

      if (this.recalculate) {
         this.recalculate();
      }

      PoseStack matrices = rc.poseStack();
      matrices.pushPose();
      matrices.translate(-rc.x(), -rc.y(), -rc.z());
      BlockPos raycastPos = null;
      if (this.gizmoList.hasActiveGizmo() && !this.gizmoList.hasGrabbedGizmo() && this.extrudedGizmo == null && this.gizmoList.size() < this.getPointLimit()) {
         RayCaster.RaycastResult raycastResult = Tool.raycastBlock();
         if (raycastResult != null) {
            raycastPos = raycastResult.blockPos();
         }
      }

      this.drawPoints(matrices, raycastPos);
      matrices.popPose();
      float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
      this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
      this.gizmoList.renderGizmos(rc);
   }

   private void drawPoints(PoseStack matrices, BlockPos raycastPos) {
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      Pose pose = matrices.last();
      int activePoint = this.gizmoList.getActiveGizmoIndex();
      List<Gizmo> gizmos = this.gizmoList.getGizmos();
      int count = this.gizmoList.hasActiveGizmo() && raycastPos != null ? gizmos.size() : gizmos.size() - 1;

      for (int i = 0; i < count; i++) {
         Vec3 from;
         Vec3 to;
         if (activePoint < 0 || raycastPos == null) {
            from = gizmos.get(i).getInterpPosition();
            to = gizmos.get(i + 1).getInterpPosition();
         } else if (i == activePoint) {
            from = gizmos.get(i).getInterpPosition();
            to = Vec3.atCenterOf(raycastPos);
         } else if (i == activePoint + 1) {
            from = Vec3.atCenterOf(raycastPos);
            to = gizmos.get(i).getInterpPosition();
         } else if (i > activePoint + 1) {
            from = gizmos.get(i - 1).getInterpPosition();
            to = gizmos.get(i).getInterpPosition();
         } else {
            from = gizmos.get(i).getInterpPosition();
            to = gizmos.get(i + 1).getInterpPosition();
         }

         Shapes.line(bufferBuilder, pose, from, to);
      }

      MeshData meshData = provider.build();
      AxiomRenderer.setShaderColour(1.0F, 0.1F, 0.1F, 1.0F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
      AxiomRenderer.setShaderColour(1.0F, 0.1F, 0.1F, 0.35F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private int getPointLimit() {
      return this.curveType[0] == 4 ? 50 : 256;
   }

   private void recalculate() {
      this.ensurePointConfigsExist();
      this.recalculate = false;
      this.chunkedBlockRegion.clear();
      if (!this.gizmoList.isEmpty() && (this.shape[0] != 2 || !Clipboard.INSTANCE.isEmpty() && this.gizmoList.size() != 1)) {
         boolean useStairsAndSlabs = this.useStairsAndSlabs && this.shape[0] != 2;
         int scaleMult = useStairsAndSlabs ? 2 : 1;
         MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
         MaskElement destinationMask;
         if (useStairsAndSlabs) {
            MaskElement raw = MaskManager.getDestMask();
            if (raw instanceof ConstantMaskElement) {
               destinationMask = raw;
            } else {
               destinationMask = (context, xx, yx, zx) -> raw.test(context, xx / 2, yx / 2, zx / 2);
            }
         } else {
            destinationMask = MaskManager.getDestMask();
         }

         ChunkedBlockRegion targetRegion = useStairsAndSlabs ? new ChunkedBlockRegion() {
            @Override
            public void addBlockWithoutDirty(int x, int y, int z, BlockState block) {
               super.addBlockWithoutDirty(x, y, z, block);
               super.addBlockWithoutDirty(x, y, z + 1, block);
               super.addBlockWithoutDirty(x, y + 1, z, block);
               super.addBlockWithoutDirty(x, y + 1, z + 1, block);
               super.addBlockWithoutDirty(x + 1, y, z, block);
               super.addBlockWithoutDirty(x + 1, y, z + 1, block);
               super.addBlockWithoutDirty(x + 1, y + 1, z, block);
               super.addBlockWithoutDirty(x + 1, y + 1, z + 1, block);
            }
         } : this.chunkedBlockRegion;
         if (this.gizmoList.size() == 1) {
            Gizmo gizmo = this.gizmoList.getGizmos().get(0);
            PointConfig pointConfig = this.pointConfigs.get(0);
            BlockPos pos = gizmo.getTargetPosition().multiply(scaleMult);
            BlockState blockState;
            if (pointConfig.overrideBlock) {
               blockState = pointConfig.block.getVanillaState();
            } else {
               blockState = Tool.getActiveBlock();
            }

            int radius;
            if (this.curveType[0] == 0) {
               radius = 0;
            } else if (pointConfig.overrideRadius && this.shape[0] != 3) {
               radius = pointConfig.radius[0] * scaleMult;
            } else {
               radius = this.radius[0] * scaleMult;
            }

            if (this.shape[0] != 1) {
               int maxRadiusSq = radius * radius + radius;

               for (int xo = -radius; xo <= radius; xo++) {
                  for (int yo = -radius; yo <= radius; yo++) {
                     for (int zo = -radius; zo <= radius; zo++) {
                        if (xo * xo + yo * yo + zo * zo <= maxRadiusSq
                           && destinationMask.test(maskContext.reset(), pos.getX() + xo, pos.getY() + yo, pos.getZ() + zo)) {
                           targetRegion.addBlockWithoutDirty(pos.getX() + xo, pos.getY() + yo, pos.getZ() + zo, blockState);
                        }
                     }
                  }
               }
            } else {
               int depth = this.depth[0];

               for (int xo = -radius; xo <= radius; xo++) {
                  for (int yo = -depth; yo <= 0; yo++) {
                     for (int zox = -radius; zox <= radius; zox++) {
                        if (destinationMask.test(maskContext.reset(), pos.getX() + xo, pos.getY() + yo, pos.getZ() + zox)) {
                           targetRegion.addBlockWithoutDirty(pos.getX() + xo, pos.getY() + yo, pos.getZ() + zox, blockState);
                        }
                     }
                  }
               }
            }

            if (useStairsAndSlabs) {
               if (this.cutout) {
                  ChunkRenderOverrider.release("Path Tool");
                  this.cutout = false;
               }

               HDVoxelMap.downsizePathTool(targetRegion, this.chunkedBlockRegion);
               this.chunkedBlockRegion.dirtyAll();
            } else {
               this.chunkedBlockRegion.dirtyAll();
               if (!blockState.isAir() && !(blockState.getBlock() instanceof LiquidBlock)) {
                  if (this.cutout) {
                     ChunkRenderOverrider.release("Path Tool");
                     this.cutout = false;
                  }
               } else {
                  if (!this.cutout) {
                     ChunkRenderOverrider.acquire("Path Tool");
                     this.cutout = true;
                  }

                  PositionSet positionSet = new PositionSet();
                  this.chunkedBlockRegion.forEachEntry((xx, yx, zx, b) -> positionSet.add(xx, yx, zx));
                  ChunkRenderOverrider.cutoutBoolean(positionSet, BlockPos.ZERO);
               }
            }
         } else {
            boolean loop = this.gizmoList.size() >= 3 && this.looped;
            boolean forceDDA = this.gizmoList.size() < 3 && (this.curveType[0] == 3 || this.curveType[0] == 4);
            if (this.curveType[0] == 0) {
               BlockPos last = loop ? this.gizmoList.getGizmos().get(this.gizmoList.size() - 1).getTargetPosition().multiply(scaleMult) : null;
               BlockState blockStatex = Tool.getActiveBlock();

               for (Gizmo gizmox : this.gizmoList.getGizmos()) {
                  BlockPos posx = gizmox.getTargetPosition().multiply(scaleMult);
                  if (last != null) {
                     Rasterization3D.bresenham(last, posx, (xx, yx, zx) -> {
                        if (destinationMask.test(maskContext.reset(), xx, yx, zx)) {
                           targetRegion.addBlockWithoutDirty(xx, yx, zx, blockStatex);
                        }
                     });
                  }

                  last = posx;
               }

               if (useStairsAndSlabs) {
                  if (this.cutout) {
                     ChunkRenderOverrider.release("Path Tool");
                     this.cutout = false;
                  }

                  HDVoxelMap.downsizePathTool(targetRegion, this.chunkedBlockRegion);
                  this.chunkedBlockRegion.dirtyAll();
               } else {
                  this.chunkedBlockRegion.dirtyAll();
                  if (!blockStatex.isAir() && !(blockStatex.getBlock() instanceof LiquidBlock)) {
                     if (this.cutout) {
                        ChunkRenderOverrider.release("Path Tool");
                        this.cutout = false;
                     }
                  } else {
                     if (!this.cutout) {
                        ChunkRenderOverrider.acquire("Path Tool");
                        this.cutout = true;
                     }

                     PositionSet positionSet = new PositionSet();
                     this.chunkedBlockRegion.forEachEntry((xx, yx, zx, b) -> positionSet.add(xx, yx, zx));
                     ChunkRenderOverrider.cutoutBoolean(positionSet, BlockPos.ZERO);
                  }
               }
            } else {
               Set<BlockState> blockSet = new HashSet<>();
               List<BlockState> blocks = new ArrayList<>();
               FloatSet radiusSet = new FloatOpenHashSet();
               FloatList radii = new FloatArrayList();
               List<BlockPos> positions = new ArrayList<>();
               List<FloatUnaryOperator> easings = new ArrayList<>();
               FloatList angles = new FloatArrayList();
               FloatList lineAngles = new FloatArrayList();

               for (int i = 0; i < this.gizmoList.size(); i++) {
                  Gizmo gizmox = this.gizmoList.getGizmos().get(i);
                  PointConfig pointConfigx = this.pointConfigs.get(i);
                  BlockPos targetPos = gizmox.getTargetPosition().multiply(scaleMult);
                  positions.add(targetPos);
                  if (pointConfigx.overrideBlock) {
                     blockSet.add(pointConfigx.block.getVanillaState());
                     blocks.add(pointConfigx.block.getVanillaState());
                  } else {
                     blockSet.add(Tool.getActiveBlock());
                     blocks.add(Tool.getActiveBlock());
                  }

                  if (pointConfigx.overrideRadius) {
                     radiusSet.add(pointConfigx.radius[0] * scaleMult);
                     radii.add(pointConfigx.radius[0] * scaleMult);
                  } else {
                     radiusSet.add(this.radius[0] * scaleMult);
                     radii.add(this.radius[0] * scaleMult);
                  }

                  FloatUnaryOperator var10000 = switch (pointConfigx.easing[0]) {
                     case 1 -> {
                        switch (pointConfigx.easingType[0]) {
                           case 1:
                              yield Easings.EASE_OUT_SLIGHT;
                           case 2:
                              yield Easings.EASE_IN_OUT_SLIGHT;
                           default:
                              yield Easings.EASE_IN_SLIGHT;
                        }
                     }
                     case 2 -> {
                        switch (pointConfigx.easingType[0]) {
                           case 1:
                              yield Easings.EASE_OUT_QUAD;
                           case 2:
                              yield Easings.EASE_IN_OUT_QUAD;
                           default:
                              yield Easings.EASE_IN_QUAD;
                        }
                     }
                     case 3 -> {
                        switch (pointConfigx.easingType[0]) {
                           case 1:
                              yield Easings.EASE_OUT_CUBIC;
                           case 2:
                              yield Easings.EASE_IN_OUT_CUBIC;
                           default:
                              yield Easings.EASE_IN_CUBIC;
                        }
                     }
                     case 4 -> {
                        switch (pointConfigx.easingType[0]) {
                           case 1:
                              yield Easings.EASE_OUT_QUARTIC;
                           case 2:
                              yield Easings.EASE_IN_OUT_QUARTIC;
                           default:
                              yield Easings.EASE_IN_QUARTIC;
                        }
                     }
                     default -> Easings.LINEAR;
                  };

                  FloatUnaryOperator easing = var10000;
                  easings.add(easing);
                  float angleLeft = Float.NaN;
                  float angleRight = Float.NaN;
                  if (i < this.gizmoList.size() - 1 || loop) {
                     int j = i < this.gizmoList.size() - 1 ? i + 1 : 0;
                     BlockPos to = this.gizmoList.getGizmos().get(j).getTargetPosition().multiply(scaleMult);
                     angleLeft = (float)Math.atan2(to.getX() - targetPos.getX(), to.getZ() - targetPos.getZ());
                     lineAngles.add(angleLeft);
                  }

                  if (i > 0 || loop) {
                     int j = i > 0 ? i - 1 : this.gizmoList.size() - 1;
                     BlockPos from = this.gizmoList.getGizmos().get(j).getTargetPosition().multiply(scaleMult);
                     angleRight = (float)Math.atan2(targetPos.getX() - from.getX(), targetPos.getZ() - from.getZ());
                  }

                  if (Float.isNaN(angleLeft) && Float.isNaN(angleRight)) {
                     angles.add(0.0F);
                  } else if (Float.isNaN(angleLeft)) {
                     angles.add(angleRight);
                  } else if (Float.isNaN(angleRight)) {
                     angles.add(angleLeft);
                  } else {
                     float delta = angleLeft - angleRight;
                     delta = (float)(delta % (Math.PI * 2));
                     if (delta < -Math.PI) {
                        delta = (float)(delta + (Math.PI * 2));
                     }

                     if (delta > Math.PI) {
                        delta = (float)(delta - (Math.PI * 2));
                     }

                     angles.add(angleRight + delta / 2.0F);
                  }
               }

               if (useStairsAndSlabs) {
                  blockSet.clear();
                  blocks.clear();
                  blockSet.add(Blocks.STONE.defaultBlockState());
                  blocks.add(Blocks.STONE.defaultBlockState());
               }

               if (loop) {
                  positions.add(positions.get(0));
                  blocks.add(blocks.get(0));
                  radii.add(radii.getFloat(0));
                  easings.add(easings.get(0));
                  angles.add(angles.getFloat(0));
               }

               boolean constantBlockType = blockSet.size() == 1;
               boolean constantRadius = radiusSet.size() == 1;
               boolean zeroRadius = constantRadius && radii.getFloat(0) <= 0.0F;
               FloatUnaryOperator[] easingsArray = easings.toArray(new FloatUnaryOperator[0]);
               float[] anglesArray = angles.toFloatArray();
               float[] lineAnglesArray = lineAngles.toFloatArray();
               PathRasterizer pathRasterizer;
               if (this.shape[0] == 2) {
                  pathRasterizer = new PathRasterizer.Custom(
                     easingsArray,
                     anglesArray,
                     lineAnglesArray,
                     new double[]{0.0},
                     new Position2FloatMap(Float.MAX_VALUE),
                     Clipboard.INSTANCE.getClipboard().blockRegion()
                  );
               } else if (this.shape[0] == 3) {
                  pathRasterizer = new PathRasterizer.Spike(
                     easingsArray,
                     this.radius[0] * scaleMult,
                     this.endRadius[0] * scaleMult,
                     blocks.toArray(new BlockState[0]),
                     new WhiteNoise(1705875030),
                     new Position2FloatMap(Float.MAX_VALUE)
                  );
               } else if (zeroRadius) {
                  if (constantBlockType) {
                     pathRasterizer = new PathRasterizer.ZeroRadiusConstantBlock(blocks.get(0));
                  } else {
                     pathRasterizer = new PathRasterizer.ZeroRadiusDynamicBlock(easingsArray, blocks.toArray(new BlockState[0]), new WhiteNoise(1705875030));
                  }
               } else if (constantRadius) {
                  if (constantBlockType) {
                     if (this.shape[0] != 1) {
                        pathRasterizer = new PathRasterizer.ConstantRadiusConstantBlock(radii.getFloat(0), blocks.get(0));
                     } else {
                        pathRasterizer = new PathRasterizer.FlatConstantRadiusConstantBlock(
                           easingsArray, anglesArray, lineAnglesArray, radii.getFloat(0), this.depth[0] * scaleMult, blocks.get(0)
                        );
                     }
                  } else if (this.shape[0] != 1) {
                     pathRasterizer = new PathRasterizer.ConstantRadiusDynamicBlock(
                        easingsArray, radii.getFloat(0), blocks.toArray(new BlockState[0]), new WhiteNoise(1705875030), new Position2FloatMap(Float.MAX_VALUE)
                     );
                  } else {
                     pathRasterizer = new PathRasterizer.FlatConstantRadiusDynamicBlock(
                        easingsArray,
                        anglesArray,
                        lineAnglesArray,
                        radii.getFloat(0),
                        this.depth[0] * scaleMult,
                        blocks.toArray(new BlockState[0]),
                        new WhiteNoise(1705875030),
                        new Position2FloatMap(Float.MAX_VALUE)
                     );
                  }
               } else if (constantBlockType) {
                  if (this.shape[0] != 1) {
                     pathRasterizer = new PathRasterizer.DynamicRadiusConstantBlock(easingsArray, radii.toFloatArray(), blocks.get(0));
                  } else {
                     pathRasterizer = new PathRasterizer.FlatDynamicRadiusConstantBlock(
                        easingsArray, anglesArray, lineAnglesArray, radii.toFloatArray(), this.depth[0] * scaleMult, blocks.get(0)
                     );
                  }
               } else if (this.shape[0] != 1) {
                  pathRasterizer = new PathRasterizer.DynamicRadiusDynamicBlock(
                     easingsArray, radii.toFloatArray(), blocks.toArray(new BlockState[0]), new WhiteNoise(1705875030), new Position2FloatMap(Float.MAX_VALUE)
                  );
               } else {
                  pathRasterizer = new PathRasterizer.FlatDynamicRadiusDynamicBlock(
                     easingsArray,
                     anglesArray,
                     lineAnglesArray,
                     radii.toFloatArray(),
                     this.depth[0] * scaleMult,
                     blocks.toArray(new BlockState[0]),
                     new WhiteNoise(1705875030),
                     new Position2FloatMap(Float.MAX_VALUE)
                  );
               }

               if (this.curveType[0] != 1 && !forceDDA) {
                  if (this.curveType[0] == 2) {
                     for (int i = 0; i < positions.size() - 1; i++) {
                        BlockPos fromBlock = positions.get(i);
                        BlockPos toBlock = positions.get(i + 1);
                        int fromX = fromBlock.getX();
                        int fromY = fromBlock.getY();
                        int fromZ = fromBlock.getZ();
                        int toX = toBlock.getX();
                        int toY = toBlock.getY();
                        int toZ = toBlock.getZ();
                        if (fromX == toX && fromZ == toZ) {
                           Vector3d from = new Vector3d(fromBlock.getX() + 0.5, fromBlock.getY() + 0.5, fromBlock.getZ() + 0.5);
                           Vector3d to = new Vector3d(toBlock.getX() + 0.5, toBlock.getY() + 0.5, toBlock.getZ() + 0.5);
                           pathRasterizer.rasterize(targetRegion, destinationMask, maskContext, from, to, i, 0.0F, 1.0F);
                        } else {
                           double dx = toX - fromX;
                           double dy = toY - fromY;
                           double dz = toZ - fromZ;
                           double dh = Math.sqrt(dx * dx + dz * dz);
                           double distanceSq = dx * dx + dy * dy + dz * dz;
                           double l = Math.sqrt(distanceSq) * (1.0 + this.slack[0] / 100.0);
                           double r = Math.sqrt(l * l - dy * dy) / dh;
                           if (r <= 1.0) {
                              Vector3d from = new Vector3d(fromBlock.getX() + 0.5, fromBlock.getY() + 0.5, fromBlock.getZ() + 0.5);
                              Vector3d to = new Vector3d(toBlock.getX() + 0.5, toBlock.getY() + 0.5, toBlock.getZ() + 0.5);
                              pathRasterizer.rasterize(targetRegion, destinationMask, maskContext, from, to, i, 0.0F, 1.0F);
                           } else {
                              double bigA = findBigA(r);
                              double a = dh / (2.0 * bigA);
                              double b = dh / 2.0 - a * atanh(dy / l);
                              double c = (toY + fromY) / 2.0 - l / (2.0 * Math.tanh(bigA));
                              int iterations = Math.max((int)Math.ceil(l / 2.0), 4) - 1;
                              Vector3d last = null;
                              double lastAmount = 0.0;

                              for (int j = 0; j <= iterations; j++) {
                                 double amount = (double)j / iterations;
                                 float y = (float)(a * Math.cosh((dh * amount - b) / a) + c);
                                 if (this.inverted) {
                                    amount = 1.0 - amount;
                                    y = fromY + toY - y;
                                 }

                                 float x = (float)(toX * amount + fromX * (1.0 - amount));
                                 float z = (float)(toZ * amount + fromZ * (1.0 - amount));
                                 Vector3d posx = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                                 if (last != null) {
                                    pathRasterizer.rasterize(
                                       targetRegion, destinationMask, maskContext, last, posx, i, (float)lastAmount, (float)(amount - lastAmount)
                                    );
                                 }

                                 last = posx;
                                 lastAmount = amount;
                              }
                           }
                        }
                     }
                  } else if (this.curveType[0] == 3) {
                     if (loop) {
                        positions.remove(positions.size() - 1);
                     }

                     List<Vector3f> positionsF = new ArrayList<>();

                     for (Gizmo gizmoxx : this.gizmoList.getGizmos()) {
                        BlockPos posx = gizmoxx.getTargetPosition();
                        positionsF.add(new Vector3f(posx.getX() + 0.5F, posx.getY() + 0.5F, posx.getZ() + 0.5F).mul(scaleMult));
                     }

                     int size = positionsF.size();
                     int maxI = loop ? size : size - 1;

                     for (int ix = 0; ix < maxI; ix++) {
                        int i0 = ix - 1;
                        int i1 = ix + 1;
                        int i2 = ix + 2;
                        if (loop) {
                           i0 %= size;
                           i1 %= size;
                           i2 %= size;
                           if (i0 < 0) {
                              i0 += size;
                           }
                        } else {
                           if (i0 < 0) {
                              i0 = 0;
                           }

                           if (i1 >= size) {
                              i1 = size - 1;
                           }

                           if (i2 >= size) {
                              i2 = size - 1;
                           }
                        }

                        if (ix != i1) {
                           float distance = positionsF.get(ix).distance((Vector3fc)positionsF.get(i1));
                           int numPoints = Math.max(4, (int)Math.ceil(distance / 2.0F));
                           List<Vector4f> spline = CatmullRomSpline.createCatmullRomSplineWithPartial(
                              positionsF.get(i0), positionsF.get(ix), positionsF.get(i1), positionsF.get(i2), numPoints, 0.0F
                           );

                           for (int k = 0; k < spline.size() - 1; k++) {
                              Vector4f item1 = spline.get(k);
                              Vector4f item2 = spline.get(k + 1);
                              Vector3d pos1 = new Vector3d(item1.x, item1.y, item1.z);
                              Vector3d pos2 = new Vector3d(item2.x, item2.y, item2.z);
                              pathRasterizer.rasterize(targetRegion, destinationMask, maskContext, pos1, pos2, ix, item1.w, item2.w - item1.w);
                           }
                        }
                     }
                  } else if (this.curveType[0] == 4) {
                     float totalLength = 0.0F;
                     int count = loop ? this.gizmoList.size() + 1 : this.gizmoList.size();
                     BezierOperator[] factors = new BezierOperator[count];
                     BlockPos lastBlockPos = null;

                     for (int ix = 0; ix < this.gizmoList.size(); ix++) {
                        BlockPos posx = this.gizmoList.getGizmos().get(ix).getTargetPosition().multiply(scaleMult);
                        if (lastBlockPos != null) {
                           totalLength = (float)(totalLength + Math.sqrt(posx.distSqr(lastBlockPos)));
                        }

                        factors[ix] = new BezierOperator(ix, count);
                        lastBlockPos = posx;
                     }

                     if (loop) {
                        factors[count - 1] = new BezierOperator(count - 1, count);
                     }

                     int iterations = Math.max((int)Math.ceil(totalLength / 2.0F), 4) - 1;
                     Vector3d last = null;
                     double lastAmount = 0.0;

                     for (int ix = 0; ix <= iterations; ix++) {
                        double amountx = (double)ix / iterations;
                        double x = 0.0;
                        double yx = 0.0;
                        double z = 0.0;

                        for (int j = 0; j < positions.size(); j++) {
                           BlockPos position = positions.get(j);
                           double factor = factors[j].applyAsDouble(amountx);
                           x += factor * position.getX();
                           yx += factor * position.getY();
                           z += factor * position.getZ();
                        }

                        Vector3d posx = new Vector3d(x + 0.5, yx + 0.5, z + 0.5);
                        if (last != null) {
                           double lastFull = lastAmount * (count - 1);
                           double lastPartial = lastFull % 1.0;
                           int lastIndex = (int)Math.floor(lastFull);
                           if (lastPartial == 0.0 && lastIndex > 0) {
                              lastPartial = 1.0;
                              lastIndex--;
                           }

                           double currFull = amountx * (count - 1);
                           double currPartial = currFull % 1.0;
                           int currIndex = (int)Math.floor(currFull);
                           if (currPartial == 0.0 && currIndex > 0) {
                              currPartial = 1.0;
                              currIndex--;
                           }

                           if (lastIndex != currIndex) {
                              double to = 1.0 - lastPartial;
                              float mid = (float)(to / (to + currPartial));
                              double midX = last.x * (1.0F - mid) + posx.x * mid;
                              double midY = last.y * (1.0F - mid) + posx.y * mid;
                              double midZ = last.z * (1.0F - mid) + posx.z * mid;
                              Vector3d midVec = new Vector3d(midX, midY, midZ);
                              pathRasterizer.rasterize(targetRegion, destinationMask, maskContext, last, midVec, lastIndex, (float)lastPartial, (float)to);
                              pathRasterizer.rasterize(targetRegion, destinationMask, maskContext, midVec, posx, currIndex, 0.0F, (float)currPartial);
                           } else {
                              pathRasterizer.rasterize(
                                 targetRegion, destinationMask, maskContext, last, posx, currIndex, (float)lastPartial, (float)(currPartial - lastPartial)
                              );
                           }
                        }

                        last = posx;
                        lastAmount = amountx;
                     }
                  }
               } else {
                  for (int ix = 0; ix < positions.size() - 1; ix++) {
                     BlockPos fromBlock = positions.get(ix);
                     BlockPos toBlock = positions.get(ix + 1);
                     Vector3d from = new Vector3d(fromBlock.getX() + 0.5, fromBlock.getY() + 0.5, fromBlock.getZ() + 0.5);
                     Vector3d to = new Vector3d(toBlock.getX() + 0.5, toBlock.getY() + 0.5, toBlock.getZ() + 0.5);
                     pathRasterizer.rasterize(targetRegion, destinationMask, maskContext, from, to, ix, 0.0F, 1.0F);
                  }
               }

               pathRasterizer.finish(targetRegion, destinationMask, maskContext);
               if (useStairsAndSlabs) {
                  if (this.cutout) {
                     ChunkRenderOverrider.release("Path Tool");
                     this.cutout = false;
                  }

                  HDVoxelMap.downsizePathTool(targetRegion, this.chunkedBlockRegion);
                  this.chunkedBlockRegion.dirtyAll();
               } else {
                  this.chunkedBlockRegion.dirtyAll();
                  boolean hasInvisible = false;
                  boolean hasVisible = false;

                  for (BlockState blockStatex : blockSet) {
                     if (!blockStatex.isAir() && !(blockStatex.getBlock() instanceof LiquidBlock)) {
                        hasVisible = true;
                     } else {
                        hasInvisible = true;
                     }
                  }

                  if (hasInvisible) {
                     if (!this.cutout) {
                        ChunkRenderOverrider.acquire("Path Tool");
                        this.cutout = true;
                     }

                     PositionSet positionSet = new PositionSet();
                     if (hasVisible) {
                        this.chunkedBlockRegion.forEachEntry((xx, yx, zx, b) -> {
                           if (b.isAir() || b.getBlock() instanceof LiquidBlock) {
                              positionSet.add(xx, yx, zx);
                           }
                        });
                     } else {
                        this.chunkedBlockRegion.forEachEntry((xx, yx, zx, b) -> positionSet.add(xx, yx, zx));
                     }

                     ChunkRenderOverrider.cutoutBoolean(positionSet, BlockPos.ZERO);
                  } else if (this.cutout) {
                     ChunkRenderOverrider.release("Path Tool");
                     this.cutout = false;
                  }
               }
            }
         }
      } else {
         if (this.cutout) {
            ChunkRenderOverrider.release("Path Tool");
            this.cutout = false;
         }
      }
   }

   private static double atanh(double v) {
      return 0.5 * Math.log((1.0 + v) / (1.0 - v));
   }

   private static double findBigA(double r) {
      double bigA;
      if (r < 3.0) {
         bigA = Math.sqrt(6.0 * (r - 1.0));
      } else {
         bigA = Math.log(2.0 * r) + Math.log(Math.log(2.0 * r));
      }

      for (int i = 0; i < 5; i++) {
         bigA -= (Math.sinh(bigA) - r * bigA) / (Math.cosh(bigA) - r);
      }

      return bigA;
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.path.curve_category"));
      boolean changed = ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.path.curve_type"),
         this.curveType,
         new String[]{
            AxiomI18n.get("axiom.tool.path.curve_bresenham"),
            AxiomI18n.get("axiom.tool.path.curve_dda"),
            AxiomI18n.get("axiom.tool.path.curve_catenary"),
            AxiomI18n.get("axiom.tool.path.curve_catmull_rom"),
            AxiomI18n.get("axiom.tool.path.curve_bezier")
         }
      );
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.looped"), this.looped)) {
         this.looped = !this.looped;
         changed = true;
      }

      BlockState activeBlock = Tool.getActiveBlock();
      if (HDVoxelMap.getAssociatedBlocks(activeBlock.getBlock()) != null && this.shape[0] != 2) {
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.use_stairs_and_slabs"), this.useStairsAndSlabs)) {
            this.useStairsAndSlabs = !this.useStairsAndSlabs;
            changed = true;
         }
      } else if (this.useStairsAndSlabs) {
         this.useStairsAndSlabs = false;
         changed = true;
      }

      if (this.curveType[0] == 2) {
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.inverted"), this.inverted)) {
            this.inverted = !this.inverted;
            changed = true;
         }

         changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.path.slack"), this.slack, 0, 200, "%d%%");
      }

      if (this.curveType[0] != 0) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush_shape"));
         changed |= ImGuiHelper.combo(
            AxiomI18n.get("axiom.tool.generic.brush_shape"),
            this.shape,
            new String[]{AxiomI18n.get("axiom.tool.shape.sphere"), AxiomI18n.get("axiom.tool.path.flat"), "Custom (Clipboard)", "Spike"}
         );
         switch (this.shape[0]) {
            case 0:
               changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.path.radius"), this.radius, 0, 32);
               break;
            case 1:
               changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.path.depth"), this.depth, 0, 32);
               changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.path.radius"), this.radius, 0, 32);
            case 2:
            default:
               break;
            case 3:
               changed |= ImGui.sliderInt("Start Radius", this.radius, 0, 32);
               changed |= ImGui.sliderInt("End Radius", this.endRadius, 0, 32);
         }
      }

      int activePoint = this.gizmoList.getActiveGizmoIndex();
      if (activePoint >= 0 && activePoint < this.pointConfigs.size() && activePoint < this.gizmoList.size()) {
         PointConfig pointConfig = this.pointConfigs.get(activePoint);
         Gizmo gizmo = this.gizmoList.getGizmos().get(activePoint);
         BlockPos pos = gizmo.getTargetPosition();
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.ruler.point_i", activePoint + 1));
         int[] positionInput = new int[]{pos.getX(), pos.getY(), pos.getZ()};
         if (ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.box_select.position"), positionInput)) {
            gizmo.moveTo(new BlockPos(positionInput[0], positionInput[1], positionInput[2]));
            changed = true;
         }

         if (this.curveType[0] != 0) {
            if (activePoint < this.gizmoList.size() - 1 || this.gizmoList.size() >= 3 && this.looped) {
               changed |= ImGuiHelper.combo(
                  AxiomI18n.get("axiom.tool.path.easing"),
                  pointConfig.easing,
                  new String[]{
                     AxiomI18n.get("axiom.tool.path.easing_linear"),
                     AxiomI18n.get("axiom.tool.path.easing_slight"),
                     AxiomI18n.get("axiom.tool.path.easing_quadratic"),
                     AxiomI18n.get("axiom.tool.path.easing_cubic"),
                     AxiomI18n.get("axiom.tool.path.easing_quartic")
                  }
               );
               if (pointConfig.easing[0] != 0) {
                  changed |= ImGuiHelper.combo(
                     AxiomI18n.get("axiom.tool.path.easing_type"),
                     pointConfig.easingType,
                     new String[]{
                        AxiomI18n.get("axiom.tool.path.easing_type_easein"),
                        AxiomI18n.get("axiom.tool.path.easing_type_easeout"),
                        AxiomI18n.get("axiom.tool.path.easing_type_easeinout")
                     }
                  );
               }
            }

            if (this.shape[0] != 2) {
               if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.override_block"), pointConfig.overrideBlock)) {
                  pointConfig.overrideBlock = !pointConfig.overrideBlock;
                  changed = true;
               }

               if (this.shape[0] != 3) {
                  ImGui.sameLine();
                  if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.override_radius"), pointConfig.overrideRadius)) {
                     pointConfig.overrideRadius = !pointConfig.overrideRadius;
                     changed = true;
                  }
               }

               if (pointConfig.overrideBlock) {
                  CustomBlockState newBlock = ImGuiHelper.blockStateWidget(this.selectBlockWidget, pointConfig.block, null, 0);
                  if (newBlock != pointConfig.block) {
                     pointConfig.block = newBlock;
                     changed = true;
                  }
               }

               if (pointConfig.overrideRadius) {
                  changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.path.radius") + "##Point", pointConfig.radius, 0, 32);
               }
            }
         }

         if (ImGui.button(AxiomI18n.get("axiom.tool.path.remove"))) {
            this.gizmoList.delete();
            changed = true;
         }
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.paste_options"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.keep_existing"), this.keepExisting)) {
         this.keepExisting = !this.keepExisting;
         changed = true;
      }

      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.extend_to_ground"), this.extendToGround)) {
         this.extendToGround = !this.extendToGround;
         changed = true;
      }

      if (ImGui.button(AxiomI18n.get("axiom.hardcoded.paste_copy"))) {
         this.pastePath();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
      if (this.gizmoList.hasActiveGizmo()) {
         ImGui.separator();
         ImGui.text(AxiomI18n.get("axiom.tool.shape.extrude_tip", Keybinds.EXTRUDE_POINT.longKeyIdentifier()));
      }

      this.recalculate |= changed;
   }

   @Override
   public String listenForEsc() {
      return this.gizmoList.hasActiveGizmo() ? AxiomI18n.get("axiom.tool.path.deselect_point_n", this.gizmoList.getActiveGizmoIndex() + 1) : null;
   }

   @Override
   public String listenForEnter() {
      return !this.chunkedBlockRegion.isEmpty() ? AxiomI18n.get("axiom.widget.confirm") : null;
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.path");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putInt("CurveType", this.curveType[0]);
      tag.putBoolean("Looped", this.looped);
      tag.putInt("Radius", this.radius[0]);
      tag.putInt("EndRadius", this.endRadius[0]);
      tag.putBoolean("CatenaryInverted", this.inverted);
      tag.putInt("CatenarySlack", this.slack[0]);
      tag.putBoolean("KeepExisting", this.keepExisting);
      tag.putBoolean("ExtendToGround", this.extendToGround);
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         BlockPos pos = player.blockPosition();
         tag.putInt("PlayerPosX", pos.getX());
         tag.putInt("PlayerPosY", pos.getY());
         tag.putInt("PlayerPosZ", pos.getZ());
      }

      ListTag pathPointsTag = new ListTag();

      for (int i = 0; i < this.gizmoList.size(); i++) {
         BlockPos position = this.gizmoList.getGizmos().get(i).getTargetPosition();
         PointConfig pointConfig = this.pointConfigs.get(i);
         CompoundTag pathPoint = new CompoundTag();
         pathPoint.putInt("X", position.getX());
         pathPoint.putInt("Y", position.getY());
         pathPoint.putInt("Z", position.getZ());
         pathPoint.putBoolean("OverrideBlock", pointConfig.overrideBlock);
         pathPoint.putBoolean("OverrideRadius", pointConfig.overrideRadius);
         if (pointConfig.overrideBlock) {
            pathPoint.putString("Block", ServerCustomBlocks.serialize(pointConfig.block));
         }

         if (pointConfig.overrideRadius) {
            pathPoint.putInt("Radius", pointConfig.radius[0]);
         }

         pathPoint.putInt("Easing", pointConfig.easing[0]);
         pathPoint.putInt("EasingType", pointConfig.easingType[0]);
         pathPointsTag.add(pathPoint);
      }

      tag.put("PathPoints", pathPointsTag);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.curveType[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "CurveType", 1);
      this.looped = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "Looped", false);
      this.radius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Radius", 0);
      this.endRadius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "EndRadius", 0);
      this.inverted = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "CatenaryInverted", false);
      this.slack[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "CatenarySlack", 20);
      this.keepExisting = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "KeepExisting", false);
      this.extendToGround = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "ExtendToGround", false);
      BlockPos offset = BlockPos.ZERO;
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         if (tag.contains("PlayerPosX")) {
            offset = new BlockPos(player.getBlockX() - VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "PlayerPosX", 0), offset.getY(), offset.getZ());
         }

         if (tag.contains("PlayerPosY")) {
            offset = new BlockPos(offset.getX(), player.getBlockY() - VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "PlayerPosY", 0), offset.getZ());
         }

         if (tag.contains("PlayerPosZ")) {
            offset = new BlockPos(offset.getX(), offset.getY(), player.getBlockZ() - VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "PlayerPosZ", 0));
         }
      }

      this.gizmoList = new GizmoList();
      this.pointConfigs.clear();

      for (Tag pathPointTag : NbtHelper.getList(tag, "PathPoints", 10)) {
         CompoundTag pathPoint = (CompoundTag)pathPointTag;
         int x = VersionUtilsNbt.helperCompoundTagGetIntOr(pathPoint, "X", 0) + offset.getX();
         int y = VersionUtilsNbt.helperCompoundTagGetIntOr(pathPoint, "Y", 0) + offset.getY();
         int z = VersionUtilsNbt.helperCompoundTagGetIntOr(pathPoint, "Z", 0) + offset.getZ();
         boolean overrideBlock = VersionUtilsNbt.helperCompoundTagGetBooleanOr(pathPoint, "OverrideBlock", false);
         boolean overrideRadius = VersionUtilsNbt.helperCompoundTagGetBooleanOr(pathPoint, "OverrideRadius", false);
         CustomBlockState customBlockState = (CustomBlockState)Blocks.STONE.defaultBlockState();
         if (overrideBlock) {
            String blockString = VersionUtilsNbt.helperCompoundTagGetStringOr(pathPoint, "Block", "");
            if (!blockString.isEmpty()) {
               customBlockState = Objects.requireNonNullElse(ServerCustomBlocks.deserialize(blockString), customBlockState);
            }
         }

         int radius = this.radius[0];
         if (overrideRadius) {
            radius = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Radius", radius);
         }

         int easing = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Easing", 0);
         int easingType = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "EasingType", 0);
         this.gizmoList.addGizmo(Vec3.atCenterOf(new BlockPos(x, y, z)));
         this.pointConfigs.add(new PointConfig(overrideBlock, overrideRadius, customBlockState, radius, easing, easingType));
      }

      this.recalculate = true;
   }

   @Override
   public char iconChar() {
      return '\ue900';
   }

   @Override
   public String keybindId() {
      return "path";
   }

   @Override
   public HistoryBuffer<?> historyBuffer() {
      return this.gizmoList.getHistoryBuffer();
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_PATH, AxiomPermission.BUILD_SECTION);
   }
}
