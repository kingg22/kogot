@file:OptIn(ExperimentalForeignApi::class, ExperimentalGodotKotlin::class)

import io.github.kingg22.godot.api.ExperimentalGodotKotlin
import io.github.kingg22.godot.api.annotations.Godot
import io.github.kingg22.godot.api.builtin.Variant
import io.github.kingg22.godot.api.builtin.from
import io.github.kingg22.godot.api.core.Node
import io.github.kingg22.godot.api.singleton.Os
import io.github.kingg22.godot.api.singleton.ProjectSettings
import io.github.kingg22.godot.api.singleton.Time
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Live check of the `String` conveniences over `GodotString` (`string_leak_check.tscn`, issue #211): the generated
 * `String` overloads and `Variant.toString()` convert through a `GodotString` they own, and must close it. Each case
 * picks a call whose engine string is a fresh allocation (a shared copy would not show in the engine memory), and
 * runs it 20,000 times: before #211 every call leaked one or two engine strings.
 */
@Godot class StringLeakCheck(nativePtr: COpaquePointer) : Node(nativePtr) {
    private val checks = Checks("StringLeakCheck")

    override fun _ready() {
        val settings = ProjectSettings.instance
        val time = Time.instance
        with(checks) {
            check("the String overloads still convert both ways") {
                val path = settings.globalizePath("res://string_leak_probe")
                expect(path.endsWith("/string_leak_probe") && !path.startsWith("res://")) { "path=$path" }
                expect(settings.hasSetting("application/config/name")) { "hasSetting=false" }
                Variant.from(123456L).use { expect(it.toString() == "123456") { "toString=$it" } }
            }
            noGrowth("a String parameter (ProjectSettings.hasSetting)") {
                settings.hasSetting("string_leak_check/a_setting_name_long_enough_to_show")
            }
            noGrowth("a String return (Time.getDatetimeStringFromSystem)") { time.getDatetimeStringFromSystem() }
            noGrowth("a String parameter and return (ProjectSettings.globalizePath)") {
                settings.globalizePath("res://string_leak_check/a_path_long_enough_to_show")
            }
            Variant.from(1234567890123L).use { variant ->
                noGrowth("Variant.toString()") { variant.toString() }
            }
        }
        getTree().quit(checks.finish())
    }

    private fun Checks.noGrowth(name: String, call: () -> Any) = check("$name does not leak") {
        val os = Os.instance
        val _ = call() // warm up
        val before = os.getStaticMemoryUsage().toLong()
        repeat(20_000) { val _ = call() }
        val growth = os.getStaticMemoryUsage().toLong() - before
        println("[StringLeakCheck] $name growth=$growth bytes over 20000 calls")
        expect(growth < 100_000) { "static memory grew by $growth bytes" }
    }
}
