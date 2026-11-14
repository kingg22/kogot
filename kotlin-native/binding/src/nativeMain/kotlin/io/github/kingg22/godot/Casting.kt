package io.github.kingg22.godot

import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.internal.binding.InternalBinding
import io.github.kingg22.godot.internal.binding.materialize
import io.github.kingg22.godot.internal.binding.wrapperOf
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
 * reference with the wrapper the generated method built around it, which gives it back when collected.
 */
@PublishedApi
@OptIn(InternalBinding::class)
internal fun <Convert : GodotObject> castToInternal(
    rawPtr: GDExtensionObjectPtr,
    godotClassName: String,
    factory: (nativePtr: GDExtensionObjectPtr) -> Convert,
): Convert? {
    val isRequestedClass = godotClassName.toStringName().use { className ->
        GodotObject(rawPtr).isClass(className)
    }
    if (!isRequestedClass) return null

    return materialize(rawPtr, factory)
}

/** The canonical wrapper of the most derived class of [rawPtr]; the factory-less casts below are plain Kotlin `as?` on it. */
@PublishedApi
@OptIn(InternalBinding::class)
internal fun wrapperOfPtr(rawPtr: GDExtensionObjectPtr): GodotObject = wrapperOf(rawPtr)

// -----------------------------------------------------------------------------
// Factory-less casts: same meaning as Kotlin's `as` / `as?`
// -----------------------------------------------------------------------------

/**
 * Like `as? Convert` on the object behind this pointer: it is [Convert] when the object really is one (or a
 * subclass), and `null` when not. No factory is needed, the wrapper already is of the object's most derived
 * class and is the same instance every time (see issue #206).
 */
public inline fun <reified Convert : GodotObject> GDExtensionObjectPtr.castToOrNull(): Convert? =
    wrapperOfPtr(this) as? Convert

/** Like `as Convert`: [ClassCastException] when the object is not a [Convert]. */
public inline fun <reified Convert : GodotObject> GDExtensionObjectPtr.castTo(): Convert =
    castToOrNull<Convert>() ?: throw ClassCastException("Failed to cast pointer to ${Convert::class.simpleName}")

/** Like `as? Convert` on this object; a different [Convert] than its real class gives `null`. */
public inline fun <reified Convert : GodotObject> GodotObject.castToOrNull(): Convert? = rawPtr.castToOrNull<Convert>()

/** Like `as Convert` on this object. */
public inline fun <reified Convert : GodotObject> GodotObject.castTo(): Convert = rawPtr.castTo<Convert>()

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
