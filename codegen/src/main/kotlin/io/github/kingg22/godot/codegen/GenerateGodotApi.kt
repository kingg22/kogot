package io.github.kingg22.godot.codegen

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import io.github.kingg22.godot.codegen.impl.GeneratorBackend
import io.github.kingg22.godot.codegen.impl.KotlinPoetGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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

    private val outputDir by option("-o", "--output", "--output-dir", help = "Directorio de salida")
        .path(canBeFile = false)
        .required()

    private val packageName by option("-p", "--package", help = "Nombre del paquete base")
        .default("")

    init {
        context {
            terminal = Terminal(ansiLevel = AnsiLevel.TRUECOLOR)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun run() {
        echo("---Generator Extension API files--- Backend: $backend")

        var firstPathParent: String? = null
        var generatedFilesCount = 0
        var time: Duration? = null

        try {
            time = measureTime {
                val json = Json
                val generator = KotlinPoetGenerator(packageName, backend)

                val extensionApi = json.decodeFromStream<ExtensionApi>(inputExtension)
                val extensionInterface = json.decodeFromStream<GDExtensionInterface>(inputInterface)

                Executors.newVirtualThreadPerTaskExecutor().use { executor: ExecutorService ->
                    val fileSpecSequence = generator.generate(extensionApi)

                    // El hilo principal recorre la secuencia (Lazy)
                    for (fileSpec in fileSpecSequence) {
                        // El hilo principal solo envía la tarea, no espera.
                        // El 'writeTo' ocurre DENTRO del Virtual Thread.
                        executor.submit {
                            val path = fileSpec.writeTo(outputDir)
                            // Actualización segura del primer path para el log
                            if (firstPathParent == null) firstPathParent = path.parent.toString()
                        }
                        generatedFilesCount++
                    }
                }
            }
        } finally {
            echo("---Total generated: $generatedFilesCount in $time to => $firstPathParent ---")
        }
    }
}
