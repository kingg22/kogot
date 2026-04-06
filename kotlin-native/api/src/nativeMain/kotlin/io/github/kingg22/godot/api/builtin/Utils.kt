package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.internal.checkVariantCompatibility
import kotlin.reflect.KProperty

public fun String.asGodotString(): GodotString = GodotString(this)

public fun String.asNodePath(): NodePath = NodePath(this)

public fun String.asStringName(): StringName = StringName(this)

public fun String?.asVariantString(): Variant = this?.asGodotString().use { it.asVariant() }

public fun String?.asVariantStringName(): Variant = this?.asStringName().use { it.asVariant() }

public fun String?.asVariantNodePath(): Variant = this?.asNodePath().use { it.asVariant() }

/**
 * Syntactic sugar for [Variant.getValue] with type checking and inferred return type.
 *
 * Must prefer explicit convert methods over this operator.
 */
@ExperimentalGodotKotlin
public inline operator fun <reified T> Variant.getValue(thisRef: T?, property: KProperty<*>): T {
    val type = getType()
    checkVariantCompatibility<T>(type)
    if (T::class == String::class) {
        error("Cannot provide kotlin String, must use GodotString instead or explicit convert method")
    }
    val value = getValue()
    @Suppress("UNCHECKED_CAST")
    return value as T
}
