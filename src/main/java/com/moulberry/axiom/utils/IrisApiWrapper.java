package com.moulberry.axiom.utils;

import com.moulberry.axiom.platform.AxiomPlatform;
import java.lang.reflect.Method;

/**
 * Optional Iris integration, accessed reflectively (no compile-time dependency on Iris).
 */
public class IrisApiWrapper {

    private static Object irisApi = null;
    private static Method isShaderPackInUseMethod = null;

    public static void setupIfIrisInstalled() {
        irisApi = null;
        if (AxiomPlatform.isModLoaded("iris")) {
            try {
                Class<?> clazz = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = clazz.getMethod("getInstance").invoke(null);
                isShaderPackInUseMethod = clazz.getMethod("isShaderPackInUse");
            } catch (Throwable t) {
                irisApi = null;
            }
        }
    }

    public static boolean isUsingShaders() {
        if (irisApi == null) {
            return false;
        }
        try {
            return (Boolean) isShaderPackInUseMethod.invoke(irisApi);
        } catch (Throwable t) {
            return false;
        }
    }
}
