package io.github.kingg22.kogot.processor.generation.kotlin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.kogot.analysis.models.ClassInfo
import io.github.kingg22.kogot.analysis.models.PropertyInfo
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
                val propertyAnnotations = context.propertyAnnotations[classInfo.qualifiedName] ?: emptyMap()
                files.add(generateBindingFile(classInfo, propertyAnnotations))
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

    private fun generateBindingFile(
        classInfo: ClassInfo,
        propertyAnnotations: Map<String, List<KSAnnotation>>,
    ): GeneratedFile {
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
            .addFunction(generateRegisterFun(classInfo, classType, godotBaseClass, propertyAnnotations))
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

    private fun generateRegisterFun(
        classInfo: ClassInfo,
        classType: ClassName,
        baseClass: String,
        propertyAnnotations: Map<String, List<KSAnnotation>>,
    ): FunSpec {
        val funSpec = FunSpec
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

        // Add signal registrations
        val registerSignalProperties = classInfo.properties.filter { prop ->
            propertyAnnotations[prop.name]
                ?.any { it.shortName.asString() == "RegisterSignal" }
                ?: false
        }

        for (prop in registerSignalProperties) {
            val ksAnnotation = propertyAnnotations[prop.name]
                ?.first { it.shortName.asString() == "RegisterSignal" }
            if (ksAnnotation != null) {
                val annotationCode = buildRegisterSignalAnnotationFromKSAnnotation(prop, ksAnnotation)
                funSpec
                    .addStatement("%M(⇥", REGISTER_CUSTOM_SIGNAL)
                    .addStatement("%S,", classInfo.shortName)
                    .addCode("%L", annotationCode)
                    .addStatement("⇤)")
            }
        }

        return funSpec.build()
    }

    private fun buildRegisterSignalAnnotationFromKSAnnotation(
        prop: PropertyInfo,
        annotation: KSAnnotation,
    ): CodeBlock {
        val builder = CodeBlock.builder()
        builder.addStatement("%T(⇥", REGISTER_SIGNAL_CLASS_NAME)

        // Extract params from the KSAnnotation directly
        val paramsArg = annotation.arguments.find { it.name?.asString() == "params" }
        val nameArg = annotation.arguments.find { it.name?.asString() == "name" }

        val signalName = nameArg?.value?.toString()?.takeIf { it.isNotEmpty() } ?: prop.name

        // Handle params list
        val paramsList = paramsArg?.value as? List<*>
        val paramAnnotations = paramsList.orEmpty().filterIsInstance<KSAnnotation>()

        if (paramAnnotations.isNotEmpty()) {
            paramAnnotations.forEach { paramAnn ->
                // Extract type and name from the param annotation
                val typeArg = paramAnn.arguments.find { it.name?.asString() == "type" }
                val nameArgVal = paramAnn.arguments.find { it.name?.asString() == "name" }
                val paramName = nameArgVal?.value?.toString().orEmpty()
                val typeEntry = typeArg?.value?.toString()
                    ?: error(
                        "Missing type for signal parameter in ${prop.name}, signal param: $paramName. Type arg: $typeArg",
                    )
                builder.addStatement(
                    "%T(%T.%L, %S),", // Param(Variant.Type.NIL, "name")
                    REGISTER_SIGNAL_PARAM_CLASS_NAME,
                    VARIANT_CLASS_NAME,
                    typeEntry,
                    paramName,
                )
            }
        }
        builder.addStatement("name = %S,", signalName)
        builder.addStatement("⇤),")

        return builder.build()
    }

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
