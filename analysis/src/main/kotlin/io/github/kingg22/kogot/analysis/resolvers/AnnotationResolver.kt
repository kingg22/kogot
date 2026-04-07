package io.github.kingg22.kogot.analysis.resolvers

import io.github.kingg22.kogot.analysis.models.AnnotationInfo

/**
 * Interface for resolving annotations.
 */
interface AnnotationResolver {
    /**
     * Resolves all annotations on a declaration.
     *
     * @param declaration The annotated declaration
     * @return List of resolved AnnotationInfo
     */
    fun resolveAnnotations(declaration: Any): List<AnnotationInfo>

    /**
     * Checks if a declaration has a specific annotation.
     *
     * @param declaration The declaration to check
     * @param annotationName Fully qualified or short name of the annotation
     * @return true if the annotation is present
     */
    fun hasAnnotation(declaration: Any, annotationName: String): Boolean

    /**
     * Gets a specific annotation from a declaration.
     *
     * @param declaration The declaration
     * @param annotationName Fully qualified or short name
     * @return AnnotationInfo or null if not found
     */
    fun getAnnotation(declaration: Any, annotationName: String): AnnotationInfo?
}

/**
 * Interface for resolving types.
 */
interface TypeResolver {
    /**
     * Resolves a type from its name string.
     *
     * @param typeName The type name (e.g., "Int", "String", "io.github.kingg22.godot.api.builtin.Vector2")
     * @return Resolved type info or null if unresolved
     */
    fun resolve(typeName: String): io.github.kingg22.kogot.analysis.models.TypeInfo?
}
