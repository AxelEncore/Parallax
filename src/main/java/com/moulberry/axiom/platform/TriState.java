package com.moulberry.axiom.platform;

import java.util.function.BooleanSupplier;
import org.jetbrains.annotations.Nullable;

/**
 * Three-valued boolean, API-compatible with Fabric's {@code net.fabricmc.fabric.api.util.TriState}
 * so ported call sites keep working unchanged. (NeoForge ships its own {@code TriState} with a
 * different method surface, hence this local replacement.)
 */
public enum TriState {
    FALSE,
    DEFAULT,
    TRUE;

    public static TriState of(boolean bool) {
        return bool ? TRUE : FALSE;
    }

    public static TriState of(@Nullable Boolean bool) {
        return bool == null ? DEFAULT : of(bool.booleanValue());
    }

    /** True only when this is {@link #TRUE}. */
    public boolean get() {
        return this == TRUE;
    }

    /** {@code null} for {@link #DEFAULT}, otherwise the boxed boolean. */
    @Nullable
    public Boolean getBoxed() {
        return this == DEFAULT ? null : this.get();
    }

    public boolean orElse(boolean value) {
        return this == DEFAULT ? value : this.get();
    }

    public boolean orElseGet(BooleanSupplier supplier) {
        return this == DEFAULT ? supplier.getAsBoolean() : this.get();
    }
}
