package io.github.kingg22.kogot.analysis.models

/**
 * Represents a class declaration.
 */
data class ClassInfo(
    val qualifiedName: String,
    val shortName: String,
    val packageName: String,
    val supertypes: List<TypeInfo> = emptyList(),
    val properties: List<PropertyInfo> = emptyList(),
    val functions: List<FunctionInfo> = emptyList(),
    val annotations: List<AnnotationInfo> = emptyList(),
    val modifiers: Set<String> = emptySet(),
    val filePath: String = "",
    val lineNumber: Int = 0,
)

/**
 * Returns true if this class has @Tool annotation.
 */
fun ClassInfo.hasTool(): Boolean =
    annotations.any { it.shortName == "Tool" || it.matches("io.github.kingg22.godot.api.annotations.Tool") }

/**
 * Returns the @Tool annotation or null if not present.
 */
fun ClassInfo.getToolAnnotation(): AnnotationInfo? =
    annotations.find { it.shortName == "Tool" || it.matches("io.github.kingg22.godot.api.annotations.Tool") }

/**
 * Returns all exported properties.
 */
fun ClassInfo.getExportedProperties(): List<PropertyInfo> = properties.filter { it.hasExport() }

/**
 * Returns all RPC functions.
 */
fun ClassInfo.getRpcFunctions(): List<FunctionInfo> = functions.filter { it.hasRpc() }

/**
 * Checks if this class inherits from Node (directly or transitively).
 */
fun ClassInfo.inheritsFromNode(): Boolean =
    supertypes.any { it.qualifiedName == "io.github.kingg22.godot.api.core.Node" }
