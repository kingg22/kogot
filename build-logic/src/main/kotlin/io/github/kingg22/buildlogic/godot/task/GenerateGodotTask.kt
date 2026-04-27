package io.github.kingg22.buildlogic.godot.task

import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Backend
import io.github.kingg22.buildlogic.godot.conventions.GodotCodegenExtension.Kind
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.submit
import org.gradle.workers.WorkerExecutor
import javax.inject.Inject

@CacheableTask
abstract class GenerateGodotTask : DefaultTask() {

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Classpath
    abstract val runnerClasspath: ConfigurableFileCollection

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
    abstract val backendName: Property<Backend>

    @get:[Input Optional Option(option = "kind", description = "Target kind")]
    abstract val outputKindName: Property<Kind>

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
        val executor = workerExecutor.noIsolation()
        val t = this
        val parameters = Action<CodegenRunnerWorkParameters> {
            val p = this
            p.classpath.setFrom(t.runnerClasspath)
            p.inputInterface.set(t.inputInterface)
            p.inputExtension.set(t.inputExtension)
            p.outputDir.set(t.outputDir)
            p.packageName.set(t.packageName)
            p.backendName.set(t.backendName)
            p.kindName.set(t.outputKindName)
            p.generateDocs.set(t.generateDocs)
            p.skipPlatformSpecificApis.set(t.skipPlatformSpecificApis)
            p.filterOnlyEnums.set(t.filterOnlyEnums)
            p.filterOnlyBuiltinClasses.set(t.filterOnlyBuiltinClasses)
            p.filterOnlyEngineClasses.set(t.filterOnlyEngineClasses)
            p.filterExcludeTypes.set(t.filterExcludeTypes)
        }
        executor.submit(CodegenRunnerWorkAction::class, parameters)
    }
}
