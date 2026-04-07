package io.github.kingg22.kogot.processor.diagnostics

/**
 * A diagnostic message with rustc-style formatting.
 *
 * ```
 * error[KOGOT-001]: @Export on invalid type
 *   --> src/MyNode.kt:15:5
 *    |
 * 15 |     @Export var invalidProp: SomeClass
 *    |         ^^^^^^^ Unsupported type 'SomeClass'. Supported: primitives, Godot types.
 *    |
 *    = help: Use a Godot built-in type or register a custom class
 *    = note: This property will not appear in the Inspector
 * ```
 */
data class DiagnosticMessage(
    val code: DiagnosticCode,
    val severity: Severity,
    val mainMessage: String,
    val location: DiagnosticLocation,
    val contextLines: List<String> = emptyList(),
    val help: String? = null,
    val note: String? = null,
) {
    enum class Severity {
        ERROR,
        WARNING,
        INFO,
    }

    fun isError(): Boolean = severity == Severity.ERROR
    fun isWarning(): Boolean = severity == Severity.WARNING

    companion object {
        fun error(
            code: DiagnosticCode,
            message: String,
            location: DiagnosticLocation,
            help: String? = null,
            note: String? = null,
        ): DiagnosticMessage = DiagnosticMessage(
            code = code,
            severity = Severity.ERROR,
            mainMessage = message,
            location = location,
            help = help,
            note = note,
        )

        fun warning(
            code: DiagnosticCode,
            message: String,
            location: DiagnosticLocation,
            note: String? = null,
        ): DiagnosticMessage = DiagnosticMessage(
            code = code,
            severity = Severity.WARNING,
            mainMessage = message,
            location = location,
            note = note,
        )
    }
}
