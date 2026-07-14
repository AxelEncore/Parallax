package com.moulberry.axiom.tools;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.core_rendering.AxiomRenderer;
import com.moulberry.axiom.editor.EditorUI;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.editor.tutorial.Tutorial;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.world_modification.HistoryBuffer;
import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public interface Tool {
   ExecutorService sharedSingleThreadExecutor = Executors.newSingleThreadExecutor();
   ForkJoinPool sharedPoolThreadExecutor = (ForkJoinPool)Executors.newWorkStealingPool();

   static BlockState getActiveBlock() {
      return EditorUI.isActive() ? EditorUI.getActiveBlock() : Blocks.STONE.defaultBlockState();
   }

   @Nullable
   static Vec3 getLookDirection() {
      if (Minecraft.getInstance().screen != null) {
         return null;
      } else if (EditorUI.isActive()) {
         return EditorUI.isMovingCamera() ? null : EditorUI.getMouseLookVector();
      } else {
         return Minecraft.getInstance().cameraEntity != null ? Minecraft.getInstance().cameraEntity.getLookAngle() : null;
      }
   }

   static boolean isShiftDown() {
      if (EditorUI.isActive()) {
         return EditorUI.getIO().getKeyShift();
      } else {
         long window = Minecraft.getInstance().getWindow().getWindow();
         return GLFW.glfwGetKey(window, 340) != 0 || GLFW.glfwGetKey(window, 344) != 0;
      }
   }

   static boolean isMouseDown(int button) {
      if (EditorUI.isActive()) {
         return button == 1 ? Keybinds.USE_TOOL.isDownIgnoreMods() : EditorUI.getIO().getMouseDown(button);
      } else if (button == 0) {
         return Minecraft.getInstance().options.keyAttack.isDown();
      } else if (button == 1) {
         return Minecraft.getInstance().options.keyUse.isDown();
      } else if (button == 2) {
         return Minecraft.getInstance().options.keyPickItem.isDown();
      } else {
         throw new UnsupportedOperationException("Unknown mouse button id: " + button);
      }
   }

   static boolean cancelUsing() {
      return Minecraft.getInstance().level == null || Minecraft.getInstance().player == null || Minecraft.getInstance().screen != null || !EditorUI.isEnabled();
   }

   static boolean defaultIncludeFluids() {
      return Axiom.configuration.blockAttributes.makeFluidHitboxesSolid;
   }

   static RayCaster.RaycastResult raycastBlock() {
      return raycastBlock(false, true, defaultIncludeFluids());
   }

   static RayCaster.RaycastResult raycastBlock(boolean includeSelection, boolean includeNonSolid, boolean includeFluids) {
      Level level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         Entity entity = Minecraft.getInstance().cameraEntity;
         if (entity == null) {
            return null;
         } else {
            Vec3 view = getLookDirection();
            if (view == null) {
               return null;
            } else {
               Vec3 start = entity.getEyePosition(1.0F);
               includeFluids &= !entity.isEyeInFluid(FluidTags.WATER) && !entity.isEyeInFluid(FluidTags.LAVA);
               return RayCaster.raycast(level, start, view, includeSelection, includeFluids, includeNonSolid);
            }
         }
      }
   }

   static void renderRaycastOverlay(AxiomWorldRenderContext rc, RayCaster.RaycastResult result) {
      if (result != null) {
         renderRaycastOverlay(rc, result.blockPos());
      }
   }

   static void renderRaycastOverlay(AxiomWorldRenderContext rc, BlockPos pos) {
      if (pos != null) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         VertexConsumerProvider provider = VertexConsumerProvider.shared();
         BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         rc.poseStack().pushPose();
         rc.poseStack().translate(pos.getX() - rc.x(), pos.getY() - rc.y(), pos.getZ() - rc.z());
         BlockState blockState = level.getBlockState(pos);
         VoxelShape voxelShape = blockState.getShape(level, pos, CollisionContext.empty());
         Shapes.blockOutline(bufferBuilder, rc.poseStack().last(), voxelShape, -668862);
         AxiomRenderer.setShaderColour(1.0F, 1.0F, 1.0F, 1.0F);
         AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(bufferBuilder.build());
         rc.poseStack().popPose();
      }
   }

   default UserAction.ActionResult callAction(UserAction action, Object object) {
      return UserAction.ActionResult.NOT_HANDLED;
   }

   default void displayImguiOptions() {
   }

   void reset();

   default void toolDeselected() {
   }

   void render(AxiomWorldRenderContext var1);

   default Vec2 renderAdjustment(float mouseX, float mouseY, Vec2 mouseDelta) {
      return mouseDelta;
   }

   default boolean initiateAdjustment() {
      return false;
   }

   String name();

   default Tutorial getTutorial() {
      return null;
   }

   default String listenForEsc() {
      return null;
   }

   default String listenForEnter() {
      return null;
   }

   default void afterBlockBufferUndo() {
   }

   default void afterBlockBufferRedo() {
   }

   void writeSettings(CompoundTag var1);

   void loadSettings(CompoundTag var1);

   char iconChar();

   default boolean showToolSmoothing() {
      return false;
   }

   String keybindId();

   default int defaultKeybind() {
      return 0;
   }

   default HistoryBuffer<?> historyBuffer() {
      return null;
   }

   EnumSet<AxiomPermission> requiredPermissions();
}
