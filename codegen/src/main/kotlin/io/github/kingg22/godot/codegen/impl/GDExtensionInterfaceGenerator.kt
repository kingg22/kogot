package io.github.kingg22.godot.codegen.impl

import com.squareup.kotlinpoet.*
import io.github.kingg22.godot.codegen.models.gextensioninterface.Arguments
import io.github.kingg22.godot.codegen.models.gextensioninterface.Deprecated
import io.github.kingg22.godot.codegen.models.gextensioninterface.GDExtensionInterface
import io.github.kingg22.godot.codegen.models.gextensioninterface.Interface
import io.github.kingg22.godot.codegen.models.gextensioninterface.Types
import io.github.kingg22.godot.codegen.models.gextensioninterface.ValueType
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

    private fun createFile(type: TypeSpec, fileName: String): FileSpec = FileSpec
        .builder(packageName, fileName)
        .commonConfiguration()
        .addType(type)
        .build()

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
                    val kdoc = buildKdoc(value.description)
                    if (kdoc.isNotBlank()) {
                        addKdoc("%L\n", kdoc)
                    }
                }
                .build()
            typeBuilder.addEnumConstant(sanitizeTypeName(value.name), enumConst)
        }

        addCommonDocs(typeBuilder, type.description, emptyList(), type.deprecated)

        return createFile(typeBuilder.build(), sanitizeTypeName(type.name))
    }

    private fun generateHandle(type: Types.HandleType): FileSpec {
        val typeBuilder = TypeSpec.classBuilder(sanitizeTypeName(type.name))
            .addModifiers(KModifier.ABSTRACT)

        val parent = type.parent?.takeIf { it.isNotBlank() }
        if (parent != null) {
            typeBuilder.superclass(typeNameFor(packageName, parent))
        }

        addCommonDocs(typeBuilder, type.description, emptyList(), type.deprecated)

        return createFile(typeBuilder.build(), sanitizeTypeName(type.name))
    }

    private fun generateAlias(type: Types.AliasType): FileSpec {
        val typeAlias = TypeAliasSpec.builder(sanitizeTypeName(type.name), typeNameFor(packageName, type.type))
        addCommonDocs(typeAlias, type.description, emptyList(), type.deprecated)

        return FileSpec.builder(packageName, sanitizeTypeName(type.name))
            .commonConfiguration()
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

        return createFile(typeBuilder.build(), sanitizeTypeName(type.name))
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

        return createFile(typeBuilder.build(), sanitizeTypeName(type.name))
    }

    private fun generateInterface(interfaces: List<Interface>): FileSpec {
        val typeBuilder = TypeSpec.classBuilder("GDExtensionInterface")
            .addModifiers(KModifier.ABSTRACT)

        val functions = interfaces.mapIndexed { index, iface ->
            val funName = safeIdentifier(iface.name.ifBlank { "function$index" })
            FunSpec
                .builder(funName)
                .addModifiers(KModifier.OPEN)
                .addParameters(argumentsToParameters(packageName, iface.arguments))
                .returns(returnTypeName(packageName, iface.returnValue))
                .apply {
                    val kdoc = buildKdoc(
                        iface.description,
                        iface.see,
                        since = iface.since,
                    )
                    if (kdoc.isNotBlank()) {
                        addKdoc("%L", kdoc)
                    }
                    iface.deprecated?.let { addAnnotation(deprecatedAnnotation(it)) }
                }
                .addStatement("TODO()")
                .build()
        }

        typeBuilder.addFunctions(functions)

        return createFile(typeBuilder.build(), "GDExtensionInterface")
    }
}

private fun argumentsToParameters(packageName: String, arguments: List<Arguments>): List<ParameterSpec> =
    arguments.mapIndexed { index, arg ->
        val name = argumentName(arg, index)
        val type = typeNameFor(packageName, arg.type)
        ParameterSpec
            .builder(name, type)
            .apply {
                val kdoc = buildKdoc(arg.description)
                addKdoc(kdoc)
                addKdocForBitfield(arg.type)
            }
            .build()
    }

private fun argumentName(arg: Arguments, index: Int): String {
    val baseName = arg.name?.takeIf { it.isNotBlank() } ?: "arg$index"
    return safeIdentifier(baseName)
}

private fun <T> addCommonDocs(
    builder: T,
    description: List<String>,
    see: List<String>,
    deprecated: Deprecated?,
) where T : Documentable.Builder<T>, T : Annotatable.Builder<T> {
    val kdoc = buildKdoc(description, see)
    if (kdoc.isNotBlank()) {
        builder.addKdoc("%L", kdoc)
    }
    deprecated?.let { builder.addAnnotation(deprecatedAnnotation(it)) }
}

private fun deprecatedAnnotation(deprecated: Deprecated): AnnotationSpec {
    val message = buildString {
        if (!deprecated.message.isNullOrBlank()) {
            append(deprecated.message)
            append(" (since ")
            append(deprecated.since)
            append(')')
        } else {
            append("Deprecated since ")
            append(deprecated.since)
        }
    }

    val builder = AnnotationSpec.builder(K_DEPRECATED)
        .addMember("message = %S", message)

    val replaceWith = deprecated.replaceWith
    if (!replaceWith.isNullOrBlank()) {
        builder.addMember("replaceWith = %T(%S)", K_REPLACE_WITH, replaceWith.snakeCaseToCamelCase())
    }

    return builder.build()
}

private fun returnTypeName(packageName: String, returnValue: ValueType?): TypeName =
    if (returnValue == null) UNIT else typeNameFor(packageName, returnValue.type)
