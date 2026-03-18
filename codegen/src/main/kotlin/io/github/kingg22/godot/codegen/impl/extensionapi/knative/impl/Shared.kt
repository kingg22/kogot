package io.github.kingg22.godot.codegen.impl.extensionapi.knative.impl

import com.squareup.kotlinpoet.*
import io.github.kingg22.godot.codegen.impl.extensionapi.knative.*
import io.github.kingg22.godot.codegen.models.extensionapi.domain.ResolvedBuiltinLayout
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

// ── meta → StructMemory function name ────────────────────────────
fun metaToGetFun(meta: String): String? = when (meta.lowercase()) {
    "float" -> "getFloat"
    "double" -> "getDouble"
    "int32" -> "getInt"
    "int64" -> "getLong"
    "uint32" -> "getUInt"
    "uint64" -> "getULong"
    "int8" -> "getByte"
    "uint8" -> "getUByte"
    "int16" -> "getShort"
    "uint16" -> "getUShort"
    else -> null // compound type (e.g. "Vector2") — caller falls back to TODO
}

fun metaToSetFun(meta: String): String? = when (meta.lowercase()) {
    "float" -> "setFloat"
    "double" -> "setDouble"
    "int32" -> "setInt"
    "int64" -> "setLong"
    "uint32" -> "setUInt"
    "uint64" -> "setULong"
    "int8" -> "setByte"
    "uint8" -> "setUByte"
    "int16" -> "setShort"
    "uint16" -> "setUShort"
    else -> null
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
    else -> null
}

/** Narrowing conversion from API property type to physical storage type (setter path). */
fun propertyToStorageConv(property: TypeName, storage: TypeName): String? = storageToPropertyConv(property, storage)

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
