@file:JvmName("GenerateGodotApiKt") // Contract for the CLI

package io.github.kingg22.godot.codegen

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.terminal.Terminal
import io.github.kingg22.godot.codegen.models.config.ApiFilters
import io.github.kingg22.godot.codegen.models.config.CodegenConfig
import io.github.kingg22.godot.codegen.models.config.GeneratorBackend
import io.github.kingg22.godot.codegen.models.config.GeneratorKind
import io.github.kingg22.godot.codegen.runner.CodegenRunnerProvider
import java.util.*

fun main(args: Array<String>) = CodegenCommand().main(args)

private class CodegenCommand : CliktCommand("Generador de Extension API para Godot") {

    private val inputInterface by option(
        "-ii",
        "--input-interface",
        "--input-file-interface",
        help = "Path al archivo GDExtension interface",
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true, canBeSymlink = true).required()

    private val inputExtension by option(
        "-ie",
        "--input-extension",
        "--input-file-extension",
        help = "Path al archivo Extension API",
    ).file(mustExist = true, canBeFile = true, canBeDir = false, mustBeReadable = true, canBeSymlink = true).required()

    private val outputDir by option("-o", "--output", "--output-dir", help = "Directorio de salida")
        .path(canBeFile = false, canBeDir = true)
        .required()

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

    private val includeEnums: Boolean by option(
        "--include-enums",
        help = "Incluir enumeraciones",
    ).boolean().default(true)

    private val includeBuiltinClasses: Boolean by option(
        "--include-builtin-classes",
        help = "Incluir clases integradas",
    ).boolean().default(true)

    private val includeEngineClasses: Boolean by option(
        "--include-engine-classes",
        help = "Incluir clases del motor",
    ).boolean().default(true)

    private val includeUtilityFunctions: Boolean by option(
        "--include-utility-functions",
    ).boolean().default(true)

    private val includeNativeStructures: Boolean by option(
        "--include-native-structures",
        help = "Incluir estructuras nativas",
    ).boolean().default(true)

    private val excludedTypes by option(
        "--exclude-types",
        help = "Tipos a excluir (separados por comas)",
    ).split(",", ", ", ";", "; ").default(emptyList())

    private val packageName by option("-p", "--package", help = "Nombre del paquete base")
        .default("")

    init {
        context {
            terminal = Terminal(ansiLevel = TRUECOLOR)
        }
    }

    override fun run() {
        val serviceLoader: ServiceLoader<CodegenRunnerProvider> = ServiceLoader.load(CodegenRunnerProvider::class.java)

        val runner = serviceLoader
            .firstOrNull { it.backend == backend && it.kind == kind }
            ?.createRunner()
            ?: error("No runner found for backend=$backend kind=$kind")

        val config = CodegenConfig(
            inputInterface = inputInterface,
            inputExtension = inputExtension,
            outputDir = outputDir,
            packageName = packageName,
            backend = backend,
            kind = kind,
            generateDocs = generateDocs,
            skipPlatformSpecificApis = skipPlatformSpecificApis,
            filters = ApiFilters(
                includeEnums = includeEnums,
                includeBuiltinClasses = includeBuiltinClasses,
                includeEngineClasses = includeEngineClasses,
                includeUtilityFunctions = includeUtilityFunctions,
                includeNativeStructures = includeNativeStructures,
                excludedTypes = excludedTypes.toSet(),
            ),
        )

        runner.run(config)
    }
}
