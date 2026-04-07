package io.github.kingg22.kogot.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.github.kingg22.kogot.processor.bridge.toClassInfo
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticCode
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticLocation
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticMessage
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticRenderer
import io.github.kingg22.kogot.processor.generation.GeneratedOutput
import io.github.kingg22.kogot.processor.generation.GeneratorContext
import io.github.kingg22.kogot.processor.generation.json.JsonGenerator
import io.github.kingg22.kogot.processor.generation.kotlin.KotlinGenerator
import io.github.kingg22.kogot.processor.validation.ValidationContext
import io.github.kingg22.kogot.processor.validation.ValidationResultImpl
import io.github.kingg22.kogot.processor.validation.ValidatorPipeline

/**
 * KSP SymbolProcessor for Kogot bindings.
 */
class KogotProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val options: KogotOptions = KogotOptions.fromMap(environment.options)
    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger

    override fun process(resolver: Resolver): List<KSClassDeclaration> {
        val classes = extractClasses(resolver)

        if (classes.isEmpty()) return emptyList()

        // Run validation
        val validationResult = runValidation(classes)

        // Report validation errors
        for (error in validationResult.errors) {
            logger.error(DiagnosticRenderer.renderRustc(error))
        }
        for (warning in validationResult.warnings) {
            logger.warn(DiagnosticRenderer.renderRustc(warning))
        }

        // Stop if validation failed
        if (!validationResult.isValid) return emptyList()

        // Generate output
        val generatedOutput = runGeneration(classes)

        // Report generation diagnostics
        for (diagnostic in generatedOutput.diagnostics) {
            when (diagnostic.severity) {
                ERROR -> logger.error(DiagnosticRenderer.renderRustc(diagnostic))
                WARNING -> logger.warn(DiagnosticRenderer.renderRustc(diagnostic))
                INFO -> logger.info(DiagnosticRenderer.renderRustc(diagnostic))
            }
        }

        // Write generated files
        for (file in generatedOutput.files) {
            writeGeneratedFile(file.relativePath, file.content)
        }

        return emptyList()
    }

    private fun extractClasses(resolver: Resolver): List<KSClassDeclaration> {
        val classes = mutableListOf<KSClassDeclaration>()

        // Find all classes with our annotations
        val exportAnnotation = "io.github.kingg22.godot.api.annotations.Export"
        val rpcAnnotation = "io.github.kingg22.godot.api.annotations.Rpc"
        val toolAnnotation = "io.github.kingg22.godot.api.annotations.Tool"

        resolver.getSymbolsWithAnnotation(exportAnnotation)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { if (!classes.contains(it)) classes.add(it) }

        resolver.getSymbolsWithAnnotation(rpcAnnotation)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { if (!classes.contains(it)) classes.add(it) }

        resolver.getSymbolsWithAnnotation(toolAnnotation)
            .filterIsInstance<KSClassDeclaration>()
            .forEach { if (!classes.contains(it)) classes.add(it) }

        return classes
    }

    private fun runValidation(classes: List<KSClassDeclaration>): ValidationResultImpl {
        val pipeline = ValidatorPipeline.empty()
        val result = ValidationResultImpl()

        for (ksClass in classes) {
            val classInfo = ksClass.toClassInfo()
            val context = ValidationContext(
                classInfo = classInfo,
                options = io.github.kingg22.kogot.processor.validation.ValidationOptions(),
            )

            val classResult = pipeline.execute(context, stopOnFirstError = false)
            result.merge(classResult)

            // Additional basic validation
            if (classInfo.annotations.any { it.shortName == "Export" }) {
                val exportedProps = classInfo.properties.filter {
                    it.annotations.any { ann -> ann.shortName == "Export" }
                }
                for (prop in exportedProps) {
                    // Basic type checking
                    if (!isValidExportType(prop.type.qualifiedName)) {
                        result.addError(
                            DiagnosticMessage.error(
                                code = DiagnosticCode.INVALID_EXPORT_TYPE,
                                message = "@Export on unsupported type '${prop.type.qualifiedName}'",
                                location = DiagnosticLocation(
                                    classInfo.filePath,
                                    classInfo.lineNumber,
                                    0,
                                ),
                                help = "Supported types: primitives (Int, Float, String, etc.) and Godot builtin types",
                                note = "This property will not appear in the Inspector",
                            ),
                        )
                    }
                }
            }
        }

        return result
    }

    private fun isValidExportType(qualifiedName: String): Boolean {
        val primitives = listOf(
            "kotlin.Int",
            "kotlin.Long",
            "kotlin.Short",
            "kotlin.Byte",
            "kotlin.Float",
            "kotlin.Double",
            "kotlin.Boolean",
            "kotlin.String",
        )
        return qualifiedName in primitives || qualifiedName.startsWith("io.github.kingg22.godot.api.builtin.")
    }

    private fun runGeneration(classes: List<KSClassDeclaration>): GeneratedOutput {
        val classInfos = classes.map { it.toClassInfo() }
        val context = GeneratorContext(
            outputPackage = options.generatedPackage,
            options = io.github.kingg22.kogot.processor.generation.GeneratorOptions(),
        )

        val outputs = mutableListOf<GeneratedOutput>()

        // Generate Kotlin code if requested
        if (options.outputMode == OutputMode.KOTLIN || options.outputMode == OutputMode.BOTH) {
            val kotlinGenerator = KotlinGenerator()
            outputs.add(kotlinGenerator.generate(context, classInfos))
        }

        // Generate JSON if requested
        if (options.outputMode == OutputMode.JSON || options.outputMode == OutputMode.BOTH) {
            val jsonGenerator = JsonGenerator()
            outputs.add(jsonGenerator.generate(context, classInfos))
        }

        // Merge outputs
        return GeneratedOutput(
            files = outputs.flatMap { it.files },
            diagnostics = outputs.flatMap { it.diagnostics },
        )
    }

    private fun writeGeneratedFile(relativePath: String, content: String) {
        // For now, just log that we would generate this file
        logger.info("Would generate file: $relativePath with ${content.length} chars")
    }

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = KogotProcessor(environment)
    }
}
