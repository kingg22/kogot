package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.InternalForInheritanceGodotApi
import org.jetbrains.annotations.ApiStatus.NonExtendable

/**
 * The [MutableList] view of a Godot array: the generated [GodotArray] extends it and supplies the engine calls.
 *
 * It is a live view, not a copy. Godot arrays are shared containers, so every change is seen by the engine and by
 * every other wrapper of the same array. Each [get] reads and converts one element with the [codec]; nothing is cached.
 *
 * The raw, [Variant]-typed access of the engine API is [getVariant] and [setVariant].
 */
@NonExtendable
@SubclassOptInRequired(InternalForInheritanceGodotApi::class)
public abstract class GodotList<@MustBeVariant T> internal constructor(private val codec: ElementCodec<T>) :
    AbstractMutableList<T>(),
    RandomAccess {

    /** Number of elements. */
    public abstract fun size(): Long

    public abstract override fun isEmpty(): Boolean

    public abstract override fun clear()

    /** The element at [index] as a new [Variant] the caller owns. The engine reports an out of range index itself. */
    public abstract fun getVariant(index: Long): Variant

    /** Sets the element at [index] to a copy of [value]. */
    public abstract fun setVariant(index: Long, value: Variant)

    /** Inserts a copy of [value] before [position], returns the engine error code (`0` is OK). */
    public abstract fun insert(position: Long, value: Variant): Long

    /** Removes the element at [position]. */
    public abstract fun removeAt(position: Long)

    final override val size: Int get() = size().toInt()

    final override fun get(index: Int): T {
        checkElementIndex(index)
        return codec.read(getVariant(index.toLong()))
    }

    final override fun set(index: Int, element: T): T {
        checkElementIndex(index)
        val previous = get(index)
        codec.write(element).use { setVariant(index.toLong(), it) }
        return previous
    }

    final override fun add(index: Int, element: T) {
        if (index !in 0..size) throw IndexOutOfBoundsException("Index: $index, Size: $size")
        val error = codec.write(element).use { insert(index.toLong(), it) }
        check(error == 0L) { "The engine could not insert at $index (error $error)" }
        modCount++
    }

    final override fun removeAt(index: Int): T {
        checkElementIndex(index)
        val previous = get(index)
        removeAt(index.toLong())
        modCount++
        return previous
    }

    private fun checkElementIndex(index: Int) {
        if (index !in indices) throw IndexOutOfBoundsException("Index: $index, Size: $size")
    }
}
