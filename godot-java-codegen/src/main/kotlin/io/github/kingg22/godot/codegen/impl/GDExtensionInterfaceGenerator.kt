package io.github.kingg22.godot.codegen.impl

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeSpec
import io.github.kingg22.godot.codegen.models.gextensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.models.gextensioninterface.Interface
import io.github.kingg22.godot.codegen.models.gextensioninterface.Types
import java.nio.file.Path

class GDExtensionInterfaceGenerator(private val packageName: String) {
    fun generate(api: GDExtensionInterface, outputDir: Path): List<Path> = api.types.asSequence().map { type ->
        val file = when (type) {
            is Types.EnumType -> generateEnum(type)
            is Types.HandleType -> generateHandle(type)
            is Types.AliasType -> generateAlias(type)
            is Types.StructType -> generateStruct(type)
            is Types.FunctionType -> generateFunctionType(type)
        }
        file.writeTo(outputDir)
    }.plusElement(generateInterface(api.interfaces).writeTo(outputDir)).toList()

    private fun generateEnum(type: Types.EnumType): FileSpec {
        val typeBuilder = TypeSpec.enumBuilder(sanitizeTypeName(type.name))
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("value", INT)
                    .build(),
            )
            .addProperty(
                PropertySpec.builder("value", INT)
                    .initializer("value")
                    .build(),
            )

        type.values.forEach { value ->
            val enumConst = TypeSpec.anonymousClassBuilder()
                .addSuperclassConstructorParameter("%L", value.value)
                .apply {
                    val kdoc = buildKdoc(value.description, emptyList(), emptyList(), null)
                    if (kdoc.isNotBlank()) {
                        addKdoc("%L\n", kdoc)
                    }
                }
                .build()
            typeBuilder.addEnumConstant(sanitizeEnumConstant(value.name), enumConst)
        }

        addCommonDocs(typeBuilder, type.description, emptyList(), type.deprecated)

        return FileSpec.builder(packageName, sanitizeTypeName(type.name))
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateHandle(type: Types.HandleType): FileSpec {
        val typeBuilder = TypeSpec.classBuilder(sanitizeTypeName(type.name))
            .addModifiers(KModifier.ABSTRACT)

        val parent = type.parent?.takeIf { it.isNotBlank() }
        if (parent != null) {
            typeBuilder.superclass(typeNameFor(packageName, parent))
        }

        addCommonDocs(typeBuilder, type.description, emptyList(), type.deprecated)

        return FileSpec.builder(packageName, sanitizeTypeName(type.name))
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateAlias(type: Types.AliasType): FileSpec {
        val typeAlias = TypeAliasSpec.builder(sanitizeTypeName(type.name), typeNameFor(packageName, type.type))
        addCommonDocs(typeAlias, type.description, emptyList(), type.deprecated)

        return FileSpec.builder(packageName, sanitizeTypeName(type.name))
            .addTypeAlias(typeAlias.build())
            .build()
    }

    private fun generateStruct(type: Types.StructType): FileSpec {
        val constructor = FunSpec.constructorBuilder()
        val typeBuilder = TypeSpec.classBuilder(sanitizeTypeName(type.name))
            .addModifiers(KModifier.DATA)

        type.members.forEachIndexed { index, member ->
            val name = safeIdentifier(member.name.ifBlank { "member$index" })
            val typeName = typeNameFor(packageName, member.type)
            constructor.addParameter(name, typeName)
            typeBuilder.addProperty(
                PropertySpec.builder(name, typeName)
                    .initializer(name)
                    .build(),
            )
        }

        typeBuilder.primaryConstructor(constructor.build())

        addCommonDocs(typeBuilder, type.description, emptyList(), type.deprecated)

        return FileSpec.builder(packageName, sanitizeTypeName(type.name))
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateFunctionType(type: Types.FunctionType): FileSpec {
        val typeBuilder = TypeSpec.classBuilder(sanitizeTypeName(type.name))
            .addModifiers(KModifier.ABSTRACT)

        val funSpec = FunSpec.builder("invoke")
            .addModifiers(KModifier.OPEN, KModifier.OPERATOR)
            .addParameters(argumentsToParameters(packageName, type.arguments))
            .returns(returnTypeName(packageName, type.returnValue))
            .addStatement("TODO()")
            .build()

        typeBuilder.addFunction(funSpec)
        addCommonDocs(typeBuilder, type.description, emptyList(), type.deprecated)

        return FileSpec.builder(packageName, sanitizeTypeName(type.name))
            .addType(typeBuilder.build())
            .build()
    }

    private fun generateInterface(interfaces: List<Interface>): FileSpec {
        val typeBuilder = TypeSpec.classBuilder("GDExtensionInterface")
            .addModifiers(KModifier.ABSTRACT)

        interfaces.forEachIndexed { index, iface ->
            val funName = safeIdentifier(iface.name.ifBlank { "function$index" })
            val funSpec = FunSpec.builder(funName)
                .addModifiers(KModifier.OPEN)
                .addParameters(argumentsToParameters(packageName, iface.arguments))
                .returns(returnTypeName(packageName, iface.returnValue))
                .apply {
                    val paramDocs = iface.arguments.mapIndexed { argIndex, arg ->
                        val name = argumentName(arg, argIndex)
                        name to arg.description
                    }
                    val kdoc = buildKdoc(
                        iface.description,
                        iface.see,
                        paramDocs,
                        iface.since,
                    )
                    if (kdoc.isNotBlank()) {
                        addKdoc("%L\n", kdoc)
                    }
                    iface.deprecated?.let { addAnnotation(deprecatedAnnotation(it)) }
                }
                .addStatement("TODO()")
                .build()
            typeBuilder.addFunction(funSpec)
        }

        return FileSpec.builder(packageName, "GDExtensionInterface")
            .addType(typeBuilder.build())
            .build()
    }
}
