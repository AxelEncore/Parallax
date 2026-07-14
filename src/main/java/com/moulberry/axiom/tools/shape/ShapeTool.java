package com.moulberry.axiom.tools.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.editor.GenericRadiusAdjustment;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.mask.elements.ConstantMaskElement;
import com.moulberry.axiom.rasterization.CapsuleRasterization;
import com.moulberry.axiom.rasterization.ConeRasterization;
import com.moulberry.axiom.rasterization.CuboidRasterization;
import com.moulberry.axiom.rasterization.CylinderRasterization;
import com.moulberry.axiom.rasterization.DiskRasterization;
import com.moulberry.axiom.rasterization.DodecahedronRasterization;
import com.moulberry.axiom.rasterization.IcosahedronRasterization;
import com.moulberry.axiom.rasterization.OctahedronRasterization;
import com.moulberry.axiom.rasterization.PlaneRasterization;
import com.moulberry.axiom.rasterization.PyramidRasterization;
import com.moulberry.axiom.rasterization.RegularPolygonRasterization;
import com.moulberry.axiom.rasterization.SphereRasterization;
import com.moulberry.axiom.rasterization.SpiralRasterization;
import com.moulberry.axiom.rasterization.SupercylinderRasterization;
import com.moulberry.axiom.rasterization.SuperellipseRasterization;
import com.moulberry.axiom.rasterization.SupersphereRasterization;
import com.moulberry.axiom.rasterization.TorusRasterization;
import com.moulberry.axiom.rasterization.TubeRasterization;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.utils.RenderHelper;
import com.moulberry.axiom.world_modification.HistoryEntry;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class ShapeTool implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private Gizmo gizmo = null;
   private boolean cutout = false;
   private boolean recalculate = false;
   private BlockState lastActiveBlock = null;
   private boolean maskWindowOpen = false;
   private boolean reorientPoint = false;
   private boolean usingRealCoordinates = false;
   private boolean decimalSizes = false;
   private boolean centerEvenDiameters = true;
   private static final int SHAPE_CATEGORY_SOLID = 0;
   private static final int SHAPE_CATEGORY_PLANE = 1;
   private final int[] shapeCategory = new int[]{0};
   private static final int SHAPE_SOLID_SPHERE = 0;
   private static final int SHAPE_SOLID_CUBOID = 1;
   private static final int SHAPE_SOLID_OCTAHEDRON = 2;
   private static final int SHAPE_SOLID_CYLINDER = 3;
   private static final int SHAPE_SOLID_SUPERSPHERE = 4;
   private static final int SHAPE_SOLID_SUPERCYLINDER = 5;
   private static final int SHAPE_SOLID_CAPSULE = 6;
   private static final int SHAPE_SOLID_TUBE = 7;
   private static final int SHAPE_SOLID_CONE = 8;
   private static final int SHAPE_SOLID_PYRAMID = 9;
   private static final int SHAPE_SOLID_TORUS = 10;
   private static final int SHAPE_SOLID_DODECAHEDRON = 11;
   private static final int SHAPE_SOLID_ICOSAHEDRON = 12;
   private final int[] shapeSolid = new int[]{0};
   private static final int SHAPE_PLANE_DISK = 0;
   private static final int SHAPE_PLANE_PLANE = 1;
   private static final int SHAPE_PLANE_REGULAR_POLYGON = 2;
   private static final int SHAPE_PLANE_SUPERELLIPSE = 3;
   private static final int SHAPE_PLANE_ARCHIMEDEAN_SPIRAL = 4;
   private final int[] shapePlane = new int[]{0};
   private boolean separateXYZ = false;
   private final float[] sizeXYZ = new float[]{5.0F};
   private final float[] sizeX = new float[]{5.0F};
   private final float[] sizeY = new float[]{5.0F};
   private final float[] sizeZ = new float[]{5.0F};
   private final GenericRadiusAdjustment radiusAdjustment = new GenericRadiusAdjustment(64);
   private final float[] rotationX = new float[]{0.0F};
   private final float[] rotationY = new float[]{0.0F};
   private final float[] rotationZ = new float[]{0.0F};
   private final Quaternionf rotationQuaternion = new Quaternionf();
   private boolean hollow = false;
   private boolean metaball = false;
   private final int[] metaballRange = new int[]{8};
   private boolean cutoff = false;
   private final int[] cutoffAmount = new int[]{50};
   private final int[] polygonSides = new int[]{3};
   private final float[] thickness = new float[]{1.0F};
   private boolean separateHV = false;
   private final float[] ringH = new float[]{2.0F};
   private final float[] ringV = new float[]{2.0F};
   private final float[] exponent = new float[]{2.0F};
   private final float[] capHeight = new float[]{1.0F};
   private final float[] loops = new float[]{5.0F};
   private final float[] coneRounding = new float[]{0.0F};
   private boolean offsetWhenPlacing = true;
   private static final int GIZMO_SPACE_GLOBAL = 0;
   private static final int GIZMO_SPACE_LOCAL = 1;
   private final int[] gizmoSpace = new int[]{0};
   private static final int PIVOT_CENTER = 0;
   private static final int PIVOT_BASE = 1;
   private final int[] pivot = new int[]{0};
   private static final int PLACE_ROTATION_KEEP = 0;
   private static final int PLACE_ROTATION_AUTO = 1;
   private static final int PLACE_ROTATION_RESET = 2;
   private final int[] placeRotation = new int[]{0};
   private boolean instantPlace = false;
   private boolean keepExisting = false;
   private boolean extendToGround = false;

   @Override
   public void reset() {
      this.gizmo = null;
      this.chunkedBlockRegion.clear();
      this.recalculate = true;
      if (this.cutout) {
         ChunkRenderOverrider.release("Shape Tool");
         this.cutout = false;
      }
   }

   public void markDirty() {
      this.recalculate = true;
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case ENTER:
            if (!this.chunkedBlockRegion.isEmpty() && this.gizmo != null) {
               this.pasteShape(false);
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }

            return UserAction.ActionResult.NOT_HANDLED;
         case RIGHT_MOUSE:
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
            if (result != null) {
               int placeRotationMode = this.getPlaceRotation();
               if (placeRotationMode == 2) {
                  this.rotationX[0] = this.rotationY[0] = this.rotationZ[0] = 0.0F;
                  this.rotationQuaternion.identity();
               } else if (placeRotationMode == 1) {
                  this.setRotationAuto(result.blockPos());
               }

               BlockPos blockPos = result.blockPos();
               if (this.offsetWhenPlacing) {
                  blockPos = blockPos.relative(result.direction());
               }

               if (this.gizmo != null) {
                  this.gizmo.moveTo(blockPos);
                  this.gizmo.enableAxes = true;
               } else {
                  this.gizmo = new Gizmo(blockPos);
               }

               this.reorientPoint = true;
               this.recalculate = true;
               if (this.instantPlace) {
                  this.pasteShape(false);
                  this.reset();
               }
            }

            return UserAction.ActionResult.USED_STOP;
         case LEFT_MOUSE:
            if (this.gizmo != null && this.gizmo.leftClick() && !this.instantPlace) {
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case ESCAPE:
            if (this.gizmo != null && this.gizmo.enableAxes && !this.instantPlace) {
               this.gizmo.enableAxes = false;
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case DELETE:
            this.reset();
            return UserAction.ActionResult.USED_STOP;
         case SCROLL:
            UserAction.ScrollAmount scrollAmount = (UserAction.ScrollAmount)object;
            if (this.handleScroll(scrollAmount.scrollX(), scrollAmount.scrollY())) {
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case UNDO:
            if (this.gizmo != null && !this.instantPlace) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   public boolean handleScroll(int xScroll, int yScroll) {
      if (this.gizmo != null) {
         Vec3 look = Tool.getLookDirection();
         this.gizmo.handleScroll(xScroll, yScroll, EditorUI.isCtrlOrCmdDown(), look);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      Selection.render(rc, 4);
      Vec3 lookDirection = Tool.getLookDirection();
      boolean isLeftDown = Tool.isMouseDown(0);
      boolean isCtrlDown = EditorUI.isCtrlOrCmdDown();
      boolean showGizmo = !EditorUI.isActive() || !isCtrlDown;
      boolean renderGizmos = showGizmo;
      if (Tool.isMouseDown(1) && this.gizmo != null && this.gizmo.enableAxes && this.reorientPoint) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
         if (result != null) {
            BlockPos blockPos = result.blockPos();
            if (this.offsetWhenPlacing) {
               blockPos = blockPos.relative(result.direction());
            }

            if (!this.gizmo.getTargetPosition().equals(blockPos)) {
               int placeRotationMode = this.getPlaceRotation();
               if (placeRotationMode == 2) {
                  this.rotationX[0] = this.rotationY[0] = this.rotationZ[0] = 0.0F;
                  this.rotationQuaternion.identity();
               } else if (placeRotationMode == 1) {
                  this.setRotationAuto(result.blockPos());
               }

               this.gizmo.moveTo(blockPos);
               this.gizmo.enableAxes = true;
               this.recalculate = true;
            }
         }
      } else {
         this.reorientPoint = false;
      }

      boolean hasGrabbedGizmo = false;
      if (this.instantPlace) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
         if (result != null) {
            BlockPos blockPosx = result.blockPos();
            if (this.offsetWhenPlacing) {
               blockPosx = blockPosx.relative(result.direction());
            }

            if (this.gizmo != null) {
               if (!this.gizmo.getTargetPosition().equals(blockPosx)) {
                  this.gizmo.moveToInstantly(blockPosx);
                  this.recalculate = this.recalculate | this.usingRealCoordinates;
               }
            } else {
               this.gizmo = new Gizmo(blockPosx);
               this.recalculate = true;
            }

            this.gizmo.localRotation = null;
         } else {
            this.reset();
         }
      }

      if (this.gizmo != null && !this.instantPlace) {
         this.gizmo.enableRotation = true;
         this.gizmo.rotationSnapRadians = (float) (Math.PI / 180.0);
         if (this.getGizmoSpace() == 1) {
            this.gizmo.localRotation = new Quaternionf(this.rotationQuaternion).invert();
         } else {
            this.gizmo.localRotation = null;
         }

         BlockPos before = this.gizmo.getTargetPosition();
         this.gizmo.update(rc.nanos(), lookDirection, isLeftDown, isCtrlDown, showGizmo);
         if (this.getGizmoSpace() == 0) {
            this.gizmo
               .setAxisDirections(
                  rc.x() > this.gizmo.getTargetPosition().getX(),
                  rc.y() > this.gizmo.getTargetPosition().getY(),
                  rc.z() > this.gizmo.getTargetPosition().getZ()
               );
         } else {
            this.gizmo.setAxisDirections(true, true, true);
         }

         if (this.gizmo.isGrabbed()) {
            renderGizmos = true;
            hasGrabbedGizmo = true;
         }

         Gizmo.GizmoRotation rotation = this.gizmo.popRotation();
         if (rotation != null) {
            this.performRotation(rotation.axis(), rotation.radians());
            this.recalculate = true;
         }

         if (this.usingRealCoordinates && !this.gizmo.getTargetPosition().equals(before)) {
            this.recalculate = true;
         }
      }

      if (!hasGrabbedGizmo && this.gizmo == null) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
         if (result != null) {
            BlockPos blockPosxx = result.blockPos();
            if (this.offsetWhenPlacing) {
               blockPosxx = blockPosxx.relative(result.direction());
            }

            Tool.renderRaycastOverlay(rc, blockPosxx);
         }
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

      if (this.gizmo != null) {
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         if (this.usingRealCoordinates) {
            this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
            this.renderShapeBounds(rc);
         } else if (this.gizmoSpace[0] == 1) {
            Gizmo.GizmoRotation gizmoRotation = this.gizmo.peekGizmoRotation();
            Quaternionf rotationx;
            if (gizmoRotation != null) {
               rotationx = new Quaternionf(this.rotationQuaternion).invert();
               switch (gizmoRotation.axis()) {
                  case X:
                     rotationx.rotateX(gizmoRotation.radians());
                     break;
                  case Y:
                     rotationx.rotateY(gizmoRotation.radians());
                     break;
                  case Z:
                     rotationx.rotateZ(gizmoRotation.radians());
               }

               rotationx.mul(this.rotationQuaternion);
            } else {
               rotationx = null;
            }

            Vector3f offset = new Vector3f(-0.5F, -0.5F, -0.5F);
            if (rotationx != null) {
               rotationx.transform(offset);
            }

            this.chunkedBlockRegion
               .render(rc, this.gizmo.getInterpPosition().add(offset.x, offset.y, offset.z), rotationx, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
            this.renderShapeBounds(rc);
         } else {
            Quaternionf rotationxx = this.gizmo.peekRotation();
            Vector3f offset = new Vector3f(-0.5F, -0.5F, -0.5F);
            if (rotationxx != null) {
               rotationxx.transform(offset);
            }

            this.chunkedBlockRegion
               .render(rc, this.gizmo.getInterpPosition().add(offset.x, offset.y, offset.z), rotationxx, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
            this.renderShapeBounds(rc);
         }

         if (renderGizmos && this.gizmo != null && !this.instantPlace) {
            this.gizmo.render(rc, isCtrlDown);
         }
      }
   }

   private void renderShapeBounds(AxiomWorldRenderContext rc) {
      switch (this.shapeCategory[0]) {
         case 0:
            float diameterX = this.sizeX[0];
            float diameterY = this.sizeY[0];
            float diameterZ = this.sizeZ[0];
            if (!this.separateXYZ) {
               diameterX = diameterY = diameterZ = this.sizeXYZ[0];
            }

            Quaternionf rotation;
            if (this.gizmoSpace[0] == 0) {
               rotation = this.gizmo.peekRotation();
            } else {
               if (this.gizmoSpace[0] != 1) {
                  throw new UnsupportedOperationException();
               }

               Gizmo.GizmoRotation gizmoRotation = this.gizmo.peekGizmoRotation();
               if (gizmoRotation != null) {
                  rotation = new Quaternionf(this.rotationQuaternion).invert();
                  switch (gizmoRotation.axis()) {
                     case X:
                        rotation.rotateX(gizmoRotation.radians());
                        break;
                     case Y:
                        rotation.rotateY(gizmoRotation.radians());
                        break;
                     case Z:
                        rotation.rotateZ(gizmoRotation.radians());
                  }

                  rotation.mul(this.rotationQuaternion);
               } else {
                  rotation = null;
               }
            }

            Vec3 center = this.gizmo.getInterpPosition();
            PoseStack matrices = rc.poseStack();
            matrices.pushPose();
            matrices.translate(-rc.x() + center.x - 0.5, -rc.y() + center.y - 0.5, -rc.z() + center.z - 0.5);
            if (rotation != null) {
               matrices.mulPose(rotation);
            }

            matrices.mulPose(new Quaternionf(this.rotationQuaternion).invert());
            switch (this.shapeSolid[0]) {
               case 0:
               case 1:
               case 2:
               case 4:
               case 11:
               case 12: {
                  BlockPos pivot = this.calculateRawPivot(diameterY);
                  matrices.translate(pivot.getX(), pivot.getY(), pivot.getZ());
                  this.renderBoundingBox(matrices, diameterX, diameterY, diameterZ);
                  break;
               }
               case 3:
               case 5:
               case 6:
               case 7:
               case 8:
               case 9: {
                  BlockPos pivot = this.calculateRawPivot(this.sizeY[0]);
                  matrices.translate(pivot.getX(), pivot.getY(), pivot.getZ());
                  this.renderBoundingBox(matrices, diameterX, this.sizeY[0], diameterZ);
                  break;
               }
               case 10: {
                  BlockPos pivot = this.calculateRawPivot(this.ringV[0]);
                  matrices.translate(pivot.getX(), pivot.getY(), pivot.getZ());
                  this.renderBoundingBox(matrices, diameterX, this.ringV[0], diameterZ);
                  break;
               }
               default:
                  throw new FaultyImplementationError();
            }

            matrices.popPose();
      }
   }

   private void renderBoundingBox(PoseStack matrices, float sizeX, float sizeY, float sizeZ) {
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      float offsetX = 1.0F - sizeX / 2.0F % 1.0F;
      float offsetY = 1.0F - sizeY / 2.0F % 1.0F;
      float offsetZ = 1.0F - sizeZ / 2.0F % 1.0F;
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      AxiomRenderer.setLineWidthLegacy(RenderHelper.baseLineWidth);
      Shapes.lineBox(
         matrices,
         bufferBuilder,
         -sizeX / 2.0F + offsetX,
         -sizeY / 2.0F + offsetY,
         -sizeZ / 2.0F + offsetZ,
         sizeX / 2.0F + offsetX,
         sizeY / 2.0F + offsetY,
         sizeZ / 2.0F + offsetZ,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         1.0F,
         RenderHelper.baseLineWidth
      );
      MeshData meshData = provider.build();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.4F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH, null, meshData, false);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 0.1F);
      AxiomRenderer.renderPipeline(AxiomRenderPipelines.LINES_IGNORE_DEPTH, null, meshData, true);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private void performRotation(Axis axis, float radians) {
      Quaternionf rotation = new Quaternionf();
      rotation.rotateYXZ((float)Math.toRadians(this.rotationY[0]), (float)Math.toRadians(this.rotationX[0]), (float)Math.toRadians(this.rotationZ[0]));
      if (this.getGizmoSpace() == 1) {
         switch (axis) {
            case X:
               rotation.rotateLocalX(-radians);
               break;
            case Y:
               rotation.rotateLocalY(-radians);
               break;
            case Z:
               rotation.rotateLocalZ(-radians);
         }
      } else {
         switch (axis) {
            case X:
               rotation.rotateX(-radians);
               break;
            case Y:
               rotation.rotateY(-radians);
               break;
            case Z:
               rotation.rotateZ(-radians);
         }
      }

      Vector3f euler = rotation.getEulerAnglesYXZ(new Vector3f());
      this.rotationX[0] = (float)Math.round(Math.toDegrees(euler.x));
      this.rotationY[0] = (float)Math.round(Math.toDegrees(euler.y));
      this.rotationZ[0] = (float)Math.round(Math.toDegrees(euler.z));
      this.rotationQuaternion.identity();
      this.rotationQuaternion
         .rotateYXZ((float)Math.toRadians(this.rotationY[0]), (float)Math.toRadians(this.rotationX[0]), (float)Math.toRadians(this.rotationZ[0]));
   }

   public void pasteShape(boolean select) {
      this.recalculate();
      if (!this.chunkedBlockRegion.isEmpty() && this.gizmo != null) {
         Level level = Minecraft.getInstance().level;
         if (this.extendToGround && level != null) {
            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            Position2ObjectMap<BlockState> newBlocks = new Position2ObjectMap<>(k -> new BlockState[4096]);
            BlockPos offset = this.usingRealCoordinates ? BlockPos.ZERO : this.gizmo.getTargetPosition();
            this.chunkedBlockRegion.forEachEntry((x, y, z, block) -> {
               if (!block.isAir()) {
                  for (int yo = 1; yo < 256 && this.chunkedBlockRegion.getBlockStateOrAir(x, y - yo, z).isAir(); yo++) {
                     BlockState below = level.getBlockState(mutableBlockPos.set(x + offset.getX(), y - yo + offset.getY(), z + offset.getZ()));
                     if (below.getBlock() == Blocks.VOID_AIR || !below.canBeReplaced()) {
                        break;
                     }

                     newBlocks.put(x, y - yo, z, block);
                  }
               }
            });
            newBlocks.forEachEntry(this.chunkedBlockRegion::addBlock);
         }

         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.placed", countString);
         int modifiers = this.keepExisting ? HistoryEntry.MODIFIER_KEEP_EXISTING : 0;
         MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
         MaskElement maskElement = MaskManager.getDestMask();
         if (maskElement instanceof ConstantMaskElement constantMaskElement) {
            if (constantMaskElement.getConstant()) {
               if (this.usingRealCoordinates) {
                  if (select) {
                     PositionSet selection = new PositionSet();
                     this.chunkedBlockRegion.forEachEntry((x, y, z, blockState) -> {
                        if (!blockState.isAir()) {
                           selection.add(x, y, z);
                        }
                     });
                     Selection.clearSelection();
                     Selection.addSet(selection);
                  }

                  RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription, modifiers);
               } else {
                  BlockPos offset = this.gizmo.getTargetPosition();
                  if (select) {
                     PositionSet selection = new PositionSet();
                     this.chunkedBlockRegion.forEachEntry((x, y, z, blockState) -> {
                        if (!blockState.isAir()) {
                           selection.add(x + offset.getX(), y + offset.getY(), z + offset.getZ());
                        }
                     });
                     Selection.clearSelection();
                     Selection.addSet(selection);
                  }

                  RegionHelper.pushBlockRegionChangeOffset(this.chunkedBlockRegion, offset, historyDescription, modifiers);
               }
            }
         } else {
            ChunkedBlockRegion newChunkedBlockRegion = new ChunkedBlockRegion();
            PositionSet selection = select ? new PositionSet() : null;
            if (this.usingRealCoordinates) {
               this.chunkedBlockRegion.forEachEntry((x, y, z, block) -> {
                  if (maskElement.test(maskContext.reset(), x, y, z)) {
                     newChunkedBlockRegion.addBlockWithoutDirty(x, y, z, block);
                     if (selection != null && !block.isAir()) {
                        selection.add(x, y, z);
                     }
                  }
               });
            } else {
               BlockPos target = this.gizmo.getTargetPosition();
               int offsetX = target.getX();
               int offsetY = target.getY();
               int offsetZ = target.getZ();
               this.chunkedBlockRegion.forEachEntry((x, y, z, block) -> {
                  if (maskElement.test(maskContext.reset(), x + offsetX, y + offsetY, z + offsetZ)) {
                     newChunkedBlockRegion.addBlockWithoutDirty(x + offsetX, y + offsetY, z + offsetZ, block);
                     if (selection != null && !block.isAir()) {
                        selection.add(offsetX, y + offsetY, z + offsetZ);
                     }
                  }
               });
            }

            if (selection != null) {
               Selection.clearSelection();
               Selection.addSet(selection);
            }

            RegionHelper.pushBlockRegionChange(newChunkedBlockRegion, historyDescription, modifiers);
         }
      }
   }

   private void recalculate() {
      this.recalculate = false;
      this.chunkedBlockRegion.clear();
      if (this.gizmo == null) {
         if (this.cutout) {
            ChunkRenderOverrider.release("Shape Tool");
            this.cutout = false;
         }
      } else {
         BlockState block = Tool.getActiveBlock();
         boolean centerEvenDiameters = this.centerEvenDiameters && !this.decimalSizes;
         switch (this.shapeCategory[0]) {
            case 0:
               BlockPos center = this.gizmo.getTargetPosition();
               float diameterX = this.sizeX[0];
               float diameterY = this.sizeY[0];
               float diameterZ = this.sizeZ[0];
               if (!this.separateXYZ) {
                  diameterX = diameterY = diameterZ = this.sizeXYZ[0];
               }

               float cutoffY = !this.metaball && this.cutoff && this.cutoffAmount[0] > 0
                  ? diameterY * (this.cutoffAmount[0] / 100.0F - 0.5F)
                  : Float.NEGATIVE_INFINITY;
               switch (this.shapeSolid[0]) {
                  case 0:
                     if (this.metaball) {
                        SphereRasterization.sphereMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     } else {
                        SphereRasterization.sphere(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(diameterY),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           cutoffY,
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 1:
                     if (this.metaball) {
                        CuboidRasterization.cuboidMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     } else {
                        CuboidRasterization.cuboid(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(diameterY),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 2:
                     if (this.metaball) {
                        OctahedronRasterization.octahedronMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     } else {
                        OctahedronRasterization.octahedron(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(diameterY),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           cutoffY,
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 3:
                     if (this.metaball) {
                        CylinderRasterization.cylinderMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(this.sizeY[0])),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     } else {
                        CylinderRasterization.cylinder(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(this.sizeY[0]),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 4:
                     if (this.metaball) {
                        SupersphereRasterization.supersphereMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.exponent[0],
                           this.rotationQuaternion
                        );
                     } else {
                        SupersphereRasterization.supersphere(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(diameterY),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           cutoffY,
                           this.exponent[0],
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 5:
                     if (this.metaball) {
                        SupercylinderRasterization.supercylinderMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.exponent[0],
                           this.rotationQuaternion
                        );
                     } else {
                        SupercylinderRasterization.supercylinder(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(this.sizeY[0]),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           cutoffY,
                           this.exponent[0],
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 6:
                     if (this.metaball) {
                        CapsuleRasterization.capsuleMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.capHeight[0],
                           this.rotationQuaternion
                        );
                     } else {
                        CapsuleRasterization.capsule(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(this.sizeY[0]),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.capHeight[0],
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 7:
                     if (this.metaball) {
                        TubeRasterization.tubeMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(this.sizeY[0])),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.thickness[0],
                           this.rotationQuaternion
                        );
                     } else {
                        TubeRasterization.tube(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(this.sizeY[0]),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.thickness[0],
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 8:
                     if (this.metaball) {
                        ConeRasterization.coneMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(this.sizeY[0])),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.coneRounding[0],
                           this.rotationQuaternion
                        );
                     } else {
                        ConeRasterization.cone(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(this.sizeY[0]),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.coneRounding[0],
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 9:
                     if (this.metaball) {
                        PyramidRasterization.pyramidMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(this.sizeY[0])),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     } else {
                        PyramidRasterization.pyramid(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(this.sizeY[0]),
                           diameterX,
                           this.sizeY[0],
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 10:
                     if (this.metaball) {
                        TorusRasterization.torusMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(this.ringV[0])),
                           diameterX,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.ringH[0],
                           this.ringV[0],
                           this.rotationQuaternion
                        );
                     } else {
                        float torusCutoffY = this.cutoff && this.cutoffAmount[0] > 0
                           ? this.ringV[0] * (this.cutoffAmount[0] / 100.0F - 0.5F)
                           : Float.NEGATIVE_INFINITY;
                        TorusRasterization.torus(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(this.ringV[0]),
                           diameterX,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           torusCutoffY,
                           this.ringH[0],
                           this.ringV[0],
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 11:
                     if (this.metaball) {
                        DodecahedronRasterization.dodhecahedronMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     } else {
                        DodecahedronRasterization.dodhecahedron(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(diameterY),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           cutoffY,
                           this.rotationQuaternion
                        );
                     }

                     return;
                  case 12:
                     if (this.metaball) {
                        IcosahedronRasterization.icosahedronMetaball(
                           this.chunkedBlockRegion,
                           this.metaballRange[0] * 2,
                           block,
                           center.offset(this.calculatePivot(diameterY)),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           this.rotationQuaternion
                        );
                     } else {
                        IcosahedronRasterization.icosahedron(
                           this.chunkedBlockRegion,
                           block,
                           this.calculatePivot(diameterY),
                           diameterX,
                           diameterY,
                           diameterZ,
                           this.hollow,
                           centerEvenDiameters,
                           cutoffY,
                           this.rotationQuaternion
                        );
                     }

                     return;
                  default:
                     throw new FaultyImplementationError();
               }
            case 1:
               float sizeX = this.sizeX[0];
               float sizeZ = this.sizeZ[0];
               if (!this.separateXYZ) {
                  sizeX = sizeZ = this.sizeXYZ[0];
               }

               switch (this.shapePlane[0]) {
                  case 0:
                     DiskRasterization.disk(
                        new Vector3i(0, 0, 0),
                        sizeX,
                        sizeZ,
                        this.hollow,
                        centerEvenDiameters,
                        this.rotationQuaternion,
                        (x, y, z) -> this.chunkedBlockRegion.addBlock(x, y, z, block)
                     );
                     break;
                  case 1:
                     PlaneRasterization.plane(
                        new Vector3i(0, 0, 0),
                        sizeX,
                        sizeZ,
                        this.hollow,
                        centerEvenDiameters,
                        this.rotationQuaternion,
                        (x, y, z) -> this.chunkedBlockRegion.addBlock(x, y, z, block)
                     );
                     break;
                  case 2:
                     RegularPolygonRasterization.regularPolygon(
                        new Vector3i(0, 0, 0),
                        sizeX,
                        sizeZ,
                        this.hollow,
                        centerEvenDiameters,
                        this.polygonSides[0],
                        this.rotationQuaternion,
                        (x, y, z) -> this.chunkedBlockRegion.addBlock(x, y, z, block)
                     );
                     break;
                  case 3:
                     SuperellipseRasterization.superellipse(
                        new Vector3i(0, 0, 0),
                        sizeX,
                        sizeZ,
                        this.hollow,
                        centerEvenDiameters,
                        this.exponent[0],
                        this.rotationQuaternion,
                        (x, y, z) -> this.chunkedBlockRegion.addBlock(x, y, z, block)
                     );
                     break;
                  case 4:
                     SpiralRasterization.archimedean(
                        new Vector3i(0, 0, 0),
                        sizeX,
                        sizeZ,
                        centerEvenDiameters,
                        this.loops[0],
                        this.rotationQuaternion,
                        (x, y, z) -> this.chunkedBlockRegion.addBlock(x, y, z, block)
                     );
               }
         }
      }
   }

   @Override
   public void displayImguiOptions() {
      boolean changed;
      this.usingRealCoordinates = true;
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape"));
      changed = ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.shape.shape_category"),
         this.shapeCategory,
         new String[]{AxiomI18n.get("axiom.tool.shape.shape_category.solid"), AxiomI18n.get("axiom.tool.shape.shape_category.flat")}
      );
      String sizeFormatting = this.decimalSizes ? "%.2f" : "%.0f";
      label94:
      switch (this.shapeCategory[0]) {
         case 0:
            changed |= ImGuiHelper.combo(
               AxiomI18n.get("axiom.tool.shape"),
               this.shapeSolid,
               new String[]{
                  AxiomI18n.get("axiom.tool.shape.sphere"),
                  AxiomI18n.get("axiom.tool.shape.cuboid"),
                  AxiomI18n.get("axiom.tool.shape.octahedron"),
                  AxiomI18n.get("axiom.tool.shape.cylinder"),
                  AxiomI18n.get("axiom.tool.shape.supersphere"),
                  AxiomI18n.get("axiom.tool.shape.supercylinder"),
                  AxiomI18n.get("axiom.tool.shape.capsule"),
                  AxiomI18n.get("axiom.tool.shape.tube"),
                  AxiomI18n.get("axiom.tool.shape.cone"),
                  AxiomI18n.get("axiom.tool.shape.pyramid"),
                  AxiomI18n.get("axiom.tool.shape.torus"),
                  AxiomI18n.get("axiom.tool.shape.dodecahedron"),
                  AxiomI18n.get("axiom.tool.shape.icosahedron")
               }
            );
            switch (this.shapeSolid[0]) {
               case 0:
               case 11:
               case 12:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXYZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayGenericShape(true, true, true);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 1:
               case 2:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXYZ(AxiomI18n.get("axiom.tool.shape.width"), sizeFormatting);
                  changed |= this.displayGenericShape(true, true, this.shapeSolid[0] == 2);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 3:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayHeight(sizeFormatting);
                  changed |= this.displayGenericShape(true, true, false);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 4:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.supersphere"));
                  changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.supersphere.exponent"), this.exponent, 0.5F, 20.0F, "%.2f", 32);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXYZ(AxiomI18n.get("axiom.tool.shape.width"), sizeFormatting);
                  changed |= this.displayGenericShape(true, true, true);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 5:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.supercylinder"));
                  changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.supersphere.exponent"), this.exponent, 0.5F, 20.0F, "%.2f", 32);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayHeight(sizeFormatting);
                  changed |= this.displayGenericShape(true, true, true);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 6:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.capsule"));
                  changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.capsule.cap_height"), this.capHeight, 0.0F, 1.0F, "%.2f");
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayHeight(sizeFormatting);
                  changed |= this.displayGenericShape(true, true, false);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 7:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.tube"));
                  changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.tube.thickness"), this.thickness, 1.0F, 64.0F, sizeFormatting);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayHeight(sizeFormatting);
                  changed |= this.displayGenericShape(true, true, false);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 8:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.cone"));
                  changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.cone.rounding"), this.coneRounding, 0.0F, 1.0F);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayHeight(sizeFormatting);
                  changed |= this.displayGenericShape(true, true, false);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 9:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.width"), sizeFormatting);
                  changed |= this.displayHeight(sizeFormatting);
                  changed |= this.displayGenericShape(true, true, false);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               case 10:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.torus.outer"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.torus.ring"));
                  changed |= this.displayRingHV(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayGenericShape(true, true, true);
                  this.usingRealCoordinates = this.metaball;
                  break label94;
               default:
                  throw new FaultyImplementationError();
            }
         case 1:
            changed |= ImGuiHelper.combo(
               AxiomI18n.get("axiom.tool.shape"),
               this.shapePlane,
               new String[]{
                  AxiomI18n.get("axiom.tool.shape.disk"),
                  AxiomI18n.get("axiom.tool.shape.plane"),
                  AxiomI18n.get("axiom.tool.shape.regular_polygon"),
                  AxiomI18n.get("axiom.tool.shape.superellipse"),
                  AxiomI18n.get("axiom.tool.shape.archimedean_spiral")
               }
            );
            switch (this.shapePlane[0]) {
               case 0:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayGenericShape(true, false, false);
                  break;
               case 1:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.width"), sizeFormatting);
                  changed |= this.displayGenericShape(true, false, false);
                  break;
               case 2:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.regular_polygon"));
                  changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.shape.regular_polygon.sides"), this.polygonSides, 3, 9);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.width"), sizeFormatting);
                  changed |= this.displayGenericShape(true, false, false);
                  break;
               case 3:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.superellipse"));
                  changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.supersphere.exponent"), this.exponent, 0.5F, 20.0F, "%.2f", 32);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayGenericShape(true, false, false);
                  break;
               case 4:
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.archimedean_spiral"));
                  changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.archimedean_spiral.loops"), this.loops, 0.1F, 20.0F);
                  ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
                  changed |= this.displayDiameterXZ(AxiomI18n.get("axiom.tool.shape.diameter"), sizeFormatting);
                  changed |= this.displayGenericShape(false, false, false);
            }

            this.usingRealCoordinates = false;
            break;
         default:
            throw new FaultyImplementationError();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.placement_options"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.offset_when_placing"), this.offsetWhenPlacing)) {
         this.offsetWhenPlacing = !this.offsetWhenPlacing;
         changed = true;
      }

      if (!this.instantPlace) {
         changed |= ImGuiHelper.combo(
            AxiomI18n.get("axiom.tool.shape.gizmo_space"),
            this.gizmoSpace,
            new String[]{AxiomI18n.get("axiom.tool.shape.gizmo_space.global"), AxiomI18n.get("axiom.tool.shape.gizmo_space.local")}
         );
      }

      if (this.shapeCategory[0] != 1) {
         changed |= ImGuiHelper.combo(
            AxiomI18n.get("axiom.tool.shape.pivot"),
            this.pivot,
            new String[]{AxiomI18n.get("axiom.tool.shape.pivot.center"), AxiomI18n.get("axiom.tool.shape.pivot.base")}
         );
      }

      changed |= ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.shape.rotation"),
         this.placeRotation,
         new String[]{
            AxiomI18n.get("axiom.tool.shape.rotation.keep"), AxiomI18n.get("axiom.tool.shape.rotation.auto"), AxiomI18n.get("axiom.tool.shape.rotation.reset")
         }
      );
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.paste_options"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.instant_place"), this.instantPlace)) {
         this.instantPlace = !this.instantPlace;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.clipboard.placement_options.keep_existing"), this.keepExisting)) {
         this.keepExisting = !this.keepExisting;
         changed = true;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.path.extend_to_ground"), this.extendToGround)) {
         this.extendToGround = !this.extendToGround;
      }

      if (ImGui.button(AxiomI18n.get("axiom.hardcoded.paste_copy"))) {
         this.pasteShape(false);
      }

      if (ImGui.button(AxiomI18n.get("axiom.hardcoded.paste_and_select"))) {
         this.pasteShape(true);
         this.reset();
      }

      if (changed) {
         this.recalculate = true;
      }
   }

   private boolean displayDiameterXYZ(String diameterTerm, String sizeFormatting) {
      boolean settingsChanged = false;
      if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.create_shape.separate_xyz") + "##SeparateXYZ", this.separateXYZ)) {
         this.separateXYZ = !this.separateXYZ;
         if (this.separateXYZ) {
            this.sizeX[0] = this.sizeXYZ[0];
            this.sizeY[0] = this.sizeXYZ[0];
            this.sizeZ[0] = this.sizeXYZ[0];
         } else {
            this.sizeXYZ[0] = Math.round(this.sizeX[0] / 3.0F + this.sizeY[0] / 3.0F + this.sizeZ[0] / 3.0F);
         }

         settingsChanged = true;
      }

      if (this.separateXYZ) {
         settingsChanged |= ImGui.sliderFloat(diameterTerm + " X##SizeX", this.sizeX, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         settingsChanged |= ImGui.sliderFloat(diameterTerm + " Y##SizeY", this.sizeY, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         settingsChanged |= ImGui.sliderFloat(diameterTerm + " Z##SizeZ", this.sizeZ, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         this.sizeXYZ[0] = Math.round(this.sizeX[0] / 3.0F + this.sizeY[0] / 3.0F + this.sizeZ[0] / 3.0F);
      } else {
         settingsChanged |= ImGui.sliderFloat(diameterTerm + "##Size", this.sizeXYZ, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         this.sizeX[0] = this.sizeXYZ[0];
         this.sizeY[0] = this.sizeXYZ[0];
         this.sizeZ[0] = this.sizeXYZ[0];
      }

      return settingsChanged;
   }

   private boolean displayDiameterXZ(String diameterTerm, String sizeFormatting) {
      boolean settingsChanged = false;
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.separate_xz"), this.separateXYZ)) {
         this.separateXYZ = !this.separateXYZ;
         if (this.separateXYZ) {
            this.sizeX[0] = this.sizeXYZ[0];
            this.sizeZ[0] = this.sizeXYZ[0];
         } else {
            this.sizeXYZ[0] = Math.round(this.sizeX[0] / 2.0F + this.sizeZ[0] / 2.0F);
         }

         settingsChanged = true;
      }

      if (this.separateXYZ) {
         settingsChanged |= ImGui.sliderFloat(diameterTerm + " X##SizeX", this.sizeX, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         settingsChanged |= ImGui.sliderFloat(diameterTerm + " Z##SizeZ", this.sizeZ, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         this.sizeXYZ[0] = Math.round(this.sizeX[0] / 2.0F + this.sizeZ[0] / 2.0F);
      } else {
         settingsChanged |= ImGui.sliderFloat(diameterTerm + "##Size", this.sizeXYZ, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         this.sizeX[0] = this.sizeXYZ[0];
         this.sizeZ[0] = this.sizeXYZ[0];
      }

      return settingsChanged;
   }

   private boolean displayRingHV(String diameterTerm, String sizeFormatting) {
      boolean settingsChanged = false;
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.separate_hv"), this.separateHV)) {
         this.separateHV = !this.separateHV;
         settingsChanged = true;
      }

      if (this.separateHV) {
         settingsChanged |= ImGui.sliderFloat(diameterTerm + " H##RingH", this.ringH, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         settingsChanged |= ImGui.sliderFloat(diameterTerm + " V##RingV", this.ringV, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
      } else {
         settingsChanged |= ImGui.sliderFloat(diameterTerm + "##RingHV", this.ringH, 1.0F, 64.0F, sizeFormatting);
         ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
         this.ringV[0] = this.ringH[0];
      }

      return settingsChanged;
   }

   private boolean displayHeight(String sizeFormatting) {
      boolean settingsChanged = ImGui.sliderFloat(AxiomI18n.get("axiom.tool.shape.height") + "##SizeY", this.sizeY, 1.0F, 64.0F, sizeFormatting);
      ImGuiHelper.tooltip(AxiomI18n.get("axiom.widget.ctrl_click_hint"));
      return settingsChanged;
   }

   private boolean displayGenericShape(boolean hollow, boolean metaball, boolean cutoff) {
      boolean settingsChanged = this.displayAdvancedRotation();
      if (hollow || metaball) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.shape.modifiers"));
         if (hollow) {
            settingsChanged |= this.displayHollow();
         }

         if (metaball) {
            settingsChanged |= this.displayMetaball();
         }

         if (cutoff) {
            settingsChanged |= this.displayCutoff();
         }
      }

      return settingsChanged;
   }

   private boolean displayHollow() {
      if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.create_shape.hollow") + "##Hollow", this.hollow)) {
         this.hollow = !this.hollow;
         return true;
      } else {
         return false;
      }
   }

   private boolean displayMetaball() {
      boolean settingsChanged = false;
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.metaball") + "##Metaball", this.metaball)) {
         this.metaball = !this.metaball;
         settingsChanged = true;
      }

      if (this.metaball) {
         settingsChanged |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.shape.metaball_range"), this.metaballRange, 2, 16);
      }

      return settingsChanged;
   }

   private boolean displayCutoff() {
      if (this.metaball) {
         return false;
      } else {
         boolean settingsChanged = false;
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.shape.cutoff"), this.cutoff)) {
            this.cutoff = !this.cutoff;
            settingsChanged = true;
         }

         if (this.cutoff) {
            settingsChanged |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.shape.cutoff_amount"), this.cutoffAmount, 0, 100, "%d%%");
         }

         return settingsChanged;
      }
   }

   private boolean displayAdvancedRotation() {
      boolean settingsChanged = false;
      if (ImGui.collapsingHeader(I18n.get("axiom.editorui.window.create_shape.advanced", new Object[0]) + "###Advanced")) {
         ImGui.indent(8.0F * EditorUI.getUiScale());
         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.decimal_sizes"), this.decimalSizes)) {
            this.decimalSizes = !this.decimalSizes;
            settingsChanged = true;
         }

         ImGuiHelper.tooltip("Allows using decimal numbers (eg. 10.5) in various size sliders");
         if (this.decimalSizes) {
            ImGui.beginDisabled();
         }

         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.center_even_diameters"), this.centerEvenDiameters && !this.decimalSizes)) {
            this.centerEvenDiameters = !this.centerEvenDiameters;
            settingsChanged = true;
         }

         ImGuiHelper.tooltip("Adjusts even diameters (eg. 10) to be centered at the lower corner");
         if (this.decimalSizes) {
            ImGui.endDisabled();
         }

         if (ImGui.button(AxiomI18n.get("axiom.tool.shape.reset_rotation"), ImGui.calcItemWidth(), 0.0F)) {
            this.rotationX[0] = 0.0F;
            this.rotationY[0] = 0.0F;
            this.rotationZ[0] = 0.0F;
            settingsChanged = true;
         }

         settingsChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.window.create_shape.yaw") + "##Yaw", this.rotationY, -180.0F, 180.0F, "%.2f°");
         settingsChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.window.create_shape.pitch") + "##Pitch", this.rotationX, -180.0F, 180.0F, "%.2f°");
         settingsChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.editorui.window.create_shape.roll") + "##Roll", this.rotationZ, -180.0F, 180.0F, "%.2f°");
         ImGui.unindent(8.0F * EditorUI.getUiScale());
      }

      if (settingsChanged) {
         this.rotationQuaternion.identity();
         this.rotationQuaternion
            .rotateYXZ((float)Math.toRadians(this.rotationY[0]), (float)Math.toRadians(this.rotationX[0]), (float)Math.toRadians(this.rotationZ[0]));
      }

      return settingsChanged;
   }

   private int getGizmoSpace() {
      return this.instantPlace ? 0 : this.gizmoSpace[0];
   }

   private int getPivot() {
      return this.shapeCategory[0] != 0 ? 0 : this.pivot[0];
   }

   private BlockPos calculateRawPivot(float height) {
      int pivot = this.getPivot();
      if (pivot == 0) {
         return BlockPos.ZERO;
      } else if (pivot == 1) {
         return new BlockPos(0, (int)Math.floor((height - 1.0F) / 2.0F), 0);
      } else {
         throw new FaultyImplementationError();
      }
   }

   private BlockPos calculatePivot(float height) {
      int pivot = this.getPivot();
      if (pivot == 0) {
         return BlockPos.ZERO;
      } else if (pivot == 1) {
         Vector3d vector = new Vector3d(0.0, Math.floor((height - 1.0F) / 2.0F), 0.0);
         this.rotationQuaternion.transformInverse(vector);
         return BlockPos.containing(vector.x + 0.5, vector.y + 0.5, vector.z + 0.5);
      } else {
         throw new FaultyImplementationError();
      }
   }

   private int getPlaceRotation() {
      return this.placeRotation[0];
   }

   private void setRotationAuto(BlockPos at) {
      this.rotationX[0] = this.rotationY[0] = this.rotationZ[0] = 0.0F;
      this.rotationQuaternion.identity();
      float offsetX = 0.0F;
      float offsetY = 0.0F;
      float offsetZ = 0.0F;
      int checkRadiusSq = 12;
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      ClientLevel world = Minecraft.getInstance().level;
      if (world != null) {
         int x = at.getX();
         int y = at.getY();
         int z = at.getZ();

         for (int xo = -3; xo <= 3; xo++) {
            for (int yo = -3; yo <= 3; yo++) {
               for (int zo = -3; zo <= 3; zo++) {
                  int distSq = xo * xo + yo * yo + zo * zo;
                  if (distSq <= checkRadiusSq) {
                     BlockState block = world.getBlockState(mutableBlockPos.set(x + xo, y + yo, z + zo));
                     if (block.blocksMotion()) {
                        float factor = 1.0F / (float)Math.max(1.0, Math.sqrt(distSq));
                        offsetX -= xo * factor;
                        offsetY -= yo * factor;
                        offsetZ -= zo * factor;
                     }
                  }
               }
            }
         }

         float lengthSq = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
         if (lengthSq > 0.01F) {
            float invNormalLength = 1.0F / (float)Math.sqrt(lengthSq);
            float normalX = offsetX * invNormalLength;
            float normalY = offsetY * invNormalLength;
            float normalZ = offsetZ * invNormalLength;
            if (Math.abs(normalX) < 1.0E-4) {
               normalX = 0.0F;
            }

            if (Math.abs(normalY) < 1.0E-4) {
               normalY = 0.0F;
            }

            if (Math.abs(normalZ) < 1.0E-4) {
               normalZ = 0.0F;
            }

            double horizontal = Math.sqrt(normalX * normalX + normalZ * normalZ);
            float pitch = Mth.wrapDegrees((float)(-(Mth.atan2(normalY, horizontal) * 180.0F / (float)Math.PI)) + 90.0F);
            float yaw = Mth.wrapDegrees((float)(Mth.atan2(normalZ, normalX) * 180.0F / (float)Math.PI) + 90.0F);
            this.rotationQuaternion.rotateXYZ((float)Math.toRadians(pitch), (float)Math.toRadians(yaw), 0.0F);
            Vector3f euler = this.rotationQuaternion.getEulerAnglesYXZ(new Vector3f());
            this.rotationX[0] = (float)Math.round(Math.toDegrees(euler.x));
            this.rotationY[0] = (float)Math.round(Math.toDegrees(euler.y));
            this.rotationZ[0] = (float)Math.round(Math.toDegrees(euler.z));
            this.rotationQuaternion.identity();
            this.rotationQuaternion
               .rotateYXZ((float)Math.toRadians(this.rotationY[0]), (float)Math.toRadians(this.rotationX[0]), (float)Math.toRadians(this.rotationZ[0]));
         }
      }
   }

   @Override
   public String listenForEsc() {
      if (this.instantPlace) {
         return null;
      } else if (this.gizmo != null && this.gizmo.enableAxes) {
         return "Deselect Point";
      } else {
         return this.gizmo != null ? AxiomI18n.get("axiom.widget.cancel") : null;
      }
   }

   @Override
   public boolean initiateAdjustment() {
      boolean allowAdjustment = false;
      if (this.gizmo == null) {
         return false;
      } else {
         if (this.shapeCategory[0] == 1) {
            allowAdjustment = true;
         } else if (this.shapeCategory[0] == 0) {
            allowAdjustment = this.shapeSolid[0] != 10;
         }

         if (allowAdjustment) {
            int radius;
            int height;
            if (this.separateXYZ) {
               radius = Math.round(this.sizeX[0] / 2.0F + this.sizeZ[0] / 2.0F);
               height = Math.round(this.sizeY[0]);
            } else {
               radius = Math.round(this.sizeXYZ[0]);
               height = -1;
            }

            float distance = (float)this.gizmo.getInterpPosition().distanceTo(Minecraft.getInstance().player.getEyePosition());
            this.radiusAdjustment.initiateAdjustment(radius, height, distance);
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      GenericRadiusAdjustment.Result result = this.radiusAdjustment.renderAdjustment(mouseX, mouseY, mouseDelta);
      if (result.changed()) {
         if (this.separateXYZ) {
            this.sizeX[0] = result.radius();
            if (result.height() >= 0) {
               this.sizeY[0] = result.height();
            }

            this.sizeZ[0] = result.radius();
         } else {
            this.sizeXYZ[0] = result.radius();
         }

         this.recalculate = true;
      }

      return result.mouseDelta();
   }

   @Override
   public String listenForEnter() {
      return !this.chunkedBlockRegion.isEmpty() ? AxiomI18n.get("axiom.widget.confirm") : null;
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.shape");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.recalculate = true;
   }

   @Override
   public char iconChar() {
      return '\ue91c';
   }

   @Override
   public String keybindId() {
      return "shape";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_SHAPE, AxiomPermission.BUILD_SECTION);
   }
}
