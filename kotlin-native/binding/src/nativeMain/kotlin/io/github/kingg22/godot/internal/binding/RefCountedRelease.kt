@file:OptIn(InternalBinding::class)

package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.StringName
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.api.internal.callable.CallableFactory
import io.github.kingg22.godot.internal.ffi.GDExtensionMethodBindPtr
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr
import kotlinx.cinterop.memScoped
import kotlin.native.ref.createCleaner

private val unreferenceMethodBind: GDExtensionMethodBindPtr by lazy(LazyThreadSafetyMode.PUBLICATION) {
    StringName("RefCounted").use { cn ->
        StringName("unreference").use { mn ->
            ClassDBBinding.getMethodBindRaw(cn.rawPtr, mn.rawPtr, 2_240_911_060L)
                ?: error("Missing method bind 'RefCounted.unreference'")
        }
    }
}

/**
 * Attaches the release [kotlin.native.ref.Cleaner] a freshly [materialize]d [RefCounted] wrapper needs:
 * `castTo`/`GD.load` wrap an *existing* engine object whose ptrcall return already transferred an owned
 * reference to Kotlin that nothing else will ever drop (see issue #114). Only ever called from
 * [materialize]'s first-sighting branch — a wrapper resolved from the identity cache, or one built by
 * our own `createInstanceFunc` (registered custom classes, whose lifetime already follows Godot's own
 * ClassDB instance/free-instance bookkeeping), never goes through here.
 *
 * The cleaner runs on Kotlin/Native's dedicated finalizer thread once the Kotlin wrapper is
 * unreachable — it deliberately never calls the engine directly from there (that crashed
 * reproducibly, see [io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl.EngineClassImplGen]'s
 * "RefCounted" doc). Instead it only builds a [io.github.kingg22.godot.api.builtin.Callable] and pushes
 * it onto Godot's `MessageQueue` via `callDeferred()` — safe to call from any thread — so the actual
 * `unreference()`/`object_destroy()` runs on the main thread at the next idle frame.
 */
@InternalBinding
public fun attachRefCountedRelease(instance: RefCounted, rawPtr: GDExtensionObjectPtr) {
    instance.kogotReleaseCleaner = createCleaner(rawPtr, ::deferRefCountedRelease)
}

/**
 * Settles one *owned* engine reference the caller just received (a ptrcall return of a `Ref<T>`) for a
 * pointer that already has a canonical Kotlin wrapper — the repeated-acquisition case [materializeOwned]
 * hits (e.g. `GD.load` of the same path while the previous result is still reachable).
 *
 * Invariant kept per live wrapper: it owns **exactly one** engine reference, released by its
 * [attachRefCountedRelease] cleaner when the wrapper is collected. So:
 * - the wrapper already has a cleaner → it already owns its one reference; the new one is redundant and
 *   is released right away ([releaseOwnedReference]), keeping the engine refcount from climbing with
 *   every repeated acquisition;
 * - the wrapper has *no* cleaner yet (a registered custom class whose instance our own
 *   `createInstanceFunc` built eagerly, so [materialize] never saw a first sighting) → nothing keeps
 *   the engine object alive for it, so the new reference is adopted instead of dropped. Dropping it
 *   would let the engine free the object while Kotlin still holds the wrapper.
 */
@InternalBinding
public fun settleOwnedReference(instance: RefCounted, rawPtr: GDExtensionObjectPtr) {
    if (instance.kogotReleaseCleaner == null) {
        attachRefCountedRelease(instance, rawPtr)
    } else {
        releaseOwnedReference(rawPtr)
    }
}

/**
 * Drops one owned engine reference the caller received but will not keep (a redundant repeated
 * acquisition, or a ptrcall return whose cast to the requested class failed), deferred to the main
 * thread exactly like the cleaner path — safe to call from any thread.
 *
 * Only valid for a pointer that really is a `RefCounted` carrying an owned reference: it ends in
 * `RefCounted::unreference()`, which is undefined on any other object.
 */
@InternalBinding
public fun releaseOwnedReference(rawPtr: GDExtensionObjectPtr) {
    deferRefCountedRelease(rawPtr)
}

private fun deferRefCountedRelease(rawPtr: GDExtensionObjectPtr) {
    val callable = CallableFactory.create(arity = 0) { _ ->
        val shouldFree = memScoped {
            val retPtr = allocGdBool()
            ObjectBinding.methodBindPtrcallRaw(unreferenceMethodBind, rawPtr, null, retPtr)
            retPtr.readGdBool()
        }
        if (shouldFree) ObjectBinding.destroyRaw(rawPtr)
        null
    }
    callable.callDeferred()
}
