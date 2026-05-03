package io.github.kingg22.godot.codegen.runner.impl

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.callable.knative.KotlinNativeCallableGenerator
import io.github.kingg22.godot.codegen.callable.knative.NativeCallablePackageRegistry
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.runner.AbstractCodegenRunner
import io.github.kingg22.godot.codegen.utils.info

class KotlinNativeCallableRunner : AbstractCodegenRunner(KOTLIN_NATIVE, CALLABLE) {
    override fun execute(config: CodegenConfig): Sequence<FileSpec> {
        logger.info { "---Generating Callable files---" }
        return KotlinNativeCallableGenerator(
            config.packageName,
            NativeCallablePackageRegistry(config.packageName),
        ).generate()
    }
}
