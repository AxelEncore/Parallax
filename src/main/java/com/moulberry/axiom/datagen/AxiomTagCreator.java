package com.moulberry.axiom.datagen;

import net.minecraft.tags.TagKey;

@FunctionalInterface
public interface AxiomTagCreator<T> {
   AxiomTagAppender<T> create(TagKey<T> var1);
}
