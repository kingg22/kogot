import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.process.CommandLineArgumentProvider

class GodotArgsProvider(private val task: GenerateGodotTask) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val args = mutableListOf<String>()

        if (task.inputInterface.isPresent) {
            args += listOf("--input-interface", task.inputInterface.get().asFile.absolutePath)
        }

        if (task.inputExtension.isPresent) {
            args += listOf("--input-extension", task.inputExtension.get().asFile.absolutePath)
        }

        args += listOf(
            "--output",
            task.outputDir.get().asFile.absolutePath,
            "--package",
            task.packageName.get(),
        )

        return args
    }
}

abstract class GenerateGodotTask : JavaExec() {
    @get:[InputFile Optional Option(option = "input-interface", description = "Path to gdextension_interface.json")]
    abstract val inputInterface: RegularFileProperty

    @get:[InputFile Optional Option(option = "input-extension", description = "Path to extension_api.json")]
    abstract val inputExtension: RegularFileProperty

    @get:[OutputDirectory Option(option = "output", description = "Output directory")]
    abstract val outputDir: DirectoryProperty

    @get:[Input Option(option = "package", description = "Target package")]
    abstract val packageName: Property<String>

    init {
        group = "codegen"
        val sourceSets = project.the<SourceSetContainer>()

        mainClass.set("io.github.kingg22.godot.codegen.GenerateGodotApiKt")
        classpath = sourceSets["main"].runtimeClasspath

        argumentProviders += GodotArgsProvider(this)
    }

    override fun exec() {
        require(inputInterface.isPresent || inputExtension.isPresent) {
            "Either --input-interface or --input-extension must be specified"
        }
        super.exec()
    }
}
