package io.github.kingg22.kogot.processor.generation

import io.github.kingg22.kogot.analysis.models.ClassInfo
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticMessage

/**
 * Context for code generation.
 */
data class GeneratorContext(val outputPackage: String, val options: GeneratorOptions)

/**
 * Options for generators.
 */
data class GeneratorOptions(val generateKdoc: Boolean = true, val addSuppressAnnotations: Boolean = true)

/**
 * Output from a generator.
 */
data class GeneratedOutput(val files: List<GeneratedFile>, val diagnostics: List<DiagnosticMessage>)

/**
 * A generated file to be written.
 */
data class GeneratedFile(val relativePath: String, val content: String)

/**
 * Interface for code generators.
 */
interface Generator {
    val name: String

    /**
     * Generates output files from the given class models.
     *
     * @param context The generation context
     * @param classes The classes to generate code for
     * @return GeneratedOutput containing files and any diagnostics
     */
    fun generate(context: GeneratorContext, classes: List<ClassInfo>): GeneratedOutput
}
