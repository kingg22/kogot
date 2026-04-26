package io.github.kingg22.buildlogic.godot.task

import io.github.kingg22.godot.codegen.models.config.ApiFilters
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
import io.github.kingg22.godot.codegen.runner.CodegenRegistry
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

@CacheableTask
abstract class GenerateGodotTask : DefaultTask() {

    @get:[
    InputFile
    PathSensitive(PathSensitivity.ABSOLUTE)
    Option(option = "input-interface", description = "Path to gdextension_interface.json")
    ]
    abstract val inputInterface: RegularFileProperty

    @get:[
    InputFile
    PathSensitive(PathSensitivity.ABSOLUTE)
    Option(option = "input-extension", description = "Path to extension_api.json")
    ]
    abstract val inputExtension: RegularFileProperty

    @get:[OutputDirectory Option(option = "output", description = "Output directory")]
    abstract val outputDir: DirectoryProperty

    @get:[Input Option(option = "package", description = "Target package")]
    abstract val packageName: Property<String>

    @get:[Input Option(option = "backend", description = "Target backend")]
    abstract val backendName: Property<GeneratorBackend>

    @get:[Input Optional Option(option = "kind", description = "Target kind")]
    abstract val outputKindName: Property<GeneratorKind>

    @get:[Input Optional Option(option = "generate-docs", description = "Generate docs")]
    abstract val generateDocs: Property<Boolean>

    @get:[Input Optional Option(option = "skip-platform-specific-apis", description = "Skip platform-specific APIs")]
    abstract val skipPlatformSpecificApis: Property<Boolean>

    @get:[Input Optional Option(option = "filter-only-enums", description = "Generate only enums")]
    abstract val filterOnlyEnums: Property<Boolean>

    @get:[Input Optional Option(option = "filter-only-builtin-classes", description = "Generate only builtin classes")]
    abstract val filterOnlyBuiltinClasses: Property<Boolean>

    @get:[Input Optional Option(option = "filter-only-engine-classes", description = "Generate only engine classes")]
    abstract val filterOnlyEngineClasses: Property<Boolean>

    @get:[Input Optional Option(option = "filter-exclude-types", description = "Comma-separated types to exclude")]
    abstract val filterExcludeTypes: ListProperty<String>

    init {
        group = "codegen"
        description = "Generate Godot API bindings"
    }

    @TaskAction
    fun run() {
        CodegenRegistry.registerDefaultRunners(logger)
        val onlyEnums = filterOnlyEnums.isPresent && filterOnlyEnums.get()
        val onlyBuiltinClasses = filterOnlyBuiltinClasses.isPresent && filterOnlyBuiltinClasses.get()
        val onlyEngineClasses = filterOnlyEngineClasses.isPresent && filterOnlyEngineClasses.get()

        val config = CodegenConfig(
            inputInterface = inputInterface.get().asFile,
            inputExtension = inputExtension.get().asFile,
            outputDir = outputDir.get().asFile.toPath(),
            packageName = packageName.get(),
            backend = backendName.get(),
            kind = outputKindName.get(),
            generateDocs = generateDocs.getOrElse(false),
            skipPlatformSpecificApis = skipPlatformSpecificApis.getOrElse(false),
            filters = when {
                onlyEnums -> ApiFilters.ALL.onlyEnums()
                onlyBuiltinClasses -> ApiFilters.ALL.onlyBuiltinClasses()
                onlyEngineClasses -> ApiFilters.ALL.onlyEngineClasses()
                filterExcludeTypes.get().isNotEmpty() -> ApiFilters.ALL.excludingTypes(
                    filterExcludeTypes.get().map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
                )
                else -> ApiFilters.ALL
            },
        )

        val runner = CodegenRegistry.find(config.backend, config.kind)
        runner.run(config)
    }
}