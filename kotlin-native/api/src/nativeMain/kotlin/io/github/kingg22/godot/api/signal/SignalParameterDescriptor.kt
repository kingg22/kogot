package io.github.kingg22.godot.api.signal

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.MustBeVariant
import kotlin.reflect.KType

/**
 * Descriptor for a signal parameter, combining name and type information.
 *
 * This is used by [Signal] with parameters to capture the Godot-side parameter
 * metadata required for signal registration.
 *
 * @param P The Kotlin type of this parameter
 * @property name The parameter name as it will appear in Godot
 * @property kType The Kotlin type for Variant conversion
 * @constructor Prefers factory method [param]
 */
public class SignalParameterDescriptor<@MustBeVariant P> @ExperimentalGodotKotlin constructor(
    public val name: String,
    public val kType: KType,
) {
    public val isOptional: Boolean get() = kType.isMarkedNullable
}
