@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.getvalue

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.*
import io.github.kingg22.godot.api.core.GodotObject
import io.github.kingg22.godot.api.core.RefCounted
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Live tests of `Variant.getValue` / `getValueOrNull` / `by variant`, ported from the in-engine `GetValueCheck` scene:
 * every supported type, the numeric narrowing limits, nullable types, mismatches and what each one throws. The
 * collections are covered by `CollectionsTest`.
 */
class GetValueTest {
    /** Sends [value] to a `Variant`, checks its `Variant.Type`, and reads it back as the same type. */
    private inline fun <@MustBeVariant reified T> roundTrip(name: String, value: T, type: Variant.Type) {
        Variant.from(value).use { variant ->
            assertEquals(type, variant.getType(), "roundtrip $name: type")
            val back = variant.getValue<T>()
            assertTrue(back == value, "roundtrip $name: read back $back, sent $value")
            (back as? AutoCloseable)?.close()
        }
        (value as? AutoCloseable)?.close()
    }

    // ── supported scalars ──

    @Test
    fun roundTripBoolean() {
        roundTrip("Boolean true", true, Variant.Type.BOOL)
        roundTrip("Boolean false", false, Variant.Type.BOOL)
    }

    @Test
    fun roundTripLong() {
        for (long in listOf(0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            roundTrip("Long $long", long, Variant.Type.INT)
        }
    }

    @Test
    fun roundTripInt() {
        for (int in listOf(0, 7, -7, Int.MAX_VALUE, Int.MIN_VALUE)) roundTrip("Int $int", int, Variant.Type.INT)
    }

    @Test
    fun roundTripShort() {
        for (short: Short in listOf(0, 7, -7, Short.MAX_VALUE, Short.MIN_VALUE)) {
            roundTrip("Short $short", short, Variant.Type.INT)
        }
    }

    @Test
    fun roundTripDouble() {
        for (double in listOf(0.0, 1.5, -1.5, Double.MAX_VALUE, Double.MIN_VALUE, Double.POSITIVE_INFINITY)) {
            roundTrip("Double $double", double, Variant.Type.FLOAT)
        }
        roundTrip("Double -Infinity", Double.NEGATIVE_INFINITY, Variant.Type.FLOAT)
    }

    @Test
    fun roundTripFloat() {
        for (float in listOf(0f, 1.5f, -1.5f, 0.1f, Float.MAX_VALUE, Float.MIN_VALUE, Float.POSITIVE_INFINITY)) {
            roundTrip("Float $float", float, Variant.Type.FLOAT)
        }
    }

    @Test
    fun roundTripDoubleNaNStaysNaN() {
        Variant.from(Double.NaN).use { assertTrue(it.getValue<Double>().isNaN(), "not NaN") }
    }

    @Test
    fun roundTripFloatNaNStaysNaN() {
        Variant.from(Float.NaN).use { assertTrue(it.getValue<Float>().isNaN(), "not NaN") }
    }

    @Test
    fun roundTripDoubleNegativeZeroKeepsItsSign() {
        Variant.from(-0.0).use { assertEquals((-0.0).toRawBits(), it.getValue<Double>().toRawBits(), "sign lost") }
    }

    @Test
    fun anIntAndAShortAreStoredAs64BitIntSoTheyAlsoReadAsLong() {
        Variant.from(Int.MIN_VALUE).use { assertEquals(Int.MIN_VALUE.toLong(), it.getValue<Long>(), "Int as Long") }
        Variant.from(Short.MAX_VALUE).use { assertEquals(Short.MAX_VALUE.toLong(), it.getValue<Long>(), "Short") }
    }

    @Test
    fun aFloatIsStoredAs64BitFloatSoItAlsoReadsAsDouble() {
        Variant.from(1.5f).use { assertEquals(1.5, it.getValue<Double>(), "Float as Double") }
    }

    @Test
    fun unitIsNilAndReadsBackAsUnit() {
        Variant.from(Unit).use { variant ->
            assertEquals(Variant.Type.NIL, variant.getType(), "type")
            assertEquals(Unit, variant.getValue<Unit>(), "not Unit")
        }
    }

    @Test
    fun aVariantReadAsVariantIsTheSameObjectWithNoCopy() {
        Variant.from(5L).use { variant -> assertSame(variant, variant.getValue<Variant>(), "copied") }
    }

    // ── strings ──

    @Test
    fun roundTripStrings() {
        val samples = listOf("", "plain", "héllo ñ", "日本語", "🎮 emoji", "line1\nline2", " spaces ")
        for (sample in samples) roundTrip("String '${sample.replace("\n", "\\n")}'", sample, Variant.Type.STRING)
    }

    @Test
    fun stringReadsBackFromAGodotStringToo() {
        GodotString("héllo").use { godot ->
            Variant.from(godot).use { variant ->
                assertEquals("héllo", variant.getValue<String>(), "got ${variant.getValue<String>()}")
            }
        }
    }

    @Test
    fun aStringNameIsNotAStringTheTypesStayDistinct() {
        StringName("name").use { name ->
            Variant.from(name).use { variant ->
                assertEquals(Variant.Type.STRING_NAME, variant.getType(), "type")
                assertFailsWith<ClassCastException>("StringName as String") { variant.getValue<String>() }
            }
        }
    }

    // ── numeric narrowing ──

    @Test
    fun longInsideTheIntRangeNarrowsToIntAndOneOutsideIsNotConvertible() {
        for (inside in listOf(0L, 1L, -1L, Int.MAX_VALUE.toLong(), Int.MIN_VALUE.toLong())) {
            Variant.from(inside).use { assertEquals(inside, it.getValue<Int>().toLong(), "$inside as Int") }
        }
        for (outside in listOf(Int.MAX_VALUE + 1L, Int.MIN_VALUE - 1L, Long.MAX_VALUE, Long.MIN_VALUE)) {
            Variant.from(outside).use { variant ->
                assertNull(variant.getValueOrNull<Int>(), "$outside gave a value as Int")
                val error = assertFailsWith<ArithmeticException>("$outside as Int") { variant.getValue<Int>() }
                assertTrue(error.message.orEmpty().contains(outside.toString()), "message: ${error.message}")
                assertTrue(error.message.orEmpty().contains("Long"), "no workaround in: ${error.message}")
            }
        }
    }

    @Test
    fun longInsideTheShortRangeNarrowsToShortAndOneOutsideIsNotConvertible() {
        for (inside in listOf(0L, 1L, -1L, Short.MAX_VALUE.toLong(), Short.MIN_VALUE.toLong())) {
            Variant.from(inside).use { assertEquals(inside, it.getValue<Short>().toLong(), "$inside as Short") }
        }
        for (outside in listOf(Short.MAX_VALUE + 1L, Short.MIN_VALUE - 1L, Int.MAX_VALUE.toLong())) {
            Variant.from(outside).use { variant ->
                assertNull(variant.getValueOrNull<Short>(), "$outside gave a value as Short")
                assertFailsWith<ArithmeticException>("$outside as Short") { variant.getValue<Short>() }
            }
        }
    }

    @Test
    fun theWorkaroundForABigValueReadTheLongNarrowItYourself() {
        Variant.from(5_000_000_000L).use { variant ->
            assertEquals(5_000_000_000L, variant.getValue<Long>(), "Long")
            assertEquals(705_032_704, variant.getValue<Long>().toInt(), "explicit truncation")
        }
    }

    @Test
    fun doubleInsideTheFloatRangeNarrowsRoundingToTheNearestFloat() {
        Variant.from(0.1).use { assertEquals(0.1f, it.getValue<Float>(), "0.1") }
        Variant.from(Float.MAX_VALUE.toDouble()).use { assertEquals(Float.MAX_VALUE, it.getValue<Float>(), "max") }
        Variant.from(-Float.MAX_VALUE.toDouble())
            .use { assertEquals(-Float.MAX_VALUE, it.getValue<Float>(), "-max") }
        Variant.from(1e-300).use { assertEquals(0f, it.getValue<Float>(), "underflow rounds to zero") }
    }

    @Test
    fun nanAndInfinitiesNarrowToFloatUnchanged() {
        Variant.from(Double.NaN).use { assertTrue(it.getValue<Float>().isNaN(), "NaN") }
        Variant.from(Double.POSITIVE_INFINITY)
            .use { assertEquals(Float.POSITIVE_INFINITY, it.getValue<Float>(), "+inf") }
        Variant.from(Double.NEGATIVE_INFINITY)
            .use { assertEquals(Float.NEGATIVE_INFINITY, it.getValue<Float>(), "-inf") }
    }

    @Test
    fun aFiniteDoubleOutsideTheFloatRangeIsNotConvertible() {
        for (outside in listOf(1e300, -1e300, Double.MAX_VALUE)) {
            Variant.from(outside).use { variant ->
                assertNull(variant.getValueOrNull<Float>(), "$outside gave a value as Float")
                val error = assertFailsWith<ArithmeticException>("$outside as Float") { variant.getValue<Float>() }
                assertTrue(error.message.orEmpty().contains("Double"), "no workaround in: ${error.message}")
            }
        }
    }

    // ── nullable types ──

    @Test
    fun nilReadsAsNullForEveryNullableScalarAndBuiltin() {
        Variant().use { nil ->
            assertNull(nil.getValue<Long?>(), "Long?")
            assertNull(nil.getValue<Int?>(), "Int?")
            assertNull(nil.getValue<Short?>(), "Short?")
            assertNull(nil.getValue<Float?>(), "Float?")
            assertNull(nil.getValue<Double?>(), "Double?")
            assertNull(nil.getValue<Boolean?>(), "Boolean?")
            assertNull(nil.getValue<String?>(), "String?")
            assertNull(nil.getValue<Vector2?>(), "Vector2?")
            assertNull(nil.getValue<StringName?>(), "StringName?")
            assertNull(nil.getValue<VariantArray?>(), "VariantArray?")
            assertNull(nil.getValue<VariantDictionary?>(), "VariantDictionary?")
            assertNull(nil.getValue<PackedInt32Array?>(), "PackedInt32Array?")
            assertNull(nil.getValue<GodotObject?>(), "GodotObject?")
            assertNotNull(nil.getValue<Variant?>(), "Variant? keeps the NIL variant itself")
        }
    }

    @Test
    fun aNullableTypeStillReadsARealValue() {
        Variant.from(5L).use { assertEquals(5L, it.getValue<Long?>(), "Long?") }
        Variant.from(5).use { assertEquals(5, it.getValue<Int?>(), "Int?") }
        Variant.from(1.5f).use { assertEquals(1.5f, it.getValue<Float?>(), "Float?") }
        Variant.from(true).use { assertEquals(true, it.getValue<Boolean?>(), "Boolean?") }
        Variant.from("text").use { assertEquals("text", it.getValue<String?>(), "String?") }
        Variant.from(Vector2(1.0, 2.0)).use { assertEquals(Vector2(1.0, 2.0), it.getValue<Vector2?>(), "Vector2?") }
    }

    @Test
    fun aNonNullableTypeRejectsNil() {
        Variant().use { nil ->
            assertFailsWith<ClassCastException>("NIL as Long") { nil.getValue<Long>() }
            assertFailsWith<ClassCastException>("NIL as Int") { nil.getValue<Int>() }
            assertFailsWith<ClassCastException>("NIL as Boolean") { nil.getValue<Boolean>() }
            assertFailsWith<ClassCastException>("NIL as String") { nil.getValue<String>() }
            assertFailsWith<ClassCastException>("NIL as Vector2") { nil.getValue<Vector2>() }
            assertFailsWith<ClassCastException>("NIL as VariantArray") { nil.getValue<VariantArray>() }
            assertNull(nil.getValueOrNull<Long>(), "getValueOrNull<Long> on NIL")
        }
    }

    @Test
    fun aNullableTypeIsNotAWayToHideAMismatchOrAValueThatDoesNotFit() {
        Variant.from("text").use { assertFailsWith<ClassCastException>("String as Long?") { it.getValue<Long?>() } }
        Variant.from(5_000_000_000L)
            .use { assertFailsWith<ArithmeticException>("5e9 as Int?") { it.getValue<Int?>() } }
        Variant.from(1e300).use { assertFailsWith<ArithmeticException>("1e300 as Float?") { it.getValue<Float?>() } }
    }

    @Test
    fun unitIsOnlyNil() {
        Variant.from(5L).use { assertFailsWith<ClassCastException>("5 as Unit") { it.getValue<Unit>() } }
    }

    // ── mismatches ──

    @Test
    fun aMismatchIsAClassCastExceptionThatNamesTheTypeAndTheValueGetValueOrNullGivesNull() {
        Variant.from("hello").use { variant ->
            val error = assertFailsWith<ClassCastException>("String as Long") { variant.getValue<Long>() }
            assertTrue(error.message.orEmpty().contains("Long"), "expected type missing: ${error.message}")
            assertTrue(error.message.orEmpty().contains("hello"), "value missing: ${error.message}")
            assertNull(variant.getValueOrNull<Long>(), "getValueOrNull")
        }
    }

    @Test
    fun thereIsNoImplicitConversionBetweenGodotTypes() {
        Variant.from(5L).use { variant ->
            assertFailsWith<ClassCastException>("INT as Double") { variant.getValue<Double>() }
            assertFailsWith<ClassCastException>("INT as Float") { variant.getValue<Float>() }
            assertFailsWith<ClassCastException>("INT as Boolean") { variant.getValue<Boolean>() }
            assertFailsWith<ClassCastException>("INT as String") { variant.getValue<String>() }
        }
        Variant.from(1.5).use { variant ->
            assertFailsWith<ClassCastException>("FLOAT as Long") { variant.getValue<Long>() }
            assertFailsWith<ClassCastException>("FLOAT as Int") { variant.getValue<Int>() }
        }
        Variant.from(true).use { variant ->
            assertFailsWith<ClassCastException>("BOOL as Long") { variant.getValue<Long>() }
        }
        Variant.from(Vector2(1.0, 2.0)).use { variant ->
            assertFailsWith<ClassCastException>("VECTOR2 as Vector2i") { variant.getValue<Vector2i>() }
            assertFailsWith<ClassCastException>("VECTOR2 as Vector3") { variant.getValue<Vector3>() }
        }
    }

    @Test
    fun aFailedReadDoesNotLeaveTheVariantUnusable() {
        Variant.from(9L).use { variant ->
            assertFailsWith<ClassCastException>("INT as String") { variant.getValue<String>() }
            assertEquals(9L, variant.getValue<Long>(), "variant changed after a failed read")
        }
    }

    // ── Godot builtins ──

    @Test
    fun roundTripVectorsAndRects() {
        roundTrip("Vector2", Vector2(1.5, -2.5), Variant.Type.VECTOR2)
        roundTrip("Vector2i", Vector2i(), Variant.Type.VECTOR2I)
        roundTrip("Vector3", Vector3(), Variant.Type.VECTOR3)
        roundTrip("Vector3i", Vector3i(), Variant.Type.VECTOR3I)
        roundTrip("Vector4", Vector4(), Variant.Type.VECTOR4)
        roundTrip("Vector4i", Vector4i(), Variant.Type.VECTOR4I)
        roundTrip("Rect2", Rect2(), Variant.Type.RECT2)
        roundTrip("Rect2i", Rect2i(), Variant.Type.RECT2I)
    }

    @Test
    fun roundTripTransformsAndMathBuiltins() {
        roundTrip("Transform2D", Transform2D(), Variant.Type.TRANSFORM2D)
        roundTrip("Transform3D", Transform3D(), Variant.Type.TRANSFORM3D)
        roundTrip("Plane", Plane(), Variant.Type.PLANE)
        roundTrip("Quaternion", Quaternion(), Variant.Type.QUATERNION)
        roundTrip("Aabb", Aabb(), Variant.Type.AABB)
        roundTrip("Basis", Basis(), Variant.Type.BASIS)
        roundTrip("Projection", Projection(), Variant.Type.PROJECTION)
    }

    @Test
    fun roundTripColorRidCallableSignalAndNames() {
        roundTrip("Color", Color(), Variant.Type.COLOR)
        roundTrip("Rid", Rid(), Variant.Type.RID)
        roundTrip("Callable", Callable(), Variant.Type.CALLABLE)
        roundTrip("Signal", Signal(), Variant.Type.SIGNAL)
        roundTrip("GodotString", GodotString("kogot"), Variant.Type.STRING)
        roundTrip("StringName", StringName("kogot"), Variant.Type.STRING_NAME)
        roundTrip("NodePath", NodePath("a/b:c"), Variant.Type.NODE_PATH)
    }

    // ── packed arrays ──

    @Test
    fun roundTripEmptyPackedArrays() {
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
    }

    @Test
    fun aPackedArrayWithContentKeepsIt() {
        PackedInt64Array().use { packed ->
            // Godot's `append` returns true when it FAILED.
            assertTrue(!(packed.append(10L) || packed.append(20L)), "append")
            Variant.from(packed).use { variant ->
                variant.getValue<PackedInt64Array>().use { back ->
                    assertTrue(back.size() == 2L && back[0] == 10L && back[1] == 20L, "size=${back.size()}")
                }
            }
        }
    }

    @Test
    fun aPackedArrayOfAnotherElementTypeIsAMismatch() {
        PackedInt32Array().use { packed ->
            Variant.from(packed)
                .use { assertFailsWith<ClassCastException>("Int32 as Int64") { it.getValue<PackedInt64Array>() } }
        }
    }

    // ── containers and objects ──

    @Test
    fun roundTripVariantArrayWithContent() {
        VariantArray().use { array ->
            array.add(Variant.from(1L))
            Variant.from(array).use { variant ->
                assertEquals(Variant.Type.ARRAY, variant.getType(), "type")
                variant.getValue<VariantArray>().use { back -> assertTrue(back == array, "back=$back array=$array") }
            }
        }
    }

    @Test
    fun roundTripVariantDictionaryWithContent() {
        VariantDictionary().use { dictionary ->
            dictionary["key".toVariant()] = Variant.from(1L)
            Variant.from(dictionary).use { variant ->
                assertEquals(Variant.Type.DICTIONARY, variant.getType(), "type")
                variant.getValue<VariantDictionary>().use { back -> assertTrue(back == dictionary, "back=$back") }
            }
        }
    }

    @Test
    fun anArrayIsNotADictionary() {
        VariantArray().use { array ->
            Variant.from(array)
                .use {
                    assertFailsWith<ClassCastException>("ARRAY as dictionary") { it.getValue<VariantDictionary>() }
                }
        }
    }

    @Test
    fun anEngineObjectReadsAsAGodotObjectWithTheSamePointer() {
        val object1 = RefCounted()
        Variant.from(object1).use { variant ->
            assertEquals(Variant.Type.OBJECT, variant.getType(), "type")
            assertEquals(object1.rawPtr, variant.getValue<GodotObject>().rawPtr, "different pointer")
        }
    }

    // ── `by variant` ──

    @Test
    fun byVariantReadsEachSupportedType() {
        val long: Long by Variant.from(7L)
        val int: Int by Variant.from(7)
        val double: Double by Variant.from(2.5)
        val text: String by Variant.from("kogot")
        val flag: Boolean by Variant.from(true)
        val vector: Vector2 by Variant.from(Vector2(1.0, 2.0))
        assertTrue(
            long == 7L && int == 7 && double == 2.5 && text == "kogot" && flag && vector == Vector2(1.0, 2.0),
            "long=$long int=$int double=$double text=$text flag=$flag",
        )
    }

    @Test
    fun byVariantWithANullableTypeReadsNilAsNull() {
        val missing: Long? by Variant()
        assertNull(missing, "missing=$missing")
    }

    @Test
    fun byVariantFailsLikeGetValue() {
        val wrong: Long by Variant.from("text")
        assertFailsWith<ClassCastException>("String delegate as Long") { wrong }
    }
}
