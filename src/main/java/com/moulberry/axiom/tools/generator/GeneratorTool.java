package com.moulberry.axiom.tools.generator;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.UserAction;
import com.moulberry.axiom.editor.ImGuiHelper;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import com.moulberry.axiom.render.regions.ChunkedBlockRegion;
import com.moulberry.axiom.restrictions.AxiomPermission;
import com.moulberry.axiom.tools.Tool;
import com.moulberry.axiom.utils.RegionHelper;
import java.text.NumberFormat;
import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class GeneratorTool implements Tool {
   private int[] generationType = new int[]{0};

   @Override
   public void reset() {
   }

   @Override
   public UserAction.ActionResult callAction(UserAction action, Object object) {
      switch (action) {
         case RIGHT_MOUSE:
            this.rightClick();
            return UserAction.ActionResult.USED_STOP;
         default:
            return UserAction.ActionResult.NOT_HANDLED;
      }
   }

   public void rightClick() {
      RayCaster.RaycastResult result = Tool.raycastBlock();
      if (result != null) {
         BlockPos start = result.blockPos();
         int type = this.generationType[0];
         ChunkedBlockRegion chunkedBlockRegion;
         if (type == 0) {
            chunkedBlockRegion = TreeGeneration.generateAsh(Minecraft.getInstance().level, start.relative(result.direction()));
         } else {
            if (type != 1) {
               return;
            }

            chunkedBlockRegion = TreeGeneration.generateCyprus(Minecraft.getInstance().level, start.relative(result.direction()));
         }

         int count = chunkedBlockRegion.count();
         if (count != 0) {
            String countString = NumberFormat.getInstance().format((long)count);
            String historyDescription = AxiomI18n.get("axiom.history_description.floodfilled", countString);
            RegionHelper.pushBlockRegionChange(chunkedBlockRegion, historyDescription);
         }
      }
   }

   @Override
   public void render(AxiomWorldRenderContext rc) {
   }

   @Override
   public void displayImguiOptions() {
      ImGuiHelper.combo("Type", this.generationType, new String[]{"Ash", "Cyprus"});
   }

   @Override
   public String name() {
      return "Generator";
   }

   @Override
   public void writeSettings(CompoundTag tag) {
   }

   @Override
   public void loadSettings(CompoundTag tag) {
   }

   @Override
   public char iconChar() {
      return '\ue919';
   }

   @Override
   public String keybindId() {
      return "generator";
   }

   @Override
   public EnumSet<AxiomPermission> requiredPermissions() {
      throw new UnsupportedOperationException();
   }
}
