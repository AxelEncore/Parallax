package com.moulberry.axiom.tools.sculpt_draw;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.Position2FloatMap;
import com.moulberry.axiom.collections.Position2ObjectMap;
import com.moulberry.axiom.collections.SimpleBlockWeightTracker;
import com.moulberry.axiom.editor.GenericRadiusAdjustment;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.editor.widgets.PresetWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.ToolPatherPoint;
import com.moulberry.axiom.rasterization.Rasterization3D;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.GaussianBlurTable;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiomclientapi.pathers.BallShape;
import imgui.moulberry92.ImGui;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class SculptDrawTool implements Tool {
   private final ChunkedBooleanRegion removeRegion = new ChunkedBooleanRegion();
   private final ChunkedBlockRegion blockRegion = new ChunkedBlockRegion();
   private final ChunkedBooleanRegion previewSphere = new ChunkedBooleanRegion();
   private final Position2ObjectMap<SimpleBlockWeightTracker> influenceMap = SimpleBlockWeightTracker.createMap();
   private final Position2FloatMap floatInfluenceMap = new Position2FloatMap();
   private int oldRadius = -1;
   private int oldBrushShape = -1;
   private ToolPatherPoint toolPather = null;
   private int[] offsets = null;
   private boolean usingTool = false;
   private final int[] radius = new int[]{5};
   private final GenericRadiusAdjustment radiusAdjustment = new GenericRadiusAdjustment(32);
   private final float[] strength = new float[]{0.5F};
   private boolean invert = false;
   private boolean maskY = false;
   private boolean denoise = true;
   private final PresetWidget presetWidget = new PresetWidget(this, "sculpt_draw");

   @Override
   public void reset() {
      if (this.usingTool) {
         ChunkRenderOverrider.release("Sculpt Draw Tool");
         this.usingTool = false;
      }

      this.previewSphere.clear();
      this.removeRegion.clear();
      this.blockRegion.clear();
      this.influenceMap.clear();
      this.floatInfluenceMap.clear();
      this.toolPather = null;
      this.oldRadius = -1;
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.reset();
            if (!this.usingTool) {
               ChunkRenderOverrider.acquire("Sculpt Draw Tool");
               this.usingTool = true;
            }

            BallShape shape = BallShape.SPHERE;
            this.toolPather = new ToolPatherPoint();
            int radius = this.radius[0];
            IntList offsets = new IntArrayList();
            float maxRadiusSq = (radius + 0.5F) * (radius + 0.5F);

            for (int x = -radius; x <= radius; x++) {
               for (int y = -radius; y <= radius; y++) {
                  for (int z = -radius; z <= radius; z++) {
                     float radiusSq = shape.distanceSq(x, y, z);
                     if (radiusSq < maxRadiusSq) {
                        offsets.add(x);
                        offsets.add(y);
                        offsets.add(z);
                     }
                  }
               }
            }

            this.offsets = offsets.toIntArray();
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

         int radius = this.radius[0];
         int brushShape = 0;
         if (this.oldRadius != radius || this.oldBrushShape != brushShape) {
            this.oldRadius = radius;
            this.oldBrushShape = brushShape;
            this.previewSphere.clear();
            BallShape.getByIndex(brushShape).fillRegion(this.previewSphere, radius);
         }

         Selection.render(rc, 4);
         this.previewSphere.render(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 3);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         if (this.invert) {
            String countString = NumberFormat.getInstance().format((long)this.removeRegion.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.sculpt_draw_tool", countString);
            RegionHelper.pushBooleanRegionChange(this.removeRegion, Blocks.AIR.defaultBlockState(), historyDescription);
         } else {
            String countString = NumberFormat.getInstance().format((long)this.blockRegion.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.sculpt_draw_tool", countString);
            RegionHelper.pushBlockRegionChange(this.blockRegion, historyDescription);
         }

         this.reset();
      } else {
         int radius = this.radius[0];
         float maxRadius = radius + 0.5F;
         float maxRadiusSq = maxRadius * maxRadius;
         GaussianBlurTable gaussianBlur = new GaussianBlurTable(this.denoise ? 0.5F : 0.0F, 0.1F);
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         Selection.render(rc, 4);
         MaskElement sourceMaskElement = MaskManager.getSourceMask();
         MaskElement destMaskElement = MaskManager.getDestMask();
         MaskContext maskContext = new MaskContext(level);
         MutableBlockPos mutableBlockPos = new MutableBlockPos();

         class Normals {
            long totalX = 0L;
            long totalY = 0L;
            long totalZ = 0L;
         }


         record PositionDistance(int x, int y, int z, float distanceSq) {
         }


         record PositionDistanceBlock(int x, int y, int z, float distanceSq, BlockState blockState) {
         }

         if (!this.invert) {
            Position2ObjectMap<SimpleBlockWeightTracker> map = SimpleBlockWeightTracker.createMap();
            this.toolPather.update((x, y, z) -> {
               Normals normal = new Normals();
               List<PositionDistanceBlock> list = new ArrayList<>();

               for (int i = 0; i < this.offsets.length; i += 3) {
                  int xo = this.offsets[i];
                  int yo = this.offsets[i + 1];
                  int zo = this.offsets[i + 2];
                  if (!sourceMaskElement.test(maskContext.reset(), x + xo, y + yo, z + zo)) {
                     normal.totalX -= xo;
                     normal.totalY -= yo;
                     normal.totalZ -= zo;
                  } else {
                     BlockState blockState = level.getBlockState(mutableBlockPos.set(x + xo, y + yo, z + zo));
                     if (blockState.blocksMotion()) {
                        list.add(new PositionDistanceBlock(x + xo, y + yo, z + zo, xo * xo + yo * yo + zo * zo, blockState));
                        normal.totalX -= xo;
                        normal.totalY -= yo;
                        normal.totalZ -= zo;
                     }
                  }
               }

               if (!list.isEmpty()) {
                  double totalLength = Math.sqrt(normal.totalX * normal.totalX + normal.totalY * normal.totalY + normal.totalZ * normal.totalZ);
                  float normalY = (float)(normal.totalY / totalLength);
                  float normalX = this.maskY ? 0.0F : (float)(normal.totalX / totalLength);
                  float normalZ = this.maskY ? 0.0F : (float)(normal.totalZ / totalLength);

                  for (PositionDistanceBlock positionDistanceBlock : list) {
                     float inverseDistance = (float)Math.sqrt(maxRadiusSq - positionDistanceBlock.distanceSq);
                     inverseDistance *= this.strength[0];
                     Vector3f from = new Vector3f(positionDistanceBlock.x + 0.5F, positionDistanceBlock.y + 0.5F, positionDistanceBlock.z + 0.5F);
                     Vector3f to = from.add(normalX * inverseDistance, normalY * inverseDistance, normalZ * inverseDistance, new Vector3f());
                     Rasterization3D.xiaolinWu(from, to, (x2, y2, z2, v) -> {
                        if (!(v < 0.01)) {
                           float traveledX = positionDistanceBlock.x - x2;
                           float traveledY = positionDistanceBlock.y - y2;
                           float traveledZ = positionDistanceBlock.z - z2;
                           float traveledSq = traveledX * traveledX + traveledY * traveledY + traveledZ * traveledZ;
                           SimpleBlockWeightTracker tracker = map.getOrCreate(x2, y2, z2);
                           tracker.addWeight(positionDistanceBlock.blockState, v, v / (traveledSq + 0.25F));
                        }
                     });
                  }
               }
            });
            float[] table = gaussianBlur.getTable();
            float threshold = 0.08F + table[table.length / 2] * 0.87F;
            gaussianBlur.applyBlurCheckThreshold(map, this.influenceMap, threshold, (pos, block) -> {
               maskContext.reset();
               if (destMaskElement.test(maskContext, pos.getX(), pos.getY(), pos.getZ())) {
                  this.blockRegion.addBlock(pos, block);
               }
            });
            float opacity = (float)Math.sin(rc.nanos() / 1000000.0 / 50.0 / 8.0);
            this.blockRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.3F - opacity * 0.2F);
         } else {
            this.toolPather
               .update(
                  (x, y, z) -> {
                     Normals normal = new Normals();
                     List<PositionDistance> list = new ArrayList<>();

                     for (int i = 0; i < this.offsets.length; i += 3) {
                        int xo = this.offsets[i];
                        int yo = this.offsets[i + 1];
                        int zo = this.offsets[i + 2];
                        if (!sourceMaskElement.test(maskContext.reset(), x + xo, y + yo, z + zo)
                           || !level.getBlockState(mutableBlockPos.set(x + xo, y + yo, z + zo)).blocksMotion()) {
                           list.add(new PositionDistance(x + xo, y + yo, z + zo, xo * xo + yo * yo + zo * zo));
                           normal.totalX -= xo;
                           normal.totalY -= yo;
                           normal.totalZ -= zo;
                        }
                     }

                     if (!list.isEmpty()) {
                        double totalLength = Math.sqrt(normal.totalX * normal.totalX + normal.totalY * normal.totalY + normal.totalZ * normal.totalZ);
                        float normalY = (float)(normal.totalY / totalLength);
                        float normalX = this.maskY ? 0.0F : (float)(normal.totalX / totalLength);
                        float normalZ = this.maskY ? 0.0F : (float)(normal.totalZ / totalLength);
                        Position2FloatMap position2FloatMap = new Position2FloatMap();

                        for (PositionDistance positionDistanceBlock : list) {
                           float inverseDistance = (float)Math.sqrt(maxRadiusSq - positionDistanceBlock.distanceSq);
                           inverseDistance *= this.strength[0];
                           Vector3f from = new Vector3f(positionDistanceBlock.x + 0.5F, positionDistanceBlock.y + 0.5F, positionDistanceBlock.z + 0.5F);
                           Vector3f to = from.add(normalX * inverseDistance, normalY * inverseDistance, normalZ * inverseDistance, new Vector3f());
                           Rasterization3D.xiaolinWu(from, to, (x2, y2, z2, v) -> {
                              if (!(v < 0.01)) {
                                 position2FloatMap.add(x2, y2, z2, v);
                              }
                           });
                        }

                        float[] tablex = gaussianBlur.getTable();
                        float thresholdx = 0.08F + tablex[tablex.length / 2] * 0.87F;
                        gaussianBlur.applyBlurCheckThreshold(position2FloatMap, this.floatInfluenceMap, thresholdx, (x2, y2, z2) -> {
                           if (destMaskElement.test(maskContext.reset(), x2, y2, z2)) {
                              ChunkRenderOverrider.setBlock(x2, y2, z2, Blocks.AIR.defaultBlockState());
                              this.removeRegion.add(x2, y2, z2);
                           }
                        });
                     }
                  }
               );
            this.removeRegion.render(rc, Vec3.ZERO, 8);
         }
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      boolean changed = ImGui.sliderInt(AxiomI18n.get("axiom.tool.generic.brush_radius"), this.radius, 1, 32);
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.sculpt_draw"));
      changed |= ImGui.sliderFloat(AxiomI18n.get("axiom.tool.sculpt_draw.strength"), this.strength, 0.1F, 5.0F, "%.3f", 32);
      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.sculpt_draw.invert"), this.invert)) {
         this.invert = !this.invert;
         changed = true;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.sculpt_draw.mask_y"), this.maskY)) {
         this.maskY = !this.maskY;
         changed = true;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.sculpt_draw.denoise"), this.denoise)) {
         this.denoise = !this.denoise;
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
      return this.radiusAdjustment.initiateAdjustment(this.radius[0], -1);
   }

   @Override
   public Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      GenericRadiusAdjustment.Result result = this.radiusAdjustment.renderAdjustment(mouseX, mouseY, mouseDelta);
      this.radius[0] = result.radius();
      return result.mouseDelta();
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.sculpt_draw");
   }

   @Override
   public Tutorial getTutorial() {
      return Tutorial.SCULPT_DRAW_TOOL;
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putInt("BrushRadius", this.radius[0]);
      tag.putFloat("Strength", this.strength[0]);
      tag.putBoolean("Invert", this.invert);
      tag.putBoolean("MaskY", this.maskY);
      tag.putBoolean("Denoise", this.denoise);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.radius[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "BrushRadius", 5);
      this.strength[0] = VersionUtilsNbt.helperCompoundTagGetFloatOr(tag, "Strength", 0.5F);
      this.invert = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "Invert", false);
      this.maskY = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "MaskY", false);
      this.denoise = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "Denoise", true);
   }

   @Override
   public char iconChar() {
      return '\ue90a';
   }

   @Override
   public String keybindId() {
      return "sculpt_draw";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_SCULPTDRAW, AxiomPermission.BUILD_SECTION);
   }
}
