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

/**
 * For the generated public constructors of `RefCounted` classes: makes [instance] the owner of the reference that
 * `classdb_construct_object3` hands to the creator ([constructEngineObject]). If shutdown already started, [PendingReleases] no longer accepts new
 * entries and nothing would ever give that reference back, so it is released right here instead of leaking.
 * Using a wrapper constructed while the extension is shutting down is not supported either way.
 */
@InternalBinding
public fun adoptConstructedReference(instance: RefCounted) {
    val rawPtr = instance.rawPtr
    if (!PendingReleases.owe(rawPtr)) {
        val _ = unreferenceAndDestroyIfLast(rawPtr)
        return
    }
    instance.kogotReleaseCleaner = createCleaner(rawPtr, ::deferRefCountedRelease)
}

/**
 * Gives back the engine reference that came with a ptrcall `Ref<T>` return when the object's wrapper already
 * owns one of its own (see [materializeOwned]).
 *
 * Runs on whatever thread received the return (a worker during a threaded load, for example), right where the
 * reference arrived: that is what Godot's own `Ref<T>` destructor does with a surplus reference, and
 * `RefCounted::unreference()` is atomic. Unlike the cleaner's release it needs no hop to the main thread, because
 * the receiving thread is already legitimately using the object.
 */
@InternalBinding
public fun dropReturnedReference(rawPtr: GDExtensionObjectPtr) {
    val _ = unreferenceAndDestroyIfLast(rawPtr)
}

/**
 * Gives back the reference that `classdb_construct_object3` hands to whoever creates an object, when that creator is
 * Kotlin itself (`instantiate<T>()`, the binding's own script classes) rather than the engine. The `@Godot` instance
 * already owns its own reference (taken by `createInstanceFunc`), so the object stays alive as long as Kotlin uses
 * it; without this release the creation reference would keep it alive forever (issue #213).
 */
@InternalBinding
public fun releaseCreationReference(rawPtr: GDExtensionObjectPtr) {
    val _ = unreferenceAndDestroyIfLast(rawPtr)
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
    // Reported, not refused: skipping the drain would leak every owed reference, which is worse than settling them.
    val _ = MainThread.check("releaseAllPendingRefCountedReferences")
    for ((ptr, count) in PendingReleases.drain()) {
        for (i in 0 until count) {
            // Destroyed means that was the last reference: any further unreference() would be a use-after-free.
            if (unreferenceAndDestroyIfLast(ptr)) break
        }
    }
}

/**
 * Gives back one engine reference, destroying the object if it was the last one. Returns `true` when it was.
 *
 * Callers pick the thread: the deferred release and the shutdown drain run it on the main thread, and
 * [dropReturnedReference] on the thread that just received the surplus reference (see there). It is never called from
 * the cleaner thread.
 */
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
