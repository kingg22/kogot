package io.github.kingg22.kogot.processor.diagnostics

/**
 * Unique identifier for a diagnostic (error/warning).
 * Format: KOGOT-XXX (e.g., KOGOT-001, KOGOT-002)
 */
data class DiagnosticCode(val code: String, val category: Category) {
    enum class Category {
        EXTRACTION,
        VALIDATION,
        GENERATION,
        INTERNAL,
    }

    companion object {
        // Extraction errors
        val UNABLE_TO_RESOLVE_TYPE = DiagnosticCode("KOGOT-001", Category.EXTRACTION)
        val UNABLE_TO_RESOLVE_ANNOTATION = DiagnosticCode("KOGOT-002", Category.EXTRACTION)
        val MALFORMED_ANNOTATION = DiagnosticCode("KOGOT-003", Category.EXTRACTION)

        // Validation errors
        val INVALID_EXPORT_TYPE = DiagnosticCode("KOGOT-101", Category.VALIDATION)
        val INVALID_RPC_SIGNATURE = DiagnosticCode("KOGOT-102", Category.VALIDATION)
        val INVALID_TOOL_CLASS = DiagnosticCode("KOGOT-103", Category.VALIDATION)
        val INVALID_NAMING = DiagnosticCode("KOGOT-104", Category.VALIDATION)
        val INVALID_GROUP_NAMING = DiagnosticCode("KOGOT-105", Category.VALIDATION)

        // Generation errors
        val GENERATION_FAILED = DiagnosticCode("KOGOT-201", Category.GENERATION)
        val FILE_WRITE_FAILED = DiagnosticCode("KOGOT-202", Category.GENERATION)

        // Internal errors
        val INTERNAL_ERROR = DiagnosticCode("KOGOT-999", Category.INTERNAL)
    }

    override fun toString(): String = code
}
