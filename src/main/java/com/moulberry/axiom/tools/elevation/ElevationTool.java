package com.moulberry.axiom.tools.elevation;

import com.mojang.blaze3d.platform.NativeImage;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2dToFloatMap;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.FalloffWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.ToolPatherPoint;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.HeightmapApplier;
import com.moulberry.axiom.tools.ImageHeightmapProvider;
import com.moulberry.axiom.tools.ServerHeightmaps;
import com.moulberry.axiom.tools.SimpleRadiusAdjustment;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.AutoCleaningDynamicTexture;
import com.moulberry.axiom.utils.BooleanWrapper;
import com.moulberry.axiom.utils.ColourUtils;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class ElevationTool implements Tool {
   private final ChunkedBooleanRegion previewRegion = new ChunkedBooleanRegion();
   private int lastPreviewRadius = -1;
   private int lastPreviewHeight = -1;
   private boolean usingTool = false;
   private int initialY = 70;
   private ToolPatherPoint toolPatherPoint = null;
   private final HeightmapApplier heightmapApplier = new HeightmapApplier();
   private final Position2dToFloatMap heightAccumulation = new Position2dToFloatMap();
   private final Position2dToFloatMap maxFalloffForApplyModeOnce = new Position2dToFloatMap(Float.MIN_VALUE);
   public MaskElement cachedSourceMask = null;
   private MaskContext cachedMaskContext = null;
   private final float[] falloffLut = new float[1057];
   private int falloffLutSize = this.falloffLut.length;
   private long lastUpdateMillis = 0L;
   private CompletableFuture<BufferedImage> loadHeightmapFuture = null;
   private int customHeightmapSize = 0;
   private float customHeightmapZoom = 1.0F;
   private float[] customHeightmap = null;
   private AutoCleaningDynamicTexture heightmapDisplay = null;
   private boolean heightmapDisplayDirty = false;
   private final int[] radius = new int[]{8};
   private final int[] height = new int[]{63};
   private final float[] smoothing = new float[]{1.0F};
   private static final int APPLY_MODE_CONTINUOUS = 0;
   private static final int APPLY_MODE_ONCE = 1;
   private final int[] applyMode = new int[]{0};
   private final int[] rate = new int[]{8};
   private static final int MODE_RAISE = 0;
   private static final int MODE_LOWER = 1;
   private static final int MODE_FLATTEN = 2;
   private static final int MODE_FLATTEN_UP = 3;
   private static final int MODE_FLATTEN_DOWN = 4;
   private final int[] mode = new int[]{0};
   private final FalloffWidget falloffWidget = new FalloffWidget();
   private final PresetWidget presetWidget = new PresetWidget(this, "elevation");
   private boolean settingsChanged = false;
   private final float[] baseRadiusAdjustment = new float[]{0.0F};

   @Override
   public void reset() {
      this.partialReset();
      this.heightAccumulation.clear();
      this.cachedSourceMask = null;
      this.cachedMaskContext = null;
   }

   @Override
   public void toolDeselected() {
      if (this.heightmapDisplay != null) {
         this.heightmapDisplay.close();
         this.heightmapDisplay = null;
      }

      this.falloffWidget.unload();
      this.heightmapDisplayDirty = true;
   }

   private void partialReset() {
      if (this.usingTool) {
         this.usingTool = false;
         ChunkRenderOverrider.release("elevation_tool");
      }

      this.previewRegion.clear();
      this.lastPreviewRadius = -1;
      this.lastPreviewHeight = -1;
      this.heightmapApplier.reset(this.smoothing[0]);
      this.lastUpdateMillis = System.currentTimeMillis();
      this.maxFalloffForApplyModeOnce.clear();
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
            if (result == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.partialReset();
            int radius = this.radius[0];
            this.falloffLutSize = Math.min(this.falloffLut.length, radius * radius + radius + 1);
            FloatUnaryOperator operator = this.falloffWidget.getFalloffFunction();

            for (int i = 0; i < this.falloffLutSize; i++) {
               double distance = Math.sqrt(i) / Math.sqrt(this.falloffLutSize - 1);
               this.falloffLut[i] = operator.apply((float)distance);
            }

            if (!this.usingTool) {
               this.usingTool = true;
               ChunkRenderOverrider.acquire("elevation_tool");
            }

            this.toolPatherPoint = new ToolPatherPoint(false);
            this.initialY = result.blockPos().getY();
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
      Selection.render(rc, 4);
      if (!this.usingTool) {
         if (this.loadHeightmapFuture != null && this.loadHeightmapFuture.isDone()) {
            BufferedImage image = this.loadHeightmapFuture.join();
            this.setHeightmapImage(image);
         }

         RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
         if (result == null) {
            Selection.render(rc, 7);
            return;
         }

         int radius = this.radius[0];
         int height = this.height[0];
         if (this.lastPreviewRadius != radius || this.lastPreviewHeight != height) {
            this.lastPreviewRadius = radius;
            this.lastPreviewHeight = height;
            this.previewRegion.clear();
            float maxRadiusSq = (radius + 0.5F) * (radius + 0.5F);

            for (int x = -radius; x <= radius; x++) {
               for (int z = -radius; z <= radius; z++) {
                  if (x * x + z * z < maxRadiusSq) {
                     for (int y = -height; y <= height; y++) {
                        this.previewRegion.add(x, y, z);
                     }
                  }
               }
            }
         }

         Selection.render(rc, 4);
         this.previewRegion.render(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 2);
      } else if (Tool.cancelUsing()) {
         this.partialReset();
      } else if (!Tool.isMouseDown(1)) {
         String countString = NumberFormat.getInstance().format((long)this.heightmapApplier.blockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.elevation_tool", countString);
         RegionHelper.pushBlockRegionChange(this.heightmapApplier.blockRegion, historyDescription);
         this.heightmapApplier.iterateOriginalY((xx, zx, v) -> {
            int constrainedY = this.heightmapApplier.getConstrainedY(xx, zx);
            if (constrainedY != Integer.MIN_VALUE) {
               int delta = constrainedY - v;
               float accumulated = this.heightAccumulation.get(xx, zx);
               if (accumulated > 0.0F) {
                  if (delta >= 0 && !(delta > accumulated)) {
                     this.heightAccumulation.put(xx, zx, accumulated - delta);
                  } else {
                     this.heightAccumulation.put(xx, zx, 0.0F);
                  }
               } else if (accumulated < 0.0F) {
                  if (delta <= 0 && !(delta < accumulated)) {
                     this.heightAccumulation.put(xx, zx, accumulated - delta);
                  } else {
                     this.heightAccumulation.put(xx, zx, 0.0F);
                  }
               }
            }
         });
         this.partialReset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         LocalPlayer player = Minecraft.getInstance().player;
         if (player == null) {
            return;
         }

         Selection.render(rc, 4);
         this.process(level);
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.heightmapApplier.blockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
         this.heightmapApplier.removeRegion.render(rc, Vec3.ZERO, 8);
      }
   }

   private void setHeightmapImage(BufferedImage image) {
      this.loadHeightmapFuture = null;
      this.heightmapDisplayDirty = true;
      this.settingsChanged = true;
      if (image != null) {
         int size = Math.max(image.getWidth(), image.getHeight());
         int offsetX = (size - image.getWidth()) / 2;
         int offsetY = (size - image.getHeight()) / 2;
         float minValue = 0.2F;
         float maxValue = 0.8F;
         float[] values = new float[size * size];
         WritableRaster raster = image.getRaster();
         int type = image.getType();
         int index = 0;

         for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
               float value = 0.0F;
               if (x >= offsetX && y >= offsetY && x - offsetX < image.getWidth() && y - offsetY < image.getHeight()) {
                  switch (type) {
                     case 10: {
                        int sample = raster.getSample(x - offsetX, y - offsetY, 0);
                        value = sample / 255.0F;
                        break;
                     }
                     case 11: {
                        int sample = raster.getSample(x - offsetX, y - offsetY, 0);
                        value = sample / 65535.0F;
                        break;
                     }
                     default:
                        int argb = image.getRGB(x - offsetX, y - offsetY);
                        int red = argb >> 16 & 0xFF;
                        int green = argb >> 8 & 0xFF;
                        int blue = argb & 0xFF;
                        value = (red + green + blue) / 765.0F;
                  }

                  if (value < minValue) {
                     minValue = value;
                  }

                  if (value > maxValue) {
                     maxValue = value;
                  }
               }

               values[index++] = value;
            }
         }

         float delta = maxValue - minValue;
         index = 0;
         float maxDistanceSq = 0.5625F;

         for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
               int i = index++;
               float value = (values[i] - minValue) / delta;
               values[i] = value;
               if (value > 0.05F) {
                  float distanceX = (float)x / (size - 1) * 2.0F - 1.0F;
                  float distanceY = (float)y / (size - 1) * 2.0F - 1.0F;
                  float distanceSq = distanceX * distanceX + distanceY * distanceY;
                  if (distanceSq > maxDistanceSq) {
                     maxDistanceSq = distanceSq;
                  }
               }
            }
         }

         this.customHeightmapZoom = 1.0F / (float)Math.sqrt(maxDistanceSq);
         this.customHeightmapSize = size;
         this.customHeightmap = values;
      }
   }

   private void process(ClientLevel level) {
      int radius = this.radius[0];
      int height = this.height[0];
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      BooleanWrapper modified = new BooleanWrapper(false);
      long currentTime = System.currentTimeMillis();
      if (currentTime < this.lastUpdateMillis) {
         this.lastUpdateMillis = currentTime;
      } else if (currentTime != this.lastUpdateMillis) {
         long timeSince = currentTime - this.lastUpdateMillis;
         if (timeSince > 1000L) {
            timeSince = 1000L;
         }

         this.lastUpdateMillis = currentTime;
         float weightThisFrame = (float)(this.rate[0] * timeSince) / 1000.0F;
         Position2dToFloatMap maxAccumulateWeight = weightThisFrame > 0.0F && this.applyMode[0] == 0 ? new Position2dToFloatMap(0.0F) : null;
         this.toolPatherPoint.update((x, y, z) -> {
            modified.value = true;
            this.apply(level, radius, height, maxAccumulateWeight, mutableBlockPos, x, y, z);
         });
         if (!modified.value && maxAccumulateWeight != null) {
            RayCaster.RaycastResult result = Tool.raycastBlock(false, false, Tool.defaultIncludeFluids());
            if (result != null) {
               this.apply(
                  level, radius, height, maxAccumulateWeight, mutableBlockPos, result.blockPos().getX(), result.blockPos().getY(), result.blockPos().getZ()
               );
            }
         }

         if (maxAccumulateWeight != null) {
            maxAccumulateWeight.forEachEntry((x, z, weight) -> {
               int originalY = this.heightmapApplier.getOriginalY(x, z);

               int count = switch (this.mode[0]) {
                  case 1 -> (int)this.heightAccumulation.add(x, z, -weightThisFrame * weight);
                  case 2 -> {
                     int delta = originalY - this.initialY;
                     if (delta > 0) {
                        float heightAccum = this.heightAccumulation.get(x, z);
                        float newHeightAccum = heightAccum - weightThisFrame * weight;
                        if (newHeightAccum <= -delta) {
                           this.heightAccumulation.put(x, z, -delta);
                           yield -delta;
                        } else {
                           this.heightAccumulation.put(x, z, newHeightAccum);
                           yield (int)newHeightAccum;
                        }
                     } else if (delta < 0) {
                        float heightAccum = this.heightAccumulation.get(x, z);
                        float newHeightAccum = heightAccum + weightThisFrame * weight;
                        if (newHeightAccum >= -delta) {
                           this.heightAccumulation.put(x, z, -delta);
                           yield -delta;
                        } else {
                           this.heightAccumulation.put(x, z, newHeightAccum);
                           yield (int)newHeightAccum;
                        }
                     } else {
                        yield Integer.MIN_VALUE;
                     }
                  }
                  case 3 -> {
                     int delta = originalY - this.initialY;
                     if (delta < 0) {
                        float heightAccum = this.heightAccumulation.get(x, z);
                        float newHeightAccum = heightAccum + weightThisFrame * weight;
                        if (newHeightAccum >= -delta) {
                           this.heightAccumulation.put(x, z, -delta);
                           yield -delta;
                        } else {
                           this.heightAccumulation.put(x, z, newHeightAccum);
                           yield (int)newHeightAccum;
                        }
                     } else {
                        yield Integer.MIN_VALUE;
                     }
                  }
                  case 4 -> {
                     int delta = originalY - this.initialY;
                     if (delta > 0) {
                        float heightAccum = this.heightAccumulation.get(x, z);
                        float newHeightAccum = heightAccum - weightThisFrame * weight;
                        if (newHeightAccum <= -delta) {
                           this.heightAccumulation.put(x, z, -delta);
                           yield -delta;
                        } else {
                           this.heightAccumulation.put(x, z, newHeightAccum);
                           yield (int)newHeightAccum;
                        }
                     } else {
                        yield Integer.MIN_VALUE;
                     }
                  }
                  default -> (int)this.heightAccumulation.add(x, z, weightThisFrame * weight);
               };
               if (count != Integer.MIN_VALUE) {
                  this.heightmapApplier.setModifiedY(x, z, originalY + count);
               }
            });
         }

         this.heightmapApplier.update();
      }
   }

   private void apply(ClientLevel level, int radius, int height, Position2dToFloatMap maxAccumulateWeight, MutableBlockPos mutableBlockPos, int x, int y, int z) {
      int maxRadiusSq = radius * radius + radius;
      float expandedMaxRadiusSq = (radius + 1.5F) * (radius + 1.5F);
      int mode = this.mode[0];
      int rate = this.rate[0];
      if (this.cachedSourceMask == null) {
         this.cachedSourceMask = MaskManager.getSourceMask();
         this.cachedMaskContext = new MaskContext(level);
      }

      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         double angle = Math.atan2(player.position().x - x, player.position().z - z);

         for (int xo = -radius - 1; xo <= radius + 1; xo++) {
            label80:
            for (int zo = -radius - 1; zo <= radius + 1; zo++) {
               int radiusSq = xo * xo + zo * zo;
               if (radiusSq <= expandedMaxRadiusSq) {
                  int originalY = this.heightmapApplier.getOriginalY(x + xo, z + zo);
                  if (originalY != Integer.MIN_VALUE) {
                     if (radiusSq <= maxRadiusSq) {
                        float falloff = this.calculateFalloff(radiusSq, radius, angle, xo, zo);
                        if (maxAccumulateWeight != null) {
                           maxAccumulateWeight.max(x + xo, z + zo, falloff);
                        } else if (this.maxFalloffForApplyModeOnce.max(x + xo, z + zo, falloff)) {
                           int amount = Math.round(rate * falloff + 0.4F);
                           int newY = this.calculateNewY(mode, amount, originalY);
                           this.heightmapApplier.setModifiedY(x + xo, z + zo, newY);
                        }
                     }
                  } else {
                     int motionBlockingHeight = level.getHeight(Types.MOTION_BLOCKING, x + xo, z + zo);
                     int startY = Math.min(motionBlockingHeight, y + height);

                     for (int h = startY; h >= y - height; h--) {
                        mutableBlockPos.set(x + xo, h, z + zo);
                        BlockState block = level.getBlockState(mutableBlockPos);
                        if (block.blocksMotion()) {
                           if (radiusSq <= maxRadiusSq && this.cachedSourceMask.test(this.cachedMaskContext.reset(), x + xo, h, z + zo)) {
                              this.heightmapApplier.setOriginalY(x + xo, z + zo, h);
                              float falloff = this.calculateFalloff(radiusSq, radius, angle, xo, zo);
                              if (maxAccumulateWeight != null) {
                                 this.heightmapApplier.setModifiedY(x + xo, z + zo, h);
                                 maxAccumulateWeight.max(x + xo, z + zo, falloff);
                                 continue label80;
                              }

                              if (this.maxFalloffForApplyModeOnce.max(x + xo, z + zo, falloff)
                                 || this.heightmapApplier.getModifiedY(x + xo, z + zo) == Integer.MIN_VALUE) {
                                 int amount = Math.round(rate * falloff + 0.4F);
                                 int newY = this.calculateNewY(mode, amount, h);
                                 this.heightmapApplier.setModifiedY(x + xo, z + zo, newY);
                              }
                              continue label80;
                           }

                           int oldModifiedY = this.heightmapApplier.getModifiedY(x + xo, z + zo);
                           if (oldModifiedY == Integer.MIN_VALUE || oldModifiedY > h) {
                              this.heightmapApplier.setModifiedY(x + xo, z + zo, h);
                           }
                           continue label80;
                        }
                     }

                     int temporaryHeight = Math.min(motionBlockingHeight, y - height - 1);
                     int oldModifiedY = this.heightmapApplier.getModifiedY(x + xo, z + zo);
                     if (oldModifiedY == Integer.MIN_VALUE || oldModifiedY > temporaryHeight) {
                        this.heightmapApplier.setModifiedY(x + xo, z + zo, temporaryHeight);
                     }
                  }
               }
            }
         }
      }
   }

   private int calculateNewY(int mode, int rate, int h) {
      return switch (mode) {
         case 1 -> h - rate;
         case 2 -> h > this.initialY
            ? (h - rate <= this.initialY ? this.initialY : h - rate)
            : (h < this.initialY ? (h + rate >= this.initialY ? this.initialY : h + rate) : h);
         case 3 -> h < this.initialY ? (h + rate >= this.initialY ? this.initialY : h + rate) : h;
         case 4 -> h > this.initialY ? (h - rate <= this.initialY ? this.initialY : h - rate) : h;
         default -> h + rate;
      };
   }

   private float calculateFalloff(int distanceSq, int maxRadius, double angle, int xo, int zo) {
      if (maxRadius <= 0) {
         return 1.0F;
      } else {
         float falloff = this.falloffLut[distanceSq * (this.falloffLutSize - 1) / (maxRadius * maxRadius + maxRadius)];
         if (this.customHeightmap != null) {
            float rotatedX = xo * (float)Math.cos(angle) - zo * (float)Math.sin(angle);
            float rotatedZ = xo * (float)Math.sin(angle) + zo * (float)Math.cos(angle);
            float partialX = rotatedX / (this.customHeightmapZoom * 2.0F * maxRadius);
            float partialZ = rotatedZ / (this.customHeightmapZoom * 2.0F * maxRadius);
            int size = this.customHeightmapSize;
            float imageX = (partialX + 0.5F) * (size - 1) + 0.5F;
            float imageY = (partialZ + 0.5F) * (size - 1) + 0.5F;
            int imageXI = (int)Math.floor(imageX);
            if (imageXI == size) {
               imageXI = size - 1;
            }

            int imageYI = (int)Math.floor(imageY);
            if (imageYI == size) {
               imageYI = size - 1;
            }

            if (imageXI >= 0 && imageXI < size && imageYI >= 0 && imageYI < size) {
               float fracX = imageX - imageXI;
               float fracY = imageY - imageYI;
               float totalGray = 0.0F;

               for (int pxo = -1; pxo <= 1; pxo++) {
                  float weightX = 1.0F - Math.min(1.0F, Math.abs(pxo - fracX + 0.5F));

                  for (int pyo = -1; pyo <= 1; pyo++) {
                     float weightXY = weightX * (1.0F - Math.min(1.0F, Math.abs(pyo - fracY + 0.5F)));
                     int pixelX = imageXI + pxo;
                     int pixelY = imageYI + pyo;
                     if (weightXY > 0.0F && pixelX >= 0 && pixelX < size && pixelY >= 0 && pixelY < size) {
                        totalGray += this.customHeightmap[pixelX * size + pixelY] * weightXY;
                     }
                  }
               }

               falloff *= totalGray;
            } else {
               falloff = 0.0F;
            }
         }

         return falloff;
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      this.settingsChanged = this.settingsChanged | ImGui.sliderInt(AxiomI18n.get("axiom.tool.generic.brush_radius"), this.radius, 1, 32);
      int[] modifiedHeight = new int[]{this.height[0] + 1};
      this.settingsChanged = this.settingsChanged | ImGui.sliderInt(AxiomI18n.get("axiom.tool.heightmap.y_limit"), modifiedHeight, 1, 64);
      this.height[0] = modifiedHeight[0] - 1;
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.elevation"));
      this.settingsChanged = this.settingsChanged
         | ImGuiHelper.combo(
            AxiomI18n.get("axiom.tool.elevation.mode"),
            this.mode,
            new String[]{
               AxiomI18n.get("axiom.tool.elevation.mode_raise"),
               AxiomI18n.get("axiom.tool.elevation.mode_lower"),
               AxiomI18n.get("axiom.tool.elevation.mode_flatten"),
               AxiomI18n.get("axiom.tool.elevation.mode_flatten_up"),
               AxiomI18n.get("axiom.tool.elevation.flatten_down")
            }
         );
      this.settingsChanged = this.settingsChanged | ImGui.sliderFloat(AxiomI18n.get("axiom.tool.heightmap.smoothing"), this.smoothing, 0.0F, 1.0F);
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.elevation.dynamics"));
      this.settingsChanged = this.settingsChanged
         | ImGuiHelper.combo(
            AxiomI18n.get("axiom.tool.elevation.apply_mode"),
            this.applyMode,
            new String[]{AxiomI18n.get("axiom.tool.elevation.apply_mode.continuous"), AxiomI18n.get("axiom.tool.elevation.apply_mode.once")}
         );
      if (this.applyMode[0] == 0) {
         this.settingsChanged = this.settingsChanged | ImGui.sliderInt(AxiomI18n.get("axiom.tool.heightmap.rate"), this.rate, 0, 32);
      } else {
         this.settingsChanged = this.settingsChanged | ImGui.sliderInt(AxiomI18n.get("axiom.tool.heightmap.amount"), this.rate, 0, 32);
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.heightmap.falloff"));
      this.settingsChanged = this.settingsChanged | this.falloffWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.elevation.custom_heightmap"));
      if (ImGui.button(AxiomI18n.get("axiom.tool.elevation.load_heightmap"))) {
         this.loadHeightmapFuture = ImageHeightmapProvider.chooseHeightmap();
         this.settingsChanged = true;
      }

      boolean hasServerHeightmaps = false;
      if (ServerHeightmaps.hasHeightmaps()) {
         hasServerHeightmaps = true;
         if (ImGui.button(AxiomI18n.get("axiom.hardcoded.browse_server_heightmaps"))) {
            ServerHeightmaps.openHeightmapModal();
         }

         BufferedImage image = ServerHeightmaps.showHeightmapModalIfOpen();
         if (image != null) {
            this.setHeightmapImage(image);
         }
      }

      if (this.customHeightmap != null) {
         if (!hasServerHeightmaps) {
            ImGui.sameLine();
         }

         if (ImGui.button(AxiomI18n.get("axiom.widget.clear"))) {
            this.loadHeightmapFuture = null;
            this.customHeightmap = null;
            this.heightmapDisplayDirty = true;
            this.settingsChanged = true;
         }
      }

      float fbScale = Math.max(EditorUI.getIO().getDisplayFramebufferScaleX(), EditorUI.getIO().getDisplayFramebufferScaleY());
      int heightmapDisplaySize = (int)(256.0F * EditorUI.getUiScale());
      int heightmapFramebufferSize = (int)(heightmapDisplaySize * fbScale);
      if (this.heightmapDisplay != null) {
         NativeImage pixels = this.heightmapDisplay.getPixels();
         if (pixels != null && pixels.getWidth() != heightmapFramebufferSize) {
            this.heightmapDisplayDirty = true;
         }
      }

      if (this.heightmapDisplayDirty) {
         this.heightmapDisplayDirty = false;
         this.uploadHeightmapPreview(heightmapFramebufferSize);
      }

      if (this.heightmapDisplay != null) {
         ImGui.image(this.heightmapDisplay.getId(), heightmapDisplaySize, heightmapDisplaySize, 0.0F, 0.0F, 1.0F, 1.0F);
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.widget.presets"));
      this.presetWidget.displayImgui(this.settingsChanged);
      this.settingsChanged = false;
   }

   private void uploadHeightmapPreview(int heightmapFramebufferSize) {
      if (this.customHeightmap == null) {
         if (this.heightmapDisplay != null) {
            this.heightmapDisplay.close();
            this.heightmapDisplay = null;
         }
      } else {
         NativeImage pixels;
         if (this.heightmapDisplay == null) {
            this.heightmapDisplay = new AutoCleaningDynamicTexture(heightmapFramebufferSize, heightmapFramebufferSize, true);
            pixels = this.heightmapDisplay.getPixels();
         } else {
            pixels = this.heightmapDisplay.getPixels();
            if (pixels == null || pixels.getWidth() != heightmapFramebufferSize) {
               this.heightmapDisplay.close();
               this.heightmapDisplay = new AutoCleaningDynamicTexture(heightmapFramebufferSize, heightmapFramebufferSize, true);
               pixels = this.heightmapDisplay.getPixels();
            }
         }

         float maxDistanceSq = 1.0019531F;
         int frameBackgroundABGR = ImGui.getColorU32(7);

         for (int x = 0; x < heightmapFramebufferSize; x++) {
            for (int y = 0; y < heightmapFramebufferSize; y++) {
               float distanceX = (float)x / (heightmapFramebufferSize - 1) * 2.0F - 1.0F;
               float distanceY = (float)y / (heightmapFramebufferSize - 1) * 2.0F - 1.0F;
               float distanceSq = distanceX * distanceX + distanceY * distanceY;
               if (distanceSq > maxDistanceSq) {
                  pixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(ColourUtils.abgrToArgb(frameBackgroundABGR)));
               } else {
                  float partialX = distanceX / (this.customHeightmapZoom * 2.0F);
                  float partialY = distanceY / (this.customHeightmapZoom * 2.0F);
                  int size = this.customHeightmapSize;
                  float imageX = (partialX + 0.5F) * (size - 1) + 0.5F;
                  float imageY = (partialY + 0.5F) * (size - 1) + 0.5F;
                  int imageXI = (int)Math.floor(imageX);
                  if (imageXI == size) {
                     imageXI = size - 1;
                  }

                  int imageYI = (int)Math.floor(imageY);
                  if (imageYI == size) {
                     imageYI = size - 1;
                  }

                  float falloff = 0.0F;
                  if (imageXI >= 0 && imageXI < size && imageYI >= 0 && imageYI < size) {
                     float fracX = imageX - imageXI;
                     float fracY = imageY - imageYI;
                     float totalGray = 0.0F;

                     for (int xo = -1; xo <= 1; xo++) {
                        float weightX = 1.0F - Math.min(1.0F, Math.abs(xo - fracX + 0.5F));

                        for (int yo = -1; yo <= 1; yo++) {
                           float weightXY = weightX * (1.0F - Math.min(1.0F, Math.abs(yo - fracY + 0.5F)));
                           int pixelX = imageXI + xo;
                           int pixelY = imageYI + yo;
                           if (weightXY > 0.0F && pixelX >= 0 && pixelX < size && pixelY >= 0 && pixelY < size) {
                              totalGray += this.customHeightmap[pixelX * size + pixelY] * weightXY;
                           }
                        }
                     }

                     falloff = totalGray;
                  }

                  int gray = (int)(falloff * 255.0F);
                  int argb = 0xFF000000 | gray << 16 | gray << 8 | gray;
                  pixels.setPixelRGBA(x, y, ColourUtils.argbToAbgr(argb));
               }
            }
         }

         this.heightmapDisplay.upload();
      }
   }

   @Override
   public String listenForEsc() {
      return !this.usingTool ? null : AxiomI18n.get("axiom.widget.cancel");
   }

   @Override
   public boolean initiateAdjustment() {
      return SimpleRadiusAdjustment.initiateAdjustment(this.radius, this.baseRadiusAdjustment);
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      return SimpleRadiusAdjustment.renderAdjustment(mouseX, mouseY, mouseDelta, 32, this.radius, this.baseRadiusAdjustment);
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.elevation");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.falloffWidget.writeSettings(tag);
      tag.putInt("BrushRadius", this.radius[0]);
      tag.putInt("Height", this.height[0]);
      tag.putInt("Mode", this.mode[0]);
      tag.putFloat("Smoothing", this.smoothing[0]);
      tag.putByte("ApplyMode", (byte)this.applyMode[0]);
      tag.putInt("Rate", this.rate[0]);
      if (this.customHeightmap != null) {
         tag.putFloat("CustomHeightmapZoom", this.customHeightmapZoom);
         tag.putInt("CustomHeightmapSize", this.customHeightmapSize);
         int[] customHeightmapAsInts = new int[this.customHeightmap.length];

         for (int i = 0; i < this.customHeightmap.length; i++) {
            customHeightmapAsInts[i] = Float.floatToIntBits(this.customHeightmap[i]);
         }

         tag.putIntArray("CustomHeightmapAsInts", customHeightmapAsInts);
      }
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.falloffWidget.loadSettings(tag);
      this.radius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BrushRadius", 8);
      this.height[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Height", 63);
      this.mode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Mode", 0);
      this.smoothing[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Smoothing", 1.0F);
      this.applyMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "ApplyMode", 0);
      this.rate[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Rate", 8);
      this.customHeightmap = null;
      if (tag.contains("CustomHeightmapAsInts")) {
         int[] customHeightmapAsInts = VersionUtilsNbt.helperCompoundTagGetIntArray(tag, "CustomHeightmapAsInts").orElse(new int[0]);
         int size = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "CustomHeightmapSize", 0);
         if (size * size == customHeightmapAsInts.length) {
            this.customHeightmapZoom = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "CustomHeightmapZoom", 1.0F);
            this.customHeightmapSize = size;
            this.customHeightmap = new float[customHeightmapAsInts.length];

            for (int i = 0; i < customHeightmapAsInts.length; i++) {
               this.customHeightmap[i] = Float.intBitsToFloat(customHeightmapAsInts[i]);
            }
         }
      }

      this.heightmapDisplayDirty = true;
   }

   @Override
   public char iconChar() {
      return '\ue917';
   }

   @Override
   public String keybindId() {
      return "elevation";
   }

   @Override
   public int defaultKeybind() {
      return 69;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_ELEVATION, AxiomPermission.BUILD_SECTION);
   }
}
