package io.github.kingg22.kogot.analysis.extractors

import io.github.kingg22.kogot.analysis.models.AnnotationInfo
import io.github.kingg22.kogot.analysis.models.ClassInfo
import io.github.kingg22.kogot.analysis.models.FunctionInfo
import io.github.kingg22.kogot.analysis.models.ParameterInfo
import io.github.kingg22.kogot.analysis.models.PropertyInfo
import io.github.kingg22.kogot.analysis.models.TypeInfo

/**
 * Interface for extracting class information from source code.
 * Backend-agnostic - implementations can use KSP, PSI, or other parsers.
 */
interface ClassExtractor {
    /**
     * Extracts class information from a declaration.
     *
     * @param declaration The class declaration (KSClassDeclaration, PsiClass, etc.)
     * @return ClassInfo representing the extracted data
     */
    fun extract(declaration: Any): ClassInfo
}

/**
 * Interface for extracting function information.
 */
interface FunctionExtractor {
    /**
     * Extracts function information from a declaration.
     *
     * @param declaration The function declaration
     * @return FunctionInfo representing the extracted data
     */
    fun extract(declaration: Any): FunctionInfo
}

/**
 * Interface for extracting property information.
 */
interface PropertyExtractor {
    /**
     * Extracts property information from a declaration.
     *
     * @param declaration The property declaration
     * @return PropertyInfo representing the extracted data
     */
    fun extract(declaration: Any): PropertyInfo
}

/**
 * Interface for extracting annotation information.
 */
interface AnnotationExtractor {
    /**
     * Extracts annotations from a declaration.
     *
     * @param declaration The annotated declaration
     * @return List of AnnotationInfo
     */
    fun extractAnnotations(declaration: Any): List<AnnotationInfo>
}

/**
 * Interface for resolving types.
 */
interface TypeExtractor {
    /**
     * Extracts type information from a type declaration.
     *
     * @param type The type (KSType, PsiType, etc.)
     * @return TypeInfo representing the extracted data
     */
    fun extractType(type: Any): TypeInfo
}
