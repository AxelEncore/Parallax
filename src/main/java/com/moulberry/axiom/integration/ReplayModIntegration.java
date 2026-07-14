package com.moulberry.axiom.integration;

/**
 * Optional ReplayMod integration. Accessed reflectively so the port has no compile-time dependency
 * on ReplayMod (mirrors {@code IrisApiWrapper}/{@code NvidiumApiWrapper}). When ReplayMod is playing
 * back a replay, Axiom must not process its inbound packets.
 */
public final class ReplayModIntegration {

    private ReplayModIntegration() {
    }

    public static boolean isPlayingReplay() {
        try {
            Class<?> clazz = Class.forName("com.replaymod.replay.ReplayModReplay");
            Object instance = clazz.getField("instance").get(null);
            Object handler = clazz.getMethod("getReplayHandler").invoke(instance);
            return handler != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
