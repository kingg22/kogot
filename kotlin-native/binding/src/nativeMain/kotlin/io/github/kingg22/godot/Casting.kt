package io.github.kingg22.godot

import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.materialize
import io.github.kingg22.godot.internal.binding.materializeOwned
import io.github.kingg22.godot.internal.binding.releaseOwnedReference
import io.github.kingg22.godot.internal.ffi.GDExtensionObjectPtr

/**
 * `ClassDBBinding.getClassTagRaw` + `ObjectBinding.castToRaw` (GDExtension's `classdb_get_class_tag` /
 * `object_cast_to`) were deprecated in Godot 4.7: "Use the `is_class` method on `Object` to check if an
 * object can be cast instead. If true, the previous pointer can be reinterpreted as a pointer to the
 * target type." There is no pointer-level cast to perform on the Kotlin/Native side either: a
 * [GDExtensionObjectPtr] is already an opaque, type-erased pointer, so "reinterpreting" it is just
 * handing the same pointer value to [factory].
 *
 * [factory] only ever runs for a native pointer's *first* sighting — [materialize] resolves every
 * later cast of the same pointer to the one already-built Kotlin wrapper (see issue #114) instead of
 * fabricating a new one, which for a stateful `@Godot` class would silently discard its live fields.
 *
 * A cast never assumes it was handed an engine reference: for a `RefCounted` the new wrapper takes its own
 * (given back when the wrapper is collected), so casting a pointer borrowed from a `Variant` or a callback
 * argument is safe. A pointer that did arrive with an owned reference (a ptrcall return) leaves that
 * reference with whoever received it — see [castToOwnedInternal] for the call sites that hand it over.
 */
@PublishedApi
@OptIn(InternalBinding::class)
internal fun <Convert : GodotObject> castToInternal(
    rawPtr: GDExtensionObjectPtr,
    godotClassName: String,
    factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert? {
    if (!isInstanceOfGodotClass(rawPtr, godotClassName)) return null

    return materialize(rawPtr, factory)
}

/**
 * [castToInternal] for a [rawPtr] the caller just *received* an owned engine reference for (a ptrcall
 * return of a `Ref<T>`, e.g. `ResourceLoader.load`) — the call site is the only place that knows
 * that, so it opts in here instead of [castToInternal] guessing (see issue #114).
 *
 * The received reference is always settled, so none leaks regardless of the outcome:
 * - first sighting: the new wrapper adopts it (released when the wrapper is collected);
 * - repeated acquisition of a still-live pointer: the wrapper already owns its own reference, the
 *   redundant one is released;
 * - [godotClassName] does not match: the caller gets `null` and nothing would ever drop it, so it is
 *   released here.
 *
 * The caller guarantees [rawPtr] is a `RefCounted` carrying an owned reference (see [materializeOwned]).
 */
@PublishedApi
@OptIn(InternalBinding::class)
internal fun <Convert : GodotObject> castToOwnedInternal(
    rawPtr: GDExtensionObjectPtr,
    godotClassName: String,
    factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert? {
    if (!isInstanceOfGodotClass(rawPtr, godotClassName)) {
        releaseOwnedReference(rawPtr)
        return null
    }

    return materializeOwned(rawPtr, factory)
}

private fun isInstanceOfGodotClass(rawPtr: GDExtensionObjectPtr, godotClassName: String): Boolean =
    godotClassName.toStringName().use { className -> GodotObject(rawPtr).isClass(className) }

// -----------------------------------------------------------------------------
// GDExtensionObjectPtr overloads
// -----------------------------------------------------------------------------

public inline fun <reified Convert : GodotObject> GDExtensionObjectPtr.castTo(
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert = castTo(Convert::class.simpleName!!, factory)

public fun <Convert : GodotObject> GDExtensionObjectPtr.castTo(
    godotClassName: String,
    factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert = castToInternal(this, godotClassName, factory)
    ?: throw ClassCastException("Failed to cast pointer to $godotClassName")

public inline fun <reified Convert : GodotObject> GDExtensionObjectPtr.castToOrNull(
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert? = castToInternal(this, Convert::class.simpleName!!, factory)

public fun <Convert : GodotObject> GDExtensionObjectPtr.castToOrNull(
    godotClassName: String,
    factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert? = castToInternal(this, godotClassName, factory)

// -----------------------------------------------------------------------------
// GodotObject overloads (Actual)
// -----------------------------------------------------------------------------

public inline fun <Actual : GodotObject, reified Convert : GodotObject> Actual.castTo(
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert = rawPtr.castTo(factory)

public inline fun <Actual : GodotObject, reified Convert : GodotObject> Actual.castTo(
    godotClassName: String,
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert = rawPtr.castTo(godotClassName, factory)

public inline fun <Actual : GodotObject, reified Convert : GodotObject> Actual.castToOrNull(
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert? = rawPtr.castToOrNull(factory)

public inline fun <Actual : GodotObject, reified Convert : GodotObject> Actual.castToOrNull(
    godotClassName: String,
    noinline factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert? = rawPtr.castToOrNull(godotClassName, factory)
