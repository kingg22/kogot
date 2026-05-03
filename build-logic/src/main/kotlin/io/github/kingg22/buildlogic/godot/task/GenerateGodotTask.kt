package io.github.kingg22.buildlogic.godot.task

import io.github.kingg22.buildlogic.godot.conventions.CodegenBackend
import io.github.kingg22.buildlogic.godot.conventions.CodegenKind
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader

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
    abstract val backendName: Property<CodegenBackend>

    @get:[Input Option(option = "kind", description = "Target kind")]
    abstract val outputKindName: Property<CodegenKind>

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

    @get:[InputFiles Classpath]
    abstract val classpath: ConfigurableFileCollection

    init {
        group = "codegen"
        description = "Generate Godot Extension API wrappers"
    }

    @TaskAction
    fun run() {
        val argsList =
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

                val excludedTypes = filterExcludeTypes.orNull?.joinToString(",")?.trim().orEmpty()
                if (excludedTypes.isNotBlank()) {
                    add("--exclude-types=$excludedTypes")
                }
            }

        val classpathFiles = classpath.files.map { it.toURI().toURL() }

        val originalContextClassLoader = Thread.currentThread().contextClassLoader

        val classLoader = URLClassLoader(
            classpathFiles.toTypedArray(),
            this::class.java.classLoader,
        )

        Thread.currentThread().contextClassLoader = classLoader

        try {
            val mainClass = classLoader.loadClass("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
            val mainMethod = mainClass.getMethod("main", Array<String>::class.java)

            mainMethod.invoke(null, argsList.toTypedArray())
        } catch (e: InvocationTargetException) {
            throw e.targetException
        } finally {
            classLoader.use {
                Thread.currentThread().contextClassLoader = originalContextClassLoader
            }
        }
    }
}
