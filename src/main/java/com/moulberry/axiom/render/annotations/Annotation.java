package com.moulberry.axiom.render.annotations;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.moulberry.axiom.annotations.data.AnnotationData;
import com.moulberry.axiom.annotations.data.ImageAnnotationData;
import com.moulberry.axiom.annotations.data.LineAnnotationData;
import com.moulberry.axiom.annotations.data.OutlineAnnotationData;
import com.moulberry.axiom.annotations.data.TextAnnotationData;
import com.moulberry.axiom.gizmo.Gizmo;
import com.moulberry.axiom.render.AxiomWorldRenderContext;
import java.util.UUID;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

public interface Annotation {
   AnnotationData getData();

   SectionPos getMinSectionY();

   SectionPos getMaxSection();

   @Nullable
   Gizmo getGizmo();

   void render(AxiomWorldRenderContext var1, UUID var2, RenderTarget var3);

   void sectionChanged();

   void close();

   default boolean renderPost() {
      return false;
   }

   @Nullable
   static Annotation fromData(AnnotationData annotationData) {
      if (annotationData instanceof LineAnnotationData lineAnnotationData) {
         return new LineAnnotation(lineAnnotationData);
      } else if (annotationData instanceof TextAnnotationData textAnnotationData) {
         return new TextAnnotation(textAnnotationData);
      } else if (annotationData instanceof ImageAnnotationData imageAnnotationData) {
         return new ImageAnnotation(imageAnnotationData);
      } else {
         return annotationData instanceof OutlineAnnotationData outlineAnnotationData ? new OutlineAnnotation(outlineAnnotationData) : null;
      }
   }
}
