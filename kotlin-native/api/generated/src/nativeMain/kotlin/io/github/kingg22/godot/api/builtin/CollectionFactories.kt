package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.builtin.internal.toGDE
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import io.github.kingg22.godot.internal.binding.ArrayBinding
import io.github.kingg22.godot.internal.binding.DictionaryBinding
import kotlinx.cinterop.COpaquePointer
import org.jetbrains.annotations.ApiStatus

// The constructors of GodotArray and Dictionary are internal: they need an ElementCodec, and a codec that does not
// match the element type would let a wrong element through. The generated `GodotArray<T>()`, `Dictionary<K, V>()` and
// the factories of their other constructors pick the codec from the reified types. These are the untyped ones, plus
// the wrappers over a pointer owned by someone else.

// ── New, empty untyped collections ───────────────────────────────────────────────────────────────

/** A new, empty untyped array. The caller owns it and must close it. */
public fun VariantArray(): VariantArray = GodotArray(ElementCodec.VARIANT)

/** A new, empty untyped dictionary. The caller owns it and must close it. */
public fun VariantDictionary(): VariantDictionary = Dictionary(ElementCodec.VARIANT, ElementCodec.VARIANT)

// ── From Kotlin collections ──────────────────────────────────────────────────────────────────────

/**
 * Copies this collection into a new Godot array typed as [T] (see [GodotArray]). The elements are converted one by
 * one: the result does not share memory with the receiver, later changes to either are not seen by the other.
 *
 * The caller owns the result and must close it.
 *
 * @throws ClassCastException if an element cannot be sent to Godot as a [T]; the partial array is closed first.
 */
public inline fun <@MustBeVariant reified T> Iterable<T>.toGodotArray(): GodotArray<T> {
    val array = GodotArray<T>()
    try {
        array.addAll(this)
    } catch (e: Throwable) {
        array.close()
        throw e
    }
    return array
}

/**
 * Copies this map into a new Godot dictionary typed as [K] and [V] (see [Dictionary]). Keys and values are converted
 * one by one: the result does not share memory with the receiver.
 *
 * The caller owns the result and must close it.
 *
 * @throws ClassCastException if a key or a value cannot be sent to Godot; the partial dictionary is closed first.
 */
public inline fun <@MustBeVariant reified K, @MustBeVariant reified V> Map<K, V>.toDictionary(): Dictionary<K, V> {
    val dictionary = Dictionary<K, V>()
    try {
        dictionary.putAll(this)
    } catch (e: Throwable) {
        dictionary.close()
        throw e
    }
    return dictionary
}

// ── Wrappers over a pointer owned by someone else (engine arguments, borrowed values) ────────────

/** Wraps the array at [rawPtr] without owning it: closing the wrapper never destroys the array. */
@UsedFromCodegenGeneratedCode
@ApiStatus.Internal
public fun VariantArray(rawPtr: COpaquePointer): VariantArray = GodotArray(rawPtr, ElementCodec.VARIANT)

/** Wraps the array at [rawPtr] without owning it: closing the wrapper never destroys the array. */
@UsedFromCodegenGeneratedCode
@ApiStatus.Internal
public inline fun <@MustBeVariant reified T> GodotArray(rawPtr: COpaquePointer): GodotArray<T> =
    wrapGodotArray(rawPtr, ElementCodec.of<T>())

/** Wraps the dictionary at [rawPtr] without owning it: closing the wrapper never destroys the dictionary. */
@UsedFromCodegenGeneratedCode
@ApiStatus.Internal
public fun VariantDictionary(rawPtr: COpaquePointer): VariantDictionary =
    Dictionary(rawPtr, ElementCodec.VARIANT, ElementCodec.VARIANT)

/** Wraps the dictionary at [rawPtr] without owning it: closing the wrapper never destroys the dictionary. */
@UsedFromCodegenGeneratedCode
@ApiStatus.Internal
public inline fun <@MustBeVariant reified K, @MustBeVariant reified V> Dictionary(
    rawPtr: COpaquePointer,
): Dictionary<K, V> = wrapDictionary(rawPtr, ElementCodec.of<K>(), ElementCodec.of<V>())

@PublishedApi
internal fun <@MustBeVariant T> wrapGodotArray(rawPtr: COpaquePointer, codec: ElementCodec<T>): GodotArray<T> =
    GodotArray(rawPtr, codec).checkedFromEngine()

@PublishedApi
internal fun <@MustBeVariant K, @MustBeVariant V> wrapDictionary(
    rawPtr: COpaquePointer,
    keyCodec: ElementCodec<K>,
    valueCodec: ElementCodec<V>,
): Dictionary<K, V> = Dictionary(rawPtr, keyCodec, valueCodec).checkedFromEngine()

// ── Godot-side typing ────────────────────────────────────────────────────────────────────────────

/**
 * Fixes the element type of this new, empty array in the engine to the type of its codec, so Godot itself rejects a
 * wrong element. An untyped codec (`Variant`, nullable, object) leaves the array untyped.
 */
@IgnorableReturnValue
@PublishedApi
internal fun <T> GodotArray<T>.applyGodotType(): GodotArray<T> {
    val type = this.codec.godotType

    if (type == Variant.Type.NIL) return this

    StringName().use { className ->
        Variant().use { script ->
            ArrayBinding.setTypedRaw(this.rawPtr, type.toGDE(), className.rawPtr, script.rawPtr)
        }
    }

    return this
}

/** Same as [applyGodotType] for a new, empty dictionary, with the key and the value type. */
@IgnorableReturnValue
@PublishedApi
internal fun <K, V> Dictionary<K, V>.applyGodotType(): Dictionary<K, V> {
    val keyType = this.keyCodec.godotType
    val valueType = this.valueCodec.godotType

    if (keyType == Variant.Type.NIL && valueType == Variant.Type.NIL) return this

    StringName().use { className ->
        Variant().use { script ->
            DictionaryBinding.setTypedRaw(
                this.rawPtr,
                keyType.toGDE(),
                className.rawPtr,
                script.rawPtr,
                valueType.toGDE(),
                className.rawPtr,
                script.rawPtr,
            )
        }
    }
    return this
}

/**
 * Checks that an array that came from the engine is typed the way the codec expects,
 * and returns it. A codec with a Godot type needs an array typed with that same type;
 * an untyped codec accepts any array.
 *
 * @throws IllegalStateException if the array is not typed as the codec expects.
 */
@IgnorableReturnValue
@PublishedApi
internal fun <T> GodotArray<T>.checkedFromEngine(): GodotArray<T> {
    val expected = codec.godotType
    if (expected == Variant.Type.NIL) return this
    // `getTypedBuiltin` is `NIL` for an untyped array: one engine call is enough.
    val actual = getTypedBuiltin()
    check(actual == expected.value) {
        "Expected an array typed as $expected but the engine array is typed as " +
            "${Variant.Type.entries.firstOrNull { it.value == actual }}"
    }
    return this
}

/**
 * Checks that a dictionary that came from the engine is typed the way the codec expects,
 * and returns it. A codec with a Godot type needs a dictionary typed with that same type;
 * an untyped codec accepts any dictionary.
 *
 * @throws IllegalStateException if the dictionary is not typed as the codec expects.
 */
@IgnorableReturnValue
@PublishedApi
internal fun <K, V> Dictionary<K, V>.checkedFromEngine(): Dictionary<K, V> {
    val key = keyCodec.godotType
    val value = valueCodec.godotType
    if (key != Variant.Type.NIL) {
        val actual = getTypedKeyBuiltin()
        check(actual == key.value) { "Expected dictionary keys typed as $key but they are typed as $actual" }
    }
    if (value != Variant.Type.NIL) {
        val actual = getTypedValueBuiltin()
        check(actual == value.value) { "Expected dictionary values typed as $value but they are typed as $actual" }
    }
    return this
}
