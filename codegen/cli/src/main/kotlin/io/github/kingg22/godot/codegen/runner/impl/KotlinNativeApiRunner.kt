package io.github.kingg22.godot.codegen.runner.impl

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.impl.KotlinPoetGenerator
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.models.config.CodegenOptions
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.runner.AbstractCodegenRunner
import io.github.kingg22.godot.codegen.utils.info
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.slf4j.Logger

class KotlinNativeApiRunner(logger: Logger) : AbstractCodegenRunner(KOTLIN_NATIVE, API, logger) {

    @OptIn(ExperimentalSerializationApi::class)
    override fun execute(config: CodegenConfig): Sequence<FileSpec> {
        val json = Json
        logger.info { "---Generating API files---" }
        val extensionInterface = json.decodeFromStream<GDExtensionInterface>(config.inputInterfaceAsInputStream)
        val extensionApi = json.decodeFromStream<ExtensionApi>(config.inputExtensionAsInputStream)
        val generator = KotlinPoetGenerator(config.packageName, backend)
        return generator.generate(
            extensionApi,
            extensionInterface,
            CodegenOptions(filters = config.filters),
        )
    }
}
