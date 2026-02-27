package io.github.kingg22.jextract.gradle.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.process.CommandLineArgumentProvider

/**
 * Runs `jextract --dump-includes` to produce a symbols list file.
 *
 * The output lists every C symbol visible from the given headers.  Edit it
 * and feed it back as an `@argsfile`, or copy individual entries into the
 * typed `--include-*` filters in [JextractTask].
 */
@CacheableTask
abstract class JextractDumpIncludesTask : Exec() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val jextractBinary: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    abstract val headerFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val includeDirs: ConfigurableFileCollection

    @get:Input
    abstract val macros: MapProperty<String, String>

    /** `-F` directories (macOS only). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val frameworkDirs: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        argumentProviders += CommandLineArgumentProvider {
            buildList {
                for ((k, v) in macros.get()) {
                    add("--define-macro")
                    add(if (v.isBlank()) k else "$k=$v")
                }
                for (dir in includeDirs) {
                    add("--include-dir")
                    add(dir.absolutePath)
                }
                for (fDir in frameworkDirs) {
                    add("-F")
                    add(fDir.absolutePath)
                }
                add("--dump-includes")
                add(outputFile.get().asFile.absolutePath)
                for (h in headerFiles) add(h.absolutePath)
            }
        }
    }

    override fun exec() {
        outputFile.get().asFile.parentFile.mkdirs()
        executable = jextractBinary.get().asFile.absolutePath
        super.exec()
        logger.info("jextract: symbols list written to ${outputFile.get().asFile.absolutePath}")
    }

    companion object {
        const val NAME = "jextractDumpIncludes"
    }
}
