package com.moulberry.axiom.tools.freehand;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.brush_shapes.BrushShape;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.editor.widgets.BrushWidget;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.mask.MaskElement;
import com.moulberry.axiom.mask.MaskManager;
import com.moulberry.axiom.pather.async.AsyncToolPathProvider;
import com.moulberry.axiom.pather.async.AsyncToolPatherUnique;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import java.text.NumberFormat;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class FreehandDraw implements Tool {
   private final ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
   private final ChunkedBooleanRegion airRegion = new ChunkedBooleanRegion();
   private boolean paintingAir = false;
   private boolean usingTool = false;
   private AsyncToolPathProvider pathProvider = null;
   private final BrushWidget brushWidget = new BrushWidget();

   @Override
   public void reset() {
      this.usingTool = false;
      if (this.pathProvider != null) {
         this.pathProvider.close();
         this.pathProvider = null;
      }

      this.chunkedBlockRegion.clear();
      this.airRegion.clear();
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            BrushShape brushShape = this.brushWidget.getBrushShape();
            MaskElement maskElement = MaskManager.getDestMask();
            MaskContext maskContext = new MaskContext(Minecraft.getInstance().level);
            BlockState blockState = Tool.getActiveBlock();
            this.reset();
            this.usingTool = true;
            if (blockState.isAir()) {
               this.paintingAir = true;
               this.pathProvider = new AsyncToolPathProvider(new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
                  maskContext.reset();
                  BlockState existing = maskContext.getBlockState(x, y, z);
                  if (!existing.isAir() && maskElement.test(maskContext, x, y, z)) {
                     this.chunkedBlockRegion.addBlock(x, y, z, blockState);
                     this.airRegion.add(x, y, z);
                  }
               }));
            } else {
               this.paintingAir = false;
               this.pathProvider = new AsyncToolPathProvider(new AsyncToolPatherUnique(brushShape, (x, y, z) -> {
                  if (maskElement.test(maskContext.reset(), x, y, z)) {
                     this.chunkedBlockRegion.addBlock(x, y, z, blockState);
                  }
               }));
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
         this.brushWidget.renderPreview(rc, Vec3.atLowerCornerOf(result.getBlockPos()), 1);
      } else if (Tool.cancelUsing()) {
         this.reset();
      } else if (!Tool.isMouseDown(1)) {
         this.pathProvider.finish();
         String countString = NumberFormat.getInstance().format((long)this.chunkedBlockRegion.count());
         String historyDescription = AxiomI18n.get("axiom.history_description.drew", countString);
         RegionHelper.pushBlockRegionChange(this.chunkedBlockRegion, historyDescription);
         this.reset();
      } else {
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

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.separatorWithText(AxiomI18n.get("axiom.tool.generic.brush"));
      this.brushWidget.displayImgui();
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
      return AxiomI18n.get("axiom.tool.freehand_draw");
   }

   @Override
   public void writeSettings(CompoundTag tag) {
      this.brushWidget.writeSettings(tag);
   }

   @Override
   public void loadSettings(CompoundTag tag) {
      this.brushWidget.loadSettings(tag);
   }

   @Override
   public boolean showToolSmoothing() {
      return true;
   }

   @Override
   public char iconChar() {
      return '\ue912';
   }

   @Override
   public String keybindId() {
      return "freehand_draw";
   }

   @Override
   public int defaultKeybind() {
      return 71;
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.TOOL_FREEHANDDRAW, AxiomPermission.BUILD_SECTION);
   }
}
