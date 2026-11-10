package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr
import kotlin.native.ref.createCleaner

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
 *
 * The reference is also recorded in [PendingReleases], so whatever is still owed when the extension shuts
 * down is settled synchronously by [releaseAllPendingRefCountedReferences] instead of leaking: a wrapper
 * that is still reachable at exit never fires its cleaner.
 */
@InternalBinding
public fun attachRefCountedRelease(instance: RefCounted, rawPtr: GDExtensionObjectPtr) {
    // Once shutdown started the drain already settled everything and the engine must not be touched.
    if (!PendingReleases.owe(rawPtr)) return
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
    if (!PendingReleases.owe(rawPtr)) return
    deferRefCountedRelease(rawPtr)
}

/**
 * Settles, synchronously, every engine reference Kotlin still owes (see [PendingReleases]) and stops
 * tracking: from here on the cleaners and any release still queued in Godot's `MessageQueue` become
 * no-ops, so nothing calls into a half-destroyed engine afterwards.
 *
 * Godot has no GC, so a reference Kotlin still holds at exit is reported as `ObjectDB instances were
 * leaked at exit` / `resources still in use` / `RID allocations ... leaked`. Must run on the main thread at
 * the `SCENE` deinitialization level, **before** any user class is unregistered (Godot refuses to
 * unregister a class that still has live instances) and before the servers go away (a `Texture2D` frees its
 * RID through the `RenderingServer`). The Kogot processor generates that call into `onDeInitScene`; a
 * hand-written entry point must make it itself.
 */
@InternalBinding
public fun releaseAllPendingRefCountedReferences() {
    for ((ptr, count) in PendingReleases.drain()) {
        for (i in 0 until count) {
            // Destroyed means that was the last reference: any further unreference() would be a use-after-free.
            if (unreferenceAndDestroyIfLast(ptr)) break
        }
    }
}

/** Main thread only. Returns `true` when that was the last reference and the object was destroyed. */
private fun unreferenceAndDestroyIfLast(rawPtr: GDExtensionObjectPtr): Boolean {
    val shouldFree = RefCounted(rawPtr).unreference()
    if (shouldFree) ObjectBinding.destroyRaw(rawPtr)
    return shouldFree
}

/**
 * Safe from any thread; the `unreference()` itself runs on the main thread, and only if still owed. The
 * `Callable` handle is closed right after queueing: the message queue keeps its own copy, and without the
 * close the handle (and the lambda it keeps alive) would never be freed.
 */
private fun deferRefCountedRelease(rawPtr: GDExtensionObjectPtr) {
    if (!PendingReleases.beginEnqueue()) return
    try {
        Callable {
            // Already settled by releaseAllPendingRefCountedReferences() if shutdown got there first.
            if (PendingReleases.settle(rawPtr)) {
                val _ = unreferenceAndDestroyIfLast(rawPtr)
            }
            null
        }.use { it.callDeferred() }
    } finally {
        PendingReleases.endEnqueue()
    }
}
