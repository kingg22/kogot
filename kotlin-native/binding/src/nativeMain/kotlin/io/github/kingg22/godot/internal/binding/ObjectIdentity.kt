package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.internal.ffi.*
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
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
 */
private class BindingSlot {
    var ref: WeakReference<GodotObject>? = null
}

private val identityCreateCallback: GDExtensionInstanceBindingCreateCallback =
    staticCFunction { _, _ -> StableRef.create(BindingSlot()).asCPointer() }

/** Disposes a [BindingSlot] created by [identityCreateCallback] or [wrapForEagerBinding]. */
@InternalBinding
public val identityFreeCallback: GDExtensionInstanceBindingFreeCallback =
    staticCFunction { _, _, binding -> binding?.asStableRef<BindingSlot>()?.dispose() }

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
    StableRef.create(BindingSlot().apply { ref = WeakReference(instance) }).asCPointer()

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
    return slotPtr.asStableRef<BindingSlot>().get().ref?.get() as T?
}

/**
 * Gets-or-creates the canonical Kotlin wrapper for [rawPtr]: if one is already cached, it is returned
 * as-is (never fabricates a second Kotlin instance for the same native pointer, see issue #114); only
 * on a genuine first sighting is [factory] invoked.
 *
 * This overload treats [rawPtr] as **borrowed**: it cannot tell a pointer that arrived with an owned
 * engine reference (a ptrcall return of a `Ref<T>`) from one read out of a `Variant`, handed to a
 * callback, or re-cast from a wrapper someone else holds. So a `RefCounted` wrapper built on a first
 * sighting takes its *own* reference (released when the wrapper is collected) and never touches the
 * caller's; a cache hit leaves the engine refcount untouched. A call site that knows it just received
 * an owned reference uses [materializeOwned] to hand that reference over instead.
 */
@InternalBinding
public fun <T : GodotObject> materialize(
    rawPtr: GDExtensionObjectPtr,
    factory: (nativePtr: GDExtensionObjectPtr) -> T,
): T = materializeInternal(rawPtr, owned = false, factory)

/**
 * [materialize] for a [rawPtr] the caller just *received* an owned engine reference for — a ptrcall
 * return of a `Ref<T>`, whose +1 nothing but Kotlin will ever drop (see issue #114).
 *
 * That ownership knowledge only exists at the call site (e.g. `GD.load`), which is why it is a separate
 * entry point rather than something [materialize] could infer. It lets a repeated acquisition of a
 * still-live pointer be settled correctly: on a first sighting the new wrapper adopts the reference
 * (its cleaner releases it); on a cache hit the wrapper already owns *its* reference, so the redundant
 * one is released instead of leaking one engine reference per call ([settleOwnedReference]).
 *
 * The caller guarantees [rawPtr] is a `RefCounted` holding an owned reference it does not otherwise
 * use, and must not call this for a borrowed pointer (one read out of a `Variant`, an engine callback
 * argument, ...): it would drop a reference it never received.
 */
@InternalBinding
public fun <T : GodotObject> materializeOwned(
    rawPtr: GDExtensionObjectPtr,
    factory: (nativePtr: GDExtensionObjectPtr) -> T,
): T = materializeInternal(rawPtr, owned = true, factory)

@OptIn(InternalBinding::class)
@Suppress("UNCHECKED_CAST")
private fun <T : GodotObject> materializeInternal(
    rawPtr: GDExtensionObjectPtr,
    owned: Boolean,
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
    (slot.ref?.get() as T?)?.let { cached ->
        if (owned && cached is RefCounted) settleOwnedReference(cached, rawPtr)
        return@memScoped cached
    }

    val instance = factory(rawPtr)
    slot.ref = WeakReference(instance)
    if (instance is RefCounted) {
        // Owned: the ptrcall return already carried the reference this wrapper adopts. Not owned: the
        // pointer is borrowed from someone else, so the wrapper takes (and later gives back) its own.
        attachRefCountedRelease(instance, rawPtr, takeOwnReference = !owned)
    }
    instance
}
