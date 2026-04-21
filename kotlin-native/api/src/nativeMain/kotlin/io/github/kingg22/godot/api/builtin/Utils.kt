package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.internal.anyToVariant
import io.github.kingg22.godot.api.builtin.internal.checkVariantCompatibility
import kotlin.reflect.KProperty

public fun String.asGodotString(): GodotString = GodotString(this)

public fun String.asNodePath(): NodePath = NodePath(this)

public fun String.asStringName(): StringName = StringName(this)

public fun String?.asVariantString(): Variant = this?.asGodotString().use { it.asVariant() }

public fun String?.asVariantStringName(): Variant = this?.asStringName().use { it.asVariant() }

public fun String?.asVariantNodePath(): Variant = this?.asNodePath().use { it.asVariant() }

/** CAUTION: Read docs of [from] converter */
@ExperimentalGodotKotlin
public inline fun <reified T> T?.asVariant(): Variant = anyToVariant(this)

/**
 * Converts an element to a [Variant] with type checking.
 *
 * This is unsafe, use caution. Prefers explicit convert methods over this function.
 *
 * **Safety**:
 * - [CPointer][kotlinx.cinterop.COpaquePointer] is not supported, use
 * [Object][io.github.kingg22.godot.api.core.GodotObject] if it's an Object pointer or
 * primary constructor of Variant if it's a Variant pointer instead.
 * - [kotlin.String] is mapped to [GodotString] and box to [Variant]
 */
@ExperimentalGodotKotlin
public inline fun <reified T> Variant.Companion.from(element: @MustBeVariant T): Variant = anyToVariant(this)

/**
 * Syntactic sugar for [Variant.getValue] with type checking and inferred return type.
 *
 * Must prefer explicit convert methods over this operator.
 *
 * **Safety**:
 * - [kotlin.String] is not supported **yet**, use [GodotString] instead
 */
@ExperimentalGodotKotlin
public inline operator fun <@MustBeVariant reified T> Variant.getValue(thisRef: T?, property: KProperty<*>): T {
    if (T::class == String::class) {
        error("Cannot provide kotlin String, must use GodotString instead or explicit convert method")
    }
    val value = getValue()
    if (value !is T) error("Expected ${T::class} but got ${value?.let { it::class }}")
    return value
}
