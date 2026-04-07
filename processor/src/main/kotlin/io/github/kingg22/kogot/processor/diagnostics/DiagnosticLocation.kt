package io.github.kingg22.kogot.processor.diagnostics

/**
 * Represents a location in source code.
 */
data class DiagnosticLocation(val filePath: String, val line: Int, val column: Int) {
    companion object {
        val UNKNOWN = DiagnosticLocation("<unknown>", 0, 0)
    }
}
