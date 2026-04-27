package io.github.kingg22.godot.codegen.runner

import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind

/**
 * Provider factory for CodegenRunner instances.
 * Discovered via Java ServiceLoader (META-INF/services).
 */
interface CodegenRunnerProvider {
    val backend: GeneratorBackend
    val kind: GeneratorKind

    /**
     * Creates a new CodegenRunner instance.
     * @param logger Gradle logger for the task
     */
    fun createRunner(): CodegenRunner
}
