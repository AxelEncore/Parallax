package com.moulberry.axiom.hooks;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.buildertools.BuilderToolManager;
import com.moulberry.axiom.capabilities.AngelPlacement;
import com.moulberry.axiom.capabilities.ArcballCamera;
import com.moulberry.axiom.capabilities.BuildSymmetry;
import com.moulberry.axiom.capabilities.ReplaceMode;
import com.moulberry.axiom.clipboard.ModifySelection;
import com.moulberry.axiom.clipboard.Placement;
import com.moulberry.axiom.clipboard.Selection;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.displayentity.DisplayEntityManipulator;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.windows.operations.AutoshadeWindow;
import com.moulberry.axiom.marker.MarkerEntityManipulator;
import com.moulberry.axiom.mask.MaskContext;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.BiomeOverlayRenderer;
import com.moulberry.axiom.render.BlockTessellator;
import com.moulberry.axiom.render.CollisionMeshOverlayRenderer;
import com.moulberry.axiom.render.ShaderManager;
import com.moulberry.axiom.render.annotations.Annotations;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.tools.ToolManager;
import com.moulberry.axiom.tools.fluidball.FluidBall;
import com.moulberry.axiom.tools.lasso_select.LassoSelect;
import com.moulberry.axiom.tools.lasso_select.PendingLassoSelect;
import com.moulberry.axiom.tools.ruler.RulerTool;
import com.moulberry.axiom.utils.FramebufferUtils;
import com.moulberry.axiom.utils.RenderHelper;
import com.moulberry.axiom.world_modification.Dispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.CompiledSection;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

public class WorldRenderHook {
   public static float distance = 0.0F;
   public static boolean hasDistance = false;
   public static final CompiledSection VERY_VISIBLE_CHUNK = new CompiledSection() {
      public boolean facesCanSeeEachother(Direction direction, Direction direction2) {
         return true;
      }
   };

   private static boolean renderHookOnPost() {
      return Minecraft.useShaderTransparency() || ToolManager.isToolActive() && ToolManager.getCurrentTool() instanceof FluidBall;
   }

   public static void renderPre(AxiomWorldRenderContext rc) {
      RenderHelper.updateBaseLineWidth(Minecraft.getInstance().getWindow());
      AxiomRenderer.setLineWidthLegacy(RenderHelper.baseLineWidth);
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      AxiomRenderer.forgetRememberedVertexBuffers();
      BlockTessellator.SHARED.reset();
      RenderBuffers renderBuffers = Minecraft.getInstance().renderBuffers();
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         Vec3 vec3 = rc.position();
         double camX = vec3.x();
         double camY = vec3.y();
         double camZ = vec3.z();

         for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldItem = Minecraft.getInstance().player.getItemInHand(hand);
            if (heldItem.getItem() instanceof BlockItem) {
               AngelPlacement.render(rc.poseStack(), renderBuffers.bufferSource(), camX, camY, camZ, rc.partialTick(), hand, heldItem);
               renderBuffers.bufferSource().endLastBatch();
               break;
            }
         }
      }

      if (!renderHookOnPost()) {
         renderHook(rc);
      }

      renderBuffers.bufferSource().endBatch();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void renderPost(AxiomWorldRenderContext rc) {
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
      if (renderHookOnPost()) {
         renderHook(rc);
      }

      Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
      BiomeOverlayRenderer.INSTANCE.render(rc);
      CollisionMeshOverlayRenderer.INSTANCE.render(rc);
      Annotations.renderPost(rc);
      ShaderManager.blitSelectionOutlineTarget();
      AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
   }

   private static void renderHook(AxiomWorldRenderContext rc) {
      long oldImGuiContext = EditorUI.pushImGuiContext();

      try {
         Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
         MaskContext.frame++;
         Annotations.renderPre(rc);
         if (Annotations.showAnnotations() && Annotations.hasVisibleOutlineAnnotation) {
            ShaderManager.preCopySelectionOutlineTarget();
         }

         ReplaceMode.renderTick();
         EditorUI.lastProjectionMatrix = new Matrix4f(rc.projection());
         EditorUI.lastViewQuaternion = new Quaternionf(rc.rotation());
         if (Placement.INSTANCE.isPlacing()) {
            Placement.INSTANCE.render(rc);
         }

         if (EditorUI.isActive()) {
            if (ModifySelection.isModifyingSelection()) {
               ModifySelection.render(rc);
            } else if (ToolManager.isToolActive()) {
               Tool currentTool = ToolManager.getCurrentTool();
               currentTool.render(rc);
            } else {
               Selection.render(rc, 7);
            }

            AutoshadeWindow.renderWorld(rc);
         } else {
            if (BuilderToolManager.isToolSlotActive()) {
               BuilderToolManager.renderWorld(rc);
            }

            BuildSymmetry.renderWorld(rc);
            DisplayEntityManipulator.render(rc);
         }

         MarkerEntityManipulator.render(rc);
         handleDepthFeatures(rc.projection(), rc.partialTick());
         if (Axiom.configuration.visuals.showRuler || ToolManager.isToolActive() && ToolManager.getCurrentTool() instanceof RulerTool) {
            RulerTool.renderPoints(rc);
         }

         Dispatcher.renderTracers(rc);
      } finally {
         EditorUI.popImGuiContext(oldImGuiContext);
      }
   }

   private static void handleDepthFeatures(Matrix4fc projection, float tickDelta) {
      if (!LassoSelect.pendingDepthBuffer.isEmpty()) {
         float[] pixels = null;
         RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();

         for (PendingLassoSelect pending : LassoSelect.pendingDepthBuffer) {
            if (pending.width() == renderTarget.width && pending.height() == renderTarget.height) {
               if (pixels == null) {
                  FramebufferUtils.bindDepth(renderTarget);
                  pixels = new float[renderTarget.width * renderTarget.height];
                  GL11.glReadPixels(0, 0, renderTarget.width, renderTarget.height, 6402, 5126, pixels);
                  renderTarget.bindWrite(true);
               }

               pending.handle(pixels, new Matrix4f(projection));
            }
         }

         LassoSelect.pendingDepthBuffer.clear();
      }

      if (EditorUI.pendingDepthActions.isEmpty()) {
         hasDistance = false;
      } else {
         RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
         Vec2 fraction = EditorUI.getMouseViewportFraction();
         updateDepthDistance(renderTarget, projection, fraction);
      }

      if (ClientEvents.waitingForOrbitCameraDepth) {
         ClientEvents.waitingForOrbitCameraDepth = false;
         RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
         updateDepthDistance(renderTarget, projection, new Vec2(0.5F, 0.5F));
         LocalPlayer player = Minecraft.getInstance().player;
         if (hasDistance && player != null) {
            Vec3 look = player.getLookAngle();
            Vec3 point = player.getEyePosition(tickDelta).add(look.scale(WorldRenderHook.distance));
            int minBuildHeight = player.level().getMinBuildHeight();
            if (point.y < minBuildHeight) {
               double yDelta = minBuildHeight - point.y;
               if (Math.abs(look.y) > 1.0E-4) {
                  double scale = yDelta / look.y;
                  point = point.add(look.scale(scale));
               }
            }

            Vec3 raycastStart = point.add(look.scale(-1.0));
            Vec3 raycastEnd = point.add(look.scale(1.0));
            BlockHitResult result = player.level().clip(new ClipContext(raycastStart, raycastEnd, Block.OUTLINE, Fluid.NONE, player));
            if (!result.isInside() && result.getType() == Type.BLOCK) {
               point = result.getLocation();
            }

            float distance = (float)point.distanceTo(player.getEyePosition());
            ArcballCamera.lock(point, distance);
         }
      }
   }

   private static void updateDepthDistance(RenderTarget renderTarget, Matrix4fc projection, Vec2 fraction) {
      if (fraction != null && fraction.x >= 0.0F && fraction.x <= 1.0F && fraction.y >= 0.0F && fraction.y <= 1.0F) {
         float fractionX = fraction.x;
         float fractionY = 1.0F - fraction.y;
         float[] pixel = new float[1];
         int x = Math.round((renderTarget.width - 1) * fractionX);
         int y = Math.round((renderTarget.height - 1) * fractionY);
         FramebufferUtils.bindDepth(renderTarget);
         GL11.glReadPixels(x, y, 1, 1, 6402, 5126, pixel);
         renderTarget.bindWrite(true);
         Matrix4f matrix = new Matrix4f(projection);
         matrix.invert();
         Vector4f forwards = new Vector4f(fraction.x * 2.0F - 1.0F, fraction.y * 2.0F - 1.0F, pixel[0] * 2.0F - 1.0F, 1.0F);
         forwards.mul(matrix);
         float a = forwards.x / forwards.w;
         float b = forwards.y / forwards.w;
         float c = forwards.z / forwards.w;
         distance = (float)Math.sqrt(a * a + b * b + c * c);
         hasDistance = true;
         float depthFar = matrix.perspectiveFar();
         if (distance > depthFar / 2.0F) {
            distance = Math.min(distance, (float)(Minecraft.getInstance().options.getEffectiveRenderDistance() * 16));
         }
      }

      Entity cameraEntity = Minecraft.getInstance().cameraEntity;
      if (cameraEntity != null) {
         RayCaster.RaycastResult result = Tool.raycastBlock();
         if (result != null) {
            double raycastDistance = result.getLocation().distanceTo(cameraEntity.getEyePosition());
            if (hasDistance) {
               distance = Math.min((float)raycastDistance, distance);
            } else {
               hasDistance = true;
               distance = (float)raycastDistance;
            }
         }
      }
   }
}
