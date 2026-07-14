package com.moulberry.axiom.hooks;

import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

public interface BlockItemStatePropertiesExt {
   void axiom$addToTooltip(Consumer<Component> var1, Set<String> var2);
}
