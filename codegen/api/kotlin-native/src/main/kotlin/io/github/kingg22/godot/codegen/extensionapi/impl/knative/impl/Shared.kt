package io.github.kingg22.godot.codegen.extensionapi.impl.knative.impl

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.kingg22.godot.codegen.extensionapi.Context
import io.github.kingg22.godot.codegen.extensionapi.TypeResolver
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinLayout
import io.github.kingg22.godot.codegen.types.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun buildLayoutConstants(layout: ResolvedBuiltinLayout): List<PropertySpec> =
    layout.memberOffsets.map { (member, offset) ->
        PropertySpec
            .builder("OFFSET_${member.uppercase()}", INT, KModifier.CONST)
            .initializer("%L", offset)
            .addKdoc(
                "Byte offset of member `%L` for build configuration `%L`.",
                member,
                layout.buildConfiguration.jsonName,
            )
            .build()
    }

/**
 * Maps a member_offsets [meta] to the StructMemory function name and the Kotlin type
 * that corresponds to the physical storage.
 *
 * This mapping is INDEPENDENT of build configuration — meta="float" always means
 * C float (32-bit), never real_t. The build-config-dependent widening (Float→Double
 * in float_64) is handled separately via [storageToPropertyConv].
 */
fun metaToStorageInfo(meta: String, type: String): Pair<String, TypeName>? {
    check(type == "set" || type == "get")
    return when (meta.lowercase()) {
        "float" -> "${type}Float" to FLOAT
        "double" -> "${type}Double" to DOUBLE
        "int32" -> "${type}Int" to INT
        "int64" -> "${type}Long" to LONG
        "uint32" -> "${type}UInt" to U_INT
        "uint64" -> "${type}ULong" to U_LONG
        "int8" -> "${type}Byte" to BYTE
        "uint8" -> "${type}UByte" to U_BYTE
        "int16" -> "${type}Short" to SHORT
        "uint16" -> "${type}UShort" to U_SHORT
        else -> null // compound (Vector2, Vector3, …) — caller falls back to todoGetter
    }
}

/** Widening conversion from physical storage type to API property type. */
fun storageToPropertyConv(storage: TypeName, property: TypeName): String? = when (storage) {
    FLOAT if property == DOUBLE -> ".toDouble()"
    DOUBLE if property == FLOAT -> ".toFloat()"
    INT if property == LONG -> ".toLong()"
    LONG if property == INT -> ".toInt()"
    U_INT if property == U_LONG -> ".toULong()"
    U_LONG if property == U_INT -> ".toUInt()"
    U_INT if property == INT -> ".toInt()"
    U_LONG if property == LONG -> ".toLong()"
    LONG if property == U_INT -> ".toUInt()"
    LONG if property == U_LONG -> ".toULong()"
    else -> null
}

/** Narrowing conversion from API property type to physical storage type (setter path). */
fun propertyToStorageConv(property: TypeName, storage: TypeName) = storageToPropertyConv(property, storage)

/** Maps a Kotlin primitive TypeName to its CVar equivalent for stack allocation, or null for builtin classes. */
fun primitiveKotlinToCVar(type: TypeName): TypeName? = when (type) {
    BOOLEAN -> U_BYTE_VAR
    FLOAT -> FLOAT_VAR
    DOUBLE -> DOUBLE_VAR
    INT -> INT_VAR
    LONG -> LONG_VAR
    BYTE -> BYTE_VAR
    SHORT -> SHORT_VAR
    U_BYTE -> U_BYTE_VAR
    U_SHORT -> U_SHORT_VAR
    U_INT -> U_INT_VAR
    U_LONG -> U_LONG_VAR
    else -> null // builtin class
}

inline fun buildLazyBlock(body: CodeBlock.Builder.() -> Unit): CodeBlock {
    contract { callsInPlace(body, InvocationKind.EXACTLY_ONCE) }
    return CodeBlock
        .builder()
        .beginControlFlow("%M(PUBLICATION)", lazyMethod)
        .apply(body)
        .endControlFlow()
        .build()
}

fun CodeBlock.Companion.ofStatement(format: String, vararg args: Any?) = builder().addStatement(format, *args).build()

// ── Shared duplication helpers for method/function generators ───────────────

/** True for the Godot primitive types that use stack-allocated CVar in generators. */
fun isGodotPrimitive(type: String): Boolean = type == "float" || type == "int" || type == "bool" || type == "double"

/**
 * Allocates a CVar stack variable for a method/function argument.
 * Works for engine methods (typeResolver-based), builtin methods (string-based),
 * and utility functions (primitive-only).
 *
 * When typeResolver is null, uses string-based dispatch:
 * - Godot primitives (float/double/int/bool) → alloc
 * - enum/bitfield → alloc LongVar
 * - engine class / singleton → alloc COpaquePointerVar
 * - builtin classes → no alloc (rawPtr passed directly)
 */
context(ctx: Context)
fun buildArgAlloc(
    arg: MethodArg,
    implPackageRegistry: ImplementationPackageRegistry,
    typeResolver: TypeResolver,
): CodeBlock {
    val name = safeIdentifier(arg.name)
    val varName = "${name}Var"

    val kotlinType = typeResolver.resolve(arg)
    val cVarType = primitiveKotlinToCVar(kotlinType)
    return buildCodeBlock {
        when {
            kotlinType == BOOLEAN -> addStatement(
                "val %N = %M(%N)",
                varName,
                implPackageRegistry.memberNameForOrDefault("allocGdBool"),
                name,
            )

            cVarType != null -> {
                addStatement("val %N = %M<%T>()", varName, cinteropAlloc, cVarType)
                addStatement("%N.%M = %N", varName, cinteropValue, name)
            }

            arg.type.startsWith("enum::") || arg.type.startsWith("bitfield::") -> {
                addStatement("val %N = %M<%T>()", varName, cinteropAlloc, LONG_VAR)
                addStatement("%N.%M = %N.value", varName, cinteropValue, name)
            }

            ctx.isEngineClass(arg.type) || ctx.isSingleton(arg.type) -> {
                addStatement("val %N = %M<%T>()", varName, cinteropAlloc, C_OPAQUE_POINTER_VAR)
                addStatement(
                    "%N.%M = %N${if (arg.isNullable) "?" else ""}.rawPtr",
                    varName,
                    cinteropValue,
                    name,
                )
            }
        }
    }
}

/**
 * Produces the pointer expression for an argument passed to a FFI invoke call.
 * Must be called AFTER [buildArgAlloc] — consumes the same type decisions.
 *
 * When typeResolver is null, uses string-based dispatch matching [buildArgAlloc].
 */
context(ctx: Context)
fun argPointerExpression(
    arg: MethodArg,
    implPackageRegistry: ImplementationPackageRegistry,
    typeResolver: TypeResolver,
): CodeBlock {
    val _ = implPackageRegistry
    val name = safeIdentifier(arg.name)
    val varName = "${name}Var"

    val kotlinType = typeResolver.resolve(arg)
    val cVarType = primitiveKotlinToCVar(kotlinType)

    return when {
        kotlinType == BOOLEAN -> CodeBlock.ofStatement("%L,", varName)

        kotlinType == COPAQUE_POINTER ||
            kotlinType == ctx.classNameForOrDefault("GDExtensionInitializationFunction") ||
            (kotlinType is ParameterizedTypeName && kotlinType.rawType == C_POINTER)
        -> CodeBlock.ofStatement("%N,", name)

        cVarType != null -> CodeBlock.ofStatement("%N.%M,", varName, cinteropPtr)

        ctx.isNativeStructure(arg.type) -> CodeBlock.ofStatement("%N.%M,", name, cinteropPtr)

        arg.type.startsWith("enum::") || arg.type.startsWith("bitfield::") ->
            CodeBlock.ofStatement("%N.%M,", varName, cinteropPtr)

        ctx.isEngineClass(arg.type) || ctx.isSingleton(arg.type) ->
            CodeBlock.ofStatement("%N.%M,", varName, cinteropPtr)

        ctx.isBuiltin(arg.type) ||
            arg.type.startsWith("array") ||
            arg.type.startsWith("dictionary") ||
            arg.type.startsWith("typeddictionary") ||
            arg.type.startsWith("typedarray")
        -> CodeBlock.ofStatement("%N${if (arg.isNullable) "?" else ""}.rawPtr,", name)

        else -> error("Invalid arg type, unknown strategy to invoke: '${arg.type}' (resolved: $kotlinType)")
    }
}

/**
 * Allocates the return buffer for a method/function invocation.
 * Works for engine methods (typeResolver-based), builtin methods (string-based),
 * and utility functions (primitive-only).
 *
 * When typeResolver is null, uses string-based dispatch:
 * - Godot primitives → alloc CVar
 * - enum/bitfield → alloc LongVar
 * - engine class → alloc COpaquePointerVar
 * - builtin classes → construct directly
 */
context(ctx: Context)
fun buildReturnAlloc(
    returnType: String,
    implPackageRegistry: ImplementationPackageRegistry,
    kotlinType: TypeName,
): CodeBlock {
    val cVarType = primitiveKotlinToCVar(kotlinType)

    return buildCodeBlock {
        when {
            kotlinType == BOOLEAN -> addStatement(
                "val retPtr = %M()",
                implPackageRegistry.memberNameForOrDefault("allocGdBool"),
            )

            cVarType != null -> addStatement("val retPtr = %M<%T>()", cinteropAlloc, cVarType)

            ctx.isNativeStructure(returnType) ->
                addStatement("val retPtr = %M<%T>()", cinteropAlloc, kotlinType)

            returnType.startsWith("enum::") || returnType.startsWith("bitfield::") ->
                addStatement("val retPtr = %M<%T>()", cinteropAlloc, LONG_VAR)

            ctx.isBuiltin(returnType) ||
                returnType.startsWith("array") ||
                returnType.startsWith("dictionary") ||
                returnType.startsWith("typeddictionary") ||
                returnType.startsWith("typedarray") ||
                kotlinType == COPAQUE_POINTER ||
                ctx.isEngineClass(returnType) ->
                addStatement("val retPtr = %M<%T>()", cinteropAlloc, C_OPAQUE_POINTER_VAR)

            kotlinType is ParameterizedTypeName && kotlinType.rawType == C_POINTER -> addStatement(
                "val retPtr = %M<%T>()",
                cinteropAlloc,
                C_POINTER_VAR.parameterizedBy(kotlinType.typeArguments),
            )

            else -> error("Invalid return type, unknown strategy: '$returnType' (resolved: $kotlinType)")
        }
    }
}

/**
 * Reads the return value from the allocated return buffer after a FFI invoke call.
 * Must be called AFTER [buildReturnAlloc] — consumes the same type decisions.
 */
context(ctx: Context)
fun buildReturnRead(
    returnType: String,
    implPackageRegistry: ImplementationPackageRegistry,
    kotlinType: TypeName,
    setterMode: Boolean = false,
): CodeBlock {
    val cVarType = primitiveKotlinToCVar(kotlinType)
    val preAppendReturn = if (setterMode) "" else "return "

    return when {
        kotlinType == BOOLEAN -> CodeBlock.ofStatement(
            "${preAppendReturn}retPtr.%M()",
            implPackageRegistry.memberNameForOrDefault("readGdBool"),
        )

        cVarType != null -> CodeBlock.ofStatement("${preAppendReturn}retPtr.%M", cinteropValue)

        kotlinType == COPAQUE_POINTER ||
            (kotlinType is ParameterizedTypeName && kotlinType.rawType == C_POINTER) -> {
            CodeBlock
                .builder()
                .addStatement("$preAppendReturn%M(retPtr.%M) {", K_REQUIRE_NOT_NULL, cinteropValue)
                .indent()
                .addStatement(
                    "%S",
                    "${returnType.removePrefix("const ").removeSuffix("*").trim()} pointer value was null",
                )
                .endControlFlow()
                .build()
        }

        returnType.startsWith("enum::") -> {
            val godotEnum = ctx.classNameForOrDefault("GodotEnum", "GodotEnum")
            CodeBlock.ofStatement("$preAppendReturn%T.fromValue<%T>(retPtr.%M)", godotEnum, kotlinType, cinteropValue)
        }

        returnType.startsWith("bitfield::") ->
            CodeBlock.ofStatement("$preAppendReturn%T(retPtr.%M)", kotlinType, cinteropValue)

        ctx.isBuiltin(returnType) ||
            ctx.isEngineClass(returnType) ||
            returnType.startsWith("array") ||
            returnType.startsWith("dictionary") ||
            returnType.startsWith("typeddictionary") ||
            returnType.startsWith("typedarray") -> {
            CodeBlock
                .builder()
                .addStatement("$preAppendReturn%T(", kotlinType)
                .indent()
                .addStatement("%M(retPtr.%M) {", K_REQUIRE_NOT_NULL, cinteropValue)
                .withIndent { addStatement("%S", "$returnType pointer value was null") }
                .withIndent { endControlFlow() }
                .addStatement(")")
                .build()
        }

        else -> CodeBlock.ofStatement("${preAppendReturn}retPtr")
    }
}
