package io.github.kingg22.godot.codegen.runner

import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
import io.github.kingg22.godot.codegen.runner.impl.KotlinNativeApiRunner
import io.github.kingg22.godot.codegen.runner.impl.KotlinNativeRuntimeRunner
import org.slf4j.Logger

data object CodegenRegistry {
    private val runners = mutableListOf<CodegenRunner>()

    @JvmStatic
    fun register(runner: CodegenRunner) {
        runners += runner
    }

    fun registerDefaultRunners(logger: Logger) {
        register(KotlinNativeApiRunner(logger))
        register(KotlinNativeRuntimeRunner(logger))
        // futuros:
        // CodegenRegistry.register(JavaFfmApiRunner(logger))
    }

    @JvmStatic
    fun find(backend: GeneratorBackend, kind: GeneratorKind): CodegenRunner = runners.firstOrNull {
        it.backend == backend && it.kind == kind
    } ?: error("No runner for backend=$backend kind=$kind")
}
