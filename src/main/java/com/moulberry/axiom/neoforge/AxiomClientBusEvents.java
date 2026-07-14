package com.moulberry.axiom.neoforge;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.ClientEvents;
import com.moulberry.axiom.render.ShaderManager;
import java.io.IOException;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/**
 * Client-only mod-bus event handlers. Auto-registered by NeoForge on the physical client only.
 */
@EventBusSubscriber(modid = AxiomNeoForge.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AxiomClientBusEvents {

    private AxiomClientBusEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping keyMapping : ClientEvents.getAllKeybinds()) {
            event.register(keyMapping);
        }
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        Axiom.registerClientReloadListeners(event);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) {
        try {
            ShaderManager.INSTANCE.register(event);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
