package com.moulberry.axiom.buildertools;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.Dummy;
import com.moulberry.axiom.VersionUtilsClient;
import com.moulberry.axiom.core_rendering.AxiomRenderPipelines;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.EffectRenderer;
import com.moulberry.axiom.render.Shapes;
import com.moulberry.axiom.render.VertexConsumerProvider;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.utils.Box;
import com.moulberry.axiom.utils.IntMatrix;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.utils.RenderHelper;
import com.moulberry.axiom.world_modification.BlockBuffer;
import com.moulberry.axiom.world_modification.HistoryEntry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public interface BuilderTool {
   ResourceLocation MOUSE_LEFT = ResourceLocation.parse("axiom:mouse/left.png");
   ResourceLocation MOUSE_RIGHT = ResourceLocation.parse("axiom:mouse/right.png");
   ResourceLocation MOUSE_SCROLL = ResourceLocation.parse("axiom:mouse/scroll.png");

   void renderScreen(GuiGraphics var1, int var2, int var3, float var4);

   void renderWorld(AxiomWorldRenderContext var1);

   void leftClick(HitResult var1);

   void rightClick(HitResult var1);

   default void middleClick(HitResult hitResult) {
   }

   default boolean setPos1(BlockPos position) {
      return false;
   }

   default boolean setPos2(BlockPos position) {
      return false;
   }

   boolean scroll(int var1);

   boolean shouldRenderBlockOutline(BlockPos var1);

   String getName();

   default List<String> getKeyHints() {
      return List.of();
   }

   default boolean canBeReset() {
      return false;
   }

   void reset(boolean var1);

   default void handleInput(boolean nudgeForwards, boolean nudgeBackwards, boolean delete) {
   }

   default BuilderToolSelectionState.Restore getSelectionRestore() {
      return null;
   }

   default void applySelectionRestore(BuilderToolSelectionState.Restore restore) {
   }

   EnumSet<AxiomPermission> requiredPermissions();

   static boolean shouldEntityByCopied(Entity entity) {
      return entity.isAlive() && !entity.isPassenger() && !(entity instanceof Player);
   }

   static void renderKeybindHelp(GuiGraphics guiGraphics, int screenWidth, int screenHeight, String text, KeyMapping mapping, int index) {
      Minecraft mc = Minecraft.getInstance();
      int y = screenHeight / 2 + (9 + 4) * (1 + index);
      int value = mapping.key.getValue();
      if (mapping.key.getType() == Type.MOUSE && (value == 0 || value == 1 || value == 2)) {
         ResourceLocation mouse;
         if (value == 0) {
            mouse = MOUSE_LEFT;
         } else if (value == 1) {
            mouse = MOUSE_RIGHT;
         } else {
            mouse = MOUSE_SCROLL;
         }

         int width = mc.font.width(text);
         guiGraphics.drawString(mc.font, text, screenWidth / 2 - width / 2, y, -2130706433);
         RenderHelper.tryFlush(guiGraphics);
         VersionUtilsClient.genericBlit(
            guiGraphics, Dummy.GUI_TEXTURED, mouse, screenWidth / 2 - width / 2 - 16, y - 4, 0.0F, 0.0F, 16, 16, 16, 16, -2130706433
         );
         RenderHelper.tryFlush(guiGraphics);
      } else {
         String key = mapping.key.getDisplayName().getString();
         text = "[" + key + "] " + text;
         int width = mc.font.width(text);
         guiGraphics.drawString(mc.font, text, screenWidth / 2 - width / 2, y, -2130706433);
         RenderHelper.tryFlush(guiGraphics);
      }
   }

   static boolean applyLimitBounds(MutableBlockPos one, MutableBlockPos two) {
      Box globalBounds = ClientRestrictions.restrictions.globalBounds;
      if (globalBounds != null) {
         if (Math.max(one.getX(), two.getX()) < globalBounds.pos1().getX()) {
            return false;
         }

         if (Math.max(one.getY(), two.getY()) < globalBounds.pos1().getY()) {
            return false;
         }

         if (Math.max(one.getZ(), two.getZ()) < globalBounds.pos1().getZ()) {
            return false;
         }

         if (Math.min(one.getX(), two.getX()) > globalBounds.pos2().getX()) {
            return false;
         }

         if (Math.min(one.getY(), two.getY()) > globalBounds.pos2().getY()) {
            return false;
         }

         if (Math.min(one.getZ(), two.getZ()) > globalBounds.pos2().getZ()) {
            return false;
         }

         one.setX(Math.min(Math.max(one.getX(), globalBounds.pos1().getX()), globalBounds.pos2().getX()));
         one.setY(Math.min(Math.max(one.getY(), globalBounds.pos1().getY()), globalBounds.pos2().getY()));
         one.setZ(Math.min(Math.max(one.getZ(), globalBounds.pos1().getZ()), globalBounds.pos2().getZ()));
         two.setX(Math.min(Math.max(two.getX(), globalBounds.pos1().getX()), globalBounds.pos2().getX()));
         two.setY(Math.min(Math.max(two.getY(), globalBounds.pos1().getY()), globalBounds.pos2().getY()));
         two.setZ(Math.min(Math.max(two.getZ(), globalBounds.pos1().getZ()), globalBounds.pos2().getZ()));
      }

      return true;
   }

   static void setOffsetFromBlock(BlockHitResult hitResult, MutableBlockPos offset, BlockPos pos1, BlockPos pos2) {
      Axis axis = hitResult.getDirection().getAxis();
      int pos1Axis = pos1.get(axis);
      int pos2Axis = pos2.get(axis);
      int max = Math.max(pos1Axis, pos2Axis);
      int min = Math.min(pos1Axis, pos2Axis);
      int count = (max - min + 1) / 2 + 1;
      if (Math.abs(max - min) % 2 == 1 && hitResult.getDirection().getAxisDirection() == AxisDirection.POSITIVE) {
         count--;
      }

      offset.set(hitResult.getBlockPos().relative(hitResult.getDirection(), count));
   }

   static void delete(BlockPos one, BlockPos two) {
      int minX = Math.min(one.getX(), two.getX());
      int minY = Math.min(one.getY(), two.getY());
      int minZ = Math.min(one.getZ(), two.getZ());
      int maxX = Math.max(one.getX(), two.getX());
      int maxY = Math.max(one.getY(), two.getY());
      int maxZ = Math.max(one.getZ(), two.getZ());
      Level level = Minecraft.getInstance().level;
      BlockBuffer setOperation = new BlockBuffer();
      BlockBuffer previousBlocksForUndo = new BlockBuffer();
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      int changeCount = 0;
      BlockState air = Blocks.AIR.defaultBlockState();
      Long2ObjectMap<List<RegionHelper.NbtTarget>> nbtMap = new Long2ObjectOpenHashMap();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockState oldState = level.getBlockState(mutableBlockPos.set(x, y, z));
               if (!oldState.isAir()) {
                  setOperation.set(x, y, z, air);
                  previousBlocksForUndo.set(x, y, z, oldState);
                  if (oldState.hasBlockEntity()) {
                     long pos = BlockPos.asLong(x, y, z);
                     ((List)nbtMap.computeIfAbsent(pos, k -> new ArrayList())).add(new RegionHelper.NbtTarget(false, x, y, z));
                  }

                  changeCount++;
               }
            }
         }
      }

      BlockPos center = new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
      String countString = NumberFormat.getInstance().format((long)changeCount);
      String historyDescription = AxiomI18n.get("axiom.history_description.deleted", countString);
      RegionHelper.pushBlockBufferChangeWithNBT(
         level, setOperation, previousBlocksForUndo, center, historyDescription, HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME, nbtMap, true
      );
   }

   static void renderScrollHelp(GuiGraphics guiGraphics, int screenWidth, int screenHeight, String text, int index) {
      Minecraft mc = Minecraft.getInstance();
      int y = screenHeight / 2 + (9 + 4) * (1 + index);
      int width = mc.font.width(text);
      guiGraphics.drawString(mc.font, text, screenWidth / 2 - width / 2, y, -2130706433);
      RenderHelper.tryFlush(guiGraphics);
      VersionUtilsClient.genericBlit(
         guiGraphics, Dummy.GUI_TEXTURED, MOUSE_SCROLL, screenWidth / 2 - width / 2 - 16, y - 4, 0.0F, 0.0F, 16, 16, 16, 16, -2130706433
      );
      RenderHelper.tryFlush(guiGraphics);
   }

   static Direction calculateDirection() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) {
         return Direction.UP;
      } else {
         Direction[] directions = Direction.orderedByNearest(player);
         if (Axiom.configuration.builderTools.directionLock) {
            if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 88) != 0) {
               for (Direction direction : directions) {
                  if (direction.getAxis() == Axis.X) {
                     return direction;
                  }
               }
            }

            if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 89) != 0) {
               for (Direction directionx : directions) {
                  if (directionx.getAxis() == Axis.Y) {
                     return directionx;
                  }
               }
            }

            if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), 90) != 0) {
               for (Direction directionxx : directions) {
                  if (directionxx.getAxis() == Axis.Z) {
                     return directionxx;
                  }
               }
            }
         }

         return directions[0];
      }
   }

   static ChunkedBlockRegion handleMoveableBlockBuffer(
      AxiomWorldRenderContext rc, ChunkedBlockRegion blockRegion, BlockPos regionOffset, IntMatrix matrix, boolean rotatePressed, boolean flipPressed
   ) {
      Direction direction = calculateDirection();
      if (rotatePressed) {
         blockRegion = blockRegion.rotate(Axis.Y, -1);
         matrix.rotateY(-1);
      }

      if (flipPressed) {
         blockRegion = blockRegion.flip(direction.getAxis());
         matrix.flip(direction.getAxis());
      }

      BlockPos min = blockRegion.min();
      BlockPos max = blockRegion.max();
      int minX = Math.min(min.getX(), max.getX());
      int minY = Math.min(min.getY(), max.getY());
      int minZ = Math.min(min.getZ(), max.getZ());
      int maxX = Math.max(min.getX(), max.getX());
      int maxY = Math.max(min.getY(), max.getY());
      int maxZ = Math.max(min.getZ(), max.getZ());
      Vec3 blockOffset = Vec3.atLowerCornerOf(regionOffset);
      float partialX = (maxX - minX) % 2 / 2.0F;
      float partialY = (maxY - minY) % 2 / 2.0F;
      float partialZ = (maxZ - minZ) % 2 / 2.0F;
      int shiftX = (int)(-Math.floor(matrix.transformDoubleX(partialX, partialY, partialZ)));
      int shiftY = (int)(-Math.floor(matrix.transformDoubleY(partialX, partialY, partialZ)));
      int shiftZ = (int)(-Math.floor(matrix.transformDoubleZ(partialX, partialY, partialZ)));
      blockOffset = blockOffset.add(shiftX, shiftY, shiftZ);
      minX += shiftX;
      maxX += shiftX;
      minY += shiftY;
      maxY += shiftY;
      minZ += shiftZ;
      maxZ += shiftZ;
      blockRegion.render(rc, blockOffset, 0.75F, 0.0F);
      minX += regionOffset.getX();
      minY += regionOffset.getY();
      minZ += regionOffset.getZ();
      maxX += regionOffset.getX();
      maxY += regionOffset.getY();
      maxZ += regionOffset.getZ();
      renderBoxWithArrow(rc, direction, minX, minY, minZ, maxX, maxY, maxZ);
      return blockRegion;
   }

   static void extend(BlockPos to, MutableBlockPos pos1, MutableBlockPos pos2) {
      if (pos1.getX() < pos2.getX()) {
         if (to.getX() < pos1.getX()) {
            pos1.setX(to.getX());
         }

         if (to.getX() > pos2.getX()) {
            pos2.setX(to.getX());
         }
      } else {
         if (to.getX() < pos2.getX()) {
            pos2.setX(to.getX());
         }

         if (to.getX() > pos1.getX()) {
            pos1.setX(to.getX());
         }
      }

      if (pos1.getY() < pos2.getY()) {
         if (to.getY() < pos1.getY()) {
            pos1.setY(to.getY());
         }

         if (to.getY() > pos2.getY()) {
            pos2.setY(to.getY());
         }
      } else {
         if (to.getY() < pos2.getY()) {
            pos2.setY(to.getY());
         }

         if (to.getY() > pos1.getY()) {
            pos1.setY(to.getY());
         }
      }

      if (pos1.getZ() < pos2.getZ()) {
         if (to.getZ() < pos1.getZ()) {
            pos1.setZ(to.getZ());
         }

         if (to.getZ() > pos2.getZ()) {
            pos2.setZ(to.getZ());
         }
      } else {
         if (to.getZ() < pos2.getZ()) {
            pos2.setZ(to.getZ());
         }

         if (to.getZ() > pos1.getZ()) {
            pos1.setZ(to.getZ());
         }
      }
   }

   static void renderBoxWithArrow(AxiomWorldRenderContext rc, Direction dir, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      Player player = Minecraft.getInstance().player;
      if (player != null) {
         EffectRenderer.renderBoundingBox(rc, new Vec3(minX - 0.01, minY - 0.01, minZ - 0.01), new Vec3(maxX + 1.01, maxY + 1.01, maxZ + 1.01), 3);
         if (dir != null) {
            float mX = 0.0F;
            float mY = 0.0F;
            float mZ = 0.0F;
            switch (dir) {
               case DOWN:
                  mY = -1.0F;
                  break;
               case UP:
                  mY = 1.0F;
                  break;
               case NORTH:
                  mZ = -1.0F;
                  break;
               case SOUTH:
                  mZ = 1.0F;
                  break;
               case WEST:
                  mX = -1.0F;
                  break;
               case EAST:
                  mX = 1.0F;
            }
            int colour = switch (dir) {
               case DOWN, UP -> 48896;
               case NORTH, SOUTH -> 191;
               case WEST, EAST -> 12517376;
               default -> throw new IncompatibleClassChangeError();
            };
            double arrowX = (minX + maxX + 1) / 2.0 + (maxX + 1 - minX) / 2.0 * mX;
            double arrowY = (minY + maxY + 1) / 2.0 + (maxY + 1 - minY) / 2.0 * mY;
            double arrowZ = (minZ + maxZ + 1) / 2.0 + (maxZ + 1 - minZ) / 2.0 * mZ;
            PoseStack matrices = rc.poseStack();
            matrices.pushPose();
            matrices.translate(arrowX - rc.x(), arrowY - rc.y(), arrowZ - rc.z());
            float distanceMultiplier = (float)Math.sqrt(player.distanceToSqr(new Vec3(arrowX, arrowY, arrowZ))) / 20.0F;
            if (distanceMultiplier < 0.5F) {
               distanceMultiplier = 0.5F;
            }

            float axisLen = 4.0F * distanceMultiplier;
            float coneLen = axisLen / 5.0F;
            float coneRadius = coneLen / 3.0F;
            VertexConsumerProvider provider = VertexConsumerProvider.shared();
            Pose pose = matrices.last();
            Matrix4f matrix4f = pose.pose();
            Shapes.drawCone(
               provider,
               matrix4f,
               new Vec3(mX * axisLen, mY * axisLen, mZ * axisLen),
               new Vec3(mX * (axisLen + coneLen), mY * (axisLen + coneLen), mZ * (axisLen + coneLen)),
               mX != 0.0F ? 0 : (mY != 0.0F ? 1 : 2),
               coneRadius,
               1073741824 | colour,
               AxiomRenderPipelines.POSITION_COLOR_IGNORE_DEPTH
            );
            BufferBuilder bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(1073741824 | colour).setNormal(pose, mX, mY, mZ), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(matrix4f, mX * axisLen, mY * axisLen, mZ * axisLen).setColor(1073741824 | colour).setNormal(pose, mX, mY, mZ),
               RenderHelper.baseLineWidth
            );
            AxiomRenderPipelines.LINES_IGNORE_DEPTH.render(provider.build());
            Shapes.drawCone(
               provider,
               matrix4f,
               new Vec3(mX * axisLen, mY * axisLen, mZ * axisLen),
               new Vec3(mX * (axisLen + coneLen), mY * (axisLen + coneLen), mZ * (axisLen + coneLen)),
               mX != 0.0F ? 0 : (mY != 0.0F ? 1 : 2),
               coneRadius,
               -1442840576 | colour,
               AxiomRenderPipelines.POSITION_COLOR_WITHOUT_WRITE_DEPTH
            );
            bufferBuilder = provider.begin(Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(-1442840576 | colour).setNormal(pose, mX, mY, mZ), RenderHelper.baseLineWidth
            );
            VersionUtilsClient.legacySetLineWidthIgnored(
               bufferBuilder.addVertex(matrix4f, mX * axisLen, mY * axisLen, mZ * axisLen).setColor(-1442840576 | colour).setNormal(pose, mX, mY, mZ),
               RenderHelper.baseLineWidth
            );
            AxiomRenderPipelines.LINES_WITHOUT_WRITE_DEPTH.render(provider.build());
            matrices.popPose();
         }
      }
   }
}
