@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.collections

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.Dictionary
import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.PackedFloat32Array
import io.github.kingg22.godot.api.builtin.PackedInt64Array
import io.github.kingg22.godot.api.builtin.PackedStringArray
import io.github.kingg22.godot.api.builtin.PackedVector2Array
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.VariantArray
import io.github.kingg22.godot.api.builtin.VariantDictionary
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.builtin.getValueOrNull
import io.github.kingg22.godot.api.builtin.toDictionary
import io.github.kingg22.godot.api.builtin.toGodotArray
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.core.Node
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Live tests of the `List` / `Map` views over Godot arrays and dictionaries (issue #119), ported from the in-engine
 * `CollectionsCheck` scene.
 */
class CollectionsTest {
    // ── List ──

    @Test
    fun listAddSizeAndGet() {
        GodotArray<Long>().use { list ->
            list.add(1L)
            list.add(2L)
            list.add(3L)
            assertEquals(3, list.size, "size")
            assertEquals(1L, list[0], "list[0]")
            assertEquals(3L, list[2], "list[2]")
        }
    }

    @Test
    fun listSetAddAtIndexRemoveAtContainsIndexOf() {
        GodotArray<Long>().use { list ->
            list.addAll(listOf(10L, 20L, 30L))
            list[1] = 25L
            list.add(0, 5L)
            val removed = list.removeAt(3)
            assertEquals(listOf(5L, 10L, 25L), list.toList(), "list")
            assertEquals(30L, removed, "removed")
            assertTrue(list.contains(25L) && list.indexOf(10L) == 1 && !list.contains(99L), "contains/indexOf")
        }
    }

    @Test
    fun listIterationSumAndSubList() {
        GodotArray<Long>().use { list ->
            list.addAll(listOf(1L, 2L, 3L, 4L))
            var sum = 0L
            for (value in list) sum += value
            assertEquals(10L, sum, "iteration sum")
            assertEquals(10L, list.sum(), "list.sum()")
            assertEquals(listOf(2L, 3L), list.subList(1, 3).toList(), "subList")
        }
    }

    @Test
    fun listRemoveElementClearAndIsEmpty() {
        GodotArray<Long>().use { list ->
            list.addAll(listOf(7L, 8L, 7L))
            list.remove(7L)
            assertEquals(listOf(8L, 7L), list.toList(), "list")
            list.clear()
            assertTrue(list.isEmpty() && list.size == 0, "not empty after clear")
        }
    }

    @Test
    fun listOutOfRangeIndexThrows() {
        GodotArray<Long>().use { list ->
            list.add(1L)
            assertFailsWith<IndexOutOfBoundsException>("get(1)") { list[1] }
            assertFailsWith<IndexOutOfBoundsException>("get(-1)") { list[-1] }
            assertFailsWith<IndexOutOfBoundsException>("add(3, x)") { list.add(3, 9L) }
        }
    }

    @Test
    fun listIntElementsAndNullableElements() {
        GodotArray<Int>().use { ints ->
            ints.add(42)
            assertEquals(42, ints[0], "int")
        }
        GodotArray<Long?>().use { nullable ->
            nullable.add(null)
            nullable.add(4L)
            assertNull(nullable[0], "nullable[0]")
            assertEquals(4L, nullable[1], "nullable[1]")
        }
    }

    @Test
    fun listStringElements() {
        GodotArray<String>().use { names ->
            names.addAll(listOf("a", "bc"))
            assertEquals(listOf("a", "bc"), names.toList(), "names")
        }
    }

    @Test
    fun listNestedArrayIsANewViewOverTheSameStorage() {
        GodotArray<VariantArray>().use { outer ->
            VariantArray().use { inner ->
                inner.add(Variant.from(1L))
                outer.add(inner)
            }
            assertEquals(1, outer.size, "outer size")
            assertEquals(1, outer[0].size, "inner size")
        }
    }

    @Test
    fun listRawVariantAccessKeepsWorking() {
        GodotArray<Long>().use { list ->
            list.add(6L)
            list.getVariant(0L).use { raw -> assertEquals(Variant.Type.INT, raw.getType(), "type") }
        }
    }

    // ── constructors / factories ──

    @Test
    fun factoryCopyOfAnotherArraySharesItsElements() {
        VariantArray().use { source ->
            source.add(Variant.from(4L))
            source.add(Variant.from(5L))
            GodotArray<Long>(source).use { copy ->
                assertEquals(listOf(4L, 5L), copy.toList(), "copy")
            }
        }
    }

    @Test
    fun factoryArrayFromAPackedInt64Array() {
        PackedInt64Array().use { packed ->
            // Godot's `append` returns true when it FAILED.
            assertTrue(!(packed.append(7L) || packed.append(8L)), "append to the packed array")
            GodotArray<Long>(packed).use { list ->
                assertEquals(listOf(7L, 8L), list.toList(), "list")
            }
        }
    }

    @Test
    fun factoryDictionaryCopySharesItsEntries() {
        VariantDictionary().use { source ->
            assertTrue(source.setVariant(Variant.from("k"), Variant.from(3L)), "set the source entry")
            Dictionary<String, Long>(source).use { copy -> assertEquals(3L, copy["k"], "copy[k]") }
        }
    }

    @Test
    fun getValueNilIsNullForANullableTypeAndAMismatchOtherwise() {
        Variant().use { nil ->
            assertNull(nil.getValue<Long?>(), "nullable NIL")
            assertFailsWith<ClassCastException>("NIL as Long") { nil.getValue<Long>() }
        }
    }

    // ── strict reads ──

    @Test
    fun strictStringElementReadAsLongThrowsClassCastException() {
        VariantArray().use { untyped ->
            untyped.add(Variant.from("text"))
            GodotArray<Long?>(untyped.rawPtr).use { asLongs ->
                assertFailsWith<ClassCastException>("read String as Long") { asLongs[0] }
            }
        }
    }

    @Test
    fun strictLongThatDoesNotFitInIntThrowsArithmeticException() {
        VariantArray().use { untyped ->
            untyped.add(Variant.from(5_000_000_000L))
            GodotArray<Int?>(untyped.rawPtr).use { asInts ->
                assertFailsWith<ArithmeticException>("5e9 as Int") { asInts[0] }
            }
        }
    }

    @Test
    fun strictNilReadsAsNullOnlyForANullableElementType() {
        VariantArray().use { untyped ->
            untyped.add(Variant())
            GodotArray<Long?>(untyped.rawPtr).use { asLongs -> assertNull(asLongs[0], "NIL as Long?") }
            assertFailsWith<IllegalStateException>("untyped array as non-nullable Long") {
                GodotArray<Long>(untyped.rawPtr)
            }
        }
    }

    // ── Map ──

    @Test
    fun mapPutGetSizeAndContainsKey() {
        Dictionary<String, Long>().use { map ->
            map["a"] = 1L
            map["b"] = 2L
            assertEquals(2, map.size, "size")
            assertEquals(1L, map["a"], "map[a]")
            assertTrue(map.containsKey("b"), "containsKey(b)")
        }
    }

    @Test
    fun mapMissingKeyReadsNullAndLogsNoEngineError() {
        Dictionary<String, Long>().use { map ->
            assertNull(map["nope"], "missing key reads null")
            assertTrue(!map.containsKey("nope"), "missing key is not contained")
        }
    }

    @Test
    fun mapPutReturnsThePreviousValueRemoveReturnsItToo() {
        Dictionary<String, Long>().use { map ->
            map["a"] = 1L
            val previous = map.put("a", 9L)
            val removed = map.remove("a")
            assertEquals(1L, previous, "previous")
            assertEquals(9L, removed, "removed")
            assertTrue(map.isEmpty(), "empty after remove")
        }
    }

    @Test
    fun mapEntriesKeysAndValuesIterateASnapshot() {
        Dictionary<String, Long>().use { map ->
            map["x"] = 10L
            map["y"] = 20L
            val pairs = map.entries.map { it.key to it.value }.toSet()
            assertEquals(setOf("x" to 10L, "y" to 20L), pairs, "pairs")
            assertEquals(setOf("x", "y"), map.keys, "keys")
            assertEquals(30L, map.values.sum(), "values=${map.values}")
        }
    }

    @Test
    fun mapEntrySetValueWritesThroughIteratorRemoveErases() {
        Dictionary<String, Long>().use { map ->
            map["k"] = 1L
            map.entries.first().setValue(5L)
            assertEquals(5L, map["k"], "after setValue")
            val iterator = map.entries.iterator()
            assertEquals("k", iterator.next().key, "first entry")
            iterator.remove()
            assertTrue(map.isEmpty(), "after iterator.remove")
        }
    }

    @Test
    fun mapNullableValuesAndRawVariantAccess() {
        Dictionary<String, Long?>().use { map ->
            map["n"] = null
            assertTrue(map.containsKey("n") && map["n"] == null, "null value")
        }
    }

    // ── typing ──

    @Test
    fun typingFactoryTypesTheArrayInTheEngine() {
        GodotArray<Long>().use { list ->
            assertTrue(list.isTyped(), "Long array typed")
            assertEquals(Variant.Type.INT.value, list.getTypedBuiltin(), "builtin")
        }
        GodotArray<String>().use { list ->
            assertEquals(Variant.Type.STRING.value, list.getTypedBuiltin(), "builtin")
        }
    }

    @Test
    fun typingVariantAndNullableElementsStayUntyped() {
        GodotArray<Variant>().use { assertTrue(!it.isTyped(), "Variant typed") }
        GodotArray<Long?>().use { assertTrue(!it.isTyped(), "Long? typed") }
    }

    @Test
    fun typingNestedArraysAreTypedAsArray() {
        GodotArray<GodotArray<Long>>().use { list ->
            assertEquals(Variant.Type.ARRAY.value, list.getTypedBuiltin(), "builtin")
        }
    }

    @Test
    fun typingFactoryTypesTheDictionaryKeyAndValue() {
        Dictionary<String, Long>().use { map ->
            assertEquals(Variant.Type.STRING.value, map.getTypedKeyBuiltin(), "key")
            assertEquals(Variant.Type.INT.value, map.getTypedValueBuiltin(), "value")
        }
    }

    @Test
    fun typingWrappingAnEngineArrayWithAnotherTypeFails() {
        GodotArray<String>().use { strings ->
            assertFailsWith<IllegalStateException>("wrap String array as Long") { GodotArray<Long>(strings.rawPtr) }
        }
        VariantArray().use { untyped ->
            assertFailsWith<IllegalStateException>("wrap untyped array as Long") { GodotArray<Long>(untyped.rawPtr) }
        }
    }

    @Test
    fun typingWrappingWithTheRightTypeWorks() {
        GodotArray<Long>().use { longs ->
            longs.add(7L)
            val view = GodotArray<Long>(longs.rawPtr)
            assertEquals(7L, view[0], "view=${view.toList()}")
        }
    }

    // ── packed ──

    @Test
    fun packedInt64ArrayIsAMutableList() {
        PackedInt64Array().use { packed ->
            val list: MutableList<Long> = packed
            list.add(3L)
            list.add(1L)
            list.add(0, 9L)
            assertEquals(3, list.size, "size")
            assertEquals(listOf(9L, 3L, 1L), list, "list")
            assertEquals(3L, list.set(1, 4L), "set returns previous")
            assertEquals(4L, packed[1L], "set wrote through")
            assertEquals(9L, list.removeAt(0), "removeAt returns removed")
            assertEquals(listOf(4L, 1L), list.toList(), "removeAt=$list")
            assertTrue(list.indexOf(1L) == 1 && 4L in list && list.sum() == 5L, "search")
            assertFailsWith<IndexOutOfBoundsException>("get(5)") { list[5] }
        }
    }

    @Test
    fun packedFloatStringAndVectorArrays() {
        PackedFloat32Array().use { floats ->
            floats.add(1.5)
            assertTrue(floats[0] == 1.5 && floats.size == 1, "floats=$floats")
        }
        PackedStringArray().use { strings ->
            strings.add("a")
            strings.add("b")
            assertEquals(listOf("a", "b"), strings.toList(), "strings=$strings")
            assertEquals("a, b", strings.joinToString(), "joinToString")
        }
        PackedVector2Array().use { vectors ->
            vectors.add(Vector2(1.0, 2.0))
            assertEquals(Vector2(1.0, 2.0), vectors.first(), "vectors")
        }
    }

    @Test
    fun packedIterationIsEmptyClearAndWriteThroughTheSharedBuffer() {
        PackedInt64Array().use { packed ->
            (1L..4L).forEach { packed.add(it) }
            assertEquals(10L, packed.sum(), "sum")
            assertEquals(listOf(2L, 4L, 6L, 8L), packed.map { it * 2 }, "map")
            packed.clear()
            assertTrue(packed.isEmpty() && packed.size == 0, "clear")
        }
    }

    // ── conversions ──

    @Test
    fun convertListToATypedGodotArrayACopy() {
        val source = mutableListOf(1L, 2L, 3L)
        source.toGodotArray().use { array ->
            source.add(4L)
            assertEquals(listOf(1L, 2L, 3L), array.toList(), "array")
            assertEquals(Variant.Type.INT.value, array.getTypedBuiltin(), "typed builtin")
        }
    }

    @Test
    fun convertMapToATypedDictionary() {
        mapOf("a" to 1L, "b" to 2L).toDictionary().use { map ->
            assertTrue(
                map["a"] == 1L && map["b"] == 2L && map.size == 2,
                "map=${map.entries.map { it.key to it.value }}",
            )
            assertEquals(Variant.Type.STRING.value, map.getTypedKeyBuiltin(), "key type")
        }
    }

    @Test
    fun convertEmptyCollections() {
        emptyList<String>().toGodotArray().use { assertTrue(it.isEmpty(), "array") }
        emptyMap<String, Long>().toDictionary().use { assertTrue(it.isEmpty(), "dictionary") }
    }

    // ── engine returns (standalone Node, no scene tree needed) ──

    @Test
    fun engineReturnsTypedArrayOfStringNameIsTypedAndReadable() {
        val node = Node()
        node.addToGroup("probe".toStringName())
        node.getGroups().use { groups ->
            assertEquals(Variant.Type.STRING_NAME.value, groups.getTypedBuiltin(), "builtin")
            assertTrue(groups.any { it.toString() == "probe" }, "groups=${groups.map { it.toString() }}")
        }
    }

    @Test
    fun engineReturnsTypedArrayOfDictionaryIsTypedAndReadable() {
        val node = Node()
        node.getPropertyList().use { properties ->
            assertEquals(Variant.Type.DICTIONARY.value, properties.getTypedBuiltin(), "builtin")
            assertTrue(properties.isNotEmpty(), "no properties")
        }
    }

    // ── equality ──
    // `==` of the Godot collections: against a Variant or its own Godot type the engine compares by content;
    // against a Kotlin List or Map the inherited Kotlin equality runs.

    @Test
    fun equalityArrayEqualToAListAnotherGodotArrayAndAVariantArray() {
        GodotArray<Long>().use { array ->
            array.addAll(listOf(1L, 2L, 3L))
            assertTrue(array == listOf(1L, 2L, 3L) && listOf(1L, 2L, 3L) == array, "List")
            GodotArray<Long>().use { other ->
                other.addAll(listOf(1L, 2L, 3L))
                assertTrue(array == other && other == array, "GodotArray<Long>")
            }
            VariantArray().use { untyped ->
                for (i in 1L..3L) Variant.from(i).use { untyped.add(it) }
                assertTrue(array == untyped, "VariantArray")
            }
            Variant(array).use { variant -> assertTrue(array == variant, "Variant") }
        }
    }

    @Test
    fun equalityArrayNegatives() {
        GodotArray<Long>().use { array ->
            array.addAll(listOf(1L, 2L, 3L))
            assertTrue(array != listOf(1L, 2L), "shorter List")
            assertTrue(array != listOf(1L, 2L, 4L), "different element")
            assertTrue(array != listOf(3L, 2L, 1L), "different order")
            assertTrue(array != listOf("1", "2", "3"), "List of another type")
            val notACollection: Any =
                "text" // `array != "text"` does not compile: String is final, so the types are incompatible
            assertTrue(array != notACollection && !array.equals(null), "not a collection, null")
            GodotArray<Long>().use { other ->
                other.addAll(listOf(1L, 2L, 4L))
                assertTrue(array != other, "GodotArray with another element")
            }
            Variant.from(5L).use { variant -> assertTrue(!array.equals(variant), "Variant of another type") }
            Variant(array).use { variant -> assertTrue(listOf(1L, 2L, 3L) != variant, "List vs Variant") }
        }
    }

    @Test
    fun equalityDictionaryEqualToAMapAnotherDictionaryAndAVariantDictionary() {
        Dictionary<String, Long>().use { map ->
            map["a"] = 1L
            map["b"] = 2L
            assertTrue(map == mapOf("a" to 1L, "b" to 2L) && mapOf("b" to 2L, "a" to 1L) == map, "Map")
            Dictionary<String, Long>().use { other ->
                other["b"] = 2L // another insertion order
                other["a"] = 1L
                assertTrue(map == other && other == map, "Dictionary<String, Long>")
            }
            VariantDictionary().use { untyped ->
                Variant.from("a").use { k -> Variant.from(1L).use { v -> untyped[k] = v } }
                Variant.from("b").use { k -> Variant.from(2L).use { v -> untyped[k] = v } }
                assertTrue(map == untyped, "VariantDictionary")
            }
            Variant(map).use { variant -> assertTrue(map == variant, "Variant") }
        }
    }

    @Test
    fun equalityDictionaryNegatives() {
        Dictionary<String, Long>().use { map ->
            map["a"] = 1L
            map["b"] = 2L
            assertTrue(map != mapOf("a" to 1L), "smaller Map")
            assertTrue(map != mapOf("a" to 1L, "b" to 3L), "different value")
            assertTrue(map != mapOf("a" to 1L, "c" to 2L), "different key")
            assertTrue(map != listOf("a", "b") && !map.equals(null), "not a map, null")
            Dictionary<String, Long>().use { other ->
                other["a"] = 1L
                assertTrue(map != other, "Dictionary with fewer entries")
            }
            Variant.from("a").use { variant -> assertTrue(!map.equals(variant), "Variant of another type") }
            Variant(map).use { variant -> assertTrue(mapOf("a" to 1L, "b" to 2L) != variant, "Map vs Variant") }
        }
    }

    @Test
    fun equalityPackedEqualToAListAnotherPackedOfTheSameTypeAndAVariant() {
        PackedInt64Array().use { packed ->
            packed.addAll(listOf(5L, 6L))
            assertTrue(packed == listOf(5L, 6L) && listOf(5L, 6L) == packed, "List")
            PackedInt64Array().use { other ->
                other.addAll(listOf(5L, 6L))
                assertTrue(packed == other && other == packed, "PackedInt64Array")
            }
            Variant(packed).use { variant -> assertTrue(packed == variant, "Variant") }
        }
        PackedStringArray().use { strings ->
            strings.addAll(listOf("x", "y"))
            assertTrue(strings == listOf("x", "y"), "strings vs List")
            Variant(strings).use { variant -> assertTrue(strings == variant, "strings vs Variant") }
        }
        PackedVector2Array().use { vectors ->
            vectors.add(Vector2(1.0, 2.0))
            assertTrue(vectors == listOf(Vector2(1.0, 2.0)), "vectors vs List")
        }
    }

    @Test
    fun equalityPackedNegatives() {
        PackedInt64Array().use { packed ->
            packed.addAll(listOf(5L, 6L))
            assertTrue(packed != listOf(5L), "shorter List")
            assertTrue(packed != listOf(5L, 7L), "different element")
            assertTrue(packed != listOf(6L, 5L), "different order")
            assertTrue(packed != listOf("5", "6") && !packed.equals(null), "List of another type, null")
            PackedInt64Array().use { other ->
                other.addAll(listOf(5L, 7L))
                assertTrue(packed != other, "packed with another element")
            }
            PackedStringArray().use { strings ->
                strings.addAll(listOf("5", "6"))
                assertTrue(!packed.equals(strings), "packed of another type")
            }
            Variant.from(5L).use { variant -> assertTrue(!packed.equals(variant), "Variant of another type") }
            Variant(packed).use { variant -> assertTrue(listOf(5L, 6L) != variant, "List vs Variant") }
        }
    }

    // ── hash ──
    // `hashCode()` is the inherited List/Map one, not the engine's; the engine's stays available as `hash()`.

    @Test
    fun hashAGodotCollectionHashesLikeTheListOrMapItIsEqualTo() {
        GodotArray<Long>().use { array ->
            array.addAll(listOf(1L, 2L, 3L))
            assertEquals(listOf(1L, 2L, 3L).hashCode(), array.hashCode(), "GodotArray")
        }
        Dictionary<String, Long>().use { map ->
            map["a"] = 1L
            map["b"] = 2L
            assertEquals(mapOf("a" to 1L, "b" to 2L).hashCode(), map.hashCode(), "Dictionary")
        }
        PackedInt64Array().use { packed ->
            packed.addAll(listOf(5L, 6L))
            assertEquals(listOf(5L, 6L).hashCode(), packed.hashCode(), "PackedInt64Array")
        }
        PackedStringArray().use { strings ->
            strings.addAll(listOf("x", "y"))
            assertEquals(listOf("x", "y").hashCode(), strings.hashCode(), "PackedStringArray")
        }
    }

    @Test
    fun hashEqualGodotCollectionsHashTheSameDifferentOnesUsuallyDiffer() {
        GodotArray<Long>().use { a ->
            GodotArray<Long>().use { b ->
                a.addAll(listOf(1L, 2L))
                b.addAll(listOf(1L, 2L))
                assertEquals(a.hashCode(), b.hashCode(), "equal arrays")
                b.add(3L)
                assertNotEquals(a.hashCode(), b.hashCode(), "different arrays")
            }
        }
        Dictionary<String, Long>().use { a ->
            Dictionary<String, Long>().use { b ->
                a["k"] = 1L
                b["k"] = 1L
                assertEquals(a.hashCode(), b.hashCode(), "equal dictionaries")
                b["k"] = 2L
                assertNotEquals(a.hashCode(), b.hashCode(), "different dictionaries")
            }
        }
    }

    @Test
    fun hashEngineHashIsSeparateFromHashCodePrinted() {
        GodotArray<Long>().use { array ->
            array.addAll(listOf(1L, 2L, 3L))
            val engine = array.hash()
            println("[CollectionsTest] GodotArray hashCode=${array.hashCode()} engine hash()=$engine")
            GodotArray<Long>().use { other ->
                other.addAll(listOf(1L, 2L, 3L))
                assertEquals(engine, other.hash(), "the engine hash of equal arrays agrees")
            }
        }
    }

    // ── nested ──

    @Test
    fun nestedInnerArrayOfGodotArrayOfGodotArrayReadsItsElementsAsLong() {
        GodotArray<GodotArray<Long>>().use { outer ->
            GodotArray<Long>().use { inner ->
                inner.add(7L)
                outer.add(inner)
            }
            val inner = outer[0]
            assertEquals(Variant.Type.INT.value, inner.getTypedBuiltin(), "engine type of the inner array")
            val value: Long = inner[0]
            assertEquals(7L, value, "value")
        }
    }

    @Test
    fun nestedThreeLevelsAndADictionaryOfArrays() {
        GodotArray<GodotArray<GodotArray<Long>>>().use { deep ->
            GodotArray<GodotArray<Long>>().use { mid ->
                GodotArray<Long>().use { leaf ->
                    leaf.add(9L)
                    mid.add(leaf)
                }
                deep.add(mid)
            }
            val value: Long = deep[0][0][0]
            assertEquals(9L, value, "deep")
        }
        Dictionary<String, GodotArray<Long>>().use { map ->
            GodotArray<Long>().use { inner ->
                inner.add(3L)
                map["k"] = inner
            }
            val value: Long = checkNotNull(map["k"])[0]
            assertEquals(3L, value, "map")
        }
    }

    @Test
    fun nestedGetValueChecksTheElementTypeTheEngineStores() {
        GodotArray<Long>().use { longs ->
            longs.add(7L)
            Variant(longs).use { variant ->
                assertEquals(listOf(7L), variant.getValue<GodotArray<Long>>().use { it.toList() }, "same type")
                assertNull(variant.getValueOrNull<GodotArray<String>>(), "another element type")
                assertFailsWith<ClassCastException>("Array[int] as GodotArray<String>") {
                    variant.getValue<GodotArray<String>>()
                }
            }
        }
        VariantArray().use { untyped ->
            Variant(untyped).use { variant ->
                assertNull(variant.getValueOrNull<GodotArray<Long>>(), "an untyped Array is not an Array[int]")
                assertTrue(variant.getValue<VariantArray>().use { it.isEmpty() }, "but it is a VariantArray")
            }
        }
    }
}
