package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.internal.checkVariantCompatibility
import io.github.kingg22.godot.api.builtin.internal.checkVariantTypeAndConvert
import io.github.kingg22.godot.api.builtin.internal.variantTypeOf
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf

public fun String.asGodotString(): GodotString = GodotString(this)

public fun String.asNodePath(): NodePath = NodePath(this)

public fun String.asStringName(): StringName = StringName(this)

public fun String?.asVariantString(): Variant = this?.asGodotString().use { it.asVariant() }

public fun String?.asVariantStringName(): Variant = this?.asStringName().use { it.asVariant() }

public fun String?.asVariantNodePath(): Variant = this?.asNodePath().use { it.asVariant() }

/** CAUTION: Read docs of [from] converter */
@ExperimentalGodotKotlin
public inline fun <reified T> T?.asVariant(): Variant = Variant.from(this)

/**
 * Converts an element to a [Variant] with type checking.
 *
 * This is unsafe, use caution. Prefers explicit convert methods over this function.
 *
 * [CPointer][kotlinx.cinterop.COpaquePointer] is not supported, use
 * [Object][io.github.kingg22.godot.api.core.GodotObject] or
 * primary constructor of Variant if you are sure it is a Variant pointer instead.
 */
@ExperimentalGodotKotlin
public inline fun <reified T> Variant.Companion.from(element: @MustBeVariant T): Variant = checkVariantTypeAndConvert(
    element = element,
    type = variantTypeOf(typeOf<T>()),
)

/**
 * Syntactic sugar for [Variant.getValue] with type checking and inferred return type.
 *
 * Must prefer explicit convert methods over this operator.
 */
@ExperimentalGodotKotlin
public inline operator fun <@MustBeVariant reified T> Variant.getValue(thisRef: T?, property: KProperty<*>): T {
    val type = getType()
    checkVariantCompatibility<T>(type)
    if (T::class == String::class) {
        error("Cannot provide kotlin String, must use GodotString instead or explicit convert method")
    }
    val value = getValue()
    @Suppress("UNCHECKED_CAST")
    return value as T
}
