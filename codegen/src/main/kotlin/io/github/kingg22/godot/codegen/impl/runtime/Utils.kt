package io.github.kingg22.godot.codegen.impl.runtime

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ParameterSpec
import io.github.kingg22.godot.codegen.impl.K_DEPRECATED
import io.github.kingg22.godot.codegen.impl.K_REPLACE_WITH
import io.github.kingg22.godot.codegen.impl.buildKdoc
import io.github.kingg22.godot.codegen.impl.safeIdentifier
import io.github.kingg22.godot.codegen.impl.snakeCaseToCamelCase
import io.github.kingg22.godot.codegen.models.extensioninterface.Arguments
import io.github.kingg22.godot.codegen.models.extensioninterface.Deprecated
import io.github.kingg22.godot.codegen.models.extensioninterface.Interface

fun buildKdoc(iface: Interface) = buildKdoc(
    description = iface.description,
    see = iface.see,
    since = iface.since,
    returns = iface.returnValue?.description.orEmpty(),
)

context(resolver: RuntimeTypeResolver)
fun buildParameter(argument: Arguments, index: Int): ParameterSpec = ParameterSpec
    .builder(
        safeIdentifier(argument.name?.takeIf(String::isNotBlank) ?: "arg$index"),
        resolver.resolveType(argument.type),
    )
    .apply {
        if (argument.description.isNotEmpty()) {
            addKdoc("%L", argument.description.joinToString("\n"))
        }
    }
    .build()

fun rawWrapperName(iface: Interface): String = "${memberName(iface).removeSurrounding("`")}Raw"

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

    return AnnotationSpec
        .builder(K_DEPRECATED)
        .addMember("message = %S", message)
        .apply {
            if (!deprecated.replaceWith.isNullOrBlank()) {
                addMember(
                    "replaceWith = %T(%S)",
                    K_REPLACE_WITH,
                    deprecated.replaceWith.snakeCaseToCamelCase(),
                )
            }
        }
        .build()
}

fun memberName(iface: Interface): String {
    val prefix = prefixOf(iface)
    val rawName = iface.name.removePrefix("${prefix}_").ifBlank { iface.name }
    return safeIdentifier(rawName)
}

fun functionPointerPropertyName(iface: Interface): String {
    val rawMemberName = memberName(iface).removeSurrounding("`")
    return safeIdentifier("${rawMemberName}_fn")
}

fun prefixOf(iface: Interface): String = prefixOf(iface.name)

fun prefixOf(symbolName: String): String = symbolName.substringBefore('_')

fun implementationNameForPrefix(prefix: String): String {
    val canonical = when (prefix) {
        "classdb" -> "ClassDB"
        "mem" -> "Memory"
        "packed" -> "PackedArray"
        "ref" -> "Reference"
        "worker" -> "WorkerThreadPool"
        else -> prefix.snakeCaseToCamelCase().replaceFirstChar(Char::uppercaseChar)
    }
    return "${canonical}Binding"
}
