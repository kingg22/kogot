package io.github.kingg22.godot.codegen.impl

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import io.github.kingg22.godot.codegen.models.extensionapi.ApiEnum
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinClass
import io.github.kingg22.godot.codegen.models.extensionapi.BuiltinEnum
import io.github.kingg22.godot.codegen.models.extensionapi.ExtensionApi
import io.github.kingg22.godot.codegen.models.extensionapi.GodotClass
import io.github.kingg22.godot.codegen.models.extensionapi.NativeStructure
import io.github.kingg22.godot.codegen.models.extensionapi.UtilityFunction
import java.nio.file.Path

private const val TYPE_PREFIX = "GD"

class ExtensionApiGenerator(private val packageName: String) {
    fun generate(api: ExtensionApi, outputDir: Path): List<Path> {
        val size = (
            api.globalEnums.size +
                api.builtinClasses.size +
                1 + // variant class
                api.classes.size +
                api.nativeStructures.size +
                1 // utility functions
            )

        val builtinClasses = api.builtinClasses.asSequence().map { clazz ->
            generateBuiltinClass(clazz).writeTo(outputDir)
        }

        val classes = api.classes.asSequence().map { clazz ->
            generateClass(clazz).writeTo(outputDir)
        }

        if (api.globalConstants.isNotEmpty()) {
            System.err.println(
                "WARNING: Global constants are not supported yet. Found: [${api.globalConstants.joinToString()}]",
            )
        }

        val (nestedEnum, globalEnums) = api.globalEnums.partition { it.name.contains(".") }

        if (nestedEnum.size > 2) {
            System.err.println(
                "WARNING: Nested enums (${nestedEnum.size}) [" + nestedEnum.joinToString(postfix = "]") { it.name },
            )
        }

        val enums = globalEnums.asSequence().map { enumDef ->
            val enumSpec = generateEnum(enumDef)
            createFile(enumSpec, enumDef.name).writeTo(outputDir)
        }

        val variantClass = generateVariant(nestedEnum).writeTo(outputDir)

        val utilityFunctions = generateUtilityFunctions(api.utilityFunctions).writeTo(outputDir)

        val nativeStructures = api.nativeStructures.asSequence().map { ns ->
            generateNativeStructure(ns).writeTo(outputDir)
        }

        return ArrayList<Path>(size).apply {
            addAll(builtinClasses)
            addAll(classes)
            addAll(enums)
            add(variantClass)
            add(utilityFunctions)
            addAll(nativeStructures)
        }
    }

    private fun createFile(type: TypeSpec, fileName: String): FileSpec = FileSpec
        .builder(packageName, fileName)
        .commonConfiguration()
        .addType(type)
        .build()

    private fun generateEnum(enumDef: ApiEnum): TypeSpec {
        val typeBuilder = TypeSpec.enumBuilder(enumDef.name)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", LONG)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", LONG)
                    .initializer("value")
                    .build(),
            )

        enumDef.values.forEach { value ->
            val enumConst = TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%L", value.value)
                .build()
            typeBuilder.addEnumConstant(sanitizeEnumConstant(value.name), enumConst)
        }
        return typeBuilder.build()
    }

    private fun generateBuiltinClass(cls: BuiltinClass): FileSpec {
        enrichExceptions({ "Generating builtin class '${cls.name}'" }) {
            val typeBuilder = TypeSpec.classBuilder(cls.name)
                .addModifiers(KModifier.OPEN)

            // Métodos
            cls.methods.forEach { method ->
                enrichExceptions({ "Error generating function '${method.name}', type: ${method.returnType}" }) {
                    val methodReturnType = method.returnType?.let { typeNameFor(packageName, it) } ?: UNIT
                    val funSpec = generateMethod(method.name, method.returnType, methodReturnType) {
                        enrichExceptions({
                            "Generating parameters: [${
                                method.arguments.joinToString {
                                    "Name: ${it.name}, type: ${it.type}"
                                }
                            }]"
                        }) {
                            methodArgsToParameters(packageName, method.isVararg, method.arguments)
                        }
                    }.build()
                    typeBuilder.addFunction(funSpec)
                }
            }

            fun BuiltinEnum.asApiEnum() = ApiEnum(name = name, isBitfield = false, values = values)

            // Enums anidados (aquí vive Variant.Type, etc.)
            cls.enums.forEach { enumDef ->
                typeBuilder.addType(generateEnum(enumDef.asApiEnum()))
            }

            return createFile(typeBuilder.build(), cls.name)
        }
    }

    private fun generateClass(cls: GodotClass): FileSpec {
        enrichExceptions({ "Generating class '${cls.name}'" }) {
            val typeBuilder = TypeSpec.classBuilder(cls.name)
                .addModifiers(KModifier.ABSTRACT)

            val parent = cls.inherits?.takeIf { it.isNotBlank() }
            if (parent != null) {
                typeBuilder.superclass(typeNameFor(packageName, parent))
            }

            cls.methods.forEach { method ->
                enrichExceptions({ "Error generating function '${method.name}', type: ${method.returnValue?.type}" }) {
                    val methodReturnType = methodReturnTypeName(packageName, method.returnValue)
                    val funSpec = generateMethod(method.name, method.returnValue?.type, methodReturnType) {
                        enrichExceptions({
                            "Generating parameters: [${
                                method.arguments.joinToString {
                                    "Name: ${it.name}, type: ${it.type}"
                                }
                            }]"
                        }) {
                            methodArgsToParameters(packageName, method.isVararg, method.arguments)
                        }
                    }.build()
                    typeBuilder.addFunction(funSpec)
                }
            }

            val enumSpecs = cls.enums.map { enumDef -> generateEnum(enumDef) }

            typeBuilder.addTypes(enumSpecs)

            return createFile(typeBuilder.build(), cls.name)
        }
    }

    private fun generateNativeStructure(ns: NativeStructure): FileSpec {
        val typeBuilder = TypeSpec.classBuilder(ns.name).addModifiers(KModifier.OPEN)
        return createFile(typeBuilder.build(), ns.name)
    }

    private fun generateMethod(
        name: String,
        returnTypeString: String?,
        returnType: TypeName,
        modifiers: List<KModifier> = listOf(KModifier.OPEN),
        arguments: () -> List<ParameterSpec>,
    ): FunSpec.Builder {
        val methodName = safeIdentifier(name)
        val funSpec = FunSpec.builder(methodName)
            .addModifiers(modifiers)
            .addParameters(arguments())
            .returns(returnType)
            .addKdocForBitfield(returnTypeString, "@return")
            .addStatement("TODO()")
            .accidentalOverride(methodName, returnType)
        return funSpec
    }

    private fun generateUtilityFunctions(functions: List<UtilityFunction>): FileSpec {
        enrichExceptions({ "Generating utility functions, count: ${functions.size}" }) {
            val typeBuilder = TypeSpec
                .objectBuilder("GD")
                .addKdoc("Utility functions for Godot API.")

            functions.forEach { method ->
                enrichExceptions({ "Error generating function '${method.name}', type: ${method.returnType}" }) {
                    val methodReturnType = method.returnType?.let { typeNameFor(packageName, it) } ?: UNIT
                    val funSpec = generateMethod(method.name, method.returnType, methodReturnType, emptyList()) {
                        enrichExceptions({
                            "Generating parameters: [${
                                method.arguments.joinToString {
                                    "Name: ${it.name}, type: ${it.type}"
                                }
                            }]"
                        }) {
                            methodArgsToParameters(packageName, method.isVararg, method.arguments)
                        }
                    }.addKdoc("Category: %L", method.category).build()
                    typeBuilder.addFunction(funSpec)
                }
            }

            return createFile(typeBuilder.build(), "GD")
        }
    }

    /** enums internos vendrán de globalEnums Variant. */
    private fun generateVariant(enums: List<ApiEnum>): FileSpec {
        enrichExceptions({ "Generating Variant class, nested enums count: ${enums.size}" }) {
            val typeBuilder = TypeSpec
                .classBuilder("Variant")
                .addModifiers(KModifier.SEALED)
                .addTypes(
                    enums.map {
                        enrichExceptions({ "Error generating nested enum '${it.name}'" }) {
                            generateEnum(it.copy(name = it.name.substringAfterLast(".")))
                        }
                    },
                )
            // TODO generate nested class of Variant.Type
            // TODO add operators
            return createFile(typeBuilder.build(), "Variant")
        }
    }

    private fun FunSpec.Builder.accidentalOverride(funName: String, returnType: TypeName): FunSpec.Builder =
        if (funName == "wait" && returnType == UNIT) {
            System.err.println("WARNING: modifying wait() to avoid conflicts with JVM Object method")
            this
                .addKdoc(
                    "Generated Note: Original name was `wait`, renamed to avoid conflicts with JVM [java.lang.Object] method.",
                )
                .build()
                .toBuilder("await")
        } else {
            this
        }

    private inline fun <T> enrichExceptions(metadata: () -> String, block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        throw RuntimeException(metadata(), e)
    }
}
