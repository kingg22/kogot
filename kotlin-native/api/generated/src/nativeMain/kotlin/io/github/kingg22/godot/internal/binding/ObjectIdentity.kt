package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.internal.ffi.*
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlin.concurrent.AtomicReference
import kotlin.native.ref.WeakReference

/**
 * One universal identity slot per native [GDExtensionObjectPtr], reachable from Kotlin via Godot's
 * generic instance-binding mechanism (`object_get_instance_binding`, keyed by [BindingProcAddressHolder.library]).
 *
 * This is the single cache every entry point that turns a raw pointer into a Kotlin wrapper must go
 * through — [castTo][io.github.kingg22.godot.castTo], `GD.load`, and [instantiate][io.github.kingg22.godot.binding.instantiate] —
 * so that a given Godot object is never represented by more than one live Kotlin instance at a time
 * (see issue #114). The reference is `weak`: for a kogot-registered custom class the object already
 * has an independent, always-alive [StableRef] via `object_set_instance` (required by ClassDB for
 * instance/virtual dispatch), so this slot merely mirrors it; for an engine-only wrapper (a `Resource`
 * from `ResourceLoader`, a `Node` from `get_node()`, ...) there is no other owner, and a weak reference
 * lets Kotlin's GC reclaim the wrapper once nothing references it, rebuilding it lazily on next access.
 *
 * This slot fixes *identity* (never fabricate a second Kotlin instance for one native pointer), and
 * [materialize] additionally attaches [attachRefCountedRelease] for a `RefCounted` wrapper, to release
 * the engine reference implicitly received by whoever handed us the pointer (a ptrcall return, a
 * property read) — see issue #114. An earlier version of that release drove it directly off a
 * `kotlin.native.ref.Cleaner`, but Cleaners run on Kotlin/Native's dedicated finalizer thread, and
 * Godot's engine calls are not generally safe off the main thread — that version reproducibly crashed
 * (`ERROR: The caller thread can't call the function 'propagate_notification()' on this node. Use
 * 'call_deferred()'...`, SIGSEGV) as soon as a shared `Resource`'s wrapper was collected while the main
 * thread was still using the same object. [attachRefCountedRelease] fixes this by never calling the
 * engine from the finalizer thread — it only queues the release via `Callable.callDeferred()`.
 *
 * ## Threads (issue #113)
 *
 * The same object can be materialized from several threads at once (a `Resource` handed to a worker while the
 * main thread reads it, `_process` of a `SUB_THREAD` node). Godot creates the slot itself only once, under the
 * object's own instance-binding mutex; the wrapper inside it is published with a compare-and-set, so two racing
 * first sightings still agree on one canonical wrapper, and a wrapper is only published once its engine
 * reference is settled (no other thread ever sees a `RefCounted` wrapper that does not own its reference yet).
 */
private class BindingSlot(initial: WeakReference<GodotObject>? = null) {
    private val ref = AtomicReference(initial)

    /** The canonical wrapper, or `null` when there is none yet or the previous one was collected. */
    fun current(): GodotObject? = ref.value?.get()

    /**
     * Publishes [candidate] unless a live wrapper is already there, and returns the canonical one: [candidate]
     * when it won, the other thread's wrapper when that one got there first.
     */
    fun publish(candidate: GodotObject): GodotObject {
        val mine = WeakReference(candidate)
        while (true) {
            val seen = ref.value
            seen?.get()?.let { return it }
            if (ref.compareAndSet(seen, mine)) return candidate
        }
    }

    /** Unconditionally makes [instance] the canonical wrapper (it was just constructed, so nothing else knows it). */
    fun replace(instance: GodotObject) {
        ref.value = WeakReference(instance)
    }
}

private val identityCreateCallback: GDExtensionInstanceBindingCreateCallback =
    staticCFunction { _, _ ->
        guardCallback("kogot.instanceBinding.create", null) { StableRef.create(BindingSlot()).asCPointer() }
    }

/**
 * Disposes a [BindingSlot] created by [identityCreateCallback] or [wrapForEagerBinding], and forgets any engine
 * reference Kotlin still owed for the destroyed object ([PendingReleases.forget], issue #218).
 */
@InternalBinding
public val identityFreeCallback: GDExtensionInstanceBindingFreeCallback =
    staticCFunction { _, instance, binding ->
        guardCallback("kogot.instanceBinding.free") {
            // The object is gone: a reference Kotlin still owed for it must never be released (issue #218).
            if (instance != null) PendingReleases.forget(instance)
            binding?.asStableRef<BindingSlot>()?.dispose()
        }
    }

@OptIn(InternalBinding::class)
private fun identityCallbacks() = cValue<GDExtensionInstanceBindingCallbacks> {
    create_callback = identityCreateCallback
    free_callback = identityFreeCallback
    reference_callback = null
}

/**
 * Wraps a freshly-constructed custom-class [instance] for storage as the *binding* payload of
 * `object_set_instance_binding`, called once by [createInstanceFunc] right after the instance is built.
 *
 * The object's real lifetime is already governed by the separate `object_set_instance` [StableRef]
 * ClassDB requires; this slot only lets [castTo][io.github.kingg22.godot.castTo]/`GD.load`/
 * [instantiate][io.github.kingg22.godot.binding.instantiate] resolve back to the *same* Kotlin
 * instance instead of fabricating a second one — the mirror is safe to be weak.
 */
@InternalBinding
public fun wrapForEagerBinding(instance: GodotObject): COpaquePointer =
    StableRef.create(BindingSlot(WeakReference(instance))).asCPointer()

/**
 * Resolves [rawPtr] to its canonical Kotlin wrapper if one has already been materialized, without
 * creating a new binding slot as a side effect of looking. Trusts the caller on [T], the same way
 * [getInstance] already does for `object_set_instance` data — the class-name check that guarantees
 * this happens in [io.github.kingg22.godot.castToInternal] before either is ever reached.
 */
@InternalBinding
@Suppress("UNCHECKED_CAST")
public fun <T : GodotObject> resolveBinding(rawPtr: GDExtensionObjectPtr): T? {
    val slotPtr = ObjectBinding.getInstanceBindingRaw(
        pO = rawPtr,
        pToken = BindingProcAddressHolder.library,
        pCallbacks = null,
    ) ?: return null
    return slotPtr.asStableRef<BindingSlot>().get().current() as T?
}

/**
 * Gets-or-creates the canonical Kotlin wrapper for [rawPtr]: if one is already cached, it is returned
 * as-is (never fabricates a second Kotlin instance for the same native pointer, see issue #114); only
 * on a genuine first sighting is [factory] invoked.
 *
 * The pointer is treated as **borrowed**: a cast cannot tell one that arrived with an owned engine
 * reference (a ptrcall return of a `Ref<T>`) from one read out of a `Variant`, handed to a callback, or
 * re-cast from a wrapper someone else holds. So a `RefCounted` wrapper built on a first sighting takes its
 * *own* reference (released when the wrapper is collected) and never touches the caller's; a cache hit
 * leaves the engine refcount untouched. A reference that came with a ptrcall return is owned by the
 * wrapper the generated method built around it (`adoptReturnedReference`), not by the cast.
 */
@InternalBinding
public fun <T : GodotObject> materialize(
    rawPtr: GDExtensionObjectPtr,
    factory: (nativePtr: GDExtensionObjectPtr) -> T,
): T = materializeImpl(rawPtr, ownsReference = false, factory)

/**
 * For the generated public constructors (`Sprite2D()`, `Resource()`): a new object of the engine class [className],
 * built with `classdb_construct_object3`. Unlike `ClassDB.instantiate` there is no `Variant` in between to free
 * (issue #214); a `RefCounted` comes back with a refcount of 1 that the caller owns ([adoptConstructedReference]).
 * Complete it with [finishConstruction].
 *
 * @throws IllegalStateException when the engine builds nothing: an abstract or unknown class (the engine reports why).
 */
@InternalBinding
public fun constructEngineObject(className: String): GDExtensionObjectPtr {
    val ptr = className.toStringName().use { ClassDBBinding.constructObject3Raw(it.rawPtr) }
    return checkNotNull(ptr) { "Cannot construct $className: the engine refused to (abstract or unknown class)" }
}

/**
 * Last step of a generated public constructor (see [constructEngineObject]): makes [instance] the canonical wrapper of
 * its object ([bindConstructed]) and sends `NOTIFICATION_POSTINITIALIZE`, which `ClassDB.instantiate` sends itself
 * and `classdb_construct_object3` leaves to its caller.
 */
@InternalBinding
public fun finishConstruction(instance: GodotObject) {
    bindConstructed(instance)
    instance.notification(GodotObject.NOTIFICATION_POSTINITIALIZE.toInt())
}

/**
 * The wrapper that was just built around a freshly created object becomes its canonical one, so a later read of the
 * same object from a `Variant`, a signal or an engine return gives this very instance instead of a second one. The
 * reference it owns is already settled by the constructor ([adoptConstructedReference] for a `RefCounted`), so this
 * only fills the slot.
 */
private fun bindConstructed(instance: GodotObject): Unit = memScoped {
    val slotPtr = requireNotNull(
        ObjectBinding.getInstanceBindingRaw(
            pO = instance.rawPtr,
            pToken = BindingProcAddressHolder.library,
            pCallbacks = identityCallbacks().ptr,
        ),
    ) { "object_get_instance_binding returned null for ${instance.rawPtr}" }
    slotPtr.asStableRef<BindingSlot>().get().replace(instance)
}

/**
 * Like [materialize], for a pointer that arrived with **one owned engine reference** (a ptrcall return of a
 * `Ref<T>`, see [adoptReturnedReference]): a new `RefCounted` wrapper adopts that reference instead of taking
 * its own, and when the object already has a live wrapper (which owns one reference already) the surplus one
 * is given back right away, so a repeated read never grows the engine refcount.
 */
@InternalBinding
public fun <T : GodotObject> materializeOwned(
    rawPtr: GDExtensionObjectPtr,
    factory: (nativePtr: GDExtensionObjectPtr) -> T,
): T = materializeImpl(rawPtr, ownsReference = true, factory)

@Suppress("UNCHECKED_CAST")
private fun <T : GodotObject> materializeImpl(
    rawPtr: GDExtensionObjectPtr,
    ownsReference: Boolean,
    factory: (nativePtr: GDExtensionObjectPtr) -> T,
): T = memScoped {
    val slotPtr = requireNotNull(
        ObjectBinding.getInstanceBindingRaw(
            pO = rawPtr,
            pToken = BindingProcAddressHolder.library,
            pCallbacks = identityCallbacks().ptr,
        ),
    ) { "object_get_instance_binding returned null for $rawPtr" }

    val slot = slotPtr.asStableRef<BindingSlot>().get()
    val existing = slot.current() as T?
    if (existing != null) {
        if (!ownsReference || existing !is RefCounted) return@memScoped existing
        if (existing.kogotReleaseCleaner != null) {
            // The wrapper owns its reference already: the one that came with the return is surplus.
            dropReturnedReference(rawPtr)
            return@memScoped existing
        }
        // Only reached for a RefCounted wrapper that owns no reference of its own, which since #213 (every `@Godot`
        // RefCounted instance owns one) only happens when it was built after shutdown started. There is nothing to
        // merge the returned reference into: a separate wrapper adopts it and gives it back when collected. The
        // canonical instance stays in the slot.
        return@memScoped factory(rawPtr).also {
            if (it is RefCounted) attachRefCountedRelease(it, rawPtr, takeOwnReference = false)
        }
    }

    val instance = factory(rawPtr)
    // Settle the reference before publishing: another thread that sees this wrapper in the slot must see one
    // that already owns its reference (see BindingSlot). If a racing first sighting published its wrapper
    // first, that one is canonical; this one keeps the reference it settled and gives it back when collected,
    // so the engine refcount stays balanced either way.
    if (instance is RefCounted) {
        attachRefCountedRelease(instance, rawPtr, takeOwnReference = !ownsReference)
    }
    slot.publish(instance) as T
}
