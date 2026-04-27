package io.github.kingg22.godot.codegen.runner.impl

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.KotlinNativeBackend
import io.github.kingg22.godot.codegen.extensionapi.impl.knative.NativePackageRegistry
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
        val json = Json.Default
        logger.info { "---Generating API files---" }
        val extensionInterface = json.decodeFromStream<GDExtensionInterface>(config.inputInterfaceAsInputStream)
        val extensionApi = json.decodeFromStream<ExtensionApi>(config.inputExtensionAsInputStream)
        return generate(
            extensionApi,
            extensionInterface,
            CodegenOptions(filters = config.filters),
            config.packageName,
        )
    }

    private fun generate(
        api: ExtensionApi,
        extensionInterface: GDExtensionInterface?,
        options: CodegenOptions,
        packageName: String,
    ): Sequence<FileSpec> = context(
        Context.buildFromApi(api, packageName, NativePackageRegistry.factory, options),
        extensionInterface,
    ) {
        KotlinNativeBackend().generateAll(api)
    }
}
