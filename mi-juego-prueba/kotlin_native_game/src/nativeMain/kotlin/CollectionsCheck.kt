@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Dictionary
import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.PackedFloat32Array
import io.github.kingg22.godot.api.builtin.PackedInt64Array
import io.github.kingg22.godot.api.builtin.PackedStringArray
import io.github.kingg22.godot.api.builtin.PackedVector2Array
import io.github.kingg22.godot.api.builtin.Vector2
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.VariantArray
import io.github.kingg22.godot.api.builtin.VariantDictionary
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.builtin.toStringName
import io.github.kingg22.godot.api.builtin.toDictionary
import io.github.kingg22.godot.api.builtin.toGodotArray
import io.github.kingg22.godot.api.builtin.getValue
import io.github.kingg22.godot.api.core.Node
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Live checks of the `List` / `Map` views over Godot arrays and dictionaries (issue #119), run inside the real engine
 * (`collections_check.tscn`). Each check prints `PASS` or `FAIL`; the scene quits with a non-zero exit code if any failed.
 */
@Godot class CollectionsCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("CollectionsCheck")

    override fun _ready() {
        listChecks()
        constructorChecks()
        strictReadChecks()
        mapChecks()
        typingChecks()
        packedChecks()
        conversionChecks()
        engineReturnChecks()
        equalityChecks()
        hashChecks()
        getTree().quit(checks.finish())
    }

    private fun listChecks() = with(checks) {
        check("list: add, size and get") {
            GodotArray<Long>().use { list ->
                list.add(1L)
                list.add(2L)
                list.add(3L)
                expect(list.size == 3 && list[0] == 1L && list[2] == 3L) { "size=${list.size} list=${list.toList()}" }
            }
        }
        check("list: set, add at index, removeAt, contains, indexOf") {
            GodotArray<Long>().use { list ->
                list.addAll(listOf(10L, 20L, 30L))
                list[1] = 25L
                list.add(0, 5L)
                val removed = list.removeAt(3)
                expect(list.toList() == listOf(5L, 10L, 25L) && removed == 30L) {
                    "list=${list.toList()} removed=$removed"
                }
                expect(list.contains(25L) && list.indexOf(10L) == 1 && !list.contains(99L)) { "contains/indexOf" }
            }
        }
        check("list: iteration, sum and subList") {
            GodotArray<Long>().use { list ->
                list.addAll(listOf(1L, 2L, 3L, 4L))
                var sum = 0L
                for (value in list) sum += value
                expect(sum == 10L && list.sum() == 10L && list.subList(1, 3).toList() == listOf(2L, 3L)) { "sum=$sum" }
            }
        }
        check("list: remove(element), clear and isEmpty") {
            GodotArray<Long>().use { list ->
                list.addAll(listOf(7L, 8L, 7L))
                list.remove(7L)
                expect(list.toList() == listOf(8L, 7L)) { "list=${list.toList()}" }
                list.clear()
                expect(list.isEmpty() && list.size == 0) { "not empty after clear" }
            }
        }
        check("list: out of range index throws") {
            GodotArray<Long>().use { list ->
                list.add(1L)
                expectThrows<IndexOutOfBoundsException>("get(1)") { list[1] }
                expectThrows<IndexOutOfBoundsException>("get(-1)") { list[-1] }
                expectThrows<IndexOutOfBoundsException>("add(3, x)") { list.add(3, 9L) }
            }
        }
        check("list: Int elements and nullable elements") {
            GodotArray<Int>().use { ints ->
                ints.add(42)
                expect(ints[0] == 42) { "int=${ints[0]}" }
            }
            GodotArray<Long?>().use { nullable ->
                nullable.add(null)
                nullable.add(4L)
                expect(nullable[0] == null && nullable[1] == 4L) { "nullable=${nullable.toList()}" }
            }
        }
        check("list: String elements") {
            GodotArray<String>().use { names ->
                names.addAll(listOf("a", "bc"))
                expect(names.toList() == listOf("a", "bc")) { "names=${names.toList()}" }
            }
        }
        check("list: nested array is a new view over the same storage") {
            GodotArray<VariantArray>().use { outer ->
                VariantArray().use { inner ->
                    inner.add(Variant.from(1L))
                    outer.add(inner)
                }
                expect(outer.size == 1 && outer[0].size == 1) { "outer=${outer.size}" }
            }
        }
        check("list: raw Variant access keeps working") {
            GodotArray<Long>().use { list ->
                list.add(6L)
                list.getVariant(0L).use { raw -> expect(raw.getType() == Variant.Type.INT) { "type=${raw.getType()}" } }
            }
        }
    }

    private fun constructorChecks() = with(checks) {
        check("factory: copy of another array shares its elements") {
            VariantArray().use { source ->
                source.add(Variant.from(4L))
                source.add(Variant.from(5L))
                GodotArray<Long>(source).use { copy -> expect(copy.toList() == listOf(4L, 5L)) { "copy=${copy.toList()}" } }
            }
        }
        check("factory: array from a PackedInt64Array") {
            PackedInt64Array().use { packed ->
                // Godot's `append` returns true when it FAILED.
                expect(!(packed.append(7L) || packed.append(8L))) { "append to the packed array" }
                GodotArray<Long>(packed).use { list -> expect(list.toList() == listOf(7L, 8L)) { "list=${list.toList()}" } }
            }
        }
        check("factory: dictionary copy shares its entries") {
            VariantDictionary().use { source ->
                expect(source.setVariant(Variant.from("k"), Variant.from(3L))) { "set the source entry" }
                Dictionary<String, Long>(source).use { copy -> expect(copy["k"] == 3L) { "copy[k]=${copy["k"]}" } }
            }
        }
        check("getValue: NIL is null for a nullable type and a mismatch otherwise") {
            Variant().use { nil ->
                expect(nil.getValue<Long?>() == null) { "nullable NIL" }
                expectThrows<ClassCastException>("NIL as Long") { nil.getValue<Long>() }
            }
        }
    }

    private fun strictReadChecks() = with(checks) {
        check("strict: a String element read as Long throws ClassCastException") {
            VariantArray().use { untyped ->
                untyped.add(Variant.from("text"))
                GodotArray<Long?>(untyped.rawPtr).use { asLongs ->
                    expectThrows<ClassCastException>("read String as Long") { asLongs[0] }
                }
            }
        }
        check("strict: a Long that does not fit in Int throws ArithmeticException") {
            VariantArray().use { untyped ->
                untyped.add(Variant.from(5_000_000_000L))
                GodotArray<Int?>(untyped.rawPtr).use { asInts ->
                    expectThrows<ArithmeticException>("5e9 as Int") { asInts[0] }
                }
            }
        }
        check("strict: NIL reads as null only for a nullable element type") {
            VariantArray().use { untyped ->
                untyped.add(Variant())
                GodotArray<Long?>(untyped.rawPtr).use { asLongs -> expect(asLongs[0] == null) { "NIL as Long?" } }
                expectThrows<IllegalStateException>("untyped array as non-nullable Long") {
                    GodotArray<Long>(untyped.rawPtr)
                }
            }
        }
    }

    private fun mapChecks() = with(checks) {
        check("map: put, get, size and containsKey") {
            Dictionary<String, Long>().use { map ->
                map["a"] = 1L
                map["b"] = 2L
                expect(map.size == 2 && map["a"] == 1L && map.containsKey("b")) {
                    "map=${map.entries.map { it.key to it.value }}"
                }
            }
        }
        check("map: missing key reads null and logs no engine error") {
            Dictionary<String, Long>().use { map ->
                expect(map["nope"] == null && !map.containsKey("nope")) { "missing key" }
            }
        }
        check("map: put returns the previous value, remove returns it too") {
            Dictionary<String, Long>().use { map ->
                map["a"] = 1L
                val previous = map.put("a", 9L)
                val removed = map.remove("a")
                expect(previous == 1L && removed == 9L && map.isEmpty()) { "previous=$previous removed=$removed" }
            }
        }
        check("map: entries, keys and values iterate a snapshot") {
            Dictionary<String, Long>().use { map ->
                map["x"] = 10L
                map["y"] = 20L
                val pairs = map.entries.map { it.key to it.value }.toSet()
                expect(pairs == setOf("x" to 10L, "y" to 20L)) { "pairs=$pairs" }
                expect(map.keys == setOf("x", "y") && map.values.sum() == 30L) {
                    "keys=${map.keys} values=${map.values}"
                }
            }
        }
        check("map: entry setValue writes through, iterator remove erases") {
            Dictionary<String, Long>().use { map ->
                map["k"] = 1L
                map.entries.first().setValue(5L)
                expect(map["k"] == 5L) { "after setValue=${map["k"]}" }
                val iterator = map.entries.iterator()
                expect(iterator.next().key == "k") { "first entry" }
                iterator.remove()
                expect(map.isEmpty()) { "after iterator.remove" }
            }
        }
        check("map: nullable values and raw Variant access") {
            Dictionary<String, Long?>().use { map ->
                map["n"] = null
                expect(map.containsKey("n") && map["n"] == null) { "null value" }
            }
        }
    }

    private fun typingChecks() = with(checks) {
        check("typing: factory types the array in the engine") {
            GodotArray<Long>().use { list ->
                expect(list.isTyped() && list.getTypedBuiltin() == Variant.Type.INT.value) { "builtin=${list.getTypedBuiltin()}" }
            }
            GodotArray<String>().use { list ->
                expect(list.getTypedBuiltin() == Variant.Type.STRING.value) { "builtin=${list.getTypedBuiltin()}" }
            }
        }
        check("typing: Variant and nullable elements stay untyped") {
            GodotArray<Variant>().use { expect(!it.isTyped()) { "Variant typed" } }
            GodotArray<Long?>().use { expect(!it.isTyped()) { "Long? typed" } }
        }
        check("typing: nested arrays are typed as ARRAY") {
            GodotArray<GodotArray<Long>>().use { list ->
                expect(list.getTypedBuiltin() == Variant.Type.ARRAY.value) { "builtin=${list.getTypedBuiltin()}" }
            }
        }
        check("typing: factory types the dictionary key and value") {
            Dictionary<String, Long>().use { map ->
                expect(map.getTypedKeyBuiltin() == Variant.Type.STRING.value) { "key=${map.getTypedKeyBuiltin()}" }
                expect(map.getTypedValueBuiltin() == Variant.Type.INT.value) { "value=${map.getTypedValueBuiltin()}" }
            }
        }
        check("typing: wrapping an engine array with another type fails") {
            GodotArray<String>().use { strings ->
                expectThrows<IllegalStateException>("wrap String array as Long") { GodotArray<Long>(strings.rawPtr) }
            }
            VariantArray().use { untyped ->
                expectThrows<IllegalStateException>("wrap untyped array as Long") { GodotArray<Long>(untyped.rawPtr) }
            }
        }
        check("typing: wrapping with the right type works") {
            GodotArray<Long>().use { longs ->
                longs.add(7L)
                val view = GodotArray<Long>(longs.rawPtr)
                expect(view[0] == 7L) { "view=${view.toList()}" }
            }
        }
    }

    private fun packedChecks() = with(checks) {
        check("packed: PackedInt64Array is a MutableList") {
            PackedInt64Array().use { packed ->
                val list: MutableList<Long> = packed
                list.add(3L)
                list.add(1L)
                list.add(0, 9L)
                expect(list.size == 3 && list == listOf(9L, 3L, 1L)) { "list=$list" }
                expect(list.set(1, 4L) == 3L && packed[1L] == 4L) { "set" }
                expect(list.removeAt(0) == 9L && list.toList() == listOf(4L, 1L)) { "removeAt=$list" }
                expect(list.indexOf(1L) == 1 && 4L in list && list.sum() == 5L) { "search" }
                expectThrows<IndexOutOfBoundsException>("get(5)") { list[5] }
            }
        }
        check("packed: float, string and vector arrays") {
            PackedFloat32Array().use { floats ->
                floats.add(1.5)
                expect(floats[0] == 1.5 && floats.size == 1) { "floats=$floats" }
            }
            PackedStringArray().use { strings ->
                strings.add("a")
                strings.add("b")
                expect(strings.toList() == listOf("a", "b") && strings.joinToString() == "a, b") { "strings=$strings" }
            }
            PackedVector2Array().use { vectors ->
                vectors.add(Vector2(1.0, 2.0))
                expect(vectors.first() == Vector2(1.0, 2.0)) { "vectors" }
            }
        }
        check("packed: iteration, isEmpty, clear and write through the shared buffer") {
            PackedInt64Array().use { packed ->
                (1L..4L).forEach { packed.add(it) }
                expect(packed.sum() == 10L && packed.map { it * 2 } == listOf(2L, 4L, 6L, 8L)) { "iteration" }
                packed.clear()
                expect(packed.isEmpty() && packed.size == 0) { "clear" }
            }
        }
    }

    private fun conversionChecks() = with(checks) {
        check("convert: List to a typed GodotArray, a copy") {
            val source = mutableListOf(1L, 2L, 3L)
            source.toGodotArray().use { array ->
                source.add(4L)
                expect(array.toList() == listOf(1L, 2L, 3L) && array.getTypedBuiltin() == Variant.Type.INT.value) {
                    "array=${array.toList()}"
                }
            }
        }
        check("convert: Map to a typed Dictionary") {
            mapOf("a" to 1L, "b" to 2L).toDictionary().use { map ->
                expect(map["a"] == 1L && map["b"] == 2L && map.size == 2) { "map=${map.entries.map { it.key to it.value }}" }
                expect(map.getTypedKeyBuiltin() == Variant.Type.STRING.value) { "key type" }
            }
        }
        check("convert: empty collections") {
            emptyList<String>().toGodotArray().use { expect(it.isEmpty()) { "array" } }
            emptyMap<String, Long>().toDictionary().use { expect(it.isEmpty()) { "dictionary" } }
        }
    }

    private fun engineReturnChecks() = with(checks) {
        check("engine returns: typedarray::StringName is typed and readable") {
            addToGroup("probe".toStringName())
            getGroups().use { groups ->
                expect(groups.getTypedBuiltin() == Variant.Type.STRING_NAME.value) { "builtin=${groups.getTypedBuiltin()}" }
                expect(groups.any { it.toString() == "probe" }) { "groups=${groups.map { it.toString() }}" }
            }
        }
        check("engine returns: typedarray::Dictionary is typed and readable") {
            getPropertyList().use { properties ->
                expect(properties.getTypedBuiltin() == Variant.Type.DICTIONARY.value) { "builtin" }
                expect(properties.isNotEmpty()) { "no properties" }
            }
        }
    }

    /**
     * `==` of the Godot collections. The rule: against a [Variant] or against its own Godot type the engine compares
     * (by content, `1` and `1.0` are equal there); against anything else, a Kotlin `List` or `Map`, the inherited
     * Kotlin equality runs. The compiler gives no diagnostic for `==` between these types (they are open, a subtype of
     * both could exist), so the cases below are the documentation of what each one does.
     */
    private fun equalityChecks() = with(checks) {
        // ── GodotArray ──
        check("equality array: equal to a List, to another GodotArray and to a VariantArray") {
            GodotArray<Long>().use { array ->
                array.addAll(listOf(1L, 2L, 3L))
                expect(array == listOf(1L, 2L, 3L) && listOf(1L, 2L, 3L) == array) { "List" }
                GodotArray<Long>().use { other ->
                    other.addAll(listOf(1L, 2L, 3L))
                    expect(array == other && other == array) { "GodotArray<Long>" }
                }
                VariantArray().use { untyped ->
                    for (i in 1L..3L) Variant.from(i).use { untyped.add(it) }
                    expect(array == untyped) { "VariantArray" }
                }
                Variant(array).use { variant -> expect(array == variant) { "Variant" } }
            }
        }
        check("equality array: negatives") {
            GodotArray<Long>().use { array ->
                array.addAll(listOf(1L, 2L, 3L))
                expect(array != listOf(1L, 2L)) { "shorter List" }
                expect(array != listOf(1L, 2L, 4L)) { "different element" }
                expect(array != listOf(3L, 2L, 1L)) { "different order" }
                expect(array != listOf("1", "2", "3")) { "List of another type" }
                val notACollection: Any = "text" // `array != "text"` does not compile: String is final, so the types are incompatible
                expect(array != notACollection && !array.equals(null)) { "not a collection, null" }
                GodotArray<Long>().use { other ->
                    other.addAll(listOf(1L, 2L, 4L))
                    expect(array != other) { "GodotArray with another element" }
                }
                Variant.from(5L).use { variant -> expect(!array.equals(variant)) { "Variant of another type" } }
                Variant(array).use { variant -> expect(listOf(1L, 2L, 3L) != variant) { "List vs Variant" } }
            }
        }
        // ── Dictionary ──
        check("equality dictionary: equal to a Map, to another Dictionary and to a VariantDictionary") {
            Dictionary<String, Long>().use { map ->
                map["a"] = 1L
                map["b"] = 2L
                expect(map == mapOf("a" to 1L, "b" to 2L) && mapOf("b" to 2L, "a" to 1L) == map) { "Map" }
                Dictionary<String, Long>().use { other ->
                    other["b"] = 2L // another insertion order
                    other["a"] = 1L
                    expect(map == other && other == map) { "Dictionary<String, Long>" }
                }
                VariantDictionary().use { untyped ->
                    Variant.from("a").use { k -> Variant.from(1L).use { v -> untyped[k] = v } }
                    Variant.from("b").use { k -> Variant.from(2L).use { v -> untyped[k] = v } }
                    expect(map == untyped) { "VariantDictionary" }
                }
                Variant(map).use { variant -> expect(map == variant) { "Variant" } }
            }
        }
        check("equality dictionary: negatives") {
            Dictionary<String, Long>().use { map ->
                map["a"] = 1L
                map["b"] = 2L
                expect(map != mapOf("a" to 1L)) { "smaller Map" }
                expect(map != mapOf("a" to 1L, "b" to 3L)) { "different value" }
                expect(map != mapOf("a" to 1L, "c" to 2L)) { "different key" }
                expect(map != listOf("a", "b") && !map.equals(null)) { "not a map, null" }
                Dictionary<String, Long>().use { other ->
                    other["a"] = 1L
                    expect(map != other) { "Dictionary with fewer entries" }
                }
                Variant.from("a").use { variant -> expect(!map.equals(variant)) { "Variant of another type" } }
                Variant(map).use { variant -> expect(mapOf("a" to 1L, "b" to 2L) != variant) { "Map vs Variant" } }
            }
        }
        // ── Packed*Array ──
        check("equality packed: equal to a List, to another packed of the same type and to a Variant") {
            PackedInt64Array().use { packed ->
                packed.addAll(listOf(5L, 6L))
                expect(packed == listOf(5L, 6L) && listOf(5L, 6L) == packed) { "List" }
                PackedInt64Array().use { other ->
                    other.addAll(listOf(5L, 6L))
                    expect(packed == other && other == packed) { "PackedInt64Array" }
                }
                Variant(packed).use { variant -> expect(packed == variant) { "Variant" } }
            }
            PackedStringArray().use { strings ->
                strings.addAll(listOf("x", "y"))
                expect(strings == listOf("x", "y")) { "strings vs List" }
                Variant(strings).use { variant -> expect(strings == variant) { "strings vs Variant" } }
            }
            PackedVector2Array().use { vectors ->
                vectors.add(Vector2(1.0, 2.0))
                expect(vectors == listOf(Vector2(1.0, 2.0))) { "vectors vs List" }
            }
        }
        check("equality packed: negatives") {
            PackedInt64Array().use { packed ->
                packed.addAll(listOf(5L, 6L))
                expect(packed != listOf(5L)) { "shorter List" }
                expect(packed != listOf(5L, 7L)) { "different element" }
                expect(packed != listOf(6L, 5L)) { "different order" }
                expect(packed != listOf("5", "6") && !packed.equals(null)) { "List of another type, null" }
                PackedInt64Array().use { other ->
                    other.addAll(listOf(5L, 7L))
                    expect(packed != other) { "packed with another element" }
                }
                PackedStringArray().use { strings ->
                    strings.addAll(listOf("5", "6"))
                    expect(!packed.equals(strings)) { "packed of another type" }
                }
                Variant.from(5L).use { variant -> expect(!packed.equals(variant)) { "Variant of another type" } }
                Variant(packed).use { variant -> expect(listOf(5L, 6L) != variant) { "List vs Variant" } }
            }
        }
    }

    /**
     * `hashCode()` of the Godot collections is the one of a Kotlin `List` or `Map` (the inherited one), not the engine's,
     * so a collection equal to a `List` or `Map` has the same hash. The engine's own hash stays available as `hash()`.
     */
    private fun hashChecks() = with(checks) {
        check("hash: a Godot collection hashes like the List or Map it is equal to") {
            GodotArray<Long>().use { array ->
                array.addAll(listOf(1L, 2L, 3L))
                expect(array.hashCode() == listOf(1L, 2L, 3L).hashCode()) { "GodotArray" }
            }
            Dictionary<String, Long>().use { map ->
                map["a"] = 1L
                map["b"] = 2L
                expect(map.hashCode() == mapOf("a" to 1L, "b" to 2L).hashCode()) { "Dictionary" }
            }
            PackedInt64Array().use { packed ->
                packed.addAll(listOf(5L, 6L))
                expect(packed.hashCode() == listOf(5L, 6L).hashCode()) { "PackedInt64Array" }
            }
            PackedStringArray().use { strings ->
                strings.addAll(listOf("x", "y"))
                expect(strings.hashCode() == listOf("x", "y").hashCode()) { "PackedStringArray" }
            }
        }
        check("hash: equal Godot collections hash the same, different ones usually differ") {
            GodotArray<Long>().use { a ->
                GodotArray<Long>().use { b ->
                    a.addAll(listOf(1L, 2L))
                    b.addAll(listOf(1L, 2L))
                    expect(a.hashCode() == b.hashCode()) { "equal arrays" }
                    b.add(3L)
                    expect(a.hashCode() != b.hashCode()) { "different arrays" }
                }
            }
            Dictionary<String, Long>().use { a ->
                Dictionary<String, Long>().use { b ->
                    a["k"] = 1L
                    b["k"] = 1L
                    expect(a.hashCode() == b.hashCode()) { "equal dictionaries" }
                    b["k"] = 2L
                    expect(a.hashCode() != b.hashCode()) { "different dictionaries" }
                }
            }
        }
        check("hash: the engine hash() is separate from hashCode(), printed") {
            GodotArray<Long>().use { array ->
                array.addAll(listOf(1L, 2L, 3L))
                val engine = array.hash()
                println("[CollectionsCheck] GodotArray hashCode=${array.hashCode()} engine hash()=$engine")
                GodotArray<Long>().use { other ->
                    other.addAll(listOf(1L, 2L, 3L))
                    expect(engine == other.hash()) { "the engine hash of equal arrays agrees" }
                }
            }
        }
    }
}
