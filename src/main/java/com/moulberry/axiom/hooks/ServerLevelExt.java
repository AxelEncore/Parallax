package com.moulberry.axiom.hooks;

import com.moulberry.axiom.world_properties.server.ServerWorldPropertiesRegistry;

public interface ServerLevelExt {
   ServerWorldPropertiesRegistry axiom$getWorldProperties();

   void axiom$markChunkDirty(int var1, int var2);

   short[] axiom$getPendingLightUpdates(int var1, int var2, int var3);

   void axiom$relightChunkStarlight(int var1, int var2);

   void axiom$processTasks();
}
