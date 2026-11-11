package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
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
    GodotArray(rawPtr, codec)

@PublishedApi
internal fun <@MustBeVariant K, @MustBeVariant V> wrapDictionary(
    rawPtr: COpaquePointer,
    keyCodec: ElementCodec<K>,
    valueCodec: ElementCodec<V>,
): Dictionary<K, V> = Dictionary(rawPtr, keyCodec, valueCodec)
