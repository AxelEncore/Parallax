package com.moulberry.axiom.tools.noise_painter;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.block_maps.BlockColourMap;
import com.moulberry.axiom.block_maps.FamilyMap;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.custom_blocks.CustomBlockState;
import com.moulberry.axiom.custom_blocks.ServerCustomBlocks;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.editor.widgets.SelectBlockWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.noise.FBMNoise;
import com.moulberry.axiom.noise.MetaballNoise;
import com.moulberry.axiom.noise.NoiseInterface;
import com.moulberry.axiom.noise.SimplexNoise;
import com.moulberry.axiom.noise.SplatterNoise;
import com.moulberry.axiom.noise.VoronoiEdgesNoise;
import com.moulberry.axiom.noise.WhiteNoise;
import com.moulberry.axiom.noise.WorleyNoise;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.AutoCleaningDynamicTexture;
import com.moulberry.axiom.utils.BlockManipulation;
import com.moulberry.axiom.utils.BlockWithFloat;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.utils.NbtHelper;
import com.moulberry.axiom.utils.OkLabColourUtils;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.type.ImString;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class NoisePainterTool implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private boolean painting = false;
   private AsyncToolPathProvider pathProvider = null;
   private NoiseInterface currentNoise = null;
   private AutoCleaningDynamicTexture previewTexture = null;
   private AutoCleaningDynamicTexture cumulativeDistributionTexture = null;
   private AutoCleaningDynamicTexture probabilityDensityTexture = null;
   private final int[] frequencyCounts = new int[64];
   private int frequencyIterations = 0;
   private int maxFrequency = 0;
   private boolean previewDirty = false;
   private final BrushWidget brushWidget = new BrushWidget();
   private boolean threeDimensionalMode = true;
   private boolean maskSurface = true;
   private boolean anisotropic = false;
   private boolean copyProperties = false;
   private boolean typeReplace = false;
   private final int[] noiseScaleX = new int[]{8};
   private final int[] noiseScaleY = new int[]{8};
   private final int[] noiseScaleZ = new int[]{8};
   private final int[] noiseType = new int[]{0};
   private final ImString noiseSeed = new ImString();
   private final int[] octaves = new int[]{1};
   private final float[] lacunarity = new float[]{2.0F};
   private final float[] gain = new float[]{0.5F};
   private final float[] jitter = new float[]{1.0F};
   private final float[] worleyFirstFactor = new float[]{-1.0F};
   private final float[] worleySecondFactor = new float[]{0.7F};
   private final float[] worleyThirdFactor = new float[]{0.3F};
   private final float[] metaballRange = new float[]{1.0F};
   private final int[] blockMode = new int[]{0};
   private final List<BlockWithFloat> blockThresholds = new ArrayList<>();
   private final SelectBlockWidget selectBlockWidget = new SelectBlockWidget(false);
   private final PresetWidget presetWidget = new PresetWidget(this, "noise_painter");
   private final int defaultRandomValue = ThreadLocalRandom.current().nextInt();

   public NoisePainterTool() {
      this.blockThresholds.add(new BlockWithFloat((CustomBlockState)Blocks.STONE.defaultBlockState(), new float[]{50.0F}, null));
      this.noiseSeed.set(String.valueOf(this.defaultRandomValue), false);
   }

   @Override
   public void reset() {
      this.painting = false;
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
            AsyncToolPather pather = this.createToolPather(this.brushWidget.getBrushShape());
            if (pather == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.painting = true;
            this.pathProvider = new AsyncToolPathProvider(pather);
            return UserAction.ActionResult.USED_STOP;
         case ESCAPE:
            if (this.painting) {
               this.reset();
               return UserAction.ActionResult.USED_STOP;
            }
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (this.previewTexture == null) {
         this.previewTexture = new AutoCleaningDynamicTexture(64, 64, true);
         this.cumulativeDistributionTexture = new AutoCleaningDynamicTexture(256, 64, true);
         this.probabilityDensityTexture = new AutoCleaningDynamicTexture(256, 64, true);
      }

      if (!this.painting) {
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
         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.painted", countString);
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         Selection.render(rc, 4);
         this.pathProvider.update();
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.chunkedBlockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
      }
   }

   private AsyncToolPather createToolPather(BrushShape brushShape) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         MaskElement solidDestMask = MaskManager.createSolidDestMask();
         MaskContext maskContext = new MaskContext(level);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         if (this.currentNoise == null) {
            Arrays.fill(this.frequencyCounts, 0);
            this.frequencyIterations = 0;
            this.maxFrequency = 0;
         }

         this.updateNoise();
         NoiseInterface noiseInterface = this.currentNoise;
         int scaleX = this.noiseType[0] == 4 ? 1 : this.noiseScaleX[0];
         int scaleY = this.noiseType[0] == 4 ? 1 : (this.anisotropic ? this.noiseScaleY[0] : this.noiseScaleX[0]);
         int scaleZ = this.noiseType[0] == 4 ? 1 : (this.anisotropic ? this.noiseScaleZ[0] : this.noiseScaleX[0]);
         if (this.blockMode[0] == 0 && this.noiseType[0] != 4 && this.frequencyIterations < 128) {
            if (Runtime.getRuntime().availableProcessors() <= 1) {
               while (this.frequencyIterations < 128) {
                  this.updateFrequenciesWithSampling();
               }
            } else {
               ForkJoinPool forkJoinPool = new ForkJoinPool();
               List<ForkJoinTask<int[]>> futures = new ArrayList<>();
               boolean threeDimensionalMode = this.threeDimensionalMode;
               int frequencyLength = this.frequencyCounts.length;
               int count = 128 - this.frequencyIterations;

               for (int i = 0; i < count; i++) {
                  ForkJoinTask<int[]> future = forkJoinPool.submit(() -> {
                     int[] samplesx = new int[frequencyLength];
                     this.updateFrequenciesWithSamplingForArray(samplesx, scaleX, scaleY, scaleZ, threeDimensionalMode);
                     return samplesx;
                  });
                  futures.add(future);
               }

               for (ForkJoinTask<int[]> future : futures) {
                  int[] samples = future.join();

                  for (int i = 0; i < samples.length; i++) {
                     this.frequencyCounts[i] = this.frequencyCounts[i] + samples[i];
                  }
               }

               for (int frequencyCount : this.frequencyCounts) {
                  if (frequencyCount > this.maxFrequency) {
                     this.maxFrequency = frequencyCount;
                  }
               }

               this.frequencyIterations = 128;
            }

            this.previewDirty = true;
         }

         List<NoisePainterTool.CalculatedBlockThreshold> thresholdInformation = this.calculateBlockThresholds();
         if (thresholdInformation.isEmpty()) {
            return null;
         } else {
            boolean copyProperties = this.canCopyProperties() && this.copyProperties;
            boolean typeReplace = this.canTypeReplace() && this.typeReplace;
            return new AsyncToolPatherUnique(
               brushShape,
               (x, y, z) -> {
                  if (solidDestMask.test(maskContext.reset(), x, y, z)) {
                     if (!this.maskSurface
                        || level.getBlockState(mutableBlockPos.set(x + 1, y, z)).canBeReplaced()
                        || level.getBlockState(mutableBlockPos.set(x - 1, y, z)).canBeReplaced()
                        || level.getBlockState(mutableBlockPos.set(x, y + 1, z)).canBeReplaced()
                        || level.getBlockState(mutableBlockPos.set(x, y - 1, z)).canBeReplaced()
                        || level.getBlockState(mutableBlockPos.set(x, y, z + 1)).canBeReplaced()
                        || level.getBlockState(mutableBlockPos.set(x, y, z - 1)).canBeReplaced()) {
                        float noise;
                        if (this.threeDimensionalMode) {
                           noise = noiseInterface.evaluate((x + 0.5) / scaleX, (y + 0.5) / scaleY, (z + 0.5) / scaleZ);
                        } else {
                           noise = noiseInterface.evaluate((x + 0.5) / scaleX, (z + 0.5) / scaleY);
                        }

                        for (NoisePainterTool.CalculatedBlockThreshold information : thresholdInformation) {
                           if (noise <= information.threshold) {
                              BlockManipulation.setWithCopyPropertiesAndTypeReplace(
                                 this.chunkedBlockRegion,
                                 x,
                                 y,
                                 z,
                                 information.blockState,
                                 typeReplace,
                                 copyProperties,
                                 information.randomizedProperties,
                                 maskContext,
                                 mutableBlockPos
                              );
                              return;
                           }
                        }
                     }
                  }
               }
            );
         }
      }
   }

   @Override
   public void displayImguiOptions() {
      if (this.previewTexture == null) {
         this.previewTexture = new AutoCleaningDynamicTexture(64, 64, true);
         this.cumulativeDistributionTexture = new AutoCleaningDynamicTexture(256, 64, true);
         this.probabilityDensityTexture = new AutoCleaningDynamicTexture(256, 64, true);
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean settingsChanged = this.brushWidget.displayImgui();
      boolean frequencyChanged = false;
      if (this.noiseType[0] != 4) {
         if (!this.anisotropic) {
            frequencyChanged |= ImGui.sliderScalar(AxiomI18n.get("axiom.tool.noise_painter.scale"), this.noiseScaleX, 1, 64, "%d");
         } else {
            frequencyChanged |= ImGui.sliderScalar(AxiomI18n.get("axiom.tool.noise_painter.scale") + " X", this.noiseScaleX, 1, 64, "%d");
            frequencyChanged |= ImGui.sliderScalar(AxiomI18n.get("axiom.tool.noise_painter.scale") + " Y", this.noiseScaleY, 1, 64, "%d");
            if (this.threeDimensionalMode) {
               frequencyChanged |= ImGui.sliderScalar(AxiomI18n.get("axiom.tool.noise_painter.scale") + " Z", this.noiseScaleZ, 1, 64, "%d");
            }
         }
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.noise_painter.3d_mode"), this.threeDimensionalMode)) {
         this.threeDimensionalMode = !this.threeDimensionalMode;
         frequencyChanged = true;
      }

      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.generic.mask_surface_short"), this.maskSurface)) {
         this.maskSurface = !this.maskSurface;
         settingsChanged = true;
      }

      if (this.noiseType[0] != 4) {
         ImGui.sameLine();
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.noise_painter.anisotropic"), this.anisotropic)) {
            this.anisotropic = !this.anisotropic;
            frequencyChanged = true;
         }
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.noise"));
      frequencyChanged |= ImGuiHelper.combo(
         "##NoiseCombo",
         this.noiseType,
         new String[]{
            AxiomI18n.get("axiom.tool.noise_painter.simplex_noise"),
            AxiomI18n.get("axiom.tool.noise_painter.voronoi_edges"),
            AxiomI18n.get("axiom.tool.noise_painter.worley_noise"),
            AxiomI18n.get("axiom.tool.noise_painter.metaball_noise"),
            AxiomI18n.get("axiom.tool.noise_painter.white_noise"),
            AxiomI18n.get("axiom.tool.noise_painter.splatter_noise")
         }
      );
      switch (this.noiseType[0]) {
         case 1:
            frequencyChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.noise_painter.jitter"), this.jitter, 0.0F, 1.0F, "%.2f");
            break;
         case 2:
            frequencyChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.noise_painter.jitter"), this.jitter, 0.0F, 1.0F, "%.2f");
            frequencyChanged |= ImGui.sliderFloat("W1", this.worleyFirstFactor, -1.0F, 1.0F, "%.2f");
            frequencyChanged |= ImGui.sliderFloat("W2", this.worleySecondFactor, -1.0F, 1.0F, "%.2f");
            frequencyChanged |= ImGui.sliderFloat("W3", this.worleyThirdFactor, -1.0F, 1.0F, "%.2f");
            break;
         case 3:
            frequencyChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.noise_painter.jitter"), this.jitter, 0.0F, 1.0F, "%.2f");
            frequencyChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.noise_painter.metaball_range"), this.metaballRange, 0.0F, 1.5F, "%.2f");
      }

      if (this.noiseType[0] != 4 && this.noiseType[0] != 5) {
         frequencyChanged |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.noise_painter.octaves"), this.octaves, 1, 8);
         if (this.octaves[0] > 1) {
            frequencyChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.noise_painter.lacunarity"), this.lacunarity, 1.0F, 3.0F);
            frequencyChanged |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.noise_painter.gain"), this.gain, 0.0F, 1.0F);
         }
      }

      settingsChanged |= ImGui.inputText(AxiomI18n.get("axiom.tool.generic.noise_seed"), this.noiseSeed);
      if (ImGui.button(AxiomI18n.get("axiom.tool.generic.noise_seed_do_randomize"))) {
         settingsChanged = true;
         this.noiseSeed.set(String.valueOf(ThreadLocalRandom.current().nextInt()), false);
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.noise_painter.blocks"));
      settingsChanged |= ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.noise_painter.block_mode") + "##BlockMode",
         this.blockMode,
         new String[]{AxiomI18n.get("axiom.tool.noise_painter.block_mode_count"), AxiomI18n.get("axiom.tool.noise_painter.block_mode_threshold")}
      );
      BlockWithFloat.ExtraRenderType extraRenderType = this.blockMode[0] == 0
         ? BlockWithFloat.ExtraRenderType.PERCENTAGE
         : BlockWithFloat.ExtraRenderType.FLOAT;
      if (BlockWithFloat.renderList(this.blockThresholds, this.selectBlockWidget, extraRenderType, 1, true)) {
         settingsChanged = true;
      }

      if (this.canCopyProperties() && ImGui.checkbox(AxiomI18n.get("axiom.editorui.window.replace.copy_properties"), this.copyProperties)) {
         this.copyProperties = !this.copyProperties;
      }

      if (this.canTypeReplace() && ImGui.checkbox(AxiomI18n.get("axiom.contextmenu.type_replace"), this.typeReplace)) {
         this.typeReplace = !this.typeReplace;
      }

      settingsChanged |= frequencyChanged;
      if (frequencyChanged || this.currentNoise == null) {
         Arrays.fill(this.frequencyCounts, 0);
         this.frequencyIterations = 0;
         this.maxFrequency = 0;
         this.previewDirty = true;
      }

      if (settingsChanged || this.currentNoise == null) {
         this.updateNoise();
         this.previewDirty = true;
      }

      if (this.frequencyIterations < 128) {
         this.updateFrequenciesWithSampling();
         this.previewDirty = true;
      }

      if (this.previewDirty) {
         this.previewDirty = false;
         NativeImage pixels = this.previewTexture.getPixels();
         List<NoisePainterTool.CalculatedBlockThreshold> colourInformation = this.calculateBlockThresholds();
         int scaleX = this.noiseType[0] == 4 ? 1 : this.noiseScaleX[0];
         int scaleY = this.noiseType[0] == 4 ? 1 : (this.anisotropic ? this.noiseScaleY[0] : this.noiseScaleX[0]);

         for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
               float noise;
               if (this.threeDimensionalMode) {
                  noise = this.currentNoise.evaluate((x + 0.5) / scaleX, (y + 0.5) / scaleY, 0.5);
               } else {
                  noise = this.currentNoise.evaluate((x + 0.5) / scaleX, (y + 0.5) / scaleY);
               }

               int argb = 0;

               for (NoisePainterTool.CalculatedBlockThreshold information : colourInformation) {
                  if (noise <= information.threshold) {
                     Vec3 lab = BlockColourMap.getLab(information.blockState.getVanillaState().getBlock());
                     if (lab == null) {
                        argb = information.argb;
                     } else {
                        argb = 0xFF000000 | OkLabColourUtils.lab2rgb(lab.x, lab.y, lab.z);
                     }
                     break;
                  }
               }

               pixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(argb));
            }
         }

         this.previewTexture.upload();
         NativeImage cumulativePixels = this.cumulativeDistributionTexture.getPixels();
         NativeImage densityPixels = this.probabilityDensityTexture.getPixels();
         List<BlockWithFloat> nonZeroBlockThresholds = new ArrayList<>();

         for (BlockWithFloat blockWithFloat : this.blockThresholds) {
            if (blockWithFloat.percentage()[0] > 1.0E-5) {
               nonZeroBlockThresholds.add(blockWithFloat);
            }
         }

         boolean[] thresholdMarkers = new boolean[256];
         if (!nonZeroBlockThresholds.isEmpty()) {
            if (this.blockMode[0] != 1 && this.noiseType[0] != 4) {
               float thresholdTotal = 0.0F;

               for (int i = 0; i < nonZeroBlockThresholds.size(); i++) {
                  BlockWithFloat blockThreshold = nonZeroBlockThresholds.get(i);
                  thresholdTotal += blockThreshold.percentage()[0] / 100.0F;
               }

               if (thresholdTotal < 1.0F) {
                  thresholdTotal = 1.0F;
               }

               float totalSamples = this.frequencyIterations * 8192;
               int thresholdIndex = 0;
               float thresholdAmount = nonZeroBlockThresholds.get(0).percentage()[0] / 100.0F / thresholdTotal;
               float currentAmount = 0.0F;
               float totalThreshold = thresholdAmount;

               for (int frequencyIndex = 0; frequencyIndex < this.frequencyCounts.length; frequencyIndex++) {
                  float oldCurrentAmount = currentAmount;
                  currentAmount += this.frequencyCounts[frequencyIndex] / totalSamples;
                  if (currentAmount >= thresholdAmount) {
                     float partialDistance = (thresholdAmount - oldCurrentAmount) / (currentAmount - oldCurrentAmount);
                     float thresholdSum = (frequencyIndex - 1 + partialDistance) / 63.0F;
                     if (totalThreshold > 0.995F && thresholdIndex == nonZeroBlockThresholds.size() - 1) {
                        thresholdSum = 1.0F;
                     }

                     int lowerThreshold = Math.round(thresholdSum * 255.0F - 1.0F);
                     int upperThreshold = Math.round(thresholdSum * 255.0F + 1.0F);
                     if (lowerThreshold < 0) {
                        lowerThreshold = 0;
                     }

                     if (upperThreshold > 255) {
                        upperThreshold = 255;
                     }

                     for (int j = lowerThreshold; j <= upperThreshold; j++) {
                        thresholdMarkers[j] = true;
                     }

                     thresholdIndex++;
                     currentAmount -= thresholdAmount;
                     if (thresholdIndex >= nonZeroBlockThresholds.size()) {
                        break;
                     }

                     thresholdAmount = nonZeroBlockThresholds.get(thresholdIndex).percentage()[0] / 100.0F / thresholdTotal;
                     totalThreshold += thresholdAmount;
                  }
               }

               if (thresholdIndex < nonZeroBlockThresholds.size()) {
                  thresholdMarkers[254] = true;
                  thresholdMarkers[255] = true;
               }
            } else {
               float thresholdTotal = 0.0F;

               for (int i = 0; i < nonZeroBlockThresholds.size(); i++) {
                  BlockWithFloat blockThreshold = nonZeroBlockThresholds.get(i);
                  thresholdTotal += blockThreshold.percentage()[0] / 100.0F;
               }

               if (thresholdTotal < 1.0F) {
                  thresholdTotal = 1.0F;
               }

               float thresholdSumx = 0.0F;

               for (int i = 0; i < nonZeroBlockThresholds.size(); i++) {
                  BlockWithFloat blockThreshold = nonZeroBlockThresholds.get(i);
                  thresholdSumx += blockThreshold.percentage()[0] / 100.0F / thresholdTotal;
                  if (thresholdSumx > 0.99F && i == nonZeroBlockThresholds.size() - 1) {
                     thresholdSumx = 1.0F;
                  }

                  if (thresholdSumx > 1.0F) {
                     thresholdSumx = 1.0F;
                  }

                  int lowerThresholdx = Math.round(thresholdSumx * 255.0F - 1.0F);
                  int upperThresholdx = Math.round(thresholdSumx * 255.0F + 1.0F);
                  if (lowerThresholdx < 0) {
                     lowerThresholdx = 0;
                  }

                  if (upperThresholdx > 255) {
                     upperThresholdx = 255;
                  }

                  for (int j = lowerThresholdx; j <= upperThresholdx; j++) {
                     thresholdMarkers[j] = true;
                  }
               }
            }
         }

         int minSize = 0;
         if (this.noiseType[0] == 3 && this.metaballRange[0] > 0.0F) {
            minSize = 1;
         }

         float sum = 0.0F;
         float[] cumulative = new float[256];

         for (int x = 0; x < 256; x++) {
            float indexF = x / 255.0F * 63.0F;
            int indexLower = (int)Math.floor(indexF);
            int indexUpper = (int)Math.ceil(indexF);
            float frac = indexF - indexLower;
            float frequencyCountLower = this.frequencyCounts[indexLower];
            float frequencyCountUpper = this.frequencyCounts[indexUpper];
            float count = frequencyCountLower * (1.0F - frac) + frequencyCountUpper * frac;
            sum += count;
            cumulative[x] = sum;
            int size = (int)Math.floor(count / this.maxFrequency * 64.0F);
            if (size > 64) {
               size = 64;
            }

            if (size < minSize) {
               size = minSize;
            }

            int set = -1;
            int unset = -16777216;
            if (thresholdMarkers[x]) {
               set = -65536;
               unset = -8388608;
            }

            for (int y = 0; y < size; y++) {
               densityPixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(set));
            }

            for (int y = size; y < 64; y++) {
               densityPixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(unset));
            }
         }

         for (int x = 0; x < 256; x++) {
            int sizex = (int)Math.floor(cumulative[x] / sum * 64.0F);
            if (sizex > 64) {
               sizex = 64;
            }

            if (sizex < minSize) {
               sizex = minSize;
            }

            int set = -1;
            int unset = -16777216;
            if (thresholdMarkers[x]) {
               set = -65536;
               unset = -8388608;
            }

            for (int y = 0; y < sizex; y++) {
               cumulativePixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(set));
            }

            for (int y = sizex; y < 64; y++) {
               cumulativePixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(unset));
            }
         }

         this.cumulativeDistributionTexture.upload();
         this.probabilityDensityTexture.upload();
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(settingsChanged);
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.noise_painter.noise_preview"));
      ImGui.image(this.previewTexture.getId(), 256.0F, 256.0F, 0.0F, 1.0F, 1.0F, 0.0F);
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.noise_painter.cumulative_distribution"));
      ImGui.image(this.cumulativeDistributionTexture.getId(), 256.0F, 64.0F, 0.0F, 1.0F, 1.0F, 0.0F);
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.noise_painter.probability_density"));
      ImGui.image(this.probabilityDensityTexture.getId(), 256.0F, 64.0F, 0.0F, 1.0F, 1.0F, 0.0F);
   }

   private boolean canCopyProperties() {
      for (BlockWithFloat blockThreshold : this.blockThresholds) {
         if (!blockThreshold.blockState().getProperties().isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private boolean canTypeReplace() {
      for (BlockWithFloat blockThreshold : this.blockThresholds) {
         if (FamilyMap.getFamilyForBase(blockThreshold.blockState().getVanillaState().getBlock()) == null) {
            return false;
         }
      }

      return true;
   }

   private void updateFrequenciesWithSampling() {
      if (this.currentNoise == null) {
         this.updateNoise();
      }

      int scaleX = this.noiseType[0] == 4 ? 1 : this.noiseScaleX[0];
      int scaleY = this.noiseType[0] == 4 ? 1 : (this.anisotropic ? this.noiseScaleY[0] : this.noiseScaleX[0]);
      int scaleZ = this.noiseType[0] == 4 ? 1 : (this.anisotropic ? this.noiseScaleZ[0] : this.noiseScaleX[0]);
      this.updateFrequenciesWithSamplingForArray(this.frequencyCounts, scaleX, scaleY, scaleZ, this.threeDimensionalMode);

      for (int frequencyCount : this.frequencyCounts) {
         if (frequencyCount > this.maxFrequency) {
            this.maxFrequency = frequencyCount;
         }
      }

      this.frequencyIterations++;
   }

   private void updateFrequenciesWithSamplingForArray(int[] frequencyCounts, int scaleX, int scaleY, int scaleZ, boolean threeDimensionalMode) {
      if (this.currentNoise == null) {
         throw new FaultyImplementationError();
      } else {
         Random random = ThreadLocalRandom.current();

         for (int i = 0; i < 8192; i++) {
            int x = random.nextInt() & -1073741825;
            int y = random.nextInt() & -1073741825;
            float noise;
            if (threeDimensionalMode) {
               int z = random.nextInt() & -1073741825;
               noise = this.currentNoise.evaluate((x + 0.5) / scaleX, (y + 0.5) / scaleY, (z + 0.5) / scaleZ);
            } else {
               noise = this.currentNoise.evaluate((x + 0.5) / scaleX, (y + 0.5) / scaleY);
            }

            int index = Math.round(noise * (frequencyCounts.length - 1));
            frequencyCounts[index]++;
         }
      }
   }

   private void updateNoise() {
      int octaves = this.octaves[0];
      String seedStr = ImGuiHelper.getString(this.noiseSeed).trim();
      int seed;
      if (seedStr.isEmpty()) {
         seed = this.defaultRandomValue;
      } else {
         try {
            seed = Integer.parseInt(seedStr);
         } catch (NumberFormatException var10) {
            seed = 0;

            for (char c : seedStr.toCharArray()) {
               seed = 31 * seed + c;
            }
         }
      }
      this.currentNoise = (NoiseInterface)(switch (this.noiseType[0]) {
         case 0 -> {
            if (octaves <= 1) {
               yield new SimplexNoise(seed);
            } else {
               NoiseInterface[] noises = new NoiseInterface[octaves];

               for (int i = 0; i < octaves; i++) {
                  noises[i] = new SimplexNoise(seed + i);
               }

               yield new FBMNoise(this.lacunarity[0], this.gain[0], noises);
            }
         }
         case 1 -> {
            float jitter = this.jitter[0];
            if (octaves <= 1) {
               yield new VoronoiEdgesNoise(seed, jitter);
            } else {
               NoiseInterface[] noises = new NoiseInterface[octaves];

               for (int i = 0; i < octaves; i++) {
                  noises[i] = new VoronoiEdgesNoise(seed + 10L * i, jitter);
               }

               yield new FBMNoise(this.lacunarity[0], this.gain[0], noises);
            }
         }
         case 2 -> {
            float jitter = this.jitter[0];
            float firstFactor = this.worleyFirstFactor[0];
            float secondFactor = this.worleySecondFactor[0];
            float thirdFactor = this.worleyThirdFactor[0];
            if (octaves <= 1) {
               yield new WorleyNoise(seed, jitter, firstFactor, secondFactor, thirdFactor);
            } else {
               NoiseInterface[] noises = new NoiseInterface[octaves];

               for (int i = 0; i < octaves; i++) {
                  noises[i] = new WorleyNoise(seed + i, jitter, firstFactor, secondFactor, thirdFactor);
               }

               yield new FBMNoise(this.lacunarity[0], this.gain[0], noises);
            }
         }
         case 3 -> {
            float jitter = this.jitter[0];
            if (octaves <= 1) {
               yield new MetaballNoise(seed, jitter, this.metaballRange[0]);
            } else {
               NoiseInterface[] noises = new NoiseInterface[octaves];

               for (int i = 0; i < octaves; i++) {
                  noises[i] = new MetaballNoise(seed + i, jitter, this.metaballRange[0]);
               }

               yield new FBMNoise(this.lacunarity[0], this.gain[0], noises);
            }
         }
         case 4 -> new WhiteNoise(seed);
         case 5 -> {
            int scaleX = this.noiseScaleX[0];
            int scaleY = this.anisotropic ? this.noiseScaleY[0] : this.noiseScaleX[0];
            int scaleZ = this.anisotropic ? this.noiseScaleZ[0] : this.noiseScaleX[0];
            yield new SplatterNoise(seed, scaleX, scaleY, scaleZ);
         }
         default -> throw new FaultyImplementationError();
      });
   }

   @NotNull
   private List<NoisePainterTool.CalculatedBlockThreshold> calculateBlockThresholds() {
      List<NoisePainterTool.CalculatedBlockThreshold> thresholdInformation = new ArrayList<>();
      List<BlockWithFloat> nonZeroBlockThresholds = new ArrayList<>();

      for (BlockWithFloat blockWithFloat : this.blockThresholds) {
         if (blockWithFloat.percentage()[0] > 1.0E-5) {
            nonZeroBlockThresholds.add(blockWithFloat);
         }
      }

      if (nonZeroBlockThresholds.isEmpty()) {
         return List.of();
      } else {
         if (this.blockMode[0] != 1 && this.noiseType[0] != 4) {
            float thresholdTotal = 0.0F;

            for (int i = 0; i < nonZeroBlockThresholds.size(); i++) {
               BlockWithFloat blockThreshold = nonZeroBlockThresholds.get(i);
               thresholdTotal += blockThreshold.percentage()[0] / 100.0F;
            }

            if (thresholdTotal < 1.0F) {
               thresholdTotal = 1.0F;
            }

            float totalSamples = this.frequencyIterations * 8192;
            int thresholdIndex = 0;
            float thresholdAmount = nonZeroBlockThresholds.get(0).percentage()[0] / 100.0F / thresholdTotal;
            float currentAmount = 0.0F;
            float totalThreshold = thresholdAmount;

            label72:
            for (int frequencyIndex = 0; frequencyIndex < this.frequencyCounts.length; frequencyIndex++) {
               float oldCurrentAmount = currentAmount;

               for (currentAmount += this.frequencyCounts[frequencyIndex] / totalSamples; currentAmount >= thresholdAmount; totalThreshold += thresholdAmount) {
                  float partialDistance = (thresholdAmount - oldCurrentAmount) / (currentAmount - oldCurrentAmount);
                  float thresholdSum = (frequencyIndex - 1 + partialDistance) / 63.0F;
                  if (totalThreshold > 0.995F && thresholdIndex == nonZeroBlockThresholds.size() - 1) {
                     thresholdSum = 1.0F;
                  }

                  int distanceFromEnd = nonZeroBlockThresholds.size() - 1 - thresholdIndex;
                  float thresholdSumMax = 1.0F - 0.001F * distanceFromEnd;
                  if (thresholdSum > thresholdSumMax) {
                     thresholdSum = thresholdSumMax;
                  }

                  int colour = Math.round(255.0F - 255.0F * thresholdIndex / nonZeroBlockThresholds.size());
                  BlockWithFloat blockWithFloatx = nonZeroBlockThresholds.get(thresholdIndex);
                  thresholdInformation.add(
                     new NoisePainterTool.CalculatedBlockThreshold(
                        thresholdSum, 0xFF000000 | colour * 65793, blockWithFloatx.blockState(), blockWithFloatx.randomProperties()
                     )
                  );
                  thresholdIndex++;
                  currentAmount -= thresholdAmount;
                  if (thresholdIndex >= nonZeroBlockThresholds.size()) {
                     break label72;
                  }

                  thresholdAmount = nonZeroBlockThresholds.get(thresholdIndex).percentage()[0] / 100.0F / thresholdTotal;
               }
            }

            if (thresholdIndex < nonZeroBlockThresholds.size()) {
               int colour = Math.round(255.0F - 255.0F * thresholdIndex / nonZeroBlockThresholds.size());
               BlockWithFloat blockWithFloatx = nonZeroBlockThresholds.get(thresholdIndex);
               thresholdInformation.add(
                  new NoisePainterTool.CalculatedBlockThreshold(
                     1.0F, 0xFF000000 | colour * 65793, blockWithFloatx.blockState(), blockWithFloatx.randomProperties()
                  )
               );
            }
         } else {
            float thresholdTotal = 0.0F;

            for (int i = 0; i < nonZeroBlockThresholds.size(); i++) {
               BlockWithFloat blockThreshold = nonZeroBlockThresholds.get(i);
               thresholdTotal += blockThreshold.percentage()[0] / 100.0F;
            }

            if (thresholdTotal < 1.0F) {
               thresholdTotal = 1.0F;
            }

            float thresholdSumx = 0.0F;

            for (int i = 0; i < nonZeroBlockThresholds.size(); i++) {
               BlockWithFloat blockThreshold = nonZeroBlockThresholds.get(i);
               thresholdSumx += blockThreshold.percentage()[0] / 100.0F / thresholdTotal;
               if (thresholdSumx > 0.99F && i == nonZeroBlockThresholds.size() - 1) {
                  thresholdSumx = 1.0F;
               }

               int colour = Math.round(255.0F - 255.0F * i / nonZeroBlockThresholds.size());
               thresholdInformation.add(
                  new NoisePainterTool.CalculatedBlockThreshold(
                     thresholdSumx, 0xFF000000 | colour * 65793, blockThreshold.blockState(), blockThreshold.randomProperties()
                  )
               );
            }
         }

         return thresholdInformation;
      }
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
      return AxiomI18n.get("axiom.tool.noise_painter");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putBoolean("ThreeDimensional", this.threeDimensionalMode);
      tag.putBoolean("MaskSurface", this.maskSurface);
      tag.putBoolean("Anisotropic", this.anisotropic);
      tag.putInt("NoiseScaleX", this.noiseScaleX[0]);
      tag.putInt("NoiseScaleY", this.noiseScaleY[0]);
      tag.putInt("NoiseScaleZ", this.noiseScaleZ[0]);
      tag.putInt("NoiseType", this.noiseType[0]);
      tag.putString("NoiseSeed", ImGuiHelper.getString(this.noiseSeed));
      tag.putInt("Octaves", this.octaves[0]);
      tag.putFloat("Lacunarity", this.lacunarity[0]);
      tag.putFloat("Gain", this.gain[0]);
      tag.putFloat("Jitter", this.jitter[0]);
      tag.putFloat("W1", this.worleyFirstFactor[0]);
      tag.putFloat("W2", this.worleySecondFactor[0]);
      tag.putFloat("W3", this.worleyThirdFactor[0]);
      tag.putFloat("MetaballRange", this.metaballRange[0]);
      tag.putInt("BlockMode", this.blockMode[0]);
      ListTag blockThresholds = new ListTag();

      for (BlockWithFloat blockWithFloat : this.blockThresholds) {
         CompoundTag blockWithFloatTag = new CompoundTag();
         blockWithFloatTag.putString("Block", ServerCustomBlocks.serialize(blockWithFloat.blockState()));
         blockWithFloatTag.putFloat("Percentage", blockWithFloat.percentage()[0]);
         blockThresholds.add(blockWithFloatTag);
      }

      tag.put("BlockThresholds", blockThresholds);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.threeDimensionalMode = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "ThreeDimensional", true);
      this.maskSurface = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "MaskSurface", true);
      this.anisotropic = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "Anisotropic", false);
      this.noiseScaleX[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "NoiseScaleX", 8);
      this.noiseScaleY[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "NoiseScaleY", 8);
      this.noiseScaleZ[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "NoiseScaleZ", 8);
      this.noiseType[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "NoiseType", 0);
      this.noiseSeed.set(VersionUtilsNbt.helperCompoundTagGetStringOr(tag, "NoiseSeed", String.valueOf(this.defaultRandomValue)), false);
      this.octaves[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Octaves", 1);
      this.lacunarity[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Lacunarity", 2.0F);
      this.gain[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Gain", 0.5F);
      this.jitter[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Jitter", 1.0F);
      this.worleyFirstFactor[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "W1", -1.0F);
      this.worleySecondFactor[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "W2", 0.7F);
      this.worleyThirdFactor[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "W3", 0.3F);
      this.metaballRange[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "MetaballRange", 1.0F);
      this.blockMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BlockMode", 0);
      this.blockThresholds.clear();

      for (Tag blockPercentageTag : NbtHelper.getList(tag, "BlockThresholds", 10)) {
         CompoundTag blockPercentage = (CompoundTag)blockPercentageTag;
         String block = VersionUtilsNbt.helperCompoundTagGetString(blockPercentage, "Block").get();
         float percentage = VersionUtilsNbt.helperCompoundTagGetFloat(blockPercentage, "Percentage").get();
         this.blockThresholds
            .add(
               new BlockWithFloat(
                  Objects.requireNonNullElse(ServerCustomBlocks.deserialize(block), (CustomBlockState)Blocks.STONE.defaultBlockState()),
                  new float[]{percentage},
                  null
               )
            );
      }

      if (this.blockThresholds.isEmpty()) {
         this.blockThresholds.add(new BlockWithFloat((CustomBlockState)Blocks.STONE.defaultBlockState(), new float[]{50.0F}, null));
      }

      this.currentNoise = null;
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue90f';
   }

   @Override
   public String keybindId() {
      return "noise_painter";
   }

   @Override
   public int defaultKeybind() {
      return 79;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_NOISEPAINTER, AxiomPermission.BUILD_SECTION);
   }

   record CalculatedBlockThreshold(float threshold, int argb, CustomBlockState blockState, Set<Property<?>> randomizedProperties) {
   }
}
