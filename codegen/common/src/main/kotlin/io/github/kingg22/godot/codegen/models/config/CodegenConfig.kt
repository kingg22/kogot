package io.github.kingg22.godot.codegen.models.config

import java.io.File
import java.nio.file.Path

data class CodegenConfig(
    val inputInterface: File,
    val inputExtension: File,
    val outputDir: Path,
    val packageName: String,
    val backend: GeneratorBackend,
    val kind: GeneratorKind,
    val generateDocs: Boolean = false,
    val skipPlatformSpecificApis: Boolean = false,
    val filters: ApiFilters = ApiFilters(),
) {
    val inputInterfaceAsInputStream get() = inputInterface.inputStream()
    val inputExtensionAsInputStream get() = inputExtension.inputStream()
}
