@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

package io.github.kingg22.godot.test.leak

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.singleton.Os
import io.github.kingg22.godot.api.singleton.ProjectSettings
import io.github.kingg22.godot.api.singleton.Time
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.test.Test
import kotlin.test.assertTrue

private const val TAG = "StringLeakTest"

/**
 * The `String` conveniences over `GodotString` (issue #211): the generated `String` overloads and `Variant.toString()`
 * convert through a `GodotString` they own, and must close it. Each case picks a call whose engine string is a fresh
 * allocation (a shared copy would not show in the engine memory), and runs it 20,000 times: before #211 every call
 * leaked one or two engine strings. Ported from the former `StringLeakCheck` scene.
 */
class StringLeakTest {
    /** #211: the String overloads still convert both ways. */
    @Test
    fun stringOverloadsStillConvertBothWays() {
        val settings = ProjectSettings.instance
        val path = settings.globalizePath("res://string_leak_probe")
        assertTrue(path.endsWith("/string_leak_probe") && !path.startsWith("res://"), "path=$path")
        assertTrue(settings.hasSetting("application/config/name"), "hasSetting=false")
        Variant.from(123456L).use { assertTrue(it.toString() == "123456", "toString=$it") }
    }

    /** #211 */
    @Test
    fun stringParameterDoesNotLeak() {
        val settings = ProjectSettings.instance
        assertStaticMemoryNoGrowth("a String parameter (ProjectSettings.hasSetting)") {
            settings.hasSetting("string_leak_check/a_setting_name_long_enough_to_show")
        }
    }

    /** #211 */
    @Test
    fun stringReturnDoesNotLeak() {
        val time = Time.instance
        assertStaticMemoryNoGrowth("a String return (Time.getDatetimeStringFromSystem)") {
            time.getDatetimeStringFromSystem()
        }
    }

    /** #211 */
    @Test
    fun stringParameterAndReturnDoNotLeak() {
        val settings = ProjectSettings.instance
        assertStaticMemoryNoGrowth("a String parameter and return (ProjectSettings.globalizePath)") {
            settings.globalizePath("res://string_leak_check/a_path_long_enough_to_show")
        }
    }

    /** #211 */
    @Test
    fun variantToStringDoesNotLeak() {
        Variant.from(1234567890123L).use { variant ->
            assertStaticMemoryNoGrowth("Variant.toString()") { variant.toString() }
        }
    }

    /** Engine static memory only (these cases never touch the C heap): must stay under 100,000 bytes over 20,000 calls. */
    private fun assertStaticMemoryNoGrowth(name: String, call: () -> Any) {
        val os = Os.instance
        val _ = call() // warm up
        val before = os.getStaticMemoryUsage().toLong()
        repeat(20_000) { val _ = call() }
        val growth = os.getStaticMemoryUsage().toLong() - before
        println("[$TAG] $name growth=$growth bytes over 20000 calls")
        assertTrue(growth < 100_000, "$name leaks: static memory grew by $growth bytes")
    }
}
