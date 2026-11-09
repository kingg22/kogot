package io.github.kingg22.godot.codegen.runner.impl

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.runner.AbstractCodegenRunner
import io.github.kingg22.godot.codegen.signal.knative.KotlinNativeSignalGenerator
import io.github.kingg22.godot.codegen.signal.knative.NativeSignalPackageRegistry
import io.github.kingg22.godot.codegen.utils.info

class KotlinNativeSignalRunner : AbstractCodegenRunner(KOTLIN_NATIVE, SIGNAL) {
    override fun execute(config: CodegenConfig): Sequence<FileSpec> {
        logger.info { "---Generating TypedSignal files---" }
        return KotlinNativeSignalGenerator(NativeSignalPackageRegistry(config.packageName)).generate()
    }
}
