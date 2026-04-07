package io.github.kingg22.kogot.processor.validation

import io.github.kingg22.kogot.analysis.models.ClassInfo

/**
 * Shared state used during validation.
 */
data class ValidationContext(val classInfo: ClassInfo, val options: ValidationOptions)

/**
 * Options that affect validation behavior.
 */
data class ValidationOptions(val allowExperimental: Boolean = false, val strictNaming: Boolean = true)

/**
 * Base interface for validators.
 */
interface Validator {
    val name: String

    /**
     * Validates the class and returns validation result.
     *
     * @param context The validation context
     * @return ValidationResult containing any errors/warnings found
     */
    fun validate(context: ValidationContext): ValidationResult
}

/**
 * A validator that can also enrich the model with additional info.
 */
interface EnrichingValidator : Validator {
    /**
     * Returns true if this validator can handle the given class.
     */
    fun canHandle(classInfo: ClassInfo): Boolean
}
