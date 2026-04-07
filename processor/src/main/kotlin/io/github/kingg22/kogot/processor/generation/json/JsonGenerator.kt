package io.github.kingg22.kogot.processor.generation.json

import io.github.kingg22.kogot.analysis.models.ClassInfo
import io.github.kingg22.kogot.analysis.models.getExportedProperties
import io.github.kingg22.kogot.analysis.models.getRpcFunctions
import io.github.kingg22.kogot.analysis.models.hasTool
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticMessage
import io.github.kingg22.kogot.processor.generation.GeneratedFile
import io.github.kingg22.kogot.processor.generation.GeneratedOutput
import io.github.kingg22.kogot.processor.generation.Generator
import io.github.kingg22.kogot.processor.generation.GeneratorContext

/**
 * Generates JSON manifest for bindings.
 */
class JsonGenerator(private val prettyPrint: Boolean = true) : Generator {
    override val name: String = "JsonGenerator"

    override fun generate(context: GeneratorContext, classes: List<ClassInfo>): GeneratedOutput {
        val files = mutableListOf<GeneratedFile>()
        val diagnostics = mutableListOf<DiagnosticMessage>()

        val manifest = buildManifest(classes)

        files.add(
            GeneratedFile(
                relativePath = "manifest.json",
                content = manifest,
            ),
        )

        return GeneratedOutput(files, diagnostics)
    }

    private fun buildManifest(classes: List<ClassInfo>): String {
        val sb = StringBuilder()
        sb.appendLine("{")

        val entries = classes.map { classInfo ->
            buildClassEntry(classInfo)
        }

        sb.appendLine("  \"bindings\": [")
        sb.append(entries.joinToString(",\n"))
        sb.appendLine()
        sb.appendLine("  ],")

        val totalProps = classes.sumOf { it.getExportedProperties().size }
        val totalRpc = classes.sumOf { it.getRpcFunctions().size }

        sb.appendLine("  \"summary\": {")
        sb.appendLine("    \"totalClasses\": ${classes.size},")
        sb.appendLine("    \"totalExportedProperties\": $totalProps,")
        sb.appendLine("    \"totalRpcFunctions\": $totalRpc")
        sb.appendLine("  }")
        sb.appendLine("}")

        return sb.toString()
    }

    private fun buildClassEntry(classInfo: ClassInfo): String {
        val exportedProps = classInfo.getExportedProperties()
        val rpcFuncs = classInfo.getRpcFunctions()

        val propNames = exportedProps.joinToString(", ") { "\"${it.name}\"" }
        val rpcNames = rpcFuncs.joinToString(", ") { "\"${it.name}\"" }

        return buildString {
            appendLine("    {")
            appendLine("      \"className\": \"${classInfo.shortName}\",")
            appendLine("      \"qualifiedName\": \"${classInfo.qualifiedName}\",")
            appendLine("      \"hasTool\": ${classInfo.hasTool()},")
            appendLine("      \"exportedProperties\": [$propNames],")
            appendLine("      \"rpcFunctions\": [$rpcNames]")
            appendLine("    }")
        }
    }
}
