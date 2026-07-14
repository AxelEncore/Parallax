package com.moulberry.axiom.tools.gradient_painter;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.core_rendering.AxiomDrawBuffer;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.EditorMovementControls;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.noise.WhiteNoise;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.BezierOperator;
import com.moulberry.axiom.utils.BlockManipulation;
import com.moulberry.axiom.utils.BlockWithFloat;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.utils.ProjectedText;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.utils.RenderHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

public class GradientPainterTool implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private AxiomDrawBuffer gridBuffer = null;
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private BlockPos pendingTargetPos = null;
   private Vec3 pos1 = null;
   private Vec3 pos2 = null;
   private Vec3 direction = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private boolean maskSurface = true;
   private final int[] gradientShape = new int[]{0};
   private final int[] gradientInterpolation = new int[]{0};
   private final int[] lockedGradient = new int[]{0};
   private boolean clampToEdge = false;
   private final ImString noiseSeed = new ImString();
   public final List<BlockWithFloat> blockPercentages = new ArrayList<>();
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private boolean copyProperties = false;
   private boolean typeReplace = false;
   private final PresetWidget presetWidget = new PresetWidget(this, "gradient_painter");
   private final int defaultRandomValue = ThreadLocalRandom.current().nextInt();

   public GradientPainterTool() {
      this.blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.STONE.defaultBlockState(), new float[]{50.0F}, null));
      this.blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.GRANITE.defaultBlockState(), new float[]{50.0F}, null));
      this.noiseSeed.set(String.valueOf(this.defaultRandomValue), false);
   }

   @Override
   public void reset() {
      this.usingTool = false;
      this.chunkedBlockRegion.clear();
      this.pendingTargetPos = null;
      if (this.lockedGradient[0] == 0) {
         this.pos1 = null;
         this.pos2 = null;
         this.direction = null;
      } else if (this.lockedGradient[0] == 1 && this.pos1 != null) {
         this.pendingTargetPos = BlockPos.containing(this.pos1);
         this.pos1 = null;
         this.pos2 = null;
         this.direction = null;
      }

      if (this.gridBuffer != null) {
         this.gridBuffer.close();
         this.gridBuffer = null;
      }

      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case ESCAPE:
            if (this.usingTool || this.pendingTargetPos != null) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.lockedGradient[0] != 0) {
               this.lockedGradient[0] = 0;
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case UNDO:
            if (this.usingTool) {
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.pos1 != null || this.pos2 != null || this.direction != null || this.pendingTargetPos != null) {
               this.pos1 = null;
               this.pos2 = null;
               this.direction = null;
               this.pendingTargetPos = null;
               return UserAction.ActionResult.USED_STOP;
            }
            break;
         case RIGHT_MOUSE:
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, false);
            if (result == null) {
               return UserAction.ActionResult.USED_STOP;
            }

            Entity cameraEntity = Minecraft.getInstance().cameraEntity;
            if (cameraEntity == null) {
               return UserAction.ActionResult.USED_STOP;
            }

            if (this.lockedGradient[0] != 2 || this.pos1 == null || this.pos2 == null) {
               if (this.pendingTargetPos == null) {
                  this.pendingTargetPos = result.blockPos();
                  return UserAction.ActionResult.USED_STOP;
               }

               BlockPos currentTargetPos = this.pendingTargetPos;
               this.reset();
               this.pos1 = Vec3.atCenterOf(currentTargetPos);
               this.pos2 = Vec3.atCenterOf(result.blockPos());
               this.direction = this.pos2.subtract(this.pos1).normalize();
            }

            AsyncToolPather pather = this.createToolPather(this.brushWidget.getBrushShape());
            if (pather == null) {
               return UserAction.ActionResult.USED_STOP;
            }

            this.usingTool = true;
            this.pathProvider = new AsyncToolPathProvider(pather).includeNonSolid(false);
            return UserAction.ActionResult.USED_STOP;
      }

      return UserAction.ActionResult.NOT_HANDLED;
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (!this.usingTool) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result == null) {
            if (EditorUI.movementControls == EditorMovementControls.none() && this.lockedGradient[0] == 0) {
               this.pendingTargetPos = null;
            }

            Selection.render(rc, 7);
            return;
         }

         Selection.render(rc, 4);
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
         if (this.pendingTargetPos != null) {
            this.renderTargetTrianglePreview(rc, result);
         }
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.painted", countString);
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         this.pathProvider.update();
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
      }

      if (this.pos1 != null && this.pos2 != null && (this.usingTool || this.lockedGradient[0] == 2)) {
         Vec3 offsetPos1 = this.pos1;
         Vec3 offsetPos2 = this.pos2;
         double distance = offsetPos1.distanceTo(offsetPos2);
         RayCaster.RaycastResult resultx = Tool.raycastBlock();
         if (resultx != null) {
            Vec3 from = this.pos1;
            Vec3 to = resultx.worldPos();
            Vec3 mid = from.lerp(to, 0.5);
            from = from.subtract(mid);
            to = to.subtract(mid);
            double dx = to.x - from.x;
            double dy = to.y - from.y;
            double dz = to.z - from.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float nx = (float)(dx / dist);
            float ny = (float)(dy / dist);
            float nz = (float)(dz / dist);
            PoseStack matrices = rc.poseStack();
            matrices.pushPose();
            matrices.translate(mid.x - rc.x(), mid.y - rc.y(), mid.z - rc.z());
            Pose pose = matrices.last();
            VertexConsumerProvider provider = VertexConsumerProvider.shared();
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, (float)from.x, (float)from.y, (float)from.z).setColor(-16729344).setNormal(pose, nx, ny, nz),
               RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(pose, (float)to.x, (float)to.y, (float)to.z).setColor(-16729344).setNormal(pose, nx, ny, nz), RenderHelper.baseLineWidth
            );
            AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
            AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(bufferBuilder.build());
            int x = resultx.blockPos().getX();
            int y = resultx.blockPos().getY();
            int z = resultx.blockPos().getZ();
            double amount;
            if (this.gradientShape[0] == 0) {
               amount = (x + 0.5 - offsetPos1.x) * this.direction.x + (y + 0.5 - offsetPos1.y) * this.direction.y + (z + 0.5 - offsetPos1.z) * this.direction.z;
            } else {
               amount = Math.sqrt(offsetPos1.distanceToSqr(x + 0.5, y + 0.5, z + 0.5));
            }

            amount /= distance;
            if (amount < 0.0) {
               amount = 0.0;
            }

            if (amount > 1.0) {
               amount = 1.0;
            }

            String text = (int)(amount * 100.0) + "%";
            this.renderInfoText(matrices, rc.projection(), text);
            matrices.popPose();
         }
      }
   }

   @Nullable
   private AsyncToolPather createToolPather(BrushShape brushShape) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         MaskElement solidDestMask = MaskManager.createSolidDestMask();
         MaskContext maskContext = new MaskContext(level);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         Vec3 pos1 = this.pos1;
         double distance = pos1.distanceTo(this.pos2);
         boolean planeGradientShape = this.gradientShape[0] == 0;
         DoubleUnaryOperator[] interpolationArray;

         record LinearOperator(int index, int count) implements DoubleUnaryOperator {
            @Override
            public double applyAsDouble(double operand) {
               return this.index == 0 && operand * this.count < 0.5 ? 1.0 : 1.0 - Math.abs(this.index + 0.5 - this.count * operand);
            }
         }

         if (this.gradientInterpolation[0] == 1) {
            interpolationArray = new DoubleUnaryOperator[this.blockPercentages.size() - 1];
            int count = this.blockPercentages.size();

            for (int i = 0; i < this.blockPercentages.size(); i++) {
               if (i < interpolationArray.length) {
                  interpolationArray[i] = new LinearOperator(i, count);
               }
            }
         } else if (this.gradientInterpolation[0] == 2) {
            interpolationArray = new DoubleUnaryOperator[this.blockPercentages.size() - 1];
            int count = this.blockPercentages.size();

            for (int ix = 0; ix < this.blockPercentages.size(); ix++) {
               if (ix < interpolationArray.length) {
                  interpolationArray[ix] = new BezierOperator(ix, count);
               }
            }
         } else {
            interpolationArray = null;
         }

         boolean copyProperties = this.canCopyProperties() && this.copyProperties;
         boolean typeReplace = this.canTypeReplace() && this.typeReplace;
         if (interpolationArray == null) {
            float totalAmount = 0.0F;

            for (BlockWithFloat blockPercentage : this.blockPercentages) {
               totalAmount += blockPercentage.percentage()[0] / 100.0F;
            }

            if (totalAmount <= 0.0F) {
               return null;
            } else {
               float totalAmountF = totalAmount;
               return new AsyncToolPatherUnique(
                  brushShape,
                  (x, y, z) -> {
                     if (solidDestMask.test(maskContext.reset(), x, y, z)) {
                        if (!this.maskSurface
                           || !level.getBlockState(mutableBlockPos.set(x + 1, y, z)).blocksMotion()
                           || !level.getBlockState(mutableBlockPos.set(x - 1, y, z)).blocksMotion()
                           || !level.getBlockState(mutableBlockPos.set(x, y + 1, z)).blocksMotion()
                           || !level.getBlockState(mutableBlockPos.set(x, y - 1, z)).blocksMotion()
                           || !level.getBlockState(mutableBlockPos.set(x, y, z + 1)).blocksMotion()
                           || !level.getBlockState(mutableBlockPos.set(x, y, z - 1)).blocksMotion()) {
                           double amount;
                           if (planeGradientShape) {
                              amount = (x + 0.5 - pos1.x) * this.direction.x + (y + 0.5 - pos1.y) * this.direction.y + (z + 0.5 - pos1.z) * this.direction.z;
                           } else {
                              amount = Math.sqrt(pos1.distanceToSqr(x + 0.5, y + 0.5, z + 0.5));
                           }

                           amount /= distance;
                           if (amount < 0.0) {
                              if (this.clampToEdge) {
                                 return;
                              }

                              amount = 0.0;
                           }

                           if (amount > 1.0) {
                              if (this.clampToEdge) {
                                 return;
                              }

                              amount = 1.0;
                           }

                           for (BlockWithFloat blockPercentage : this.blockPercentages) {
                              amount -= blockPercentage.percentage()[0] / 100.0F / totalAmountF;
                              if (amount < 0.0) {
                                 CustomBlockState blockState = blockPercentage.blockState();
                                 BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                                    this.chunkedBlockRegion,
                                    x,
                                    y,
                                    z,
                                    blockState,
                                    typeReplace,
                                    copyProperties,
                                    blockPercentage.randomProperties(),
                                    maskContext,
                                    mutableBlockPos
                                 );
                                 return;
                              }
                           }

                           BlockWithFloat last = this.blockPercentages.get(this.blockPercentages.size() - 1);
                           BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                              this.chunkedBlockRegion,
                              x,
                              y,
                              z,
                              last.blockState(),
                              typeReplace,
                              copyProperties,
                              last.randomProperties(),
                              maskContext,
                              mutableBlockPos
                           );
                        }
                     }
                  }
               );
            }
         } else {
            String seedStr = ImGuiHelper.getString(this.noiseSeed).trim();
            int seed;
            if (seedStr.isEmpty()) {
               seed = this.defaultRandomValue;
            } else {
               try {
                  seed = Integer.parseInt(seedStr);
               } catch (NumberFormatException var20) {
                  seed = 0;

                  for (char c : seedStr.toCharArray()) {
                     seed = 31 * seed + c;
                  }
               }
            }

            WhiteNoise whiteNoise = new WhiteNoise(seed);
            return new AsyncToolPatherUnique(
               brushShape,
               (x, y, z) -> {
                  if (solidDestMask.test(maskContext.reset(), x, y, z)) {
                     if (!this.maskSurface
                        || !level.getBlockState(mutableBlockPos.set(x + 1, y, z)).blocksMotion()
                        || !level.getBlockState(mutableBlockPos.set(x - 1, y, z)).blocksMotion()
                        || !level.getBlockState(mutableBlockPos.set(x, y + 1, z)).blocksMotion()
                        || !level.getBlockState(mutableBlockPos.set(x, y - 1, z)).blocksMotion()
                        || !level.getBlockState(mutableBlockPos.set(x, y, z + 1)).blocksMotion()
                        || !level.getBlockState(mutableBlockPos.set(x, y, z - 1)).blocksMotion()) {
                        double amount;
                        if (planeGradientShape) {
                           amount = (x + 0.5 - pos1.x) * this.direction.x + (y + 0.5 - pos1.y) * this.direction.y + (z + 0.5 - pos1.z) * this.direction.z;
                        } else {
                           amount = Math.sqrt(pos1.distanceToSqr(x + 0.5, y + 0.5, z + 0.5));
                        }

                        amount /= distance;
                        if (amount < 0.0) {
                           if (this.clampToEdge) {
                              return;
                           }

                           amount = 0.0;
                        }

                        if (amount > 1.0) {
                           if (this.clampToEdge) {
                              return;
                           }

                           amount = 1.0;
                        }

                        double randomValue = whiteNoise.evaluate(x + 0.5, y + 0.5, z + 0.5);
                        int ixx = 0;

                        for (DoubleUnaryOperator operator : interpolationArray) {
                           randomValue -= Math.max(0.0, operator.applyAsDouble(amount));
                           if (randomValue < 0.0) {
                              CustomBlockState blockState = this.blockPercentages.get(ixx).blockState();
                              Set<Property<?>> randomizedProperties = this.blockPercentages.get(ixx).randomProperties();
                              BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                                 this.chunkedBlockRegion, x, y, z, blockState, typeReplace, copyProperties, randomizedProperties, maskContext, mutableBlockPos
                              );
                              return;
                           }

                           ixx++;
                        }

                        BlockWithFloat last = this.blockPercentages.get(this.blockPercentages.size() - 1);
                        BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                           this.chunkedBlockRegion,
                           x,
                           y,
                           z,
                           last.blockState(),
                           typeReplace,
                           copyProperties,
                           last.randomProperties(),
                           maskContext,
                           mutableBlockPos
                        );
                     }
                  }
               }
            );
         }
      }
   }

   private void renderTargetTrianglePreview(AxiomWorldRenderContext rc, RayCaster.RaycastResult result) {
      Vec3 from = Vec3.atCenterOf(this.pendingTargetPos);
      Vec3 to = result.worldPos();
      Vec3 mid = from.lerp(to, 0.5);
      from = from.subtract(mid);
      to = to.subtract(mid);
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      float nx = (float)(dx / dist);
      float ny = (float)(dy / dist);
      float nz = (float)(dz / dist);
      PoseStack matrix = rc.poseStack();
      matrix.pushPose();
      matrix.translate(mid.x - rc.x(), mid.y - rc.y(), mid.z - rc.z());
      Pose pose = matrix.last();
      VertexConsumerProvider provider = VertexConsumerProvider.shared();
      BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)from.x, (float)from.y, (float)from.z).setColor(-7829249).setNormal(pose, nx, ny, nz), RenderHelper.baseLineWidth
      );
      VersionUtilsClient.legacySetLineWidthIgnored(
         bufferBuilder.addVertex(pose, (float)to.x, (float)to.y, (float)to.z).setColor(-7829249).setNormal(pose, nx, ny, nz), RenderHelper.baseLineWidth
      );
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
      this.renderInfoText(matrix, rc.projection(), String.format("%.2f", dist));
      matrix.popPose();
   }

   private void renderInfoText(PoseStack matrix, Matrix4fc projection, String text) {
      ProjectedText.setupProjectedText();
      ProjectedText.renderProjectedText(text, matrix, projection, 0.0F, 0.0F, 0.0F);
      ProjectedText.finishProjectedText();
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.generic.mask_surface"), this.maskSurface)) {
         this.maskSurface = !this.maskSurface;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.gradient_painter.gradient"));
      changed |= ImGuiHelper.radio(
         AxiomI18n.get("axiom.tool.gradient_painter.gradient_shape"),
         this.gradientShape,
         new String[]{AxiomI18n.get("axiom.tool.gradient_painter.gradient_shape_plane"), AxiomI18n.get("axiom.tool.gradient_painter.gradient_shape_sphere")}
      );
      changed |= ImGuiHelper.radio(
         AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation"),
         this.gradientInterpolation,
         new String[]{
            AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation_nearest"),
            AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation_linear"),
            AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation_bezier")
         }
      );
      if (ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.gradient_painter.locking"),
         this.lockedGradient,
         new String[]{
            AxiomI18n.get("axiom.tool.gradient_painter.lock_none"),
            AxiomI18n.get("axiom.tool.gradient_painter.lock_pos1"),
            AxiomI18n.get("axiom.tool.gradient_painter.lock_both")
         }
      )) {
         changed = true;
         this.reset();
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.gradient_painter.clamp_to_edge"), this.clampToEdge)) {
         this.clampToEdge = !this.clampToEdge;
         changed = true;
      }

      if (this.gradientInterpolation[0] != 0) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.noise"));
         changed |= ImGui.inputText(AxiomI18n.get("axiom.tool.generic.noise_seed"), this.noiseSeed);
         if (ImGui.button(AxiomI18n.get("axiom.tool.generic.noise_seed_do_randomize"))) {
            this.noiseSeed.set(String.valueOf(ThreadLocalRandom.current().nextInt()), false);
            changed = true;
         }
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.noise_painter.blocks"));
      BlockWithFloat.ExtraRenderType extraRenderType = this.gradientInterpolation[0] == 0
         ? BlockWithFloat.ExtraRenderType.PERCENTAGE
         : BlockWithFloat.ExtraRenderType.BLOCKNAME;
      changed |= BlockWithFloat.renderList(this.blockPercentages, this.selectBlockWidget, extraRenderType, 2, true);
      if (this.canCopyProperties() && ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.replace.copy_properties"), this.copyProperties)) {
         this.copyProperties = !this.copyProperties;
      }

      if (this.canTypeReplace() && ImGui.checkbox(AxiomI18n.get("axiom.contextmenu.type_replace"), this.typeReplace)) {
         this.typeReplace = !this.typeReplace;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
   }

   private boolean canCopyProperties() {
      for (BlockWithFloat blockThreshold : this.blockPercentages) {
         if (!blockThreshold.blockState().getProperties().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean canTypeReplace() {
      for (BlockWithFloat blockThreshold : this.blockPercentages) {
         if (FamilyMap.getFamilyForBase(blockThreshold.blockState().getVanillaState().getBlock()) == null) {
            return false;
         }
      }

      return true;
   }

   @Override
   public String listenForEsc() {
      if (this.usingTool || this.pendingTargetPos != null) {
         return AxiomI18n.get("axiom.widget.cancel");
      } else {
         return this.lockedGradient[0] != 0 ? "Unlock" : null;
      }
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
      return AxiomI18n.get("axiom.tool.gradient_painter");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putBoolean("MaskSurface", this.maskSurface);
      tag.putInt("GradientShape", this.gradientShape[0]);
      tag.putInt("GradientInterpolation", this.gradientInterpolation[0]);
      tag.putBoolean("ClampToEdge", this.clampToEdge);
      tag.putString("NoiseSeed", ImGuiHelper.getString(this.noiseSeed));
      ListTag blockPercentages = new ListTag();

      for (BlockWithFloat blockWithFloat : this.blockPercentages) {
         CompoundTag blockWithFloatTag = new CompoundTag();
         blockWithFloatTag.putString("Block", ServerCustomBlocks.serialize(blockWithFloat.blockState()));
         blockWithFloatTag.putFloat("Percentage", blockWithFloat.percentage()[0]);
         blockPercentages.add(blockWithFloatTag);
      }

      tag.put("BlockPercentages", blockPercentages);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.maskSurface = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "MaskSurface", true);
      this.gradientShape[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "GradientShape", 0);
      this.gradientInterpolation[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "GradientInterpolation", 0);
      this.clampToEdge = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "ClampToEdge", false);
      this.noiseSeed.set(VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "NoiseSeed", String.valueOf(this.defaultRandomValue)), false);
      this.blockPercentages.clear();

      for (Tag blockPercentageTag : NbtHelper.getList(tag, "BlockPercentages", 10)) {
         CompoundTag blockPercentage = (CompoundTag)blockPercentageTag;
         String block = VersionUtilsNbt.helperCompoundTagGetString(blockPercentage, "Block").get();
         float percentage = VersionUtilsNbt.helperCompoundTagGetFloat(blockPercentage, "Percentage").get();
         this.blockPercentages
            .add(
               new BlockWithFloat(
                  Objects.requireNonNullElse(ServerCustomBlocks.deserialize(block), (CustomBlockState)Blocks.STONE.defaultBlockState()),
                  new float[]{percentage},
                  null
               )
            );
      }

      BlockWithFloat.ensureFilledToMinimum(this.blockPercentages, 2);
   }

   public void setBlocks(List<CustomBlockState> blocks) {
      this.blockPercentages.clear();

      for (CustomBlockState block : blocks) {
         this.blockPercentages.add(new BlockWithFloat(block, new float[]{100.0F / blocks.size()}, null));
      }

      BlockWithFloat.ensureFilledToMinimum(this.blockPercentages, 2);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue91a';
   }

   @Override
   public String keybindId() {
      return "gradient_painter";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_GRADIENTPAINTER, AxiomPermission.BUILD_SECTION);
   }
}
