package io.github.kingg22.godot.codegen.runner

import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind

data object CodegenRegistry {
    private val runners = mutableListOf<CodegenRunner>()

    @JvmStatic
    fun register(runner: CodegenRunner) {
        runners += runner
    }

    @JvmStatic
    fun find(backend: GeneratorBackend, kind: GeneratorKind): CodegenRunner = runners.firstOrNull {
        it.backend == backend && it.kind == kind
    } ?: error("No runner for backend=$backend kind=$kind")
}
