package io.github.kingg22.kogot.processor.generation.kotlin

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.kogot.analysis.models.ClassInfo
import io.github.kingg22.kogot.analysis.models.getParentClassShortName
import io.github.kingg22.kogot.analysis.models.hasGodotAnnotation
import io.github.kingg22.kogot.analysis.models.inheritsFromNode2D
import io.github.kingg22.kogot.analysis.models.inheritsFromSprite2D
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticCode
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticLocation
import io.github.kingg22.kogot.processor.diagnostics.DiagnosticMessage
import io.github.kingg22.kogot.processor.generation.GeneratedFile
import io.github.kingg22.kogot.processor.generation.GeneratedOutput
import io.github.kingg22.kogot.processor.generation.Generator
import io.github.kingg22.kogot.processor.generation.GeneratorContext

class GodotBindingGenerator : Generator {
    override val name: String = "GodotBindingGenerator"

    override fun generate(context: GeneratorContext, classes: List<ClassInfo>): GeneratedOutput {
        val files = mutableListOf<GeneratedFile>()
        val diagnostics = mutableListOf<DiagnosticMessage>()

        val godotClasses = classes.filter { it.hasGodotAnnotation() }

        if (godotClasses.isEmpty()) {
            return GeneratedOutput(emptyList(), emptyList())
        }

        for (classInfo in godotClasses) {
            try {
                files.add(generateBindingFile(classInfo))
            } catch (e: Exception) {
                diagnostics.add(
                    DiagnosticMessage.error(
                        code = DiagnosticCode.GENERATION_FAILED,
                        message = "Failed to generate binding for ${classInfo.qualifiedName}: ${e.message}",
                        location = DiagnosticLocation(
                            classInfo.filePath,
                            classInfo.lineNumber,
                            0,
                        ),
                    ),
                )
            }
        }

        files.add(generateCallbacksFile(godotClasses))

        return GeneratedOutput(files, diagnostics)
    }

    private fun generateBindingFile(classInfo: ClassInfo): GeneratedFile {
        val bindingClassName = "${classInfo.shortName}_Binding"
        val packageName = classInfo.packageName

        val parentClassName = classInfo.getParentClassShortName()
            ?: error("No parent class")

        val godotBaseClass = when {
            classInfo.inheritsFromSprite2D() -> "Sprite2D"
            classInfo.inheritsFromNode2D() -> "Node2D"
            else -> parentClassName
        }

        val classType = ClassName.bestGuess(classInfo.qualifiedName)

        val fileSpec = FileSpec
            .builder(packageName, bindingClassName)
            .applyCommonConfig()
            .optInInternalBindingAndForeignNative()

        val typeSpec = TypeSpec
            .objectBuilder(bindingClassName)
            .addAnnotation(InternalBindingClassName)
            .addFunction(generateRegisterFun(classInfo, classType, godotBaseClass))
            .build()

        // fileSpec.addFunction(generateCreateInstanceFun(classInfo, classType, godotBaseClass))
        fileSpec.addType(typeSpec)

        val content = StringBuilder()
        fileSpec.build().writeTo(content)

        return GeneratedFile(
            "${packageName.replace('.', '/')}/$bindingClassName.kt",
            content.toString(),
        )
    }

    private fun generateCreateInstanceFun(classInfo: ClassInfo, classType: ClassName, godotBaseClass: String) = FunSpec
        .builder(classInfo.shortName)
        .returns(classType)
        .addCode("return %T(\n", classType)
        .addStatement("⇥%M(", CREATE_INSTANCE_FUN)
        .addStatement("⇥%S,", godotBaseClass)
        .addStatement("%S,", classInfo.shortName)
        .addStatement("::%T,", classType)
        .addStatement("⇤)!!,")
        .addCode("⇤)")
        .build()

    private fun generateRegisterFun(classInfo: ClassInfo, classType: ClassName, baseClass: String): FunSpec = FunSpec
        .builder("register")
        .addStatement("%M<%T>(", REGISTER_CLASS, classType)
        .addStatement("⇥%S,", classInfo.shortName)
        .addStatement("%S,", baseClass)
        .addStatement("%M { _, notifyPostInitialize ->", STATIC_C_FUNCTION)
        .addStatement(
            "⇥%M(%S, %S, ::%T, notifyPostInitialize == %T.%M)⇤",
            CREATE_INSTANCE_FUN,
            baseClass,
            classInfo.shortName,
            classType,
            GDExtensionBoolClassName,
            GDExtensionBoolTrueMember,
        )
        .addStatement("},")
        .addStatement("%M(),", CREATE_FREE_INSTANCE_FUN)
        .addStatement("%T.getVirtual,", NodeVirtualDispatcherClassName)
        .addStatement("⇤)")
        .build()

    private fun generateCallbacksFile(classes: List<ClassInfo>): GeneratedFile {
        val packageName = classes.first().packageName + ".generated"
        val className = "GeneratedBindings"

        val type = TypeSpec.classBuilder(className)
            .superclass(BindingInitializationCallbacksClassName)
            .addFunction(
                FunSpec
                    .builder("onInitScene")
                    .addModifiers(KModifier.OVERRIDE)
                    .apply {
                        classes.forEach {
                            addStatement(
                                "%T.register()",
                                ClassName(it.packageName, "${it.shortName}_Binding"),
                            )
                        }
                    }
                    .build(),
            )
            .build()

        val file = FileSpec.builder(packageName, className)
            .applyCommonConfig()
            .optInInternalBindingAndForeignNative()
            .addType(type)
            .build()

        val content = StringBuilder()
        file.writeTo(content)

        return GeneratedFile(
            "${packageName.replace('.', '/')}/$className.kt",
            content.toString(),
        )
    }
}
