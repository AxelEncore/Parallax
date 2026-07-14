package com.moulberry.axiom.world_modification;

import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

@FunctionalInterface
public interface EntityDataConsumer {
   void accept(Map<UUID, CompoundTag> var1);
}
