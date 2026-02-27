package io.github.kingg22.jextract.gradle.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.options.Option
import org.gradle.process.CommandLineArgumentProvider

/**
 * Escape hatch: passes arbitrary arguments directly to the jextract binary.
 *
 * ```
 * ./gradlew jextractRun --args="--help"
 * ./gradlew jextractRun --args="--version"
 * ./gradlew jextractRun --args="-t com.example -l GL include/gl.h --output build/out"
 * ```
 */
abstract class JextractRunTask : Exec() {

    @get:[InputFile PathSensitive(PathSensitivity.ABSOLUTE)]
    abstract val jextractBinary: RegularFileProperty

    @get:[Input Option(option = "args", description = "Arguments forwarded verbatim to jextract (space-separated)")]
    abstract val rawArgs: ListProperty<String>

    init {
        standardOutput = System.out
        logging.captureStandardOutput(LogLevel.INFO)
        argumentProviders += CommandLineArgumentProvider { rawArgs.get() }
    }

    override fun exec() {
        if (rawArgs.get().isEmpty()) logger.warn("jextractRun: no --args provided; jextract will print its usage.")
        executable = jextractBinary.get().asFile.absolutePath
        super.exec()
    }

    companion object {
        const val NAME = "jextractRun"
    }
}
