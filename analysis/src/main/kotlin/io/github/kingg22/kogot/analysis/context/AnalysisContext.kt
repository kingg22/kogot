package io.github.kingg22.kogot.analysis.context

import io.github.kingg22.kogot.analysis.models.ClassInfo

/**
 * Abstract interface for analysis context.
 * This is backend-agnostic - implementations can be KSP, IDE-based, or test mocks.
 */
interface AnalysisContext {
    /**
     * The backend type identifier (e.g., "ksp", "idea", "test").
     */
    val backend: String

    /**
     * Resolves a class by its qualified name.
     * Returns null if the class cannot be found.
     */
    fun resolveClass(qualifiedName: String): ClassInfo?

    /**
     * Returns all classes available in the analysis scope.
     */
    fun getAllClasses(): List<ClassInfo>

    /**
     * Checks if a type is a Godot builtin type.
     */
    fun isGodotBuiltinType(qualifiedName: String): Boolean

    /**
     * Checks if a type is a valid exportable type.
     */
    fun isValidExportType(qualifiedName: String): Boolean
}
