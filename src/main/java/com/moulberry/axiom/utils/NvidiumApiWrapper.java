package com.moulberry.axiom.utils;

import com.moulberry.axiom.platform.AxiomPlatform;
import java.lang.reflect.Method;

/**
 * Optional Nvidium integration, accessed reflectively (no compile-time dependency on Nvidium).
 */
public class NvidiumApiWrapper {

    private static Object nvidiumApi = null;
    private static Method hideSectionMethod = null;
    private static Method showSectionMethod = null;

    public static void setupIfNvidiumInstalled() {
        nvidiumApi = null;
        if (AxiomPlatform.isModLoaded("nvidium")) {
            try {
                Class<?> clazz = Class.forName("me.cortex.nvidium.api0.NvidiumAPI");
                nvidiumApi = clazz.getConstructor(String.class).newInstance("axiom");
                hideSectionMethod = clazz.getMethod("hideSection", int.class, int.class, int.class);
                showSectionMethod = clazz.getMethod("showSection", int.class, int.class, int.class);
            } catch (Throwable t) {
                nvidiumApi = null;
            }
        }
    }

    public static boolean isNvidiumAvailable() {
        return nvidiumApi != null;
    }

    public static void hideSection(int x, int y, int z) {
        if (nvidiumApi != null) {
            try {
                hideSectionMethod.invoke(nvidiumApi, x, y, z);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void showSection(int x, int y, int z) {
        if (nvidiumApi != null) {
            try {
                showSectionMethod.invoke(nvidiumApi, x, y, z);
            } catch (Throwable ignored) {
            }
        }
    }
}
