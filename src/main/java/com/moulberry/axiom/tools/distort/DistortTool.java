package com.moulberry.axiom.tools.distort;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.noise.SimplexDomainWarp;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherMinSDF;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2FloatMap.Entry;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.concurrent.ThreadLocalRandom;
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
import org.joml.Vector3d;

public class DistortTool implements Tool {
   private final ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private final float[] distance = new float[]{2.0F};
   private final int[] scale = new int[]{16};
   private final int[] iterations = new int[]{2};
   private final int[] seed = new int[]{0};
   private boolean separateAxis = false;
   private final float[] distanceX = new float[]{2.0F};
   private final float[] distanceY = new float[]{2.0F};
   private final float[] distanceZ = new float[]{2.0F};
   private boolean smoothEdges = true;
   private final PresetWidget presetWidget = new PresetWidget(this, "distort");
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();

   @Override
   public void reset() {
      if (this.usingTool) {
         this.usingTool = false;
         ChunkRenderOverrider.release("distort");
      }

      this.blockRegion.clear();
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
            if (!this.usingTool) {
               this.usingTool = true;
               ChunkRenderOverrider.acquire("distort");
            }

            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            MutableBlockPos mutableBlockPos = new MutableBlockPos();
            int iterations = this.iterations[0];
            float amplitudeX;
            float amplitudeY;
            float amplitudeZ;
            if (this.separateAxis) {
               amplitudeX = this.distanceX[0] * 50.0F / iterations;
               amplitudeY = this.distanceY[0] * 50.0F / iterations;
               amplitudeZ = this.distanceZ[0] * 50.0F / iterations;
            } else {
               amplitudeX = amplitudeY = amplitudeZ = this.distance[0] * 50.0F / iterations;
            }

            float frequency = 1.0F / this.scale[0];
            int seed = this.seed[0];
            if (this.smoothEdges) {
               AsyncToolPather pather = this.createToolPatherSmoothEdges(
                  level, mutableBlockPos, iterations, amplitudeX, amplitudeY, amplitudeZ, frequency, seed
               );
               this.pathProvider = new AsyncToolPathProvider(pather);
            } else {
               AsyncToolPather pather = this.createToolPatherNoSmoothEdges(
                  level, mutableBlockPos, iterations, amplitudeX, amplitudeY, amplitudeZ, frequency, seed
               );
               this.pathProvider = new AsyncToolPathProvider(pather);
            }

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
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         String countString = NumberFormat.getInstance().format((long)this.blockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.distorted", countString);
         RegionHelper.pushBlockRegionChange(this.blockRegion, historyDescription);
         this.reset();
      } else {
         this.pathProvider.update();
         float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
         this.blockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
      }
   }

   private AsyncToolPather createToolPatherNoSmoothEdges(
      ClientLevel level, MutableBlockPos mutableBlockPos, int iterations, float amplitudeX, float amplitudeY, float amplitudeZ, float frequency, int seed
   ) {
      MaskElement sourceMaskElement = MaskManager.getSourceMask();
      MaskElement destMaskElement = MaskManager.getDestMask();
      MaskContext maskContext = new MaskContext(level);
      BlockState air = Blocks.AIR.defaultBlockState();
      return new AsyncToolPatherUnique(this.brushWidget.getBrushShape(), (x, y, z) -> {
         if (destMaskElement.test(maskContext.reset(), x, y, z)) {
            Vector3d vector3d = new Vector3d(x + 0.5, y + 0.5, z + 0.5);

            for (int i = 0; i < iterations; i++) {
               SimplexDomainWarp.SingleDomainWarpOpenSimplex3DGradient(seed, amplitudeX, amplitudeY, amplitudeZ, frequency, vector3d);
            }

            float replacableWeight = 0.0F;
            float nonreplacableWeight = 0.0F;
            Object2FloatArrayMap<BlockState> blockWeights = new Object2FloatArrayMap();
            int blockX = (int)Math.floor(vector3d.x);
            int blockY = (int)Math.floor(vector3d.y);
            int blockZ = (int)Math.floor(vector3d.z);
            float fracX = (float)(vector3d.x - blockX);
            float fracY = (float)(vector3d.y - blockY);
            float fracZ = (float)(vector3d.z - blockZ);

            for (int xo = -1; xo <= 1; xo++) {
               float weightX = 1.0F - Math.min(1.0F, Math.abs(xo - fracX + 0.5F));

               for (int yo = -1; yo <= 1; yo++) {
                  float weightXY = weightX * (1.0F - Math.min(1.0F, Math.abs(yo - fracY + 0.5F)));

                  for (int zo = -1; zo <= 1; zo++) {
                     float weightXYZ = weightXY * (1.0F - Math.min(1.0F, Math.abs(zo - fracZ + 0.5F)));
                     if (!(weightXYZ <= 0.0F)) {
                        BlockState blockState;
                        if (!sourceMaskElement.test(maskContext.reset(), blockX + xo, blockY + yo, blockZ + zo)) {
                           blockState = air;
                        } else {
                           blockState = level.getBlockState(mutableBlockPos.set(blockX + xo, blockY + yo, blockZ + zo));
                        }

                        if (blockState.isAir()) {
                           blockState = Blocks.AIR.defaultBlockState();
                        }

                        if (blockState.canBeReplaced()) {
                           replacableWeight += weightXYZ;
                        } else {
                           nonreplacableWeight += weightXYZ;
                        }

                        blockWeights.computeFloat(blockState, (block, value) -> value == null ? weightXYZ : value + weightXYZ);
                     }
                  }
               }
            }

            boolean useCanBeReplaced = replacableWeight >= nonreplacableWeight;
            BlockState closestState = AIR;
            float closestWeight = Float.MIN_VALUE;
            ObjectIterator var37 = blockWeights.object2FloatEntrySet().iterator();

            while (var37.hasNext()) {
               Entry<BlockState> entry = (Entry<BlockState>)var37.next();
               BlockState blockStatex = (BlockState)entry.getKey();
               if (blockStatex.canBeReplaced() == useCanBeReplaced && entry.getFloatValue() > closestWeight) {
                  closestState = blockStatex;
                  closestWeight = entry.getFloatValue();
               }
            }

            if (!closestState.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
               ChunkRenderOverrider.setBlock(x, y, z, AIR);
            }

            this.blockRegion.addBlock(x, y, z, closestState);
         }
      });
   }

   private AsyncToolPather createToolPatherSmoothEdges(
      ClientLevel level, MutableBlockPos mutableBlockPos, int iterations, float amplitudeX, float amplitudeY, float amplitudeZ, float frequency, int seed
   ) {
      MaskElement sourceMaskElement = MaskManager.getSourceMask();
      MaskElement destMaskElement = MaskManager.getDestMask();
      MaskContext maskContext = new MaskContext(level);
      BlockState air = Blocks.AIR.defaultBlockState();
      return new AsyncToolPatherMinSDF(this.brushWidget.getBrushShape(), (x, y, z, distance) -> {
         if (destMaskElement.test(maskContext.reset(), x, y, z)) {
            float falloff = 0.0F;
            if (distance > 0.6) {
               falloff = (distance - 0.6F) / 0.4F;
            }

            if (!BuildConfig.DEBUG || !(falloff < 0.0F) && !(falloff > 1.0F)) {
               float scaledAmplitudeX = amplitudeX * (1.0F - falloff);
               float scaledAmplitudeY = amplitudeY * (1.0F - falloff);
               float scaledAmplitudeZ = amplitudeZ * (1.0F - falloff);
               Vector3d vector3d = new Vector3d(x + 0.5, y + 0.5, z + 0.5);

               for (int i = 0; i < iterations; i++) {
                  SimplexDomainWarp.SingleDomainWarpOpenSimplex3DGradient(seed, scaledAmplitudeX, scaledAmplitudeY, scaledAmplitudeZ, frequency, vector3d);
               }

               float replacableWeight = 0.0F;
               float nonreplacableWeight = 0.0F;
               Object2FloatArrayMap<BlockState> blockWeights = new Object2FloatArrayMap();
               int blockX = (int)Math.floor(vector3d.x);
               int blockY = (int)Math.floor(vector3d.y);
               int blockZ = (int)Math.floor(vector3d.z);
               float fracX = (float)(vector3d.x - blockX);
               float fracY = (float)(vector3d.y - blockY);
               float fracZ = (float)(vector3d.z - blockZ);

               for (int xo = -1; xo <= 1; xo++) {
                  float weightX = 1.0F - Math.min(1.0F, Math.abs(xo - fracX + 0.5F));

                  for (int yo = -1; yo <= 1; yo++) {
                     float weightXY = weightX * (1.0F - Math.min(1.0F, Math.abs(yo - fracY + 0.5F)));

                     for (int zo = -1; zo <= 1; zo++) {
                        float weightXYZ = weightXY * (1.0F - Math.min(1.0F, Math.abs(zo - fracZ + 0.5F)));
                        BlockState blockState;
                        if (!sourceMaskElement.test(maskContext.reset(), blockX + xo, blockY + yo, blockZ + zo)) {
                           blockState = air;
                        } else {
                           blockState = level.getBlockState(mutableBlockPos.set(blockX + xo, blockY + yo, blockZ + zo));
                        }

                        if (blockState.isAir()) {
                           blockState = Blocks.AIR.defaultBlockState();
                        }

                        if (blockState.canBeReplaced()) {
                           replacableWeight += weightXYZ;
                        } else {
                           nonreplacableWeight += weightXYZ;
                        }

                        blockWeights.computeFloat(blockState, (block, value) -> value == null ? weightXYZ : value + weightXYZ);
                     }
                  }
               }

               boolean useCanBeReplaced = replacableWeight >= nonreplacableWeight;
               BlockState closestState = Blocks.AIR.defaultBlockState();
               float closestWeight = Float.MIN_VALUE;
               ObjectIterator var42 = blockWeights.object2FloatEntrySet().iterator();

               while (var42.hasNext()) {
                  Entry<BlockState> entry = (Entry<BlockState>)var42.next();
                  BlockState blockStatex = (BlockState)entry.getKey();
                  if (blockStatex.canBeReplaced() == useCanBeReplaced && entry.getFloatValue() > closestWeight) {
                     closestState = blockStatex;
                     closestWeight = entry.getFloatValue();
                  }
               }

               if (!closestState.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                  ChunkRenderOverrider.setBlock(x, y, z, AIR);
               }

               this.blockRegion.addBlock(x, y, z, closestState);
            } else {
               throw new FaultyImplementationError();
            }
         }
      });
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.distort.distortion"));
      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.distort.scale"), this.scale, 2, 32);
      if (this.separateAxis) {
         changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.distort.distance_x"), this.distanceX, 0.0F, 16.0F, "%.2f");
         changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.distort.distance_y"), this.distanceY, 0.0F, 16.0F, "%.2f");
         changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.distort.distance_z"), this.distanceZ, 0.0F, 16.0F, "%.2f");
      } else {
         changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.distort.distance"), this.distance, 1.0F, 16.0F, "%.2f");
      }

      changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.distort.iterations"), this.iterations, 1, 5);
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.distort.separate_axis"), this.separateAxis)) {
         this.separateAxis = !this.separateAxis;
         if (this.separateAxis) {
            this.distanceX[0] = this.distance[0];
            this.distanceY[0] = this.distance[0];
            this.distanceZ[0] = this.distance[0];
         } else {
            this.distance[0] = (this.distanceX[0] + this.distanceY[0] + this.distanceZ[0]) / 3.0F;
         }

         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.noise"));
      changed |= ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.generic.noise_seed"), this.seed);
      if (ImGui.button(AxiomI18n.get("axiom.tool.generic.noise_seed_do_randomize"))) {
         this.seed[0] = ThreadLocalRandom.current().nextInt();
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.advanced_options"));
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.distort.smooth_edges"), this.smoothEdges)) {
         this.smoothEdges = !this.smoothEdges;
         changed = true;
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
      return AxiomI18n.get("axiom.tool.distort");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putInt("DistortionScale", this.scale[0]);
      tag.putBoolean("SeparateAxis", this.separateAxis);
      if (this.separateAxis) {
         tag.putFloat("DistanceX", this.distanceX[0]);
         tag.putFloat("DistanceY", this.distanceY[0]);
         tag.putFloat("DistanceZ", this.distanceZ[0]);
      } else {
         tag.putFloat("Distance", this.distance[0]);
      }

      tag.putByte("Iterations", (byte)this.iterations[0]);
      tag.putInt("Seed", this.seed[0]);
      tag.putBoolean("SmoothEdges", this.smoothEdges);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.scale[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "DistortionScale", 16);
      this.separateAxis = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "SeparateAxis", false);
      if (this.separateAxis) {
         this.distanceX[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "DistanceX", 2.0F);
         this.distanceY[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "DistanceY", 2.0F);
         this.distanceZ[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "DistanceZ", 2.0F);
      } else {
         this.distance[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Distance", 2.0F);
      }

      this.iterations[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Iterations", 2);
      this.seed[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Seed", 0);
      this.smoothEdges = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "SmoothEdges", true);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue915';
   }

   @Override
   public String keybindId() {
      return "distort";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_DISTORT, AxiomPermission.BUILD_SECTION);
   }
}
