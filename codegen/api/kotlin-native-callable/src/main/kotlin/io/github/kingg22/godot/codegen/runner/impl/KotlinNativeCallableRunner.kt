package io.github.kingg22.godot.codegen.runner.impl

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.callable.knative.KotlinNativeCallableGenerator
import io.github.kingg22.godot.codegen.callable.knative.KotlinNativeSignalGenerator
import io.github.kingg22.godot.codegen.callable.knative.NativeCallablePackageRegistry
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.runner.AbstractCodegenRunner
import io.github.kingg22.godot.codegen.utils.info

class KotlinNativeCallableRunner : AbstractCodegenRunner(KOTLIN_NATIVE, CALLABLE) {
    override fun execute(config: CodegenConfig): Sequence<FileSpec> {
        logger.info { "---Generating Callable and TypedSignal files---" }
        val packageRegistry = NativeCallablePackageRegistry(config.packageName)
        // Typed signal handlers are built with the Callable factories, so both are generated together.
        return KotlinNativeCallableGenerator(packageRegistry).generate() +
            KotlinNativeSignalGenerator(packageRegistry).generate()
    }
}
