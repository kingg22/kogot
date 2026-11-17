package io.github.kingg22.kogot.gradle.test

import java.io.File

/**
 * Finds the Godot executable the way `godot-version/track.sh` does. On macOS an `.app` bundle is resolved to
 * `Contents/MacOS/Godot`.
 */
internal object GodotBinaryResolver {
    fun resolve(explicit: String?, version: String?, env: Map<String, String> = System.getenv()): File {
        val requested = explicit?.takeIf(String::isNotBlank) ?: env["GODOT_BIN"]?.takeIf(String::isNotBlank)
        if (requested != null) {
            return resolveBundle(File(requested)).takeIf { it.isFile && it.canExecute() }
                ?: error("Godot binary '$requested' is not an executable file")
        }
        findInDownloads(version)?.let { return it }
        findInPath(env)?.let { return it }
        error(
            "Godot executable not found. Set kogotTest.godotBin / -Pgodot.bin / GODOT_BIN, or put 'godot' on PATH" +
                (version?.let { " (requested version $it)" } ?: ""),
        )
    }

    /** `godot --version`, e.g. `4.7.2.stable.official.abc123`. */
    fun reportedVersion(binary: File): String? = runCatching {
        val process = ProcessBuilder(binary.absolutePath, "--version").redirectErrorStream(true).start()
        val out = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        out.ifBlank { null }
    }.getOrNull()

    private fun resolveBundle(file: File): File {
        if (!file.isDirectory || !file.name.endsWith(".app")) return file
        val macos = File(file, "Contents/MacOS")
        return File(macos, "Godot").takeIf { it.canExecute() }
            ?: macos.listFiles { f -> f.isFile && f.canExecute() }?.firstOrNull()
            ?: file
    }

    private fun findInPath(env: Map<String, String>): File? {
        val names = listOf("godot", "godot4")
        return env["PATH"].orEmpty().split(File.pathSeparatorChar).asSequence()
            .flatMap { dir -> names.asSequence().map { File(dir, it) } }
            .firstOrNull { it.isFile && it.canExecute() }
    }

    /** Only with a requested version: an unversioned search would pick any download at random. */
    private fun findInDownloads(version: String?): File? {
        if (version == null) return null
        val tag = versionTag(version)
        return File(System.getProperty("user.home"), "Downloads").listFiles().orEmpty()
            .asSequence()
            .filter { it.name.contains("godot", ignoreCase = true) && it.name.contains(tag, ignoreCase = true) }
            .filterNot { it.name.endsWith(".zip") }
            .map(::resolveBundle)
            .filter { it.isFile && it.canExecute() }
            .maxByOrNull { it.absolutePath }
    }

    /** `4.7.2` -> `4.7.2-stable`, `4.7.0` -> `4.7-stable`: the naming of Godot's release assets. */
    internal fun versionTag(version: String): String {
        val parts = version.split('.')
        val patch = parts.getOrElse(2) { "0" }
        return if (patch == "0") "${parts[0]}.${parts[1]}-stable" else "${parts[0]}.${parts[1]}.$patch-stable"
    }
}
