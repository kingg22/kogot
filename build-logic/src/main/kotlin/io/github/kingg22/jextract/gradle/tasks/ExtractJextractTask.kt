package io.github.kingg22.jextract.gradle.tasks

import io.github.kingg22.jextract.gradle.internal.JextractPlatform
import io.github.kingg22.jextract.gradle.internal.verifySha256
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Extracts the jextract `.tar.gz` archive after verifying its SHA-256 checksum.
 *
 * ## Key design: `jextractBinary` is derived at configuration time
 *
 * The binary path is fully known before the task runs — it is computed from
 * [outputDir] + [extractedDirName] + the current platform.  This means:
 *
 *  - Gradle can validate the property before execution (no "value not set" errors).
 *  - Downstream tasks can wire `extractTask.flatMap { it.jextractBinary }` as a
 *    lazy `Provider<RegularFile>` that carries the task dependency automatically.
 *  - The file does not have to *exist* yet at configuration time; Gradle only
 *    requires the *path* to be declared.
 *
 * Declaring it `@OutputFile` (rather than `@Internal`) also means Gradle's
 * up-to-date check covers the binary itself — if it is deleted, the task reruns.
 */
@CacheableTask
abstract class ExtractJextractTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archiveFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sha256File: RegularFileProperty

    /** The directory name produced inside the archive, e.g. `"jextract-25"`. */
    @get:Input
    abstract val extractedDirName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    /**
     * Path to the jextract executable — derived from [outputDir] and
     * [extractedDirName] at *configuration time*, so it is always set before
     * Gradle validates task properties.
     *
     * The value is wired in the plugin via [jextractBinary]:
     * ```kotlin
     * val jextractBinary: Provider<RegularFile> = extractTask.flatMap { it.jextractBinary }
     * ```
     */
    @get:Internal
    val jextractBinary: Provider<RegularFile> = outputDir.zip(extractedDirName) { dir, name ->
        val home = dir.asFile.resolve(name)
        val binary = JextractPlatform.resolveJextractBinary(home)
        dir.file(binary.relativeTo(dir.asFile).path)
    }

    @TaskAction
    fun extract() {
        val archive = archiveFile.get().asFile
        val dest = outputDir.get().asFile

        // ── Skip if already extracted ─────────────────────────────────────────
        val binDir = dest.resolve("${extractedDirName.get()}/bin")
        if (binDir.exists() && binDir.listFiles()?.isNotEmpty() == true) {
            logger.debug("jextract: already extracted at ${dest.absolutePath}, skipping.")
            state.didWork = false
            return
        }

        // ── SHA-256 verification ──────────────────────────────────────────────
        val expectedDigest = sha256File.get().asFile.readText().trim()
        logger.lifecycle("jextract: verifying SHA-256 of ${archive.name} …")
        verifySha256(archive, archive.name, expectedDigest)
        logger.lifecycle("jextract: SHA-256 OK.")

        // ── Extract ───────────────────────────────────────────────────────────
        logger.lifecycle("jextract: extracting ${archive.name} → ${dest.absolutePath} …")
        dest.mkdirs()

        val systemTar = findSystemTar()
        if (systemTar != null) {
            extractWithTar(systemTar, archive, dest)
        } else {
            extractWithCommonsCompress(archive, dest)
        }

        logger.lifecycle("jextract: extraction complete.")
        removeMacQuarantine(dest)

        val binary = jextractBinary.get().asFile
        check(binary.exists()) {
            "jextract binary not found after extraction: ${binary.absolutePath}"
        }
        if (!binary.canExecute()) binary.setExecutable(true)
    }

    // ── Extraction ─────────────────────────────────────────────────────────────

    private fun findSystemTar(): String? {
        for (candidate in listOf("/usr/bin/tar", "/bin/tar", "tar")) {
            runCatching {
                val p = ProcessBuilder(candidate, "--version").redirectErrorStream(true).start()
                p.waitFor(5, TimeUnit.SECONDS)
                if (p.exitValue() == 0) return candidate
            }.onFailure { exception ->
                logger.trace("jextract: failed to execute system tar on '$candidate'", exception)
            }
        }
        return null
    }

    private fun extractWithTar(tar: String, archive: File, dest: File) {
        val proc = ProcessBuilder(tar, "xzf", archive.absolutePath, "-C", dest.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        check(proc.waitFor(30, TimeUnit.MINUTES) && proc.exitValue() == 0) {
            "tar extraction failed:\n$output"
        }
    }

    private fun extractWithCommonsCompress(archive: File, dest: File) {
        val buffer = ByteArray(32_768)
        archive.inputStream().buffered().use { raw ->
            GZIPInputStream(raw).use { gz ->
                TarArchiveInputStream(gz).use { tar ->
                    var entry: TarArchiveEntry? = tar.nextEntry
                    while (entry != null) {
                        val outFile = File(dest, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().buffered().use { sink ->
                                var n = tar.read(buffer)
                                while (n != -1) {
                                    sink.write(buffer, 0, n)
                                    n = tar.read(buffer)
                                }
                            }
                            if (entry.mode and 0b001_000_000 != 0) outFile.setExecutable(true, false)
                            if (entry.mode and 0b000_001_000 != 0) outFile.setExecutable(true, true)
                        }
                        entry = tar.nextEntry
                    }
                }
            }
        }
    }

    // ── macOS quarantine ───────────────────────────────────────────────────────

    private fun removeMacQuarantine(dir: File) {
        if (!JextractPlatform.current.isMac) return
        logger.lifecycle("jextract: removing macOS quarantine from ${dir.absolutePath} …")
        runCatching {
            ProcessBuilder("xattr", "-r", "-d", "com.apple.quarantine", dir.absolutePath)
                .redirectErrorStream(true).start().waitFor(30, TimeUnit.SECONDS)
        }.onFailure { exception ->
            logger.warn("jextract: failed to remove quarantine", exception)
        }
    }

    companion object {
        const val NAME = "extractJextract"
    }
}
