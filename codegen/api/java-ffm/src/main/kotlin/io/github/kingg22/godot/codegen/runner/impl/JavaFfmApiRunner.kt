package io.github.kingg22.godot.codegen.runner.impl

import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.impl.jffm.JavaFfmBackend
import io.github.kingg22.godot.codegen.extensionapi.impl.jffm.JavaFfmPackageRegistry
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.models.config.CodegenOptions
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.runner.AbstractCodegenRunner
import io.github.kingg22.godot.codegen.utils.info
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class JavaFfmApiRunner : AbstractCodegenRunner(JAVA_FFM, API) {
    @OptIn(ExperimentalSerializationApi::class)
    override fun execute(config: CodegenConfig): Sequence<FileSpec> {
        val json = Json.Default
        logger.info { "---Generating API files---" }
        val extensionInterface = json.decodeFromStream<GDExtensionInterface>(config.inputInterfaceAsInputStream)
        val extensionApi = json.decodeFromStream<ExtensionApi>(config.inputExtensionAsInputStream)
        return context(
            Context.buildFromApi(
                extensionApi,
                config.packageName,
                JavaFfmPackageRegistry.factory,
                CodegenOptions(filters = config.filters),
            ),
            extensionInterface,
        ) {
            JavaFfmBackend().generateAll(extensionApi)
        }
    }
}
