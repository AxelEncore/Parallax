package com.moulberry.axiom.utils;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;

/**
 * Optional Starlight integration, accessed reflectively (no compile-time dependency on Starlight).
 * Only invoked when Starlight/ScalableLux is present, in which case the light engine implements
 * {@code StarLightLightingProvider} and exposes {@code getLightEngine().relightChunks(...)}.
 */
public class StarlightHelper {

    public static void relightChunks(ThreadedLevelLightEngine engine, Set<ChunkPos> chunks,
                                     Consumer<ChunkPos> chunkLightCallback, IntConsumer onComplete) {
        try {
            Object lightEngine = engine.getClass().getMethod("getLightEngine").invoke(engine);
            lightEngine.getClass()
                    .getMethod("relightChunks", Set.class, Consumer.class, IntConsumer.class)
                    .invoke(lightEngine, chunks, chunkLightCallback, onComplete);
        } catch (Throwable ignored) {
        }
    }
}
