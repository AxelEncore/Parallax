package com.moulberry.axiom.datagen;

import net.minecraft.tags.TagKey;

public interface AxiomTagAppender<T> {
   void addTag(TagKey<T> var1);

   void add(T var1);
}
