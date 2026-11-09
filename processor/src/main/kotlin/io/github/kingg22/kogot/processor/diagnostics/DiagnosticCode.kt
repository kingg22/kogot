package io.github.kingg22.kogot.processor.diagnostics

/**
 * Unique identifier for a diagnostic (error/warning).
 * Format: KOGOT-XXX (e.g., KOGOT-001, KOGOT-002)
 */
data class DiagnosticCode(val code: String) {
    companion object {
        val INVALID_EXPORT_TYPE = DiagnosticCode("KOGOT-101")
        val INVALID_EXPORT_METHOD_TYPE = DiagnosticCode("KOGOT-102")
        val UNSUPPORTED_METHOD_DEFAULT_ARGUMENT = DiagnosticCode("KOGOT-103")
        val GENERATION_FAILED = DiagnosticCode("KOGOT-201")
        val INVALID_SIGNAL_PROPERTY_TYPE = DiagnosticCode("KOGOT-301")
        val REDUNDANT_SIGNAL_PARAMS = DiagnosticCode("KOGOT-302")
        val SIGNAL_NAME_MISMATCH = DiagnosticCode("KOGOT-303")
        val UNNAMED_SIGNAL_PARAMETER = DiagnosticCode("KOGOT-304")
        val INVALID_SIGNAL_PARAMETER_TYPE = DiagnosticCode("KOGOT-305")
        val DUPLICATE_SIGNAL_NAME = DiagnosticCode("KOGOT-306")
    }

    override fun toString(): String = code
}
