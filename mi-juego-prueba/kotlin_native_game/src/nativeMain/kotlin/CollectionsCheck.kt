@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Dictionary
import io.github.kingg22.godot.api.builtin.GodotArray
import io.github.kingg22.godot.api.builtin.PackedInt64Array
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.VariantArray
import io.github.kingg22.godot.api.builtin.VariantDictionary
import io.github.kingg22.godot.api.builtin.from
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
                GodotArray<Long>(untyped.rawPtr).use { asLongs ->
                    expectThrows<ClassCastException>("read String as Long") { asLongs[0] }
                }
            }
        }
        check("strict: a Long that does not fit in Int throws ArithmeticException") {
            VariantArray().use { untyped ->
                untyped.add(Variant.from(5_000_000_000L))
                GodotArray<Int>(untyped.rawPtr).use { asInts ->
                    expectThrows<ArithmeticException>("5e9 as Int") { asInts[0] }
                }
            }
        }
        check("strict: NIL read as a non-nullable type throws") {
            VariantArray().use { untyped ->
                untyped.add(Variant())
                GodotArray<Long>(untyped.rawPtr).use { asLongs ->
                    expectThrows<ClassCastException>("NIL as Long") { asLongs[0] }
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
}
