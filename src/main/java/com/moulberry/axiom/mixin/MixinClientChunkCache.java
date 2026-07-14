package com.moulberry.axiom.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({ClientChunkCache.class})
public abstract class MixinClientChunkCache {
}
