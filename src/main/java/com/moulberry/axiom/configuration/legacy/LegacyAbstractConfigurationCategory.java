package com.moulberry.axiom.configuration.legacy;

import com.moulberry.axiom.BuildConfig;
import com.moulberry.axiom.configuration.AxiomConfig;
import com.moulberry.axiom.exceptions.FaultyImplementationError;
import org.spongepowered.configurate.CommentedConfigurationNode;

public abstract class LegacyAbstractConfigurationCategory {
   protected final CommentedConfigurationNode node;

   public LegacyAbstractConfigurationCategory(CommentedConfigurationNode node) {
      this.node = node;
   }

   public <T> boolean has(Class<T> clazz, String key) {
      CommentedConfigurationNode node = (CommentedConfigurationNode)this.node.node(new Object[]{key});
      Object raw = node.raw();
      return !node.virtual() && clazz.isInstance(raw);
   }

   public <T> T load(Class<T> clazz, String key, T defaultValue) {
      CommentedConfigurationNode node = (CommentedConfigurationNode)this.node.node(new Object[]{key});
      Object raw = node.raw();
      if (BuildConfig.DEBUG && Number.class.isAssignableFrom(clazz) && clazz != Number.class) {
         throw new FaultyImplementationError("Use Number instead of " + clazz);
      } else if (!node.virtual() && clazz.isInstance(raw)) {
         return clazz.cast(raw);
      } else {
         if (defaultValue != null) {
            node.raw(defaultValue);
         }

         return defaultValue;
      }
   }

   public abstract void applyToNewConfiguration(AxiomConfig var1);
}
