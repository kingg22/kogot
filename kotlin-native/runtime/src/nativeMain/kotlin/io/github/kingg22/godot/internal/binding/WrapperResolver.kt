package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr
import kotlin.concurrent.AtomicReference

/**
 * Finds the most derived wrapper class known to the binding for a native object, without the engine
 * knowing anything about Kotlin and without a global name-to-constructor map.
 *
 * 1. Ask the engine for the most derived class name `N` ([classNameOf], `object_get_class_name`).
 * 2. Build its ancestor chain with [parentOf] (`ClassDB.get_parent_class`).
 * 3. Descend the generated [ClassNode] tree following that chain, and stop at the deepest known node.
 *
 * A class the binding does not know (another GDExtension, a newer Godot) therefore resolves to its nearest
 * known ancestor. The outcome is cached per class name, so the engine is asked once per distinct class.
 *
 * It is a pure function of its three inputs: no engine access, so it can be tested with fakes.
 */
@InternalBinding
public class WrapperResolver<out W : Any>(
    private val root: ClassNode<W>,
    private val classNameOf: (GDExtensionObjectPtr) -> String?,
    private val parentOf: (String) -> String?,
) {
    private val cache = AtomicReference<Map<String, ClassNode<W>>>(emptyMap())

    /** The node whose wrapper class best matches the real class of [ptr]. Falls back to the root. */
    public fun nodeFor(ptr: GDExtensionObjectPtr): ClassNode<W> {
        val className = classNameOf(ptr)?.takeIf { it.isNotEmpty() } ?: return root
        cache.value[className]?.let { return it }

        val node = descend(className)
        publish(className, node)
        return node
    }

    /** Builds the most derived known wrapper around [ptr]. */
    public fun create(ptr: GDExtensionObjectPtr): W = nodeFor(ptr).create(ptr)

    private fun descend(className: String): ClassNode<W> {
        // Ancestors from the class itself up to the root, then walked back down.
        val chain = ArrayList<String>()
        var current: String? = className
        while (!current.isNullOrEmpty() && chain.size < MAX_DEPTH) {
            if (current == root.godotName) break
            chain += current
            current = parentOf(current)
        }

        var node = root
        for (i in chain.indices.reversed()) {
            node = node.child(chain[i]) ?: break
        }
        return node
    }

    private fun publish(className: String, node: ClassNode<W>) {
        while (true) {
            val old = cache.value
            if (old.containsKey(className)) return
            if (cache.compareAndSet(old, old + (className to node))) return
        }
    }

    private companion object {
        /** The deepest Godot hierarchy is below 10; this only protects against a cyclic `parentOf`. */
        const val MAX_DEPTH = 64
    }
}
