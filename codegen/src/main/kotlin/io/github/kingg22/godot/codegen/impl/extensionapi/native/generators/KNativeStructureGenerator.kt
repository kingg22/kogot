package io.github.kingg22.godot.codegen.impl.extensionapi.native.generators

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.impl.K_SUPPRESS
import io.github.kingg22.godot.codegen.impl.createFile
import io.github.kingg22.godot.codegen.impl.extensionapi.Context
import io.github.kingg22.godot.codegen.impl.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.extensionapi.native.*
import io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver.NativeStructureParser
import io.github.kingg22.godot.codegen.impl.extensionapi.native.resolver.NativeStructureParser.NativeStructureField
import io.github.kingg22.godot.codegen.impl.renameGodotClass
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.sanitizeTypeName
import io.github.kingg22.godot.codegen.models.extensionapi.NativeStructure

class KNativeStructureGenerator(private val typeResolver: TypeResolver, private val body: BodyGenerator) {

    context(context: Context)
    fun generateFile(ns: NativeStructure): FileSpec? {
        val spec = generateSpec(ns) ?: return null
        return createFile(spec, spec.name!!, context.packageForOrDefault(spec.name!!))
    }

    context(_: Context)
    fun generateSpec(ns: NativeStructure): TypeSpec? {
        val fields = NativeStructureParser.parseFormat(ns.format)

        // Determinar si la estructura puede ser CStructVar pura
        val canBeCStructVar = canGenerateAsCStructVar(fields)

        return if (canBeCStructVar) {
            if (ns.name == "ObjectID" || ns.name == "AudioFrame") return null
            generateCStructVar(ns, fields)
        } else {
            generateOpaqueWrapper(ns, fields)
        }
    }

    // ESTRATEGIA 1: CStructVar (compatible con cinterop)
    context(_: Context)
    private fun generateCStructVar(ns: NativeStructure, fields: List<NativeStructureField>): TypeSpec {
        println(
            "WARNING: Generating CStructVar for ${ns.name} with fields: ${fields.joinToString { it.name }}, " +
                "must be defined in header file",
        )
        val className = sanitizeTypeName(ns.name.renameGodotClass())

        return TypeSpec
            .classBuilder(className)
            .superclass(C_STRUCT_VAR)
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addParameter("rawPtr", C_NATIVE_PTR)
                    .addModifiers(KModifier.INTERNAL)
                    .build(),
            )
            .addSuperclassConstructorParameter("rawPtr")
            .experimentalApiAnnotation(ns.name)
            .addKdoc(
                """
                Native C structure: ${ns.name}

                This class inherits from CStructVar and can be used directly with cinterop.
                All fields are backed by native memory.
                """.trimIndent(),
            )
            .addProperties(fields.map { field -> generateCStructVarProperty(field) })
            .build()
    }

    context(_: Context)
    private fun generateCStructVarProperty(field: NativeStructureField): PropertySpec {
        val fieldType = typeResolver.resolve(field.type)

        return when {
            // Arrays → usar arrayMemberAt<T>(offset)
            field.arraySize != null -> {
                generateArrayProperty(field, fieldType)
            }

            // Primitivos y punteros → usar memberAt<T>(offset)
            else -> {
                generateSimpleProperty(field, fieldType)
            }
        }
    }

    private fun generateSimpleProperty(field: NativeStructureField, fieldType: TypeName): PropertySpec {
        val propertyName = safeIdentifier(field.name)
        return PropertySpec
            .builder(propertyName, mapKotlinToCinteropType(fieldType))
            .mutable(true)
            .getter(FunSpec.getterBuilder().addModifiers(KModifier.EXTERNAL).build())
            .setter(FunSpec.setterBuilder().addModifiers(KModifier.EXTERNAL).build())
            // .initializer("%M(%L)", C_MEMBER_AT_FUN, calculateOffset(field))
            .addKdoc("Original name: `%S`", field.name)
            .addKdoc("\n\nOriginal type: `%S`", field.type)
            .build()
    }

    private fun generateArrayProperty(field: NativeStructureField, elementType: TypeName): PropertySpec {
        val propertyName = safeIdentifier(field.name)
        val arraySize = field.arraySize!!

        // CArrayPointer<T> para arrays
        val arrayType = C_ARRAY_POINTER.parameterizedBy(
            mapKotlinToCinteropType(elementType),
        )

        return PropertySpec
            .builder(propertyName, arrayType)
            .delegate("arrayMemberAt<%T>(%L)", elementType, calculateOffset(field))
            .addKdoc("Original name: `%S`", field.name)
            .addKdoc("\n\nArray of %L elements of type `%T`", arraySize, elementType)
            .build()
    }

    private fun mapKotlinToCinteropType(elementType: TypeName) = when (elementType) {
        BYTE -> BYTE_VAR
        SHORT -> SHORT_VAR
        INT -> INT_VAR
        LONG -> LONG_VAR
        U_BYTE -> U_BYTE_VAR
        U_SHORT -> U_SHORT_VAR
        U_INT -> U_INT_VAR
        U_LONG -> U_LONG_VAR
        FLOAT -> FLOAT_VAR
        DOUBLE -> DOUBLE_VAR
        else -> error("Unsupported array element type: $elementType")
    }

    // ESTRATEGIA 2: Wrapper Opaco (para estructuras con tipos de API)
    context(context: Context)
    private fun generateOpaqueWrapper(ns: NativeStructure, fields: List<NativeStructureField>): TypeSpec {
        val className = sanitizeTypeName(ns.name.renameGodotClass())

        return TypeSpec
            .classBuilder(className)
            .experimentalApiAnnotation(ns.name)
            .addKdoc(
                """
                Native structure wrapper: ${ns.name}

                ⚠️ This structure contains references to generated API types.
                It uses an opaque handle and TODO() accessors until the implementation layer is complete.
                """.trimIndent(),
            )
            .primaryConstructor(
                FunSpec
                    .constructorBuilder()
                    .addModifiers(KModifier.INTERNAL)
                    .addParameter("handle", COPAQUE_POINTER)
                    .build(),
            )
            .superclass(C_STRUCT_VAR)
            .addSuperclassConstructorParameter("handle.rawValue")
            .addProperties(
                // Generar properties con TODO() como antes
                fields.map { field ->
                    val type = typeResolver.resolve(field.type)
                    val resolvedType = if (field.arraySize != null) {
                        context
                            .classNameForOrDefault("Array", "GodotArray", typedClass = true)
                            .parameterizedBy(type)
                    } else {
                        type
                    }
                    val kotlinName = safeIdentifier(field.name)

                    PropertySpec
                        .builder(kotlinName, resolvedType)
                        .mutable(true)
                        .getter(body.todoGetter())
                        .setter(
                            FunSpec
                                .setterBuilder()
                                .addParameter("value", resolvedType)
                                .addCode(body.todoBody())
                                .build(),
                        )
                        .apply {
                            addKdoc("Original name: `%S`", field.name)
                            if (field.arraySize != null) {
                                addKdoc("\n\nArray size: %L, type: `%T`", field.arraySize, type)
                            } else {
                                addKdoc("\n\nOriginal type: `%S`", field.type)
                            }
                        }
                        .build()
                },
            )
            .addType(
                TypeSpec.companionObjectBuilder()
                    .superclass(C_STRUCT_VAR_TYPE)
                    .addSuperclassConstructorParameter("size = %L, align = %L", calculateStructSize(fields), 0)
                    .addAnnotation(
                        AnnotationSpec
                            .builder(K_SUPPRESS)
                            .addMember("%S", "DEPRECATION")
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    // HELPERS
    private fun canGenerateAsCStructVar(fields: List<NativeStructureField>): Boolean = fields.all { field ->
        val type = field.type.removePrefix("const ").trim().removeSuffix("*").trim()

        // Solo permitir primitivos, punteros opacos y tipos nativos conocidos
        isPrimitiveCType(type) ||
            isOpaquePointer(type) ||
            isKnownNativeType(type)
    }

    private fun isPrimitiveCType(type: String): Boolean = type.lowercase() in setOf(
        "int8_t", "uint8_t", "int16_t", "uint16_t",
        "int32_t", "uint32_t", "int64_t", "uint64_t",
        "float", "double", "bool",
        "char", "short", "int", "long",
        "size_t", "intptr_t", "uintptr_t",
    )

    private fun isOpaquePointer(type: String): Boolean = type == "void" ||
        (type.startsWith("GDExtension") && type.endsWith("Ptr")) ||
        type.endsWith("*")

    private fun isKnownNativeType(type: String): Boolean {
        // Tipos que sabemos que son seguros para CStructVar
        // (otros structs nativos que ya fueron generados)
        return false // Por ahora, conservador
    }

    private fun calculateOffset(field: NativeStructureField): Int {
        // TODO: Implementar cálculo real de offsets basado en el formato
        // Por ahora placeholder
        return 0
    }

    private fun calculateStructSize(fields: List<NativeStructureField>): Int {
        // TODO: Implementar cálculo real del tamaño
        // Por ahora placeholder
        return fields.size * 8 // Estimación burda
    }
}
