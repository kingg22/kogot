@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class, InternalBinding::class)

package io.github.kingg22.godot.test.refcounted

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.RefCounted
import io.github.kingg22.godot.api.core.refcounted.Image
import io.github.kingg22.godot.api.singleton.ClassDB
import io.github.kingg22.godot.api.utils.GD
import io.github.kingg22.godot.binding.instantiate
import io.github.kingg22.godot.castTo
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.test.downcast.DowncastResource
import io.github.kingg22.godot.test.leak.settleCleaners
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Issue #114 / #213: a wrapper around a `RefCounted` owns exactly one engine reference, however the object reached
 * Kotlin: a generated method returning a `Ref<T>`, a public constructor, a cast of a pointer somebody else owns, or
 * `instantiate<T>()` of a `@Godot` class. Ported from the former `TestOne` scene.
 *
 * The reference is given back by a Cleaner once the wrapper is collected, through the engine's message queue at an
 * idle frame. A test is synchronous and no frame runs while it executes, so what is checked here is the count while
 * the wrapper is alive.
 *
 * Not ported: the object counts that must come back down after the collection (generated returns, constructors,
 * dropped `@Godot` instances), the refcount of a repeatedly loaded `res://icon.svg`, and the `loadOrNull` whose cast
 * fails: they need idle frames after the collection, or assets that the test host project does not have. Repeated
 * reads of one live object are covered by `DowncastTest`, and the memory growth by `NativeLeakTest`.
 */
class RefCountedOwnershipTest {
    @Test
    fun generatedMethodReturningARefOwnsTheReturnedReference() {
        val image = Image.createEmpty(8, 8, false, Image.Format.RGBA8)
        assertEquals(1, image.getReferenceCount(), "refcount=${image.getReferenceCount()}")
    }

    @Test
    fun publicConstructorOwnsTheReferenceTheObjectIsCreatedWith() {
        val image = Image()
        assertEquals(1, image.getReferenceCount(), "refcount=${image.getReferenceCount()}")
    }

    /**
     * `castTo` on a pointer the caller holds no reference to. A `Variant` is the only real owner of an `Image` here
     * (`ClassDB.instantiate` hands it back wrapped in one). The cast wrapper must take its *own* engine reference: if
     * it assumed it had been handed one, its cleaner would `unreference()` a reference it never took and the engine
     * would free the `Image` underneath the `Variant`.
     */
    @Test
    fun castOfABorrowedPointerTakesItsOwnReference() {
        val owner = "Image".toStringName().use { ClassDB.instance.instantiate(it) }
        try {
            // The raw pointer, without a wrapper: `toObject()` would build the canonical wrapper (which owns a
            // reference of its own since #206) and the cast below would then reuse it instead of taking one.
            val ptr = owner.toObjectRawPtr()
            val baseline = RefCounted(ptr).getReferenceCount()
            val cast = ptr.castTo<Image>()
            val withWrapper = RefCounted(ptr).getReferenceCount()
            assertEquals(baseline + 1, withWrapper, "baseline=$baseline, with wrapper=$withWrapper")
            assertEquals(ptr, cast.rawPtr, "the cast built another object")
        } finally {
            owner.close()
        }
    }

    /** #213: a `@Godot` RefCounted made from Kotlin owns exactly one reference: its own, not the creation one. */
    @Test
    fun godotRefCountedMadeFromKotlinOwnsExactlyItsOwnReference() {
        val kept = instantiate<DowncastResource>()
        assertEquals(1, kept.getReferenceCount(), "refcount=${kept.getReferenceCount()}")
    }

    /** #213: the instance Kotlin still holds survives the collections caused by the ones it dropped. */
    @Test
    fun godotRefCountedStillHeldSurvivesTheCollectionOfDroppedOnes() {
        val kept = instantiate<DowncastResource>()
        createAndDropGodotRefCounted()
        settleCleaners()
        assertTrue(GD.isInstanceIdValid(kept.getInstanceId().toLong()), "the kept instance was freed")
        assertEquals(1, kept.getReferenceCount(), "refcount=${kept.getReferenceCount()}")
    }

    /** In its own frame so none of the instances stays reachable from a local of the caller. */
    private fun createAndDropGodotRefCounted() {
        repeat(20) {
            val _ = instantiate<DowncastResource>().getInstanceId()
        }
    }
}
