package io.github.kingg22.jextract.gradle.tasks

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/** Prints the jextract `--version` string. */
abstract class JextractVersionTask : Exec() {
    @get:[InputFile PathSensitive(PathSensitivity.ABSOLUTE)]
    abstract val jextractBinary: RegularFileProperty

    init {
        args("--version")
        standardOutput = System.out
        logging.captureStandardOutput(LogLevel.INFO)
    }

    override fun exec() {
        executable = jextractBinary.get().asFile.absolutePath
        super.exec()
    }

    companion object {
        const val NAME = "jextractVersion"
    }
}
