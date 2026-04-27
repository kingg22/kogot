package io.github.kingg22.godot.codegen.runner

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
import io.github.kingg22.godot.codegen.utils.currentClassLogger
import io.github.kingg22.godot.codegen.utils.debug
import io.github.kingg22.godot.codegen.utils.info
import java.nio.file.Path
import java.util.concurrent.StructuredTaskScope
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.time.measureTime

abstract class AbstractCodegenRunner(
    final override val backend: GeneratorBackend,
    final override val kind: GeneratorKind,
) : CodegenRunner {
    protected val logger get() = currentClassLogger()

    final override fun run(config: CodegenConfig) {
        var generatedFilesCount = 0
        var totalFilesCount = 0

        val time = measureTime {
            logger.info { "Running codegen: backend=$backend, kind=$kind" }

            validate(config)
            val sequenceFiles = execute(config)

            StructuredTaskScope
                .ShutdownOnFailure("Godot CodeGen", Thread.ofVirtual().factory())
                .use { scope ->
                    try {
                        for (fileSpec in sequenceFiles) {
                            totalFilesCount++
                            scope.fork {
                                if (fileSpec.writeIfChanged(config.outputDir)) {
                                    generatedFilesCount++
                                }
                            }
                        }
                    } finally {
                        // Wait for all tasks to complete, ensure that all exceptions are handled
                        scope.join()
                        scope.exception().ifPresent { throw it }
                    }
                }
        }

        logger.info {
            "---Total generated: $generatedFilesCount, skipped: ${totalFilesCount - generatedFilesCount} in $time to ${config.outputDir} ---"
        }
    }

    protected open fun validate(config: CodegenConfig) {}

    protected abstract fun execute(config: CodegenConfig): Sequence<FileSpec>

    private fun FileSpec.writeIfChanged(directory: Path): Boolean {
        require(directory.notExists() || directory.isDirectory()) {
            "path $directory exists but is not a directory."
        }

        val outputPath = directory.resolve(relativePath)
        outputPath.parent.createDirectories()

        if (outputPath.exists()) {
            val oldContent = outputPath.readText()
            val appendable = StringBuilder()
            this.writeTo(appendable)
            if (oldContent == appendable.toString()) {
                logger.debug { "Skipping unchanged file: ${outputPath.name}" }
                return false
            }
        }

        outputPath.outputStream().bufferedWriter().use(::writeTo)
        return true
    }
}
