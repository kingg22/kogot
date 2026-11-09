package io.github.kingg22.kogot.processor.model

/**
 * Represents a property (field) of a class.
 */
data class PropertyInfo(
    val name: String,
    val type: TypeInfo,
    val isMutable: Boolean = true,
    val hasDefaultValue: Boolean = false,
    val annotations: List<AnnotationInfo> = emptyList(),
    val modifiers: Set<String> = emptySet(),
    val lineNumber: Int = 0,
    /** What the property's type says about a signal; `null` when it is not a signal type. */
    val signal: SignalDeclaration? = null,
)

/** The signal a property declares through its type. */
sealed interface SignalDeclaration {
    /** `TypedSignal<(p1: P1, ...) -> Unit>`: parameters come from the function type. */
    data class Typed(val parameters: List<SignalParameterInfo>) : SignalDeclaration

    /** The builtin `Signal`: parameters can only come from `@RegisterSignal(params)`. */
    data object Builtin : SignalDeclaration

    /** A `TypedSignal` whose type argument is not a function type (e.g. an unresolved type). */
    data class Invalid(val reason: String) : SignalDeclaration
}

/**
 * One parameter of a typed signal.
 *
 * @property name the function type's parameter name (`@ParameterName`), empty when it has none.
 * @property godotClassName for a `GodotObject` subclass (passed as `OBJECT`), the class Godot knows it as: the nearest
 * registered class in its hierarchy, empty when none is known (any object); `null` for other types.
 */
data class SignalParameterInfo(val name: String, val type: TypeInfo, val godotClassName: String? = null) {
    val isGodotObject: Boolean get() = godotClassName != null
}

/**
 * Returns true if this property has @Export annotation.
 */
fun PropertyInfo.hasExport(): Boolean =
    annotations.any { it.shortName == "Export" || it.matches("io.github.kingg22.godot.api.annotations.Export") }

/**
 * Returns the @Export annotation or null if not present.
 */
fun PropertyInfo.getExportAnnotation(): AnnotationInfo? =
    annotations.find { it.shortName == "Export" || it.matches("io.github.kingg22.godot.api.annotations.Export") }

/**
 * Returns the @RegisterSignal annotation or null if not present.
 */
fun PropertyInfo.getRegisterSignalAnnotation(): AnnotationInfo? = annotations.find {
    it.shortName == "RegisterSignal" || it.matches("io.github.kingg22.godot.api.annotations.RegisterSignal")
}
