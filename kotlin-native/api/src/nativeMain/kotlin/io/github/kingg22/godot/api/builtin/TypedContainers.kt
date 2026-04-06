package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.internal.checkVariantCompatibility
import io.github.kingg22.godot.api.builtin.internal.checkVariantTypeAndConvert
import io.github.kingg22.godot.api.builtin.internal.toGDE
import io.github.kingg22.godot.api.builtin.internal.variantTypeOf
import io.github.kingg22.godot.internal.binding.ArrayBinding
import io.github.kingg22.godot.internal.binding.DictionaryBinding
import kotlin.contracts.contract
import kotlin.reflect.typeOf

/** Performs a conversion to a typed array of the given [type], returns [VariantArray] */
public fun GodotArray<*>.toTypedArray(
    type: Variant.Type,
    className: StringName = StringName(""),
    script: Variant = Variant(),
): VariantArray = toTypedArray<Variant>(
    type = type,
    className = className,
    script = script,
)

/** Performs a conversion to a typed array of the given [type] or inferred from [T], returns [GodotArray] of the given [T] */
@ExperimentalGodotKotlin
@Suppress("UNCHECKED_CAST")
public inline fun <reified T : Any> GodotArray<*>.toTypedArray(
    type: Variant.Type = variantTypeOf(typeOf<T>()),
    className: StringName = StringName(""),
    script: Variant = Variant(),
): GodotArray<T> = GodotArray<T>(
    base = this as VariantArray,
    type = type.value,
    className = className,
    script = script,
).also { assert(it.isTyped()) { "Array is not typed after constructing it as typed" } }

/** Performs a conversion to a typed dictionary of the given [keyType] and [valueType], returns [VariantDictionary] */
public fun Dictionary<*, *>.toTypedDictionary(
    keyType: Variant.Type,
    valueType: Variant.Type,
    keyClassName: StringName = StringName(""),
    keyScript: Variant = Variant(),
    valueClassName: StringName = StringName(""),
    valueScript: Variant = Variant(),
): VariantDictionary = toTypedDictionary<Variant, Variant>(
    keyType = keyType,
    keyClassName = keyClassName,
    keyScript = keyScript,
    valueType = valueType,
    valueClassName = valueClassName,
    valueScript = valueScript,
)

/** Performs a conversion to a typed dictionary of the given [keyType] and [valueType], returns [Dictionary] of the given [K] and [V] */
@ExperimentalGodotKotlin
@Suppress("UNCHECKED_CAST")
public inline fun <reified K : Any, reified V : Any> Dictionary<*, *>.toTypedDictionary(
    keyType: Variant.Type = variantTypeOf(typeOf<K>()),
    valueType: Variant.Type = variantTypeOf(typeOf<V>()),
    keyClassName: StringName = StringName(""),
    keyScript: Variant = Variant(),
    valueClassName: StringName = StringName(""),
    valueScript: Variant = Variant(),
): Dictionary<K, V> = Dictionary<K, V>(
    base = this as VariantDictionary,
    keyType = keyType.value,
    keyClassName = keyClassName,
    keyScript = keyScript,
    valueType = valueType.value,
    valueClassName = valueClassName,
    valueScript = valueScript,
).also { assert(it.isTyped()) { "Dictionary is not typed after constructing it as typed" } }

/**
 * Sets the [type] of the array and returns the array itself typed.
 *
 * See the constructor typed of [GodotArray] for more information.
 *
 * @param className The name of the object (if [type] is [Variant.Type.OBJECT]).
 * @param script The Script object (if [type] is [Variant.Type.OBJECT] and the base class is extended by a script).
 */
public fun GodotArray<*>.setTyped(
    type: Variant.Type,
    className: StringName = StringName(""),
    script: Variant = Variant(),
): VariantArray = setTyped<Variant>(
    type = type,
    className = className,
    script = script,
)

/** Sets the [type] of the array and returns the array itself. */
@ExperimentalGodotKotlin
@Suppress("UNCHECKED_CAST")
@IgnorableReturnValue
public inline fun <reified T : Any> GodotArray<*>.setTyped(
    type: Variant.Type = variantTypeOf(typeOf<T>()),
    className: StringName = StringName(""),
    script: Variant = Variant(),
): GodotArray<T> {
    contract { returns() implies (this@setTyped is GodotArray<T>) }
    ArrayBinding.instance.setTypedRaw(
        pSelf = rawPtr,
        pType = type.toGDE(),
        pClassName = className.rawPtr,
        pScript = script.rawPtr,
    )
    assert(this.isTyped()) {
        "Array is not typed after setting its type."
    }
    return this as GodotArray<T>
}

@ExperimentalGodotKotlin
@Suppress("UNCHECKED_CAST")
@IgnorableReturnValue
public inline fun <reified K : Any, reified V : Any> Dictionary<*, *>.setTyped(
    keyType: Variant.Type = variantTypeOf(typeOf<K>()),
    valueType: Variant.Type = variantTypeOf(typeOf<V>()),
    keyClassName: StringName = StringName(""),
    keyScript: Variant = Variant(),
    valueClassName: StringName = StringName(""),
    valueScript: Variant = Variant(),
): Dictionary<K, V> {
    contract { returns() implies (this@setTyped is Dictionary<K?, V?>) }
    DictionaryBinding.instance.setTypedRaw(
        pSelf = rawPtr,
        pKeyType = keyType.toGDE(),
        pKeyClassName = keyClassName.rawPtr,
        pKeyScript = keyScript.rawPtr,
        pValueType = valueType.toGDE(),
        pValueClassName = valueClassName.rawPtr,
        pValueScript = valueScript.rawPtr,
    )
    assert(this.isTyped()) {
        "Dictionary is not typed after setting its type."
    }
    return this as Dictionary<K, V>
}

@IgnorableReturnValue
public fun Dictionary<*, *>.setTyped(
    keyType: Variant.Type,
    valueType: Variant.Type,
    keyClassName: StringName = StringName(""),
    keyScript: Variant = Variant(),
    valueClassName: StringName = StringName(""),
    valueScript: Variant = Variant(),
): VariantDictionary = setTyped<Variant, Variant>(
    keyType,
    valueType,
    keyClassName,
    keyScript,
    valueClassName,
    valueScript,
)

@ExperimentalGodotKotlin
public inline fun <reified T : Any> godotArrayOf(
    type: Variant.Type = variantTypeOf(typeOf<T>()),
    className: StringName = StringName(""),
    script: Variant = Variant(),
): GodotArray<T> {
    checkVariantCompatibility<T>(type)
    return VariantArray().setTyped<T>(
        type = type,
        className = className,
        script = script,
    )
}

/** **Slow path** for creating a typed [GodotArray] with initial values. */
@ExperimentalGodotKotlin
public inline fun <reified T : Any> godotArrayOf(
    vararg elements: T? = arrayOf(),
    type: Variant.Type = variantTypeOf(typeOf<T>()),
    className: StringName = StringName(""),
    script: Variant = Variant(),
): GodotArray<T> {
    val base = godotArrayOf<T>(type = type, className = className, script = script)
    val _ = base.resize(elements.size.toLong())

    elements.forEachIndexed { index, element ->
        base[index.toLong()] = checkVariantTypeAndConvert(element, type)
    }

    return base
}

@ExperimentalGodotKotlin
public inline fun <reified K : Any, reified V : Any> godotDictionaryOf(
    keyType: Variant.Type = variantTypeOf(typeOf<K>()),
    valueType: Variant.Type = variantTypeOf(typeOf<V>()),
    keyClassName: StringName = StringName(""),
    keyScript: Variant = Variant(),
    valueClassName: StringName = StringName(""),
    valueScript: Variant = Variant(),
): Dictionary<K, V> {
    checkVariantCompatibility<K>(keyType)
    checkVariantCompatibility<V>(valueType)
    return VariantDictionary().setTyped<K, V>(
        keyType = keyType,
        valueType = valueType,
        keyClassName = keyClassName,
        keyScript = keyScript,
        valueClassName = valueClassName,
        valueScript = valueScript,
    )
}

/** **Slow path** for creating a typed [Dictionary] with initial values. */
@ExperimentalGodotKotlin
public inline fun <reified K : Any, reified V : Any> godotDictionaryOf(
    vararg pairs: Pair<K, V> = arrayOf(),
    keyType: Variant.Type = variantTypeOf(typeOf<K>()),
    valueType: Variant.Type = variantTypeOf(typeOf<V>()),
    keyClassName: StringName = StringName(""),
    keyScript: Variant = Variant(),
    valueClassName: StringName = StringName(""),
    valueScript: Variant = Variant(),
): Dictionary<K, V> {
    val base = godotDictionaryOf<K, V>(
        keyType = keyType,
        valueType = valueType,
        keyClassName = keyClassName,
        keyScript = keyScript,
        valueClassName = valueClassName,
        valueScript = valueScript,
    )

    pairs.forEach { (k, v) ->
        base[checkVariantTypeAndConvert(k, keyType)] = checkVariantTypeAndConvert(v, valueType)
    }

    return base
}
