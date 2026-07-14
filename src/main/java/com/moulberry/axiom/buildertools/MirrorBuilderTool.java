package com.moulberry.axiom.buildertools;

import com.moulberry.axiom.capabilities.BuildSymmetry;
import com.moulberry.axiom.editor.keybinds.Keybinds;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.utils.InputHelper;
import java.util.EnumSet;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class MirrorBuilderTool implements BuilderTool {
   @Override
   public void renderScreen(GuiGraphics guiGraphics, int screenWidth, int screenHeight, float partialTick) {
      if (!BuildSymmetry.isActive()) {
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.setup_symmetry.place_symmetry"), Minecraft.getInstance().options.keyUse, 0
         );
      } else {
         BuilderTool.renderKeybindHelp(
            guiGraphics, screenWidth, screenHeight, AxiomI18n.get("axiom.buildertool.setup_symmetry.move_symmetry"), Minecraft.getInstance().options.keyUse, 0
         );
         if (InputHelper.isCtrlOrCmdDownRaw()) {
            BuilderTool.renderKeybindHelp(
               guiGraphics,
               screenWidth,
               screenHeight,
               AxiomI18n.get("axiom.buildertool.setup_symmetry.nudge_symmetry"),
               Minecraft.getInstance().options.keyPickItem,
               1
            );
         } else if (Minecraft.ON_OSX) {
            BuilderTool.renderKeybindHelp(
               guiGraphics,
               screenWidth,
               screenHeight,
               AxiomI18n.get("axiom.buildertool.setup_symmetry.cmd_nudge_symmetry"),
               Minecraft.getInstance().options.keyPickItem,
               1
            );
         } else {
            BuilderTool.renderKeybindHelp(
               guiGraphics,
               screenWidth,
               screenHeight,
               AxiomI18n.get("axiom.buildertool.setup_symmetry.ctrl_nudge_symmetry"),
               Minecraft.getInstance().options.keyPickItem,
               1
            );
         }

         Minecraft mc = Minecraft.getInstance();
         int y = screenHeight / 2 + (9 + 4) * 3;
         String text = "[DEL] " + AxiomI18n.get("axiom.widget.clear");
         int width = mc.font.width(text);
         guiGraphics.drawString(mc.font, text, screenWidth / 2 - width / 2, y, -2130706433);
         y = screenHeight / 2 + (9 + 4) * 4;
         String flipAxis = this.getLookAxis().getSerializedName().toUpperCase(Locale.ROOT);
         text = "[" + Keybinds.FLIP_PLACEMENT.shortKeyIdentifier() + "] " + AxiomI18n.get("axiom.buildertool.setup_symmetry.flip", flipAxis);
         width = mc.font.width(text);
         guiGraphics.drawString(mc.font, text, screenWidth / 2 - width / 2, y, -2130706433);
         y = screenHeight / 2 + (9 + 4) * 5;
         text = "[" + Keybinds.ROTATE_PLACEMENT.shortKeyIdentifier() + "] " + AxiomI18n.get("axiom.buildertool.setup_symmetry.rotate", "Y");
         width = mc.font.width(text);
         guiGraphics.drawString(mc.font, text, screenWidth / 2 - width / 2, y, -2130706433);
      }
   }

   @Override
   public void renderWorld(AxiomWorldRenderContext rc) {
   }

   private Axis getLookAxis() {
      Direction direction = BuilderTool.calculateDirection();
      return direction.getAxis();
   }

   @Override
   public void leftClick(HitResult hitResult) {
      this.rightClick(hitResult);
   }

   @Override
   public void rightClick(HitResult hitResult) {
      if (hitResult.getType() == Type.BLOCK) {
         BuildSymmetry.setSymmetryPoint(hitResult.getLocation());
      }
   }

   @Override
   public void middleClick(HitResult hitResult) {
   }

   @Override
   public boolean scroll(int scroll) {
      if (BuildSymmetry.isActive() && InputHelper.isCtrlOrCmdDownRaw()) {
         Direction direction = BuilderTool.calculateDirection();
         Vec3 symmetry = BuildSymmetry.getSymmetryPoint();
         if (symmetry == null) {
            return false;
         } else {
            symmetry = symmetry.add(
               direction.getStepX() * Math.signum((float)scroll) * 0.5F,
               direction.getStepY() * Math.signum((float)scroll) * 0.5F,
               direction.getStepZ() * Math.signum((float)scroll) * 0.5F
            );
            BuildSymmetry.setSymmetryPoint(symmetry);
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean shouldRenderBlockOutline(BlockPos blockPos) {
      return true;
   }

   @Override
   public String getName() {
      return AxiomI18n.get("axiom.buildertool.setup_symmetry");
   }

   @Override
   public void reset(boolean apply) {
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      return EnumSet.of(AxiomPermission.BUILDERTOOL_SETUPSYMMETRY, AxiomPermission.BUILD_PLACE);
   }
}
