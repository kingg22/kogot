package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.InternalForInheritanceGodotApi
import org.jetbrains.annotations.ApiStatus.NonExtendable

/**
 * The [MutableList] view of a Godot packed array: the generated `Packed*Array` classes extend it and supply the engine
 * calls, which take a [Long] index.
 *
 * It is a live view, not a copy: each read is one engine call that returns the element, and a write goes through the
 * engine, which detaches a shared buffer (copy-on-write) before changing it. Unlike [GodotList] nothing passes through
 * a [Variant]: a packed array stores its elements as they are.
 *
 * The [Long]-indexed members are the engine's own API; the [Int]-indexed ones are the [List] contract.
 */
@NonExtendable
@SubclassOptInRequired(InternalForInheritanceGodotApi::class)
public abstract class GodotPackedList<@MustBeVariant E> internal constructor() :
    AbstractMutableList<E>(),
    RandomAccess {

    /** Number of elements. */
    public abstract fun size(): Long

    public abstract override fun isEmpty(): Boolean

    public abstract override fun clear()

    /** The element at [index]; the engine reports an out of range index itself. */
    public abstract operator fun get(index: Long): E

    /** Sets the element at [index] to [value]. */
    public abstract operator fun set(index: Long, value: E)

    /** Inserts [value] before [atIndex], returns the engine error code (`0` is OK). */
    public abstract fun insert(atIndex: Long, value: E): Long

    /** Removes the element at [index]. */
    public abstract fun removeAt(index: Long)

    final override val size: Int get() = size().toInt()

    final override fun get(index: Int): E {
        checkElementIndex(index)
        return get(index.toLong())
    }

    final override fun set(index: Int, element: E): E {
        checkElementIndex(index)
        val previous = get(index.toLong())
        set(index.toLong(), element)
        return previous
    }

    final override fun add(index: Int, element: E) {
        if (index !in 0..size) throw IndexOutOfBoundsException("Index: $index, Size: $size")
        val error = insert(index.toLong(), element)
        check(error == 0L) { "The engine could not insert at $index (error $error)" }
        modCount++
    }

    final override fun removeAt(index: Int): E {
        checkElementIndex(index)
        val previous = get(index.toLong())
        removeAt(index.toLong())
        modCount++
        return previous
    }

    private fun checkElementIndex(index: Int) {
        if (index !in indices) throw IndexOutOfBoundsException("Index: $index, Size: $size")
    }
}
