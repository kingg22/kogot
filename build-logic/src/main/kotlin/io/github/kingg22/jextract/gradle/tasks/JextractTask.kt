package io.github.kingg22.jextract.gradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider

/**
 * Runs jextract to generate Java FFM source files from C header(s).
 *
 * The [jextractBinary] property is a [org.gradle.api.provider.Provider] wired directly to the
 * resolved Gradle artifact transform output — Gradle automatically tracks
 * the dependency so no explicit `dependsOn` is needed.
 */
@CacheableTask
abstract class JextractTask : Exec() {

    // ── Tool ──────────────────────────────────────────────────────────────────

    /**
     * Path to the `jextract` (or `jextract.bat` on Windows) executable.
     * Wired by the plugin to the transform-resolved binary.
     */
    @get:[InputFile PathSensitive(PathSensitivity.ABSOLUTE)]
    abstract val jextractBinary: RegularFileProperty

    // ── Header inputs ─────────────────────────────────────────────────────────

    @get:[InputFiles PathSensitive(PathSensitivity.ABSOLUTE) SkipWhenEmpty]
    abstract val headerFiles: ConfigurableFileCollection

    /** Optional @argsfile — a text file with additional flags, one per line. */
    @get:[InputFile PathSensitive(PathSensitivity.ABSOLUTE) Optional]
    abstract val argsFile: RegularFileProperty

    // ── CLI options ───────────────────────────────────────────────────────────

    @get:[Input Optional]
    abstract val packageName: Property<String>

    @get:[Input Optional]
    abstract val headerClassName: Property<String>

    @get:Input
    abstract val libraries: ListProperty<String>

    @get:[Input Optional]
    abstract val useSystemLoadLibrary: Property<Boolean>

    @get:[InputFiles PathSensitive(PathSensitivity.ABSOLUTE)]
    abstract val includeDirs: ConfigurableFileCollection

    @get:Input
    abstract val macros: MapProperty<String, String>

    // ── Symbol filters ────────────────────────────────────────────────────────

    @get:Input
    abstract val includeFunctions: ListProperty<String>

    @get:Input
    abstract val includeConstants: ListProperty<String>

    @get:Input
    abstract val includeStructs: ListProperty<String>

    @get:Input
    abstract val includeUnions: ListProperty<String>

    @get:Input
    abstract val includeTypedefs: ListProperty<String>

    @get:Input
    abstract val includeVars: ListProperty<String>

    // ── Advanced naming ───────────────────────────────────────────────────────

    @get:[Input Optional]
    abstract val symbolsClassName: Property<String>

    // ── macOS-only ────────────────────────────────────────────────────────────

    @get:[InputFiles PathSensitive(PathSensitivity.ABSOLUTE)]
    abstract val frameworkDirs: ConfigurableFileCollection

    @get:Input
    abstract val frameworks: ListProperty<String>

    // ── Output ────────────────────────────────────────────────────────────────

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:[OutputFile Optional]
    abstract val dumpIncludesFile: RegularFileProperty

    init {
        argumentProviders += JextractArgs()
        standardOutput = System.out
        logging.captureStandardOutput(LogLevel.INFO)
    }

    override fun exec() {
        val binary = jextractBinary.get().asFile
        check(binary.exists() && binary.canExecute()) { "jextract binary not executable: ${binary.absolutePath}" }

        val out = outputDir.get().asFile
        out.mkdirs()

        executable = binary.absolutePath

        super.exec()

        logger.lifecycle("jextract: generation complete → ${out.absolutePath}")
    }

    private inner class JextractArgs : CommandLineArgumentProvider {
        override fun asArguments(): Iterable<String> = buildList {
            argsFile.orNull?.asFile?.let { add("@${it.absolutePath}") }

            for ((key, value) in macros.get()) {
                add("--define-macro")
                add(if (value.isBlank()) key else "$key=$value")
            }
            for (dir in includeDirs) {
                add("--include-dir")
                add(dir.absolutePath)
            }
            for (lib in libraries.get()) {
                add("--library")
                add(lib)
            }

            if (useSystemLoadLibrary.orNull == true) add("--use-system-load-library")

            packageName.orNull?.let {
                add("--target-package")
                add(it)
            }
            headerClassName.orNull?.let {
                add("--header-class-name")
                add(it)
            }
            symbolsClassName.orNull?.let {
                add("--symbols-class-name")
                add(it)
            }
            dumpIncludesFile.orNull?.asFile?.let {
                add("--dump-includes")
                add(it.absolutePath)
            }

            for (fn in includeFunctions.get()) {
                add("--include-function")
                add(fn)
            }
            for (c in includeConstants.get()) {
                add("--include-constant")
                add(c)
            }
            for (s in includeStructs.get()) {
                add("--include-struct")
                add(s)
            }
            for (u in includeUnions.get()) {
                add("--include-union")
                add(u)
            }
            for (t in includeTypedefs.get()) {
                add("--include-typedef")
                add(t)
            }
            for (v in includeVars.get()) {
                add("--include-var")
                add(v)
            }

            for (fDir in frameworkDirs) {
                add("-F")
                add(fDir.absolutePath)
            }
            for (fw in frameworks.get()) {
                add("--framework")
                add(fw)
            }

            add("--output")
            add(outputDir.get().asFile.absolutePath)

            for (header in headerFiles) add(header.absolutePath)
        }
    }

    companion object {
        const val NAME = "jextract"
    }
}
