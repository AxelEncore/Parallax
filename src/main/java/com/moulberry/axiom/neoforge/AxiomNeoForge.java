package com.moulberry.axiom.neoforge;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.network.AxiomNetworking;
import com.moulberry.axiom.utils.Authorization;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * NeoForge entrypoint for the Axiom port.
 *
 * <p>Phase A bootstrap: this class establishes the mod container and the mod event bus that later
 * phases hook into (registrations, networking, client setup). It is intentionally minimal and is
 * expanded module-by-module per the porting plan; it does not replace the ported
 * {@code com.moulberry.axiom.Axiom} logic, it wires it into NeoForge's lifecycle.
 */
@Mod(AxiomNeoForge.MOD_ID)
public final class AxiomNeoForge {

    public static final String MOD_ID = "axiom";

    private static final Logger LOGGER = LogUtils.getLogger();

    public AxiomNeoForge(IEventBus modBus) {
        // PreLaunch parity: the original mod logs its user agent very early (Fabric preLaunch entrypoint).
        LOGGER.info("Initializing Parallax/{}", com.moulberry.axiom.platform.AxiomPlatform.modVersion());

        // Protocol registration (mod event bus). Mirrors the packet registrations in Axiom.onInitialize().
        modBus.addListener(AxiomNetworking::onRegister);

        // Split of the Fabric ModInitializer.onInitialize() across NeoForge setup phases.
        Axiom axiom = new Axiom();
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(axiom::initCommon));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(axiom::initClient));
        }
    }
}
