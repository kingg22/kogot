package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.MustBeVariant
import io.github.kingg22.godot.api.builtin.Signal
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.internal.variantTypeOf
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Descriptor for a signal parameter, combining name and type information.
 *
 * This is used by [Signal] with parameters to capture the Godot-side parameter
 * metadata required for signal registration.
 *
 * @param P The Kotlin type of this parameter
 * @property name The parameter name as it will appear in Godot
 * @property kType The Kotlin type of [P] for Variant conversion
 * @constructor Prefers factory method [param]
 */
public data class SignalParameterDescriptor<@MustBeVariant P> @ExperimentalGodotKotlin constructor(
    public val name: String,
    public val kType: KType,
) {
    init {
        require(kType != typeOf<SignalParameterDescriptor<*>>()) { "SignalParameter cannot be a signal parameter" }
    }

    @Suppress("DEPRECATION") // TODO find a better way to do this
    public val variantType: Variant.Type = variantTypeOf(kType)

    public val isOptional: Boolean get() = kType.isMarkedNullable
}
