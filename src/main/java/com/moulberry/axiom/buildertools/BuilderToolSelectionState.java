package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.clipboard.SelectionBuffer;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.configuration.BuilderToolMiddleClick;
import com.moulberry.axiom.hooks.ScreenRenderHook;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.EffectRenderer;
import com.moulberry.axiom.render.regions.ChunkedBooleanRegion;
import com.moulberry.axiom.restrictions.ClientRestrictions;
import com.moulberry.axiom.tools.magic_select.MagicSelectionPreciseTask;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BuilderToolSelectionState {
   private MutableBlockPos pos1 = null;
   private MutableBlockPos pos2 = null;
   private ChunkedBooleanRegion magicSelectRegion = null;

   public void renderWorld(AxiomWorldRenderContext rc) {
      if (this.pos1 != null && this.pos2 != null) {
         int minX = Math.min(this.pos1.getX(), this.pos2.getX());
         int minY = Math.min(this.pos1.getY(), this.pos2.getY());
         int minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
         int maxX = Math.max(this.pos1.getX(), this.pos2.getX());
         int maxY = Math.max(this.pos1.getY(), this.pos2.getY());
         int maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());
         BuilderTool.renderBoxWithArrow(rc, BuilderTool.calculateDirection(), minX, minY, minZ, maxX, maxY, maxZ);
      } else if (this.pos1 != null) {
         int x = this.pos1.getX();
         int y = this.pos1.getY();
         int z = this.pos1.getZ();
         EffectRenderer.renderBoundingBox(rc, new Vec3(x, y, z), new Vec3(x + 1, y + 1, z + 1), 3);
      }

      this.renderWorldMagicSelect(rc);
   }

   public void renderWorldMagicSelect(AxiomWorldRenderContext rc) {
      if (this.magicSelectRegion != null) {
         this.magicSelectRegion.render(rc, Vec3.ZERO, 7);
      }
   }

   public void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
      if (this.hasSelection()) {
         if (ClientEvents.builderToolNudgeScrollKeyBind.isDown()) {
            BuilderTool.renderScrollHelp(guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.scroll_nudge"), 0);
         } else {
            BuilderTool.renderScrollHelp(guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.move.scroll"), 0);
            if (Axiom.configuration.builderTools.middleClick == BuilderToolMiddleClick.EXTEND_SELECT) {
               BuilderTool.renderKeybindHelp(
                  guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.scroll_extend"), Minecraft.getInstance().options.keyPickItem, 1
               );
            }
         }
      } else {
         if (this.pos1 == null) {
            BuilderTool.renderKeybindHelp(
               guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.first_point"), Minecraft.getInstance().options.keyAttack, 0
            );
         } else {
            BuilderTool.renderKeybindHelp(
               guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.second_point"), Minecraft.getInstance().options.keyUse, 0
            );
         }

         if (Axiom.configuration.builderTools.middleClick == BuilderToolMiddleClick.MAGIC_SELECT) {
            BuilderTool.renderKeybindHelp(
               guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.scroll_magic"), Minecraft.getInstance().options.keyPickItem, 1
            );
         }
      }
   }

   public SelectionBuffer createSelectionBuffer() {
      SelectionBuffer selection = SelectionBuffer.EMPTY;
      if (this.pos1 != null && this.pos2 != null && BuilderTool.applyLimitBounds(this.pos1, this.pos2)) {
         int minX = Math.min(this.pos1.getX(), this.pos2.getX());
         int minY = Math.min(this.pos1.getY(), this.pos2.getY());
         int minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
         int maxX = Math.max(this.pos1.getX(), this.pos2.getX());
         int maxY = Math.max(this.pos1.getY(), this.pos2.getY());
         int maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());
         selection = selection.addAABB(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), false);
      }

      if (this.magicSelectRegion != null) {
         selection = selection.addSet(this.magicSelectRegion.unsafeGetPositionSet(), false);
      }

      return ClientRestrictions.constrainSelection(selection);
   }

   public void nudge(int amount) {
      Direction direction = BuilderTool.calculateDirection();
      if (this.pos1 != null && this.pos2 != null) {
         Axis axis = direction.getAxis();
         boolean facingNegative = direction.getAxisDirection() == AxisDirection.NEGATIVE;
         if (this.pos1.get(axis) == this.pos2.get(axis) && amount < 0) {
            this.pos1.move(direction, amount);
            this.pos2.move(direction, amount);
         } else if (this.pos1.get(axis) < this.pos2.get(axis) == facingNegative) {
            this.pos1.move(direction, amount);
         } else {
            this.pos2.move(direction, amount);
         }
      } else if (this.pos1 != null) {
         this.pos1.move(direction, amount);
      } else if (this.pos2 != null) {
         this.pos2.move(direction, amount);
      }
   }

   public void showTextInActionBar() {
      if (this.pos1 != null && this.pos2 != null) {
         int sizeX = Math.abs(this.pos1.getX() - this.pos2.getX()) + 1;
         int sizeY = Math.abs(this.pos1.getY() - this.pos2.getY()) + 1;
         int sizeZ = Math.abs(this.pos1.getZ() - this.pos2.getZ()) + 1;
         ScreenRenderHook.setOverlayText(
            Component.literal(AxiomI18n.get("axiom.hardcoded.size_prefix"))
               .withStyle(ChatFormatting.YELLOW)
               .append(Component.literal(sizeX + " ").withStyle(ChatFormatting.RED))
               .append(Component.literal(sizeY + " ").withStyle(ChatFormatting.GREEN))
               .append(Component.literal(sizeZ + "").withStyle(ChatFormatting.AQUA))
         );
      } else if (this.magicSelectRegion != null) {
         String text = "Size: " + this.magicSelectRegion.count() + " blocks";
         ScreenRenderHook.setOverlayText(Component.literal(text).withStyle(ChatFormatting.YELLOW));
      }
   }

   public boolean hasSelection() {
      return this.pos1 != null && this.pos2 != null || this.magicSelectRegion != null;
   }

   public void resetSelection() {
      this.pos1 = null;
      this.pos2 = null;
      if (this.magicSelectRegion != null) {
         this.magicSelectRegion.close();
         this.magicSelectRegion = null;
      }
   }

   public boolean selectionContains(BlockPos blockPos) {
      if (this.magicSelectRegion != null && this.magicSelectRegion.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ())) {
         return true;
      } else if (this.pos1 != null) {
         if (this.pos2 == null) {
            return blockPos.equals(this.pos1);
         } else {
            int minX = Math.min(this.pos1.getX(), this.pos2.getX());
            int minY = Math.min(this.pos1.getY(), this.pos2.getY());
            int minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
            int maxX = Math.max(this.pos1.getX(), this.pos2.getX());
            int maxY = Math.max(this.pos1.getY(), this.pos2.getY());
            int maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());
            int x = blockPos.getX();
            int y = blockPos.getY();
            int z = blockPos.getZ();
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
         }
      } else {
         return this.pos2 != null ? blockPos.equals(this.pos2) : false;
      }
   }

   public void setPos1(BlockPos blockPos) {
      this.pos1 = blockPos.mutable();
   }

   public void setPos2(BlockPos blockPos) {
      this.pos2 = blockPos.mutable();
   }

   public void leftClick(BlockHitResult blockHitResult) {
      this.pos1 = blockHitResult.getBlockPos().mutable();
   }

   public void rightClick(BlockHitResult blockHitResult) {
      if (this.pos1 != null) {
         this.pos2 = blockHitResult.getBlockPos().mutable();
      }
   }

   public void middleClick(BlockHitResult blockHitResult) {
      if (Axiom.configuration.builderTools.middleClick == BuilderToolMiddleClick.MAGIC_SELECT) {
         BlockPos hit = blockHitResult.getBlockPos();
         MagicSelectionPreciseTask task = new MagicSelectionPreciseTask(new PositionSet(), Minecraft.getInstance().level, hit, 0);
         task.fill(Axiom.configuration.builderTools.magicSelectCount);
         if (this.magicSelectRegion == null) {
            this.magicSelectRegion = new ChunkedBooleanRegion(task.positionSet);
         } else {
            this.magicSelectRegion.addAll(task.positionSet);
         }
      } else if (this.pos1 == null) {
         this.pos1 = blockHitResult.getBlockPos().mutable();
      } else if (this.pos2 == null) {
         this.pos2 = blockHitResult.getBlockPos().mutable();
      } else {
         BuilderTool.extend(blockHitResult.getBlockPos(), this.pos1, this.pos2);
      }
   }

   @Nullable
   public BuilderToolSelectionState.Restore getSelectionRestore() {
      return this.pos1 == null && this.pos2 == null && this.magicSelectRegion == null
         ? null
         : new BuilderToolSelectionState.Restore(
            this.pos1 == null ? null : this.pos1.immutable(),
            this.pos2 == null ? null : this.pos2.immutable(),
            this.magicSelectRegion == null ? null : this.magicSelectRegion.copyPositionSet()
         );
   }

   public void restoreFrom(BuilderToolSelectionState.Restore restore) {
      if (restore != null) {
         this.pos1 = restore.pos1 == null ? null : restore.pos1.mutable();
         this.pos2 = restore.pos2 == null ? null : restore.pos2.mutable();
         if (this.magicSelectRegion != null) {
            this.magicSelectRegion.close();
            this.magicSelectRegion = null;
         }

         if (restore.set != null) {
            this.magicSelectRegion = new ChunkedBooleanRegion(restore.set);
         }
      }
   }

   public record Restore(BlockPos pos1, BlockPos pos2, PositionSet set) {
   }
}
