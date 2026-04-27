package io.github.kingg22.godot.codegen.runner.impl

import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
import io.github.kingg22.godot.codegen.runner.CodegenRunner
import io.github.kingg22.godot.codegen.runner.CodegenRunnerProvider

class KotlinNativeRuntimeRunnerProvider : CodegenRunnerProvider {
    override val backend = GeneratorBackend.KOTLIN_NATIVE
    override val kind = GeneratorKind.RUNTIME

    override fun createRunner(): CodegenRunner = KotlinNativeRuntimeRunner()
}
