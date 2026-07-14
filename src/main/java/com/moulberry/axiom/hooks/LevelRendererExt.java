package com.moulberry.axiom.hooks;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface LevelRendererExt {
   void axiom$pushTranslucentRenderTarget(RenderTarget var1);

   void axiom$popTranslucentRenderTarget();
}
