package io.github.kingg22.godot.codegen

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.squareup.kotlinpoet.FileSpec
import io.github.kingg22.godot.codegen.impl.KotlinPoetGenerator
import io.github.kingg22.godot.codegen.impl.runtime.RuntimeFFIGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.models.internal.BuildConfiguration
import io.github.kingg22.godot.codegen.models.internal.CodegenOptions
import io.github.kingg22.godot.codegen.models.internal.GeneratorBackend
import io.github.kingg22.godot.codegen.models.internal.GeneratorKind
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.util.concurrent.StructuredTaskScope
import kotlin.time.Duration
import kotlin.time.measureTime

fun main(args: Array<String>) = CodegenCommand().main(args)

private class CodegenCommand : CliktCommand("Generador de Extension API para Godot") {

    private val inputInterface by option(
        "-ii",
        "--input-interface",
        "--input-file-interface",
        help = "Path al archivo GDExtension interface",
    ).inputStream().required()

    private val inputExtension by option(
        "-ie",
        "--input-extension",
        "--input-file-extension",
        help = "Path al archivo Extension API",
    ).inputStream().required()

    private val backend by option("-b", "--backend", help = "Backend de generación")
        .enum<GeneratorBackend>(ignoreCase = true)
        .required()

    private val kind by option("-k", "--kind", help = "Tipo de generación")
        .enum<GeneratorKind>(ignoreCase = true)
        .default(GeneratorKind.API)

    private val generateDocs by option("--docs", "--generate-docs", help = "Generar los KDocs o no")
        .boolean()
        .default(true)

    private val skipPlatformSpecificApis by option(
        "--skip-platform-specific-apis",
        help =
        "Skip platform-specific APIs (e.g., string_new_with_wide_chars) that are not common across all native targets",
    ).boolean().default(true)

    private val outputDir by option("-o", "--output", "--output-dir", help = "Directorio de salida")
        .path(canBeFile = false)
        .required()

    private val packageName by option("-p", "--package", help = "Nombre del paquete base")
        .default("")

    private val buildConfiguration by option(
        "--build-config",
        help = "Configuracion activa para builtin sizes/offsets: float_32, float_64, double_32, double_64",
    ).enum<BuildConfiguration>(ignoreCase = true)

    init {
        context {
            terminal = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun run() {
        var firstPathParent: String? = null
        var generatedFilesCount = 0
        var time: Duration? = null

        try {
            time = measureTime {
                val json = Json

                val sequenceFiles: Sequence<FileSpec> = when (kind) {
                    GeneratorKind.API -> {
                        echo("---Generator Extension API files--- Backend: $backend")
                        val extensionInterface = json.decodeFromStream<GDExtensionInterface>(inputInterface)
                        val extensionApi = json.decodeFromStream<ExtensionApi>(inputExtension)
                        // TODO pass generateDocs option
                        val generator = KotlinPoetGenerator(packageName, backend)
                        generator.generate(
                            extensionApi,
                            extensionInterface,
                            CodegenOptions(buildConfiguration = buildConfiguration),
                        )
                    }

                    GeneratorKind.RUNTIME -> {
                        echo("---Generating Runtime FFI Layer---")
                        val extensionInterface = json.decodeFromStream<GDExtensionInterface>(inputInterface)
                        val runtimeGenerator = RuntimeFFIGenerator(packageName)
                        runtimeGenerator.generate(
                            extensionInterface,
                            CodegenOptions(skipPlatformSpecificApis = skipPlatformSpecificApis),
                        )
                    }
                }

                StructuredTaskScope.open<Nothing?>().use { scope ->
                        // El hilo principal recorre la secuencia (Lazy)
                        for (fileSpec in sequenceFiles) {
                            // El hilo principal solo envía la tarea, no espera aquí
                            // El 'writeTo' ocurre DENTRO del Virtual Thread.
                            scope.fork(java.util.concurrent.Callable {
                                val path = fileSpec.writeTo(outputDir)
                                if (firstPathParent == null) firstPathParent = path.parent.toString()
                                null
                            })
                            generatedFilesCount++
                        }

                        scope.join() // espera; lanza FailedException si algún thread falló
                    }
            }
        } finally {
            echo("---Total generated: $generatedFilesCount in $time to => $firstPathParent ---")
        }
    }
}
