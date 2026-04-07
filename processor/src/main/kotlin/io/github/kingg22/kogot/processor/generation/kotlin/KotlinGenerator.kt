package io.github.kingg22.kogot.processor.generation.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.kogot.analysis.models.ClassInfo
import io.github.kingg22.kogot.analysis.models.getExportedProperties
import io.github.kingg22.kogot.analysis.models.getRpcFunctions
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticMessage
import io.github.kingg22.kogot.processor.generation.GeneratedFile
import io.github.kingg22.kogot.processor.generation.GeneratedOutput
import io.github.kingg22.kogot.processor.generation.Generator
import io.github.kingg22.kogot.processor.generation.GeneratorContext

/**
 * Generates Kotlin binding code for annotated classes.
 */
class KotlinGenerator : Generator {
    override val name: String = "KotlinGenerator"

    override fun generate(context: GeneratorContext, classes: List<ClassInfo>): GeneratedOutput {
        val files = mutableListOf<GeneratedFile>()
        val diagnostics = mutableListOf<DiagnosticMessage>()

        for (classInfo in classes) {
            try {
                val file = generateBindingFile(classInfo, context)
                files.add(GeneratedFile(file.relativePath, file.content))
            } catch (e: Exception) {
                diagnostics.add(
                    DiagnosticMessage.error(
                        code = io.github.kingg22.kogot.processor.diagnostics.DiagnosticCode.GENERATION_FAILED,
                        message = "Failed to generate binding for ${classInfo.qualifiedName}: ${e.message}",
                        location = io.github.kingg22.kogot.processor.diagnostics.DiagnosticLocation(
                            classInfo.filePath,
                            classInfo.lineNumber,
                            0,
                        ),
                    ),
                )
            }
        }

        return GeneratedOutput(files, diagnostics)
    }

    private fun generateBindingFile(classInfo: ClassInfo, context: GeneratorContext): GeneratedFile {
        val bindingClassName = "${classInfo.shortName}Binding"
        val packageName = context.outputPackage

        val fileSpec = FileSpec.builder(packageName, bindingClassName)

        // Add suppress annotation if configured
        if (context.options.addSuppressAnnotations) {
            fileSpec.addAnnotation(
                com.squareup.kotlinpoet.AnnotationSpec.builder(Suppress::class)
                    .addMember("\"UNUSED\"")
                    .build(),
            )
        }

        val typeSpec = TypeSpec.classBuilder(bindingClassName)

        // Add KDoc if configured
        if (context.options.generateKdoc) {
            typeSpec.addKdoc("Generated binding for %L\n", classInfo.qualifiedName)
            typeSpec.addKdoc("\nThis class is auto-generated. Do not modify manually.")
        }

        // Add companion object with registration function
        val companion = TypeSpec.companionObjectBuilder()

        val registerFun = FunSpec.builder("register")
            .addStatement("println(%S)", "Registering binding for ${classInfo.shortName}")

        // Register exported properties
        val exportedProps = classInfo.getExportedProperties()
        for (prop in exportedProps) {
            registerFun.addStatement(
                "// Register property: %L",
                prop.name,
            )
        }

        // Register RPC functions
        val rpcFuncs = classInfo.getRpcFunctions()
        for (rpc in rpcFuncs) {
            registerFun.addStatement(
                "// Register RPC: %L",
                rpc.name,
            )
        }

        companion.addFunction(registerFun.build())
        typeSpec.addType(companion.build())

        fileSpec.addType(typeSpec.build())

        val builtFile = fileSpec.build()
        val content = StringBuilder()
        builtFile.writeTo(content)

        return GeneratedFile(
            relativePath = "${packageName.replace('.', '/')}/$bindingClassName.kt",
            content = content.toString(),
        )
    }
}
