package com.moulberry.axiom.editor.windows;

import com.moulberry.axiom.RayCaster;
import com.moulberry.axiom.editor.EditorWindowType;
import com.moulberry.axiom.i18n.AxiomI18n;
import com.moulberry.axiom.tools.Tool;
import imgui.moulberry92.ImGui;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TargetInfoWindow {
   public static void render() {
      if (EditorWindowType.TARGET_INFO.isOpen()) {
         Level level = Minecraft.getInstance().level;
         if (level != null) {
            Entity entity = Minecraft.getInstance().cameraEntity;
            if (entity != null) {
               if (EditorWindowType.TARGET_INFO.begin("###TargetInfo", true)) {
                  Vec3 view = Tool.getLookDirection();
                  if (view == null) {
                     ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.target_info.no_target"));
                     EditorWindowType.TARGET_INFO.end();
                     return;
                  }

                  Vec3 start = entity.getEyePosition(1.0F);
                  boolean includeFluids = !entity.isEyeInFluid(FluidTags.WATER) && !entity.isEyeInFluid(FluidTags.LAVA);
                  RayCaster.RaycastResult raycastResult = RayCaster.raycast(level, start, view, false, includeFluids, true);
                  if (raycastResult == null) {
                     ImGui.textDisabled(AxiomI18n.get("axiom.editorui.window.target_info.no_target"));
                     EditorWindowType.TARGET_INFO.end();
                     return;
                  }

                  BlockPos pos = raycastResult.blockPos();
                  BlockState targeted = level.getBlockState(pos);
                  double distance = start.distanceTo(raycastResult.getLocation());
                  String blockName = AxiomI18n.get(targeted.getBlock().getDescriptionId());
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.target_info.targeted_block", blockName));
                  if (level.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2) instanceof Reference<Biome> reference) {
                     ResourceKey<Biome> oldBiomeKey = reference.key();
                     ImGui.text(AxiomI18n.get("axiom.editorui.window.target_info.targeted_biome", oldBiomeKey.location().toString()));
                  }

                  ImGui.text(AxiomI18n.get("axiom.editorui.window.target_info.position", pos.getX(), pos.getY(), pos.getZ()));
                  ImGui.text(AxiomI18n.get("axiom.editorui.window.target_info.distance", String.format("%.2f", distance)));
               }

               EditorWindowType.TARGET_INFO.end();
            }
         }
      }
   }
}
