package com.moulberry.axiom.tools.freehand;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.VersionUtilsNbt;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPather;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class FreehandSelect implements Tool {
   private ChunkedBooleanRegion chunkedBooleanRegion = new ChunkedBooleanRegion();
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();
   private final int[] mode = new int[]{1};
   private static final int MODE_REPLACE = 0;
   private static final int MODE_ADD = 1;
   private static final int MODE_SUBTRACT = 2;
   private static final int MODE_INTERSECT = 3;

   @Override
   public void reset() {
      this.usingTool = false;
      this.chunkedBooleanRegion.clear();
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
            this.usingTool = true;
            this.pathProvider = new AsyncToolPathProvider(this.createToolPather(this.brushWidget.getBrushShape()));
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
         RayCaster.RaycastResult result = Tool.raycastBlock(false, true, Tool.defaultIncludeFluids());
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
         PositionSet positionSet = this.chunkedBooleanRegion.unsafeGetPositionSet();
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

         this.reset();
      } else {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         this.pathProvider.update();
         Selection.render(rc, 4);
         this.chunkedBooleanRegion.render(rc, Vec3.ZERO, 3);
      }
   }

   private AsyncToolPather createToolPather(BrushShape brushShape) {
      MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
      MaskElement destMask = MaskManager.getSelectionMask();
      return new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
         if (destMask.test(maskContext.reset(), x, y, z)) {
            this.chunkedBooleanRegion.add(x, y, z);
         }
      });
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      this.brushWidget.displayImgui();
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
      return AxiomI18n.get("axiom.tool.freehand_select");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
      tag.putByte("Mode", (byte)this.mode[0]);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
      this.mode[0] = VersionUtilsNbt.helperCompoundTagGetIntOr(tag, "Mode", 1);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue913';
   }

   @Override
   public String keybindId() {
      return "freehand_select";
   }

   @Override
   public int defaultKeybind() {
      return 78;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_FREEHANDSELECT);
   }
}
