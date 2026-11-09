package io.github.kingg22.kogot.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticCode
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticLocation
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticMessage
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticRenderer
import io.github.kingg22.kogot.processor.generators.kotlin.GodotBindingGenerator
import io.github.kingg22.kogot.processor.ksp.toClassInfo
import io.github.kingg22.kogot.processor.model.ClassInfo
import io.github.kingg22.kogot.processor.model.GodotPrimitives
import io.github.kingg22.kogot.processor.model.PropertyInfo
import io.github.kingg22.kogot.processor.model.SignalDeclaration
import io.github.kingg22.kogot.processor.model.SignalParameterInfo
import io.github.kingg22.kogot.processor.model.TypeInfo
import io.github.kingg22.kogot.processor.model.getExportedMethods
import io.github.kingg22.kogot.processor.model.getExportedProperties
import io.github.kingg22.kogot.processor.model.getRegisterSignalAnnotation
import io.github.kingg22.kogot.processor.model.isGodotBuiltin

/**
 * KSP SymbolProcessor for Kogot bindings.
 */
class KogotProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val codeGenerator: CodeGenerator = environment.codeGenerator
    private val logger: KSPLogger = environment.logger
    private val generator = GodotBindingGenerator()

    override fun process(resolver: Resolver): List<KSClassDeclaration> {
        logger.info("KogotProcessor: Starting processing")

        val classes = extractClasses(resolver)
        logger.info("KogotProcessor: Found ${classes.size} classes with @Godot annotation")

        if (classes.isEmpty()) {
            logger.info("KogotProcessor: No classes found, returning emptyList")
            return emptyList()
        }

        val classInfos = classes.map { it.toClassInfo() }
        val containingFileByQualifiedName = classes.zip(classInfos)
            .mapNotNull { (ksClass, info) -> ksClass.containingFile?.let { info.qualifiedName to it } }
            .toMap()

        // Validate
        val validationDiagnostics = validateClasses(classInfos)
        for (diagnostic in validationDiagnostics) {
            when (diagnostic.severity) {
                ERROR -> logger.error(DiagnosticRenderer.renderRustc(diagnostic))
                WARNING -> logger.warn(DiagnosticRenderer.renderRustc(diagnostic))
                INFO -> logger.info(DiagnosticRenderer.renderRustc(diagnostic))
            }
        }
        if (validationDiagnostics.any { it.isError() }) return emptyList()

        // Generate
        val result = generator.generate(classInfos)
        for (diagnostic in result.diagnostics) {
            when (diagnostic.severity) {
                ERROR -> logger.error(DiagnosticRenderer.renderRustc(diagnostic))
                WARNING -> logger.warn(DiagnosticRenderer.renderRustc(diagnostic))
                INFO -> logger.info(DiagnosticRenderer.renderRustc(diagnostic))
            }
        }

        for ((relativePath, content, sourceClassNames) in result.files) {
            writeGeneratedFile(relativePath, content, sourceClassNames, containingFileByQualifiedName)
        }

        return emptyList()
    }

    private fun extractClasses(resolver: Resolver): Set<KSClassDeclaration> {
        // Find all classes with @Godot annotation
        val godotAnnotation = "io.github.kingg22.godot.api.annotations.Godot"

        return resolver.getSymbolsWithAnnotation(godotAnnotation)
            .filterIsInstance<KSClassDeclaration>()
            .toSet()
    }

    private fun validateClasses(classInfos: List<ClassInfo>): List<DiagnosticMessage> {
        val diagnostics = mutableListOf<DiagnosticMessage>()

        for (classInfo in classInfos) {
            diagnostics += classInfo.validateSignals()

            for ((_, type) in classInfo.getExportedProperties()) {
                if (!isValidExportType(type)) {
                    diagnostics.add(
                        DiagnosticMessage.error(
                            code = DiagnosticCode.INVALID_EXPORT_TYPE,
                            message = "@Export on unsupported type '${type.qualifiedName}'",
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

            for (function in classInfo.getExportedMethods()) {
                val returnType = function.returnType
                if (returnType != null && !isValidExportType(returnType)) {
                    diagnostics.add(
                        DiagnosticMessage.error(
                            code = DiagnosticCode.INVALID_EXPORT_METHOD_TYPE,
                            message =
                            "@ExportMethod '${function.name}' has unsupported return type " +
                                "'${returnType.qualifiedName}'",
                            location = DiagnosticLocation(classInfo.filePath, classInfo.lineNumber, 0),
                            help = "Supported types: primitives (Int, Float, String, etc.) and Godot builtin types",
                            note = "This is a compile error: binding generation is aborted for the whole " +
                                "build until it's fixed",
                        ),
                    )
                }

                for (parameter in function.parameters) {
                    if (!isValidExportType(parameter.type)) {
                        diagnostics.add(
                            DiagnosticMessage.error(
                                code = DiagnosticCode.INVALID_EXPORT_METHOD_TYPE,
                                message =
                                "@ExportMethod '${function.name}' has unsupported parameter type " +
                                    "'${parameter.type.qualifiedName}' for '${parameter.name}'",
                                location = DiagnosticLocation(classInfo.filePath, classInfo.lineNumber, 0),
                                help =
                                "Supported types: primitives (Int, Float, String, etc.) and Godot builtin types",
                                note = "This is a compile error: binding generation is aborted for the whole " +
                                    "build until it's fixed",
                            ),
                        )
                    }

                    if (parameter.hasDefaultValue) {
                        diagnostics.add(
                            DiagnosticMessage.error(
                                code = DiagnosticCode.UNSUPPORTED_METHOD_DEFAULT_ARGUMENT,
                                message =
                                "@ExportMethod '${function.name}' parameter '${parameter.name}' has a " +
                                    "default value, which is not supported yet",
                                location = DiagnosticLocation(classInfo.filePath, classInfo.lineNumber, 0),
                                help = "Remove the default value or call this method without @ExportMethod",
                                note = "This is a compile error: binding generation is aborted for the whole " +
                                    "build until it's fixed",
                            ),
                        )
                    }
                }
            }
        }

        return diagnostics
    }

    class Provider : SymbolProcessorProvider {
        override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor = KogotProcessor(environment)
    }

    private fun writeGeneratedFile(
        relativePath: String,
        content: String,
        sourceClassNames: List<String>,
        containingFileByQualifiedName: Map<String, KSFile>,
    ) {
        val parts = relativePath.split("/")
        val packageName = if (parts.size > 1) parts.dropLast(1).joinToString(".") else ""
        // Remove .kt extension if present since extensionName will add it
        val fileNameWithExt = parts.last()
        val fileName = fileNameWithExt.removeSuffix(".kt")
        check(fileName.isNotBlank()) { "File name is empty" }

        logger.info("Generating file: $relativePath with ${content.length} chars")

        val sourceFiles = sourceClassNames.mapNotNull { containingFileByQualifiedName[it] }.distinct()
        val dependencies = if (sourceFiles.isEmpty()) {
            Dependencies.ALL_FILES
        } else {
            Dependencies(false, *sourceFiles.toTypedArray())
        }

        codeGenerator.createNewFile(
            packageName = packageName,
            fileName = fileName,
            extensionName = "kt",
            dependencies = dependencies,
        ).writer().use { it.write(content) }
    }
}

private fun isValidExportType(type: TypeInfo): Boolean =
    GodotPrimitives.isPrimitive(type.qualifiedName) || type.isGodotBuiltin()

private const val SIGNAL_NOTE = "This is a compile error: binding generation is aborted for the whole build until " +
    "it's fixed"

/**
 * Validates the `@RegisterSignal` properties of this class: their type, that a `TypedSignal` is the single source of
 * its name and parameters, the parameter names and types, and that signal names are unique.
 */
internal fun ClassInfo.validateSignals(): List<DiagnosticMessage> = buildList {
    val registeredNames = mutableMapOf<String, PropertyInfo>()

    for (property in properties) {
        val annotation = property.getRegisterSignalAnnotation() ?: continue
        val location = DiagnosticLocation(filePath, property.lineNumber, 0)
        fun report(code: DiagnosticCode, message: String, help: String) {
            add(DiagnosticMessage.error(code, message, location, help, SIGNAL_NOTE))
        }

        val explicitName = (annotation.arguments["name"] as? String).orEmpty()
        val signalName = explicitName.ifEmpty { property.name }
        val previous = registeredNames.put(signalName, property)
        if (previous != null) {
            report(
                DiagnosticCode.DUPLICATE_SIGNAL_NAME,
                "Signal '$signalName' of '$shortName' is declared by both '${previous.name}' and '${property.name}'",
                "Rename one of the properties",
            )
        }

        when (val signal = property.signal) {
            null -> report(
                DiagnosticCode.INVALID_SIGNAL_PROPERTY_TYPE,
                "@RegisterSignal '${property.name}' has type '${property.type.qualifiedName}', not a signal",
                "Declare it as `val ${property.name} by signal<(name: Type) -> Unit>()`",
            )

            is SignalDeclaration.Invalid -> report(
                DiagnosticCode.INVALID_SIGNAL_PROPERTY_TYPE,
                "@RegisterSignal '${property.name}' is a TypedSignal, but ${signal.reason}",
                "Use a function type, e.g. `signal<(value: Long) -> Unit>()`",
            )

            SignalDeclaration.Builtin -> Unit

            is SignalDeclaration.Typed -> {
                if ((annotation.arguments["params"] as? List<*>).orEmpty().isNotEmpty()) {
                    report(
                        DiagnosticCode.REDUNDANT_SIGNAL_PARAMS,
                        "@RegisterSignal '${property.name}' declares params, but they come from its type",
                        "Remove `params`: name and type each parameter in the function type instead",
                    )
                }
                if (explicitName.isNotEmpty() && explicitName != property.name) {
                    report(
                        DiagnosticCode.SIGNAL_NAME_MISMATCH,
                        "@RegisterSignal '${property.name}' registers '$explicitName', but a TypedSignal emits " +
                            "under its property name",
                        "Remove `name`, or rename the property to '$explicitName'",
                    )
                }
                signal.parameters.forEachIndexed { index, parameter ->
                    validateSignalParameter(property, index, parameter, ::report)
                }
            }
        }
    }
}

private fun validateSignalParameter(
    property: PropertyInfo,
    index: Int,
    parameter: SignalParameterInfo,
    report: (DiagnosticCode, String, String) -> Unit,
) {
    val label = parameter.name.ifEmpty { "#${index + 1}" }
    if (parameter.name.isEmpty()) {
        report(
            DiagnosticCode.UNNAMED_SIGNAL_PARAMETER,
            "Parameter #${index + 1} of signal '${property.name}' has no name",
            "Name it in the function type, e.g. `(value: ${parameter.type.shortName}) -> Unit`; Godot shows it in " +
                "the editor and in connected methods",
        )
    }
    val type = parameter.type
    val isVariant = type.qualifiedName == VARIANT_FQN
    if (type.qualifiedName == UNRESOLVED_TYPE) {
        report(
            DiagnosticCode.INVALID_SIGNAL_PARAMETER_TYPE,
            "The type of parameter '$label' of signal '${property.name}' cannot be resolved",
            "Check its import; kotlinc reports the unresolved reference itself",
        )
    } else if (type.qualifiedName == GodotPrimitives.BYTE) {
        // Same rule as the compiler plugin: Byte is signed in Kotlin and unsigned in Godot.
        report(
            DiagnosticCode.INVALID_SIGNAL_PARAMETER_TYPE,
            "Parameter '$label' of signal '${property.name}' is a Byte, which Godot treats as unsigned",
            "Use Int (or Short) instead",
        )
    } else if (!parameter.isGodotObject && !isValidExportType(type)) {
        report(
            DiagnosticCode.INVALID_SIGNAL_PARAMETER_TYPE,
            "Parameter '$label' of signal '${property.name}' has unsupported type '${type.qualifiedName}'",
            "Use a primitive (Int, Long, Float, Double, Boolean, String), a Godot builtin type, Variant or a " +
                "Godot object",
        )
    } else if (type.isNullable && !parameter.isGodotObject && !isVariant) {
        report(
            DiagnosticCode.INVALID_SIGNAL_PARAMETER_TYPE,
            "Parameter '$label' of signal '${property.name}' is nullable '${type.shortName}?'",
            "Only objects and Variant can be null in Godot; use '${type.shortName}' or Variant",
        )
    }
}

private const val VARIANT_FQN = "io.github.kingg22.godot.api.builtin.Variant"

/** What KspMapping records for a type KSP cannot resolve. */
private const val UNRESOLVED_TYPE = "<unknown>"
