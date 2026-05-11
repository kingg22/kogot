package io.github.kingg22.godot.api.annotations

import io.github.kingg22.godot.api.PropertyHint
import io.github.kingg22.godot.api.PropertyUsageFlags
import io.github.kingg22.godot.api.builtin.Variant

/**
 * Requires to be annotated in a property of [io.github.kingg22.godot.api.builtin.Signal]
 * @param name The signal name. If empty, the property name is used.
 * @param params The signal parameters. Must be used in the same order to connect to the signal.
 * @see io.github.kingg22.godot.api.builtin.Signal
 */
@Retention(SOURCE)
@Target(PROPERTY)
public annotation class RegisterSignal(vararg val params: Param = [], val name: String = "") {

    @Retention(SOURCE)
    @Target()
    public annotation class Param(
        val type: Variant.Type,
        val name: String,
        val hints: Array<PropertyHint> = [],
        val hintString: String = "",
        val usages: Array<PropertyUsageFlags> = [],
    )
}
