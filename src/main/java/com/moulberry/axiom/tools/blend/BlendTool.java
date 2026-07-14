package com.moulberry.axiom.tools.blend;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.noise.NoiseHelper;
import com.moulberry.axiom.noise.SimplexNoise;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPatherMinSDF;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.ChamferClosestBlockMap;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2FloatMap.Entry;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class BlendTool implements Tool {
   private final ChunkedBooleanRegion region = new ChunkedBooleanRegion();
   private final Long2FloatOpenHashMap blockDistances = new Long2FloatOpenHashMap();
   private AsyncToolPathProvider pathProvider = null;
   private boolean usingTool = false;
   private final BrushWidget brushWidget = new BrushWidget();
   private final int[] mode = new int[]{0};
   private final int[] spreadDistance = new int[]{4};
   private final float[] spreadPower = new float[]{0.75F};
   private final int[] scale = new int[]{8};
   private final int[] seed = new int[]{0};
   private boolean falloff = false;
   private boolean maskSurface = true;
   private final PresetWidget presetWidget = new PresetWidget(this, "blend");

   @Override
   public void reset() {
      this.usingTool = false;
      this.region.clear();
      this.blockDistances.clear();
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
               return UserAction.ActionResult.NOT_HANDLED;
            }

            this.reset();
            this.usingTool = true;
            new MutableBlockPos();
            MaskElement maskElement = MaskManager.getSourceMask();
            MaskContext maskContext = new MaskContext(level);
            boolean maskSurface = this.maskSurface;
            PositionSet blacklistedPositions = new PositionSet();
            this.pathProvider = new AsyncToolPathProvider(
               new AsyncToolPatherMinSDF(
                  this.brushWidget.getBrushShape(),
                  (x, y, z, distance) -> {
                     if (!blacklistedPositions.contains(x, y, z)) {
                        maskContext.reset();
                        if (!maskElement.test(maskContext, x, y, z)) {
                           blacklistedPositions.add(x, y, z);
                        } else if (!maskContext.getBlockState(x, y, z).blocksMotion()) {
                           blacklistedPositions.add(x, y, z);
                        } else if (maskSurface
                           && maskContext.getBlockState(x, y, z, 1, 0, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, -1, 0, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, 1, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, -1, 0).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, 0, 1).blocksMotion()
                           && maskContext.getBlockState(x, y, z, 0, 0, -1).blocksMotion()) {
                           blacklistedPositions.add(x, y, z);
                        } else {
                           this.region.add(x, y, z);
                           this.blockDistances.put(BlockPos.asLong(x, y, z), distance);
                        }
                     }
                  }
               )
            );
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
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            this.reset();
            return;
         }

         this.pathProvider.finish();
         Selection.render(rc, 4);
         if (this.mode[0] == 0) {
            this.applySpread();
         } else {
            this.applyWarp();
         }

         this.reset();
      } else {
         ClientLevel levelx = Minecraft.getInstance().level;
         if (levelx == null) {
            return;
         }

         this.pathProvider.update();
         this.region.render(rc, Vec3.ZERO, 3);
      }
   }

   private void applySpread() {
      if (!(this.spreadPower[0] < 0.01)) {
         ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
         Level level = Minecraft.getInstance().level;
         ArrayList<Entry> entries = new ArrayList<>(this.blockDistances.long2FloatEntrySet());
         Collections.shuffle(entries);
         ThreadLocalRandom random = ThreadLocalRandom.current();
         LongSet swapped = new LongOpenHashSet();
         int rawDistance = Math.max(1, this.spreadDistance[0]);
         double spreadPower = Math.sqrt(this.spreadPower[0]);
         double invSpreadPower = 1.0 / spreadPower;
         LongArrayList closest = new LongArrayList();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         for (Entry entry : entries) {
            long pos = entry.getLongKey();
            if (!swapped.contains(pos)) {
               float distance = rawDistance;
               if (this.falloff) {
                  distance *= 1.0F - Math.max(0.0F, entry.getFloatValue() * 2.0F - 1.0F);
                  distance = Math.max(1.0F, distance);
               }

               int distanceI = (int)Math.ceil(distance);
               double spreadAmount = random.nextDouble();
               spreadAmount = Math.pow(1.0 - Math.pow(1.0 - spreadAmount, spreadPower), invSpreadPower);
               double targetDistance = distance * spreadAmount;
               double targetDistanceSq = targetDistance * targetDistance;
               closest.clear();
               double closestError = Double.MAX_VALUE;

               for (int x = -distanceI; x <= distanceI; x++) {
                  for (int y = -distanceI; y <= distanceI; y++) {
                     for (int z = -distanceI; z <= distanceI; z++) {
                        int distSq = x * x + y * y + z * z;
                        double error = Math.abs(distSq - targetDistanceSq);
                        if (!(error > closestError)) {
                           long offsetPos = BlockPos.offset(pos, x, y, z);
                           if (this.blockDistances.containsKey(offsetPos) && !swapped.contains(offsetPos)) {
                              if (error < closestError) {
                                 closest.clear();
                                 closestError = error;
                              }

                              closest.add(offsetPos);
                           }
                        }
                     }
                  }
               }

               if (!closest.isEmpty()) {
                  long chosen = closest.getLong(random.nextInt(closest.size()));
                  int fromX = BlockPos.getX(pos);
                  int fromY = BlockPos.getY(pos);
                  int fromZ = BlockPos.getZ(pos);
                  int toX = BlockPos.getX(chosen);
                  int toY = BlockPos.getY(chosen);
                  int toZ = BlockPos.getZ(chosen);
                  swapped.add(pos);
                  swapped.add(chosen);
                  BlockState from = level.getBlockState(mutableBlockPos.set(fromX, fromY, fromZ));
                  BlockState to = level.getBlockState(mutableBlockPos.set(toX, toY, toZ));
                  if (from.getBlock() != Blocks.VOID_AIR && to.getBlock() != Blocks.VOID_AIR) {
                     blockRegion.addBlockWithoutDirty(fromX, fromY, fromZ, to);
                     blockRegion.addBlockWithoutDirty(toX, toY, toZ, from);
                  }
               }
            }
         }

         String countString = NumberFormat.getInstance().format((long)blockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.blend_tool", countString);
         RegionHelper.pushBlockRegionChange(blockRegion, historyDescription);
      }
   }

   private static void addDirection(
      Long2ObjectOpenHashMap<Vector3d> cellToDirectionMap,
      Random random,
      Vector3d avgDirection,
      double x,
      double y,
      double z,
      int offX,
      int offY,
      int offZ,
      int scale,
      int seed
   ) {
      double x1 = x + scale * offX / 2.0;
      double y1 = y + scale * offY / 2.0;
      double z1 = z + scale * offZ / 2.0;
      int cellX = (int)Math.floor(x1 / scale);
      int cellY = (int)Math.floor(y1 / scale);
      int cellZ = (int)Math.floor(z1 / scale);
      long cellPos = BlockPos.asLong(cellX, cellY, cellZ);
      Vector3d direction = (Vector3d)cellToDirectionMap.computeIfAbsent(cellPos, cellPos2 -> {
         random.setSeed(NoiseHelper.hash(seed, cellX * 501125321, cellY * 1136930381, cellZ * 1720413743));
         double phi = random.nextDouble(Math.PI * 2);
         double cosTheta = random.nextDouble(-1.0, 1.0);
         double theta = Math.acos(cosTheta);
         double dirX = Math.sin(theta) * Math.cos(phi);
         double dirY = Math.sin(theta) * Math.sin(phi);
         double dirZ = Math.cos(theta);
         return new Vector3d(dirX, dirY, dirZ);
      });
      double weight = (1.0 - Math.abs((cellX + 0.5) * scale - x) / scale)
         * (1.0 - Math.abs((cellY + 0.5) * scale - y) / scale)
         * (1.0 - Math.abs((cellZ + 0.5) * scale - z) / scale);
      avgDirection.x = avgDirection.x + direction.x * weight;
      avgDirection.y = avgDirection.y + direction.y * weight;
      avgDirection.z = avgDirection.z + direction.z * weight;
   }

   private static void warp(Long2ObjectOpenHashMap<Vector3d> cellToDirectionMap, Random random, int scale, int seed, double distance, Vector3d point) {
      Vector3d averageDirection = new Vector3d();
      double x = point.x;
      double y = point.y;
      double z = point.z;
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, -1, -1, -1, scale, seed);
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, 1, -1, -1, scale, seed);
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, -1, 1, -1, scale, seed);
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, 1, 1, -1, scale, seed);
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, -1, -1, 1, scale, seed);
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, 1, -1, 1, scale, seed);
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, -1, 1, 1, scale, seed);
      addDirection(cellToDirectionMap, random, averageDirection, x, y, z, 1, 1, 1, scale, seed);
      float noise = SimplexNoise.evaluateStatic((x + 0.5) / scale, (y + 0.5) / scale, (z + 0.5) / scale, seed * 668265261);
      averageDirection.normalize(distance * noise);
      point.x = point.x + averageDirection.x;
      point.y = point.y + averageDirection.y;
      point.z = point.z + averageDirection.z;
   }

   private void applyWarp() {
      int rawDistance = this.spreadDistance[0];
      int scale = this.scale[0];
      int seed = this.seed[0];
      ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
      Long2ObjectOpenHashMap<Vector3d> cellToDirectionMap = new Long2ObjectOpenHashMap();
      Random random = new Random();
      ChamferClosestBlockMap closestBlockMap = new ChamferClosestBlockMap(Minecraft.getInstance().level, blockState -> !blockState.isAir(), rawDistance);
      closestBlockMap.withMaskPositions(this.blockDistances.keySet());
      this.blockDistances.long2FloatEntrySet().fastForEach(entry -> {
         long pos = entry.getLongKey();
         float sdf = entry.getFloatValue();
         float distance = rawDistance;
         if (this.falloff) {
            distance *= 1.0F - Math.max(0.0F, sdf * 2.0F - 1.0F);
            distance = Math.max(1.0F, distance);
         }

         int x = BlockPos.getX(pos);
         int y = BlockPos.getY(pos);
         int z = BlockPos.getZ(pos);

         Vector3d point;
         for (point = new Vector3d(x + 0.5, y + 0.5, z + 0.5); distance > 0.0F; distance--) {
            warp(cellToDirectionMap, random, scale, seed, Math.min((double)distance, 1.0), point);
         }

         int blockX = (int)Math.floor(point.x);
         int blockY = (int)Math.floor(point.y);
         int blockZ = (int)Math.floor(point.z);
         BlockState blockState = closestBlockMap.get(blockX, blockY, blockZ);
         if (!blockState.isAir()) {
            blockRegion.addBlock(x, y, z, blockState);
         }
      });
      LongIterator iterator = this.blockDistances.keySet().longIterator();

      while (iterator.hasNext()) {
         long countString = iterator.nextLong();
      }

      String countString = NumberFormat.getInstance().format((long)blockRegion.count());
      String historyDescription = AxiomI18n.get("axiom.history_description.blend_tool", countString);
      RegionHelper.pushBlockRegionChange(blockRegion, historyDescription);
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = this.brushWidget.displayImgui();
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.generic.mask_surface"), this.maskSurface)) {
         this.maskSurface = !this.maskSurface;
         changed = true;
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.blend"));
      changed |= ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.selection.mode"),
         this.mode,
         new String[]{AxiomI18n.get("axiom.tool.blend.mode_spread"), AxiomI18n.get("axiom.tool.blend.mode_warp")}
      );
      if (this.mode[0] == 0) {
         changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.blend.spread_dist") + "###SpreadDist", this.spreadDistance, 1, 16);
         changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.blend.spread_pow") + "###SpreadPow", this.spreadPower, 0.01F, 1.0F);
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.blend.falloff") + "###Falloff", this.falloff)) {
            this.falloff = !this.falloff;
            changed = true;
         }
      } else {
         changed |= ImGui.sliderInt("Distance###Distance", this.spreadDistance, 1, 16);
         changed |= ImGui.sliderInt(AxiomI18n.get("axiom.tool.distort.scale"), this.scale, 2, 32);
         if (ImGui.checkbox(AxiomI18n.get("axiom.tool.blend.falloff") + "###Falloff", this.falloff)) {
            this.falloff = !this.falloff;
            changed = true;
         }

         ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.noise"));
         changed |= ImGuiHelper.inputInt(AxiomI18n.get("axiom.tool.generic.noise_seed"), this.seed);
         if (ImGui.button(AxiomI18n.get("axiom.tool.generic.noise_seed_do_randomize"))) {
            this.seed[0] = ThreadLocalRandom.current().nextInt();
            changed = true;
         }
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
      return AxiomI18n.get("axiom.tool.blend");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putBoolean("MaskSurface", this.maskSurface);
      tag.putInt("Mode", this.mode[0]);
      tag.putInt("Distance", this.spreadDistance[0]);
      tag.putFloat("SpreadPower", this.spreadPower[0]);
      tag.putBoolean("Falloff", this.falloff);
      tag.putInt("Scale", this.scale[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.maskSurface = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "MaskSurface", true);
      this.mode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Mode", 0);
      this.spreadDistance[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Distance", 4);
      this.spreadPower[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "SpreadPower", 0.75F);
      this.falloff = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "Falloff", false);
      this.scale[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Scale", 8);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue929';
   }

   @Override
   public String keybindId() {
      return "blend";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_BLEND, AxiomPermission.BUILD_SECTION);
   }
}
