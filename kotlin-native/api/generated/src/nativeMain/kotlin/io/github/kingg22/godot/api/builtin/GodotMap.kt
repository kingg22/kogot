package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.InternalForInheritanceGodotApi
import org.jetbrains.annotations.ApiStatus.NonExtendable

/**
 * The [MutableMap] view of a Godot dictionary: the generated [Dictionary] extends it and supplies the engine calls.
 *
 * It is a live view, not a copy. Godot dictionaries are shared containers, so every change is seen by the engine and
 * by every other wrapper of the same dictionary. Keys and values are converted one at a time with their codecs.
 * Iterating takes a snapshot of the keys when it starts; a value is read when its entry is reached.
 *
 * The raw, [Variant]-typed access of the engine API is [getVariant] and [setVariant].
 */
@NonExtendable
@SubclassOptInRequired(InternalForInheritanceGodotApi::class)
public abstract class GodotMap<@MustBeVariant K, @MustBeVariant V> internal constructor(
    internal val keyCodec: ElementCodec<K>,
    internal val valueCodec: ElementCodec<V>,
) : AbstractMutableMap<K, V>() {

    /** Number of entries. */
    public abstract fun size(): Long

    public abstract override fun isEmpty(): Boolean

    public abstract override fun clear()

    /** Whether the dictionary has [key]. */
    public abstract fun has(key: Variant): Boolean

    /** Removes [key], returns whether it was there. */
    public abstract fun erase(key: Variant): Boolean

    /** The value of [key] as a new [Variant] the caller owns, or [default] if it is missing. */
    public abstract fun getVariant(key: Variant, default: Variant = Variant()): Variant

    /** Sets [key] to a copy of [value], returns whether the engine accepted it. */
    public abstract fun setVariant(key: Variant, value: Variant): Boolean

    /** A snapshot of the keys, as a new array the caller owns and must close. */
    public abstract fun keys(): VariantArray

    final override val size: Int get() = size().toInt()

    final override fun containsKey(key: K): Boolean = keyCodec.write(key).use { has(it) }

    // `getVariant` of a missing key logs an engine error, so the callers ask `containsKey` first.
    private fun valueOf(key: K): V = keyCodec.write(key).use { valueCodec.read(getVariant(it)) }

    final override fun get(key: K): V? = if (containsKey(key)) valueOf(key) else null

    final override fun put(key: K, value: V): V? {
        val previous = get(key)
        keyCodec.write(key).use { variantKey ->
            valueCodec.write(value).use { variantValue ->
                check(setVariant(variantKey, variantValue)) { "The engine rejected the entry for key $key" }
            }
        }
        return previous
    }

    final override fun remove(key: K): V? {
        val previous = get(key)
        keyCodec.write(key).use { erase(it) }
        return previous
    }

    final override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = object :
        AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int get() = this@GodotMap.size

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
            throw UnsupportedOperationException("Add entries with put")

        override fun clear() = this@GodotMap.clear()

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            val snapshot = keys().use { array -> List(array.size) { keyCodec.read(array.getVariant(it.toLong())) } }
            return object : MutableIterator<MutableMap.MutableEntry<K, V>> {
                private var next = 0
                private var current: K? = null
                private var hasCurrent = false

                override fun hasNext(): Boolean = next < snapshot.size

                override fun next(): MutableMap.MutableEntry<K, V> {
                    if (!hasNext()) throw NoSuchElementException()
                    val key = snapshot[next++]
                    current = key
                    hasCurrent = true
                    // The value is read now: an entry is a snapshot of the pair, like the stdlib maps.
                    if (!containsKey(key)) throw ConcurrentModificationException("The key $key was removed")
                    return Entry(key, valueOf(key))
                }

                override fun remove() {
                    check(hasCurrent) { "next() has not been called" }
                    @Suppress("UNCHECKED_CAST")
                    this@GodotMap.remove(current as K)
                    hasCurrent = false
                }
            }
        }
    }

    private inner class Entry(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
        override fun setValue(newValue: V): V {
            val previous = value
            put(key, newValue)
            value = newValue
            return previous
        }

        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && other.key == key && other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}
