package io.github.kingg22.godot.codegen.impl

import com.squareup.kotlinpoet.*
import io.github.kingg22.godot.codegen.models.extensionapi.MethodArg
import io.github.kingg22.godot.codegen.models.extensionapi.MethodReturn
import io.github.kingg22.godot.codegen.models.gextensioninterface.Arguments
import io.github.kingg22.godot.codegen.models.gextensioninterface.Deprecated
import io.github.kingg22.godot.codegen.models.gextensioninterface.ValueType

// https://kotlinlang.org/docs/reference/keyword-reference.html
val kotlinKeywords = setOf(
    // Hard keywords
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",

    // Soft keywords
    "by",
    "catch",
    "constructor",
    "delegate",
    "dynamic",
    "field",
    "file",
    "finally",
    "get",
    "import",
    "init",
    "param",
    "property",
    "receiver",
    "set",
    "setparam",
    "where",

    // Modifier keywords
    "actual",
    "abstract",
    "annotation",
    "companion",
    "const",
    "crossinline",
    "data",
    "enum",
    "expect",
    "external",
    "final",
    "infix",
    "inline",
    "inner",
    "internal",
    "lateinit",
    "noinline",
    "open",
    "operator",
    "out",
    "override",
    "private",
    "protected",
    "public",
    "reified",
    "sealed",
    "suspend",
    "tailrec",
    "value",
    "vararg",

    // These aren't keywords anymore but still break some code if unescaped. https://youtrack.jetbrains.com/issue/KT-52315
    "header",
    "impl",

    // Other reserved keywords
    "yield",
)

val nameRegex = Regex("[^A-Za-z0-9_]")
val K_DEPRECATED = ClassName("kotlin", "Deprecated")
val K_REPLACE_WITH = ClassName("kotlin", "ReplaceWith")

fun typeNameFor(packageName: String, rawType: String): TypeName {
    var type = rawType.trim()
    type = type.removePrefix("const ").trim()
    while (type.endsWith("*")) {
        type = type.removeSuffix("*").trim()
    }

    val normalizedType = normalizeTypeName(type)
    val normalized = normalizedType.lowercase()
    return when (normalized) {
        "void" -> UNIT

        "bool", "boolean" -> BOOLEAN

        "char" -> CHAR

        "float" -> FLOAT

        "double" -> DOUBLE

        "string", "char16_t", "char32_t", "wchar_t" -> STRING

        "int", "int32_t" -> INT

        "uint", "uint32_t" -> U_INT

        "short", "int16_t" -> SHORT

        "ushort", "uint16_t" -> U_SHORT

        "byte", "int8_t" -> BYTE

        "ubyte", "uint8_t" -> U_BYTE

        "long", "int64_t", "intptr_t" -> LONG

        "ulong", "uint64_t", "uintptr_t", "size_t" -> U_LONG

        else -> ClassName(packageName, sanitizeTypeName(normalizedType))
    }
}

fun argumentsToParameters(packageName: String, arguments: List<Arguments>): List<ParameterSpec> = arguments.mapIndexed {
        index,
        arg,
    ->
    ParameterSpec.builder(argumentName(arg, index), typeNameFor(packageName, arg.type)).build()
}

fun methodArgsToParameters(packageName: String, arguments: List<MethodArg>?): List<ParameterSpec> {
    if (arguments.isNullOrEmpty()) return emptyList()
    return arguments.mapIndexed { index, arg ->
        ParameterSpec.builder(methodArgName(arg, index), typeNameFor(packageName, arg.type)).build()
    }
}

fun argumentName(arg: Arguments, index: Int): String {
    val baseName = arg.name?.takeIf { it.isNotBlank() } ?: "arg$index"
    return safeIdentifier(baseName)
}

fun methodArgName(arg: MethodArg, index: Int): String {
    val baseName = arg.name.takeIf { it.isNotBlank() } ?: "arg$index"
    return safeIdentifier(baseName)
}

fun returnTypeName(packageName: String, returnValue: ValueType?): TypeName =
    if (returnValue == null) UNIT else typeNameFor(packageName, returnValue.type)

fun methodReturnTypeName(packageName: String, returnValue: MethodReturn?): TypeName =
    if (returnValue == null) UNIT else typeNameFor(packageName, returnValue.type)

fun addCommonDocs(builder: TypeSpec.Builder, description: List<String>, see: List<String>, deprecated: Deprecated?) {
    val kdoc = buildKdoc(description, see, emptyList(), null)
    if (kdoc.isNotBlank()) {
        builder.addKdoc("%L\n", kdoc)
    }
    deprecated?.let { builder.addAnnotation(deprecatedAnnotation(it)) }
}

fun addCommonDocs(
    builder: TypeAliasSpec.Builder,
    description: List<String>,
    see: List<String>,
    deprecated: Deprecated?,
) {
    val kdoc = buildKdoc(description, see, emptyList(), null)
    if (kdoc.isNotBlank()) {
        builder.addKdoc("%L\n", kdoc)
    }
    deprecated?.let { builder.addAnnotation(deprecatedAnnotation(it)) }
}

fun buildKdoc(
    description: List<String>,
    see: List<String>,
    paramDocs: List<Pair<String, List<String>>>,
    since: String?,
): String {
    val lines = mutableListOf<String>()
    if (description.isNotEmpty()) {
        lines.addAll(description)
    }
    if (!since.isNullOrBlank()) {
        lines.add("@since $since")
    }
    paramDocs.forEach { (name, docs) ->
        val text = if (docs.isEmpty()) "" else " ${docs.joinToString(" ")}"
        lines.add("@param $name$text")
    }
    see.forEach { entry ->
        lines.add("@see $entry")
    }
    return lines.joinToString("\n")
}

fun deprecatedAnnotation(deprecated: Deprecated): AnnotationSpec {
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
        builder.addMember("replaceWith = %T(%S)", K_REPLACE_WITH, replaceWith)
    }

    return builder.build()
}

fun safeIdentifier(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return "_"
    val sanitized = trimmed.replace(nameRegex, "_")
    val fixed = if (sanitized.first().isDigit()) "_$sanitized" else sanitized
    return if (isKotlinKeyword(fixed)) "`$fixed`" else fixed
}

fun sanitizeEnumConstant(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return "UNKNOWN"
    val sanitized = trimmed.replace(nameRegex, "_")
    val fixed = if (sanitized.first().isDigit()) "_$sanitized" else sanitized
    return if (isKotlinKeyword(fixed)) "${fixed}_" else fixed
}

fun isKotlinKeyword(name: String): Boolean = name in kotlinKeywords

fun sanitizeTypeName(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return "Unnamed"
    val sanitized = trimmed.replace(nameRegex, "_")
    val fixed = if (sanitized.first().isDigit()) "_$sanitized" else sanitized
    return if (isKotlinKeyword(fixed)) "${fixed}_" else fixed
}

fun normalizeTypeName(rawType: String): String {
    var type = rawType.trim()
    if (type.startsWith("enum::")) {
        type = type.removePrefix("enum::")
    }
    if (type.contains("::")) {
        type = type.split("::").joinToString("")
    }
    if (type.contains(".")) {
        type = type.split('.').joinToString("")
    }
    return type
}
