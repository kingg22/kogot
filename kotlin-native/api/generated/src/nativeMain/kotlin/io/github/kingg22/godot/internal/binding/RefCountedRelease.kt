package io.github.kingg22.godot.internal.binding

import io.github.kingg22.godot.api.builtin.Callable
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr
import kotlin.native.ref.createCleaner

/**
 * Makes [instance] the owner of exactly one engine reference on [rawPtr], given back when the wrapper is
 * collected (issue #114). Godot has no GC, so without this every wrapper around a `RefCounted` leaks the
 * reference it holds.
 *
 * Where that one reference comes from depends on how the pointer reached Kotlin:
 * - [takeOwnReference] `false` **adopts** the reference a ptrcall return already transferred — a method
 *   whose result is a `Ref<T>` hands the caller a +1 that nothing but Kotlin will ever drop (see
 *   [adoptReturnedReference], which every generated `Ref<T>`-returning method goes through);
 * - [takeOwnReference] `true` is for a *borrowed* pointer (one read out of a `Variant`, an engine callback
 *   argument, a re-cast of something another owner holds) that carried no reference of its own, so the
 *   wrapper takes one itself with `RefCounted::reference()` — adopting nothing there would make the cleaner
 *   `unreference()` a reference that was never taken and let the engine free the object under its real owner.
 *
 * The cleaner runs on Kotlin/Native's dedicated finalizer thread once the wrapper is unreachable. It
 * deliberately never calls the engine directly from there (that crashed reproducibly: engine calls are not
 * generally safe off the main thread). It only builds a [io.github.kingg22.godot.api.builtin.Callable] and
 * pushes it onto Godot's `MessageQueue` via `callDeferred()` — safe from any thread — so the actual
 * `unreference()` / `object_destroy()` runs on the main thread at the next idle frame.
 *
 * The reference is also recorded in [PendingReleases], so whatever is still owed when the extension shuts
 * down is settled synchronously by [releaseAllPendingRefCountedReferences] instead of leaking: a wrapper
 * that is still reachable at exit never fires its cleaner.
 */
@InternalBinding
public fun attachRefCountedRelease(
    instance: RefCounted,
    rawPtr: GDExtensionObjectPtr,
    takeOwnReference: Boolean = false,
) {
    // Once shutdown started the drain already settled everything and the engine must not be touched.
    if (!PendingReleases.owe(rawPtr)) return
    if (takeOwnReference) takeReference(rawPtr)
    instance.kogotReleaseCleaner = createCleaner(rawPtr, ::deferRefCountedRelease)
}

/**
 * Wraps the result of a generated method that returns a `Ref<T>`: the ptrcall just handed the caller one
 * owned engine reference, and this makes [instance] the owner that gives it back. Emitted by the code
 * generator around the wrapper it builds for every `RefCounted`-derived return type — without it each
 * `ResourceLoader.load`, `Texture2D.getImage`, property getter, ... leaked one reference per call.
 *
 * Only for a wrapper built straight from a fresh ptrcall return value: it adopts a reference instead of
 * taking one, so using it on a borrowed pointer would give back a reference that was never taken.
 */
@InternalBinding
public fun <T : RefCounted> adoptReturnedReference(instance: T): T {
    attachRefCountedRelease(instance, instance.rawPtr, takeOwnReference = false)
    return instance
}

/** Takes one more engine reference on [rawPtr] (`RefCounted::reference()`); safe from any thread, the count is atomic. */
private fun takeReference(rawPtr: GDExtensionObjectPtr) {
    val _ = RefCounted(rawPtr).reference()
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
