package com.moulberry.mixinconstraints.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies the annotated mixin (or method) only when the named mod (or any alias) is absent.
 * Recreated from Moulberry's mixinconstraints; evaluated by {@code AxiomMixinPlugin} against
 * NeoForge's {@code ModList}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface IfModAbsent {
    String value();

    String minVersion() default "";

    String maxVersion() default "";

    String[] aliases() default {};
}
