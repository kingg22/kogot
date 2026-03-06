import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
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
    abstract val backendName: Property<String>

    init {
        group = "codegen"
        description = "Generate Godot Extension API wrappers"
        mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
        argumentProviders += CommandLineArgumentProvider {
            listOf(
                "--input-interface",
                inputInterface.get().asFile.absolutePath,
                "--input-extension",
                inputExtension.get().asFile.absolutePath,
                "--output",
                outputDir.get().asFile.absolutePath,
                "--package",
                packageName.get(),
                "--backend",
                backendName.get(),

            )
        }
    }
}
