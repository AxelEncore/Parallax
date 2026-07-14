package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.extrude.ExtrudeExpandTask;
import com.moulberry.axiom.tools.extrude.ExtrudeShrinkTask;
import com.moulberry.axiom.utils.RegionHelper;
import com.moulberry.axiom.world_modification.HistoryEntry;
import java.text.NumberFormat;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class ExtrudeBuilderTool implements BuilderTool {
   @Override
   public void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      BuilderTool.renderKeybindHelp(
         guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.tool.extrude.mode.expand"), Minecraft.getInstance().options.keyUse, 0
      );
      BuilderTool.renderKeybindHelp(
         guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.tool.extrude.mode.shrink"), Minecraft.getInstance().options.keyAttack, 1
      );
   }

   @Override
   public void renderWorld(AxiomWorldRenderContext rc) {
   }

   @Override
   public void leftClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         ExtrudeShrinkTask task = new ExtrudeShrinkTask(
            Minecraft.getInstance().level, blockHitResult.getBlockPos(), blockHitResult.getDirection(), 0, true, false, 1, false
         );
         task.perform(1000000);
         String description = AxiomI18n.get("axiom.history_description.shrunk", NumberFormat.getInstance().format((long)task.chunkedBlockRegion.count()));
         RegionHelper.pushBlockRegionChangeWithNBT(task.chunkedBlockRegion, description, HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME, null);
      }
   }

   @Override
   public void rightClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK && hitResult instanceof BlockHitResult blockHitResult) {
         ChunkedBlockRegion chunkedBlockRegion = new ChunkedBlockRegion();
         ExtrudeExpandTask task = new ExtrudeExpandTask(
            Minecraft.getInstance().level, blockHitResult.getBlockPos(), blockHitResult.getDirection(), 0, false, 1, false
         );
         task.perform(1000000);
         task.expandBlocks.forEachEntry(chunkedBlockRegion::addBlock);
         int xo = -blockHitResult.getDirection().getStepX();
         int yo = -blockHitResult.getDirection().getStepY();
         int zo = -blockHitResult.getDirection().getStepZ();
         int flags = HistoryEntry.MODIFIER_CAN_BE_UNDONE_INGAME;
         if (BuilderToolManager.keepExisting) {
            flags |= HistoryEntry.MODIFIER_KEEP_EXISTING;
         }

         String description = AxiomI18n.get("axiom.history_description.expanded", Integer.toString(chunkedBlockRegion.count()));
         RegionHelper.pushBlockRegionChangeWithNBT(chunkedBlockRegion, description, flags, pos -> {
            int x = BlockPos.getX(pos);
            int y = BlockPos.getY(pos);
            int z = BlockPos.getZ(pos);
            return BlockPos.asLong(x + xo, y + yo, z + zo);
         });
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
      return AxiomI18n.get("axiom.tool.extrude");
   }

   @Override
   public void reset(boolean apply) {
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.BUILDERTOOL_EXTRUDE, AxiomPermission.BUILD_SECTION);
   }
}
