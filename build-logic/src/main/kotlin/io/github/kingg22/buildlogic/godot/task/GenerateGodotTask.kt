package io.github.kingg22.buildlogic.godot.task

import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.options.Option
import org.gradle.process.CommandLineArgumentProvider

@CacheableTask
abstract class GenerateGodotTask : JavaExec() {

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
    abstract val backendName: Property<GodotCodegenExtension.Backend>

    @get:[Input Option(option = "kind", description = "Target kind")]
    abstract val outputKindName: Property<GodotCodegenExtension.Kind>

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

    @get:[Input Optional Option(option = "filter-only-native-struct", description = "Generate only native structures")]
    abstract val filterOnlyNativeStruct: Property<Boolean>

    @get:[Input Optional Option(option = "filter-exclude-types", description = "Comma-separated types to exclude")]
    abstract val filterExcludeTypes: ListProperty<String>

    init {
        group = "codegen"
        description = "Generate Godot Extension API wrappers"
        mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
        enableAssertions = true
        argumentProviders += CommandLineArgumentProvider {
            buildList {
                add("--input-interface")
                add(inputInterface.get().asFile.absolutePath)

                add("--input-extension")
                add(inputExtension.get().asFile.absolutePath)

                add("--output")
                add(outputDir.get().asFile.absolutePath)

                add("--package")
                add(packageName.get())

                add("--backend")
                add(backendName.get().name)

                add("--kind")
                add(outputKindName.get().name)

                if (generateDocs.isPresent) {
                    add("--generate-docs")
                }

                if (skipPlatformSpecificApis.isPresent) {
                    add("--skip-platform-specific-apis=${skipPlatformSpecificApis.get()}")
                }

                if (filterOnlyEnums.isPresent) {
                    add("--include-enums=${filterOnlyEnums.get()}")
                }

                if (filterOnlyBuiltinClasses.isPresent) {
                    add("--include-builtin-classes=${filterOnlyBuiltinClasses.get()}")
                }

                if (filterOnlyEngineClasses.isPresent) {
                    add("--include-engine-classes=${filterOnlyEngineClasses.get()}")
                }

                if (filterOnlyNativeStruct.isPresent) {
                    add("--include-native-structs=${filterOnlyNativeStruct.get()}")
                }

                val excludedTypes = filterExcludeTypes.get().joinToString().trim()
                if (excludedTypes.isNotBlank()) {
                    add("--exclude-types=$excludedTypes")
                }
            }
        }
    }
}
