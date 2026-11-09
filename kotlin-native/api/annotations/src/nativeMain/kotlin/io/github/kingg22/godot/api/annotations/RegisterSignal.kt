package io.github.kingg22.godot.api.annotations

import io.github.kingg22.godot.api.PropertyHint
import io.github.kingg22.godot.api.PropertyUsageFlags
import io.github.kingg22.godot.api.builtin.Variant

/**
 * Registers a signal in Godot.
 *
 * On a `TypedSignal` (`val hit by signal<(damage: Long) -> Unit>()`) the name and parameters come from the property:
 * leave [name] and [params] empty. On a builtin [io.github.kingg22.godot.api.builtin.Signal] they must be declared here.
 *
 * @param name The signal name. If empty, the property name is used.
 * @param params The signal parameters, in emission order (builtin `Signal` only).
 * @see io.github.kingg22.godot.api.builtin.Signal
 */
@Retention(SOURCE)
@Target(PROPERTY)
public annotation class RegisterSignal(vararg val params: Param = [], val name: String = "") {

    /**
     * One signal parameter, as Godot shows it in the editor and docs.
     *
     * @param className For an [OBJECT][Variant.Type.OBJECT] parameter, its Godot class (e.g. `"Node"`); empty means
     * any object. Ignored for other types.
     * @param usages Combined with OR; empty means [PropertyUsageFlags.DEFAULT].
     */
    @Retention(SOURCE)
    @Target()
    public annotation class Param(
        val type: Variant.Type,
        val name: String,
        val className: String = "",
        val hint: PropertyHint = PropertyHint.NONE,
        val hintString: String = "",
        val usages: Array<PropertyUsageFlags> = [],
    )
}
