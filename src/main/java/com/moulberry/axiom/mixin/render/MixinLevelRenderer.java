package com.moulberry.axiom.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.moulberry.axiom.AxiomClient;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.capabilities.NoClip;
import com.moulberry.axiom.capabilities.ReplaceMode;
import com.moulberry.axiom.capabilities.Tinker;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.hooks.LevelRendererExt;
import com.moulberry.axiom.hooks.WorldRenderHook;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.ChunkRenderOverrider;
import com.moulberry.axiom.utils.IrisApiWrapper;
import com.moulberry.axiom.utils.RenderHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {LevelRenderer.class},
   priority = 1007
)
public class MixinLevelRenderer implements LevelRendererExt {
   @Shadow
   @Nullable
   private ClientLevel level;
   @Shadow
   @Final
   private RenderBuffers renderBuffers;
   @Shadow
   @Final
   private Minecraft minecraft;
   @Unique
   private RenderTarget overrideTranslucentRenderTarget = null;
   @Unique
   private static AxiomWorldRenderContext renderContext = null;

   @Override
   public void axiom$pushTranslucentRenderTarget(RenderTarget renderTarget) {
      this.overrideTranslucentRenderTarget = renderTarget;
   }

   @Override
   public void axiom$popTranslucentRenderTarget() {
      this.overrideTranslucentRenderTarget = null;
   }

   @Inject(
      method = {"getTranslucentTarget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void getMainRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
      if (this.overrideTranslucentRenderTarget != null) {
         cir.setReturnValue(this.overrideTranslucentRenderTarget);
      }
   }

   @Inject(
      method = {"compileSections"},
      at = {@At("HEAD")}
   )
   public void compileSections(Camera camera, CallbackInfo ci) {
      Vec3 position = camera.getPosition();
      ChunkRenderOverrider.uploadDirty(position.x, position.y, position.z);
   }

   @Inject(
      method = {"renderSectionLayer"},
      at = {@At("RETURN")}
   )
   public void renderSectionLayerPost(RenderType renderType, double d, double e, double f, Matrix4f transform, Matrix4f projection, CallbackInfo ci) {
      ChunkRenderOverrider.AxiomChunkOverrideLayer axiomLayer = ChunkRenderOverrider.AxiomChunkOverrideLayer.fromVanilla(renderType);
      RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
      ChunkRenderOverrider.render(axiomLayer, target, transform, d, e, f);
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("HEAD")}
   )
   public void renderLevelHead(
      CallbackInfo ci,
      @Local(argsOnly = true) DeltaTracker deltaTracker,
      @Local(argsOnly = true) Camera camera,
      @Local(argsOnly = true,ordinal = 0) Matrix4f modelView,
      @Local(argsOnly = true,ordinal = 1) Matrix4f projection
   ) {
      EditorUI.lastProjectionMatrix = new Matrix4f(projection);
      PoseStack poseStack = new PoseStack();
      poseStack.mulPose(VersionUtilsClient.matrix4fcToMatrix4f(modelView));
      renderContext = new AxiomWorldRenderContext(camera, deltaTracker.getGameTimeDeltaPartialTick(false), poseStack, projection);
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V",
         shift = Shift.AFTER
      )}
   )
   public void renderLevelPre(CallbackInfo ci) {
      if (AxiomClient.isAxiomActive() && !IrisApiWrapper.isUsingShaders()) {
         this.renderBuffers.bufferSource().endBatch();
         RenderHelper.pushModelViewStackWithIdentity();
         WorldRenderHook.renderPre(renderContext);
         this.renderBuffers.bufferSource().endBatch();
         RenderHelper.popModelViewStack();
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("RETURN")}
   )
   public void renderLevelLast(CallbackInfo ci) {
      ChunkRenderOverrider.afterRenderLevel();
      if (AxiomClient.isAxiomActive() && IrisApiWrapper.isUsingShaders()) {
         this.renderBuffers.bufferSource().endBatch();
         RenderHelper.pushModelViewStackWithIdentity();
         WorldRenderHook.renderPre(renderContext);
         WorldRenderHook.renderPost(renderContext);
         this.renderBuffers.bufferSource().endBatch();
         RenderHelper.popModelViewStack();
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Options;getCloudsType()Lnet/minecraft/client/CloudStatus;",
         shift = Shift.BEFORE
      )}
   )
   public void renderLevelPost(CallbackInfo ci) {
      if (AxiomClient.isAxiomActive() && !IrisApiWrapper.isUsingShaders()) {
         this.renderBuffers.bufferSource().endBatch();
         RenderHelper.pushModelViewStackWithIdentity();
         WorldRenderHook.renderPost(renderContext);
         this.renderBuffers.bufferSource().endBatch();
         RenderHelper.popModelViewStack();
      }
   }

   @Inject(
      method = {"renderHitOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void renderHitOutline(
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      Entity entity,
      double d,
      double e,
      double f,
      BlockPos blockPos,
      BlockState blockState,
      CallbackInfo ci
   ) {
      ReplaceMode.ReplaceModeRenderState replaceModeRenderState = ReplaceMode.extractRenderState(this.level, entity, blockState, blockPos);
      if (replaceModeRenderState != null) {
         ReplaceMode.renderHitOutline(poseStack, this.renderBuffers.bufferSource(), d, e, f, replaceModeRenderState);
         ci.cancel();
      } else if (Tinker.legacyRenderHitOutline(poseStack, vertexConsumer, entity, d, e, f, blockPos, blockState)) {
         ci.cancel();
      }
   }

   @WrapOperation(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"
      )}
   )
   public boolean tick_isSpectator(LocalPlayer instance, Operation<Boolean> original) {
      return NoClip.canNoClip(instance) ? true : (Boolean)original.call(new Object[]{instance});
   }
}
