package io.github.kingg22.buildlogic.godot.test

import java.io.File

/**
 * Finds the Godot executable the same way `godot-version/track.sh` does: an explicit path, then `PATH`, then the
 * Downloads folder (matching the requested version). On macOS an `.app` bundle is resolved to its real binary.
 */
object GodotBinaryResolver {
    /**
     * @param explicit `-Pgodot.bin` / `GODOT_BIN`: an executable or, on macOS, an `.app` bundle.
     * @param version `-Pgodot.version` (`X.Y.Z`), used to pick a binary from the Downloads folder and to validate it.
     */
    fun resolve(explicit: String?, version: String?, home: File = File(System.getProperty("user.home"))): File {
        explicit?.takeIf(String::isNotBlank)?.let { path ->
            return resolveBundle(File(path)).takeIf { it.canExecute() }
                ?: error("Godot binary '$path' (godot.bin / GODOT_BIN) is not an executable file")
        }
        if (version == null) findInPath()?.let { return it }
        findInDownloads(home, version)?.let { return it }
        // A version was requested but no matching download exists: a PATH binary is only acceptable if it matches.
        findInPath()?.takeIf { version == null || reportedVersion(it)?.startsWith(version.substringBeforeLast('.')) == true }
            ?.let { return it }
        error(
            "Godot executable not found. Pass -Pgodot.bin=<path>, set GODOT_BIN, or put 'godot' on PATH" +
                (version?.let { " (requested version $it)" } ?: ""),
        )
    }

    /** `godot --version` output, e.g. `4.7.2.stable.official.abc123`, or null when it cannot be run. */
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

    private fun findInPath(): File? {
        val names = listOf("godot", "godot4")
        return System.getenv("PATH").orEmpty().split(File.pathSeparatorChar)
            .asSequence()
            .flatMap { dir -> names.asSequence().map { File(dir, it) } }
            .firstOrNull { it.isFile && it.canExecute() }
    }

    private fun findInDownloads(home: File, version: String?): File? {
        val tag = version?.let(::versionTag)
        return File(home, "Downloads").listFiles().orEmpty()
            .filter { it.name.contains("godot", ignoreCase = true) && (tag == null || it.name.contains(tag, ignoreCase = true)) }
            .map(::resolveBundle)
            .filter { it.isFile && it.canExecute() && !it.name.endsWith(".zip") }
            .maxByOrNull { it.absolutePath }
    }

    /** `4.7.2` -> `4.7.2-stable`, `4.7.0` -> `4.7-stable`, the naming of Godot's release assets. */
    internal fun versionTag(version: String): String {
        val (major, minor, patch) = version.split('.').let { Triple(it[0], it[1], it.getOrElse(2) { "0" }) }
        return if (patch == "0") "$major.$minor-stable" else "$major.$minor.$patch-stable"
    }
}
