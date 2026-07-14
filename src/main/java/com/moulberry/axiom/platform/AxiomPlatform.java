package com.moulberry.axiom.platform;

import java.nio.file.Path;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Loader abstraction for the NeoForge port.
 *
 * <p>Replaces the {@code net.fabricmc.loader.api.FabricLoader} calls that the original mod scatters
 * throughout its code base. Ported classes call these helpers instead of the Fabric API so the rest
 * of the source can stay structurally close to the original.
 */
public final class AxiomPlatform {

    public static final String MOD_ID = "axiom";

    private AxiomPlatform() {
    }

    /** Loader config directory, e.g. {@code .minecraft/config} (mirrors {@code FabricLoader.getConfigDir()}). */
    public static Path configDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    /** Axiom's own config directory: {@code <config>/axiom} (what {@code Axiom.getConfigDirectory()} returned). */
    public static Path axiomConfigDir() {
        return configDir().resolve(MOD_ID);
    }

    /** The Minecraft run/game directory (mirrors {@code FabricLoader.getGameDir()}). */
    public static Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    /** Whether another mod is present (mirrors {@code FabricLoader.isModLoaded(id)}). */
    public static boolean isModLoaded(String id) {
        ModList list = ModList.get();
        return list != null && list.isLoaded(id);
    }

    /** True in a dev workspace (mirrors {@code FabricLoader.isDevelopmentEnvironment()}). */
    public static boolean isDevelopment() {
        return !FMLEnvironment.production;
    }

    /** True on the physical client (mirrors {@code EnvType.CLIENT}). */
    public static boolean isPhysicalClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    /** True on a dedicated server (mirrors {@code EnvType.SERVER}). */
    public static boolean isDedicatedServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    /** Axiom's own version string, e.g. {@code 5.4.2}. */
    public static String modVersion() {
        ModList list = ModList.get();
        if (list == null) {
            return "unknown";
        }
        return list.getModContainerById(MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
}
