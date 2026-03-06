package io.github.kingg22.godot.codegen

import io.github.kingg22.godot.codegen.impl.GeneratorBackend
import io.github.kingg22.godot.codegen.impl.KotlinPoetGenerator
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensioninterface.GDExtensionInterface
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.measureTime

// error codes
private const val SUCCESS = 0
private const val FAILURE = 1
private const val OPTION_ERROR = 2
private const val INPUT_ERROR = 3
private const val FATAL_ERROR = 5
private const val OUTPUT_ERROR = 6

@OptIn(ExperimentalSerializationApi::class)
fun main(args: Array<String>) {
    val logger = Logger()

    fun printOptionError(message: String?, code: Int = OPTION_ERROR): Nothing {
        logger.err(message ?: "Unknown error")
        exitProcess(code)
    }

    val args = try {
        CommandLine.parse(args.toMutableList())
    } catch (ioexp: IOException) {
        logger.fatal(ioexp, "argfile.read.error", ioexp)
        exitProcess(OPTION_ERROR)
    }

    val parser = OptionParser.builder {
        accepts("--input-interface", listOf("-ii", "--input-file-interface"), "help.input", true)
        accepts("--input-extension", listOf("-ie", "--input-file-extension"), "help.input", true)
        accepts("--backend", listOf("-b", "--backend"), "help.backend", true)

        accepts("--output", listOf("-o", "--output-dir"), "help.output", true)
        accepts("--package", listOf("-p"), "help.package", true)
    }

    val optionSet = try {
        parser.parse(args)
    } catch (oe: OptionParser.OptionException) {
        printOptionError(oe.message)
    }

    if (!optionSet.has("--input-extension") && !optionSet.has("--input-interface")) {
        printOptionError("Missing input extension file or input interface file, must specify one of them.", INPUT_ERROR)
    }

    val extensionFile = optionSet.valueOf("--input-extension")?.let { Path(it) }
    val interfaceFile = optionSet.valueOf("--input-interface")?.let { Path(it) }
    val outputDir = optionSet.valueOf("--output")?.let { Path(it).createDirectories() }
    val packageName = optionSet.valueOf("--package").orEmpty()
    val backend = optionSet.valueOf("--backend").orEmpty()

    if (outputDir == null) printOptionError("Missing output directory, must specify one of them.", OUTPUT_ERROR)

    if (!outputDir.exists() || !outputDir.isDirectory()) {
        printOptionError("directory.not.found $outputDir", OUTPUT_ERROR)
    }

    if (interfaceFile == null) {
        printOptionError("Unexpected. Missing input GDExtension interface file, must specify one of them.", INPUT_ERROR)
    }

    if (!interfaceFile.exists()) {
        printOptionError("file.not.found $interfaceFile", INPUT_ERROR)
    }

    if (extensionFile == null) {
        printOptionError("Unexpected. Missing input Extension API file, must specify one of them.", INPUT_ERROR)
    }

    if (!extensionFile.exists()) {
        printOptionError("file.not.found $extensionFile", INPUT_ERROR)
    }

    if (backend.isBlank()) {
        printOptionError(
            "Missing backend, must specify one of: ${GeneratorBackend.entries.joinToString { it.name }}",
            INPUT_ERROR,
        )
    }

    val backendEnum = GeneratorBackend.entries.find { it.name.equals(backend, true) } ?: run {
        printOptionError(
            "Invalid backend: $backend, must be one of: [${GeneratorBackend.entries.joinToString { it.name }}]",
            INPUT_ERROR,
        )
    }

    println("---Generator Extension API files--- Backend: $backendEnum")

    var firstPathParent: String? = null
    var generatedFilesCount = 0
    var time: Duration? = null

    try {
        time = measureTime {
            val json = Json
            val generator = KotlinPoetGenerator(packageName, backendEnum)
            val extensionApi = json.decodeFromStream<ExtensionApi>(extensionFile.inputStream())
            val extensionInterface = json.decodeFromStream<GDExtensionInterface>(interfaceFile.inputStream())

            // Creamos un executor que usa Virtual Threads
            Executors.newVirtualThreadPerTaskExecutor().use { executor: ExecutorService ->
                val fileSpecSequence = generator.generate(extensionApi)
                val futures = mutableListOf<Future<*>>()

                // El hilo principal recorre la secuencia (Lazy)
                for (fileSpec in fileSpecSequence) {
                    // El hilo principal solo envía la tarea, no espera.
                    // El 'writeTo' ocurre DENTRO del Virtual Thread.
                    val future: Future<*> = executor.submit {
                        val path = fileSpec.writeTo(outputDir)

                        // Actualización segura del primer path para el log
                        if (firstPathParent == null) firstPathParent = path.parent.toString()
                    }
                    futures.add(future)
                }

                // Esperamos resultados mientras se escriben de forma paralela
                futures.forEach { it.get() }
                generatedFilesCount = futures.size
            }
        }
    } catch (e: Exception) {
        logger.fatal(e, "fatal.error")
        exitProcess(FATAL_ERROR)
    } finally {
        println("---Total generated: $generatedFilesCount in $time to => $firstPathParent ---")
    }

    exitProcess(SUCCESS)
}
