package com.moulberry.axiom.core_rendering;

import net.minecraft.client.renderer.RenderType;

public enum AxiomShader {
   BLIT_SCREEN_OLD,
   LINES,
   AXIOM_BLOCK,
   POSITION_COLOR,
   POSITION_TEX_COLOR,
   POSITION_TEX,
   TERRAIN_SOLID,
   TERRAIN_CUTOUT,
   TERRAIN_CUTOUT_MIPPED,
   TERRAIN_TRANSLUCENT,
   TERRAIN_TRIPWIRE,
   POSITION_COLOR_NO_VERTEX_COLOR,
   BIOME_OVERLAY,
   COLLISION_MESH_OVERLAY,
   BRIGHTEN_LIGHT_TEXTURE;

   public void setupRenderState() {
      switch (this) {
         case AXIOM_BLOCK:
         case TERRAIN_TRANSLUCENT:
            RenderType.translucent().setupRenderState();
         case POSITION_COLOR:
         case POSITION_TEX_COLOR:
         case POSITION_TEX:
         default:
            break;
         case TERRAIN_SOLID:
            RenderType.solid().setupRenderState();
            break;
         case TERRAIN_CUTOUT:
            RenderType.cutout().setupRenderState();
            break;
         case TERRAIN_CUTOUT_MIPPED:
            RenderType.cutoutMipped().setupRenderState();
            break;
         case TERRAIN_TRIPWIRE:
            RenderType.tripwire().setupRenderState();
      }
   }

   public void clearRenderState() {
      switch (this) {
         case AXIOM_BLOCK:
         case TERRAIN_TRANSLUCENT:
            RenderType.translucent().clearRenderState();
         case POSITION_COLOR:
         case POSITION_TEX_COLOR:
         case POSITION_TEX:
         default:
            break;
         case TERRAIN_SOLID:
            RenderType.solid().clearRenderState();
            break;
         case TERRAIN_CUTOUT:
            RenderType.cutout().clearRenderState();
            break;
         case TERRAIN_CUTOUT_MIPPED:
            RenderType.cutoutMipped().clearRenderState();
            break;
         case TERRAIN_TRIPWIRE:
            RenderType.tripwire().clearRenderState();
      }
   }
}
