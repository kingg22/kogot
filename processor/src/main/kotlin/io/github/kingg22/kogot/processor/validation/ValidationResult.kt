package io.github.kingg22.kogot.processor.validation

import io.github.kingg22.kogot.processor.diagnostics.DiagnosticMessage

/**
 * Accumulates validation errors and warnings.
 */
interface ValidationResult {
    val errors: List<DiagnosticMessage>
    val warnings: List<DiagnosticMessage>
    val isValid: Boolean

    fun addError(message: DiagnosticMessage)
    fun addWarning(message: DiagnosticMessage)
    fun merge(other: ValidationResult)
}

/**
 * Implementation of ValidationResult.
 */
class ValidationResultImpl : ValidationResult {
    private val _errors = mutableListOf<DiagnosticMessage>()
    private val _warnings = mutableListOf<DiagnosticMessage>()

    override val errors: List<DiagnosticMessage> get() = _errors
    override val warnings: List<DiagnosticMessage> get() = _warnings
    override val isValid: Boolean get() = _errors.isEmpty()

    override fun addError(message: DiagnosticMessage) {
        _errors.add(message)
    }

    override fun addWarning(message: DiagnosticMessage) {
        _warnings.add(message)
    }

    override fun merge(other: ValidationResult) {
        _errors.addAll(other.errors)
        _warnings.addAll(other.warnings)
    }
}
