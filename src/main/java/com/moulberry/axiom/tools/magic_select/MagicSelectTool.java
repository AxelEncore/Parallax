package com.moulberry.axiom.tools.magic_select;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import java.util.EnumSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public class MagicSelectTool implements Tool {
   private final ChunkedBooleanRegion booleanRegion = new ChunkedBooleanRegion();
   private BlockPos lastTargetBlock = null;
   private Future<MagicSelectionTask> livePreviewFuture = null;
   private int[] compareMode = new int[]{0};
   private final float[] limit = new float[]{100000.0F};
   private final int[] range = new int[]{1};
   private boolean maskSurface = false;
   private boolean propagateUp = true;
   private boolean propagateDown = true;
   private boolean propagateHorizontal = true;
   private boolean propagateCorners = true;
   private final int[] mode = new int[]{1};
   private static final int MODE_REPLACE = 0;
   private static final int MODE_ADD = 1;
   private static final int MODE_SUBTRACT = 2;
   private static final int MODE_INTERSECT = 3;

   @Override
   public void reset() {
      this.booleanRegion.clear();
      this.lastTargetBlock = null;
      if (this.livePreviewFuture != null) {
         this.livePreviewFuture.cancel(true);
         this.livePreviewFuture = null;
      }
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      if (action != UserAction.RIGHT_MOUSE) {
         return UserAction.ActionResult.NOT_HANDLED;
      } else {
         this.rightClick();
         return UserAction.ActionResult.USED_STOP;
      }
   }

   private int getPropagationFlags() {
      int flags = 0;
      if (this.propagateUp) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_UP;
      }

      if (this.propagateDown) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_DOWN;
      }

      if (this.propagateHorizontal) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_HORIZONTAL;
      }

      if (this.propagateCorners) {
         flags |= MagicSelectionTask.PROPAGATION_FLAG_CORNERS;
      }

      return flags;
   }

   public void rightClick() {
      RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
      if (result != null) {
         BlockPos position = result.getBlockPos();
         this.lastTargetBlock = position;
         ClientLevel world = Minecraft.getInstance().level;
         PositionSet positionSet = new PositionSet();
         MaskElement maskElement = MaskManager.getSelectionMask();
         MagicSelectionTask task;
         if (this.range[0] > 1) {
            task = new MagicSelectionRangedTask(positionSet, world, position, this.compareMode[0], maskElement, this.range[0], this.getPropagationFlags());
         } else if (this.maskSurface) {
            task = new MagicSelectionSurfaceTask(positionSet, world, position, this.compareMode[0], maskElement, this.getPropagationFlags());
         } else {
            task = new MagicSelectionTask(positionSet, world, position, this.compareMode[0], maskElement, this.getPropagationFlags());
         }

         task.fill((int)this.limit[0]);
         if (!positionSet.isEmpty()) {
            switch (this.mode[0]) {
               case 0:
                  Selection.clearSelection();
                  Selection.addSet(positionSet);
                  break;
               case 1:
                  Selection.addSet(positionSet);
                  break;
               case 2:
                  Selection.subtractSet(positionSet);
                  break;
               case 3:
                  Selection.intersectSet(positionSet);
                  break;
               default:
                  throw new FaultyImplementationError();
            }
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
               MagicSelectionTask task = this.livePreviewFuture.get();
               if (Tool.isMouseDown(1) && Selection.getSelectionBuffer().size() + task.positionSet.count() < 1048576) {
                  switch (this.mode[0]) {
                     case 0:
                        Selection.clearSelection();
                        Selection.setBuffer(Selection.getSelectionBuffer().addSet(task.positionSet, true));
                        break;
                     case 1:
                        Selection.setBuffer(Selection.getSelectionBuffer().addSet(task.positionSet, false));
                        break;
                     case 2:
                        Selection.setBuffer(Selection.getSelectionBuffer().subtractSet(task.positionSet, false));
                        break;
                     case 3:
                        Selection.setBuffer(Selection.getSelectionBuffer().intersectSet(task.positionSet, false));
                        break;
                     default:
                        throw new FaultyImplementationError();
                  }
               }

               this.booleanRegion.clear();
               task.positionSet.forEach(this.booleanRegion::add);
               this.livePreviewFuture = null;
            } catch (ExecutionException | InterruptedException var7) {
               throw new RuntimeException(var7);
            }
         }

         if (this.livePreviewFuture == null) {
            RayCaster.RaycastResult result = Tool.raycastBlock(false, true, true);
            if (result == null) {
               return;
            }

            if (!result.getBlockPos().equals(this.lastTargetBlock)) {
               this.lastTargetBlock = result.getBlockPos();
               BlockPos position = result.getBlockPos();
               ClientLevel world = Minecraft.getInstance().level;
               MaskElement maskElement = MaskManager.getSelectionMask();
               MagicSelectionTask task;
               if (this.range[0] > 1) {
                  task = new MagicSelectionRangedTask(
                     new PositionSet(), world, position, this.compareMode[0], maskElement, this.range[0], this.getPropagationFlags()
                  );
               } else if (this.maskSurface) {
                  task = new MagicSelectionSurfaceTask(new PositionSet(), world, position, this.compareMode[0], maskElement, this.getPropagationFlags());
               } else {
                  task = new MagicSelectionTask(new PositionSet(), world, position, this.compareMode[0], maskElement, this.getPropagationFlags());
               }

               this.livePreviewFuture = Tool.sharedPoolThreadExecutor.submit(() -> {
                  task.fill((int)this.limit[0]);
                  return task;
               });
            }
         }

         this.booleanRegion.render(rc, Vec3.ZERO, 3);
      }
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.magic_select"));
      ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.magic_select.compare_mode"),
         this.compareMode,
         new String[]{
            AxiomI18n.get("axiom.tool.magic_select.compare_block"),
            AxiomI18n.get("axiom.tool.magic_select.compare_blockstate"),
            AxiomI18n.get("axiom.tool.magic_select.compare_solid"),
            AxiomI18n.get("axiom.tool.magic_select.compare_material"),
            AxiomI18n.get("axiom.tool.magic_select.compare_any")
         }
      );

      String tooltip = switch (this.compareMode[0]) {
         case 0 -> "Matches the same block, ignoring block properties";
         case 1 -> "Matches the same blockstate, checking block properties";
         case 2 -> "Matches all solid blocks";
         case 3 -> "Matches blocks that have the same material (eg. cobblestone includes Cobblestone, Cobblestone Stairs, Cobblestone Walls, etc.)";
         case 4 -> "Matches all non-air blocks";
         default -> throw new FaultyImplementationError();
      };
      ImGuiHelper.tooltip(tooltip);
      ImGui.sliderFloat(AxiomI18n.get("axiom.tool.generic.limit"), this.limit, 2.0F, 1000000.0F, "%'.0f", 32);
      ImGui.sliderInt(AxiomI18n.get("axiom.tool.magic_select.range"), this.range, 1, 5);
      if (this.range[0] <= 1 && ImGui.checkbox(AxiomI18n.get("axiom.tool.generic.surface_only"), this.maskSurface)) {
         this.maskSurface = !this.maskSurface;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.up"), this.propagateUp)) {
         this.propagateUp = !this.propagateUp;
      }

      ImGui.sameLine();
      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.down"), this.propagateDown)) {
         this.propagateDown = !this.propagateDown;
      }

      if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.horizontal"), this.propagateHorizontal)) {
         this.propagateHorizontal = !this.propagateHorizontal;
      }

      if (this.range[0] > 1) {
         this.propagateCorners = true;
      } else if (!this.propagateHorizontal) {
         this.propagateCorners = false;
      } else {
         ImGui.sameLine();
         if (ImGui.checkbox(AxiomI18n.get("axiom.hardcoded.corners"), this.propagateCorners)) {
            this.propagateCorners = !this.propagateCorners;
         }
      }

      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.selection"));
      ImGuiHelper.combo(
         AxiomI18n.get("axiom.tool.selection.mode"),
         this.mode,
         new String[]{
            AxiomI18n.get("axiom.tool.selection.replace"),
            AxiomI18n.get("axiom.tool.selection.add"),
            AxiomI18n.get("axiom.tool.selection.subtract"),
            AxiomI18n.get("axiom.tool.selection.intersect")
         }
      );
   }

   @Override
   public String name() {
      return AxiomI18n.get("axiom.tool.magic_select");
   }

   @Override
   public Tutorial getTutorial() {
      return Tutorial.MAGIC_SELECT_TOOL;
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      tag.putByte("CompareMode", (byte)this.compareMode[0]);
      tag.putInt("Limit", (int)this.limit[0]);
      tag.putInt("Range", this.range[0]);
      tag.putBoolean("SurfaceOnly", this.maskSurface);
      tag.putByte("SelectionMode", (byte)this.mode[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.compareMode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "CompareMode", 0);
      this.limit[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Limit", 100000);
      this.range[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Range", 1);
      this.maskSurface = VersionUtilsNbt.helperCompoundTagGetBooleanOr(tag, "SurfaceOnly", false);
      this.mode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "SelectionMode", 1);
   }

   @Override
   public char iconChar() {
      return '\ue902';
   }

   @Override
   public String keybindId() {
      return "magic_select";
   }

   @Override
   public int defaultKeybind() {
      return 77;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_MAGICSELECT);
   }
}
