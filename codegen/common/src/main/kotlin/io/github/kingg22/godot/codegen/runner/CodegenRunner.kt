package io.github.kingg22.godot.codegen.runner

import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind

interface CodegenRunner {
    val backend: GeneratorBackend
    val kind: GeneratorKind

    fun run(config: CodegenConfig)
}
