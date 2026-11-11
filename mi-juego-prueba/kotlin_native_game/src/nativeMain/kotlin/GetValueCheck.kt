@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.core.RefCounted
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Live checks of `Variant.getValue` / `getValueOrNull` / `by variant` (`get_value_check.tscn`): every supported type,
 * the numeric narrowing limits, nullable types, mismatches and what each one throws. It does not touch the collections:
 * those are `CollectionsCheck`.
 */
@Godot
class GetValueCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("GetValueCheck")

    override fun _ready() {
        scalars()
        strings()
        narrowing()
        nullables()
        mismatches()
        builtins()
        packedArrays()
        containersAndObjects()
        delegates()
        getTree().quit(checks.finish())
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────────────────────────────

    /** Sends [value] to a `Variant`, checks its `Variant.Type`, and reads it back as the same type. */
    private inline fun <@MustBeVariant reified T> Checks.roundTrip(name: String, value: T, type: Variant.Type) =
        check("roundtrip $name") {
            Variant.from(value).use { variant ->
                expect(variant.getType() == type) { "type=${variant.getType()}, expected $type" }
                val back = variant.getValue<T>()
                expect(back == value) { "read back $back, sent $value" }
                (back as? AutoCloseable)?.close()
            }
            (value as? AutoCloseable)?.close()
        }

    // ── supported scalars ─────────────────────────────────────────────────────────────────────────────────────────

    private fun scalars() = with(checks) {
        roundTrip("Boolean true", true, Variant.Type.BOOL)
        roundTrip("Boolean false", false, Variant.Type.BOOL)
        for (long in listOf(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            roundTrip(
                "Long $long",
                long,
                Variant.Type.INT,
            )
        }
        for (int in listOf(0, 7, -7, Int.MAX_VALUE, Int.MIN_VALUE)) roundTrip("Int $int", int, Variant.Type.INT)
        for (short: Short in listOf(0, 7, -7, Short.MAX_VALUE, Short.MIN_VALUE)) {
            roundTrip("Short $short", short, Variant.Type.INT)
        }
        for (double in listOf(0.0, 1.5, -1.5, Double.MAX_VALUE, Double.MIN_VALUE, Double.POSITIVE_INFINITY)) {
            roundTrip("Double $double", double, Variant.Type.FLOAT)
        }
        roundTrip("Double -Infinity", Double.NEGATIVE_INFINITY, Variant.Type.FLOAT)
        for (float in listOf(0f, 1.5f, -1.5f, 0.1f, Float.MAX_VALUE, Float.MIN_VALUE, Float.POSITIVE_INFINITY)) {
            roundTrip("Float $float", float, Variant.Type.FLOAT)
        }
        check("roundtrip Double NaN stays NaN") {
            Variant.from(Double.NaN).use { expect(it.getValue<Double>().isNaN()) { "not NaN" } }
        }
        check("roundtrip Float NaN stays NaN") {
            Variant.from(Float.NaN).use { expect(it.getValue<Float>().isNaN()) { "not NaN" } }
        }
        check("roundtrip Double -0.0 keeps its sign") {
            Variant.from(-0.0).use { expect(it.getValue<Double>().toRawBits() == (-0.0).toRawBits()) { "sign lost" } }
        }
        check("an Int and a Short are stored as 64-bit INT, so they also read as Long") {
            Variant.from(Int.MIN_VALUE).use { expect(it.getValue<Long>() == Int.MIN_VALUE.toLong()) { "Int as Long" } }
            Variant.from(Short.MAX_VALUE).use { expect(it.getValue<Long>() == Short.MAX_VALUE.toLong()) { "Short" } }
        }
        check("a Float is stored as a 64-bit FLOAT, so it also reads as Double") {
            Variant.from(1.5f).use { expect(it.getValue<Double>() == 1.5) { "Float as Double" } }
        }
        check("Unit is NIL and reads back as Unit") {
            Variant.from(Unit).use { variant ->
                expect(variant.getType() == Variant.Type.NIL) { "type=${variant.getType()}" }
                expect(variant.getValue<Unit>() == Unit) { "not Unit" }
            }
        }
        check("a Variant read as Variant is the same object, with no copy") {
            Variant.from(5L).use { variant -> expect(variant.getValue<Variant>() === variant) { "copied" } }
        }
    }

    private fun strings() = with(checks) {
        val samples = listOf("", "plain", "héllo ñ", "日本語", "🎮 emoji", "line1\nline2", " spaces ")
        for (sample in samples) roundTrip("String '${sample.replace("\n", "\\n")}'", sample, Variant.Type.STRING)
        check("String reads back from a GodotString too") {
            GodotString("héllo").use { godot ->
                Variant.from(godot).use { variant ->
                    expect(variant.getValue<String>() == "héllo") { "got ${variant.getValue<String>()}" }
                }
            }
        }
        check("a StringName is not a String: the types stay distinct") {
            StringName("name").use { name ->
                Variant.from(name).use { variant ->
                    expect(variant.getType() == Variant.Type.STRING_NAME) { "type=${variant.getType()}" }
                    expectThrows<ClassCastException>("StringName as String") { variant.getValue<String>() }
                }
            }
        }
    }

    // ── numeric narrowing ─────────────────────────────────────────────────────────────────────────────────────────

    private fun narrowing() = with(checks) {
        check("Long inside the Int range narrows to Int, and one outside is not convertible") {
            for (inside in listOf(0L, 1L, -1L, Int.MAX_VALUE.toLong(), Int.MIN_VALUE.toLong())) {
                Variant.from(inside).use { expect(it.getValue<Int>().toLong() == inside) { "$inside as Int" } }
            }
            for (outside in listOf(Int.MAX_VALUE + 1L, Int.MIN_VALUE - 1L, Long.MAX_VALUE, Long.MIN_VALUE)) {
                Variant.from(outside).use { variant ->
                    expect(variant.getValueOrNull<Int>() == null) { "$outside gave a value as Int" }
                    val error = expectThrows<ArithmeticException>("$outside as Int") { variant.getValue<Int>() }
                    expect(error.message.orEmpty().contains(outside.toString())) { "message: ${error.message}" }
                    expect(error.message.orEmpty().contains("Long")) { "no workaround in: ${error.message}" }
                }
            }
        }
        check("Long inside the Short range narrows to Short, and one outside is not convertible") {
            for (inside in listOf(0L, 1L, -1L, Short.MAX_VALUE.toLong(), Short.MIN_VALUE.toLong())) {
                Variant.from(inside).use { expect(it.getValue<Short>().toLong() == inside) { "$inside as Short" } }
            }
            for (outside in listOf(Short.MAX_VALUE + 1L, Short.MIN_VALUE - 1L, Int.MAX_VALUE.toLong())) {
                Variant.from(outside).use { variant ->
                    expect(variant.getValueOrNull<Short>() == null) { "$outside gave a value as Short" }
                    expectThrows<ArithmeticException>("$outside as Short") { variant.getValue<Short>() }
                }
            }
        }
        check("the workaround for a big value: read the Long, narrow it yourself") {
            Variant.from(5_000_000_000L).use { variant ->
                expect(variant.getValue<Long>() == 5_000_000_000L) { "Long" }
                expect(variant.getValue<Long>().toInt() == 705_032_704) { "explicit truncation" }
            }
        }
        check("Double inside the Float range narrows, rounding to the nearest Float") {
            Variant.from(0.1).use { expect(it.getValue<Float>() == 0.1f) { "0.1" } }
            Variant.from(Float.MAX_VALUE.toDouble()).use { expect(it.getValue<Float>() == Float.MAX_VALUE) { "max" } }
            Variant.from(-Float.MAX_VALUE.toDouble())
                .use { expect(it.getValue<Float>() == -Float.MAX_VALUE) { "-max" } }
            Variant.from(1e-300).use { expect(it.getValue<Float>() == 0f) { "underflow rounds to zero" } }
        }
        check("NaN and infinities narrow to Float unchanged") {
            Variant.from(Double.NaN).use { expect(it.getValue<Float>().isNaN()) { "NaN" } }
            Variant.from(Double.POSITIVE_INFINITY)
                .use { expect(it.getValue<Float>() == Float.POSITIVE_INFINITY) { "+inf" } }
            Variant.from(Double.NEGATIVE_INFINITY)
                .use { expect(it.getValue<Float>() == Float.NEGATIVE_INFINITY) { "-inf" } }
        }
        check("a finite Double outside the Float range is not convertible") {
            for (outside in listOf(1e300, -1e300, Double.MAX_VALUE)) {
                Variant.from(outside).use { variant ->
                    expect(variant.getValueOrNull<Float>() == null) { "$outside gave a value as Float" }
                    val error = expectThrows<ArithmeticException>("$outside as Float") { variant.getValue<Float>() }
                    expect(error.message.orEmpty().contains("Double")) { "no workaround in: ${error.message}" }
                }
            }
        }
    }

    // ── nullable types ────────────────────────────────────────────────────────────────────────────────────────────

    private fun nullables() = with(checks) {
        check("NIL reads as null for every nullable scalar and builtin") {
            Variant().use { nil ->
                expect(nil.getValue<Long?>() == null) { "Long?" }
                expect(nil.getValue<Int?>() == null) { "Int?" }
                expect(nil.getValue<Short?>() == null) { "Short?" }
                expect(nil.getValue<Float?>() == null) { "Float?" }
                expect(nil.getValue<Double?>() == null) { "Double?" }
                expect(nil.getValue<Boolean?>() == null) { "Boolean?" }
                expect(nil.getValue<String?>() == null) { "String?" }
                expect(nil.getValue<Vector2?>() == null) { "Vector2?" }
                expect(nil.getValue<StringName?>() == null) { "StringName?" }
                expect(nil.getValue<VariantArray?>() == null) { "VariantArray?" }
                expect(nil.getValue<VariantDictionary?>() == null) { "VariantDictionary?" }
                expect(nil.getValue<PackedInt32Array?>() == null) { "PackedInt32Array?" }
                expect(nil.getValue<GodotObject?>() == null) { "GodotObject?" }
                expect(nil.getValue<Variant?>() != null) { "Variant? keeps the NIL variant itself" }
            }
        }
        check("a nullable type still reads a real value") {
            Variant.from(5L).use { expect(it.getValue<Long?>() == 5L) { "Long?" } }
            Variant.from(5).use { expect(it.getValue<Int?>() == 5) { "Int?" } }
            Variant.from(1.5f).use { expect(it.getValue<Float?>() == 1.5f) { "Float?" } }
            Variant.from(true).use { expect(it.getValue<Boolean?>() == true) { "Boolean?" } }
            Variant.from("text").use { expect(it.getValue<String?>() == "text") { "String?" } }
            Variant.from(Vector2(1.0, 2.0)).use { expect(it.getValue<Vector2?>() == Vector2(1.0, 2.0)) { "Vector2?" } }
        }
        check("a non-nullable type rejects NIL") {
            Variant().use { nil ->
                expectThrows<ClassCastException>("NIL as Long") { nil.getValue<Long>() }
                expectThrows<ClassCastException>("NIL as Int") { nil.getValue<Int>() }
                expectThrows<ClassCastException>("NIL as Boolean") { nil.getValue<Boolean>() }
                expectThrows<ClassCastException>("NIL as String") { nil.getValue<String>() }
                expectThrows<ClassCastException>("NIL as Vector2") { nil.getValue<Vector2>() }
                expectThrows<ClassCastException>("NIL as VariantArray") { nil.getValue<VariantArray>() }
                expect(nil.getValueOrNull<Long>() == null) { "getValueOrNull<Long> on NIL" }
            }
        }
        check("a nullable type is not a way to hide a mismatch or a value that does not fit") {
            Variant.from("text").use { expectThrows<ClassCastException>("String as Long?") { it.getValue<Long?>() } }
            Variant.from(5_000_000_000L)
                .use { expectThrows<ArithmeticException>("5e9 as Int?") { it.getValue<Int?>() } }
            Variant.from(1e300).use { expectThrows<ArithmeticException>("1e300 as Float?") { it.getValue<Float?>() } }
        }
        check("Unit is only NIL") {
            Variant.from(5L).use { expectThrows<ClassCastException>("5 as Unit") { it.getValue<Unit>() } }
        }
    }

    // ── mismatches ────────────────────────────────────────────────────────────────────────────────────────────────

    private fun mismatches() = with(checks) {
        check("a mismatch is a ClassCastException that names the type and the value, getValueOrNull gives null") {
            Variant.from("hello").use { variant ->
                val error = expectThrows<ClassCastException>("String as Long") { variant.getValue<Long>() }
                expect(error.message.orEmpty().contains("Long")) { "expected type missing: ${error.message}" }
                expect(error.message.orEmpty().contains("hello")) { "value missing: ${error.message}" }
                expect(variant.getValueOrNull<Long>() == null) { "getValueOrNull" }
            }
        }
        check("there is no implicit conversion between Godot types") {
            Variant.from(5L).use { variant ->
                expectThrows<ClassCastException>("INT as Double") { variant.getValue<Double>() }
                expectThrows<ClassCastException>("INT as Float") { variant.getValue<Float>() }
                expectThrows<ClassCastException>("INT as Boolean") { variant.getValue<Boolean>() }
                expectThrows<ClassCastException>("INT as String") { variant.getValue<String>() }
            }
            Variant.from(1.5).use { variant ->
                expectThrows<ClassCastException>("FLOAT as Long") { variant.getValue<Long>() }
                expectThrows<ClassCastException>("FLOAT as Int") { variant.getValue<Int>() }
            }
            Variant.from(true).use { variant ->
                expectThrows<ClassCastException>("BOOL as Long") { variant.getValue<Long>() }
            }
            Variant.from(Vector2(1.0, 2.0)).use { variant ->
                expectThrows<ClassCastException>("VECTOR2 as Vector2i") { variant.getValue<Vector2i>() }
                expectThrows<ClassCastException>("VECTOR2 as Vector3") { variant.getValue<Vector3>() }
            }
        }
        check("a failed read does not leave the Variant unusable") {
            Variant.from(9L).use { variant ->
                expectThrows<ClassCastException>("INT as String") { variant.getValue<String>() }
                expect(variant.getValue<Long>() == 9L) { "variant changed after a failed read" }
            }
        }
    }

    // ── Godot builtins ────────────────────────────────────────────────────────────────────────────────────────────

    private fun builtins() = with(checks) {
        roundTrip("Vector2", Vector2(1.5, -2.5), Variant.Type.VECTOR2)
        roundTrip("Vector2i", Vector2i(), Variant.Type.VECTOR2I)
        roundTrip("Vector3", Vector3(), Variant.Type.VECTOR3)
        roundTrip("Vector3i", Vector3i(), Variant.Type.VECTOR3I)
        roundTrip("Vector4", Vector4(), Variant.Type.VECTOR4)
        roundTrip("Vector4i", Vector4i(), Variant.Type.VECTOR4I)
        roundTrip("Rect2", Rect2(), Variant.Type.RECT2)
        roundTrip("Rect2i", Rect2i(), Variant.Type.RECT2I)
        roundTrip("Transform2D", Transform2D(), Variant.Type.TRANSFORM2D)
        roundTrip("Transform3D", Transform3D(), Variant.Type.TRANSFORM3D)
        roundTrip("Plane", Plane(), Variant.Type.PLANE)
        roundTrip("Quaternion", Quaternion(), Variant.Type.QUATERNION)
        roundTrip("Aabb", Aabb(), Variant.Type.AABB)
        roundTrip("Basis", Basis(), Variant.Type.BASIS)
        roundTrip("Projection", Projection(), Variant.Type.PROJECTION)
        roundTrip("Color", Color(), Variant.Type.COLOR)
        roundTrip("Rid", Rid(), Variant.Type.RID)
        roundTrip("Callable", Callable(), Variant.Type.CALLABLE)
        roundTrip("Signal", Signal(), Variant.Type.SIGNAL)
        roundTrip("GodotString", GodotString("kogot"), Variant.Type.STRING)
        roundTrip("StringName", StringName("kogot"), Variant.Type.STRING_NAME)
        roundTrip("NodePath", NodePath("a/b:c"), Variant.Type.NODE_PATH)
    }

    private fun packedArrays() = with(checks) {
        roundTrip("PackedByteArray", PackedByteArray(), Variant.Type.PACKED_BYTE_ARRAY)
        roundTrip("PackedInt32Array", PackedInt32Array(), Variant.Type.PACKED_INT32_ARRAY)
        roundTrip("PackedInt64Array", PackedInt64Array(), Variant.Type.PACKED_INT64_ARRAY)
        roundTrip("PackedFloat32Array", PackedFloat32Array(), Variant.Type.PACKED_FLOAT32_ARRAY)
        roundTrip("PackedFloat64Array", PackedFloat64Array(), Variant.Type.PACKED_FLOAT64_ARRAY)
        roundTrip("PackedStringArray", PackedStringArray(), Variant.Type.PACKED_STRING_ARRAY)
        roundTrip("PackedVector2Array", PackedVector2Array(), Variant.Type.PACKED_VECTOR2_ARRAY)
        roundTrip("PackedVector3Array", PackedVector3Array(), Variant.Type.PACKED_VECTOR3_ARRAY)
        roundTrip("PackedVector4Array", PackedVector4Array(), Variant.Type.PACKED_VECTOR4_ARRAY)
        roundTrip("PackedColorArray", PackedColorArray(), Variant.Type.PACKED_COLOR_ARRAY)
        check("a packed array with content keeps it") {
            PackedInt64Array().use { packed ->
                // Godot's `append` returns true when it FAILED.
                expect(!(packed.append(10L) || packed.append(20L))) { "append" }
                Variant.from(packed).use { variant ->
                    variant.getValue<PackedInt64Array>().use { back ->
                        expect(back.size() == 2L && back[0] == 10L && back[1] == 20L) { "size=${back.size()}" }
                    }
                }
            }
        }
        check("a packed array of another element type is a mismatch") {
            PackedInt32Array().use { packed ->
                Variant.from(packed)
                    .use { expectThrows<ClassCastException>("Int32 as Int64") { it.getValue<PackedInt64Array>() } }
            }
        }
    }

    private fun containersAndObjects() = with(checks) {
        check("roundtrip VariantArray with content") {
            VariantArray().use { array ->
                array.add(Variant.from(1L))
                Variant.from(array).use { variant ->
                    expect(variant.getType() == Variant.Type.ARRAY) { "type=${variant.getType()}" }
                    variant.getValue<VariantArray>().use { back -> expect(back == array) { "back=$back array=$array" } }
                }
            }
        }
        check("roundtrip VariantDictionary with content") {
            VariantDictionary().use { dictionary ->
                dictionary["key".toVariant()] = Variant.from(1L)
                Variant.from(dictionary).use { variant ->
                    expect(variant.getType() == Variant.Type.DICTIONARY) { "type=${variant.getType()}" }
                    variant.getValue<VariantDictionary>().use { back -> expect(back == dictionary) { "back=$back" } }
                }
            }
        }
        check("an array is not a dictionary") {
            VariantArray().use { array ->
                Variant.from(array)
                    .use {
                        expectThrows<ClassCastException>("ARRAY as dictionary") { it.getValue<VariantDictionary>() }
                    }
            }
        }
        check("an engine object reads as a GodotObject with the same pointer") {
            val object1 = RefCounted()
            Variant.from(object1).use { variant ->
                expect(variant.getType() == Variant.Type.OBJECT) { "type=${variant.getType()}" }
                expect(variant.getValue<GodotObject>().rawPtr == object1.rawPtr) { "different pointer" }
            }
        }
    }

    // ── `by variant` ──────────────────────────────────────────────────────────────────────────────────────────────

    private fun delegates() = with(checks) {
        check("by variant reads each supported type") {
            val long: Long by Variant.from(7L)
            val int: Int by Variant.from(7)
            val double: Double by Variant.from(2.5)
            val text: String by Variant.from("kogot")
            val flag: Boolean by Variant.from(true)
            val vector: Vector2 by Variant.from(Vector2(1.0, 2.0))
            expect(long == 7L && int == 7 && double == 2.5 && text == "kogot" && flag && vector == Vector2(1.0, 2.0)) {
                "long=$long int=$int double=$double text=$text flag=$flag"
            }
        }
        check("by variant with a nullable type reads NIL as null") {
            val missing: Long? by Variant()
            expect(missing == null) { "missing=$missing" }
        }
        check("by variant fails like getValue") {
            val wrong: Long by Variant.from("text")
            expectThrows<ClassCastException>("String delegate as Long") { wrong }
        }
    }
}
