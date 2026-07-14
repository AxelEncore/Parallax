package com.moulberry.axiom.annotations.data;

import com.moulberry.axiomclientapi.funcinterfaces.TriIntConsumer;

public interface OutlineAnnotationData extends AnnotationData {
   int getColour();

   void iteratePositions(TriIntConsumer var1);
}
