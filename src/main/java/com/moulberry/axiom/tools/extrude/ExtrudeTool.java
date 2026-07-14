package com.moulberry.axiom.tools.extrude;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import imgui.moulberry92.ImGui;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public class ExtrudeTool implements Tool {
   private ChunkedBooleanRegion shrinkRegion = new ChunkedBooleanRegion();
   private ChunkedBlockRegion expandRegion = new ChunkedBlockRegion();
   private BlockPos lastHoveredBlockPos = null;
   private Direction lastHoveredDirection = null;
   private long lastUseMillis = 0L;
   private boolean shrink = false;
   private boolean displace = false;
   private int[] compareMode = new int[]{0};
   private final float[] limit = new float[]{100000.0F};
   private int[] count = new int[]{1};
   private boolean corners = false;
   private Future<Object> livePreviewFuture = null;

   @Override
   public void reset() {
      this.lastHoveredBlockPos = null;
      this.lastHoveredDirection = null;
      this.lastUseMillis = System.currentTimeMillis();
      this.expandRegion.clear();
      this.shrinkRegion.clear();
      if (this.livePreviewFuture != null) {
         this.livePreviewFuture.cancel(true);
         this.livePreviewFuture = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.rightClick();
            return UserAction.ActionResult.USED_STOP;
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   public void rightClick() {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result != null) {
         ClientLevel world = Minecraft.getInstance().level;
         if (this.shrink) {
            ExtrudeShrinkTask task = new ExtrudeShrinkTask(
               world, result.getBlockPos(), result.getDirection(), this.compareMode[0], true, this.displace, this.count[0], this.corners
            );
            task.perform((int)this.limit[0]);
            String countString = NumberFormat.getInstance().format((long)task.chunkedBlockRegion.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.extrude_tool", countString);
            RegionHelper.pushBlockRegionChange(task.chunkedBlockRegion, historyDescription);
         } else {
            ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
            ExtrudeExpandTask task = new ExtrudeExpandTask(
               world, result.getBlockPos(), result.getDirection(), this.compareMode[0], this.displace, this.count[0], this.corners
            );
            task.perform((int)this.limit[0]);
            task.expandBlocks.forEachEntry(chunkedBlockRegion::addBlock);
            String countString = NumberFormat.getInstance().format((long)chunkedBlockRegion.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.extrude_tool", countString);
            RegionHelper.pushBlockRegionChange(chunkedBlockRegion, historyDescription);
         }
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
      if (Tool.getLookDirection() == null) {
         Selection.render(rc, 7);
      } else {
         Selection.render(rc, 4);
         if (this.livePreviewFuture != null && this.livePreviewFuture.isDone()) {
            try {
               Object extrudeTask = this.livePreviewFuture.get();
               if (extrudeTask instanceof ExtrudeExpandTask extrudeExpandTask) {
                  if (!this.shrink) {
                     this.lastHoveredDirection = extrudeExpandTask.direction;
                     this.lastHoveredBlockPos = extrudeExpandTask.initialPosition;
                     this.expandRegion.clear();
                     extrudeExpandTask.expandBlocks.forEachEntry(this.expandRegion::addBlockWithoutDirty);
                     this.expandRegion.dirtyAll();
                  }
               } else if (extrudeTask instanceof ExtrudeShrinkTask extrudeShrinkTask && this.shrink) {
                  this.lastHoveredDirection = extrudeShrinkTask.direction;
                  this.lastHoveredBlockPos = extrudeShrinkTask.initialPosition;
                  this.shrinkRegion.clear();
                  extrudeShrinkTask.shrinkBlocks.forEach(this.shrinkRegion::add);
               }
            } catch (ExecutionException | InterruptedException var5) {
               throw new RuntimeException(var5);
            }

            this.livePreviewFuture = null;
         }

         if (this.livePreviewFuture == null) {
            RayCaster.RaycastResult result = Tool.raycastBlock();
            if (result == null) {
               return;
            }

            if (!result.getDirection().equals(this.lastHoveredDirection)) {
               this.updateSelectedBlocks(result);
            } else if (!result.getBlockPos().equals(this.lastHoveredBlockPos)) {
               this.updateSelectedBlocks(result);
            }
         }

         if (this.shrink) {
            this.shrinkRegion.render(rc, Vec3.ZERO, 8);
         } else {
            long sinceLastUse = System.currentTimeMillis() - this.lastUseMillis;
            if (sinceLastUse > 1000000.0) {
               this.lastUseMillis = System.currentTimeMillis() - 2000L;
            }

            float opacity = (float)Math.sin((float)sinceLastUse / 400.0F - 3.429F);
            if (sinceLastUse > 0L && sinceLastUse < 2000L) {
               this.expandRegion.render(rc, Vec3.ZERO, (float)sinceLastUse / 2000.0F, 0.75F - opacity * 0.5F);
            } else {
               this.expandRegion.render(rc, Vec3.ZERO, 0.75F + opacity * 0.25F, 0.75F - opacity * 0.5F);
            }
         }
      }
   }

   private void updateSelectedBlocks(RayCaster.RaycastResult result) {
      ClientLevel world = Minecraft.getInstance().level;
      if (this.shrink) {
         ExtrudeShrinkTask task = new ExtrudeShrinkTask(
            world, result.getBlockPos(), result.getDirection(), this.compareMode[0], false, false, this.count[0], this.corners
         );
         this.livePreviewFuture = Tool.sharedPoolThreadExecutor.submit(() -> {
            task.perform((int)this.limit[0]);
            return task;
         });
      } else {
         ExtrudeExpandTask task = new ExtrudeExpandTask(
            world, result.getBlockPos(), result.getDirection(), this.compareMode[0], false, this.count[0], this.corners
         );
         this.livePreviewFuture = Tool.sharedPoolThreadExecutor.submit(() -> {
            task.perform((int)this.limit[0]);
            return task;
         });
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.extrude.mode"));
      int newShrink = -1;
      if (!this.shrink) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.extrude.mode.expand"))) {
         newShrink = 0;
      }

      if (!this.shrink) {
         ImGui.endDisabled();
      }

      ImGui.sameLine();
      if (this.shrink) {
         ImGui.beginDisabled();
      }

      if (ImGui.button(AxiomI18n.get("axiom.tool.extrude.mode.shrink"))) {
         newShrink = 1;
      }

      if (this.shrink) {
         ImGui.endDisabled();
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.tool.extrude.displace"), this.displace)) {
         this.displace = !this.displace;
      }

      ImGui.sameLine();
      if (this.shrink) {
         ImGuiHelper.helpMarker(AxiomI18n.get("axiom.tool.extrude.displace_shrink_description"));
      } else {
         ImGuiHelper.helpMarker(AxiomI18n.get("axiom.tool.extrude.displace_expand_description"));
      }

      if (newShrink != -1) {
         this.shrink = newShrink == 1;
         if (this.shrink) {
            this.expandRegion.clear();
         } else {
            this.shrinkRegion.clear();
         }

         if (this.livePreviewFuture != null) {
            this.livePreviewFuture.cancel(true);
            this.livePreviewFuture = null;
         }
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.extrude"));
      ImGuiHelper.Label compare = ImGuiHelper.translateLabel("axiom.tool.magic_select.compare_mode");
      ImGuiHelper.Label limit = ImGuiHelper.translateLabel("axiom.tool.generic.limit");
      ImGuiHelper.LabelPosition labelPosition = ImGuiHelper.calcLabelPosition(compare, limit);
      ImGuiHelper.drawLabelledWidget(
         compare,
         labelPosition,
         label -> ImGuiHelper.combo(
            label,
            this.compareMode,
            new String[]{
               AxiomI18n.get("axiom.tool.magic_select.compare_block"),
               AxiomI18n.get("axiom.tool.magic_select.compare_blockstate"),
               AxiomI18n.get("axiom.tool.magic_select.compare_solid"),
               AxiomI18n.get("axiom.tool.magic_select.compare_any")
            }
         )
      );
      ImGuiHelper.drawLabelledWidget(limit, labelPosition, label -> ImGui.sliderFloat(label, this.limit, 2.0F, 1.0E7F, "%'.0f", 32));
      if (!this.displace) {
         ImGui.sliderInt("Count", this.count, 1, 32);
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.corners"), this.corners)) {
         this.corners = !this.corners;
      }
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.extrude");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putBoolean("Shrink", this.shrink);
      tag.putBoolean("Displace", this.displace);
      tag.putInt("Compare", this.compareMode[0]);
      tag.putInt("Limit", (int)this.limit[0]);
      tag.putInt("Count", this.count[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.shrink = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "Shrink", false);
      this.displace = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "Displace", false);
      this.compareMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Compare", 0);
      this.limit[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Limit", 100000);
   }

   @Override
   public char iconChar() {
      return '\ue904';
   }

   @Override
   public String keybindId() {
      return "extrude";
   }

   @Override
   public int defaultKeybind() {
      return 89;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_EXTRUDE, AxiomPermission.BUILD_SECTION);
   }
}
