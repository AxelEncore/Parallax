package com.moulberry.axiom.annotations;

import com.moulberry.axiom.annotations.data.AnnotationData;
import java.util.List;
import java.util.UUID;

public record AnnotationHistoryElement(List<AnnotationUpdateAction> undo, List<AnnotationUpdateAction> redo) {
   public AnnotationHistoryElement(AnnotationUpdateAction undo, AnnotationUpdateAction redo) {
      this(List.of(undo), List.of(redo));
   }

   public AnnotationUpdateAction singleUndo() {
      return this.undo.size() == 1 ? this.undo.get(0) : null;
   }

   public AnnotationUpdateAction singleRedo() {
      return this.redo.size() == 1 ? this.redo.get(0) : null;
   }

   public static AnnotationHistoryElement makeDeleteAnnotation(UUID uuid, AnnotationData annotation) {
      return new AnnotationHistoryElement(
         List.of(new AnnotationUpdateAction.CreateAnnotation(uuid, annotation)), List.of(new AnnotationUpdateAction.DeleteAnnotation(uuid))
      );
   }
}
