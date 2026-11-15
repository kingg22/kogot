package io.github.kingg22.godot.api.builtin.internal

import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.internal.UsedFromCodegenGeneratedCode
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.nativeHeap
import platform.posix.memset

/**
 * Markers for the default arguments of generated engine methods that build a closeable builtin (`GodotArray()`,
 * `StringName("")`, `Callable()`, ...), issue #232.
 *
 * Kotlin evaluates a default argument at the call site and nothing could close it, so every defaulted call leaked the
 * value. The generated parameter now defaults to the marker of its type, and the method body, seeing the marker
 * ([withDefaultArgument]), builds the real default, uses it for the call and frees it. A fresh value per call, never a
 * shared one: an engine method may write into an `Array` or `Dictionary` argument (`OS.execute`'s `output`).
 *
 * Each marker is a borrowed wrapper over [storage], which is never initialized: a marker only ever serves to be
 * compared by identity and never reaches the engine, and closing it is a no-op. The storage lives for the process.
 */
@UsedFromCodegenGeneratedCode
@PublishedApi
internal object DefaultArguments {
    /** Large enough for any builtin a marker wraps (the biggest, a `Callable` or a `Signal`, is 16 bytes). */
    private val storage = nativeHeap.allocArray<ByteVar>(32).also { memset(it, 0, 32u) }

    val string: GodotString = GodotString(storage)
    val stringName: StringName = StringName(storage)
    val nodePath: NodePath = NodePath(storage)
    val callable: Callable = Callable(storage)
    val signal: Signal = Signal(storage)

    /** `Variant` has no borrowed constructor; this one owns 24 bytes for the process and is never initialized. */
    val variant: Variant = Variant()

    private val anyArray: VariantArray = GodotArray(storage, ElementCodec.of<Variant>())
    private val anyDictionary: VariantDictionary =
        Dictionary(storage, ElementCodec.of<Variant>(), ElementCodec.of<Variant>())

    /** The marker of any `GodotArray<T>`: element types are erased, one instance stands for all of them. */
    @Suppress("UNCHECKED_CAST")
    fun <T> array(): GodotArray<T> = anyArray as GodotArray<T>

    /** The marker of any `Dictionary<K, V>`, see [array]. */
    @Suppress("UNCHECKED_CAST")
    fun <K, V> dictionary(): Dictionary<K, V> = anyDictionary as Dictionary<K, V>

    val packedByteArray: PackedByteArray = PackedByteArray(storage)
    val packedColorArray: PackedColorArray = PackedColorArray(storage)
    val packedFloat32Array: PackedFloat32Array = PackedFloat32Array(storage)
    val packedFloat64Array: PackedFloat64Array = PackedFloat64Array(storage)
    val packedInt32Array: PackedInt32Array = PackedInt32Array(storage)
    val packedInt64Array: PackedInt64Array = PackedInt64Array(storage)
    val packedStringArray: PackedStringArray = PackedStringArray(storage)
    val packedVector2Array: PackedVector2Array = PackedVector2Array(storage)
    val packedVector3Array: PackedVector3Array = PackedVector3Array(storage)
    val packedVector4Array: PackedVector4Array = PackedVector4Array(storage)
}

/**
 * Runs [block] with the argument a caller passed, or, when [value] is the default-argument [marker] (see
 * [DefaultArguments]), with a fresh default built by [create] and freed once [block] returns.
 */
@UsedFromCodegenGeneratedCode
@PublishedApi
internal inline fun <T : AutoCloseable, R> withDefaultArgument(
    value: T,
    marker: T,
    create: () -> T,
    block: (T) -> R,
): R = if (value !== marker) block(value) else create().use(block)
