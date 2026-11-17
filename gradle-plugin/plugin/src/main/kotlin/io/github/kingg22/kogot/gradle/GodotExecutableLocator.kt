package io.github.kingg22.kogot.gradle

import java.io.File

/**
 * The one way every kogot task finds the Godot executable (`runGodot`, `testGodot`, the Godot test launcher), in this
 * order: the explicit value (`kogot.godotExecutable`), the `GODOT_BIN` environment variable, then `godot4` / `godot` on
 * `PATH`. On macOS the explicit value or `GODOT_BIN` may be the `.app` bundle: it is resolved to its real binary, like
 * `godot-version/track.sh` does.
 */
object GodotExecutableLocator {
    const val ENV_VARIABLE = "GODOT_BIN"

    /** The executable, or null when none is configured and none is on `PATH`. A configured value must exist. */
    fun locate(explicit: String?, env: Map<String, String> = System.getenv()): File? {
        val configured = explicit?.takeIf(String::isNotBlank) ?: env[ENV_VARIABLE]?.takeIf(String::isNotBlank)
        if (configured != null) {
            return resolveBundle(File(configured)).takeIf { it.isFile && it.canExecute() }
                ?: error("Godot executable '$configured' is not an executable file")
        }
        return env["PATH"].orEmpty().split(File.pathSeparatorChar).asSequence()
            .flatMap { dir -> KogotConventions.GODOT_EXECUTABLE_CANDIDATES.asSequence().map { File(dir, it) } }
            .firstOrNull { it.isFile && it.canExecute() }
    }

    private fun resolveBundle(file: File): File {
        if (!file.isDirectory || !file.name.endsWith(".app")) return file
        val macos = File(file, "Contents/MacOS")
        return File(macos, "Godot").takeIf { it.canExecute() }
            ?: macos.listFiles { f -> f.isFile && f.canExecute() }?.firstOrNull()
            ?: file
    }
}
