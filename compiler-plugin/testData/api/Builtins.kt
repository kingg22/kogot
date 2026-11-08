// Shared stand-in for the generated Godot API (see GodotNative.kt). No calls to annotated APIs in here: diagnostics
// in shared files would fail every test.
package io.github.kingg22.godot.api.builtin

import io.github.kingg22.godot.api.GodotNative

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
annotation class MustBeVariant

/** Every builtin is a final GodotNative + AutoCloseable class, like the generated ones. */
abstract class Builtin : GodotNative, AutoCloseable {
    override fun close() {}
}

class Variant : Builtin() { companion object }
class GodotString : Builtin()
class StringName : Builtin()
class NodePath : Builtin()
class Vector2 : Builtin()
class Vector2i : Builtin()
class Rect2 : Builtin()
class Rect2i : Builtin()
class Vector3 : Builtin()
class Vector3i : Builtin()
class Vector4 : Builtin()
class Vector4i : Builtin()
class Transform2D : Builtin()
class Transform3D : Builtin()
class Plane : Builtin()
class Quaternion : Builtin()
class Aabb : Builtin()
class Basis : Builtin()
class Projection : Builtin()
class Color : Builtin()
class Rid : Builtin()
class Signal : Builtin()
class Dictionary<K, V> : Builtin()
class GodotArray<T> : Builtin()
class PackedByteArray : Builtin()
class PackedInt32Array : Builtin()
class PackedInt64Array : Builtin()
class PackedFloat32Array : Builtin()
class PackedFloat64Array : Builtin()
class PackedStringArray : Builtin()
class PackedVector2Array : Builtin()
class PackedVector3Array : Builtin()
class PackedVector4Array : Builtin()
class PackedColorArray : Builtin()

typealias AABB = Aabb
typealias RID = Rid

/** Zero arguments is a constructor; 1+ arguments are typed top-level factories (generated shape, arity 1..3 here). */
class Callable() : Builtin() {
    constructor(lambda: () -> @MustBeVariant Any?) : this()
}

inline fun <reified @MustBeVariant P1, @MustBeVariant R> Callable(
    noinline lambda: (@MustBeVariant P1) -> @MustBeVariant R,
): Callable = Callable()

inline fun <reified @MustBeVariant P1, reified @MustBeVariant P2, @MustBeVariant R> Callable(
    noinline lambda: (@MustBeVariant P1, @MustBeVariant P2) -> @MustBeVariant R,
): Callable = Callable()

inline fun <reified @MustBeVariant P1, reified @MustBeVariant P2, reified @MustBeVariant P3, @MustBeVariant R> Callable(
    noinline lambda: (@MustBeVariant P1, @MustBeVariant P2, @MustBeVariant P3) -> @MustBeVariant R,
): Callable = Callable()

fun anyToVariant(element: @MustBeVariant Any?): Variant = Variant()

inline fun <reified T> Variant.Companion.from(element: @MustBeVariant T): Variant = Variant()

fun <@MustBeVariant T> variantOf(): Variant = Variant()

/** Return position only: reading a value declares no call-site contract. */
val Variant.value: @MustBeVariant Any? get() = null

fun notAnnotated(element: Any?): Variant = Variant()
