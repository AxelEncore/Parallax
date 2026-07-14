package com.moulberry.axiom.core_rendering;

public class AxiomRenderPipelines {
   public static AxiomRenderPipeline BLIT = new AxiomRenderPipeline("blit", AxiomShader.BLIT_SCREEN_OLD);
   public static AxiomRenderPipeline BLIT_NO_BLEND = new AxiomRenderPipeline("blit_no_blend", AxiomShader.BLIT_SCREEN_OLD);
   public static AxiomRenderPipeline BLIT_ONE_ONE_MINUS_SRC_ALPHA = new AxiomRenderPipeline("blit_one_one_minus_src_alpha", AxiomShader.BLIT_SCREEN_OLD);
   public static AxiomRenderPipeline LINES_IGNORE_DEPTH = new AxiomRenderPipeline("lines_ignore_depth", AxiomShader.LINES);
   public static AxiomRenderPipeline LINES_WITHOUT_WRITE_DEPTH = new AxiomRenderPipeline("lines_without_write_depth", AxiomShader.LINES);
   public static AxiomRenderPipeline LINES_WITHOUT_WRITE_DEPTH_WITH_OFFSET = new AxiomRenderPipeline("lines_without_write_depth_with_offset", AxiomShader.LINES);
   public static AxiomRenderPipeline LINES_IGNORE_DEPTH_WITH_CUSTOM_WIDTH = new AxiomRenderPipeline("lines_ignore_depth_with_custom_width", AxiomShader.LINES);
   public static AxiomRenderPipeline LINES_WITH_CUSTOM_WIDTH = new AxiomRenderPipeline("lines_with_custom_width", AxiomShader.LINES);
   public static AxiomRenderPipeline POSITION_COLOR = new AxiomRenderPipeline("position_color", AxiomShader.POSITION_COLOR);
   public static AxiomRenderPipeline POSITION_TEX = new AxiomRenderPipeline("position_tex", AxiomShader.POSITION_TEX);
   public static AxiomRenderPipeline POSITION_COLOR_POLYGON_OFFSET = new AxiomRenderPipeline("position_color_polygon_offset", AxiomShader.POSITION_COLOR);
   public static AxiomRenderPipeline POSITION_TEX_COLOR = new AxiomRenderPipeline("position_tex_color", AxiomShader.POSITION_TEX_COLOR);
   public static AxiomRenderPipeline AXIOM_BLOCK = new AxiomRenderPipeline("axiom_block", AxiomShader.AXIOM_BLOCK);
   public static AxiomRenderPipeline AXIOM_BLOCK_WITH_OFFSET = new AxiomRenderPipeline("axiom_block_with_offset", AxiomShader.AXIOM_BLOCK);
   public static AxiomRenderPipeline AXIOM_BLOCK_NO_MIPMAP = new AxiomRenderPipeline("axiom_block_no_mipmap", AxiomShader.AXIOM_BLOCK);
   public static AxiomRenderPipeline POSITION_COLOR_IGNORE_DEPTH = new AxiomRenderPipeline("position_color_ignore_depth", AxiomShader.POSITION_COLOR);
   public static AxiomRenderPipeline POSITION_COLOR_WITHOUT_WRITE_DEPTH = new AxiomRenderPipeline(
      "position_color_without_write_depth", AxiomShader.POSITION_COLOR
   );
   public static AxiomRenderPipeline IMAGE_ANNOTATION_PIPELINE = new AxiomRenderPipeline("image_annotation_pipeline", AxiomShader.POSITION_TEX);
   public static AxiomRenderPipeline LINE_ANNOTATION_PIPELINE = new AxiomRenderPipeline("line_annotation_pipeline", AxiomShader.LINES);
   public static AxiomRenderPipeline GIZMO_LINES = new AxiomRenderPipeline("gizmo_lines", AxiomShader.LINES);
   public static AxiomRenderPipeline GIZMO_POSITION_COLOR = new AxiomRenderPipeline("gizmo_position_color", AxiomShader.POSITION_COLOR);
   public static AxiomRenderPipeline GIZMO_POSITION_COLOR_WITH_BLENDING = new AxiomRenderPipeline(
      "gizmo_position_color_with_blending", AxiomShader.POSITION_COLOR
   );
   public static AxiomRenderPipeline BIOME_OVERLAY_PIPELINE = new AxiomRenderPipeline("biome_overlay_pipeline", AxiomShader.BIOME_OVERLAY);
   public static AxiomRenderPipeline COLLISION_MESH_OVERLAY_PIPELINE = new AxiomRenderPipeline(
      "collision_mesh_overlay_pipeline", AxiomShader.COLLISION_MESH_OVERLAY
   );
   public static AxiomRenderPipeline BRIGHTEN_LIGHT_TEXTURE_PIPELINE = new AxiomRenderPipeline(
      "brighten_light_texture_pipeline", AxiomShader.BRIGHTEN_LIGHT_TEXTURE
   );
   public static AxiomRenderPipeline EFFECT_RENDERER_NORMAL_PIPELINE = new AxiomRenderPipeline("effect_renderer_depth_pipeline", AxiomShader.POSITION_COLOR);
   public static AxiomRenderPipeline EFFECT_RENDERER_RED_INVERSE_DEPTH = new AxiomRenderPipeline(
      "effect_renderer_red_inverse_depth", AxiomShader.POSITION_COLOR
   );
   public static AxiomRenderPipeline EFFECT_RENDERER_BLUE_BACK = new AxiomRenderPipeline("effect_renderer_blue_back", AxiomShader.POSITION_COLOR);
   public static AxiomRenderPipeline EFFECT_RENDERER_OUTLINE = new AxiomRenderPipeline("effect_renderer_outline", AxiomShader.POSITION_COLOR_NO_VERTEX_COLOR);
   public static AxiomRenderPipeline OUTLINE_WITH_DEPTH = new AxiomRenderPipeline("outline_with_depth", AxiomShader.POSITION_COLOR_NO_VERTEX_COLOR);
   public static AxiomRenderPipeline TERRAIN_SOLID_PIPELINE = new AxiomRenderPipeline("terrain_solid_pipeline", AxiomShader.TERRAIN_SOLID);
   public static AxiomRenderPipeline TERRAIN_CUTOUT_MIPPED_PIPELINE = new AxiomRenderPipeline(
      "terrain_cutout_mipped_pipeline", AxiomShader.TERRAIN_CUTOUT_MIPPED
   );
   public static AxiomRenderPipeline TERRAIN_CUTOUT_PIPELINE = new AxiomRenderPipeline("terrain_cutout_pipeline", AxiomShader.TERRAIN_CUTOUT);
   public static AxiomRenderPipeline TERRAIN_TRANSLUCENT_PIPELINE = new AxiomRenderPipeline("terrain_translucent_pipeline", AxiomShader.TERRAIN_TRANSLUCENT);
   public static AxiomRenderPipeline TERRAIN_TRIPWIRE_PIPELINE = new AxiomRenderPipeline("terrain_tripwire_pipeline", AxiomShader.TERRAIN_TRIPWIRE);

   static {
      BLIT.depthTest = false;
      BLIT.cull = false;
      BLIT.writeAlpha = false;
      BLIT.mipmapTexture = false;
      BLIT_NO_BLEND.depthTest = false;
      BLIT_NO_BLEND.cull = false;
      BLIT_NO_BLEND.blend = false;
      BLIT_NO_BLEND.writeAlpha = false;
      BLIT_NO_BLEND.mipmapTexture = false;
      BLIT_ONE_ONE_MINUS_SRC_ALPHA.depthTest = false;
      BLIT_ONE_ONE_MINUS_SRC_ALPHA.cull = false;
      BLIT_ONE_ONE_MINUS_SRC_ALPHA.blendFunction = AxiomBlending.ONE_ONE_MINUS_SRC_ALPHA;
      BLIT_NO_BLEND.writeAlpha = false;
      BLIT_ONE_ONE_MINUS_SRC_ALPHA.mipmapTexture = false;
      LINES_IGNORE_DEPTH.depthTest = false;
      LINES_IGNORE_DEPTH.cull = false;
      LINES_IGNORE_DEPTH.lineWidthMultiplier = 1.0F;
      LINES_WITHOUT_WRITE_DEPTH.lineWidthMultiplier = 1.0F;
      LINES_WITHOUT_WRITE_DEPTH.cull = false;
      LINES_WITHOUT_WRITE_DEPTH.depthWrite = false;
      LINES_WITHOUT_WRITE_DEPTH_WITH_OFFSET.lineWidthMultiplier = 1.0F;
      LINES_WITHOUT_WRITE_DEPTH_WITH_OFFSET.cull = false;
      LINES_WITHOUT_WRITE_DEPTH_WITH_OFFSET.depthWrite = false;
      LINES_WITHOUT_WRITE_DEPTH_WITH_OFFSET.polygonOffset = -2.0F;
      LINES_IGNORE_DEPTH_WITH_CUSTOM_WIDTH.depthTest = false;
      LINES_IGNORE_DEPTH_WITH_CUSTOM_WIDTH.cull = false;
      LINES_WITH_CUSTOM_WIDTH.depthWrite = false;
      LINES_WITH_CUSTOM_WIDTH.cull = false;
      POSITION_COLOR_POLYGON_OFFSET.polygonOffset = -1.0F;
      AXIOM_BLOCK_WITH_OFFSET.polygonOffset = -2.0F;
      AXIOM_BLOCK_NO_MIPMAP.mipmapTexture = false;
      POSITION_COLOR_IGNORE_DEPTH.depthTest = false;
      POSITION_COLOR_WITHOUT_WRITE_DEPTH.depthWrite = false;
      IMAGE_ANNOTATION_PIPELINE.cull = false;
      IMAGE_ANNOTATION_PIPELINE.polygonOffset = -1.0F;
      IMAGE_ANNOTATION_PIPELINE.disableFog = true;
      LINE_ANNOTATION_PIPELINE.cull = false;
      LINE_ANNOTATION_PIPELINE.polygonOffset = -1.0F;
      LINE_ANNOTATION_PIPELINE.disableFog = true;
      GIZMO_LINES.blendFunction = AxiomBlending.ONE_ZERO;
      GIZMO_LINES.depthFunc = 519;
      GIZMO_LINES.cull = false;
      GIZMO_LINES.disableFog = true;
      GIZMO_POSITION_COLOR.blendFunction = AxiomBlending.ONE_ZERO;
      GIZMO_POSITION_COLOR.depthFunc = 519;
      GIZMO_POSITION_COLOR.cull = false;
      GIZMO_POSITION_COLOR.disableFog = true;
      GIZMO_POSITION_COLOR_WITH_BLENDING.depthFunc = 519;
      GIZMO_POSITION_COLOR_WITH_BLENDING.disableFog = true;
      BIOME_OVERLAY_PIPELINE.blendFunction = AxiomBlending.ONE_ZERO;
      COLLISION_MESH_OVERLAY_PIPELINE.blendFunction = AxiomBlending.ONE_ZERO;
      BRIGHTEN_LIGHT_TEXTURE_PIPELINE.depthTest = false;
      BRIGHTEN_LIGHT_TEXTURE_PIPELINE.cull = false;
      EFFECT_RENDERER_NORMAL_PIPELINE.polygonOffset = -4.0F;
      EFFECT_RENDERER_RED_INVERSE_DEPTH.polygonOffset = -4.0F;
      EFFECT_RENDERER_RED_INVERSE_DEPTH.depthWrite = false;
      EFFECT_RENDERER_RED_INVERSE_DEPTH.cullFace = 1028;
      EFFECT_RENDERER_RED_INVERSE_DEPTH.depthFunc = 516;
      EFFECT_RENDERER_BLUE_BACK.polygonOffset = -4.0F;
      EFFECT_RENDERER_BLUE_BACK.cullFace = 1028;
      EFFECT_RENDERER_OUTLINE.polygonOffset = -4.0F;
      EFFECT_RENDERER_OUTLINE.depthTest = false;
      OUTLINE_WITH_DEPTH.polygonOffset = -4.0F;
      TERRAIN_SOLID_PIPELINE.blend = false;
      TERRAIN_SOLID_PIPELINE.useLightLayer = true;
      TERRAIN_CUTOUT_MIPPED_PIPELINE.alphaCutout = 0.5F;
      TERRAIN_CUTOUT_MIPPED_PIPELINE.blend = false;
      TERRAIN_CUTOUT_MIPPED_PIPELINE.useLightLayer = true;
      TERRAIN_CUTOUT_PIPELINE.alphaCutout = 0.1F;
      TERRAIN_CUTOUT_PIPELINE.blend = false;
      TERRAIN_CUTOUT_PIPELINE.useLightLayer = true;
      TERRAIN_TRANSLUCENT_PIPELINE.useLightLayer = true;
      TERRAIN_TRANSLUCENT_PIPELINE.alphaCutout = 0.01F;
      TERRAIN_TRIPWIRE_PIPELINE.alphaCutout = 0.1F;
      TERRAIN_TRIPWIRE_PIPELINE.useLightLayer = true;
   }
}
