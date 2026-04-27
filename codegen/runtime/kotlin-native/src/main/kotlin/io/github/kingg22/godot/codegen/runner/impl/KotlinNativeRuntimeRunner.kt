package io.github.kingg22.godot.codegen.runner.impl

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.models.config.CodegenOptions
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.runner.AbstractCodegenRunner
import io.github.kingg22.godot.codegen.runtime.impl.RuntimeFFIGenerator
import io.github.kingg22.godot.codegen.utils.info
import org.slf4j.Logger

class KotlinNativeRuntimeRunner(logger: Logger) : AbstractCodegenRunner(KOTLIN_NATIVE, RUNTIME, logger) {

    @OptIn(ExperimentalSerializationApi::class)
    override fun execute(config: CodegenConfig): Sequence<FileSpec> {
        logger.info { "---Generating ${backend.name} Runtime FFI Layer---" }
        val extensionInterface = Json.decodeFromStream<GDExtensionInterface>(config.inputInterfaceAsInputStream)
        val runtimeGenerator = RuntimeFFIGenerator(config.packageName)
        return runtimeGenerator.generate(
            extensionInterface,
            CodegenOptions(skipPlatformSpecificApis = config.skipPlatformSpecificApis),
        )
    }
}
