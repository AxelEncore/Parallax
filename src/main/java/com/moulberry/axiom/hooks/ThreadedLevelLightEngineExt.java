package com.moulberry.axiom.hooks;

import java.util.concurrent.CompletableFuture;

public interface ThreadedLevelLightEngineExt {
   void axiom$checkSectionBlocks(int var1, int var2, int var3, short[] var4);

   CompletableFuture<?> axiom$waitForPendingTasks(int var1, int var2);
}
