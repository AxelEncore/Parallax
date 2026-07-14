package com.moulberry.axiom.tools.painter;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.brush_shapes.SinglePointBrushShape;
import com.moulberry.axiom.clipboard.Clipboard;
import com.moulberry.axiom.clipboard.ClipboardObject;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
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
import com.moulberry.axiom.pather.async.AsyncToolPatherMinSDF;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.BezierOperator;
import com.moulberry.axiom.utils.BlockManipulation;
import com.moulberry.axiom.utils.BlockWithFloat;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PainterTool implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private final ChunkedBooleanRegion airRegion = new ChunkedBooleanRegion();
   private boolean paintingAir = false;
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private boolean maskSurface = true;
   private boolean copyProperties = false;
   private boolean typeReplace = false;
   private final int[] mode = new int[]{0};
   private Position2FloatMap minDistances = new Position2FloatMap(Float.MAX_VALUE);
   private final int[] gradientInterpolation = new int[]{0};
   private final List<BlockWithFloat> blockPercentages = new ArrayList<>();
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private final ImString noiseSeed = new ImString();
   private final int defaultRandomValue = ThreadLocalRandom.current().nextInt();
   private boolean softEdge = false;
   private boolean mergeStrokes = true;
   private final float[] minimumRadius = new float[]{0.0F};
   private final PresetWidget presetWidget = new PresetWidget(this, "painter");

   public PainterTool() {
      this.blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.STONE.defaultBlockState(), new float[]{50.0F}, null));
      this.blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.GRANITE.defaultBlockState(), new float[]{50.0F}, null));
      this.noiseSeed.set(String.valueOf(this.defaultRandomValue), false);
   }

   @Override
   public void reset() {
      this.partialReset();
      this.minDistances.clear();
   }

   private void partialReset() {
      this.usingTool = false;
      this.chunkedBlockRegion.clear();
      this.airRegion.clear();
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.partialReset();
            BrushShape brushShape = this.brushWidget.getBrushShape();
            AsyncToolPather pather = this.createToolPather(brushShape);
            if (pather != null) {
               this.usingTool = true;
               this.pathProvider = new AsyncToolPathProvider(pather).includeNonSolid(false);
            }

            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
            if (this.usingTool) {
               this.partialReset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Override
   public void afterBlockBufferUndo() {
      this.reset();
   }

   @Override
   public void afterBlockBufferRedo() {
      this.reset();
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (!this.usingTool) {
         RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
         if (result == null) {
            Selection.render(rc, 7);
            return;
         }

         Selection.render(rc, 4);
         if (this.mode[0] != 2) {
            this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
         }
      } else if (Tool.cancelUsing()) {
         this.partialReset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.painted", countString);
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
         this.partialReset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         Selection.render(rc, 4);
         this.pathProvider.update();
         if (this.paintingAir) {
            this.airRegion.render(rc, Vec3.ZERO, 8);
         } else {
            float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
            this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
         }
      }
   }

   private static <T extends Comparable<T>> BlockState copyProperty(BlockState blockState, BlockState blockState2, Property<T> property) {
      return (BlockState)blockState2.setValue(property, blockState.getValue(property));
   }

   @Nullable
   private AsyncToolPather createToolPather(BrushShape brushShape) {
      MaskElement destMask = MaskManager.createSolidDestMask();
      MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
      this.paintingAir = false;
      boolean copyProperties = this.canCopyProperties() && this.copyProperties;
      boolean typeReplace = this.canTypeReplace() && this.typeReplace;

      record LinearOperator(int index, int count) implements DoubleUnaryOperator {
         @Override
         public double applyAsDouble(double operand) {
            return this.index == 0 && operand * this.count < 0.5 ? 1.0 : 1.0 - Math.abs(this.index + 0.5 - this.count * operand);
         }
      }

      switch (this.mode[0]) {
         case 1:
            ClipboardObject clipboardObjectx = Clipboard.INSTANCE.getClipboard();
            if (clipboardObjectx == null) {
               return null;
            } else {
               ChunkedBlockRegion copyFrom = clipboardObjectx.blockRegion();
               if (!copyFrom.isEmpty() && copyFrom.min() != null && copyFrom.max() != null) {
                  int minX = copyFrom.min().getX();
                  int minY = copyFrom.min().getY();
                  int minZ = copyFrom.min().getZ();
                  int maxX = copyFrom.max().getX();
                  int maxY = copyFrom.max().getY();
                  int maxZ = copyFrom.max().getZ();
                  int sizeX = maxX - minX + 1;
                  int sizeY = maxY - minY + 1;
                  int sizeZ = maxZ - minZ + 1;
                  return new AsyncToolPatherUnique(
                     brushShape,
                     (x, y, z) -> {
                        if (destMask.test(maskContext.reset(), x, y, z)) {
                           if (!this.maskSurface
                              || !maskContext.getBlockState(x, y, z, 1, 0, 0).blocksMotion()
                              || !maskContext.getBlockState(x, y, z, -1, 0, 0).blocksMotion()
                              || !maskContext.getBlockState(x, y, z, 0, 1, 0).blocksMotion()
                              || !maskContext.getBlockState(x, y, z, 0, -1, 0).blocksMotion()
                              || !maskContext.getBlockState(x, y, z, 0, 0, 1).blocksMotion()
                              || !maskContext.getBlockState(x, y, z, 0, 0, -1).blocksMotion()) {
                              int modX = x % sizeX;
                              if (modX < 0) {
                                 modX += sizeX;
                              }

                              int modY = y % sizeY;
                              if (modY < 0) {
                                 modY += sizeY;
                              }

                              int modZ = z % sizeZ;
                              if (modZ < 0) {
                                 modZ += sizeZ;
                              }

                              BlockState blockState = copyFrom.getBlockStateOrAir(minX + modX, minY + modY, minZ + modZ);
                              if (!blockState.isAir()) {
                                 this.chunkedBlockRegion.addBlock(x, y, z, blockState);
                              }
                           }
                        }
                     }
                  );
               }

               return null;
            }
         case 2:
            ClipboardObject clipboardObject = Clipboard.INSTANCE.getClipboard();
            if (clipboardObject == null) {
               return null;
            } else {
               ChunkedBlockRegion copyFrom = clipboardObject.blockRegion();
               if (!copyFrom.isEmpty() && copyFrom.min() != null && copyFrom.max() != null) {
                  int minX = copyFrom.min().getX();
                  int minY = copyFrom.min().getY();
                  int minZ = copyFrom.min().getZ();
                  int maxX = copyFrom.max().getX();
                  int maxY = copyFrom.max().getY();
                  int maxZ = copyFrom.max().getZ();
                  return new AsyncToolPatherUnique(
                     new SinglePointBrushShape(),
                     (x, y, z) -> {
                        for (int xo = minX; xo <= maxX; xo++) {
                           for (int yo = minY; yo <= maxY; yo++) {
                              for (int zo = minZ; zo <= maxZ; zo++) {
                                 if (destMask.test(maskContext.reset(), x + xo, y + yo, z + zo)
                                    && (
                                       !this.maskSurface
                                          || !maskContext.getBlockState(x + xo, y + yo, z + zo, 1, 0, 0).blocksMotion()
                                          || !maskContext.getBlockState(x + xo, y + yo, z + zo, -1, 0, 0).blocksMotion()
                                          || !maskContext.getBlockState(x + xo, y + yo, z + zo, 0, 1, 0).blocksMotion()
                                          || !maskContext.getBlockState(x + xo, y + yo, z + zo, 0, -1, 0).blocksMotion()
                                          || !maskContext.getBlockState(x + xo, y + yo, z + zo, 0, 0, 1).blocksMotion()
                                          || !maskContext.getBlockState(x + xo, y + yo, z + zo, 0, 0, -1).blocksMotion()
                                    )) {
                                    BlockState blockState = copyFrom.getBlockStateOrAir(xo, yo, zo);
                                    if (!blockState.isAir()) {
                                       this.chunkedBlockRegion.addBlock(x + xo, y + yo, z + zo, blockState);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  );
               }

               return null;
            }
         case 3:
            PositionSet blacklistedPositions = new PositionSet();
            DoubleUnaryOperator[] interpolationArray;
            BlockWithFloat[] blockArray;
            if (this.gradientInterpolation[0] == 1) {
               int size = this.softEdge ? this.blockPercentages.size() + 1 : this.blockPercentages.size();
               interpolationArray = new DoubleUnaryOperator[size - 1];
               blockArray = new BlockWithFloat[size];

               for (int i = 0; i < size; i++) {
                  if (i < interpolationArray.length) {
                     interpolationArray[i] = new LinearOperator(i, size);
                  }

                  if (i < this.blockPercentages.size()) {
                     blockArray[i] = this.blockPercentages.get(i);
                  }
               }
            } else if (this.gradientInterpolation[0] == 2) {
               int size = this.softEdge ? this.blockPercentages.size() + 1 : this.blockPercentages.size();
               interpolationArray = new DoubleUnaryOperator[size - 1];
               blockArray = new BlockWithFloat[size];

               for (int i = 0; i < size; i++) {
                  if (i < interpolationArray.length) {
                     interpolationArray[i] = new BezierOperator(i, size);
                  }

                  if (i < this.blockPercentages.size()) {
                     blockArray[i] = this.blockPercentages.get(i);
                  }
               }
            } else {
               interpolationArray = null;
               blockArray = null;
            }

            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            if (blockArray != null && interpolationArray != null) {
               String seedStr = ImGuiHelper.getString(this.noiseSeed).trim();
               int seed;
               if (seedStr.isEmpty()) {
                  seed = this.defaultRandomValue;
               } else {
                  try {
                     seed = Integer.parseInt(seedStr);
                  } catch (NumberFormatException var17) {
                     seed = 0;

                     for (char c : seedStr.toCharArray()) {
                        seed = 31 * seed + c;
                     }
                  }
               }

               WhiteNoise whiteNoise = new WhiteNoise(seed);
               float minimumRadius = this.gradientInterpolation[0] == 2 ? this.minimumRadius[0] : 0.0F;
               return new AsyncToolPatherMinSDF(
                  brushShape,
                  (x, y, z, sdf) -> {
                     if (!blacklistedPositions.contains(x, y, z)) {
                        if (!destMask.test(maskContext.reset(), x, y, z)) {
                           blacklistedPositions.add(x, y, z);
                        } else if (this.maskSurface
                           && maskContext.getBlockState(x, y, z, 1, 0, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, -1, 0, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, 1, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, -1, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, 0, 1).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, 0, -1).blocksMotion()) {
                           blacklistedPositions.add(x, y, z);
                        } else {
                           if (this.mergeStrokes) {
                              float old = this.minDistances.get(x, y, z);
                              if (old < sdf) {
                                 sdf = old;
                              } else {
                                 this.minDistances.put(x, y, z, sdf);
                              }
                           }

                           if (sdf <= minimumRadius) {
                              BlockWithFloat blockWithFloat = blockArray[0];
                              if (blockWithFloat != null) {
                                 BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                                    this.chunkedBlockRegion,
                                    x,
                                    y,
                                    z,
                                    blockWithFloat.blockState(),
                                    typeReplace,
                                    copyProperties,
                                    blockWithFloat.randomProperties(),
                                    maskContext,
                                    mutableBlockPos
                                 );
                              }
                           } else {
                              sdf = (sdf - minimumRadius) / (1.0F - minimumRadius);
                              double randomValue = whiteNoise.evaluate(x + 0.5, y + 0.5, z + 0.5);
                              int i = 0;

                              for (DoubleUnaryOperator operator : interpolationArray) {
                                 randomValue -= Math.max(0.0, operator.applyAsDouble(sdf));
                                 if (randomValue < 0.0) {
                                    BlockWithFloat blockWithFloat = blockArray[i];
                                    if (blockWithFloat != null) {
                                       BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                                          this.chunkedBlockRegion,
                                          x,
                                          y,
                                          z,
                                          blockWithFloat.blockState(),
                                          typeReplace,
                                          copyProperties,
                                          blockWithFloat.randomProperties(),
                                          maskContext,
                                          mutableBlockPos
                                       );
                                    }

                                    return;
                                 }

                                 i++;
                              }

                              BlockWithFloat blockWithFloat = blockArray[blockArray.length - 1];
                              if (blockWithFloat != null) {
                                 BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                                    this.chunkedBlockRegion,
                                    x,
                                    y,
                                    z,
                                    blockWithFloat.blockState(),
                                    typeReplace,
                                    copyProperties,
                                    blockWithFloat.randomProperties(),
                                    maskContext,
                                    mutableBlockPos
                                 );
                              }
                           }
                        }
                     }
                  }
               );
            } else {
               float totalAmount = 0.0F;

               for (BlockWithFloat blockPercentage : this.blockPercentages) {
                  totalAmount += blockPercentage.percentage()[0] / 100.0F;
               }

               float totalAmountF = totalAmount;
               return totalAmount <= 0.0F
                  ? null
                  : new AsyncToolPatherMinSDF(
                     brushShape,
                     (x, y, z, sdf) -> {
                        if (!blacklistedPositions.contains(x, y, z)) {
                           if (!destMask.test(maskContext.reset(), x, y, z)) {
                              blacklistedPositions.add(x, y, z);
                           } else if (this.maskSurface
                              && maskContext.getBlockState(x, y, z, 1, 0, 0).blocksMotion()
                              && maskContext.getBlockState(x, y, z, -1, 0, 0).blocksMotion()
                              && maskContext.getBlockState(x, y, z, 0, 1, 0).blocksMotion()
                              && maskContext.getBlockState(x, y, z, 0, -1, 0).blocksMotion()
                              && maskContext.getBlockState(x, y, z, 0, 0, 1).blocksMotion()
                              && maskContext.getBlockState(x, y, z, 0, 0, -1).blocksMotion()) {
                              blacklistedPositions.add(x, y, z);
                           } else {
                              if (this.mergeStrokes) {
                                 float old = this.minDistances.get(x, y, z);
                                 if (old < sdf) {
                                    sdf = old;
                                 } else {
                                    this.minDistances.put(x, y, z, sdf);
                                 }
                              }

                              for (BlockWithFloat blockPercentage : this.blockPercentages) {
                                 sdf -= blockPercentage.percentage()[0] / 100.0F / totalAmountF;
                                 if (sdf < 0.0F) {
                                    BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                                       this.chunkedBlockRegion,
                                       x,
                                       y,
                                       z,
                                       blockPercentage.blockState(),
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
         default:
            BlockState blockState = Tool.getActiveBlock();
            if (blockState.isAir()) {
               this.paintingAir = true;
            }

            FamilyMap.AxiomBlockFamily typeReplaceFamily = typeReplace ? FamilyMap.getFamilyForBase(blockState.getBlock()) : null;
            mutableBlockPos = new MutableBlockPos();
            return new AsyncToolPatherUnique(
               brushShape,
               (x, y, z) -> {
                  if (destMask.test(maskContext.reset(), x, y, z)) {
                     if (!this.maskSurface
                        || !maskContext.getBlockState(x, y, z, 1, 0, 0).blocksMotion()
                        || !maskContext.getBlockState(x, y, z, -1, 0, 0).blocksMotion()
                        || !maskContext.getBlockState(x, y, z, 0, 1, 0).blocksMotion()
                        || !maskContext.getBlockState(x, y, z, 0, -1, 0).blocksMotion()
                        || !maskContext.getBlockState(x, y, z, 0, 0, 1).blocksMotion()
                        || !maskContext.getBlockState(x, y, z, 0, 0, -1).blocksMotion()) {
                        if (typeReplaceFamily != null) {
                           BlockState existing = maskContext.getBlockState(x, y, z);
                           BlockState to = FamilyMap.typeReplace(existing, typeReplaceFamily, mutableBlockPos.set(x, y, z), Minecraft.getInstance().level);
                           if (to != null) {
                              this.chunkedBlockRegion.addBlock(x, y, z, to);
                           }
                        } else if (copyProperties) {
                           BlockState existing = maskContext.getBlockState(x, y, z);
                           BlockState newState = blockState;

                           for (Property<?> property : existing.getProperties()) {
                              if (newState.hasProperty(property)) {
                                 newState = copyProperty(existing, newState, property);
                              }
                           }

                           this.chunkedBlockRegion.addBlock(x, y, z, newState);
                        } else {
                           this.chunkedBlockRegion.addBlock(x, y, z, blockState);
                           if (this.paintingAir) {
                              this.airRegion.add(x, y, z);
                           }
                        }
                     }
                  }
               }
            );
      }
   }

   @Override
   public void displayImguiOptions() {
      boolean changed = false;
      if (this.mode[0] != 2) {
         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
         changed |= this.brushWidget.displayImgui();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.painter"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.generic.mask_surface"), this.maskSurface)) {
         this.maskSurface = !this.maskSurface;
         changed = true;
      }

      if (this.mode[0] == 0) {
         boolean showCopyProperties = true;
         boolean filledLine = false;
         if (FamilyMap.getFamilyForBase(Tool.getActiveBlock().getBlock()) != null) {
            ImGui.sameLine();
            if (ImGui.checkbox(AxiomI18n.get("axiom.contextmenu.type_replace"), this.typeReplace)) {
               this.typeReplace = !this.typeReplace;
            }

            if (this.typeReplace) {
               showCopyProperties = false;
            }

            filledLine = true;
         }

         if (showCopyProperties && !Tool.getActiveBlock().getProperties().isEmpty()) {
            if (!filledLine) {
               ImGui.sameLine();
            }

            if (ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.replace.copy_properties"), this.copyProperties)) {
               this.copyProperties = !this.copyProperties;
            }
         }
      }

      changed |= ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.painter.paint_mode"),
         this.mode,
         new String[]{
            AxiomI18n.get("axiom.tool.painter.paint_mode.active_block"),
            AxiomI18n.get("axiom.tool.painter.paint_mode.tiled_clipboard"),
            AxiomI18n.get("axiom.tool.painter.paint_mode.stamped_clipboard"),
            AxiomI18n.get("axiom.tool.painter.paint_mode.gradient")
         }
      );
      if (this.mode[0] == 1) {
         ClipboardObject clipboardObject = Clipboard.INSTANCE.getClipboard();
         boolean showEmptyWarning;
         if (clipboardObject == null) {
            showEmptyWarning = true;
         } else {
            ChunkedBlockRegion copyFrom = clipboardObject.blockRegion();
            showEmptyWarning = copyFrom.isEmpty() || copyFrom.min() == null || copyFrom.max() == null;
         }

         if (showEmptyWarning) {
            ImGuiHelper.setupBorder();
            ImGui.text("⚠ " + AxiomI18n.get("axiom.tool.painter.clipboard_empty_warning"));
            ImGuiHelper.finishBorder();
         }
      } else if (this.mode[0] == 3) {
         ImGuiHelper.setupBorder();
         changed |= ImGuiHelper.radio(
            AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation"),
            this.gradientInterpolation,
            new String[]{
               AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation_nearest"),
               AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation_linear"),
               AxiomI18n.get("axiom.tool.gradient_painter.gradient_interpolation_bezier")
            }
         );
         if (this.gradientInterpolation[0] != 0) {
            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.noise"));
            changed |= ImGui.inputText(AxiomI18n.get("axiom.tool.generic.noise_seed"), this.noiseSeed);
            if (ImGui.button(AxiomI18n.get("axiom.tool.generic.noise_seed_do_randomize"))) {
               this.noiseSeed.set(String.valueOf(ThreadLocalRandom.current().nextInt()), false);
               changed = true;
            }

            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
            if (ImGui.checkbox(AxiomI18n.get("axiom.tool.painter.soft_edge"), this.softEdge)) {
               this.softEdge = !this.softEdge;
               changed = true;
            }
         } else {
            ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.options"));
         }

         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.painter.merge_strokes"), this.mergeStrokes)) {
            this.mergeStrokes = !this.mergeStrokes;
            changed = true;
         }

         if (this.gradientInterpolation[0] == 2) {
            changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.painter.inner_radius"), this.minimumRadius, 0.0F, 1.0F, "%.2f");
         }

         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.noise_painter.blocks"));
         BlockWithFloat.ExtraRenderType extraRenderType = this.gradientInterpolation[0] == 0
            ? BlockWithFloat.ExtraRenderType.PERCENTAGE
            : BlockWithFloat.ExtraRenderType.BLOCKNAME;
         changed |= BlockWithFloat.renderList(this.blockPercentages, this.selectBlockWidget, extraRenderType, this.softEdge ? 1 : 2, true);
         if (this.mode[0] == 3) {
            if (this.canCopyProperties() && ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.replace.copy_properties"), this.copyProperties)) {
               this.copyProperties = !this.copyProperties;
            }

            if (this.canTypeReplace() && ImGui.checkbox(AxiomI18n.get("axiom.contextmenu.type_replace"), this.typeReplace)) {
               this.typeReplace = !this.typeReplace;
            }
         }

         ImGuiHelper.finishBorder();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(changed);
   }

   private boolean canCopyProperties() {
      if (this.mode[0] == 0) {
         BlockState blockState = Tool.getActiveBlock();
         return !blockState.getProperties().isEmpty();
      } else {
         if (this.mode[0] == 3) {
            for (BlockWithFloat blockThreshold : this.blockPercentages) {
               if (!blockThreshold.blockState().getProperties().isEmpty()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean canTypeReplace() {
      if (this.mode[0] == 0) {
         BlockState blockState = Tool.getActiveBlock();
         return FamilyMap.getFamilyForBase(blockState.getBlock()) != null;
      } else {
         if (this.mode[0] == 3) {
            for (BlockWithFloat blockThreshold : this.blockPercentages) {
               if (!blockThreshold.blockState().getProperties().isEmpty()) {
                  return true;
               }
            }
         }

         return true;
      }
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
      return AxiomI18n.get("axiom.tool.painter");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putBoolean("MaskSurface", this.maskSurface);
      tag.putByte("Mode", (byte)this.mode[0]);
      tag.putByte("GradientInterpolation", (byte)this.gradientInterpolation[0]);
      ListTag blockPercentages = new ListTag();

      for (BlockWithFloat blockWithFloat : this.blockPercentages) {
         CompoundTag blockWithFloatTag = new CompoundTag();
         blockWithFloatTag.putString("Block", ServerCustomBlocks.serialize(blockWithFloat.blockState()));
         blockWithFloatTag.putFloat("Percentage", blockWithFloat.percentage()[0]);
         blockPercentages.add(blockWithFloatTag);
      }

      tag.put("BlockPercentages", blockPercentages);
      tag.putString("NoiseSeed", ImGuiHelper.getString(this.noiseSeed));
      tag.putBoolean("SoftEdge", this.softEdge);
      tag.putBoolean("MergeStrokes", this.mergeStrokes);
      tag.putFloat("MinimumRadius", this.minimumRadius[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.maskSurface = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "MaskSurface", true);
      this.mode[0] = VersionUtilsNbt.helperCompoundTagGetByteOr(tag, "Mode", (byte)0);
      this.gradientInterpolation[0] = VersionUtilsNbt.helperCompoundTagGetByteOr(tag, "GradientInterpolation", (byte)0);
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

      if (this.blockPercentages.isEmpty()) {
         this.blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.STONE.defaultBlockState(), new float[]{50.0F}, null));
         this.blockPercentages.add(new BlockWithFloat((CustomBlockState)Blocks.GRANITE.defaultBlockState(), new float[]{50.0F}, null));
      }

      this.noiseSeed.set(VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "NoiseSeed", String.valueOf(this.defaultRandomValue)), false);
      this.softEdge = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "SoftEdge", false);
      this.mergeStrokes = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "MergeStrokes", true);
      this.minimumRadius[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "MinimumRadius", 0.0F);
   }

   public void setGradientBlocks(List<CustomBlockState> blocks) {
      this.mode[0] = 3;
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
      return '\ue910';
   }

   @Override
   public String keybindId() {
      return "painter";
   }

   @Override
   public int defaultKeybind() {
      return 80;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_PAINTER, AxiomPermission.BUILD_SECTION);
   }
}
