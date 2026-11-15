package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.internal.ffi.GDExtensionClassInstancePtr
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlin.concurrent.AtomicReference
import kotlin.contracts.contract
import kotlin.native.ref.WeakReference

/**
 * The per-instance data a `@Godot` class instance is registered with (`object_set_instance`), held in a [StableRef]
 * for the whole native life of the object and disposed by `free_instance` (issue #114).
 *
 * It holds the Kotlin instance strongly or weakly:
 * - **strong** while the engine has a use for the object: every non-`RefCounted` object, and a `RefCounted` whose
 *   engine refcount is above 1. The engine can call into the instance at any time, so it must not be collected.
 * - **weak** while a `RefCounted`'s only reference is the one the Kotlin instance owns itself (refcount 1). Nothing
 *   but Kotlin code can reach the object then, so the GC is free to collect the instance; its cleaner gives the
 *   reference back and the engine frees the object. A strong link here would be a cycle that never dies (the object
 *   keeps the instance alive, the instance keeps its reference on the object).
 *
 * This is the scheme of Godot's own C# module (`CSharpLanguage::_instance_binding_reference_callback`), driven by the
 * class's `reference_func` / `unreference_func`, which Godot calls on the 1↔2 refcount transitions, from any thread.
 * Like C#, an instance that was already collected when the engine references the object again cannot come back: the
 * object then lives on without its Kotlin half until it dies, and callbacks into it report an error.
 */
@InternalBinding
public class InstanceHandle(instance: Any, public val rawPtr: GDExtensionObjectPtr) {
    private val weak = WeakReference(instance)
    private val strong = AtomicReference<Any?>(instance)

    /** The Kotlin instance, or `null` if it was weak and has been collected. */
    public val instance: Any? get() = strong.value ?: weak.get()

    /** Holds the instance strongly again. Returns `false` when it was already collected (nothing to hold). */
    public fun makeStrong(): Boolean {
        val target = weak.get() ?: return false
        strong.value = target
        return true
    }

    /** Lets the GC collect the instance once nothing in Kotlin references it. */
    public fun makeWeak() {
        strong.value = null
    }
}

/**
 * Creates the per-instance data of a freshly constructed `@Godot` class [instance] (strong; see [InstanceHandle]).
 * The returned pointer is what `object_set_instance` stores and every trampoline receives as its instance pointer.
 */
@InternalBinding
public fun createInstanceData(instance: Any, rawPtr: GDExtensionObjectPtr): COpaquePointer =
    StableRef.create(InstanceHandle(instance, rawPtr)).asCPointer()

/**
 * The Kotlin object behind an instance pointer or a callable userdata: the target of an [InstanceHandle] (a `@Godot`
 * class instance) or the object of a plain [StableRef] (a `KotlinCallable`, a script instance's target). `null` for a
 * `null` pointer or a collected weak instance.
 */
@InternalBinding
public fun COpaquePointer?.instanceOrNull(): Any? {
    val target = this?.asStableRef<Any>()?.get() ?: return null
    return if (target is InstanceHandle) target.instance else target
}

/**
 * Inline function to get an instance from a [GDExtensionClassInstancePtr] ([COpaquePointer]) that is assumed to be a
 * [StableRef], either of an [InstanceHandle] or of the object itself (see [instanceOrNull]).
 *
 * @throws [IllegalArgumentException] if it's `null`, or if it is a `@Godot` instance that was already collected.
 */
@InternalBinding
public inline fun <reified T : Any> GDExtensionClassInstancePtr?.getInstance(): T {
    contract { returns() implies (this@getInstance != null) }
    val target = requireNotNull(requireNotNull(this).instanceOrNull()) {
        "The Kotlin instance behind $this was already collected (see InstanceHandle)"
    }
    return target as T
}
