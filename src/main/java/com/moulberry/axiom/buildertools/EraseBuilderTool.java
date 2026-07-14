package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.collections.PositionSet;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.EffectRenderer;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.magic_select.MagicSelectionPreciseTask;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryEntry;
import java.text.NumberFormat;
import java.util.EnumSet;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class EraseBuilderTool implements BuilderTool {
   private MutableBlockPos pos1 = null;
   private MutableBlockPos pos2 = null;

   @Override
   public void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      if (this.pos1 == null) {
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.erase.erase_box"), Minecraft.getInstance().options.keyAttack, 0
         );
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.erase.erase_connected"), Minecraft.getInstance().options.keyUse, 1
         );
      } else if (this.pos2 == null) {
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.second_point"), Minecraft.getInstance().options.keyUse, 0
         );
      } else {
         Minecraft mc = Minecraft.getInstance();
         int y = screenHeight / 2 + 9 + 4;
         String text = "[" + Keybinds.BUILDER_TOOL_DELETE.longKeyIdentifier().toUpperCase(Locale.ROOT) + "] " + AxiomI18n.get("axiom.buildertool.erase.delete");
         int width = mc.font.width(text);
         guiGraphics.drawString(mc.font, text, screenWidth / 2 - width / 2, y, -2130706433);
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.scroll_extend"), Minecraft.getInstance().options.keyPickItem, 1
         );
      }
   }

   @Override
   public void renderWorld(AxiomWorldRenderContext rc) {
      if (this.pos1 != null && this.pos2 != null) {
         int minX = Math.min(this.pos1.getX(), this.pos2.getX());
         int minY = Math.min(this.pos1.getY(), this.pos2.getY());
         int minZ = Math.min(this.pos1.getZ(), this.pos2.getZ());
         int maxX = Math.max(this.pos1.getX(), this.pos2.getX());
         int maxY = Math.max(this.pos1.getY(), this.pos2.getY());
         int maxZ = Math.max(this.pos1.getZ(), this.pos2.getZ());
         BuilderTool.renderBoxWithArrow(rc, null, minX, minY, minZ, maxX, maxY, maxZ);
         Level level = Minecraft.getInstance().level;
         if (level == null) {
            return;
         }

         if (Minecraft.getInstance().screen == null && Keybinds.BUILDER_TOOL_DELETE.isDown()) {
            BuilderTool.delete(this.pos1, this.pos2);
            this.pos1 = null;
            this.pos2 = null;
         }
      } else if (this.pos1 != null) {
         int x = this.pos1.getX();
         int y = this.pos1.getY();
         int z = this.pos1.getZ();
         EffectRenderer.renderBoundingBox(rc, new Vec3(x, y, z), new Vec3(x + 1, y + 1, z + 1), 3);
      }
   }

   @Override
   public boolean setPos1(BlockPos position) {
      this.pos1 = position.mutable();
      return true;
   }

   @Override
   public boolean setPos2(BlockPos position) {
      this.pos2 = position.mutable();
      return true;
   }

   @Override
   public void leftClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK) {
         this.pos1 = ((BlockHitResult)hitResult).getBlockPos().mutable();
      }
   }

   @Override
   public void rightClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         if (this.pos1 != null) {
            this.pos2 = ((BlockHitResult)hitResult).getBlockPos().mutable();
         } else {
            BlockPos hit = blockHitResult.getBlockPos();
            MagicSelectionPreciseTask task = new MagicSelectionPreciseTask(new PositionSet(), Minecraft.getInstance().level, hit, 0);
            task.fill(Axiom.configuration.builderTools.magicSelectCount);
            String countString = NumberFormat.getInstance().format((long)task.positionSet.count());
            String historyDescription = AxiomI18n.get("axiom.history_description.deleted", countString);
            RegionHelper.pushPositionSetRegionChangeWithNBT(
               task.positionSet, Blocks.AIR.defaultBlockState(), hit, historyDescription, HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME, null
            );
         }
      }
   }

   @Override
   public void middleClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         if (this.pos1 == null) {
            this.pos1 = blockHitResult.getBlockPos().mutable();
         } else if (this.pos2 == null) {
            this.pos2 = blockHitResult.getBlockPos().mutable();
         } else {
            BlockPos pos = blockHitResult.getBlockPos();
            BuilderTool.extend(pos, this.pos1, this.pos2);
         }
      }
   }

   @Override
   public boolean scroll(int scroll) {
      return false;
   }

   @Override
   public boolean shouldRenderBlockOutline(BlockPos blockPos) {
      return true;
   }

   @Override
   public String getName() {
      return AxiomI18n.get("axiom.buildertool.erase");
   }

   @Override
   public boolean canBeReset() {
      return this.pos1 != null || this.pos2 != null;
   }

   @Override
   public void reset(boolean apply) {
      this.pos1 = null;
      this.pos2 = null;
   }

   @Override
   public BuilderToolSelectionState.Restore getSelectionRestore() {
      return new BuilderToolSelectionState.Restore(this.pos1 == null ? null : this.pos1.immutable(), this.pos2 == null ? null : this.pos2.immutable(), null);
   }

   @Override
   public void applySelectionRestore(BuilderToolSelectionState.Restore restore) {
      if (restore.pos1() != null) {
         this.pos1 = restore.pos1().mutable();
      }

      if (restore.pos2() != null) {
         this.pos2 = restore.pos2().mutable();
      }
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.BUILDERTOOL_ERASE, AxiomPermission.BUILD_SECTION);
   }
}
